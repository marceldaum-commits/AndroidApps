package de.kurvenflug.core.model

/**
 * Macht aus einer Sprechblase von Lux einen Text, den eine Sprachausgabe sauber vorlesen kann: Formelzeichen werden zu Wörtern, Emojis fallen weg.
 *
 * Ablauf: erst Häkchen, Integrale und eckige Klammern, dann die Klammer- und Betragsstruktur (Punkt, Funktion, Rechenklammer, Einschub),
 * danach die einzelnen Zeichen und zum Schluss die Satzzeichen. Gemeinsamer Code für Android und WebApp – nur Kotlin-Standardbibliothek.
 */
object Speech {
    /** Probe-Satz in den Einstellungen */
    const val PROBE = "Hallo, ich bin Volt! So klinge ich. Die Gerade y = 2x + 1 trifft die Station bei (3 | 7)."
    /** Weiche Pause: wird am Ende zu einem Komma, wenn dort nicht ohnehin ein Satzzeichen steht. */
    private const val PAUSE = '\u0001'
    /** Geteilt-Zeichen und Fakultät, vorgemerkt, bevor die Klammern aufgelöst werden. */
    private const val DIVIDE = '\u0002'
    private const val FACTORIAL = '\u0003'
    private const val L = "A-Za-zÄÖÜäöüß"
    private val sub = mapOf('₀' to "0", '₁' to "1", '₂' to "2", '₃' to "3", '₄' to "4", '₅' to "5", '₆' to "6", '₇' to "7", '₈' to "8", '₉' to "9",
        '₊' to "+", '₋' to "−", 'ₐ' to "a", 'ₑ' to "e", 'ₓ' to "x", 'ₙ' to "n", 'ₜ' to "t", 'ₖ' to "k", 'ᵢ' to "i")
    private val sup = mapOf('⁰' to "0", '¹' to "1", '²' to "2", '³' to "3", '⁴' to "4", '⁵' to "5", '⁶' to "6", '⁷' to "7", '⁸' to "8", '⁹' to "9",
        '⁺' to "+", '⁻' to "−", 'ᵃ' to "a", 'ᵇ' to "b", 'ᶜ' to "c", 'ᵈ' to "d", 'ᵉ' to "e", 'ⁿ' to "n", 'ᵗ' to "t", 'ˣ' to "x", 'ᵏ' to "k", 'ⁱ' to "i")
    private val subs = "[" + sub.keys.joinToString("") + "]+"
    private val sups = "[" + sup.keys.joinToString("") + "]+"
    private fun low(s: String) = s.map { sub[it] ?: it.toString() }.joinToString("")
    private fun high(s: String) = s.map { sup[it] ?: it.toString() }.joinToString("")

    private val fractions = listOf("⁴⁄₃" to "vier Drittel", "½" to "ein halb", "⅓" to "ein Drittel", "⅔" to "zwei Drittel", "¼" to "ein Viertel", "¾" to "drei Viertel",
        "⅕" to "ein Fünftel", "⅖" to "zwei Fünftel", "⅗" to "drei Fünftel", "⅘" to "vier Fünftel", "⅙" to "ein Sechstel", "⅚" to "fünf Sechstel", "⅛" to "ein Achtel", "⅒" to "ein Zehntel")
    private val words = listOf(
        "%" to " Prozent", "°" to " Grad", "€" to " Euro", "‴" to " drei Strich ", "″" to " zwei Strich ", "&" to " und ", "′" to " Strich ",
        "≈" to " ungefähr ", "≠" to " ungleich ", "≤" to " kleiner gleich ", "≥" to " größer gleich ", "<" to " kleiner als ", ">" to " größer als ", "±" to " plus minus ", "∓" to " minus plus ",
        "∥" to " parallel zu ", "⊥" to " senkrecht zu ", "∞" to " unendlich ", "·" to " mal ", "×" to " Kreuz ", "√" to " Wurzel aus ",
        "π" to " Pi ", "α" to " Alpha ", "β" to " Beta ", "γ" to " Gamma ", "δ" to " Delta ", "Δ" to " Delta ", "ε" to " Epsilon ", "λ" to " Lambda ", "μ" to " mü ", "σ" to " Sigma ",
        "φ" to " Phi ", "ϕ" to " Phi ", "θ" to " Theta ", "ω" to " Omega ",
    )
    private val functions = setOf("ln", "lg", "log", "exp", "sin", "cos", "tan")
    private val names = "[A-Z][₀-₉′″]?(?:(?:,| und) [A-Z][₀-₉′″]?)*"
    private val number = "−?[0-9]+(?:,[0-9]+)?"

