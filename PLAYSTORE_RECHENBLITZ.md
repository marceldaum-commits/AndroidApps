# Rechenblitz als Android-App (APK) und im Google Play Store

Stand 14.11.2026, App 3.5.2. Veröffentlichung laut Marcel **ab 2027**. Bis dahin wird die APK mit jeder Version mitgebaut und getestet.

## Entscheidung (Marcel 13.11.2026)

> „Die App muss, wenn APK, auch offline spielbar sein. Wer erwirbt eine Rechenapp mit Internetzwang … daher muss zukünftig die WebApp und die APK parallel gepflegt werden.“

Deshalb ist die APK **keine Hülle um die Website** (TWA). Sie liefert **alle Dateien der Web-App im Paket mit** (Capacitor) und läuft ab dem ersten Start komplett ohne Internet, ohne Service Worker und ohne Laden.

- **Eine Quelle:** `kopfrechnen/` ist für Web **und** APK. `android-app/build.sh` kopiert die Dateien beim Bauen 1:1 ins Paket.
- **Parallel pflegen:** Jede neue Web-Version bekommt eine neue APK mit derselben Versionsnummer. `versionName` = `APP_VERSION`, `versionCode` = z. B. 3.5.2 → 30502, beides automatisch aus `js/changelog.js`.
- **Unterschiede nur in `kopfrechnen/js/native.js`**, aktiv ausschließlich in der APK:
  - Server-Funktionen (Fehler melden, Gutschein, Klassen-Challenge) gehen an `https://rechenblitz.mathenachhilfe-online.com/api/…` über natives HTTP (`CapacitorHttp`, keine CORS-Änderung am Server nötig). Ohne Netz wartet alles im Postausgang wie in der Web-App.
  - Die **Erinnerung** wird lokal auf dem Gerät geplant (`@capacitor/local-notifications`): kein Server, kein Internet, kein Push-Abo. Bedienung wie in der Web-App (Schalter, Uhrzeit, Anleitung bei Sperre).
  - Kein Hinweis „Als App installieren“, kein Service Worker, keine Test-Vorschau des Adventskalenders.
  - Geteilte Links und QR-Codes zeigen auf die öffentliche Web-App.

| Punkt | Wert |
|---|---|
| Paketname | `com.mathenachhilfeonline.rechenblitz` (nach dem ersten Hochladen nicht mehr änderbar; Leitstand/Marcel bitte bestätigen) |
| Android | ab 7.0 (minSdk 23), Ziel-SDK 35 |
| Berechtigungen | Internet (nur für freiwillige Meldungen/Gutschein), Mitteilungen, Neustart-Empfang + Wecker (damit die Erinnerung nach einem Neustart bleibt) |
| Symbol / Startbild | aus `kopfrechnen/icons` erzeugt (`android-app/icons.py`) |
| Größe | ca. 2,4 MB (R8 verkleinert, Bilder als Palette) |

## APK bauen

```bash
cd android-app
npm install                 # einmalig
bash build.sh               # Test-APK: android/app/build/outputs/apk/debug/app-debug.apk
bash build.sh release       # Store-Fassung (APK + AAB), braucht den Signierschlüssel
```

Voraussetzungen: Node 18+, JDK 21, Android-SDK (Plattform 35, Build-Tools 35), `ANDROID_HOME` gesetzt. SDK ohne Android Studio einrichten:

```bash
mkdir -p ~/android-sdk && cd ~/android-sdk
curl -sSLO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && unzip -q commandlinetools-linux-*.zip
mkdir -p cmdline-tools/latest && mv cmdline-tools/{bin,lib,NOTICE.txt,source.properties} cmdline-tools/latest/
yes | cmdline-tools/latest/bin/sdkmanager --sdk_root=$HOME/android-sdk "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Meldet Maven Central „429 Too Many Requests“, eine Minute warten und neu starten. Bereits Geladenes bleibt erhalten.

**Signieren:** Der Upload-Schlüssel liegt nur bei Marcel (Bitwarden), nie im Repo (`*.jks`/`*.keystore` stehen in `.gitignore`). Für `release` die Signier-Angaben in `android/app/build.gradle` (`signingConfigs`) über Umgebungsvariablen eintragen, oder in Android Studio „Generate Signed Bundle“ nutzen. Google verwaltet den eigentlichen App-Signaturschlüssel (Play App Signing).

## Selbst-Aktualisierung der Test-APK (3.7.2)

Die APK fragt einmal täglich die neueste Release-Seite ab (`apkUpdates` in `config.js`). Gibt es eine neuere Version, zeigt die Startseite „Jetzt aktualisieren“. Der Knopf lädt die APK, Android installiert sie als Update. **Für die Play-Store-Fassung `apkUpdates: null` setzen.**

## Inhalte aktualisieren sich selbst (ab 3.13.0, Hülle 2 – Marcel 15.11.2026)

- **Ablauf in der App:** Beim Start lädt die App im Hintergrund `stand.json`, mit Version, Hüllen-Nummer und SHA-256 jeder Datei.
  - **Test-Kanal:** `https://raw.githubusercontent.com/marceldaum-commits/School/rechenblitz-test/kopfrechnen/stand.json`, ersatzweise `https://rechenblitz-test.mathenachhilfe-online.com/stand.json`.
  - **Live-/Store-Kanal:** dieselben Adressen mit `rechenblitz-live` bzw. `https://rechenblitz.mathenachhilfe-online.com/`.
