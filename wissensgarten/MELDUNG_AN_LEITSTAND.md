# Meldungen an den Leitstand – Wissensgarten

## Thorsten vom eigenen Stimm-Server (Version 2.0.1 vom 17.11.2026) – Web = Android

- **Quelle:** App und WebApp laden Thorsten jetzt von `https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx` (63 201 294 Bytes, sha256 `7e64762d…`). Hugging Face wird nicht mehr angefragt.
- **Problem:** sherpa-onnx (Android) braucht Metadaten im Modell; die Server-Datei hat keine. Der Fehler „'sample_rate' does not exist in the metadata“ ist mit sherpa-onnx geprüft.
- **Lösung ohne Änderung am Server:** Die bisherige sherpa-Fassung ist Byte für Byte die Server-Datei plus 127 angehängte Bytes (ONNX-Metadaten: model_type vits, sample_rate 22050, n_speakers 1 …). Beide Fassungen hängen diese 127 Bytes nach dem Laden an und prüfen dann die sha256 der fertigen Datei (`1c305abe…`). Das Ergebnis ist identisch zur bisherigen Datei. Bereits geladene Stimmen bleiben gültig.
- **Andere Apps:** Für sherpa-onnx können sie denselben Weg nutzen (Anhang siehe `ThorstenFiles.SHERPA_TAIL_B64` in `wissensgarten/app/.../platform/Thorsten.kt`) – oder ihr legt die 127 Bytes größere sherpa-Fassung zusätzlich auf den Server.
- **Beweis:** Echter Download im Headless-Chromium von eurem Server (CORS `http://localhost`): 5 s, Prüfsumme ok, Thorsten erzeugt Samples, keine Anfrage an Hugging Face. Test `ThorstenSourceTest` prüft, dass App und WebApp dieselbe Quelle, denselben Anhang und dieselbe Prüfsumme nutzen.
- **Datenschutz-Text und Lizenzen** nennen jetzt den eigenen Server. Der Hinweis „am besten im WLAN“ stand schon im Text.
- **Prüfung:** Alle Tests grün, Lint 0 Fehler.

## Musik, Vorlesen, Versionsschema (Version 2.0.0 vom 17.11.2026) – Web = Android

**Versionsschema:** neu ab **2.0.0**. Der versionCode läuft weiter (44 → 45). App, WebApp und `version.json` zeigen 2.0.0.

**Klang-Identität Wissensgarten**
- **Entstehung:** Alles ist im Code komponiert und erzeugt (`core/model/Music.kt` Komponist, `shared/ui/fx/MusicSynth.kt` und `Synth.kt` Klangerzeugung). Es gibt keine fremden Samples, keine Dateien und keine Lizenzen. Android und Web nutzen denselben Kotlin-Code, deshalb klingt beides gleich.
- **Instrumente:** Marimba (Holz-Obertöne ×3,93 und ×9,2), Glockenspiel, Kalimba, Nylon-Gitarre, Spieluhr. **Natur:** Vögel, Grillen, Wind, Wassertropfen, Brummis Summen. Gesang gibt es nicht.
- **Jede Jahreszeit eigen:**
  - Frühling: Marimba und Glockenspiel, Vögel und Tropfen
  - Sommer: Kalimba mit Gitarren-Zupfmuster, Summen
  - Herbst: Gitarre und Marimba, Wind und Regen
  - Winter: Glockenspiel und Spieluhr, leiser Wind
  - Dazu Schlaflied mit Grillen, Gieß-Runde (ohne Natur) und die Feste.
- **Nicht nervig:**
  - Form A A' B A C A: Teil C spielt ein zweites Instrument, danach folgt eine 6–8 Takte lange Atempause nur mit Natur und Teppich.
  - Ein Stück dauert 53–109 s und wiederholt sich nahtlos.
  - Messung: Anteil über 4 kHz unter 1,4 %, nichts Schrilles.
- **Effekte:** gießen, wachsen (Tonhöhe steigt mit der Stufe), richtig (Marimba und Glockenspiel), falsch (sanft und tief), welken (leiser Seufzer, einmal pro Start), Ernte (Baum ausgewachsen, Kreuzung), Sammelfigur, Erkennungsmelodie „Wis-sens-gar-ten“.
- **Bedienung und Respekt:**
  - Musik und Geräusche getrennt schaltbar, mit eigenen Lautstärken (Standard 45 % bzw. 65 %).
  - Nichts startet vor der ersten Berührung; im Browser zählt bloßes Darüberfahren nicht.
  - Android folgt Lautlos und Vibration, auch beim Umschalten während des Spielens.
  - Leiser, während vorgelesen wird.
- **Schnell:** Der Start ist unverändert. Musik entsteht erst nach der ersten Berührung, im Web in kleinen Häppchen; eine frühere 1-Sekunden-Blockade ist beseitigt (gemessen im Headless-Chromium).
- **Barrierefreiheit:** Ton ist immer nur Zugabe; alles ist auch zu sehen.

**Vorlesen**
- **Was:** Frage, Antwort und info werden vorgelesen, außerdem Brummis Sätze (Antippen). Schaltbar mit Automatisch/Immer/Nie.
- **Stimme:** Gerätestimmen, im Web nur `localService` (keine Online-Stimmen), oder **Thorsten**. Dazu Tempo 50–150 % und Probe-Taste. Die Wahl wird gemerkt.
- **Fremdsprachen:** Englisch, Französisch und Spanisch liest immer eine Gerätestimme der Sprache.
- **Thorsten auf Android:** wie bei Raumlicht, sherpa-onnx 2.0.1, ohne espeak-ng und ohne GPL. Die Lautschrift ist vorab erzeugt (Wörterbuch mit 10 606 Wörtern, 91 KB). Abdeckung 100 % der Pakettexte (6472/6472, Test). Kennt Thorsten ein Wort nicht, liest die Gerätestimme diesen Text.
- **Thorsten im Web:**
  - onnxruntime-web 1.22.0 (MIT) liegt unter `webapp/ort/`. Es wird nur bei Bedarf geladen und ist nicht im Precache.
  - Das Modell kommt per fetch von Hugging Face, wird per SHA-256 geprüft und im Browser-Speicher abgelegt.
  - Beweis im Headless-Chromium: Es entstehen Audio-Samples.
- **Modell:** 63 MB, nur auf Wunsch von huggingface.co geladen. Datenschutz-Text ist ergänzt, `LIZENZEN.md` und `lizenzen.txt` sind neu.
- **Offen:** Thorsten ist auf Android nur gebaut und getestet, nicht auf einem echten Gerät angehört. Der erste Satz braucht im Browser einige Sekunden. Lizenztexte gibt es bisher nur als Datei, ohne Menüpunkt.

**Prüfung:** Alle Tests grün, Lint 0 Fehler. Bildschirmfotos angesehen (`67_stimme`, `68_stimme_laedt`, `69_stimme_thorsten`, `70_ton`). Browser-Probelauf ohne Fehler. `wissensgarten-live` ist unverändert.

## Theorie-Knopf (Version 0.27.0 vom 17.11.2026)
- **Neu:** Bei jeder ruhigen Übung gibt es den Knopf „📖 Theorie“: in der Gieß-Runde (alle Spielarten), beim Unkraut-Jäten und beim Gedächtnis-Spaziergang. Er öffnet die Erklär-Szenen des passenden Pakets („Brummi erklärt“, mit Bildern, auf Wunsch vorgelesen). Solange die Erklärung offen ist, läuft nichts automatisch weiter.
- **Bewusst ohne Knopf:** die Zeit-Spiele Blitz, Rhythmus und Duell, damit sie fair bleiben.
- **Plus:** Plus-Pakete bekommen den Knopf ebenso, weil ihre Intros mitgeliefert werden.
- **Prüfung:**
  - Alle Tests grün, Lint ohne Fehler.
  - Bildschirmfotos angesehen: `07_giessen_grundschule.png` und neu `66_theorie.png`.
  - WebApp neu exportiert.
- **Keine Datenbank-Änderung.** Der Lernstand bleibt erhalten.
- **Stand:** APK und WebApp 0.27.0. `wissensgarten-test` wird von der CI nachgezogen. `wissensgarten-live` ist unverändert, das Umschalten liegt beim Leitstand.

## Korrektheitsprüfung 17.11. (Version 0.26.0 vom 17.11.2026)

**Was geprüft wurde und wie:**
- **Inhalte: alle 85 Pakete mit 2.923 Karten**, davon 72 freie Pakete (2.472 Karten) und 13 Plus-Pakete (451 Karten, Zweig `wg-plus-pakete`), jeweils mit Intros, Titeln, Beschreibungen und Klassenstufen.
  - **Zweitprüfung:** 8 unabhängige Prüfer, keiner hat seine eigenen Pakete geprüft, mit je 347–381 Karten.
  - **Prüfpunkte je Karte:** Frage, Antwort, Ablenker, info, Merkbild, Stufe.
  - **Zahlen** mit python3 bzw. von Hand nachgerechnet.
  - **Fakten** nach gesichertem Wissen geprüft, ohne Web-Recherche (siehe „nicht prüfbar“).
  - **Freie Eingabe:** Für jede Eingabe-Karte wurde geprüft, ob eine zweite übliche Schreibweise oder ein Synonym als falsch gewertet würde.
  - Danach `check_pack.py` (Format, Emoji ≤ 11, 1 MB) für alle 85 Pakete grün, Abholprüfung des Plus-Ordners grün.
