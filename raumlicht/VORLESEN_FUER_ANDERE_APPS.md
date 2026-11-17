# Vorlesen mit der Stimme „Nova“ – Anleitung für andere Apps

Stand: Raumlicht 2.18.1 vom 17.11.2026. Ersetzt die frühere Anleitung zur Stimme „Thorsten“ (sherpa-onnx, Lautschrift, 63-MB-Download) – das alles ist ausgebaut.

## Kurz

- Die App rechnet keine Sprache und spricht mit keinem fremden Dienst. Sie spielt **fertige MP3-Dateien** von unserem Server `stimmen.mathenachhilfe-online.com`.
- „Nova“ ist eine **KI-generierte Stimme (OpenAI)**. Das muss in der App stehen („Nova – KI-generierte Stimme“). Das Tempo legt der Server fest.
- Vorlesen bleibt **aus, bis man es einschaltet**. Stimmenwahl: Nova oder aus.
- **Sprechtempo** (seit 2.17.1): Regler mit den Stufen 0,75 · 0,9 · 1,0 · 1,15 · 1,3 · 1,5, gemerkt. 1,0 = so, wie der Server aufnimmt. Die App spielt dieselbe Datei
  schneller oder langsamer bei gleicher Tonhöhe: Android `MediaPlayer.playbackParams` (Speed, Pitch 1), Web `<audio>` mit `preservesPitch` (bei der ersten Berührung aufschließen;
  klappt das nicht, normales Tempo über Web Audio). Die Geräte-Stimme als Notlösung bekommt dieselbe Einstellung.

## 1. Feste Texte: Liste beim Bauen erzeugen

- Die WebApp liefert `vorlesen/texte.json` aus: `{"format":1,"app":"<app>","texte":[{"text":"…","sprache":"de"},…]}`, je Text höchstens 1 500 Zeichen.
- Jeder Text steht dort **genau so, wie er gesprochen wird** (Formeln und Vektoren ausgeschrieben, kein Markup). In Raumlicht: `Vorlesen.texts()` = `Speech.clean` + Leerraum glätten.
- Erzeugt wird die Datei bei jedem Bau (`:core:model:vorleseTexte`, eingehängt in `:web:webapp`). Der Server holt sie alle 30 Minuten und vertont Neues.
- Ein Test prüft Format, Länge, Eindeutigkeit, NFC und dass keine Formelzeichen übrig sind (`VorlesenTest`).

## 2. Dateiname

`h` = die ersten 32 Hex-Zeichen von SHA-256(`"nova|" + sprache + "|" + text`), Adresse `…/nova/<h[0:2]>/<h>.mp3`.
Text vorher: NFC, jeder Leerraum zu einem Leerzeichen, getrimmt. Prüfwert: `de`, `Hallo Welt` → `cb1e6d67882767042ebaa54d163e450c`.
**Die App muss denselben Text hashen, der in der Liste steht** – in Raumlicht geht beides durch `Vorlesen.spoken()`.

## 3. Ablauf beim Vorlesen (Android: `NovaVoice.kt`, Web: `rl-nova.js`)

1. Datei schon gespeichert → sofort abspielen (geht offline).
2. Sonst `GET …/nova/<h[0:2]>/<h>.mp3`.
3. Antwort 404 und Text ≤ 300 Zeichen (entsteht erst in der Aufgabe): `POST …/jetzt` mit `{"text":"…","sprache":"de"}` → 200 `{"url":…}` → Datei holen. **Nichts über den Nutzer mitsenden.**
4. Antwort 204, Fehler oder offline → diesen einen Satz liest still die Stimme des Geräts.

Dazu: ein neuer Satz unterbricht den alten; Musik wird unter der Stimme leiser; nur Antworten annehmen, die auf den eigenen Server zeigen und wie MP3 aussehen;
gespeicherte Sätze begrenzen (Android: 60 MB im Cache, älteste zuerst weg; Web: eigener Cache `raumlicht-nova`, den der Service Worker bei Updates stehen lässt).

## 3a. Alle festen Texte offline laden (seit 2.18.0)

- Einstellungen unter „Lux spricht“: „Offline laden – Standard (ca. X MB)“, „Offline laden – Bestqualität (ca. Z MB)“ und, sobald etwas da ist, „Offline-Stimme löschen (Y MB)“ (löscht beide).
- Größe aus `…/nova/stand-<host>.json`: `bytes` (Standard), `bytes_original` (Bestqualität), abzüglich dessen, was schon geladen ist. Android fragt den Live-Host, sonst den Test-Host.
- Bestqualität: `…/nova/original/<h[0:2]>/<h>.mp3`; bei 404 die normale Datei. 404 der normalen Datei still überspringen.
- Nur Fehlendes, 4 gleichzeitig, Fortschritt „x von n“, Abbrechen; nächstes Tippen macht weiter. Laufzeit-Texte gehören nicht dazu.
- Speicher: Android `filesDir/nova-offline/{standard,best}` (wird nicht automatisch aufgeräumt), Web Cache `raumlicht-nova-offline` (Service Worker lässt ihn stehen).
- Die Wiedergabe schaut zuerst dort nach (Bestqualität vor Standard), dann im normalen Speicher, dann im Netz.
- Dateien: `app/…/ui/fx/NovaOffline.kt`, `rl-nova.js` (Abschnitt „offline“), gemeinsame Knöpfe `OfflineVoiceCard` in `shared/…/settings/ProScreen.kt`; Tests `NovaOfflineTest`, `web-nova.mjs`.

## 4. Stolpersteine

- **Browser:** Der Server muss CORS für die WebApp-Adressen erlauben (GET und POST). Ton gibt es erst nach einer Berührung – deshalb Web Audio statt `<audio>`.
- **Zwei Texte, ein Hash:** Schon ein doppeltes Leerzeichen ergibt einen anderen Dateinamen. Immer dieselbe Funktion für Liste und App benutzen.
- **Trailing-Lambda in Kotlin:** `NovaVoice(ctx, fallback) { … }` bindet an den letzten Parameter – `onSpeaking = { … }` ausschreiben.
- **Datenschutz:** Laufzeit-Texte können Eingaben enthalten und gehen über unseren Server an OpenAI. Das gehört in App-Text, Store-Formular und Datenschutzerklärung.
- **Aufräumen:** Beim ersten Start die alte Stimme löschen (Android `filesDir/stimme`, Web Cache `raumlicht-stimme`).

## 5. Dateien zum Abschauen

`core/model/…/Vorlesen.kt`, `Sha256.kt`, `Speech.kt` · `core/model/src/jvmMain/…/VorleseTexte.kt` · `app/…/ui/fx/NovaVoice.kt`, `Voice.kt` (Geräte-Stimme) ·
`web/src/wasmJsMain/resources/rl-nova.js`, `web/…/Main.kt` · Tests: `VorlesenTest`, `NovaVoiceTest`, `scene/test/web-nova.mjs`.
