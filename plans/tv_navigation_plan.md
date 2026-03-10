# Aktionsplan zur Verbesserung der TV/Fire-TV-Navigation (DjoudinisIPPlayer-v2)

Dieses Dokument enthält den detaillierten Plan, um die D-Pad-Navigation und das allgemeine TV-Erlebnis für die App zu optimieren. Das Projekt wird in Phasen unterteilt. Nach jeder Phase wird ein Build-Test (z.B. `./gradlew assembleDebug`) durchgeführt, um sicherzustellen, dass keine Fehler eingebaut wurden.

## Phase 1: TV-Spezifischer Einrichtungsassistent (Onboarding)
**Ziel:** Die initiale Anmeldung (Xtream / M3U) für D-Pad-Nutzer verbessern.
*   **Schritt 1:** Eigene TV-Screens für das Onboarding im Package `presentation/ui/tv/` erstellen (`TvOnboardingScreen`, `TvLoginXtreamScreen`, `TvLoginM3uScreen`).
*   **Schritt 2:** Sichtbarkeit von Fokus-Zuständen bei Textfeldern drastisch erhöhen (z.B. durch farbige Rahmen oder Hintergrundwechsel bei Fokus).
*   **Schritt 3:** `FocusableCard` für Login-Buttons anwenden, um den D-Pad Fokus deutlich sichtbar zu machen.
*   **Schritt 4:** Routing in der `MainActivity` / `NavGraph` anpassen: Wenn `isTvDevice == true`, leite den Nutzer in das TV-Onboarding um, ansonsten in das mobile Onboarding.
*   **Check:** App kompilieren (`./gradlew assembleDebug`) und verifizieren.

## Phase 2: Navigation & Focus-Management auf dem Dashboard
**Ziel:** Versehentliches Springen in die Suche verhindern und Kacheln sichtbarer machen.
*   **Schritt 1:** Das Focus-Verhalten im `TvDashboardScreen` und den Kategorien analysieren. Die Suchleiste ("Search") im Header soll nur erreicht werden können, wenn man explizit nach OBEN navigiert, nicht beim Navigieren nach RECHTS in einer Kachel-Liste.
*   **Schritt 2:** Die `FocusableCard` überarbeiten, damit der Border-Glow (der farbige Rahmen um fokussierte Elemente) dicker und leuchtender wird, was die Navigation auf Distanz verbessert.
*   **Check:** App kompilieren (`./gradlew assembleDebug`) und verifizieren.

## Phase 3: Zappen & Live-TV Navigation
**Ziel:** Komfortabler Kanalwechsel ohne Rückkehr zur Senderliste.
*   **Schritt 1:** Im `TvPlayerOverlay.kt` die Key-Events für `DPAD_UP` und `DPAD_DOWN` so anpassen, dass sie nicht mehr das Overlay anzeigen, sondern stattdessen einen "Zap"-Befehl auslösen (vorheriger Kanal / nächster Kanal).
*   **Schritt 2:** Logik im `PlayerViewModel` implementieren, um innerhalb der aktuellen Senderliste den vorherigen (`playPreviousChannel()`) bzw. nächsten Kanal (`playNextChannel()`) zu ermitteln und den Stream nahtlos zu wechseln.
*   **Check:** App kompilieren (`./gradlew assembleDebug`) und verifizieren.

## Phase 4: VOD (Filme) & Serien (Binge-Watching)
**Ziel:** Bessere Filmbeschreibungen (Plot) anzeigen und einfaches Weiterklicken zur nächsten Episode ermöglichen.
*   **Schritt 1:** Erstellen eines `TvVodDetailScreen.kt`, das für Fernseher optimiert ist. Hierbei wird sichergestellt, dass Filmcover, Titel, Genre und vor allem der *Plot (die Handlung)* prominent und gut lesbar auf einer eigenen Seite vor dem Start des Films angezeigt werden.
*   **Schritt 2:** Im TV-Player die Funktion "Nächste Episode" für Serien ergänzen. Wenn ein Video vom Typ `Episode` abgespielt wird, soll eine Taste (z.B. `DPAD_RIGHT` lang drücken oder ein extra Button im Overlay) direkt das nächste Video der Staffel laden.
*   **Schritt 3:** Routing anpassen, sodass TV-Geräte für Filme und Serien in die dedizierten TV-Details-Screens geleitet werden statt in die mobilen Varianten.
*   **Check:** App kompilieren (`./gradlew assembleDebug`) und verifizieren.

## Zusammenfassung
Diese schrittweise Herangehensweise garantiert, dass wir das TV-Erlebnis Modul für Modul verbessern, ohne das bestehende funktionierende System zu beschädigen.