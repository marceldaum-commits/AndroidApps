package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class Season2Test {
    @Test fun everyRescueHasFiveSolvableMixedStages() {
        for (m in Season2.crew) {
            val s = Season2.stages(m.n)
            assertEquals(5, s.size)
            s.forEach { l ->
                assertTrue("${l.id} lösbar", Flight.fly(l, l.solution).success)
                assertFalse("${l.id} Startwerte", Flight.fly(l, l.start).success)
                assertNotEquals("${l.id} Ersatz-Mission", Gen.FALLBACK, l.title)
            }
            assertTrue("Rettung ${m.n}: gemischte Themen", s.map { it.kind }.toSet().size >= 3)
            assertEquals(s, Season2.stages(m.n))   // immer gleich
            assertEquals(5, s.map { it.id }.toSet().size)
        }
    }

    @Test fun rescuesUnlockStepByStepAndGoBeyondTheFullAurora() {
        assertNotNull(Season2.lock(1, 2, 0))
        assertNull(Season2.lock(1, 3, 0))
        assertNull(Season2.lock(4, 15, 0))
        assertNotNull(Season2.lock(5, 17, 0))
        assertNull(Season2.lock(5, 18, 0))
        assertNotNull(Season2.lock(6, 18, 2))
        assertNull(Season2.lock(8, 18, 9))
    }

    @Test fun goldNeedsThreeStarsEverywhereInTheChapter() {
        val c = Levels.chapters.first { it.number == 2 }
        val two = c.levels.associate { it.id to 3 } + (c.levels.last().id to 2)
        assertTrue(Season2.gold(Levels.chapters, two).isEmpty())
        assertEquals(listOf(2), Season2.gold(Levels.chapters, c.levels.associate { it.id to 3 }).map { it.chapter })
    }
}
