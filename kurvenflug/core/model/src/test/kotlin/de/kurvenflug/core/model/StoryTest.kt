package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class StoryTest {
    @Test fun everyChapterHasExactlyOnePart() {
        assertEquals(Levels.chapters.map { it.number }.sorted(), Story.parts.map { it.chapter }.sorted())
        assertEquals(Story.parts.size, Story.parts.map { it.name }.toSet().size)
        assertTrue(Story.intro.any { it.contains("${Story.parts.size}") })
    }

    @Test fun partIsFoundWhenTheWholeChapterIsSolved() {
        val c = Levels.chapters.first { it.number == 2 }
        val almost = c.levels.dropLast(1).associate { it.id to 1 }
        assertTrue(Story.found(Levels.chapters, almost).isEmpty())
        val all = c.levels.associate { it.id to 1 }
        assertEquals(listOf("Linker Flügel"), Story.found(Levels.chapters, all).map { it.name })
        assertNotNull(Story.arrival(2))
    }
}
