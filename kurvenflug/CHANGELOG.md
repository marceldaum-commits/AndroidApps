# Was ist neu in Kurvenflug?

Kurz und verständlich für Schüler und Eltern. Der Abschnitt der aktuellen Version erscheint automatisch
im GitHub-Release und in der App unter „App-Update“.

## 0.21.1 – Schneller Start
- **🚀 Startet fast doppelt so schnell**: Die Web-Version lädt beim ersten Öffnen nur noch gut 5 MB statt knapp 10 MB –
  vorher wurde für das Offline-Spielen alles ein zweites Mal geladen. Mit schnellem Handy-Netz ist Kurvenflug nach
  etwa 6 Sekunden spielbereit (vorher gut 10), bei langsamem Netz nach knapp 30 statt über 50 Sekunden.
  Ab dem zweiten Öffnen startet es in unter einer Sekunde – auch ganz ohne Internet.
- **📊 Echter Ladebalken**: Beim ersten Öffnen zeigt Volt, wie viel schon geladen ist („Lade … 54 %“).
- **🌌 Kein weißes Aufblitzen mehr**: Auf Android 12 und neuer startet die App direkt im dunklen Weltall-Blau.

## 0.21.0 – Bereit für Google Play
- **📦 Kurvenflug kommt in den Play Store**: Es gibt jetzt eine eigene Play-Store-Version – ohne Internet, ohne eigenen
  Update-Knopf. Updates kommen dort von Google Play direkt in der App und werden im Hintergrund installiert.
- **🎲 Neues Kapitel Binomialverteilung** (Kl. 10–13): Die Sonde fliegt über die Säulen eines Histogramms – Dichte P(X = k)
  und kumuliert P(X ≤ k). n und p so einstellen, dass die Säulen die Stationen treffen: Münzwurf, Glücksrad,
  Erwartungswert, „mindestens einer“, Treppe bis 100 %.
- **📜 Open-Source-Lizenzen**: In den Einstellungen steht jetzt, welche freie Software in Kurvenflug steckt.
- Die Version aus GitHub (APK) bleibt wie sie ist, mit dem bekannten Update-Knopf.

## 0.20.0 – Viel Neues zum Rechnen
- **38 neue Level in 7 neuen Kapiteln** – jede Aufgabe mit Tipp, Rechenweg, Rechen-Frage und Live-Rechnung:
  - **🗺️ Maßstab & Karten** (Kl. 5–6): echte Entfernungen in Kästchen umrechnen – bis zur Sternenkarte in Lichtjahren.
  - **💶 Prozent & Zinseszins** (Kl. 7–8): + 25 % heißt mal 1,25. Sparschwein, Rabatte, Algen, Zinsen.
  - **🚗 Beschleunigte Bewegung** (Physik 9–10): s = ½ · a · t² + v₀ · t – losdüsen, bremsen, Notbremsung.
  - **📊 Potenzfunktionen** (Kl. 10): S-Kurve, Hochzahl erkennen, Wurzelbogen.
  - **🌊 Verschobene Wellen** (Kl. 10–11): y = a · sin(b · (x − c)) + d – die Welle wandert.
  - **🏃 Waagerechter Wurf** (Physik 10): von der Klippe – auf Erde, Mond und Mars. Mit Klippe und Höhe im Bild.
  - **📈 e-Funktion** (Kl. 11–12): Wachstum, Zerfall, Abkühlen und Medikamente – mit ln umkehren.
- Geprüft: alle Level automatisch lösbar, ein Reglerschritt daneben trifft nicht (Raten reicht nicht), App und WebApp
  rechnen genau gleich.

## 0.19.0 – Oberstufe (Klasse 11–13)
- **🎓 Jetzt bis Klasse 13**: Bei der Klassenwahl gibt es 11, 12 und 13 – mit vier neuen Planeten:
  - **🏂 Ableitung & Tangente**: Die Sonde gleitet auf der Kurve und springt tangential ab. Die Steigung ist f'(x₀).
  - **⛰️ Extrempunkte**: Hoch-, Tief- und Wendepunkt – die Station ist versteckt, also: f'(x) = 0 rechnen!
  - **⛽ Integral**: Treibstoff = Fläche unter der Kurve. Beim Flug sieht man, wie die Fläche „verbraucht“ wird.
  - **🧭 Vektoren**: Mit dem Richtungsvektor in Parameterform x⃗ = P + t · v⃗ zur Station.
