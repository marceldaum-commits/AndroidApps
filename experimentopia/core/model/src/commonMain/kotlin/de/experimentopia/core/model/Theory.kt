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
                p("🔍", "Vergleiche die Zahlen: Kork hat 0,25 g/cm³, das ist weniger als Wasser mit 1 g/cm³, also schwimmt Kork. Glas hat 2,5 g/cm³, das ist mehr, also sinkt Glas."),
            ),
            listOf(
                p("📜", "Archimedes: Die Auftriebskraft ist so groß wie die Gewichtskraft der verdrängten Flüssigkeit."),
                p("🚢", "Ein schwimmendes Ding taucht so tief ein, bis es genau sein eigenes Gewicht an Wasser verdrängt."),
                p("🧮", "Dichte ρ = m ÷ V (m = Masse in g, V = Volumen in cm³, ρ in g/cm³): Ein Stein mit 30 g und 12 cm³ hat ρ = 30 g ÷ 12 cm³ = 2,5 g/cm³. Das ist mehr als 1,0 g/cm³, also sinkt er."),
                p("⚓", "Eintauchanteil beim Schwimmen = ρ Körper ÷ ρ Flüssigkeit: Ein Kunststoffklotz mit 0,8 g/cm³ liegt in Wasser (1,0 g/cm³) zu 0,8 ÷ 1,0 = 8 Zehnteln (80 %) unter Wasser."),
                p("🏋️", "Auftriebskraft F_A = ρ_Fl · V · g (ρ_Fl = Dichte der Flüssigkeit, V = eingetauchtes Volumen, g = 9,81 N/kg): Ein Körper mit 250 cm³ ganz in Wasser verdrängt 250 g = 0,25 kg Wasser, also F_A = 0,25 kg · 9,81 N/kg ≈ 2,5 N."),
            ),
            "Im Toten Meer kann man auf dem Wasser liegen und Zeitung lesen – das Wasser ist so salzig, dass es sehr dicht ist.",
            listOf("Dichte" to "Masse pro Volumen", "Auftrieb" to "Kraft, mit der eine Flüssigkeit nach oben drückt", "Verdrängen" to "Platz im Wasser einnehmen")),

        TheoryCard(Topic.HEBEL, "Mit einem Hebel kann man mit wenig Kraft viel heben – wenn man weit weg vom Drehpunkt drückt.",
            listOf(
                p("⚖️", "Jeder Hebel hat einen Drehpunkt. Die Wippe auf dem Spielplatz ist ein Hebel."),
                p("↔️", "Je weiter weg vom Drehpunkt eine Kraft wirkt, desto mehr Wirkung hat sie."),
                p("🧒", "Ein leichtes Kind weit außen kann ein schweres Kind nahe am Drehpunkt hochheben."),
                p("⚖️", "Rechentrick Wippe: Masse mal Feld muss links und rechts gleich sein. Sitzen mehrere auf einer Seite, zählt man zusammen. Beispiel: 10 kg auf Feld 3 und 5 kg auf Feld 2 ergeben 30 + 10 = 40. Rechts passt dann 8 kg auf Feld 5, denn 8 · 5 = 40."),
            ),
            listOf(
                p("🧮", "Hebelgesetz: Kraft × Kraftarm = Last × Lastarm. Gleichgewicht, wenn beide Seiten gleich sind."),
                p("🔄", "Das Produkt Kraft × Abstand heißt Drehmoment (Einheit N·m)."),
                p("🛒", "Zweiseitiger Hebel: Last und Kraft auf verschiedenen Seiten (Wippe). Einseitiger Hebel: beide auf derselben Seite (Schubkarre, Nussknacker)."),
                p("🧮", "Hebelgesetz F₁ · a₁ = F₂ · a₂ (F in N, Abstände a in m) mit Gewichtskraft F = m · g (g = 9,81 N/kg): Eine Last von 50 kg (490,5 N) liegt 0,2 m vom Drehpunkt, das ergibt 98,1 N·m. Bei 1 m Kraftarm braucht man F = 98,1 N·m ÷ 1 m ≈ 98 N."),
                p("➕", "Mehrere Kräfte auf einer Seite: Man addiert die Drehmomente. 20 N bei 0,3 m und 10 N bei 0,6 m ergeben 6 N·m + 6 N·m = 12 N·m. Auf der anderen Seite reichen dafür 24 N bei 0,5 m."),
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
                p("⛰️", "Schiefe Ebene ohne Reibung: F = G · h ÷ l (G = Gewichtskraft in N, h = Höhe, l = Rampenlänge, beide in m). Eine 30-kg-Kiste (G ≈ 294 N) auf eine 6 m lange Rampe 1,5 m hoch braucht F = 294 N · 1,5 ÷ 6 ≈ 74 N."),
                p("🧮", "Flaschenzug durchgerechnet: 120 kg (G ≈ 1177 N) mit 5 tragenden Seilstücken braucht F = 1177 N ÷ 5 ≈ 235 N, und für 2 m Hub zieht man 10 m Seil. Schafft man nur 250 N, braucht man n ≥ 1177 ÷ 250 = 4,7 – aufgerundet 5 Seilstücke."),
                p("💪", "Hubarbeit W = m · g · h (W in J): Die 30-kg-Kiste 1,5 m hoch: W = 30 · 9,81 · 1,5 ≈ 441 J – genauso viel wie 74 N · 6 m auf der Rampe. Die Kraft wird kleiner, die Arbeit bleibt gleich."),
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
                p("🧮", "F = m · a durchgerechnet (F in N, m in kg, a in m/s²): Ein 3-kg-Wagen mit 12 N bekommt a = 12 N ÷ 3 kg = 4 m/s². Umgekehrt brauchen 5 kg für 3 m/s² die Kraft F = 5 kg · 3 m/s² = 15 N."),
                p("🏁", "Aus dem Stand mit gleichbleibender Beschleunigung: v = a · t und s = ½ · a · t². Beispiel a = 4 m/s²: Nach 2 s ist v = 8 m/s und s = ½ · 4 · 2² = 8 m."),
                p("🛹", "Abstoßen: Beide spüren gleich lange dieselbe Kraft, also m₁ · v₁ = m₂ · v₂ – die Geschwindigkeiten verhalten sich umgekehrt wie die Massen. Bei 50 kg und 25 kg rollt das 25-kg-Kind doppelt so schnell."),
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
                p("⏱️", "Beispiel freier Fall aus h = 45 m: t = √(2 · 45 m ÷ 9,81 m/s²) = √9,17 s² ≈ 3,0 s. Am Boden ist v = g · t = 9,81 m/s² · 3,0 s ≈ 30 m/s (rund 107 km/h)."),
                p("📐", "Verhältnisse: Die Fallzeit wächst mit √h und mit 1/√g. Neunfache Höhe gibt dreifache Zeit, ein Viertel von g gibt doppelte Zeit. Sprunghöhe h = v² ÷ (2g): Bei halbem g springt man mit gleichem Absprung doppelt so hoch."),
                p("🪶", "Ist die Endgeschwindigkeit erreicht, fällt der Körper gleichmäßig weiter: s = v · t, also t = s ÷ v. Ein Ahornsamen mit 1 m/s braucht für 3 m etwa 3 m ÷ 1 m/s = 3 s."),
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
                p("🧮", "Für kleine Ausschläge: T = 2π · √(l ÷ g). Länge und g stehen unter der Wurzel: 16-fache Länge ergibt vierfache Dauer, ein Viertel von g ergibt doppelte Dauer."),
                p("📐", "Bei großen Ausschlägen wird die Dauer etwas länger – darum haben Uhren nur kleine Ausschläge."),
                p("⏱️", "T = 2π · √(l ÷ g) (T in s, l in m, g in m/s²): Ein 2,25-m-Pendel auf der Erde hat T = 6,28 · √(2,25 ÷ 9,81) s = 6,28 · 0,48 s ≈ 3,0 s."),
                p("📏", "Umgestellt nach der Länge: l = g · (T ÷ 2π)². Für T = 4 s braucht man l = 9,81 · (4 ÷ 6,28)² ≈ 9,81 · 0,405 ≈ 4,0 m."),
            ),
            "Auf der Schaukel schwingst du wie ein Pendel – mit langen Ketten langsamer.",
            listOf("Schwingungsdauer" to "Zeit für einmal hin und zurück", "Ausschlag" to "wie weit das Pendel ausschwingt")),

        TheoryCard(Topic.STROM, "Strom fließt nur, wenn der Kreis geschlossen ist – wie Wasser in einem Rohr ohne Loch.",
            listOf(
                p("🔋", "Eine Batterie treibt den Strom an. Er fließt vom einen Pol durch Kabel und Lampe zum anderen Pol."),
                p("💡", "Leiter lassen Strom durch (Metalle, Graphit), Isolatoren nicht (Holz, Plastik, Gummi, Glas)."),
                p("🔀", "Reihenschaltung: alles hintereinander, ein Weg. Parallelschaltung: mehrere Wege nebeneinander."),
                p("⚠️", "Kurzschluss: Der Strom nimmt einen Weg fast ohne Widerstand – es fließt sehr viel Strom, die Kabel werden heiß. Nie an der Steckdose experimentieren!"),
            ),
            listOf(
                p("🧮", "Ohmsches Gesetz: U = R · I. Spannung in Volt (V), Stromstärke in Ampere (A), Widerstand in Ohm (Ω)."),
                p("➕", "Reihe: Widerstände addieren sich, der Strom ist überall gleich, die Spannung teilt sich auf."),
                p("🔀", "Parallel: An jedem Zweig liegt dieselbe Spannung, die Ströme addieren sich."),
                p("➕", "Reihe: R_ges = R₁ + R₂ (R in Ω). Beispiel: 4 Ω + 8 Ω = 12 Ω an 12 V ergibt I = U ÷ R = 12 V ÷ 12 Ω = 1 A. Am 8-Ω-Widerstand liegen dann U = R · I = 8 Ω · 1 A = 8 V."),
                p("🔀", "Parallel: 1/R_ges = 1/R₁ + 1/R₂ – der Gesamtwiderstand ist kleiner als der kleinste Einzelwiderstand. Beispiel: 30 Ω und 60 Ω parallel an 12 V: Ströme 0,4 A + 0,2 A = 0,6 A, also R_ges = 12 V ÷ 0,6 A = 20 Ω."),
                p("💡", "Leistung P = U · I (P in Watt W, U in V, I in A): Eine Lampe mit 12 Ω an 6 V bekommt I = 0,5 A, also P = 6 V · 0,5 A = 3 W. Bei halber Spannung fließt nur halber Strom – die Leistung sinkt auf ein Viertel (0,75 W)."),
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

        TheoryCard(Topic.ELEKTROMAGNETISMUS, "Strom und Magnetismus gehören zusammen: Strom macht Magnetfelder – und bewegte Magnetfelder machen Strom.",
            listOf(
                p("🧲", "Fließt Strom durch einen Draht, entsteht ein Magnetfeld. Wickelt man den Draht zu einer Spule, wird das Feld stark – und es verschwindet, wenn der Strom ausgeschaltet wird (Elektromagnet)."),
                p("🔩", "Ein Eisenkern in der Spule verstärkt das Feld stark. Mehr Windungen oder mehr Strom machen es auch stärker."),
                p("⚙️", "Auf Strom im Magnetfeld wirkt eine Kraft. Polt man den Strom um, kehrt sich die Kraft um. Daraus baut man den Elektromotor: Eine Spule dreht sich, weil die Kraft sie immer weiterschiebt."),
                p("🚲", "Bewegt man einen Magneten in einer Spule, entsteht Spannung (Induktion) – aber nur, solange er sich bewegt. So arbeitet der Fahrraddynamo und das Kraftwerk."),
                p("🔌", "Ein Transformator hat zwei Spulen auf einem Eisenkern. Mehr Windungen auf der zweiten Spule, höhere Spannung; weniger Windungen, niedrigere Spannung. Er funktioniert nur mit Wechselspannung."),
            ),
            listOf(
                p("🌀", "Feld einer langen Spule: B = μ₀ · N · I ÷ l (B Flussdichte in Tesla (T), μ₀ = 1,2566·10⁻⁶ T·m/A, N Windungen, I Stromstärke in A, l Länge in m). Beispiel: N = 250, I = 1,6 A, l = 0,10 m: B = 1,2566·10⁻⁶ · 250 · 1,6 ÷ 0,10 = 5,03·10⁻³ T ≈ 5,0 mT. Mit Eisenkern (im Modell μr = 200) ist B 200-mal so groß: ≈ 1,0 T."),
                p("💪", "Kraft auf einen Leiter (senkrecht zum Feld): F = B · I · l (F in Newton, B in Tesla, I in Ampere, l in Meter). Beispiel: B = 0,1 T, I = 5 A, l = 0,06 m: F = 0,1 · 5 · 0,06 = 0,03 N. Richtung: Dreifingerregel der rechten Hand (technische Stromrichtung): Daumen = Strom, Zeigefinger = Feld (Nord → Süd), Mittelfinger = Kraft."),
                p("⚙️", "Gleichstrommotor (Modell, Leerlauf): Die Drehzahl ist proportional zur Spannung, n = 600 min⁻¹ je Volt · U. Beispiel: Für 3 000 min⁻¹ braucht man U = 3 000 ÷ 600 = 5 V. Mit Last dreht der echte Motor langsamer."),
                p("📈", "Induktionsgesetz: U = N · ΔΦ ÷ Δt (Φ magnetischer Fluss in Weber, 1 Wb = 1 V·s). Beispiel: N = 500, ΔΦ = 2,5·10⁻⁵ Wb, Δt = 0,125 s: U = 500 · 2,5·10⁻⁵ ÷ 0,125 = 0,0125 ÷ 0,125 = 0,10 V."),
                p("🔄", "Generator: Û = N · B · A · 2π · f (Scheitelspannung; A Fläche in m², f Frequenz in Hz). Effektivwert U_eff = Û ÷ √2. Beispiel: N = 100, B = 0,1 T, A = 0,002 m², f = 20 Hz: Û = 100 · 0,1 · 0,002 · 2π · 20 ≈ 2,51 V; U_eff ≈ 1,78 V. Die Steckdose liefert 230 V effektiv, das sind Û ≈ 325 V bei 50 Hz."),
                p("🔌", "Transformator: U₁ ÷ U₂ = N₁ ÷ N₂. Ideal ohne Verluste: P₁ = P₂, also I₁ ÷ I₂ = N₂ ÷ N₁. Beispiel: U₁ = 12 V, N₁ = 600, N₂ = 900: U₂ = 12 · 900 ÷ 600 = 18 V; bei R = 6 Ω ist I₂ = 18 ÷ 6 = 3 A, P = 18 · 3 = 54 W und I₁ = 54 ÷ 12 = 4,5 A."),
                p("🗼", "Fernleitung: Strom I = P ÷ U, Verlust P_V = I² · R. Beispiel: P = 2 MW, R = 5 Ω. Bei 10 kV: I = 200 A, P_V = 200² · 5 = 200 kW (10 %). Bei 100 kV: I = 20 A, P_V = 20² · 5 = 2 kW (0,1 %). Zehnfache Spannung = hundertfach kleinerer Verlust."),
            ),
            "Das Ladegerät deines Handys enthält einen Trafo, der Fahrraddynamo arbeitet nach dem Induktionsprinzip, und jede Straßenbahn wird von einem Elektromotor angetrieben.",
            listOf("Spule" to "aufgewickelter Draht mit vielen Windungen", "Induktion" to "Spannung durch sich änderndes Magnetfeld", "Tesla" to "Einheit der magnetischen Flussdichte",
                "Wechselspannung" to "Spannung, die ständig ihr Vorzeichen wechselt", "Transformator" to "wandelt Wechselspannung mit zwei Spulen um")),

        TheoryCard(Topic.WAERME, "Alles besteht aus winzigen Teilchen. Wärme heißt: Die Teilchen bewegen sich schneller.",
            listOf(
                p("🧊", "Fest: Teilchen sitzen fest am Platz und zittern. Flüssig: Sie rutschen aneinander vorbei. Gasförmig: Sie fliegen frei herum."),
                p("🌡️", "Jeder Stoff schmilzt und siedet bei seiner eigenen Temperatur. Wasser: schmilzt bei 0 °C, siedet bei 100 °C."),
                p("🛀", "Mischt man warm und kalt, gibt das Warme Wärme ab und das Kalte nimmt sie auf."),
                p("🥤", "Wasser mit Wasser mischen: Die Mischtemperatur liegt näher bei der größeren Menge. Man rechnet (Menge · Temperatur + Menge · Temperatur) ÷ Gesamtmenge. Beispiel: 100 g mit 90 °C und 300 g mit 30 °C ergeben (9000 + 9000) ÷ 400 = 45 °C."),
            ),
            listOf(
                p("🧮", "Wärme Q = m · c · ΔT. c ist die spezifische Wärmekapazität: Wasser 4,19 J/(g·K), Eisen 0,45 J/(g·K)."),
                p("⏸️", "Beim Schmelzen und Sieden bleibt die Temperatur gleich – die Energie löst die Teilchen voneinander (Schmelz- und Verdampfungswärme)."),
                p("⚖️", "Mischregel (Richmann): Abgegebene Wärme = aufgenommene Wärme, also m₁ · c₁ · (T₁ − T) = m₂ · c₂ · (T − T₂). T₁ = Temperatur des warmen Körpers, T₂ die des kalten, T die Mischtemperatur."),
                p("🔥", "Q = m · c · ΔT (Q in J, m in g, c in J/(g·K), ΔT = Temperaturänderung in K): 500 g Wasser von 20 °C auf 60 °C brauchen Q = 500 · 4,19 · 40 ≈ 83 800 J ≈ 84 kJ."),
                p("🧊", "Schmelzen braucht 334 J pro Gramm Eis, Verdampfen 2257 J pro Gramm Wasser – die Temperatur bleibt dabei stehen. 50 g Eis brauchen zum Schmelzen 50 · 334 J = 16 700 J ≈ 17 kJ."),
                p("🧪", "Mischregel durchgerechnet: 200 g Eisen (c = 0,45) mit 80 °C kommen in 300 g Wasser (c = 4,19) mit 20 °C. Aus 200 · 0,45 · (80 − T) = 300 · 4,19 · (T − 20) folgt 90 · (80 − T) = 1257 · (T − 20), also T ≈ 24 °C."),
            ),
            "Eine Wärmflasche bleibt lange warm, weil Wasser viel Wärme speichern kann.",
            listOf("Aggregatzustand" to "fest, flüssig oder gasförmig", "Schmelztemperatur" to "ab hier wird ein Stoff flüssig", "Teilchenmodell" to "Vorstellung: alles besteht aus winzigen Teilchen")),

        TheoryCard(Topic.LICHT, "Licht mischt sich anders als Malfarbe: Licht addiert sich, Malfarbe nimmt Licht weg.",
            listOf(
                p("🔦", "Aus rotem, grünem und blauem Licht kann man fast alle Farben mischen. Alle drei zusammen ergeben Weiß."),
                p("🎨", "Malfarben schlucken einen Teil des Lichts. Je mehr Farben man mischt, desto dunkler wird es."),
                p("🌈", "Weißes Licht besteht aus allen Regenbogenfarben. Ein Prisma fächert sie auf."),
                p("🎞️", "Ein Farbfilter lässt nur seine eigene Farbe durch und schluckt den Rest."),
                p("🎨", "Im Farbkreis liegt eine Mischfarbe immer zwischen ihren beiden Ausgangsfarben. Weiß dazu macht eine Farbe heller."),
            ),
            listOf(
                p("➕", "Additive Mischung (Licht): Rot + Grün = Gelb, Grün + Blau = Cyan, Rot + Blau = Magenta."),
                p("➖", "Subtraktive Mischung (Farbe, Filter): Cyan (Türkis) schluckt Rot, Magenta (Pink) schluckt Grün, Gelb schluckt Blau."),
                p("🔺", "Brechung: Licht ändert beim Übergang zwischen Luft und Glas die Richtung. Violett wird stärker gebrochen als Rot."),
                p("🎞️", "Folien-Rechnung: Starte mit Weiß = Rot + Grün + Blau und streiche bei jeder Folie die Farbe, die sie schluckt. Was übrig bleibt, kommt durch. Beispiel: Eine Türkis-Folie allein ergibt Weiß − Rot = Grün + Blau, also Türkis."),
                p("📺", "Zahl der Bildschirmfarben = Stufen · Stufen · Stufen (Rot, Grün und Blau mit gleich vielen Helligkeitsstufen): Mit 4 Stufen je Lampe sind es 4 · 4 · 4 = 64 Farben."),
            ),
            "Jeder Bildschirm besteht aus winzigen roten, grünen und blauen Lichtpunkten.",
            listOf("additiv" to "Licht kommt dazu", "subtraktiv" to "Licht wird weggenommen", "Spektrum" to "alle Farben des Lichts nebeneinander")),

        TheoryCard(Topic.OPTIK, "Licht läuft geradeaus – bis es auf einen Spiegel, eine Wasseroberfläche oder eine Linse trifft.",
            listOf(
                p("➡️", "Licht breitet sich geradlinig aus. Wo es nicht hinkommt, ist Schatten. Je näher die Lampe an einem Gegenstand, desto größer sein Schatten."),
                p("↩️", "Ein Spiegel wirft Licht zurück wie eine Wand einen Ball: So schräg es ankommt, so schräg fliegt es weiter."),
                p("💧", "Geht Licht schräg von Luft in Wasser oder Glas, knickt es ab. Darum sieht ein Strohhalm im Glas geknickt aus."),
                p("🔍", "Eine Sammellinse (Lupe) bündelt Licht. Hält man ein Blatt in den richtigen Abstand, erscheint ein Bild – auf dem Kopf."),
                p("👁️", "Auch dein Auge hat eine Linse: Sie wirft ein umgedrehtes Bild auf die Netzhaut, das Gehirn dreht es wieder um."),
                p("🌑", "Rechentrick Schatten: Steht die Lampe halb so weit von der Figur weg, wird der Schatten doppelt so groß. Beispiel: Wand 100 cm weg, Lampe 50 cm vor einer 10-cm-Figur → 20 cm Schatten; Lampe 25 cm vor der Figur → 40 cm."),
            ),
            listOf(
                p("📐", "Strahlensatz für Schatten: S = G · d_W ÷ d_G (S = Schattengröße, G = Gegenstandsgröße in cm, d_W = Abstand Lampe–Wand, d_G = Abstand Lampe–Gegenstand). Beispiel: G = 10 cm, d_W = 100 cm, d_G = 40 cm → S = 10 cm · 100 ÷ 40 = 25 cm."),
                p("📐", "Reflexionsgesetz: Einfallswinkel α = Reflexionswinkel β, beide gemessen gegen das Lot (die Senkrechte auf dem Spiegel). Dreht man den Spiegel um 10°, dreht sich der zurückgeworfene Strahl um 20°."),
                p("📏", "Brechungsgesetz (Snellius): n₁ · sin α = n₂ · sin β (n = Brechzahl: Luft 1,0; Wasser 1,33; Glas 1,5; Diamant 2,42). Beispiel Luft → Wasser bei α = 42°: sin β = 1,0 · 0,669 ÷ 1,33 ≈ 0,503, also β ≈ 30° – zum Lot hin."),
                p("🔦", "Totalreflexion: Von dicht nach dünn gibt es einen Grenzwinkel mit sin α_G = n₂ ÷ n₁. Glas → Luft: sin α_G = 1 ÷ 1,5 ≈ 0,667, α_G ≈ 42°. Darüber bleibt alles Licht drin – so funktionieren Glasfaserkabel."),
                p("🧮", "Linsengleichung: 1/f = 1/g + 1/b (f = Brennweite, g = Gegenstandsweite, b = Bildweite, alles in cm). Beispiel f = 10 cm, g = 25 cm: 1/b = 1/10 − 1/25 = 0,06 → b ≈ 16,7 cm."),
                p("🔎", "Abbildungsmaßstab: B ÷ G = b ÷ g (B = Bildgröße). Beispiel f = 10 cm, g = 15 cm → b = 30 cm, das Bild ist 30 ÷ 15 = 2-mal so groß. Bei g = 2f ist das Bild gleich groß, bei g < f entsteht kein Bild auf dem Schirm – die Linse wirkt als Lupe."),
            ),
            "Glasfaserkabel bringen das Internet ins Haus: Das Licht wird in der dünnen Faser tausendfach total reflektiert und kommt so um jede Kurve.",
            listOf("Lot" to "Senkrechte auf Spiegel oder Grenzfläche", "Brechung" to "Knick des Lichts an einer Grenzfläche", "Brennweite" to "Abstand vom Brennpunkt zur Linse",
                "Totalreflexion" to "Licht wird vollständig zurückgeworfen", "Kernschatten" to "Bereich, in den gar kein Licht fällt")),

        TheoryCard(Topic.SCHALL, "Schall ist Schwingung: Etwas zittert und bringt die Luft zum Zittern.",
            listOf(
                p("🎵", "Je schneller etwas schwingt, desto höher klingt der Ton."),
                p("📏", "Kurze Saiten, kurze Luftsäulen und kleine Dinge klingen hoch, lange und große tief."),
                p("💨", "Schall braucht Luft, Wasser oder einen festen Stoff. Im Weltall ist es still."),
                p("🥛", "Anschlagen und Pusten verhalten sich verschieden. Frag dich immer: Was schwingt hier – das Glas mit dem Wasser oder die Luft darüber?"),
            ),
            listOf(
                p("🧮", "Frequenz f in Hertz (Hz) = Schwingungen pro Sekunde. Doppelte Frequenz = eine Oktave höher."),
                p("🎸", "Saite: Halbe Länge → doppelte Frequenz. Röhre, die an einer Seite zu ist: f ≈ c ÷ (4 · L), c = 343 m/s."),
                p("🎻", "Saite: Die Frequenz ist umgekehrt proportional zur schwingenden Länge, f₂ = f₁ · L₁ ÷ L₂. Beispiel: Eine Saite mit 200 Hz wird auf drei Viertel verkürzt und klingt dann mit 200 Hz · 4/3 ≈ 267 Hz (eine Quarte höher)."),
                p("🧪", "Röhre umgestellt: L = c ÷ (4 · f) (L = Luftsäule in m, c = 343 m/s, f in Hz), Wasserhöhe = Röhrenlänge − L. Beispiel: Eine 30-cm-Röhre soll 600 Hz geben: L = 343 ÷ 2400 ≈ 0,14 m, also 14 cm Luft und 16 cm Wasser."),
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
                p("🌈", "Farb-Detektiv (Chromatografie): Ein Filzstift-Punkt auf Filterpapier wandert mit Wasser nach oben. Jeder Farbstoff darin hält sich verschieden fest am Papier und kommt verschieden weit – so sieht man, aus welchen Farben eine Farbe gemischt ist."),
                p("🔢", "Schritte zählen: Jedes Trennverfahren holt einen Stoff heraus, der letzte bleibt einfach übrig. Bei n Stoffen reichen n − 1 Schritte. Beispiel: Murmeln, Büroklammern, Erbsen und Mehl sind 4 Stoffe, also höchstens 3 Schritte."),
            ),
            listOf(
                p("🖊️", "Chromatografie: Farbstoffe wandern verschieden weit mit dem Laufmittel, weil sie verschieden stark am Papier haften."),
                p("🧪", "Reinstoff = nur ein Stoff; Gemisch = mehrere Stoffe. Lösung = ein Stoff ist in einem anderen unsichtbar verteilt."),
                p("⚖️", "Dichte ρ = m ÷ V (Einheit g/cm³; 1 mL = 1 cm³), Wasser hat 1,0 g/cm³: Dichteres sinkt nach unten, weniger Dichtes schwimmt oben. Beispiel: 50 mL Sirup wiegen 65 g, also ρ = 65 g ÷ 50 cm³ = 1,3 g/cm³ – im Scheidetrichter liegt der Sirup unten."),
                p("🌡️", "Siedetemperatur: Eindampfen und Destillieren nutzen, dass Stoffe verschieden heiß sieden. Beispiel: Alkohol siedet bei 78 °C, Wasser bei 100 °C. Bei etwa 80 °C verdampft zuerst der Alkohol und wird im gekühlten Rohr wieder flüssig aufgefangen."),
                p("🔗", "Gemisch oder Verbindung: Ein Gemisch lässt sich mit Trennverfahren trennen. Eine Verbindung ist ein neuer Reinstoff – ihre Bausteine bekommt man nur durch eine chemische Reaktion zurück. Beispiel: Wasser aus Wasserstoff und Sauerstoff trennt kein Filter und kein Magnet."),
            ),
            "Im Klärwerk wird Abwasser gesiebt und gefiltert, bevor es zurück in den Fluss darf.",
            listOf("Gemisch" to "mehrere Stoffe zusammen", "Lösung" to "Gelöstes ist unsichtbar verteilt", "Filtrat" to "was durch den Filter läuft")),

        TheoryCard(Topic.SAEURE, "Sauer, neutral oder basisch – das verrät der pH-Wert.",
            listOf(
                p("🍋", "Saure Stoffe: Zitrone, Essig, Cola. Basische Stoffe (Laugen): Seife, Natron, Waschsoda."),
                p("🟣", "Rotkohlsaft ist ein Indikator: sauer wird er rot, neutral violett, basisch blau, grün oder gelb."),
                p("📏", "pH-Wert: unter 7 sauer, genau 7 neutral, über 7 basisch."),
                p("⚠️", "Starke Säuren und Laugen sind ätzend – nur mit Schutzbrille im Chemieraum."),
                p("📏", "Je kleiner die pH-Zahl, desto saurer. Beispiel: Zitronensaft (pH 2) ist saurer als Tomatensaft (pH 4). Seifenwasser (pH 10) ist basischer als Meerwasser (pH 8)."),
            ),
            listOf(
                p("⚖️", "Neutralisation: Säure + Lauge → Salz + Wasser. Gleich viele Säure- und Laugenteilchen heben sich auf."),
                p("🧮", "Neutralisation: neutral, wenn n(Säure) = n(Lauge), also c_S · V_S = c_L · V_L (n in mol, c in mol/L, V in L; 1 mL = 0,001 L). Beispiel: 25 mL Säure mit 0,2 mol/L enthalten 0,2 · 0,025 = 0,005 mol; Lauge mit 0,5 mol/L: V = 0,005 ÷ 0,5 = 0,010 L = 10 mL."),
                p("🔟", "Eine Stufe auf der pH-Skala bedeutet zehnmal mehr oder weniger saure Teilchen (H⁺)."),
                p("💧", "Verdünnen bringt den pH-Wert näher an 7 – aber nie darüber hinaus."),
                p("🔟", "pH = −log c(H⁺) (c in mol/L): c(H⁺) = 0,001 mol/L ergibt pH 3. Jede Stufe ist ×10 – pH 3 ist 100-mal saurer als pH 5; 0,3 Stufen ≈ 2-mal, 0,5 Stufen ≈ 3-mal. 100-fach verdünnt heißt zwei Stufen mehr (pH 2 → pH 4), aber eine Säure wird nie basischer als pH 7."),
                p("➕", "Überschuss: Rest-Teilchen = Säure-Teilchen − Lauge-Teilchen, geteilt durch das Gesamtvolumen. Beispiel: 10 mL HCl (0,1 mol/L) + 8 mL NaOH (0,1 mol/L) → Rest 0,2 mmol H⁺ in 18 mL ≈ 0,011 mol/L, also pH ≈ 2. Ist Lauge übrig: pOH = −log c(OH⁻) und pH = 14 − pOH; c(OH⁻) = 0,001 mol/L ergibt pH 11."),
            ),
            "Gegen Sodbrennen hilft Natron: Es neutralisiert zu viel Magensäure.",
            listOf("Indikator" to "Stoff, der durch Farbe sauer oder basisch anzeigt", "neutralisieren" to "Säure und Lauge heben sich auf", "Konzentration" to "wie viel gelöst ist")),

        TheoryCard(Topic.REAKTION, "Bei einer chemischen Reaktion entstehen neue Stoffe – aber kein Atom geht verloren.",
            listOf(
                p("⚛️", "Alle Stoffe bestehen aus Atomen. Bei einer Reaktion ordnen sich die Atome neu."),
                p("💥", "Man erkennt eine Reaktion an neuen Eigenschaften: Gas, Farbe, Licht, Wärme."),
                p("🔥", "Verbrennen ist eine Reaktion mit Sauerstoff aus der Luft."),
                p("🎈", "Eine Reaktion hört auf, wenn ein Ausgangsstoff aufgebraucht ist – der knappste Stoff bestimmt, wie viel entsteht. Beispiel: 1 Tütchen Backpulver in viel Essig gibt ein Glas voll Schaum, 2 Tütchen etwa doppelt so viel. Mehr Essig allein bringt dann nichts."),
                p("⚖️", "Kein Atom geht verloren: Holt sich ein Stoff etwas aus der Luft, wird er schwerer – brennende Stahlwolle auf der Waage nimmt Sauerstoff auf. Entweicht ein Gas, wird es scheinbar leichter: Die Kerze schrumpft, weil Kohlenstoffdioxid und Wasserdampf davonziehen."),
            ),
            listOf(
                p("⚖️", "Reaktionsgleichung: Links und rechts stehen gleich viele Atome jeder Sorte (Massenerhaltung)."),
                p("🔢", "Ausgleichen nur mit den Zahlen vor den Formeln (Koeffizienten), nie mit den kleinen Zahlen in den Formeln."),
                p("🔄", "Oxidation: Ein Stoff verbindet sich mit Sauerstoff. Redoxreaktion: Ein Stoff gibt Sauerstoff ab, ein anderer nimmt ihn auf."),
                p("🧮", "Ausgleichen in 3 Schritten: 1. C zählen, 2. H zählen, 3. zum Schluss O mit O₂. Beispiel Ethen: C₂H₄ + ? O₂ → 2 CO₂ + 2 H₂O; rechts 4 + 2 = 6 O, also 3 O₂. Kommt ein halbes O₂ heraus, verdoppelt man alles: Ethan braucht 3½ O₂, also 2 C₂H₆ + 7 O₂ → 4 CO₂ + 6 H₂O."),
                p("⚖️", "Massenerhaltung m(Ausgangsstoffe) = m(Produkte) in g: 5,6 g Eisen + 3,2 g Schwefel → 8,8 g Eisensulfid. Nimmt ein Metall Sauerstoff auf, ist das Produkt genau um diesen Sauerstoff schwerer: 2,7 g Aluminium + 2,4 g Sauerstoff → 5,1 g Aluminiumoxid."),
                p("🔢", "Koeffizienten sind Teilchenverhältnisse – bei Gasen auch Volumenverhältnisse. Beispiel: N₂ + 3 H₂ → 2 NH₃: 1 L Stickstoff braucht 3 L Wasserstoff und ergibt 2 L Ammoniak. Halbiert man den knappsten Stoff, halbiert sich das Produkt."),
            ),
            "Rost entsteht, wenn Eisen langsam mit Sauerstoff und Wasser reagiert.",
            listOf("Atom" to "kleinster Baustein eines Elements", "Molekül" to "verbundene Atome", "Koeffizient" to "Zahl vor einer Formel")),

        TheoryCard(Topic.VERERBUNG, "Eltern geben ihren Kindern Erbanlagen weiter – von jedem Elternteil eine.",
            listOf(
                p("🧬", "Für jedes Merkmal hat ein Lebewesen zwei Erbanlagen (Allele): eine von der Mutter, eine vom Vater."),
                p("💪", "Dominant: Diese Anlage setzt sich durch und ist zu sehen. Rezessiv: Sie zeigt sich nur, wenn beide Anlagen rezessiv sind (aa)."),
                p("🎲", "Welche Anlage ein Kind bekommt, ist Zufall – wie beim Münzwurf."),
                p("🪙", "Verhältnisse wie 3 : 1 sind Wahrscheinlichkeiten – sie stimmen erst bei vielen Kindern ungefähr. Wie beim Münzwurf: Bei 10 Würfen kommt selten genau 5-mal Kopf, bei 1000 Würfen fast genau die Hälfte."),
            ),
            listOf(
                p("🔲", "Kreuzungsquadrat: Die Anlagen der Eltern an den Rändern, im Feld alle möglichen Kombinationen – jede gleich wahrscheinlich."),
                p("📜", "Mendels Regeln: Uniformitätsregel (AA × aa → alle Aa), Spaltungsregel (Aa × Aa → 3 : 1 bei dominant-rezessiv)."),
                p("🩸", "Intermediär: Mischerbige sehen anders aus (rosa). Kodominant: Beide zeigen sich (Blutgruppe AB)."),
                p("🔤", "Genotyp = die Erbanlagen (Aa). Phänotyp = das Aussehen (gelb)."),
                p("🔲", "Kreuzungsquadrat Schritt für Schritt: Schwarz (B) ist dominant über Braun (b), Eltern Bb × BB. Oben B | b, links B | B → Felder BB, Bb, BB, Bb. Jedes Feld ist 1/4 = 25 %: 50 % BB, 50 % Bb, alle Kinder schwarz. Anteil = passende Felder ÷ 4 × 100 %."),
                p("🌸", "Intermediär: Mischerbige sehen eigen aus, darum zählt jeder Genotyp einzeln – Löwenmäulchen Rot RR × Rosa Rr ergibt RR, RR, Rr, Rr, also 50 % rot und 50 % rosa. Testkreuzung: mit einem reinerbig rezessiven Partner (aa) kreuzen; an den Anteilen der Kinder erkennt man, ob AA oder Aa."),
                p("🧬", "Blutgruppen: drei Allele A, B und 0. A und B sind dominant über 0 und kodominant zueinander: AA oder A0 → A, BB oder B0 → B, AB → AB, 00 → 0. Beispiel: Mama AB × Papa 00 ergibt A0, A0, B0, B0 – 50 % Blutgruppe A, 50 % B; 0 oder AB ist hier unmöglich."),
            ),
            "Zwei schwarze Labradore können gelbe Welpen bekommen – wenn beide die Anlage für Gelb versteckt tragen.",
            listOf("Allel" to "eine Form einer Erbanlage", "dominant" to "setzt sich durch", "rezessiv" to "zeigt sich nur doppelt")),

        TheoryCard(Topic.ZELLEN, "Alle Lebewesen bestehen aus Zellen – viele so klein, dass man ein Mikroskop braucht.",
            listOf(
                p("🔬", "Ein Mikroskop vergrößert Winziges. Je stärker es vergrößert, desto kleiner ist der Ausschnitt, den man sieht."),
                p("🦠", "Einzeller wie das Pantoffeltierchen bestehen aus nur einer Zelle und erledigen damit alles."),
                p("🥔", "Pflanzenzellen haben eine feste Zellwand und einen großen Saftraum (Vakuole)."),
                p("💧", "Osmose: Wasser wandert durch die Zellhaut dorthin, wo mehr Salz oder Zucker gelöst ist."),
                p("🔍", "„10-fach vergrößert“ heißt: Alles wirkt 10-mal so groß. Eine Ameise (5 mm) wirkt dann so groß wie 5 cm. Merksatz: Wirkt etwas kleiner als 1 mm, sieht man nur einen Punkt – ab etwa 1 cm erkennt man Einzelheiten."),
            ),
            listOf(
                p("🧮", "Gesamtvergrößerung = Okular × Objektiv, z. B. 10 × 40 = 400-fach."),
                p("🧫", "Die Zellmembran ist halbdurchlässig: Wasser kommt durch, gelöste Stoffe kaum."),
                p("🌀", "Plasmolyse: In salziger Umgebung verliert die Pflanzenzelle Wasser, der Saftraum schrumpft, die Zellwand bleibt."),
                p("📐", "Scheinbare Größe = echte Größe × Gesamtvergrößerung (in mm): Ein Pollenkorn (0,05 mm) wirkt bei 200-fach (Okular 10 × Objektiv 20) 0,05 mm × 200 = 10 mm groß – gut zu erkennen."),
                p("⭕", "Bildausschnitt ≈ 200 mm ÷ Gesamtvergrößerung (bei Okular 10×): Bei 200-fach sieht man einen Kreis von etwa 1 mm, da passen 1 mm ÷ 0,05 mm = 20 Pollenkörner nebeneinander. Doppelte Vergrößerung heißt halber Ausschnitt."),
                p("💧", "Osmose rechnen: Wasser fließt zur höheren Konzentration; ist es innen und außen gleich (isotonisch), bleibt die Masse gleich. Massenänderung in % = (nachher − vorher) ÷ vorher × 100. Beispiel Rosine in Wasser: 1,0 g → 1,5 g ergibt +50 % – innen war es konzentrierter."),
            ),
            "Schlapper Salat wird in kaltem Wasser wieder knackig – seine Zellen nehmen Wasser auf.",
            listOf("Zelle" to "kleinster Baustein des Lebens", "Vakuole" to "Saftraum der Pflanzenzelle", "Osmose" to "Wasser wandert durch eine Zellhaut")),

        TheoryCard(Topic.ERNAEHRUNG, "Unsere Nahrung enthält Nährstoffe – und Enzyme zerlegen sie bei der Verdauung.",
            listOf(
                p("🍞", "Kohlenhydrate (Stärke, Zucker) liefern Energie. Fette liefern viel Energie. Eiweiße braucht der Körper zum Aufbauen."),
                p("🔍", "Mit Nachweisen findet man Nährstoffe: Iod färbt Stärke blau-schwarz, Fett macht einen bleibenden Fleck auf Papier."),
                p("👅", "Die Verdauung beginnt im Mund: Speichel zerlegt Stärke in Zucker."),
                p("🍬", "Zucker ist nicht gleich Zucker: Traubenzucker, Fruchtzucker, Milchzucker, Malzzucker, Haushaltszucker. Jeder Nachweis erkennt nur einen bestimmten Stoff."),
            ),
            listOf(
                p("🧪", "Enzyme sind Eiweiße, die Reaktionen beschleunigen. Jedes Enzym hat seine Lieblingstemperatur (im Körper etwa 37 °C)."),
                p("♨️", "Zu viel Hitze verändert die Form eines Enzyms dauerhaft (Denaturierung) – dann arbeitet es nicht mehr."),
                p("🧾", "Nachweise: Glukose-Teststreifen für Traubenzucker, Biuret-Probe (violett) für Eiweiß."),
                p("🔬", "Ein Kontrollversuch zeigt, ob wirklich die vermutete Ursache wirkt."),
                p("🌡️", "RGT-Faustregel: 10 °C wärmer ≈ doppelt so schnell, 10 °C kälter ≈ halb so schnell – aber nur bis zur Lieblingstemperatur (Optimum), darüber fällt es steil ab (Denaturierung). Beispiel: 10 Minuten bei 30 °C → etwa 20 Minuten bei 20 °C, etwa 40 Minuten bei 10 °C."),
                p("🔥", "Energiegehalt: 1 g Fett ≈ 37 kJ (9 kcal), 1 g Kohlenhydrate oder Eiweiß ≈ 17 kJ (4 kcal). Beispiel: 10 g Butter (≈ 8 g Fett) liefern 8 × 37 kJ ≈ 300 kJ, 10 g Zucker nur 10 × 17 kJ = 170 kJ."),
            ),
            "Unreife Bananen schmecken mehlig, reife süß – beim Reifen wird Stärke zu Zucker umgebaut.",
            listOf("Nährstoff" to "Stoff in der Nahrung, den der Körper braucht", "Enzym" to "Eiweiß, das Stoffe zerlegt oder umbaut", "Nachweis" to "Test, ob ein Stoff da ist")),

        TheoryCard(Topic.OEKOLOGIE, "Pflanzen machen aus Licht Nahrung – und alle Tiere hängen davon ab.",
            listOf(
                p("🌿", "Fotosynthese: Pflanzen bauen mit Licht aus Wasser und Kohlenstoffdioxid Zucker auf. Dabei entsteht Sauerstoff."),
                p("🌱", "Samen keimen, wenn sie Wasser, Wärme und Luft bekommen. Licht brauchen sie erst danach."),
                p("🦊", "Nahrungskette: Pflanze → Pflanzenfresser → Fleischfresser. Der Pfeil heißt „wird gefressen von“."),
                p("🐇", "Räuber und Beute hängen voneinander ab: Viel Beute → mehr Räuber → weniger Beute → weniger Räuber."),
                p("🔺", "Auf jeder Stufe der Nahrungskette bleibt nur wenig Futter-Energie übrig – darum gibt es oben nur wenige Tiere, und Ketten haben selten mehr als 4–5 Glieder. Beispiel: viele Blattläuse, weniger Marienkäfer, ganz wenige Meisen."),
                p("🕸️", "Nahrungsnetz: Wer viele verschiedene Beutetiere frisst, kann ausweichen, wenn eines fehlt. Spezialisten trifft es hart – der Pandabär frisst fast nur Bambus."),
            ),
            listOf(
                p("⚗️", "Fotosynthese: Kohlenstoffdioxid + Wasser → (Lichtenergie) → Traubenzucker + Sauerstoff."),
                p("🔺", "Energiepyramide: Von Stufe zu Stufe kommt nur etwa ein Zehntel der Energie an (10-%-Regel)."),
                p("☠️", "Gifte, die nicht abgebaut werden, reichern sich am Ende der Nahrungskette an."),
                p("🕸️", "Viele Nahrungsketten zusammen bilden ein Nahrungsnetz."),
                p("🔟", "10-%-Regel: Energie der nächsten Stufe ≈ Energie der Stufe davor × 0,1 (in kJ). Beispiel: 50 000 kJ in Pflanzen → Raupen 5000 kJ → Meisen 500 kJ → Sperber 50 kJ. Der Rest geht für Bewegung und Wärme verloren oder wird nicht gefressen."),
                p("💡", "Licht und Abstand: I ~ 1/d² (I = Lichtstärke, d = Abstand). Neue Lichtstärke = alte × (alter Abstand ÷ neuer Abstand)². Beispiel: Lampe von 40 cm auf 20 cm heran → (40 ÷ 20)² = 4, also viermal so viel Licht; von 10 cm auf 50 cm → (10 ÷ 50)² = 1/25."),
                p("🌱", "Begrenzender Faktor: Die Fotosynthese braucht Licht, CO₂, Wasser und passende Wärme – der knappste Faktor bestimmt das Tempo. Fehlt CO₂, bringt eine hellere Lampe kaum mehr Bläschen."),
            ),
            "Ohne Pflanzen gäbe es keinen Sauerstoff zum Atmen und keine Nahrung für Tiere.",
            listOf("Produzent" to "Pflanze, die ihre Nahrung selbst herstellt", "Konsument" to "Tier, das andere frisst", "Keimung" to "Samen beginnt zu wachsen")),

        TheoryCard(Topic.KOERPER, "Das Herz ist eine Pumpe, die Tag und Nacht Blut durch den Körper schickt.",
            listOf(
                p("❤️", "Das Herz ist ein Muskel, etwa so groß wie deine Faust."),
                p("✋", "Bei jedem Schlag spürt man eine Welle in den Adern: den Puls, am Handgelenk oder am Hals."),
                p("🏃", "Beim Rennen brauchen die Muskeln mehr Sauerstoff – das Herz schlägt schneller."),
                p("⏱️", "Puls messen: 15 Sekunden lang zählen und mal 4 nehmen (eine Minute = 4 × 15 s). Beispiel: 18 Schläge in 15 s ergeben 18 × 4 = 72 Schläge pro Minute."),
                p("🧮", "Hochrechnen: 1 Stunde = 60 Minuten, 1 Tag = 24 × 60 = 1440 Minuten. Beispiel: Bei 60 Schlägen pro Minute sind das 3600 pro Stunde und 86 400 pro Tag."),
            ),
            listOf(
                p("🧮", "Herzzeitvolumen = Puls × Schlagvolumen (Schläge pro Minute × ml pro Schlag = ml pro Minute; 1000 ml = 1 L). Beispiel beim Sport: 150 × 100 ml = 15 000 ml = 15 Liter pro Minute."),
                p("📉", "Ruhepuls: Kinder etwa 70–110, Erwachsene etwa 60–80 Schläge pro Minute. Training senkt ihn."),
                p("⏱️", "Erholungspuls: Je schneller der Puls nach Anstrengung sinkt, desto fitter ist das Herz-Kreislauf-System."),
                p("🩸", "Blutmenge ≈ 7–8 % des Körpergewichts (1 kg Blut ≈ 1 Liter). Beispiel: Ein Kind mit 30 kg hat 30 kg × 0,075 ≈ 2,3 kg, also etwa 2,3 Liter Blut."),
            ),
            "Ein Herz schlägt in einem langen Leben rund drei Milliarden Mal.",
            listOf("Puls" to "Druckwelle bei jedem Herzschlag", "Kreislauf" to "Weg des Blutes durch den Körper", "Ruhepuls" to "Puls, wenn man still sitzt")),
    ).associateBy { it.topic }
}
