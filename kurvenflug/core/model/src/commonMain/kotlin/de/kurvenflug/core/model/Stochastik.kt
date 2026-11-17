package de.kurvenflug.core.model

import kotlin.math.roundToInt

/**
 * Stochastik (Klasse 10–13): Binomialverteilung B(n; p) als Histogramm – Dichte P(X = k) oder kumuliert P(X ≤ k).
 * Säule k steht bei x = k − 9, Höhe 10 Kästchen = 100 % (1 Kästchen = 10 %). Die Sonde fliegt über die Säulen;
 * n oder p so einstellen, dass die Säulen genau an den Stationen enden.
 */
object Stochastik {
    fun choose(n: Int, k: Int): Double {
        if (k < 0 || k > n) return 0.0
        var r = 1.0
        for (i in 1..minOf(k, n - k)) r = r * (n - minOf(k, n - k) + i) / i
        return r
    }
    fun pmf(n: Int, p: Double, k: Int) = if (k < 0 || k > n) 0.0 else choose(n, k) * powK(p, k.toDouble()) * powK(1 - p, (n - k).toDouble())
    fun cdf(n: Int, p: Double, k: Int) = (0..minOf(k, n)).sumOf { pmf(n, p, it) }

    /** Höhe des Histogramms bei x (NaN außerhalb der Säulen 0 … n) */
    fun height(v: Map<String, Double>, x: Double): Double {
        val n = v.getValue("n").roundToInt(); val p = v.getValue("p")
        val k = (x + 9).roundToInt()
        if (k < 0 || k > n || x < -9.5) return Double.NaN
        return 10 * (if ((v["cum"] ?: 0.0) > 0.5) cdf(n, p, k) else pmf(n, p, k))
    }

    fun percent(x: Double) = num(x * 100, 1) + " %"

    // ------------------------------------------------------------------ Kapitel 30

    private val pn = Param("n", "n", 1.0, 18.0, 1.0, 2.0)
    private val pp = Param("p", "p", 0.05, 0.95, 0.05, 0.5)
    private fun s(text: String, answer: Double, sets: String? = null, extra: List<Double> = emptyList()) = Walkthrough.step(text, answer, sets, extra)

    private fun binom(nr: Int, title: String, n: Int, p: Double, cum: Boolean, ks: List<Int>, key: String, task: String, tip: String,
                      quiz: List<String>, walk: List<Step>, start: Double? = null): Level {
        val all = mapOf("n" to n.toDouble(), "p" to p, "cum" to if (cum) 1.0 else 0.0)
        val param = (if (key == "n") pn else pp).let { if (start != null) it.copy(start = start) else it }
        return Level("30-$nr", 30, nr, title, Kind.BINOM, task,
            ks.map { k -> Body(k - 9.0, 10 * (if (cum) cdf(n, p, k) else pmf(n, p, k)), 0.1) },
            listOf(param), mapOf(key to all.getValue(key)), fixed = all - key, tip = tip, quiz = quiz, walk = walk)
    }

