# Meldungen der Raumlicht-Sitzung an den Leitstand

Neueste oben.

## Korrektheitsprüfung 17.11.

**Version nach der Prüfung: Raumlicht 2.13.0 vom 17.11.2026** (Android-APK, Play-Paket und WebApp auf raumlicht-test). Auftrag von Marcel direkt bestätigt.

**Kurzfassung:** Alle Aufgaben wurden unabhängig nachgerechnet. **Keine hinterlegte Lösung war falsch, kein Rechenweg enthielt einen Zahlenfehler.**
Echte Rechenfehler gab es nur außerhalb der Aufgaben: bei *eigenen* Funktionen in „Kurve abfahren“, im Funktions-Parser und im Zahlformat für sehr große Zahlen.
Dazu fehlende Angaben in vier Aufgaben, eine Bühne mit falschem Maßstab, missverständliche Rückmeldungen, Schreibweise, Vorlese-Umsetzung von Formeln, fehlende Lizenztexte. Alles unten Genannte ist behoben, sofern nicht unter „Nicht geändert“ oder „Nicht prüfbar“ aufgeführt.

### 1. Was geprüft wurde und wie

| Bereich | Umfang | Wie |
|---|---|---|
| Tour-Aufgaben | 136 Aufgaben in 25 Kapiteln, alle Kapitel-Einführungen | Je Paket eine unabhängige Nachrechnung nur aus Aufgabentext und gezeigten Angaben (Python: Brüche, sympy), ohne die hinterlegte Lösung vorauszusetzen. Geprüft: Lösung, Eindeutigkeit, jeder Rechenweg-Schritt, Tipp, Rückmeldungen, 3D-Bühne (Koordinaten, Ebenengleichungen, Kurvenpunkte, Flächen, Labels) |
| Prüfungsaufgaben | 7 Aufgaben, 28 Teilaufgaben; 9 Schritt-für-Schritt-Aufgaben | wie oben, zusätzlich Zusammenhang der Teile und Punktesummen (je 12 BE) |
| Endloses Üben, alle Stufen | 29 Aufgabenarten × 40 Zahlensätze = 1160 Aufgaben, dazu 31 Tagesaufgaben | je Art ein eigener Löser, der die Angaben aus dem Text liest; Grenzfälle (parallel, windschief, identisch, Nullvektor, Punkt auf Spiegel, entartete Figuren) gezielt gesucht – keine gefunden |
| Angenommene Antworten | alle Aufgaben mit durchzählbarem Reglerbereich | alle Reglerstellungen durchprobiert: Es wird genau angenommen, was richtig ist (auch Vielfache von Richtungs- und Normalenvektoren) |
| Spielarten | Lage von Geraden (20 000 Runden), Binomial (9900 Kombinationen n, p), Galton, Zufallsmaschine, Datum (1,5 Mio. Tage), Flüge, Ballerbude-Regeln, Ringflug (5000), Baumeister, Kurve abfahren, Funktions-Parser, 3D-Rechner, Geisterfahrt | Kotlin-Rechenkern aufgerufen und mit Python nachgerechnet |
| Sprache | alle Aufgabentexte, alle Oberflächen-Texte (Android, WebApp), Vorlese-Texte, Store-Text, README, Änderungsliste, Lizenzen | vollständig gelesen; Übungsaufgaben und Vorlese-Texte in Stichproben plus Muster-Suche über alles |
| Funktion | 129 Tests im Rechenkern, 139 App-Tests (Robolectric, mit Bildschirmfotos), Lint für GitHub- und Play-Fassung | alle grün |
| WebApp | 54 Bildschirme, 72 Aufrufe in Handy- und PC-Größe; Ablauf, Update, Offline-Start, 3D-Bedienung | Chromium: keine Konsolenfehler, kein leeres Bild; Übersichtsblätter aller Bildschirme angesehen |
| Texte nach außen | Store-Eintrag, Datensicherheit, Datenschutz-Abschnitt in der App, Lizenzen | gegen Berechtigungen, Internet-Adressen im Code und die tatsächlich eingebundenen Bibliotheken geprüft |
| Stufen-Nachweis | Einstieg 35,75 → Abi 47,93 → Profi 78,44 | weiter grün |

Neu im Repo, damit das so bleibt: `PruefTextTest` (Schreibweise aller 1364 Aufgaben-Varianten, auch bei zufälligen falschen Eingaben; Lösung wird angenommen, Start nicht),
`PruefDumpTest` (schreibt alle Aufgaben für eine Nachrechnung von außen nach `core/model/build/pruefung/`), `scene/test/web-screens.mjs` (öffnet jeden Bildschirm der WebApp),
Vergleich der Kurvendiskussion mit sympy an 90 Termen als Dauertest.

### 2. Abweichungen und Korrekturen (vorher → nachher)

**A. Echte Rechenfehler (nicht in Aufgaben, sondern in Spielarten)**

| Wo | vorher | nachher |
|---|---|---|
| Kurve abfahren, eigene Funktion | x³, x⁴, (x − 1)², x³ − 6x² + 9x: Nullstellen bzw. Extrempunkte fehlten | werden gefunden (auch Berührstellen und Nullstellen am Rand) |
| Kurve abfahren, eigene Funktion | 1/x², ln(x²), sin(x)/x: „Hochpunkt/Tiefpunkt bei x = 0“ an einer Polstelle oder Lücke | dort keine Stelle |
| Kurve abfahren, eigene Funktion | Geraden wie e^(ln x), x²/x: bis zu 60 falsche Wendepunkte | keine |
| Kurve abfahren, eigene Funktion | |sin x|: Tiefpunkt mit Knick als „Hochpunkt“ | „Tiefpunkt“ mit Regel „Knick: f′ wechselt das Vorzeichen“ |
| Kurve abfahren, Anzeige | „keine Krümmung (Wendestelle)“ bei jeder Geraden und auf 27 % der fertigen Bahn „Logarithmus“ | „Wendestelle“ nur an echten Wendestellen, sonst „fast gerade“ |
| Kurve abfahren, Anzeige | ln(x) bei x = −1: „Höhe 0, Steigung 0“; 1/x bei 0,01: Höhe 14,4; Fläche über eine Polstelle hinweg „−4,36“ | „nicht definiert“; 100 mit „außerhalb des Bildes“; Fläche „–“ |
| Kurve abfahren | Stellen am Rand nie markiert; fielen zwei Stellen zusammen, wurde nur eine angesagt | Rand-Nullstellen markiert; „Tiefpunkt und Nullstelle bei x = 3“ |
| Funktions-Parser | „πx“, „√x“, „pi x“ abgelehnt („pix kenne ich nicht“); „sin 2x“ = sin(2)·x; „2 2“ = 22; Anzeige „x^23“ als „x²3“ | werden richtig gelesen; sin 2x = sin(2x); Fehlermeldung; richtige Anzeige. Neu: „Verstanden als: f(x) = …“ |
| Zahlformat | ab 21 Millionen „2,147483647E7“; −0,125 → „−0,12“, aber 0,125 → „0,13“ | ganze Zahl bzw. „4,85 · 10¹⁴“, „∞“, „nicht definiert“; symmetrisch gerundet |
| 3D-Rechner | identische Ebenen hießen „parallel“; Treffer hinter dem Laser-Start als Treffer gemeldet | „identisch“; „die Gerade trifft …, der Strahl nicht“; parallele Ebenen mit Abstand |
| Flug | eigener Flug ohne Ebene: „Abstand 0 · genau in der Ebene“; Tangente an die Kugel als Ein- und Austritt; „Boden: x₃ = 1“ | „kein Abstand“; „Berührt K“; „E₃: x₃ = 1“ |
| Binomial-Labor | „meist zwischen μ − σ und μ + σ“ (teils negativ, teils unter 50 %) | „Mit 66 % zwischen 4 und 6 Treffern (μ ± σ)“ – ausgerechnet |
| Geisterfahrt | flache Nulllinie: 55 %, „die Richtung passt“ | unter 50 % |
| Ballerbude | „−1“ eingeblendet, obwohl bei 0 Punkten nichts abgezogen wird | Abzug nur, wenn wirklich abgezogen wird |

**B. Aufgaben: fehlende Angaben und Bühne**

| Aufgabe | vorher | nachher |
|---|---|---|
| 20-4 | Ebene E stand weder im Auftrag noch in der Formel (nur im Tipp) | „E: x₁ + 2x₂ + 2x₃ = 9“ steht in der Formel |
| 8-5 | Punkt P nur als Bühnen-Label, A unbenannt | „P(5|3|4)“ in der Formel, „A(0|1|0) ist der Stützpunkt“ |
| 11-3, Üben „Flughöhe“, abi-drohnen-b | leere zweite Formelzeile, Zielhöhe nur im Text | „Scheibe: x₃ = 4“ / „Höhe: x₃ = h“ / „Boden: x₃ = 0“ |
| Prüfungsaufgaben und Schritt-für-Schritt | Sachzusammenhang (z. B. f(x) des Lichtbogens, Lostrommel, Ebene) war während der Teilaufgaben nicht sichtbar | bleibt bei jeder Teilaufgabe über der Formel stehen |
| 17-5 | Bühne zeigte N/100, Ziellinie bei 8, Text „800“ | Hinweis „Hochachse: N in Hundert“ |
| 23-5 | 6 von 20 Kugeln gelb bei Grenze 50 (Skala 40–70) | anteilsgetreu: 10 von 20 bei 50, 12 von 20 bei 59 |
| 10-1 | verkürzter Pfeil mit Label „a⃗×b⃗“ | „Richtung von a⃗×b⃗“ |
| 10-2 | „A“ zugleich Punkt und Fläche | Fläche heißt F |
| 11-4, 11-5 | Bahnen „g“, „h“ beschriftet, Formel kennt nur D₁, D₂ | Bahn-Labels entfernt |
| 3-5, Üben „Zielen“ | Labels „Z1“, „Z2“ | „Z₁“, „Z₂“ |
| abi-zelt-d | Schattenpunkt hieß „S“ wie die Spitze | „S′“ |
| abi-trommel-c | Auftrag „Und ohne Zurücklegen?“ | „Zwei Züge ohne Zurücklegen: Wie wahrscheinlich sind jetzt zwei Gewinne?“ |
| abi-bogen-b/-d, abi-einlass-a/-c | Antwort ohne Einheit | m, m², h |
| 4-4, 6-1, 5-6, 2-3 | „fehlende Komponente c“ ohne Angabe welche; v⃗ nicht genannt; Start „O(1|1|1)“ (O ist sonst der Ursprung) | „x₃-Komponente c in der Richtung von h“, „x₂-Komponente c von b⃗“, „v⃗ = (1|2|−1)“, „S(1|1|1)“ |

**C. Aufgaben: Rückmeldungen und Fachliches**

