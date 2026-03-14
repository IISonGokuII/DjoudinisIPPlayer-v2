# Feature Checkliste

Stand: 2026-03-14
Branch: `claude/app-review-audit-4FFvP`

## 1. Fertig implementiert

- [x] Live TV Wiedergabe
- [x] VOD / Filme Wiedergabe
- [x] Serien Wiedergabe
- [x] EPG / TV Guide
- [x] Multi View
- [x] Globale Suche
- [x] Favoriten
- [x] Continue Watching / Watch Progress
- [x] Xtream Codes Login
- [x] M3U Import
- [x] Kategorien-Auswahl vor dem Sync
- [x] Playlist Sync
- [x] EPG Sync
- [x] Auto Sync
- [x] Mobile UI
- [x] TV UI
- [x] Dashboard
- [x] Onboarding
- [x] Film-Detailseiten
- [x] Serien-Detailseiten
- [x] Resume Playback
- [x] Audio-Spur-Auswahl
- [x] Untertitel-Auswahl
- [x] Aspect Ratio / Bildformat
- [x] Audio Delay
- [x] Sleep Timer
- [x] Fullscreen
- [x] TV D-Pad Navigation
- [x] Gesture Controls auf Mobile
- [x] Kanal-Zapping
- [x] Auto-Play nächste Episode
- [x] Stream-Fallback bei Fehlern
- [x] Live-TV-Aufnahme
- [x] VPN Setup Wizard
- [x] WireGuard Config Import
- [x] VPN Verbinden / Trennen
- [x] VPN Auto Connect
- [x] VPN Statusanzeige
- [x] Ping / Speed Test
- [x] Crashlytics Integration

## 2. Teilweise fertig oder funktional vorhanden, aber noch absichern

- [ ] Aufnahmen Ende-zu-Ende auf echten Geräten testen
  Status: Implementiert, aber Dateispeicherung und Foreground-Service-Verhalten sollten auf Android 8 bis 14 geprüft werden.

- [ ] VPN Boot Auto Connect auf echten Geräten testen
  Status: Implementiert, aber Boot-/Permission-Verhalten ist geräteabhängig.

- [ ] TV Remote Sonderfälle prüfen
  Status: Standard-Navigation ist vorhanden, Long-Press- und Sondertasten sollten noch sauber validiert werden.

- [ ] Multi View Stabilität unter Last prüfen
  Status: Feature ist da, aber Performance- und Speicherverhalten auf schwächeren TV-Geräten sollten geprüft werden.

- [ ] EPG Datenqualität je Provider prüfen
  Status: Sync und Anzeige sind vorhanden, aber Match-Qualität hängt stark von `tvgId` und Quelldaten ab.

- [ ] Lokalisierungen fachlich vervollständigen
  Status: Build-blockierende Übersetzungsfehler sind entschärft, aber die Sprachdateien sind noch nicht vollständig gepflegt.

## 3. Vor Release testen

- [ ] Erststart ohne Playlist
- [ ] Xtream Login mit gültigen Daten
- [ ] Xtream Login mit ungültigen Daten
- [ ] M3U Import mit kleiner Playlist
- [ ] M3U Import mit großer Playlist
- [ ] Kategorien auswählen und Sync starten
- [ ] Dashboard nach frischem Sync prüfen
- [ ] Live TV Kanal starten
- [ ] Kanalwechsel hoch/runter im Player
- [ ] Kanalnummern-Eingabe im TV-Player
- [ ] VOD starten
- [ ] Serie öffnen und Episode starten
- [ ] Resume Dialog für VOD prüfen
- [ ] Resume Dialog für Episoden prüfen
- [ ] Nächste Episode Auto-Play prüfen
- [ ] Audio-Spur wechseln
- [ ] Untertitel wechseln
- [ ] Aspect Ratio durchschalten
- [ ] Audio Delay ändern und resetten
- [ ] Sleep Timer setzen und Ablauf prüfen
- [ ] Favorit setzen und wieder entfernen
- [ ] Suche für Live, VOD und Serien prüfen
- [ ] EPG Sync manuell auslösen
- [ ] EPG Anzeige im Live Player prüfen
- [ ] Multi View starten und beenden
- [ ] Aufnahme starten und stoppen
- [ ] Datei danach in Downloads prüfen
- [ ] App-Neustart nach Aufnahme prüfen
- [ ] VPN Konfiguration importieren
- [ ] VPN verbinden
- [ ] VPN trennen
- [ ] VPN Reconnect
- [ ] Ping / Speed Test prüfen
- [ ] App auf Mobile testen
- [ ] App auf Android TV / Fire TV testen
- [ ] App-Hintergrund / Rückkehr in den Player testen
- [ ] Rotation / Fullscreen auf Mobile testen

## 4. Technisch verifiziert

- [x] `:app:compileDebugKotlin`
- [x] `testDebugUnitTest`
- [x] `lintDebug`

## 5. Sinnvolle nächste Ausbaustufen

- [ ] Vollständige Übersetzungen für `de`, `fr`, `tr`
- [ ] Release-Build mit `assembleRelease` prüfen
- [ ] Instrumentation- oder UI-Tests für Login, Player und Settings ergänzen
- [ ] Recording historisieren und im UI anzeigen
- [ ] Bessere Fehlertexte für Provider-/Netzwerkfehler
- [ ] Export / Import von App-Einstellungen
- [ ] Mehr Qualitätsmetriken für VPN und Streams
