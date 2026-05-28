@echo off
REM Lance le backend Spring Boot pour l'usage web.
REM Usage : scripts\start-backend.bat

setlocal
set "ROOT_DIR=%~dp0.."

echo [backend] Demarrage Spring Boot (infra:bootRun)...
cd /d "%ROOT_DIR%"
call gradlew.bat infra:bootRun

