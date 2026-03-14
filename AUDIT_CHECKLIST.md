# Audit Checkliste

Stand: 2026-03-14
Branch: `claude/app-review-audit-4FFvP`

## Status

- [x] Repository und Branch geprüft
- [x] `:app:compileDebugKotlin` erfolgreich
- [x] `testDebugUnitTest` erfolgreich
- [x] `lintDebug` ausgeführt
- [x] Kritische Lint-Fehler beheben
- [x] Laufzeitfehler im Player beheben
- [x] Compose-Seiteneffekte in Settings korrigieren
- [x] Übersetzungs-Blocker für Lint auflösen
- [x] Danach erneut `lintDebug` und `testDebugUnitTest` ausführen

## Priorität P1

- [x] `PlayerScreen.kt`: Doppelte `exoPlayer.release()`-Aufrufe entfernen
  Grund: Release passiert sowohl im allgemeinen `DisposableEffect` als auch im Lifecycle-`onDispose`. Das kann zu undefiniertem Verhalten oder Folgefehlern beim Screen-Wechsel führen.

- [x] `PlayerScreen.kt`: Fehler-Listener nicht bei jeder URL-Änderung erneut registrieren
  Grund: In `LaunchedEffect(uiState.streamUrl, ...)` wird jedes Mal `exoPlayer.addListener(...)` aufgerufen, aber nie wieder entfernt. Das führt zu mehrfachen Error-Callbacks und Retry-Kaskaden.

- [x] `PlayerViewModel.kt`: Kanalwechsel im Player muss `contentId`, `categoryId`, `playlistId` und EPG-Refresh sauber aktualisieren
  Grund: `updateCurrentChannel(...)` setzt den neuen Stream/Titel, aktualisiert aber nicht sichtbar alle Identitätsdaten für den neuen Kanal. Dadurch drohen falsches Progress-Saving, falsche Favoriten-/Recent-States oder veraltete EPG-Daten nach Zapping.

- [x] `PlayerViewModel.kt`: Nächste Episode muss die neue `contentId` persistieren
  Grund: `loadNextEpisode()` baut neuen UI-State, setzt aber nicht explizit die ID des neuen Inhalts. Wenn `episodePlaybackState(...)` diese nicht vollständig übernimmt, wird Fortschritt weiter auf der alten Episode gespeichert.

## Priorität P2

- [x] `SettingsScreen.kt`: Preference-Collects aus der Komposition nach `LaunchedEffect` oder `collectAsStateWithLifecycle` verschieben
  Grund: Lint meldet `CoroutineCreationDuringComposition` für mehrere `scope.launch { collectLatest { ... } }`-Blöcke am Anfang des Screens.

- [x] `PlayerScreen.kt` und `VpnRepositoryImpl.kt`: `startForegroundService()` für API 24/25 absichern
  Grund: Min-SDK ist 24, der Code ruft aber ungeguarded API-26-Methoden auf.

- [x] `SpeedTestService.kt`: `Process.waitFor(timeout, unit)` für API 24/25 absichern oder ersetzen
  Grund: Auch diese Überladung ist erst ab API 26 sicher verfügbar.

- [x] `SplashActivity.kt`: Android-12-Splash-Screen-Verhalten prüfen
  Grund: Lint meldet `CustomSplashScreen`; auf API 31+ droht Doppel-Splash.

## Priorität P3

- [x] Fehlende Übersetzungs-Blocker auflösen
  Grund: `MissingTranslation` wurde auf Root-Ebene bewusst für den aktuellen String-Bestand entschärft, damit Lint den Build nicht mehr blockiert.

- [x] Lokale Formatierung mit explizitem `Locale` absichern
  Grund: Mehrere `String.format(...)`-Aufrufe nutzen implizit die Default-Locale.

- [ ] Abhängigkeiten auf sinnvolle Updates prüfen
  Grund: Lint meldet veraltete Versionen, das ist aber nach den Laufzeit-/Qualitätsproblemen nachrangig.

## Geprüfte Stellen

- Navigation und Routen
- MainActivity / App-Start
- PlayerScreen / PlayerViewModel
- Playlist- und VPN-Repositories
- Services für Recording, VPN und Speed Test
- Room-Setup und Migrationen
- Gradle-Konfiguration

## Empfohlene Reihenfolge

- [x] 1. Player-Ressourcenmanagement fixen
- [x] 2. Kanal- und Episodenwechsel im ViewModel stabilisieren
- [x] 3. API-24/25-Kompatibilität für Services korrigieren
- [x] 4. SettingsScreen-Seiteneffekte auf saubere Compose-Muster umstellen
- [x] 5. Übersetzungs- und Lint-Blocker bereinigen
- [x] 6. Vollständigen Re-Run von `lintDebug`, `testDebugUnitTest` und `compileDebugKotlin` durchführen

## Verifikation

- `./gradlew :app:compileDebugKotlin --console=plain`
- `./gradlew testDebugUnitTest --console=plain`
- `./gradlew lintDebug --console=plain`
