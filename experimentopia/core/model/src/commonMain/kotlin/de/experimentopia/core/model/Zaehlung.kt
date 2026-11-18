package de.experimentopia.core.model

/**
 * Anonyme Nutzungszählung (gemeinsames Format aller Apps, Leitstand A3) und Links zu unseren Angeboten (C1).
 *
 * Grundsätze: keine Kennung, keine Gerätedaten, kein Zeitstempel, kein Lernstand, keine Namen – nur App, Version, Ereignis, Ort und Thema.
 * Der Server zählt nur Tagessummen. Eltern können die Zählung im Elternbereich abschalten; dann wird nichts gesendet.
 * Hinweise auf die Nachhilfe erscheinen nur hinter dem Eltern-Tor, nie im Kinderbereich und nie durch Blubb.
 */
object Zaehlung {
    const val APP = "experimentopia"
    fun url(test: Boolean) = (if (test) "https://trainer-test.mathenachhilfe-online.com" else "https://trainer.mathenachhilfe-online.com") + "/zaehler/v1/e"

    /** Abschließende Liste der Ereignisse, die Experimentopia sendet */
    val EREIGNISSE = setOf("start", "elternbereich", "pro_ansehen", "pro_kasse", "code_eingeloest", "probestunde_klick")
    val ORTE = setOf("elternbereich", "gesperrt", "klassenarbeit", "einstellungen", "link", "eingabe")
    /** Themen-Kennungen (am Server für Experimentopia freigeschaltet, Leitstand 18.11.) */
    val THEMEN: Set<String> get() = Topic.entries.map { ParentReport.slug(it) }.toSet()

    /** Inhalt eines Zählaufrufs als JSON – null, wenn Ereignis oder Ort nicht in den Positivlisten stehen (dann wird nichts gesendet) */
    fun inhalt(version: String, ereignis: String, ort: String? = null, thema: String? = null): String? {
        if (ereignis !in EREIGNISSE) return null
        if (ort != null && ort !in ORTE) return null
        val t = thema?.takeIf { it in THEMEN }
        val v = version.take(20).filter { it.isLetterOrDigit() || it == '.' || it == '-' }
        return buildString {
            append("{\"app\":\"").append(APP).append("\",\"v\":\"").append(v).append("\",\"ev\":\"").append(ereignis).append('"')
            if (ort != null) append(",\"ort\":\"").append(ort).append('"')
            if (t != null) append(",\"thema\":\"").append(t).append('"')
            append('}')
        }
    }
}

/** Links aus dem Elternbereich zur Website – mit Herkunft (src), Thema und Klasse */
object Angebote {
    const val SEITE = "https://www.mathenachhilfe-online.com"
    /** Vorgabe Leitstand: „/probestunde-anfrage/“ – liefert am 18.11. noch 404, darum vorerst die bestehende Seite „/probestunde/“ */
    const val PROBESTUNDE_PFAD = "probestunde"
    const val PROBESTUNDE_TEXT = "Eine kostenlose Probestunde kann vor der Arbeit helfen."

    fun probestunde(thema: String?, klasse: Int?): String = buildString {
        append(SEITE).append('/').append(PROBESTUNDE_PFAD).append("/?src=").append(Zaehlung.APP).append("-klassenarbeit")
        if (thema != null && thema in Zaehlung.THEMEN) append("&thema=").append(thema)
        if (klasse != null && klasse in 1..13) append("&klasse=").append(klasse)
    }
}
