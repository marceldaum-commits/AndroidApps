package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChapterLinksTest {
    @Test fun everyChapterHasAUniqueName() {
        Levels.chapters.forEach { assertTrue(it.number in ChapterLinks.NAMES, "Kapitel ${it.number} ohne Link-Namen") }
        assertEquals(ChapterLinks.NAMES.size, ChapterLinks.NAMES.values.toSet().size)
        ChapterLinks.NAMES.values.forEach { assertTrue(Regex("[a-z0-9-]+").matches(it), it) }
    }

    @Test fun extractsFromHashAndSearch() {
        assertEquals("rendezvous", ChapterLinks.extract("#kapitel=rendezvous"))
        assertEquals("energie", ChapterLinks.extract("?src=x&kapitel=Energie "))
        assertEquals("7", ChapterLinks.extract(" #kapitel=7"))
        assertNull(ChapterLinks.extract("#pro=KF123"))
        assertNull(ChapterLinks.extract("#unterkapitel=energie"))
    }

    @Test fun resolvesNamesNumbersAndUnknown() {
        assertEquals(7, ChapterLinks.chapter("rendezvous")?.number)
        assertEquals(12, ChapterLinks.chapter("energie")?.number)
        assertEquals(7, ChapterLinks.chapter("7")?.number)
        assertNull(ChapterLinks.chapter("gibtsnicht"))
        assertNull(ChapterLinks.chapter("999"))
    }

    @Test fun opensFirstUnsolvedLevel() {
        val c = ChapterLinks.chapter("geraden")!!
        assertEquals(c.levels[0].id, ChapterLinks.level(c, emptyMap()).id)
        assertEquals(c.levels[2].id, ChapterLinks.level(c, mapOf(c.levels[0].id to 3, c.levels[1].id to 1)).id)
        assertEquals(c.levels[0].id, ChapterLinks.level(c, c.levels.associate { it.id to 3 }).id)
    }
}
