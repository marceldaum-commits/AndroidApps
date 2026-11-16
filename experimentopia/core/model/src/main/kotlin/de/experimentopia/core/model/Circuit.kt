package de.experimentopia.core.model

import kotlin.math.abs

/** Bauteile für den Stromkreis. Widerstand in Ohm; ∞ = Strom kommt nicht durch. */
enum class Part(val title: String, val emoji: String, val ohm: Double, val conducts: Boolean = ohm.isFinite()) {
    LEER("leer", "◻️", Double.POSITIVE_INFINITY),
    KABEL("Kabel", "〰️", 0.0),
    LAMPE("Lämpchen", "💡", 10.0),
    MOTOR("Motor", "🌀", 10.0),
    SUMMER("Summer", "🔔", 10.0),
    SCHALTER_AUF("Schalter (offen)", "🔓", Double.POSITIVE_INFINITY),
    SCHALTER_ZU("Schalter (zu)", "🔒", 0.0),
    // Stoffe für den Leitertest
    NAGEL("Eisennagel", "📌", 0.0),
    MUENZE("Münze", "🥉", 0.0),
    ALUFOLIE("Alufolie", "🥈", 0.0),
    BLEISTIFT("Bleistiftmine", "✏️", 8.0),     // Graphit leitet, aber schlechter als Metall
    SALZWASSER("Salzwasser", "🧂", 25.0),       // leitet schwach: Lampe glimmt
    LEITUNGSWASSER("Leitungswasser", "🚰", 5000.0), // leitet so wenig, dass die Lampe aus bleibt
    HOLZ("Holzstab", "🌲", Double.POSITIVE_INFINITY),
    GUMMI("Radiergummi", "🧽", Double.POSITIVE_INFINITY),
    GLAS("Glasmurmel", "🔮", Double.POSITIVE_INFINITY),
    PLASTIK("Plastiklöffel", "🥄", Double.POSITIVE_INFINITY),
    WIDERSTAND("Widerstand", "▭", 20.0),
    ;
    val isLoad get() = this == LAMPE || this == MOTOR || this == SUMMER
    val isSwitch get() = this == SCHALTER_AUF || this == SCHALTER_ZU
    val isMaterial get() = this in setOf(NAGEL, MUENZE, ALUFOLIE, BLEISTIFT, SALZWASSER, LEITUNGSWASSER, HOLZ, GUMMI, GLAS, PLASTIK)
    fun toggled() = when (this) { SCHALTER_AUF -> SCHALTER_ZU; SCHALTER_ZU -> SCHALTER_AUF; else -> this }
}

/** Ein Platz im Stromkreis mit Bauteil und (für Widerstände) eigenem Wert. */
data class Slot(val part: Part, val ohm: Double = part.ohm) {
    val r get() = if (part == Part.WIDERSTAND) ohm else part.ohm
}

/**
 * Schaltplan als Baum: Reihe und Parallel. Gezeichnet wird er als Leiter: Spalten von links nach rechts,
 * eine Spalte ist entweder ein Bauteil oder mehrere parallele Zweige (jeder Zweig selbst eine Reihe).
 */
data class Layout(val columns: List<List<List<Int>>>) {
    /** Alle Platz-Nummern */
    val slots get() = columns.flatten().flatten()
    companion object {
        fun single(n: Int) = Layout(List(n) { listOf(listOf(it)) })
        /** [before] Plätze in Reihe, dann parallele Zweige, dann [after] in Reihe */
        fun ladder(before: Int, branches: List<Int>, after: Int = 0): Layout {
            var k = 0
            val cols = mutableListOf<List<List<Int>>>()
            repeat(before) { cols += listOf(listOf(k++)) }
            cols += branches.map { len -> List(len) { k++ } }
            repeat(after) { cols += listOf(listOf(k++)) }
            return Layout(cols)
        }
    }
}

/** Ergebnis der Rechnung: Stromstärke (A) und Spannung (V) je Platz, gesamt, Kurzschluss? */
data class CircuitResult(val totalA: Double, val totalOhm: Double, val amps: List<Double>, val volts: List<Double>, val short: Boolean) {
    /** Helligkeit relativ zu einem einzelnen Lämpchen an der gleichen Batterie (P = I²·R) */
    fun power(i: Int, slots: List<Slot>) = amps[i] * amps[i] * slots[i].r.takeIf { it.isFinite() }.let { it ?: 0.0 }
}

object Circuits {
    /** Ab dieser Stromstärke gilt es als Kurzschluss (die Batterie würde heiß) */
    const val SHORT_A = 3.0
    /** Unter dieser Leistung (W) bleibt ein Lämpchen dunkel */
    const val GLOW_W = 0.01

    private fun series(rs: List<Double>) = rs.sum()
    private fun parallel(rs: List<Double>): Double {
        if (rs.any { it == 0.0 }) return 0.0
        val g = rs.sumOf { if (it.isInfinite()) 0.0 else 1 / it }
        return if (g == 0.0) Double.POSITIVE_INFINITY else 1 / g
    }

    fun solve(layout: Layout, slots: List<Slot>, volts: Double): CircuitResult {
        val n = slots.size
        val amps = MutableList(n) { 0.0 }; val vs = MutableList(n) { 0.0 }
        fun branchR(b: List<Int>) = series(b.map { slots[it].r })
        fun colR(c: List<List<Int>>) = if (c.size == 1) branchR(c[0]) else parallel(c.map(::branchR))
        val total = series(layout.columns.map(::colR))
        // Ideale Batterie: R = 0 → Kurzschluss; die Rechnung nimmt dann 0,1 Ω Innenwiderstand, damit Zahlen endlich bleiben
        val short = total.isFinite() && volts / maxOf(total, 0.1) > SHORT_A
        val i = when { total.isInfinite() -> 0.0; else -> volts / maxOf(total, 0.1) }
        for (c in layout.columns) {
            val rc = colR(c)
            val uc = if (rc.isInfinite()) 0.0 else i * rc
            for (b in c) {
                val rb = branchR(b)
                val ib = when {
                    c.size == 1 -> i
                    rb == 0.0 -> if (rc == 0.0) i / c.count { branchR(it) == 0.0 } else 0.0
                    rb.isInfinite() -> 0.0
                    rc == 0.0 -> 0.0  // ein anderer Zweig überbrückt alles
                    else -> uc / rb
                }
                for (s in b) { amps[s] = ib; vs[s] = if (slots[s].r.isFinite()) ib * slots[s].r else 0.0 }
            }
        }
        return CircuitResult(i, total, amps, vs, short)
    }

    /** Leuchtet die Lampe auf Platz i? */
    fun glows(r: CircuitResult, slots: List<Slot>, i: Int) = slots[i].part.isLoad && !r.short && r.power(i, slots) >= GLOW_W

    /** Helligkeit 0..1, bezogen auf ein einzelnes Lämpchen an derselben Batterie */
    fun brightness(r: CircuitResult, slots: List<Slot>, i: Int, volts: Double): Float {
        if (!slots[i].part.isLoad || r.short) return 0f
        val ref = volts * volts / Part.LAMPE.ohm
        return (r.power(i, slots) / ref).toFloat().coerceIn(0f, 1.2f)
    }

    /** Vergleich mit einer einzelnen Lampe: −1 dunkler, 0 gleich hell (±10 %), 1 heller */
    fun compareToSingle(b: Float) = when { abs(b - 1f) <= 0.1f -> 0; b < 1f -> -1; else -> 1 }

    /** Ohmsches Gesetz */
    fun amps(volts: Double, ohm: Double) = volts / ohm
    fun ohmFor(volts: Double, amps: Double) = volts / amps
}
