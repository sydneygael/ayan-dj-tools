#!/usr/bin/env bash
# Lance le frontend Angular web.
# Usage : ./scripts/start-web.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
WEB_DIR="$ROOT_DIR/ayan_dj_tools_web"

if [ ! -f "$WEB_DIR/package.json" ]; then
  echo "[web] ERREUR : projet Angular introuvable dans $WEB_DIR"
  exit 1
fi

echo "[web] Installation des dependances npm..."
cd "$WEB_DIR"
npm install

echo "[web] Demarrage Angular dev server sur http://127.0.0.1:4200 ..."
npm run start -- --host 127.0.0.1 --port 4200

