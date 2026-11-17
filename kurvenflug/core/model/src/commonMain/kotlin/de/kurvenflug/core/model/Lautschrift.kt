package de.kurvenflug.core.model

/**
 * Lautschrift für Volts natürliche Stimme „Thorsten“ – ohne Sprach-Baustein in der App: Die Laute wurden vorab erzeugt (`tools/stimme/lautschrift.py`)
 * und liegen als Daten bei. Feste Texte stehen als ganze Sätze drin, veränderliche Texte werden Wort für Wort aus dem Wörterbuch gesetzt.
 *
 * @param saetze  Zeilen „Text ⇥ Laute Satz 1|Laute Satz 2“
 * @param woerter Zeilen „wort ⇥ Laute“ (auch die Zahlen 0 bis 999)
 */
class Lautschrift(saetze: Sequence<String>, woerter: Sequence<String>) {
    private val fixed: Map<String, List<String>> = saetze.mapNotNull { l -> val i = l.indexOf('\t'); if (i <= 0) null else l.substring(0, i) to l.substring(i + 1).split('|').filter { it.isNotBlank() } }.toMap()
    private val lex: Map<String, String> = woerter.mapNotNull { l -> val i = l.indexOf('\t'); if (i <= 0) null else l.substring(0, i) to l.substring(i + 1) }.toMap()
    val size get() = fixed.size + lex.size

    private fun number(n: Long): String? = when {
        n < 1000 -> lex[n.toString()]
        n < 1_000_000 -> { val head = if (n / 1000 == 1L) lex["eintausend"] else number(n / 1000)?.let { a -> lex["tausend"]?.let { "$a $it" } }
            val rest = n % 1000; if (head == null) null else if (rest == 0L) head else number(rest)?.let { "$head $it" } }
        else -> null
    }
    private fun word(token: String): String? {
        if (!token[0].isDigit()) return lex[token.lowercase()]
        val parts = token.split(','); val whole = parts[0].toLongOrNull()?.let(::number) ?: return null
        if (parts.size == 1) return whole
        val komma = lex["komma"] ?: return null
        return whole + " " + komma + " " + parts[1].map { lex[it.toString()] ?: return null }.joinToString(" ")
    }

    /** Wörter, für die es keine Laute gibt (leer = der Text ist sprechbar) */
    fun missing(clean: String): List<String> = if (clean in fixed) emptyList() else TOKEN.findAll(clean).map { it.value }.filter { it[0].isLetterOrDigit() && word(it) == null }.toList()

    /** Laute je Satz – oder null, wenn ein Wort unbekannt ist (dann liest die Stimme des Geräts) */
    fun of(clean: String): List<String>? {
        fixed[clean]?.let { return it }
        val out = ArrayList<String>(); val cur = StringBuilder()
        for (m in TOKEN.findAll(clean)) { val t = m.value
            if (t[0].isLetterOrDigit()) { val w = word(t) ?: return null; if (cur.isNotEmpty()) cur.append(' '); cur.append(w) }
            else if (cur.isNotEmpty()) { cur.append(t); if (t in ".!?") { out += cur.toString(); cur.clear() } }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val TOKEN = Regex("""[A-Za-zÄÖÜäöüß]+|\d+(?:,\d+)?|[.,!?:;]""")
        /** Zeichenliste der Stimme (tokens.txt: „Zeichen Nummer“ je Zeile, das Zeichen kann ein Leerzeichen sein) → Nummer je Unicode-Zeichen */
        fun tokens(lines: Sequence<String>): Map<Int, Int> = lines.mapNotNull { l -> val i = l.lastIndexOf(' ')
            if (i <= 0) null else { val sym = l.substring(0, i); val cp = codepoints(sym); if (cp.size != 1) null else l.substring(i + 1).trim().toIntOrNull()?.let { cp[0] to it } } }.toMap()
        /** Eingabe fürs Stimm-Modell (Piper): Anfang, Pause, dann je Laut seine Nummer und eine Pause, zum Schluss Ende – so wie piper/phoneme_ids.py */
        fun ids(sentence: String, tokens: Map<Int, Int>): IntArray {
            val pad = tokens['_'.code] ?: 0; val out = ArrayList<Int>(); out += tokens['^'.code] ?: 1; out += pad
            codepoints(sentence).forEach { c -> tokens[c]?.let { out += it; out += pad } }
            out += tokens['$'.code] ?: 2; return out.toIntArray()
        }
        /** Laute als Zeichen-Nummern, so wie die Stimme sie erwartet */
        fun codepoints(sentence: String): IntArray { val out = ArrayList<Int>(); var i = 0
            while (i < sentence.length) { val c = sentence[i]
                if (c.isHighSurrogate() && i + 1 < sentence.length) { out += 0x10000 + ((c.code - 0xD800) shl 10) + (sentence[i + 1].code - 0xDC00); i += 2 } else { out += c.code; i++ } }
            return out.toIntArray() }
    }
}
