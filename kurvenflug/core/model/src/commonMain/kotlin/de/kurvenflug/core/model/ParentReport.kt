package de.kurvenflug.core.model

/**
 * Bericht für „Eltern-Handy koppeln“ (Leitstand/Marcel 18.11., Format 1). Wird in der WebApp mit AES-GCM-256 verschlüsselt
 * hochgeladen – der Schlüssel steht nur im Kopplungs-Link (nach „#“) und verlässt nie das Gerät der Familie.
 * Enthält nur Lernstand: keine E-Mail, keine Gerätedaten, kein Standort; der Name nur als frei gewählter Spitzname.
 */
object ParentReport {
    const val DAYS = 14

    /** Titel der gemeinsamen Themen (für die Eltern-Seite) */
    private val TOPIC_TITLES = mapOf(
        "geometrie" to "Geometrie", "lineare-funktionen" to "Lineare Funktionen", "quadratische-funktionen" to "Quadratische Funktionen",
        "physik-mechanik" to "Physik: Mechanik", "trigonometrie" to "Trigonometrie", "gleichungssysteme" to "Gleichungssysteme",
        "zuordnungen" to "Zuordnungen", "pythagoras" to "Satz des Pythagoras", "exponential-log" to "Exponential & Logarithmus",
        "physik-optik" to "Physik: Optik", "physik-energie" to "Physik: Energie", "brueche" to "Bruchrechnung", "analysis" to "Analysis",
        "vektoren" to "Vektoren", "prozent" to "Prozent & Zinsen", "potenzen-wurzeln" to "Potenzen & Wurzeln", "stochastik" to "Stochastik",
    )

