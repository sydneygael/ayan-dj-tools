@echo off
REM Build complet : JAR Spring Boot + installeur Electron Windows.
REM Usage : build-all.bat

setlocal enabledelayedexpansion
set "ROOT_DIR=%~dp0.."

echo [build] === Etape 1/3 : Build JAR Spring Boot ===
cd /d "%ROOT_DIR%"
call gradlew.bat infra:bootJar
if errorlevel 1 (
  echo [build] ERREUR : gradlew infra:bootJar a echoue
  exit /b 1
)
if not exist "infra\build\libs\ayan-dj-tools.jar" (
  echo [build] ERREUR : JAR introuvable
  exit /b 1
)
echo [build] JAR OK

echo [build] === Etape 2/3 : Install dependances frontend ===
cd /d "%ROOT_DIR%\music-tagger-ui"
call npm ci
if errorlevel 1 exit /b 1

echo [build] === Etape 3/3 : Build installeur Electron Windows ===
call npm run electron:dist:win
if errorlevel 1 exit /b 1

echo [build] === Build termine ===
echo [build] Installeur disponible dans : music-tagger-ui\release\
dir "%ROOT_DIR%\music-tagger-ui\release\" 2>nul
