package de.kurvenflug.core.model

import kotlin.math.abs

/** Ein Level von Kurvenflug. */
data class Level(
    val id: String,
    val chapter: Int,
    val number: Int,
    val title: String,
    val kind: Kind,
    /** Auftrag, den Volt vorliest */
    val task: String,
    val targets: List<Body>,
    /** einstellbare Werte – alle anderen stehen in [fixed] */
    val params: List<Param>,
    /** Lösung (für Tests: jedes Level muss lösbar sein) */
    val solution: Map<String, Double>,
    val fixed: Map<String, Double> = emptyMap(),
    val asteroids: List<Body> = emptyList(),
    val cells: List<Body> = emptyList(),
    /** Ziel erst nach dem Treffer zeigen (nur der Funkspruch verrät die Koordinaten) */
    val hidden: Boolean = false,
    val planet: Planet = Planet.EARTH,
    val launch: Vec = Vec(-9.0, -9.0),
    val ground: Double = -9.5,
    /** Masse je Asteroid/Planet (gleiche Reihenfolge wie [asteroids]); > 0 heißt: zieht die Sonde an */
    val masses: List<Double> = emptyList(),
    /** Tipp nach dem zweiten Fehlversuch */
    val tip: String,
    /** eigene Schreibweise der Gleichungen (z. B. „2x + y = 7“) statt der automatisch erzeugten */
    val equations: List<String>? = null,
) {
    /** Startwerte + feste Werte + aktuelle Einstellung */
    fun values(v: Map<String, Double>): Map<String, Double> = params.associate { it.key to it.start } + fixed + v
    val start: Map<String, Double> get() = params.associate { it.key to it.start }
}

/** Ein Kapitel = ein Schulthema. */
data class Chapter(
    val number: Int,
    val title: String,
    val grades: String,
    val emoji: String,
    /** Volts kurze Erklärung vor dem ersten Level – Schritt für Schritt */
    val intro: List<String>,
    val levels: List<Level>,
)

/** Volts Rückmeldung nach einem Fehlversuch: konkret, mit Zahlen, ohne Schimpfen. */
object Feedback {
    fun after(level: Level, values: Map<String, Double>, r: FlightResult): String {
        if (r.success) return ""
        if (r.crash != null) return if (level.masses.getOrElse(r.crash) { 0.0 } > 0)
            "Eingefangen! 🪐 Der Planet hat dich angezogen. Schneller sein oder weiter an ihm vorbeizielen."
        else "Bumm – Asteroid! ☄️ Die Bahn muss an ihm vorbei."
        val missing = level.targets.indices.firstOrNull { it !in r.hitTargets } ?: 0
        val t = level.targets[missing]
        val v = level.values(values)
        return when (level.kind) {
            Kind.POINT -> {
                val dx = t.x - v.getValue("x"); val dy = t.y - v.getValue("y")
                when {
                    dx != 0.0 && dy != 0.0 -> "Gelandet bei (${num(v.getValue("x"))} | ${num(v.getValue("y"))}). Beide Werte passen noch nicht."
                    dx != 0.0 -> "Die Höhe stimmt! Aber nach rechts/links (x) passt es noch nicht."
                    else -> "x stimmt! Jetzt noch die Höhe (y) – oben ist plus, unten minus."
                }
            }
            Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP -> {
                val y = Flight.f(level, values, t.x)
                if (abs(y) > Flight.R) "Bei x = ${num(t.x)} ist deine Kurve ganz aus dem Bild (y = ${num(y)}). Ziel: y = ${num(t.y)}."
                else "Bei x = ${num(t.x)} war die Sonde bei y = ${num(y)} – das Ziel liegt bei y = ${num(t.y)}. " +
                    (if (y < t.y) "Etwas höher! ⬆️" else "Etwas tiefer! ⬇️")
            }
            Kind.RENDEZVOUS -> {
                val x = v.getValue("x"); val y = v.getValue("y")
                val ya = v.getValue("m1") * x + v.getValue("b1"); val yb = v.getValue("m2") * x + v.getValue("b2")
                if (kotlin.math.abs(ya - yb) > 0.3) "Bei x = ${num(x)} war Sonde A auf y = ${num(ya)}, Sonde B auf y = ${num(yb)} – dort treffen sie sich nicht. Setz beide Gleichungen gleich!"
                else "Die Sonden treffen sich bei x = ${num(x)} auf y = ${num(ya)} – aber dein Andockpunkt hat y = ${num(y)}."
            }
            Kind.ORBIT -> {
                val near = r.path.minByOrNull { kotlin.math.hypot(it.x - t.x, it.y - t.y) } ?: return "Nochmal!"
                val d = kotlin.math.hypot(near.x - t.x, near.y - t.y) - t.r
                val side = if (near.y > t.y) "über" else "unter"
                "Knapp ${num(d, 1)} Kästchen $side der Station vorbei. Winkel oder Tempo ein bisschen ändern – die Planeten lenken mit!"
            }
            Kind.THROW -> {
                val land = r.path.lastOrNull() ?: return "Nochmal!"
                val dx = land.x - t.x
                when {
                    r.path.any { it.x >= t.x } && land.x > t.x && r.path.first { it.x >= t.x }.y > t.y + t.r -> "Zu hoch übers Ziel geflogen – um ${num(abs(dx), 1)} m zu weit."
                    dx < 0 -> "${num(abs(dx), 1)} m zu kurz. Mehr Schwung oder ein anderer Winkel!"
                    else -> "${num(abs(dx), 1)} m zu weit."
                }
            }
        }
    }
}

