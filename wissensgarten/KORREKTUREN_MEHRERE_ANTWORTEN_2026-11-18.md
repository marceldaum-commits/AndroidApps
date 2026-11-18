# Fallliste – Aufgaben mit mehreren richtigen Antworten (18.11.2026)

Vorgabe: „Manche Aufgaben haben zwei Wege, aber bei der Kontrolle gibt es nur einen. Das darf nicht sein.“ Jeder Fall ist gelöst mit **(a)** – die Kontrolle akzeptiert alle richtigen Antworten und die Lösungsanzeige nennt sie (Klammer-Alternative „Wort (Alternative)“ bzw. „Wort (A / B)“) – oder **(b)** – die Frage ist jetzt eindeutig.

Geprüft: **2 923 Karten** in 85 Paketen (72 frei, 13 Plus, davon 451 Plus-Karten) – automatisch mit `content/src/test/java/de/wissensgarten/content/AntwortKontrolleTest.kt`, dazu alle eintippbaren (1 591) und sprechbaren Antworten von Hand gelesen. Karten-ids unverändert; jedes geänderte Paket hat `version` + 1.

**118 Karten im Inhalt geändert** (114 frei, 4 Plus) in 43 Paketen.

## Inhalt: Fälle je Art

| Art | (a) Alternative | (b) eindeutig |
|---|---|---|
| Bedeutungsangabe mit mehreren Wörtern | 16 | 0 |
| Einzahl / Mehrzahl | 1 | 0 |
| Kurzform genügt | 10 | 2 |
| Synonym / gleichwertiger Begriff | 55 | 0 |
| andere Schreibweise | 13 | 0 |
| englische Variante | 8 | 0 |
| offene Frage – ohne Auswahl viele Antworten richtig | 0 | 16 |

## Kontrolle: Fälle, die jetzt der Code löst (a)

Gilt für alle Karten, ohne Inhaltsänderung (`TypedAnswer`, `SpokenAnswer` in `core/model/.../Games.kt`). Die Frage wird mitgegeben.

| Fall | Beispiel | vorher | jetzt |
|---|---|---|---|
| Zahlwort statt Ziffer (0–99, Zehner, 100, 1000; Deutsch und Englisch) | „vier“ für 4 (s1-deutsch-grammatik/001) | falsch | richtig – außer das Wort steht schon in der Frage („Welche Zahl ist six?“) |
| Bruch, Dezimalzahl, Prozent | „0,5“, „½“, „50 %“ für 1/2 (s1-mathe-wahrscheinlichkeit) | falsch | richtig – außer die Frage verlangt die Schreibweise (Kürze/Erweitere, als Bruch, als Dezimalzahl, in Prozent, gemischte Zahl, Kehrwert) |
| Einheit ausgeschrieben | „50 Prozent“, „100 Grad“, „2,50 Euro“, „5 Zentimeter“ | falsch | richtig |
| Einheit mit Schrägstrich | „20“ für „20 m/s“ (s1-physik-einheiten-formeln/018) | falsch | richtig |
| Tausender mit Leerzeichen | „1000“/„tausend“ für „1 000“ (s1-mathe-geometrie-formeln/033) | nur „1000“ | alle |
| Aufzählung in anderer Reihenfolge | „Remus und Romulus“ (s1-geschichte-antike-mittelalter/013), „Haken, Appel“ | falsch | richtig – außer bei Reihenfolge-Fragen (Reihenfolge, von … nach …) und festen Ausdrücken (Epoche, „bedeutet“: „Sturm und Drang“, „Bete und arbeite“) |
| Englische Kurzformen | „did not see“ für „didn't see“, „I am full“ (s1-englisch-tenses, -alltag) | beim Sprechen falsch | richtig (nur echte Formen: „willn't“ bleibt falsch) |
| Infinitiv mit „to“ | „read“ für „to read“ (s1-englisch-grundwortschatz/035, 036) | falsch | richtig |
| Präposition vorne | „Schweiz“ für „In der Schweiz“ (s1-erdkunde-gebirge-fluesse/024) | falsch | richtig (eine andere Präposition bleibt falsch) |
| Nachname bei „Wer …?“ (Sprechen) | „Einstein“ für „Albert Einstein“, „da Vinci“ | falsch | richtig – außer die Frage verlangt „Vor- und Nachname“ |
| Ziffern der Spracherkennung | „halb 8“ für „halb acht“ (gs-mathe-groessen/022) | falsch | richtig |
| Klammer-Alternativen beim Sprechen | „Kiew“ für „Kyjiw (Kiew)“ (Brummi-Radio, Auswahl per Stimme) | nicht erkannt | erkannt |
| Mehrere Klammer-Alternativen | „Nomen (Namenwort / Substantiv)“ | – | jede Form zählt |
| Tiefgestellte Ziffern | „CO2“ für „CO₂“ | falsch | richtig |
| Offene Auswahlfrage frei abgefragt | „Welches Wort ist ein Nomen?“, „Was reimt sich auf Maus?“, „Welches Wort ist richtig geschrieben?“ | konnte eingetippt/gesprochen werden (viele Antworten richtig) | diese Auswahl-Karten kommen nicht mehr als Tippen/Sprechen (wie schon im Brummi-Radio) |

