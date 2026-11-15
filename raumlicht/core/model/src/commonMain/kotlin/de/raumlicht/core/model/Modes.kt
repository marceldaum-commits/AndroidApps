package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt
import kotlin.random.Random

// ───────────────────────── Abi-Simulation ─────────────────────────

/** Teilaufgabe einer Abi-Simulation mit Bewertungseinheiten (BE). */
data class ExamPart(val label: String, val points: Int, val level: Level)

/** Eine zusammenhängende Aufgabe im Abitur-Stil: mehrere Teilaufgaben an einem Objekt. */
data class Exam(val id: String, val title: String, val emoji: String, val intro: String, val parts: List<ExamPart>) {
    val maxPoints get() = parts.sumOf { it.points }
    companion object {
        /** Volle BE beim ersten Versuch, die Hälfte beim zweiten, sonst 1 BE. [stars] wie im Level (3/2/1). */
        fun points(part: ExamPart, stars: Int) = when (stars) { 3 -> part.points; 2 -> (part.points + 1) / 2; else -> 1 }
        /** Zeit im freiwilligen Zeit-Modus: 45 Sekunden je Bewertungseinheit */
        fun seconds(exam: Exam) = exam.maxPoints * 45
        fun clock(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        fun verdict(got: Int, max: Int) = when {
            got * 100 >= max * 85 -> "Stark! Das wäre im Abi eine sehr gute Aufgabe."
            got * 100 >= max * 60 -> "Solide. Schau dir die Teile an, die zwei Anläufe brauchten."
            else -> "Dranbleiben: Die Rechenwege zeigen dir, wo es hakt."
        }
    }
}

object Exams {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3) = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + t · "), col(d))
    private fun loop(vararg pts: V3) = pts.indices.map { pts[it] to pts[(it + 1) % pts.size] }

    private val tent = run {
        val base = arrayOf(p(1, 1, 0), p(5, 1, 0), p(5, 5, 0), p(1, 5, 0)); val s = p(3, 3, 3)
        val body = { listOf(Stage.edges("zelt", loop(*base) + base.map { it to s }, "yellow", faces = base.indices.map { listOf(base[it], base[(it + 1) % 4], s) })) }
        Exam("zelt", "Festivalzelt", "🎪", "Ein Zelt hat die Form einer quadratischen Pyramide mit A(1|1|0), B(5|1|0), C(5|5|0), D(1|5|0). Es ist 3 hoch.", listOf(
            ExamPart("a", 2, Families.point("abi-zelt-a", 0, "Spitze", "Die Spitze S liegt senkrecht über dem Mittelpunkt der Grundfläche, in Höhe 3. Gib S an.", s, hidden = true, name = "S",
                given = listOf(listOf(Txt("A(1|1|0)  B(5|1|0)  C(5|5|0)  D(1|5|0)"))),
                tip = "Mittelpunkt der Diagonale AC, dann 3 nach oben.", way = listOf("Mittelpunkt von AC: (3|3|0)", "S(3|3|3)"),
                extras = { listOf(Stage.edges("grund", loop(*base), "cyan")) })),
            ExamPart("b", 4, Families.plane("abi-zelt-b", 0, "Seitenfläche", "Bestimme eine Koordinatengleichung der Ebene, in der die Seitenfläche ABS liegt.",
                listOf("A" to base[0], "B" to base[1], "S" to s), p(0, 3, -2), 3,
                tip = "n⃗ = AB⃗ × AS⃗ mit AB⃗ = (4|0|0), AS⃗ = (2|2|3). Danach A einsetzen.",
                way = listOf("AB⃗ × AS⃗ = (0|−12|8), gekürzt n⃗ = (0|3|−2)", "A einsetzen: 3 · 1 − 0 = 3", "E: 3x₂ − 2x₃ = 3"), extras = { body() })),
            ExamPart("c", 3, Families.scalar("abi-zelt-c", 0, "Volumen", "Berechne das Volumen des Zelts.", "V", 0..12, 8, 0,
                tip = "V = ⅓ · G · h mit G = 4 · 4 und h = 3.", way = listOf("G = 16, h = 3", "V = ⅓ · 16 · 3 = 16"), color = "yellow", step = 2,
                formula = { a -> listOf(listOf(Txt("V = ⅓ · G · h = "), Num(Geo.num(a * 2), "a"))) },
                objects = { _, _ -> body() }, check = { a -> Families.compare(a * 2, 16, "Das Volumen", "16 Volumeneinheiten.") })),
            ExamPart("d", 3, Families.pierce("abi-zelt-d", 0, "Schatten", "Sonnenlicht fällt in Richtung v⃗ = (1|1|−1). Wo landet der Schatten der Spitze S auf dem Boden?",
                s, p(1, 1, -1), p(0, 0, 1), 0, planeText = "x₃ = 0 (Boden)",
                tip = "Gerade durch S mit Richtung v⃗, dritte Zeile null setzen: 3 − t = 0.", way = listOf("g: x⃗ = (3|3|3) + t · (1|1|−1)", "x₃ = 0 → t = 3", "S′(6|6|0)"), extras = body())),
        ))
    }

