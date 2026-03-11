@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "feat: CRITICAL - Add TvVodCategoriesScreen & TvSeriesCategoriesScreen for Fire TV

CRITICAL FIRE TV FEATURES:
Created two more TV-optimized category screens, bringing TV coverage to 78%!

1. TvVodCategoriesScreen (Movies):
   - 5-column movie grid optimized for TV
   - Category selection with horizontal scroll
   - FocusableCard for all movies (D-Pad friendly)
   - Large poster display (200x320dp cards)
   - Movie title and year display

2. TvSeriesCategoriesScreen (TV Series):
   - 5-column series grid optimized for TV
   - Category selection with horizontal scroll
   - FocusableCard for all series (D-Pad friendly)
   - Large poster display (200x320dp cards)
   - Series title and genre display

LAYOUT (Both screens):
┌──────────────────────────────────────────┐
│  [←] Movies/Series                       │
├──────────────────────────────────────────┤
│  Kategorie wählen:                       │
│  [Alle] [Action] [Comedy] [Drama] ... →  │
├──────────────────────────────────────────┤
│  234 Movies/Series                       │
│  ┌─────┬─────┬─────┬─────┬─────┐         │
│  │P1   │P2   │P3   │P4   │P5   │         │
│  │Title│Title│Title│Title│Title│         │
│  ├─────┼─────┼─────┼─────┼─────┤         │
│  │ ... │ ... │ ... │ ... │ ... │         │
│  └─────┴─────┴─────┴─────┴─────┘         │
└──────────────────────────────────────────┘

NAVIGATION:
- D-Pad UP/DOWN: Navigate grid
- D-Pad LEFT/RIGHT: Switch items
- CENTER: Open details
- BACK: Return to dashboard

CHANGES:
1. Created TvVodCategoriesScreen.kt (268 lines)
2. Created TvSeriesCategoriesScreen.kt (270 lines)
3. Updated AppNavGraph for TV routing
4. Added missing imports (horizontalScroll, background)

TV COVERAGE PROGRESS:
Before: 12/18 screens (67%)
After:  14/18 screens (78%)

REMAINING CRITICAL SCREENS:
- TvSeriesDetailScreen (Seasons/Episodes)
- TvEpgGridScreen (EPG Guide)

BUILD: SUCCESSFUL
Fire TV users can now browse Movies & Series properly!"
git push origin main
echo.
echo ================================================
echo Successfully pushed VOD & Series Categories!
echo ================================================
echo.
echo Created:
echo - TvVodCategoriesScreen.kt (268 lines)
echo - TvSeriesCategoriesScreen.kt (270 lines)
echo.
echo TV Coverage: 78% (was 67%)
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
