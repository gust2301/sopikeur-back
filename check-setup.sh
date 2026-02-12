#!/bin/bash

# ==============================================================================
# Script de Vérification - Configuration Docker/CI-CD
# ==============================================================================
# Vérifie que tous les fichiers et configurations sont en place
# ==============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

ERRORS=0
WARNINGS=0

# ==============================================================================
# Vérification des Fichiers Docker
# ==============================================================================

print_header "1. Fichiers Docker"

# Dockerfile
if [ -f "Dockerfile" ]; then
    print_success "Dockerfile présent"

    # Vérifier multi-stage
    if grep -q "AS builder" Dockerfile && grep -q "FROM.*jre" Dockerfile; then
        print_success "  Multi-stage build détecté"
    else
        print_warning "  Multi-stage build non détecté"
        WARNINGS=$((WARNINGS+1))
    fi

    # Vérifier non-root user
    if grep -q "USER spring" Dockerfile; then
        print_success "  Utilisateur non-root configuré"
    else
        print_error "  Utilisateur non-root manquant (sécurité)"
        ERRORS=$((ERRORS+1))
    fi

    # Vérifier healthcheck
    if grep -q "HEALTHCHECK" Dockerfile; then
        print_success "  Healthcheck configuré"
    else
        print_warning "  Healthcheck manquant (recommandé)"
        WARNINGS=$((WARNINGS+1))
    fi
else
    print_error "Dockerfile manquant"
    ERRORS=$((ERRORS+1))
fi

# .dockerignore
if [ -f ".dockerignore" ]; then
    print_success ".dockerignore présent"

    # Vérifier que .git est ignoré
    if grep -q "^\.git$" .dockerignore; then
        print_success "  .git exclu (optimisation)"
    fi

    # Vérifier que secrets sont ignorés
    if grep -q "\.env\.prod" .dockerignore; then
        print_success "  Secrets exclus"
    fi
else
    print_warning ".dockerignore manquant (build sera plus lent)"
    WARNINGS=$((WARNINGS+1))
fi

# docker-compose.prod.yml
if [ -f "docker-compose.prod.yml" ]; then
    print_success "docker-compose.prod.yml présent"

    # Vérifier MySQL
    if grep -q "mysql:" docker-compose.prod.yml; then
        print_success "  Service MySQL configuré"
    fi

    # Vérifier backend
    if grep -q "backend:" docker-compose.prod.yml; then
        print_success "  Service backend configuré"
    fi

    # Vérifier que MySQL n'est PAS exposé
    if grep -A 5 "mysql:" docker-compose.prod.yml | grep -q "ports:"; then
        print_warning "  MySQL semble exposé publiquement (risque de sécurité)"
        WARNINGS=$((WARNINGS+1))
    else
        print_success "  MySQL non exposé (sécurité OK)"
    fi

    # Vérifier image GHCR
    if grep -q "ghcr.io" docker-compose.prod.yml; then
        print_success "  Image GHCR configurée"
    else
        print_error "  Image GHCR non trouvée"
        ERRORS=$((ERRORS+1))
    fi

    # Vérifier healthcheck MySQL
    if grep -A 5 "mysql:" docker-compose.prod.yml | grep -q "healthcheck:"; then
        print_success "  Healthcheck MySQL configuré"
    fi

    # Vérifier depends_on
    if grep -q "depends_on:" docker-compose.prod.yml; then
        print_success "  Dépendances entre services configurées"
    fi
else
    print_error "docker-compose.prod.yml manquant"
    ERRORS=$((ERRORS+1))
fi

# ==============================================================================
# Vérification Configuration
# ==============================================================================

print_header "2. Configuration"


# .env.prod
if [ -f ".env.prod" ]; then
    print_success ".env.prod présent"

    # Vérifier permissions
    PERMS=$(stat -c "%a" .env.prod 2>/dev/null || stat -f "%OLp" .env.prod 2>/dev/null)
    if [ "$PERMS" = "600" ]; then
        print_success "  Permissions correctes (600)"
    else
        print_warning "  Permissions: $PERMS (recommandé: 600)"
        print_info "    Exécuter: chmod 600 .env.prod"
    fi

    # Vérifier variables clés
    if grep -q "APP_IMAGE_TAG=" .env.prod; then
        print_success "  APP_IMAGE_TAG défini"
    fi

    if grep -q "MYSQL_PASSWORD=" .env.prod; then
        print_success "  MYSQL_PASSWORD défini"
    fi

    if grep -q "APP_JWT_SECRET=" .env.prod; then
        print_success "  APP_JWT_SECRET défini"
    fi

    # Vérifier mots de passe par défaut
    if grep -q "change-me\|CHANGEZ_MOI" .env.prod; then
        print_error "  Mots de passe par défaut détectés (SÉCURITÉ)"
        print_info "    Générer des secrets: openssl rand -base64 32"
        ERRORS=$((ERRORS+1))
    else
        print_success "  Pas de mots de passe par défaut détectés"
    fi
else
    print_error ".env.prod manquant (requis pour déploiement)"
    print_info "  Créer le fichier avec les variables nécessaires"
    ERRORS=$((ERRORS+1))
fi

