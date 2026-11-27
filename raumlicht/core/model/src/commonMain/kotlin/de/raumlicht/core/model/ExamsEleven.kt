package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Txt

/** Drei weitere Abi-Prüfungen (seit 2.18.43): Pyramide, Spiegelsaal, Schiffe. [ExamsElevenTest] rechnet jede Lösung unabhängig nach. */
internal object ExamsEleven {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun number(id: String, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int, tip: String, way: List<String>,
                       given: List<List<Part>>, objects: List<Obj>, ok: String, mistakes: Map<Int, String> = emptyMap(), what: String = "Der Wert", step: Int = 1, suffix: String = "") =
        ExamsTen.number(id, title, mission, name, range, solution, start, tip, way, given, objects, ok, mistakes, step = step, suffix = suffix, what = what)

    // ───────────── Pyramide ─────────────
    val pyA = p(0, 0, 0); val pyB = p(6, 0, 0); val pyC = p(6, 6, 0); val pyD = p(0, 6, 0); val pyS = p(3, 3, 4)
    val pyN = p(0, 4, -3); const val PY_C = 0; const val PY_V = 48
    val pyQ = p(0, 5, 0)    // Abstand zur Seitenfläche ABS: |4 · 5| : 5 = 4

    private val pyramid = run {
        val base = arrayOf(pyA, pyB, pyC, pyD)
        val body = { listOf(Stage.edges("pyramide", base.indices.map { base[it] to base[(it + 1) % 4] } + base.map { it to pyS }, "yellow", faces = base.indices.map { listOf(base[it], base[(it + 1) % 4], pyS) })) }
        val plane = { listOf(Stage.plane("E", pyN, PY_C.toDouble(), "violet", "ABS")) }
        Exam("pyramide", "Glaspyramide", "🔺", "Eine Glaspyramide hat die quadratische Grundfläche ABCD mit A(0|0|0), B(6|0|0), C(6|6|0), D(0|6|0). Die Spitze S liegt 4 Meter senkrecht über dem Mittelpunkt der Grundfläche.", listOf(
            ExamPart("a", 3, Families.point("abi-pyramide-a", 0, "Spitze", "Wo liegt die Spitze S?", pyS, hidden = true, name = "S",
                given = listOf(listOf(Txt("A(0|0|0)   C(6|6|0)   Höhe 4"))), tip = "Mittelpunkt M der Grundfläche = Mitte von A und C. Dann 4 nach oben.",
                way = listOf("M = ½ · (A + C) = (3|3|0)", "S liegt 4 Meter senkrecht darüber: x₃ = 4", "S(3|3|4)"),
                extras = { listOf(Stage.edges("grund", base.indices.map { base[it] to base[(it + 1) % 4] }, "yellow")) })),   // nur die Grundfläche: Die Spitze soll gerechnet werden
            ExamPart("b", 4, Families.plane("abi-pyramide-b", 0, "Seitenfläche", "Bestimme eine Koordinatengleichung der Ebene, in der die Seitenfläche ABS liegt.",
                listOf("A" to pyA, "B" to pyB, "S" to pyS), pyN, PY_C,
                tip = "n⃗ = AB⃗ × AS⃗ mit AB⃗ = (6|0|0) und AS⃗ = (3|3|4). Dann c = n⃗ · A.",
                way = listOf("AB⃗ × AS⃗ = (0·4 − 0·3 | 0·3 − 6·4 | 6·3 − 0·3) = (0|−24|18) = −6 · (0|4|−3)", "E: 4x₂ − 3x₃ = c; A einsetzen: c = 0", "Probe S: 4 · 3 − 3 · 4 = 0 ✓"), extras = { body() })),
            ExamPart("c", 3, number("abi-pyramide-c", "Volumen", "Wie groß ist das Volumen der Pyramide?", "V", 0..60, PY_V, 0,
                tip = "V = ⅓ · Grundfläche · Höhe.", way = listOf("Grundfläche G = 6 · 6 = 36", "Höhe h = 4", "V = ⅓ · 36 · 4 = 48"),
                given = listOf(listOf(Txt("Grundfläche 6 · 6, Höhe 4"))), objects = body(), ok = "48 Kubikmeter.", what = "Das Volumen", mistakes = mapOf(144 to "144 ist G · h. Das Drittel fehlt noch."))),
            ExamPart("d", 2, number("abi-pyramide-d", "Abstand", "Ein Strahler steht in Q(0|5|0). Wie weit ist er von der Seitenfläche ABS (4x₂ − 3x₃ = 0) entfernt?", "d", 0..8, 4, 0,
                tip = "d = |n⃗ · Q − c| : |n⃗| mit |n⃗| = √(16 + 9) = 5.", way = listOf("n⃗ · Q = 4 · 5 = 20, |n⃗| = 5", "d = |20 − 0| : 5 = 4"),
                given = listOf(listOf(Txt("Q(0|5|0)   E: 4x₂ − 3x₃ = 0"))), objects = body() + plane() + Stage.point("Q", pyQ, "cyan", "Q"), ok = "4 Meter.", what = "Der Abstand", mistakes = mapOf(20 to "20 ist der Zähler. Teile noch durch |n⃗| = 5."))),
        ))
    }