- **Ergebnis: 255 Korrekturen in 46 Paketen**, davon 10 Plus-Pakete. Die vollständige Liste (vorher → nachher, Grund) steht in **`wissensgarten/KORREKTUREN_2026-11-17.md`**.
  - **171 × freie Eingabe eindeutig gemacht.** Richtige Eingaben wären als falsch gewertet worden, z. B. Nachname statt Vor- und Nachname, „Atlantik“/„Atlantischer Ozean“, Listen mit fester Reihenfolge, Formeln mit Hochzahlen, „Jod“/„Iod“.
    Gelöst wurde das durch eine präzisere Frage, z. B. „(Vor- und Nachname)“ oder „(ein Wort)“, eine kürzere Antwort oder Ablenker, sodass die Karte als Auswahl gespielt wird.
  - **35 × fachlich genauer bzw. richtig.** Beispiele:
    - Grundgesetz „verkündet am 23. Mai 1949“
    - Marianengraben „fast 11 000 m“
    - Catalan-Zahl C₅ eindeutig gemacht
    - „Reparatur“ und „parat“ stammen beide von lat. parare
    - Neandertaler-DNA nur außerhalb Afrikas
    - 1871–1949 sind 78 Jahre
    - Schaltjahr „fast alle 4 Jahre“
    - periphere Nerven leiten in beide Richtungen
    - Erklärung zu „halb sechs“ berichtigt
  - **3 × Ablenker war ebenfalls richtig:** „rule“ (auch Lineal), „hound“ (auch Hund), „gat“ (veraltete Form von „get“). Dazu kommt „Amsel“ bei Zugvögeln, gezählt unter „fachlich“.
  - **22 × Ablenker näher an der Antwort** (Allgemeinwissen-Quiz). Der Stufen-Nachweis lief im CI ohne die privaten Plus-Pakete nicht durch: Kl. 8–10 war bei der Ablenker-Ähnlichkeit knapp unter Kl. 5–7 (0,546 < 0,550).
    Jetzt sind die Ablenker anspruchsvoller (Paketwert 0,41 → 0,66), weiterhin eindeutig falsch und von mir gegengelesen. Der Nachweis ist mit und ohne Plus-Pakete grün.
  - **24 × Sprache/Rechtschreibung.** Darunter 15 von Namen abgeleitete Adjektive klein nach § 62, z. B. „ohmsches Gesetz“, „eulersche Phi-Funktion“; dazu Grammatik („Sonne und Blume ergeben“) und Zusammenschreibung („dreimal“).
- **Antwortprüfung der App verbessert**, weil mehrere Prüfer dieselben Grenzfälle fanden (mit Tests):
  - Groß/klein, Bindestriche, Leerzeichen, Artikel („Blauwal“ = „Der Blauwal“), Schlusspunkt und Akzente („Reykjavik“ = „Reykjavík“) sind kein Fehler mehr. Das gilt nicht im Fach Deutsch, dort zählt die Schreibung weiter streng.
  - Klammer-Antworten: „Kyjiw“ und „Kiew“ zählen für „Kyjiw (Kiew)“.
  - Zahlen werden nach Wert verglichen.
  - **Auswahl-Karten werden auf hohen Stufen nur noch eingetippt, wenn die Antwort ein Wort oder eine Zahl ist.** Vorher hätte man Antworten wie „Nach etwa 12 Jahren“ wörtlich treffen müssen.
- **Funktion:**
  - **Tests:** alle Unit- und Robolectric-Tests grün (399 Tests, darunter 60+ Bildschirmfotos aller Bildschirme der gemeinsamen Oberfläche, Interaktionstests, Migration, Sync, Updates, Plus/Lizenz/Test-Zeitraum), Lint ohne Fehler, Play-Fassung kompiliert.
  - **WebApp:** bei 320, 768 und 1280 px Start und Onboarding ohne Konsolenfehler, kein waagerechtes Scrollen.
  - **Plus:** mit eurem echten Test-Code Freischalt-Link, Endpunkt (200/403) und Laden aller 13 Plus-Pakete geprüft, auch mit Service Worker.
  - **Offline:** Der Service Worker speichert alles vorab; der zweite Start lädt 0 Byte.
  - **Stufen-Nachweis** (`DifficultyTest` mit Plus-Paketen) weiter grün, Tabelle in `SCHWIERIGKEIT.md`.
- **Texte nach außen:**
  - **Store-Beschreibung:** veraltete Angaben berichtigt („47 Pakete · über 1 600 Karten“ → „72 Wissenspakete · über 2 400 Karten“). Der Plus-Abschnitt sagt nicht mehr „bis zum Start alle Plus-Pakete gratis“.
  - **Datenschutz-Abschnitt** passt zum Stand (Plus-Code nur lokal, Endpunkt sieht Code und IP).
  - **Preise** überall mit „inkl. MwSt.“ (19,99 € / 49,99 €).

**Nicht oder nicht sicher prüfbar:**
- **Fakten ohne Quellenrecherche:** Die Prüfer hatten keinen Web-Zugriff. Unsichere Werte sind vorsichtig formuliert („etwa“, „gilt als“, „wohl“), z. B. Flusslängen, Epochengrenzen, Herkunft von Redewendungen, Sprecherzahlen. Die Erste-Hilfe-Inhalte sind nach ERC/DRK-Stand aus dem Gedächtnis geprüft.
  Die Angabe „27 EU-Staaten“ beruht auf dem Wissensstand.
- **Android auf echten Geräten:** Ein Gerät oder Emulator ist hier nicht verfügbar. Die Oberfläche ist über Robolectric geprüft. Startbild, Widget, Benachrichtigung, Drive-Sync und Kamera brauchen einen Gerätetest.
- **WebApp:** Nicht jeder Bildschirm wurde in jeder Breite einzeln angeklickt. Die gemeinsame Oberfläche ist über die Android-Bildschirmfotos abgedeckt.
- **Barrierefreiheit:** Nur die vorhandenen Beschriftungen und Kontraste sind geprüft, kein Screenreader-Durchlauf.
- **Echter Kauf über die Kasse:** Den macht Marcel.

**Stand:** 0.26.0 vom 17.11.2026 liegt auf wissensgarten-test. Die korrigierten Plus-Pakete liegen in `wg-plus-pakete`, die neuen Versionen stehen in `versions.json` (Commit `2f86855`).


## 0.25.1 vom 17.11.2026 – Test mit eurem echten Plus-Code: zwei Fehler gefunden und behoben

Ich habe mit dem echten Test-Code geprüft (Produkt 1, Nr. 1001, Kauftag 17.11.2026). Die Signatur mit dem Live-Schlüssel ist gültig, die App zeigt „aktiv bis 16.11.2027“.

**Fehler in 0.25.0:**
1. **Plus-Pakete wurden nicht geladen.** Die Vorschau eines Pakets hat dieselbe Versionsnummer und galt als „schon vorhanden“, deshalb kam kein Download. Behoben: Fürs Nachladen zählen nur echte Pakete (`PackCatalog.installedVersions()`, mit Test).
2. **WebApp:** Der Service Worker hat auch `/wg-api/` abgefangen und im Cache abgelegt (cache-first). `versions.json` wäre so nie aktualisiert worden, und lizenzierte Inhalte lagen im Browser-Cache.
   Behoben: `/wg-api/` geht am Service Worker vorbei. Alte Caches löscht die neue Version beim Aktivieren.

**Ergebnis mit 0.25.1** (WebApp lokal, `/wg-api/` an wissensgarten-test durchgereicht, mit und ohne Service Worker):
- Link `?pro=…` → „Plus ist freigeschaltet“, der Code ist aus der Adresse entfernt.
- Danach kamen **13 Plus-Pakete** an mit „🌟 13 neue Plus-Pakete sind da!“. Keine Fehler.
- Der Endpunkt liefert mit dem Code 200, ohne Code 403.

Alle Tests grün, Lint ohne Fehler.
**Bitte 0.25.1 statt 0.25.0 nach dem echten Kauf live schalten.** Den Test-Code habe ich nirgends gespeichert oder committet.

## 0.25.0 vom 16.11.2026 – Plus aktiv, Plus-Pakete nur noch über den Endpunkt

- **Sperren aktiv:** Die Kassen-URLs 4139 (Jahr) und 4140 (für immer) stehen in `pro/plus.properties`. In Web und GitHub-APK sind damit die Kauf-Knöpfe sichtbar und die Sperren aktiv:
  Plus-Pakete, mehr als 2 Profile, Wochenbericht. Die Play-Fassung bleibt weiter ohne Kauf und ohne Sperren.
- **13 Plus-Pakete raus** aus APK und WebApp (`content/…/packs` und `webapp/packs`): Es bleiben 72 freie Pakete.
  Die vollen Pakete gibt es nur noch über `/wg-api/plus/` (Stand Website_Backup `53afb08`).
