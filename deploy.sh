#!/bin/bash

# ==============================================================================
# Script de Déploiement - Sopikeur Backend
# ==============================================================================
# Usage: ./deploy.sh [SHA]
# Exemple: ./deploy.sh abc1234
#          ./deploy.sh latest
# ==============================================================================

set -e  # Exit on error

# Couleurs pour output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
ENV_FILE=".env.prod"
COMPOSE_FILE="docker-compose.prod.yml"
IMAGE_TAG="${1:-latest}"

# ==============================================================================
# Fonctions Helper
# ==============================================================================

print_info() {
    echo -e "${BLUE}ℹ ${1}${NC}"
}

print_success() {
    echo -e "${GREEN}✓ ${1}${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ${1}${NC}"
}

print_error() {
    echo -e "${RED}✗ ${1}${NC}"
    exit 1
}

# ==============================================================================
# Vérifications pré-déploiement
# ==============================================================================

print_info "Déploiement de sopikeur-back:${IMAGE_TAG}"
echo ""

# Vérifier que Docker est installé
if ! command -v docker &> /dev/null; then
    print_error "Docker n'est pas installé"
fi

# Vérifier que Docker Compose est installé
if ! docker compose version &> /dev/null; then
    print_error "Docker Compose n'est pas installé"
fi

# Vérifier que docker-compose.prod.yml existe
if [ ! -f "$COMPOSE_FILE" ]; then
    print_error "Fichier $COMPOSE_FILE introuvable"
fi

print_success "Vérifications OK"
echo ""

# ==============================================================================
# Mise à jour du tag dans .env.prod
# ==============================================================================

print_info "Mise à jour de APP_IMAGE_TAG dans $ENV_FILE"

# Backup de .env.prod
cp "$ENV_FILE" "${ENV_FILE}.backup"
print_success "Backup créé: ${ENV_FILE}.backup"

# Mettre à jour ou ajouter APP_IMAGE_TAG
if grep -q "^APP_IMAGE_TAG=" "$ENV_FILE"; then
    # Remplacer la ligne existante
    sed -i.tmp "s/^APP_IMAGE_TAG=.*/APP_IMAGE_TAG=${IMAGE_TAG}/" "$ENV_FILE"
    rm -f "${ENV_FILE}.tmp"
else
    # Ajouter la ligne au début
    echo "APP_IMAGE_TAG=${IMAGE_TAG}" | cat - "$ENV_FILE" > temp && mv temp "$ENV_FILE"
fi

print_success "APP_IMAGE_TAG mis à jour: ${IMAGE_TAG}"
echo ""

# ==============================================================================
# Pull de l'image Docker
# ==============================================================================

print_info "📦 Pull de l'image depuis GHCR"

if docker compose -f "$COMPOSE_FILE" pull backend; then
    print_success "Image téléchargée avec succès"
else
    print_error "Échec du pull de l'image. Vérifiez votre connexion et les credentials GHCR"
fi

echo ""

# ==============================================================================
# Déploiement
# ==============================================================================

print_info "Déploiement des services"

if docker compose -f "$COMPOSE_FILE" up -d; then
    print_success "Services démarrés"
else
    print_error "Échec du démarrage des services"
fi

echo ""

# ==============================================================================
# Vérification du déploiement
# ==============================================================================

print_info "Vérification de la santé du backend (peut prendre jusqu'à 60s)"

# Attendre que le container soit démarré
sleep 5

# Vérifier le status du container
if docker ps | grep -q "sopikeur-backend"; then
    print_success "Container backend démarré"
else
    print_error "Container backend non trouvé"
fi

# Attendre que l'application soit prête (healthcheck)
print_info "Attente du healthcheck (timeout 90s)..."

COUNTER=0
MAX_ATTEMPTS=30

while [ $COUNTER -lt $MAX_ATTEMPTS ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "Application répond correctement !"
        break
    fi

    echo -n "."
    sleep 3
    COUNTER=$((COUNTER+1))
done

echo ""

if [ $COUNTER -eq $MAX_ATTEMPTS ]; then
    print_warning "Timeout atteint. L'application met plus de temps que prévu à démarrer."
    print_info "Vérifiez les logs: docker compose -f $COMPOSE_FILE logs backend"
else
    # Test de l'endpoint health
    HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

    if [ "$HEALTH_STATUS" = "UP" ]; then
        print_success "Health check: UP ✓"
    else
        print_warning "Health check: $HEALTH_STATUS"
    fi
fi

echo ""

# ==============================================================================
# Résumé et logging
# ==============================================================================

print_info "Résumé du déploiement"
echo ""
echo "Image déployée:    ghcr.io/gust2301/sopikeur-back:${IMAGE_TAG}"
echo "Date/Heure:        $(date)"
echo "Services actifs:   $(docker compose -f $COMPOSE_FILE ps --services --filter "status=running" | wc -l)"
echo ""

# Logger le déploiement
echo "$(date +%Y-%m-%d\ %H:%M:%S) - Deployed: ${IMAGE_TAG}" >> deploy-history.txt
print_success "Déploiement logged dans deploy-history.txt"

echo ""
print_success "Déploiement terminé avec succès !"
echo ""

# ==============================================================================
# Commandes utiles post-déploiement
# ==============================================================================

print_info "Commandes utiles:"
echo ""
echo "  Voir les logs:              docker compose -f $COMPOSE_FILE logs -f backend"
echo "  Status des services:        docker compose -f $COMPOSE_FILE ps"
echo "  Health check:               curl http://localhost:8080/actuator/health"
echo "  Redémarrer backend:         docker compose -f $COMPOSE_FILE restart backend"
echo "  Arrêter tous les services:  docker compose -f $COMPOSE_FILE down"
echo ""

# ==============================================================================
# Instructions de rollback
# ==============================================================================

if [ "$IMAGE_TAG" != "latest" ]; then
    print_info "Pour rollback, exécutez:"
    echo ""
    echo "  ./deploy.sh [ANCIEN_SHA]"
    echo ""
fi
