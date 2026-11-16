# Änderungen

Jede Version steht hier mit ihrem Datum.

## 2.11.1 – Weißer Bildschirm auf dem iPad: Ursache gefunden (16.11.2026)
- **iPad:** Die WebApp lief dort, zeigte aber nur Weiß. Die Mitschrift der Diagnose-Seite hat die Stelle gezeigt: Das iPad konnte einen **Farbverlauf** nicht anlegen, und daran brach jedes Bild ab.
  Alle Farbverläufe der App sind jetzt so gebaut, dass sie nie mehr ein Bild kosten – klappt ein Verlauf nicht, nimmt die App eine einfachere Füllung.
- Auf Android, iPhone und am PC sieht alles aus wie vorher.
- Die Mitschrift der Diagnose-Seite zählt gleiche Meldungen jetzt zusammen, statt die Liste zu füllen, und hält fest, ob die Verläufe geklappt haben.

## 2.11.0 – Acht neue Aufgaben-Arten zum Üben (16.11.2026)
- **Neu im endlosen Üben** (Themen „Punkte & Vektoren“ und „Geraden“), jedes Mal mit anderen Zahlen:
  - **Einstieg:** Punkte mit Bedingungen finden (zum Beispiel einen Punkt, der in zwei Koordinatenebenen zugleich liegt – hier gibt es viele richtige Antworten), Spiegeln an einer Koordinatenebene.
  - **Abi:** Spiegeln an einer Koordinatenachse, Parallelogramm prüfen und den Schnittpunkt der Diagonalen finden, „Liegen drei Punkte auf einer Geraden?“, dieselbe Gerade in anderer Schreibweise.
  - **Profi:** zwei Spiegelungen hintereinander, Raute mit Parameter.
- **„Alle“ mischt jetzt wirklich alle Aufgaben-Arten** aus allen Stufen.
- Die Schwierigkeit steigt weiterhin nachweislich von Stufe zu Stufe; alle neuen Lösungen sind unabhängig nachgerechnet.
- Im Hintergrund vorbereitet, aber ausgeschaltet: der spätere „Abi-Pass“. Für dich ändert sich nichts – alles bleibt frei.

## 2.10.1 – Direkter Weg zur Diagnose auf dem iPad (16.11.2026)
- **WebApp auf dem iPad:** Das Bild bleibt dort weiterhin weiß (auf dem iPhone läuft es); die Ursache ist noch offen. Nach dem Start erscheint auf iPads jetzt 20 Sekunden lang
  unten der Knopf „Kein Bild zu sehen? Hier tippen“ – er führt direkt zur Diagnose-Seite mit den Tests.
- Sonst keine Änderungen.

## 2.10.0 – Drei Schwierigkeits-Stufen im endlosen Üben (16.11.2026)
- **Stufe wählbar:** Unter „Üben“ gibt es jetzt „Einstieg“, „Abi“ und „Profi“ (oder wie bisher „Alle“ gemischt). Die Wahl wird gemerkt.
  - **Einstieg:** ein, zwei Rechenschritte mit glatten Zahlen – ablesen, mitteln, abziehen, einsetzen.
  - **Abi:** mehrere Schritte wie in der Prüfung – Gleichungen mit Parameter lösen, Winkel, Abstände, Lotfußpunkte.
  - **Profi (neu):** fünf mehrstufige Aufgabenarten – Spiegelbild an der Ebene, Abstand windschiefer Geraden, Schnittkreis von Kugel und Ebene,
    Spiegeln an einer Geraden, Parallelität mit Parameter. Die Zwischenwerte sind oft krumm, das Ergebnis lässt sich trotzdem genau einstellen.
- **Nachweislich steigend:** Ein Test misst je Stufe Rechenschritte, Länge des Rechenwegs, Parameter und den Anteil krummer Werte – und schlägt fehl, wenn eine Stufe nicht schwerer ist als die vorige.
- Die festen Aufgaben der Kapitel bleiben unverändert.

## 2.9.3 – Versuch gegen das weiße Bild auf dem iPad (16.11.2026)
- **WebApp auf dem iPad:** Auf dem iPhone läuft Raumlicht, auf dem iPad blieb das Bild weiß. Der Unterschied ist die viel größere Zeichenfläche.
  Auf iPads zeichnet die Oberfläche deshalb jetzt mit höchstens so vielen Bildpunkten wie auf einem iPhone – etwas weniger scharf.
  Ob das die Ursache trifft, ist noch nicht bestätigt; die Diagnose-Seite hat dafür zwei neue Tests (einfache und volle Schärfe).
- Sonst keine Änderungen.

## 2.9.2 – Aufgeräumte Startseite und Fehlersuche für das iPad (16.11.2026)
- **Startseite aufgeräumt:** Die Kapitel stehen jetzt in einer einzigen Zeile zum Wischen statt in sechs Reihen. Das gewählte Kapitel rutscht von selbst ins Bild,
  darüber steht, wie viele Kapitel das Gebiet hat. Die Aufgaben des Kapitels sind damit ohne Scrollen zu sehen.
- **WebApp:** Auf einem iPad bleibt das Bild nach dem Laden weiß, obwohl die App läuft (Ton kommt). Die Ursache ist noch nicht gefunden.
  Diese Version hilft beim Suchen: Die Diagnose-Seite zeigt jetzt, was die App beim letzten Start gemeldet hat, und bietet Test-Knöpfe,
  die Raumlicht ohne einzelne Teile starten (3D-Bühne, Schriften). Alles bleibt auf dem Gerät.
- Sonst keine Änderungen.

