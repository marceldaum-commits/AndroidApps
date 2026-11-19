package de.raumlicht.core.model

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Anonyme Nutzungszählung (abschaltbar): Die App meldet unserem Server nur, DASS etwas geschah – „App geöffnet“, „Link zur Probestunde angetippt“.
 * Kein Cookie, keine Kennung, kein Gerät, kein Lernstand; der Server führt nur Tageszähler. Format und Listen gelten für alle unsere Apps.
 */
object Zaehler {
    const val URL = "https://trainer.mathenachhilfe-online.com/zaehler/v1/e"
    const val APP = "raumlicht"
    /** Ereignisse, die es in Raumlicht gibt (aus der gemeinsamen, abschließenden Liste) */
    val EVENTS = setOf("start", "level_start", "level_geschafft", "theorie", "pro_ansehen", "pro_kasse", "code_eingeloest", "probestunde_klick", "elternbereich", "nachhilfe_hinweis", "geschenk_klick")
    val PLACES = setOf("gesperrt", "einstellungen", "link", "eingabe", "lernstand", "klassenarbeit", "geschenk", "fusszeile")
    val TOPICS = setOf("vektoren", "analysis", "stochastik")
    const val SWITCH = "Anonyme Nutzungszählung"
    const val SWITCH_NOTE = "Hilft uns, die App zu verbessern. Gezählt wird nur, dass etwas geschah – etwa „App geöffnet“, „Aufgabe gestartet“, „Aufgabe geschafft“, „Theorie geöffnet“ oder „Link angetippt“. Ohne Kennung, ohne Gerät; welche Aufgabe es war und wie gut du bist, wird nicht übertragen."

    /** Der Zählaufruf als Text – oder null, wenn etwas nicht auf den Listen steht (dann wird nichts gesendet) */
    fun body(event: String, place: String?, topic: String?, version: String): String? {
        if (event !in EVENTS || (place != null && place !in PLACES) || (topic != null && topic !in TOPICS) || version.length > 20) return null
        return buildJsonObject { put("app", APP); put("v", version); put("ev", event); if (place != null) put("ort", place); if (topic != null) put("thema", topic) }.toString()
    }
    /** Link zu unserer Website oder zum Shop mit Herkunft: src=raumlicht-<ort>, dazu das Thema, wenn bekannt */
    fun link(url: String, place: String, topic: String? = null): String =
        url + (if ('?' in url) "&" else "?") + "src=$APP-$place" + (topic?.let { "&thema=$it" } ?: "")
    /** Themen-Kennung eines Kapitels (gemeinsame Liste aller Apps) */
    fun topic(area: Area) = when (area) { Area.GEOMETRIE -> "vektoren"; Area.ANALYSIS -> "analysis"; else -> "stochastik" }
}

/**
 * Sachlicher Hinweis auf unsere Nachhilfe im Lernstand – nur wenn ein Gebiet erkennbar hakt, höchstens ein Hinweis gleichzeitig, je Gebiet höchstens alle
 * [PAUSE_DAYS] Tage, ausblendbar und in den Einstellungen abschaltbar. Ohne Figur, ohne Preis, ohne Druck (Raumlicht ist für die Oberstufe; der Hinweis spricht die Lernenden selbst an).
 */
object Nachhilfe {
    const val TRIAL_URL = "https://www.mathenachhilfe-online.com/probestunde/"
    const val GIFT_URL = "https://www.mathenachhilfe-online.com/geschenke/"
    const val HOME_URL = "https://www.mathenachhilfe-online.com/"
    /** „← Alle Apps“ oben auf der Startseite: zurück zur App-Auswahl auf der Website (auf der Testseite deren Vorstufe) */
    const val APPS_URL = "https://www.mathenachhilfe-online.com/lern-apps/?src=raumlicht-kopf"
    const val APPS_TEST_URL = "https://staging.mathenachhilfe-online.com/lern-apps/?src=raumlicht-kopf"
    const val APPS_LABEL = "← Alle Apps"
    const val FOOTER = "Ein Angebot von Nachhilfe – online"
    const val SWITCH = "Hinweise auf unsere Nachhilfe zeigen"
    const val SWITCH_NOTE = "Im Lernstand, wenn ein Gebiet noch hakt: ein kurzer Hinweis auf eine kostenlose Probestunde. Nie in den Aufgaben."
    const val PAUSE_DAYS = 14
    /** So viele gespielte Aufgaben braucht ein Gebiet, bevor es als „hakt“ gelten kann */
    const val MIN_PLAYED = 6
    /** „Hakt“: weniger als 60 % der möglichen Sterne in den gespielten Aufgaben */
    const val WEAK_PERCENT = 60

