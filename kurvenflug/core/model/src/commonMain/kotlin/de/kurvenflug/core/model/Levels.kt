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
    /** Spiegel für den Laser: je Spiegel [x1, y1, x2, y2] */
    val mirrors: List<List<Double>> = emptyList(),
    /** Merkbild: rechtwinkliges Dreieck vom Start zur ersten Station mit beschrifteten Seiten (Trigonometrie) */
    val triangle: Triangle? = null,
    /** Kristall für den Laser: [x1, y1, x2, y2, Brechzahl n] (Rechteck) */
    val crystal: List<Double>? = null,
    /** Geodreieck am Laser einblenden (Winkel messen, Kl. 5–6) */
    val protractor: Boolean = false,
    /** gegebene Gleichung, die umgeformt werden soll (z. B. „y = x² − 6x + 5“) */
    val given: String? = null,
    /** eigene Rechen-Frage: erste = richtig, dann zwei typische Fehler (sonst automatisch je Bahnart) */
    val quiz: List<String>? = null,
    /** eigener Rechenweg (sonst automatisch je Bahnart) */
    val walk: List<Step>? = null,
) {
    /** Startwerte + feste Werte + aktuelle Einstellung */
    fun values(v: Map<String, Double>): Map<String, Double> = params.associate { it.key to it.start } + fixed + v
    val start: Map<String, Double> get() = params.associate { it.key to it.start }
}

/**
 * Rechtwinkliges Dreieck als Merkbild: Ecke A = Start, B = (Station.x | Start.y) mit rechtem Winkel, C = Station.
 * Beschriftungen sind frei („12“, „r = ?“, „30°“); null = Seite ohne Beschriftung.
 */
data class Triangle(val angle: String, val width: String? = null, val height: String? = null, val hyp: String? = null)

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

/** Abstand an der Station nach einem Fehlversuch: Bahn bei x war auf Höhe [from], die Station liegt auf [to]. */
data class Gap(val x: Double, val from: Double, val to: Double) {
    val distance get() = kotlin.math.abs(to - from)
    val tooLow get() = from < to
    /** „3 zu tief“ – kurz genug für das Spielfeld */
    val label get() = "${num(distance, 1)} zu ${if (tooLow) "tief" else "hoch"}"
}

/** Volts Rückmeldung nach einem Fehlversuch: konkret, mit Zahlen, ohne Schimpfen. */
object Feedback {
    /**
     * Für den Pfeil im Spielfeld: Wie weit lag die Kurve an der Stelle der verfehlten Station daneben?
     * Nur bei Funktionskurven, ohne Absturz und nicht bei getarnten Stationen (der Pfeil würde sie verraten).
     */
    fun gap(level: Level, values: Map<String, Double>, r: FlightResult): Gap? {
        if (r.success || r.crash != null || level.hidden || level.kind !in Flight.functionKinds) return null
        val i = level.targets.indices.firstOrNull { it !in r.hitTargets } ?: return null
        val t = level.targets[i]
        val y = Flight.f(level, values, t.x)
        if (y.isNaN()) return null
        val lim = Flight.R + 0.5
        return Gap(t.x, y.coerceIn(-lim, lim), t.y)
    }

