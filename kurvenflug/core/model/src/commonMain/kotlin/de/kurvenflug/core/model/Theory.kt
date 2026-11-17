package de.kurvenflug.core.model

/**
 * „📖 Theorie“-Knopf in jeder Übung: die passende Erklärung zum Nachlesen, jederzeit und ohne Sterne-Abzug.
 * Kapitel-Level zeigen die Einführung ihres Kapitels; Missionen ohne eigenes Kapitel (Sturm, Duell, Andromeda …)
 * die Einführung des Kapitels mit derselben Bahnart. Dazu die Merkformel der Bahnart.
 */
data class Theory(val emoji: String, val title: String, val grades: String, val points: List<String>, val formula: String?)

object Theories {
    fun of(level: Level, chapter: Chapter?): Theory? {
        val own = chapter?.takeIf { it.intro.isNotEmpty() && it.levels.any { l -> l.kind == level.kind } }
            ?: Levels.chapters.firstOrNull { it.number == level.chapter && it.intro.isNotEmpty() }
        val c = own ?: Levels.chapters.firstOrNull { c -> c.intro.isNotEmpty() && c.levels.any { it.kind == level.kind } } ?: return null
        val formula = Templates.tip(level.kind).takeIf { it.isNotBlank() }?.let { "${Templates.label(level.kind)}: $it" }
        return Theory(c.emoji, c.title, c.grades, c.intro, formula)
    }
}
