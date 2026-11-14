package de.wissensgarten.core.model

/**
 * Deutsche Sprechsilben für die Silbenmethode der Grundschule („Gi-raf-fe“, „Schmet-ter-ling“).
 * Regeln: Buchstabengruppen wie sch/ch/ck/ph/qu bleiben zusammen, Doppellaute (ei, au, eu, äu, ie, aa …) sind ein Kern.
 * Zwischen zwei Silbenkernen: ein Mitlaut geht zur nächsten Silbe (Ta-fel, Zu-cker), bei mehreren wandert nur der letzte
 * (Fens-ter, Kin-der, Ap-fel). Treffen zwei Kerne direkt aufeinander, wird dazwischen getrennt (Bau-er).
 * Zusammengesetzte Wörter trifft die Regel meist (Erd-bee-re), aber nicht immer – für Kinderwörter reicht das.
 */
object Syllables {
    private val VOWELS = "aeiouäöüy"
    private val VOWEL_GROUPS = listOf("äu", "eu", "ei", "ai", "au", "ie", "ey", "aa", "ee", "oo")
    private val CONSONANT_GROUPS = listOf("sch", "ch", "ck", "ph", "qu", "th")
    /** Lautverbindungen, die eine Silbe beginnen dürfen (Ze-bra, Zi-tro-ne, Schild-krö-te) – bewusst ohne st/sp/pf */
    private val ONSETS = setOf("bl", "br", "dr", "fl", "fr", "gl", "gr", "kl", "kn", "kr", "pl", "pr", "tr", "schl", "schm", "schn", "schr", "schw", "zw")

    /** Teilt ein einzelnes Wort in Silben (Groß-/Kleinschreibung bleibt). */
    fun split(word: String): List<String> {
        if (word.length < 3 || word.any { !it.isLetter() }) return listOf(word)
        val w = word.lowercase()
        // in Laut-Einheiten zerlegen: (Start, Ende, istVokal)
        data class U(val s: Int, val e: Int, val v: Boolean)
        val units = ArrayList<U>()
        var i = 0
        while (i < w.length) {
            val vg = VOWEL_GROUPS.firstOrNull { w.startsWith(it, i) }
            val cg = CONSONANT_GROUPS.firstOrNull { w.startsWith(it, i) }
            when {
                vg != null -> { units += U(i, i + vg.length, true); i += vg.length }
                cg != null -> { units += U(i, i + cg.length, false); i += cg.length }
                else -> { units += U(i, i + 1, w[i] in VOWELS); i++ }
            }
        }
        val nuclei = units.indices.filter { units[it].v }
        if (nuclei.size < 2) return listOf(word)
        val cuts = mutableListOf<Int>()
        for (k in 0 until nuclei.size - 1) {
            val a = nuclei[k]; val b = nuclei[k + 1]
            val between = b - a - 1
            val cutUnit = when (between) {
                0 -> b                 // Bau-er
                1 -> a + 1             // Ta-fel
                else -> {
                    val pair = w.substring(units[b - 2].s, units[b - 1].e)
                    if (pair in ONSETS) b - 2 else b - 1   // Ze-bra · Fens-ter
                }
            }
            cuts += units[cutUnit].s
        }
        val out = ArrayList<String>()
        var start = 0
        for (c in cuts) { out += word.substring(start, c); start = c }
        out += word.substring(start)
        return out.filter { it.isNotEmpty() }
    }

    /** Text in Stücke: (Text, Silbenindex innerhalb des Wortes oder -1 für Nicht-Buchstaben). */
    fun segments(text: String): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        Regex("\\p{L}+|[^\\p{L}]+").findAll(text).forEach { m ->
            val t = m.value
            if (t[0].isLetter()) split(t).forEachIndexed { i, s -> out += s to i } else out += t to -1
        }
        return out
    }

    fun count(word: String) = split(word).size
}
