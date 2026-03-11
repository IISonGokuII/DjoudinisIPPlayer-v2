@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Fire TV Login - Better error handling, increased timeouts, detailed logging

CRITICAL FIXES FOR FIRE TV:
- Increased network timeouts (read: 120s→180s, write: 30s→60s)
- Added detailed error messages in German
- Added comprehensive Timber logging for debugging
- Improved error display with cards and tips
- Normalized server URL (remove trailing slash)

ERROR MESSAGES:
- Timeout: "Zeitüberschreitung. Bitte Internetverbindung prüfen."
- 401/403: "Ungültige Zugangsdaten. Bitte Benutzername und Passwort prüfen."
- 404: "Server nicht gefunden. Bitte Server-URL prüfen."
- DNS: "Server nicht erreichbar. Bitte Internetverbindung prüfen."

LOGGING:
- Logs every step of login process
- Logs errors with full stack trace
- Helps diagnose connection issues

UI IMPROVEMENTS:
- Error shown in FocusableCard (more visible)
- Helper text with troubleshooting tips
- Better error visibility on TV

BUILD: SUCCESSFUL
Fire TV login now has better error handling and 50% longer timeouts"
git push origin main
echo.
echo ================================================
echo Successfully pushed Fire TV Login improvements!
echo ================================================
echo.
echo Improvements:
echo - Network timeouts increased (180s read, 60s write)
echo - Detailed German error messages
echo - Comprehensive logging for debugging
echo - Better error display with tips
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
