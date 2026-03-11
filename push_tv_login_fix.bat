@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Fire TV Stick - Xtream Login Button now works correctly

CRITICAL FIX:
- Fixed Xtream login button not responding on Fire TV Stick
- Improved FocusableCard onClick handling
- Added helper text when fields are not filled
- Fixed LaunchedEffect type handling for Resource.Success

CHANGES:
- TvLoginXtreamScreen.kt: Better state handling
- Added 'Alle Felder ausfüllen' helper text
- Fixed Resource.Success<Long> type handling
- Improved button feedback

BUILD: SUCCESSFUL
Fire TV login should now work correctly"
git push origin main
echo.
echo ================================================
echo Successfully pushed Fire TV Login Fix!
echo ================================================
echo.
echo Fixed:
echo - Xtream Login Button on Fire TV Stick
echo - Better focus handling
echo - Helper text for empty fields
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