| Wo | vorher | nachher |
|---|---|---|
| alle Schnitt-/Durchstoßaufgaben (4-1, 4-2, 4-5, 5-1, 5-2, 5-6, 9-3, Üben, Prüfungen) | „Dein Punkt liegt auf g und liegt auf h noch nicht ✗.“ (gemeint: beides nicht) | „Dein Punkt liegt weder auf g noch auf h ✗.“ / „… liegt in E ✓, aber noch nicht auf g ✗.“ |
| Zielen (3-1, 3-2, 3-5, 4-3, 10-4, 19-3, Üben) | „Jedes Vielfache dieser Richtung hätte auch getroffen.“ | „Jedes positive Vielfache …“ |
| Zielen | „Der Strahl geht 5 Einheiten am Ziel vorbei.“ (Abstand zur ganzen Geraden, auch wenn das Ziel hinter dem Laser liegt; „1 Einheiten“) | Abstand zum Strahl, mit Namen des Ziels: „… 6,4 Einheiten an Z vorbei.“; „1 Einheit“ |
| 5-4 | „Der Strahl geht 2,24 Einheiten am Ziel vorbei“ (Ziel unsichtbar, gefragt ist senkrecht) | „n⃗ · u⃗ = 2 und n⃗ · v⃗ = 0 – beide müssen 0 sein.“ |
| Parameter-Aufgaben (3-4 u. a.) | „g: x⃗ = (6|5|4) + −3 · (1|1|1)“; „Übers Ziel hinaus“, obwohl falsche Richtung; „t = −3 – genau so oft passt der Richtungsvektor hinein“; „Lux braucht …“ (Lux spricht sonst in der Ich-Form) | „+ (−3) ·“; „Falsche Richtung: t muss kleiner sein.“; „t = −3 – 3-mal entgegen dem Richtungsvektor.“ |
| 8-5, 11-3, 20-4, abi-drohnen-b, Üben „Flughöhe“ | Erfolgsmeldung „genau so oft passt der Richtungsvektor hinein“ auf eine Zeit- oder Abstandsfrage | eigene Sätze, z. B. „Nach 4 Sekunden landet Drohne 1 – bei (8|2|0).“ |
| 5-5, 9-4 | „Die Scheibe sitzt genau richtig!“ (Auftrag: Lichtfläche); „setz die Punkte ein“ bei einem Punkt; „S ✓  A, B ✗“ | „Die Ebene sitzt …“; „Setz den Punkt ein“; „S ✓ – A, B ✗: …“ |
| Vektor-Aufgaben | „x₃ stimmt ✓ – x₁ und x₂ noch nicht.“ | „Die 3. Komponente stimmt ✓ – die 1. und 2. Komponente noch nicht.“ |
| abi-einlass-a/-c | „Der Zeitpunkt ist größer.“ | „Der Zeitpunkt liegt später.“ |
| 2-2 | „zurück und nach oben“ bei v⃗ = (4|−4|3) | „nach links und nach oben“ |
| 7-2 | „nicht in einer Koordinatenebene“ | „nicht beide in derselben Koordinatenebene“ |
| 11-2 | „Die Geschwindigkeit ist die Länge des Vektors“ (Einführung: Länge = Tempo) | „Das Tempo ist die Länge des Geschwindigkeitsvektors“ |
| 19-1 | „Winkel zwischen den Normalenvektoren“ | „der spitze Winkel … – dafür sorgt der Betrag im Zähler“ |
| 20-2, 20-5 | A, B im Rechenweg unbenannt | als Stützpunkte benannt |
| 14-4, 25-4, Einführung Kap. 14 | Wendestelle nur mit f″(x) = 0 | mit f‴ ≠ 0 bzw. Vorzeichenwechsel |
| 14-2, 14-3, 14-4 | „Hochpunkt bei x = 2“ | „Hochpunkt H(2|4): Bei x = 2 …“ (Punkt und Stelle getrennt) |
| 15-3 | Scheitel angenommen, drittes Tor nie geprüft | Probe f(3) = 3 ergänzt |
| 17-4 | Lage des Rechtecks nur aus dem Bild; Maximum ohne Nachweis | Auftrag präzisiert; A″(x) = −4 < 0 |
| 16-5 | Rechenweg „meist zwischen 40 und 60“, Finale „45 und 55“ | einheitlich 45 und 55 (μ ± σ) |
| 23-3, 24-5 | „unter 10 %“, „über 90 %“, „klappt es sicher“ (96,9 %), „Mit 1 Versuchen“ | „höchstens 10 %“, „mindestens 90 %“, „reicht es auch“, „Mit 1 Versuch“ |
| 23-5 | „Irrtum höchstens 5 %“ | „Irrtumswahrscheinlichkeit“ |
| 13-4, 13-5 | Finale zweimal derselbe Satz; „Mindestens einer/eines/eins“ | zwei Sätze; einheitlich „eines“ |
| 12-3, 12-4, Einführung Kap. 12 | „V = … = 8π, b = 1“; „x von -3 bis 3“; „Du stellst das Vielfache von π ein“ | „Ziel: V = π · ∫₀ᵇ f(x)² dx = 8π.“; „−3“; „Meist …“ |
| Einführungen Kap. 4 und 10 | vierter Fall „identisch“ fehlte; „Das letzte Kapitel der Geometrie“ | ergänzt; ersetzt |
| Schritt-für-Schritt, offene Punkt-Aufgabe | „Mit t = -1“; „liegt in der … und liegt in der … nicht“ | „−1“; „liegt nicht in der … und liegt nicht in der …“ |

**D. Schreibweise in erzeugten Übungsaufgaben**

| Art | vorher | nachher |
|---|---|---|
| Schräge Scheibe | „14 + −4 · t = 10“ | „14 − 4 · t = 10“ |
| Drei auf einer Geraden, Dieselbe Gerade | „r = 2 : −2“, „k = 2 + −1 · 2“, „k = −2 · −1“ | „2 : (−2)“, „2 + (−1) · 2“, „−2 · (−1)“ |
| Parallel mit Parameter | „2 · 3 + −1 · a + 2 · −1“, „2 · a + −6 = 0“, „1 · a + 2 = 0“ | „2 · 3 + (−1) · a + 2 · (−1)“, „2 · a − 6 = 0“, „a + 2 = 0“ |
| Schnittkreis, Spiegelbild | „n⃗ · M = -8“, „|11 − −1|“, „F(3,33|6,67|2,33) = … mit t = 0,67“ | „−8“, „|11 + 1|“, „t = ⅔: F ≈ (3,33|6,67|2,33)“ |
| Winkel, Spiegeln an der Achse | gerundete Werte mit „=“ („= 16,97“, „= 4,12“) | „≈“ |
| Zweimal spiegeln, Raute | „2 · √25 ≈ 10“, „√9 ≈ 3“, „√9 lang“ | „= 10“, „= 3“, „3 lang“ |
| e-Funktion-Hinweise | „f′(-2) = −0,41“ | „f′(−2) ≈ −0,41“ |

**E. Vorlesen (Lux liest vor)** – `Speech.kt`, danach Lautschrift-Daten neu erzeugt (610 feste Sätze, 2101 Wörter)

| Zeichen | vorher gesprochen | nachher |
|---|---|---|
| d = |9 − 18| : 3 | „d gleich unter der Bedingung 9 minus 18 unter der Bedingung : 3“ | „d gleich Betrag von 9 minus 18, geteilt durch 3“ |
| P′ = 2 · F − P | „P gleich 2 mal F minus P“ | „P Strich gleich 2 mal F minus P“ |
| f″(1) = e > 0 | „f zwei Strich 1 gleich e 0“ | „f zwei Strich von 1 gleich e größer als 0“ |
| (x − 3)(x + 1) | „x minus 3 x plus 1“ | „Klammer auf x minus 3 Klammer zu mal Klammer auf x plus 1 Klammer zu“ |
| 3 + 3t = 9 → t = 2; t = ⅔; e⁰; ∫₀²; 2 €; ✓ ✗ ∥ | Zeichen fielen weg, der Sinn kippte | „also“, „zwei Drittel“, „e hoch 0“, „Integral von 0 bis 2“, „2 Euro“, „stimmt“, „parallel zu“ |

Nach der Korrektur enthält die Umsetzung aller 3679 Aufgabentexte kein Sonderzeichen mehr.

**F. Oberfläche, Begriffe, Rechtschreibung**
- Alte Namen ersetzt: „Level“ → „Aufgabe“ (Erfolge), „Backstage“ → „Mehr“, „Tagesgig“ → „Tagesaufgabe“ (Widget-Name), „Jam-Session“ → „Endlos üben“, „Klassenpaket“ → „Klassen-Paket“.
- Einzahl ergänzt: „1 Aufgabe“, „1 Tagesaufgabe gelöst“, „1 Punkt“, „1 typischer Fehler“, „1 Aufgabenart“, „1 Kugel“, „1 Schritt“, „1 Erfolg“, „Nach 1 Stunde“.
- „kein Krümmen“ → „keine Krümmung“; „Los geht's“ → „Los geht’s“; „Nochmal“/„noch mal“ → „Noch einmal“; „ich baller“ → „ich ballere“; Größenangabe im Update immer mit Komma.
- Ballerbude: „Daneben! Serie weg“ → „Daneben – die Serie startet neu“ (der Store-Text verspricht „keine verlorenen Serien“).
- Binomial-Labor: „P(X ≤ 0) = P(X = 0) + … + P(X = 0)“ → „P(X ≤ 0) = P(X = 0)“; „In etwa 0 von 100“ → „Selten: In weniger als 1 von 100“; bei genau 100 % „Immer“ statt „so gut wie sicher“.
- Lux spricht durchgehend in der Ich-Form (Flug-Fehlermeldungen, 20-4).
- Barrierefreiheit: Zurück-Pfeil („Zurück“), Zahnrad („Einstellungen“) und die Plus/Minus-Knöpfe der Regler („x₁ größer“) haben jetzt Namen für TalkBack. Vorher hatte kein Bedienelement eine Beschreibung.

**G. Texte nach außen**
- Store-Text: „Spickzettel“ → „Formelsammlung“, „Abzeichen“ → „Erfolge“. Zahlen (136/25/7) und Längen stimmen.
- README: Kopfteil nannte noch „75 Gigs in 14 Kapiteln“ und alte Namen → auf heutigen Stand gebracht.
- Änderungsliste: Komma, „Aufgaben-Arten“ → „Aufgabenarten“.
- **Lizenzen:** In App und WebApp fehlten die Lizenztexte der freien Bausteine. In der WebApp steckt außerdem js-joda (BSD), das nirgends genannt war; Skia (BSD), Kotlin/AndroidX (Apache 2.0) und die Ed25519-Prüfung nach TweetNaCl fehlten in `LIZENZEN.md`.
  Jetzt: `lizenzen.txt` mit allen Texten liegt der Android-App und der WebApp bei, erreichbar über Einstellungen → „Fremde Bausteine & Lizenzen“; `LIZENZEN.md` ergänzt.
- **GPL:** weiterhin kein GPL-Baustein in der App (sherpa-onnx 2.0.1 ohne espeak-ng; der Bau prüft das Store-Paket darauf). Unverändert.
- **Datenschutz WebApp:** Die WebApp verspricht „keine Daten an einen Server“, wählte zum Vorlesen aber notfalls eine Browser-Stimme, die den Text an einen Server schicken kann.
  Jetzt wird nur mit einer deutschen Stimme vorgelesen, die auf dem Gerät eingebaut ist; gibt es keine, ist „Lux spricht“ nicht verfügbar. Text angepasst.
- Datensicherheit, Berechtigungen (Play-Fassung ohne Installations-Berechtigung), Internet-Adressen (nur GitHub-Updates in der GitHub-Fassung, Hugging Face für die freiwillige Stimme, Impressum/Datenschutz) stimmen mit `STORE-RELEASE.md` überein.

### 3. Nicht geändert – bitte entscheiden (Marcel)

