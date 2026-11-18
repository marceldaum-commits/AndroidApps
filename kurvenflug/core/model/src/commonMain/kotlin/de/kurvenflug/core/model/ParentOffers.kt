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

    /** Crashkurs vor einer Klassenarbeit (Wortlaut Leitstand: „kostenlos für Familien mit Stundenpaket“) */
    const val CRASH_COURSE = "$SITE/produkt/easy-learning-crash-kurs/?src=kurvenflug-klassenarbeit"

    /**
     * Gemeinsame Themen-Kennungen (Leitstand 18.11.; andere Werte verwirft das Probestunden-Formular) je Kurvenflug-Kapitel.
     */
    val TOPICS: Map<Int, String> = mapOf(
        1 to "geometrie", 2 to "lineare-funktionen", 3 to "quadratische-funktionen", 4 to "physik-mechanik", 5 to "trigonometrie",
        6 to "physik-mechanik", 7 to "gleichungssysteme", 8 to "zuordnungen", 9 to "pythagoras", 10 to "exponential-log",
        11 to "physik-optik", 12 to "physik-energie", 13 to "physik-mechanik", 14 to "trigonometrie", 15 to "brueche",
        16 to "geometrie", 17 to "physik-optik", 18 to "quadratische-funktionen", 19 to "analysis", 20 to "analysis",
        21 to "analysis", 22 to "vektoren", 23 to "prozent", 24 to "physik-mechanik", 25 to "potenzen-wurzeln",
        26 to "trigonometrie", 27 to "physik-mechanik", 28 to "exponential-log", 29 to "zuordnungen", 30 to "stochastik",
    )
    /** Die erlaubte gemeinsame Liste (zur Prüfung im Test) */
    val ALLOWED_TOPICS = setOf("grundrechenarten", "einmaleins", "brueche", "dezimalzahlen", "prozent", "zuordnungen", "terme-gleichungen",
        "lineare-funktionen", "gleichungssysteme", "pythagoras", "quadratische-funktionen", "potenzen-wurzeln", "exponential-log", "trigonometrie",
        "geometrie", "vektoren", "analysis", "stochastik", "physik-mechanik", "physik-optik", "physik-energie", "sachkunde", "deutsch", "englisch",
        "allgemeinwissen")

    /** Kostenlose Probestunde zum Thema und zur Klasse; [reason] bestimmt die Quelle (elternbericht bzw. klassenarbeit) */
    fun trialLesson(chapter: Int?, grade: Int?, reason: Reason = Reason.STUCK): String =
        "$SITE/probestunde/?src=kurvenflug-" + (if (reason == Reason.EXAM) "klassenarbeit" else "elternbericht") +
            (chapter?.let { TOPICS[it] }?.let { "&thema=$it" } ?: "") + (grade?.let { "&klasse=$it" } ?: "")

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
    val EVENTS = setOf("start", "level_start", "level_geschafft", "theorie", "elternbereich", "pro_ansehen", "pro_kasse", "code_eingeloest",
        "probestunde_klick", "neukunde_klick", "crashkurs_klick", "nachhilfe_hinweis", "geschenk_klick")
    /** Der Zähler nimmt je Ereignis höchstens so viele an */
    const val MAX_PER_EVENT = 50

    /** Inhalt der Sendung: {"app":"kurvenflug","v":"2.5.22","e":{"start":1,…}} – null, wenn nichts zu senden ist */
    fun body(version: String, counts: Map<String, Int>): String? {
        val e = counts.filterKeys { it in EVENTS }.filterValues { it > 0 }
        if (e.isEmpty()) return null
        val list = e.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\":${it.value.coerceAtMost(MAX_PER_EVENT)}" }
        return "{\"app\":\"kurvenflug\",\"v\":\"${version.filter { it.isLetterOrDigit() || it == '.' }}\",\"e\":{$list}}"
    }
}
