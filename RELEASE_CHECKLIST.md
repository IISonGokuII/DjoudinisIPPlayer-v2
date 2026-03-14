# Release Checkliste

Stand: 2026-03-14
Branch: `claude/app-review-audit-4FFvP`

## Kritisch

- [ ] `assembleDebug` erfolgreich bauen
- [ ] `assembleRelease` erfolgreich bauen
- [ ] App-Start ohne Crash auf Mobile prüfen
- [ ] App-Start ohne Crash auf Android TV / Fire TV prüfen
- [ ] Xtream Login mit echten Testdaten prüfen
- [ ] M3U Import mit echter Test-Playlist prüfen
- [ ] Live TV Wiedergabe prüfen
- [ ] VOD Wiedergabe prüfen
- [ ] Serien Wiedergabe prüfen
- [ ] Kanalwechsel im Live Player prüfen
- [ ] Resume Playback für VOD und Episoden prüfen
- [ ] EPG Sync und EPG Anzeige prüfen
- [ ] Suche prüfen
- [ ] Favoriten prüfen
- [ ] VPN Import, Verbinden und Trennen prüfen
- [ ] Aufnahme starten und stoppen prüfen
- [ ] APK manuell auf Zielgerät installieren und smoke-testen

## Wichtig

- [ ] Multi View auf Zielgeräten prüfen
- [ ] Audio-Spur-Wechsel prüfen
- [ ] Untertitel-Wechsel prüfen
- [ ] Aspect Ratio prüfen
- [ ] Audio Delay prüfen
- [ ] Sleep Timer prüfen
- [ ] Gesture Controls auf Mobile prüfen
- [ ] D-Pad Navigation auf TV vollständig prüfen
- [ ] Kanalnummern-Eingabe auf TV prüfen
- [ ] App-Hintergrund / Rückkehr in Player prüfen
- [ ] VPN Auto-Connect prüfen
- [ ] VPN Ping / Speed Test prüfen
- [ ] Aufgenommene Datei in Downloads prüfen
- [ ] Deutsche, französische und türkische Texte stichprobenartig prüfen

## Optional

- [ ] Vollständige Übersetzungen nachziehen
- [ ] Dependency-Updates planen
- [ ] Mehr UI- und Instrumentation-Tests ergänzen
- [ ] Recording-Historie im UI ergänzen
- [ ] Release Notes schreiben
- [ ] Screenshots für GitHub / Store / README aktualisieren

## Bereits verifiziert

- [x] `:app:compileDebugKotlin`
- [x] `testDebugUnitTest`
- [x] `lintDebug`