    private fun esc(s: String) = buildString { s.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c < ' ') append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c) } } }
    private fun two(n: Int) = n.toString().padStart(2, '0')
    private fun date(d: CalDate) = "${d.year}-${two(d.monthValue)}-${two(d.dayOfMonth)}"

    /** ISO-Zeit mit Zeitzone, z. B. 2026-11-18T16:30:00+01:00 */
    fun iso(millis: Long, offsetMin: Int): String {
        val local = millis + offsetMin * 60_000L
        val day = CalDate.ofEpochDay(Math2.floorDiv(local, Weekly.DAY))
        val ms = local - Math2.floorDiv(local, Weekly.DAY) * Weekly.DAY
        val h = (ms / 3_600_000L).toInt(); val m = ((ms / 60_000L) % 60).toInt(); val s = ((ms / 1000L) % 60).toInt()
        val sign = if (offsetMin < 0) "-" else "+"; val o = kotlin.math.abs(offsetMin)
        return "${date(day)}T${two(h)}:${two(m)}:${two(s)}$sign${two(o / 60)}:${two(o % 60)}"
    }

    /**
     * [plays]: alle Durchgänge (beliebig viele, gefiltert wird hier), [progress]: Level → Sterne (Stand jetzt),
     * [now]/[todayStart]/[offsetMin]: Zeit und Zeitzone des Geräts.
     */
    fun json(plays: List<PlayRecord>, progress: Map<String, Int>, now: Long, todayStart: Long, offsetMin: Int, grade: Int?, nick: String?, examSoon: Boolean = false, months: List<MonthSum> = emptyList()): String {
        val from = todayStart - (DAYS - 1) * Weekly.DAY
        val recent = plays.filter { it.at >= from && it.at <= now + Weekly.DAY }
        val minutes = (recent.sumOf { it.seconds.coerceAtLeast(0) } + 30) / 60
        fun dayIndex(at: Long) = Math2.floorDiv(at - from, Weekly.DAY).toInt()
        val active = recent.map { dayIndex(it.at) }.distinct().size
        val solved = recent.count { it.stars > 0 }
        val pct = if (recent.isEmpty()) 0 else (solved * 100 + recent.size / 2) / recent.size

        // Je Thema: Anteil geschaffter Durchgänge (ab 3 Durchgängen aussagekräftig)
        data class T(val id: String, val chapter: Chapter, val n: Int, val ok: Int) { val p get() = (ok * 100 + n / 2) / n }
        val byTopic = recent.mapNotNull { r -> Weekly.chapterOf(r.levelId)?.let { c -> ParentOffers.TOPICS[c.number]?.let { it to (c to r) } } }
            .groupBy({ it.first }, { it.second })
            .map { (id, l) -> T(id, l.groupingBy { it.first }.eachCount().maxBy { it.value }.key, l.size, l.count { it.second.stars > 0 }) }
            .filter { it.n >= 3 }
        val strong = byTopic.filter { it.p >= 80 }.sortedByDescending { it.p }.take(3)
        val weak = byTopic.filter { it.p <= 60 }.sortedBy { it.p }.take(3)

        // Fortschritt in den zuletzt gespielten Kapiteln
        val chapters = recent.sortedByDescending { it.at }.mapNotNull { Weekly.chapterOf(it.levelId) }.distinct().take(5)
        // Letzte Tage mit Übung (neueste zuerst, höchstens 7)
        val days: List<Pair<Int, List<PlayRecord>>> = recent.groupBy { dayIndex(it.at) }.toList().sortedByDescending { it.first }.take(7)

        return buildString {
            append("{\"format\":1,\"app\":\"kurvenflug\",\"appName\":\"KurvenFlug\"")
            nick?.trim()?.takeIf { it.isNotEmpty() }?.let { append(",\"name\":\"${esc(it.take(30))}\"") }
            grade?.let { append(",\"klasse\":$it") }
            append(",\"stand\":\"${iso(now, offsetMin)}\",\"zeitraum\":\"letzte $DAYS Tage\"")
            append(",\"lernzeitMin\":$minutes,\"tageAktiv\":$active,\"aufgaben\":${recent.size},\"richtigProzent\":$pct")
            append(",\"staerken\":[" + strong.joinToString(",") { "{\"thema\":\"${it.id}\",\"titel\":\"${esc(TOPIC_TITLES[it.id] ?: it.chapter.title)}\",\"prozent\":${it.p}}" } + "]")
            append(",\"schwach\":[" + weak.joinToString(",") { t ->
                val tip = TheoryFormulas.formula(t.chapter.number, t.chapter.levels.first().kind)?.removePrefix("Merkformel: ")
                "{\"thema\":\"${t.id}\",\"titel\":\"${esc(TOPIC_TITLES[t.id] ?: t.chapter.title)}\",\"prozent\":${t.p}" + (tip?.let { ",\"tipp\":\"${esc("Theorie „${t.chapter.title}“: $it")}\"" } ?: "") + "}"
            } + "]")
            append(",\"fortschritt\":[" + chapters.joinToString(",") { c ->
                "{\"titel\":\"${esc("Kapitel ${c.number} – ${c.title}")}\",\"geschafft\":${c.levels.count { (progress[it.id] ?: 0) > 0 }},\"von\":${c.levels.size}}"
            } + "]")
            append(",\"letzte\":[" + days.joinToString(",") { (d, l) ->
                "{\"tag\":\"${date(CalDate.ofEpochDay(Math2.floorDiv(from + offsetMin * 60_000L, Weekly.DAY) + d))}\",\"min\":${(l.sumOf { it.seconds.coerceAtLeast(0) } + 30) / 60},\"aufgaben\":${l.size},\"richtig\":${l.count { it.stars > 0 }}}"
            } + "]")
            // Monatsverlauf: höchstens 12 Monate, älteste zuerst
            append(",\"verlauf\":[" + months.sortedBy { it.month }.takeLast(MonthSums.MAX).joinToString(",") {
                "{\"monat\":\"${it.month}\",\"min\":${it.minutes},\"aufgaben\":${it.tasks},\"richtigProzent\":${it.percent}}" } + "]")
            // „Klassenarbeit bald“ (Schalter der Eltern): Hinweis mit dem Thema, das gerade am meisten Übung braucht bzw. zuletzt dran war
            if (examSoon) {
                val topic = weak.firstOrNull()?.let { TOPIC_TITLES[it.id] ?: it.chapter.title } ?: chapters.firstOrNull()?.title
                append(",\"hinweis\":\"${esc("Bald steht eine Klassenarbeit an" + (topic?.let { ": $it" } ?: "."))}\"")
            }
            append("}")
        }
    }
}
