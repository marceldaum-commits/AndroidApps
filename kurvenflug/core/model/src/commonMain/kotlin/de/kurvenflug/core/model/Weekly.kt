package de.kurvenflug.core.model

/** Ein Durchgang (aus dem Protokoll): geschafft (stars > 0) oder hängen geblieben (stars = 0). */
data class PlayRecord(val levelId: String, val stars: Int, val attempts: Int, val seconds: Int, val at: Long)

/** Wochenübersicht für Eltern. */
data class WeekSummary(
    /** je Tag (ältester zuerst, 7 Tage): geschaffte Level und Minuten */
    val days: List<Pair<Int, Int>>,
    val minutes: Int,
    val solved: Int,
    val activeDays: Int,
    /** Themen dieser Woche mit Anzahl Durchgänge */
    val topics: List<Pair<Chapter, Int>>,
    /** Hier täte Hilfe gut: hängen geblieben und seitdem nicht geschafft */
    val needsHelp: List<Chapter>,
    /** Läuft richtig gut: viele Sterne beim ersten Versuch */
    val strong: List<Chapter>,
)

/**
 * Wochenübersicht – was wurde geübt, wo hakt es? Ohne Wertung, ohne Rangliste.
 * Einzelne Durchgänge zählen höchstens 15 Minuten (falls das Handy offen liegen blieb).
 */
object Weekly {
    const val DAY = 86_400_000L

    private fun chapterOf(levelId: String): Chapter? {
        val n = when {
            Learning.isReview(levelId) -> levelId.removePrefix(Learning.PREFIX).substringBefore('-').toIntOrNull()
            else -> Levels.byId(levelId)?.chapter
        }
        return Levels.chapters.firstOrNull { it.number == n }
    }

    fun summary(plays: List<PlayRecord>, now: Long, todayStart: Long): WeekSummary {
        val weekStart = todayStart - 6 * DAY
        val week = plays.filter { it.at >= weekStart }
        val days = (0..6).map { d ->
            val from = weekStart + d * DAY; val to = from + DAY
            val p = week.filter { it.at in from until to }
            p.count { it.stars > 0 } to p.sumOf { minOf(it.seconds, 900) } / 60
        }
        val topics = week.mapNotNull { chapterOf(it.levelId) }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
        val recent = plays.filter { it.at >= todayStart - 13 * DAY }
        val needsHelp = recent.filter { it.stars == 0 }.mapNotNull { stuck ->
            val solvedLater = recent.any { it.levelId == stuck.levelId && it.stars > 0 && it.at >= stuck.at }
            if (solvedLater) null else chapterOf(stuck.levelId)
        }.distinct()
        val strong = recent.filter { it.stars > 0 }.groupBy { chapterOf(it.levelId) }
            .filter { (c, l) -> c != null && l.size >= 3 && l.count { it.stars == 3 } * 3 >= l.size * 2 }.keys.filterNotNull()
            .filter { it !in needsHelp }
        return WeekSummary(days, week.sumOf { minOf(it.seconds, 900) } / 60, week.filter { it.stars > 0 }.map { it.levelId }.distinct().size,
            days.count { it.first > 0 || it.second > 0 }, topics, needsHelp, strong)
    }
}
