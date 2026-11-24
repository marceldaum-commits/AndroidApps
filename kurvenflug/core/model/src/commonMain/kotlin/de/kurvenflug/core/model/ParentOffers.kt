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
    /** Nachhilfe-Hinweis höchstens einmal in 4 Wochen je Thema (Recht V15) */
    const val HINT_PAUSE_DAYS = 28L
    /** Kinder-Tipp (Kl. 9–10, ohne Tor) höchstens einmal pro Woche */
    const val KID_TIP_PAUSE_DAYS = 7L
    const val PROBESTUNDE = "$SITE/probestunde/"
    /** „Klassenarbeit bald“ gilt so lange, nachdem die Eltern es angetippt haben */
    const val EXAM_DAYS = 21L

    // Kein Crashkurs-Link mehr (Marcel 18.11.: Crashkurs nur für Kunden) – vor Klassenarbeiten die kostenlose Probestunde

    /**
     * Gemeinsame Themen-Kennungen (Leitstand 18.11.; andere Werte verwirft das Probestunden-Formular) je Kurvenflug-Kapitel.
     */
    val TOPICS: Map<Int, String> = mapOf(
        1 to "geometrie", 2 to "lineare-funktionen", 3 to "quadratische-funktionen", 4 to "physik-mechanik", 5 to "trigonometrie",
        6 to "physik-mechanik", 7 to "gleichungssysteme", 8 to "zuordnungen", 9 to "pythagoras", 10 to "exponential-log",
        11 to "physik-optik", 12 to "physik-energie", 13 to "physik-mechanik", 14 to "trigonometrie", 15 to "brueche",
        16 to "geometrie", 17 to "physik-optik", 18 to "quadratische-funktionen", 23 to "prozent", 24 to "physik-mechanik",
        25 to "potenzen-wurzeln", 26 to "trigonometrie", 27 to "physik-mechanik", 29 to "zuordnungen",
    )
    /** Die erlaubte gemeinsame Liste (zur Prüfung im Test) */
    val ALLOWED_TOPICS = setOf("grundrechenarten", "einmaleins", "brueche", "dezimalzahlen", "prozent", "zuordnungen", "terme-gleichungen",
        "lineare-funktionen", "gleichungssysteme", "pythagoras", "quadratische-funktionen", "potenzen-wurzeln", "exponential-log", "trigonometrie",
        "geometrie", "vektoren", "analysis", "stochastik", "physik-mechanik", "physik-optik", "physik-energie", "sachkunde", "deutsch", "englisch",
        "allgemeinwissen")

    /** Probestunde anfragen (Recht V15: nur die Seite und die Quelle – kein Thema, keine Klasse) */
    fun trialLesson(reason: Reason = Reason.STUCK): String =
        "$PROBESTUNDE?src=kurvenflug-" + (if (reason == Reason.EXAM) "klassenarbeit" else "elternbericht")
    /** Link des Tipps für Kl. 9–10 (ohne Tor) */
    const val KID_TIP_LINK = "$PROBESTUNDE?src=kurvenflug-tipp"

    /** Kinder-Tipp nur in Klasse 9–10, höchstens 1× pro Woche, wegklickbar */
    fun kidTip(grade: Int?, today: Long, lastShown: Long, dismissedToday: Boolean = false): Boolean =
        grade != null && grade in 9..10 && !dismissedToday && (lastShown == 0L || lastShown == today || today - lastShown >= KID_TIP_PAUSE_DAYS)
    const val KID_TIP_TEXT = "Tipp: Wenn ein Thema länger hakt, kann eine persönliche Erklärung helfen. Mit deinen Eltern könnt ihr bei uns eine kostenlose, unverbindliche Probestunde anfragen."
    const val KID_HELP_TEXT = "Du kommst hier gerade nicht weiter? Das ist okay. Probier eine leichtere Stufe oder schau dir die Erklärung noch einmal an. Und du kannst jederzeit deine Eltern oder deine Lehrkraft fragen."
    /** Hinweis für Eltern (Recht V15, Wortlaut genau so) */
    fun parentHintText(topic: String, nick: String) =
        "Hinweis: In $topic hat $nick in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. " +
            "Wenn ihr persönliche Unterstützung möchtet: Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe."

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
    const val URL = "/zaehler/v1/e"
    /** Erlaubte Ereignisse – nichts anderes wird je gezählt */
    /** In der Sammelsendung (ohne Ort): Anzahl je Sitzung */
    val EVENTS = setOf("start", "level_start", "level_geschafft", "theorie", "elternbereich")
    /** Mit Ort: sofort einzeln beim Antippen (Leitstand 18.11.) */
    val SINGLE = setOf("pro_ansehen", "pro_kasse", "code_eingeloest", "probestunde_klick", "nachhilfe_hinweis", "geschenk_klick")
    /** Recht V15: bei Probestunden-Klick und Hinweis nie ein Thema mitsenden */
    val NO_TOPIC = setOf("probestunde_klick", "nachhilfe_hinweis")
    val PLACES = setOf("elternbericht", "elternbereich", "gesperrt", "klassenarbeit", "kindertipp", "geschenk", "einstellungen", "link", "eingabe", "lernstand", "fusszeile")

    /** Einzelaufruf: {"app":"kurvenflug","v":"…","ev":"probestunde_klick","ort":"klassenarbeit","thema":"lineare-funktionen"} – null bei unbekannten Werten */
    fun single(version: String, event: String, place: String, topic: String? = null): String? {
        if (event !in SINGLE || place !in PLACES) return null
        val t = topic?.takeIf { it in ParentOffers.ALLOWED_TOPICS && event !in NO_TOPIC }
        return "{\"app\":\"kurvenflug\",\"v\":\"${version.filter { it.isLetterOrDigit() || it == '.' }}\",\"ev\":\"$event\",\"ort\":\"$place\"" +
            (t?.let { ",\"thema\":\"$it\"" } ?: "") + "}"
    }
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
