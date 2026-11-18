# Änderungen

Jede Version steht hier mit ihrem Datum.

## 2.18.22 – Anonyme Zählung, Lern-Geschenk und ein Hinweis auf die Nachhilfe – alles abschaltbar (18.11.2026)
- **Anonyme Zählung, abschaltbar:** Raumlicht meldet uns ab jetzt, dass etwas geschah – zum Beispiel „App geöffnet“ oder „Link angetippt“. Ohne Kennung, ohne Cookie, ohne Gerätedaten, ohne Lernstand. Das hilft uns zu sehen, was genutzt wird. Schalter: Einstellungen → „Anonyme Nutzungszählung“.
- **Hinweis auf unsere Nachhilfe, abschaltbar:** Wenn im Lernstand ein Gebiet noch hakt, kann dort ein kurzer, sachlicher Hinweis auf eine kostenlose Probestunde stehen. Nie in den Aufgaben, ohne Preis, ohne Lux, höchstens alle 14 Tage je Gebiet, mit „Ausblenden“. Schalter: Einstellungen → „Hinweise auf unsere Nachhilfe zeigen“.
- **🎁 Lern-Geschenk verschenken:** neuer Eintrag in den Einstellungen, führt zur Geschenke-Seite auf unserer Website.
- Ganz unten in den Einstellungen steht jetzt, von wem Raumlicht kommt: „Ein Angebot von Nachhilfe – online“.
- **Für Eltern:** Fremde Werbung gibt es weiterhin nicht. Neu sind Hinweise auf unsere eigenen Angebote; beides oben lässt sich abschalten.

## 2.18.21 – Zweite Fassung für Safari, iPhone und iPad (18.11.2026)
- **Raumlicht gibt es jetzt in zwei Fassungen**, die gleich aussehen und dasselbe können. Safari sowie alle Browser auf iPhone und iPad bekommen von selbst die Fassung, die dort zuverlässig ein Bild zeigt; alle anderen behalten die schnellere.
- **Startet eine Fassung auf einem Gerät nicht, wechselt Raumlicht von selbst zur anderen** und merkt sich das. Der Spielstand bleibt.
- Dazu zwei weitere Vorkehrungen gegen die weiße Seite im neuen Safari.
- Für die Fehlersuche: `?modus=js` oder `?modus=wasm` an der Adresse wählt die Fassung von Hand, `?diag=1` zeigt die Schritte des Starts.

## 2.18.20 – Der KI-Abi-Pass ist im Shop (18.11.2026)
- **KI-Tutor:** Auf der KI-Seite (Einstellungen → KI-Tutor) führt jetzt ein Knopf zum Shop. Der „Raumlicht KI-Abi-Pass“ kostet einmalig 14,99 €, ist kein Abo und gilt bis zum 31. Juli des gewählten Abi-Jahres (2027, 2028 oder 2029) – mit 10 KI-Punkten an jedem Tag.
- 3 Proben bleiben frei. Nach dem Kauf kommt eine E-Mail mit Link und Code; ein Tipp auf den Link schaltet frei.
- **Für Eltern:** Gekauft wird im Shop auf mathenachhilfe-online.com, nicht in der App. Wer noch nicht 18 ist, lässt die Eltern kaufen.

## 2.18.19 – Weiße Seite auf iPhone, iPad und Mac: Ursache gefunden (18.11.2026)
- **Die Ursache ist ein Fehler im neuen Safari (27.2):** Er gibt der Zeichenbibliothek an bestimmten Stellen kaputte Zahlen weiter. Dadurch lief Raumlicht zwar, malte aber nichts – die Seite blieb weiß.
- **Raumlicht umgeht den Fehler jetzt.** Beim Bauen der App werden alle betroffenen Aufrufe so umgeschrieben, dass Safari sie richtig ausführt. Für andere Browser ändert sich nichts.
- Bestätigt ist das erst, wenn es auf einem echten iPhone oder iPad wieder ein Bild gibt – bitte ausprobieren.

