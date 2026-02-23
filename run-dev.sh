#!/usr/bin/env bash
# Lance le backend Spring Boot en mode dev avec les variables de .env.dev
set -e

ENV_FILE="$(dirname "$0")/.env.dev"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌  Fichier .env.dev introuvable. Copie .env.dev.example et remplis les valeurs."
  exit 1
fi

# Exporter toutes les variables du fichier .env.dev (ignore lignes vides et commentaires)
set -o allexport
# shellcheck source=/dev/null
source "$ENV_FILE"
set +o allexport

echo "✅  Variables chargées depuis .env.dev"
echo "→  Profil Spring : $SPRING_PROFILES_ACTIVE"
echo "→  DB : $SPRING_DATASOURCE_URL"
echo ""

./mvnw spring-boot:run
