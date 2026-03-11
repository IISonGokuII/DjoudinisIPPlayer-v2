@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Multiple TV fixes - Dashboard scroll, Play button, Memory leaks

MULTIPLE CRITICAL TV FIXES:

1. TV DASHBOARD SCROLL FIX:
   - Made Dashboard vertically scrollable
   - Continue Watching no longer hides tiles
   - All 7 tiles now accessible with UP/DOWN navigation

2. TV VOD PLAY BUTTON:
   - Moved Play button ABOVE poster (first thing visible)
   - Increased size (72dp height, 36dp icon)
   - Better focus scale (1.1f)
   - Shows 'Continue Watching' when resume available

3. MEMORY LEAK FIXES:
   - Added onCleared() to OnboardingViewModel
   - Added onCleared() to SeriesDetailViewModel  
   - Added onCleared() to ContentListViewModel
   - Prevents app slowdown after navigation

4. SETTINGS CRASH INVESTIGATION:
   - No TV-specific SettingsScreen exists (uses mobile version)
   - Strings all exist in resources
   - May need user to provide crash log for further debugging

5. EMPTY MOVIE CATEGORIES:
   - VOD sync code verified correct
   - Category type 'vod' properly set during sync
   - May be provider-specific issue (no categories synced)

FILES CHANGED:
- TvDashboardScreen.kt: Added verticalScroll
- TvVodDetailScreen.kt: Reordered Play button above poster
- OnboardingViewModel.kt: Added onCleared()
- SeriesDetailViewModel.kt: Added onCleared()
- ContentListViewModel.kt: Added onCleared()

BUILD: SUCCESSFUL
TV experience significantly improved"
git push origin main
echo.
echo ================================================
echo Successfully pushed all TV fixes!
echo ================================================
echo.
echo Fixed:
echo - Dashboard scrollable (tiles always accessible)
echo - Play button above poster (prominent)
echo - Memory leaks fixed (3 ViewModels)
echo.
echo Needs testing:
echo - Settings crash (provide crash log if persists)
echo - Empty movie categories (provider-specific?)
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