## 2.18.18 – Start auf dem iPhone: erst das Bild, dann laden (18.11.2026)
- **Das Ladebild erscheint jetzt immer zuerst.** Alles Schwere wird erst danach geholt – so ist beim Öffnen nie nichts zu sehen.
- **iPhone und iPad laden die beiden großen Teile der App nacheinander** statt gleichzeitig. Das braucht weniger Speicher auf einmal.
- **Schrittprotokoll für die Fehlersuche:** Hängt man `?diag=1` an die Adresse, schreibt Raumlicht beim Start jeden Schritt sichtbar mit. Bricht ein Start ab, steht beim nächsten Öffnen auf dem Ladebild, wie weit er kam.

## 2.18.17 – iPad und iPhone: gestrichelte Linien abgesichert (18.11.2026)
- Auf iPads mit neuem Safari kann die Zeichenbibliothek manche Muster nicht anlegen. Für Farbverläufe gab es schon einen Ersatz; jetzt auch für gestrichelte Linien. Betroffen waren „Binomial-Labor“, „Zufallsmaschine“ und „Geisterfahrt“ – dort konnte der Bildschirm leer bleiben. Die Linien sind auf solchen Geräten jetzt durchgezogen.

## 2.18.16 – Keine weiße Seite mehr auf dem iPhone (18.11.2026)
- **Auf manchen iPhones blieb Raumlicht weiß.** Die Ursache auf dem echten Gerät kennen wir noch nicht sicher; wahrscheinlich nimmt das iPhone Raumlicht den Speicher oder die Grafik weg. Diese Version geht beides an.
- **Weniger Speicher auf dem iPhone:** Raumlicht zeichnet dort mit doppelter statt dreifacher Schärfe. Das sieht man kaum, braucht aber nur gut halb so viel Bildspeicher.
- **Immer eine Meldung statt einer leeren Seite:** Fällt die Anzeige aus oder bleibt der Start hängen, erscheint ein Hinweis mit Versionsnummer und dem Knopf „Neu laden“.
- **Raumlicht merkt, wenn das Gerät es beendet hat,** sagt das beim nächsten Start und schaltet nach dem zweiten Mal von selbst in einen Sparmodus. Der Spielstand bleibt dabei erhalten.
- Die Diagnose-Seite zeigt jetzt auch, was kurz vor einem solchen Abbruch passiert ist.

## 2.18.15 – Raumlicht als App installieren, KI-Tutor für alle (18.11.2026)
- **Raumlicht gibt es vorerst nur noch als Web-App** – im Browser, auf Wunsch installiert wie eine App. Die Android-App (APK) wird in den nächsten ein bis zwei Monaten nicht weiter ausgeliefert.
- **Neu: „App installieren“** in den Einstellungen. Ein Tipp legt Raumlicht auf den Startbildschirm; dann startet es im Vollbild und auch ohne Internet. Auf iPhone und iPad zeigt eine kurze Anleitung den Weg über „Teilen → Zum Home-Bildschirm“. Ist Raumlicht schon installiert, verschwindet der Hinweis.
- **Der KI-Tutor ist jetzt für alle da:** Nach einem Fehlversuch erklärt Lux die Aufgabe auf Wunsch anders und beantwortet Rückfragen. 3 Proben sind frei; den KI-Abi-Pass gibt es bald im Shop.
- Raumlicht Pro und der KI-Abi-Pass gelten in der Web-App.
- **Für Eltern:** Der KI-Tutor arbeitet nur, wenn man ihn antippt. Was dabei gesendet wird, steht auf der KI-Seite in den Einstellungen.

## 2.18.14 – Vorlesen macht Pause, die WebApp hält sich selbst aktuell (18.11.2026)
- **Vorlesen ist vorerst abgeschaltet.** Der Schalter „Lux spricht“, das Sprechtempo und das Offline-Laden der Stimme sind aus den Einstellungen verschwunden. Lux zeigt ihre Sprechblasen weiter – sie liest sie nur nicht mehr vor. Vielleicht kommt das Vorlesen später zurück.
- **Musik und Geräusche bleiben** und lassen sich wie bisher getrennt ein- und ausschalten und einzeln leiser stellen.
- **Für Eltern:** Damit geht Raumlicht zum Vorlesen nicht mehr ins Internet; auch die Sprachausgabe des Geräts wird nicht mehr benutzt.
- **WebApp immer aktuell:** Die WebApp schaut beim Öffnen, bei jeder Rückkehr in den Tab und alle 30 Minuten nach einer neuen Version – jetzt auf zwei Wegen, damit niemand auf einer alten Version hängen bleibt. Neu geladen wird nur an einer ruhigen Stelle, nie mitten in einer Aufgabe; der Spielstand bleibt. Danach sagt ein kurzer Hinweis „Neue Version … geladen“.