    fun after(level: Level, values: Map<String, Double>, r: FlightResult): String {
        if (r.success) return ""
        if (r.crash != null && level.kind == Kind.RADIUS) return "Störsender gestreift! 📡 Deine Welle ist zu groß – genau ausrechnen, nicht aufrunden."
        if (r.crash != null && level.kind == Kind.LASER) return "Der Asteroid hat den Strahl geschluckt. Anderer Winkel – über einen Spiegel!"
        if (r.crash != null) return if (level.masses.getOrElse(r.crash) { 0.0 } > 0)
            "Eingefangen! 🪐 Der Planet hat dich angezogen. Schneller sein oder weiter an ihm vorbeizielen."
        else "Bumm – Asteroid! ☄️ Die Bahn muss an ihm vorbei."
        val missing = level.targets.indices.firstOrNull { it !in r.hitTargets } ?: 0
        val t = level.targets[missing]
        val v = level.values(values)
        return when (level.kind) {
            Kind.POINT -> {
                val dx = t.x - v.getValue("x"); val dy = t.y - v.getValue("y")
                // Zahlenstrahl (nur x einstellbar)
                if (level.params.none { it.key == "y" })
                    return "Gelandet bei ${num(v.getValue("x"))}. Die Station liegt weiter ${if (dx > 0) "rechts ➡️" else "links ⬅️"}."
                when {
                    dx != 0.0 && dy != 0.0 -> "Gelandet bei (${num(v.getValue("x"))} | ${num(v.getValue("y"))}). Beide Werte passen noch nicht."
                    dx != 0.0 -> "Die Höhe stimmt! Aber nach rechts/links (x) passt es noch nicht."
                    else -> "x stimmt! Jetzt noch die Höhe (y) – oben ist plus, unten minus."
                }
            }
            Kind.RADIUS -> {
                val dx = t.x - level.launch.x; val dy = t.y - level.launch.y
                if (level.triangle != null) {
                    val r0 = v.getValue("r")
                    return "Deine Welle hatte r = ${num(r0)} – ${if (r0 < kotlin.math.hypot(dx, dy)) "zu kurz" else "zu weit"}. " +
                        "r ist die Hypotenuse: Rechne mit sin oder cos!"
                }
                "Deine Welle hatte r = ${num(v.getValue("r"))}. Zur Station: ${num(kotlin.math.abs(dx))} nach ${if (dx >= 0) "rechts" else "links"}, " +
                    "${num(kotlin.math.abs(dy))} nach ${if (dy >= 0) "oben" else "unten"} – Pythagoras: r² = Δx² + Δy²."
            }
            Kind.LASER -> {
                if (level.protractor) {
                    val want = toDegrees(kotlin.math.atan2(t.y - level.launch.y, t.x - level.launch.x))
                    val got = v.getValue("angle")
                    return "Dein Winkel: ${num(got)}°. Die Station liegt bei einem ${if (want > got) "größeren" else "kleineren"} Winkel – " +
                        "weiter nach ${if (want > got) "links" else "rechts"} drehen."
                }
                if (level.crystal != null && r.crash == null) {
                    val near = r.path.minByOrNull { kotlin.math.hypot(it.x - t.x, it.y - t.y) } ?: return "Nochmal!"
                    return "Knapp ${num(kotlin.math.hypot(near.x - t.x, near.y - t.y) - t.r, 1)} Kästchen daneben. " +
                        "Im Glas knickt der Strahl zum Lot hin, beim Herauskommen vom Lot weg."
                }
                val near = r.path.minByOrNull { kotlin.math.hypot(it.x - t.x, it.y - t.y) } ?: return "Nochmal!"
                "Der Strahl verfehlt die Station um ${num(kotlin.math.hypot(near.x - t.x, near.y - t.y) - t.r, 1)} Kästchen. Einfallswinkel = Ausfallswinkel!"
            }
            Kind.RAMP -> {
                val land = r.path.lastOrNull() ?: return "Nochmal!"
                val h0 = v.getValue("h0")
                if (v.getValue("h") <= h0) "Die Kugel kommt nicht über die Kante (${num(h0)} m) – sie braucht mehr Lageenergie!"
                else { val dx = land.x - t.x; "${num(kotlin.math.abs(dx), 1)} m ${if (dx < 0) "zu kurz – höher starten!" else "zu weit – tiefer starten!"}" }
            }
            Kind.LINE, Kind.PARABOLA, Kind.SINE, Kind.EXP, Kind.PROP, Kind.ANTI, Kind.LOG, Kind.MOTION, Kind.ACCEL, Kind.POWER, Kind.EXPE -> {
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
            Kind.TANGENT -> {
                val m = v.getValue("m"); val c = Upper.c(v); val x0 = v.getValue("x0")
                val y = Upper.f(c, x0) + m * (t.x - x0)
                "Mit m = ${num(m)} warst du bei x = ${num(t.x)} auf y = ${num(y, 1)} – die Station liegt bei y = ${num(t.y)}. " +
                    "Die Tangente hat die Steigung der Kurve an der Absprungstelle: f'(${num(x0)})."
            }
            Kind.PEAK -> {
                val x = v.getValue("x"); val c = Upper.c(v)
                val slope = Upper.df(c, x)
                "Bei x = ${num(x)} ist die Steigung f'(${num(x)}) = ${num(slope, 2)} – noch nicht 0. " +
                    (if (level.tip.contains("f''(x) = ")) "Beim Wendepunkt ist f''(x) = 0." else "Am Extrempunkt ist die Kurve waagrecht: f'(x) = 0.")
            }
            Kind.AREA -> {
                val end = r.path.lastOrNull() ?: return "Nochmal!"
                if (end.x < t.x) "Der Treibstoff reicht nur bis x = ${num(end.x, 1)}. Mehr tanken – die Fläche bis ${num(t.x)} ist größer!"
                else "Zu viel getankt: Die Sonde flog bis x = ${num(end.x, 1)}. Die Fläche bis ${num(t.x)} ist kleiner."
            }
            Kind.VECTOR -> {
                val end = r.path.lastOrNull() ?: return "Nochmal!"
                "Nach t = ${num(v.getValue("T"))} warst du bei (${num(end.x, 1)} | ${num(end.y, 1)}) – die Station ist bei (${num(t.x)} | ${num(t.y)}). " +
                    "Rechne v = (Q − P) : t."
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
    private const val PI = kotlin.math.PI
    private fun planet(x: Double, y: Double, r: Double) = Body(x, y, r)
    private val pk = p("k", "k", -20.0, 20.0, 0.25, 1.0)
    /** Hyperbel: k ist hier immer ganzzahlig – feinere Schritte würden Raten belohnen */
    private val pkAnti = p("k", "k", -20.0, 20.0, 1.0, 1.0)
    private val pr = p("r", "Radius r", 0.5, 15.0, 0.1, 1.0)
    private val lb = p("b", "b", 0.5, 10.0, 0.5, 10.0)
    private val ld = p("d", "d", -8.0, 8.0, 1.0, 0.0)
    private val la = p("angle", "Winkel", -89.0, 89.0, 1.0, 0.0, "°")
    private val ph = p("h", "Höhe h", 0.5, 17.5, 0.5, 5.0, " m")
    private val mv = p("v", "v", -3.0, 3.0, 0.25, 1.0, " m/s")
    private val ms = p("s0", "s₀", -9.0, 9.0, 1.0, 0.0, " m")
    private fun mirror(x1: Double, y1: Double, x2: Double, y2: Double) = listOf(x1, y1, x2, y2)
    /** Landeplatz der Rampen-Kugel: Weite s = 2·√((h − h0)·h0) ab der Kante */
    private fun rampTarget(h: Double, h0: Double) = Body(Flight.RAMP_EDGE_X + 2 * kotlin.math.sqrt((h - h0) * h0), -9.0, 0.8)
    private fun rampLevel(n: Int, title: String, h: Double, h0: Double, task: String, tip: String, planet: Planet = Planet.EARTH, rocks: List<Body> = emptyList()) =
        Level("12-$n", 12, n, title, Kind.RAMP, task, listOf(rampTarget(h, h0)), listOf(ph), mapOf("h" to h), fixed = mapOf("h0" to h0),
            ground = -9.0, planet = planet, asteroids = rocks, tip = tip)

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

    private val baseChapters: List<Chapter> = listOf(
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
                    "Die Station ist getarnt! Funkspruch: „x = 2 − 4,  y = 3 + 4“",
                    listOf(t(-2.0, 7.0)), listOf(px, py), mapOf("x" to -2.0, "y" to 7.0), hidden = true,
                    tip = "2 − 4: von 2 aus vier Schritte nach links – du landest bei −2. Und 3 + 4 = 7."),
                Level("1-6", 1, 6, "Spiegelwelt", Kind.POINT,
                    "Getarnt: Die Station ist das Spiegelbild von (5 | −3) – gespiegelt an der x-Achse.",
                    listOf(t(5.0, 3.0)), listOf(px, py), mapOf("x" to 5.0, "y" to 3.0), hidden = true,
                    tip = "Beim Spiegeln an der x-Achse bleibt x gleich, y wechselt das Vorzeichen."),
                Level("1-7", 1, 7, "Halbe Strecke", Kind.POINT,
                    "Getarnt: Die Station liegt genau in der Mitte zwischen (−6 | 6) und (2 | −2).",
                    listOf(t(-2.0, 2.0)), listOf(px, py), mapOf("x" to -2.0, "y" to 2.0), hidden = true,
                    tip = "Zähl die Kästchen: von −6 bis 2 sind es 8, die Hälfte ist 4 → x = −6 + 4. Bei y genauso: von 6 bis −2 sind es 8, also 4 nach unten."),
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
                    "y = −(x − d)² + e. Die Station sitzt genau auf dem Gipfel bei (−3 | 5), eine zweite wartet bei (−1 | 1).",
                    listOf(t(-3.0, 5.0), t(-1.0, 1.0)), listOf(pd, pe), mapOf("d" to -3.0, "e" to 5.0), fixed = mapOf("a" to -1.0),
                    asteroids = listOf(rock(4.0, 6.0)),
                    tip = "Der Gipfel ist der Scheitel (d | e). Probe: 2 Schritte neben dem Gipfel geht es um 2² = 4 nach unten."),
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


    private val extraChapters = listOf(
        Chapter(
            8, "Zuordnungen", "Klasse 6–7", "⚖️",
            listOf(
                "Proportional heißt: doppelt so viel x → doppelt so viel y. Die Gerade geht durch (0 | 0): y = k · x.",
                "k ist der Quotient: k = y : x. Prozent sind auch proportional – 25 % heißt k = 0,25.",
                "Antiproportional heißt: doppelt so viel x → halb so viel y. Dann ist x · y immer gleich k: y = k : x.",
                "Die Bahn ist dann eine Hyperbel – sie kommt der Achse nahe, berührt sie aber nie.",
            ),
            listOf(
                Level("8-1", 8, 1, "Treibstoff", Kind.PROP, "Proportional: y = k · x. Die Station liegt bei (4 | 6).",
                    listOf(t(4.0, 6.0)), listOf(pk), mapOf("k" to 1.5), cells = listOf(cell(2.0, 3.0)), tip = "k = y : x = 6 : 4."),
                Level("8-2", 8, 2, "Bergab", Kind.PROP, "y = k · x durch (6 | −3).",
                    listOf(t(6.0, -3.0)), listOf(pk), mapOf("k" to -0.5), asteroids = listOf(rock(4.0, 4.0)), tip = "k = −3 : 6."),
                Level("8-3", 8, 3, "Prozent-Schub", Kind.PROP, "Bei x = 8 soll y genau 25 % von 8 sein. Wie groß ist k?",
                    listOf(t(8.0, 2.0)), listOf(pk), mapOf("k" to 0.25), cells = listOf(cell(4.0, 1.0)), tip = "25 % = 25/100 = 0,25."),
                Level("8-4", 8, 4, "Hyperbel", Kind.ANTI, "Antiproportional: y = k : x. Die Station liegt bei (2 | 4).",
                    listOf(t(2.0, 4.0)), listOf(pkAnti), mapOf("k" to 8.0), cells = listOf(cell(1.0, 8.0)), tip = "k = x · y = 2 · 4."),
                Level("8-5", 8, 5, "Zwei Stationen", Kind.ANTI, "y = k : x durch (2 | 6) und (6 | 2).",
                    listOf(t(2.0, 6.0), t(6.0, 2.0)), listOf(pkAnti), mapOf("k" to 12.0), asteroids = listOf(rock(6.0, 6.0)),
                    tip = "Bei beiden ist x · y gleich – genau das ist k."),
                Level("8-6", 8, 6, "Kopfüber", Kind.ANTI, "y = k : x durch (2 | −4).",
                    listOf(t(2.0, -4.0)), listOf(pkAnti), mapOf("k" to -8.0), tip = "k = 2 · (−4)."),
            ),
        ),
        Chapter(
            13, "Weg & Zeit", "Physik Klasse 7", "⏱️",
            listOf(
                "Jetzt ist das Bild ein Weg-Zeit-Diagramm: nach rechts die Zeit t in Sekunden, nach oben der Weg s in Metern.",
                "Gleichförmige Bewegung: s = v · t + s₀. v ist die Geschwindigkeit, s₀ der Startpunkt.",
                "Je steiler die Gerade, desto schneller. Fällt sie, fliegt die Sonde zurück.",
                "Triff die Stationen zur richtigen Zeit am richtigen Ort!",
            ),
            listOf(
                Level("13-1", 13, 1, "Pünktlich", Kind.MOTION, "Start bei 0 m. Nach 4 s soll die Sonde bei 8 m sein.",
                    listOf(t(4.0, 8.0)), listOf(mv), mapOf("v" to 2.0), fixed = mapOf("s0" to 0.0), tip = "v = Weg : Zeit = 8 m : 4 s."),
                Level("13-2", 13, 2, "Schleichfahrt", Kind.MOTION, "Start bei 2 m. Nach 6 s bei 5 m.",
                    listOf(t(6.0, 5.0)), listOf(mv), mapOf("v" to 0.5), fixed = mapOf("s0" to 2.0), cells = listOf(cell(2.0, 3.0)),
                    tip = "In 6 s kommen 3 m dazu."),
                Level("13-3", 13, 3, "Zwei Termine", Kind.MOTION, "Nach 2 s bei 6 m, nach 8 s bei 9 m. Wo und wie schnell?",
                    listOf(t(2.0, 6.0), t(8.0, 9.0)), listOf(mv, ms), mapOf("v" to 0.5, "s0" to 5.0),
                    tip = "v = (9 − 6) : (8 − 2). Dann zurückrechnen auf t = 0."),
                Level("13-4", 13, 4, "Rückflug", Kind.MOTION, "Start bei 8 m, nach 8 s zurück an der Basis (0 m).",
                    listOf(t(8.0, 0.0)), listOf(mv, ms), mapOf("v" to -1.0, "s0" to 8.0), cells = listOf(cell(4.0, 4.0)),
                    tip = "Zurück heißt: v ist negativ."),
            ),
        ),
        Chapter(
            11, "Laser-Optik", "Physik Klasse 7", "🔦",
            listOf(
                "Laser! Der Strahl läuft schnurgerade – bis er auf einen Spiegel trifft.",
                "Reflexionsgesetz: Einfallswinkel = Ausfallswinkel. Der Strahl prallt ab wie ein Ball von der Bande.",
                "Asteroiden schlucken das Licht. Du stellst nur den Winkel ein, unter dem der Laser feuert.",
                "Tipp: Spiegel die Station am Spiegel – dann zielst du einfach auf das Spiegelbild!",
            ),
            listOf(
                Level("11-1", 11, 1, "Über Bande", Kind.LASER, "Der direkte Weg ist versperrt. Nutz den Spiegel am Boden!",
                    listOf(t(6.0, 3.0, 0.6)), listOf(la), mapOf("angle" to -45.0), mirrors = listOf(mirror(-6.0, -6.0, 3.0, -6.0)),
                    asteroids = listOf(rock(-1.5, 1.5, 1.2)), launch = Vec(-9.0, 0.0),
                    tip = "Spiegelbild der Station unterm Boden-Spiegel: (6 | −15). Ziel darauf – oder probier −45°."),
                Level("11-2", 11, 2, "Periskop", Kind.LASER, "Zwei schräge Spiegel wie in einem U-Boot-Periskop.",
                    listOf(t(8.0, 7.0, 0.6)), listOf(la), mapOf("angle" to 4.0),
                    mirrors = listOf(mirror(-2.0, -7.0, 2.0, -3.0), mirror(-2.0, 3.0, 2.0, 7.0)),
                    asteroids = listOf(rock(-4.0, -1.5, 1.2)), launch = Vec(-9.0, -5.0),
                    tip = "Der 45°-Spiegel lenkt einen waagerechten Strahl senkrecht nach oben. Ziel fast waagerecht!"),
                Level("11-3", 11, 3, "Zickzack", Kind.LASER, "Decke und Boden sind Spiegel. Die Station liegt hinter dem Felsen.",
                    listOf(t(8.0, -7.0, 0.6)), listOf(la), mapOf("angle" to 58.0),
                    mirrors = listOf(mirror(-6.0, 7.0, 6.0, 7.0), mirror(-4.0, -8.0, 4.0, -8.0)),
                    asteroids = listOf(rock(0.0, -6.5, 1.5)), launch = Vec(-9.0, -7.0),
                    tip = "Steil nach oben, an der Decke abprallen – der Strahl kommt steil herunter."),
                Level("11-4", 11, 4, "Kehrtwende", Kind.LASER, "Die Station liegt links oben – hinter dir! Der Spiegel rechts hilft.",
                    listOf(t(-8.0, 6.0, 0.6)), listOf(la), mapOf("angle" to 16.0), mirrors = listOf(mirror(7.0, -6.0, 7.0, 6.0)),
                    asteroids = listOf(rock(-8.5, 2.0)), launch = Vec(-9.0, -3.0),
                    tip = "Am senkrechten Spiegel kehrt sich nur die Richtung nach rechts/links um – die Steigung bleibt."),
                Level("11-5", 11, 5, "Doppel-Z", Kind.LASER, "Erst nach rechts unten, dann hoch, dann zurück nach links.",
                    listOf(t(-6.0, 4.0, 0.6)), listOf(la), mapOf("angle" to -1.0),
                    mirrors = listOf(mirror(5.0, -8.0, 8.0, -5.0), mirror(4.0, 6.0, 8.0, 2.0)),
                    asteroids = listOf(rock(-7.5, -1.0, 1.2), rock(0.0, 0.0, 1.5)), launch = Vec(-9.0, -6.0),
                    tip = "Fast waagerecht feuern – die schrägen Spiegel machen den Rest."),
            ),
        ),
        Chapter(
            9, "Pythagoras", "Klasse 8–9", "📡",
            listOf(
                "Funkstille! Dein Sender schickt eine kreisrunde Welle mit Radius r.",
                "Die Welle muss genau die Station erreichen – aber keinen Störsender ☄️ streifen. Also: r genau ausrechnen!",
                "Abstand zwischen zwei Punkten: erst Δx und Δy zählen, dann Satz des Pythagoras: r² = Δx² + Δy².",
                "Beispiel: 3 rechts, 4 hoch → r² = 9 + 16 = 25 → r = 5.",
            ),
            listOf(
                Level("9-1", 9, 1, "Erstkontakt", Kind.RADIUS, "Sender in der Mitte. Station bei (3 | 4). Welcher Radius?",
                    listOf(t(3.0, 4.0, 0.25)), listOf(pr), mapOf("r" to 5.0), asteroids = listOf(rock(7.0, 6.0)), launch = Vec(0.0, 0.0),
                    tip = "r² = 3² + 4²."),
                Level("9-2", 9, 2, "Weiter Weg", Kind.RADIUS, "Sender bei (−5 | −5), Station bei (1 | 3).",
                    listOf(t(1.0, 3.0, 0.25)), listOf(pr), mapOf("r" to 10.0), launch = Vec(-5.0, -5.0),
                    tip = "Δx = 6, Δy = 8."),
                Level("9-3", 9, 3, "Wurzel-Welle", Kind.RADIUS, "Sender in der Mitte, Station bei (5 | −5). Achtung, Störsender oben links!",
                    listOf(t(5.0, -5.0, 0.25)), listOf(pr), mapOf("r" to 7.1), asteroids = listOf(rock(-6.0, 6.0)), launch = Vec(0.0, 0.0),
                    tip = "r² = 25 + 25 = 50 → r = √50 ≈ 7,1. Nicht aufrunden – sonst triffst du den Störsender!"),
                Level("9-4", 9, 4, "Große Reichweite", Kind.RADIUS, "Sender bei (−6 | 2), Station bei (6 | 7).",
                    listOf(t(6.0, 7.0, 0.25)), listOf(pr), mapOf("r" to 13.0), launch = Vec(-6.0, 2.0),
                    tip = "Δx = 12, Δy = 5 → 144 + 25."),
                Level("9-5", 9, 5, "Doppelfunk", Kind.RADIUS, "Eine Welle für zwei Stationen: (6 | 8) und (−8 | −6).",
                    listOf(t(6.0, 8.0, 0.25), t(-8.0, -6.0, 0.25)), listOf(pr), mapOf("r" to 10.0),
                    asteroids = listOf(rock(0.0, 4.0)), launch = Vec(0.0, 0.0), tip = "Beide sind gleich weit weg: 36 + 64."),
            ),
        ),
        Chapter(
            12, "Energie", "Physik Klasse 8–9", "🎢",
            listOf(
                "Eine Kugel rollt die Rampe hinunter. Oben hat sie Lageenergie, unten Bewegungsenergie.",
                "Energieerhaltung (ohne Reibung): m · g · h = ½ · m · v². Je höher du startest, desto schneller ist sie unten.",
                "An der Kante fliegt sie waagerecht ab. Die Weite ist: s = 2 · √((h − h₀) · h₀), h₀ = Höhe der Kante.",
                "Verrückt: In dieser Formel kommt g gar nicht vor! Wähl die Starthöhe h.",
            ),
            listOf(
                rampLevel(1, "Erster Sprung", 8.0, 4.0, "Die Kante liegt 4 m hoch. Das Ziel ist 8 m hinter der Kante.",
                    "8 = 2 · √((h − 4) · 4) → √((h − 4) · 4) = 4 → (h − 4) · 4 = 16."),
                rampLevel(2, "Niedrige Kante", 10.0, 2.0, "Kante nur 2 m hoch, Ziel wieder 8 m weit.",
                    "(h − 2) · 2 = 16 – rechne h aus."),
                rampLevel(3, "Mini-Kante", 10.0, 1.0, "Kante 1 m hoch, Ziel 6 m weit.", "(h − 1) · 1 = 9."),
                rampLevel(4, "Mond-Rampe", 8.0, 4.0, "Auf dem Mond! Kante 4 m, Ziel 8 m – wie auf der Erde. Ändert sich was?",
                    "Die Weite hängt nicht von g ab – es ist dieselbe Rechnung wie in Level 1!", planet = Planet.MOON),
                rampLevel(5, "Mars-Hürde", 7.0, 3.0, "Mars. Kante 3 m, Ziel rund 6,9 m weit.",
                    "(h − 3) · 3 = 12 → h − 3 = 4.", planet = Planet.MARS),
            ),
        ),
        Chapter(
            10, "Logarithmus", "Klasse 10", "🔢",
            listOf(
                "Der Logarithmus ist die Umkehrung der Potenz: log₂(8) = 3, weil 2³ = 8.",
                "log_b(x) fragt: Mit welcher Hochzahl wird b zu x?",
                "Die Kurve y = log_b(x) + d gibt es nur rechts von der y-Achse. Bei x = 1 ist log immer 0 – dort liest du d ab.",
                "Ist b kleiner als 1, fällt die Kurve. Los!",
            ),
            listOf(
                Level("10-1", 10, 1, "Zweierpotenz", Kind.LOG, "y = log_b(x). Die Station liegt bei (8 | 3).",
                    listOf(t(8.0, 3.0)), listOf(lb), mapOf("b" to 2.0), fixed = mapOf("d" to 0.0), cells = listOf(cell(4.0, 2.0)),
                    tip = "b³ = 8. Welche Zahl hoch 3 ist 8?"),
                Level("10-2", 10, 2, "Dreierpotenz", Kind.LOG, "y = log_b(x) durch (9 | 2).",
                    listOf(t(9.0, 2.0, 0.35)), listOf(lb), mapOf("b" to 3.0), fixed = mapOf("d" to 0.0), tip = "b² = 9."),
                Level("10-3", 10, 3, "Höhenschub", Kind.LOG, "y = log₂(x) + d durch (4 | 5).",
                    listOf(t(4.0, 5.0)), listOf(ld), mapOf("d" to 3.0), fixed = mapOf("b" to 2.0), tip = "log₂(4) = 2, also 2 + d = 5."),
                Level("10-4", 10, 4, "Abwärts", Kind.LOG, "y = log_b(x) durch (4 | −2). Die Kurve fällt!",
                    listOf(t(4.0, -2.0)), listOf(lb), mapOf("b" to 0.5), fixed = mapOf("d" to 0.0),
                    tip = "b⁻² = 4 → b = 1/2. Bei b < 1 fällt die Kurve."),
                Level("10-5", 10, 5, "Doppelspur", Kind.LOG, "Beide Regler: durch (1 | −2) und (8 | 1).",
                    listOf(t(1.0, -2.0), t(8.0, 1.0)), listOf(lb, ld), mapOf("b" to 2.0, "d" to -2.0),
                    tip = "Bei x = 1 ist log = 0 – das verrät d. Dann: log_b(8) = 3."),
            ),
        ),
        Chapter(
            14, "Trigonometrie", "Klasse 9–10", "📐",
            listOf(
                "Im rechtwinkligen Dreieck hängen Winkel und Seiten fest zusammen – damit zielen echte Raumsonden!",
                "tan α = Gegenkathete : Ankathete – beim Laser also Höhe : Weite. Den Winkel holst du mit tan⁻¹ (Taschenrechner: SHIFT + tan).",
                "sin α = Gegenkathete : Hypotenuse (GK : r), cos α = Ankathete : Hypotenuse (AK : r). Beim Funk ist die Hypotenuse der Radius r.",
                "Ich zeichne dir das Dreieck ein. Taschenrechner ist erlaubt – rechnen musst du trotzdem selbst! 🧮",
            ),
            listOf(
                Level("14-1", 14, 1, "Steigflug", Kind.LASER, "Die Station liegt 12 weiter rechts und 9 höher. Unter welchem Winkel feuern?",
                    listOf(t(4.0, 3.0, 0.5)), listOf(la), mapOf("angle" to 37.0), launch = Vec(-8.0, -6.0),
                    triangle = Triangle("α = ?", width = "12", height = "9"),
                    tip = "tan α = 9 : 12 = 0,75 → α = tan⁻¹(0,75) ≈ 37°."),
                Level("14-2", 14, 2, "Flacher Schuss", Kind.LASER, "Weite 17, Höhe 4 – ein ganz flacher Winkel.",
                    listOf(t(8.0, 2.0, 0.5)), listOf(la), mapOf("angle" to 13.0), launch = Vec(-9.0, -2.0),
                    asteroids = listOf(rock(0.0, 5.0, 1.2)),
                    triangle = Triangle("α = ?", width = "17", height = "4"),
                    tip = "tan α = 4 : 17 ≈ 0,235 → α ≈ 13°."),
                Level("14-3", 14, 3, "Sinkflug", Kind.LASER, "Diesmal abwärts: 14 nach rechts, 10 nach unten. Ein Winkel nach unten ist negativ.",
                    listOf(t(6.0, -4.0, 0.5)), listOf(la), mapOf("angle" to -36.0), launch = Vec(-8.0, 6.0),
                    triangle = Triangle("α = ?", width = "14", height = "10"),
                    tip = "tan α = 10 : 14 ≈ 0,714 → 35,5°, gerundet 36° – nach unten, also −36°."),
                Level("14-4", 14, 4, "Berechnete Bande", Kind.LASER,
                    "Der Felsen versperrt den direkten Weg. Spiegel die Station am Boden-Spiegel und rechne den Winkel aus!",
                    listOf(t(7.0, 3.0, 0.5)), listOf(la), mapOf("angle" to -48.0), launch = Vec(-9.0, 3.0),
                    mirrors = listOf(mirror(-6.0, -6.0, 6.0, -6.0)), asteroids = listOf(rock(-1.0, 3.0, 1.2)),
                    tip = "Spiegelbild der Station: (7 | −15). Weite 16, Tiefe 18 → tan α = 18 : 16 = 1,125 → α ≈ −48°."),
                Level("14-5", 14, 5, "Funkmast", Kind.RADIUS, "Die Station liegt unter 30° und 5 Kästchen höher als der Sender. Wie weit muss die Welle reichen?",
                    listOf(t(0.66, -3.0, 0.25)), listOf(pr), mapOf("r" to 10.0), launch = Vec(-8.0, -8.0),
                    triangle = Triangle("30°", height = "5", hyp = "r = ?"),
                    tip = "sin 30° = 5 : r. Weil sin 30° = 0,5 ist: r = 5 : 0,5 = 10."),
                Level("14-6", 14, 6, "Weitfunk", Kind.RADIUS, "Unter 40°, 10 Kästchen weiter rechts. Wie groß ist r?",
                    listOf(t(2.0, 0.39, 0.25)), listOf(pr), mapOf("r" to 13.1), launch = Vec(-8.0, -8.0),
                    triangle = Triangle("40°", width = "10", hyp = "r = ?"),
                    tip = "cos 40° = 10 : r → r = 10 : cos 40° ≈ 10 : 0,766 ≈ 13,1."),
                Level("14-7", 14, 7, "Steilfunk", Kind.RADIUS, "Steil nach oben: 60° und 9 Kästchen Höhe. Achtung, Störsender!",
                    listOf(t(-0.8, 1.0, 0.25)), listOf(pr), mapOf("r" to 10.4), launch = Vec(-6.0, -8.0),
                    asteroids = listOf(rock(5.3, -8.0, 0.5)),
                    triangle = Triangle("60°", height = "9", hyp = "r = ?"),
                    tip = "sin 60° = 9 : r → r = 9 : sin 60° ≈ 9 : 0,866 ≈ 10,4. Nicht aufrunden – der Störsender wartet bei 11!"),
            ),
        ),
    )

    /** Zahlenstrahl: nur x, y bleibt 0 (Kl. 5–6) */
    private val nlQ = p("x", "x", -9.0, 9.0, 0.25, 0.0)
    private val nlD = p("x", "x", -9.0, 9.0, 0.1, 0.0)
    private fun nl(x: Double) = Body(x, 0.0, 0.06)
    /** Winkel 0–180° mit Geodreieck, Laser von unten Mitte */
    private val wAng = p("angle", "Winkel", 0.0, 180.0, 1.0, 0.0, "°")
    private val wLaunch = Vec(0.0, -7.0)
    private fun atAngle(deg: Double, dist: Double = 10.0) =
        Body(wLaunch.x + dist * kotlin.math.cos(toRadians(deg)), wLaunch.y + dist * kotlin.math.sin(toRadians(deg)), 0.3)
    private fun angleLevel(n: Int, title: String, deg: Double, task: String, tip: String, hidden: Boolean = false) =
        Level("16-$n", 16, n, title, Kind.LASER, task, listOf(atAngle(deg)), listOf(wAng), mapOf("angle" to deg),
            launch = wLaunch, protractor = true, hidden = hidden, tip = tip)

    private val newChapters = listOf(
        Chapter(
            15, "Brüche & Kommazahlen", "Klasse 5–6", "🍕",
            listOf(
                "Jetzt landest du auf dem Zahlenstrahl! Ein Kästchen ist 1 – und es lässt sich teilen wie eine Pizza. 🍕",
                "½ heißt: ein Kästchen in 2 Teile, einer davon. ¾: in 4 Teile, drei davon.",
                "Kommazahlen sind Zehntel: 0,5 = 5 Zehntel = ½. Und 0,25 = ¼.",
                "Minus heißt wieder: nach links. Los geht's!",
            ),
            listOf(
                Level("15-1", 15, 1, "Halbe Sache", Kind.POINT, "Die Station liegt bei 2½ – zweieinhalb Kästchen nach rechts.",
                    listOf(nl(2.5)), listOf(nlQ), mapOf("x" to 2.5), fixed = mapOf("y" to 0.0), tip = "2½ = 2 + ½. Mit dem Regler: 2,5."),
                Level("15-2", 15, 2, "Dreiviertel", Kind.POINT, "Station bei ¾. Ein Kästchen hat 4 Viertel.",
                    listOf(nl(0.75)), listOf(nlQ), mapOf("x" to 0.75), fixed = mapOf("y" to 0.0), tip = "¾ = 3 Viertel = 0,75."),
                Level("15-3", 15, 3, "Mehr als ein Ganzes", Kind.POINT, "Station bei 7/4. Wie viele Ganze stecken drin?",
                    listOf(nl(1.75)), listOf(nlQ), mapOf("x" to 1.75), fixed = mapOf("y" to 0.0), tip = "7/4 = 4/4 + 3/4 = 1¾ = 1,75."),
                Level("15-4", 15, 4, "Links vom Start", Kind.POINT, "Station bei −1¼. Minus heißt: nach links!",
                    listOf(nl(-1.25)), listOf(nlQ), mapOf("x" to -1.25), fixed = mapOf("y" to 0.0), tip = "1¼ = 1,25 – also −1,25."),
                Level("15-5", 15, 5, "Zehntel", Kind.POINT, "Station bei 3,7 – drei Ganze und sieben Zehntel.",
                    listOf(nl(3.7)), listOf(nlD), mapOf("x" to 3.7), fixed = mapOf("y" to 0.0), tip = "3,7: bis zur 3, dann noch 7 kleine Striche."),
                Level("15-6", 15, 6, "Komma-Funkspruch", Kind.POINT, "Getarnt! Funkspruch: „0,8 + 1,5“",
                    listOf(nl(2.3)), listOf(nlD), mapOf("x" to 2.3), fixed = mapOf("y" to 0.0), hidden = true,
                    tip = "Zehntel zusammenzählen: 8 + 15 = 23 Zehntel = 2,3."),
                Level("15-7", 15, 7, "Die Hälfte", Kind.POINT, "Getarnt! Die Station liegt bei der Hälfte von 7.",
                    listOf(nl(3.5)), listOf(nlQ), mapOf("x" to 3.5), fixed = mapOf("y" to 0.0), hidden = true, tip = "7 : 2 = 3½ = 3,5."),
                Level("15-8", 15, 8, "Bruch wird Komma", Kind.POINT, "Getarnt! Station bei −3/5.",
                    listOf(nl(-0.6)), listOf(nlD), mapOf("x" to -0.6), fixed = mapOf("y" to 0.0), hidden = true,
                    tip = "3/5 = 6/10 = 0,6 – und das Minus heißt links."),
            ),
        ),
        Chapter(
            16, "Winkel", "Klasse 5–6", "📐",
            listOf(
                "Das ist ein Geodreieck! Der Laser dreht sich von rechts (0°) über oben (90°) bis nach links (180°).",
                "90° ist ein rechter Winkel – genau senkrecht. Kleiner als 90°: spitz. Größer: stumpf.",
                "Lies die Station am Geodreieck ab – oder rechne den Winkel aus.",
                "Alle Winkel im Dreieck zusammen: 180°. Nebenwinkel ergänzen sich auch zu 180°.",
            ),
            listOf(
                angleLevel(1, "Rechter Winkel", 90.0, "Feuere genau senkrecht nach oben – ein rechter Winkel.", "Rechter Winkel = 90°."),
                angleLevel(2, "Halb so viel", 45.0, "Die Station liegt genau auf halbem Weg zwischen 0° und 90°.", "90° : 2 = 45°."),
                angleLevel(3, "Spitz", 30.0, "Lies den Winkel der Station am Geodreieck ab.", "Zähl die kleinen Striche: je 10°."),
                angleLevel(4, "Stumpf", 120.0, "Ein stumpfer Winkel – über die 90° hinaus.", "90° + 30° = 120°."),
                angleLevel(5, "Nebenwinkel", 130.0, "Getarnt! Der Strahl soll den Nebenwinkel von 50° nehmen.",
                    "Nebenwinkel: 180° − 50° = 130°.", hidden = true),
                angleLevel(6, "Dreieck", 50.0, "Getarnt! Im Dreieck sind zwei Winkel 60° und 70°. Feuere unter dem dritten.",
                    "180° − 60° − 70° = 50°.", hidden = true),
            ),
        ),
        Chapter(
            17, "Lichtbrechung", "Physik Klasse 9", "💎",
            listOf(
                "Kristalle! Wenn Licht von Luft in Glas geht, knickt es ab – zum Lot hin. Beim Herausgehen wieder vom Lot weg.",
                "Brechungsgesetz: sin α₁ = n · sin α₂. Glas hat n ≈ 1,5, Wasser n ≈ 1,33.",
                "Kommt Licht von innen sehr flach an die Kante, kann es gar nicht heraus: Totalreflexion! Wie in einem Glasfaserkabel.",
                "Tipp: „Ganze Bahn“ zeigt dir, wie der Strahl knickt – zum Tüfteln erlaubt.",
            ),
            listOf(
                Level("17-1", 17, 1, "Glasplatte", Kind.LASER, "Der Strahl muss durch die Glasplatte. Danach läuft er parallel weiter – aber versetzt!",
                    listOf(t(8.0, 2.0, 0.5)), listOf(la), mapOf("angle" to 22.0), launch = Vec(-9.0, -4.0),
                    crystal = listOf(-3.0, -9.5, 3.0, 9.5, 1.5),
                    tip = "Im Glas knickt der Strahl zum Lot hin – er wird flacher. Ziel also etwas steiler als direkt."),
                Level("17-2", 17, 2, "Wasserbecken", Kind.LASER, "Von oben ins Wasser: Die Station liegt am Grund.",
                    listOf(t(4.0, -8.0, 0.5)), listOf(la), mapOf("angle" to -46.0), launch = Vec(-8.0, 7.0),
                    crystal = listOf(-9.5, -9.5, 9.5, -1.0, 1.33),
                    tip = "Beim Eintauchen knickt der Strahl zum Lot (senkrecht) hin – er wird steiler."),
                Level("17-3", 17, 3, "Totalreflexion", Kind.LASER, "Du bist im Glas, ein Felsen ist im Weg. Lass den Strahl an der Glaskante zurückprallen – ganz ohne Spiegel!",
                    listOf(t(1.5, -6.0, 0.5)), listOf(la), mapOf("angle" to 40.0), launch = Vec(-8.0, -6.0),
                    crystal = listOf(-9.5, -9.5, 9.5, -2.0, 1.5), asteroids = listOf(rock(-3.0, -6.0, 1.0)),
                    tip = "Flach genug zielen (unter 48°), dann bleibt das Licht im Glas: Totalreflexion."),
                Level("17-4", 17, 4, "Um die Ecke", Kind.LASER, "Ein Felsen versperrt den Weg. Der Glasblock lenkt den Strahl.",
                    listOf(t(7.0, -7.0, 0.5)), listOf(la), mapOf("angle" to -25.0), launch = Vec(-9.0, 5.0),
                    crystal = listOf(-3.0, -9.5, 9.5, 0.0, 1.5), asteroids = listOf(rock(1.0, -3.0, 1.2)),
                    tip = "Im Glas wird der Strahl steiler. Probier mit „Ganze Bahn“, wo er durchkommt."),
                Level("17-5", 17, 5, "Glasfaser", Kind.LASER, "Ein langer Glasstab. Der Strahl soll im Zickzack bis zum Ende laufen.",
                    listOf(t(8.5, 0.0, 0.5)), listOf(la), mapOf("angle" to 13.0), launch = Vec(-9.0, 0.0),
                    crystal = listOf(-9.5, -2.0, 9.5, 2.0, 1.5), asteroids = listOf(rock(0.0, 0.0, 1.0)),
                    tip = "Schräg feuern, aber flach genug für Totalreflexion an den Kanten."),
            ),
        ),
        Chapter(
            18, "Nullstellen", "Klasse 9", "🎯",
            listOf(
                "Wo landet die Sonde? Dort, wo die Parabel die x-Achse schneidet: die Nullstellen.",
                "Die Nullstellen liegen symmetrisch um den Scheitel. Also: Scheitel-x = Mitte der Nullstellen.",
                "Normalform y = x² + px + q in Scheitelform bringen: d = −p : 2 und e = q − d² (quadratische Ergänzung).",
                "Oder gleich die pq-Formel: x = −p/2 ± √((p/2)² − q). Los!",
            ),
            listOf(
                Level("18-1", 18, 1, "Genau in der Mitte", Kind.PARABOLA, "y = (x − d)² − 4. Die Sonde soll bei x = 1 und x = 5 landen.",
                    listOf(t(1.0, 0.0), t(5.0, 0.0)), listOf(pd), mapOf("d" to 3.0), fixed = mapOf("a" to 1.0, "e" to -4.0),
                    tip = "Der Scheitel liegt genau in der Mitte: (1 + 5) : 2."),
                Level("18-2", 18, 2, "Wie tief?", Kind.PARABOLA, "y = (x − d)² + e. Landeplätze bei x = −2 und x = 4.",
                    listOf(t(-2.0, 0.0), t(4.0, 0.0)), listOf(pd, pe), mapOf("d" to 1.0, "e" to -9.0), fixed = mapOf("a" to 1.0),
                    tip = "d = Mitte = 1. Von dort 3 bis zur Nullstelle: 3² = 9 nach unten, also e = −9."),
                Level("18-3", 18, 3, "Normalform", Kind.PARABOLA, "Gegeben ist y = x² − 4x − 5. Bring sie in Scheitelform!",
                    listOf(t(-1.0, 0.0), t(5.0, 0.0)), listOf(pd, pe), mapOf("d" to 2.0, "e" to -9.0), fixed = mapOf("a" to 1.0),
                    given = "y = x² − 4x − 5", tip = "d = −p : 2 = 4 : 2 = 2. e = q − d² = −5 − 4 = −9."),
                Level("18-4", 18, 4, "pq-Formel", Kind.PARABOLA, "Getarnt! y = x² + 2x − 8. Die Landeplätze sind unsichtbar – rechne!",
                    listOf(t(-4.0, 0.0), t(2.0, 0.0)), listOf(pd, pe), mapOf("d" to -1.0, "e" to -9.0), fixed = mapOf("a" to 1.0),
                    given = "y = x² + 2x − 8", hidden = true,
                    tip = "pq-Formel: x = −1 ± √(1 + 8) = −1 ± 3. Scheitel: d = −1, e = −8 − 1 = −9."),
                Level("18-5", 18, 5, "Gestreckt", Kind.PARABOLA, "Gegeben ist y = 2x² − 8x + 6. Erst die 2 ausklammern!",
                    listOf(t(1.0, 0.0), t(3.0, 0.0)), listOf(pd, pe), mapOf("d" to 2.0, "e" to -2.0), fixed = mapOf("a" to 2.0),
                    given = "y = 2x² − 8x + 6", tip = "y = 2·(x² − 4x + 3). Innen: d = 2, 3 − 4 = −1. Mal 2: e = −2."),
                Level("18-6", 18, 6, "Kopfüber", Kind.PARABOLA, "Gegeben ist y = −x² + 6x − 5. Die Parabel ist nach unten geöffnet.",
                    listOf(t(1.0, 0.0), t(5.0, 0.0)), listOf(pd, pe), mapOf("d" to 3.0, "e" to 4.0), fixed = mapOf("a" to -1.0),
                    given = "y = −x² + 6x − 5", tip = "y = −(x² − 6x + 5). Innen: d = 3, 5 − 9 = −4. Mal −1: e = 4."),
            ),
        ),
    )

    /** Reihenfolge nach Klassenstufe (Kapitelnummern bleiben fest – daran hängt der Spielstand) */
    private val order = listOf(1, 15, 16, 29, 8, 13, 11, 2, 23, 7, 9, 3, 18, 12, 17, 4, 24, 14, 5, 25, 27, 26, 10, 6, 19, 20, 22, 21, 28)
    val chapters: List<Chapter> = (baseChapters + extraChapters + newChapters + Upper.chapters + More.chapters).sortedBy { order.indexOf(it.number) }

    val all: List<Level> = chapters.flatMap { it.levels }
    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun next(id: String): Level? = all.indexOfFirst { it.id == id }.let { all.getOrNull(it + 1) }
}
