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
    val EREIGNISSE = setOf("start", "elternbereich", "pro_ansehen", "pro_kasse", "code_eingeloest", "probestunde_klick", "umzug")
    /** „umzug“ (Recht V45a): nur der Ereignisname, nur bei eingeschalteter Zählung, höchstens einmal je Gerät und Tag; entfällt mit der 301-Umleitung */
    const val UMZUG = "umzug"
    val ORTE = setOf("elternbereich", "gesperrt", "klassenarbeit", "einstellungen", "link", "eingabe", "elternhinweis", "oberstufe_tipp", "familie")
    /** Themen-Kennungen (am Server für Experimentopia freigeschaltet, Leitstand 18.11.) */
    val THEMEN: Set<String> get() = Topic.entries.map { ParentReport.slug(it) }.toSet()

    /** Inhalt eines Zählaufrufs als JSON – null, wenn Ereignis oder Ort nicht in den Positivlisten stehen (dann wird nichts gesendet) */
    fun inhalt(version: String, ereignis: String, ort: String? = null, thema: String? = null): String? {
        if (ereignis !in EREIGNISSE) return null
        if (ort != null && ort !in ORTE) return null
        // Probestunden-Klicks zählen nur anonym: nie mit Thema (Recht V15)
        val t = thema?.takeIf { ereignis != Nachhilfe.ZAEHL_EREIGNIS && it in THEMEN }
        val v = version.take(20).filter { it.isLetterOrDigit() || it == '.' || it == '-' }
        return buildString {
            append("{\"app\":\"").append(APP).append("\",\"v\":\"").append(v).append("\",\"ev\":\"").append(ereignis).append('"')
            if (ort != null) append(",\"ort\":\"").append(ort).append('"')
            if (t != null) append(",\"thema\":\"").append(t).append('"')
            append('}')
        }
    }
}

/** Links zur Website – nur mit Herkunft (src), nie mit Thema, Klasse oder Lernstand (Recht V15) */
object Angebote {
    const val SEITE = "https://www.mathenachhilfe-online.com"
    /** Probestunden-Seite (vom Leitstand am 18.11. bestätigt: „/probestunde/“) */
    const val PROBESTUNDE_PFAD = "probestunde"
    const val PROBESTUNDE_TEXT = "Eine kostenlose Probestunde kann vor der Arbeit helfen."

    /** „← Alle Apps“: zurück zur App-Auswahl auf der Website (Test-Stufe: staging) */
    fun alleApps(test: Boolean) = (if (test) "https://staging.mathenachhilfe-online.com" else SEITE) + "/lern-apps/?src=experimentopia-kopf"

    /** [ort]: Stelle der App, nur für die anonyme Zählung (z. B. „klassenarbeit“, „elternhinweis“, „oberstufe_tipp“) */
    fun probestunde(ort: String = "klassenarbeit"): String = SEITE + "/" + PROBESTUNDE_PFAD + "/?src=" + Zaehlung.APP + "-" + ort.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
}