    // ───────────── Spiegelsaal ─────────────
    val mN = p(2, 1, 2); const val M_C = 12
    val mL = p(1, 1, 0); val mF = p(3, 2, 2); val mL2 = p(5, 3, 4); val mZ = p(5, 0, 0)

    private val mirror = run {
        val e = { listOf(Stage.plane("E", mN, M_C.toDouble(), "violet", "Spiegel")) }
        Exam("spiegelsaal", "Spiegelsaal", "🪞", "Ein ebener Spiegel liegt in der Ebene E: 2x₁ + x₂ + 2x₃ = 12. Eine Lampe L(1|1|0) leuchtet.", listOf(
            ExamPart("a", 3, Families.point("abi-spiegelsaal-a", 0, "Lotfußpunkt", "Wo trifft das Lot von L auf den Spiegel (Lotfußpunkt F)?", mF, hidden = true, name = "F",
                given = listOf(listOf(Txt("L(1|1|0)   E: 2x₁ + x₂ + 2x₃ = 12"))), tip = "Lot: x⃗ = L + t · n⃗ mit n⃗ = (2|1|2). In E einsetzen und t bestimmen.",
                way = listOf("(1 + 2t, 1 + t, 2t) in E: 2 + 4t + 1 + t + 4t = 12 → 3 + 9t = 12", "t = 1", "F(3|2|2)"), extras = { e() + Stage.point("L", mL, "cyan", "L") })),
            ExamPart("b", 3, number("abi-spiegelsaal-b", "Abstand", "Wie weit ist die Lampe vom Spiegel entfernt?", "d", 0..8, 3, 0,
                tip = "|LF⃗| oder d = |n⃗ · L − c| : |n⃗|.", way = listOf("n⃗ · L = 2 + 1 + 0 = 3, |n⃗| = 3", "d = |3 − 12| : 3 = 3"),
                given = listOf(listOf(Txt("L(1|1|0)   E: 2x₁ + x₂ + 2x₃ = 12"))), objects = e() + Stage.point("L", mL, "cyan", "L"), ok = "3 Einheiten.", what = "Der Abstand")),
            ExamPart("c", 3, Families.point("abi-spiegelsaal-c", 0, "Spiegelbild", "Wo erscheint das Spiegelbild L′ der Lampe?", mL2, hidden = true, name = "L′",
                given = listOf(listOf(Txt("L(1|1|0)   F(3|2|2)"))), tip = "L′ = 2 · F − L.", way = listOf("L′ = 2 · (3|2|2) − (1|1|0)", "= (6 − 1 | 4 − 1 | 4 − 0)", "L′(5|3|4)"),
                extras = { e() + listOf(Stage.point("L", mL, "cyan", "L"), Stage.point("F", mF, "white", "F")) })),
            ExamPart("d", 3, number("abi-spiegelsaal-d", "Weg des Lichts", "Das Licht geht von L über den Spiegel zum Ziel Z(5|0|0). Wie lang ist der Weg? (Tipp: Er ist so lang wie die Strecke von L′ nach Z.)", "s", 0..9, 5, 0,
                tip = "Der Weg über den Spiegel ist so lang wie die gerade Strecke L′Z.", way = listOf("Z − L′ = (5|0|0) − (5|3|4) = (0|−3|−4)", "|Z − L′| = √(0 + 9 + 16) = 5"),
                given = listOf(listOf(Txt("L′(5|3|4)   Z(5|0|0)"))), objects = e() + listOf(Stage.point("L", mL, "cyan", "L"), Stage.point("Z", mZ, "yellow", "Z")), ok = "5 Einheiten.", what = "Der Weg", mistakes = mapOf(3 to "3 ist der Weg von L zum Spiegel. Gesucht ist der ganze Weg bis Z."))),
        ))
    }

