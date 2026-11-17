package de.experimentopia.core.model

/**
 * Macht aus einem App-Text etwas, das eine Sprachausgabe sauber vorlesen kann:
 * chemische Formeln (H₂O → „H 2 O“), Einheiten (m/s² → „Meter pro Sekunde zum Quadrat“), Rechenzeichen, Abkürzungen; Emojis fallen weg.
 */
object Speech {
    private val SUB = mapOf('₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4', '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9')
    private val SUP = mapOf('⁰' to "0", '¹' to "1", '²' to "2", '³' to "3", '⁴' to "4", '⁵' to "5", '⁶' to "6", '⁷' to "7", '⁸' to "8", '⁹' to "9", '⁻' to "minus ", '⁺' to "plus ")
    /** Elementsymbole, die ohne Index als Formel gelesen werden (sonst wären es Wörter) */
    private val FORMULAS = setOf("NaOH", "HCl", "NaCl", "MgO", "CuO", "FeS", "Mg", "Fe", "Cu", "Al", "Na", "Cl", "Zn", "Hg", "Au", "Ag", "CO", "Rf")

    /** Einheiten hinter einer Zahl – längere zuerst, damit „mol/L“ nicht als „mol“ endet */
    private val UNITS = listOf(
        "J/(g·K)" to "Joule pro Gramm und Kelvin", "W/(m·K)" to "Watt pro Meter und Kelvin", "m/s²" to "Meter pro Sekunde zum Quadrat", "g/cm³" to "Gramm pro Kubikzentimeter",
        "kg/m³" to "Kilogramm pro Kubikmeter", "mol/L" to "Mol pro Liter", "km/h" to "Kilometer pro Stunde", "m/s" to "Meter pro Sekunde", "N/kg" to "Newton pro Kilogramm",
        "N·m" to "Newtonmeter", "cm³" to "Kubikzentimeter", "m³" to "Kubikmeter", "m²" to "Quadratmeter", "°C" to "Grad Celsius",
        "kJ" to "Kilojoule", "kg" to "Kilogramm", "km" to "Kilometer", "cm" to "Zentimeter", "mm" to "Millimeter", "nm" to "Nanometer", "mL" to "Milliliter",
        "Hz" to "Hertz", "min" to "Minuten", "mol" to "Mol", "Ω" to "Ohm", "%" to "Prozent", "°" to "Grad",
        "m" to "Meter", "g" to "Gramm", "L" to "Liter", "s" to "Sekunden", "h" to "Stunden", "N" to "Newton", "J" to "Joule", "W" to "Watt", "V" to "Volt", "A" to "Ampere", "K" to "Kelvin",
    )
    private val SINGULAR = mapOf("Sekunden" to "Sekunde", "Minuten" to "Minute", "Stunden" to "Stunde")
    private val ABBR = listOf("z. B." to "zum Beispiel", "u. a." to "unter anderem", "d. h." to "das heißt", "bzw." to "beziehungsweise", "ca." to "zirka", "usw." to "und so weiter",
        "Kl." to "Klasse", "Nr." to "Nummer", "pH" to "p H", "Rf-Wert" to "R f Wert")
    private val SIGNS = listOf("≈" to " ungefähr ", "≠" to " ungleich ", "≤" to " kleiner gleich ", "≥" to " größer gleich ", "→" to " ergibt ", "⇌" to " ergibt ",
        "·" to " mal ", "×" to " mal ", "÷" to " geteilt durch ", "√" to " Wurzel aus ", "π" to " Pi ", "Δ" to " Delta ", "ρ" to " rho ", "∼" to " proportional zu ",
        "½" to " ein halb ", "⅓" to " ein Drittel ", "¼" to " ein Viertel ", "′" to " Strich", "″" to " zwei Strich", "…" to ".", "„" to "", "“" to "", "‚" to "", "‘" to "", "’" to "", "»" to "", "«" to "")

    private val number = """\d+(?:,\d+)?"""

