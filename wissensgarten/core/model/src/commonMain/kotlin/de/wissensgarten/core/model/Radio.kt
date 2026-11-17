package de.wissensgarten.core.model

/**
 * Brummi-Radio: Lernen nur mit Ohren und Stimme (Auto, Schulweg, Einschlafen).
 * Hier steckt nur die Logik: was Brummi sagt und wie gesprochene Antworten verstanden werden.
 */
object Radio {
    enum class Mode {
        /** Neue Karte: Brummi sagt Frage und Antwort, das Kind spricht nach */
        LEARN,
        /** Kurze Antwort frei sagen */
        FREE,
        /** Antwort aus A, B, C (D) wählen */
        CHOICE,
        /** Lange Antwort ohne Auswahl: kurz nachdenken, Antwort hören, „ja“/„nein“ */
        SELF,
    }

    data class Question(val card: StudyCard, val mode: Mode, val options: List<String> = emptyList())

    sealed interface Reply {
        data object Correct : Reply
        data class Wrong(val heard: String) : Reply
        data object DontKnow : Reply
        data object Repeat : Reply
        data object Stop : Reply
        data object Yes : Reply
        data object No : Reply
        data object Unclear : Reply
    }

    private val LETTERS = listOf("A", "B", "C", "D")

    fun question(card: StudyCard, peers: List<String>): Question = when {
        card.memory.isSeed -> Question(card, Mode.LEARN)
        Games.speakPossible(card.back) -> Question(card, Mode.FREE)
        else -> Games.options(card, peers)?.take(4)?.let { opts ->
            // richtige Antwort sicher dabei, Reihenfolge fest je Karten-Inhalt (auf jedem Gerät gleich → vorab vertont)
            val withAnswer = (if (card.back in opts) opts else opts.dropLast(1) + card.back)
            Question(card, Mode.CHOICE, withAnswer.sortedBy { (it + card.front).hashCode() })
        } ?: Question(card, Mode.SELF)
    }

    /** Was Brummi vorliest. */
    fun prompt(q: Question): String = when (q.mode) {
        Mode.LEARN -> "Etwas Neues: ${q.card.front} Die Antwort ist: ${q.card.back}. Sag es mir nach!"
        Mode.FREE -> q.card.front
        Mode.CHOICE -> q.card.front + " " + q.options.mapIndexed { i, o -> "${LETTERS[i]}: $o." }.joinToString(" ")
        Mode.SELF -> q.card.front + " Denk kurz nach …"
    }

    /** Nach der Denkpause bei SELF */
    fun selfReveal(q: Question) = "Die Antwort ist: ${q.card.back}. Wusstest du es? Ja oder nein?"

    private val PRAISE = listOf("Richtig!", "Super!", "Genau!", "Klasse!", "Stimmt!", "Bravo!")
    fun praise(n: Int) = PRAISE[n.mod(PRAISE.size)]

    /** Feste Sätze (ohne Platzhalter) – für die Vorab-Vertonung der Stimme Nova */
    val FIXED: List<String> get() = PRAISE + summary(0, 0)

    fun correction(q: Question) = "Fast! Die Antwort ist: ${q.card.back}."

    /** Zweimal nichts verstanden: ohne Bewertung weiter */
    fun later(q: Question) = "Macht nichts – die heben wir uns für später auf. Die Antwort ist: ${q.card.back}."

    /** Neue Karte nicht richtig nachgesprochen */
    fun again(card: StudyCard) = "Hör noch mal: ${card.back}. Das üben wir bald wieder."