- **Vorschau statt Paket:** `plus_katalog.json` (APK-Asset und `webapp/plus_katalog.json`) enthält je Plus-Paket nur Titel, Beschreibung, Erklär-Szenen, Kartenzahl und 3 Beispielkarten.
  - In der Samenhandlung stehen sie mit „🌟 Plus“; Antippen führt (Eltern-Tor) zur Plus-Seite.
  - Mit gültigem Code lädt die App die vollen Pakete vom Endpunkt; Antippen stößt das Laden sofort an.
  - Vorschauen gehen nie ins Lernen und verändern nie bestehende Beete.
- **Bestehende Beete** mit früheren Plus-Paketen bleiben vollständig lernbar, die Karten liegen ja schon im Lernstand.
- **7-Tage-Test:** Er schaltet Profile und Wochenbericht frei, die Plus-Pakete aber nicht, weil der Endpunkt ohne Kaufcode nichts ausliefert.
  Falls der Test auch Pakete öffnen soll: Der Lizenz-Dienst könnte 7-Tage-Test-Codes ausgeben, z. B. als neues Produkt.
- **Nachweis Schwierigkeit:** Der `DifficultyTest` liest die Plus-Pakete jetzt aus der privaten Ablage, lokal `website_backup/wissensgarten-plus` oder über `WG_PLUS_DIR`.
  Im CI ohne Zugriff prüft er die freien Stufen; `SCHWIERIGKEIT.md` wird nur mit allen Paketen neu geschrieben.
- **Tests:** alle grün, Lint ohne Fehler. WebApp ohne Fehler; der Endpunkt antwortet mit 200 (versions.json) bzw. 403 ohne Code.
  **Kauf mit echtem Code konnte ich nicht testen** (kein Kaufcode vorhanden). Bitte beim Prüfen einmal über die Kasse kaufen und den Link aus der E-Mail öffnen.
- **Bitte 0.25.0 auf wissensgarten-test prüfen (inkl. Kasse) und dann live schalten.**

## 0.24.0 vom 16.11.2026 – Wissensgarten Plus gebaut (Sperren noch aus)

**Gebaut (Web und GitHub-APK; Play ohne Kauf):**
- **Plus-Bereich** im Eltern-Bereich, hinter dem Eltern-Tor (Einstellungen → „Für Eltern“ → 🌟 Wissensgarten Plus).
- **7-Tage-Test:** einmal je Gerät, ohne Zahlungsdaten, endet von allein.
- **Kauf-Knöpfe:** „1 Jahr 19,99 €“ und „Für immer 49,99 €“, jeweils inkl. MwSt.
- **Code-Eingabe** im Eltern-Bereich.
- **Freischalt-Link** `https://wissensgarten.mathenachhilfe-online.com/?pro=<Code ohne Bindestriche>`. Der Code wird danach aus der Adresszeile entfernt.
- **Code-Format:** das gemeinsame Format, 1:1 wie KurvenFlug: Version 1, App 3, Präfix „WG-“, Ed25519, Crockford-Base32.
  - Produkt 1 = Plus Jahr, gültig ab Tag im Code für 365 Tage.
  - Produkt 3 = für immer.
  - Öffentlicher Schlüssel: `979eb515…b812`. Prüfung offline in `core/model/License.kt`; Generator für den Lizenz-Dienst: `wissensgarten/pro/plus_code.py`.
- **Sperren, sobald aktiv:**
  - Plus-Pakete: in der Samenhandlung „🌟 Plus“ statt „Pflanzen“.
  - Mehr als 2 Profile.
  - Wochenbericht.
  - Schon gepflanzte Beete bleiben immer lernbar, der Lernstand geht nie verloren.
- **Plus-Pakete:** alle `ob-…` (Abi, 11–13) und `aw-experte-…` (14 Pakete, je Version +1). `s1-geschichte-20-jahrhundert` (ab Kl. 9) ist jetzt frei, passend zu „frei bis Kl. 10“.

**Sperren greifen erst mit Kassen-URL:**
- Bitte in `wissensgarten/pro/plus.properties` die Werte `checkoutUrlYear=` (WG-PLUS-JAHR) und `checkoutUrlForever=` (WG-PLUS-IMMER) eintragen. Danach baut der nächste Release mit Sperren.
- Bis dahin ist alles frei, und die Kauf-Knöpfe sind ausgeblendet.
- Die Play-Fassung bleibt ganz ohne Kauf und ohne Sperren, bis Play Billing kommt.

**Plus-Pakete und Endpunkt:**
- Die App ruft bei aktivem Plus mit Code `wg-api/plus/versions.json` und `wg-api/plus/<datei>.json` mit Header `X-Lizenz` ab, genau nach der Schnittstelle oben.
  - Android: über die Live-Adresse.
  - WebApp: relativ zur eigenen Domain.
- Mit dem 7-Tage-Test wird nichts abgerufen, weil es keinen Code gibt. Er schaltet nur die mitgelieferten Plus-Pakete frei.
- **Ehrlicher Hinweis zu den 14 Plus-Paketen in der App:** Die 14 vorhandenen Plus-Pakete liegen schon in der öffentlichen Git-Historie, im APK und in `webapp/packs`.
  Sie sind in der App gesperrt, aber technisch nicht geheim. **Neue Plus-Pakete** lege ich nicht mehr ins öffentliche Repo, sondern liefere sie an euch für den Endpunkt. Den Ablageort bitte nennen.
  Sollen auch die 14 vorhandenen aus APK und WebApp raus (dann nur noch über den Endpunkt)? Das geht, sobald der Endpunkt läuft.

**Datenschutz-Ergänzung** (in App und Vorlage schon drin, Abschnitt „Käufe (Wissensgarten Plus)“):

> Alle Pakete bis Klasse 10 und alle Spielarten sind frei. „Wissensgarten Plus“ ist freiwillig und wird nur im Eltern-Bereich nach der Eltern-Aufgabe gekauft. WebApp und GitHub-Version: Der Kauf läuft über den Shop auf mathenachhilfe-online.com (WooCommerce). Dort gebt ihr die für Kauf und Rechnung nötigen Daten an; Rechnung und Freischalt-Code kommen per E-Mail. In der App wird kein Konto angelegt. Der Code wird nur auf diesem Gerät gespeichert und offline geprüft (digitale Unterschrift) – er enthält keinen Namen und keine E-Mail-Adresse. Der 7-Tage-Test wird ebenfalls nur auf dem Gerät vermerkt. Mit aktivem Plus lädt die App höchstens einmal am Tag neue Plus-Pakete von unserem Server (wissensgarten.mathenachhilfe-online.com) und schickt dabei den Code mit, damit der Server prüfen kann, ob Plus gültig ist. Der Server sieht dabei den Code, die IP-Adresse und den Zeitpunkt; daraus wird kein Profil gebildet, Lernstand und Profile werden nie übertragen. In der Play-Store-Version gibt es noch keine Käufe – dort ist im Moment alles frei.

**Tests:**
- 397 Tests grün, darunter neue Tests für Code-Prüfung, Ablauf, Link, Test-Zeitraum und Sperren mit und ohne Kassen-URL.
- Lint ohne Fehler, Play-Fassung kompiliert.
- Bildschirmfoto `screenshots/65_plus.png` angesehen.
- **Browser:** Ein `?pro=`-Link mit fremdem Code zeigt „ungültig“, der Parameter verschwindet aus der Adresse, keine Fehler.
- **Bitte 0.24.0 auf wissensgarten-test prüfen.**

## 16.11.2026 – Plus: Schnittstelle für den Paket-Endpunkt (bitte so bauen)

Basis: `https://wissensgarten.mathenachhilfe-online.com/wg-api/plus/`. Dieselbe Adresse gilt für die Test-WebApp (`wissensgarten-test.…`), die Android-App nutzt die Live-Adresse.

| Aufruf | Lizenz nötig | Antwort |
|---|---|---|
| `GET /wg-api/plus/versions.json` | nein | `200`, `{"ob-abi-xyz.json": 2, …}` – Dateiname → Paket-Version (wie `packs/versions.json`), `Cache-Control: no-cache` |
| `GET /wg-api/plus/<datei>.json` mit Header `X-Lizenz: <Code>` | ja | `200` Paket-JSON (Format wie `content/PACK_FORMAT.md`), `Content-Type: application/json`, `Cache-Control: private, no-store` |
| … ohne/mit ungültigem Code | – | `403` `{"fehler": "lizenz"}` |
| … Jahres-Plus abgelaufen | – | `403` `{"fehler": "abgelaufen"}` |
| … unbekannte Datei | – | `404` |

Prüfung auf dem Server, gleich wie in der App:
- **Code:** Format `WG-…` (Version 1, App-Byte 3), Ed25519 mit dem bekannten Schlüsselpaar.
- **Produkt 1 (Plus Jahr):** Gültig ab dem Tag im Code (Tage seit 1.1.2020) für 365 Tage, letzter gültiger Tag = Tag + 364.
- **Produkt 3 (Plus für immer):** Kein Ablaufdatum.

Weitere Regeln:
- Bindestriche und Klein-/Großschreibung im Header ignorieren, wie `License.extract`.
- Dateinamen nur nach `^[a-z0-9][a-z0-9-]{0,60}\.json$` annehmen.
- **CORS:** Die WebApp ruft von derselben Domain auf, also nichts nötig. Android ruft direkt auf.
- **Kein Logging des Codes** im Klartext (höchstens Hash), keine Cookies.
- **Rate-Limit:** z. B. 300 Abrufe je Code und Tag.