    private val screen = run {
        val n = p(1, 2, 2); val a = p(1, 2, 2); val e = { listOf(Stage.plane("E", n, 18.0, "violet", "E")) }
        Exam("leinwand", "Laser & Leinwand", "🔦", "Eine Leinwand liegt in der Ebene E: x₁ + 2x₂ + 2x₃ = 18. Ein Laser steht in A(1|2|2) und strahlt in Richtung (1|2|2).", listOf(
            ExamPart("a", 3, Families.pierce("abi-leinwand-a", 0, "Durchstoßpunkt", "In welchem Punkt trifft der Laser die Leinwand?", a, n, n, 18,
                tip = "Gerade in E einsetzen: (1 + t) + 2(2 + 2t) + 2(2 + 2t) = 18.", way = listOf("9 + 9t = 18 → t = 1", "S(2|4|4)"))),
            ExamPart("b", 2, Families.scalar("abi-leinwand-b", 0, "Auftreffwinkel", "Unter welchem Winkel trifft der Laser auf die Leinwand?", "α", 0..12, 6, 0,
                tip = "Vergleiche die Richtung des Lasers mit dem Normalenvektor der Ebene.", way = listOf("d⃗ = (1|2|2) = n⃗", "Der Laser läuft entlang der Normalen", "α = 90°"), color = "yellow", step = 15, suffix = "°",
                formula = { v -> listOf(lineText("g", a, n), listOf(Txt("E: x₁ + 2x₂ + 2x₃ = 18    α = "), Num("${v * 15}°", "a"))) },
                objects = { _, _ -> e() + listOf(Stage.point("A", a, "cyan", "A"), Stage.line("g", a, n, 0.0, 1.0, "cyan", "g")) },
                check = { v -> Families.compare(v * 15, 90, "Der Winkel", "90° – der Laser trifft senkrecht auf.") })),
            ExamPart("c", 3, Families.scalar("abi-leinwand-c", 0, "Abstand", "Wie weit ist der Laser A von der Leinwand entfernt?", "d", 0..8, 3, 1,
                tip = "d = |n⃗ · a⃗ − 18| : |n⃗| mit |n⃗| = 3.", way = listOf("n⃗ · a⃗ = 1 + 4 + 4 = 9", "d = |9 − 18| : 3 = 3"),
                formula = { v -> listOf(listOf(Txt("A(1|2|2)   E: x₁ + 2x₂ + 2x₃ = 18")), listOf(Txt("d(A, E) = "), Num(Geo.num(v), "a"))) },
                objects = { _, _ -> e() + listOf(Stage.point("A", a, "cyan", "A"), Stage.stub("richtung", a, n, "pink", length = 0.9)) },
                check = { v -> Families.compare(v, 3, "Der Abstand", "3 Einheiten.") { ok -> listOf(Beam(a, a + n * (v / 3.0), "pink", ok)) } })),
            ExamPart("d", 4, Families.point("abi-leinwand-d", 0, "Spiegelbild", "Die Leinwand spiegelt. Wo erscheint das Spiegelbild A′ des Lasers?", p(3, 6, 6), hidden = true, name = "A′",
                given = listOf(listOf(Txt("A(1|2|2)   E: x₁ + 2x₂ + 2x₃ = 18"))),
                tip = "Lotfußpunkt ist der Durchstoßpunkt aus a). Denselben Weg noch einmal gehen.", way = listOf("F = S(2|4|4)", "A′ = A + 2 · AF⃗ = (1|2|2) + 2 · (1|2|2)", "A′(3|6|6)"),
                extras = { e() + listOf(Stage.point("A", a, "cyan", "A(1|2|2)")) })),
        ))
    }

