package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

class CurriculumTest {
    @Test fun upcomingIsNotAlreadyAChapter() {
        val have = Levels.chapters.map { it.title.lowercase() }.toSet()
        Curriculum.upcoming.forEach { u -> assertTrue(u.title.lowercase() !in have, "${u.title} ist schon ein Kapitel") }
        assertTrue(Curriculum.upcoming.map { it.title }.toSet().size == Curriculum.upcoming.size, "doppelt")
        Curriculum.upcoming.forEach { u -> assertTrue(u.grades.first in 5..13 && u.grades.last in 5..13, u.title) }
        // ohne Datum, ohne Kaufhinweis
        Curriculum.upcoming.forEach { u -> assertTrue(listOf("Pro", "€", "kaufen", "202").none { it in u.what || it in u.title }, u.title) }
    }
}