    fun clean(text: String): String {
        var s = text.replace("\u20D7", "").replace('\u00A0', ' ').replace('\u202F', ' ')
        // Geteilt-Zeichen steht frei zwischen zwei Rechenteilen – ein Doppelpunkt als Satzzeichen klebt am Wort davor
        s = s.replace(" : ", " $DIVIDE ")
        s = units(s)
        // f'(x) mit einfachem Apostroph wie f′(x)
        s = Regex("""([A-Za-z])('{1,3})(?=\()""").replace(s) { it.groupValues[1] + listOf("′", "″", "‴")[it.groupValues[2].length - 1] }
        s = checks(s)
        fractions.forEach { (a, b) -> s = s.replace(a, " $b ") }
        s = Regex("($sups)⁄($subs)").replace(s) { " ${high(it.groupValues[1])} durch ${low(it.groupValues[2])} " }
        // Integral mit Grenzen, Stammfunktion in eckigen Klammern, Intervall
        s = Regex("∫($subs)($sups)").replace(s) { " Integral von ${low(it.groupValues[1])} bis ${high(it.groupValues[2])} über " }.replace("∫", " Integral über ")
        s = Regex("""\[([^\[\];]+); ?([^\[\];]+)\]""").replace(s) { "${it.groupValues[1]} bis ${it.groupValues[2]}" }
        s = Regex("\\]($subs)($sups)").replace(s) { "$PAUSE in den Grenzen von ${low(it.groupValues[1])} bis ${high(it.groupValues[2])}$PAUSE" }.replace("[", " ").replace("]", " ")
        s = Regex("([0-9])!(?= [$DIVIDE·]|\\))").replace(s) { "${it.groupValues[1]}$FACTORIAL" }
        // „95-%-Bereich“, „15°-Schritte“: als ein Wort mit Bindestrichen
        s = Regex("""([0-9])-? ?%-""").replace(s) { it.groupValues[1] + "-Prozent-" }
        s = Regex("""([0-9])-? ?°-""").replace(s) { it.groupValues[1] + "-Grad-" }
        // Flächen- und Raummaße: „4 m²“ sind Quadratmeter, nicht „m hoch 2“
        s = Regex("""([0-9]) ?(mm|cm|dm|m|km)([²³])(?![${L}])""").replace(s) { m ->
            "${m.groupValues[1]} " + (if (m.groupValues[3] == "²") "Quadrat" else "Kubik") + mapOf("mm" to "millimeter", "cm" to "zentimeter", "dm" to "dezimeter", "m" to "meter", "km" to "kilometer")[m.groupValues[2]]
        }
        s = render(s).replace(DIVIDE.toString(), " durch ").replace(FACTORIAL.toString(), " Fakultät ")
        s = Regex(sups).replace(s) { " hoch ${high(it.value)} " }
        s = low(s)
        // Pfeil: zwischen zwei einzelnen Zahlen „aus 1 wird minus 1“, sonst eine Folgerung
        s = Regex("($number) → ($number)").replace(s) { m ->
            val before = s.substring(0, m.range.first).trimEnd(); val after = s.substring(m.range.last + 1).trimStart()
            val alone = (before.isEmpty() || before.last() in ":.,;!?" || before.last().isLetter() && !before.endsWith(" von")) && (after.isEmpty() || after.first() in ".,;!?$PAUSE")
            if (alone) "aus ${m.groupValues[1]} wird ${m.groupValues[2]}" else "${m.groupValues[1]}$PAUSE also ${m.groupValues[2]}"
        }
        s = s.replace("→", "$PAUSE also ").replace("⇒", "$PAUSE also ")
        words.forEach { (a, b) -> s = s.replace(a, b) }
        s = Regex("""(^|[^${L}0-9])-(?=[0-9])""").replace(s) { it.groupValues[1] + " minus " }
        s = Regex("""([${L}0-9])[–—]([${L}0-9])""").replace(s) { it.groupValues[1] + " " + it.groupValues[2] }
        s = s.replace("−", " minus ").replace("–", "$PAUSE ").replace("—", "$PAUSE ").replace("^", " hoch ").replace("+", " plus ").replace("=", " gleich ")
        // Schrägstrich: zwischen Wörtern „oder“ (rechts/links), sonst geteilt (π/2, x²/9)
        s = Regex("""([$L]{2,})/([$L]{2,})""").replace(s) { "${it.groupValues[1]} oder ${it.groupValues[2]}" }.replace("/", " durch ")
        s = Regex("""(^|[^$L])(sin|cos|tan)(?![$L])""").replace(s) { it.groupValues[1] + mapOf("sin" to "Sinus", "cos" to "Kosinus", "tan" to "Tangens")[it.groupValues[2]] }
        return tidy(s)
    }

