#!/usr/bin/env bash
# Vide les volumes et données des services Docker d'Ayan DJ Tools.
#
# Usage :
#   ./reset-services.sh              — Redis + PostgreSQL + Qdrant (conserve Ollama)
#   ./reset-services.sh --redis      — Redis seulement
#   ./reset-services.sh --postgres   — PostgreSQL seulement
#   ./reset-services.sh --qdrant     — Qdrant seulement
#   ./reset-services.sh --ollama     — Ollama seulement (modèles à re-télécharger)
#   ./reset-services.sh --all        — Tout
#   ./reset-services.sh --restart    — Redémarre les services après reset
#   Flags combinables : --redis --qdrant --restart

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

RESET_REDIS=false
RESET_POSTGRES=false
RESET_QDRANT=false
RESET_OLLAMA=false
DO_RESTART=false

if [[ $# -eq 0 ]]; then
  RESET_REDIS=true
  RESET_POSTGRES=true
  RESET_QDRANT=true
fi

for arg in "$@"; do
  case $arg in
    --redis)    RESET_REDIS=true ;;
    --postgres) RESET_POSTGRES=true ;;
    --qdrant)   RESET_QDRANT=true ;;
    --ollama)   RESET_OLLAMA=true ;;
    --all)      RESET_REDIS=true; RESET_POSTGRES=true; RESET_QDRANT=true; RESET_OLLAMA=true ;;
    --restart)  DO_RESTART=true ;;
    *)          echo "Option inconnue : $arg  (--redis|--postgres|--qdrant|--ollama|--all|--restart)"; exit 1 ;;
  esac
done

echo ""
echo "======================================"
echo "  Reset services — Ayan DJ Tools"
echo "======================================"
echo ""
$RESET_REDIS    && echo "  ● Redis      — cache lookups, chat-memory, plans"
$RESET_POSTGRES && echo "  ● PostgreSQL — tracks scannées, métadonnées enrichies"
$RESET_QDRANT   && echo "  ● Qdrant     — vecteurs RAG"
$RESET_OLLAMA   && echo "  ● Ollama     — modèles AI  ⚠ re-téléchargement requis ensuite"
echo ""
echo "--------------------------------------"
read -rp "Confirmer ? [y/N] " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Annulé."; exit 0; }
echo ""

cd "$ROOT_DIR"

echo "[reset] Arrêt des conteneurs..."
docker-compose down

# Détecte le préfixe de volume Docker Compose (= nom du projet = dossier en minuscules)
PROJECT=$(basename "$ROOT_DIR" | tr '[:upper:]' '[:lower:]')

rm_volume() {
  local vol="$1"
  docker volume rm "${PROJECT}_${vol}" 2>/dev/null \
    || docker volume rm "ayan-dj-tools_${vol}" 2>/dev/null \
    || echo "  [warn] volume '${vol}' introuvable ou déjà absent"
}

$RESET_REDIS    && { echo "[reset] Suppression volume Redis..."      ; rm_volume "redis-data"    ; }
$RESET_POSTGRES && { echo "[reset] Suppression volume PostgreSQL..." ; rm_volume "postgres-data" ; }
$RESET_QDRANT   && { echo "[reset] Suppression volume Qdrant..."     ; rm_volume "qdrant-data"   ; }
$RESET_OLLAMA   && { echo "[reset] Suppression volume Ollama..."     ; rm_volume "ollama-data"   ; }

echo ""
echo "[reset] Terminé."

if $DO_RESTART; then
  echo "[reset] Redémarrage des services..."
  "$SCRIPT_DIR/start-services.sh"
else
  echo "[reset] Lance './scripts/start-services.sh' pour redémarrer."
  $RESET_OLLAMA && echo "[reset] N'oublie pas de re-pull les modèles Ollama après le démarrage."
fi