## 2.9.1 – Emojis bleiben von selbst vollständig (16.11.2026)
- **WebApp:** Kommt ein neues Emoji oder Sonderzeichen in die Texte, landet es beim nächsten Bau automatisch in der Schrift. Zusätzlich prüft GitHub vor jeder Auslieferung,
  dass kein benutztes Zeichen fehlt – sonst geht die WebApp nicht auf die Testadresse.
- Behoben: Im Show-Baukasten stand im Browser ein Kästchen statt des Pluszeichens.

## 2.9.0 – Schneller Start mit Ladebild (16.11.2026)
- **WebApp startet viel schneller:** Beim ersten Öffnen werden nur noch rund 7,7 MB statt 30 MB übertragen. Gemessen im schnellen Mobilfunknetz: 5,8 statt 27,9 Sekunden,
  im langsamen: 29 statt 153 Sekunden. Jeder weitere Start dauert gut 2 Sekunden und braucht kein Netz.
- **Ladebild statt leerem Bildschirm:** Sofort erscheinen Nachtblau, Lux und der Name – dazu ein Balken, der den echten Download zeigt. Danach blendet die App weich ein.
- **Wie:** Die Emoji-Schrift enthält nur noch die Emojis, die Raumlicht benutzt (0,2 MB statt 10,8 MB), der App-Kern wird sofort angefordert,
  die 3D-Bühne erst danach, und der Zwischenspeicher lädt nichts mehr doppelt.
- **Android:** Startbild in App-Farbe mit dem Raumlicht-Symbol, bis der erste Bildschirm steht.

## 2.8.0 – Binomialverteilung, e-Funktion und Logarithmus (16.11.2026)
- **📊 Binomial-Labor (unter „Mehr“):** n, p und k frei einstellen und sofort sehen, wie die Verteilung aussieht. Drei Fragen: genau, höchstens, mindestens k Treffer.
  Die Säulen zeigen wahlweise die einzelnen Wahrscheinlichkeiten P(X = k) oder die aufsummierten (kumulierten) P(X ≤ k). Säule antippen wählt k;
  dazu stehen der Ansatz wie in der Klausur, Erwartungswert und Streuung – und Lux übersetzt die Zahl in Alltagssprache.
- **Neues Kapitel „Binomialverteilung“** (5 Aufgaben): genau, höchstens, mindestens, „von … bis“ mit der kumulierten Tabelle, kleinstes k für 90 % Sicherheit.
- **Neues Kapitel „e-Funktion & Logarithmus“** (7 Aufgaben): Gleichungen mit e und ln, Kurvendiskussion mit Produkt- und Kettenregel (Tiefpunkt, Wendestelle),
  Tiefpunkt mit ln, Integrale mit e^x und 1 : x.
- **Kurve abfahren:** zwei neue fertige Bahnen – „e-Buckel“ (4x · e^(−x)) und „Logarithmus“ (ln x). Eigene Funktionen mit e^x, exp und ln gingen schon vorher.
- **WebApp:** Startet sie auf einem Gerät nicht, steht jetzt eine verständliche Meldung da, mit Link zu einer Diagnose-Seite. Auf großen Bildschirmen braucht die 3D-Bühne weniger Grafikspeicher.
- Behoben: Leuchtflächen unter nach oben gewölbten Kurven füllten über die Kurve hinaus.
- Jetzt 136 Aufgaben in 25 Kapiteln. Alle neuen Lösungen sind unabhängig nachgerechnet.

## 2.7.0 – Sechs neue Kapitel und zwei Abi-Aufgaben (16.11.2026)
- **35 neue Aufgaben in sechs Kapiteln** – jede mit Tipp, Rechenweg zum Nachlesen und eigener 3D-Bühne:
  - **Ebenen in allen Formen:** Parameterform umwandeln, Normalenform lesen, Spurpunkte, Achsenabschnittsform, Punktprobe, Ebene aus Gerade und Punkt
  - **Schnitte & Schnittwinkel:** Winkel zwischen Ebenen und zwischen Gerade und Ebene, Schnittgerade, parallele Gerade, Abstand Gerade–Ebene
  - **Spiegeln & Lote:** Spiegeln an Punkt, Gerade und Ebene, Abstand windschiefer Geraden
  - **Kugel & Ebene:** Berühren, Berührpunkt, Schnittkreis (Mittelpunkt und Radius), zwei Kugeln, Flug durch die Kugel
  - **Integrale & Flächen:** Fläche unter Kurven, zwischen zwei Kurven, Vorzeichen, obere Grenze gesucht, Bestand aus Änderungsrate
  - **Erwartung, Sigma, Testen:** fairer Einsatz, „höchstens einmal“, Mindestanzahl an Versuchen, Sigma-Regel, Hypothesentest
- **Zwei neue Prüfungsaufgaben im Abi-Stil** (je vier Teilaufgaben, 12 Bewertungseinheiten): „Planetarium“ (Geometrie) und „Einlass“ (Analysis).
- Damit hat Raumlicht jetzt 124 Aufgaben in 23 Kapiteln und 7 Prüfungsaufgaben.
- **Geprüft:** Jede Lösung ist unabhängig nachgerechnet (auch Integrale und der Hypothesentest), jede Zahlen-Aufgabe hat genau eine richtige Antwort.
- Lux kann alle neuen Texte auch mit der natürlichen Stimme sprechen. Gilt für Android und die WebApp.

## 2.6.0 – Bereit für den Play Store (16.11.2026)
- **Natürliche Stimme ohne fremden Sprach-Baustein:** Die Stimme „Thorsten“ klingt wie bisher, aber die App bringt die nötige Lautschrift jetzt selbst als Daten mit.
  Der bisher enthaltene Baustein espeak-ng (mit strenger GPL-Lizenz) ist damit entfernt. Kennt Lux ein Wort einmal nicht, liest diesen einen Satz die Handy-Stimme.
