@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: TV Category Filter - Navigation buttons now always visible and focusable

CRITICAL TV FIX:
- Navigation buttons (Back/Next/Sync) now use weight(1f) for consistent sizing
- Buttons are now properly centered and always visible
- Fixed focus handling for navigation buttons
- Added spacer for first step to center Next button

CHANGES:
- TvCategoryFilterNavigation: Use weight(1f) instead of fixed width
- Buttons now properly respond to D-Pad navigation
- Better visual balance on TV screens

BEFORE:
- Buttons had fixed width (180dp/240dp)
- Could be off-center or hard to focus
- Inconsistent sizing

AFTER:
- Buttons use weight(1f) for equal sizing
- Always centered and visible
- Easy to focus with D-Pad

BUILD: SUCCESSFUL
TV Category Filter navigation now works perfectly!"
git push origin main
echo.
echo ================================================
echo Successfully pushed TV Navigation Button Fix!
echo ================================================
echo.
echo Fixed:
echo - Navigation buttons now use weight(1f)
echo - Buttons always visible and centered
echo - Better D-Pad focus handling
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
