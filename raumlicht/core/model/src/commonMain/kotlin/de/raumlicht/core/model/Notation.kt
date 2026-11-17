package de.raumlicht.core.model

/**
 * Zwei Schreibweisen für das Rechnen mit Punkten:
 * - **kurz** (Voreinstellung, so steht es in vielen Schulheften): „v⃗ = B − A“, „M = ½ · (A + B)“, „g: x⃗ = A + t · d⃗“
 * - **streng** (mit Ortsvektoren): „v⃗ = OB⃗ − OA⃗“, „OM⃗ = ½ · (OA⃗ + OB⃗)“, „g: x⃗ = OA⃗ + t · d⃗“
 * Umgestellt wird in den Einstellungen ([Prefs.strictVectors]). [strict] schreibt einen fertigen Text um; Koordinaten wie „A(1|2|3)“,
 * Funktionen wie „F(3) − F(0)“ und Zahlen-Größen wie „V = 9π“ bleiben, wie sie sind.
 */
object Notation {
    /** Ein Punktname: ein Großbuchstabe, auch mit Strich oder Index – aber nicht Teil eines Wortes, einer Funktion „F(…)“ oder eines Vektors „AB⃗“ */
    private const val T = """(?<![A-Za-zÄÖÜäöüß⃗′″₀-₉])[A-Z][′″₁₂₃₄]?(?![A-Za-zÄÖÜäöüß⃗′″₀-₉(])"""
    private const val M = "\u0001"   // Marke für „schon umgeschrieben“
    private val pair = Regex("""($T) ([+−]) ($T)""")
    /** Punkt plus/minus ein Vektor: „A + t · d⃗“, „P + 2 · PF⃗“, „A + BC⃗“, „P + t · (1|2|2)“, „… − t · d⃗“ */
    private val plusVector = Regex("""($T)(?= [+−] (?:[^\s=]{1,6} · )?(?:[A-Za-z]{1,2}[′₁₂₃]?⃗|\([^()|]*\|))""")
    /** dritter Summand einer Kette („OA⃗ + OB⃗ + C“) und Punkt nach einem Vektor („BC⃗ + A“) */
    private val afterPair = Regex("""(?<=⃗ [+−] )($T)""")
    /** rechte Seite, die nur aus einem Punkt besteht: „OA⃗ + r · v⃗ = Z“ */
    private val right = Regex("""( = )($T)(?=$|[.,;) ])""")
    private val left = Regex("""($T)( = )""")
    private val columnSum = Regex("""\([^()|]*\|[^()]*\) [+−] """)

    fun strict(text: String): String {
        if (text.none { it in 'A'..'Z' }) return text
        var s = text
        // Summen und Differenzen von Punkten – auch Ketten wie „A + B + C“ (deshalb wiederholt)
        repeat(4) { s = pair.replace(s) { m -> mark(m.groupValues[1]) + " " + m.groupValues[2] + " " + mark(m.groupValues[3]) }; s = afterPair.replace(s) { mark(it.value) } }
        s = plusVector.replace(s) { mark(it.value) }
        // linke Seite: „M = ½ · (OA⃗ + OB⃗)“ → „OM⃗ = …“; „S = (1|1|1) + 2 · (2|1|1)“ → „OS⃗ = …“
        s = left.replace(s) { m -> val rest = s.substring(m.range.last + 1).substringBefore(" = ").substringBefore(" →").substringBefore(", ")
            if (M in rest || columnSum.containsMatchIn(rest)) mark(m.groupValues[1]) + m.groupValues[2] else m.value }
        s = right.replace(s) { m -> val before = s.substring(0, m.range.first).substringAfterLast(" = ").substringAfterLast(": ")
            if (M in before) m.groupValues[1] + mark(m.groupValues[2]) else m.value }
        return s.replace(M, "")
    }
    /** „B“ → „OB⃗“ (mit Marke, damit es nicht noch einmal angefasst wird) */
    private fun mark(point: String) = if (point.startsWith(M)) point else "${M}O$point⃗"
}

/** Dieselbe Aufgabe mit umgeschriebenen Texten (Auftrag, Tipp, Rechenweg, Formelzeilen, Rückmeldungen) – z. B. für die strenge Vektorschreibweise */
fun Level.withText(f: (String) -> String): Level = Level(id, chapter, title, f(mission), bounds, controls, solution, f(tip), way.map(f), abi, handle,
    finale = finale?.let { fin -> { v: Values -> fin(v).let { it.copy(acts = it.acts.map { a -> a.copy(say = f(a.say)) }) } } },
    formula = { v -> formula(v).map { row -> row.map { p -> if (p is Part.Txt) Part.Txt(f(p.s)) else p } } },
    objects = objects, evaluate = { v -> evaluate(v).let { it.copy(message = f(it.message)) } })
