package de.experimentopia.core.model

/**
 * Lernen nach Fach, am Schulstoff entlang: Fachbereiche eines Fachs in der Reihenfolge, in der sie in der Schule
 * meist drankommen (nach der niedrigsten Klassenstufe ihrer Versuche), Versuche je Fachbereich von leicht nach schwer.
 */
object Curriculum {
    fun startGrade(t: Topic) = t.experiments().minOf { it.grades.first }
    fun grades(t: Topic) = t.experiments().let { l -> l.minOf { it.grades.first }..l.maxOf { it.grades.last } }

    /** Fachbereiche eines Fachs; mit Klasse ([grade] > 0) nur die, zu denen es passende Versuche gibt */
    fun topics(s: Subject, grade: Int = 0): List<Topic> =
        Topic.entries.filter { it.subject == s && (grade == 0 || it.experiments().any { e -> grade in e.grades }) }
            .sortedWith(compareBy({ startGrade(it) }, { it.ordinal }))

    /** Versuche eines Fachbereichs: zur Klasse passende zuerst, jeweils von leicht nach schwer */
    fun experiments(t: Topic, grade: Int = 0): List<Experiment> =
        t.experiments().sortedWith(compareBy({ if (grade == 0 || grade in it.grades) 0 else 1 }, { it.grades.first }, { it.grades.last }))

    /** Nächster Versuch: der erste noch nicht gemachte, passend zur Klasse; null = alles geschafft */
    fun next(t: Topic, done: Set<String>, grade: Int = 0): Experiment? =
        experiments(t, grade).firstOrNull { it.id !in done && (grade == 0 || grade in it.grades) } ?: experiments(t, grade).firstOrNull { it.id !in done }

    fun progress(t: Topic, done: Set<String>) = t.experiments().count { it.id in done } to t.experiments().size
}
