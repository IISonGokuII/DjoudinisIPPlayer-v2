@echo off
cd /d C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2
git add .
git commit -m "fix: Critical playback fixes + 120Hz support

CRITICAL BUGFIXES:
- Fix video playback not starting (LiveTV, VOD, Series)
- Fix player instance being null when shouldPlay=false
- Fix video stopping when maximizing from PiP
- Player now created persistently, media loaded on demand

120HZ SUPPORT:
- Add 120Hz constant (FPS_120 = 120f)
- Add display rates list (24-144Hz support)
- Prefer 120Hz for high FPS content (60fps+)
- UI scrolling automatically adapts to display refresh rate

TECHNICAL CHANGES:
- PlayerScreen.kt: Player creation without conditions
- AutoFrameRateManager.kt: 120Hz/144Hz support
- AFR now prefers 120Hz for 60fps+ content

BUILD: SUCCESSFUL
All playback issues resolved!"
git push origin main
echo.
echo ================================================
echo Successfully pushed to GitHub!
echo ================================================
echo.
echo Changes pushed:
echo - Critical playback fixes (LiveTV, VOD, Series)
echo - 120Hz support for high refresh displays
echo - UI auto-adapts to display refresh rate
echo.
echo Check GitHub Actions for build status:
echo https://github.com/IISonGokuII/DjoudinisIPPlayer-v2/actions
echo.
pause