    /**
     * Versteht die Antwort. [heard]: Vorschläge der Spracherkennung, wahrscheinlichster zuerst.
     * Steuerwörter (stopp, nochmal, weiß nicht) gehen vor.
     */
    fun interpret(heard: List<String>, q: Question): Reply {
        // „A“ ist für die Normalisierung ein Artikel – für Buchstaben-Antworten den Rohtext behalten
        val raw = heard.map { nurWorte(it.lowercase()).trim().replace(Regex("\\s+"), " ") }.filter { it.isNotBlank() }
        if (raw.isEmpty()) return Reply.Unclear
        val all = heard.map { SpokenAnswer.normalize(it) }.filter { it.isNotBlank() }.ifEmpty { raw }
        val first = all.first()
        fun has(vararg w: String) = all.any { h -> w.any { h == it || h.startsWith("$it ") || h.endsWith(" $it") || h.contains(" $it ") } }
        // Ist die richtige Antwort selbst ein Steuerwort („Ende“, „weiter“, „was“ …)? Dann zählt sie als Antwort.
        if ((q.mode == Mode.FREE || q.mode == Mode.LEARN) && all.any { SpokenAnswer.matches(it, q.card.back) }) return Reply.Correct
        if (has("stopp", "stop", "aufhören", "beenden", "ende", "schluss")) return Reply.Stop
        if (has("nochmal", "noch mal", "wiederholen", "wie bitte", "was")) return Reply.Repeat
        if (all.any { it.contains("weiß nicht") || it.contains("weiss nicht") || it.contains("keine ahnung") } || has("passe", "weiter")) return Reply.DontKnow

        return when (q.mode) {
            Mode.SELF -> when {
                has("ja", "jo", "jap", "jep", "klar", "yes", "jawohl", "genau") -> Reply.Yes
                has("nein", "nö", "no", "nee", "ne") -> Reply.No
                else -> Reply.Unclear
            }
            Mode.CHOICE -> {
                val idx = raw.firstNotNullOfOrNull { h -> letterIndex(h) }
                    ?: all.firstNotNullOfOrNull { h -> q.options.indexOfFirst { SpokenAnswer.matches(h, it) }.takeIf { it >= 0 } }
                when {
                    idx == null -> Reply.Unclear
                    q.options.getOrNull(idx) == q.card.back -> Reply.Correct
                    else -> Reply.Wrong(raw.first())
                }
            }
            Mode.FREE, Mode.LEARN -> if (all.any { SpokenAnswer.matches(it, q.card.back) }) Reply.Correct else Reply.Wrong(first)
        }
    }

    /** „A“, „be“, „antwort c“, „die zweite“ … → Index */
    private fun letterIndex(h: String): Int? {
        val words = h.split(" ")
        val map = mapOf(
            "a" to 0, "ah" to 0, "eins" to 0, "erste" to 0, "ersten" to 0,
            "b" to 1, "be" to 1, "bee" to 1, "zwei" to 1, "zweite" to 1, "zweiten" to 1,
            "c" to 2, "ce" to 2, "zeh" to 2, "tse" to 2, "drei" to 2, "dritte" to 2, "dritten" to 2,
            "d" to 3, "de" to 3, "dee" to 3, "vier" to 3, "vierte" to 3, "vierten" to 3,
        )
        // nur wenn es kurz ist („A“ / „Antwort B“), sonst ist es eher die Antwort selbst
        if (words.size > 3) return null
        return words.firstNotNullOfOrNull { map[it] }
    }

    /** Bewertung für den Lernstand */
    fun rating(r: Reply, mode: Mode): Rating? = when (r) {
        Reply.Correct -> if (mode == Mode.LEARN) Rating.GOOD else Rating.GOOD
        is Reply.Wrong, Reply.DontKnow, Reply.No -> Rating.AGAIN
        Reply.Yes -> Rating.GOOD
        else -> null
    }

    fun summary(correct: Int, total: Int) = when {
        total == 0 -> "Heute gibt es nichts zu gießen. Bis morgen!"
        correct == total -> "Wow, alle $total richtig! Dein Garten blüht. Tschüss und bis bald!"
        correct * 2 >= total -> "Prima, $correct von $total richtig! Dein Garten freut sich. Bis bald!"
        else -> "Gut gemacht – $total Pflanzen gegossen! Beim nächsten Mal sitzt es noch besser. Bis bald!"
    }
}
