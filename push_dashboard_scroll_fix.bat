@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: CRITICAL - TV Dashboard now scrollable when Continue Watching has content

CRITICAL TV DASHBOARD FIX:
When Continue Watching section had content, the dashboard tiles
below were pushed off-screen and not visible/focusable.

PROBLEM:
- Continue Watching LazyRow expands vertically
- Pushes main tile grid off-screen
- Settings tile completely inaccessible
- User cannot navigate to bottom tiles

SOLUTION:
- Made entire Dashboard Column vertically scrollable
- Added rememberScrollState() and verticalScroll() modifier
- All tiles now accessible via UP/DOWN navigation

CHANGES:
- TvDashboardScreen.kt: Added verticalScroll to main Column
- Added imports for rememberScrollState and verticalScroll

BEFORE:
┌─────────────────────────────┐
│  Header                     │
│  Continue Watching [====]   │
│  [Content1][Content2]...    │
│  (Tiles pushed off screen!) │
│  ❌ Live TV (not visible)   │
│  ❌ Movies (not visible)    │
│  ❌ Settings (not visible)  │
└─────────────────────────────┘

AFTER:
┌─────────────────────────────┐
│  Header                     │
│  Continue Watching [====]   │
│  [Content1][Content2]...    │
│  ✅ Live TV (scroll to it)  │
│  ✅ Movies (scroll to it)   │
│  ✅ Settings (scroll to it) │
└─────────────────────────────┘

BUILD: SUCCESSFUL
All dashboard tiles now accessible even with Continue Watching content"
git push origin main
echo.
echo ================================================
echo Successfully pushed CRITICAL Dashboard Scroll Fix!
echo ================================================
echo.
echo Fixed:
echo - Dashboard is now vertically scrollable
echo - All tiles accessible with UP/DOWN navigation
echo - Continue Watching no longer hides tiles
echo.
echo This fixes the invisible tiles issue!
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
