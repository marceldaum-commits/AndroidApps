package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class DailyTest {
    private val days = (20_400L until 20_400L + 3 * 365)   // drei Jahre ab Nov. 2025

    @Test fun everyDayIsSolvableAndNotTrivial() {
        var fallbacks = 0
        for (d in days) {
            val l = Daily.forDay(d)
            assertEquals(Daily.id(d), l.id)
            assertTrue("Tag $d nicht lösbar", Flight.fly(l, l.solution).success)
            assertFalse("Tag $d mit Startwerten gelöst", Flight.fly(l, l.start).success)
            l.params.forEach { p -> assertEquals("Tag $d/${p.key}", l.solution.getValue(p.key), p.snap(l.solution.getValue(p.key)), 1e-9) }
            if (l.task.isEmpty() || l.kind == Kind.LINE && l.title != "Tagesmission") fallbacks++
            if (Levels.all.any { it.targets == l.targets && it.kind == l.kind }) fallbacks++
        }
        assertEquals("Notfall-Level benutzt", 0, fallbacks)
    }

    @Test fun sameDaySameMissionAndKindsRotate() {
        assertEquals(Daily.forDay(20_500), Daily.forDay(20_500))
        assertEquals(4, (20_500L until 20_504L).map { Daily.forDay(it).kind }.toSet().size)
        assertNotEquals(Daily.forDay(20_500).targets, Daily.forDay(20_504).targets)
    }
}