    // ───────────── Schiffe ─────────────
    val sa = p(0, 2, 0); val sad = p(1, 1, 0); val sb = p(6, 0, 0); val sbd = p(-1, 1, 0)
    val sPos3 = p(3, 5, 0); val sCross = p(2, 4, 0); const val S_TIME = 3; const val S_DIST = 2

    private val ships = run {
        val lines = { listOf(Stage.fullLine("g", sa, sad, Bounds(), "cyan", "Schiff 1"), Stage.fullLine("h", sb, sbd, Bounds(), "yellow", "Schiff 2")) }
        Exam("schiffe", "Zwei Schiffe", "🚢", "Zwei Schiffe fahren gleichzeitig los (t in Minuten, Angaben in Kilometern). Schiff 1: x⃗ = (0|2|0) + t · (1|1|0), Schiff 2: x⃗ = (6|0|0) + t · (−1|1|0).", listOf(
            ExamPart("a", 2, Families.point("abi-schiffe-a", 0, "Ort", "Wo ist Schiff 1 nach 3 Minuten?", sPos3, hidden = true, home = sa,
                given = listOf(lineText("g", sa, sad)), tip = "t = 3 einsetzen.", way = listOf("x⃗(3) = (0|2|0) + 3 · (1|1|0)", "P(3|5|0)"), extras = { lines() })),
            ExamPart("b", 3, Families.crossing("abi-schiffe-b", 0, "Kreuzung der Kurse", "In welchem Punkt kreuzen sich die beiden Kurse?", sa, sad, sb, sbd,
                tip = "Gleichsetzen: s = 6 − u und 2 + s = u (zwei verschiedene Parameter!).", way = listOf("Zeile 1: s = 6 − u; Zeile 2: 2 + s = u", "2 + 6 − u = u → u = 4, s = 2; Zeile 3: 0 = 0 ✓", "S(2|4|0)"), abi = true)),
            ExamPart("c", 4, number("abi-schiffe-c", "Engste Stelle", "Zu welchem Zeitpunkt sind sich die Schiffe am nächsten?", "t", 0..8, S_TIME, 0,
                tip = "Abstandsvektor: (2t − 6|2|0). Seine Länge ist am kleinsten, wenn 2t − 6 = 0.", way = listOf("g(t) − h(t) = (t − (6 − t) | (2 + t) − t | 0) = (2t − 6|2|0)", "Länge² = (2t − 6)² + 4, am kleinsten für 2t − 6 = 0", "t = 3"),
                given = listOf(lineText("g", sa, sad), lineText("h", sb, sbd)), objects = lines() + listOf(Stage.point("s1", sPos3, "cyan", "S₁", "target")), ok = "Nach 3 Minuten.", what = "Der Zeitpunkt",
                mistakes = mapOf(2 to "Bei t = 2 ist der Abstand √(4 + 4) = √8 ≈ 2,83. Es geht noch näher."))),
            ExamPart("d", 3, number("abi-schiffe-d", "Mindestabstand", "Wie groß ist der Abstand der Schiffe zu diesem Zeitpunkt (in Kilometern)?", "d", 0..8, S_DIST, 0,
                tip = "Abstandsvektor bei t = 3 ausrechnen.", way = listOf("(2 · 3 − 6|2|0) = (0|2|0)", "Länge = √(0 + 4 + 0) = 2"),
                given = listOf(listOf(Txt("Abstandsvektor: (2t − 6|2|0), t = 3"))), objects = lines(), ok = "2 Kilometer.", what = "Der Abstand")),
        ))
    }

    val all = listOf(pyramid, mirror, ships)
}