/** Alle Kapitel. Koordinaten in Kästchen (Wurf: Meter). */
object Levels {
    private fun p(key: String, label: String, min: Double, max: Double, step: Double, start: Double, unit: String = "") =
        Param(key, label, min, max, step, start, unit)
    private fun t(x: Double, y: Double, r: Double = 0.7) = Body(x, y, r)
    private fun rock(x: Double, y: Double, r: Double = 1.0) = Body(x, y, r)
    private fun cell(x: Double, y: Double) = Body(x, y, 0.45)

    private val px = p("x", "x", -9.0, 9.0, 1.0, 0.0)
    private val py = p("y", "y", -9.0, 9.0, 1.0, 0.0)
    private val pm = p("m", "m", -5.0, 5.0, 0.5, 0.0)
    private val pb = p("b", "b", -9.0, 9.0, 1.0, 0.0)
    private val pa = p("a", "a", -3.0, 3.0, 0.25, 1.0)
    private val pd = p("d", "d", -8.0, 8.0, 1.0, 0.0)
    private val pe = p("e", "e", -9.0, 9.0, 1.0, 0.0)
    private val pAngle = p("angle", "Winkel", 5.0, 85.0, 1.0, 45.0, "°")
    private fun pV(start: Double = 8.0) = p("v", "Tempo", 1.0, 20.0, 0.5, start, "m/s")
    private val sa = p("a", "a", -5.0, 5.0, 0.5, 1.0)
    private val sb = p("b", "b", 0.25, 3.0, 0.25, 1.0)
    private val sd = p("d", "d", -8.0, 8.0, 1.0, 0.0)
    private val ea = p("a", "a", 0.25, 4.0, 0.25, 1.0)
    private val eb = p("b", "b", 0.25, 4.0, 0.25, 1.0)
    private val oAngle = p("angle", "Winkel", -80.0, 80.0, 1.0, 0.0, "°")
    private val oV = p("v", "Tempo", 1.0, 10.0, 0.5, 4.0)
    private const val PI = Math.PI
    private fun planet(x: Double, y: Double, r: Double) = Body(x, y, r)
    private val rx = p("x", "x", -9.0, 9.0, 0.5, 0.0)
    private val ry = p("y", "y", -9.0, 9.0, 0.5, 0.0)

    /** Rendezvous-Level: Lösung = Schnittpunkt, Station dort versteckt. */
    private fun meet(n: Int, title: String, m1: Double, b1: Double, m2: Double, b2: Double, task: String, tip: String,
                     equations: List<String>? = null, rocks: List<Body> = emptyList()): Level {
        val s = Flight.intersection(m1, b1, m2, b2)!!
        return Level("7-$n", 7, n, title, Kind.RENDEZVOUS, task, listOf(Body(s.x, s.y, 0.35)), listOf(rx, ry),
            mapOf("x" to s.x, "y" to s.y), fixed = mapOf("m1" to m1, "b1" to b1, "m2" to m2, "b2" to b2),
            asteroids = rocks, hidden = true, tip = tip, equations = equations)
    }