    class Hint(val topic: String, val title: String) {
        val text get() = "Bei $title hakt es noch? Üben hilft – manchmal braucht es aber eine Erklärung von einem Menschen. Eine kostenlose Probestunde kann helfen."
        val link get() = Zaehler.link(TRIAL_URL, "lernstand", topic)
    }

    private fun parse(state: String): Map<String, String> = state.split(';').mapNotNull { e -> e.split('=').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    private fun encode(m: Map<String, String>) = m.entries.joinToString(";") { "${it.key}=${it.value}" }

    /** Anteil der möglichen Sterne in den gespielten Aufgaben je Gebiet (in Prozent) – nur Gebiete mit genug gespielten Aufgaben */
    fun weak(topics: List<TopicState>, stars: Map<String, Int>): List<Pair<Area, Int>> = topics.groupBy { it.chapter.area }.mapNotNull { (area, list) ->
        val played = list.flatMap { it.chapter.levels }.filter { (stars[it.id] ?: 0) > 0 }
        if (played.size < MIN_PLAYED) null else (area to played.sumOf { stars[it.id] ?: 0 } * 100 / (played.size * 3)).takeIf { it.second < WEAK_PERCENT }
    }.sortedBy { it.second }

    /**
     * Der Hinweis, der heute zu zeigen ist – oder null. [state] merkt je Gebiet den Tag der letzten Anzeige oder „aus“ (ausgeblendet).
     * Ein Hinweis bleibt an dem Tag sichtbar, an dem er zuerst erschien; danach ruht dieses Gebiet [PAUSE_DAYS] Tage.
     */
    fun hint(topics: List<TopicState>, stars: Map<String, Int>, today: Long, state: String, enabled: Boolean): Hint? {
        if (!enabled) return null
        val seen = parse(state)
        val area = weak(topics, stars).map { it.first }.firstOrNull { a -> val s = seen[Zaehler.topic(a)]
            s != "aus" && (s?.toLongOrNull()?.let { today == it || today >= it + PAUSE_DAYS } ?: true) } ?: return null
        return Hint(Zaehler.topic(area), if (area == Area.GEOMETRIE) "Vektoren" else area.title)
    }
    /** Hinweis im Klausur-Planer: 3 bis 14 Tage vor dem eingetragenen Termin, je Klausur einmal ausblendbar. Seit 2.18.30 führt er zur kostenlosen Probestunde (der Crashkurs ist nur für Kunden). */
    class ExamHint(val days: Int, val topic: String?, val key: String, base: String) {
        val text get() = "In $days Tagen ist deine Klausur. Eine kostenlose Probestunde kann vor der Arbeit helfen."
        val link = Zaehler.link(base, "klassenarbeit", topic)
    }
    fun exam(plan: Plan?, today: Long, state: String, enabled: Boolean, url: String = TRIAL_URL): ExamHint? {
        if (!enabled || plan == null || url.isEmpty()) return null
        val days = Planner.daysLeft(plan, today); if (days !in 3..14) return null
        val key = "klausur${plan.examDay}"; if (parse(state)[key] == "aus") return null
        // Thema nur, wenn alle gewählten Kapitel aus einem Gebiet sind
        val areas = Levels.chapters.filter { it.number in plan.chapters }.map { it.area }.toSet()
        return ExamHint(days, areas.singleOrNull()?.let(Zaehler::topic), key, url)
    }
    /** true, wenn dieser Hinweis heute zum ersten Mal (in dieser 14-Tage-Runde) erscheint – dann wird gezählt und der Tag gemerkt */
    fun isNew(state: String, topic: String, today: Long) = parse(state)[topic]?.toLongOrNull() != today
    fun shown(state: String, topic: String, today: Long) = encode(parse(state) + (topic to today.toString()))
    fun hide(state: String, topic: String) = encode(parse(state) + (topic to "aus"))
}