- **Punkt-/Vektor-Kurzschreibweise** „v⃗ = B − A“, „M = ½ · (A + B)“, „g: x⃗ = A + t · d⃗“ (Kapitel 1–3, 6, Formelsammlung): gängige Schulkurzform, streng wäre „OB⃗ − OA⃗“. An einer Stelle (2-6) steht die strenge Form. Welche soll gelten?
- **„e^x“** neben hochgestelltem „e⁰“, „2ᵗ“ (Kapitel 25, Binomial-Formel): Lange Exponenten lassen sich nicht hochstellen; so gelassen.
- **Eingabefelder als Gleichung:** „d(P, E) = [Eingabe]“ (6-4), „OZ⃗ = … + r · …“ (2-4), „V = … = 8π, b = [Eingabe]“: Die Zeile zeigt die aktuelle Eingabe. Bewusst so, in 12-3 klarer formuliert.
- **Üben „Parameter“:** Bei 15 von 40 Zahlensätzen ragt die Spitze des Richtungspfeils aus dem gezeichneten Raum. Rechnerisch richtig, nur Darstellung.
- **Üben „Rotationskörper“:** nur 8 verschiedene Kurven (fester Katalog), „Tempo“ wiederholt sich öfter. Kein Fehler, aber wenig Abwechslung.
- **Namen von Erfolgen** „Abi-ready“, „Residency“ (englisch) und **geschützte Leerzeichen** bei „z. B.“ und „30 %“ (können am Zeilenende umbrechen): Geschmack bzw. Typografie, nicht geändert.
- **Zielgruppe im Play Store:** `STORE-RELEASE.md` empfiehlt „16–17 und 18+“. Schüler der Klasse 11 können 15 sein. Ob „13–15“ dazugehört (mit den strengeren Regeln), ist deine Entscheidung.
- **Kurve abfahren:** Wo eine eigene Funktion nicht definiert ist, zeichnet die Bühne die Bahn weiter auf Höhe 0 (ein durchgehender Linienzug). Cockpit, Fläche und Hinweis sagen dort ehrlich „nicht definiert“.
- **3D-Rechner:** unterscheidet jetzt Strahl und Gerade nur am Startpunkt; Treffer jenseits des Raumrands zählen weiter als Treffer.

### 4. Nicht prüfbar

- **Echte Geräte:** Nichts wurde auf einem Handy, iPhone oder iPad geprüft – nur Robolectric (Android) und Chromium (WebApp). Das iPad-Problem (weißer Bildschirm, Korrektur seit 2.11.1) wartet auf Marcels Test.
- **Vorlese-Hilfen:** In der WebApp (Zeichenfläche) gibt es keine Beschriftungen für Screenreader; die neuen Namen wirken nur in der Android-App. TalkBack selbst wurde nicht ausprobiert.
- **Klang der Stimme:** Geprüft ist der Text, der vorgelesen wird, nicht die Aussprache (z. B. „Klammer auf … Klammer zu“ klingt bei langen Gleichungen hölzern; Vektor-Rechnungen bleiben Zahlenfolgen).
- **Ob eine Aufgabe didaktisch gut gestellt ist** (Schwierigkeit, Reihenfolge, Abi-Nähe je Bundesland) kann die Nachrechnung nicht beurteilen.
- **Bestandteile in sherpa-onnx:** Die Lizenzen der darin enthaltenen Unterbibliotheken sind nicht einzeln geprüft (Lizenzliste nennt sherpa-onnx und ONNX Runtime).
- **Play In-App Updates, Abi-Pass mit echtem Schlüssel, Freischalt-Link auf der Live-Adresse:** erst mit Store-Installation bzw. echtem Test-Code prüfbar.
- **Rückmeldungen bei jeder denkbaren falschen Eingabe:** geprüft sind Start-Einstellung, typische Fehler und je Aufgabe 25 zufällige Eingaben, nicht alle.

---

## 16.11.2026 – Stand Play Store und WebApp (Antwort auf die Fragen vom 16.11.)

**Aktuelle Version:** Android 2.4.1 vom 15.11.2026, WebApp-Vorschau 2.4.1 auf der Testadresse. Seit dem 15.11. keine neue Version.

**Play Store – noch nicht begonnen, Start am 17.11.2026 um 15:00 Uhr.** Marcel hat den Auftrag bestätigt, aber ausdrücklich erst ab diesem Zeitpunkt freigegeben.
Geplante Reihenfolge:
1. *GPL / espeak-ng:* Marcel hat sich für einen Weg ohne GPL-Baustein entschieden. Am 16.11. getestet und funktionsfähig: sherpa-onnx 2.0.1 (enthält den
   espeak-ng-Kern nicht mehr) spricht die Stimme „Thorsten“ aus vorab erzeugter Lautschrift – feste Texte je ganzem Satz, veränderliche aus einem Wörterbuch,
   unbekannte Wörter über die Handy-Stimme. Damit entfällt espeak-ng in der App ganz (Store und GitHub). Einbau ab 17.11.; bis dahin enthält 2.4.1 espeak-ng weiterhin.
   Hinweis: „Lizenztext + Quellcode-Angebot“ hätte nach meinem Verständnis nicht gereicht – espeak-ng ist fest eingebunden, die GPL-3 beträfe die ganze App.
2. *In-App-Updates:* Die Store-Variante bekommt die Play In-App Updates API (flexibel, bei kritischen Versionen sofort) und keinen eigenen APK-Updater.
   Die GitHub-Variante behält ihren Updater. Nicht geplant: Bühnen-Inhalte vom Server nachladen (von Marcel nicht verlangt, alles bleibt offline in der App).
3. Signiertes AAB, deutscher Store-Eintrag mit Grafiken, Vorlagen für Datensicherheit und Altersfreigabe, Track „Interner Test“. Ergebnis kommt nach `raumlicht/STORE-RELEASE.md`.
Nicht testbar von hier: In-App-Updates (nur mit einer über Play installierten App) und der Start der Sprach-Bibliothek auf einem echten Gerät.

**WebApp – Web ist noch NICHT gleich Android.** Unverändert seit der Meldung vom 15.11.: gemeinsam sind Rechenlogik, 3D-Bühne und Versionsnummer; die Vorschau zeigt
alle 89 Tour-Aufgaben mit einer schlanken eigenen Oberfläche. Der Umzug der Android-Oberfläche und der Extras in den gemeinsamen Code ist nicht begonnen und
von Marcel noch nicht beauftragt (meine Frage dazu ist offen). Hinzugekommen am 15.11.: automatisches Update ohne Knopf (geprüft mit `scene/test/web-update.mjs`)
und der Workflow `raumlicht-web-test.yml`, der diesen Branch nach jedem Push vorspult. Eine Freigabe für `raumlicht-live` frage ich erst an, wenn Web = Android.

**Hinweis zum Ablageort der Schlüssel:** erledigt am 16.11. (von Marcel bestätigt) – in `raumlicht-release.yml` und `raumlicht/.gitignore` steht jetzt „Bitwarden“ statt Drive.

## 15.11.2026 – Erste Web-Vorschau (Version 2.4.1)

**Stand:** Web-Vorschau, gebaut aus demselben Code wie die Android-App 2.4.1 vom 15.11.2026.
Die fertigen Dateien liegen in `raumlicht/webapp/` (15 MB; nur html, js, map, json, webmanifest, png, ttf, wasm – keine Symlinks, kein Server-Code, keine fremden Adressen).

**Weg (entschieden):** Compose Multiplatform mit Kotlin/Wasm. Gemeinsam sind bereits
- die gesamte Rechenlogik (`core/model`: alle 89 Aufgaben, Auswertung, Tipps, Spiele-Logik) – ein Modul für Android und Web,
- die 3D-Bühne: dieselbe `scene.js` (three.js) wie in der Android-App, im Browser als eigenes Fenster-Element eingeblendet,
- die Versionsnummer: Web und Android lesen sie aus derselben Stelle (`app/build.gradle.kts`), sie kann nicht auseinanderlaufen.

**Was die Vorschau kann:** alle 89 Tour-Aufgaben in drei Gebieten mit echter 3D-Bühne, Regler, Auswertung, Tipp, Sterne.
Fortschritt nur im Browser (localStorage, keine Konten, nichts an Server). Installierbar (webmanifest), offline nutzbar (Service Worker),
Links zu Impressum und Datenschutz, Version sichtbar auf der Startseite und in `version.json`.

**Automatisches Update (Marcels Wunsch, kein Knopf):** Die WebApp fragt beim Öffnen, beim Zurückkehren und alle 30 Minuten nach einer neuen Version
(`sw.js` wird am Browser-Zwischenspeicher vorbei geholt). Ist eine da, wird sie im Hintergrund geladen, der alte Speicher weggeräumt und die Seite lädt sich
von selbst neu – auf der Startseite sofort, in einer Aufgabe erst beim Zurückgehen oder wenn die App im Hintergrund ist. Geprüft mit `scene/test/web-update.mjs`
(Versionswechsel nachgestellt, auch offline). Bitte `sw.js` und `index.html` vom Server nicht lange zwischenspeichern lassen (kurze oder keine Cache-Dauer).

**Was noch fehlt (ehrlich):** Die Oberfläche der Vorschau ist noch eine schlanke eigene – die Android-Oberfläche (Startseite mit Lux, Kapitel-Erklärungen,
Rechenweg, Formelanzeige, Animationen, Ton) und alle Extras (Kurve abfahren, Baumeister, Ballerbude, Stochastik-Spiele, Abi-Aufgaben, Planer …) ziehen als Nächstes
schrittweise in den gemeinsamen Code um. Erst dann ist Web = Android. Emojis fehlen im Web noch (eigene Schrift nötig). Nur Android bleiben vorerst: Widgets,
natürliche Stimme „Thorsten“, In-App-Updater.