    fun clean(text: String): String {
        var s = " ${text.replace(Typo.NBSP, ' ')} "
        // Tausender mit Leerzeichen zusammenziehen: „100 000“ → „100000“
        s = Regex("""(\d) (\d{3})(?!\d)""").replace(s) { "${it.groupValues[1]}${it.groupValues[2]}" }
        s = Regex("""(\d) (\d{3})(?!\d)""").replace(s) { "${it.groupValues[1]}${it.groupValues[2]}" }
        ABBR.forEach { (a, b) -> s = s.replace(a, b) }
        // Kreuzungen: „AA × aa“ → „A A gekreuzt mit a a“
        s = Regex("""(?<![\p{L}\d])([A-Za-z0]{2}) × ([A-Za-z0]{2})(?![\p{L}\d])""").replace(s) { "${spell(it.groupValues[1])} gekreuzt mit ${spell(it.groupValues[2])}" }
        // Erbanlagen und Blutgruppen-Anlagen allein: Aa, aa, Ss, Rr, A0, B0, 00, AB
        s = Regex("""(?<![\p{L}\d])([A-Za-z0]{2})(?![\p{L}\d])""").replace(s) { m -> if (isAlleles(m.value)) spell(m.value) else m.value }
        // Hochzahlen: 10⁻⁷ → „10 hoch minus 7“, m², cm³ in Einheiten zuerst
        UNITS.forEach { (u, w) ->
            s = Regex("""($number)\s?${Regex.escape(u)}(?![\p{L}\d²³])""").replace(s) { m -> val n = m.groupValues[1]; "$n ${if (n == "1") SINGULAR[w] ?: w else w}" }
        }
        // zusammengesetzte Einheiten auch ohne Zahl davor („Einheit m/s²“)
        UNITS.filter { (u, _) -> u.length >= 3 && (u.contains('/') || u.contains('³') || u.contains('²') || u.contains('·')) }.forEach { (u, w) -> s = s.replace(u, w) }
        s = Regex("""(\d)([⁰¹²³⁴⁵⁶⁷⁸⁹⁻⁺]+)""").replace(s) { m -> m.groupValues[1] + " hoch " + m.groupValues[2].map { SUP[it] ?: "" }.joinToString("").trim() }
        // Chemische Formeln: H₂O, CO₂, Fe₂O₃, H⁺, OH⁻, NaOH …
        s = Regex("""(?<![\p{L}])((?:[A-Z][a-z]?[₀-₉]*)+)([⁺⁻]?)(?![\p{L}])""").replace(s) { m ->
            val f = m.groupValues[1]; val charge = m.groupValues[2]
            val isFormula = f.any { it in SUB } || charge.isNotEmpty() || f in FORMULAS
            if (!isFormula) m.value
            else Regex("""([A-Z][a-z]?)([₀-₉]*)""").findAll(f).joinToString(" ") { e -> spell(e.groupValues[1]) + (if (e.groupValues[2].isNotEmpty()) " " + e.groupValues[2].map { SUB[it] }.joinToString("") else "") } +
                (if (charge == "⁺") " plus" else if (charge == "⁻") " minus" else "")
        }
        // Verhältnisse und Bereiche: „3 : 1“ → „3 zu 1“, „3–4“ → „3 bis 4“
        s = Regex("""(\d) ?: ?(\d)""").replace(s) { "${it.groupValues[1]} zu ${it.groupValues[2]}" }
        s = Regex("""(\d) ?[–-] ?(\d)""").replace(s) { "${it.groupValues[1]} bis ${it.groupValues[2]}" }
        s = Regex("""(\d) ?/ ?(\d)""").replace(s) { "${it.groupValues[1]} durch ${it.groupValues[2]}" }
        SIGNS.forEach { (a, b) -> s = s.replace(a, b) }
        s = s.replace(" = ", " gleich ").replace("=", " gleich ").replace(" + ", " plus ").replace(" − ", " minus ").replace("−", " minus ")
        s = s.replace("–", ", ").replace("—", ", ").replace("%", " Prozent").replace("°", " Grad")
        // Emojis und sonstige Bildzeichen stumm
        s = s.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?()-/'" }
        s = s.replace(" (", ", ").replace("(", " ").replace(")", " ").replace("/", " ").replace("-", " ")
        return s.replace(Regex("""\s+"""), " ").replace(" ,", ",").replace(",,", ",").replace(" .", ".").replace(" !", "!").replace(" ?", "?").replace(" :", ":").trim().trim(',').trim()
    }

    /** Zwei Zeichen einer Erbanlage: gleicher Buchstabe (Aa, aa, SS) oder Blutgruppen-Anlagen (A0, B0, 00, AB) */
    private fun isAlleles(t: String) = (t[0].isLetter() && t[0].lowercaseChar() == t[1].lowercaseChar()) || t in setOf("A0", "B0", "00", "AB")

    /** Buchstabe für Buchstabe: „Na“ → „N a“ */
    private fun spell(w: String) = w.toList().joinToString(" ")
}