- **Zwei Fassungen der App:** Die APK von GitHub bleibt wie sie ist (mit eigenem Update-Knopf). Neu ist eine Fassung für den Play Store:
  ohne Berechtigung „Apps installieren“, Updates kommen dort über den Store – im Hintergrund, eingespielt beim nächsten App-Start.
- **Store-Unterlagen:** Texte, Grafiken, Bildschirmfotos und eine Klick-Liste liegen in `STORE-RELEASE.md` und `store/`. Veröffentlicht ist noch nichts.
- Für alle, die die natürliche Stimme schon geladen haben: Sie bleibt, es muss nichts neu geladen werden.

## 2.5.1 – Musik im Browser, Lux erst einmal still (16.11.2026)
- **🎵 Musik auch im Browser:** Die WebApp spielt jetzt dieselbe im Code erzeugte Hintergrundmusik wie die Android-App – ruhig im Menü, flotter bei Fahrt und Flug.
  Sie beginnt nach der ersten Berührung (das verlangen Browser), ist in den Einstellungen abschaltbar und folgt dem Lautstärke-Regler.
- **🗣️ Lux spricht nur noch auf Wunsch:** Das Vorlesen ist überall ausgeschaltet – in der Android-App und im Browser.
  Wer es mag, schaltet es in den Einstellungen unter „Lux spricht“ wieder ein. Die Sprechblasen zum Lesen bleiben wie gewohnt.

## 2.5.0 – Raumlicht im Browser (16.11.2026)
- **🌐 Die ganze App läuft jetzt auch im Browser:** Startseite, alle Kapitel mit Erklärungen und Rechenweg, Tagesaufgabe, Üben, Abi-Aufgaben,
  Kurve abfahren, Flug durch den Raum, Ballerbude, Baumeister, Geisterfahrt, Ringflug, Zufallsmaschine, Galton-Brett, Planer, Einstellungen –
  dieselben Bildschirme wie auf dem Handy, weil beide aus demselben Code entstehen. Testadresse: https://raumlicht-test.mathenachhilfe-online.com
- **Im Browser:** Sterne und Einstellungen bleiben im Browser auf dem Gerät gespeichert (keine Anmeldung, nichts geht an einen Server).
  Lux spricht mit der Sprachausgabe des Browsers, kurze Klänge kommen aus dem Browser. Die Zurück-Taste des Browsers schließt eine Ebene.
  Die WebApp aktualisiert sich von selbst.
- **Am PC:** Bühne mit der Maus ziehen zum Drehen, Mausrad zum Zoomen.
- **Noch nur in der Android-App:** Hintergrundmusik, natürliche Stimme „Thorsten“, Widgets für den Startbildschirm, Erfolgskarte als Bild teilen.
- **Android:** keine sichtbaren Änderungen – die App nutzt jetzt denselben gemeinsamen Code wie die WebApp.

## 2.4.1 – App bleibt offen (15.11.2026)
- **Kein „Wegklappen“ mehr durch die 3D-Bühne:** Wenn Android die Darstellung der Bühne beendet (meist bei knappem Speicher), ging bisher die ganze App zu.
  Jetzt bleibt die App offen und die Bühne baut sich an derselben Stelle neu auf.
- **Abbruch-Bericht:** Geht die App doch einmal unerwartet zu, steht beim nächsten Öffnen in den Einstellungen, was passiert ist.
  Der Bericht bleibt auf dem Gerät; man kann ihn kopieren und weitergeben.

## 2.4.0 – Natürliche Stimme für Lux (15.11.2026)
- **✨ Natürliche Stimme „Thorsten“:** Auf Wunsch spricht Lux mit einer deutlich natürlicheren Stimme, die ganz auf dem Handy rechnet – ohne Internet,
  nichts wird verschickt. In den Einstellungen unter „Lux spricht“ einmalig laden (63 MB, am besten im WLAN), danach ein- und ausschalten oder wieder entfernen.
- Ohne Download bleibt alles wie bisher: Dann spricht die Stimme des Handys.
- Der Download wird per Prüfsumme kontrolliert; ein abgebrochener oder beschädigter Download hinterlässt nichts.
- Die App selbst ist durch die Sprach-Bibliothek größer geworden.
- Für Eltern und Weitergabe: Die Sprach-Bibliothek enthält einen Baustein unter GPL-3 (siehe `LIZENZEN.md`).

## 2.3.3 – Ruhe bei schneller Fahrt (15.11.2026)
- **Kurve abfahren:** Kommen Hochpunkte, Nullstellen und Wendepunkte dicht hintereinander (hohes Tempo, enge Kurve), wird es nicht mehr hektisch:
  Dann gibt es nur einen leisen Tick und das Schild. Treffer-Ton, Lichtblitz und Ansage kommen erst wieder, wenn knapp zwei Sekunden Ruhe war.
- Lux fällt sich bei diesen Ansagen nicht mehr selbst ins Wort, und bei Tempo 2× und 4× sagt sie nur noch kurz „Hochpunkt“ statt den ganzen Satz.

## 2.3.2 – Lux klingt angenehmer (15.11.2026)
- **Natürlichere Stimme:** Lux spricht jetzt in natürlicher Tonlage und etwas ruhiger. Die stark erhöhte „Drohnen“-Lage ließ die Stimme blechern klingen.
- **Beste Stimme des Geräts:** Unter den deutschen Stimmen, die ohne Internet auskommen, wird die hochwertigste gewählt.
- **Stimme aussuchen:** In den Einstellungen unter „Lux spricht“ kannst du durch die Stimmen des Geräts schalten (jede stellt sich vor) und die Lage wählen
  (natürlich oder hell). Dort steht auch, wo man in Android bessere Stimmen nachlädt.
- Klammern und Gedankenstriche werden als kurze Pausen gesprochen statt mitgelesen.