**Bitte auf der Testadresse prüfen** (https://raumlicht-test.mathenachhilfe-online.com):
1. Seite lädt, oben steht „Version 2.4.1 vom 15.11.2026“; keine Fehler in der Browser-Konsole.
2. Aufgabe 1-1 öffnen: 3D-Bühne erscheint im Rahmen, lässt sich mit Finger/Maus drehen. P auf (4|2|0) stellen, „Los“ → Erfolgsmeldung, Stern auf der Startseite.
3. Seite neu laden: Stern ist noch da.
4. Handy (Hochformat) und PC: Bühne sitzt jeweils im Rahmen.
5. Installieren („Zum Startbildschirm“) und offline öffnen.
6. Der Server muss `.wasm` als `application/wasm` und `.webmanifest` als `application/manifest+json` ausliefern – sonst startet die App nicht bzw. ist nicht installierbar.

**Von hier geprüft (Chromium, Handy- und PC-Größe, lokal ausgeliefert):** Punkte 1–4 automatisch per `scene/test/web.mjs`, ohne Konsolenfehler. Nicht geprüft:
Safari/Firefox, echtes Handy, Installation und Offline-Betrieb über die echte Adresse.

**Hinweise:** Die Android-App enthält seit 2.4.0 espeak-ng (GPL-3, siehe `LIZENZEN.md`) – vor Shop/Play Store klären. Die WebApp enthält das nicht.
Bitte noch nichts ankündigen: Das ist eine Vorschau, kein fertiges Produkt. `raumlicht-live` fasse ich nicht an.


---

## 16.11.2026 – WebApp 2.5.0: jetzt die ganze App (Auftrag von Marcel: „Bau die webapp“)

**Stand:** Die WebApp zeigt jetzt dieselben Bildschirme wie die Android-App – aus demselben Code (Modul `shared`). Drin sind: Startseite mit Lux und Bühne,
alle Kapitel mit Einführung, Rechenweg und Formelanzeige, Tagesaufgabe, Üben, Abi, „Mehr“ mit allen Extras (Kurve abfahren, Flug, Ballerbude, Baumeister,
Geisterfahrt, Ringflug, Zufallsmaschine, Galton-Brett …), Planer, Einstellungen (mit Links zu Impressum und Datenschutz, „Fortschritt zurücksetzen“).
Emojis und Mathe-Zeichen kommen aus mitgelieferten Schriften (DejaVu Sans, Noto Color Emoji – beide frei, siehe `LIZENZEN.md`).

**Technik, die ihr wissen solltet:** `webapp/` ist jetzt rund 27 MB (vorher 15 MB; die Emoji-Schrift hat 10,8 MB, wird wie alles andere vom Service Worker
einmal gespeichert). Dateitypen unverändert (`.wasm`, `.ttf`, `.js`, `.html`, `.json`, `.webmanifest`, `.png`, `.map`). Gespeichert wird nur im Browser
(localStorage, Schlüssel `raumlicht.*`); keine Anfragen an fremde Server, keine Cookies. Sprachausgabe: `speechSynthesis` des Browsers –
je nach Browser/System rechnet die auf dem Gerät oder beim Browser-Hersteller; das steht so auch in den Einstellungen der WebApp. Bitte für die Datenschutzseite bedenken.

**Noch nicht gleich mit Android (ehrlich):** Hintergrundmusik, natürliche Stimme „Thorsten“, Widgets, Erfolgskarte als Bild teilen (im Web wird nur Text geteilt).
Der In-App-Updater entfällt im Web (aktualisiert sich selbst).

**Von hier geprüft (Chromium ohne echte Grafikkarte, Handy- und PC-Größe, lokal ausgeliefert):** `scene/test/web.mjs` – Start, Aufgabe 1-1 mit Einführung öffnen,
Bühne drehen und zoomen, Aufgabe lösen, Sterne gespeichert, Browser-Zurück, „Mehr“, Neuladen; keine Konsolenfehler. `scene/test/web-update.mjs` – automatisches Update und Offline-Start.
**Nicht geprüft:** Safari und Firefox, echtes Handy (Fingerbedienung der Bühne, Zwei-Finger-Zoom), Ton und Stimme (im Test nicht hörbar),
die Extras einzeln im Browser (sie sind derselbe Code wie unter Android und dort getestet, aber im Browser nicht einzeln durchgeklickt).

**Bitte auf der Testadresse prüfen:** 1. „Version 2.5.0 vom 16.11.2026“ in den Einstellungen (Startseite → Mehr → Einstellungen). 2. Aufgabe 1-1 lösen, neu laden, Sterne noch da.
3. Auf einem echten Handy: Bühne mit einem Finger drehen, mit zwei zoomen; Punkt in einer Zieh-Aufgabe ziehen. 4. Safari (iPhone) und Firefox starten überhaupt.
5. Je ein Extra aus „Mehr“ öffnen (Kurve abfahren, Ballerbude, Galton-Brett). 6. Ton/Stimme nach dem ersten Antippen hörbar.

**Freigabe:** `raumlicht-live` beantrage ich noch **nicht** – erst wenn Punkte 3 und 4 auf echten Geräten bestätigt sind. `raumlicht-live` fasse ich nicht an.

---

## 16.11.2026 – WebApp 2.5.1 (Wunsch von Marcel)

Hintergrundmusik läuft jetzt auch in der WebApp (im Browser erzeugt, keine Audiodateien, startet nach der ersten Berührung, abschaltbar).
Das Vorlesen („Lux spricht“) ist in Android und Web ab jetzt **aus**, bis man es in den Einstellungen einschaltet – die Sprachausgabe des Browsers
wird also ohne Zutun gar nicht mehr benutzt. Noch nur Android: natürliche Stimme, Widgets, Erfolgskarte als Bild. Im Test nicht hörbar geprüft (kein Lautsprecher) –
bitte auf der Testadresse einmal anhören. `raumlicht-live` weiterhin nicht beantragt.

---

## 16.11.2026 – Antwort zu Datenschutz-Abschnitt 10e (WebApp 2.5.1)

Der Abschnitt stimmt im Kern. Drei Genauigkeiten, bitte so übernehmen:

1. **Lokaler Speicher:** richtig. Gespeichert wird nur im Browser des Geräts: Spielstand und Einstellungen (localStorage, Schlüssel `raumlicht.*`)
   sowie die App-Dateien selbst für den Offline-Betrieb (Cache des Service Workers `raumlicht-<Version>`). Keine Cookies, kein Konto, keine Analyse.
   „Fortschritt zurücksetzen“ in den Einstellungen löscht den Spielstand; Browserdaten löschen entfernt alles.
2. **Vorlesen nur auf Wunsch:** richtig – seit 2.5.1 ist es aus, bis man „Lux spricht“ einschaltet. **Aber bitte nicht „rein auf dem Gerät“ versprechen:**
   Die WebApp nutzt die Sprachausgabe des Browsers und bevorzugt eine Stimme, die auf dem Gerät rechnet. Hat der Browser nur Online-Stimmen
   (bei Chrome am PC möglich), schickt **der Browser** den vorzulesenden Text (Aufgaben- und Hinweistexte der App, keine Eingaben mit Personenbezug)
   an seinen Hersteller. Vorschlag für den Text: „… über die Sprachausgabe Ihres Browsers bzw. Geräts; je nach Browser kann dieser den vorzulesenden Text
   zur Sprachsynthese an seinen Anbieter übertragen.“
3. **Keine fremden Server:** für die WebApp richtig – sie lädt ausschließlich von der eigenen Adresse (App-Dateien, Schriften, `version.json`, Update-Prüfung über `sw.js`).
   Für die **Android-App** gilt das nicht wörtlich: Sie fragt GitHub nach Updates (abschaltbar) und lädt die natürliche Stimme auf Wunsch einmalig von Hugging Face.
   Falls 10e nur die WebApp meint, bitte das dazuschreiben.

Hinweis: Musik und Klänge entstehen im Browser (keine Audiodateien, kein Abruf). Am echten Gerät habe ich nichts gehört oder angefasst – eure Prüfung am Handy ist die einzige.

---

## 16.11.2026 – Version 2.6.0: bereit für den Play Store (von Marcel auf heute vorgezogen)

1. **espeak-ng ist raus:** sherpa-onnx 2.0.1 (ohne espeak-ng), Hilfsdaten entfernt; die Lautschrift liefert die App als vorab erzeugte Daten mit
   (`tools/stimme/lautschrift.py`, `LIZENZEN.md`). Test deckt alle Sprechtexte ab; unbekannte Wörter liest die Handy-Stimme.
2. **Play-Fassung:** Fassung `play` ohne eigenen Updater und ohne Berechtigung „Apps installieren“, mit Play In-App Updates
   (im Hintergrund, eingespielt beim nächsten Start; dringend ab Priorität 4). Die GitHub-APK behält ihren Updater.
3. **Signiertes AAB** baut der GitHub-Lauf und hängt es an den Release (`Raumlicht-2.6.0-play.aab`); geprüft werden Signatur, fehlende Installations-Berechtigung, keine espeak-Daten.
4. **Store-Eintrag, Datensicherheit, Altersfreigabe, Zielgruppe, Klick-Liste:** `raumlicht/STORE-RELEASE.md` und `raumlicht/store/` (Texte, Icon 512, Grafik 1024×500, acht Bildschirmfotos).
   Wichtig darin: die Schlüssel-Entscheidung bei Play App Signing (sonst verlieren bisherige APK-Nutzer den Spielstand).

Veröffentlicht ist nichts; hochladen muss Marcel selbst. WebApp 2.6.0 ist inhaltlich gleich 2.5.1 (nur neue Versionsnummer).
Nicht geprüft: echtes Gerät, Play In-App Updates (geht erst mit einer über den Store installierten Version). Der Inhaltsausbau folgt als nächste Version.

---

## 16.11.2026 – Version 2.7.0: Inhaltsausbau (Auftrag „sehr viel Inhalt, alles prüfen“, von Marcel bestätigt)

**Neu:** 35 Aufgaben in sechs Kapiteln (18 Ebenen in allen Formen, 19 Schnitte & Schnittwinkel, 20 Spiegeln & Lote, 21 Kugel & Ebene, 22 Integrale & Flächen,
23 Erwartung/Sigma/Testen) und zwei Prüfungsaufgaben im Abi-Stil („Planetarium“, „Einlass“). Gesamt: 124 Aufgaben in 23 Kapiteln, 7 Prüfungsaufgaben.
Jede Aufgabe hat Tipp, Rechenweg, gezielte Hinweise bei typischen Fehlern und eine 3D-Bühne (Analysis im 2D-Koordinatensystem, Stochastik ohne Achsen).

**Prüfung (automatisch, `:core:model:jvmTest`, 84 Tests):** Lösung wird anerkannt, Startwerte lösen nicht, Werte im erlaubten Bereich, Bühne in beiden Phasen gültig,
alles Sichtbare im Raum; zusätzlich `TourSixTest`: jede Lösung unabhängig nachgerechnet (Kreuzprodukte, Abstände, Spiegelpunkte, numerische Integrale,
Binomialverteilung exakt – Hypothesentest P(X ≥ 59) ≈ 4,4 %), jede Zahlen-Aufgabe hat genau eine richtige Antwort. App-Tests (132) grün.
**Web = Android:** derselbe Code; WebApp 2.7.0 gebaut, `scene/test/web.mjs` (Handy- und PC-Größe) und `web-update.mjs` ohne Fehler.
**Angesehen:** alle 35 neuen Bühnen gerendert (vorher/gelöst), drei Darstellungen nachgebessert.
**Nicht geprüft:** echtes Gerät; die neuen Aufgaben im Browser einzeln durchgespielt (sie nutzen dieselben Bausteine wie die alten); fachliche Gegenprüfung durch einen Menschen.

---

## 16.11.2026 – Version 2.8.0 (Wünsche von Marcel) und ein offener Fehler: iPad zeigt weißen Bildschirm

**Neu:** Binomial-Labor unter „Mehr“ (n, p, k frei; genau/höchstens/mindestens; einzeln und kumuliert), Kapitel 24 „Binomialverteilung“ (5 Aufgaben),
Kapitel 25 „e-Funktion & Logarithmus“ (7 Aufgaben), zwei neue Bahnen in „Kurve abfahren“. 136 Aufgaben in 25 Kapiteln; alles unabhängig nachgerechnet
(`TourSixTest`: Binomial-Rechenkern gegen die Formel, abgedruckte Tabellenwerte, Ableitungen und Integrale numerisch).

**Offener Fehler (von Marcel gemeldet):** Auf seinem iPad blieb der Bildschirm beim Start weiß. Ursache noch **nicht** gefunden – hier gibt es kein Safari/WebKit zum Testen.
Die Server liefern korrekt aus (geprüft: Dateien, Dateitypen, auch `.wasm`). Bekannt ist: Die WebApp braucht WebAssembly-GC, das Safari erst ab iOS/iPadOS 18.2 kann.
Eingebaut in 2.8.0: (1) sichtbare Meldung statt leerem Bildschirm, wenn der Start scheitert (auch bei fehlendem WebAssembly-GC, sofort geprüft),
(2) `diagnose.html` – zeigt Browser, WebAssembly-GC, WebGL 2, letzten Startfehler, Zwischenspeicher; nichts wird verschickt, (3) kleinere Bildpuffer der 3D-Bühne auf großen Bildschirmen.
**Bitte prüft auf einem iPad/iPhone** (Safari, und als Startbildschirm-App): Startet es? Was zeigt `/diagnose.html`? Bis das geklärt ist, bitte nicht mit „läuft auf iPad“ werben.

---

## 16.11.2026 – Version 2.9.0: Start optimiert (Auftrag „Start optimieren, kein weißer Bildschirm“, von Marcel bestätigt)

**1. Kein leerer Bildschirm:** Ladebild direkt im HTML/CSS (Nachtblau, Lux-Logo als inline-SVG, Name) – steht ohne Skript ab dem ersten Byte.
Fortschrittsbalken mit echten Bytes für App-Kern (.wasm) und Schriften (entpackte Bytes gegen im Bau eingetragene Größen), danach 0,35 s Überblenden.
Scheitert der Start, steht dort eine Meldung mit Link zu `diagnose.html`.

**2. Schneller:** Emoji-Schrift 10,8 MB → 0,18 MB und DejaVu 0,76 MB → 0,12 MB (nur benutzte Zeichen, `tools/schrift/schriften.py`); .wasm und Schriften werden sofort angefordert
(nicht erst, wenn `raumlicht.js` sie verlangt); die 3D-Bühne (iframe) lädt erst danach; der Service Worker meldet sich erst nach dem Start an, lädt nichts mehr doppelt
und speichert jetzt auch die .wasm-Dateien schon beim ersten Besuch (zweiter Start ohne Netz).

**3. Messung** (`scene/test/web-speed.mjs`, Chromium, Handy-Größe; der Testserver bremst wie „Fast 4G“ 9 Mbit/s/60 ms und „Slow 4G“ 1,6 Mbit/s/150 ms und liefert wie euer Server aus:
gzip nur für .html und .wasm):

| | 2.8.0 vorher | 2.9.0 nachher | 2.9.0, wenn der Server auch .js/.ttf/.json komprimiert |
|---|---|---|---|
| Fast 4G, erster Start: erster Inhalt / bedienbar | 0,1 s / 27,9 s | 0,2 s / 5,8 s | 0,2 s / 5,3 s |
| Fast 4G, zweiter Start: bedienbar | 2,4 s | 2,3 s | 2,3 s |
| Slow 4G, erster Start: erster Inhalt / bedienbar | 0,2 s / 153,2 s | 0,2 s / 29,2 s | 0,2 s / 26,5 s |
| Slow 4G, zweiter Start: bedienbar | 2,5 s | 2,6 s | 2,6 s |
| übertragen beim ersten Besuch (mit Speichern) | 30,2 MB | 7,7 MB | 5,8 MB |
| Ordner `webapp/` | 28 MB | 17 MB | – |

Ehrlich dazu: gemessen auf einem Rechner ohne echte Grafikkarte, nicht auf einem Handy; die 2,3–2,6 s beim zweiten Start sind Rechenzeit (App-Kern übersetzen, Schriften, erste Zeichnung), kein Netz.

**Bitte am Server (liegt nicht in meiner Hand):**
- Heute kommen nur `.html` und `.wasm` komprimiert an (geprüft mit `Accept-Encoding: br, gzip`): **`.js`, `.json`, `.ttf` bitte ebenfalls komprimieren** – spart rund 1,9 MB beim ersten Besuch.
- **Brotli** statt gzip für `.wasm` brächte nochmal etwa ein Viertel weniger (die beiden .wasm sind zusammen 5,3 MB mit gzip).
- `.wasm`-Dateien haben den Inhalt im Namen: gern **lange Cache-Dauer** (`max-age=31536000, immutable`). `index.html`, `sw.js`, `version.json` weiter ohne Cache.

**4. Android:** Startbild über AndroidX SplashScreen (Nachtblau `#05071A`, Raumlicht-Symbol), danach App-Design. Das Fenster war schon vorher dunkel, weiß blitzte dort nichts.

**Weiterhin offen:** der weiße Bildschirm auf Marcels iPad. Das Ladebild hilft nur, wenn die Seite überhaupt lädt; die Ursache ist nicht gefunden (kein Safari hier).
Bitte auf iPad/iPhone prüfen und `diagnose.html` ansehen.
**Geprüft:** `web.mjs`, `web-update.mjs` (Update, Offline), `web-speed.mjs`; App-Tests und Lint. Nicht geprüft: echtes Gerät, Safari, Firefox.

---

## 16.11.2026 – Version 2.9.1 und Antwort zu „Skripte bündeln und verkleinern“

- **2.9.1:** Die gekürzten Schriften entstehen jetzt automatisch vor jedem Bau (`:web:schriften`); der Lauf „Raumlicht WebApp → Test“ prüft vor dem Vorspulen, dass kein benutztes Zeichen fehlt.
- **Zu „gebündelt und verkleinert“:** Das ist bei Raumlicht schon so – `raumlicht.js` (Webpack, minimiert, eine Datei) und `scene/scene.js` (esbuild `--bundle --minify`, eine Datei).
  Was fehlt, ist allein die **Komprimierung beim Ausliefern**: `raumlicht.js` 594 KB → 107 KB und `scene.js` 570 KB → 148 KB mit gzip. Das ist eine Server-Einstellung
  (gzip/Brotli auch für `.js`, `.json`, `.ttf`), kein Umbau der App. Bitte einschalten; geprüft habe ich es so: `curl -H "Accept-Encoding: br, gzip" -D - -o /dev/null <Adresse>/raumlicht.js` zeigt kein `content-encoding`.

---

## 16.11.2026 – Nachgemessen: Komprimierung am Server ist aktiv (danke)

Geprüft auf raumlicht-test (2.9.1) mit `Accept-Encoding: br, gzip`: alle großen Dateien kommen mit `content-encoding: gzip` an –
raumlicht.js 107 KB, scene.js 148 KB, Emoji-Schrift 169 KB, DejaVu 64 KB, die beiden .wasm 3,24 MB + 1,49 MB, index.html 4 KB. **Zusammen rund 5,2 MB** für den ersten Besuch
(2.8.0 ohne die Änderungen: 30,2 MB).

**Startzeiten** dazu aus der Simulation mit voller Komprimierung (`web-speed.mjs … alles-komprimiert`, Chromium am Rechner, kein echtes Handy):
erster Start 5,3 s („Fast 4G“) bzw. 26,5 s („Slow 4G“), zweiter Start 2,3–2,6 s ohne Netz. Gegen die echte Adresse kann ich die Zeiten nicht gebremst messen;
die übertragenen Bytes oben sind echt.

Noch möglich (klein): `.wasm` trägt den Inhalt im Namen und kommt ohne `cache-control` – `max-age=31536000, immutable` würde dem Browser die Rückfrage ersparen.
Live steht noch auf 2.5.1; 2.9.1 wartet auf eure Prüfung. Der weiße Bildschirm auf Marcels iPad ist weiter offen (er testet selbst).

---

## 16.11.2026 – Version 2.9.2: iPad-Befund, Fehlersuche, aufgeräumte Startseite

**Befund von Marcels iPad (Testadresse, 2.9.1):** Ladebild erscheint, danach kommt der Ton – aber das Bild bleibt **weiß**. Die App läuft also (Wasm, Logik, Ton), nur die Darstellung der Oberfläche in Safari fehlt.
Ursache noch offen; hier gibt es kein Safari. Verdächtig sind: das durchsichtige Fenster zur 3D-Bühne (die Bühne liegt als iframe unter der Compose-Zeichenfläche), zwei WebGL-Kontexte gleichzeitig, die Schriften.

**Eingebaut zum Eingrenzen:** (1) Mitschrift – Fehler/Warnungen der App landen in `localStorage` (`raumlicht.mitschrift`, letzte 40 Zeilen, nur auf dem Gerät) und stehen auf `diagnose.html`;
(2) Schalter `?ohne=fenster|buehne|emoji|ton` (kombinierbar) und dazu vier Test-Knöpfe auf `diagnose.html`; (3) echter WebGL-2-Zeichentest auf der Diagnose-Seite.
**Wenn ihr ein iPad/iPhone habt:** bitte `diagnose.html` öffnen, Tests 1–4 antippen und melden, ab welchem das Bild erscheint, plus Mitschrift. Bis dahin Raumlicht bitte nicht als „läuft auf iPad/iPhone“ bewerben.

**Außerdem:** Startseite aufgeräumt (Kapitel als eine wischbare Zeile statt Knopf-Wand) – Wunsch von Marcel nach einem Bildschirmfoto vom Android-Handy, wo die WebApp läuft.

---

## 16.11.2026 – Version 2.9.3: iPad, nächster Schritt

Neuer Befund von Marcel: **iPhone läuft, iPad bleibt weiß** (sein Handy ist Android, dort läuft es auch). Gleiche Browser-Technik, also liegt es vermutlich an der Größe der Zeichenfläche
(iPad bei doppelter Schärfe bis rund 5,6 Millionen Bildpunkte, iPhone rund 3). **Vermutung, nicht bestätigt.**
2.9.3 begrenzt deshalb auf iPads die Schärfe der Oberfläche so, dass höchstens rund 3 Millionen Bildpunkte gezeichnet werden (`window.devicePixelRatio` wird vor dem Start überschrieben;
erkannt über „iPad“ im User-Agent bzw. „Macintosh“ mit Touch). Schalter zum Prüfen: `?ohne=schaerfe` (Faktor 1), `?mit=schaerfe` (unbegrenzt) – als Test 5 und 6 auf `diagnose.html`.
Andere Geräte sind nicht betroffen. Bitte Live-Freigabe erst, wenn Marcel das iPad-Ergebnis gemeldet hat.

---

## 16.11.2026 – Version 2.10.0: Schwierigkeit, nachweislich steigend (Vorgabe von Marcel – er hat Weg 1 gewählt: nur das endlose Üben)

**Umfang (von Marcel so entschieden):** Raumlicht hatte keine Schwierigkeits-Auswahl. Neu ist sie im **endlosen Üben**: „Einstieg“, „Abi“, „Profi“ (oder „Alle“ wie bisher).
Die 136 festen Kapitel-Aufgaben, die Abi-Aufgaben und die Spiele bleiben unverändert – das war ausdrücklich nicht gewünscht.

**Stufen:** Einstieg = Aufgaben ohne Parameter-Gleichung (6 Arten). Abi = Gleichungen mit Parameter, Winkel, Abstände, Lotfußpunkt (10 Arten).
Profi = fünf neue mehrstufige Arten: Spiegelbild an der Ebene (Abstand + Lotfußpunkt + Spiegelung), Abstand windschiefer Geraden, Schnittkreis Kugel–Ebene,
Spiegeln an einer Geraden, Parallelität mit Parameter a. Zwischenwerte sind dort oft krumm (Lotfußpunkte mit Dritteln); das Ergebnis bleibt einstellbar (ganze Zahl bzw. Gitterpunkt),
weil die App nur ganze Zahlen als Eingabe kennt.

**Nachweis** (`DifficultyTest`, läuft in `:core:model:jvmTest`; Mittelwerte über 60 Zufallsaufgaben je Aufgabenart):

| Stufe | Aufgabenarten | Rechenschritte | Länge des Rechenwegs (Zeichen) | beteiligte Objekte | Parameter | Anteil krummer Werte | Gesamtwert |
|---|---|---|---|---|---|---|---|
| Einstieg | 6 | 1,84 | 24,46 | 3,67 | 0,67 | 0,00 % | 35,22 |
| Abi | 10 | 2,40 | 47,73 | 3,40 | 0,70 | 3,28 % | 43,13 |
| Profi | 5 | 4,00 | 124,48 | 5,00 | 1,20 | 10,38 % | 75,52 |

Der Test schlägt fehl, wenn von einer Stufe zur nächsten **Rechenschritte, Länge des Rechenwegs, Parameter, Anteil krummer Werte** oder der **Gesamtwert** (mindestens +20 %) nicht steigen,
wenn Profi nicht die meisten beteiligten Objekte hat, oder wenn die leichteste Profi-Aufgabe nicht über dem Abi-Durchschnitt liegt.
Ehrlich dazu: „beteiligte Objekte“ steigt von Einstieg zu Abi **nicht** (3,67 → 3,40) und wird dort deshalb nicht geprüft; „Parameter“ steigt von Einstieg zu Abi nur knapp (0,67 → 0,70).
Die Kennzahlen werden aus Rechenweg, Tipp, Angaben und Bühne gelesen – sie messen Aufwand, nicht, wie schwer sich eine Aufgabe für Schüler anfühlt.
Zusätzlich: jede Profi-Lösung unabhängig aus den angezeigten Angaben nachgerechnet (120 Zufallsaufgaben je Art), genau eine richtige Antwort, alles Sichtbare im Raum.

**Nicht umgesetzt (bewusst):** Brüche als Eingabe, Stufen für Kapitel/Abi/Spiele, mehrstufige Stochastik im Üben. Web = Android (gleicher Code), WebApp 2.10.0 gebaut und getestet.
Das iPad-Ergebnis zu 2.9.3 steht noch aus.

---

## 16.11.2026 – Version 2.10.1: iPad weiter weiß, Vermutung „Zeichenfläche zu groß“ widerlegt

Marcel hat mit der Begrenzung aus 2.9.3 erneut getestet: **iPhone läuft, iPad bleibt weiß.** Die Größe der Zeichenfläche ist es also nicht.
Bleibende Unterschiede iPhone ↔ iPad: (a) das iPad gibt sich standardmäßig als Mac aus („Desktop-Website“), (b) möglicherweise eine ältere iPadOS-/Safari-Version als auf dem iPhone.
Beides unbestätigt. 2.10.1 zeigt auf iPads nach dem Start 20 s lang einen HTML-Knopf „Kein Bild zu sehen? Hier tippen“ → `diagnose.html` (liegt über der Zeichenfläche, ist also auch bei weißem Bild sichtbar).
Dort stehen Mitschrift, Geräte-Modus und die Tests. **Raumlicht bitte weiterhin nicht als iPad-tauglich bewerben und 2.10.x erst live schalten, wenn ihr wollt – der iPad-Fehler besteht auch in der Live-Version 2.5.1.**

---

## 16.11.2026 – Konzept Pro für Raumlicht (Auftrag von Marcel bestätigt; **nur Konzept, nichts gebaut**)

Marcels Vorgaben dazu: Die bisherigen Aussagen „keine Käufe“ werden angepasst, sobald Pro gebaut wird; ein KI-Tutor ist als Bezahlfunktion in Ordnung.

### 1. Was ist frei, was Pro – und der Preis

**Grundsatz:** Frei muss für sich überzeugen (Empfehlungen, Schule, Eltern), Pro ist „alles bis zum Abi“. Keine Werbung, kein Druck, kein Abo, das sich verlängert.

| | Frei | Pro („Abi-Pass“) |
|---|---|---|
| Kapitel 1–6 (Punkte, Vektoren, Geraden, Lage, Ebenen, Skalarprodukt) | komplett, 36 Aufgaben | ✓ |
| Kapitel 7–25 | je Kapitel die ersten zwei Aufgaben zum Reinschnuppern | komplett (alle 136 Aufgaben) |
| Tagesaufgabe | ✓ | ✓ |
| Endlos üben | Stufe „Einstieg“ | zusätzlich „Abi“ und „Profi“ |
| Prüfungsaufgaben im Abi-Stil (7) | die erste („Festivalzelt“) | alle, mit Zeit-Modus |
| Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett | ✓ (fertige Bahnen) | zusätzlich eigene Funktionen, Baumeister, Geisterfahrt, Ringflug, Binomial-Labor |
| Spickzettel, Einstellungen, Musik, Vorlesen | ✓ | ✓ |
| Klausur-Planer, Fehler-Tagebuch, Lernstand | – | ✓ |
| „Lux erklärt“ (KI-Tutor, siehe 4) | – | ✓ mit Tagesgrenze |

**Preisvorschlag:** **Abi-Pass als Einmalkauf 14,99 €** (Einführung 9,99 €), gilt dauerhaft, kein Abo. Begründung im Vergleich (Preise aus der Recherche vom 16.11.2026, können abweichen):

| App | Modell | Preis |
|---|---|---|
| simpleclub unlimited | Abo | 16,49 €/Monat oder 89,99 €/Jahr |
| StudySmarter Premium | Abo | 9,99 €/Monat oder 89,99 €/Jahr |
| Mathegym (Privatlizenz) | Abo | 16 €/Monat oder 65 €/Jahr |
| sofatutor | Abo | je nach Laufzeit rund 20–35 €/Monat |
| Photomath Plus | Abo | 7,99 €/Monat oder 49,99 €/Jahr |

Diese Angebote decken alle Fächer oder alle Klassen ab; Raumlicht ist ein Spezialist (Oberstufen-Mathe, 136 Aufgaben). Ein Einmalpreis deutlich unter einem Jahresabo der Großen ist für Eltern leicht zu entscheiden und passt zum Versprechen „keine Abo-Falle“.
Von 14,99 € bleiben nach 19 % Umsatzsteuer rund 12,60 €; im Play Store gehen davon nach meinem Kenntnisstand 15 % Gebühr ab (bitte prüfen), über die Website nur die Zahlungsgebühren.

**Offen für Marcel:** (a) Preis, (b) wo genau die Grenze liegt, (c) Umgang mit heutigen Nutzern – bisher ist alles frei; wer die App schon hat, verliert Funktionen. Vorschlag: zum Start ein kostenloser „Gründer-Code“ für bisherige Tester.

### 2. Bezahlwege

- **Play-Fassung:** Google Play Billing, ein dauerhaftes Einmalprodukt (`raumlicht_pro`). Wiederherstellen über die Kaufabfrage beim Start – kein Code, kein Konto. In dieser Fassung kein Hinweis auf den Kauf über die Website (Play-Regeln; für die EU gibt es Sonderregeln, bitte prüfen).
- **Web- und GitHub-Fassung:** Woo-Kasse → Code per E-Mail → in den Einstellungen eingeben → freigeschaltet, ohne Konto. Rechnung und Nachweis über WooCommerce.
- In der App derselbe Schalter „Pro aktiv“ für beide Wege; die Fassungen `play`/`github` gibt es seit 2.6.0.

### 3. Was ich vom gemeinsamen Lizenz-Dienst brauche

1. **Aktivieren:** `POST /aktivieren {code, app: "raumlicht", geraet: <zufällige Installations-Kennung>}` → **signierter Freischalt-Schein** (z. B. Ed25519; Inhalt: App, Produkt, ausgestellt am, gültig bis oder „dauerhaft“, Kennung). Die App prüft die Signatur **ohne Netz** mit einem eingebauten öffentlichen Schlüssel – Pro funktioniert offline.
2. **Geräte:** z. B. bis zu 5 Aktivierungen je Code, selbst wieder freigebbar. Im Browser liegt der Schein im Browser-Speicher; wer den löscht, gibt den Code neu ein.
3. **Auffrischen:** gelegentlich (z. B. alle 30 Tage, mit Kulanz ohne Netz), damit Erstattungen wirken; Sperrliste.
4. **Play-Käufe:** optional Kaufbeleg serverseitig bei Google prüfen und denselben Schein ausstellen – nötig, sobald der KI-Tutor auch in der Play-Fassung laufen soll.
5. **KI-Zwischenstation:** `POST /tutor` nimmt Schein + Aufgabe, prüft, begrenzt (pro Tag), ruft die Claude-API auf. **Der API-Schlüssel liegt nur auf dem Server**, nie in App oder WebApp.
6. **Drumherum:** CORS für die App-Adressen, Testcodes, Webhook von Woo zum Erzeugen der Codes, Schlüsselwechsel möglich. Gespeichert werden nur Code, zufällige Kennung und Zeitpunkte – keine Namen in der App.

### 4. KI-Tutor „Lux erklärt“ – sinnvoll? Kosten?

**Sinnvoll, aber als zweite Stufe und eng geführt.** Idee: Nach einer falschen Antwort ein Knopf „Lux, erklär mir meinen Fehler“. Gesendet werden nur die Aufgabe, die eingestellten Werte und der richtige Rechenweg der App – **kein freier Text, keine persönlichen Daten**. Das Modell erklärt die bekannte Lösung, es rechnet nicht selbst (so bleiben Rechenfehler der KI draußen).

Kosten je Erklärung (rund 1.500 Zeichen-Einheiten hinein, 700 heraus; Preise der Claude-API in US-Dollar, Stand der Preisliste 03.11.2026):

| Modell | Preis je Million (rein / raus) | je Erklärung | 30 Erklärungen am Tag, ein Monat |
|---|---|---|---|
| Claude Haiku 5.5 | 0,10 $ / 0,50 $ | rund 0,05 Cent | rund 0,45 $ |
| Claude Sonnet 5.5 | 2 $ / 10 $ | rund 1 Cent | rund 9 $ |
| Claude Opus 5.5 | 4 $ / 20 $ | rund 2 Cent | rund 18 $ |

Folgerung: Mit Haiku 5.5 ist der Tutor im Einmalpreis tragbar, wenn es eine Tagesgrenze gibt (z. B. 30). Mit Sonnet oder Opus trägt ein Einmalkauf einen Vielnutzer nicht – dann bräuchte es ein Zeit-Paket. Welches Modell gut genug erklärt, müsste man an echten Aufgaben ausprobieren; das ist Marcels Entscheidung.

**Risiken, ehrlich:** (1) Daten verlassen das Gerät – Datenschutzerklärung, Play-Angaben „Datensicherheit“, Auftragsverarbeitung und das Alter der Nutzer (Schüler) müssen vorher geklärt sein; (2) laufende Kosten ohne Obergrenze, wenn die Tagesgrenze fehlt; (3) braucht Internet; (4) Erklärungen können trotz Vorgabe daneben liegen – deshalb nur die Lösung der App erklären lassen und „Stimmt nicht?“-Rückmeldung einbauen.

### Was beim Bau angepasst werden muss (noch nicht geschehen)

Texte „keine Käufe“ in den Einstellungen (Android und Web), Store-Eintrag (`store/texte.md`), Antworten zur Datensicherheit (`STORE-RELEASE.md`), Datenschutzerklärung der Website. Die Repo-Leitlinie verlangt, dass Bezahl-Angebote nie „nackter“ sind als der Rest – Pro-Inhalte bekommen dieselbe Ausstattung.

**Reihenfolge, wenn freigegeben:** 1) Lizenz-Dienst abstimmen, 2) Pro-Schalter und Sperren in der App samt Code-Eingabe, 3) Play Billing, 4) Tutor. Bis dahin baue ich nichts davon.