    private val flight = run {
        val ga = p(0, 2, 4); val gv = p(2, 0, -1); val hb = p(6, 2, 1); val hw = p(0, 1, 1)
        val lines = { listOf(Stage.fullLine("g", ga, gv, Bounds(), "cyan", "g"), Stage.fullLine("h", hb, hw, Bounds(), "yellow", "h")) }
        Exam("drohnen", "Drohnen-Show", "🛸", "Zwei Drohnen starten gleichzeitig (t in Sekunden). Drohne 1 fliegt auf g: x⃗ = (0|2|4) + t · (2|0|−1), Drohne 2 auf h: x⃗ = (6|2|1) + t · (0|1|1).", listOf(
            ExamPart("a", 2, Families.point("abi-drohnen-a", 0, "Ort", "Wo befindet sich Drohne 1 nach 2 Sekunden?", p(4, 2, 2), hidden = true, home = ga,
                given = listOf(lineText("g", ga, gv)), tip = "t = 2 einsetzen.", way = listOf("x⃗(2) = (0|2|4) + 2 · (2|0|−1)", "P(4|2|2)"),
                extras = { listOf(Stage.fullLine("g", ga, gv, Bounds(), "cyan", "g")) })),
            ExamPart("b", 2, Families.param("abi-drohnen-b", 0, "Landung", "Nach wie vielen Sekunden erreicht Drohne 1 den Boden?", ga, gv, 4, 0..6, hidden = true,
                tip = "Am Boden ist x₃ = 0: 4 − t = 0.", way = listOf("x₃(t) = 4 − t = 0", "t = 4 → Landepunkt (8|2|0)"))),
            ExamPart("c", 4, Families.scalar("abi-drohnen-c", 0, "Engste Stelle", "Zu welchem Zeitpunkt kommen sich die beiden Drohnen am nächsten?", "t", 0..4, 2, 0,
                tip = "Abstandsvektor d⃗(t) = (6|0|−3) + t · (−2|1|2); minimal, wenn d⃗(t) · (−2|1|2) = 0.", way = listOf("d⃗(t) · (−2|1|2) = −18 + 9t", "= 0 → t = 2"), color = "yellow",
                formula = { v -> listOf(lineText("g", ga, gv), lineText("h", hb, hw), listOf(Txt("t = "), Num(Geo.num(v), "a"))) },
                objects = { v, _ -> lines() + listOf(Stage.point("d1", ga + gv * v, "cyan", "D₁", "target"), Stage.point("d2", hb + hw * v, "yellow", "D₂", "target")) },
                check = { v -> if (v == 2) Outcome(true, "Bei t = 2 – richtig.") else Outcome(false, "Bei t = $v beträgt der Abstand ${Geo.num(((ga + gv * v) - (hb + hw * v)).length)}. Es geht noch näher.") })),
            ExamPart("d", 4, Families.scalar("abi-drohnen-d", 0, "Mindestabstand", "Wie groß ist der Abstand der Drohnen in diesem Moment? (Vorgeschrieben sind mindestens 2 Einheiten.)", "d", 0..8, 3, 1,
                tip = "Orte bei t = 2: (4|2|2) und (6|4|3). Länge des Verbindungsvektors.", way = listOf("d⃗(2) = (2|2|1)", "|d⃗| = √(4 + 4 + 1) = 3", "3 ≥ 2: Sicherheitsabstand eingehalten"),
                formula = { v -> listOf(listOf(Txt("D₁(4|2|2)   D₂(6|4|3)")), listOf(Txt("d = "), Num(Geo.num(v), "a"))) },
                objects = { _, _ -> lines() + listOf(Stage.point("d1", p(4, 2, 2), "cyan", "D₁", "target"), Stage.point("d2", p(6, 4, 3), "yellow", "D₂", "target")) },
                check = { v -> Families.compare(v, 3, "Der Abstand", "3 Einheiten – Sicherheitsabstand eingehalten.") { ok -> listOf(Beam(p(4, 2, 2), p(4, 2, 2) + p(2, 2, 1) * (v / 3.0), "pink", ok)) } })),
        ))
    }

