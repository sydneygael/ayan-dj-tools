#!/usr/bin/env bash
# Lance le backend Spring Boot pour l'usage web.
# Usage : ./scripts/start-backend.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "[backend] Demarrage Spring Boot (infra:bootRun)..."
cd "$ROOT_DIR"
./gradlew infra:bootRun