- **Übernahme:**
  - Neue oder geänderte Dateien werden geladen und geprüft. Nur ein vollständig geprüfter Stand wird übernommen, und zwar in einen eigenen Ordner auf dem Gerät.
  - Aktiv wird er beim nächsten Start oder sofort über „Jetzt neu starten“.
  - Ohne Netz bleibt der letzte gute Stand. Ältere Stände als der mitgelieferte werden nie geladen.
- **Kanal festlegen:** über die Umgebungsvariable `RB_CHANNEL` beim Bauen. `test` ist der Standard (GitHub-Ablauf); für den Store gilt `RB_CHANNEL=live bash build.sh release`.
- **Hüllen-Nummer:**
  - `android-app/SHELL` erhöhen, sobald sich die App-Hülle ändert.
  - Verlangt ein Stand eine neuere Hülle, übernimmt die App nichts. Sie zeigt dann „Neue App-Version nötig“ und den Download der neuesten Release-APK.
- **Code:**
  - `RbUpdatePlugin.java` (Hülle);
  - `liveUpdateCheck()` in `kopfrechnen/js/native.js` (Anzeige);
  - `android-app/stand.js` erzeugt und prüft `stand.json`.
- **Abgerufene Adressen (für die Datenschutzerklärung):**
  - `raw.githubusercontent.com`: stand.json und geänderte Dateien;
  - `rechenblitz.mathenachhilfe-online.com` bzw. `rechenblitz-test.mathenachhilfe-online.com`: Ersatz;
  - nur wenn eine neue Hülle nötig ist: `api.github.com` (Release-Info) sowie `github.com` / `objects.githubusercontent.com` (APK-Download nach Tipp).
  - Dabei werden keine Nutzerdaten gesendet. Übertragen werden nur die üblichen Verbindungsdaten (IP-Adresse) und die Kennung „Rechenblitz-App“.

## Release-Seiten auf GitHub (Marcel 14.11.2026, Variante 1)

Bei jedem Push auf `rechenblitz-test`, der `kopfrechnen/` oder `android-app/` ändert, baut GitHub (`.github/workflows/rechenblitz-release.yml`) automatisch:
- die APK,
- die Web-App als ZIP.

Beides legt GitHub **dauerhaft** auf die Release-Seite `rechenblitz-X.Y.Z` (Titel „Rechenblitz X.Y.Z“, Text aus `CHANGELOG`). Wird dieselbe Version erneut gepusht, ersetzt der Ablauf die Dateien. Starten lässt sich der Ablauf auch von Hand („Run workflow“). Die APK dort hat keine Größengrenze. Drive („AndroidApps/Rechenblitz“) wird zusätzlich befüllt, solange die APK unter ca. 3 MB bleibt.

**Fester Test-Schlüssel:**
- Er liegt im Repo-Geheimnis `RECHENBLITZ_TEST_KEYSTORE` (Base64 eines Android-Debug-Keystores, Passwort `android`, Alias `androiddebugkey`).
- Er ist derselbe Schlüssel wie bei der ersten Drive-APK 3.5.2 (SHA-256 `C8:E0:E9:77:…:52:FF:6D`). Deshalb installiert sich jede neue Test-APK als Update, und der Lernstand bleibt.
- Fehlt das Geheimnis, bricht der Ablauf bewusst ab.
- Eine Kopie gehört in Bitwarden.
- Der Schlüssel ist nur für Testfassungen. Der Play Store bekommt Marcels eigenen Upload-Schlüssel.

## Test je Version (Pflicht, siehe ROADMAP-Regeln)

1. Web-App im Browser testen (Handy/PC, DE/EN/UK, 320 px, offline nach dem ersten Besuch).
2. APK-Modus im Browser mit nachgebildetem Capacitor testen (keine Netzanfrage, lokale Erinnerung).
3. `bash build.sh` → APK bauen, Inhalt prüfen (`aapt2 dump badging`).
4. **Auf einem echten Android-Handy:** APK installieren, Flugmodus an, App starten und spielen; Erinnerung an, 2 Minuten später einstellen und warten.

## Store-Eintrag (03.12., Roadmap Tag 20)

- Kurz- und Langbeschreibung (DE, EN, UK), Symbol 512 px, Vorschaubild 1024×500, Bildschirmfotos Handy und Tablet.
- **Datensicherheit:**
  - Freiwillig an den Server: Fehlermeldungen (Text, optional Name/E-Mail) und der Gutschein-Abruf (Klassenstufe).
  - Sonst bleibt alles auf dem Gerät. Kein Konto, keine Werbe-ID, keine Weitergabe, kein Tracking.
- Zielgruppe Kinder und Eltern/Lehrkräfte: Familien-Richtlinien von Google (keine Werbung Dritter ✅, kein Tracking ✅).
- Ablauf: interner Test → geschlossener Test → Produktion (2027). Organisationskonto mit D-U-N-S, daher keine 12-Tester-Pflicht.

## Offen

- Paketname bestätigen.
- APK auf einem echten Handy testen (Marcel). Hier gibt es keinen Emulator.
- Datenschutzerklärung:
  - Abschnitt Rechenblitz um die Erinnerung ergänzen (Web: Push-Adresse + Uhrzeit; APK: nur lokal).
  - Abschnitt um die Android-App ergänzen.