## 2.18.13 – Lux erklärt’s anders: der KI-Tutor ist eingebaut (18.11.2026)
- **Neu, noch im Probebetrieb:** Hängst du nach einem Fehlversuch fest, erklärt dir Lux die Aufgabe mit KI-Hilfe noch einmal anders – passend zu dem, was du eingestellt hast. Du kannst nachfragen wie bei einer Nachhilfe. Lux führt dich zum Weg; die Lösung findest du selbst.
- **Sichtbar ist der KI-Tutor zunächst nur auf der Testseite.** Für alle wird er freigeschaltet, sobald unser Server dafür bereit ist. Bis dahin ändert sich in der App nichts.
- **Kosten und Grenzen:** 3 Proben sind frei. Mit dem „Raumlicht KI-Abi-Pass“ (kommt in den Shop, einmalig, kein Abo) gibt es 10 KI-Punkte an jedem Tag bis zum 31. Juli des Abi-Jahres. Raumlicht Pro braucht man dafür nicht.
- **Für Eltern:** Der KI-Tutor arbeitet nur, wenn man ihn antippt. Dann gehen die Aufgabe, die Eingabe und die Frage an unseren Server, der sie von einer KI (Anthropic) beantworten lässt – ohne Namen, ohne Konto, ohne Lernstände. Jede Antwort ist als KI-Antwort gekennzeichnet. Im Pflichtteil „ohne Hilfsmittel“ gibt es ihn nicht, in der Play-Store-Fassung auch nicht.

## 2.18.12 – Ton auch bei stummem Klingelton (18.11.2026)
- **Android:** Raumlicht blieb still, wenn der Klingelton des Handys auf stumm oder Vibration stand – auch wenn die Medien-Lautstärke aufgedreht war. Jetzt gilt wie bei Musik und Videos: Laut ist, was die Medien-Lautstärke sagt (die Lauter- und Leiser-Tasten).
- Steht die Medien-Lautstärke auf 0, sagen das die Einstellungen weiterhin.

## 2.18.11 – Jede richtige Antwort zählt (18.11.2026)
- **Mehrere Wege, alle richtig:** Eine Ebene lässt sich auf viele Arten aufschreiben – mit doppelten Zahlen oder mit umgedrehten Vorzeichen. Jede Fassung, die dieselbe Ebene beschreibt, wird als richtig gewertet; die Regler reichen dafür jetzt von −6 bis 6. Die Erfolgsmeldung sagt das auch.
- **Eindeutiger Auftrag:** Bei „Normalenvektor“ (5-4) steht jetzt dabei, dass der Vektor nach unten zur Leinwand zeigen soll.
- Ein neuer Dauertest prüft bei jeder Änderung, dass gleichwertige Antworten angenommen werden.

## 2.18.10 – Ein Logo überall, Nova kennt mehr Sätze vorab (18.11.2026)
- **Ein Logo:** Das App-Symbol der Android-App, das Startbild und das Ladebild im Browser zeigen jetzt dasselbe Bild wie die WebApp und der Shop – Lux, die Lichtdrohne. Auf dem Handy ändert sich dadurch das Symbol auf dem Startbildschirm.
- **Nova:** 522 typische Rückmeldungen auf Eingaben in den festen Aufgaben sind jetzt vorab vertont – sie kommen ohne Wartezeit und lassen sich mit „Alle Nova-Texte offline laden“ mitnehmen.

## 2.18.9 – Datum berichtigt (18.11.2026)
- Die Version 2.18.8 zeigte in den Einstellungen noch das Datum vom Vortag. Sonst ist nichts geändert.