- Überall mit Live-Rechnung, Rechen-Stern und Lösungsweg. Brüche stehen exakt da (x³/3 statt 0,333x³).
- Die Aurora-Geschichte (Klasse 5–10) bleibt unverändert – fertige Schiffe bleiben fertig.

## 0.18.0 – Verstehen statt nur Schieben
- **🧮 Die Rechnung läuft mit**: Unter der Formel steht jetzt die Rechnung mit deinen Zahlen – z. B.
  „17 · tan 13° ≈ 3,9 · Station: 4 ↑“. So siehst du beim Schieben, was die Zahl bedeutet und warum sie passt.
- **🧠 Rechen-Stern**: Nach dem Treffer fragt Volt: „Wie hast du's gerechnet?“ Wer die richtige Rechnung erkennt,
  bekommt den 3. Stern. Danach zeigt Volt den ganzen Rechenweg mit den Zahlen des Levels. Daneben getippt? Kein
  Problem – der Stern wartet beim nächsten Mal.
- **📋 Aufgabe bleibt sichtbar**: Nach einem Fehlversuch steht die Aufgabe weiter unter Volts Tipp.

## 0.17.1 – WebApp bleibt von selbst aktuell
- **🔄 Automatische Updates im Browser**: Die WebApp holt neue Versionen selbst – ohne Knopf. Wer sie gerade öffnet,
  bekommt sofort die neue Version. Wer mitten im Spiel ist, wird nicht unterbrochen: Gewechselt wird erst, wenn die App
  kurz im Hintergrund war. Der Spielstand bleibt dabei erhalten.

## 0.17.0 – Kurvenflug im Browser
- **🌐 Jetzt auch als WebApp**: Kurvenflug läuft im Browser – am Handy, Tablet und PC, ganz ohne Installation.
  Gleiche Level, gleiche Geschichte, gleiche Version wie die App. Auf dem Handy lässt sie sich wie eine App auf den
  Startbildschirm legen und funktioniert danach auch ohne Internet.
- **Datensparsam**: Auch im Browser gibt es keine Anmeldung und keine Werbung. Der Spielstand bleibt nur in diesem Browser
  gespeichert, nichts geht an einen Server.
- **Unter der Haube**: App und WebApp teilen sich jetzt dieselbe Spiellogik und dieselben Bildschirme. Ein Test prüft bei
  jeder Version, dass beide jede Aufgabe genau gleich rechnen. Geteilte Codes (Level, Challenges, Ergebnisse)
  funktionieren zwischen Handy und Browser.

## 0.16.1 – Reagiert schneller
- **Tippen wirkt sofort**: Nach dem Zurückgehen hat die App fast eine Sekunde lang Tipps verschluckt (wegen einer langen
  Überblend-Animation). Jetzt reagiert jeder Knopf gleich.
- **Zurück-Taste**: Auf „Galaxie“, „Spiele“ oder „Ich“ springt sie jetzt zu „Heute“, statt die App zu schließen.
  Im Level schließt sie zuerst den Rechenweg bzw. Volts Erklärung.
- **Stabiler**: Wenn das Handy gerade keine Musik abspielen kann, läuft die App jetzt einfach still weiter, statt abzustürzen.

## 0.16.0 – Zuhause im All
- **🏠 Deine Raumstation**: Jeder Stern, den du sammelst, wird zu Sternenstaub ✨. Damit schaltest du Räume frei –
  Labor, Garten, Werkstatt, Sternwarte, Lounge – und klebst deine Sticker an die Wände. Deine gerettete Crew zieht ein
  und läuft dort herum. (🎒 Ich → Station)
- **🎃 Gast-Planeten**: Zu Halloween, in der Adventszeit, rund um Ostern und in den Sommerferien kommt ein besonderer
  Planet zu Besuch – 5 Etappen und ein Themen-Sticker. Keine Sorge: Er kommt jedes Jahr wieder.

## 0.15.0 – Andromeda
- **🌌 Eine zweite Galaxie**: Ist die Aurora fertig, fliegt Volt mit dir nach Andromeda. Jeder Planet dort hat
  3 schwerere Etappen – mehr Asteroiden, kniffligere Zahlen.
- **🚀 Die Nova**: Jeder geschaffte Andromeda-Planet bringt ein Teil für das zweite Schiff. 18 Teile, dann fliegt sie!
- **Endlos**: Andromeda hört nicht auf – hinter jedem Planeten wartet ein neuer. Wie weit kommst du?

