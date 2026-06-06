#!/usr/bin/env bash
# Lance tous les composants pour usage navigateur :
# 1) Services Docker, 2) Backend Spring Boot, 3) Frontend Angular.
# Usage : ./scripts/start-all.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Charger les variables d'environnement depuis .env
# shellcheck source=scripts/load-env.sh
source "$SCRIPT_DIR/load-env.sh"

echo "[all] Etape 1/3 - Demarrage des services externes..."
"$ROOT_DIR/scripts/start-services.sh"

echo "[all] Etape 2/3 - Demarrage backend en arriere-plan..."
"$ROOT_DIR/scripts/start-backend.sh" &
BACKEND_PID=$!

cleanup() {
  echo "[all] Arret du backend (PID $BACKEND_PID)..."
  kill "$BACKEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "[all] Etape 3/3 - Demarrage frontend..."
"$ROOT_DIR/scripts/start-web.sh"

