package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/** Kapitel 26: Hessesche Normalform, Abstände paralleler Geraden und Ebenen, Winkel zwischen Geraden und zwischen Ebenen – auf Abi-Niveau. */
internal object TourNine {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))

    /** Abstand als ganze Zahl einstellen; der Messstrahl läuft vom Punkt [from] in Richtung Lotfußpunkt [foot] */
    private fun distance(id: String, title: String, mission: String, solution: Int, from: V3, foot: V3, formula: List<List<Part>>, label: String, tip: String, way: List<String>,
                         abi: Boolean = false, mistakes: Map<Int, String> = emptyMap(), ok: String, objects: List<Obj>) =
        Families.scalar(id, 26, title, mission, "d", 0..9, solution, 1, tip, way, abi = abi,
            formula = { a -> formula + listOf(listOf(Txt("$label = "), Num(Geo.num(a), "a"))) },
            objects = { _, ph -> objects + if (ph == Phase.DONE) listOf(Stage.line("lot", from, foot - from, 0.0, 1.0, "pink")) else listOf(Stage.stub("richtung", from, foot - from, "pink", length = 0.9)) },
            check = { a -> Families.compare(a, solution, "Der Abstand", ok, mistakes) { hit -> listOf(Beam(from, from + (foot - from) * (a / solution.toDouble()), "pink", hit)) } })

    /** Winkel in 15°-Schritten; der Bogen liegt zwischen [u] und [v] am Punkt [o] */
    private fun angle(id: String, title: String, mission: String, o: V3, u: V3, v: V3, degrees: Int, formula: List<List<Part>>, tip: String, way: List<String>,
                      mistakes: Map<Int, String> = emptyMap(), ok: String, objects: List<Obj>) =
        Families.scalar(id, 26, title, mission, "α", 0..12, degrees / 15, 0, tip, way, "yellow", step = 15, suffix = "°",
            formula = { a -> formula + listOf(listOf(Txt("α = "), Num("${a * 15}°", "a"))) },
            objects = { _, _ -> objects + Stage.arc("bogen", o, u, v, 1.8) },
            check = { a -> Families.compare(a * 15, degrees, "Der Winkel", ok, mistakes) })

    private val ch26 = run {
        val n1 = p(2, 2, 1); val q1 = p(4, 3, 4); val f1 = V3(4 - 8 / 3.0, 3 - 8 / 3.0, 4 - 4 / 3.0)
        val n2 = p(1, 2, 2); val q2 = p(2, 1, 3)
        val a3 = p(5, 4, 1); val d3 = p(1, 0, 1); val n3 = p(2, 1, -2); val f3 = p(1, 2, 5)
        val n4 = p(2, -1, 2); val q4 = p(2, 2, 1); val f4 = p(4, 1, 3)
        val a5 = p(1, 2, 2); val u5 = p(2, 1, 2); val b5 = p(5, 1, 3); val f5 = p(3, 3, 4)
        val o6 = p(4, 4, 3); val u6 = p(2, 0, 2); val v6 = p(0, -3, -3)
        val o7 = p(3, 4, 3); val n7a = p(1, 0, 1); val n7b = p(0, 0, 1)
        val q8 = p(4, 4, 6); val f8 = p(2, 0, 2)
        listOf(
            distance("26-1", "Maßband für Ebenen", "Die Hessesche Normalform macht aus der Scheibe ein Maßband: Punkt einsetzen, Abstand ablesen. Wie weit ist P von E entfernt?", 4, q1, f1,
                listOf(listOf(Txt("P(4|3|4)    E: 2x₁ + 2x₂ + x₃ = 6")), listOf(Txt("HNF: (2x₁ + 2x₂ + x₃ − 6) : |n⃗| = 0"))), "d(P, E)",
                tip = "|n⃗| = √(4 + 4 + 1) = 3. P in die linke Seite der HNF einsetzen: (8 + 6 + 4 − 6) : 3.",
                way = listOf("n⃗ = (2|2|1), |n⃗| = √(4 + 4 + 1) = 3", "HNF: (2x₁ + 2x₂ + x₃ − 6) : 3 = 0", "P einsetzen: (8 + 6 + 4 − 6) : 3 = 12 : 3 = 4"),
                mistakes = mapOf(6 to "6 kommt heraus, wenn du die 6 von rechts vergisst: Im Zähler steht 8 + 6 + 4 − 6."), ok = "Genau 4 – die HNF hat es direkt ausgespuckt.",
                objects = listOf(Stage.plane("E", n1, 6.0, "violet", "E"), Stage.point("P", q1, "cyan", "P(4|3|4)"))),
            Families.scalar("26-2", 26, "Abstand nach Maß", "Die Scheibe E: x₁ + 2x₂ + 2x₃ = c soll von P genau 3 Einheiten entfernt sein und hinter P liegen (c > 10). Welches c passt?", "c", 0..24, 19, 10,
                tip = "HNF mit P eingesetzt: |2 + 2 + 6 − c| : 3 = 3, also |10 − c| = 9. Zwei Lösungen – gesucht ist die mit c > 10.",
                way = listOf("n⃗ = (1|2|2), |n⃗| = 3, n⃗ · P = 2 + 2 + 6 = 10", "|10 − c| : 3 = 3 → |10 − c| = 9", "c = 1 oder c = 19; gesucht ist c > 10, also c = 19"), color = "violet",
                formula = { a -> listOf(listOf(Txt("P(2|1|3)    E: x₁ + 2x₂ + 2x₃ = "), Num(Geo.num(a), "a")), listOf(Txt("d(P, E) = |n⃗ · P − c| : |n⃗| = 3"))) },
                objects = { a, _ -> listOf(Stage.plane("E", n2, a.toDouble(), "violet", "E"), Stage.point("P", q2, "cyan", "P(2|1|3)"), Stage.stub("richtung", q2, n2, "pink", length = 0.9)) },
                check = { a -> Families.compare(a, 19, "c", "c = 19 – die Scheibe hängt genau 3 Einheiten hinter P.",
                    mapOf(1 to "c = 1 hat auch den Abstand 3, liegt aber auf der anderen Seite von P. Gesucht ist c > 10.", 10 to "Bei c = 10 liegt P genau in der Scheibe – der Abstand wäre 0.")) }),
            distance("26-3", "Strahl neben der Scheibe", "Der Strahl g läuft parallel zur Scheibe E. Prüf das kurz – und bestimme dann den Abstand.", 6, a3, f3,
                listOf(lineText("g", a3, d3), listOf(Txt("E: 2x₁ + x₂ − 2x₃ = −6"))), "d(g, E)",
                tip = "Parallel, weil d⃗ · n⃗ = 2 + 0 − 2 = 0. Dann reicht der Stützpunkt A: |n⃗ · A − c| : |n⃗| mit c = −6.",
                way = listOf("d⃗ · n⃗ = (1|0|1) · (2|1|−2) = 0 → g ist parallel zu E", "n⃗ · A = 10 + 4 − 2 = 12, |n⃗| = 3", "d = |12 − (−6)| : 3 = 18 : 3 = 6"),
                mistakes = mapOf(2 to "2 kommt heraus, wenn du 12 − 6 rechnest. Rechts steht aber −6: 12 − (−6) = 18."), ok = "6 Einheiten – überall entlang des Strahls.",
                objects = listOf(Stage.plane("E", n3, -6.0, "violet", "E"), Stage.fullLine("g", a3, d3, Bounds(), "cyan", "g"), Stage.point("A", a3, "cyan", "A(5|4|1)"))),
            distance("26-4", "Doppelverglasung", "Zwei Scheiben hängen parallel – aber ihre Gleichungen sehen verschieden aus. Wie groß ist ihr Abstand?", 3, q4, f4,
                listOf(listOf(Txt("E₁: 2x₁ − x₂ + 2x₃ = 4")), listOf(Txt("E₂: 4x₁ − 2x₂ + 4x₃ = 26"))), "d(E₁, E₂)",
                tip = "Erst E₂ durch 2 teilen, damit beide denselben Normalenvektor haben: 2x₁ − x₂ + 2x₃ = 13. Dann |c₂ − c₁| : |n⃗|.",
                way = listOf("E₂ durch 2: 2x₁ − x₂ + 2x₃ = 13", "n⃗ = (2|−1|2), |n⃗| = 3", "d = |13 − 4| : 3 = 9 : 3 = 3"),
                mistakes = mapOf(9 to "9 ist |c₂ − c₁|. Teile noch durch die Länge des Normalenvektors: |n⃗| = 3."), ok = "3 Einheiten – erst gleich machen, dann abziehen.",
                objects = listOf(Stage.plane("E1", n4, 4.0, "violet", "E₁"), Stage.plane("E2", n4, 13.0, "cyan", "E₂"), Stage.point("Q", q4, "pink"))),
            distance("26-5", "Zwei parallele Traversen", "Die Traversen g und h laufen parallel. Wie groß ist ihr Abstand?", 3, b5, f5,
                listOf(lineText("g", a5, u5, "s"), lineText("h", b5, u5)), "d(g, h)",
                tip = "Nimm den Stützpunkt B von h und fälle das Lot auf g: t = (B − A) · u⃗ : (u⃗ · u⃗) = 9 : 9 = 1.",
                way = listOf("B − A = (4|−1|1), t = (8 − 1 + 2) : 9 = 1", "F = A + 1 · u⃗ = (3|3|4)", "d = |B − F| = |(2|−2|−1)| = √9 = 3"),
                ok = "3 Einheiten – an jeder Stelle der beiden Traversen.",
                objects = listOf(Stage.fullLine("g", a5, u5, Bounds(), "cyan", "g"), Stage.fullLine("h", b5, u5, Bounds(), "violet", "h"), Stage.point("B", b5, "violet", "B(5|1|3)"))),
            angle("26-6", "Zwei Laser kreuzen sich", "Die Laser g und h kreuzen sich. Unter welchem Winkel? Gemeint ist immer der kleinere der beiden Winkel.", o6, u6, -v6, 60,
                listOf(lineText("g", o6, u6, "s"), lineText("h", o6, v6), listOf(Txt("cos α = |u⃗ · v⃗| : (|u⃗| · |v⃗|)"))),
                tip = "u⃗ · v⃗ = 0 + 0 − 6 = −6, |u⃗| = √8, |v⃗| = √18. Mit Betrag: 6 : √144 = 6 : 12.",
                way = listOf("u⃗ · v⃗ = (2|0|2) · (0|−3|−3) = −6", "|u⃗| · |v⃗| = √8 · √18 = √144 = 12", "cos α = |−6| : 12 = ½ → α = 60°"),
                mistakes = mapOf(120 to "120° kommt ohne den Betrag heraus – das ist der Nebenwinkel. Zwischen Geraden zählt der kleinere: 180° − 120°."), ok = "60° – mit dem Betrag landest du immer beim kleineren Winkel.",
                objects = listOf(Stage.fullLine("g", o6, u6, Bounds(), "cyan", "g"), Stage.fullLine("h", o6, v6, Bounds(), "pink", "h"), Stage.point("S", o6, "white", "S"))),
            angle("26-7", "Dachneigung", "Die Dachfläche E₁ steht schräg auf dem Boden E₂. Wie groß ist der Neigungswinkel zwischen Dach und Boden?", o7, n7a, n7b, 45,
                listOf(listOf(Txt("E₁: x₁ + x₃ = 6    E₂: x₃ = 0")), listOf(Txt("cos α = |n⃗₁ · n⃗₂| : (|n⃗₁| · |n⃗₂|)"))),
                tip = "n⃗₁ = (1|0|1), n⃗₂ = (0|0|1). Skalarprodukt 1, Längen √2 und 1.",
                way = listOf("n⃗₁ = (1|0|1), n⃗₂ = (0|0|1)", "n⃗₁ · n⃗₂ = 1, |n⃗₁| = √2, |n⃗₂| = 1", "cos α = 1 : √2 ≈ 0,707 → α = 45°"),
                ok = "45° – der Winkel zwischen den Flächen ist der zwischen ihren Normalen.",
                objects = listOf(Stage.plane("E1", n7a, 6.0, "violet", "E₁"), Stage.vector("n1", o7, o7 + n7a * 2, "violet", "n⃗₁"), Stage.vector("n2", o7, o7 + n7b * 2, "cyan", "n⃗₂"))),
            distance("26-8", "Abi-Check: Sonnensegel", "Ein Sonnensegel ist an den Achsen befestigt: bei 6 auf der x₁-Achse, bei 3 auf der x₂-Achse und bei 3 auf der x₃-Achse. Wie weit ist der Strahler P vom Segel entfernt?", 6, q8, f8,
                listOf(listOf(Txt("Achsenabschnitte: 6, 3 und 3    P(4|4|6)")), listOf(Txt("E: x₁ : 6 + x₂ : 3 + x₃ : 3 = 1"))), "d(P, E)", abi = true,
                tip = "Erst die Ebene: mal 6 ergibt x₁ + 2x₂ + 2x₃ = 6. Dann die HNF mit P: (4 + 8 + 12 − 6) : 3.",
                way = listOf("x₁ : 6 + x₂ : 3 + x₃ : 3 = 1, mal 6: x₁ + 2x₂ + 2x₃ = 6", "n⃗ = (1|2|2), |n⃗| = 3", "d = |4 + 8 + 12 − 6| : 3 = 18 : 3 = 6"),
                mistakes = mapOf(8 to "8 kommt heraus, wenn du die 6 von rechts vergisst: Im Zähler steht 24 − 6."), ok = "6 Einheiten – Ebene aufstellen, HNF anwenden, fertig.",
                objects = listOf(Stage.plane("E", p(1, 2, 2), 6.0, "violet", "E"), Stage.point("P", q8, "cyan", "P(4|4|6)"))),
        )
    }

    val chapters = listOf(
        Chapter(26, "HNF, Abstände & Winkel", "Hessesche Normalform, parallele Geraden und Ebenen, Winkel zwischen Geraden und zwischen Ebenen", "Glaspavillon", "🧲", "violet", listOf(
            "Im Glaspavillon wird gemessen: Abstände und Winkel – so, wie sie im Abi drankommen.",
            "Die Hessesche Normalform ist ein Maßband: Ebenengleichung durch |n⃗| teilen, Punkt einsetzen, Abstand ablesen.",
            "Bei Geraden und Ebenen zählt der kleinere Winkel – deshalb steht in der Formel ein Betrag."), ch26),
    )
}