    val chapter = Chapter(30, "Binomialverteilung", "Klasse 10–13", "🎲",
        listOf(
            "n-mal dasselbe Experiment, jedes Mal Treffer mit Wahrscheinlichkeit p – zum Beispiel n Münzwürfe. X zählt die Treffer.",
            "Dichte: P(X = k) = (n über k) · pᵏ · (1 − p)ⁿ⁻ᵏ – genau k Treffer. Die Säulen zeigen das als Histogramm.",
            "Kumuliert: P(X ≤ k) = P(X = 0) + … + P(X = k) – höchstens k Treffer. Die Säulen wachsen dann wie eine Treppe bis 100 %.",
            "Ein Kästchen nach oben sind 10 %. Stell n oder p so ein, dass die Säulen genau an den Stationen enden!",
        ),
        listOf(
            binom(1, "Münzwurf", 4, 0.5, false, listOf(0, 2), "n", "Eine Münze wird n-mal geworfen (p = 0,5). P(X = 0) soll 6,25 % sein. Wie oft wird geworfen?",
                "P(X = 0) = 0,5ⁿ = 0,0625 → n = 4. Probe: P(X = 2) = 6 · 0,5⁴ = 37,5 %.",
                listOf("0,5ⁿ = 0,0625", "0,5 · n = 0,0625", "n · 0,0625 = 1", "Wie findest du n?"),
                listOf(s("0,5² = ?", 0.25), s("0,5⁴ = ? %", 6.25), s("n = ?", 4.0, "n"))),
            binom(2, "Glücksrad", 5, 0.2, false, listOf(0, 1), "p", "Ein Glücksrad wird 5-mal gedreht. P(X = 0) = 32,8 % und P(X = 1) = 41,0 %. Wie groß ist die Trefferchance p?",
                "P(X = 0) = (1 − p)⁵ = 0,328 → 1 − p = 0,8 → p = 0,2.",
                listOf("(1 − p)⁵ = 0,328", "p⁵ = 0,328", "5 · p = 0,328", "Welche Gleichung gehört zu P(X = 0)?"),
                listOf(s("⁵√0,328 = 1 − p ≈ ?", 0.8), s("p = 1 − 0,8 = ?", 0.2, "p"))),
            binom(3, "Am häufigsten", 10, 0.3, false, listOf(0, 3), "p", "10 Versuche. Die höchste Säule soll bei k = 3 stehen (26,7 %), P(X = 0) = 2,8 %. p = ?",
                "Die höchste Säule liegt beim Erwartungswert μ = n · p ≈ 3 → p = 0,3. Probe: P(X = 3) = 120 · 0,3³ · 0,7⁷ ≈ 26,7 %.",
                listOf("μ = n · p = 3 → p = 3 : 10", "p = 10 : 3", "p = 3 · 10"),
                listOf(s("Erwartungswert μ = n · p. 3 = 10 · p → p = ?", 0.3, "p"), s("(10 über 3) = ?", 120.0))),
            binom(4, "Höchstens …", 10, 0.5, true, listOf(3, 5), "p", "Kumuliert! 10 Versuche. P(X ≤ 3) = 17,2 % und P(X ≤ 5) = 62,3 %. Wie groß ist die Trefferchance p?",
                "Bei p = 0,5 ist die Verteilung symmetrisch um 5: P(X ≤ 5) liegt etwas über 50 %.",
                listOf("P(X ≤ 5) = P(X = 0) + … + P(X = 5)", "P(X ≤ 5) = P(X = 5)", "P(X ≤ 5) = 5 · p"),
                listOf(s("Symmetrisch um n : 2 = 5 – dann ist p = ?", 0.5, "p")), start = 0.3),
            binom(5, "Mindestens einer", 5, 0.3, true, listOf(0), "n", "Kumuliert. Trefferchance 30 %. Wie oft muss man es versuchen, damit P(X ≤ 0) – also kein Treffer – nur noch 16,8 % ist?",
                "P(X = 0) = 0,7ⁿ = 0,168 → n = 5. Dann ist P(mindestens 1 Treffer) = 1 − 0,168 = 83,2 %.",
                listOf("0,7ⁿ = 0,168", "0,3ⁿ = 0,168", "1 − 0,3 · n = 0,168", "Welche Gleichung gilt für „kein Treffer“?"),
                listOf(s("0,7² = ?", 0.49), s("0,7⁵ ≈ ? %", 16.81), s("n = ?", 5.0, "n"))),
            binom(6, "Treppe bis 100 %", 6, 0.5, true, listOf(2, 4), "n", "Kumuliert, p = 0,5. P(X ≤ 2) = 34,4 % und P(X ≤ 4) = 89,1 %. Wie groß ist n?",
                "P(X ≤ 2) = (1 + 6 + 15) : 64 = 22 : 64 ≈ 34,4 % → n = 6.",
                listOf("(1 + n + n·(n−1)/2) : 2ⁿ = 0,344", "n : 2ⁿ = 0,344", "2 : n = 0,344"),
                listOf(s("Für n = 6: 1 + 6 + 15 = ?", 22.0), s("22 : 2⁶ ≈ ? %", 34.38), s("n = ?", 6.0, "n")), start = 3.0),
        ))
}
