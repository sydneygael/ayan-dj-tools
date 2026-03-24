@echo off
REM Démarre les services externes requis par Ayan DJ Tools.
REM Prérequis : Docker Desktop installé et lancé.

setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
echo [services] Démarrage des services Docker...
cd /d "%ROOT_DIR%"
docker-compose up -d

echo [services] Attente Redis...
:wait_redis
docker exec dj-tagger-redis redis-cli ping 2>nul | findstr /i "PONG" >nul 2>&1
if errorlevel 1 (
  timeout /t 1 /nobreak >nul
  goto wait_redis
)
echo [services] Redis OK

echo [services] Attente Qdrant...
:wait_qdrant
curl -sf http://localhost:6333/healthz >nul 2>&1
if errorlevel 1 (
  timeout /t 1 /nobreak >nul
  goto wait_qdrant
)
echo [services] Qdrant OK

echo [services] Attente Ollama...
:wait_ollama
curl -sf http://localhost:11434/api/tags >nul 2>&1
if errorlevel 1 (
  timeout /t 1 /nobreak >nul
  goto wait_ollama
)
echo [services] Ollama OK

echo [services] Vérification des modèles AI...
docker exec dj-tagger-ollama ollama list 2>nul | findstr /i "mistral" >nul 2>&1
if errorlevel 1 (
  echo [services] Pull mistral...
  docker exec dj-tagger-ollama ollama pull mistral
)
docker exec dj-tagger-ollama ollama list 2>nul | findstr /i "nomic-embed-text" >nul 2>&1
if errorlevel 1 (
  echo [services] Pull nomic-embed-text...
  docker exec dj-tagger-ollama ollama pull nomic-embed-text
)

echo [services] Tous les services sont prets.
