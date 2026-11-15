package de.kurvenflug.core.model

/** Fach eines Kapitels – zum Filtern in der Galaxie. */
enum class Subject(val label: String, val emoji: String) { MATH("Mathe", "📐"), PHYSICS("Physik", "⚛️") }

/** Klassenstufen eines Kapitels, aus „Klasse 7–8“ bzw. „Physik Klasse 7–10“. */
val Chapter.gradeRange: IntRange
    get() {
        val n = Regex("\\d+").findAll(grades).map { it.value.toInt() }.toList()
        return if (n.isEmpty()) 5..10 else n.first()..n.last()
    }

val Chapter.subject: Subject get() = if (grades.contains("Physik")) Subject.PHYSICS else Subject.MATH

/** Ein Abschnitt der Galaxie, z. B. „Deine Klasse 7“. */
data class Section(val title: String, val hint: String, val chapters: List<Chapter>)

/**
 * Ordnung für die Startseite: Kapitel passend zur Klasse zuerst, Älteres zum Wiederholen, Späteres für Neugierige.
 * Nichts wird gesperrt – alle Kapitel bleiben spielbar, sie stehen nur an der richtigen Stelle.
 */
object Plan {
    val grades = 5..10

    fun sections(chapters: List<Chapter>, grade: Int?, subject: Subject? = null): List<Section> {
        val list = chapters.filter { subject == null || it.subject == subject }
        if (grade == null) return listOf(Section("Alle Kapitel", "nach Klassenstufe sortiert", list)).filter { it.chapters.isNotEmpty() }
        return listOf(
            Section("Deine Klasse $grade", "Das passt gerade zum Unterricht", list.filter { grade in it.gradeRange }),
            Section("Zum Wiederholen", "Schon dran gewesen – frisch halten", list.filter { it.gradeRange.last < grade }),
            Section("Für später", "Für Neugierige – schon mal reinschnuppern", list.filter { it.gradeRange.first > grade }),
        ).filter { it.chapters.isNotEmpty() }
    }

    /**
     * „Weiter, wo du warst“: im zuletzt gespielten Kapitel das nächste offene Level; ist es fertig, das erste offene
     * Level in einem Kapitel der eigenen Klasse, sonst irgendwo. null = alles geschafft.
     */
    fun next(chapters: List<Chapter>, grade: Int?, progress: Map<String, Played>): Level? {
        fun solved(l: Level) = (progress[l.id]?.stars ?: 0) > 0
        fun firstOpen(c: Chapter) = c.levels.firstOrNull { !solved(it) }
        val last = chapters.flatMap { c -> c.levels.mapNotNull { l -> progress[l.id]?.takeIf { it.stars > 0 }?.let { c to it.at } } }
            .maxByOrNull { it.second }?.first
        last?.let { firstOpen(it) }?.let { return it }
        val mine = if (grade == null) chapters else chapters.filter { grade in it.gradeRange }
        return (mine + chapters).firstNotNullOfOrNull { firstOpen(it) }
    }
}