## 2.18.8 – Kauf-Hinweis sachlicher (18.11.2026)
- **Raumlicht Pro:** Der Knopf heißt jetzt „Zum Shop: Raumlicht Pro · 7,99 €“ statt einer Kaufaufforderung, und darunter steht klar: Wer noch nicht 18 ist, lässt die Eltern kaufen.

## 2.18.7 – Raumlicht Pro ist da (17.11.2026)
- **Raumlicht Pro:** Die Vollversion gibt es jetzt – einmalig 7,99 €, kein Abo, kein Ablauf. Gekauft wird im Shop auf mathenachhilfe-online.com (am besten zusammen mit den Eltern); danach kommt eine E-Mail mit Link und Code. Link antippen oder Code in den Einstellungen unter „Raumlicht Pro“ eingeben – fertig. Auf der Startseite steht dann „🎓 Pro aktiv“.
- **Das bleibt frei:** Kapitel 1 bis 6 komplett, aus jedem weiteren Kapitel die ersten 2 Aufgaben, die Tagesaufgabe, Üben in der Stufe „Einstieg“, die erste Prüfungsaufgabe sowie Kurve abfahren, Flug, Ballerbude, Zufallsmaschine, Galton-Brett und der 3D-Rechner.
- **Zu Pro gehören:** alle 136 Aufgaben, alle 7 Prüfungsaufgaben (auch auf Zeit), Üben in „Abi“ und „Profi“, Achterbahn-Baumeister, Geisterfahrt, Ringflug, Binomial-Labor, Klausur-Planer, Fehler-Tagebuch und Lernstand. Dein bisheriger Fortschritt bleibt erhalten.
- **Für Eltern:** Der Code wird nur auf dem Gerät geprüft und gespeichert; beim Freischalten wird nichts übertragen. In der Fassung aus dem Play Store gibt es keinen Kauf – dort bleibt alles frei.
- **Behoben:** Der Freischalt-Link aus der E-Mail wurde im Browser nicht erkannt, wenn der Code – wie üblich – mit „RL“ begann.

## 2.18.6 – Nova spricht in der Android-App jetzt wirklich alles (17.11.2026)
- **Android:** Sätze, die erst in der Aufgabe entstehen (zum Beispiel Rückmeldungen mit deinen Zahlen), hat unser Stimmen-Server von der Android-App bisher abgelehnt – deshalb war dort die Handy-Stimme zu hören oder gar nichts. Die App meldet sich jetzt richtig an, und Nova spricht auch diese Sätze.
- Im Browser war das schon in Ordnung.

## 2.18.5 – Vorbereitung für Raumlicht Pro (17.11.2026)
- **Hinter den Kulissen:** Die geplante Vollversion heißt künftig „Raumlicht Pro“ (einmalig 7,99 €, kein Abo, kein Ablauf). Sie ist noch nicht erhältlich – bis dahin bleibt alles frei, und in der App ändert sich nichts.

## 2.18.4 – Klarere Aufgaben, Nova ohne fremde Stimme (17.11.2026)
- **Aufgaben verständlicher:** Alle Aufgaben, Prüfungsteile und Übungs-Vorlagen wurden noch einmal darauf geprüft, ob eindeutig ist, was gefragt ist. An rund 60 Stellen steht jetzt eine klare Frage oder die fehlende Angabe dabei – zum Beispiel „Wie groß ist sein Volumen?“ beim Lichtkegel, das Intervall bei Drehkörpern, „Winkel zwischen Strahl und Segel-Ebene (nicht zum Lot)“ oder welche Zahl bei Parameter-Aufgaben eigentlich gesucht ist.
- **Nichts mehr verraten:** In „Ausfälle einplanen“ zeigte die Bühne die Lösung schon vorher – jetzt sind alle Scheinwerfer gleich gefärbt. Rückmeldungen nennen kein unsichtbares Ziel mehr.
- **Nova statt Handy-Stimme:** Hakt die Verbindung, versucht es die App noch einmal und zeigt dezent „Nova lädt …“. Die Stimme des Geräts liest nur noch, wenn wirklich kein Netz da ist und der Satz nicht gespeichert war. Ist Nova einmal nicht erreichbar, steht das kurz oben – höchstens einmal je Sitzung.
- **Mehr vorab vertont:** Häufige Rückmeldungen (auch mit „Tipp gefällig?“), die Urteile der Geisterfahrt und die Regeln der Ballerbude gehören jetzt zu den festen Texten und lassen sich offline laden.

