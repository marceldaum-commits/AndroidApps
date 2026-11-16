# Meldungen an den Leitstand – Kurvenflug WebApp

## Konzept Lehrerzugang für KurvenFlug (Auftrag Marcel vom 16.11. – nur Konzept, noch nicht gebaut)

**Heute schon da (ohne Konto, ohne Server):** Klassen-Challenge – die Lehrkraft stellt Level zusammen, die App gibt
einen Code; Schüler spielen und schicken einen Ergebnis-Code zurück, die App der Lehrkraft sammelt ihn ein
(Ergebnis-Tafel). Das bleibt als Offline-Weg erhalten, der Lehrerzugang macht es bequem und automatisch.

**1. Was die Lehrkraft sieht und steuert**

| Bereich | Inhalt |
|---|---|
| Auftrag | Kapitel (z. B. „Geraden“) **oder** eigene Level-Auswahl (wie Klassen-Challenge); Stufe Normal/Schwer/Profi/Extrem (Extrem-Prüfung optional); Anzahl Level; Frist; „Rechen-Stern Pflicht“ (Rechenweg-Frage muss stimmen) |
| Ergebnisse je Schüler | erledigte Level des Auftrags, Sterne, Fehlversuche, Rechen-Stern richtig/falsch, Zielhilfe benutzt ja/nein, Zeit pro Level, zuletzt aktiv |
| Ergebnisse je Thema (Klasse) | Anteil gelöst, Ø Sterne, Ø Fehlversuche, häufigste falsche Rechen-Antwort (zeigt typischen Denkfehler, z. B. „25 : 100 statt 1 + 25 : 100“), schwierigstes Level |
| Freischalten | Kapitel bzw. Stufen für die Klasse öffnen/sperren (z. B. Oberstufe nur ab Kl. 11, Extrem erst nach Kapitelende) |
| Prüfungsmodus | für einen Auftrag: keine Zielhilfe, kein Rechenweg, kein Lösungs-Vorführen durch Volt, ein Versuch je Level, Zeitfenster |
| Rangliste | Sterne/Sturm-Rekord der Klasse (Spitznamen), abschaltbar |

**2. Daten an den Klassen-Dienst (so knapp wie möglich)**

| Wann | Felder |
|---|---|
| Beitritt | Klassen-Code, Spitzname, zufällige Geräte-Kennung (UUID, nur für diese Klasse), App-Version |
| Nach jedem Level eines Auftrags (gesammelt, max. 1× pro Minute; offline in Warteschlange) | Auftrag-ID, Level-ID, Sterne 0–3, Fehlversuche, Rechen-Stern ja/nein, gewählte Rechen-Antwort (Index 0–3), Zielhilfe ja/nein, Sekunden, Zeitpunkt |
| Rangliste (wenn an) | Summe Sterne, Sturm-Rekord – höchstens 1× täglich |
| Abruf (beim Start, dann alle 7 Tage bzw. bei „Aktualisieren“) | neue Aufträge, Freischaltungen, Prüfungsmodus, Lizenzstatus der Klasse (aktiv bis …) |

Keine Klarnamen, keine E-Mail, kein Freitext, keine Standortdaten, keine Werbe-Kennung. Spielstand außerhalb von
Aufträgen bleibt auf dem Gerät. Austreten löscht die Klassendaten des Geräts beim Dienst.

**3. Beitritt und was der Schüler sieht**
- Beitritt per **Klassen-Code** (6 Zeichen), **QR-Code** (Kamera der Handy-App bzw. Link) oder **Link**
  `https://kurvenflug.mathenachhilfe-online.com/?klasse=<Code>`. Spitzname aus Vorschlagsliste (Tier + Farbe, z. B. „Blauer Fuchs“) – kein Freitext.
- Neuer Bereich „🏫 Meine Klasse“ (pro Spieler-Profil): Klassenname, offene Aufträge mit Frist und Fortschritt, Rangliste
  (falls an), „Pro durch Schullizenz bis 31.07.“. Andere Schüler sieht er nur in der Rangliste (Spitzname + Sterne).
- Eltern sehen in den Einstellungen: welche Klasse, welche Daten geschickt werden, „Klasse verlassen“.
- Play-Fassung: braucht dann eine Internet-Berechtigung (nur für den Klassen-Dienst) – Formulare (Datensicherheit:
  „App-Aktivitäten“, an Lehrkraft übermittelt) und Datenschutztext anpassen. Ohne Klasse bleibt die App offline.

**4. Frei oder Schullizenz**

| Frei | Schullizenz (Lehrkraft) |
|---|---|
| Klasse anlegen, Beitritt, Rangliste | Aufträge mit Frist und Auswertung je Schüler/Thema |
| Klassen-Challenge per Code (wie heute, offline) | Freischalten/Sperren von Kapiteln und Stufen, Prüfungsmodus |
| | Pro für alle Schüler der Klasse (solange in der Klasse und Lizenz gültig), Export (CSV) |

Vorschlag Ablauf in der App: Lizenzstatus kommt beim Beitritt vom Dienst, gilt offline bis zum Ablaufdatum, Prüfung
alle 7 Tage; wer die Klasse verlässt, behält nur Einzelkauf/eigenen Code.

## 0.23.0 – Kurvenflug Pro gebaut (Freigabe Marcel: „ja, bau es so“; nicht USt.-befreit)

- **Version:** 0.23.0 · **Commit:** eae218c (Zweig claude/gifted-shannon-657gpr)
- **Noch sperrt nichts:** Pro-Sperren greifen erst, wenn ein Kaufweg angeboten wird – Play: Produkt `kurvenflug_pro`
  in der Play Console aktiv; Web/GitHub: `kurvenflug/pro/pro.properties` mit `publicKey` (Lizenz-Dienst) und
  `checkoutUrl` (Woo-Kasse). Bis dahin zeigt die Pro-Seite „Pro kommt bald – bis dahin ist alles frei“.
