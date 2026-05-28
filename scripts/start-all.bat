@echo off
REM Lance tous les composants pour usage navigateur :
REM 1) Services Docker, 2) Backend Spring Boot, 3) Frontend Angular.
REM Usage : scripts\start-all.bat

setlocal
set "ROOT_DIR=%~dp0.."

echo [all] Etape 1/3 - Demarrage des services externes...
call "%ROOT_DIR%\scripts\start-services.bat"
if errorlevel 1 exit /b 1

echo [all] Etape 2/3 - Demarrage backend dans une nouvelle fenetre...
start "Ayan Backend" cmd /k ""%ROOT_DIR%\scripts\start-backend.bat""

echo [all] Etape 3/3 - Demarrage frontend dans cette fenetre...
call "%ROOT_DIR%\scripts\start-web.bat"