## 2.18.3 – Falscher Hinweis an der 3D-Bühne behoben (17.11.2026)
- **Android:** Über der 3D-Bühne stand manchmal „Die 3D-Bühne startet nicht …“, obwohl sie längst lief – zum Beispiel, wenn man kurz in eine andere App gewechselt war. Der Hinweis erscheint jetzt nur noch, wenn die Bühne wirklich nicht startet, und verschwindet von selbst, sobald sie da ist.

## 2.18.2 – Musik gleich beim Öffnen (17.11.2026)
- **Android:** Die Musik läuft jetzt sofort, wenn du die App öffnest – nicht erst nach dem ersten Tippen. Im Browser beginnt sie weiterhin beim ersten Tippen oder Klick, weil Browser Ton vorher nicht erlauben.
- „Musik aus“ in den Einstellungen gilt wie bisher, und bei stumm geschaltetem Handy bleibt Raumlicht still.
- Aufgeräumt: letzte Erwähnungen der früheren Stimme aus Code und Unterlagen entfernt (vorgelesen wird nur noch mit Nova).

## 2.18.1 – Feinschliff vor dem Betriebstest (17.11.2026)
- **Kleine Handys:** Auf schmalen Bildschirmen wird nichts mehr abgeschnitten – Kacheln und Gebiete auf der Startseite, lange Titel, das Abzeichen „Präzisionsarbeit“ und die Zahlen-Regler im Galton-Brett und im Binomial-Labor passen jetzt hinein.
- **Zufallsmaschine:** Die Erwartungslinie läuft nicht mehr durch den Hinweistext.
- **Nova:** Bei wackeliger Verbindung stauen sich keine alten Sätze mehr. Fehlt eine Datei in Bestqualität noch auf dem Server, wird sie beim nächsten Tippen nachgeholt.
- **Für Eltern:** Ist „Lux spricht“ ausgeschaltet, fragen die Einstellungen nichts mehr beim Stimmen-Server ab.

## 2.18.0 – Nova auch ohne Internet (17.11.2026)
- **Alle Nova-Texte offline laden:** In den Einstellungen unter „Lux spricht“ kannst du alle festen Vorlese-Texte auf einmal laden – am besten im WLAN. Danach spricht Lux auch ohne Internet mit Nova.
- **Zwei Qualitäten:** „Standard“ (kleiner) oder „Bestqualität“ (größer) – die App zeigt die aktuelle Größe an. Der Fortschritt wird angezeigt, du kannst abbrechen und später weitermachen. „Offline-Stimme löschen“ gibt den Speicher wieder frei.
- **Für Eltern:** Geladen werden nur Audiodateien von unserem Server. Texte, die erst in der Aufgabe entstehen, brauchen weiterhin Internet.

## 2.17.2 – Datenschutz-Hinweis ergänzt (17.11.2026)
- **Für Eltern:** In den Einstellungen steht jetzt zusätzlich, dass unser Stimmen-Server keine Zugriffsprotokolle speichert.

## 2.17.1 – Sprechtempo für Nova (17.11.2026)
- **Sprechtempo wählbar:** In den Einstellungen gibt es wieder einen Regler – sechs Stufen von gemütlich (0,75×) bis flott (1,5×). Die Stimme klingt dabei gleich hoch, sie spricht nur langsamer oder schneller. Die Wahl wird gemerkt.
- Gilt auch für die Stimme des Geräts, die einspringt, wenn es kein Internet gibt.

