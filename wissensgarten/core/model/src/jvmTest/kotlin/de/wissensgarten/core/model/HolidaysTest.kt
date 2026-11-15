package de.wissensgarten.core.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidaysTest {
    private val day = Scheduler.DAY_MS
    @After fun reset() { Scheduler.holidays = emptyList() }

    @Test fun holidayTimeCountsAQuarter() {
        val p = listOf(10 * day..30 * day)
        assertEquals(10 * day, Holidays.elapsed(0, 10 * day, p))
        assertEquals(10 * day + 5 * day, Holidays.elapsed(0, 30 * day, p))
        assertEquals(15 * day + 5 * day, Holidays.elapsed(0, 35 * day, p))
    }

    @Test fun plantsWiltSlowerInHolidays() {
        val m = Memory(stability = 5.0, lastReview = 0, reps = 3)
        val normal = Scheduler.retrievability(m, 20 * day)
        Scheduler.holidays = listOf(0L..Long.MAX_VALUE)
        val holiday = Scheduler.retrievability(m, 20 * day)
        assertTrue(holiday > normal)
        assertEquals(Scheduler.retrievability(m.copy(), 5 * day).let { 1.0 / (1.0 + 5.0 / 45.0) }, holiday, 1e-9)
    }

    @Test fun dueAtIsInverseOfElapsed() {
        val p = listOf(3 * day..13 * day, 20 * day..Long.MAX_VALUE)
        for (g in listOf(1 * day, 3 * day, 4 * day, 5 * day, 12 * day, 40 * day)) {
            val t = Holidays.after(0, g, p)
            assertEquals(g.toDouble(), Holidays.elapsed(0, t, p).toDouble(), 1000.0)
        }
        Scheduler.holidays = p
        val m = Memory(stability = 4.0, lastReview = 0, reps = 2)
        assertEquals(0.9, Scheduler.retrievability(m, Scheduler.dueAt(m)), 1e-4)
    }

    @Test fun toggleAndStore() {
        var p = Holidays.toggle(emptyList(), true, 100 * day)
        assertTrue(Holidays.isActive(p))
        assertEquals(p, Holidays.toggle(p, true, 101 * day))
        p = Holidays.toggle(p, false, 110 * day)
        assertFalse(Holidays.isActive(p))
        assertEquals(listOf(100 * day..110 * day), p)
        assertEquals(p, Holidays.decode(Holidays.encode(p)))
        assertEquals(emptyList<LongRange>(), Holidays.decode("kaputt;1-x"))
    }
}
