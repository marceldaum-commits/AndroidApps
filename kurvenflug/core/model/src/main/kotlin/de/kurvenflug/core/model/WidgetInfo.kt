package de.kurvenflug.core.model

/** Was das Startbildschirm-Widget zeigt – alles aus dem Spielstand auf dem Gerät. */
data class WidgetInfo(
    val stars: Int,
    val maxStars: Int,
    val cells: Int,
    val dailyId: String,
    val dailyLabel: String,
    val dailyDone: Boolean,
    /** fälliges Auffrischen: Kennung + „🌙 Parabeln“ */
    val reviewId: String?,
    val reviewTitle: String?,
    /** „Weiter, wo du warst“: Kennung + „🌙 Parabeln · Level 2“ */
    val nextId: String?,
    val nextTitle: String?,
    val volt: String,
) {
    companion object {
        fun of(progress: Map<String, Played>, cells: Int, day: Long, now: Long, grade: Int?): WidgetInfo {
            val daily = Daily.forDay(day)
            val dailyDone = (progress[daily.id]?.stars ?: 0) > 0
            val review = Learning.review(progress, now, day)
            val reviewChapter = review?.let { r -> Levels.chapters.firstOrNull { it.number == r.chapter } }
            val next = Plan.next(Levels.chapters, grade, progress)
            val nextChapter = next?.let { n -> Levels.chapters.firstOrNull { it.number == n.chapter } }
            val stars = progress.filterKeys { Levels.byId(it) != null }.values.sumOf { it.stars }
            val volt = when {
                !dailyDone -> "Neue Tagesmission wartet! 🛰️"
                review != null -> "Kurz auffrischen? Dann sitzt es! 🧠"
                next != null -> "Tagesmission geschafft – stark! 💪"
                else -> "Alles geschafft – du bist ein Profi! 🏆"
            }
            return WidgetInfo(stars, Levels.all.size * 3, cells, daily.id, Daily.label(daily), dailyDone,
                review?.id, reviewChapter?.let { "${it.emoji} ${it.title}" },
                next?.id, nextChapter?.let { "${it.emoji} ${it.title} · Level ${next.number}" }, volt)
        }
    }
}
