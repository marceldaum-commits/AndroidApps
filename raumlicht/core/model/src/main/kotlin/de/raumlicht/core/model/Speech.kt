package de.raumlicht.core.model

/** Macht aus einer Sprechblase von Lux einen Text, den eine Sprachausgabe sauber vorlesen kann: Formelzeichen werden zu Wörtern, Emojis fallen weg. */
object Speech {
    private val words = listOf(
        "f″" to "f zwei Strich", "f′" to "f Strich", "F′" to "F Strich", "⃗" to "", "≈" to " ungefähr ", "≠" to " ungleich ", "≤" to " kleiner gleich ", "≥" to " größer gleich ",
        "·" to " mal ", "×" to " kreuz ", "−" to " minus ", "–" to ", ", "√" to " Wurzel aus ", "²" to " hoch 2", "³" to " hoch 3", "π" to " Pi ", "∫" to " Integral ", "μ" to " mü ", "σ" to " sigma ",
        "½" to " ein halb ", "⅓" to " ein Drittel ", "¼" to " ein Viertel ", "⅖" to " zwei Fünftel ", "⁴⁄₃" to " vier Drittel ", "°" to " Grad", "%" to " Prozent", "„" to "", "“" to "", "‚" to "", "’" to "", "›" to "", "▶" to "", "✓" to "",
    )
    private val subscripts = mapOf('₀' to "0", '₁' to "1", '₂' to "2", '₃' to "3", '₄' to "4")
    fun clean(text: String): String {
        var s = text
        // Punkte und Vektoren wie (2|3|1) werden als Zahlenfolge gelesen
        s = Regex("""\(([^()|]+(?:\|[^()|]+)+)\)""").replace(s) { m -> " " + m.groupValues[1].split('|').joinToString(", ") { it.trim() } + " " }
        s = s.replace("|", " unter der Bedingung ")
        words.forEach { (a, b) -> s = s.replace(a, b) }
        s = s.map { subscripts[it] ?: it.toString() }.joinToString("")
        s = Regex("""(\d) : (\d)""").replace(s) { "${it.groupValues[1]} durch ${it.groupValues[2]}" }
        s = s.replace("^", " hoch ").replace(" = ", " gleich ").replace(" + ", " plus ").replace("=", " gleich ")
        // Emojis und sonstige Bildzeichen stumm
        s = s.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?()-/" }
        return s.replace(Regex("""\s+"""), " ").replace(" ,", ",").replace(" .", ".").trim()
    }
}
