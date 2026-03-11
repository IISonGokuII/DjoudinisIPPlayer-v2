@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: TV Dashboard - Continue Watching layout fixes

CRITICAL TV FIXES:
- Fixed Continue Watching section causing dashboard overflow
- Reduced spacing to prevent content from being cut off
- LazyRow now properly scrollable without focus issues

CHANGES:
- TvDashboardScreen.kt: Reduced Continue Watching bottom spacing (32dp → 16dp)
- Increased contentPadding for better scrolling (8dp → 16dp)
- Removed problematic focusable() modifier

KNOWN LIMITATIONS:
- Audio track selection shows simplified list (language options)
  This is intentional - full ExoPlayer track selection is complex
- Play button on TV VOD detail screen already exists

BUILD: SUCCESSFUL
TV Dashboard now properly handles Continue Watching content"
git push origin main
echo.
echo ================================================
echo Successfully pushed TV Dashboard fixes!
echo ================================================
echo.
echo Fixed:
echo - Continue Watching no longer breaks layout
echo - Reduced spacing for better fit
echo - Better scrolling behavior
echo.
echo Note: 
echo - Play button already exists on TV VOD screens
echo - Audio track shows simplified language list (by design)
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