Die App lädt neue oder aktualisierte Plus-Pakete höchstens alle 20 h und nur bei aktivem Plus. Sie speichert sie lokal (Android: privater App-Speicher, Web: Browser-Speicher).
Ohne gültigen Code wird nichts abgerufen.

## 16.11.2026 – Konzept Lehrerzugang (noch nicht gebaut)

**Heute:** Es gibt den Klassen-Code `WGK1-…`. Die Lehrkraft teilt ein eigenes Beet als QR-Code bzw. Link, alles steckt im Code, ohne Server und ohne Rückmeldung.
Künftig wird er zum **Beitritts-Code des gemeinsamen Klassen-Dienstes**. Das alte Format bleibt für „nur Karten teilen“ ohne Konto erhalten.

### 1. Was die Lehrkraft sieht und steuert (im zentralen Lehrerzugang)

| Funktion | Inhalt |
|---|---|
| Klassen | Anlegen und löschen; je Klasse Name, Klassenstufe, Fach |
| Beitritt | Code/QR, z. B. `WG-7B-4K9P`; neu erzeugen; Beitritt sperren |
| Schülerliste | Nur Spitzname und Tier-Avatar; einzelne Kinder entfernen |
| Aufträge | Pakete aus dem Katalog (auch Plus bei Schullizenz) **oder eigene Karten der Lehrkraft** mit Frist und optional Mindestanzahl Karten „sicher“ |
| Klassenarbeit | Datum und Pakete vorgeben. Bei jedem Kind läuft dann der vorhandene Klassenarbeits-Countdown, der die Wiederholung bis zum Termin verteilt |
| Ergebnisse je Kind × Auftrag | Karten gelernt, sicher (Busch und höher), Wackelkandidaten, letzte Aktivität (Tag), Auftrag erledigt ja/nein |
| Klassensicht | „Diese 5 Karten machen der Klasse Mühe“ (häufigste Fehler), Fortschritt aller Kinder als Balken |
| Export / Löschen | CSV je Klasse; Klasse samt aller Daten löschen; Daten werden 30 Tage nach Schuljahresende automatisch gelöscht |
| Schullizenz | Laufzeit, Anzahl Klassen und Kinder, Lehrkräfte der Schule einladen (Schul-Admin) |

### 2. Daten, die die App schickt (nur im Klassenmodus, sonst nichts)

| Gesendet | Nicht gesendet |
|---|---|
| Zufällige Kind-ID (UUID, nur für diese Klasse), Spitzname aus Auswahl, Avatar | Echter Name, E-Mail, Alter, Gerät, Standort, Werbe-ID |
| Je Auftrag und Karte: Stufe (Samen … Baum), Anzahl richtig/falsch, Tag der letzten Wiederholung | Getippte Antworten, Fotos, Sprache, Zeichnungen |
| Einmal am Tag ein kleines Paket (ca. 1–5 KB), nur wenn ein Auftrag aktiv ist | Lernzeit pro Minute, Nutzung außerhalb der Aufträge, eigene private Beete |

Die Daten liegen auf dem Server der Website (DE). Die Lehrkraft bzw. Schule ist verantwortlich, wir sind Auftragsverarbeiter (AV-Vertrag als Vorlage).
Dazu kommt ein Abschnitt in der Datenschutzerklärung.

### 3. Beitritt und was das Kind sieht

| Schritt | Ablauf |
|---|---|
| Beitreten | Einstellungen → **Eltern-Tor** → „Klasse beitreten“ → QR scannen oder Code tippen |
| Spitzname | Aus einer Liste (z. B. „Flinker Fuchs“ und Tier) wählen; kein Freitext, kein Chat, keine Sicht auf andere Kinder |
| Im Garten | Ein eigenes **Klassen-Beet mit Schul-Schild 🏫**. Aufträge erscheinen als Beet mit Frist („noch 5 Tage“) und Brummi-Hinweis |
| Belohnung | Normal wie immer (Tautropfen, Figuren). Es gibt **keine Rangliste**, nur „Die Klasse hat zusammen 1.240 Karten gegossen 🌻“ |
| Austreten | Eltern-Tor → „Klasse verlassen“. Die Daten des Kindes beim Server werden sofort gelöscht, der eigene Lernstand bleibt |

### 4. Frei und Schullizenz

| Frei für Lehrkräfte | Schullizenz |
|---|---|
| 1 Klasse, bis 30 Kinder | Unbegrenzt Klassen und Lehrkräfte der Schule |
| Aufträge mit freien Paketen und eigenen Karten | Alle Plus-Pakete (Abi/Experte) für die Klasse |
| Ergebnisse je Kind (gelernt / sicher / Fehler) | Klassenarbeits-Planung, Klassensicht „häufigste Fehler“, CSV-Export |
| – | **Alle Kinder der Klassen haben Plus** (zu Hause, auch KI-Brummi mit Schulgrenze) für die Laufzeit |

**Preise – entschieden von Marcel am 16.11.2026** (inkl. 19 % USt, je Schuljahr bis 31.07.; ersetzt den Vorschlag 49/299/449 €):

| Lizenz | Brutto | Netto |
|---|---|---|
| Lehrkraft-Lizenz Wissensgarten (eine Lehrkraft, ihre Klassen) | 29 € | 24,37 € |
| Schul-Lizenz Wissensgarten (alle Lehrkräfte, egal wie viele Kinder) | 199 € | 167,23 € |
| Paket alle Apps – je Lehrkraft | 79 € | 66,39 € |
| Paket alle Apps – je Schule | 499 € | 419,33 € |

Verkauf und Verwaltung laufen über den zentralen Lehrerzugang (baut der Leitstand). **In der App wird nichts gebaut, bis die Schnittstelle steht.**
Verkauf über die Woo-Kasse mit Rechnung (auch auf Rechnung bzw. Bestellnummer des Schulträgers). Keine Abo-Automatik, die Lizenz läuft zum Schuljahresende aus, mit Erinnerung.

## 16.11.2026 – Konzept „Wissensgarten Plus“ (noch nicht gebaut)

### 1. Frei und Plus

Leitgedanke wie bei ANTON: **Das Lernen für Schulkinder bleibt vollständig frei.** Plus richtet sich an Abi, Erwachsene, Familien mit
mehreren Kindern und alle, die KI-Hilfe wollen. Werbung gibt es nie. Plus ist bewusst kein „Weiterlernen nur gegen Geld“.

| Frei (für immer) | Plus |
|---|---|
| Alle Grundschul- und Klasse-5–10-Pakete (75 Pakete, ca. 2.580 Karten) | **Abi- und Experten-Stufe:** die 6 Oberstufen-Pakete (bisher schon `plus`), die 8 neuen Abi-/Experten-Pakete und `s1-geschichte-20-jahrhundert` (heute `plus`) |
| Garten, alle Spielarten, Brummi, Profi-Stufe, Abenteuer, Sammelfiguren, Duelle, Widget, Erinnerung | **Neue Plus-Pakete jeden Monat** (Nachladen wie heute) |
| Eigene Karten/Beete (von Hand), Klassen-Code, Klassenarbeits-Countdown | **KI-Brummi** (siehe 4): erklärt falsche Antworten, macht Karten aus Text oder Foto, wertet freie Antworten nach Sinn |
| 2 Profile | **Familie:** bis 6 Profile, Wochenbericht je Kind als PDF, Geräte-Sync für die ganze Familie |
| Sync über das eigene Google Drive | Kosmetik: Plus-Gartenmotive (z. B. Nachtgarten, Unterwasser), nicht spielentscheidend |

**Preise** (Vergleich, Stand Nov. 2026; Preise je Land und Store unterschiedlich):

| App | Modell | Preis |
|---|---|---|
| ANTON Plus Familie | Kern gratis, Plus freiwillig | Stufen 19,99 / 24,99 / 49,99 € im App Store |
| Quizlet Plus | Abo | ca. 35,99 $/Jahr bzw. 7,99 $/Monat (EU-Angaben teils 19,99 €/Jahr) |
| Brainscape Pro | Abo | ca. 95,99 $/Jahr, lebenslang 199,99 $ |
| AnkiMobile (iOS) | einmalig | 24,99 $ (Android/Desktop gratis) |

**Vorschlag:**
- **Plus Familie – Jahr:** 19,99 €. Bis 6 Profile, alle Geräte der Familie, KI mit fairer Grenze. Das liegt auf ANTON-Plus-Niveau und unter Quizlet.
- **Plus Familie – für immer:** 49,99 € einmalig. Für Eltern, die kein Abo wollen. Die KI ist dann fürs erste Jahr enthalten, danach 9,99 €/Jahr optional.
- **Kein Monatsabo:** Für eine Kinder-App ist es zu kleinteilig, Eltern wollen eine klare Summe. Kein Testabo mit Kündigungsfalle.
  Stattdessen 7 Tage Plus gratis per Knopf, ohne Zahlungsdaten, endet von allein.

