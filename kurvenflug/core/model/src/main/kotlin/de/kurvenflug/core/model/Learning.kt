package de.kurvenflug.core.model

/** Wie sicher ein Thema sitzt – für Kind und Eltern auf einen Blick. */
enum class Mastery(val label: String, val emoji: String) {
    NEW("Neu", "🌱"),
    PRACTICING("Übt noch", "🔧"),
    SECURE("Sicher", "⭐"),
}

/** Ein gespieltes Level aus dem Spielstand: beste Sterne und wann es (zuerst) geschafft wurde. */
data class Played(val stars: Int, val at: Long)

/** Lernstand eines Kapitels. [lastPractice] = 0 heißt: noch nie geübt. */
data class TopicStatus(
    val chapter: Chapter,
    val solved: Int,
    val stars: Int,
    val mastery: Mastery,
    val lastPractice: Long,
    val reviewDue: Boolean,
) {
    val maxStars get() = chapter.levels.size * 3
    val share get() = if (maxStars == 0) 0f else stars.toFloat() / maxStars
}

/**
 * Lernstand und Auffrischen (verteiltes Wiederholen).
 *
 * Recherche: Üben in Abständen und gemischtes Üben wirken in Schulstudien deutlich besser als Pauken am Stück
 * (Meta-Analysen um g ≈ 0,7; Rohrer u. a., 7. Klasse, d ≈ 0,8). Deshalb holt die App geschaffte Themen nach ein paar
 * Tagen von selbst wieder hervor – ein kurzes Level, ohne Druck: verpasste Tage verfallen einfach.
 *
 * Eine Auffrischung hat die Kennung `review-<Kapitel>-<Tag>-<Quelle>`; Quelle ist „gen“ (neu gewürfelt) oder die
 * Kennung eines Kapitel-Levels. So lässt sich das Level allein aus der Kennung wieder bauen (z. B. nach dem Drehen).
 */
object Learning {
    const val PREFIX = "review-"
    const val DAY_MS = 86_400_000L
    /** Pause bis zur nächsten Auffrischung: sichere Themen seltener, wackelige öfter */
    const val SECURE_DAYS = 7
    const val PRACTICING_DAYS = 2

    /** Kapitel, für die der Generator neue Level würfeln kann – sonst wird ein geschafftes Level wiederholt. */
    private fun genKind(chapter: Int, day: Long): Kind? = when (chapter) {
        1 -> Kind.POINT; 2 -> Kind.LINE; 7 -> Kind.RENDEZVOUS; 3 -> Kind.PARABOLA; 4 -> Kind.THROW
        5 -> if (day % 2 == 0L) Kind.SINE else Kind.EXP
        else -> null
    }

    fun isReview(id: String) = id.startsWith(PREFIX)
    fun reviewId(chapter: Int, day: Long, source: String) = "$PREFIX$chapter-$day-$source"

    private fun chapterOfReview(id: String): Int? = id.removePrefix(PREFIX).substringBefore('-').toIntOrNull()
    private fun dayOfReview(id: String): Long? = id.removePrefix(PREFIX).split('-').getOrNull(1)?.toLongOrNull()

    fun mastery(chapter: Chapter, progress: Map<String, Played>): Mastery {
        val solved = chapter.levels.count { (progress[it.id]?.stars ?: 0) > 0 }
        val stars = chapter.levels.sumOf { progress[it.id]?.stars ?: 0 }
        return when {
            solved == 0 -> Mastery.NEW
            solved == chapter.levels.size && stars * 2 >= chapter.levels.size * 5 -> Mastery.SECURE   // Schnitt ≥ 2,5 ⭐
            else -> Mastery.PRACTICING
        }
    }

    fun status(progress: Map<String, Played>, now: Long): List<TopicStatus> = Levels.chapters.map { c ->
        val m = mastery(c, progress)
        val times = c.levels.mapNotNull { progress[it.id]?.takeIf { p -> p.stars > 0 }?.at } +
            progress.filterKeys { isReview(it) && chapterOfReview(it) == c.number }.values.map { it.at }
        val last = times.maxOrNull() ?: 0L
        val pause = (if (m == Mastery.SECURE) SECURE_DAYS else PRACTICING_DAYS) * DAY_MS
        TopicStatus(c, c.levels.count { (progress[it.id]?.stars ?: 0) > 0 }, c.levels.sumOf { progress[it.id]?.stars ?: 0 },
            m, last, m != Mastery.NEW && last > 0 && now - last >= pause)
    }

    /** Heute schon aufgefrischt? Dann reicht es – kurze Einheiten, kein Dauerdruck. */
    fun doneToday(progress: Map<String, Played>, day: Long) = progress.keys.any { isReview(it) && dayOfReview(it) == day }

    /**
     * Das heutige Auffrisch-Level: wackelige Themen zuerst, dann die mit wenig Sternen, dann die am längsten nicht geübten.
     * null = nichts fällig (oder heute schon erledigt).
     */
    fun review(progress: Map<String, Played>, now: Long, day: Long): Level? {
        if (doneToday(progress, day)) return null
        val pick = status(progress, now).filter { it.reviewDue }
            .sortedWith(compareBy<TopicStatus>({ if (it.mastery == Mastery.PRACTICING) 0 else 1 }, { it.share }, { it.lastPractice }))
            .firstOrNull() ?: return null
        val source = if (genKind(pick.chapter.number, day) != null) "gen"
        else pick.chapter.levels.filter { (progress[it.id]?.stars ?: 0) > 0 }.minByOrNull { progress.getValue(it.id).stars }?.id ?: return null
        return levelFor(reviewId(pick.chapter.number, day, source))
    }

    /** Baut ein Auffrisch-Level allein aus seiner Kennung. */
    fun levelFor(id: String): Level? {
        if (!isReview(id)) return null
        val parts = id.removePrefix(PREFIX).split('-', limit = 3)
        val chapter = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val day = parts.getOrNull(1)?.toLongOrNull() ?: return null
        val source = parts.getOrNull(2) ?: return null
        val c = Levels.chapters.firstOrNull { it.number == chapter } ?: return null
        val base = if (source == "gen") genKind(chapter, day)?.let { Gen.level(it, day * 131 + chapter * 7919, 1, id) }
        else Levels.byId(source)
        return base?.copy(id = id, chapter = chapter, number = 0, title = "Auffrischen: ${c.title}")
    }
}
