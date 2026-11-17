package de.wissensgarten.core.model

/**
 * Text für Brummis natürliche Stimme „Thorsten“ vorbereiten: Emojis und Zeichen weg, Abkürzungen und Rechenzeichen als Wörter.
 * Dieselbe Aufbereitung nutzen App, WebApp und der Abdeckungs-Test – das Werkzeug `tools/stimme/lautschrift.py` sammelt dazu passend die Wörter.
 */
object SprechText {
    private val ABBREV = listOf(
        Regex("""\bz\.\s?B\.""") to "zum Beispiel", Regex("""\bd\.\s?h\.""") to "das heißt", Regex("""\bu\.\s?a\.""") to "unter anderem",
        Regex("""\busw\.""") to "und so weiter", Regex("""\bbzw\.""") to "beziehungsweise", Regex("""\bca\.""") to "circa",
        Regex("""\bv\.\s?Chr\.""") to "vor Christus", Regex("""\bn\.\s?Chr\.""") to "nach Christus", Regex("""\bNr\.""") to "Nummer",
        Regex("""\bS\.(?=\s?\d)""") to "Seite", Regex("""\bu\.\s?v\.\s?m\.""") to "und vieles mehr", Regex("""\betc\.""") to "et cetera",
    )
    private val THOUSANDS = Regex("""(\d)\.(\d{3})(?!\d)""")
    private val NEG = Regex("""(^|[\s(])[-−](?=\d)""")
    private val MINUS = Regex("""(\d)\s?[−-]\s?(?=\d)""")
    private val KEEP = Regex("""[^\p{L}\p{Nd}\s.,!?:;]""")
    private val SPACES = Regex("""\s+""")
    private val SPACE_PUNCT = Regex("""\s+([.,!?:;])""")
    private val PUNCT_RUN = Regex("""([.,!?:;])[.,!?:;]+""")

    fun clean(text: String): String {
        var s = text.replace('\u00A0', ' ').replace('\u202F', ' ').replace('\u2009', ' ')
        ABBREV.forEach { (r, w) -> s = r.replace(s, w) }
        repeat(2) { s = THOUSANDS.replace(s, "$1$2") }
        s = NEG.replace(s) { it.groupValues[1] + "minus " }
        s = MINUS.replace(s) { it.groupValues[1] + " minus " }
        s = s.replace("°C", " Grad Celsius").replace("°", " Grad").replace("%", " Prozent").replace("€", " Euro")
            .replace("+", " plus ").replace("=", " gleich ").replace("×", " mal ").replace("·", " mal ").replace("÷", " durch ")
            .replace("&", " und ").replace("…", ".").replace("–", ",").replace("—", ",").replace("→", ",").replace("(", ",").replace(")", ",")
            .replace("-", " ").replace("/", " ").replace("_", " ")
        s = KEEP.replace(s, " ")
        s = SPACES.replace(s, " ").trim()
        s = SPACE_PUNCT.replace(s, "$1")
        s = PUNCT_RUN.replace(s) { m -> val all = m.value; all.firstOrNull { it in ".!?" }?.toString() ?: all.take(1) }
        return s.trimStart(',', ' ', ';', ':').trim()
    }
}

/**
 * Lautschrift für Brummis natürliche Stimme – ohne Sprach-Baustein in der App: Die Laute wurden vorab erzeugt
 * (`tools/stimme/lautschrift.py`) und liegen als Wörterbuch bei. Ein Text wird Wort für Wort gesetzt;
 * fehlt ein Wort, liefert [of] null – dann liest die Stimme des Geräts diesen Text.
 *
 * @param woerter Zeilen „wort ⇥ Laute“ (Kleinbuchstaben; Abkürzungen in Großbuchstaben wie „DNA“; Zahlen 0 bis 999)
 */
class Lautschrift(woerter: Sequence<String>) {
    private val lex: Map<String, String> = HashMap<String, String>().also { m ->
        woerter.forEach { l -> val i = l.indexOf('\t'); if (i > 0) m[l.substring(0, i)] = l.substring(i + 1) }
    }
    val size get() = lex.size