Zugleich korrigiert (Kontrolle war zu nachsichtig, Ablenker galten als richtig): **falscher Artikel** („le maison“ für „la maison“, „el casa“ – alle Artikel-Ablenker in Französisch und Spanisch) zählt nur noch als „fast“; **Formeln mit Ableitungsstrich** (z. B. „u' · v“ gegen „u · v'“) werden nicht mehr gleichgesetzt (betraf Ablenker in einem Plus-Paket); **„willn't tell“** (s1-englisch-tenses/030) ist falsch; Klammern nach einer Formel („h / (4π)“) oder mitten in der Antwort sind keine Alternative mehr.

Bewusst so gelassen: Ablenker, die sich nur im Akzent unterscheiden („le velo“), gelten beim Tippen wie bisher als richtig („Akzente werden ignoriert“), in der Auswahl sind sie eindeutig falsch.

## Inhalt: Fälle je Paket

Zeile: Karte · Feld · vorher → nachher · Fall · Lösung. Plus-Pakete liegen privat; dort nur Karten-id, Fall und Lösung.

### aw-allgemeinwissen-mix

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 035 | a | Kohlenstoffdioxid | Kohlenstoffdioxid (Kohlendioxid) | Synonym / gleichwertiger Begriff: „Kohlendioxid“ ist gleich richtig | (a) |

### gs-englisch-basics

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 026 | a | hello | hello (hi) | englische Variante: „hi“ heißt ebenfalls „Hallo“ | (a) |
| 027 | a | thank you | thank you (thanks) | englische Variante: „thanks“ heißt ebenfalls „Danke“ | (a) |
| 028 | a | goodbye | goodbye (bye) | englische Variante: „bye“ ist die übliche Übersetzung von „Tschüss“ | (a) |

### gs-englisch-familie-koerper

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 019 | a | trousers | trousers (pants) | englische Variante: amerikanisch „pants“ | (a) |

### gs-englisch-farben-zahlen

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 009 | a | grey | grey (gray) | englische Variante: amerikanische Schreibweise „gray“ | (a) |

### gs-gesund-ernaehrung

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 001 | a | Möhre | Möhre (Karotte) | Synonym / gleichwertiger Begriff: „Karotte“ ist dasselbe Gemüse | (a) |
| 009 | a | Kalzium | Kalzium (Calcium) | andere Schreibweise: Fachschreibweise „Calcium“ | (a) |