## 2.3.1 – Fehlerkorrekturen nach dem Durchprüfen (15.11.2026)
- **Ballerbude mit Aufgabe:** Nach dem Regelwechsel wurde weiter nach der ersten Regel gewertet – jetzt zählt immer die Regel, die gerade gilt.
- **Ballerbude zu zweit:** Tippen beide gleichzeitig, zählen jetzt beide Treffer (vorher ging beides verloren).
- **Ballerbude und Baumeister:** Die Bühne dreht sich dort nicht mehr mit, wenn man danebengreift. Tippen mit leicht wackelndem Finger wird sicherer erkannt.
- **Baumeister:** Nach einer Fahrt in der Innenansicht geht der Blick wieder nach außen, damit man die Stützpunkte sieht.
- **Zufallsmaschine:** Das Feld für die eigene Chance lässt sich leeren und neu beschreiben.
- **Lux' Stimme:** startet auf allen Geräten zuverlässig; die Einstellungen melden „keine deutsche Stimme“ erst, wenn das wirklich feststeht.

## 2.3.0 – Baumeister, Geisterfahrt, Ringflug (15.11.2026)
- **🏗️ Achterbahn-Baumeister (neu):** Fünf Stützpunkte hoch- und runterziehen, die App legt eine glatte Bahn hindurch – dann Probefahrt mit echter Physik.
  Bergab wird der Wagen schneller, bergauf langsamer; ist ein Hügel höher, als der Schwung reicht, rollt er zurück. Fünf Aufträge (Sterne einsammeln,
  mit Tempo ins Ziel) und freies Bauen.
- **👻 Geisterfahrt gegen f′ (neu):** Oben die Kurve, unten zeichnest du mit dem Finger die Ableitung. „Auflösen“ blendet die echte f′ ein und zeigt
  deine Trefferquote – grafisches Ableiten wie im Abi, nur als Zeichenspiel. Geht auch mit eigener Funktion.
- **⭕ Ringflug (neu):** Start und drei Ringe sind gegeben, du stellst den Richtungsvektor ein. Stimmt er nicht, fliegt Lux sichtbar vorbei
  und sagt, um wie viel. Jede Runde ist neu.

## 2.2.0 – Zufallsmaschine, Ballerbude mit Aufgabe und zu zweit (15.11.2026)
- **🎲 Zufallsmaschine (neu):** Münze, Würfel, Glücksrad oder eine eigene Trefferchance – 10 bis 10 000 Versuche in Sekunden.
  Erst wetten („Wie oft klappt es?“), dann zusehen, wie sich die relative Häufigkeit der Wahrscheinlichkeit nähert. Dazu Erwartungswert und Streuung.
- **🧮 Ballerbude mit Aufgabe:** Die Ballons tragen Zahlen, und nur was zur Regel passt, soll platzen – Primzahlen, Quadratzahlen,
  „Nullstellen von x² − 5x + 6“ … Alle 15 Sekunden kommt eine neue Regel. Falsche Zahl: −1 und die Serie ist weg.
- **👥 Ballerbude zu zweit:** Links gegen rechts am selben Handy – jedem gehört eine Hälfte.
- **Galton-Brett flexibel:** Die Zahl der Reihen lässt sich jetzt wählen (4 bis 12).

## 2.1.0 – Eigener Flug und Fehlschüsse (15.11.2026)
- **✏️ Eigener Flug:** In „Flug durch den Raum“ legst du jetzt selbst fest, wohin es geht – Startpunkt und Richtung der Geraden,
  bis zu zwei Ebenen (Normalenvektor und rechte Seite) und eine Kugel. Durchstoßpunkte, Ein- und Austritt werden berechnet und unterwegs gemeldet.
  Unpassende Eingaben werden erklärt; der letzte eigene Flug bleibt gespeichert.
- **Ballerbude:** Tippst du daneben, ballert Lux genau dorthin ins Leere – das kostet keine Punkte, aber die Serie.

## 2.0.0 – Lux spricht (15.11.2026)
- **🗣️ Lux kann sprechen:** Lux liest ihre Sprechblasen laut vor – den Auftrag, Rückmeldungen, Tipps, die Kapitel-Erklärung, jeden Schritt ihrer Aktionen
  („1. Zug: pink, 2 von 5 …“) und beim Kurvenfahren die besonderen Stellen („Hochpunkt bei x = 2“). Formelzeichen werden dabei als Wörter gesprochen.
- Die Stimme kommt von der Sprachausgabe des Geräts, etwas höher gestellt. Während Lux spricht, wird die Musik leiser.
- Abschaltbar in den Einstellungen unter „Ton & Gefühl“; die Lautstärke folgt dem Lautstärke-Regler. Ist auf dem Gerät keine deutsche Stimme eingerichtet, bleibt Lux stumm.
- Die Versionsnummer springt auf 2.0.0.

## 0.7.2 – Eigene Funktion leichter eingeben (15.11.2026)
- Bei „Eigene Funktion“ steht die Funktion jetzt als gut sichtbare Zeile über der Bühne: antippen („✏️ Funktion eingeben“) öffnet die Eingabe.
  Vorher war sie nur über den kleinen Knopf „ändern“ erreichbar.
- **Sichtbaren Abschnitt frei wählen:** „von“ und „bis“ lassen sich jetzt direkt eintippen (auch negativ und weit weg), zusätzlich zu − und +.
  Der gewählte Abschnitt steht in der Zeile über der Bühne.

## 0.7.1 – Ableitungen am richtigen Achsenkreuz (15.11.2026)
- **Fehler behoben:** Beim Einschalten von f′ rutschte die Kurve auf eine neue Höhe, das Achsenkreuz blieb aber stehen – Kurve, Ableitung und Zahlen passten nicht zusammen.
  Jetzt wandert das Achsenkreuz mit.