- **Gebaut:** Pro-Regeln mit Bestandsschutz (Pro.kt), Pro-Seite mit Vorteilen, Preis (Web: „7,99 € einmalig, inkl. 19 % MwSt.“,
  Play: Preis von Google), **Frage für Erwachsene** (Geburtsjahr, ≥ 18) vor jedem Kauf, Code-Eingabe, Freischalt-Link
  `…/?pro=KF…` (Web) bzw. „Teilen → Kurvenflug“ / Einfügen (GitHub-APK), Play Billing 8.3 (Kauf, Bestätigung,
  Wiederherstellen), Sperren an Kapitelkarten, Extrem-Prüfungen, Sturm (ohne Pro bis „Schwer“), Andromeda, Spieler-Profilen,
  Werkstatt; Einstellungen → „⭐ Kurvenflug Pro“; Eltern-Text angepasst.
- **Play-Fassung bleibt ohne Internet-Berechtigung** (die Billing-Bibliothek bringt INTERNET mit, ist entfernt – der Kauf
  läuft über die Play-Store-App). **Pflicht vor Freigabe:** Testkauf im internen Test (Anleitung `store/PLAY_CONSOLE_ANLEITUNG.md` §6a).
- **Für den Lizenz-Dienst (Leitstand):** Code-Format und Prüfung in `core/model/.../License.kt`, Erzeuger
  `kurvenflug/pro/pro_code.py` (`keygen`, `pro <Nr>`, `school <Nr> <bis-Datum>`; braucht `cryptography`). Ed25519, der Code
  enthält App-Nummer (Kurvenflug = 1) – **ein Schlüsselpaar kann für alle Apps dienen**. Geheimer Schlüssel nur in
  Bitwarden/beim Dienst. Codes sind 117 Zeichen lang (Signatur) → in der E-Mail als Link + zum Kopieren, nicht zum Abtippen.
  Freischalt-Link angenommen als `https://kurvenflug.mathenachhilfe-online.com/?pro=<Code>` (Live-Adresse bitte bestätigen).
- **Tests:** Ed25519/SHA-512 gegen die Java-Krypto (Zufallsschlüssel), feste Code-Beispiele auf JVM **und** im Browser (Wasm),
  gefälscht/fremde App/abgelaufene Schullizenz werden abgelehnt, Code mitten in E-Mail/Link wird gefunden; Pro-Regeln
  (ohne Kaufweg alles frei, 24 freie Kapitel, 35 Pro-Level, Bestandsschutz, Sturm-Grenze).
- **Bitte prüfen (kurvenflug-test):** Einstellungen → „⭐ Kurvenflug Pro“ (zeigt „kommt bald“); sonst alles wie bisher frei.

## Konzept Pro für KurvenFlug (Auftrag Marcel vom 16.11. – nur Konzept, noch nicht gebaut)

### 1. Was bleibt frei, was wird Pro?
Leitgedanke: Kinder sollen **den ganzen Schulstoff bis Klasse 10 kostenlos** üben können – sonst verlieren wir die
Empfehlung von Schulen und Eltern. Pro ist für **Oberstufe, Könner und Fans**.

| Frei (wie heute) | Pro |
|---|---|
| Alle Kapitel Klasse 5–10 (24 Kapitel, ca. 140 Level) mit Rechenweg, Rechen-Stern, Live-Rechnung | **Oberstufe**: Ableitung, Extrempunkte, Integral, Vektoren, e-Funktion, Binomialverteilung (6 Kapitel, 35 Level) |
| Tagesmission, Duell, Formel-Detektiv, Lernstand, Wiederholen | **🔥 Extrem-Prüfungen** aller 30 Kapitel |
| Asteroidensturm Leicht bis Schwer (Welle 1–12) | **Sturm Profi und Extrem** (ab Welle 13), eigene Rekordliste |
| Bosse, Aurora-Geschichte, Andromeda Planet 1–6 | **Andromeda ab Planet 7** (Profi/Extrem) und das Schiff Nova |
| Werkstatt: Grund-Aussehen | Werkstatt: zusätzliche Sonden, Spuren, Volt-Kostüme (rein kosmetisch) |
| Bis 2 Spieler-Profile | Bis 6 Profile (Familie, Geschwister) |
| Klassen-Challenge mit freien Leveln – **bleibt für Lehrkräfte kostenlos** (bringt die App in Klassen) | Klassen-Challenge mit Oberstufen-/Extrem-Leveln; Schullizenz (s. u.) |

**Bestandsschutz:** Wer ein Pro-Level schon geschafft hat, behält Sterne und kann es weiter spielen. Kein Fortschritt geht verloren.

**Preis:** **Einmalkauf statt Abo** – für Eltern ehrlich und ohne Abo-Falle, passend zu einer Kinder-App.
- **Kurvenflug Pro: 7,99 € einmalig, brutto inkl. 19 % USt.** (Familie, alle Profile auf dem Gerät/Browser), zum Start 4,99 €.
- **Schullizenz: 29 € brutto pro Lehrkraft, gültig bis 31.07.** (festgelegt von Marcel): Pro für eine Lehrkraft und alle
  Schülerinnen und Schüler ihrer Klassen, im Unterricht und zu Hause, auf beliebig vielen Geräten. Umsetzung: ein Schul-Code
  (Produkt 2, Ablaufdatum 31.07.), den die Lehrkraft an ihre Klassen weitergibt – kein Geräte- oder Schülerlimit in der App.
- Vergleich (Recherche 16.11., Preise schwanken je Land/Aktion):
  - DragonBox Algebra (Kahoot!): Einmalkauf ca. 7–8 €.
  - Photomath Plus: ca. 55–65 € im Jahr bzw. ca. 10 € im Monat.
  - Prodigy Math: 59–119 $ im Jahr.
  - Brilliant Premium: ca. 150 $ im Jahr (teils mehr).

  Mit 7,99 € einmalig liegt Kurvenflug am unteren Ende, nah an DragonBox. Ein Abo wäre bei einem Spiel ohne laufende
  Serverkosten schwer zu begründen.

