package de.wissensgarten.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class WeekReportTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val day = Scheduler.DAY_MS
    private val now = 100 * day + 10 * 3_600_000L

    @Test fun emptyWeekGivesFriendlyTip() {
        val r = WeekReport.build(emptyList(), emptyMap(), now, utc)
        assertEquals(0, r.activeDays); assertEquals(0, r.reviews)
        assertTrue(r.tip.contains("Pause"))
    }

    @Test fun countsDaysSubjectsHardAndStrong() {
        val cards = mapOf(
            "a" to WeekReport.CardInfo("7 × 8", "mathe", 3.0, 2),
            "b" to WeekReport.CardInfo("Hauptstadt von Frankreich", "erdkunde", 20.0, 0),
            "c" to WeekReport.CardInfo("dog", "englisch", 5.0, 0),
        )
        val e = listOf(
            WeekReport.Entry("a", now - 1 * day, Rating.AGAIN.ordinal),
            WeekReport.Entry("a", now - 1 * day + 60_000, Rating.GOOD.ordinal),
            WeekReport.Entry("b", now - 3 * day, Rating.EASY.ordinal),
            WeekReport.Entry("c", now, Rating.GOOD.ordinal),
            WeekReport.Entry("c", now - 9 * day, Rating.AGAIN.ordinal), // älter als eine Woche
        )
        val r = WeekReport.build(e, cards, now, utc)
        assertEquals(listOf(false, false, false, true, false, true, true), r.days)
        assertEquals(4, r.reviews)
        assertEquals(75, r.rightPct)
        assertEquals("mathe", r.subjects.first().name)
        assertEquals(listOf("7 × 8"), r.hard)
        assertEquals(listOf("Hauptstadt von Frankreich", "dog"), r.strong)
        assertEquals(1, r.minutes)
    }
}

class StudyTimeTest {
    @Test fun limit() {
        assertEquals(false, StudyTime.isOver(999 * 60_000L, 0, 0))
        assertEquals(false, StudyTime.isOver(19 * 60_000L, 20, 0))
        assertEquals(true, StudyTime.isOver(20 * 60_000L, 20, 0))
        assertEquals(false, StudyTime.isOver(30 * 60_000L, 20, 15))
    }
}