## 0.14.0 – Staffel 2: Gold & Crew
- **✨ Gold-Teile**: Die Aurora ist fertig? Jetzt wird sie golden! Schaffst du alle Level eines Kapitels mit 3 ⭐, glänzt
  sein Bauteil golden. Mit allen 18 Gold-Teilen zündet der Warp-Antrieb.
- **🆘 Rettungsmissionen**: 8 Freunde von Volt sind in der Galaxie gestrandet – eine Piratin, ein Physik-Professor,
  eine Mond-Katze und mehr. Jede Rettung: 5 Etappen mit Aufgaben aus ganz verschiedenen Themen. Kein Zeitdruck,
  kein Scheitern – probier so oft, wie du willst.
- **🎖️ Deine Crew**: Gerettete Freunde ziehen ein und sagen jeden Tag etwas auf der Startseite – Tipps, Witze, Lob.
- Die ersten Notrufe kommen schon unterwegs (ab 3 Bauteilen), die letzten erst mit fertiger und goldener Aurora.

## 0.13.0 – Gemeinsam spielen
- **👥 Mehrere Spieler**: Geschwister teilen sich das Handy? Jeder bekommt einen eigenen Spielstand mit Spitzname und Bild
  (🎒 Ich → Spieler). Wechseln mit einem Tipp. Bis zu 6 Spieler. Dein bisheriger Spielstand bleibt natürlich erhalten.
- **👪 Für Eltern**: Eine Wochenübersicht zeigt, an wie vielen Tagen und wie lange geübt wurde, welche Themen dran waren,
  wo Hilfe gut täte und was richtig gut läuft. Ohne Bewertung – und alles bleibt auf dem Gerät.
- **🏆 Bestenliste für die Klasse**: Nach der Klassen-Challenge schicken Schüler ihr Ergebnis mit einem Tipp an die Lehrkraft.
  Die fügt alle Nachrichten im Baukasten unter „Ergebnisse sammeln“ ein und bekommt eine Bestenliste – mit Prüfcode gegen
  geschummelte Ergebnisse. Ganz ohne Anmeldung oder Server.

## 0.12.0 – Volt wird lebendig
- **🤖 Volt spricht**: Er plappert jetzt in Roboter-Sprache, wenn er etwas sagt. (Einstellungen → „Volt-Stimme“ zum Abschalten.)
- **😴 Volt hat Launen**: Abends ist er müde und schlägt vor, bald ins Bett zu gehen. Nach ein paar Tagen Pause freut er sich
  riesig, dass du wieder da bist. Und bei 3 ⭐ macht er einen Freudentanz!
- **📒 Sticker sammeln**: Nach geschafften Leveln gibt es manchmal einen Überraschungs-Sticker – 24 Stück, manche selten oder
  super-selten, und ganz selten mit Glitzer ✨. Mit 3 ⭐ öfter. Das Album findest du unter 🎒 Ich.
- **📸 Fotomodus**: Deine schönste Flugbahn als Bild teilen – direkt nach dem Treffer.

## 0.11.0 – Die Reise der Aurora
- **🛸 Eine Geschichte für die ganze Galaxie**: Volts Mutterschiff, die Aurora, ist in einem Meteoritensturm zerbrochen.
  Auf jedem der 18 Planeten treibt ein Bauteil – schaffst du ein Kapitel ganz, bringst du es zurück!
- **🪐 Sternenkarte**: Die Galaxie ist jetzt eine echte Karte mit Planeten auf einem leuchtenden Pfad. Deine Rakete schwebt
  dort, wo du gerade bist. Planeten deiner Klasse strahlen. (Die Liste gibt es weiter über „📋 Liste“.)
- **🔧 Hangar**: Hier wächst die Aurora Teil für Teil zusammen. Mit allen 18 Teilen zünden die Triebwerke!
- Beim ersten Besuch eines Planeten verrät Volt, welches Bauteil dort liegt.

## 0.10.0 – Vier neue Kapitel
- **🍕 Brüche & Kommazahlen (Klasse 5–6)**: Lande auf dem Zahlenstrahl bei 2½, ¾, 7/4 oder 3,7 – mit Vierteln und Zehnteln
  zum Ablesen. Die App zeigt dir, dass 2,75 dasselbe ist wie 2¾. 8 Level.
