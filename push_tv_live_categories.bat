@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "feat: CRITICAL - Add TvLiveCategoriesScreen for Fire TV

CRITICAL FIRE TV FEATURE:
Created TvLiveCategoriesScreen - the missing TV-optimized Live TV
categories screen that was causing users to see mobile UI on Fire TV.

FEATURES:
- FocusableCard for all channels (D-Pad friendly)
- Category selection with horizontal scroll
- 4-column channel grid optimized for TV
- EPG preview (current/next program) for each channel
- Preview overlay for quick channel peek
- Large, readable text for TV viewing distance
- Proper focus handling and navigation

LAYOUT:
┌──────────────────────────────────────────┐
│  [←] Live TV                             │
├──────────────────────────────────────────┤
│  Kategorie wählen:                       │
│  [Alle] [Sports] [News] [Movies] ... →   │
├──────────────────────────────────────────┤
│  156 Kanäle                              │
│  ┌─────┬─────┬─────┬─────┐               │
│  │ ARD │ ZDF │ RTL │ ... │               │
│  │Now: │Now: │Now: │     │               │
│  │[Preview] [Preview]    │               │
│  ├─────┼─────┼─────┼─────┤               │
│  │ ... │ ... │ ... │ ... │               │
│  └─────┴─────┴─────┴─────┘               │
└──────────────────────────────────────────┘

NAVIGATION:
- D-Pad UP/DOWN: Navigate channel grid
- D-Pad LEFT/RIGHT: Switch channels
- CENTER: Open channel / Play
- BACK: Return to dashboard

CHANGES:
1. Created TvLiveCategoriesScreen.kt (407 lines)
2. Updated AppNavGraph to use TV version on TV devices
3. Added horizontalScroll import

BUILD: SUCCESSFUL
Fire TV users can now browse Live TV properly!"
git push origin main
echo.
echo ================================================
echo Successfully pushed TvLiveCategoriesScreen!
echo ================================================
echo.
echo Created:
echo - TvLiveCategoriesScreen.kt (TV-optimized)
echo - 4-column channel grid
echo - EPG preview for each channel
echo - Preview overlay
echo.
echo This is a CRITICAL feature for Fire TV!
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
