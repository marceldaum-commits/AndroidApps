package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class WidgetInfoTest {
    private val day = 20_500L
    private val now = day * Learning.DAY_MS + 3_600_000L

    @Test fun newPlayerSeesDailyAndFirstLevel() {
        val w = WidgetInfo.of(emptyMap(), 0, day, now, 5)
        assertFalse(w.dailyDone)
        assertEquals(Daily.id(day), w.dailyId)
        assertEquals("1-1", w.nextId)
        assertNull(w.reviewId)
        assertTrue(w.volt.contains("Tagesmission"))
    }

    @Test fun afterDailyAReviewIsSuggestedWhenDue() {
        val p = Levels.chapters.first { it.number == 2 }.levels.associate { it.id to Played(2, now - 3 * Learning.DAY_MS) } +
            (Daily.id(day) to Played(3, now))
        val w = WidgetInfo.of(p, 4, day, now, 7)
        assertTrue(w.dailyDone)
        assertNotNull(w.reviewId)
        assertEquals("📈 Geraden", w.reviewTitle)
        assertEquals(Levels.chapters.first { it.number == 2 }.levels.size * 2, w.stars)  // Tagesmission zählt nicht zu den Kapitel-Sternen
        assertTrue(w.volt.contains("auffrischen"))
        assertEquals(w.reviewId, Learning.levelFor(w.reviewId!!)!!.id)
    }
}