## 2.17.0 – Lux spricht mit Nova (17.11.2026)
- **Neue Stimme:** Lux liest jetzt mit „Nova“ vor – einer KI-generierten Stimme, die viel natürlicher klingt. In den Einstellungen steht deutlich „Nova – KI-generierte Stimme“. Vorlesen bleibt aus, bis du es einschaltest.
- **Kein großer Download mehr:** Die Stimme „Thorsten“ (63 MB) und alles, was dazugehörte, ist entfernt; schon geladene Dateien räumt die App von selbst weg. Die App wird dadurch deutlich kleiner.
- **Für Eltern – wichtig:** Zum Vorlesen braucht die App jetzt Internet. Feste Texte haben wir vorab vertont; die App lädt nur die Audiodatei von unserem Server. Texte, die erst in der Aufgabe entstehen (zum Beispiel eine Rückmeldung mit den eingegebenen Zahlen), schickt die App ohne Angaben zur Person an unseren Server; der lässt sie einmalig von OpenAI vertonen. Namen, Konten oder Geräte-Kennungen werden nicht übertragen. Wer das nicht möchte, lässt „Lux spricht“ ausgeschaltet.
- Ohne Internet liest still die Stimme des Geräts. Das Sprechtempo ist jetzt fest; der Regler dafür ist entfallen.

## 2.16.4 – Vorbereitung für die neue Vorlese-Stimme (17.11.2026)
- **Hinter den Kulissen:** Die WebApp liefert jetzt eine Liste aller festen Vorlese-Texte aus (`vorlesen/texte.json`). Damit kann unser Server sie für die kommende Stimme „Nova“ vorab vertonen.
- In der App ändert sich noch nichts: Vorgelesen wird weiter wie bisher, und es werden keine Daten gesendet.

## 2.16.3 – „Warum höre ich nichts?“ (17.11.2026)
- **Kein Ton trotz eingeschaltetem Ton?** Raumlicht bleibt mit Absicht still, wenn das Handy stumm oder auf Vibration steht (damit im Unterricht nichts losdudelt). Das sagen die Einstellungen jetzt deutlich – ebenso, wenn die Medien-Lautstärke auf 0 steht. Sobald du das Handy laut stellst, verschwindet der Hinweis.
- **iPhone und iPad (WebApp):** Dort steht jetzt der Tipp, den Stumm-Schalter zu prüfen.

## 2.16.2 – Abzeichen „Abi-Pass aktiv“ (17.11.2026)
- **Abi-Pass auf einen Blick:** Ist der Abi-Pass freigeschaltet, leuchtet auf dem Startbildschirm das Abzeichen „🎓 Abi-Pass aktiv“ – und in den Einstellungen steht es ebenfalls. Antippen zeigt, was alles dazugehört. (Den Abi-Pass gibt es noch nicht zu kaufen; bis dahin bleibt alles frei.)
- **Für Eltern:** In den Einstellungen steht jetzt ausdrücklich, dass die natürliche Stimme von unserem eigenen Server geladen wird.

## 2.16.1 – Eine Stimm-Datei für alle Apps (17.11.2026)
- **Android:** Die natürliche Stimme „Thorsten“ kommt weiterhin von unserem eigenen Server – jetzt als fertig aufbereitete Datei, die alle unsere Apps gemeinsam benutzen. Für dich ändert sich nichts; wer die Stimme schon geladen hat, behält sie.
- Beim Laden der Stimme geht nichts an fremde Server, und beim Vorlesen verlässt kein Text dein Gerät.

## 2.16.0 – Thorsten spricht jetzt auch in der WebApp (17.11.2026)
- **WebApp:** Die natürliche Stimme „Thorsten“ gibt es jetzt auch im Browser – dieselbe wie in der Android-App. In den Einstellungen unter „Lux spricht“: „Stimme laden (63 MB)“, am besten im WLAN.
  Sie kommt einmalig von unserem eigenen Server, wird geprüft und bleibt im Browser gespeichert; danach rechnet sie auf deinem Gerät, auch ohne Internet. Vorgelesene Texte verlassen dein Gerät nie.
- Du kannst jederzeit zur Stimme des Geräts zurückschalten oder Thorsten wieder entfernen. Kennt Thorsten ein Wort nicht, liest diesen einen Satz die Stimme des Geräts.
- Der Start der WebApp wird dadurch nicht langsamer: Die Rechen-Bausteine werden erst geladen, wenn Thorsten zum ersten Mal spricht.