---

## 16.11.2026 – Konzept Pro: Marcels Entscheidungen

- **Grenze frei/Pro:** wie im Konzept – **ja**.
- **Bisherige Nutzer:** kostenloser „Gründer-Code“ für bisherige Tester – **ja**. Der Lizenz-Dienst braucht dafür Codes mit Preis 0.
- **KI-Tutor:** mit **Claude Haiku 5.5** starten und an echten Aufgaben testen – **ja**.
- **Preis/Zeitraum:** Einmalkauf; den Zeitraum hat Marcel offen gefragt. Mein Vorschlag an ihn: Inhalte **dauerhaft**, der KI-Tutor **12 Monate ab Kauf** (laufende Kosten), danach günstig verlängerbar.
  Für den Lizenz-Dienst heißt das: Ein Freischalt-Schein braucht **zwei Angaben** – „Pro dauerhaft“ und „Tutor gültig bis“. Entscheidung von Marcel steht noch aus.

Weiterhin nichts gebaut; ich warte auf die gemeinsame Abstimmung des Leitstands und Marcels Freigabe.

---

## 16.11.2026 – Konzept Pro, Nachtrag Umsatzsteuer

Alle Preise sind **Endpreise inkl. 19 % USt** und werden in App und Shop so angezeigt („inkl. MwSt.“).

