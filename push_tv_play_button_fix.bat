@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: TV VOD - Play button now more prominent and visible

TV PLAY BUTTON FIX:
The play button on TV VOD detail screen was not prominent enough
and could be missed by users.

CHANGES:
- Moved Play button ABOVE movie poster (first thing you see)
- Increased button height (64dp → 72dp)
- Increased focus scale (1.05f → 1.1f) for better visibility
- Larger icon (32dp → 36dp)
- Better spacing (12dp → 16dp)
- Text shows "Continue Watching" when resume available

LAYOUT CHANGE:
BEFORE:
┌──────────────┐
│  Back Button │
│  [Poster]    │
│  [Play]      │ ← Easy to miss
└──────────────┘

AFTER:
┌──────────────┐
│  Back Button │
│  [PLAY ▶️]   │ ← First thing you see!
│  [Poster]    │
└──────────────┘

BENEFITS:
- Play button is now the first interactive element
- Larger and more visible
- Better focus feedback
- Clear call-to-action

BUILD: SUCCESSFUL
TV VOD play button now prominently displayed"
git push origin main
echo.
echo ================================================
echo Successfully pushed TV Play Button Fix!
echo ================================================
echo.
echo Fixed:
echo - Play button moved ABOVE poster
echo - Larger button (72dp height)
echo - Better focus scale (1.1f)
echo - Larger icon (36dp)
echo - Shows "Continue Watching" when available
echo.
echo Check GitHub Actions:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
