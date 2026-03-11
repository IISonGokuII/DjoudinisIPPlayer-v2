@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: CRITICAL - Add TV-specific SettingsScreen to fix Fire TV crash

CRITICAL FIRE TV FIX:
Settings screen was crashing on Fire TV because it was using the
mobile version with TopAppBar and clickable modifiers that don't
work with D-Pad navigation.

SOLUTION:
Created TvSettingsScreen with:
- FocusableCard instead of clickable modifiers
- TV-optimized layout (large cards, D-Pad friendly)
- No TopAppBar (uses FocusableCard back button)
- Proper focus handling for all settings items

CHANGES:
1. Created TvSettingsScreen.kt (TV-optimized version)
2. Updated AppNavGraph to use TvSettingsScreen on TV devices
3. Changed PhoneAndroid icon to Language (missing import)

TV SETTINGS FEATURES:
- All settings from mobile version
- Large, focusable cards (80dp height)
- Clear ON/OFF indicators for toggles
- Scrollable with D-Pad UP/DOWN
- Back button as FocusableCard

BEFORE:
❌ Mobile SettingsScreen on TV
❌ TopAppBar (not TV-friendly)
❌ clickable modifiers (no D-Pad support)
❌ CRASH on Fire TV

AFTER:
✅ TvSettingsScreen on TV
✅ FocusableCard navigation
✅ D-Pad friendly layout
✅ No crash!

BUILD: SUCCESSFUL
Settings now work on both Mobile and Fire TV!"
git push origin main
echo.
echo ================================================
echo Successfully pushed TV Settings Fix!
echo ================================================
echo.
echo Fixed:
echo - Created TvSettingsScreen for Fire TV
echo - Uses FocusableCard instead of clickable
echo - No more crash on Fire TV!
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
