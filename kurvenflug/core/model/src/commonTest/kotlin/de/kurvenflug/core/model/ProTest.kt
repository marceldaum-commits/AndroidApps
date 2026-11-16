package de.kurvenflug.core.model

import kotlin.test.*

class ProTest {
    private val none = emptyMap<String, Int>()

    @Test fun nothingLockedWithoutShop() {
        for (l in Levels.all) assertTrue(Pro.open(l.id, pro = false, locking = false, stars = none))
        assertEquals(Tier.MAX, Pro.maxStormTier(false, false))
        assertFalse(Pro.planetLocked(10, false, false, none))
    }

    @Test fun freeKeepsAllSchoolContentUpToGrade10() {
        val free = Levels.chapters.filter { !Pro.isProChapter(it.number) }
        assertEquals(24, free.size)
        for (c in free) for (l in c.levels) assertTrue(Pro.open(l.id, false, true, none), l.id)
        assertEquals(35, Levels.chapters.filter { Pro.isProChapter(it.number) }.sumOf { it.levels.size })
    }

    @Test fun proContentLockedWhenShopOffered() {
        assertFalse(Pro.open("19-1", false, true, none))
        assertFalse(Pro.open(ChapterExtreme.id(2), false, true, none))
        assertFalse(Pro.open("andro-7-1", false, true, none))
        assertTrue(Pro.open("andro-6-3", false, true, none))
        assertEquals(Tier.HARD, Pro.maxStormTier(false, true))
        assertEquals(2, Pro.maxProfiles(false, true))
        // mit Pro alles offen
        assertTrue(Pro.open("19-1", true, true, none))
        assertTrue(Pro.open("andro-40-2", true, true, none))
        assertEquals(Tier.MAX, Pro.maxStormTier(true, true))
    }

    @Test fun existingProgressIsKept() {
        // ein Stern im Kapitel Ableitung → ganzes Kapitel bleibt offen
        val s = mapOf("19-1" to 2)
        assertTrue(Pro.open("19-6", false, true, s))
        assertFalse(Pro.chapterLocked(19, false, true, s))
        assertTrue(Pro.chapterLocked(20, false, true, s))
        // geschaffte Extrem-Prüfung bleibt
        assertTrue(Pro.open(ChapterExtreme.id(5), false, true, mapOf(ChapterExtreme.id(5) to 1)))
        // schon auf Planet 7 gewesen → Andromeda bleibt offen
        val a = mapOf("andro-7-1" to 3)
        assertTrue(Pro.open("andro-9-2", false, true, a))
        assertFalse(Pro.planetLocked(12, false, true, a))
    }

    @Test fun stormStaysHardWithoutPro() {
        assertTrue(Storm.level(3, 20, Pro.maxStormTier(false, true)).title.contains("Schwer"))
        assertTrue(Storm.level(3, 20, Pro.maxStormTier(true, true)).title.contains("Extrem"))
    }
}
