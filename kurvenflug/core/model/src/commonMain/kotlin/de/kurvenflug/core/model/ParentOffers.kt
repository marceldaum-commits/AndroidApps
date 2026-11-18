package de.kurvenflug.core.model

/**
 * Angebote von „Nachhilfe – online“ in Kurvenflug (Leitstand 18.11., von Marcel freigegeben; UWG Anhang Nr. 28:
 * keine Kaufaufforderung an Kinder). Alles hier erscheint nur im Elternbereich hinter dem Eltern-Tor – nie durch Volt,
 * nie im Spiel – und nennt keine Preise. Anrede der Eltern: du.
 */
object ParentOffers {
    const val SITE = "https://www.mathenachhilfe-online.com"
    const val GIFT = "$SITE/geschenke/?src=kurvenflug-geschenk"
    const val FOOTER = "$SITE/?src=kurvenflug-fusszeile"
    const val FOOTER_TEXT = "Ein Angebot von Nachhilfe – online"
    /** Nachhilfe-Hinweis höchstens an einem Tag pro Woche (danach 6 Tage Pause) */
    const val HINT_PAUSE_DAYS = 7L
    /** „Klassenarbeit bald“ gilt so lange, nachdem die Eltern es angetippt haben */
    const val EXAM_DAYS = 21L

    /** Kostenlose Probestunde zum Thema (Kapitel-Nummer) und zur Klasse */
    fun trialLesson(chapter: Int, grade: Int?): String =
        "$SITE/probestunde/?src=kurvenflug-eltern&thema=kf-$chapter" + (grade?.let { "&klasse=$it" } ?: "")

    /** Warum der Hinweis kommt */
    enum class Reason { STUCK, EXAM }

    /**
     * Soll der Nachhilfe-Hinweis heute erscheinen? Nur wenn ein Auslöser da ist („Thema hakt“ oder „Klassenarbeit bald“),
     * er nicht ausgeblendet ist und er nicht schon an einem der letzten [HINT_PAUSE_DAYS] − 1 Tage zu sehen war
     * (am selben Tag bleibt er sichtbar). [lastShown]/[examUntil]: Tag (epochDay), 0 = nie.
     */
    fun hint(today: Long, stuck: Boolean, examUntil: Long, hidden: Boolean, lastShown: Long): Reason? {
        if (hidden) return null
        val reason = when { examUntil >= today -> Reason.EXAM; stuck -> Reason.STUCK; else -> return null }
        return if (lastShown == 0L || lastShown == today || today - lastShown >= HINT_PAUSE_DAYS) reason else null
    }
}

/**
 * Anonyme Nutzungszählung (A3, nur wenn die Eltern sie einschalten): eine Liste „Ereignis → Anzahl“, am Ende der Sitzung
 * einmal als Text an den Zähler geschickt – ohne Cookies, ohne Kennung, ohne Spielstand oder Namen.
 */
object UsageCount {
    const val URL = "https://trainer.mathenachhilfe-online.com/zaehler/v1/e"
    /** Erlaubte Ereignisse – nichts anderes wird je gezählt */
    val EVENTS = setOf("start", "level_start", "level_geschafft", "theorie")

    /** Inhalt der Sendung: {"app":"kurvenflug","v":"2.5.21","f":"wasm","e":{"start":1,…}} – null, wenn nichts zu senden ist */
    fun body(version: String, variant: String, counts: Map<String, Int>): String? {
        val e = counts.filterKeys { it in EVENTS }.filterValues { it > 0 }
        if (e.isEmpty()) return null
        val list = e.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\":${it.value}" }
        return "{\"app\":\"kurvenflug\",\"v\":\"${version.filter { it.isLetterOrDigit() || it == '.' }}\",\"f\":\"${variant.filter { it.isLetter() }}\",\"e\":{$list}}"
    }
}