### gs-koerper-natur

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 004 | a | Ohr | Ohr (Ohren) | Einzahl / Mehrzahl: „Ohren“ ist gleich richtig | (a) |
| 029 | q | Was brauchen grüne Pflanzen, um Nahrung herzustellen? | Was brauchen grüne Pflanzen außer Wasser und Luft, um Nahrung herzustellen? | offene Frage – ohne Auswahl viele Antworten richtig: Wasser und Luft (CO₂) wären ebenfalls richtig gewesen | (b) |
| 029 | a | Sonnenlicht | Sonnenlicht (Licht) | Synonym / gleichwertiger Begriff: „Licht“ genügt | (a) |

### gs-lesestart

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 020 | q | Welcher Buchstabe ist ein Selbstlaut? | Welcher Buchstabe ist ein Selbstlaut: B, E, K oder T? | offene Frage – ohne Auswahl viele Antworten richtig: ohne Auswahl wäre jeder Selbstlaut richtig | (b) |
| 021 | q | Welcher Buchstabe ist ein Mitlaut? | Welcher Buchstabe ist ein Mitlaut: A, M, I oder O? | offene Frage – ohne Auswahl viele Antworten richtig: ohne Auswahl wäre jeder Mitlaut richtig | (b) |

### gs-pflanzen-wald

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 005 | q | Welches Tier trägt Blütenstaub von Blüte zu Blüte? | Welches Insekt, das Honig macht, trägt Blütenstaub von Blüte zu Blüte? | offene Frage – ohne Auswahl viele Antworten richtig: auch Hummeln und Schmetterlinge tragen Blütenstaub | (b) |

### gs-satzzeichen-satzarten

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 014 | q | Welches Wort ist ein Fragewort? | Welches Wort ist ein Fragewort: weil, warum, und oder dann? | offene Frage – ohne Auswahl viele Antworten richtig: ohne Auswahl wäre jedes Fragewort richtig (wer, wie, was …) | (b) |

### gs-uhrzeit

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 019 | a | 0 Uhr | 0 Uhr (Mitternacht) | Synonym / gleichwertiger Begriff: „Mitternacht“ ist die übliche Antwort | (a) |

### gs-verkehr-deutschland

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 016 | a | Radweg | Radweg (Fahrradweg) | Synonym / gleichwertiger Begriff: „Fahrradweg“ gleichwertig | (a) |
| 024 | q | Welche Stadt ist auch ein eigenes Bundesland? | Welche dieser Städte ist auch ein eigenes Bundesland: Köln, München, Frankfurt am Main oder Hamburg? | offene Frage – ohne Auswahl viele Antworten richtig: Berlin und Bremen sind ebenfalls Stadtstaaten | (b) |

### gs-wetter-jahreszeiten

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 012 | a | Sturm | Sturm (Orkan) | Synonym / gleichwertiger Begriff: ein Orkan ist ebenfalls sehr starker Wind | (a) |
| 017 | a | Minusgrade | Minusgrade (Frost) | Synonym / gleichwertiger Begriff: „Frost“ gleichwertig | (a) |
| 028 | a | Nüsse | Nüsse (Eicheln) | Synonym / gleichwertiger Begriff: Eichhörnchen sammeln auch Eicheln | (a) |
| 030 | q | Was ziehst du bei Regen an? | Was ziehst du bei Regen über deine Kleidung? | offene Frage – ohne Auswahl viele Antworten richtig: auch Gummistiefel wären richtig gewesen | (b) |
| 030 | a | Regenjacke | Regenjacke (Regenmantel) | Synonym / gleichwertiger Begriff: „Regenmantel“ gleichwertig | (a) |
| 031 | q | Was schützt deine Haut vor Sonnenbrand? | Was cremst du auf die Haut, damit du keinen Sonnenbrand bekommst? | offene Frage – ohne Auswahl viele Antworten richtig: auch Kleidung, Hut oder Schatten schützen | (b) |
| 031 | a | Sonnencreme | Sonnencreme (Sonnenmilch) | Synonym / gleichwertiger Begriff: „Sonnenmilch“ gleichwertig | (a) |
| 032 | a | Handschuhe | Handschuhe (Fäustlinge) | Synonym / gleichwertiger Begriff: „Fäustlinge“ gleichwertig | (a) |