- **Alles unverzerrt an einer y-Achse:** f, f′ und f″ werden im selben Maßstab gezeichnet. Jeder Wert lässt sich direkt an der y-Achse ablesen
  (keine gestauchte Ableitung mehr).
- **Neu: f″ als dritte Bahn** (violett). Ein Lot zeigt: f′ kreuzt die x-Achse unter jedem Hoch- und Tiefpunkt, f″ unter jedem Wendepunkt.

## 0.7.0 – Ballerbude und saubere Ableitung (15.11.2026)
- **🎈 Ballerbude – nur zum Spaß:** 45 Sekunden lang steigen Leuchtballons auf. Antippen, und Lux ballert sie mit dem Laser ab.
  Treffer in Folge bauen eine Serie auf (alle fünf Treffer zählt jeder Ballon einen Punkt mehr), der Goldballon bringt 5 Punkte,
  der dicke orange Blindgänger kostet 2 und die Serie. Verpasste Ballons kosten nichts. Zu finden unter „Mehr“ → Erleben.
- **Ableitung passt jetzt zur Kurve:** f′ und f teilen sich dieselbe x-Achse. Unter jedem Hoch- und Tiefpunkt geht die Ableitungsbahn
  genau durch die Achse – ein gestricheltes Lot zeigt die Stelle. Die Achse rückt dafür so weit hoch, dass auch der negative Teil von f′ Platz hat
  (vorher hatte f′ teils eine eigene, verschobene Nulllinie). Muss f′ in der Höhe gestaucht werden, steht der Faktor am Schild.

## 0.6.0 – Eigene Funktionen abfahren (15.11.2026)
- **✏️ Eigene Funktion:** In „Kurve abfahren“ kannst du jetzt einen beliebigen Term eintippen – z. B. `x^3-3x`, `2sin(x)`, `e^(-x)*cos(3x)`, `√(x+1)` –
  und den Abschnitt von–bis wählen. Die App baut daraus die Bahn; der Maßstab passt sich an, das Achsenkreuz zeigt die echten Werte.
- **Wichtige Stellen automatisch:** Nullstellen, Hoch- und Tiefpunkte und Wendepunkte werden gesucht, beschildert und beim Vorbeifahren gemeldet.
  Höhe, Steigung, Krümmung, f′ als zweite Bahn und die Fläche funktionieren wie bei den fertigen Bahnen.
- **Tempo einstellen:** 🐢 ½×, 1×, 🚀 2× oder ⚡ 4× – für alle Bahnen.
- **Beliebig weit fahren:** „⏩ nächster Abschnitt“ hängt den nächsten gleich langen Abschnitt nahtlos an.
- Tasten für x, ^, ², Klammern, sin, cos, √, e^, ln, π. Tippfehler werden erklärt; Polstellen werden abgeschnitten und angezeigt.
  Die zuletzt gebaute Funktion bleibt gespeichert.

## 0.5.4 – Lux tut, wonach gefragt ist (15.11.2026)
- **Stochastik: Aktion passt zur Frage.** Nach der Lösung spielt Lux genau das nach, worum es ging, und sagt dazu, was passiert:
  - *mit Zurücklegen:* ziehen – zurücklegen – noch einmal ziehen; es bleiben immer alle Kugeln da.
  - *ohne Zurücklegen:* jede gezogene Kugel wird abgeschossen und bleibt draußen („nur noch 1 von 4“).
  - *ein Zug:* erst die günstigen Kugeln zeigen, dann eine ziehen.
  - *mindestens eins:* vier gleich wahrscheinliche Fälle – nur „keins“ fällt weg.
  - *Baumdiagramm:* Lux leuchtet genau die zwei passenden Pfade nach. *Pfade zählen:* alle sechs Möglichkeiten nacheinander.
  - *Vierfeldertafel / bedingte Wahrscheinlichkeit:* die Felder, die nicht mitzählen, verschwinden.
  - gilt auch für die Abi-Aufgabe „Lostrommel“.
- **Ton lauter und regelbar:** Musik und Geräusche sind ab Werk lauter; in den Einstellungen gibt es einen Lautstärke-Regler.

## 0.5.3 – Neu anfangen (15.11.2026)
- **🧹 Fortschritt zurücksetzen:** In den Einstellungen gibt es jetzt einen Knopf, der alles Bisherige löscht – Sterne, Rekorde, Abi-Punkte,
  Fehler-Tagebuch, Kalender und das freigespielte Aussehen von Lux. Vorher wird nachgefragt; Ton-Einstellungen und Klausur-Plan bleiben.

## 0.5.2 – Jedes Gebiet mit passender Bühne (15.11.2026)
- **Raumachsen nur noch bei Vektoren:** Das dreidimensionale Koordinatensystem x₁/x₂/x₃ gibt es nur noch in der Geometrie.
- **Analysis überall eben:** Auch die Rotationskörper haben jetzt das x-y-Achsenkreuz ihrer Kurve.
- **Stochastik auf der Showbühne:** Kugeln, Baumdiagramm, Vierfeldertafel und Galton-Brett stehen auf einer Bühne mit Leuchtringen und Lauflicht –
  ganz ohne Koordinatensystem.
- **Kurve abfahren:** In der Fahrersicht ist das Achsenkreuz ausgeblendet (es stand als Strich mitten im Blick); von außen bleibt es sichtbar.

## 0.5.1 – Funktionen mit echtem Achsenkreuz (15.11.2026)
- **Analysis lässt sich jetzt richtig ablesen:** Funktionsgraphen bekommen ein eigenes x-y-Achsenkreuz mit Zahlen in den Einheiten der Funktion
  (vorher standen dort die Raumachsen x₁/x₂/x₃ – Nullstellen und Schnittpunkte passten nicht zur Aufgabe).