- **📐 Winkel (Klasse 5–6)**: Ein Geodreieck am Laser! Rechte, spitze und stumpfe Winkel ablesen, Nebenwinkel und
  Winkel im Dreieck ausrechnen. 6 Level.
- **💎 Lichtbrechung (Physik Klasse 9)**: Glasplatte, Wasserbecken und Glasfaser – Licht knickt im Kristall ab
  (Brechungsgesetz) und bleibt bei flachem Winkel sogar ganz drin: Totalreflexion. 5 Level.
- **🎯 Nullstellen (Klasse 9)**: Wo landet die Sonde? Scheitel in der Mitte der Nullstellen, Normalform in Scheitelform
  bringen, pq-Formel. Mit Rechenweg zum Mitrechnen. 6 Level.
- Jetzt 107 Level in 18 Kapiteln.

## 0.9.1 – Neu anfangen
- **🔄 Fortschritt zurücksetzen**: In den Einstellungen ganz unten unter „Neu anfangen“. Löscht Sterne, Energiezellen,
  Tagesmissionen, Rekorde, Abzeichen und die gewählte Klasse – praktisch, wenn ein Geschwisterkind von vorn beginnt.
  Ton-Einstellungen und selbst gebaute Level bleiben erhalten.
- Gegen Versehen: Der Knopf muss 2 Sekunden gedrückt gehalten werden, ein Balken läuft dabei voll.

## 0.9.0 – Widget für den Startbildschirm
- **📱 Kurvenflug-Widget**: Lange auf den Startbildschirm drücken → Widgets → Kurvenflug. Volt zeigt dort, was heute dran ist:
  die Tagesmission, „Weiter geht's“ mit deinem nächsten Level und – wenn fällig – ein Thema zum Auffrischen.
  Ein Tipp, und du bist direkt im Level.
- Das Widget lässt sich groß oder klein ziehen. Klein zeigt es nur deine Sterne und „Weiter“.
- Es liest nur deinen Spielstand auf dem Handy – keine Daten nach draußen.

## 0.8.0 – Aufgeräumte Startseite
- **Vier Reiter statt einer langen Liste**: 🚀 Heute, 🗺️ Galaxie, 🎮 Spiele und 🎒 Ich.
- **🚀 Weiter geht's!**: Eine große Karte bringt dich mit einem Tipp genau dorthin, wo du aufgehört hast.
- **🎓 Deine Klasse**: Volt fragt einmal, in welcher Klasse du bist, und zeigt zuerst, was gerade im Unterricht dran ist.
  Darunter „Zum Wiederholen“ und „Für später“. Gesperrt wird nichts – und die Klasse lässt sich jederzeit ändern.
- **🗺️ Galaxie**: Kapitel sind eingeklappt und zeigen ihren Fortschritt als Ring. Antippen klappt die Level auf.
  Mit den Filtern 📐 Mathe und ⚛️ Physik findest du dein Fach schneller.

## 0.7.0 – Rechenweg mit Volt
- **🧮 Rechenweg zum Mitrechnen**: Klappt es dreimal nicht, rechnet Volt mit dir Schritt für Schritt – du tippst jedes
  Zwischenergebnis selbst an. Beim ersten Mal rechnet Volt einen Schritt vor, danach rechnest du allein.
  Den Regler stellst du am Ende selbst ein. Für 42 Level, von Geraden über Pythagoras bis Trigonometrie.
- Im Duell, im Sturm und beim Boss gibt es keinen Rechenweg – da zählt dein eigener Kopf.

## 0.6.0 – Trigonometrie, Lernstand & Auffrischen
- **📐 Neues Kapitel „Trigonometrie“ (Klasse 9–10)**: Mit tan⁻¹ den Laserwinkel ausrechnen, mit sin und cos den Funkradius.
  Volt zeichnet das rechtwinklige Dreieck mit allen Seiten ins Spielfeld. Taschenrechner erlaubt! 7 neue Level.
- **📊 Mein Lernstand**: Für jedes Thema siehst du, ob es „Neu“, „Übt noch“ oder „Sicher“ ist und wann du es zuletzt
  geübt hast. Mit einer kurzen Erklärung für Eltern.
- **🔁 Auffrischen**: Geschaffte Themen kommen nach ein paar Tagen als kurzes Level zurück – wackelige nach 2 Tagen,
  sichere nach 7. So bleibt das Gelernte im Kopf. Verpasste Tage verfallen einfach, kein Druck.
