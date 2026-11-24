package de.wissensgarten.core.model

/**
 * Anonyme Nutzungszählung und Links zu unseren Angeboten (Ratgeber „Geld verdienen“, Teil 5: Gemeinsames Format, A3, B4, C1).
 *
 * Grundsätze: keine Kennung, keine Gerätedaten, kein Lernstand, keine Namen – nur App, Version, Ereignis, Ort und Thema.
 * Der Server zählt nur Tagessummen. Eltern können die Zählung im Elternbereich abschalten; dann wird nichts gesendet.
 * Hinweise auf die Nachhilfe erscheinen nur hinter dem Eltern-Tor, nie im Kinderbereich und nie durch Brummi.
 */
object Zaehlung {
    const val APP = "wissensgarten"
    const val URL = "https://trainer.mathenachhilfe-online.com/zaehler/v1/e"
    /** Test-Stufe zählt auf dem Test-Server, damit die echten Zahlen nicht verfälscht werden */
    const val URL_TEST = "https://trainer-test.mathenachhilfe-online.com/zaehler/v1/e"
    fun url(test: Boolean) = if (test) URL_TEST else URL

    /** Abschließende Liste der Ereignisse (gemeinsames Format) */
    val EREIGNISSE = setOf(
        "start", "elternbereich", "pro_ansehen", "pro_kasse", "code_eingeloest", "probestunde_klick",
        "neukunde_klick", "crashkurs_klick", "nachhilfe_hinweis", "geschenk_klick",
    )
    val ORTE = setOf("elternbericht", "elternbereich", "gesperrt", "klassenarbeit", "geschenk", "einstellungen", "link", "eingabe", "lernstand", "fusszeile")
    /** Themen, die der Zähl-Server für den Wissensgarten annimmt (Leitstand 18.11.) – andere werden beim Zählen weggelassen */
    val ZAEHL_THEMEN = setOf("sachkunde", "allgemeinwissen", "deutsch", "englisch", "einmaleins", "grundrechenarten")
    val THEMEN = setOf(
        "grundrechenarten", "einmaleins", "brueche", "dezimalzahlen", "prozent", "zuordnungen", "terme-gleichungen",
        "lineare-funktionen", "gleichungssysteme", "pythagoras", "quadratische-funktionen", "potenzen-wurzeln", "exponential-log",
        "trigonometrie", "geometrie", "vektoren", "analysis", "stochastik", "physik-mechanik", "physik-optik", "physik-energie",
        "sachkunde", "deutsch", "englisch", "allgemeinwissen",
    )

    /**
     * Inhalt eines Zählaufrufs als JSON – oder null, wenn Ereignis oder Ort nicht in den Positivlisten stehen (dann wird nichts gesendet).
     * Absichtlich ohne Kennung, Zeitstempel, Gerät, Name oder Lernstand.
     */
    fun inhalt(version: String, ereignis: String, ort: String? = null, thema: String? = null): String? {
        if (ereignis !in EREIGNISSE) return null
        if (ort != null && ort !in ORTE) return null
        val t = thema?.takeIf { it in ZAEHL_THEMEN }   // unbekanntes Thema: Ereignis trotzdem zählen, Thema weglassen (Server: 400)
        val v = version.take(20).filter { it.isLetterOrDigit() || it == '.' || it == '-' }
        return buildString {
            append("{\"app\":\"").append(APP).append("\",\"v\":\"").append(v).append("\",\"ev\":\"").append(ereignis).append('"')
            if (ort != null) append(",\"ort\":\"").append(ort).append('"')
            if (t != null) append(",\"thema\":\"").append(t).append('"')
            append('}')
        }
    }
}

/** Links aus dem Elternbereich zur Website – immer mit Herkunft (src), wo bekannt mit Thema und Klasse. */
object Angebote {
    const val SEITE = "https://www.mathenachhilfe-online.com"

    fun link(pfad: String, ort: String, thema: String? = null, klasse: Int? = null): String = buildString {
        append(SEITE).append('/')
        if (pfad.isNotEmpty()) append(pfad).append('/')
        append("?src=").append(Zaehlung.APP).append('-').append(ort)
        if (thema != null && thema in Zaehlung.THEMEN) append("&thema=").append(thema)
        if (klasse != null && klasse in 1..13) append("&klasse=").append(klasse)
    }

    /** Probestunde anfragen (V15): nur Herkunft (src), nie Thema oder Klasse – die Zählung bleibt ohne Lernstand */
    fun probestunde(ort: String, @Suppress("UNUSED_PARAMETER") thema: String? = null, @Suppress("UNUSED_PARAMETER") klasse: Int? = null) = link("probestunde", ort)
    /** Neukundenpaket (Einzelnachhilfe) */
    fun neukundenpaket(ort: String, thema: String?, klasse: Int?) = link("produkt/einzelnachhilfe-neukundenpaket", ort, thema, klasse)
    /** Easy-Learning-Crashkurs vor Klassenarbeiten – nur Mittel- und Oberstufe (Klasse ≥ 5) */
    fun crashkurs(thema: String?, klasse: Int?) = link("produkt/easy-learning-crash-kurs", "klassenarbeit", thema, klasse)
    fun crashkursPasst(klasse: Int?) = klasse != null && klasse in 5..13
    /** „Lern-Geschenk verschenken“ (Weihnachts-Gutscheine) */
    val geschenk = link("geschenke", "geschenk")
    /** Absender-Hinweis „Ein Angebot von Nachhilfe – online“ */
    val fusszeile = link("", "fusszeile")

