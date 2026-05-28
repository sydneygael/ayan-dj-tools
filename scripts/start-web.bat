@echo off
REM Lance le frontend Angular web.
REM Usage : scripts\start-web.bat

setlocal
set "ROOT_DIR=%~dp0.."
set "WEB_DIR=%ROOT_DIR%\ayan_dj_tools_web"

if not exist "%WEB_DIR%\package.json" (
  echo [web] ERREUR : projet Angular introuvable dans "%WEB_DIR%"
  exit /b 1
)

echo [web] Installation des dependances npm...
cd /d "%WEB_DIR%"
call npm install
if errorlevel 1 exit /b 1

echo [web] Demarrage Angular dev server sur http://127.0.0.1:4200 ...
call npm run start -- --host 127.0.0.1 --port 4200