# .gitignore
if [ -f ".gitignore" ]; then
    print_success ".gitignore présent"

    # Vérifier que .env.prod est ignoré
    if grep -q "\.env\.prod" .gitignore; then
        print_success "  .env.prod ignoré par Git (sécurité)"
    else
        print_error "  .env.prod NON ignoré (risque de commit de secrets)"
        ERRORS=$((ERRORS+1))
    fi
else
    print_error ".gitignore manquant"
    ERRORS=$((ERRORS+1))
fi

# ==============================================================================
# Vérification CI/CD
# ==============================================================================

print_header "3. CI/CD (GitHub Actions)"

if [ -f ".github/workflows/docker-image.yml" ]; then
    print_success "Workflow GitHub Actions présent"

    # Vérifier login GHCR
    if grep -q "ghcr.io" .github/workflows/docker-image.yml; then
        print_success "  Login GHCR configuré"
    fi

    # Vérifier tags
    if grep -q "type=sha" .github/workflows/docker-image.yml; then
        print_success "  Tags SHA configurés"
    fi

    # Vérifier cache
    if grep -q "cache-from" .github/workflows/docker-image.yml; then
        print_success "  Cache Docker Buildx activé"
    fi

    # Vérifier triggers
    if grep -q "push:" .github/workflows/docker-image.yml; then
        print_success "  Déclencheurs configurés"
    fi
else
    print_error "Workflow GitHub Actions manquant"
    print_info "  Créer: .github/workflows/docker-image.yml"
    ERRORS=$((ERRORS+1))
fi

# ==============================================================================
# Vérification Scripts
# ==============================================================================

print_header "4. Scripts Helper"

if [ -f "deploy.sh" ]; then
    print_success "Script deploy.sh présent"

    # Vérifier exécutable
    if [ -x "deploy.sh" ]; then
        print_success "  Script exécutable"
    else
        print_warning "  Script non exécutable"
        print_info "    Exécuter: chmod +x deploy.sh"
        WARNINGS=$((WARNINGS+1))
    fi
else
    print_warning "Script deploy.sh manquant (optionnel mais utile)"
    WARNINGS=$((WARNINGS+1))
fi

# ==============================================================================
# Vérification Documentation
# ==============================================================================

print_header "5. Documentation"

DOCS=("DEPLOY.md" "DOCKER_CHOICES.md" "INFRASTRUCTURE_SUMMARY.md" "README.md")

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        print_success "$doc présent"
    else
        print_warning "$doc manquant"
        WARNINGS=$((WARNINGS+1))
    fi
done

# ==============================================================================
# Vérification Outils Locaux
# ==============================================================================

print_header "6. Outils Requis (Local)"

# Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | grep -oP '\d+\.\d+\.\d+' | head -1)
    print_success "Docker installé (v$DOCKER_VERSION)"
else
    print_error "Docker non installé"
    ERRORS=$((ERRORS+1))
fi

# Docker Compose
if docker compose version &> /dev/null; then
    COMPOSE_VERSION=$(docker compose version | grep -oP '\d+\.\d+\.\d+' | head -1)
    print_success "Docker Compose installé (v$COMPOSE_VERSION)"
else
    print_error "Docker Compose non installé"
    ERRORS=$((ERRORS+1))
fi

# Git
if command -v git &> /dev/null; then
    GIT_VERSION=$(git --version | grep -oP '\d+\.\d+\.\d+')
    print_success "Git installé (v$GIT_VERSION)"
else
    print_warning "Git non installé (requis pour CI/CD)"
    WARNINGS=$((WARNINGS+1))
fi

# Maven (optionnel)
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -1 | grep -oP '\d+\.\d+\.\d+')
    print_success "Maven installé (v$MVN_VERSION) - optionnel en prod"
else
    print_info "Maven non installé (pas requis si build via Docker)"
fi

# ==============================================================================
# Résumé
# ==============================================================================

print_header "Résumé"

echo ""
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓✓✓ Configuration parfaite ! Prêt pour le déploiement.${NC}"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS avertissement(s) - Configuration fonctionnelle mais peut être améliorée${NC}"
else
    echo -e "${RED}✗ $ERRORS erreur(s) trouvée(s) - Corriger avant déploiement${NC}"
    echo -e "${YELLOW}⚠ $WARNINGS avertissement(s)${NC}"
fi

echo ""

# ==============================================================================
# Prochaines Étapes
# ==============================================================================

if [ $ERRORS -eq 0 ]; then
    print_info "Prochaines étapes:"
    echo ""
    echo "  1. Vérifier/créer .env.prod avec toutes les variables nécessaires"
    echo ""
    echo "  2. Configurer les secrets (mots de passe, JWT)"
    echo "     nano .env.prod"
    echo ""
    echo "  3. Commiter et push sur GitHub"
    echo "     git add ."
    echo "     git commit -m 'feat: add Docker + CI/CD infrastructure'"
    echo "     git push origin main"
    echo ""
    echo "  4. Vérifier le build sur GitHub Actions"
    echo "     https://github.com/gust2301/sopikeur-back/actions"
    echo ""
    echo "  5. Déployer sur VPS"
    echo "     ./deploy.sh <SHA>"
    echo ""
fi

exit $ERRORS
