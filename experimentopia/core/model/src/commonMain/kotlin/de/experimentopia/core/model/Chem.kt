package de.experimentopia.core.model

/** Chemische Elemente mit Kugelmodell-Farbe (übliche CPK-Farben) */
enum class Element(val symbol: String, val title: String, val color: Long) {
    H("H", "Wasserstoff", 0xFFF2F2F2), C("C", "Kohlenstoff", 0xFF4A4A4A), N("N", "Stickstoff", 0xFF3F6BFF), O("O", "Sauerstoff", 0xFFFF3B3B),
    Na("Na", "Natrium", 0xFFAB5CF2), Cl("Cl", "Chlor", 0xFF3DDC3D), Mg("Mg", "Magnesium", 0xFF8AFF00), Fe("Fe", "Eisen", 0xFFE06633),
    Ca("Ca", "Calcium", 0xFF3DFF00), S("S", "Schwefel", 0xFFFFE135), Cu("Cu", "Kupfer", 0xFFC88033);
    companion object { fun of(s: String) = entries.firstOrNull { it.symbol == s } ?: error("Unbekanntes Element $s") }
}

/** Liest Summenformeln wie „H2O“, „CH3COOH“, „Fe2O3“ oder „Ca(OH)2“ und zählt die Atome. */
object Formula {
    fun atoms(f: String): Map<Element, Int> {
        var i = 0
        fun number(): Int { val s = i; while (i < f.length && f[i].isDigit()) i++; return if (s == i) 1 else f.substring(s, i).toInt() }
        fun group(): Map<Element, Int> {
            val m = mutableMapOf<Element, Int>()
            while (i < f.length && f[i] != ')') {
                if (f[i] == '(') { i++; val inner = group(); i++; val k = number(); inner.forEach { (e, n) -> m[e] = (m[e] ?: 0) + n * k } }
                else {
                    val s = i; i++
                    while (i < f.length && f[i].isLowerCase()) i++
                    val e = Element.of(f.substring(s, i)); val k = number(); m[e] = (m[e] ?: 0) + k
                }
            }
            return m
        }
        return group()
    }
    fun count(side: List<String>, coeffs: List<Int>): Map<Element, Int> {
        val m = mutableMapOf<Element, Int>()
        side.forEachIndexed { k, f -> atoms(f).forEach { (e, n) -> m[e] = (m[e] ?: 0) + n * coeffs[k] } }
        return m
    }
    /** Schöne Schreibweise mit tiefgestellten Zahlen: H₂O */
    fun pretty(f: String) = f.map { if (it.isDigit()) "₀₁₂₃₄₅₆₇₈₉"[it - '0'] else it }.joinToString("")
}

/** Wie die Reaktion im Labor aussieht, wenn die Gleichung stimmt */
enum class Effect { KNALL, FLAMME, BLITZ, ROST, SPRUDEL, SCHAUM, BLATT, KRISTALL, NEBEL }

data class Molecule(val formula: String, val name: String)

/** Eine Reaktion mit der kleinsten richtigen Ausgleichung [coeffs] (erst links, dann rechts). */
data class Reaction(val id: String, val title: String, val emoji: String, val left: List<Molecule>, val right: List<Molecule>, val coeffs: List<Int>, val effect: Effect, val story: String) {
    val all get() = left + right
    fun balanced(c: List<Int>): Boolean = c.all { it > 0 } &&
        Formula.count(left.map { it.formula }, c.take(left.size)) == Formula.count(right.map { it.formula }, c.drop(left.size))
    /** Ausgeglichen, aber nicht mit den kleinsten Zahlen (z. B. 4 H₂ + 2 O₂ → 4 H₂O) */
    fun notSmallest(c: List<Int>) = balanced(c) && c.reduce(::gcd) > 1
    fun equation(c: List<Int> = coeffs) = left.indices.joinToString(" + ") { k -> co(c[k]) + Formula.pretty(left[k].formula) } + " → " +
        right.indices.joinToString(" + ") { k -> co(c[left.size + k]) + Formula.pretty(right[k].formula) }
    private fun co(n: Int) = if (n == 1) "" else "$n "
    companion object { fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b) }
}

