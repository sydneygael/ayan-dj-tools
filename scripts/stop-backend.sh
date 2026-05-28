#!/usr/bin/env bash
# Arrete le backend Spring Boot lance via les scripts du projet.
# Usage : ./scripts/stop-backend.sh

set -e

echo "[backend] Recherche des processus backend..."
PIDS="$(pgrep -f 'infra:bootRun|com.djtools.ayan.musictagger.MusicTaggerApplication|ayan-dj-tools.jar' || true)"

if [ -z "$PIDS" ]; then
  echo "[backend] Aucun processus backend detecte."
  exit 0
fi

echo "[backend] Arret des PID: $PIDS"
kill $PIDS
echo "[backend] Backend arrete."