**Umsatzsteuer (Nachtrag 16.11.):**
- Alle Preise sind **Endpreise inkl. 19 % USt** und werden in App und Shop mit „inkl. MwSt.“ angezeigt.
- **Web:** Das Woo-Produkt bekommt die Steuerklasse Standard 19 %, die Rechnung weist die USt aus. Für Privatkunden in anderen EU-Ländern rechnet die Kasse den dortigen Satz ab (OSS).
  Empfehlung: Der **Bruttopreis bleibt in der ganzen EU gleich** (19,99 €), nur der Netto-Anteil schwankt etwas, z. B. bei 20 % USt in AT netto 16,66 €.
- **Play:** Google führt die USt selbst ab. Vom Bruttopreis gehen erst die USt und dann 15 % Servicegebühr ab (Abos und Umsätze unter 1 Mio. $ pro Jahr).

| Angebot | Brutto | darin USt 19 % | Netto | **Web:** Zahlungsgebühr* | **Web: kommt an** | **Play:** 15 % vom Netto | **Play: kommt an** |
|---|---|---|---|---|---|---|---|
| Plus Familie – Jahr | 19,99 € | 3,19 € | **16,80 €** | 0,85 € | **15,95 €** | 2,52 € | **14,28 €** |
| Plus Familie – für immer | 49,99 € | 7,98 € | **42,01 €** | 1,60 € | **40,41 €** | 6,30 € | **35,71 €** |
| KI-Verlängerung – Jahr (für „für immer“) | 9,99 € | 1,60 € | **8,39 €** | 0,60 € | **7,79 €** | 1,26 € | **7,13 €** |

\* Annahme für die Woo-Kasse (PayPal/Stripe): ca. 2,5 % + 0,35 € je Zahlung vom Bruttobetrag. Bitte mit Marcels tatsächlichem Tarif ersetzen.
Davon gehen noch die KI-Kosten ab: im Schnitt deutlich unter 1 €, bei einer sehr aktiven Familie bis ca. 3,70 € (4 $) im Jahr.
Ergebnis: Ein Web-Kauf bringt je Jahres-Plus ca. **1,67 € mehr** als Play. Im Play-Build darf darauf aber nicht hingewiesen werden (siehe unten).

### 2. Kauf: Eltern-Tor, Play Billing, Website

- **Vor jedem Kauf das Eltern-Tor** (gibt es schon: Rechenaufgabe für Erwachsene). Kaufknöpfe stehen nur im Eltern-Bereich, nie mitten im Spiel. Kein Druck („nur heute“) und keine Countdown-Angebote.
- **Play-Fassung:** Google Play Billing (Pflicht). Produkte: `plus_familie_jahr` (Abo, 1 Jahr) und `plus_familie_immer` (einmaliger In-App-Kauf).
  Im Play-Build **keine** Hinweise oder Links auf den günstigeren Web-Kauf, sonst gibt es Ärger mit Play- und Familien-Richtlinien.
- **Web- und GitHub-Fassung:** Kauf über die Woo-Kasse auf mathenachhilfe-online.com. Danach geht der **Freischalt-Code** per E-Mail an den Käufer,
  die Rechnung geht an Käufer und Marcel, und der Beleg liegt in WooCommerce. Der Code wird in der App eingegeben (Eltern-Tor), **ohne Konto**.
  - **Format:** `WGP1-…`, von uns **digital signiert** (Ed25519). Die App prüft die Unterschrift offline mit dem öffentlichen Schlüssel; der geheime Schlüssel liegt nur auf dem Server.
  - **Inhalt:** Produkt, Ablaufdatum (bzw. „immer“), Bestell-Nr. als Hash. Es steht kein Name und keine E-Mail im Code.
  - **Übertragen:** Derselbe Code darf auf allen Familiengeräten eingegeben werden (Familie = bis 6 Profile). Missbrauch begrenzen wir bewusst nicht hart (keine Gerätebindung, keine Datensammlung).
- **Bestehende Plus-Käufer anderer Wege:** Ein Web-Code wirkt auch in der GitHub-APK. Ein Play-Kauf wirkt in der Play-Fassung. Gemeinsam nutzen über den Familien-Sync wäre möglich, ist aber nicht nötig.

### 3. Schutz der Plus-Pakete

**Heute:** Alle Pakete, auch `tier: plus`, liegen offen in der APK, in der WebApp (`webapp/packs/`) und über raw.githubusercontent.com.
Das muss sich vor dem Start ändern:
1. **Plus-Pakete raus aus Repo, APK und WebApp.** Sie kommen in ein privates Repo bzw. auf den Server (außerhalb des Web-Roots).
   Im öffentlichen Katalog bleiben nur Titel, Beschreibung, Kartenzahl und 3 Beispielkarten, damit die Samenhandlung sie zeigen kann.
   Achtung: Die bisherigen 6 Plus-Pakete liegen schon in der öffentlichen Git-Historie. Das nehmen wir hin (alter Stand), neue Plus-Inhalte kommen nie ins öffentliche Repo.
2. **Ausgabe nur gegen Nachweis:** Ein Server-Endpunkt `…/wg-api/plus/<paket>` (PHP, beim WordPress) liefert das Paket nur
   - für einen gültigen, signierten Web-Code (Prüfung der Signatur und des Ablaufdatums), oder
   - für ein Play-Kaufbeleg-Token. Der Server prüft es über die Google Play Developer API (Dienstkonto, Rechte nur für Käufe).
3. **Auf dem Gerät** speichern wir das Paket im privaten App-Speicher (Web: IndexedDB). Echtes DRM gibt es nicht, und für Lernkarten wäre es übertrieben.
   Ziel ist: nicht frei im Netz, nicht ohne Kauf ladbar.
4. **Datensparsam:** Der Endpunkt sieht nur Code bzw. Token und IP, ohne Konto und ohne Tracking. Diese Datenschutz-Ergänzung kommt in Abschnitt 10c.

### 4. KI (Claude-API) – sinnvoll, günstig, aber nur mit Bedacht

Sinnvoll, in dieser Reihenfolge:
1. **„Brummi, warum?“** Nach einer falschen Antwort erklärt Brummi kurz und altersgerecht, warum die richtige Antwort stimmt.
   Hoher Lernwert. Die Antwort bezieht sich nur auf die Karte (Frage, Antwort, `info`), sodass wenig erfunden werden kann.
2. **Freie Antworten nach Sinn werten** (Abi/Experte): „Strukturisomere“ statt „Konstitutionsisomere“ zählt dann als richtig, mit Begründung.
3. **Karten aus Text oder Foto machen** (Lernzettel, Arbeitsblatt): Die KI schlägt 10–30 Karten mit Ablenkern und Merkbild vor, Eltern bzw. Kind bestätigen.

**Kosten** (Anthropic-Listenpreise, Nov. 2026): Claude Haiku 5.5 kostet 0,10 $ pro 1 Mio. Eingabe-Tokens und 0,50 $ pro 1 Mio. Ausgabe-Tokens.
Claude Sonnet 5.5 kostet 2 $ bzw. 10 $ pro 1 Mio. Tokens.

| Aufgabe | Modell | Tokens (ca.) | Kosten je Aufruf |
|---|---|---|---|
| Brummi erklärt | Haiku 5.5 | 600 ein / 150 aus | ca. 0,014 ct |
| Freie Antwort werten | Haiku 5.5 | 400 / 60 | ca. 0,007 ct |
| 30 Karten aus Text | Sonnet 5.5 | 2.000 / 3.000 | ca. 3,4 ct |

**Vielnutzer-Familie** (50 Erklärungen/Tag + 4 Kartensätze/Monat): ca. 2,50 $ + 1,60 $ ≈ **4 $ pro Jahr**. Das passt gut in 19,99 €.
Faire Grenze in Plus: 100 Erklärungen pro Tag und 20 Kartensätze pro Monat.

**Technik und Schutz:**
- Die App ruft **nie direkt** die API auf, der Schlüssel bleibt auf dem Server. Der Weg ist: App → unser Endpunkt (prüft Plus-Code bzw. -Kauf, Grenze) → Claude-API.
- Gesendet wird **nur der Karteninhalt**, bei der Wertung zusätzlich die getippte Antwort. Es geht kein Name, kein Profil und keine Geräte-ID hinaus. Bei Anthropic sollte Zero Data Retention beantragt werden.
- **Kinderschutz:** Die KI antwortet nur zur Karte, mit festem System-Prompt (kindgerecht, keine Links, keine persönlichen Fragen). Jede KI-Antwort bekommt einen Knopf „Das stimmt nicht / unpassend melden“ (Pflicht nach den Play-Richtlinien für KI-Inhalte).
  KI-Funktionen sind nur hinter dem Eltern-Tor einschaltbar und standardmäßig **aus**.
- **Datenschutz:** neuer Abschnitt „KI-Hilfe (optional)“ mit Anthropic als Auftragsverarbeiter. In der Play-Data-Safety wird „App-Aktivität: Inhalte werden an Dienst übermittelt, nicht gespeichert“ ergänzt.
- **Ohne Plus:** Freie Antworten werden weiter wie heute ohne KI geprüft (Zahlen nach Wert, Schreibvarianten).