    private fun number(n: Long): String? = when {
        n < 1000 -> lex[n.toString()]
        n in 1100..1999 && n % 100 != 0L -> year(n) ?: thousands(n)
        else -> thousands(n)
    }
    /** Jahreszahlen wie „neunzehnhundertvierzehn“ */
    private fun year(n: Long): String? = lex[(n / 100).toString()]?.let { h -> lex["hundert"]?.let { hu -> lex[(n % 100).toString()]?.let { "$h $hu $it" } } }
    private fun thousands(n: Long): String? = when {
        n < 1000 -> lex[n.toString()]
        n < 1_000_000 -> { val head = if (n / 1000 == 1L) lex["eintausend"] else number(n / 1000)?.let { a -> lex["tausend"]?.let { "$a $it" } }
            val rest = n % 1000; if (head == null) null else if (rest == 0L) head else number(rest)?.let { "$head $it" } }
        else -> null
    }
    private fun word(token: String): String? {
        if (!token[0].isDigit()) return lex[token] ?: lex[token.lowercase()]
        val parts = token.split(','); val whole = parts[0].toLongOrNull()?.let(::number) ?: return null
        if (parts.size == 1) return whole
        val komma = lex["komma"] ?: return null
        return whole + " " + komma + " " + parts[1].map { lex[it.toString()] ?: return null }.joinToString(" ")
    }

    /** Wörter eines aufbereiteten Textes ([SprechText.clean]), für die es keine Laute gibt (leer = sprechbar) */
    fun missing(clean: String): List<String> = TOKEN.findAll(clean).map { it.value }.filter { it[0].isLetterOrDigit() && word(it) == null }.toList()

    /** Laute je Satz – oder null, wenn ein Wort unbekannt ist (dann liest die Stimme des Geräts) */
    fun of(clean: String): List<String>? {
        val out = ArrayList<String>(); val cur = StringBuilder()
        for (m in TOKEN.findAll(clean)) { val t = m.value
            if (t[0].isLetterOrDigit()) { val w = word(t) ?: return null; if (cur.isNotEmpty() && !cur.endsWith(' ')) cur.append(' '); cur.append(w) }
            else if (cur.isNotEmpty()) {
                while (cur.endsWith(' ')) cur.setLength(cur.length - 1)
                cur.append(t)
                if (t in ".!?") { out += cur.toString(); cur.clear() } else cur.append(' ')
            }
        }
        while (cur.endsWith(' ')) cur.setLength(cur.length - 1)
        if (cur.isNotEmpty()) out += cur.toString()
        return out.takeIf { it.isNotEmpty() }
    }

    companion object {
        /** Wörter (auch mit Akzenten wie „Café“ oder griechische Buchstaben), Zahlen mit Komma, Satzzeichen */
        val TOKEN = Regex("""\p{L}+|\d+(?:,\d+)?|[.,!?:;]""")
    }
}

/**
 * Laute → Zahlenfolge für das Piper-Modell („Thorsten“): Anfang „^“, nach jedem Laut eine Pause „_“, Ende „$“ –
 * genau wie piper (phoneme_ids.py). Die Zuordnung steht in `tokens.txt` der Stimme.
 */
object PiperIds {
    /** Zeilen „Zeichen Nummer“ (das Leerzeichen steht als „  3“ drin) → Zeichen-Nummer (Unicode) zu Modell-Nummer */
    fun tokens(lines: Sequence<String>): Map<Int, Int> = buildMap {
        lines.forEach { l -> val i = l.lastIndexOf(' '); if (i <= 0) return@forEach
            val id = l.substring(i + 1).trim().toIntOrNull() ?: return@forEach
            val sym = l.substring(0, i); if (sym.isEmpty()) return@forEach
            put(codepoints(sym).first(), id) }
    }

    fun encode(sentence: String, map: Map<Int, Int>): IntArray {
        val pad = map['_'.code] ?: 0; val out = ArrayList<Int>()
        map['^'.code]?.let { out += it }; out += pad
        for (cp in codepoints(sentence)) { val id = map[cp] ?: continue; out += id; out += pad }
        map['$'.code]?.let { out += it }
        return out.toIntArray()
    }

    /** Text als Unicode-Zeichennummern (Ersatzpaare zusammengefasst) */
    fun codepoints(s: String): IntArray { val out = ArrayList<Int>(); var i = 0
        while (i < s.length) { val c = s[i]
            if (c.isHighSurrogate() && i + 1 < s.length) { out += 0x10000 + ((c.code - 0xD800) shl 10) + (s[i + 1].code - 0xDC00); i += 2 } else { out += c.code; i++ } }
        return out.toIntArray() }
}