| Produkt | brutto | netto (ohne 19 % USt) | kommt an über Play (netto minus 15 % Gebühr) | kommt an über die Website (netto minus Zahlungsgebühr) |
|---|---|---|---|---|
| Abi-Pass | 14,99 € | 12,60 € | 10,71 € | 11,81 € |
| Abi-Pass, Einführungspreis | 9,99 € | 8,39 € | 7,14 € | 7,76 € |
| Tutor-Verlängerung (Vorschlag) | 4,99 € | 4,19 € | 3,56 € | 3,70 € |

Annahmen: Play-Gebühr 15 % (nicht nachgeprüft); Zahlungsgebühr der Woo-Kasse beispielhaft 2,9 % + 0,35 € vom Bruttobetrag – bitte durch den echten Tarif des Zahlungsanbieters ersetzen.
Web-Kauf: Steuerklasse Standard 19 %, Rechnung weist die USt aus; bei Privatkunden in anderen EU-Ländern gilt deren Satz (OSS) – dann weicht der Nettobetrag ab. Play: Google führt die USt selbst ab.
Keine Steuerberatung.

---

## 16.11.2026 – Version 2.11.0: neue Aufgaben-Arten, Pro-Schalter vorbereitet (beides von Marcel direkt bestätigt)

### Aufgaben-Arten zur Analytischen Geometrie
Nur die Arten übernommen – Texte und Zahlen sind eigene, die Zahlen entstehen je Aufgabe neu.

