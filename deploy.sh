#!/bin/bash

# ==============================================================================
# Script de Déploiement - Sopiker Backend (VPS, /srv/sopiker)
# ==============================================================================
# À exécuter SUR LE VPS, depuis n'importe quel dossier :
#   sudo /srv/sopiker/deploy.sh <production|staging> <TAG>
# Exemples :
#   sudo /srv/sopiker/deploy.sh staging abc1234
#   sudo /srv/sopiker/deploy.sh production abc1234
# Rollback : relancer avec l'ancien TAG (affiché à la fin de chaque déploiement).
# ==============================================================================

set -euo pipefail

# Couleurs pour output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info()    { echo -e "${BLUE}ℹ ${1}${NC}"; }
print_success() { echo -e "${GREEN}✓ ${1}${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ ${1}${NC}"; }
print_error()   { echo -e "${RED}✗ ${1}${NC}"; exit 1; }

usage() {
    echo "Usage: sudo $0 <production|staging> <TAG>"
    echo "  TAG : SHA court publié par GitHub Actions (ex: abc1234) ou 'latest'"
    exit 1
}

# ==============================================================================
# Paramètres
# ==============================================================================

TARGET="${1:-}"
IMAGE_TAG="${2:-}"
SOPIKER_ROOT="${SOPIKER_ROOT:-/srv/sopiker}"

case "$TARGET" in
    production) SERVICE=backend;         CONTAINER=sopiker-backend ;;
    staging)    SERVICE=backend_staging; CONTAINER=sopiker-backend-staging ;;
    *) usage ;;
esac
[ -n "$IMAGE_TAG" ] || usage
[[ "$IMAGE_TAG" =~ ^[A-Za-z0-9._-]+$ ]] || print_error "TAG invalide: $IMAGE_TAG"

DIR="$SOPIKER_ROOT/$TARGET"
ENV_FILE="$DIR/.env"

# ==============================================================================
# Vérifications pré-déploiement
# ==============================================================================

print_info "Déploiement de sopikeur-back:${IMAGE_TAG} sur ${TARGET}"
echo ""

docker info > /dev/null 2>&1 || print_error "Docker inaccessible (lancer avec sudo)"
docker compose version > /dev/null 2>&1 || print_error "Docker Compose n'est pas installé"
[ -f "$DIR/compose.yml" ] || print_error "Fichier $DIR/compose.yml introuvable"
[ -f "$ENV_FILE" ] || print_error "Fichier $ENV_FILE introuvable"

cd "$DIR"

if grep -q '^COMPOSE_FILE=.*compose.live.yml' "$ENV_FILE"; then
    print_info "Mode: LIVE (accès Internet sortant activé)"
else
    print_warning "Mode: ISOLÉ (pas d'accès Internet : paiements, e-mails et notifications bloqués)"
fi

print_success "Vérifications OK"
echo ""

# ==============================================================================
# Mise à jour du tag dans .env
# ==============================================================================

PREVIOUS_TAG=$(grep -E '^APP_IMAGE_TAG=' "$ENV_FILE" | cut -d= -f2- || true)
print_info "Tag actuel: ${PREVIOUS_TAG:-aucun} → nouveau: ${IMAGE_TAG}"

# Backup de .env (mêmes permissions 600)
cp -p "$ENV_FILE" "${ENV_FILE}.backup"
print_success "Backup créé: ${ENV_FILE}.backup"

if grep -q '^APP_IMAGE_TAG=' "$ENV_FILE"; then
    sed -i "s/^APP_IMAGE_TAG=.*/APP_IMAGE_TAG=${IMAGE_TAG}/" "$ENV_FILE"
else
    echo "APP_IMAGE_TAG=${IMAGE_TAG}" >> "$ENV_FILE"
fi
print_success "APP_IMAGE_TAG mis à jour: ${IMAGE_TAG}"
echo ""

# ==============================================================================
# Pull de l'image Docker
# ==============================================================================

print_info "📦 Pull de l'image depuis GHCR"
if ! docker compose pull "$SERVICE"; then
    cp -p "${ENV_FILE}.backup" "$ENV_FILE"
    print_error "Échec du pull (tag inexistant ou 'docker login ghcr.io' manquant). .env restauré."
fi
print_success "Image téléchargée avec succès"
echo ""

# ==============================================================================
# Déploiement
# ==============================================================================

print_info "Déploiement du service ${SERVICE}"
docker compose up -d "$SERVICE" || print_error "Échec du démarrage des services"
print_success "Services démarrés"
echo ""

# ==============================================================================
# Vérification du déploiement (healthcheck Docker, timeout 180s)
# ==============================================================================

print_info "Attente du healthcheck (timeout 180s)..."
STATUS=starting
for _ in $(seq 1 60); do
    STATUS=$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo missing)
    [ "$STATUS" = healthy ] || [ "$STATUS" = unhealthy ] && break
    echo -n "."
    sleep 3
done
echo ""

if [ "$STATUS" = healthy ]; then
    print_success "Health check: healthy ✓"
else
    print_warning "Health check: ${STATUS}"
    print_info "Logs: docker compose -f $DIR/compose.yml logs --tail=100 $SERVICE"
    print_info "Rollback: sudo $0 $TARGET ${PREVIOUS_TAG:-<ANCIEN_TAG>}"
    exit 1
fi
echo ""

# ==============================================================================
# Résumé et logging
# ==============================================================================

echo "Environnement:     ${TARGET}"
echo "Image déployée:    ghcr.io/gust2301/sopikeur-back:${IMAGE_TAG}"
echo "Date/Heure:        $(date)"
echo ""

echo "$(date +%Y-%m-%d\ %H:%M:%S) - ${TARGET} - Deployed: ${IMAGE_TAG} (previous: ${PREVIOUS_TAG:-none})" >> "$DIR/deploy-history.txt"
print_success "Déploiement logged dans $DIR/deploy-history.txt"
echo ""
print_success "Déploiement terminé avec succès !"
echo ""

print_info "Commandes utiles (dans $DIR) :"
echo "  Logs:      sudo docker compose logs -f $SERVICE"
echo "  Status:    sudo docker compose ps"
echo ""
if [ -n "$PREVIOUS_TAG" ] && [ "$PREVIOUS_TAG" != "$IMAGE_TAG" ]; then
    print_info "Pour rollback: sudo $0 $TARGET $PREVIOUS_TAG"
fi