**Empfehlung zur Reihenfolge:**
1. Plus-Pakete schützen.
2. Web-Code und Play Billing anbinden.
3. Erst danach KI-Brummi (Punkt 1), als Testballon mit Messung der Kosten.

## 0.23.0 vom 16.11.2026 – Schwierigkeit steigt nachweislich bis zur Experten-Stufe

**Stufen im Wissensgarten:**
- **Beim Spielen:** das Wachstum jeder Karte, Samen → Baum = „Profi“. Es steuert jetzt die Schwierigkeit:
  - Zahl der Antworten (3 → 4 → keine Auswahl)
  - Ähnlichkeit der Ablenker (unähnlichste → ähnlichste)
  - Anteil freier Eingabe (0 % → 100 %)
  - Rate-Chance; „Stimmt's?“ (50 %) gibt es nur noch auf der leichtesten Stufe
  - Wiederholungsabstand
  In der Runde zeigt ein Abzeichen die Stufe (🌱 Leicht … 🏆 Profi).
- **Bei den Inhalten:** die Klassen-Bänder Grundschule → Kl. 5–7 → Kl. 8–10 → Abi → **Experte (neu)**.

**Neu (Profi/Experte):** 8 Pakete mit 270 Karten, unabhängig fachlich geprüft (Abi: 3 Korrekturen, Experte: 13 Korrekturen).
- **Abi:** ob-abi-stochastik, ob-abi-biologie, ob-abi-chemie, ob-abi-geschichte
- **Experte:** aw-experte-naturwissenschaft, aw-experte-geschichte-kultur, aw-experte-sprache, aw-experte-mathe-logik
- **Eigenschaften:** überwiegend freie Eingabe, sehr ähnliche Ablenker
- **Zeitdruck:** keiner. Er passt nicht zum Lernen mit Wiederholung; die Blitzrunde bleibt ein freiwilliges Extra.

**Nachweis:** `content/src/test/java/de/wissensgarten/content/DifficultyTest.kt`
- **Was er misst:** alle 85 Pakete und 2.923 Karten werden je Stufe gespielt und gemessen.
- **Wann er fehlschlägt:**
  - Beim Spielen: wenn die Rate-Chance nicht strikt sinkt, der Abstand nicht strikt wächst oder freie Eingabe, Antwortzahl oder Ablenker-Ähnlichkeit fallen. Außerdem, wenn Profi unter 90 % freie Eingabe hat.
  - Bei den Inhalten: wenn Klasse, freie Eingabe, Ablenker-Ähnlichkeit oder Antwortlänge nicht strikt steigen.
- **Wo er läuft:** im Release-Workflow (`:content:test`).
- **Bericht:** `wissensgarten/SCHWIERIGKEIT.md`. Ähnlichkeit ist die normierte Levenshtein-Ähnlichkeit (0 = ganz anders, 1 = gleich).

| Stufe | Pflanze | freie Eingabe | Ø Antwortmöglichkeiten | Ablenker-Ähnlichkeit | Rate-Chance | Wiederholungsabstand |
|---|---|---|---|---|---|---|
| Entdecken | Samen | 0 % | 3,0 | 0,34 | 100 % | neu |
| Leicht | Keimling | 0 % | 3,0 | 0,34 | 39 % | ≈ 1 Tag |
| Mittel | Spross | 27 % | 4,0 | 0,53 | 17 % | ≈ 4 Tage |
| Knifflig | Busch | 43 % | 4,0 | 0,55 | 13 % | ≈ 14 Tage |
| Schwer | Blüte | 63 % | 4,0 | 0,58 | 9 % | ≈ 40 Tage |
| Profi | Baum | 100 % | keine Auswahl | – | 0 % | ≈ 90 Tage |

| Stufe | Pakete | Karten | Ø Klasse | freie Eingabe | Ablenker-Ähnlichkeit | Ø Antwortlänge |
|---|---|---|---|---|---|---|
| Grundschule | 24 | 786 | 2,9 | 1 % | 0,46 | 8,1 Zeichen |
| Klasse 5–7 | 38 | 1342 | 7,3 | 16 % | 0,54 | 14,5 Zeichen |
| Klasse 8–10 | 13 | 452 | 10,3 | 34 % | 0,57 | 16,0 Zeichen |
| Oberstufe/Abi | 6 | 207 | 12,0 | 54 % | 0,75 | 19,4 Zeichen |
| Experte | 4 | 136 | Erw. | 76 % | 0,90 | 21,5 Zeichen |

**Dabei verbessert:**
- **Zahlen:** Getippte Zahlen werden nach Wert verglichen (0,5 = 0,50; kein „fast richtig“ mehr für 0,2462 statt 0,2461; Vorzeichen zählt).
- **Bindestriche:** Varianten wie „Marshall-Plan“ und „Marshallplan“ zählen beide als richtig, außer im Fach Deutsch.

**Tests:** 384 Tests grün, Lint ohne Fehler, Web = Android gleich (85 Pakete), WebApp im Browser ohne Fehler.
**Bitte prüfen und `wissensgarten-live` auf 0.23.0 schalten.** Danach bekommen auch installierte Apps die 8 neuen Pakete per Nachladen.

## 16.11.2026 – Nachmessung nach der gzip-Korrektur

Gemessen mit echtem GET und `Accept-Encoding: gzip`:
- `wissensgarten.js` überträgt jetzt **129 KB statt 662 KB**.
- `versions.json` und die Schriften sind ebenfalls gzip.

**Erster Start der Test-WebApp 0.22.0** (pro Datei gemessen):

| Datei | übertragen |
|---|---|
| Emoji-Schrift | 1.915 KB |
| wasm (App) | 3.162 KB |
| wasm (Skia) | 1.945 KB |
| JS | 126 KB |
| **Summe** | **ca. 7,0 MB** |

**Vergleich:** 0.21.0 vor der Korrektur lud **12,3 MB**, also jetzt **−43 %**. Der zweite Start lädt 0 Byte (Service Worker).
Live (noch 0.21.0) profitiert ebenfalls schon: JS 129 KB, Emoji-Schrift gzip 5,8 MB (ab 0.22.0: 1,9 MB).
**0.22.0 kann live geschaltet werden.**

## 16.11.2026 – Server-Einstellung: Komprimierung für die WebApp (nginx)

**Befund (live geprüft):** nginx komprimiert `.wasm` bereits, `wissensgarten.js` (647 KB, gepackt 127 KB) und `.ttf` aber nicht.
Mit der Einstellung unten lädt jedes neue Gerät beim ersten Start ca. 0,6 MB weniger. Die App muss dafür nicht geändert werden.

In den `server { … }`-Block von **wissensgarten.mathenachhilfe-online.com** und **wissensgarten-test.mathenachhilfe-online.com**
(oder einmal in den `http { … }`-Block) eintragen:

```nginx
gzip on;
gzip_comp_level 6;
gzip_min_length 1024;
gzip_vary on;
gzip_types text/javascript application/javascript application/json application/manifest+json application/wasm font/ttf text/css image/svg+xml;
```

`text/html` komprimiert nginx immer, das muss nicht in die Liste. `application/wasm` steht drin, damit die heutige Komprimierung erhalten bleibt.

**Testen** (nach `nginx -t` und `systemctl reload nginx`):
```bash
curl -sI -H "Accept-Encoding: gzip" https://wissensgarten.mathenachhilfe-online.com/wissensgarten.js | grep -i content-encoding
curl -sI -H "Accept-Encoding: gzip" https://wissensgarten.mathenachhilfe-online.com/composeResources/de.wissensgarten.web.res/font/noto_color_emoji.ttf | grep -i content-encoding
```
Beide Befehle sollen `content-encoding: gzip` zeigen.

## 0.22.0 vom 16.11.2026 – Start optimiert (Web + Android)

**1. Kein weißer Bildschirm**
- **Ladebild:** Das Ladebild (Brummi, Name, App-Farben) steht inline in HTML/CSS und ist ab dem ersten Byte ohne JS sichtbar. Erstes Bild nach 0,4 s (Fast 4G) bzw. 1,2 s (Slow 4G).
- **Fortschrittsbalken:** Der Balken ist echt und zeigt Prozent. Ein `fetch`-Wrapper zählt per ReadableStream die Bytes von App-JS, beiden `.wasm` und den Schriften. Die Gesamtgröße schreibt das Build in die Seite.
- **Übergang:** Danach blendet die Seite in 0,5 s in die App über.
- **Ursache des weißen Bildschirms:** Eine zweite CSS-Regel hatte die Ebene des Startbilds wieder heruntergesetzt (z-index 10), daher sah Marcel nur den Ladekreis der App auf Weiß. Beides ist behoben: Das Startbild liegt oben, und auch der Lade-Zwischenschritt der App hat jetzt die Garten-Farben statt Weiß/Lila.

**2. Schneller**
- **Komprimierung geprüft:** Auf dem Live-Server kommen `.wasm` gzip-komprimiert an, `wissensgarten.js` aber **nicht** (647 KB statt 127 KB gzip) und `.ttf` ebenfalls nicht.
  Empfehlung für nginx: `gzip_types text/javascript application/javascript font/ttf application/json;`