    val all = listOf(tent, screen, flight) + MoreExams.all
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

// ───────────────────────── Lage-Check ─────────────────────────

/** Zwei Geraden – wie liegen sie zueinander? Erst entscheiden, dann in 3D sehen. */
data class LageRound(val a: V3, val d1: V3, val b: V3, val d2: V3, val relation: Relation) {
    val formula: List<List<Part>> get() = listOf(
        listOf(Txt("g: x⃗ = "), col(a), Txt(" + s · "), col(d1)), listOf(Txt("h: x⃗ = "), col(b), Txt(" + t · "), col(d2)))
    val explanation: String get() = when (relation) {
        Relation.PARALLEL -> "Die Richtungsvektoren sind Vielfache voneinander, aber der Stützpunkt von h liegt nicht auf g: echt parallel."
        Relation.IDENTICAL -> "Die Richtungsvektoren sind Vielfache und der Stützpunkt von h liegt auf g: dieselbe Gerade."
        Relation.INTERSECT -> "Keine Vielfachen – und das Gleichungssystem hat eine Lösung: Schnittpunkt ${Geo.lineLine(a, d1, b, d2).point!!.fmt()}."
        Relation.SKEW -> "Keine Vielfachen – und das Gleichungssystem führt auf einen Widerspruch: windschief (Abstand ${Geo.num(Geo.lineLine(a, d1, b, d2).distance)})."
    }
    fun scene(revealed: Boolean): String = Stage.encode(Bounds(), listOf(Stage.drone(V3(8, 0, 1), if (revealed) "happy" else "idle")) +
        if (!revealed) emptyList() else listOf(Stage.fullLine("g", a, d1, Bounds(), "cyan", "g"), Stage.fullLine("h", b, d2, Bounds(), "yellow", "h")) +
            (Geo.lineLine(a, d1, b, d2).point?.let { listOf(Stage.point("S", it, "pink", "S", "target")) } ?: emptyList()))
}

object LageCheck {
    const val ROUNDS = 8
    val names = mapOf(Relation.PARALLEL to "parallel", Relation.IDENTICAL to "identisch", Relation.INTERSECT to "schneiden sich", Relation.SKEW to "windschief")
    private fun inRoom(v: V3) = v.x in 0.0..8.0 && v.y in 0.0..8.0 && v.z in 0.0..6.0
    private fun Random.pt() = V3(nextInt(0, 9), nextInt(0, 9), nextInt(0, 7))
    private fun Random.dir(): V3 { while (true) { val d = V3(nextInt(-2, 3), nextInt(-2, 3), nextInt(-2, 3)); if (d.ints.count { it != 0 } >= 2) return d } }