### 2. Kaufwege
- **Play-Fassung: Google Play Billing (Pflicht)**: ein Einmal-Produkt „pro“ (nicht verbrauchbar), Kauf bestätigen (acknowledge).
  Kauf läuft über die Play-Store-App, die Play-Fassung braucht dafür **keine Internet-Berechtigung**. Google behält
  15 %. Familienrichtlinie: Kauf nur hinter einer **Eltern-Frage** (z. B. „Gib dein Geburtsjahr ein“ / Rechenaufgabe für
  Erwachsene). Keine Lockangebote, keine Kauf-Hinweise mitten im Level, klarer Preis. Family-Link-Freigabe greift automatisch.
  Formulare anpassen: IARC „Digitale Käufe: Ja“, Store-Text „In-App-Käufe“.
- **Web- und GitHub-Fassung: über unsere Website** (wie Mathe-Trainer Pro): Knopf „Pro freischalten“ (hinter
  Eltern-Frage) → öffnet die Woo-Kasse auf mathenachhilfe-online.com → nach Zahlung Code auf der Danke-Seite und per
  E-Mail an Kunde und Marcel, Bestellung bleibt in WooCommerce als Nachweis → Code in der App eingeben (oder Link
  `…/kurvenflug/?pro=CODE` öffnet die App und schaltet frei). **Kein Konto**, Code gilt für Web und GitHub-APK.
  An der Kasse: Häkchen zum sofortigen Beginn der Leistung (Widerrufsrecht bei digitalen Inhalten).
- **Was bei Marcel ankommt (je Kauf, gerundet; Zahlungsgebühren Web geschätzt: Stripe 1,5 % + 0,25 €, PayPal 2,99 % + 0,39 €):**

  | Produkt | Brutto (inkl. 19 % USt.) | Netto | USt. | Web: Stripe | Web: PayPal | Google Play (netto − 15 %) |
  |---|---|---|---|---|---|---|
  | Kurvenflug Pro | 7,99 € | 6,71 € | 1,28 € | ca. 6,34 € | ca. 6,09 € | 5,71 € |
  | Pro Startpreis | 4,99 € | 4,19 € | 0,80 € | ca. 3,87 € | ca. 3,65 € | 3,56 € |
  | Schullizenz (Jahr) | 29,00 € | 24,37 € | 4,63 € | ca. 23,68 € | ca. 23,11 € | 20,71 € (falls über Play) |

  Bei Google Play: Google führt die USt. ab, vom Netto gehen 15 % Gebühr ab (Abo- und Einmalkäufe für kleine
  Entwickler). Web: die eigene Zahlungsgebühr des gewählten Anbieters (Werte oben nur Richtwerte, bitte mit eurem
  Woo-Zahlungsanbieter abgleichen). Anzeige in der App: Web „7,99 € einmalig · inkl. 19 % MwSt.“, Play zeigt Googles Endpreis.
- **Umsatzsteuer (Marcel ist nicht USt.-befreit)**: Preise sind Bruttopreise inkl. 19 % USt.; Woo-Rechnung mit
  ausgewiesener USt., Woo-Steuerklasse Standard 19 %. Digitale Leistungen an Verbraucher in anderen EU-Ländern: Steuersatz des Kundenlandes (OSS) –
  in WooCommerce einstellen bzw. mit dem Steuerberater klären. Bei Google Play führt Google die USt. selbst ab.
- Play-Käufer, die auch im Browser spielen wollen: Code per E-Mail-Anfrage mit Play-Bestellnummer (später automatisch,
  s. Punkt 3).

### 3. Was der gemeinsame Lizenz-Dienst können muss
- **Signierte Codes, offline prüfbar**: Der Dienst signiert `{App, Produkt, Bestell-Kennung (gehasht), Datum}` mit einem
  privaten Ed25519-Schlüssel; die App enthält nur den öffentlichen Schlüssel und prüft ohne Internet. Privater Schlüssel
  nur auf dem Server / in Bitwarden, nie im Repo. Code-Format kurz zum Abtippen, z. B. `KF-PRO-7Q4M-2XH9-…` (Base32 + Prüfsumme).
- **Woo-Anbindung**: Webhook „Bestellung abgeschlossen“ → Code erzeugen → in der Bestellung speichern → E-Mail an
  Kunde + Marcel (Rechnung aus WooCommerce). Ein Produkt je App plus optional ein **Bundle** über alle Apps.
- **Sperrliste** für Erstattungen/Missbrauch: kleine JSON-Datei (`/lizenz/sperrliste.json`), Web und GitHub-APK lesen
  sie gelegentlich; die Play-Fassung braucht sie nicht (Google erstattet selbst).
- **Schullizenz**: ein einziger Code mit Ablaufdatum 31.07. (Produkt 2) für Lehrkraft und alle ihre Klassen, beliebig viele Geräte.
- **Später, optional**: Prüfung von Play-Kaufbelegen über die Google Play Developer API → Web-Code für Play-Käufer
  automatisch.
- Für die Apps: eine gemeinsame kleine Bibliothek „Lizenz“ (Code prüfen, speichern, Play-Billing-Adapter, Eltern-Frage),
  damit alle Apps gleich funktionieren. In Kurvenflug: Schalter `PRO_PLAY` / `PRO_WEB` je Variante wie `SELF_UPDATE`.
- Datenschutz: Die App speichert nur „Pro: ja + Code“. Keine E-Mail, kein Name in der App.

### 4. KI über die Claude-API – sinnvoll?
- **Kosten wären gering**: Claude Haiku 5.5 kostet 0,10 $ je Mio. Eingabe- und 0,50 $ je Mio. Ausgabe-Token. Eine
  Erklärung („Warum hat meine Bahn die Station verfehlt?“, ca. 1.500 Token rein, 300 raus) kostet etwa **0,03 Cent**;
  100 Erklärungen pro Kind und Monat ≈ 3 Cent.