- **Emoji-Schrift:** Sie enthält nur die genutzten Emojis, 2,0 statt 6,0 MB (seit 0.21.2). Sie wird per `<link rel="preload">` parallel geladen.
- **Service Worker:** Er speichert beim Installieren alle Dateien (App, wasm, Schriften, 77 Pakete). Angemeldet wird er erst, wenn die App steht, damit er beim ersten Laden nicht um die Leitung konkurriert. Der zweite Start lädt 0 Byte und läuft offline.
- **Code-Splitting:** Kotlin/Wasm (Compose Multiplatform) unterstützt kein Aufteilen des Programmcodes in nachladbare Teile. Die zwei wasm-Dateien (App und Grafik-Engine Skia) werden gebraucht, bevor überhaupt etwas gezeichnet wird. Nachgeladen wird daher nur, was Daten sind (Pakete, Schriften).

**3. Messungen**
- **Aufbau:** Pixel 7 (Playwright). Beide Versionen kommen vom selben lokalen Server mit fester Bandbreite, gzip wie nginx und ETag.
  Grund: Die Drosselung von Chrome erfasste nicht alle Downloads.
- **Profile:** Fast 4G = 9 Mbit/s, 165 ms; Slow 4G = 1,6 Mbit/s, 562 ms.
- **Erster vs. zweiter Start:** Der zweite Start folgt, nachdem der Service Worker fertig gespeichert hat.

| Version | Netz | 1. Start: erstes Bild | 1. Start: bedienbar | 2. Start: erstes Bild | 2. Start: bedienbar |
| --- | --- | --- | --- | --- | --- |
| vorher 0.21.0 | Fast 4G | 0,4 s | 16,9 s | 0,2 s | 4,0 s |
| **nachher 0.22.0** | Fast 4G | 0,4 s | **13,0 s** | 0,2 s | 4,2 s |
| vorher 0.21.0 | Slow 4G | 1,2 s | 67,3 s | 0,6 s | 5,0 s |
| **nachher 0.22.0** | Slow 4G | 1,2 s | **39,8 s** | 0,6 s | 4,4 s |

**Downloads beim 1. Start** (gemessen pro Datei):

| Datei | vorher (live) | nachher |
| --- | --- | --- |
| Emoji-Schrift | 6.055 KB | 2.045 KB |
| wasm (App) | 3.550 KB | 3.187 KB |
| wasm (Skia) | 2.240 KB | 1.968 KB |
| JS | 647 KB (live ohne gzip) | 127 KB (mit gzip) |
| Symbolschrift | 120 KB | 120 KB |
| **Summe** | **ca. 12,3 MB** | **ca. 7,3 MB (−41 %)** |

- **Restzeit:** Etwa 4 s des zweiten Starts sind reine Rechenzeit: Übersetzen des wasm-Codes auf der langsamen Test-Maschine. Auf einem aktuellen Handy ist das weniger.

**4. Android**
- **Startbild:** Android-12-Splash-API (`core-splashscreen`, rückwärtskompatibel), hell in Creme `#FBF6EC` und dunkel in `#15190F`, mit dem Garten-Logo. Danach `postSplashScreenTheme` mit gleichem Fensterhintergrund, also kein weißes Aufblitzen.
- **Nicht auf Gerät geprüft:** Das Startbild konnte ich nicht auf einem echten Gerät ansehen (kein Emulator hier). Kompiliert und Lint sind sauber.

**Tests:** 379 Tests grün, Lint ohne Fehler, Play-Variante kompiliert. Im Browser keine Fehler.
**Bitte prüfen und `wissensgarten-live` auf 0.22.0 schalten.**

## 0.21.2 vom 16.11.2026 – WebApp: erster Start deutlich schneller

Marcel meldete: „Lädt recht lange“. Beim ersten Öffnen wurden ca. 11 MB übertragen. Mehr als die Hälfte davon war die Emoji-Schrift (6 MB),
weil sie alle Flaggen- und Hautfarben-Varianten enthielt.
- Die Schrift enthält jetzt nur die tatsächlich genutzten Emojis (+ Smileys): **2 MB statt 6 MB**. Damit werden beim ersten Start insgesamt ca. 7 statt 11 MB geladen.
  Alle Glyphen aus Paketen und Code sind enthalten; geprüft per Skript, dazu ein Sichttest im Browser.
- Die Schrift wird per `<link rel="preload">` gleich parallel zur App geladen statt erst danach.
- Die 379 Android-Tests sind grün, Lint ohne Fehler.
- Im Browser (Pixel 7) startet die WebApp ohne Fehler, die Emojis sind korrekt dargestellt.

Tipp für den Server (nginx): `gzip_types` um `font/ttf` ergänzen. Das spart bei der Schrift nur ca. 7 %, kostet aber nichts. WASM wird schon gzip-komprimiert.
**Bitte nach Prüfung `wissensgarten-live` auf 0.21.2 schalten.**

## 0.21.1 vom 16.11.2026 – WebApp: Startbild bleibt beim ersten Laden sichtbar

**Problem (von Marcel am Handy gemeldet):** Beim ersten Öffnen über mobiles Netz war nur ein weißer Bildschirm mit lila Ladekreis zu sehen.
Ursache: Die Zeichenfläche der App lag über Brummis Startbild, während einmalig ca. 10 MB geladen werden (App + Emoji-Schrift).
Das ist bei schwachem Netz entsprechend langsam.

**Lösung:**
- Das Startbild liegt jetzt sicher oben und zeigt einen Ladebalken.
- Nach 6 s erscheint ein Hinweis, dass der erste Start einmalig ca. 10 MB lädt, nach 30 s „Netz langsam“, nach 90 s ein Knopf „Nochmal versuchen“.
- Ab dem zweiten Start kommt alles aus dem Zwischenspeicher.

**Getestet:** Mit gedrosseltem 4G-Profil (Pixel 7): Startbild und Hinweis sind sichtbar, danach erscheint die App, ohne Fehler.
Nur die WebApp hat sich geändert; die APK hat aus Gründen des Gleichstands die gleiche Versionsnummer.
**Bitte `wissensgarten-live` auf 0.21.1 schalten.**

## 0.21.0 vom 16.11.2026 – Inhalte stark erweitert (30 neue Pakete)

**Umfang:** 30 neue Wissenspakete mit 1.046 neuen Karten. Jetzt sind es **77 Pakete mit 2.653 Karten** (vorher 47 Pakete).
Jedes neue Paket hat 4–5 Erklär-Szenen (Brummi), zu jeder Karte 3 Ablenker, eine Erklärung und ein Emoji-Merkbild.
- **Grundschule (10):**
  - Mathe/Deutsch: Rechnen bis 100, Zahlen bis 1000, Schriftlich rechnen, Formen und Körper, Satzzeichen und Sätze
  - Englisch und Sachkunde: Farben/Zahlen/Tiere, Familie/Körper/Kleidung, Pflanzen und Wald, Gesund und fit, Ritter und Burgen
- **Klasse 5–10 (19):**
  - Mathe: Kopfrechnen-Tricks, Negative Zahlen, Gleichungen, Zufall und Statistik, Pythagoras und Trigonometrie
  - Sprachen: English Tenses, Englisch im Alltag, Komma & Co., Fremdwörter
  - Naturwissenschaften: Pflanzen, Wirbeltiere, Organe, Strom und Magnetismus, Säuren/Basen/Salze
  - Gesellschaft: Steinzeit und Ägypten, DDR und Deutsche Einheit, Klima und Naturgefahren, Flüsse/Berge/Meere, Weltreligionen (erstes Ethik-Paket)
- **Für alle (1):** Redewendungen.

**Fachliche Prüfung:** Jedes Paket hat ein Autor geschrieben und ein zweiter, unabhängiger Prüfer Karte für Karte gegengelesen.
Der Prüfer hat Rechnungen nachgerechnet und geprüft, dass kein Ablenker ebenfalls richtig ist. Dabei gab es 16 Korrekturen, zum Beispiel:
- vertretbare Ablenker ersetzt (DDR war doch UN-Mitglied; „We visit …“ im amerikanischen Englisch)
- umstrittene Aussagen entschärft (Bergfried als „letzte Zuflucht“)
- eine ungenaue Regel („vor dass immer Komma“)

Zusätzlich habe ich 20 Karten stichprobenartig selbst geprüft: ohne Fehler.
Umstrittene Rekorde formulieren die Karten vorsichtig, etwa Nil/Amazonas als „streiten um den Titel“.

**Tests:**
- **Pakete:** Neues Prüfskript `content/tools/check_pack.py` (Format, ids, Versionen, Emoji nur bis Version 11, max. 1 MB) – 77/77 in Ordnung.
  `PackValidationTest` prüft jetzt auch die 1-MB-Grenze und läuft ab jetzt im Release-Workflow mit; vorher lief er dort nicht und war rot (Intro mit 6 Szenen, 🧃 in gs-geld → behoben, gs-geld v2).
- **Android:** 379 Unit-/Robolectric-Tests grün, Lint ohne Fehler, Bildschirmfotos angesehen (Samenhandlung: „77 Wissenspakete mit 2653 Karten“).
- **Gleichstand Web = Android:** `diff` zwischen App-Paketen und `webapp/packs` ohne Unterschied; `versions.json`/`index.json` mit 77 Einträgen.
- **Browser:** Handy (Pixel 7) und PC (1280×800): Start, alle 77 Paketdateien abrufbar, keine Konsolenfehler, Onboarding klickbar.
  Die Emoji-Schrift der WebApp ist neu erzeugt; keine fehlende Glyphe in den Paketen.