    fun round(seed: Long): LageRound {
        val r = Random(seed)
        val want = Relation.entries[r.nextInt(4)]
        repeat(20_000) {
            val d1 = r.dir(); val a = r.pt()
            val c = when (want) {
                Relation.PARALLEL, Relation.IDENTICAL -> {
                    val d2 = d1 * listOf(-2, -1, 2).random(r)
                    val b = if (want == Relation.IDENTICAL) a + d1 * listOf(-2, -1, 1, 2).random(r) else r.pt()
                    LageRound(a, d1, b, d2, want)
                }
                Relation.INTERSECT -> { val s = r.pt(); val d2 = r.dir(); LageRound(s - d1 * r.nextInt(1, 4), d1, s - d2 * r.nextInt(1, 4), d2, want) }
                Relation.SKEW -> LageRound(a, d1, r.pt(), r.dir(), want)
            }
            if (inRoom(c.a) && inRoom(c.b) && Geo.lineLine(c.a, c.d1, c.b, c.d2).relation == want && !c.a.near(c.b)) return c
        }
        error("Keine Runde für $seed")
    }
}

// ───────────────────────── Duell ─────────────────────────

/** Duell: Ein kurzer Code steht für dieselben fünf Aufgaben – ohne Server, zum Verschicken per Messenger. */
data class Duel(val topic: JamTopic, val seed: Long) {
    fun level(n: Int) = Gen.jam(topic, seed, n)
    val code: String get() { val body = topic.ordinal.toString(36) + seed.toString(36).padStart(4, '0'); return "RL-${body.uppercase()}${DuelCode.check(body)}" }
}

object DuelCode {
    const val TASKS = 5
    private const val ALPHA = "0123456789abcdefghijklmnopqrstuvwxyz"
    internal fun check(body: String) = ALPHA[body.lowercase().fold(7) { h, c -> (h * 31 + ALPHA.indexOf(c)) % 36 }].uppercaseChar()
    fun create(topic: JamTopic, random: Random = Random.Default) = Duel(topic, random.nextLong(36L * 36 * 36 * 36))
    /** Findet einen Duell-Code irgendwo im Text (z. B. in einer weitergeleiteten Nachricht). Tippfehler erkennt die Prüfziffer. */
    fun find(text: String): Duel? {
        val m = Regex("RL-([0-9A-Za-z]{5})([0-9A-Za-z])", RegexOption.IGNORE_CASE).find(text) ?: return null
        val body = m.groupValues[1].lowercase()
        if (check(body) != m.groupValues[2].uppercase()[0]) return null
        val topic = JamTopic.entries.getOrNull(ALPHA.indexOf(body[0])) ?: return null
        return Duel(topic, body.substring(1).toLong(36))
    }
    fun shareText(d: Duel, firstTry: Int) = "Raumlicht-Duell ${d.topic.emoji} ${d.topic.title}: Ich habe $firstTry von $TASKS beim ersten Versuch getroffen. Schaffst du mehr? Code: ${d.code}"
}

// ───────────────────────── Show-Baukasten ─────────────────────────

enum class ShowType(val title: String, val emoji: String, val labels: List<String>, val min: List<Int>, val max: List<Int>, val start: List<Int>) {
    LASER("Laser", "🔦", listOf("p₁", "p₂", "p₃", "d₁", "d₂", "d₃"), listOf(0, 0, 0, -4, -4, -4), listOf(8, 8, 6, 4, 4, 4), listOf(0, 0, 0, 2, 2, 1)),
    PLANE("Scheibe", "🔷", listOf("n₁", "n₂", "n₃", "c"), listOf(-3, -3, -3, -10), listOf(3, 3, 3, 25), listOf(0, 0, 1, 4)),
    BALL("Kugel", "🔮", listOf("m₁", "m₂", "m₃", "r"), listOf(0, 0, 0, 1), listOf(8, 8, 6, 4), listOf(4, 4, 3, 2)),
    DOT("Punkt", "📍", listOf("x₁", "x₂", "x₃"), listOf(0, 0, 0), listOf(8, 8, 6), listOf(2, 2, 2)),
}

data class ShowItem(val type: ShowType, val v: List<Int>) {
    private val colors = listOf("cyan", "pink", "yellow", "green", "orange", "violet")
    val text: String get() = when (type) {
        ShowType.LASER -> "x⃗ = (${v[0]}|${v[1]}|${v[2]}) + t · (${Geo.num(v[3])}|${Geo.num(v[4])}|${Geo.num(v[5])})"
        ShowType.PLANE -> if (v.take(3).all { it == 0 }) "n⃗ = (0|0|0) – keine Ebene" else Geo.planeText(V3(v[0], v[1], v[2]), v[3])
        ShowType.BALL -> "M(${v[0]}|${v[1]}|${v[2]}), r = ${v[3]}"
        ShowType.DOT -> "(${v[0]}|${v[1]}|${v[2]})"
    }
    fun set(i: Int, x: Int) = copy(v = v.toMutableList().also { it[i] = x.coerceIn(type.min[i], type.max[i]) })
    fun obj(i: Int): Obj? = when (type) {
        ShowType.LASER -> V3(v[3], v[4], v[5]).takeIf { !it.isZero }?.let { d -> val a = V3(v[0], v[1], v[2]); Stage.line("s$i", a, d, 0.0, Geo.exit(a, d, Bounds().outerLo, Bounds().outerHi), colors[i % colors.size]) }
        ShowType.PLANE -> V3(v[0], v[1], v[2]).takeIf { !it.isZero }?.let { Stage.plane("s$i", it, v[3].toDouble(), colors[(i + 5) % colors.size]) }
        ShowType.BALL -> Stage.sphere("s$i", V3(v[0], v[1], v[2]), v[3].toDouble(), colors[i % colors.size])
        ShowType.DOT -> Stage.point("s$i", V3(v[0], v[1], v[2]), colors[i % colors.size], "P${i + 1}")
    }
    /** Strahl für „Show starten“ (nur Laser) */
    fun beam(i: Int): Beam? = if (type != ShowType.LASER) null else V3(v[3], v[4], v[5]).takeIf { !it.isZero }?.let { d ->
        val a = V3(v[0], v[1], v[2]); Beam(a, a + d * Geo.exit(a, d, Bounds().outerLo, Bounds().outerHi), colors[i % colors.size], true) }
}

/** Eigene Lichtshow aus Lasern, Scheiben und Kugeln – gebaut mit Formeln, teilbar als Code. */
object Show {
    const val MAX = 8
    private const val ALPHA = "0123456789abcdefghijklmnopqrstuvwxyz"
    val demo = listOf(ShowItem(ShowType.BALL, listOf(4, 4, 3, 2)), ShowItem(ShowType.LASER, listOf(0, 0, 0, 2, 2, 1)), ShowItem(ShowType.LASER, listOf(8, 0, 5, -2, 2, -1)), ShowItem(ShowType.PLANE, listOf(0, 1, 0, 8)))

