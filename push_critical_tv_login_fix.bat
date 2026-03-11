@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: CRITICAL - Fire TV Xtream Login now uses loginSuccess SharedFlow (same as mobile)

CRITICAL FIX FOR FIRE TV LOGIN:
- Changed from loginState StateFlow to loginSuccess SharedFlow
- Now identical to mobile LoginXtreamScreen implementation
- This fixes the issue where login would abort immediately

THE PROBLEM:
- TV was using loginState (StateFlow) to detect success
- Mobile uses loginSuccess (SharedFlow) to detect success
- loginState doesn't properly emit Success for navigation
- loginSuccess is specifically designed for navigation events

THE SOLUTION:
- TV now uses viewModel.loginSuccess.collect { playlistId -> ... }
- Identical to mobile implementation
- Ensures proper navigation to Category Filter after login

FILES CHANGED:
- TvLoginXtreamScreen.kt: Use loginSuccess SharedFlow

BUILD: SUCCESSFUL
Fire TV login should now work exactly like mobile!"
git push origin main
echo.
echo ================================================
echo CRITICAL Fire TV Login Fix pushed!
echo ================================================
echo.
echo Fixed:
echo - TV now uses loginSuccess SharedFlow (like mobile)
echo - Login flow identical to mobile
echo - Should now navigate to Category Filter after login
echo.
echo This is the fix that should make it work!
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