- **🎯 Abstands-Pfeil**: Nach einem Fehlversuch zeigt ein gelber Pfeil an der Station, wie weit du daneben warst
  („2 zu tief“) – dann weißt du sofort, in welche Richtung du nachrechnen musst.

## 0.5.3 – Laser-Zielhilfe & passendere Aufgaben
- **🔦 Laser-Zielhilfe**: Beim Zielen siehst du den Strahl jetzt bis zum ersten Spiegel. Wohin er abprallt, findest du selbst
  heraus – Einfallswinkel = Ausfallswinkel. So kommt man mit Überlegen zum Ziel statt mit Raten.
- **📍 Koordinaten (Klasse 5–6)**: Die Funksprüche passen jetzt zur Klassenstufe – „2 − 4“ statt Teilen mit Minuszahlen,
  und die Mitte zwischen zwei Punkten findet man durch Kästchenzählen.
- **🌙 Gipfelstation**: Eine zweite Station sorgt dafür, dass wirklich der Gipfel (Scheitel) getroffen werden muss.

## 0.5.2 – Rechnen lohnt sich
- **Treffer zählen jetzt nur, wenn die Rechnung stimmt**: Bisher reichte es manchmal, wenn eine steile Kurve die Station nur
  am Rand streifte – dann klappte es auch mit geratenen Zahlen. Jetzt muss die Kurve an der Stelle der Station die richtige
  Höhe haben. Wer rechnet, trifft beim ersten Versuch – und Volt sagt bei einem Fehlversuch genau, wie weit es noch ist.
- **Hyperbel-Level (Zuordnungen)**: k geht jetzt in ganzen Schritten, und die Stationen liegen so, dass man k sicher ausrechnen kann.
- Schon gesammelte Sterne bleiben natürlich erhalten.

## 0.5.1 – Formeln wie im Schulbuch
- **Formeln lesen sich jetzt wie im Heft**: statt „y = log₃(x) + (−2)“ steht da „y = log₃(x) − 2“. Bei der Parabel wird aus
  „(x − (−3))“ einfach „(x + 3)“. Das gilt für alle Kurven – Geraden, Parabeln, Sinus, Wachstum, Logarithmus und Weg & Zeit.
- **Rampe und Wurf**: Die Meter-Angabe ganz rechts am Boden wird nicht mehr abgeschnitten.

## 0.5.0 – Sechs neue Kapitel, Lexikon & Werkstatt
- **⚖️ Zuordnungen (Kl. 6–7)**: proportional y = k · x (auch Prozent) und antiproportional y = k : x – die Hyperbel.
- **⏱️ Weg & Zeit (Physik Kl. 7)**: gleichförmige Bewegung im s-t-Diagramm, s = v · t + s₀.
- **🔦 Laser-Optik (Physik Kl. 7)**: Strahl über Spiegel lenken – Einfallswinkel = Ausfallswinkel. Periskop, Zickzack, Kehrtwende.
- **📡 Pythagoras (Kl. 8–9)**: Funkwelle mit genau dem richtigen Radius – zu groß, und ein Störsender funkt dazwischen.
- **🎢 Energie (Physik Kl. 8–9)**: Kugel von der Rampe – Lageenergie wird Bewegungsenergie. Überraschung auf dem Mond!
- **🔢 Logarithmus (Kl. 10)**: y = log_b(x) + d, auch mit fallender Kurve.
- **🪐 Planeten-Lexikon**: 14 Himmelskörper mit echten Fakten – mit Sternen freischalten.
- **🎨 Werkstatt**: Sonden-Formen (Rakete, UFO, Komet), Spuren (Funken, Regenbogen, Doppel) und Hüte für Volt – mit Energiezellen freischalten.
- Kapitel sind jetzt nach Klassenstufe sortiert. Insgesamt 75 Level in 13 Kapiteln.

## 0.4.0 – Baukasten, Klassen-Challenge, Kurve ziehen & Formel-Detektiv
- **🛠️ Baukasten**: Eigene Level bauen – Stationen, Asteroiden, Energiezellen und Planeten aufs Feld tippen, Lösung einstellen,
  als Code an Freunde schicken. Geteilt wird nur, was fair und lösbar ist.
- **🏫 Klassen-Challenge**: Lehrkräfte wählen bis zu 10 Level (auch eigene) und teilen einen Code. Die Schüler spielen
  der Reihe nach und zeigen am Ende eine Ergebnis-Karte mit Datum und Prüfcode.
