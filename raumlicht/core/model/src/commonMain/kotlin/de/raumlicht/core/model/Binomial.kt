package de.raumlicht.core.model

import kotlin.math.sqrt

/** Welche Frage man an die Verteilung stellt */
enum class BinomialMode(val title: String, val sign: String) { EXACT("genau k", "="), AT_MOST("höchstens k", "≤"), AT_LEAST("mindestens k", "≥") }

/** Binomialverteilung B(n; p): [n] unabhängige Versuche, jeder klappt mit Wahrscheinlichkeit [p]. */
class Binomial(val n: Int, val p: Double) {
    init { require(n in 1..MAX_N && p > 0.0 && p < 1.0) }
    /** P(X = k) für k = 0 … n – schrittweise berechnet, damit auch große n ohne riesige Zwischenwerte gehen */
    val single: List<Double> = run {
        // beim wahrscheinlichsten Wert anfangen (dort ist die Zahl am größten) und nach beiden Seiten weiterrechnen, am Ende auf Summe 1 bringen
        val mode = ((n + 1) * p).toInt().coerceIn(0, n); val w = DoubleArray(n + 1); w[mode] = 1.0; val q = 1 - p
        for (k in mode + 1..n) w[k] = w[k - 1] * (n - k + 1) / k * p / q
        for (k in mode - 1 downTo 0) w[k] = w[k + 1] * (k + 1) / (n - k) * q / p
        val sum = w.sum(); w.map { it / sum }
    }
    /** P(X ≤ k) – die aufsummierte (kumulierte) Verteilung */
    val upTo: List<Double> = single.runningReduce { a, b -> a + b }.map { it.coerceAtMost(1.0) }
    val mean get() = n * p
    val sigma get() = sqrt(n * p * (1 - p))
    fun exactly(k: Int) = single.getOrElse(k) { 0.0 }
    fun atMost(k: Int) = if (k < 0) 0.0 else upTo[k.coerceAtMost(n)]
    fun atLeast(k: Int) = if (k <= 0) 1.0 else if (k > n) 0.0 else (1 - upTo[k - 1]).coerceAtLeast(0.0)
    fun between(a: Int, b: Int) = if (a > b) 0.0 else (atMost(b) - atMost(a - 1)).coerceAtLeast(0.0)
    fun ask(mode: BinomialMode, k: Int) = when (mode) { BinomialMode.EXACT -> exactly(k); BinomialMode.AT_MOST -> atMost(k); BinomialMode.AT_LEAST -> atLeast(k) }
    /** Gehört der Wert [x] zum gefragten Ereignis? (für die Färbung der Säulen) */
    fun counts(mode: BinomialMode, k: Int, x: Int) = when (mode) { BinomialMode.EXACT -> x == k; BinomialMode.AT_MOST -> x <= k; BinomialMode.AT_LEAST -> x >= k }
    /** Der Ansatz in Worten und Formel – so schreibt man es in der Klausur hin */
    fun approach(mode: BinomialMode, k: Int): String = when (mode) {
        BinomialMode.EXACT -> "P(X = $k) = ($n über $k) · ${pct(p)}^$k · ${pct(1 - p)}^${n - k}"
        BinomialMode.AT_MOST -> "P(X ≤ $k) = P(X = 0) + … + P(X = $k)  (kumuliert)"
        BinomialMode.AT_LEAST -> if (k <= 0) "P(X ≥ 0) = 1" else "P(X ≥ $k) = 1 − P(X ≤ ${k - 1})  (Gegenereignis)"
    }
    /** Lux' Satz dazu: was die Zahl im Alltag heißt */
    fun story(mode: BinomialMode, k: Int): String { val v = ask(mode, k); val per = (v * 100).let { kotlin.math.round(it).toInt() }
        val what = when (mode) { BinomialMode.EXACT -> "genau $k"; BinomialMode.AT_MOST -> "höchstens $k"; BinomialMode.AT_LEAST -> "mindestens $k" }
        return when { v > 0.9995 -> "Praktisch immer: $what Treffer gibt es so gut wie sicher."; v < 0.0005 -> "Praktisch nie: $what Treffer kommen so gut wie nicht vor."
            else -> "In etwa $per von 100 Durchgängen gibt es $what Treffer." } }
    companion object {
        const val MAX_N = 100
        private fun pct(v: Double) = Geo.num(kotlin.math.round(v * 100) / 100)
        /** Prozent mit zwei Nachkommastellen, deutsch geschrieben */
        fun percent(v: Double): String { val h = kotlin.math.round(v * 10000).toLong(); return "${h / 100},${(h % 100).toString().padStart(2, '0')} %" }
    }
}
