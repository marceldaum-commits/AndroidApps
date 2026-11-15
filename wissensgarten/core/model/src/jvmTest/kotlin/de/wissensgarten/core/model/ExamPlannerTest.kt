package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class ExamPlannerTest {
    private val day = Scheduler.DAY_MS
    private val now = 20_000 * day + 10 * 3_600_000L // 10 Uhr
    private fun seed(i: Int, bed: String = "b") = StudyCard("c$i", bed, "q$i", "a$i", null, null, Memory(), null)
    private fun learned(i: Int, stab: Double, lastAgoDays: Double, bed: String = "b") =
        StudyCard("c$i", bed, "q$i", "a$i", null, null, Memory(stab, 5.0, now - (lastAgoDays * day).toLong(), 2, 0), null)
    private fun exam(inDays: Int) = Exam("e", "b", "Mathe-Arbeit", ExamPlanner.day(now) + inDays)

    @Test fun spreadsSeedsOverTheDaysInsteadOfCramming() {
        val cards = (1..40).map { seed(it) }
        val p = ExamPlanner.plan(exam(9), cards, now)
        assertEquals(ExamPlan.Phase.BUILD, p.phase)
        assertEquals(5, p.today.size)        // 40 Karten / 8 Arbeitstage
        assertEquals(0.0, p.fitness, 1e-9)
    }

    @Test fun shortNoticeMeansMorePerDayButCapped() {
        val p = ExamPlanner.plan(exam(3), (1..100).map { seed(it) }, now)
        assertEquals(ExamPlanner.MAX_PER_DAY, p.today.size)
    }

    @Test fun onlyCardsOfTheExamBed() {
        val cards = (1..10).map { seed(it) } + (11..20).map { seed(it, bed = "other") }
        assertTrue(ExamPlanner.plan(exam(5), cards, now).today.all { it.bedId == "b" })
    }

    @Test fun cardsThatWillHoldAreNotPracticedAgain() {
        val solid = learned(1, stab = 200.0, lastAgoDays = 1.0)
        val p = ExamPlanner.plan(exam(7), listOf(solid, seed(2)), now)
        assertTrue(solid !in p.today)
        assertEquals(1, p.readyCards)
    }

    @Test fun respectsSpacingGap() {
        // gestern geübt, nicht durstig, aber bis zur Arbeit in 20 Tagen nicht sicher → noch nicht wieder dran
        val c = learned(1, stab = 6.0, lastAgoDays = 1.0)
        assertTrue(ExamPlanner.plan(exam(20), listOf(c), now).today.isEmpty())
        // vor 5 Tagen geübt → jetzt wieder dran
        val c2 = learned(2, stab = 6.0, lastAgoDays = 5.0)
        assertTrue(c2 in ExamPlanner.plan(exam(20), listOf(c2), now).today)
    }

    @Test fun examDayIsOnlyAWarmUp() {
        val cards = (1..30).map { learned(it, stab = 3.0, lastAgoDays = 2.0) } + (31..40).map { seed(it) }
        val p = ExamPlanner.plan(exam(0), cards, now)
        assertEquals(ExamPlan.Phase.EXAM_DAY, p.phase)
        assertEquals(10, p.today.size)
        assertTrue(p.today.none { it.memory.isSeed })
    }

    @Test fun pastExamPlansNothing() {
        val p = ExamPlanner.plan(exam(-1), listOf(seed(1)), now)
        assertEquals(ExamPlan.Phase.PAST, p.phase); assertTrue(p.today.isEmpty())
    }

    @Test fun brummiIsEncouraging() {
        val s = ExamPlanner.brummiSays(ExamPlanner.plan(exam(9), (1..10).map { seed(it) }, now))
        assertTrue(s, s.contains("Noch 9 Tage"))
    }
}