### gs-wortarten

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 007 | a | Nomen | Nomen (Namenwort / Substantiv) | Synonym / gleichwertiger Begriff: „Namenwort“ und „Substantiv“ sind gleichwertig | (a) |
| 008 | a | Verb | Verb (Tunwort / Zeitwort) | Synonym / gleichwertiger Begriff: „Tunwort“ und „Zeitwort“ sind gleichwertig | (a) |
| 009 | a | Adjektiv | Adjektiv (Wiewort / Eigenschaftswort) | Synonym / gleichwertiger Begriff: „Wiewort“ und „Eigenschaftswort“ sind gleichwertig | (a) |
| 010 | a | Nomen | Nomen (Namenwort / Substantiv) | Synonym / gleichwertiger Begriff: „Namenwort“ und „Substantiv“ sind gleichwertig | (a) |
| 011 | a | Verb | Verb (Tunwort / Zeitwort) | Synonym / gleichwertiger Begriff: „Tunwort“ und „Zeitwort“ sind gleichwertig | (a) |
| 012 | a | Adjektiv | Adjektiv (Wiewort / Eigenschaftswort) | Synonym / gleichwertiger Begriff: „Wiewort“ und „Eigenschaftswort“ sind gleichwertig | (a) |
| 020 | a | Nomen | Nomen (Namenwort / Substantiv) | Synonym / gleichwertiger Begriff: „Namenwort“ und „Substantiv“ sind gleichwertig | (a) |
| 028 | a | ein | ein (eine) | Synonym / gleichwertiger Begriff: „eine“ ist ebenfalls ein unbestimmter Artikel | (a) |
| 031 | a | Adjektiv | Adjektiv (Wiewort / Eigenschaftswort) | Synonym / gleichwertiger Begriff: „Wiewort“ und „Eigenschaftswort“ sind gleichwertig | (a) |
| 032 | a | Verb | Verb (Tunwort / Zeitwort) | Synonym / gleichwertiger Begriff: „Tunwort“ und „Zeitwort“ sind gleichwertig | (a) |

### ob-abi-biologie (Plus)

| Karte | Fall | Lösung |
|---|---|---|
| 001 | andere Schreibweise: Schreibweise mit „c“ | (a) |
| 025 | Synonym / gleichwertiger Begriff: gleichwertiger Begriff | (a) |

### ob-bio-genetik (Plus)

| Karte | Fall | Lösung |
|---|---|---|
| 001 | andere Schreibweise: Schreibweise mit „c“ | (a) |
| 017 | Synonym / gleichwertiger Begriff: ausgeschriebene Form | (a) |

### s1-bio-oekologie

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 008 | a | Produzenten | Produzenten (Erzeuger) | Synonym / gleichwertiger Begriff: deutscher Fachbegriff | (a) |
| 009 | a | Destruenten | Destruenten (Zersetzer) | Synonym / gleichwertiger Begriff: deutscher Fachbegriff | (a) |
| 029 | a | Fotosynthese | Fotosynthese (Photosynthese) | andere Schreibweise: Schreibweise mit „Ph“ | (a) |
| 030 | a | Kohlenstoffdioxid (CO₂) | Kohlenstoffdioxid (CO₂ / Kohlendioxid) | Synonym / gleichwertiger Begriff: „Kohlendioxid“ gleichwertig | (a) |

### s1-bio-organe

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 035 | a | Kohlenstoffmonoxid | Kohlenstoffmonoxid (Kohlenmonoxid) | Synonym / gleichwertiger Begriff: „Kohlenmonoxid“ gleichwertig | (a) |

