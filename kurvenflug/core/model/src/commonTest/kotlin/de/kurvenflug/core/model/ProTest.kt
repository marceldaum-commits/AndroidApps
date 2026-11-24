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
        // Seit 2.6.0: KurvenFlug = Klasse 5–10, alle 26 Kapitel und 162 Level frei (Pro = Extras, nicht Schulstoff)
        assertEquals(26, Levels.chapters.size)
        assertEquals(162, Levels.all.size)
        for (c in Levels.chapters) { assertFalse(Pro.isProChapter(c.number)); for (l in c.levels) assertTrue(Pro.open(l.id, false, true, none), l.id) }
    }

    @Test fun proContentLockedWhenShopOffered() {
        assertFalse(Pro.open(ChapterExtreme.id(2), false, true, none))
        assertFalse(Pro.open("andro-7-1", false, true, none))
        assertTrue(Pro.open("andro-6-3", false, true, none))
        assertEquals(Tier.HARD, Pro.maxStormTier(false, true))
        assertEquals(2, Pro.maxProfiles(false, true))
        // mit Pro alles offen
        assertTrue(Pro.open(ChapterExtreme.id(2), true, true, none))
        assertTrue(Pro.open("andro-40-2", true, true, none))
        assertEquals(Tier.MAX, Pro.maxStormTier(true, true))
    }

    @Test fun existingProgressIsKept() {
        // kein Kapitel ist gesperrt (Oberstufe seit 2.6.0 nicht mehr in KurvenFlug)
        assertTrue(Levels.chapters.none { Pro.chapterLocked(it.number, false, true, none) })
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