## 2.15.1 – Die Stimme kommt jetzt vom eigenen Server (17.11.2026)
- **Android:** Die natürliche Stimme „Thorsten“ wird – wie bisher nur auf deinen Wunsch – jetzt von unserem eigenen Server geladen, nicht mehr von einem fremden. Sie ist dieselbe wie vorher und wird doppelt geprüft.
- Wer die Stimme schon geladen hat, muss nichts tun.

## 2.15.0 – Raumlicht klingt jetzt wie Raumlicht (17.11.2026)
- **Eigene Musik, neu komponiert:** gläsern und schwebend, wie Licht im Raum – ruhige Flächen, angeschlagenes Glas, feiner Schimmer. Beim Rechnen kommt ein leises Perlen dazu. Keine fremden Aufnahmen: Alles entsteht im Programm.
- **Erkennungsmelodie:** fünf aufsteigende Töne, wie ein Lichtstrahl. Du hörst sie leise bei der ersten Berührung, in der Musik – und festlich, wenn du ein Kapitel geschafft hast.
- **Die Lichtshow klingt mit:** Jeder Laserstrahl ist ein Ton, mehrere Strahlen ergeben zusammen einen Akkord. Geht Licht durch eine Scheibe, schillert es.
- **Alle Geräusche neu** und aus derselben Klangwelt. „Falsch“ ist jetzt leise und sanft – kein Summer.
- **Getrennt einstellbar:** Musik und Geräusche haben je einen eigenen Lautstärke-Regler (zusätzlich zur Gesamt-Lautstärke) und lassen sich wie bisher einzeln ausschalten.
- **Rücksichtsvoll:** Die Musik beginnt erst nach der ersten Berührung, wird unter Lux’ Stimme leiser, ist in Prüfungen auf Zeit und im Pflichtteil aus – und bleibt still, wenn dein Handy stumm geschaltet ist.
- In der Android-App und in der WebApp klingt alles gleich.

## 2.14.0 – Theorie auf Knopfdruck (17.11.2026)
- **Neu: „📖 Theorie“ bei jeder Aufgabe.** Hängst du fest, erklärt Lux kurz, worum es geht: die Idee, die Schritte, ein durchgerechnetes Beispiel und der typische Fehler.
  Das gibt es für alle Aufgaben der Tour, die Prüfungsaufgaben, die Tagesaufgabe und das endlose Üben. **Nachlesen kostet keinen Stern** – nur der Tipp zur konkreten Aufgabe kostet weiterhin einen.
- **Schreibweise wählbar:** In den Einstellungen kannst du auf die strenge Vektorschreibweise umstellen („AB⃗ = OB⃗ − OA⃗“ statt „AB⃗ = B − A“) – so, wie es deine Lehrkraft möchte. Voreingestellt ist die kurze Form.
- **Mehr Abwechslung beim Üben:** 29 statt 8 Rotationskörper und dreimal so viele Tempo-Aufgaben.
- Die Pfeile in den Übungsaufgaben ragen nicht mehr aus dem Raum.
- Zwei Erfolge haben deutsche Namen bekommen: „Abi-reif“ und „Dauergast“.

