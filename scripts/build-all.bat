@echo off
REM Build complet : JAR Spring Boot + application Flutter Desktop Windows.
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

echo [build] === Etape 2/3 : Install dependances Flutter ===
cd /d "%ROOT_DIR%\ayan_dj_tools_flutter"
flutter pub get
if errorlevel 1 exit /b 1

echo [build] === Etape 3/3 : Build Flutter Desktop Windows ===
flutter build windows --release
if errorlevel 1 exit /b 1

echo [build] === Build termine ===
echo [build] Executable disponible dans : ayan_dj_tools_flutter\build\windows\x64\runner\Release\
dir "%ROOT_DIR%\ayan_dj_tools_flutter\build\windows\x64\runner\Release\" 2>nul
