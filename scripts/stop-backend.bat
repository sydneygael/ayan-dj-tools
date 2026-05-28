@echo off
REM Arrete le backend Spring Boot lance via les scripts du projet.
REM Usage : scripts\stop-backend.bat

setlocal enabledelayedexpansion
set "FOUND=0"

echo [backend] Tentative d'arret via la fenetre "Ayan Backend"...
taskkill /FI "WINDOWTITLE eq Ayan Backend*" /T /F >nul 2>&1
if not errorlevel 1 (
  set "FOUND=1"
  echo [backend] Fenetre backend fermee.
)

echo [backend] Recherche des processus bootRun/Java backend...
for /f %%P in ('powershell -NoProfile -Command ^
  "$patterns = @('infra:bootRun','com.djtools.ayan.musictagger.MusicTaggerApplication','ayan-dj-tools.jar');" ^
  "Get-CimInstance Win32_Process | Where-Object { $cmd = $_.CommandLine; $cmd -and ($patterns | Where-Object { $cmd -like ('*' + $_ + '*') }) } | Select-Object -ExpandProperty ProcessId"') do (
  echo [backend] Arret PID %%P
  taskkill /PID %%P /T /F >nul 2>&1
  set "FOUND=1"
)

if "%FOUND%"=="0" (
  echo [backend] Aucun processus backend detecte.
) else (
  echo [backend] Backend arrete.
)

endlocal