    /** Physik-Einheiten mit Schrägstrich (Kurvenflug): „3 m/s²“ sind Meter pro Sekunde zum Quadrat, nicht „m durch s hoch 2“. */
    private fun units(text: String): String {
        // Live-Rechnung: „… ≈ 4  ·  Station: 4“ – der weite Punkt trennt zwei Sätze; „Bahn 1 → 5“ heißt „Bahn 1 ergibt 5“
        var s = text.replace("  ·  ", ". ")
        s = Regex("""(Bahn [0-9]+|Sonde) → """).replace(s) { it.groupValues[1] + " ergibt " }
        // Einheiten nach einer Zahl: „1,01 s“, „5 m“, „3 kg“, „12 N“
        s = Regex("""([0-9]) (s|m|cm|km|kg|N|h)(?=$|[ .,;:!?)])""").replace(s) { m ->
            val one = s.substring(0, m.range.first + 1).let { b -> Regex("""(^|[^0-9,])1$""").containsMatchIn(b) }
            val w = mapOf("s" to listOf("Sekunde", "Sekunden"), "m" to listOf("Meter", "Meter"), "cm" to listOf("Zentimeter", "Zentimeter"), "km" to listOf("Kilometer", "Kilometer"),
                "kg" to listOf("Kilogramm", "Kilogramm"), "N" to listOf("Newton", "Newton"), "h" to listOf("Stunde", "Stunden"))[m.groupValues[2]]!!
            m.groupValues[1] + " " + (if (one) w[0] else w[1])
        }
        if ('/' !in s) return s
        listOf("m/s²" to "Meter pro Sekunde zum Quadrat", "m/s" to "Meter pro Sekunde", "km/h" to "Kilometer pro Stunde", "km/s" to "Kilometer pro Sekunde")
            .forEach { (u, w) -> s = Regex("""(^|[0-9 (])${Regex.escape(u)}(?![$L])""").replace(s) { it.groupValues[1] + " " + w + " " } }
        return s
    }

    /** Häkchen und Kreuzchen: „B ✗“ heißt „B stimmt noch nicht“, nach einer Rechnung heißt ✓ „stimmt“ – sonst sagt es der Satz schon selbst. */
    private fun checks(text: String): String {
        if ('✓' !in text && '✗' !in text && '✔' !in text && '✘' !in text) return text
        var s = text.replace('✔', '✓').replace('✘', '✗')
        fun verb(list: String, tail: String) = list + (if (", " in list || " und " in list) " stimmen" else " stimmt") + tail
        s = Regex("(^|[–.] +|✓ +)($names) ✗").replace(s) { m -> m.groupValues[1] + verb(m.groupValues[2], " noch nicht") }
        s = Regex("(^|[–.] +)($names) ✓\\s*").replace(s) { m ->
            val rest = s.substring(m.range.last + 1)
            m.groupValues[1] + verb(m.groupValues[2], "") + if (rest.isEmpty()) "" else if (rest.first().isUpperCase()) ". " else " "
        }
        s = Regex("""([0-9]|\|[^()|]*\)) ?✓""").replace(s) { m -> "${m.groupValues[1]}$PAUSE stimmt$PAUSE" }
        return s.replace(" ✓", "").replace(" ✗", "").replace("✓", "").replace("✗", "")
    }

    /** Ende der Klammer, die bei [open] aufgeht – oder -1, wenn sie nie zugeht. */
    private fun closing(s: String, open: Int): Int {
        var depth = 0
        for (i in open until s.length) { if (s[i] == '(') depth++ else if (s[i] == ')') { depth--; if (depth == 0) return i } }
        return -1
    }
    /** Zerlegt an den Strichen der obersten Ebene (Striche in inneren Klammern zählen nicht). */
    private fun pieces(s: String): List<String> {
        val out = ArrayList<String>(); var depth = 0; var from = 0
        s.forEachIndexed { i, c -> if (c == '(') depth++ else if (c == ')') depth-- else if (c == '|' && depth == 0) { out += s.substring(from, i); from = i + 1 } }
        out += s.substring(from); return out
    }
    /** Der Text ohne innere Klammern – daran entscheidet sich, ob es Rechnung oder Einschub ist. */
    private fun top(s: String): String { val out = StringBuilder(); var depth = 0; s.forEach { c -> if (c == '(') depth++ else if (c == ')') depth-- else if (depth == 0) out.append(c) }; return out.toString() }
    private fun single(s: String) = s.trim().let { t -> t.isNotEmpty() && t.withIndex().all { (i, c) -> c.isLetterOrDigit() || c in ",′″‴" || c in sub || c in sup || c == FACTORIAL || (i == 0 && c in "−-+") } }
    private val word = Regex("[$L]{3,}")
    private fun aside(inner: String): Boolean {
        val t = top(inner)
        return t.any { it in "=<>≤≥≈≠→!?" } || ": " in t || ", " in t || ". " in t || word.findAll(t).any { it.value.lowercase() !in functions }
    }