object Reactions {
    private fun m(f: String, n: String) = Molecule(f, n)
    val all = listOf(
        Reaction("knallgas", "Knallgas", "💥", listOf(m("H2", "Wasserstoff"), m("O2", "Sauerstoff")), listOf(m("H2O", "Wasser")), listOf(2, 1, 2), Effect.KNALL,
            "Wasserstoff verbrennt mit lautem Knall – und heraus kommt nur Wasser!"),
        Reaction("kohle", "Kohle verbrennt", "🔥", listOf(m("C", "Kohlenstoff"), m("O2", "Sauerstoff")), listOf(m("CO2", "Kohlenstoffdioxid")), listOf(1, 1, 1), Effect.FLAMME,
            "Grillkohle glüht: Kohlenstoff und Sauerstoff werden zu Kohlenstoffdioxid."),
        Reaction("magnesium", "Magnesium-Blitz", "⚡", listOf(m("Mg", "Magnesium"), m("O2", "Sauerstoff")), listOf(m("MgO", "Magnesiumoxid")), listOf(2, 1, 2), Effect.BLITZ,
            "Magnesium brennt so grell, dass man nicht hinschauen darf – früher gab es so Blitzlicht."),
        Reaction("erdgas", "Erdgas-Flamme", "🔵", listOf(m("CH4", "Methan"), m("O2", "Sauerstoff")), listOf(m("CO2", "Kohlenstoffdioxid"), m("H2O", "Wasser")), listOf(1, 2, 1, 2), Effect.FLAMME,
            "Die blaue Flamme am Gasherd: Methan verbrennt."),
        Reaction("rost", "Eisen rostet", "🟤", listOf(m("Fe", "Eisen"), m("O2", "Sauerstoff")), listOf(m("Fe2O3", "Rost (Eisenoxid)")), listOf(4, 3, 2), Effect.ROST,
            "Rosten ist eine ganz langsame Verbrennung."),
        Reaction("salz", "Kochsalz entsteht", "🧂", listOf(m("Na", "Natrium"), m("Cl2", "Chlor")), listOf(m("NaCl", "Natriumchlorid")), listOf(2, 1, 2), Effect.KRISTALL,
            "Ein weiches Metall und ein giftiges Gas werden zu Salz, das wir essen!"),
        Reaction("neutral", "Säure trifft Lauge", "⚖️", listOf(m("HCl", "Salzsäure"), m("NaOH", "Natronlauge")), listOf(m("NaCl", "Kochsalz"), m("H2O", "Wasser")), listOf(1, 1, 1, 1), Effect.NEBEL,
            "Neutralisation: Säure und Lauge heben sich auf – übrig bleiben Salz und Wasser."),
        Reaction("natron", "Natron + Essig", "💨", listOf(m("NaHCO3", "Natron"), m("CH3COOH", "Essigsäure")), listOf(m("CH3COONa", "Natriumacetat"), m("H2O", "Wasser"), m("CO2", "Kohlenstoffdioxid")), listOf(1, 1, 1, 1, 1), Effect.SPRUDEL,
            "Es sprudelt: Das Gas Kohlenstoffdioxid entsteht."),
        Reaction("zahnpasta", "Elefantenzahnpasta", "🐘", listOf(m("H2O2", "Wasserstoffperoxid")), listOf(m("H2O", "Wasser"), m("O2", "Sauerstoff")), listOf(2, 2, 1), Effect.SCHAUM,
            "Wasserstoffperoxid zerfällt blitzschnell – der Sauerstoff macht riesige Schaumberge."),
        Reaction("grill", "Gasgrill (Propan)", "🍖", listOf(m("C3H8", "Propan"), m("O2", "Sauerstoff")), listOf(m("CO2", "Kohlenstoffdioxid"), m("H2O", "Wasser")), listOf(1, 5, 3, 4), Effect.FLAMME,
            "Propan aus der Gasflasche verbrennt."),
        Reaction("ammoniak", "Ammoniak (Dünger)", "🌾", listOf(m("N2", "Stickstoff"), m("H2", "Wasserstoff")), listOf(m("NH3", "Ammoniak")), listOf(1, 3, 2), Effect.NEBEL,
            "Aus Luft-Stickstoff wird Dünger – das ernährt heute Milliarden Menschen."),
        Reaction("fotosynthese", "Fotosynthese", "🌿", listOf(m("CO2", "Kohlenstoffdioxid"), m("H2O", "Wasser")), listOf(m("C6H12O6", "Traubenzucker"), m("O2", "Sauerstoff")), listOf(6, 6, 1, 6), Effect.BLATT,
            "Pflanzen bauen mit Sonnenlicht Zucker – und schenken uns Sauerstoff."),
    )
    fun byId(id: String) = all.first { it.id == id }
}

/**
 * Ballon-Versuch: Natron + Essig → Kohlenstoffdioxid. Das Gas kann nur so lange entstehen, wie von beidem etwas da ist
 * (der Stoff, der zuerst aufgebraucht ist, bestimmt die Menge).
 */
object Fizz {
    const val NATRON_G_PER_SPOON = 5.0     // 1 Teelöffel ≈ 5 g
    const val NATRON_M = 84.01             // g/mol
    const val ACETIC_M = 60.05             // g/mol
    const val VINEGAR_FRACTION = 0.05      // Haushaltsessig: 5 g Essigsäure je 100 mL
    const val MOLAR_L = 24.0               // 1 mol Gas ≈ 24 L bei 20 °C

    fun natronMol(spoons: Double) = spoons * NATRON_G_PER_SPOON / NATRON_M
    fun acidMol(vinegarMl: Double) = vinegarMl * VINEGAR_FRACTION / ACETIC_M
    /** Gas in Litern (ohne Verluste) */
    fun gasL(spoons: Double, vinegarMl: Double) = minOf(natronMol(spoons), acidMol(vinegarMl)) * MOLAR_L
    /** Welcher Stoff ist zuerst aufgebraucht? */
    fun limiting(spoons: Double, vinegarMl: Double) = if (natronMol(spoons) <= acidMol(vinegarMl)) "Natron" else "Essig"
}