    val chapters: List<Chapter> = listOf(
        Chapter(
            1, "Koordinaten", "Klasse 5–6", "📍",
            listOf(
                "Hi, ich bin Volt! 🤖 Ich steuere deine Sonde – aber du gibst die Befehle.",
                "Jeder Ort im All hat zwei Zahlen: (x | y). Die Sonde startet in der Mitte bei (0 | 0).",
                "x sagt: so weit nach rechts (plus) oder links (minus). y sagt: so weit nach oben (plus) oder unten (minus).",
                "Erst x, dann y – wie beim Lesen: erst waagerecht, dann senkrecht. Los geht's!",
            ),
            listOf(
                Level("1-1", 1, 1, "Erster Kontakt", Kind.POINT,
                    "Die Raumstation liegt bei (4 | 3). Stell x und y ein und starte!",
                    listOf(t(4.0, 3.0)), listOf(px, py), mapOf("x" to 4.0, "y" to 3.0),
                    tip = "x = 4 heißt: 4 Kästchen nach rechts. y = 3: dann 3 nach oben."),
                Level("1-2", 1, 2, "Links herum", Kind.POINT,
                    "Notruf von (−5 | 2)! Minus bei x bedeutet: nach links.",
                    listOf(t(-5.0, 2.0)), listOf(px, py), mapOf("x" to -5.0, "y" to 2.0),
                    cells = listOf(cell(-3.0, 0.0)),
                    tip = "x = −5: fünf Kästchen nach links. Dann 2 nach oben."),
                Level("1-3", 1, 3, "Abtauchen", Kind.POINT,
                    "Die Forschungsbasis liegt tief unten bei (3 | −6).",
                    listOf(t(3.0, -6.0)), listOf(px, py), mapOf("x" to 3.0, "y" to -6.0),
                    cells = listOf(cell(3.0, -3.0)),
                    tip = "Minus bei y heißt: nach unten."),
                Level("1-4", 1, 4, "Ganz im Eck", Kind.POINT,
                    "Lies die Koordinaten der Station selbst ab!",
                    listOf(t(-7.0, -4.0)), listOf(px, py), mapOf("x" to -7.0, "y" to -4.0),
                    tip = "Zähle von der Mitte aus: erst die Kästchen nach links, dann nach unten. Beides ist minus."),
                Level("1-5", 1, 5, "Funkspruch", Kind.POINT,
                    "Die Station ist getarnt! Funkspruch: „x = −8 : 4,  y = 3 + 4“",
                    listOf(t(-2.0, 7.0)), listOf(px, py), mapOf("x" to -2.0, "y" to 7.0), hidden = true,
                    tip = "−8 : 4 = −2. Und 3 + 4 = 7."),
                Level("1-6", 1, 6, "Spiegelwelt", Kind.POINT,
                    "Getarnt: Die Station ist das Spiegelbild von (5 | −3) – gespiegelt an der x-Achse.",
                    listOf(t(5.0, 3.0)), listOf(px, py), mapOf("x" to 5.0, "y" to 3.0), hidden = true,
                    tip = "Beim Spiegeln an der x-Achse bleibt x gleich, y wechselt das Vorzeichen."),
                Level("1-7", 1, 7, "Halbe Strecke", Kind.POINT,
                    "Getarnt: Die Station liegt genau in der Mitte zwischen (−6 | 8) und (2 | −4).",
                    listOf(t(-2.0, 2.0)), listOf(px, py), mapOf("x" to -2.0, "y" to 2.0), hidden = true,
                    tip = "Mitte = Durchschnitt: x = (−6 + 2) : 2, y = (8 + (−4)) : 2."),
            ),
        ),
        Chapter(
            2, "Geraden", "Klasse 7–8", "📈",
            listOf(
                "Jetzt fliegt die Sonde auf einer Geraden quer durchs Bild: y = m · x + b.",
                "b ist der Startpunkt auf der y-Achse – dort, wo x = 0 ist.",
                "m ist die Steigung: Pro Kästchen nach rechts geht es m Kästchen nach oben. Minus heißt bergab.",
                "Tipp: Rechne mit den Koordinaten der Ziele – dann triffst du beim ersten Versuch. ⭐⭐⭐",
            ),
            listOf(
                Level("2-1", 2, 1, "Geradeaus", Kind.LINE,
                    "Nur b ist frei (m = 0): Die Sonde fliegt waagerecht. Ziel bei (6 | 4).",
                    listOf(t(6.0, 4.0)), listOf(pb), mapOf("b" to 4.0), fixed = mapOf("m" to 0.0),
                    asteroids = listOf(rock(-4.0, 0.0)),
                    tip = "Bei m = 0 ist y überall gleich b. Welche Höhe hat das Ziel?"),
                Level("2-2", 2, 2, "Bergauf", Kind.LINE,
                    "Jetzt ist nur die Steigung m frei (b = 0). Ziel bei (4 | 4).",
                    listOf(t(4.0, 4.0)), listOf(pm), mapOf("m" to 1.0), fixed = mapOf("b" to 0.0),
                    asteroids = listOf(rock(-4.0, 0.0)), cells = listOf(cell(-6.0, -6.0)),
                    tip = "4 nach rechts und 4 nach oben – wie viel nach oben pro Kästchen?"),
                Level("2-3", 2, 3, "Steil bergab", Kind.LINE,
                    "b = 0. Ziel bei (3 | −6).",
                    listOf(t(3.0, -6.0)), listOf(pm), mapOf("m" to -2.0), fixed = mapOf("b" to 0.0),
                    cells = listOf(cell(-2.0, 4.0)),
                    tip = "3 nach rechts, 6 nach unten: m = −6 : 3."),
                Level("2-4", 2, 4, "Halbe Kraft", Kind.LINE,
                    "b = 0. Ziel bei (8 | 4) – und vorsichtig, Asteroid!",
                    listOf(t(8.0, 4.0)), listOf(pm), mapOf("m" to 0.5), fixed = mapOf("b" to 0.0),
                    asteroids = listOf(rock(4.0, 4.0)), cells = listOf(cell(-4.0, -2.0)),
                    tip = "8 nach rechts, nur 4 nach oben. m kann auch eine Kommazahl sein."),
                Level("2-5", 2, 5, "Zwei Regler", Kind.LINE,
                    "Jetzt m und b! Die Sonde muss durch beide Stationen.",
                    listOf(t(0.0, 1.0), t(2.0, 5.0)), listOf(pm, pb), mapOf("m" to 2.0, "b" to 1.0),
                    tip = "Die Station bei x = 0 verrät dir b. Von dort: 2 nach rechts, 4 nach oben."),
                Level("2-6", 2, 6, "Doppeltreffer", Kind.LINE,
                    "Durch beide Stationen: (−4 | −1) und (4 | 3).",
                    listOf(t(-4.0, -1.0), t(4.0, 3.0)), listOf(pm, pb), mapOf("m" to 0.5, "b" to 1.0),
                    cells = listOf(cell(0.0, 1.0)),
                    tip = "Steigung = Höhenunterschied : Breitenunterschied = (3 − (−1)) : (4 − (−4))."),
                Level("2-7", 2, 7, "Asteroidengürtel", Kind.LINE,
                    "Durch (−6 | 8) und (2 | −4) – mitten durch den Gürtel.",
                    listOf(t(-6.0, 8.0), t(2.0, -4.0)), listOf(pm, pb), mapOf("m" to -1.5, "b" to -1.0),
                    asteroids = listOf(rock(0.0, 3.0), rock(-2.0, 6.5), rock(4.0, 1.0), rock(-6.0, 2.0)),
                    cells = listOf(cell(-4.0, 5.0)),
                    tip = "m = (−4 − 8) : (2 − (−6)). Dann b: Setze einen Punkt in y = m·x + b ein."),
                Level("2-8", 2, 8, "Nadelöhr", Kind.LINE,
                    "Zwischen den Asteroiden hindurch zur Station (6 | −5). Die Lücke liegt bei (−2 | 3).",
                    listOf(t(6.0, -5.0)), listOf(pm, pb), mapOf("m" to -1.0, "b" to 1.0),
                    asteroids = listOf(rock(-2.0, 5.5, 1.2), rock(-2.0, 0.5, 1.2), rock(3.0, 3.0)),
                    cells = listOf(cell(-2.0, 3.0)),
                    tip = "Die Gerade muss durch (−2 | 3) und (6 | −5). Berechne m und b."),
            ),
        ),
        Chapter(
            7, "Rendezvous", "Klasse 8", "🤝",
            listOf(
                "Zwei Sonden, zwei Geraden! Sonde A kommt von links, Sonde B von rechts.",
                "Sie sollen sich an einer Raumstation treffen. Die Station ist getarnt – du musst den Treffpunkt ausrechnen.",
                "Am Treffpunkt sind beide y-Werte gleich. Also: beide Gleichungen gleichsetzen und nach x auflösen. Dann y ausrechnen.",
                "Das nennt man ein lineares Gleichungssystem. Stell den Andockpunkt (x | y) ein – und los!",
            ),
            listOf(
                meet(1, "Erstes Treffen", 1.0, 1.0, -1.0, 5.0, "A: y = x + 1 und B: y = −x + 5. Wo treffen sie sich?",
                    "x + 1 = −x + 5 → 2x = 4 → x = 2. Dann y = 2 + 1."),
                meet(2, "Gegenverkehr", 2.0, -4.0, -1.0, 5.0, "A: y = 2x − 4 und B: y = −x + 5.",
                    "2x − 4 = −x + 5 → 3x = 9."),
                meet(3, "Sanfte Steigung", 0.5, 2.0, -0.5, 4.0, "A: y = 0,5x + 2 und B: y = −0,5x + 4.",
                    "0,5x + 2 = −0,5x + 4 → x = 2.", rocks = listOf(Body(-4.0, 6.0, 1.0))),
                meet(4, "Halbe Sachen", 3.0, 1.0, 1.0, 4.0, "A: y = 3x + 1 und B: y = x + 4. Achtung, Kommazahlen!",
                    "3x + 1 = x + 4 → 2x = 3 → x = 1,5."),
                meet(5, "Tief unten", -2.0, 1.0, 1.0, -5.0, "A: y = −2x + 1 und B: y = x − 5.",
                    "−2x + 1 = x − 5 → −3x = −6."),
                meet(6, "Andere Schreibweise", -2.0, 7.0, 1.0, 1.0, "Funkspruch: A: 2x + y = 7 und B: x − y = −1.",
                    "Erst nach y umstellen: y = −2x + 7 und y = x + 1. Dann gleichsetzen.",
                    equations = listOf("A:  2x + y = 7", "B:  x − y = −1")),
                meet(7, "Kreuzung im Gürtel", 1.5, -2.0, -0.5, 4.0, "A: y = 1,5x − 2 und B: y = −0,5x + 4.",
                    "1,5x − 2 = −0,5x + 4 → 2x = 6. y mit Kommastelle!",
                    rocks = listOf(Body(-3.0, 0.5, 1.0), Body(6.0, 3.0, 1.0))),
            ),
        ),
        Chapter(
            3, "Parabeln", "Klasse 9", "🌙",
            listOf(
                "Bögen! Die Sonde fliegt auf einer Parabel: y = a · (x − d)² + e.",
                "Der Scheitel – der tiefste oder höchste Punkt – liegt bei (d | e). d schiebt nach rechts, e nach oben.",
                "a bestimmt die Form: positiv = Tal ∪, negativ = Hügel ∩. Je größer a, desto schmaler.",
                "Trick: Setz die Koordinaten eines Ziels ein und rechne den fehlenden Wert aus.",
            ),
            listOf(
                Level("3-1", 3, 1, "Talfahrt", Kind.PARABOLA,
                    "y = x² + e. Verschiebe die Parabel nach oben oder unten, bis sie (2 | 1) trifft.",
                    listOf(t(2.0, 1.0)), listOf(pe), mapOf("e" to -3.0), fixed = mapOf("a" to 1.0, "d" to 0.0),
                    cells = listOf(cell(0.0, -3.0)),
                    tip = "Bei x = 2 ist x² = 4. Also 4 + e = 1."),
                Level("3-2", 3, 2, "Zur Seite", Kind.PARABOLA,
                    "y = (x − d)² − 1. Schiebe die Parabel seitlich, bis sie (5 | 3) trifft.",
                    listOf(t(5.0, 3.0)), listOf(pd), mapOf("d" to 3.0), fixed = mapOf("a" to 1.0, "e" to -1.0),
                    tip = "(5 − d)² − 1 = 3, also (5 − d)² = 4. Welche Zahl hoch 2 ergibt 4?"),
                Level("3-3", 3, 3, "Umgedreht", Kind.PARABOLA,
                    "y = a · x² + 6. Der Bogen muss (4 | 2) treffen.",
                    listOf(t(4.0, 2.0)), listOf(pa), mapOf("a" to -0.25), fixed = mapOf("d" to 0.0, "e" to 6.0),
                    cells = listOf(cell(0.0, 6.0)),
                    tip = "a · 16 + 6 = 2 → a · 16 = −4."),
                Level("3-4", 3, 4, "Gipfelstation", Kind.PARABOLA,
                    "y = −(x − d)² + e. Die Station sitzt genau auf dem Gipfel bei (−3 | 5).",
                    listOf(t(-3.0, 5.0)), listOf(pd, pe), mapOf("d" to -3.0, "e" to 5.0), fixed = mapOf("a" to -1.0),
                    asteroids = listOf(rock(4.0, 6.0)),
                    tip = "Der Gipfel ist der Scheitel (d | e)."),
                Level("3-5", 3, 5, "Drei Stationen", Kind.PARABOLA,
                    "Alle drei Regler! Durch (−2 | 2), (1 | −7) und (4 | 2).",
                    listOf(t(-2.0, 2.0), t(1.0, -7.0), t(4.0, 2.0)), listOf(pa, pd, pe),
                    mapOf("a" to 1.0, "d" to 1.0, "e" to -7.0),
                    asteroids = listOf(rock(1.0, 3.0, 1.2)),
                    tip = "(−2 | 2) und (4 | 2) liegen gleich hoch – der Scheitel ist genau in der Mitte."),
                Level("3-6", 3, 6, "Brückenbogen", Kind.PARABOLA,
                    "Ein Bogen von (−6 | −4) nach (6 | −4), Gipfel bei (0 | 5).",
                    listOf(t(-6.0, -4.0), t(6.0, -4.0)), listOf(pa, pd, pe),
                    mapOf("a" to -0.25, "d" to 0.0, "e" to 5.0),
                    asteroids = listOf(rock(0.0, -2.0, 1.5)), cells = listOf(cell(0.0, 5.0)),
                    tip = "Scheitel (0 | 5). Dann a · 36 + 5 = −4."),
            ),
        ),
        Chapter(
            4, "Wurf & Schwerkraft", "Physik Klasse 7–10", "🎯",
            listOf(
                "Jetzt echte Physik: Du schießt eine Kapsel mit Tempo v und Winkel α ab. Die Schwerkraft zieht sie nach unten.",
                "Waagerecht fliegt sie gleichmäßig: x = v · cos α · t. Senkrecht wird sie gebremst: y = v · sin α · t − ½ · g · t².",
                "g ist die Fallbeschleunigung: Erde 9,81 m/s², Mars 3,71, Mond nur 1,62 – dort fliegt alles viel weiter!",
                "Auf ebenem Boden ist die Wurfweite w = v² · sin(2α) : g. Bei 45° fliegt es am weitesten.",
            ),
            listOf(
                Level("4-1", 4, 1, "Erster Schuss", Kind.THROW,
                    "Erde, Winkel 45°. Das Ziel liegt 10 m entfernt. Welches Tempo?",
                    listOf(t(1.0, -9.0, 0.8)), listOf(pV()), mapOf("v" to 10.0), fixed = mapOf("angle" to 45.0),
                    tip = "Bei 45° gilt w = v² : g. Also v² = 10 · 9,81 ≈ 98."),
                Level("4-2", 4, 2, "Mondsprung", Kind.THROW,
                    "Auf dem Mond! 45°, Ziel 16 m entfernt.",
                    listOf(t(7.0, -9.0, 0.8)), listOf(pV(3.0)), mapOf("v" to 5.0), fixed = mapOf("angle" to 45.0),
                    planet = Planet.MOON,
                    tip = "v² = 16 · 1,62 ≈ 26. Wurzel daraus?"),
                Level("4-3", 4, 3, "Der richtige Winkel", Kind.THROW,
                    "Erde, Tempo fest 12 m/s. Ziel 12 m entfernt. Stell den Winkel ein!",
                    listOf(t(3.0, -9.0, 0.8)), listOf(pAngle), mapOf("angle" to 27.0), fixed = mapOf("v" to 12.0),
                    tip = "sin(2α) = 12 · 9,81 : 144 ≈ 0,82. Es gibt zwei Lösungen: flach oder steil!"),
                Level("4-4", 4, 4, "Mars-Plattform", Kind.THROW,
                    "Mars. Die Landeplattform liegt 15 m weit und 6 m hoch.",
                    listOf(t(6.0, -3.0, 0.8)), listOf(pAngle, pV()), mapOf("angle" to 45.0, "v" to 9.5),
                    planet = Planet.MARS, asteroids = listOf(Body(6.0, -6.5, 2.5)),
                    tip = "Probier 45°. Bei 45° gilt: y = x − g · x² : v². Mit x = 15, y = 6 bekommst du v."),
                Level("4-5", 4, 5, "Über den Berg", Kind.THROW,
                    "Erde. Ein Felsen versperrt den flachen Weg. Ziel 16 m entfernt.",
                    listOf(t(7.0, -9.0, 0.8)), listOf(pAngle, pV()), mapOf("angle" to 60.0, "v" to 13.5),
                    asteroids = listOf(Body(0.0, -7.0, 2.5)), cells = listOf(cell(0.0, -2.5)),
                    tip = "Steiler werfen! Bei 60° ist sin(120°) ≈ 0,87 → v² = 16 · 9,81 : 0,87."),
                Level("4-6", 4, 6, "Unter dem Meteor", Kind.THROW,
                    "Mond. Ein Meteor schwebt in der Mitte – flach drunter durch! Ziel 18 m entfernt.",
                    listOf(t(9.0, -9.0, 0.8)), listOf(pAngle, pV(3.0)), mapOf("angle" to 35.0, "v" to 5.5),
                    planet = Planet.MOON, asteroids = listOf(Body(0.0, -2.5, 1.3)),
                    tip = "Flacher als 45°, z. B. 35°: v² = 18 · 1,62 : sin(70°)."),
            ),
        ),

        Chapter(
            5, "Wellen & Wachstum", "Klasse 10", "🌊",
            listOf(
                "Funkwellen und Bakterien! Jetzt fliegt die Sonde auf Schwingungen und Wachstumskurven.",
                "Sinus: y = a · sin(b · x) + d. a ist die Amplitude – so hoch schlägt die Welle aus.",
                "b staucht die Welle: Eine ganze Schwingung ist 2π : b lang. d schiebt alles nach oben oder unten.",
                "Exponentiell: y = a · bˣ. Pro Kästchen nach rechts wird mit b malgenommen – b > 1 wächst, b < 1 zerfällt.",
            ),
            listOf(
                Level("5-1", 5, 1, "Wellenreiter", Kind.SINE,
                    "y = a · sin(x). Der Wellenberg bei x = π/2 ≈ 1,57 soll die Station (1,57 | 3) treffen.",
                    listOf(t(PI / 2, 3.0)), listOf(sa), mapOf("a" to 3.0), fixed = mapOf("b" to 1.0, "d" to 0.0),
                    cells = listOf(cell(-PI / 2, -3.0)),
                    tip = "sin(π/2) = 1. Also ist der Berg genau a hoch."),
                Level("5-2", 5, 2, "Lange Welle", Kind.SINE,
                    "y = 3 · sin(b · x). Der erste Wellenberg soll erst bei x = π ≈ 3,14 kommen.",
                    listOf(t(PI, 3.0)), listOf(sb), mapOf("b" to 0.5), fixed = mapOf("a" to 3.0, "d" to 0.0),
                    asteroids = listOf(rock(PI / 2, 3.6, 0.6)),
                    tip = "Der Berg liegt dort, wo b · x = π/2 ist. Mit x = π: b · π = π/2."),
                Level("5-3", 5, 3, "Höhenflug", Kind.SINE,
                    "y = 2 · sin(x) + d. Das Wellental bei x = −π/2 soll die Station (−1,57 | 3) treffen.",
                    listOf(t(-PI / 2, 3.0)), listOf(sd), mapOf("d" to 5.0), fixed = mapOf("a" to 2.0, "b" to 1.0),
                    cells = listOf(cell(-4.0, 2 * kotlin.math.sin(-4.0) + 5)),
                    tip = "sin(−π/2) = −1, also −2 + d = 3."),
                Level("5-4", 5, 4, "Wellentunnel", Kind.SINE,
                    "Alle drei Regler! Durch (0 | −1) und den Wellenberg (1,57 | 2).",
                    listOf(t(0.0, -1.0), t(PI / 2, 2.0)), listOf(sa, sb, sd), mapOf("a" to 3.0, "b" to 1.0, "d" to -1.0),
                    asteroids = listOf(rock(-3.0, 3.0), rock(3.5, 4.0)),
                    tip = "Bei x = 0 ist sin = 0 – die Station dort verrät dir d. Dann: Berg = a + d."),
                Level("5-5", 5, 5, "Verdopplung", Kind.EXP,
                    "Bakterien verdoppeln sich: y = a · 2ˣ. Die Kurve soll durch (3 | 4).",
                    listOf(t(3.0, 4.0)), listOf(ea), mapOf("a" to 0.5), fixed = mapOf("b" to 2.0, "d" to 0.0),
                    cells = listOf(cell(1.0, 1.0)),
                    tip = "2³ = 8. Also a · 8 = 4."),
                Level("5-6", 5, 6, "Wachstumsrate", Kind.EXP,
                    "y = bˣ. Welcher Wachstumsfaktor b bringt dich nach (2 | 9)?",
                    listOf(t(2.0, 9.0)), listOf(eb), mapOf("b" to 3.0), fixed = mapOf("a" to 1.0, "d" to 0.0),
                    asteroids = listOf(rock(3.0, 8.0, 0.7)), cells = listOf(cell(1.0, 3.0)),
                    tip = "b² = 9. Welche Zahl mal sich selbst ist 9?"),
                Level("5-7", 5, 7, "Zerfall", Kind.EXP,
                    "Radioaktiver Zerfall: y = 8 · bˣ. Nach 3 Kästchen ist nur noch 1 übrig – Station (3 | 1).",
                    listOf(t(3.0, 1.0)), listOf(eb), mapOf("b" to 0.5), fixed = mapOf("a" to 8.0, "d" to 0.0),
                    cells = listOf(cell(1.0, 4.0)),
                    tip = "8 · b³ = 1 → b³ = 1/8. Jedes Kästchen halbiert sich der Wert: Halbwertszeit 1!"),
            ),
        ),
        Chapter(
            6, "Schwerkraft-Schleuder", "Physik Klasse 10", "🪐",
            listOf(
                "Jetzt wird's wie bei echten Raumsonden: Planeten ziehen dich an!",
                "Gravitationsgesetz: F = G · m · M / r². Doppelte Masse M → doppelte Kraft. Doppelter Abstand r → nur ein Viertel der Kraft.",
                "Fliegst du nah vorbei, wird deine Bahn stark gebogen. Schnell vorbei → weniger Ablenkung.",
                "So holen sich echte Sonden wie Voyager Schwung. Stell Winkel und Tempo ein – die Planeten lenken mit!",
            ),
            listOf(
                Level("6-1", 6, 1, "Um die Ecke", Kind.ORBIT,
                    "Die Station liegt genau hinter dem Planeten. Tempo 5 ist fest – welcher Winkel biegt dich drumherum?",
                    listOf(t(8.5, 0.0, 0.8)), listOf(oAngle), mapOf("angle" to -20.0), fixed = mapOf("v" to 5.0),
                    asteroids = listOf(planet(0.0, 0.0, 1.6)), masses = listOf(25.0), launch = Vec(-9.0, 0.0),
                    tip = "Zielst du etwas am Planeten vorbei, zieht er dich zur Station zurück. Probier Winkel um −20°."),
                Level("6-2", 6, 2, "Abwärts", Kind.ORBIT,
                    "Die Station liegt tief unten rechts. Lass dich vom großen Planeten hinunterziehen.",
                    listOf(t(7.0, -7.0, 0.8)), listOf(oAngle, oV), mapOf("angle" to -55.0, "v" to 4.5),
                    asteroids = listOf(planet(1.0, -2.0, 1.5), rock(6.0, 2.0)), masses = listOf(35.0, 0.0), launch = Vec(-9.0, 5.0),
                    tip = "Steil nach unten zielen (etwa −55°) und nicht zu schnell."),
                Level("6-3", 6, 3, "Kehrtwende", Kind.ORBIT,
                    "Die Station liegt hinter dir oben links! Schwing dich um den Planeten herum zurück.",
                    listOf(t(-7.0, 6.0, 0.8)), listOf(oAngle, oV), mapOf("angle" to 75.0, "v" to 7.0),
                    asteroids = listOf(planet(2.0, 1.0, 1.4)), masses = listOf(40.0), launch = Vec(-9.0, -3.0),
                    tip = "Fast senkrecht nach oben (um 75°) und schnell – dann holt dich der Planet in einem Bogen zurück."),
                Level("6-4", 6, 4, "Doppelzug", Kind.ORBIT,
                    "Zwei Planeten ziehen gleichzeitig. Ziel: die Station oben rechts.",
                    listOf(t(8.0, 7.0, 0.8)), listOf(oAngle, oV), mapOf("angle" to 0.0, "v" to 3.5),
                    asteroids = listOf(planet(-2.0, -3.0, 1.3), planet(4.0, 3.0, 1.2), rock(1.0, 8.0)), masses = listOf(25.0, 25.0, 0.0),
                    launch = Vec(-9.0, -8.0),
                    tip = "Flach starten (0°), ein bisschen langsamer als 4 – die Planeten heben dich hoch."),
                Level("6-5", 6, 5, "Slalom", Kind.ORBIT,
                    "Zwischen zwei Planeten hindurch zur Station oben rechts.",
                    listOf(t(8.5, 5.0, 0.8)), listOf(oAngle, oV), mapOf("angle" to -1.0, "v" to 4.0),
                    asteroids = listOf(planet(-4.0, 3.0, 1.2), planet(3.0, -2.0, 1.2), rock(2.0, 6.0)), masses = listOf(20.0, 30.0, 0.0),
                    launch = Vec(-9.0, -6.0),
                    tip = "Fast waagerecht (um 0°) mit Tempo 4 – die Planeten machen den Rest."),
            ),
        ),
    )

    val all: List<Level> = chapters.flatMap { it.levels }
    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun next(id: String): Level? = all.indexOfFirst { it.id == id }.let { all.getOrNull(it + 1) }
}