- Der Blick geht bei Funktionen von vorn auf die Ebene; drehen und zoomen geht weiterhin.
- Kurven enden am Rand des Achsenkreuzes und werden nicht mehr am Boden abgeknickt.

## 0.5.0 – Das große Erlebnis-Paket (15.11.2026)
**Erleben**
- **🚀 Flug durch den Raum:** Geometrie aus der Fahrersicht – du fliegst eine Gerade entlang, durch Ebenen und die Discokugel. Das Cockpit zeigt Ort, Parameter t
  und den Abstand zur nächsten Ebene; an jedem Durchstoßpunkt meldet sich Lux.
- **🎢 Kurve abfahren – jetzt mit Ableitung und Fläche:** f′ läuft als zweite Bahn mit (Gipfel oben = Nulldurchgang unten), und die Fläche unter der Kurve
  füllt sich während der Fahrt mit Licht; unter der Achse wird abgezogen.
- **🎰 Galton-Brett:** Kugeln fallen durch das Nagelbrett, in den Fächern wächst die Binomialverteilung. Die Wahrscheinlichkeit p lässt sich verstellen.
- **🛠️ Funktionen bauen (neues Kapitel):** Gerade und Parabel mit Reglern so formen, dass die Bahn durch alle Tore führt – bis zur Steckbriefaufgabe.
- **Lichtshow im Takt:** Im Baukasten feuern die Laser jetzt im Rhythmus.
- **Lux führt vor:** Beim ersten Öffnen eines Kapitels zeigt Lux in der Bühne, wie die erste Aufgabe gelöst wird.

**Abi-Vorbereitung** (jetzt 89 Aufgaben in 17 Kapiteln)
- **🌳 Mehr Stochastik (neues Kapitel):** Baumdiagramm, Pfade zählen, Vierfeldertafel, bedingte Wahrscheinlichkeit, Standardabweichung.
- **🧰 Ableiten & Anwenden (neues Kapitel):** Steigung im Punkt, Kettenregel, Tangentengleichung, größte Leinwand (Extremwert), Wachstum.
- **🎓 Zwei neue Abi-Aufgaben:** „Lichtbogen“ (Analysis) und „Lostrommel“ (Stochastik).
- **✍️ Ohne Hilfsmittel:** sechs kurze Aufgaben quer durch alle Gebiete, ohne Tipp – wie im Pflichtteil.
- **📔 Fehler-Tagebuch:** Lux notiert deine typischen Fehler und zeigt, welche am häufigsten vorkommen – mit Knopf zum gezielten Üben.

**Drumherum**
- **🎟️ Kalender:** zeigt, an welchen Tagen du die Tagesaufgabe gelöst hast. Verpasste Tage kosten nichts.
- **🎨 Lux gestalten:** Leuchtspur (Funken, Regenbogen) und Aufsätze (Antenne, Lichtring, Krone), freigeschaltet durch Erfolge.
- **Zweites Widget „Lernstand“:** drei Balken für Geometrie, Analysis und Stochastik.
- **📤 Ergebnis-Karte:** nach einer Abi-Aufgabe ein Bild mit deinen Punkten zum Teilen.
- **Seite „Mehr“ in Gruppen:** Erleben, Üben, Planen, Gemeinsam, Werkzeuge, Für dich.
- Formelsammlung mit neuen Karten für Ableitung und Wahrscheinlichkeit.

## 0.4.3 – Mehr Action (15.11.2026)
- **Lux feiert richtig:** Stimmt die Lösung, tut Lux etwas Passendes zur Aufgabe.
  - Stochastik: Lux schießt die gezogenen Kugeln ab – sie zerplatzen in Funken.
  - Kurvendiskussion: Lux fährt die ganze Bahn ab und hält an der gesuchten Stelle.
  - Winkel, Flächen, Volumen, Kugeln, Ebenen: Lux blitzt nacheinander die wichtigen Punkte an.
  - Danach dreht Lux eine Jubel-Spirale mit Salto.
- Bei einem Fehlversuch schüttelt Lux kurz den Kopf, und ein Fehlschuss verpufft in kleinen Funken.
- **Behoben:** Beim Einstellen von Prozent- und Winkelwerten blitzte kurz eine falsche Zahl auf (z. B. „3“ statt „30 %“).

## 0.4.2 – Finden statt suchen (15.11.2026)
- **Drei Gebiete wie im Abitur:** Die Startseite ist in **Geometrie, Analysis und Stochastik** aufgeteilt. Ein Tipp auf das Gebiet zeigt alle Kapitel dazu mit Namen.
- **Namen, die sagen, was drin ist:** Kapitel heißen jetzt nach ihrem Thema („Geraden“, „Ebenen“, „Kurvendiskussion“ …) statt nach der Kulisse.
  Aus „Gig“ wird „Aufgabe“, aus „Tagesgig“ die „Tagesaufgabe“, aus „Jam-Session“ „Endlos üben“, aus „Backstage“ „Mehr“,
  aus „Spickzettel“ die „Formelsammlung“, aus „Zweite Chance“ „Fehler wiederholen“, aus „Abzeichen“ „Erfolge“.
- Lernstand und Klassen-Paket sind ebenfalls nach Gebieten geordnet.
- **Aufgabe wieder anzeigen:** Steht ein Tipp oder eine Rückmeldung in der Sprechblase, holt „📋 Aufgabe zeigen“ (oder ein Tipp auf die Sprechblase) die Aufgabenstellung zurück.
- Der Spielstand bleibt vollständig erhalten.