### s1-bio-pflanzen

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 002 | a | Traubenzucker | Traubenzucker (Glucose) | Synonym / gleichwertiger Begriff: Fachbegriff „Glucose“ | (a) |
| 007 | a | Staubblätter | Staubblätter (Staubgefäße) | Synonym / gleichwertiger Begriff: älterer, gleichwertiger Begriff | (a) |

### s1-bio-wirbeltiere

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 020 | a | Gleichwarm | Gleichwarm (warmblütig) | Synonym / gleichwertiger Begriff: „warmblütig“ gleichwertig | (a) |
| 027 | a | Waagerecht | Waagerecht (horizontal) | Synonym / gleichwertiger Begriff: „horizontal“ gleichwertig | (a) |

### s1-bio-zelle-mensch

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 004 | a | Chloroplasten | Chloroplasten (Blattgrünkörner) | Synonym / gleichwertiger Begriff: deutscher Begriff (wie s1-bio-oekologie/004) | (a) |
| 006 | a | Zellmembran | Zellmembran (Plasmamembran) | Synonym / gleichwertiger Begriff: Fachbegriff gleichwertig | (a) |
| 016 | a | Arterien | Arterien (Schlagadern) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 017 | a | Venen | Venen (Blutadern) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 025 | a | Netzhaut | Netzhaut (Retina) | Synonym / gleichwertiger Begriff: Fachbegriff „Retina“ | (a) |

### s1-chemie-grundbegriffe

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 012 | a | Elektronenpaarbindung | Elektronenpaarbindung (Atombindung) | Synonym / gleichwertiger Begriff: „Atombindung“ gleichwertig | (a) |

### s1-deutsch-fremdwoerter

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 001 | a | Vorhersage | Vorhersage (Voraussage) | Synonym / gleichwertiger Begriff: „Voraussage“ gleichwertig | (a) |
| 011 | a | zwiespältig, hin- und hergerissen | zwiespältig (hin- und hergerissen) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 014 | a | Ergebnis, Schlussfolgerung | Ergebnis (Schlussfolgerung) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 016 | a | wichtig, bedeutsam | wichtig (bedeutsam) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 022 | a | einleuchtend, nachvollziehbar | einleuchtend (nachvollziehbar) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 023 | a | Mangel, Fehlbetrag | Mangel (Fehlbetrag) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 024 | a | Übereinstimmung, Einigkeit | Übereinstimmung (Einigkeit) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |

### s1-deutsch-grammatik

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 020 | a | Umstellprobe | Umstellprobe (Verschiebeprobe) | Synonym / gleichwertiger Begriff: „Verschiebeprobe“ gleichwertig | (a) |
| 021 | q | „Ich hoffe, ___ du morgen kommst.“ | „Ich hoffe, ___ du morgen kommst.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 022 | q | „Das Fahrrad, ___ dort steht, gehört mir.“ | „Das Fahrrad, ___ dort steht, gehört mir.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 023 | q | „___ Wetter ist heute schön.“ | „___ Wetter ist heute schön.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 024 | q | „Er sagt, ___ er müde ist.“ | „Er sagt, ___ er müde ist.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 026 | a | Präsens | Präsens (Gegenwart) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 027 | a | Präteritum | Präteritum (Imperfekt) | Synonym / gleichwertiger Begriff: älterer, gleichwertiger Begriff | (a) |
| 029 | a | Plusquamperfekt | Plusquamperfekt (Vorvergangenheit) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |

### s1-deutsch-stilmittel

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 003 | a | Alliteration | Alliteration (Stabreim) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 004 | a | Personifikation | Personifikation (Personifizierung) | Synonym / gleichwertiger Begriff: gleichwertige Wortform | (a) |
| 005 | a | Hyperbel | Hyperbel (Übertreibung) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 013 | a | Euphemismus | Euphemismus (Beschönigung) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 020 | a | Personifikation | Personifikation (Personifizierung) | Synonym / gleichwertiger Begriff: gleichwertige Wortform | (a) |
| 024 | a | Hyperbel | Hyperbel (Übertreibung) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |
| 026 | a | Alliteration | Alliteration (Stabreim) | Synonym / gleichwertiger Begriff: deutscher Begriff | (a) |