- **✋ Kurve ziehen**: Die Bahn direkt mit dem Finger formen – Scheitel, Steigung oder Zielpfeil ziehen, die Formel rechnet live mit.
- **🔎 Formel-Detektiv**: Umgekehrt! Die Kurve ist zu sehen, du tippst die Formel (Komma und Brüche wie 1/2 gehen). 8 Fälle pro Runde.
- Codes aus WhatsApp: „Teilen → Kurvenflug“ öffnet Level, Challenges und Duelle direkt.

## 0.3.0 – Duell, Bosse, Sturm & Rendezvous
- **⚔️ Duell**: zu zweit am selben Handy (3 Runden, abwechselnd) oder per Code an Freunde schicken – wer weniger Versuche braucht, gewinnt.
  Den Code einfach in WhatsApp teilen; „Teilen → Kurvenflug“ öffnet die Herausforderung direkt.
- **🤝 Neues Kapitel „Rendezvous“ (Klasse 8)**: Zwei Sonden auf zwei Geraden sollen sich treffen – du rechnest den Schnittpunkt aus
  (lineare Gleichungssysteme), 7 Level.
- **👾 Boss-Kämpfe**: Am Ende jedes Kapitels wartet ein Alien-Mutterschiff mit drei Schild-Segmenten – drei schwere Etappen am Stück.
- **☄️ Asteroidensturm**: Welle um Welle, jede muss beim ersten Versuch sitzen. Keine Uhr, nur dein Rekord.
- **🏅 Abzeichen**: 14 Abzeichen zum Sammeln – Volt feiert jedes neue.

## 0.2.1 – Updates kommen schneller
- Die App schaut jetzt bei **jedem Öffnen** nach einer neuen Version (höchstens alle 30 Minuten) – auch wenn sie nur aus dem Hintergrund zurückkommt. Vorher nur einmal am Tag.
- Ist eine neue Version da, erscheint oben auf der Startseite ein 🎁-Hinweis. Antippen → laden → installieren. Dein Fortschritt bleibt erhalten.

## 0.2.0 – Klasse 10, Schwerkraft und Tagesmission
- **Neues Kapitel „Wellen & Wachstum“ (Klasse 10)** 🌊: Sinus-Wellen y = a · sin(b · x) + d mit π-Markierungen,
  Wachstum und radioaktiver Zerfall y = a · bˣ – 7 neue Level.
- **Neues Kapitel „Schwerkraft-Schleuder“ (Physik)** 🪐: Planeten ziehen deine Sonde an (Gravitationsgesetz).
  Schwing dich um Planeten herum wie echte Raumsonden – 5 neue Level, echt durchgerechnet.
- **Tagesmission** 🛰️: Jeden Tag ein neues Level (Gerade, Parabel, Wachstum oder Wurf), garantiert lösbar.
  Verpasste Tage verfallen einfach – kein Druck.
- **Musik** 🎵: leise Weltraum-Klänge, im Level etwas schneller. In den Einstellungen abschaltbar.
- Aus „Quanta“ wurde „Kurvenflug“ – eine eigene App nur für dieses Spiel.

## 0.1.0 – Start
- **Kurvenflug** 🚀: Du steuerst eine Raumsonde – die Flugbahn ist eine Formel, die du selbst einstellst.
  Stationen treffen, Asteroiden ausweichen, Energiezellen ⚡ sammeln.
- **27 Level in 4 Kapiteln**: Koordinaten (Kl. 5–6), Geraden y = m·x + b (Kl. 7–8), Parabeln in Scheitelform (Kl. 9),
  schräger Wurf mit echter Schwerkraft auf Erde, Mond und Mars (Physik).
- **Volt**, dein Roboter-Kumpel, erklärt jedes Kapitel und sagt nach einem Fehlversuch genau, was nicht gepasst hat
  („Bei x = 4 warst du bei y = 2 – das Ziel liegt bei y = 3“).
- **Sterne für Köpfchen**: Wer vorher rechnet und beim ersten Versuch trifft, bekommt 3 ⭐. Mit „Ganze Bahn“-Hilfe sind es 2 ⭐.
- **Hangar**: Neue Sonden-Farben für gesammelte Sterne.
- Keine Anmeldung, keine Werbung, keine Käufe. Updates direkt in der App.
