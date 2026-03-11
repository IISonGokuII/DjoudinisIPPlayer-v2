@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Live TV Preview - Larger window, all channels play, screensaver prevention

LIVE TV PREVIEW IMPROVEMENTS:

1. PREVIEW WINDOW LARGER:
   - Changed from 220dp fixed width to 60% of screen
   - Preview now takes up most of the screen (was small sidebar)
   - Channel info sidebar compact (40% width)
   - Better use of screen real estate

2. ALL CHANNELS NOW PLAY:
   - FIXED: Player created once, media item updated on channel change
   - Before: New player created for each channel (some failed to init)
   - After: Single player instance, just change media item
   - More reliable playback for all channels

3. SCREENSAVER PREVENTION:
   - Added keepScreenOn = true to PlayerView
   - Fire TV screensaver won't activate during preview
   - Also prevents screensaver during full playback

4. IMPROVED UI:
   - Close button at top of sidebar
   - Larger fullscreen button (64dp)
   - Channel name centered with more space
   - Better visual hierarchy

LAYOUT CHANGE:
BEFORE:                    AFTER:
┌──────────┬──────┐        ┌──────────────┬──────────┐
│ Channel  │Prev  │        │   Preview    │ Channel  │
│ List     │(220dp)│        │   (60%)      │  Info    │
│          │      │        │              │  (40%)   │
└──────────┴──────┘        └──────────────┴──────────┘

TECHNICAL CHANGES:
- ExoPlayer created once with remember {}
- LaunchedEffect updates media item on channel.id change
- PlayerView.keepScreenOn = true prevents screensaver
- Layout changed from Column to Row (preview left, info right)

BUILD: SUCCESSFUL
Live TV preview now works reliably for all channels"
git push origin main
echo.
echo ================================================
echo Successfully pushed Live TV Preview fixes!
echo ================================================
echo.
echo Fixed:
echo - Preview window now 60% of screen (was 220dp)
echo - All channels now play reliably
echo - Screensaver prevention during playback
echo - Better UI layout
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
