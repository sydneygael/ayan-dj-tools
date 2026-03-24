#!/usr/bin/env bash
# Build complet : JAR Spring Boot + installeur Electron.
# Usage : ./scripts/build-all.sh [--win|--mac|--linux]
# Par défaut, détecte la plateforme courante.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PLATFORM="${1:-}"

echo "[build] === Étape 1/3 : Build JAR Spring Boot ==="
cd "$ROOT_DIR"
./gradlew infra:bootJar
JAR="$ROOT_DIR/infra/build/libs/ayan-dj-tools.jar"
if [ ! -f "$JAR" ]; then
  echo "[build] ERREUR : JAR introuvable à $JAR"
  exit 1
fi
echo "[build] JAR OK : $JAR"

echo "[build] === Étape 2/3 : Install dépendances frontend ==="
cd "$ROOT_DIR/music-tagger-ui"
npm ci

echo "[build] === Étape 3/3 : Build installeur Electron ==="
if [ -n "$PLATFORM" ]; then
  npm run "electron:dist$PLATFORM"
else
  npm run electron:dist
fi

echo "[build] === Build terminé ==="
echo "[build] Installeur(s) disponible(s) dans : music-tagger-ui/release/"
ls "$ROOT_DIR/music-tagger-ui/release/" 2>/dev/null || true