    /** Themen-Kennung (gemeinsame Liste) für ein Wissenspaket – null, wenn es dazu kein passendes Nachhilfe-Thema gibt. */
    fun thema(packId: String?, subject: String?): String? {
        val id = packId.orEmpty()
        return when {
            id.contains("einmaleins") -> "einmaleins"
            id.contains("brueche") || id.contains("bruch") -> "brueche"
            id.contains("prozent") -> "prozent"
            id.contains("pythagoras") || id.contains("trigo") -> "pythagoras"
            id.contains("geometrie") || id.contains("formen") -> "geometrie"
            id.contains("gleichung") || id.contains("algebra") -> "terme-gleichungen"
            id.contains("wahrscheinlich") || id.contains("statistik") || id.contains("stochastik") -> "stochastik"
            id.contains("analysis") -> "analysis"
            id.contains("vektor") -> "vektoren"
            subject == "mathe" -> "grundrechenarten"
            subject == "deutsch" -> "deutsch"
            subject == "englisch" -> "englisch"
            subject == "sachunterricht" -> "sachkunde"
            else -> null
        }
    }

    /** Lesbarer Name eines Themas für den Hinweis an Eltern */
    fun themaName(thema: String): String = when (thema) {
        "grundrechenarten" -> "Rechnen"
        "einmaleins" -> "das Einmaleins"
        "brueche" -> "Bruchrechnung"
        "prozent" -> "Prozentrechnung"
        "terme-gleichungen" -> "Gleichungen"
        "pythagoras" -> "Pythagoras und Trigonometrie"
        "geometrie" -> "Geometrie"
        "stochastik" -> "Wahrscheinlichkeit"
        "analysis" -> "Analysis"
        "vektoren" -> "Vektoren"
        "deutsch" -> "Deutsch"
        "englisch" -> "Englisch"
        "sachkunde" -> "Sachunterricht"
        else -> thema
    }
}

/**
 * Weg zur Probestunde im Elternbericht (Recht V15, 19.11.2026): Ein Thema „hakt“, wenn es in den letzten 14 Tagen mindestens 10 Antworten
 * mit weniger als 50 % richtig gab. Berechnung nur auf dem Gerät. Höchstens ein Hinweis gleichzeitig, je Thema höchstens alle 28 Tage (4 Wochen),
 * wegklickbar, dazu der Schalter „Hinweise auf Nachhilfe ausblenden“. Nie in der Kinderansicht.
 * Ein einmal gezeigter Hinweis bleibt einen Tag lang stehen (damit er beim erneuten Öffnen nicht springt).
 */
object NachhilfeHinweis {
    const val TAGE = 14
    const val MIN_ANTWORTEN = 10
    const val GRENZE_PROZENT = 50
    /** Je Thema höchstens ein Hinweis in 4 Wochen */
    const val SPERRE_TAGE = 28
    private const val DAY = 24L * 60 * 60 * 1000

    data class Antwort(val thema: String, val zeit: Long, val richtig: Boolean)
    data class Hinweis(val thema: String, val prozent: Int, val anzahl: Int, val neu: Boolean)

    /** Wortlaut von Recht (V15), unverändert. [name] = Spitzname des Kindes. */
    fun text(thema: String, name: String): String =
        "Hinweis: ${inThema(thema)} hat $name in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. " +
            "Wenn ihr persönliche Unterstützung möchtet: Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe."
    const val KNOPF = "Probestunde anfragen"
    const val AUSBLENDEN = "Solche Hinweise ausblenden"
    /** „In Bruchrechnung“, aber „Im Einmaleins“ */
    fun inThema(thema: String): String = Angebote.themaName(thema).let { if (it.startsWith("das ")) "Im " + it.removePrefix("das ").replaceFirstChar { c -> c.uppercase() } else "In $it" }

    fun pruefe(antworten: List<Antwort>, jetzt: Long, zuletztGezeigt: Map<String, Long>, ausgeblendet: Set<String>): Hinweis? {
        val kandidaten = antworten.filter { jetzt - it.zeit in 0 until TAGE * DAY }
            .groupBy { it.thema }
            .filterKeys { it in Zaehlung.THEMEN && it !in ausgeblendet }
            .mapNotNull { (thema, l) ->
                val pct = l.count { it.richtig } * 100 / l.size
                if (l.size >= MIN_ANTWORTEN && pct < GRENZE_PROZENT) Triple(thema, pct, l.size) else null
            }
        // Schon heute gezeigt? Dann genau diesen weiter zeigen (kein neues Zählen)
        kandidaten.firstOrNull { (t) -> zuletztGezeigt[t]?.let { jetzt - it in 0 until DAY } == true }
            ?.let { (t, p, n) -> return Hinweis(t, p, n, neu = false) }
        return kandidaten.filter { (t) -> zuletztGezeigt[t]?.let { jetzt - it < SPERRE_TAGE * DAY } != true }
            .minByOrNull { it.second }
            ?.let { (t, p, n) -> Hinweis(t, p, n, neu = true) }
    }

    /** Klassenarbeit in 3 bis 14 Tagen (Tage als lokaler Epoch-Tag) – die nächste, die nicht ausgeblendet ist */
    fun klassenarbeit(exams: List<Exam>, heute: Long, ausgeblendet: Set<String>): Exam? =
        exams.filter { it.id !in ausgeblendet && it.day - heute in 3..14 }.minByOrNull { it.day }
}