- **Aber jetzt nicht empfohlen**:
  1. Die Play-Fassung hat bewusst kein Internet. Das ist ein Pluspunkt bei Eltern und in den Datenschutz-Formularen.
  2. Kinder-Daten: Wir bräuchten einen eigenen Server als Zwischenstation (kein Schlüssel in der App), Mengenbegrenzung,
     Filter, Datenschutz-Folgenabschätzung und eine Prüfung der Anbieter-Regeln für Minderjährige.
  3. Kurvenflug erklärt schon heute ohne KI: Rechenweg, Rechen-Frage, Live-Rechnung und Volts Tipps sind für jede Aufgabe fest hinterlegt.
- **Wenn später gewünscht**: „Frag Volt“ nur in Web/GitHub-Pro, freiwillig, ohne Freitext-Eingabe (Kind tippt auf
  „Erklär's mir anders“, wir senden nur Level-Daten und die falschen Reglerwerte, nie persönliche Daten), über einen
  Server-Proxy mit Tageslimit.

### Nächste Schritte (nach Freigabe durch den Leitstand/Marcel)
1. Gemeinsamer Lizenz-Dienst + Woo-Produkt (Leitstand). 2. Lizenz-Bibliothek + Eltern-Frage in Kurvenflug,
Pro-Sperren mit Bestandsschutz, Play-Billing in der Play-Variante (parallel zur Play-Store-Arbeit). 3. Datenschutz-Text und
Play-Formulare anpassen.

## 0.22.0 – Schwierigkeit steigt nachweislich bis „Extrem“ (Vorgabe Marcel vom 16.11.)

- **Version:** 0.22.0 · **Commit:** a5fec15 (Zweig claude/gifted-shannon-657gpr)
- **Wo es in Kurvenflug Schwierigkeit gibt:** der Level-Generator (Asteroidensturm, Andromeda, Rettungsmissionen,
  Bosse, Gastplaneten, Tagesmission, Duell) und die 30 Kapitel. Werkstatt (nur Aussehen) und Klassen-Challenge
  (Lehrkraft wählt Level) haben keine eigene Schwierigkeit.
- **Generator: 5 Stufen** 🟢 Leicht · 🔵 Normal · 🟠 Schwer · 🔴 **Profi** (neu) · 🔥 **Extrem** (neu).
  - Profi: Zielzonen 60 %, 3 Stationen, Regler doppelt so fein, 4 Asteroiden dicht an der Bahn, Wurf von einer Klippe
    (Winkel, Tempo, Schwerkraft und Höhe zugleich), getarnte Koordinaten mit 3 Rechenschritten in halben Kästchen.
  - Extrem: Zielzonen 40 %, 4 Stationen, 6 Asteroiden 0,2 Kästchen neben der Bahn, **zusätzlicher Parameter**
    (Sinus mit Verschiebung c → 4 Unbekannte, Wachstum mit Verschiebung d), Wurf mit 2 Ringen unterwegs,
    Koordinaten in Viertel-Kästchen mit Wurzel im Funkspruch, Rendezvous mit Viertel-Steigungen.
  - Einsatz: Asteroidensturm alle 4 Wellen eine Stufe höher (ab Welle 17 Extrem, Stufe steht im Titel),
    Andromeda Planet 1–6 Schwer, 7–12 Profi, ab 13 Extrem; Rettungsmission ab Nr. 8 mit Profi-Schluss-Etappe.
    Tagesmission und Duell bleiben unverändert (Duell-Codes funktionieren weiter).
- **Kapitel:** Die Kapitel steigern sich inhaltlich (neue Rechenarten), messbar war die letzte Aufgabe aber oft nicht
  schwerer als die erste. Deshalb neu: **🔥 Extrem-Prüfung in jedem der 30 Kapitel** (öffnet nach allen Leveln,
  freiwillig, zählt nicht zum Kapitel-Abschluss → niemand verliert Fortschritt; eigene Sterne).
- **Nachweis (automatische Tests, schlagen fehl, wenn eine Stufe nicht strikt schwerer ist):**
  `core/model/src/jvmTest/.../DifficultyTest.kt` (je Bahnart und gesamt: Gesamtwert strikt steigend, Lösungsbereich
  kleiner, Zielzone kleiner, Hindernisse/Rechenschritte/Parameter nicht weniger; Extrem: Zielzone ≤ 0,35,
  ≥ 4,5 Hindernisse, Lösungsbereich ≤ 5 %, jedes Level lösbar und nur auf dem Raster) und `ChapterExtremeTest.kt`
  (Prüfung schwerer als jedes Level ihres Kapitels).
  Kennzahlen: Parameter = Unbekannte; Zielzone = mittlerer Radius; Rechenschritte = Unbekannte + Stationen +
  Rechenzeichen im Funkspruch + Klippe; Lösungsbereich = Anteil der Reglerstellungen, die noch treffen, wenn man
  einen Regler allein bewegt (Löser prüft jede Stellung); Gesamtwert = Parameter + Rechenschritte + ½·Hindernisse +
  1/Zielzone + 20·(1 − Lösungsbereich). Mittel über alle 7 Bahnarten × 12 Startwerte:

| Stufe | Parameter | Zielzone (Radius, Kästchen) | Hindernisse | Rechenschritte | Lösungsbereich | Gesamtwert |
|---|---|---|---|---|---|---|
| 🟢 Leicht | 2,3 | 0,86 | 0,0 | 4,1 | 11,0 % | 25,5 |
| 🔵 Normal | 2,3 | 0,66 | 1,0 | 4,4 | 9,9 % | 26,8 |
| 🟠 Schwer | 2,3 | 0,56 | 2,4 | 4,7 | 9,1 % | 28,3 |
| 🔴 Profi | 2,3 | 0,39 | 3,4 | 5,7 | 4,6 % | 31,5 |
| 🔥 Extrem | 2,6 | 0,26 | 4,6 | 6,7 | 3,8 % | 34,9 |

  Extrem-Prüfung je Kapitel (Gesamtwert gegenüber dem schwersten Level des Kapitels):

| Kapitel | schwerstes Level (Gesamtwert) | Extrem-Prüfung | Zielzone | Hindernisse | Lösungsbereich Vorlage → Extrem |
|---|---|---|---|---|---|
| 📍 Koordinaten | 27,4 | **31,3** | 0,70 → 0,35 | 4 | 5,3 % → 2,7 % |
| 🍕 Brüche & Kommazahlen | 40,6 | **59,3** | 0,06 → 0,03 | 4 | 0,6 % → 0,3 % |
| 📐 Winkel | 26,0 | **31,5** | 0,30 → 0,15 | 4 | 1,7 % → 0,8 % |
| 🗺️ Maßstab & Karten | 25,9 | **30,5** | 0,50 → 0,25 | 4 | 5,3 % → 2,7 % |
| ⚖️ Zuordnungen | 24,8 | **28,6** | 0,70 → 0,35 | 5 | 7,3 % → 3,7 % |
| ⏱️ Weg & Zeit | 26,5 | **30,4** | 0,70 → 0,35 | 4 | 4,6 % → 2,4 % |
| 🔦 Laser-Optik | 25,4 | **28,1** | 0,60 → 0,36 | 5 | 1,1 % → 0,8 % |
| 📈 Geraden | 28,4 | **32,3** | 0,70 → 0,35 | 8 | 5,0 % → 2,6 % |
| 💶 Prozent & Zinseszins | 26,4 | **32,7** | 0,25 → 0,13 | 4 | 2,9 % → 1,4 % |
| 🤝 Rendezvous | 30,3 | **35,4** | 0,35 → 0,18 | 6 | 2,7 % → 1,4 % |
| 📡 Pythagoras | 27,8 | **34,2** | 0,25 → 0,13 | 5 | 3,4 % → 1,7 % |
| 🌙 Parabeln | 29,9 | **33,8** | 0,70 → 0,35 | 5 | 5,0 % → 2,6 % |
| 🎯 Nullstellen | 28,3 | **32,3** | 0,70 → 0,35 | 4 | 5,6 % → 2,9 % |
| 🎢 Energie | 22,0 | **26,1** | 0,80 → 0,40 | 4 | 14,3 % → 7,2 % |
| 💎 Lichtbrechung | 24,7 | **29,3** | 0,50 → 0,25 | 5 | 6,7 % → 1,1 % |
| 🎯 Wurf & Schwerkraft | 25,9 | **29,1** | 0,80 → 0,40 | 5 | 11,8 % → 4,4 % |
| 🚗 Beschleunigte Bewegung | 27,4 | **32,5** | 0,40 → 0,20 | 4 | 5,3 % → 2,7 % |
| 📐 Trigonometrie | 26,8 | **33,2** | 0,25 → 0,13 | 5 | 3,4 % → 1,7 % |
| 🌊 Wellen & Wachstum | 27,4 | **32,6** | 0,70 → 0,35 | 6 | 15,1 % → 6,3 % |
| 📊 Potenzfunktionen | 24,3 | **29,2** | 0,40 → 0,20 | 4 | 7,7 % → 4,0 % |
| 🏃 Waagerechter Wurf | 25,8 | **30,6** | 0,40 → 0,20 | 4 | 3,4 % → 1,8 % |
| 🌊 Verschobene Wellen | 28,3 | **34,1** | 0,30 → 0,15 | 4 | 5,3 % → 2,7 % |
| 🔢 Logarithmus | 26,3 | **30,4** | 0,35 → 0,33 | 4 | 5,4 % → 2,8 % |
| 🪐 Schwerkraft-Schleuder | 27,0 | **29,7** | 0,80 → 0,48 | 6 | 3,9 % → 2,1 % |
| 🏂 Ableitung & Tangente | 24,6 | **28,5** | 0,60 → 0,30 | 5 | 3,0 % → 1,5 % |
| ⛰️ Extrempunkte | 25,3 | **30,4** | 0,35 → 0,18 | 4 | 2,7 % → 1,4 % |
| 🧭 Vektoren | 27,7 | **32,2** | 0,45 → 0,23 | 4 | 2,7 % → 1,4 % |
| ⛽ Integral | 56,2 | **91,6** | 0,03 → 0,02 | 4 | 0,6 % → 0,3 % |
| 📈 e-Funktion | 28,5 | **34,3** | 0,30 → 0,15 | 4 | 4,0 % → 2,0 % |
| 🎲 Binomialverteilung | 32,9 | **45,5** | 0,10 → 0,05 | 4 | 5,3 % → 2,7 % |

- **Geprüft:** alle Unit-/Oberflächentests (Gleichstand JVM = Wasm), Lint beider Varianten, APK + AAB, WebApp bei 320 px
  ohne Konsolenfehler, Bildschirmfotos 91–93 angesehen.
- **Bitte prüfen:** Galaxie → ein fertiges Kapitel → „🔥 Extrem-Prüfung“; Spiele → Asteroidensturm (Stufe im Titel).

## 0.21.1 – Schneller Start, Ladebalken, kein weißer Bildschirm (Auftrag Marcel vom 16.11.)

- **Version:** 0.21.1 · **Commit:** d6d0962 (Zweig claude/gifted-shannon-657gpr)
- **Hauptursache gefunden:** Beim ersten Start lud der Service Worker (Offline-Speicher) **alle Dateien ein zweites Mal**,
  parallel zur App – auf dem Handy also ~9,9 MB statt 5,2 MB über dieselbe Leitung. Jetzt registriert er sich erst,
  wenn die App läuft, und fragt nur „geändert?“ (der Testserver antwortet korrekt mit 304, geprüft).
- **Schneller:** Wasm-Dateien und Mathe-Schrift werden sofort beim Seitenaufruf parallel angefragt (nicht erst nach
  kurvenflug.js) und als Strom an die App gereicht – das Übersetzen des Wasm läuft weiter schon während des Ladens.
- **Echter Ladebalken:** „Lade … 54 %“ nach geladenen Bytes (Gesamtgrößen setzt der Build ein), danach
  „Starte die Triebwerke …“ und sanftes Überblenden in die App. Ladebild erscheint sofort (inline, kein weißer Moment).
- **Android:** Android-12+-Startbildschirm in App-Farbe (`values-v31`, `windowSplashScreenBackground` #0B1026),
  darunter war das Fenster schon dunkel – kein weißes Aufblitzen.
- **Messung** (Chromium, Drosselung **am Server**, damit auch Service-Worker-Downloads zählen; 412 px; gzip wie Testserver):

  | Netz | Start | vorher (0.21.0) bedienbar | nachher (0.21.1) bedienbar | Download vorher → nachher | erster Inhalt |
  |---|---|---|---|---|---|
  | Fast 4G (9 Mbit/s, 60 ms) | 1. Start | 10,5 s | **6,0 s** | 9,9 MB → **5,2 MB** | ~0,1 s (beide) |
  | Fast 4G | 2. Start | 0,85 s | 0,84 s | 0 MB | ~0,06 s |
  | Slow 4G (1,6 Mbit/s, 150 ms) | 1. Start | 52,5 s | **28,2 s** | 9,9 MB → **5,2 MB** | ~0,2 s (beide) |
  | Slow 4G | 2. Start | 0,9 s | 0,84 s | 0 MB | ~0,07 s |

  Größen (gzip): Skiko-Wasm 3,26 MB, App-Wasm 1,55 MB, Emoji-Schrift 0,52 MB, kurvenflug.js 0,11 MB, Rest < 20 kB.
- **Geprüft:** Auto-Update (0.21.1 → Testversion 0.99.0 wird nach wenigen Sekunden übernommen), Offline-Start ohne
  Netz, 320 px ohne Konsolenfehler, alle Unit-/Oberflächentests, Lint beider Varianten, APK + AAB gebaut.
- **Bitte prüfen:** Ladebalken beim ersten Öffnen (am besten im privaten Fenster), zweiter Start < 1 s, Android-Start dunkel.
- **Zu Marcels Frage „bündeln und verkleinern“:** Ist bereits so (webpack-Produktionsbuild + Binaryen-optimiertes Wasm).
  Mehr bringt nur noch **Brotli am Server** (5,2 → ca. 4,0 MB, ~25 % weniger) – das ist Server-Konfiguration,
  die Dateien selbst bleiben gleich. Der große Brocken ist die Grafik-Engine (Skia), die jede Compose-Web-App braucht.

## 0.21.0 – Play Store vorbereitet + Binomialverteilung (Auftrag vom 16.11., vorgezogen)

- **Version:** 0.21.0 · **Commit:** 3d4f3e6 (Zweig claude/gifted-shannon-657gpr)
- **Play Store (alles erledigt, was ohne Marcels Konto geht):**
  - Zwei Varianten aus einem Code: `github` (APK, eigener Updater) und `play` (AAB). Die Play-Variante hat **kein Internet,
    keine Installations-Berechtigung, keinen eigenen Updater**; Updates über die **Play In-App Updates API** (flexibel,
    Installation im Hintergrund; „sofort“ bei Priorität ≥ 4 oder > 30 Tage alt). Kein Nachladen von Code.
  - **Signiertes AAB** baut der Release-Workflow (`Kurvenflug-<V>-play.aab` am GitHub-Release), Signatur wird geprüft.
  - **App-Signaturschlüssel:** neuer Workflow „Kurvenflug: Signaturschlüssel für Google Play“ exportiert den bestehenden
    Schlüssel mit Googles PEPK verschlüsselt – Marcel muss nichts installieren. Damit Wechsel APK ↔ Play ohne Deinstallieren.
  - **Store-Eintrag:** `kurvenflug/store/` – Titel, Kurz-/Langbeschreibung, Release-Text, 8 Screenshots 1080 × 2160,
    Feature-Grafik 1024 × 500, Icon 512.
  - **Formulare:** Datensicherheit (keine Datenerhebung), IARC-Einstufung (alles Nein → USK 0/PEGI 3), Zielgruppe 9–17
    mit Familienprogramm, Werbung Nein, Datenschutz-URL + Textvorschlag – alles in `store/PLAY_CONSOLE_ANLEITUNG.md`.
  - **Lizenzen:** geprüft, alles Apache 2.0 bzw. Play-Core-Bedingungen (nur Play-Build); Hinweis in der App
    (Einstellungen → Open-Source-Lizenzen), Übersicht `store/LIZENZEN.md`.
  - **Klick-Liste für Marcel:** `store/PLAY_CONSOLE_ANLEITUNG.md` (App anlegen → Schlüssel hochladen → Store-Eintrag →
    App-Inhalte → interner Test → geschlossener Test 12 Tester / 14 Tage → Produktion).
- **Neu: Binomialverteilung** (Kapitel 30, Kl. 10–13): Histogramm mit Dichte P(X = k) und kumuliert P(X ≤ k), 6 Level,
  jede mit Rechenweg und Rechen-Frage. Jetzt **175 Level in 30 Kapiteln**.
- **Prüfung:** Gleichstand JVM = Wasm (neuer Fingerabdruck), alle Level lösbar, Nachbarwerte treffen nicht, Regression,
  Lint beider Varianten; Browser bei 320 px und PC ohne Konsolenfehler (siehe unten).
- **Ladezeit (Bitte an den Server):** Die WebApp ist bereits gebündelt und verkleinert (webpack-Produktionsbuild,
  Wasm mit Binaryen optimiert). Den größten Gewinn bringt jetzt **Komprimierung durch den Webserver** für `.wasm`, `.js`,
  `.ttf`, `.json`: gemessen 14,6 MB → **3,8 MB mit Brotli** (gzip: 4,9 MB). Bitte Brotli/gzip für diese Typen aktivieren
  (vorkomprimierte .br/.gz-Dateien sind laut Dateityp-Liste nicht erlaubt). Ab dem zweiten Besuch lädt ohnehin alles aus dem Service Worker.
- **Standard-Branch:** Bitwarden-Hinweis in `.github/workflows/kurvenflug-release.yml` auch auf `ccr-0e3accf3-cdxlc8` (fa5687c).

---

## 0.20.0 – großer Inhaltsausbau (Auftrag vom 16.11.)

- **Version:** 0.20.0 · **Commit:** ae5831d (Zweig claude/gifted-shannon-657gpr)
- **Neu: 38 Level in 7 Kapiteln** (jetzt 169 Level in 29 Kapiteln, Klasse 5–13):
  Maßstab & Karten (Kl. 5–6), Prozent & Zinseszins (Kl. 7–8), Beschleunigte Bewegung (Physik 9–10),
  Potenzfunktionen inkl. Wurzel (Kl. 10), Verschobene Wellen a·sin(b(x−c))+d (Kl. 10–11),
  Waagerechter Wurf auf Erde/Mond/Mars (Physik 10), e-Funktion & ln (Kl. 11–12).
- **Erklärung je Aufgabe:** Tipp, eigener Rechenweg (Schritte zum Antippen), eigene Rechen-Frage mit typischen
  Fehlern als Ablenkern, Live-Rechnung beim Schieben, Lösungsweg nach dem Treffer, Kapitel-Einführung durch Volt.
- **Prüfung (automatisch):**
  - Gleichstand JVM = Wasm: Fingerabdruck aller 169 Level, Bahnen, Tagesmissionen und Codes identisch.
  - Alle Level lösbar, Startwerte lösen nicht, **ein Reglerschritt neben der Lösung trifft nicht** (Raten reicht nicht).
  - Jeder Rechenweg wird gegen die Lösung geprüft; jede Rechen-Frage hat drei verschiedene Antworten.
  - Regression: 213 Tests grün (Logik, Datenbank-Migration, 102 Oberflächen-/Bildschirmfoto-Tests inkl. Klicktest), Lint ohne Fehler.
  - Browser (Chromium) bei **320 × 640** und **1280 × 800**: alle 11 neuen bzw. Oberstufen-Kapitel geöffnet – keine Konsolenfehler.
- **Dabei behoben:** auf sehr schmalen Handys (320 px) wurde der Start-Knopf zerquetscht (jetzt kompakte Knöpfe);
  im Browser fehlte das Zeichen „ˣ“ und Pfeile erschienen als bunte Emoji (Schriften korrigiert).
- **Prüfen auf der Testadresse:** Klasse 8 wählen → Galaxie → Liste → „Prozent & Zinseszins“ → Level 1: Faktor b = 1,25
  → Treffer → Rechen-Frage „1 + 25 : 100“. Physik: „Waagerechter Wurf“ → Mondklippe (v = 3 m/s).

---

## Stand 16.11.2026 (Antwort auf die Anfrage von heute)

**Hinweis:** Diese Datei liegt auf dem Entwicklungszweig `claude/gifted-shannon-657gpr` und auf `kurvenflug-test`
(nicht auf `main`).

**Web = Android: erledigt.** Eine gemeinsame Codebasis (Kotlin Multiplatform + Compose Multiplatform, Kotlin/Wasm).
Jede Version erscheint gleichzeitig als APK (GitHub-Release) und WebApp (`kurvenflug/webapp/` auf `kurvenflug-test`,
plus ZIP am Release) mit derselben Nummer. Ein Gleichstands-Test vergleicht bei jedem Build einen Fingerabdruck aller
Level, Bahnen, Tagesmissionen und Codes auf JVM und Wasm. Aktuell: **0.19.0** (Oberstufe Kl. 11–13).
WebApp: installierbar, offline, aktualisiert sich selbst (ohne Knopf), Impressum/Datenschutz, keine externen Aufrufe.

**Play Store: noch nicht begonnen** – Marcel hat den Start auf **17.11., 15:00** gelegt. Geplant: Play-Variante
(ohne eigenen APK-Updater und ohne Installations-Berechtigung, mit Play In-App Updates), signiertes AAB über den
Workflow, Store-Eintrag (Texte, Screenshots, Feature-Grafik, Icon), Datensicherheit/Altersfreigabe (Familien-Richtlinien),
Lizenzprüfung, Klickliste für die Play Console. Damit Wechsel APK ↔ Play ohne Deinstallieren geht, soll der bestehende
Schlüssel beim Anlegen der App als App-Signaturschlüssel hochgeladen werden – **die App in der Play Console bitte bis
dahin nicht anlegen** (die Wahl des Signaturschlüssels ist danach nicht mehr änderbar).

**Workflow-Kopfzeile:** „Drive, Ordner Schlüssel (geheim)“ durch „Bitwarden“ ersetzt (auch in `kurvenflug/.gitignore`).

---

Neueste Meldung oben. Zweig `kurvenflug-test`, Ordner `kurvenflug/webapp/` (index.html oben). `kurvenflug-live` setzt nur der Leitstand.

---

## 0.19.0 – Oberstufe Klasse 11–13 (Gleichstand mit APK 0.19.0)

- **Version:** 0.19.0 · **Commit:** 021eca8 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** Klassenwahl 5–13; vier neue Kapitel: Ableitung & Tangente, Extrempunkte, Integral (Treibstoff = Fläche),
  Vektoren (X = P + t · v) – je mit Live-Rechnung, Rechen-Stern und Lösungsweg. Aurora-Geschichte (Kl. 5–10) unverändert.
- **Prüfen:** Klasse 12 wählen → „Weiter geht's!“ führt zu „Vektoren · Erster Pfeil“: v = (8 | 6) → Treffer.
  Galaxie → Liste: Kapitel „Integral“ – beim Flug füllt sich die Fläche unter der Kurve.
- Gleichstands-Test (JVM ↔ Wasm) deckt die neuen Kapitel mit ab. Browser: keine Konsolenfehler.

---

## 0.18.0 – Verstehen statt nur Schieben (Gleichstand mit APK 0.18.0)

- **Version:** 0.18.0 · **Commit:** fefa4b4 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** Live-Rechnung unter der Formel, „Rechen-Stern“ (3. Stern nur mit der richtigen Rechnung aus drei), danach
  der Lösungsweg mit den Zahlen des Levels; die Aufgabe bleibt nach einem Fehlversuch sichtbar.
- **Prüfen:** Level „Erster Kontakt“ gewinnen → 2 Sterne + Frage „Wie liest du (4 | 3)?“ → richtige Antwort → 3. Stern,
  „So rechnet man's“. Seite neu laden: 3 Sterne gespeichert.
- **Technik-Hinweis:** Der Bildschirmleser-Spiegel von Compose (unsichtbare Textkopie für Screenreader) ist abgeschaltet –
  er schrieb bei Levelende gelegentlich „IllegalStateException: Node … not found“ in die Konsole (Fehler der Bibliothek,
  auch in 0.17.x). Danach in 10 Durchläufen keine Konsolenfehler.

---

## 0.17.1 – WebApp aktualisiert sich selbst (Gleichstand mit APK 0.17.1)

- **Version:** 0.17.1 (Ladebildschirm, `version.json`, Einstellungen)
- **Commit:** 4e9fdb3 (Zweig claude/gifted-shannon-657gpr)
- **Neu:** kein Update-Knopf, die WebApp aktualisiert sich selbst. Sie sucht beim Öffnen, beim Zurückkehren in den Tab
  und alle 30 Minuten nach einer neuen Version. Ist die Seite gerade erst geöffnet (unter 15 s), wird sofort neu geladen.
  Sonst wird gewechselt, sobald der Tab im Hintergrund war (nie mitten im Level). Der Spielstand bleibt erhalten.
- **Prüfen:** Seite öffnen → „Version 0.17.1“. Wer 0.17.0 schon offen hatte, sieht nach dem nächsten Öffnen 0.17.1, ohne etwas zu tun.
- Selbst geprüft: Wechsel A → B beim Öffnen (sofort) und im laufenden Spiel (erst nach Hintergrund), ohne Konsolenfehler.

---

## 0.17.0 – erste WebApp (Gleichstand mit APK 0.17.0)

- **Version:** 0.17.0 (APK und WebApp gleich; steht im Ladebildschirm, in `version.json` und unter Einstellungen „Version 0.17.0 · WebApp“)
- **Commit:** 3d78727 (Zweig claude/gifted-shannon-657gpr) (Quellcode), WebApp darauf gebaut mit `./gradlew :web:webapp`
- **Größe:** ca. 15 MB, nur erlaubte Dateitypen (html, js, wasm, json, webmanifest, png, svg, ttf, txt), keine Symlinks, kein Server-Code
- **Technik:** eine gemeinsame Codebasis (Kotlin Multiplatform + Compose Multiplatform, Kotlin/Wasm). Ein Gleichstands-Test
  rechnet alle Level, Flugbahnen, Tagesmissionen und Codes auf Android (JVM) und im Browser (Wasm) und vergleicht den Fingerabdruck.
- **Datenschutz:** keine externen Aufrufe (keine CDNs, Schriften liegen bei, kein Tracking). Spielstand nur in `localStorage`
  (Schlüssel `kf.*`), nichts an den Server. Links „Impressum“ und „Datenschutz“ im Ladebildschirm und unter Einstellungen.

### Bitte auf https://kurvenflug-test.mathenachhilfe-online.com prüfen
1. Seite lädt (Ladebildschirm mit Rakete → Startseite mit Volt), unten im Ladebildschirm „Version 0.17.0“.
2. Klasse wählen → Geschichte „Die Reise der Aurora“ → Startseite. Reiter Heute / Galaxie / Spiele / Ich antippen.
3. „Weiter geht's!“ → Level „Erster Kontakt“: x mit + auf 4, y auf 3, „Start“ → 3 Sterne, Sticker.
4. Seite neu laden: Spielstand bleibt (Abzeichen „Abheben“ erscheint).
5. ⚙️ Einstellungen: „Version 0.17.0 · WebApp“, Links Impressum und Datenschutz öffnen die richtigen Seiten.
6. Browser-„Zurück“ (bzw. Esc am PC) führt eine Ebene zurück, auf der Startseite verlässt es die Seite.
7. Installieren: Chrome/Edge „App installieren“ bzw. iPhone „Zum Home-Bildschirm“. Danach Flugmodus → App startet trotzdem (Service Worker).
8. Am PC: App erscheint als Handy-Spalte in der Mitte, Spielfeld und Regler sind ganz sichtbar.

### Hinweise für den Server
- `.wasm` muss als `application/wasm` ausgeliefert werden (sonst lädt die App nicht), `.webmanifest` als `application/manifest+json`.
- `sw.js`, `index.html` und `version.json` am besten ohne langes Caching (`Cache-Control: no-cache`); die `.wasm`-Dateien haben
  Hash-Namen und dürfen lange gecacht werden.
- Läuft in aktuellen Browsern mit WebAssembly-GC: Chrome/Edge ab 119, Firefox ab 120, Safari ab 18.2.

### Selbst geprüft (Chromium, Handy 412×860 und PC 1280×800)
Keine Konsolenfehler. Laden ca. 1 s (lokal). Klassenwahl, Geschichte, alle 4 Reiter, alle 14 Kacheln unter Spiele/Ich
(jeweils hinein und mit Browser-Zurück heraus), Level gewonnen, Neuladen mit erhaltenem Spielstand, offline neu geladen.
