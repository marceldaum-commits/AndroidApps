package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class LearningTest {
    private val day = 20_500L
    private val now = day * Learning.DAY_MS + 12 * 3_600_000L
    private fun chapter(n: Int) = Levels.chapters.first { it.number == n }
    private fun solvedAll(n: Int, stars: Int, daysAgo: Int) =
        chapter(n).levels.associate { it.id to Played(stars, now - daysAgo * Learning.DAY_MS) }

    @Test fun masteryGrowsFromNewToSecure() {
        val c = chapter(2)
        assertEquals(Mastery.NEW, Learning.mastery(c, emptyMap()))
        assertEquals(Mastery.PRACTICING, Learning.mastery(c, mapOf(c.levels[0].id to Played(3, now))))
        assertEquals(Mastery.PRACTICING, Learning.mastery(c, solvedAll(2, 2, 0)))  // alles geschafft, aber im Schnitt nur 2 ⭐
        assertEquals(Mastery.SECURE, Learning.mastery(c, solvedAll(2, 3, 0)))
    }

    @Test fun nothingDueRightAfterPlaying() {
        assertNull(Learning.review(solvedAll(2, 2, 0), now, day))
        assertNull(Learning.review(emptyMap(), now, day))
    }

    @Test fun shakyTopicsComeBackSoonerThanSecureOnes() {
        // wackelig nach 2 Tagen fällig, sicher erst nach 7
        assertNotNull(Learning.review(solvedAll(2, 2, 2), now, day))
        assertNull(Learning.review(solvedAll(2, 3, 3), now, day))
        assertNotNull(Learning.review(solvedAll(2, 3, 7), now, day))
    }

    @Test fun shakyTopicIsPickedFirstAndLevelIsRebuiltFromItsId() {
        val p = solvedAll(2, 3, 10) + solvedAll(3, 1, 3)
        val r = Learning.review(p, now, day)!!
        assertEquals(3, r.chapter)
        assertTrue(Learning.isReview(r.id))
        assertEquals(r, Learning.levelFor(r.id))
        assertTrue("Auffrisch-Level muss lösbar sein", Flight.fly(r, r.solution).success)
    }

    @Test fun chaptersWithoutGeneratorReplayTheWeakestSolvedLevel() {
        val c = chapter(14)
        val p = c.levels.associate { it.id to Played(if (it.id == "14-3") 1 else 3, now - 8 * Learning.DAY_MS) }
        val r = Learning.review(p, now, day)!!
        assertEquals("${Learning.PREFIX}14-$day-14-3", r.id)
        assertEquals(Levels.byId("14-3")!!.solution, r.solution)
        assertEquals(r, Learning.levelFor(r.id))
    }

    @Test fun reviewCountsAsPracticeAndOnlyOncePerDay() {
        val p = solvedAll(2, 2, 3)
        val r = Learning.review(p, now, day)!!
        val after = p + (r.id to Played(3, now))
        assertNull("heute schon aufgefrischt", Learning.review(after, now, day))
        assertFalse(Learning.status(after, now).first { it.chapter.number == 2 }.reviewDue)
    }

    @Test fun generatedReviewsAreSolvableForManyDays() {
        for (c in listOf(1, 2, 3, 4, 5, 7)) for (d in day until day + 60) {
            val l = Learning.levelFor(Learning.reviewId(c, d, "gen"))!!
            assertTrue("$c/$d", Flight.fly(l, l.solution).success)
        }
    }
}
