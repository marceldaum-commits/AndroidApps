package de.experimentopia.core.model

/** Ein Punkt der Theorie: Bild + Satz */
data class TheoryPoint(val emoji: String, val text: String)

/**
 * Theorie zu einem Fachbereich – kurz, mit Bildern, ohne die Lösung eines bestimmten Versuchs zu verraten.
 * [basics] für alle, [pro] für Klasse 7–10 (Formeln, Fachwörter), [everyday] ein Beispiel aus dem Alltag.
 */
data class TheoryCard(val topic: Topic, val idea: String, val basics: List<TheoryPoint>, val pro: List<TheoryPoint>, val everyday: String, val words: List<Pair<String, String>>)

private fun p(e: String, t: String) = TheoryPoint(e, t)

object Theory {
    fun of(t: Topic): TheoryCard = cards.getValue(t)
    fun of(e: Experiment): TheoryCard = of(e.topic)

    private val cards: Map<Topic, TheoryCard> = listOf(
        TheoryCard(Topic.AUFTRIEB, "Ob etwas schwimmt, hängt nicht davon ab, wie schwer es ist – sondern wie dicht es ist.",
            listOf(
                p("🧱", "Dichte heißt: Wie viel Masse steckt in einem bestimmten Raum? Ein Kubikzentimeter Wasser wiegt 1 g, einer aus Eisen fast 8 g."),
                p("🏊", "Ist ein Ding im Mittel weniger dicht als die Flüssigkeit, schwimmt es. Ist es dichter, sinkt es. Gleich dicht: es schwebt."),
                p("⬆️", "Wasser drückt alles, was eintaucht, nach oben. Diese Kraft heißt Auftrieb."),
                p("🧂", "Salzwasser ist dichter als Süßwasser – darum trägt es mehr."),
            ),
            listOf(
                p("🧮", "Dichte ρ = Masse ÷ Volumen, Einheit g/cm³. Wasser: 1,0 g/cm³."),
                p("📜", "Archimedes: Die Auftriebskraft ist so groß wie die Gewichtskraft der verdrängten Flüssigkeit."),
                p("🚢", "Ein schwimmendes Ding taucht so tief ein, bis es genau sein eigenes Gewicht an Wasser verdrängt."),
            ),
            "Im Toten Meer kann man auf dem Wasser liegen und Zeitung lesen – das Wasser ist so salzig, dass es sehr dicht ist.",
            listOf("Dichte" to "Masse pro Volumen", "Auftrieb" to "Kraft, mit der eine Flüssigkeit nach oben drückt", "Verdrängen" to "Platz im Wasser einnehmen")),

        TheoryCard(Topic.HEBEL, "Mit einem Hebel kann man mit wenig Kraft viel heben – wenn man weit weg vom Drehpunkt drückt.",
            listOf(
                p("⚖️", "Jeder Hebel hat einen Drehpunkt. Die Wippe auf dem Spielplatz ist ein Hebel."),
                p("↔️", "Je weiter weg vom Drehpunkt eine Kraft wirkt, desto mehr Wirkung hat sie."),
                p("🧒", "Ein leichtes Kind weit außen kann ein schweres Kind nahe am Drehpunkt hochheben."),
            ),
            listOf(
                p("🧮", "Hebelgesetz: Kraft × Kraftarm = Last × Lastarm. Gleichgewicht, wenn beide Seiten gleich sind."),
                p("🔄", "Das Produkt Kraft × Abstand heißt Drehmoment (Einheit N·m)."),
                p("🛒", "Zweiseitiger Hebel: Last und Kraft auf verschiedenen Seiten (Wippe). Einseitiger Hebel: beide auf derselben Seite (Schubkarre, Nussknacker)."),
            ),
            "Eine Türklinke, eine Schere und ein Flaschenöffner sind Hebel – lange Griffe machen die Arbeit leicht.",
            listOf("Drehpunkt" to "Punkt, um den sich der Hebel dreht", "Kraftarm" to "Abstand der Kraft vom Drehpunkt", "Lastarm" to "Abstand der Last vom Drehpunkt")),

        TheoryCard(Topic.MASCHINEN, "Rollen und Rampen sparen Kraft – aber man muss dafür einen längeren Weg gehen.",
            listOf(
                p("🏗️", "Eine feste Rolle lenkt nur die Kraft um: Man zieht nach unten, die Last geht nach oben."),
                p("🪢", "Eine lose Rolle hängt an zwei Seilstücken. Jedes trägt die Hälfte – man braucht nur halb so viel Kraft."),
                p("📐", "Auf einer Rampe schiebt man eine Last schräg nach oben. Je länger und flacher die Rampe, desto weniger Kraft."),
            ),
            listOf(
                p("🧮", "Goldene Regel der Mechanik: Was man an Kraft spart, muss man an Weg zulegen. Kraft × Weg (die Arbeit) bleibt gleich – ohne Reibung."),
                p("🔢", "Flaschenzug mit n tragenden Seilstücken: Zugkraft = Gewichtskraft ÷ n, Seilweg = Hubhöhe × n."),
                p("⚡", "Arbeit W = F · s, Einheit Joule (J). Gewichtskraft F = m · g mit g ≈ 9,81 N/kg."),
            ),
            "Serpentinen am Berg sind lange, flache Rampen: Der Weg ist länger, aber man kommt leichter hoch.",
            listOf("Rolle" to "Rad, über das ein Seil läuft", "Schiefe Ebene" to "Rampe", "Arbeit" to "Kraft mal Weg")),

        TheoryCard(Topic.NEWTON, "Kräfte verändern Bewegungen – und ohne Kraft bleibt eine Bewegung, wie sie ist.",
            listOf(
                p("🧊", "Trägheit: Was rollt, will weiterrollen. Was steht, will stehen bleiben."),
                p("🛒", "Je größer die Kraft, desto schneller wird etwas schneller. Je schwerer es ist, desto langsamer geht das."),
                p("🛹", "Wenn du etwas wegschubst, schubst es dich genauso stark zurück."),
            ),
            listOf(
                p("1️⃣", "1. Newtonsches Gesetz: Ohne Kraft bleibt ein Körper in Ruhe oder bewegt sich gleichförmig geradeaus."),
                p("2️⃣", "2. Gesetz: F = m · a. Beschleunigung a = Kraft ÷ Masse, Einheit m/s²."),
                p("3️⃣", "3. Gesetz (actio = reactio): Zu jeder Kraft gibt es eine gleich große Gegenkraft in entgegengesetzter Richtung."),
            ),
            "Ein Puck gleitet lange über das glatte Eis, weil ihn kaum Reibung bremst.",
            listOf("Trägheit" to "Körper behalten ihre Bewegung bei", "Beschleunigung" to "wie schnell die Geschwindigkeit sich ändert", "Reibung" to "Kraft, die Bewegungen bremst")),

        TheoryCard(Topic.FALL, "Ohne Luft fallen alle Dinge gleich schnell – egal wie schwer.",
            listOf(
                p("🌍", "Die Erde zieht alles an. Diese Kraft heißt Schwerkraft oder Gewichtskraft."),
                p("🪶", "Die Luft bremst. Leichte, breite Dinge (Feder, Papier) bremst sie stark, schwere, kleine (Kugel) kaum."),
                p("🌙", "Auf dem Mond zieht die Schwerkraft schwächer, und es gibt keine Luft."),
            ),
            listOf(
                p("🧮", "Freier Fall ohne Luft: Fallstrecke h = ½ · g · t², also t = √(2h ÷ g). Auf der Erde g ≈ 9,81 m/s², auf dem Mond 1,62 m/s²."),
                p("🪂", "Mit Luftwiderstand wird ein Körper schneller, bis Luftwiderstand und Gewichtskraft gleich groß sind (Endgeschwindigkeit)."),
                p("🔭", "Galilei erkannte: Die Fallzeit hängt nicht von der Masse ab, wenn die Luft keine Rolle spielt."),
            ),
            "1971 ließ ein Astronaut auf dem Mond einen Hammer und eine Feder fallen – ohne Luft landeten beide gleichzeitig.",
            listOf("Schwerkraft" to "Anziehung der Erde", "Luftwiderstand" to "Bremsen durch die Luft", "Vakuum" to "Raum ohne Luft")),

        TheoryCard(Topic.SCHWINGUNG, "Ein Pendel schwingt im gleichen Takt hin und her – darum gehen Pendeluhren genau.",
            listOf(
                p("🕰️", "Eine Schwingung ist einmal hin und wieder zurück. Die Zeit dafür heißt Schwingungsdauer."),
                p("📏", "Ein längeres Pendel schwingt langsamer, ein kürzeres schneller."),
                p("⚖️", "Wie schwer das Pendel ist, spielt fast keine Rolle."),
            ),
            listOf(
                p("🧮", "Für kleine Ausschläge: T = 2π · √(l ÷ g). Vierfache Länge → doppelte Dauer."),
                p("📐", "Bei großen Ausschlägen wird die Dauer etwas länger – darum haben Uhren nur kleine Ausschläge."),
            ),
            "Auf der Schaukel schwingst du wie ein Pendel – mit langen Ketten langsamer.",
            listOf("Schwingungsdauer" to "Zeit für einmal hin und zurück", "Ausschlag" to "wie weit das Pendel ausschwingt")),

        TheoryCard(Topic.STROM, "Strom fließt nur, wenn der Kreis geschlossen ist – wie Wasser in einem Rohr ohne Loch.",
            listOf(
                p("🔋", "Eine Batterie treibt den Strom an. Er fließt vom einen Pol durch Kabel und Lampe zum anderen Pol."),
                p("💡", "Leiter lassen Strom durch (Metalle, Graphit), Isolatoren nicht (Holz, Plastik, Gummi, Glas)."),
                p("🔀", "Reihenschaltung: alles hintereinander, ein Weg. Parallelschaltung: mehrere Wege nebeneinander."),
                p("⚠️", "Kurzschluss: Der Strom nimmt einen Weg ohne Widerstand – Kabel werden heiß. Nie an der Steckdose experimentieren!"),
            ),
            listOf(
                p("🧮", "Ohmsches Gesetz: U = R · I. Spannung in Volt (V), Stromstärke in Ampere (A), Widerstand in Ohm (Ω)."),
                p("➕", "Reihe: Widerstände addieren sich, der Strom ist überall gleich, die Spannung teilt sich auf."),
                p("🔀", "Parallel: An jedem Zweig liegt dieselbe Spannung, die Ströme addieren sich."),
            ),
            "Lichterketten im Haus sind parallel geschaltet: Geht eine Lampe kaputt, leuchten die anderen weiter.",
            listOf("Stromkreis" to "geschlossener Weg für den Strom", "Spannung" to "Antrieb des Stroms (Volt)", "Widerstand" to "wie stark etwas den Strom bremst")),

        TheoryCard(Topic.MAGNET, "Magnete ziehen nur bestimmte Metalle an – und haben zwei verschiedene Pole.",
            listOf(
                p("🧲", "Magnete ziehen Eisen und die meisten Stahlsorten an, außerdem Nickel und Kobalt. Alu, Kupfer, Gold, Holz und Plastik nicht."),
                p("🔴", "Jeder Magnet hat einen Nordpol und einen Südpol."),
                p("↔️", "Gleiche Pole stoßen sich ab, verschiedene ziehen sich an."),
                p("📄", "Die Magnetkraft wirkt auch durch Papier, Holz und Wasser hindurch."),
            ),
            listOf(
                p("🧭", "Die Erde ist selbst ein großer Magnet. Die Kompassnadel richtet sich nach ihrem Magnetfeld aus."),
                p("🌀", "Um einen Magneten herum liegt ein Magnetfeld; Eisenspäne zeigen seine Feldlinien."),
            ),
            "Kühlschrankmagnete halten, weil die Tür aus Stahlblech ist.",
            listOf("Pol" to "Ende eines Magneten, wo er am stärksten zieht", "Magnetfeld" to "Raum, in dem die Magnetkraft wirkt")),

        TheoryCard(Topic.WAERME, "Alles besteht aus winzigen Teilchen. Wärme heißt: Die Teilchen bewegen sich schneller.",
            listOf(
                p("🧊", "Fest: Teilchen sitzen fest am Platz und zittern. Flüssig: Sie rutschen aneinander vorbei. Gasförmig: Sie fliegen frei herum."),
                p("🌡️", "Jeder Stoff schmilzt und siedet bei seiner eigenen Temperatur. Wasser: schmilzt bei 0 °C, siedet bei 100 °C."),
                p("🛀", "Mischt man warm und kalt, gibt das Warme Wärme ab und das Kalte nimmt sie auf."),
            ),
            listOf(
                p("🧮", "Wärme Q = m · c · ΔT. c ist die spezifische Wärmekapazität: Wasser 4,19 J/(g·K), Eisen 0,45 J/(g·K)."),
                p("⏸️", "Beim Schmelzen und Sieden bleibt die Temperatur gleich – die Energie löst die Teilchen voneinander (Schmelz- und Verdampfungswärme)."),
                p("⚖️", "Mischregel (Richmann): m₁ · c₁ · (T₁ − T) = m₂ · c₂ · (T − T₂)."),
            ),
            "Eine Wärmflasche bleibt lange warm, weil Wasser viel Wärme speichern kann.",
            listOf("Aggregatzustand" to "fest, flüssig oder gasförmig", "Schmelztemperatur" to "ab hier wird ein Stoff flüssig", "Teilchenmodell" to "Vorstellung: alles besteht aus winzigen Teilchen")),

        TheoryCard(Topic.LICHT, "Licht mischt sich anders als Malfarbe: Licht addiert sich, Malfarbe nimmt Licht weg.",
            listOf(
                p("🔦", "Aus rotem, grünem und blauem Licht kann man fast alle Farben mischen. Alle drei zusammen ergeben Weiß."),
                p("🎨", "Malfarben schlucken einen Teil des Lichts. Je mehr Farben man mischt, desto dunkler wird es."),
                p("🌈", "Weißes Licht besteht aus allen Regenbogenfarben. Ein Prisma fächert sie auf."),
                p("🎞️", "Ein Farbfilter lässt nur seine eigene Farbe durch und schluckt den Rest."),
            ),
            listOf(
                p("➕", "Additive Mischung (Licht): Rot + Grün = Gelb, Grün + Blau = Cyan, Rot + Blau = Magenta."),
                p("➖", "Subtraktive Mischung (Farbe, Filter): Cyan, Magenta und Gelb nehmen je eine Lichtfarbe weg."),
                p("🔺", "Brechung: Licht ändert beim Übergang zwischen Luft und Glas die Richtung. Violett wird stärker gebrochen als Rot."),
            ),
            "Jeder Bildschirm besteht aus winzigen roten, grünen und blauen Lichtpunkten.",
            listOf("additiv" to "Licht kommt dazu", "subtraktiv" to "Licht wird weggenommen", "Spektrum" to "alle Farben des Lichts nebeneinander")),

        TheoryCard(Topic.SCHALL, "Schall ist Schwingung: Etwas zittert und bringt die Luft zum Zittern.",
            listOf(
                p("🎵", "Je schneller etwas schwingt, desto höher klingt der Ton."),
                p("📏", "Kurze Saiten, kurze Luftsäulen und kleine Dinge klingen hoch, lange und große tief."),
                p("💨", "Schall braucht Luft, Wasser oder einen festen Stoff. Im Weltall ist es still."),
            ),
            listOf(
                p("🧮", "Frequenz f in Hertz (Hz) = Schwingungen pro Sekunde. Doppelte Frequenz = eine Oktave höher."),
                p("🎸", "Saite: Halbe Länge → doppelte Frequenz. Röhre, die an einer Seite zu ist: f ≈ c ÷ (4 · L), c = 343 m/s."),
            ),
            "Bei einer Gitarre drückt man die Saite auf das Griffbrett: Der schwingende Teil wird kürzer, der Ton höher.",
            listOf("Frequenz" to "Schwingungen pro Sekunde", "Oktave" to "doppelte Frequenz", "Hertz" to "Einheit der Frequenz")),

        TheoryCard(Topic.TRENNEN, "Gemische trennt man, indem man ausnutzt, worin sich die Stoffe unterscheiden.",
            listOf(
                p("🧺", "Sieben: Große Körner bleiben hängen, kleine fallen durch."),
                p("🧲", "Magnet: Holt Eisen heraus."),
                p("☕", "Filtern: Feste Körner bleiben im Filter, Flüssiges und Gelöstes läuft durch."),
                p("🔥", "Eindampfen: Das Wasser verdampft, Gelöstes wie Salz bleibt zurück."),
                p("⏳", "Scheidetrichter: Flüssigkeiten, die sich nicht mischen (Öl und Wasser), trennt man nach der Dichte."),
            ),
            listOf(
                p("🖊️", "Chromatografie: Farbstoffe wandern verschieden weit mit dem Laufmittel, weil sie verschieden stark am Papier haften."),
                p("🧪", "Reinstoff = nur ein Stoff; Gemisch = mehrere Stoffe. Lösung = ein Stoff ist in einem anderen unsichtbar verteilt."),
            ),
            "Im Klärwerk wird Abwasser gesiebt und gefiltert, bevor es zurück in den Fluss darf.",
            listOf("Gemisch" to "mehrere Stoffe zusammen", "Lösung" to "Gelöstes ist unsichtbar verteilt", "Filtrat" to "was durch den Filter läuft")),

        TheoryCard(Topic.SAEURE, "Sauer, neutral oder basisch – das verrät der pH-Wert.",
            listOf(
                p("🍋", "Saure Stoffe: Zitrone, Essig, Cola. Basische Stoffe (Laugen): Seife, Natron, Waschsoda."),
                p("🟣", "Rotkohlsaft ist ein Indikator: sauer wird er rot, neutral violett, basisch blau, grün oder gelb."),
                p("📏", "pH-Wert: unter 7 sauer, genau 7 neutral, über 7 basisch."),
                p("⚠️", "Starke Säuren und Laugen sind ätzend – nur mit Schutzbrille im Chemieraum."),
            ),
            listOf(
                p("⚖️", "Neutralisation: Säure + Lauge → Salz + Wasser. Gleich viele Säure- und Laugenteilchen heben sich auf."),
                p("🧮", "Konzentration in mol/L. Die Menge in mol = Konzentration × Volumen."),
                p("🔟", "Eine Stufe auf der pH-Skala bedeutet zehnmal mehr oder weniger saure Teilchen (H⁺)."),
                p("💧", "Verdünnen bringt den pH-Wert näher an 7 – aber nie darüber hinaus."),
            ),
            "Gegen Sodbrennen hilft Natron: Es neutralisiert zu viel Magensäure.",
            listOf("Indikator" to "Stoff, der durch Farbe sauer oder basisch anzeigt", "neutralisieren" to "Säure und Lauge heben sich auf", "Konzentration" to "wie viel gelöst ist")),

        TheoryCard(Topic.REAKTION, "Bei einer chemischen Reaktion entstehen neue Stoffe – aber kein Atom geht verloren.",
            listOf(
                p("⚛️", "Alle Stoffe bestehen aus Atomen. Bei einer Reaktion ordnen sich die Atome neu."),
                p("💥", "Man erkennt eine Reaktion an neuen Eigenschaften: Gas, Farbe, Licht, Wärme."),
                p("🔥", "Verbrennen ist eine Reaktion mit Sauerstoff aus der Luft."),
            ),
            listOf(
                p("⚖️", "Reaktionsgleichung: Links und rechts stehen gleich viele Atome jeder Sorte (Massenerhaltung)."),
                p("🔢", "Ausgleichen nur mit den Zahlen vor den Formeln (Koeffizienten), nie mit den kleinen Zahlen in den Formeln."),
                p("🔄", "Oxidation: Ein Stoff verbindet sich mit Sauerstoff. Redoxreaktion: Ein Stoff gibt Sauerstoff ab, ein anderer nimmt ihn auf."),
            ),
            "Rost entsteht, wenn Eisen langsam mit Sauerstoff und Wasser reagiert.",
            listOf("Atom" to "kleinster Baustein eines Elements", "Molekül" to "verbundene Atome", "Koeffizient" to "Zahl vor einer Formel")),

        TheoryCard(Topic.VERERBUNG, "Eltern geben ihren Kindern Erbanlagen weiter – von jedem Elternteil eine.",
            listOf(
                p("🧬", "Für jedes Merkmal hat ein Lebewesen zwei Erbanlagen (Allele): eine von der Mutter, eine vom Vater."),
                p("💪", "Dominant: Diese Anlage setzt sich durch und ist zu sehen. Rezessiv: Sie zeigt sich nur, wenn beide Anlagen gleich sind."),
                p("🎲", "Welche Anlage ein Kind bekommt, ist Zufall – wie beim Münzwurf."),
            ),
            listOf(
                p("🔲", "Kreuzungsquadrat: Die Anlagen der Eltern an den Rändern, im Feld alle möglichen Kombinationen – jede gleich wahrscheinlich."),
                p("📜", "Mendels Regeln: Uniformitätsregel (AA × aa → alle Aa), Spaltungsregel (Aa × Aa → 3 : 1 bei dominant-rezessiv)."),
                p("🩸", "Intermediär: Mischerbige sehen anders aus (rosa). Kodominant: Beide zeigen sich (Blutgruppe AB)."),
                p("🔤", "Genotyp = die Erbanlagen (Aa). Phänotyp = das Aussehen (gelb)."),
            ),
            "Zwei schwarze Labradore können gelbe Welpen bekommen – wenn beide die Anlage für Gelb versteckt tragen.",
            listOf("Allel" to "eine Form einer Erbanlage", "dominant" to "setzt sich durch", "rezessiv" to "zeigt sich nur doppelt")),

        TheoryCard(Topic.ZELLEN, "Alle Lebewesen bestehen aus Zellen – viele so klein, dass man ein Mikroskop braucht.",
            listOf(
                p("🔬", "Ein Mikroskop vergrößert Winziges. Je stärker es vergrößert, desto kleiner ist der Ausschnitt, den man sieht."),
                p("🦠", "Einzeller wie das Pantoffeltierchen bestehen aus nur einer Zelle und erledigen damit alles."),
                p("🥔", "Pflanzenzellen haben eine feste Zellwand und einen großen Saftraum (Vakuole)."),
                p("💧", "Osmose: Wasser wandert durch die Zellhaut dorthin, wo mehr Salz oder Zucker gelöst ist."),
            ),
            listOf(
                p("🧮", "Gesamtvergrößerung = Okular × Objektiv, z. B. 10 × 40 = 400-fach."),
                p("🧫", "Die Zellmembran ist halbdurchlässig: Wasser kommt durch, gelöste Stoffe kaum."),
                p("🌀", "Plasmolyse: In salziger Umgebung verliert die Pflanzenzelle Wasser, der Saftraum schrumpft, die Zellwand bleibt."),
            ),
            "Schlapper Salat wird in kaltem Wasser wieder knackig – seine Zellen nehmen Wasser auf.",
            listOf("Zelle" to "kleinster Baustein des Lebens", "Vakuole" to "Saftraum der Pflanzenzelle", "Osmose" to "Wasser wandert durch eine Zellhaut")),

        TheoryCard(Topic.ERNAEHRUNG, "Unsere Nahrung enthält Nährstoffe – und Enzyme zerlegen sie bei der Verdauung.",
            listOf(
                p("🍞", "Kohlenhydrate (Stärke, Zucker) liefern Energie. Fette liefern viel Energie. Eiweiße braucht der Körper zum Aufbauen."),
                p("🔍", "Mit Nachweisen findet man Nährstoffe: Iod färbt Stärke blau-schwarz, Fett macht einen bleibenden Fleck auf Papier."),
                p("👅", "Die Verdauung beginnt im Mund: Speichel zerlegt Stärke in Zucker."),
            ),
            listOf(
                p("🧪", "Enzyme sind Eiweiße, die Reaktionen beschleunigen. Jedes Enzym hat seine Lieblingstemperatur (im Körper etwa 37 °C)."),
                p("♨️", "Zu viel Hitze verändert die Form eines Enzyms dauerhaft (Denaturierung) – dann arbeitet es nicht mehr."),
                p("🧾", "Nachweise: Glukose-Teststreifen für Traubenzucker, Biuret-Probe (violett) für Eiweiß."),
                p("🔬", "Ein Kontrollversuch zeigt, ob wirklich die vermutete Ursache wirkt."),
            ),
            "Unreife Bananen schmecken mehlig, reife süß – beim Reifen wird Stärke zu Zucker umgebaut.",
            listOf("Nährstoff" to "Stoff in der Nahrung, den der Körper braucht", "Enzym" to "Eiweiß, das Stoffe zerlegt oder umbaut", "Nachweis" to "Test, ob ein Stoff da ist")),

        TheoryCard(Topic.OEKOLOGIE, "Pflanzen machen aus Licht Nahrung – und alle Tiere hängen davon ab.",
            listOf(
                p("🌿", "Fotosynthese: Pflanzen bauen mit Licht aus Wasser und Kohlenstoffdioxid Zucker auf. Dabei entsteht Sauerstoff."),
                p("🌱", "Samen keimen, wenn sie Wasser, Wärme und Luft bekommen. Licht brauchen sie erst danach."),
                p("🦊", "Nahrungskette: Pflanze → Pflanzenfresser → Fleischfresser. Der Pfeil heißt „wird gefressen von“."),
                p("🐇", "Räuber und Beute hängen voneinander ab: Viel Beute → mehr Räuber → weniger Beute → weniger Räuber."),
            ),
            listOf(
                p("⚗️", "Fotosynthese: Kohlenstoffdioxid + Wasser → (Lichtenergie) → Traubenzucker + Sauerstoff."),
                p("🔺", "Energiepyramide: Von Stufe zu Stufe kommt nur etwa ein Zehntel der Energie an (10-%-Regel)."),
                p("☠️", "Gifte, die nicht abgebaut werden, reichern sich am Ende der Nahrungskette an."),
                p("🕸️", "Viele Nahrungsketten zusammen bilden ein Nahrungsnetz."),
            ),
            "Ohne Pflanzen gäbe es keinen Sauerstoff zum Atmen und keine Nahrung für Tiere.",
            listOf("Produzent" to "Pflanze, die ihre Nahrung selbst herstellt", "Konsument" to "Tier, das andere frisst", "Keimung" to "Samen beginnt zu wachsen")),

        TheoryCard(Topic.KOERPER, "Das Herz ist eine Pumpe, die Tag und Nacht Blut durch den Körper schickt.",
            listOf(
                p("❤️", "Das Herz ist ein Muskel, etwa so groß wie deine Faust."),
                p("✋", "Bei jedem Schlag spürt man eine Welle in den Adern: den Puls, am Handgelenk oder am Hals."),
                p("🏃", "Beim Rennen brauchen die Muskeln mehr Sauerstoff – das Herz schlägt schneller."),
            ),
            listOf(
                p("🧮", "Herzzeitvolumen = Puls × Schlagvolumen: In Ruhe etwa 70 × 70 ml ≈ 5 Liter pro Minute."),
                p("📉", "Ruhepuls: Kinder etwa 70–110, Erwachsene etwa 60–80 Schläge pro Minute. Training senkt ihn."),
                p("⏱️", "Erholungspuls: Je schneller der Puls nach Anstrengung sinkt, desto fitter ist das Herz-Kreislauf-System."),
            ),
            "Ein Herz schlägt in einem langen Leben rund drei Milliarden Mal.",
            listOf("Puls" to "Druckwelle bei jedem Herzschlag", "Kreislauf" to "Weg des Blutes durch den Körper", "Ruhepuls" to "Puls, wenn man still sitzt")),
    ).associateBy { it.topic }
}