### s1-deutsch-zeichensetzung

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 025 | q | Ergänze: „Mama sagt, ___ wir gleich essen.“ | Ergänze: „Mama sagt, ___ wir gleich essen.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 026 | q | Ergänze: „Das Spiel, ___ wir gespielt haben, war super.“ | Ergänze: „Das Spiel, ___ wir gespielt haben, war super.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 027 | q | Ergänze: „Ich finde es toll, ___ du mir hilfst.“ | Ergänze: „Ich finde es toll, ___ du mir hilfst.“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |
| 028 | q | Ergänze: „Siehst du das Pferd? ___ ist schnell!“ | Ergänze: „Siehst du das Pferd? ___ ist schnell!“ (das oder dass?) | offene Frage – ohne Auswahl viele Antworten richtig: Lückensatz ließ andere Wörter zu (z. B. „welches“, „dieses“, „wenn“, „es“); jetzt „(das oder dass?)“ | (b) |

### s1-englisch-alltag

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 011 | a | break | break (recess) | englische Variante: amerikanisch „recess“ | (a) |

### s1-englisch-grundwortschatz

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 033 | a | station | station (train station) | englische Variante: „train station“ gleichwertig | (a) |

### s1-englisch-irregular-verbs

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 040 | a | got – got | got – got/gotten | englische Variante: amerikanisch „gotten“ (wie „was/were – been“) | (a) |

### s1-erdkunde-europa

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 028 | a | Vatikanstadt | Vatikanstadt (Vatikan) | Kurzform genügt: „Vatikan“ genügt | (a) |

### s1-erdkunde-gebirge-fluesse

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 034 | a | Watt | Watt (Wattenmeer) | Synonym / gleichwertiger Begriff: „Wattenmeer“ gleichwertig | (a) |

### s1-erdkunde-klima

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 018 | a | Pangäa | Pangäa (Pangaea) | andere Schreibweise: Schreibweise „Pangaea“ | (a) |
| 029 | a | Seismograf | Seismograf (Seismograph) | andere Schreibweise: Schreibweise mit „ph“ | (a) |

### s1-erdkunde-welt

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 008 | a | Mount Everest | Mount Everest (Everest) | Kurzform genügt: „Everest“ genügt | (a) |
| 017 | a | Baikalsee | Baikalsee (Baikal) | Kurzform genügt: „Baikal“ genügt | (a) |
| 022 | a | Washington, D.C. | Washington, D.C. (Washington) | Kurzform genügt: „Washington“ genügt | (a) |
| 027 | a | Tokio | Tokio (Tokyo) | andere Schreibweise: englische Schreibweise | (a) |
| 028 | a | Peking | Peking (Beijing) | andere Schreibweise: heutige Umschrift „Beijing“ | (a) |
| 029 | a | Neu-Delhi | Neu-Delhi (New Delhi) | andere Schreibweise: englische Schreibweise | (a) |

### s1-ethik-weltreligionen

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 025 | a | Haddsch | Haddsch (Hadsch) | andere Schreibweise: gleich übliche Schreibweise | (a) |
| 034 | a | Nirwana | Nirwana (Nirvana) | andere Schreibweise: Schreibweise mit „v“ | (a) |
| 035 | a | Vesakh | Vesakh (Vesak) | andere Schreibweise: gleich übliche Schreibweise | (a) |

### s1-franzoesisch-grundwortschatz

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 017 | a | le vélo | le vélo (la bicyclette) | Synonym / gleichwertiger Begriff: „la bicyclette“ gleichwertig | (a) |