    /**
     * 3D-Rechner: was sich aus den Bausteinen ausrechnen lässt – Lagebeziehungen, Schnitt- und Durchstoßpunkte, Abstände, Winkel.
     * So lässt sich eine Aufgabe aus dem Schulbuch eingeben und nachprüfen.
     */
    fun facts(items: List<ShowItem>): List<String> {
        val out = ArrayList<String>()
        fun name(i: Int) = "${items[i].type.title} ${i + 1}"
        fun pt(i: Int) = V3(items[i].v[0], items[i].v[1], items[i].v[2])
        fun dir(i: Int) = V3(items[i].v[3], items[i].v[4], items[i].v[5])
        for (i in items.indices) for (j in i + 1 until items.size) {
            val (a, b) = if (items[i].type.ordinal <= items[j].type.ordinal) i to j else j to i
            val ta = items[a].type; val tb = items[b].type; val head = "${name(a)} & ${name(b)}: "
            when {
                ta == ShowType.LASER && tb == ShowType.LASER -> if (!dir(a).isZero && !dir(b).isZero) {
                    val r = Geo.lineLine(pt(a), dir(a), pt(b), dir(b))
                    out += head + when (r.relation) {
                        Relation.INTERSECT -> "schneiden sich in ${r.point!!.fmt()}, Winkel ${Geo.num(Geo.angle(dir(a), dir(b)).let { minOf(it, 180 - it) })}°"
                        Relation.SKEW -> "windschief, Abstand ${Geo.num(r.distance)}"
                        Relation.PARALLEL -> "parallel, Abstand ${Geo.num(r.distance)}"
                        Relation.IDENTICAL -> "identisch"
                    }
                }
                ta == ShowType.LASER && tb == ShowType.PLANE -> if (!dir(a).isZero && !pt(b).isZero) {
                    val n = pt(b); val c = items[b].v[3].toDouble(); val t = Geo.linePlane(pt(a), dir(a), n, c)
                    out += head + if (t == null) (if (Geo.onPlane(pt(a), n, c)) "Gerade liegt in der Ebene" else "parallel, Abstand ${Geo.num(Geo.distPointPlane(pt(a), n, c))}")
                    else "Durchstoßpunkt ${(pt(a) + dir(a) * t).fmt()}, Winkel ${Geo.num(90 - Geo.angle(dir(a), n).let { minOf(it, 180 - it) })}°"
                }
                ta == ShowType.LASER && tb == ShowType.BALL -> if (!dir(a).isZero) {
                    val d = Geo.distPointLine(pt(b), pt(a), dir(a)); val r = items[b].v[3]
                    out += head + when { d < r - Geo.EPS -> "zwei Treffer (Abstand zum Mittelpunkt ${Geo.num(d)})"; d < r + Geo.EPS -> "Tangente (berührt)"; else -> "kein Treffer (Abstand zum Mittelpunkt ${Geo.num(d)})" }
                }
                ta == ShowType.LASER && tb == ShowType.DOT -> if (!dir(a).isZero) out += head + (if (Geo.onLine(pt(b), pt(a), dir(a))) "Punkt liegt auf der Geraden" else "Abstand ${Geo.num(Geo.distPointLine(pt(b), pt(a), dir(a)))}")
                ta == ShowType.PLANE && tb == ShowType.PLANE -> if (!pt(a).isZero && !pt(b).isZero) out += head +
                    (if (Geo.parallel(pt(a), pt(b))) "parallel" else "schneiden sich, Winkel ${Geo.num(Geo.angle(pt(a), pt(b)).let { minOf(it, 180 - it) })}°, Schnittgerade in Richtung ${(pt(a) cross pt(b)).fmt()}")
                ta == ShowType.PLANE && tb == ShowType.BALL -> if (!pt(a).isZero) {
                    val d = Geo.distPointPlane(pt(b), pt(a), items[a].v[3].toDouble()); val r = items[b].v[3]
                    out += head + when { d < r - Geo.EPS -> "Schnittkreis mit Radius ${Geo.num(kotlin.math.sqrt(r * r - d * d))}"; d < r + Geo.EPS -> "Tangentialebene (berührt)"; else -> "kein Schnitt (Abstand ${Geo.num(d)})" }
                }
                ta == ShowType.PLANE && tb == ShowType.DOT -> if (!pt(a).isZero) {
                    val n = pt(a); val c = items[a].v[3].toDouble()
                    out += head + (if (Geo.onPlane(pt(b), n, c)) "Punkt liegt in der Ebene" else "Abstand ${Geo.num(Geo.distPointPlane(pt(b), n, c))}, Lotfußpunkt ${Geo.footOnPlane(pt(b), n, c).fmt()}")
                }
                ta == ShowType.BALL && tb == ShowType.DOT -> { val d = (pt(a) - pt(b)).length; val r = items[a].v[3]
                    out += head + when { d < r - Geo.EPS -> "Punkt liegt in der Kugel"; d < r + Geo.EPS -> "Punkt liegt auf der Kugel"; else -> "Punkt liegt außerhalb (Abstand zum Mittelpunkt ${Geo.num(d)})" } }
                ta == ShowType.DOT && tb == ShowType.DOT -> out += head + "Abstand ${Geo.num((pt(a) - pt(b)).length)}, Mittelpunkt ${((pt(a) + pt(b)) * 0.5).fmt()}, Verbindungsvektor ${(pt(b) - pt(a)).fmt()}"
                else -> {}
            }
        }
        return out
    }

