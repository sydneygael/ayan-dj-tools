@echo off
REM Appelé via : call "%ROOT_DIR%\scripts\load-env.bat"
REM
REM Lit le .env à la racine du projet et set toutes les variables KEY=VALUE
REM dans l'environnement courant (pas de setlocal — les variables sont visibles
REM par le script appelant).
REM Les lignes vides et les commentaires (#) sont ignorés.
REM Si .env est absent, le crée depuis .env.example.

set "_env_file=%~dp0..\.env"
set "_example_file=%~dp0..\.env.example"

if not exist "%_env_file%" (
    if exist "%_example_file%" (
        echo [env] .env absent - copie de .env.example en .env
        copy /y "%_example_file%" "%_env_file%" > nul
        echo [env] Completez vos cles API dans .env puis relancez.
    ) else (
        echo [env] Avertissement : .env introuvable, aucune variable chargee.
    )
    set "_env_file="
    set "_example_file="
    goto :eof
)

echo [env] Chargement de .env...
for /f "usebackq eol=# tokens=*" %%i in ("%_env_file%") do (
    REM Ignorer les lignes ne ressemblant pas à KEY=VALUE
    echo(%%i | findstr /r "^[A-Za-z_][A-Za-z0-9_]*=" > nul 2>&1
    if not errorlevel 1 set "%%i"
)
echo [env] Variables chargees.

set "_env_file="
set "_example_file="
