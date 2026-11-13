package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class SchedulerTest {
    private val day = Scheduler.DAY_MS
    private val t0 = 1_700_000_000_000L
    private val seed = Memory()

    @Test fun seedIsNeverThirsty() {
        assertFalse(Scheduler.isThirsty(seed, t0 + 1000 * day))
        assertEquals(Stage.SEED, Stage.of(seed))
    }

    @Test fun retrievabilityIsNinetyPercentAfterStabilityDays() {
        val m = Scheduler.review(seed, Rating.GOOD, t0)
        val due = Scheduler.dueAt(m)
        assertEquals(0.9, Scheduler.retrievability(m, due), 0.001)
        assertFalse(Scheduler.isThirsty(m, due - 1000))
        assertTrue(Scheduler.isThirsty(m, due + 60_000))
    }

    @Test fun goodReviewsOnTimeGrowToBloom() {
        var m = Scheduler.review(seed, Rating.GOOD, t0)
        repeat(5) {
            val next = Scheduler.review(m, Rating.GOOD, Scheduler.dueAt(m))
            assertTrue(next.stability > m.stability * 1.5)
            m = next
        }
        assertTrue(Stage.of(m) >= Stage.BLOOM)
    }

    @Test fun forgettingShrinksStability() {
        val m = Scheduler.review(seed, Rating.EASY, t0)
        val f = Scheduler.review(m, Rating.AGAIN, Scheduler.dueAt(m))
        assertTrue(f.stability < m.stability)
        assertEquals(1, f.lapses)
        assertTrue(f.difficulty > m.difficulty)
    }

    @Test fun ratingsAreOrdered() {
        val m = Scheduler.review(seed, Rating.GOOD, t0)
        val days = Rating.entries.map { Scheduler.previewDays(m, it, Scheduler.dueAt(m)) }
        assertEquals(days.sorted(), days)
    }

    @Test fun earlyReviewNeverShrinks() {
        val m = Scheduler.review(seed, Rating.GOOD, t0)
        assertTrue(Scheduler.review(m, Rating.HARD, t0 + 1000).stability >= m.stability)
    }

    @Test fun wiltGrowsOverTime() {
        val m = Scheduler.review(seed, Rating.GOOD, t0)
        assertEquals(0f, Scheduler.wilt(m, t0), 0.0001f)
        assertTrue(Scheduler.wilt(m, t0 + 30 * day) > 0.5f)
    }
}

class SessionTest {
    private val t0 = 1_700_000_000_000L
    private fun card(id: String, bed: String, m: Memory = Memory()) = StudyCard(id, bed, "f", "b", null, null, m)

    @Test fun seedsAreLimitedAndMixedAcrossBeds() {
        val cards = List(10) { card("a$it", "A") } + List(10) { card("b$it", "B") }
        val s = SessionBuilder.build(cards, t0, 6)
        assertEquals(6, s.size)
        assertEquals(3, s.count { it.bedId == "A" })
    }

    @Test fun thirstyComeFirst() {
        val old = Scheduler.review(Memory(), Rating.GOOD, t0 - 30 * Scheduler.DAY_MS)
        val cards = listOf(card("s", "A"), card("t", "A", old))
        assertEquals("t", SessionBuilder.build(cards, t0, 5).first().id)
    }

    @Test fun seedsLeftResetsNextDay() {
        val today = t0 / Scheduler.DAY_MS
        assertEquals(3, SessionBuilder.seedsLeft(10, today, 7, t0))
        assertEquals(10, SessionBuilder.seedsLeft(10, today - 1, 7, t0))
    }

    @Test fun optionsContainAnswerOnceAndAreStable() {
        val c = StudyCard("x", "A", "3×4", "12", listOf("7", "14", "16"), null, Memory())
        assertEquals(4, c.options().size)
        assertTrue("12" in c.options())
        assertEquals(c.options(), c.options())
    }

    @Test fun levelRangeLabels() {
        assertEquals("Klasse 5–8", Levels.range(listOf(5, 6, 7, 8)))
        assertEquals("Klasse 3", Levels.range(listOf(3)))
        assertEquals("ab Klasse 10", Levels.range(listOf(10, 11, 14)))
        assertEquals("Erwachsene", Levels.range(listOf(14)))
    }
}
