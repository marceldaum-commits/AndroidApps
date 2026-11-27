package de.raumlicht.core.model

import de.raumlicht.core.model.Families.col
import de.raumlicht.core.model.Part.Num
import de.raumlicht.core.model.Part.Txt

/**
 * Kapitel 28: Lage von Gerade und Ebene, Lage zweier Ebenen – schneiden, parallel, liegt drin.
 * Jede Aufgabe ist von Hand durchgerechnet; [TourElevenTest] rechnet sie unabhängig nach.
 */
internal object TourEleven {
    private fun p(x: Int, y: Int, z: Int) = V3(x, y, z)
    private fun lineText(name: String, a: V3, d: V3, param: String = "t") = listOf(Txt("$name: x⃗ = "), col(a), Txt(" + $param · "), col(d))
    private fun eq(name: String, n: V3, c: Int) = listOf(Txt("$name: ${Geo.planeText(n, c)}"))

    // Gerade–Ebene (E: x₁ + x₂ + x₃ = 6)
    val nE = p(1, 1, 1); const val cE = 6
    val a1 = p(3, 1, 0); val d1 = p(1, 0, -1)          // parallel (n · d = 0, n · A = 4 ≠ 6)
    val a2 = p(2, 2, 2); val d2 = p(1, -2, 1)          // liegt in E (n · d = 0, n · A = 6)
    val a3 = p(1, 1, 1); val d3 = p(2, 1, 0)           // schneidet (n · d = 3), t = 1 → S(3|2|1)
    // Parameter a: g liegt in E4: x₁ + x₂ + 2x₃ = 7
    val n4 = p(1, 1, 2); const val c4 = 7; val a4 = p(1, 2, 2); fun d4(a: Int) = p(3, a, -1); const val A4 = -1
    // Ebene–Ebene
    val s5 = listOf(TourTen.Eq(p(1, 2, -1), 3), TourTen.Eq(p(2, 4, -2), 6))          // identisch
    val s6 = listOf(TourTen.Eq(p(2, -1, 2), 4), TourTen.Eq(p(4, -2, 4), 10))         // parallel
    val e7 = TourTen.Eq(p(1, 1, 1), 3); fun f7(k: Int) = TourTen.Eq(p(2, 2, k), 7); const val K7 = 2
    // Abi-Check: Schnittgerade zweier Ebenen
    val s8 = listOf(TourTen.Eq(p(1, 2, 1), 9), TourTen.Eq(p(1, -1, 1), 3)); val p8 = p(1, 2, 4)

    private fun pl(id: String, n: V3, c: Int, color: String, label: String) = Stage.plane(id, n, c.toDouble(), color, label)

    private fun lineVsPlane(id: String, title: String, mission: String, a: V3, d: V3, solution: Int, tip: String, way: List<String>, ok: String, mistakes: Map<Int, String> = emptyMap()) =
        Families.scalar(id, 28, title, mission, "n", 0..2, solution, if (solution == 1) 0 else 1, tip, way, "pink",
            formula = { v -> listOf(lineText("g", a, d), eq("E", nE, cE), listOf(Txt("g ist "), Num(listOf("parallel zu E", "schneidet E", "liegt in E")[v], "a"))) },
            objects = { _, _ -> listOf(pl("E", nE, cE, "violet", "E"), Stage.fullLine("g", a, d, Bounds(), "cyan", "g"), Stage.point("A", a, "cyan", "A")) },
            check = { v -> Families.compare(v, solution, "Die Zahl", ok, mistakes) })

    private fun planes(id: String, title: String, mission: String, s: List<TourTen.Eq>, solution: Int, tip: String, way: List<String>, ok: String, mistakes: Map<Int, String> = emptyMap()) =
        Families.scalar(id, 28, title, mission, "n", 0..2, solution, if (solution == 1) 0 else 1, tip, way, "pink",
            formula = { v -> listOf(eq("E₁", s[0].n, s[0].c), eq("E₂", s[1].n, s[1].c), listOf(Txt("E₁ und E₂ sind "), Num(listOf("parallel", "schneiden sich", "identisch")[v], "a"))) },
            objects = { _, _ -> listOf(pl("E1", s[0].n, s[0].c, "violet", "E₁"), pl("E2", s[1].n, s[1].c, "cyan", "E₂")) },
            check = { v -> Families.compare(v, solution, "Die Zahl", ok, mistakes) })

