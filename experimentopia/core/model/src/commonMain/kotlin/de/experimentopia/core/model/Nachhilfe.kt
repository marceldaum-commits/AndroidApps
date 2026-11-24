package de.experimentopia.core.model

/**
 * Weg zur Probestunde (Recht V15, Marcel 19.11.2026: „Ja, alles so umsetzen“; Wortlaut verbindlich, AppZentrale).
 * Alles wird NUR auf dem Gerät berechnet – keine Lerndaten gehen für Werbezwecke an einen Server.
 *  1. Elternbereich (hinter dem Tor): Hinweis bei objektiven Schwierigkeiten, höchstens 1× in 4 Wochen je Thema, wegklickbar, abschaltbar.
 *  2. Kinderansicht: nur der Hilfesatz [KIND_HILFE] mit zwei Knöpfen – kein Außenlink, kein Wort von Probestunde oder Eltern-Bereich.
 *  3. Klasse 9–10 ohne Tor: [TIPP_OBERSTUFE], höchstens 1× pro Woche, wegklickbar.
 * Nachhilfe gibt es nur in Physik – darum zählen nur Physik-Themen.
 */
object Nachhilfe {
    const val KIND_HILFE = "Du kommst hier gerade nicht weiter? Das ist okay. Probier eine leichtere Stufe oder schau dir die Erklärung noch einmal an. Und du kannst jederzeit deine Eltern oder deine Lehrkraft fragen."
    const val KIND_LEICHTER = "Leichter üben"
    const val KIND_ERKLAERUNG = "Erklärung ansehen"
    const val TIPP_OBERSTUFE = "Tipp: Wenn ein Thema länger hakt, kann eine persönliche Erklärung helfen. Mit deinen Eltern könnt ihr bei uns eine kostenlose, unverbindliche Probestunde anfragen."
    const val TIPP_KNOPF = "Mehr erfahren"
    const val ELTERN_ANGEBOT = "Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe."
    const val ELTERN_KNOPF = "Probestunde anfragen"
    const val ELTERN_AUSBLENDEN = "Solche Hinweise ausblenden"
    const val SCHALTER = "Hinweise auf Nachhilfe ausblenden"

    fun elternHinweis(thema: String, name: String?) =
        "Hinweis: In $thema hat ${name?.takeIf { it.isNotBlank() } ?: "euer Kind"} in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. " +
            "Wenn ihr persönliche Unterstützung möchtet: $ELTERN_ANGEBOT"

    /** Zählt eine Probestunden-Anfrage: anonym, ohne Thema und ohne Lernstand */
    const val ZAEHL_EREIGNIS = "probestunde_klick"

    private const val TAG = 86_400_000L
    const val FENSTER_TAGE = 28
    const val MIN_TAGE = 14
    const val MIN_ANTWORTEN = 6
    const val SCHWELLE = 0.5
    const val PAUSE_THEMA_TAGE = 28
    const val PAUSE_TIPP_TAGE = 7

    /**
     * Objektive Schwierigkeit: In den letzten [FENSTER_TAGE] Tagen mindestens [MIN_ANTWORTEN] Antworten in einem Physik-Thema,
     * die ältesten mindestens [MIN_TAGE] Tage alt (also „über 2 Wochen“), und weniger als 50 % richtig geraten bzw. erklärt.
     */
    fun schwierigeThemen(journal: List<NoteRow>, nowMs: Long, themaOf: (String) -> Topic?): List<Topic> =
        journal.filter { it.at >= nowMs - FENSTER_TAGE * TAG && it.at <= nowMs }
            .mapNotNull { n -> themaOf(n.experimentId)?.takeIf { it.subject == Subject.PHYSIK }?.let { it to n } }
            .groupBy({ it.first }, { it.second })
            .filter { (_, l) -> l.size >= MIN_ANTWORTEN && nowMs - l.minOf { it.at } >= MIN_TAGE * TAG && l.count { it.right }.toDouble() / l.size < SCHWELLE }
            .keys.toList()

    /** Darf ein Hinweis (für ein Thema bzw. der Oberstufen-Tipp) jetzt erscheinen? [letzte] = Zeitpunkt des letzten Zeigens oder null */
    fun darfZeigen(letzte: Long?, nowMs: Long, pauseTage: Int) = letzte == null || nowMs - letzte >= pauseTage * TAG

    /** Oberstufen-Tipp nur für Klasse 9–10 – bei unbekannter Klasse (0) nie */
    fun tippFuerKlasse(klasse: Int) = klasse in 9..10
}