    fun scene(items: List<ShowItem>): String = Stage.encode(Bounds(), listOf(Stage.drone(V3(8, 0, 1))) + items.mapIndexedNotNull { i, it -> it.obj(i) })

    /** „RLS-“ + je Baustein ein Buchstabe für die Art und ein Zeichen je Zahl (Wert + 10, Basis 36) */
    fun encode(items: List<ShowItem>) = "RLS-" + items.joinToString("") { it.type.name[0] + it.v.joinToString("") { x -> ALPHA[x + 10].toString() } }
    fun decode(text: String): List<ShowItem>? {
        val body = Regex("RLS-([0-9A-Za-z]+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1) ?: return null
        val out = ArrayList<ShowItem>(); var i = 0
        while (i < body.length) {
            val type = ShowType.entries.firstOrNull { it.name[0] == body[i].uppercaseChar() } ?: return null
            val n = type.labels.size; if (i + 1 + n > body.length) return null
            val v = body.substring(i + 1, i + 1 + n).map { ALPHA.indexOf(it.lowercaseChar()) - 10 }
            if (v.indices.any { v[it] < type.min[it] || v[it] > type.max[it] }) return null
            out += ShowItem(type, v); i += n + 1
        }
        return out.takeIf { it.isNotEmpty() && it.size <= MAX }
    }
}

// ───────────────────────── Lux-Werkstatt ─────────────────────────

/** Farbe für Lux – freigeschaltet durch Sterne auf der Tour. */
data class Skin(val color: String, val name: String, val stars: Int)

object Skins {
    val all = listOf(Skin("pink", "Neon-Pink", 0), Skin("cyan", "Eisblau", 12), Skin("green", "Laser-Grün", 30), Skin("yellow", "Gold", 60), Skin("orange", "Glut", 100), Skin("violet", "Ultraviolett", 150))
    fun unlocked(tourStars: Int) = all.filter { tourStars >= it.stars }
}

// ───────────────────────── Schwachstellen-Training ─────────────────────────

object Training {
    /** Aufgaben, die schon gelöst sind, aber nicht beim ersten Versuch – die lohnen eine zweite Runde. */
    fun levels(stars: Map<String, Int>): List<Level> = Levels.all.filter { (stars[it.id] ?: 0) in 1..2 }
}
