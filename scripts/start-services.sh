#!/usr/bin/env bash
# Démarre les services externes requis par Ayan DJ Tools.
# Prérequis : Docker + docker-compose installés.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "[services] Démarrage des services Docker..."
cd "$ROOT_DIR"
docker-compose up -d

echo "[services] Attente Redis..."
until docker exec dj-tagger-redis redis-cli ping 2>/dev/null | grep -q PONG; do
  sleep 1
done
echo "[services] Redis OK"

echo "[services] Attente Qdrant..."
until curl -sf http://localhost:6333/healthz > /dev/null 2>&1; do
  sleep 1
done
echo "[services] Qdrant OK"

echo "[services] Attente Ollama..."
until curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; do
  sleep 1
done
echo "[services] Ollama OK"

echo "[services] Vérification des modèles AI..."
if ! docker exec dj-tagger-ollama ollama list 2>/dev/null | grep -q mistral; then
  echo "[services] Pull mistral..."
  docker exec dj-tagger-ollama ollama pull mistral
fi
if ! docker exec dj-tagger-ollama ollama list 2>/dev/null | grep -q nomic-embed-text; then
  echo "[services] Pull nomic-embed-text..."
  docker exec dj-tagger-ollama ollama pull nomic-embed-text
fi

echo "[services] Tous les services sont prêts."