## 0.4.1 – Einsteigen bitte (15.11.2026)
- **🎢 Achterbahn aus der Fahrersicht:** Der Graph ist die Bahn – du sitzt im Wagen. Schienen und Schwellen laufen auf dich zu,
  bergab wird es schneller, bergauf langsamer.
- **Cockpit mit Live-Daten:** Stelle x, Höhe f(x), Steigung f′(x) mit Pfeil, Wort und Winkel in Grad, Krümmung f″(x) als Links- oder Rechtskurve.
- **Besondere Stellen melden sich:** An Hoch-, Tief- und Wendepunkten und an Nullstellen erscheint Lux mit der passenden Regel („f′ = 0 und f″ < 0“).
- Selbst fahren lassen oder mit dem Regler vor- und zurückschieben; jederzeit zwischen Fahrersicht und Blick von außen wechseln.
- Fünf Bahnen: Sanfter Hügel, Berg und Tal, Doppelkurve, Schüssel, Welle (Sinus).
- Zu finden unter Backstage → „Achterbahn fahren“ und in Kapitel 14 über den Knopf „🎢 Mitfahren“ in der Bühne.

## 0.4.0 – Mehr als Geometrie (15.11.2026)
- **Lux erkennt typische Fehler:** falsch herum abgezogen, nur addiert statt gemittelt, Kosinus statt Sinus, das Drittel vergessen …
  Statt „daneben“ sagt Lux jetzt genau, was passiert ist.
- **🧩 Schritt für Schritt:** vier schwere Verfahren (Durchstoßpunkt, Spiegelpunkt, Schnittpunkt, Abstand windschiefer Geraden) in kleinen Schritten,
  mit Rückmeldung nach jedem Schritt.
- **2 neue Touren, 10 neue Gigs** (jetzt 75 in 14 Kapiteln):
  - 🎲 **Stochastik** – Leuchtkugeln ziehen: Laplace, Pfadregel mit und ohne Zurücklegen, Erwartungswert, Gegenereignis.
  - 🎢 **Analysis** – Lux fährt die Achterbahn ab: Nullstelle, Hoch- und Tiefpunkt, Wendestelle, Fläche. Die Tangente kippt live mit.
- **🧮 3D-Rechner im Baukasten:** Punkte, Geraden, Ebenen und Kugeln aus dem Schulbuch eingeben – die App rechnet Lagebeziehung, Schnitt- und Durchstoßpunkte,
  Abstände und Winkel aus.
- **📊 Lernstand:** pro Thema ein Balken – sicher, wackelig oder offen.
- **🗓️ Klausur-Planer:** Termin und Kapitel eintragen, die App teilt den Stoff in Tagespakete auf. Das Widget zeigt „Klausur in 4 Tagen · heute 3 Gigs“.
- **🏫 Klasse:** Aufgabenpaket als Code für den Unterricht – ohne Konto, ohne Namen.
- **👥 Zu zweit:** abwechselnd an einem Gerät, Punkte nur für Treffer beim ersten Versuch.
- **Querformat und Tablet:** Die Bühne steht links groß, Formel und Regler rechts. Die App dreht sich jetzt mit.

## 0.3.3 – Alles auf einen Blick (15.11.2026)
- **Startseite ohne lange Liste:** Die zwölf Kapitel sind jetzt eine schmale Leiste zum seitlichen Wischen. Darunter steht nur das gewählte Kapitel
  mit seinen Gigs. Die ganze Startseite passt auf einen Bildschirm.
- Die Leiste springt beim Öffnen zu dem Kapitel, in dem es weitergeht (grün umrandet).

## 0.3.2 – Feinschliff (15.11.2026)
- **Abi-Simulation auf Zeit:** freiwilliger Schalter – 9 Minuten je Aufgabe, die Uhr läuft oben mit. Ist die Zeit um, zählt, was bis dahin gelöst ist.
- **Jam-Session „Bewegung & Rotation“:** neue Zufallsaufgaben zu Ort nach t Sekunden, Tempo, Flughöhe und Rotationskörpern – auch in „Alles gemischt“.
- Vom Spickzettel geht es bei Bewegung und Rotationskörpern jetzt auch direkt in die Jam-Session.
- Die Bühne hat wieder runde Ecken.

## 0.3.1 – Widget (15.11.2026)
- **Neu: Widget für den Startbildschirm.** Es zeigt den Tagesgig, ob er heute schon geschafft ist, und deine Sterne. Ein Tipp öffnet die Aufgabe.
  Hinzufügen: lange auf den Startbildschirm drücken → Widgets → Raumlicht.
- Das Widget braucht kein Internet und liest nur den Spielstand auf dem Gerät.

## 0.3.0 – Backstage (15.11.2026)
- **🎓 Abi-Simulation:** drei zusammenhängende Aufgaben wie im Abitur (Festivalzelt, Laser & Leinwand, Drohnen-Show), je vier Teilaufgaben
  mit Bewertungseinheiten. Volle Punkte gibt es nur beim ersten Versuch, am Ende steht eine Auswertung.
- **2 neue Kapitel, 10 neue Gigs** (jetzt 65 in 12 Kapiteln):
  - 🛫 **Bewegung** – der Parameter wird zur Zeit: Ort nach t Sekunden, Tempo, Treffpunkt, Beinahe-Kollision. Die Drohnen wandern mit, wenn man an der Zeit dreht.
  - 🏺 **Rotationskörper** – Analysis in 3D: Eine Kurve dreht sich zur Vase, zum Kegel, zur Kugel. Volumen mit dem Integral.
