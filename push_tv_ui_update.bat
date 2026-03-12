@echo off
REM Push-Skript für DjoudinisIPPlayer-v2
REM Commit und Push aller Änderungen zu GitHub

echo ========================================
echo DjoudinisIPPlayer-v2 - Push to GitHub
echo ========================================
echo.

REM 1. Alle Änderungen stagen
echo [1/4] Staging all changes...
git add -A

REM 2. Commit erstellen
echo [2/4] Creating commit...
git commit -m "TV UI Update: Settings als Icon, Outlook-Sidebar, Unit Tests, Critical Fixes"

REM 3. Status prüfen
echo [3/4] Checking git status...
git status

REM 4. Push zu GitHub
echo [4/4] Pushing to GitHub...
git push origin main

echo.
echo ========================================
echo Done! Check GitHub for build status:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo ========================================
echo.
pause