## 2.13.0 – Große Korrektheitsprüfung (17.11.2026)
Alle 136 Aufgaben, alle 7 Prüfungsaufgaben und über 1200 Übungsaufgaben wurden unabhängig nachgerechnet. **Keine einzige Lösung war falsch** – aber rundherum gab es einiges zu verbessern:
- **Klarere Rückmeldungen:** „Dein Punkt liegt weder auf g noch auf h“ statt eines Satzes, den man falsch verstehen konnte. Beim Zielen zählt jetzt der Abstand zum Strahl, nicht zur ganzen Geraden.
- **Nichts fehlt mehr:** In Aufgabe 20-4 stand die Ebenengleichung nicht dabei, in 8-5 der Punkt P. In den Prüfungsaufgaben bleibt der Sachzusammenhang (zum Beispiel die Funktion des Lichtbogens) jetzt bei jeder Teilaufgabe sichtbar.
- **Saubere Schreibweise:** negative Zahlen in Klammern („2 + (−1) · 3“), „≈“ bei gerundeten Werten, Einheiten in den Prüfungsaufgaben, Einzahl und Mehrzahl („1 Einheit“).
- **Kurve abfahren mit eigenen Funktionen** rechnet genauer: Nullstellen wie bei x³ oder (x − 1)² werden gefunden, Polstellen (1/x²) nicht mehr als Hochpunkt gemeldet, „Wendestelle“ steht nur noch dort, wo wirklich eine ist. Eingaben wie „πx“, „√x“ und „sin 2x“ werden richtig verstanden.
- **Lux liest Formeln verständlich vor:** „Betrag von“, „geteilt durch“, „P Strich“, „größer als“, „zwei Drittel“ – vorher gingen diese Zeichen beim Vorlesen verloren.
- **Für Vorlese-Hilfen** (TalkBack) haben der Zurück-Pfeil, das Zahnrad und die Plus/Minus-Knöpfe jetzt Namen.
- **Lizenzen:** In den Einstellungen steht jetzt, welche freien Bausteine in Raumlicht stecken, mit den Lizenztexten.
- **WebApp:** Vorgelesen wird nur noch mit Stimmen, die auf dem Gerät eingebaut sind – so geht dabei nichts ins Internet.
- Viele kleine Textkorrekturen (einheitliche Namen, Rechtschreibung, Fachbegriffe wie „Tempo“ und „Irrtumswahrscheinlichkeit“).

## 2.12.0 – Abi-Pass eingebaut, aber noch nicht eingeschaltet (16.11.2026)
- **Für dich ändert sich nichts:** Alles bleibt frei, nirgends steht etwas von einem Kauf. Der Abi-Pass wird erst sichtbar, wenn es ihn auf der Website wirklich gibt.
- Im Hintergrund fertig: die Abi-Pass-Seite mit Lux (was dazugehört, was immer frei bleibt), der Knopf zur Kasse auf der Website und die **Freischaltung per Code** –
  eintippen, die ganze E-Mail einfügen oder in der WebApp einfach auf den Link aus der E-Mail tippen. Geprüft wird der Code auf dem Gerät, ohne Konto und ohne Server.
- Einmal freigeschaltet bleibt freigeschaltet – auch ohne Internet.
- Die Play-Store-Fassung bekommt vorerst keinen Kauf und keine Sperren.

## 2.11.1 – Weißer Bildschirm auf dem iPad: Ursache gefunden (16.11.2026)
- **iPad:** Die WebApp lief dort, zeigte aber nur Weiß. Die Mitschrift der Diagnose-Seite hat die Stelle gezeigt: Das iPad konnte einen **Farbverlauf** nicht anlegen, und daran brach jedes Bild ab.
  Alle Farbverläufe der App sind jetzt so gebaut, dass sie nie mehr ein Bild kosten – klappt ein Verlauf nicht, nimmt die App eine einfachere Füllung.
- Auf Android, iPhone und am PC sieht alles aus wie vorher.
- Die Mitschrift der Diagnose-Seite zählt gleiche Meldungen jetzt zusammen, statt die Liste zu füllen, und hält fest, ob die Verläufe geklappt haben.

## 2.11.0 – Acht neue Aufgabenarten zum Üben (16.11.2026)
- **Neu im endlosen Üben** (Themen „Punkte & Vektoren“ und „Geraden“), jedes Mal mit anderen Zahlen:
  - **Einstieg:** Punkte mit Bedingungen finden (zum Beispiel einen Punkt, der in zwei Koordinatenebenen zugleich liegt – hier gibt es viele richtige Antworten), Spiegeln an einer Koordinatenebene.
  - **Abi:** Spiegeln an einer Koordinatenachse, Parallelogramm prüfen und den Schnittpunkt der Diagonalen finden, „Liegen drei Punkte auf einer Geraden?“, dieselbe Gerade in anderer Schreibweise.
  - **Profi:** zwei Spiegelungen hintereinander, Raute mit Parameter.
- **„Alle“ mischt jetzt wirklich alle Aufgabenarten** aus allen Stufen.
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
- **Zwei Fassungen der App:** Die APK von GitHub bleibt, wie sie ist (mit eigenem Update-Knopf). Neu ist eine Fassung für den Play Store:
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