| Art aus der Anregung | schon vorhanden? | neu im endlosen Üben | Stufe |
|---|---|---|---|
| Punkt in zwei Koordinatenebenen / gleich weit von zwei Achsen (offene Antwort) | nein | „Punkt mit Bedingungen“ in drei Varianten, Prüfung über Bedingungen statt fester Lösung, der Ursprung zählt nie | Einstieg |
| Spiegeln an einer Koordinatenebene | als feste Aufgabe 1-5 | zusätzlich als Übungsaufgabe | Einstieg |
| Spiegeln an einer Koordinatenachse | nein | neu, mit Original- und Bildpunkt in der Bühne | Abi |
| zwei Ebenen-Spiegelungen hintereinander | nein | neu (mit dem Hinweis: wirkt wie Spiegelung an einer Achse) | Profi |
| Parallelogramm nachweisen, Diagonalen-Schnittpunkt | vierte Ecke als feste Aufgabe 2-5 | neu: Nachweis über gleiche Vektoren + Schnittpunkt als Mittelpunkt | Abi |
| Raute prüfen | nein | neu als „Raute mit Parameter“ (gleiche Seitenlängen über Quadrate) | Profi |
| zweite Parameterdarstellung / dieselbe Gerade | nein | neu: „Für welches k beschreibt h dieselbe Gerade?“ (Richtungen kollinear, Stützpunkt liegt auf g) | Abi |
| drei Punkte auf einer Geraden | Richtung zu zwei Ringen (3-5) | neu: „Für welches k liegt C auf der Geraden durch A und B?“ | Abi |

Raum mit negativen Koordinaten fürs Spiegeln. „Alle“ mischt jetzt alle Arten aller Stufen.

**Schwierigkeit weiter nachweislich steigend** (`DifficultyTest`, jetzt 29 Aufgabenarten):

| Stufe | Aufgabenarten | Rechenschritte | Länge des Rechenwegs (Zeichen) | beteiligte Objekte | Parameter | Anteil krummer Werte | Gesamtwert |
|---|---|---|---|---|---|---|---|
| Einstieg | 8 | 1,88 | 33,76 | 3,59 | 0,50 | 1,33 % | 35,75 |
| Abi | 14 | 2,57 | 67,52 | 3,79 | 0,64 | 4,34 % | 47,91 |
| Profi | 7 | 4,00 | 149,93 | 5,57 | 0,86 | 12,18 % | 78,43 |

Geprüft: jede neue Art mit 250 Zufallsaufgaben (lösbar, Start löst nicht, alles im Raum, genau eine richtige Zahl) und unabhängig nachgerechnet
(offene Punkte: alle Gitterpunkte durchprobiert, Bedingungen geometrisch über den Achsenabstand bestätigt). 94 Logik-Tests, 138 App-Tests.

### Pro-Schalter und Sperren – vorbereitet, **ausgeschaltet**
- `Pro.ENABLED = false`: Im ausgelieferten Stand ist nichts gesperrt, es gibt keinen Kauf und keinen neuen Text für Nutzer. Ein Test (`ProTest`) hält das fest.
- Regeln nach Konzept: Kapitel 1–6 frei (das sind **34** Aufgaben – im Konzept stand irrtümlich 36), ab Kapitel 7 je die ersten zwei; Tagesaufgabe frei; Üben nur „Einstieg“ frei;
  die erste Prüfungsaufgabe frei; Baumeister, Geisterfahrt, Ringflug, Binomial-Labor, Klausur-Planer, Fehler-Tagebuch, Lernstand gehören zu Pro.
- Eine zentrale Sperre im App-Rahmen (`AppRoot`, für Android und Web gleich) zeigt bei gesperrten Zielen die Hinweis-Seite „Abi-Pass“; Schlösser an Aufgaben und Übungsstufen.
- Freigeschaltet wird später über `Prefs.pro` – **die Stelle, an der euer Lizenz-Dienst bzw. Play Billing andockt**. Kein Bezahlvorgang, kein Code-Feld, kein Tutor, kein Lizenz-Dienst gebaut.
- Noch offen für den echten Start: eigene Funktionen in „Kurve abfahren“/„Flug“ sperren, Texte „keine Käufe“, Store-Angaben.

### Preis
Entschieden hat Marcel: Grenze frei/Pro wie im Konzept, Gründer-Code, Tutor-Start mit Claude Haiku 5.5. **Nicht entschieden:** der Zeitraum und der Preis der Tutor-Verlängerung –
die „4,99 € pro Jahr“ waren mein Vorschlag, Marcel hat darauf mit „4,99 im Jahr?????“ reagiert. Bitte nicht als beschlossen führen.

---

## 16.11.2026 – Konzept Pro: Stand Preis der Tutor-Verlängerung

Marcel fand meinen geänderten Vorschlag gut („das ist nice“): Tutor-Verlängerung eher **9,99 € pro Jahr** statt 4,99 € (bei 4,99 € brutto kommen rund 3,60 € an; ein Vielnutzer an der Tagesgrenze kostet mit Claude Haiku 5.5 rund 5 $ im Jahr),
alternativ eine niedrigere Tagesgrenze. Das ist eine Tendenz, **noch keine ausdrückliche Entscheidung** – ich habe ihn um ein klares Ja gebeten.
Vorschlag im Ganzen: Abi-Pass 14,99 € einmalig (Einführung 9,99 €), Inhalte dauerhaft, Tutor 12 Monate inklusive, Verlängerung 9,99 € pro Jahr; alle Preise inkl. 19 % USt.
Bei 9,99 € brutto: 8,39 € netto, über Play rund 7,14 €, über die Website rund 7,76 € (Annahmen wie im USt-Nachtrag).

---

## 16.11.2026 – Konzept Pro: Preise von Marcel beschlossen

Marcel hat direkt bestätigt („ja“), alle Preise inkl. 19 % USt:

| Produkt | Preis | Zeitraum | kommt an über Play / Website (Annahmen wie im USt-Nachtrag) |
|---|---|---|---|
| Abi-Pass | 14,99 € einmalig (Einführung 9,99 €) | Inhalte **dauerhaft** | 10,71 € / 11,81 € (Einführung 7,14 € / 7,76 €) |
| KI-Tutor | im Abi-Pass enthalten | **12 Monate ab Kauf** | – |
| Tutor-Verlängerung | 9,99 € | je weiteres Jahr | 7,14 € / 7,76 € |

Für den Lizenz-Dienst: Der Freischalt-Schein braucht zwei Angaben – „Pro dauerhaft“ und „Tutor gültig bis“; die Verlängerung ist ein eigenes Produkt, das nur das Tutor-Datum verschiebt.
Ebenfalls beschlossen: Grenze frei/Pro wie im Konzept, Gründer-Code für bisherige Tester, Tutor-Start mit Claude Haiku 5.5 (an echten Aufgaben testen).
Gebaut ist davon weiterhin nur der ausgeschaltete Pro-Schalter mit den Sperren (2.11.0).

---

## 16.11.2026 – Konzept Lehrerzugang für Raumlicht (Auftrag von Marcel direkt bestätigt; **nur Konzept, nichts gebaut**)

**Was es heute schon gibt:** „Klassen-Paket“ (Mehr → Gemeinsam): Die Lehrkraft wählt Aufgaben, die App macht daraus einen Code; die Klasse gibt ihn ein, spielt das Paket und kann das Ergebnis als Text teilen.
Alles ohne Server und ohne Konto – das bleibt frei und ist der natürliche Baustein für Aufträge (das Code-Format kann der Lehrerzugang übernehmen).

### 1. Was die Lehrkraft sieht und steuert

| Funktion | Inhalt | wo |
|---|---|---|
| Auftrag „Kapitel/Aufgaben“ | feste Aufgaben auswählen (wie Klassen-Paket), Frist | Lehrerzugang → erscheint in der App unter „Mein Kurs“ |
| Auftrag „Üben“ | Thema + Stufe (Einstieg/Abi/Profi) + Anzahl, z. B. „10 Aufgaben Geraden, Stufe Abi“, Frist | wie oben |
| Klausur-Probe | eine Prüfungsaufgabe im Abi-Stil, auf Zeit, Punkte in Bewertungseinheiten | wie oben; Ergebnis je Teilaufgabe |
| Ergebnisse | je Schüler (Spitzname) und Kapitel: gelöst, Sterne, Versuche, zuletzt aktiv; je Auftrag: erledigt/offen | Lehrerzugang |
| „Wo hakt es?“ | je Aufgabe: Anteil beim ersten Versuch richtig – ohne Namen, für die Stundenplanung | Lehrerzugang |
| Beamer-Modus | Aufgabe groß mit 3D-Bühne, Lösung und Rechenweg schrittweise aufdeckbar, keine Schülerdaten sichtbar | in der App/WebApp selbst, braucht keinen Server |
| Kurs verwalten, Export, Löschen | zentral beim Leitstand | Lehrerzugang |

### 2. Welche Daten die App schickt (nur solange jemand in einem Kurs ist)

| Datum | wozu |
|---|---|
| Kurs-Code | Zuordnung |
| zufällige Installations-Kennung | Wiedererkennen desselben Geräts, kein Gerätename, keine Werbe-ID |
| Spitzname (selbst gewählt, Vorschlag wie „Lux-417“) | Anzeige bei der Lehrkraft |
| je gelöster Aufgabe: Aufgaben-Kennung bzw. Auftrag, Sterne (1–3), Zahl der Versuche, Datum | Ergebnisse |
| bei Klausur-Probe: Punkte je Teilaufgabe, benötigte Zeit | Auswertung |

**Nicht** gesendet: Klarname, E-Mail, Eingaben/Rechenwege, Standort, Geräte-Infos, Nutzung außerhalb der Aufträge (einstellbar: „nur Aufträge“ als Voreinstellung, „gesamter Fortschritt“ nur, wenn die Lehrkraft es für den Kurs einschaltet und die App es beim Beitritt anzeigt).
Ohne Netz wird gesammelt und später gesendet. „Kurs verlassen“ in den Einstellungen stoppt alles und bittet den Dienst um Löschung.

### 3. Wie ein Schüler beitritt
Einstellungen bzw. „Mehr“ → „Kurs beitreten“ → **Code eintippen oder QR/Link** (`…/?kurs=ABC123`, in der WebApp direkt) → Spitznamen wählen → eine Seite zeigt klar, was die Lehrkraft sehen kann → beitreten. Kein Konto, kein Passwort.
Der Dienst antwortet mit Kursname, Aufträgen und einem **signierten Schein „Pro über Kurs gültig bis …“** – dieselbe Mechanik wie beim Abi-Pass; in der App dockt er an `Prefs.pro` an (zweite Quelle „Kurs“).

### 4. Frei oder Schullizenz – mit Preisvorschlag (alle Preise inkl. 19 % USt)

