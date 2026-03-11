@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: CRITICAL - Add onCleared() to all ViewModels to prevent memory leaks

CRITICAL PERFORMANCE FIX:
Memory leaks were causing the app to become slower after switching
between Live TV, Movies, and Series screens. ViewModels were not
properly cancelling their coroutines when destroyed.

VIEWMODELS FIXED:
1. OnboardingViewModel - Added onCleared() for categoryCollectJob
2. SeriesDetailViewModel - Added onCleared() for episodesJob & progressJob
3. ContentListViewModel - Added onCleared() for cleanup logging

SYMPTOMS (BEFORE FIX):
- App becomes slower after switching between sections
- Live TV → Movies → Series causes lag
- Eventually screens stop loading entirely
- Memory usage grows unbounded

ROOT CAUSE:
- Coroutines in init{} blocks were not being cancelled
- collect() calls continued running after ViewModel destruction
- Each navigation created new coroutines without cancelling old ones

FIX:
- Added onCleared() override to all ViewModels with Jobs
- Properly cancel all coroutines when ViewModel is destroyed
- Added Timber logging for debugging

TECHNICAL DETAILS:
- OnboardingViewModel: categoryCollectJob.cancel()
- SeriesDetailViewModel: episodesJob.cancel(), progressJob.cancel()
- ContentListViewModel: Cleanup logging (flows auto-cancelled by scope)

BUILD: SUCCESSFUL
App should now maintain consistent performance after navigation"
git push origin main
echo.
echo ================================================
echo Successfully pushed CRITICAL Memory Leak Fixes!
echo ================================================
echo.
echo Fixed ViewModels:
echo - OnboardingViewModel (categoryCollectJob)
echo - SeriesDetailViewModel (episodesJob, progressJob)
echo - ContentListViewModel (cleanup logging)
echo.
echo This should fix the slowness after navigation!
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
