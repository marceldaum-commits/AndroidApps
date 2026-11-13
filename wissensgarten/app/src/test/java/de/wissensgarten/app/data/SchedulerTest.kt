package de.wissensgarten.app.data

import org.junit.Assert.*
import org.junit.Test

class SchedulerTest {
    private val day = Scheduler.DAY_MS
    private val t0 = 1_700_000_000_000L
    private fun seed() = Card("x", "F", "A")

    @Test fun seedIsNeverThirsty() {
        assertFalse(Scheduler.isThirsty(seed(), t0 + 1000 * day))
        assertEquals(Stage.SEED, Stage.of(seed()))
    }

    @Test fun retrievabilityIsNinetyPercentAfterStabilityDays() {
        val c = Scheduler.review(seed(), Grade.GOOD, t0)
        val r = Scheduler.retrievability(c, t0 + (c.stability * day).toLong())
        assertEquals(0.9, r, 0.001)
        assertFalse(Scheduler.isThirsty(c, t0 + (c.stability * day).toLong() - 1000))
        assertTrue(Scheduler.isThirsty(c, t0 + (c.stability * day).toLong() + 60_000))
    }

    @Test fun goodReviewOnTimeGrowsStability() {
        var c = Scheduler.review(seed(), Grade.GOOD, t0)
        var t = t0
        repeat(5) {
            t = Scheduler.dueAt(c)
            val next = Scheduler.review(c, Grade.GOOD, t)
            assertTrue("Stabilität muss wachsen", next.stability > c.stability * 1.5)
            c = next
        }
        assertTrue(Stage.of(c) >= Stage.BLOOM)
    }

    @Test fun forgettingShrinksStabilityAndCountsLapse() {
        val c = Scheduler.review(seed(), Grade.EASY, t0)
        val f = Scheduler.review(c, Grade.AGAIN, Scheduler.dueAt(c))
        assertTrue(f.stability < c.stability)
        assertEquals(1, f.lapses)
        assertTrue(f.difficulty > c.difficulty)
    }

    @Test fun gradesAreOrdered() {
        val c = Scheduler.review(seed(), Grade.GOOD, t0)
        val t = Scheduler.dueAt(c)
        val days = Grade.entries.map { Scheduler.previewDays(c, it, t) }
        assertEquals(days.sorted(), days)
    }

    @Test fun earlyReviewNeverShrinks() {
        val c = Scheduler.review(seed(), Grade.GOOD, t0)
        val early = Scheduler.review(c, Grade.HARD, t0 + 1000)
        assertTrue(early.stability >= c.stability)
    }

    @Test fun wiltGrowsOverTime() {
        val c = Scheduler.review(seed(), Grade.GOOD, t0)
        assertEquals(0f, Scheduler.wilt(c, t0), 0.0001f)
        assertTrue(Scheduler.wilt(c, t0 + 30 * day) > 0.5f)
    }

    @Test fun sessionLimitsSeedsPerDay() {
        val bed = Bed("b", "B", "x", 0f, List(30) { Card("c$it", "f", "b") })
        val s = GardenState(beds = listOf(bed))
        assertEquals(Session.SEEDS_PER_DAY, Session.build(s, t0).size)
        val used = s.copy(seedDay = t0 / day, seedsToday = 7)
        assertEquals(3, Session.build(used, t0).size)
    }
}
