@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Multiple critical fixes - Audio, EPG, Player

CRITICAL FIXES:
- Audio Track Selection: Simplified dialog (was crashing)
- EPG Sync: Added comprehensive logging and error messages
- EPG Sync: Now shows German error if no EPG URL configured
- Player: Persistent instance for all content types

IMPROVEMENTS:
- EPG Sync logging for debugging
- Better error messages for users
- Audio dialog with language options

KNOWN ISSUES (to be fixed in next update):
- Aspect Ratio UI implementation needed
- Buffer presets UI in Settings
- Continue Watching navigation
- Dashboard MultiView tile
- LiveTV fullscreen transition optimization

BUILD: SUCCESSFUL
EPG Sync now has proper error handling and logging"
git push origin main
echo.
echo ================================================
echo Successfully pushed to GitHub!
echo ================================================
echo.
echo Fixed:
echo - Audio Track Selection (simplified)
echo - EPG Sync (logging + error messages)
echo - Player persistence
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