    private val ch28 = listOf(
        lineVsPlane("28-1", "Läuft daneben", "Die Gerade g und die Scheibe E: Wie liegen sie zueinander? 0 = parallel, 1 = schneidet, 2 = liegt in E.", a1, d1, 0,
            tip = "Erst n⃗ · d⃗ ausrechnen: Ist es null, läuft g parallel oder liegt in E. Dann den Stützpunkt A in E einsetzen.",
            way = listOf("n⃗ = (1|1|1), d⃗ = (1|0|−1): n⃗ · d⃗ = 1 + 0 − 1 = 0 → parallel oder drin", "A(3|1|0) in E: 3 + 1 + 0 = 4 ≠ 6", "A liegt nicht in E → g ist echt parallel zu E"),
            ok = "0 – parallel: Der Strahl läuft nie durch die Scheibe.",
            mistakes = mapOf(2 to "Erst n⃗ · d⃗ = 0 zeigt „parallel oder drin“. Ob g drin liegt, entscheidet die Punktprobe: 3 + 1 + 0 = 4 ≠ 6.")),
        lineVsPlane("28-2", "Mitten drin", "Wieder g und E. Diesmal ein anderer Strahl: 0 = parallel, 1 = schneidet, 2 = liegt in E.", a2, d2, 2,
            tip = "n⃗ · d⃗ ausrechnen, dann die Punktprobe mit A.",
            way = listOf("n⃗ · d⃗ = 1 · 1 + 1 · (−2) + 1 · 1 = 0 → parallel oder drin", "A(2|2|2) in E: 2 + 2 + 2 = 6 ✓", "A liegt in E und g läuft in E → g liegt in E"),
            ok = "2 – g liegt ganz in der Scheibe.",
            mistakes = mapOf(0 to "n⃗ · d⃗ = 0 gilt, aber die Punktprobe stimmt: 2 + 2 + 2 = 6. Dann liegt g in E – nicht nur parallel.")),
        lineVsPlane("28-3", "Durchstich", "Und noch ein Strahl: 0 = parallel, 1 = schneidet, 2 = liegt in E.", a3, d3, 1,
            tip = "n⃗ · d⃗ ≠ 0 heißt: g stößt durch die Scheibe.",
            way = listOf("n⃗ · d⃗ = 1 · 2 + 1 · 1 + 1 · 0 = 3 ≠ 0 → g schneidet E", "Schnittpunkt: (1 + 2t) + (1 + t) + 1 = 6 → 3 + 3t = 6 → t = 1", "S(3|2|1)"),
            ok = "1 – schneidet, im Punkt S(3|2|1).",
            mistakes = mapOf(0 to "Parallel wäre n⃗ · d⃗ = 0. Hier ist es 2 + 1 + 0 = 3.")),
        Families.scalar("28-4", 28, "Wunschrichtung", "Der Strahl g soll ganz in der Scheibe E liegen. Der Stützpunkt A liegt schon drin. Welche Zahl a muss in der Richtung stehen?", "a", -4..4, A4, 0,
            tip = "Liegt A in E (Punktprobe), dann liegt g genau dann in E, wenn n⃗ · d⃗ = 0 ist.",
            way = listOf("A(1|2|2) in E: 1 + 2 + 2 · 2 = 7 ✓", "n⃗ · d⃗ = 1 · 3 + 1 · a + 2 · (−1) = 1 + a", "1 + a = 0 → a = −1"), color = "yellow",
            formula = { v -> listOf(listOf(Txt("g: x⃗ = "), col(a4), Txt(" + t · (3 | "), Num(Geo.num(v), "a"), Txt(" | −1)")), eq("E", n4, c4)) },
            objects = { v, _ -> listOf(pl("E", n4, c4, "violet", "E"), Stage.fullLine("g", a4, d4(v), Bounds(), "cyan", "g"), Stage.point("A", a4, "cyan", "A")) },
            check = { v -> Families.compare(v, A4, "Die Zahl a", "a = −1 – dann steht d⃗ senkrecht auf n⃗ und g bleibt in E.", mapOf(1 to "Mit a = 1 ist n⃗ · d⃗ = 2 ≠ 0: g steigt aus der Scheibe heraus.")) }),
        planes("28-5", "Doppelt gemoppelt", "Wie liegen die beiden Scheiben zueinander? 0 = parallel, 1 = schneiden sich, 2 = identisch.", s5, 2,
            tip = "Vergleiche die Normalenvektoren: Ist einer ein Vielfaches des anderen? Dann auch die rechte Seite vergleichen.",
            way = listOf("n⃗₂ = (2|4|−2) = 2 · (1|2|−1) = 2 · n⃗₁ → parallel oder identisch", "rechte Seite: 6 = 2 · 3 – dasselbe Vielfache", "E₂ ist E₁ mit 2 multipliziert → identisch"),
            ok = "2 – dieselbe Scheibe, nur anders aufgeschrieben.",
            mistakes = mapOf(0 to "Die Normalenvektoren sind Vielfache (Faktor 2). Auch die rechte Seite passt (6 = 2 · 3): Dann sind die Ebenen identisch, nicht nur parallel.")),
        planes("28-6", "Fast gleich", "Wie liegen diese beiden Scheiben zueinander? 0 = parallel, 1 = schneiden sich, 2 = identisch.", s6, 0,
            tip = "Normalenvektoren vergleichen, dann die rechte Seite mit demselben Faktor.",
            way = listOf("n⃗₂ = (4|−2|4) = 2 · (2|−1|2) = 2 · n⃗₁ → parallel oder identisch", "rechte Seite: 2 · 4 = 8 ≠ 10", "gleiche Richtung, aber verschoben → echt parallel"),
            ok = "0 – parallel, aber nicht dieselbe Scheibe.",
            mistakes = mapOf(2 to "Mit Faktor 2 müsste rechts 2 · 4 = 8 stehen, es steht aber 10: Die Scheiben sind verschoben, nicht identisch.")),
        Families.scalar("28-7", 28, "Parallel mit Parameter", "Für welches k sind E₁ und E₂ parallel (und verschieden)?", "k", 0..6, K7, 0,
            tip = "Parallel heißt: n⃗₂ ist ein Vielfaches von n⃗₁. Die ersten beiden Zahlen (2|2) sind das Doppelte von (1|1) – also muss k das Doppelte von 1 sein.",
            way = listOf("n⃗₁ = (1|1|1), n⃗₂ = (2|2|k): Faktor 2 aus den ersten beiden Zeilen → k = 2", "rechte Seite: 2 · 3 = 6 ≠ 7 → die Scheiben sind verschoben, also nicht identisch", "k = 2: echt parallel"), color = "violet",
            formula = { v -> listOf(eq("E₁", e7.n, e7.c), listOf(Txt("E₂: 2x₁ + 2x₂ + "), Num(Geo.num(v), "a"), Txt("x₃ = 7"))) },
            objects = { v, _ -> listOf(pl("E1", e7.n, e7.c, "violet", "E₁"), pl("E2", f7(v).n, f7(v).c, "cyan", "E₂")) },
            check = { v -> Families.compare(v, K7, "Die Zahl k", "k = 2 – gleiche Richtung, verschobene Scheibe.", mapOf(1 to "Mit k = 1 ist n⃗₂ = (2|2|1) kein Vielfaches von (1|1|1): Die Scheiben schneiden sich.")) }),
        Families.point("28-8", 28, "Abi-Check: Nahtstelle", "Zwei Dachflächen treffen sich in einer Geraden (der Kante). Auf der Kante soll ein Lichtpunkt sitzen: der Punkt mit x₁ = 1. Wo ist er?",
            p8, hidden = true, abi = true, tip = "E₁ − E₂ verschiebt x₁ und x₃ weg: nur noch x₂ bleibt. Dann x₁ = 1 einsetzen.",
            way = listOf("E₁ − E₂: 3x₂ = 6, also x₂ = 2", "in E₂: x₁ − 2 + x₃ = 3, also x₁ + x₃ = 5; mit x₁ = 1 folgt x₃ = 4", "Probe in E₁: 1 + 4 + 4 = 9 ✓; Kante: (5 − t | 2 | t); P(1|2|4)"),
            given = listOf(eq("E₁", s8[0].n, s8[0].c), eq("E₂", s8[1].n, s8[1].c)),
            extras = { listOf(pl("E1", s8[0].n, s8[0].c, "violet", "E₁"), pl("E2", s8[1].n, s8[1].c, "cyan", "E₂")) },
            mistakes = listOf(p(1, 2, 2) to "Zur Probe in E₁: 1 + 4 + 2 = 7 ≠ 9. Rechne x₃ aus x₁ + x₃ = 5.")),
    )

    val chapters = listOf(
        Chapter(28, "Lage von Gerade und Ebene", "schneidet, liegt drin oder läuft parallel – und wie zwei Ebenen zueinander liegen", "Prismenhalle", "🔭", "orange", listOf(
            "In der Prismenhalle stehen Scheiben und Strahlen. Du entscheidest: Trifft der Strahl, läuft er daneben oder liegt er mittendrin?",
            "Der Schlüssel ist n⃗ · d⃗: Ist es null, läuft der Strahl parallel zur Scheibe – ob er drin liegt, zeigt dann die Punktprobe.",
            "Bei zwei Scheiben vergleichst du die Normalenvektoren: Vielfache heißt parallel oder identisch, sonst schneiden sie sich in einer Geraden."), ch28),
    )
}