- **🧭 Lage-Check:** acht schnelle Runden „parallel, identisch, schneidend oder windschief?“ – erst entscheiden, dann zeigt die Bühne die Geraden.
- **⚔️ Duell:** fünf Aufgaben als kurzer Code zum Verschicken. Wer den Code eingibt, bekommt genau dieselben Aufgaben.
- **🔁 Zweite Chance:** spielt noch einmal die Gigs, die nicht beim ersten Versuch geklappt haben.
- **🎇 Show-Baukasten:** eigene Lichtshow aus Lasern, Scheiben und Kugeln bauen – mit Formeln statt mit dem Finger – starten und als Code teilen.
- **🎨 Lux-Werkstatt:** sechs Leuchtfarben für Lux, freigeschaltet durch Sterne.
- **🎧 Jam-Session** kann jetzt auch Winkel, Abstände, Lotfußpunkte und Kugelradien.
- **📓 Spickzettel:** Von jeder Formelkarte geht es direkt in das passende Kapitel oder in die Jam-Session. Neue Karten für Bewegung und Rotationskörper.
- Die Startseite bleibt aufgeräumt: Alles Weitere liegt hinter „Backstage“.
- In den Einstellungen steht jetzt das Datum der Version.

## 0.2.1 – Aufgeräumt (15.11.2026)
- **Startseite übersichtlicher:** Die Kapitel sind jetzt eine kompakte Liste mit Fortschrittsbalken. Aufgeklappt ist nur das Kapitel, in dem es weitergeht –
  die anderen öffnen sich beim Antippen.
- Tagesgig, Jam-Session, Spickzettel und Abzeichen stehen als schmale Leiste direkt unter der Bühne.

## 0.2.0 – Die große Tour (15.11.2026)
- **4 neue Kapitel, 21 neue Gigs** (jetzt 55 in 10 Kapiteln):
  - 📐 **Winkel** – zwischen Vektoren, Gerade und Ebene, zwei Ebenen. Der Winkelbogen leuchtet in der Bühne.
  - 📏 **Abstände** – Lot auf eine Gerade, parallele Ebenen, windschiefe Geraden, nächster Punkt einer Flugbahn.
  - 🔮 **Kugeln** – eine Discokugel aus Glas in 3D: Radius, Mittelpunkt, Laser trifft Kugel, Tangentialebene, Schnittkreis.
  - 🎆 **Kreuzprodukt** – Flächen von Lichtsegeln, Volumen von Zelten, Schnittgerade zweier Ebenen.
- **🎧 Jam-Session:** endlos neue Aufgaben mit immer anderen Zahlen – nach Thema oder bunt gemischt wie in der Klausur.
  Wer beim ersten Versuch trifft, baut eine Serie auf.
- **📅 Tagesgig:** jeden Tag eine neue Aufgabe.
- **📓 Spickzettel:** alle Formeln der analytischen Geometrie zum Nachschlagen.
- **🏅 Abzeichen:** 14 Auszeichnungen für Können – nicht fürs bloße Wiederkommen.
- Zahlen-Aufgaben (Winkel, Abstand, Fläche, Volumen) sagen nach einem Fehlversuch, ob das Ergebnis größer oder kleiner ist.

## 0.1.3 – Bühne sichtbar (15.11.2026)
- **Behoben:** Die 3D-Bühne blieb dunkel, weil sie intern keine Höhe bekam. Jetzt füllt sie ihre Fläche.
- Hat die Bühne trotzdem einmal keine Größe, meldet sie das.

## 0.1.2 – Fehlersuche Bühne (15.11.2026)
- Die 3D-Bühne wird im Level nicht mehr mit runden Ecken beschnitten – auf manchen Handys blieb sie dadurch leer.
- Die Bühne prüft sich selbst: Bleibt das Bild einfarbig, schaltet sie auf einfaches Zeichnen um und meldet sonst die Ursache.
- Dreimal oben rechts auf „drehen · zoomen“ tippen zeigt technische Angaben zur Bühne (für die Fehlersuche).

## 0.1.1 – Bühne an (15.11.2026)
- **Behoben:** Auf manchen Handys blieb die 3D-Bühne schwarz. Ursache war ein Fehler in der Licht-Berechnung, den nur echte Handy-Grafikchips zeigen.
- Kann ein Gerät den Leucht-Effekt nicht darstellen, zeichnet die Bühne jetzt automatisch ohne ihn – sichtbar bleibt sie immer.
- Falls die Bühne doch einmal nicht startet, steht jetzt eine Meldung da statt einer schwarzen Fläche.

## 0.1.0 – Erste Tour (15.11.2026)
- **Neu: Raumlicht** – analytische Geometrie der Oberstufe als Lichtshow in echtem 3D. Du bist Lichtdesigner:in:
  Laser sind Geraden, Glasscheiben sind Ebenen, die Drohne Lux fliegt deine Vektoren.
- 34 Gigs in 6 Kapiteln: Punkte im Raum, Vektoren, Geraden, Lagebeziehungen, Ebenen, Skalarprodukt/Abstand/Spiegelung.
  Jedes Kapitel endet mit einem **Abi-Check** im Stil echter Prüfungsaufgaben.
- Die Bühne lässt sich mit dem Finger drehen und zoomen. Punkte und Vektoren zieht man direkt im Raum oder stellt sie mit Zahlen ein –
  die Formel (mit Spaltenvektoren wie im Heft) ändert sich live mit.
- Wer rechnet statt rät, trifft beim ersten Versuch: 3 Sterne. Lux sagt nach einem Fehlversuch genau, was schon stimmt.
- Nach jedem Gig steht der Rechenweg zum Nachlesen da. Ein Tipp hilft weiter (kostet einen Stern).
- Die App aktualisiert sich selbst: Sie fragt bei jedem Öffnen live nach einer neuen Version und bietet sie zum Installieren an (abschaltbar).
- Musik, Geräusche und Vibration sind abschaltbar. Keine Werbung, keine Anmeldung, der Spielstand bleibt auf dem Gerät.