    /** Löst Klammern und Betragsstriche auf; alle übrigen Zeichen bleiben für später stehen. */
    private fun render(s: String): String {
        val out = StringBuilder(); var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '|') {
                var end = -1; var depth = 0; var j = i + 1
                while (j < s.length) { val d = s[j]; if (d == '(') depth++ else if (d == ')') depth-- else if (d == '|' && depth == 0) { end = j; break }; j++ }
                val inner = if (end > 0) s.substring(i + 1, end) else ""
                if (inner.isNotBlank() && inner.first() != ' ' && inner.last() != ' ') {
                    out.append(" Betrag von ").append(render(inner)); if (!single(inner)) out.append(PAUSE) else out.append(' ')
                    i = end + 1
                } else { out.append(if (s.getOrNull(i - 1) == ' ' && s.getOrNull(i + 1) == ' ') " unter der Bedingung " else ", "); i++ }
                continue
            }
            if (c == ')') { out.append(PAUSE); i++; continue }
            if (c != '(') { out.append(c); i++; continue }
            val end = closing(s, i)
            if (end < 0) { out.append(PAUSE); i++; continue }
            val inner = s.substring(i + 1, end); val parts = pieces(inner); val before = s.getOrNull(i - 1)
            var k = i; while (k > 0 && (s[k - 1].isLetter() && s[k - 1] !in sup || s[k - 1] in "′″‴" || s[k - 1] in sub)) k--
            val name = s.substring(k, i); val letters = name.filter { it.isLetter() }
            val function = letters.isNotEmpty() && (letters.length <= 2 || letters.lowercase() in functions)
            // „2(x + 1)“ und „(x − 3)(x + 1)“: das Mal dazwischen wird mitgesprochen
            val times = if (before != null && (before.isDigit() || before == ')') && parts.size == 1 && !aside(inner)) " mal" else ""
            when {
                parts.size > 1 && parts.none { it.isBlank() } ->
                    if (name == "P" && parts.size == 2 && parts.all { it.trim().first().isUpperCase() }) out.append(" von ").append(render(parts[0].trim())).append(" unter der Bedingung ").append(render(parts[1].trim())).append(' ')
                    else out.append(' ').append(parts.joinToString(", ") { render(it).trim() }).append(' ')
                before == '√' || before == '^' -> out.append(render(inner)).append(PAUSE)
                parts.size == 1 && function -> out.append(" von ").append(render(inner)).append(' ')
                parts.size == 1 && aside(inner) -> out.append(PAUSE).append(' ').append(render(inner)).append(PAUSE)
                parts.size == 1 && single(inner) -> out.append(times).append(' ').append(inner).append(' ')
                else -> out.append(times).append(" Klammer auf ").append(render(inner)).append(" Klammer zu ")
            }
            i = end + 1
            // „(2+6):2“ – geteilt ohne Leerzeichen direkt hinter der Klammer
            if (s.getOrNull(i) == ':' && s.getOrNull(i + 1)?.let { it.isLetterOrDigit() || it in "(−√" } == true) { out.append(" durch "); i++ }
        }
        return out.toString()
    }

    private fun tidy(text: String): String {
        var s = text.replace(PAUSE, ',')
        // Emojis und sonstige Bildzeichen stumm
        s = s.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-/" }
        s = s.replace(Regex("""\s+"""), " ")
        s = s.replace(Regex(""" ?,(?: ?,)+"""), ",").replace(Regex(""" +([.,;:!?])"""), "$1")
        s = s.replace(Regex("""([.;:!?]),+"""), "$1").replace(Regex(""",+([.;:!?])(?![0-9])"""), "$1").replace(Regex("""([.!?])\.(?![0-9])"""), "$1")
        s = s.replace(Regex(""",(?=[^ 0-9])"""), ", ")
        s = s.replace(", durch ", ", geteilt durch ").replace(Regex(""", geteilt durch ([^ ,]+) gleich """), ", geteilt durch $1, gleich ")
        return s.trim(' ', ',')
    }
}