| | frei | Kurs-/Schullizenz |
|---|---|---|
| Klassen-Paket per Code (ohne Server), Beamer-Modus | ✓ | ✓ |
| Kurs beitreten, Aufträge sehen | ✓ (ein Kurs zum Ausprobieren, bis 5 Schüler, 30 Tage) | ✓ |
| Ergebnisse je Schüler/Kapitel, „Wo hakt es?“, Export | – | ✓ |
| Pro-Inhalte (Abi-Pass) für alle im Kurs | – | ✓ für die Dauer der Lizenz |
| Klausur-Probe mit Auswertung | – | ✓ |
| KI-Tutor | – | vorerst nicht; später mit Kursgrenze (z. B. 300 Erklärungen pro Kurs und Monat) |

| Lizenz | Vorschlag | netto |
|---|---|---|
| Kurslizenz (ein Kurs, bis 35 Schüler, 12 Monate) | 49 € | 41,18 € |
| Schullizenz (alle Oberstufen-Kurse einer Schule, 12 Monate) | 199 € | 167,23 € |

Einordnung: Mathegym nennt für Schullizenzen 219 € (bis 50 Nutzer) bis 989 € (unbegrenzt) pro Jahr (Recherche vom 16.11.2026, kann abweichen) – für alle Klassenstufen; Raumlicht deckt nur die Oberstufe ab.
Ein Kurs mit 25 Schülern zahlt bei 49 € knapp 2 € pro Kopf statt 14,99 € für den Einzelkauf.

### Was ich vom zentralen Dienst brauche
`beitreten(code, spitzname, kennung)` → Kurs, Aufträge, Schein; `auftraege(kurs)`; `ergebnis(…)` (auch gesammelt); `verlassen(kennung)` mit Löschung. Aufträge am besten im Format des vorhandenen Klassen-Pakets plus „Üben: Thema, Stufe, Anzahl“ und „Prüfungsaufgabe: Kennung, auf Zeit“.

### Offen bzw. Risiken (ehrlich)
- **Datenschutz Schule:** Erstmals verlassen Schülerdaten das Gerät. Auch Spitzname + Kennung sind personenbezogen, sobald die Lehrkraft weiß, wer dahintersteckt. Nötig: Vertrag zur Auftragsverarbeitung mit der Schule, Einwilligung bzw. Rechtsgrundlage (Minderjährige), Löschfristen, Serverstandort. Das ist rechtlich zu klären, nicht von mir.
- Texte „keine Daten an einen Server“ in App, Store-Eintrag und Datensicherheit müssen dann geändert werden.
- Spitznamen können missbraucht werden (Beleidigungen) – die Lehrkraft muss umbenennen/entfernen können.
- Preise und die Grenze frei/Lizenz sind Vorschläge; Marcels Entscheidung steht aus.

---

## 16.11.2026 – Klärungsliste Recht und Datenschutz

Auf Marcels Wunsch („Benenne es, das kläre ich dann“) stehen alle offenen Rechts- und Datenschutzpunkte zu Lehrerzugang, Abi-Pass und KI-Tutor in `raumlicht/KLAERUNGSLISTE-RECHT.md`
(A1–A14 Schule, B1–B8 Bezahlen, C1–C6 KI). Vieles davon gilt für alle Apps gleich und betrifft euren zentralen Dienst (Serverstandort, Unterauftragnehmer, Löschkonzept, technische Maßnahmen, AVV-Muster).
Keine Rechtsberatung. Gebaut wird erst, wenn Marcel die Punkte geklärt hat.

---

## 16.11.2026 – Schulpreise von Marcel direkt bestätigt (ersetzt meinen Vorschlag im Konzept Lehrerzugang)

Mein früherer Vorschlag (Kurslizenz 49 €, Schullizenz 199 €) ist damit **überholt**. Es gilt, inkl. 19 % USt, je Schuljahr bis 31.07., ohne KI-Tutor:

| Lizenz | Preis | netto |
|---|---|---|
| Lehrkraft-Lizenz (eine Lehrkraft und ihre Kurse), je App | 29 € | 24,37 € |
| Schul-Lizenz (alle Lehrkräfte einer Schule), je App | 199 € | 167,23 € |
| Paket alle Apps, je Lehrkraft | 79 € | 66,39 € |
| Paket alle Apps, je Schule | 499 € | 419,33 € |

Folgen für Raumlicht: Die Lizenz hängt an der **Lehrkraft** (nicht am einzelnen Kurs) und endet am **31.07.** – der Schein „Pro über Kurs“ braucht also ein festes Enddatum statt „12 Monate ab Beitritt“.
Verkauf und Verwaltung über den zentralen Lehrerzugang; in der App wird nichts dazu gebaut, bis die Schnittstelle steht. Die Klärungsliste (`KLAERUNGSLISTE-RECHT.md`) gilt weiter.
Noch offen von Marcel: A9 (sieht die Lehrkraft nur Aufträge oder den ganzen Fortschritt?).

---

## 16.11.2026 – Lehrerzugang: Marcel hat A9 entschieden – gesamter Fortschritt

Die Lehrkraft sieht den **gesamten Fortschritt** der Schüler im Kurs, nicht nur die Aufträge. Das ersetzt meine Voreinstellung „nur Aufträge“ im Konzept.
Für die Schnittstelle: Die App sendet nach dem Beitritt das Ergebnis jeder gelösten Aufgabe (Tour-Aufgaben mit Kennung; Übungs- und Tagesaufgaben mit Art/Stufe statt Einzelkennung) – beim Beitritt einmal auch den bisherigen Stand,
sonst wäre der Fortschritt lückenhaft. Die Beitritts-Seite muss das klar sagen. In der Klärungsliste ist A9 entsprechend geändert (mehr Daten → ausdrücklich in Einwilligung und AVV).
Weiterhin nichts gebaut.

---

## 16.11.2026 – Zur Nachricht „Web = Android: Ja“ – das ist schon erledigt

Die Android-Oberfläche mit allen Extras läuft **seit 2.5.0 (16.11.2026)** aus gemeinsamem Code in der WebApp (Modul `shared`, `AppRoot`); auf raumlicht-test liegt aktuell **2.11.0**.
Nur Android haben weiterhin: natürliche Stimme „Thorsten“, Widgets, Erfolgskarte als Bild, In-App-Updater. Musik läuft seit 2.5.1 auch im Web.
Auch das Play-Store-Paket ist seit 2.6.0 fertig (auf Marcels Wunsch vorgezogen): `STORE-RELEASE.md`, `store/`, AAB am GitHub-Release.

**Was der Freigabe für raumlicht-live noch im Weg steht:** der offene Fehler „iPad zeigt nach dem Laden ein weißes Bild“ (iPhone und Android laufen). Der besteht auch in der Live-Version 2.5.1.
Ob ihr 2.11.0 trotzdem live schaltet, entscheidet ihr bzw. Marcel – aus meiner Sicht spricht nichts dagegen, weil sich für iPads nichts verschlechtert und es dort jetzt den Diagnose-Knopf gibt.

Zu den Punkten 2 und 3 (ein Kurs frei nur mit Rangliste; KI-Tutor nicht in Schullizenzen): zur Kenntnis genommen, in der App wird dazu nichts gebaut, bis eure Schnittstelle steht.
Marcel hat mir die Lizenzpreise (29 € / 199 € / Pakete 79 € / 499 €) und „gesamter Fortschritt“ direkt bestätigt; „ein Kurs frei, nur Rangliste“ hat er mir gegenüber noch nicht bestätigt.

---

## 16.11.2026 – Von Marcel direkt bestätigt

„Ein Kurs frei, nur mit Rangliste; mehr Kurse, Aufträge, Auswertung und Pro für alle nur mit Lizenz“ und „KI-Tutor vorerst nicht in Schullizenzen“ – beides hat Marcel mir bestätigt.
Damit ist für den Lehrerzugang aus Raumlicht-Sicht alles entschieden; gebaut wird, sobald eure Schnittstelle steht.

---

## 16.11.2026 – Version 2.11.1: iPad-Fehler eingegrenzt und umgangen (noch nicht auf einem iPad bestätigt)

Marcel hat die Diagnose-Ausgabe seines iPads geschickt (Safari 27.2, Desktop-Modus, WebGL 2 und WebAssembly-GC vorhanden). In der Mitschrift stand in jedem Bild
`RuntimeException: Can't wrap nullptr`; aufgelöst: `Shader.<init>` ← `LinearGradient.createShader` ← `BackgroundNode.draw`. Das Anlegen eines linearen Farbverlaufs liefert dort nichts zurück,
die Ausnahme bricht jedes Bild ab, übrig bleibt die weiße Grundfläche. Warum Safari dort nichts liefert, ist noch offen.

Geändert: alle 17 Verläufe laufen über `shared/.../ui/fx/Verlauf.kt` (feste, endliche Punkte; bei Fehler stufenweise einfacher bis einfarbig; Ergebnis steht einmal in der Mitschrift).
Auf raumlicht-test liegt 2.11.1. **Bestätigt ist die Behebung erst, wenn Marcel es auf dem iPad gesehen hat** – hier gibt es kein Safari zum Nachprüfen.

---

## 16.11.2026 – Version 2.12.0: Abi-Pass gebaut (von Marcel direkt bestätigt), Sperren aus bis zur Kassen-Adresse

Gebaut nach eurer Nachricht vom 16.11. und dem Vorbild KurvenFlug:

- **Code-Format:** Version 1, App 4, Produkt 1, Vorsilbe „RL-“, Ed25519, euer Base32-Alphabet; 9 Byte Inhalt + 64 Byte Signatur = 117 Zeichen (`core/model/.../License.kt`, `Ed25519.kt`, `Sha512.kt`).
  Andere App-Nummern und andere Produkte werden abgelehnt (also auch Schullizenz-Codes – die kommen erst mit dem Lehrerzugang).
- **Öffentlicher Schlüssel:** in `Pro.PUBLIC_KEY` eingetragen (979e…b812).
- **Freischalt-Link:** `https://raumlicht.mathenachhilfe-online.com/?pro=<Code ohne Bindestriche>` – mit oder ohne „RL“ am Anfang. Die WebApp prüft, speichert, nimmt den Code aus der Adresszeile
  und zeigt die Abi-Pass-Seite. Auf der Testadresse funktioniert derselbe Link.
- **Code-Eingabe:** Einstellungen → „Abi-Pass“ (WebApp und GitHub-APK); man kann die ganze E-Mail einfügen.
- **Sperren und Kauf-Knopf greifen erst, wenn `Pro.CHECKOUT_URL` gesetzt ist** (`core/model/.../Pro.kt`, derzeit leer). Bis dahin: kein Eintrag in den Einstellungen, keine Sperre, kein Wort von einem Kauf.
  Mit der Kassen-Adresse wechselt der Datenschutz-Satz in der App von „keine Käufe“ automatisch auf „kein Abo – nur der einmalige Abi-Pass, bezahlt auf unserer Website“.
  **Store-Texte und Datenschutzerklärung müsst ihr im selben Schritt ändern.** `ProTest.ohneKasseIstAllesFrei` schlägt dann absichtlich an, damit das niemand vergisst.
- **Preise in der App:** „zum Start 9,99 €, danach 14,99 € (inkl. MwSt.)“ (`Pro.INTRO_PRICE`, `Pro.PRICE`). Wenn der Startpreis endet, bitte Bescheid geben – der Text steht fest im Code.
- **Play-Fassung:** keine Sperren, kein Kauf-Knopf, kein Eintrag (dort wäre Play Billing Pflicht).
- **Android (GitHub-APK):** kein Link-Abfangen, nur Code-Eingabe. Der Link öffnet die WebApp.
- **Bestandsschutz:** nichts in der App – kostenlose Codes für Tester erzeugt ihr.

**Was ich brauche:** die Kassen-Adresse für RL-ABIPASS – und vorher einen echten Test-Code (Produkt 1, App 4), damit Marcel die Freischaltung einmal mit dem richtigen Schlüssel sieht.
Geprüft ist sie bisher nur mit einem Test-Schlüssel in den automatischen Tests.
