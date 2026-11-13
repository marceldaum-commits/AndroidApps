package de.wissensgarten.app.notify

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RemindersTest {
    @Test fun nothingToDoMeansNoNotification() {
        assertNull(Reminders.message(listOf(Triple("Lena", 0, 0), Triple("Ben", 0, 0))))
    }

    @Test fun thirstyPlantsAreListedPerProfile() {
        val m = Reminders.message(listOf(Triple("Lena", 5, 0), Triple("Ben", 3, 10), Triple("Mama", 0, 0)))!!
        assertEquals("🥀 8 Pflanzen haben Durst", m.title)
        assertEquals("Lena: 5 · Ben: 3 – jetzt gießen!", m.text)
    }

    @Test fun singleProfileGetsFriendlyText() {
        val m = Reminders.message(listOf(Triple("Lena", 1, 0)))!!
        assertEquals("🥀 Eine Pflanze hat Durst", m.title)
        assertTrue(m.text.startsWith("1 Pflanze wartet"))
    }

    @Test fun seedsOnlyWhenNothingIsThirsty() {
        val m = Reminders.message(listOf(Triple("Lena", 0, 10)))!!
        assertEquals("🌱 Neue Samen warten", m.title)
    }

    @Test fun delayPointsToNextOccurrenceOfHour() {
        val c = Calendar.getInstance().apply { set(2026, 10, 14, 16, 30, 0); set(Calendar.MILLISECOND, 0) }
        val now = c.timeInMillis
        assertEquals(30 * 60_000L, Reminders.delayUntil(17, now))           // heute 17:00
        assertEquals((24 * 60 - 30) * 60_000L, Reminders.delayUntil(16, now))  // morgen 16:00
    }
}