### s1-geschichte-20-jahrhundert

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 003 | a | Versailler Vertrag | Versailler Vertrag (Vertrag von Versailles) | Synonym / gleichwertiger Begriff: gleichwertige Wortstellung | (a) |
| 012 | a | Novemberpogrome | Novemberpogrome (Reichspogromnacht) | Synonym / gleichwertiger Begriff: gängiger Begriff | (a) |
| 015 | a | Auschwitz-Birkenau | Auschwitz-Birkenau (Auschwitz) | Kurzform genügt: „Auschwitz“ genügt | (a) |

### s1-geschichte-antike-mittelalter

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 019 | a | Kolosseum | Kolosseum (Colosseum) | andere Schreibweise: lateinische Schreibweise | (a) |

### s1-geschichte-ddr-einheit

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 005 | a | Zentrale Planwirtschaft | Zentrale Planwirtschaft (Planwirtschaft) | Kurzform genügt: „Planwirtschaft“ genügt | (a) |
| 036 | a | Die Treuhandanstalt | Die Treuhandanstalt (Treuhand) | Kurzform genügt: „Treuhand“ genügt | (a) |

### s1-informatik-grundlagen

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 015 | a | Boolean | Boolean (bool) | Kurzform genügt: übliche Kurzform | (a) |
| 017 | a | Integer | Integer (int) | Kurzform genügt: übliche Kurzform | (a) |

### s1-latein-grundwortschatz

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 005 | a | sagen | sagen (sprechen) | Bedeutungsangabe mit mehreren Wörtern: dicere = sagen, sprechen | (a) |
| 009 | a | rufen, schreien | rufen (schreien) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 011 | a | laufen, eilen | laufen (eilen) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 017 | a | fürchten | fürchten (Angst haben) | Bedeutungsangabe mit mehreren Wörtern: gleichwertige Übersetzung | (a) |
| 018 | a | müssen, sollen | müssen (sollen) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 020 | a | rufen, nennen | rufen (nennen) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 021 | a | Sklave, Diener | Sklave (Diener) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 025 | a | Junge | Junge (Knabe) | Bedeutungsangabe mit mehreren Wörtern: klassische Übersetzung „Knabe“ | (a) |
| 029 | a | Land, Erde | Land (Erde) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |
| 030 | a | Weg, Straße | Weg (Straße) | Bedeutungsangabe mit mehreren Wörtern: jedes der beiden Wörter allein ist richtig | (a) |

### s1-mathe-pythagoras-trigo

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 015 | a | Gegenkathete von α | Gegenkathete | Kurzform genügt: „von α“ steht schon in der Frage; „Gegenkathete“ allein wurde beim Sprechen nicht erkannt | (b) |
| 015 | choices | Ankathete von α · Hypotenuse · Seitenhalbierende | Ankathete · Hypotenuse · Seitenhalbierende | Kurzform genügt: Ablenker passend gekürzt | (b) |
| 016 | a | Ankathete von α | Ankathete | Kurzform genügt: „von α“ steht schon in der Frage; „Ankathete“ allein wurde beim Sprechen nicht erkannt | (b) |
| 016 | choices | Gegenkathete von α · Hypotenuse · Seitenhalbierende | Gegenkathete · Hypotenuse · Seitenhalbierende | Kurzform genügt: Ablenker passend gekürzt | (b) |

### s1-physik-strom

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 009 | a | Parallel | Parallel (Parallelschaltung) | Synonym / gleichwertiger Begriff: „Parallelschaltung“ gleichwertig | (a) |

### s1-politik-grundgesetz

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 032 | a | Frankfurt am Main | Frankfurt am Main (Frankfurt) | Kurzform genügt: „Frankfurt“ genügt | (a) |

### s1-wirtschaft-grundbegriffe

| Karte | Feld | vorher | nachher | Fall | Lösung |
|---|---|---|---|---|---|
| 023 | a | Der effektive Jahreszins | Der effektive Jahreszins (Effektivzins) | Synonym / gleichwertiger Begriff: „Effektivzins“ gleichwertig | (a) |
