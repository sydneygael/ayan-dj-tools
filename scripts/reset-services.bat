@echo off
REM Vide les volumes et donnees des services Docker d'Ayan DJ Tools.
REM
REM Usage :
REM   reset-services.bat              -- Redis + PostgreSQL + Qdrant (conserve Ollama)
REM   reset-services.bat --redis      -- Redis seulement
REM   reset-services.bat --postgres   -- PostgreSQL seulement
REM   reset-services.bat --qdrant     -- Qdrant seulement
REM   reset-services.bat --ollama     -- Ollama seulement (modeles a re-telecharger)
REM   reset-services.bat --all        -- Tout
REM   reset-services.bat --restart    -- Redemarre les services apres reset
REM   Flags combinables : reset-services.bat --redis --qdrant --restart

setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "SCRIPT_DIR=%~dp0"

set RESET_REDIS=0
set RESET_POSTGRES=0
set RESET_QDRANT=0
set RESET_OLLAMA=0
set DO_RESTART=0
set HAS_FLAG=0

for %%A in (%*) do (
  set HAS_FLAG=1
  if "%%A"=="--redis"    set RESET_REDIS=1
  if "%%A"=="--postgres" set RESET_POSTGRES=1
  if "%%A"=="--qdrant"   set RESET_QDRANT=1
  if "%%A"=="--ollama"   set RESET_OLLAMA=1
  if "%%A"=="--all"      set RESET_REDIS=1 & set RESET_POSTGRES=1 & set RESET_QDRANT=1 & set RESET_OLLAMA=1
  if "%%A"=="--restart"  set DO_RESTART=1
)

if %HAS_FLAG%==0 (
  set RESET_REDIS=1
  set RESET_POSTGRES=1
  set RESET_QDRANT=1
)

echo.
echo ======================================
echo   Reset services -- Ayan DJ Tools
echo ======================================
echo.
if %RESET_REDIS%==1    echo   * Redis      -- cache lookups, chat-memory, plans
if %RESET_POSTGRES%==1 echo   * PostgreSQL -- tracks scannees, metadonnees enrichies
if %RESET_QDRANT%==1   echo   * Qdrant     -- vecteurs RAG
if %RESET_OLLAMA%==1   echo   * Ollama     -- modeles AI  [!] re-telechargement requis ensuite
echo.
echo --------------------------------------
set /p confirm="Confirmer ? [y/N] "
if /i not "%confirm%"=="y" (
  echo Annule.
  exit /b 0
)
echo.

cd /d "%ROOT_DIR%"

echo [reset] Arret des conteneurs...
docker-compose down

if %RESET_REDIS%==1 (
  echo [reset] Suppression volume Redis...
  docker volume rm ayan-dj-tools_redis-data 2>nul || echo   [warn] volume redis-data absent
)

if %RESET_POSTGRES%==1 (
  echo [reset] Suppression volume PostgreSQL...
  docker volume rm ayan-dj-tools_postgres-data 2>nul || echo   [warn] volume postgres-data absent
)

if %RESET_QDRANT%==1 (
  echo [reset] Suppression volume Qdrant...
  docker volume rm ayan-dj-tools_qdrant-data 2>nul || echo   [warn] volume qdrant-data absent
)

if %RESET_OLLAMA%==1 (
  echo [reset] Suppression volume Ollama...
  docker volume rm ayan-dj-tools_ollama-data 2>nul || echo   [warn] volume ollama-data absent
)

echo.
echo [reset] Termine.

if %DO_RESTART%==1 (
  echo [reset] Redemarrage des services...
  call "%SCRIPT_DIR%start-services.bat"
) else (
  echo [reset] Lance 'scripts\start-services.bat' pour redemarrer.
  if %RESET_OLLAMA%==1 echo [reset] N'oublie pas de re-pull les modeles Ollama apres le demarrage.
)