- **Regression:** Bestehende Pakete unverändert (außer gs-geld v2: Merkbild 🧃 → 🥤); der Lernfortschritt bleibt erhalten.

**Android-Apps:** Bekommen die Pakete mit 0.21.0 und ohne App-Update über das Nachladen, sobald `wissensgarten-live` 0.21.0 trägt.
**Bitte prüfen und `wissensgarten-live` schalten.**

## 16.11.2026 – Schlüssel-Ablage: Bitwarden statt Drive

Im Wissensgarten stehen jetzt nur noch Bitwarden und die Repo-Secrets als Ablage. Geändert sind die Kopfzeile von `wissensgarten-release.yml`,
`wissensgarten/README.md` (Abschnitt „Signieren“) und der Kommentar in `wissensgarten/.gitignore`.
`kurvenflug-release.yml` und `raumlicht-release.yml` haben wir **nicht** geändert: Laut unserer Vorgabe dürfen wir KurvenFlug und
Raumlicht nicht anfassen. Dort steht in Zeile 7 noch „Drive, Ordner „Schlüssel (geheim)““ – bitte an die zuständigen Sitzungen geben.

## 0.20.0 vom 16.11.2026 – Updates ohne Store-Besuch + Wissenspakete nachladen

1. **Play In-App Updates:** Standard ist „Flexible“. Die neue Version lädt im Hintergrund und wird eingespielt, sobald die App
   in den Hintergrund geht oder beim nächsten Öffnen. Eine Lernrunde wird also nie unterbrochen.
   „Immediate“ gilt nur für kritische Versionen: In der Play Console beim Release die **In-App-Update-Priorität 4 oder 5** setzen
   (über die Play Developer API, Feld `inAppUpdatePriority`). Ein abgebrochenes Sofort-Update wird beim nächsten Start fortgesetzt.
2. **Inhalte ohne Store-Update:** Die App lädt höchstens alle 20 h neue oder verbesserte Wissenspakete. Das sind **nur JSON-Daten**
   (Fragen, Texte, Emoji-Merkbilder) – kein dex/so/jar und kein Code.
   - **Quelle:** die freigegebene WebApp-Fassung auf GitHub, Branch `wissensgarten-live`, Pfad `wissensgarten/webapp/packs/`
     (`versions.json` + Paketdateien), abgerufen über `raw.githubusercontent.com`. Was der Leitstand live schaltet, bekommen also auch die Apps.
     Solange `wissensgarten-live` nicht existiert, wird nichts nachgeladen.
   - **Prüfung:** Dateiname, Paket-id und Version müssen passen, Größe höchstens 1 MB. Geladen wird nur, was neuer ist; eine
     ältere Fassung verdrängt nie ein mitgeliefertes Paket. Der Fortschritt in den Beeten bleibt erhalten.
   - **Abschaltbar:** Einstellungen (hinter dem Eltern-Tor) → „Neue Wissenspakete“.
3. **Datenschutz, Abschnitt „Updates“ – bitte in 10c übernehmen:**

   > App-Updates: In der Play-Store-Version kommen neue Versionen über Google Play – die App lädt sie im Hintergrund und spielt sie ein,
   > ohne dass der Store geöffnet werden muss. In der GitHub-Version fragt die App bei GitHub nach, ob es eine neue Version gibt.
   > Neue Wissenspakete: Höchstens einmal am Tag schaut die App bei GitHub (raw.githubusercontent.com, betrieben von GitHub Inc.) nach neuen oder
   > verbesserten Wissenspaketen und lädt sie herunter. Das sind nur Inhalte (Fragen, Texte, Merkbilder) – kein Programmcode.
   > Dabei wird nichts über euch oder euer Kind gesendet; GitHub sieht nur, wie bei jedem Webseitenaufruf, die IP-Adresse und den Zeitpunkt.
   > Das Nachladen der Pakete und (in der GitHub-Version) die Update-Suche lassen sich in den Einstellungen abschalten.

   In App und WebApp ist der Text schon so drin, ebenso in der Vorlage `wissensgarten/web/datenschutz/index.html`.
   **Data Safety (Play):** keine Änderung nötig. Es werden keine Nutzerdaten erhoben; die IP-Adresse beim Abruf ist ein normaler Netzwerkzugriff.
4. **Qualität:** 379 Tests grün (davon 4 neue zum Nachladen), Lint ohne Fehler. Die WebApp 0.20.0 ist exportiert und lädt fehlerfrei.
   Die WebApp selbst lädt nichts nach, sie bringt ihre Pakete mit.

## 16.11.2026 – Play Store: Wissensgarten ist store-fertig (Version 0.19.0)

1. **Signiertes AAB:** `Wissensgarten-0.19.0-PlayStore.aab` hängt am GitHub-Release 0.19.0, signiert mit dem Upload-Schlüssel
   (GitHub-Secrets; Original nur in Bitwarden). Play-Variante: kein eigener Updater, Updates über Play; Kamera-Safari aus (Familien-Richtlinie).
2. **Store-Eintrag (Deutsch):** `wissensgarten/store/` – Titel, Kurz- und Langbeschreibung (`texte/`), 8 Handy- und 8 Tablet-Screenshots,
   Titelbild 1024×500, Icon 512.
3. **Datensicherheit und Altersfreigabe:** fertige Antworten in `wissensgarten/store/PLAY-STORE.md` (Abschnitte 4 und 5) –
   keine Werbung, kein Tracking, Zielgruppe Kinder + Eltern (Familienprogramm), Eltern-Tor für Einstellungen/Teilen/Sync, erwartete Einstufung USK 0.
4. **Datenschutz-URL:** `https://www.mathenachhilfe-online.com/datenschutz/` (so schon in App und WebApp verlinkt). Vorschlag: keine
   eigene Seite, sondern ein Abschnitt „Wissensgarten (App und WebApp)“ in der Haupt-Datenschutzerklärung; Vorlage: `wissensgarten/web/datenschutz/index.html`.
5. **Ablauf:** zuerst Track „Interner Test“. Marcel hat ein Organisationskonto beantragt – dann gilt die 12-Tester-/14-Tage-Regel nicht;
   falls die Play Console sie doch verlangt, steht der geschlossene Test als Alternative in der Klick-Liste.
   **Klick-Liste für Marcel:** `wissensgarten/store/PLAY-STORE.md`, Abschnitt „Schritt für Schritt“.

## 0.19.0 vom 16.11.2026 – erste WebApp-Version (Test)

**Stand:** Android-App und WebApp entstehen jetzt aus derselben Codebasis (Compose Multiplatform, Web: Kotlin/Wasm) und tragen
dieselbe Versionsnummer. Die fertige WebApp liegt in `wissensgarten/webapp/` (index.html oben), Größe ca. 23 MB, nur erlaubte
Dateitypen (html, js, json, webmanifest, png, ttf, wasm). Der Branch `wissensgarten-test` wird vom Workflow
„Wissensgarten WebApp → Test“ automatisch vorgespult, nachdem Vollständigkeit, Dateitypen, Größe und Gleichstand
(Version + Wissenspakete identisch zur Android-App) geprüft sind. `wissensgarten-live` fasse ich nicht an.

**Bitte auf https://wissensgarten-test.mathenachhilfe-online.com prüfen:**
1. Start: Brummi-Ladebild, dann „Wissensgarten – Los geht's“ (Handy und PC). Version 0.19.0 steht im Ladebild und unter Mehr → Über.
2. Einrichtung: Name eingeben, Klasse wählen, Beete anlegen (47 Pakete), Anleitung überspringen → Garten.
3. Samen säen / Gießen: Lernrunde mit Spielen, Emojis/Merkbilder werden angezeigt, Ton nach dem ersten Antippen.
4. Seite neu laden: Garten und Tautropfen sind noch da (Speicher nur im Browser, kein Server, kein Konto).
5. Installieren („Zum Startbildschirm hinzufügen“) und offline öffnen.
6. Links „Impressum“ und „Datenschutz“ (Startseite der Einrichtung und Mehr → Über) führen auf mathenachhilfe-online.com.
7. Updates kommen automatisch ohne Knopf: beim Öffnen sofort, sonst beim nächsten Verlassen der Seite.

**Bewusst nur in der Android-App:** Widget, tägliche Erinnerung, Geräte-Sync über Google Drive, Wissens-Safari (Kamera),
QR-Scanner (Klassen-Code kann im Web eingefügt werden), Postkarte und Urkunde. Bastelbogen öffnet im Web eine Druckseite.

**Für die Website (Leitstand):** Datenschutz-Seite um die WebApp ergänzen: Lernstand liegt im localStorage des Browsers,
keine Cookies, keine Analyse, keine Server-Übertragung; Vorlesen/Spracherkennung nutzen die Funktionen des Browsers
(bei Chrome kann die Spracherkennung über Google-Server laufen – nur wenn das Kind das Mikrofon im Radio/Sprech-Spiel nutzt).
