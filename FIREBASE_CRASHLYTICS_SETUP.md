# 🔥 Firebase Crashlytics Einrichtung - Anleitung

## Übersicht

Firebase Crashlytics wurde eingerichtet für **Online-Crash-Reporting** auf Fire TV und anderen Geräten.

### Was wurde implementiert?

| Komponente | Status | Beschreibung |
|------------|--------|--------------|
| ✅ Crash Handler | Vorhanden | Lokale Crash-Reports + Firebase Integration |
| ✅ SeriesDetailViewModel Fix | Implementiert | Bessere Fehlerbehandlung bei Navigation |
| ✅ Firebase Dependencies | Hinzugefügt | In `build.gradle.kts` und `libs.versions.toml` |
| ⚠️ google-services.json | **FEHLT** | Muss von Firebase Console heruntergeladen werden |

---

## 📋 Einrichtungsschritte

### Schritt 1: Firebase Projekt erstellen

1. Gehe zu [Firebase Console](https://console.firebase.google.com/)
2. Klicke auf **"Projekt hinzufügen"**
3. Projektnamen eingeben (z.B. "DjoudinisIPPlayer")
4. Google Analytics: **Aktivieren** (empfohlen für Crash-Analyse)

### Schritt 2: Android-App registrieren

1. In Firebase Console: **"Android-App hinzufügen"** klicken
2. **Package Name** eingeben: `com.djoudini.iplayer`
3. App-Spitzname (optional): "Djoudinis IP Player"
4. **Debug Signing Certificate SHA-1** (optional für Crashlytics, benötigt für Analytics)
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Auf **"App registrieren"** klicken

### Schritt 3: google-services.json herunterladen

1. Firebase Console zeigt Download-Link für `google-services.json`
2. Datei herunterladen
3. Datei in folgenden Ordner kopieren:
   ```
   C:\Users\WhatsappBot\Desktop\DjoudinisIPPlayer-v2\app\google-services.json
   ```

### Schritt 4: Crashlytics aktivieren

1. In Firebase Console: Linke Sidebar → **Crashlytics**
2. Auf **"Erste Schritte"** klicken
3. Firebase SDK ist bereits im Code eingerichtet

### Schritt 5: Build und Test

```bash
# Debug Build für Tests
./gradlew assembleDebug

# Auf Fire TV installieren und testen
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Schritt 6: Crash Report auslösen (Test)

Um Crashlytics zu testen, füge temporär einen Test-Crash hinzu:

```kotlin
// In einer Activity oder ViewModel
FirebaseCrashlytics.getInstance().crash()
```

Nach dem Crash:
1. App neu starten
2. In Firebase Console → Crashlytics warten (~5 Minuten bis Report erscheint)

---

## 🔍 Crash-Reports anzeigen

### Firebase Console

1. Gehe zu [Firebase Console](https://console.firebase.google.com/)
2. Projekt auswählen
3. **Crashlytics** im linken Menü
4. Dashboard zeigt:
   - **Crash-freie Benutzer** (%)
   - **Fehler** (nach Häufigkeit sortiert)
   - **Neueste Probleme**

### Crash-Report Details

Jeder Report zeigt:
- 📱 Gerät (Fire TV Stick 4K, etc.)
- 🤖 Android Version
- 📦 App Version
- 🧵 Stack Trace
- 📊 Betroffene Benutzer

---

## 🛠️ Debug-Features

### Lokale Crash-Reports

Der bestehende `CrashHandler` speichert weiterhin lokale Reports:

**Pfad auf Gerät:**
```
/Downloads/DjoudinisIPPlayer_CrashReports/crash_YYYY-MM-DD_HH-mm-ss.txt
```

**Interner Speicher (ohne Berechtigung):**
```
/data/data/com.djoudini.iplayer/files/crash_reports/crash_YYYY-MM-DD_HH-mm-ss.txt
```

### Timber Logs (Debug Build)

Alle Logs werden im Debug-Modus angezeigt:
```bash
# ADB LogCat Filter
adb logcat -s "DjoudinisIPPlayer"
adb logcat -s "SeriesDetailViewModel"
adb logcat -s "CrashHandler"
```

---

## 🎯 Spezifischer Fix: Series Cover Crash

### Problem

Beim Klicken auf ein Serie-Cover in der TV-App stürzte die App ab wegen:
- `seriesId` wurde nicht korrekt an `SeriesDetailViewModel` übergeben
- Fallback auf `0L` führte zu ungültiger Datenbankabfrage

### Lösung

**Datei:** `SeriesDetailViewModel.kt`

**Änderungen:**
1. ✅ Bessere Fehlermeldung wenn `seriesId` fehlt
2. ✅ Loggt alle verfügbaren Keys im SavedStateHandle
3. ✅ Wirft `IllegalStateException` statt stillschweigendem Fallback
4. ✅ Validiert `seriesId > 0` sofort im `init`-Block

```kotlin
private val seriesId: Long = savedStateHandle[NavArgs.SERIES_ID] ?: run {
    val allKeys = savedStateHandle.keySet()
    Timber.e("seriesId is null! Available keys: $allKeys")
    throw IllegalStateException("seriesId argument is missing! Navigation error.")
}
```

### Test

1. Fire TV App starten
2. Zur Kategorie "Serien" navigieren
3. Auf ein Serie-Cover klicken
4. **Erwartet:** Serie-Details werden geladen OHNE Crash
5. **Bei Crash:** Report erscheint in Firebase Console nach ~5 Min

---

## 📊 Crashlytics Dashboard Beispiel

```
Crash-freie Benutzer: 98.5%

Probleme (3):
┌─────────────────────────────────────┬──────────┬────────────┐
│ Fehler                              │ Benutzer │ Ereignisse │
├─────────────────────────────────────┼──────────┼────────────┤
│ IllegalStateException (seriesId)    │ 12       │ 45         │
│ NullPointerException (CoverLoader)  │ 5        │ 8          │
│ OutOfMemoryError (ImageCache)       │ 2        │ 3          │
└─────────────────────────────────────┴──────────┴────────────┘
```

---

## 🔧 Troubleshooting

### "No google-services.json found"

**Lösung:** Datei muss im `app/` Ordner liegen:
```
app/
├── google-services.json  ← HIER
├── src/
└── build.gradle.kts
```

### Crashlytics zeigt keine Reports

1. **Wartezeit:** Reports erscheinen mit 5-30 Min Verzögerung
2. **Debug Build:** Crashlytics ist standardmäßig aktiviert
3. **Internet:** Gerät muss mit Firebase verbinden können
4. **App Version:** Muss mit registrierter Version übereinstimmen

### Build-Fehler nach Plugin-Änderung

```bash
# Gradle Sync erzwingen
./gradlew --refresh-dependencies

# Clean Build
./gradlew clean assembleDebug
```

---

## 📝 Nächste Schritte

1. ✅ `google-services.json` herunterladen und einfügen
2. ✅ App bauen und auf Fire TV testen
3. ✅ Serie-Cover Crash testen (sollte jetzt behoben sein)
4. ✅ Firebase Console prüfen auf Crash-Reports
5. ✅ Regelmäßig Crashlytics Dashboard überwachen

---

## 📚 Nützliche Links

- [Firebase Crashlytics Dokumentation](https://firebase.google.com/docs/crashlytics)
- [Crashlytics für Android TV](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Firebase Console](https://console.firebase.google.com/)

---

**Erstellt:** 2026-03-13
**Status:** ✅ Implementiert, ⚠️ google-services.json erforderlich
