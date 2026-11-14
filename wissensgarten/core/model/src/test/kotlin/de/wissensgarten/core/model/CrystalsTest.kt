package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class CrystalsTest {
    @Test fun hardCardBecomesCrystalWhenItFinallyHolds() {
        val day = Scheduler.DAY_MS
        var m = Memory()
        var t = 0L
        // zweimal vergessen …
        m = Scheduler.review(m, Rating.GOOD, t); t += 2 * day
        m = Scheduler.review(m, Rating.AGAIN, t); t += 1 * day
        m = Scheduler.review(m, Rating.AGAIN, t)
        assertFalse(Crystals.isCrystal(m))
        assertNotNull(Crystals.progress(m))
        // … dann immer wieder gewusst, bis sie sitzt
        var formedAt = -1
        for (i in 1..12) {
            t = Scheduler.dueAt(m)
            val after = Scheduler.review(m, Rating.GOOD, t)
            if (Crystals.formed(m, after)) formedAt = i
            m = after
        }
        assertTrue("Kristall entsteht irgendwann", formedAt > 0)
        assertTrue(Crystals.isCrystal(m))
    }

    @Test fun easyCardsAreNoCrystals() {
        assertFalse(Crystals.isCrystal(Memory(stability = 100.0, lapses = 0, reps = 5)))
        assertNull(Crystals.progress(Memory(stability = 100.0, lapses = 1, reps = 5)))
    }
}
