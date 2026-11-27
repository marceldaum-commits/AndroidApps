package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/**
 * Vier weitere Abi-Prüfungsaufgaben zur Geometrie im Raum (seit 2.18.40): Dachfläche, Kran, Ballon (Kugel), Rutsche.
 * Jede Teilaufgabe ist von Hand durchgerechnet; [ExamsTenTest] rechnet alles noch einmal unabhängig nach.
 */
internal object ExamsTen {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))

    /** Eine Zahl einstellen (Abstand, Radius, Winkel …) – [check] gibt die Rückmeldung */
    internal fun number(id: String, title: String, mission: String, name: String, range: IntRange, solution: Int, start: Int, tip: String, way: List<String>,
                       given: List<List<Part>>, objects: List<Obj>, ok: String, mistakes: Map<Int, String> = emptyMap(),
                       step: Int = 1, suffix: String = "", color: String = "pink", what: String = "Der Wert", beam: ((Int) -> Beam)? = null) =
        Families.scalar(id, 0, title, mission, name, range, solution, start, tip, way, color, step, suffix,
            formula = { a -> given + listOf(listOf(Txt("$name = "), Num(if (suffix.isEmpty()) Geo.num(a * step) else "${a * step}$suffix", "a"))) },
            objects = { _, _ -> objects },
            check = { a -> Families.compare(a * step, solution * step, what, ok, mistakes) { hit -> beam?.let { listOf(it(a)) } ?: emptyList() } })

    // ───────────── Dachfläche ─────────────
    val roofA = p(6, 0, 3); val roofB = p(0, 3, 3); val roofC = p(0, 0, 6); val roofN = p(1, 2, 2); const val ROOF_C = 12
    val chimney = p(2, 2, 0)          // Abstand 2 zum Dach
    val rainA = p(2, 1, 6); val rainD = p(0, 0, -1); val rainS = p(2, 1, 4)
    val mirrorP = p(1, 1, 0); val mirrorQ = p(3, 5, 4)

    private val roof = run {
        val e = { listOf(Stage.plane("E", roofN, ROOF_C.toDouble(), "violet", "Dach")) }
        Exam("dach", "Dachfläche", "🏠", "Die schräge Dachfläche eines Hauses geht durch die Punkte A(6|0|3), B(0|3|3) und C(0|0|6).", listOf(
            ExamPart("a", 4, Families.plane("abi-dach-a", 0, "Dachebene", "Bestimme eine Koordinatengleichung der Ebene E, in der die Dachfläche liegt.",
                listOf("A" to roofA, "B" to roofB, "C" to roofC), roofN, ROOF_C,
                tip = "n⃗ = AB⃗ × AC⃗ mit AB⃗ = (−6|3|0) und AC⃗ = (−6|0|3). Dann c = n⃗ · A.",
                way = listOf("AB⃗ × AC⃗ = (9|18|18) = 9 · (1|2|2)", "E: x₁ + 2x₂ + 2x₃ = c; A einsetzen: c = 6 + 0 + 6 = 12", "E: x₁ + 2x₂ + 2x₃ = 12"))),
            ExamPart("b", 2, number("abi-dach-b", "Schornstein", "Der Punkt Q(2|2|0) liegt am Fuß eines Schornsteins. Wie weit ist Q von der Dachebene E: x₁ + 2x₂ + 2x₃ = 12 entfernt?", "d", 0..6, 2, 0,
                tip = "d = |n⃗ · Q − c| : |n⃗| mit |n⃗| = 3.", way = listOf("n⃗ · Q = 2 + 4 + 0 = 6", "d = |6 − 12| : 3 = 2"),
                given = listOf(listOf(Txt("Q(2|2|0)   E: x₁ + 2x₂ + 2x₃ = 12"))), objects = e() + Stage.point("Q", chimney, "cyan", "Q"),
                ok = "2 Einheiten – direkt aus der Hesseschen Normalform.", what = "Der Abstand", mistakes = mapOf(6 to "6 ist der Zähler |6 − 12|. Teile noch durch |n⃗| = 3."))),
            ExamPart("c", 3, Families.pierce("abi-dach-c", 0, "Regenrohr", "Ein Regentropfen fällt senkrecht von A(2|1|6) nach unten, Richtung (0|0|−1). Wo trifft er das Dach?", rainA, rainD, roofN, ROOF_C,
                tip = "Gerade in E einsetzen: 2 + 2 · 1 + 2 · (6 − t) = 12.", way = listOf("(2) + 2(1) + 2(6 − t) = 12", "16 − 2t = 12 → t = 2", "S(2|1|4)"))),
            ExamPart("d", 3, Families.point("abi-dach-d", 0, "Spiegelbild", "Die Dachfläche spiegelt (wie ein Fenster). Wo erscheint das Spiegelbild P′ von P(1|1|0)?", mirrorQ, hidden = true, name = "P′",
                given = listOf(listOf(Txt("P(1|1|0)   E: x₁ + 2x₂ + 2x₃ = 12"))),
                tip = "Lot von P auf E: x⃗ = P + t · (1|2|2). Lotfußpunkt F bestimmen, dann P′ = 2 · F − P.",
                way = listOf("P + t · n⃗ in E: (1 + t) + 2(1 + 2t) + 2(2t) = 12 → 3 + 9t = 12 → t = 1", "F = (2|3|2)", "P′ = 2 · F − P = (3|5|4)"),
                extras = { e() + Stage.point("P", mirrorP, "cyan", "P(1|1|0)") })),
        ))
    }

    // ───────────── Kran ─────────────
    val craneA = p(1, 1, 2); val craneD = p(1, 1, 0); val ropeB = p(3, 0, 2); val ropeD = p(0, 1, 0)
    val craneS = p(3, 3, 2); val ropeW = p(3, 7, 2); val ropeM = p(3, 5, 2)
    val cableK = p(0, 4, 5); val cableD = p(0, 1, 0)   // verläuft über dem Ausleger

    private val crane = run {
        val lines = { listOf(Stage.fullLine("g", craneA, craneD, Bounds(), "cyan", "Ausleger g"), Stage.fullLine("h", ropeB, ropeD, Bounds(), "yellow", "Seil h")) }
        Exam("kran", "Baukran", "🏗️", "Der Ausleger eines Krans liegt auf g: x⃗ = (1|1|2) + s · (1|1|0), ein Seil auf h: x⃗ = (3|0|2) + t · (0|1|0). Alle Angaben in Metern.", listOf(
            ExamPart("a", 3, Families.crossing("abi-kran-a", 0, "Aufhängung", "In welchem Punkt S schneiden sich Ausleger und Seil?", craneA, craneD, ropeB, ropeD,
                tip = "Gleichsetzen: 1 + s = 3, 1 + s = t, 2 = 2.", way = listOf("Zeile 1: s = 2; Zeile 2: t = 1 + s = 3", "Zeile 3: 2 = 2 ✓", "S(3|3|2)"), abi = true)),
            ExamPart("b", 2, number("abi-kran-b", "Winkel", "Unter welchem Winkel schneiden sich Ausleger und Seil? (Gemeint ist der kleinere der beiden Winkel.)", "α", 0..12, 3, 0,
                tip = "cos α = |u⃗ · v⃗| : (|u⃗| · |v⃗|) mit u⃗ = (1|1|0) und v⃗ = (0|1|0).", way = listOf("u⃗ · v⃗ = 1, |u⃗| = √2, |v⃗| = 1", "cos α = 1 : √2 ≈ 0,707", "α = 45°"),
                given = listOf(lineText("g", craneA, craneD, "s"), lineText("h", ropeB, ropeD)), objects = lines() + Stage.point("S", craneS, "white", "S"),
                ok = "45° – der kleinere Winkel zwischen den Richtungen.", what = "Der Winkel", step = 15, suffix = "°", color = "yellow",
                mistakes = mapOf(135 to "135° wäre der Nebenwinkel. Zwischen Geraden zählt der kleinere (Betrag im Zähler)."))),
            ExamPart("c", 4, number("abi-kran-c", "Kabel darüber", "Ein Kabel k: x⃗ = (0|4|5) + r · (0|1|0) verläuft über dem Ausleger. Wie groß ist der Abstand von k und g? (Die Geraden sind windschief.)", "d", 0..8, 3, 0,
                tip = "n⃗ = (1|1|0) × (0|1|0) = (0|0|1). Abstand = |(K − A) · n⃗| : |n⃗| mit K − A = (−1|3|3).", way = listOf("n⃗ = u⃗ × v⃗ = (0|0|1), |n⃗| = 1", "K − A = (0|4|5) − (1|1|2) = (−1|3|3)", "d = |(−1|3|3) · (0|0|1)| : 1 = 3"),
                given = listOf(lineText("g", craneA, craneD, "s"), lineText("k", cableK, cableD, "r")), objects = lines() + Stage.fullLine("k", cableK, cableD, Bounds(), "pink", "Kabel k"),
                ok = "3 Meter – der Höhenunterschied der beiden Geraden.", what = "Der Abstand", mistakes = mapOf(5 to "5 ist die Höhe von k über dem Boden. Der Abstand zu g ist der Unterschied der Höhen."))),
            ExamPart("d", 3, Families.point("abi-kran-d", 0, "Seilmitte", "Das Seil ist zwischen S(3|3|2) und der Winde W(3|7|2) gespannt. Wo hängt der Haken in der Mitte des Seils?", ropeM, hidden = true, name = "M",
                given = listOf(listOf(Txt("S(3|3|2)   W(3|7|2)"))), tip = "M = ½ · (S + W), Zeile für Zeile.", way = listOf("x₁ = (3 + 3) : 2 = 3", "x₂ = (3 + 7) : 2 = 5, x₃ = (2 + 2) : 2 = 2", "M(3|5|2)"),
                extras = { lines() + listOf(Stage.point("S", craneS, "white", "S"), Stage.point("W", ropeW, "white", "W")) })),
        ))
    }

    // ───────────── Ballon (Kugel) ─────────────
    val balloonA = p(2, 2, 2); val balloonB = p(6, 6, 4); val balloonM = p(4, 4, 3); const val BALLOON_R = 3
    val touch = p(6, 5, 5); val touchN = p(2, 1, 2); const val TOUCH_C = 27
    val flier = p(4, 8, 3)

    private val balloon = run {
        val body = { listOf(Stage.sphere("kugel", balloonM, BALLOON_R.toDouble(), "cyan")) }
        Exam("ballon", "Heißluftballon", "🎈", "Ein Ballon hat Kugelform. Die Punkte A(2|2|2) und B(6|6|4) sind die Enden eines Durchmessers.", listOf(
            ExamPart("a", 2, Families.point("abi-ballon-a", 0, "Mittelpunkt", "Wo liegt der Mittelpunkt M der Kugel?", balloonM, hidden = true, name = "M",
                given = listOf(listOf(Txt("A(2|2|2)   B(6|6|4)"))), tip = "M ist die Mitte des Durchmessers: M = ½ · (A + B).", way = listOf("x₁ = (2 + 6) : 2 = 4, x₂ = (2 + 6) : 2 = 4", "x₃ = (2 + 4) : 2 = 3", "M(4|4|3)"),
                extras = { listOf(Stage.point("A", balloonA, "white", "A"), Stage.point("B", balloonB, "white", "B")) })),   // ohne Kugel: Der Mittelpunkt soll gerechnet werden
            ExamPart("b", 3, number("abi-ballon-b", "Radius", "Wie groß ist der Radius der Kugel?", "r", 0..8, BALLOON_R, 0,
                tip = "r = ½ · |AB⃗| mit AB⃗ = (4|4|2).", way = listOf("AB⃗ = (4|4|2), |AB⃗| = √(16 + 16 + 4) = 6", "r = 6 : 2 = 3"),
                given = listOf(listOf(Txt("A(2|2|2)   B(6|6|4)"))), objects = body() + listOf(Stage.point("A", balloonA, "white", "A"), Stage.point("B", balloonB, "white", "B")),
                ok = "r = 3 – der halbe Durchmesser.", what = "Der Radius", mistakes = mapOf(6 to "6 ist der Durchmesser |AB⃗|. Der Radius ist die Hälfte."))),
            ExamPart("c", 4, number("abi-ballon-c", "Berührebene", "Im Punkt T(6|5|5) liegt eine Ebene tangential an der Kugel an. Ihre Gleichung lautet 2x₁ + x₂ + 2x₃ = c. Bestimme c.", "c", 0..40, TOUCH_C, 10,
                tip = "Der Normalenvektor MT⃗ = (2|1|2) passt zu 2x₁ + x₂ + 2x₃. T liegt in der Ebene: c = n⃗ · T.", way = listOf("MT⃗ = T − M = (2|1|2), |MT⃗| = 3 = r ✓", "c = n⃗ · T = 12 + 5 + 10", "c = 27"),
                given = listOf(listOf(Txt("M(4|4|3)   r = 3   T(6|5|5)"))), objects = body() + listOf(Stage.point("T", touch, "pink", "T"), Stage.plane("E", touchN, TOUCH_C.toDouble(), "violet", "E")),
                ok = "c = 27 – die Ebene berührt die Kugel genau in T.", what = "Die Zahl c", color = "violet")),
            ExamPart("d", 3, number("abi-ballon-d", "Abstand zur Hülle", "Ein Segelflieger fliegt im Punkt F(4|8|3). Wie weit ist er von der Oberfläche der Kugel entfernt?", "d", 0..6, 1, 0,
                tip = "Abstand zum Mittelpunkt ausrechnen und den Radius abziehen.", way = listOf("MF⃗ = (0|4|0), |MF⃗| = 4", "d = 4 − 3 = 1"),
                given = listOf(listOf(Txt("F(4|8|3)   M(4|4|3)   r = 3"))), objects = body() + Stage.point("F", flier, "yellow", "F"),
                ok = "1 Einheit bis zur Hülle.", what = "Der Abstand", mistakes = mapOf(4 to "4 ist der Abstand zum Mittelpunkt. Zur Oberfläche musst du noch den Radius abziehen."))),
        ))
    }

    // ───────────── Rutsche ─────────────
    val slideA = p(0, 3, 4); val slideD = p(1, 0, -1); val slideGround = p(4, 3, 0)
    val railB = p(0, 5, 4); val slideMid = p(3, 3, 1)

    private val slide = run {
        val g = { listOf(Stage.fullLine("g", slideA, slideD, Bounds(), "cyan", "Rutsche g"), Stage.plane("B", p(0, 0, 1), 0.0, "violet", "Boden")) }
        Exam("rutsche", "Wasserrutsche", "🛝", "Eine Wasserrutsche verläuft auf der Geraden g: x⃗ = (0|3|4) + t · (1|0|−1). Der Boden ist die Ebene x₃ = 0.", listOf(
            ExamPart("a", 3, Families.pierce("abi-rutsche-a", 0, "Landepunkt", "Wo trifft die Rutsche den Boden?", slideA, slideD, p(0, 0, 1), 0,
                tip = "Auf dem Boden ist x₃ = 0: 4 − t = 0.", way = listOf("x₃(t) = 4 − t = 0", "t = 4", "S(4|3|0)"), planeText = "x₃ = 0 (Boden)")),
            ExamPart("b", 2, number("abi-rutsche-b", "Neigung", "Unter welchem Winkel steigt die Rutsche gegen den Boden an?", "α", 0..12, 3, 0,
                tip = "sin α = |d⃗ · n⃗| : (|d⃗| · |n⃗|) mit n⃗ = (0|0|1).", way = listOf("d⃗ · n⃗ = −1, |d⃗| = √2, |n⃗| = 1", "sin α = 1 : √2 ≈ 0,707", "α = 45°"),
                given = listOf(lineText("g", slideA, slideD), listOf(Txt("Boden: x₃ = 0"))), objects = g(),
                ok = "45° – zwischen Gerade und Ebene rechnest du mit dem Sinus.", what = "Der Winkel", step = 15, suffix = "°", color = "yellow")),
            ExamPart("c", 3, number("abi-rutsche-c", "Handlauf", "Parallel zur Rutsche läuft ein Handlauf h: x⃗ = (0|5|4) + t · (1|0|−1). Wie groß ist der Abstand der beiden parallelen Geraden?", "d", 0..6, 2, 0,
                tip = "Bei parallelen Geraden reicht ein Stützpunkt: (B − A) = (0|2|0), die Richtung (1|0|−1) steht senkrecht darauf.", way = listOf("B − A = (0|2|0)", "(B − A) · d⃗ = 0 → der Vektor steht senkrecht auf der Richtung", "d = |B − A| = 2"),
                given = listOf(lineText("g", slideA, slideD), lineText("h", railB, slideD)), objects = g() + Stage.fullLine("h", railB, slideD, Bounds(), "pink", "Handlauf h"),
                ok = "2 Meter – der Handlauf ist seitlich verschoben.", what = "Der Abstand")),
            ExamPart("d", 4, Families.point("abi-rutsche-d", 0, "Sprungturm", "In welchem Punkt der Rutsche liegt man in 1 m Höhe?", slideMid, hidden = true, home = slideA,
                given = listOf(lineText("g", slideA, slideD)), tip = "x₃ = 4 − t = 1 → t = 3. Dann t in g einsetzen.", way = listOf("4 − t = 1 → t = 3", "(0|3|4) + 3 · (1|0|−1)", "P(3|3|1)"),
                extras = { g() })),
        ))
    }

    val all = listOf(roof, crane, balloon, slide)
}
