#!/usr/bin/env bash
# Build complet : JAR Spring Boot + application Flutter Desktop.
# Usage : ./scripts/build-all.sh [--win|--mac|--linux]
# Par défaut : --win (Windows).
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PLATFORM="${1:---win}"

echo "[build] === Étape 1/3 : Build JAR Spring Boot ==="
cd "$ROOT_DIR"
./gradlew infra:bootJar
JAR="$ROOT_DIR/infra/build/libs/ayan-dj-tools.jar"
if [ ! -f "$JAR" ]; then
  echo "[build] ERREUR : JAR introuvable à $JAR"
  exit 1
fi
echo "[build] JAR OK : $JAR"

echo "[build] === Étape 2/3 : Install dépendances Flutter ==="
cd "$ROOT_DIR/ayan_dj_tools_flutter"
flutter pub get

echo "[build] === Étape 3/3 : Build Flutter Desktop ($PLATFORM) ==="
case "$PLATFORM" in
  --win)   flutter build windows --release ;;
  --mac)   flutter build macos --release ;;
  --linux) flutter build linux --release ;;
  *)
    echo "[build] ERREUR : plateforme inconnue '$PLATFORM' (utiliser --win, --mac ou --linux)"
    exit 1
    ;;
esac

echo "[build] === Build terminé ==="
case "$PLATFORM" in
  --win)   echo "[build] Exécutable : ayan_dj_tools_flutter/build/windows/x64/runner/Release/" ;;
  --mac)   echo "[build] App : ayan_dj_tools_flutter/build/macos/Build/Products/Release/" ;;
  --linux) echo "[build] Exécutable : ayan_dj_tools_flutter/build/linux/x64/release/bundle/" ;;
esac
