package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlinx.datetime.LocalDate

class SeasonsTest {
    @Test fun easterDatesAreCorrect() {
        assertEquals(LocalDate(2024, 3, 31), Seasons.easter(2024))
        assertEquals(LocalDate(2025, 4, 20), Seasons.easter(2025))
        assertEquals(LocalDate(2026, 4, 5), Seasons.easter(2026))
        assertEquals(LocalDate(2027, 3, 28), Seasons.easter(2027))
    }

    @Test fun seasonsByMonth() {
        assertEquals(Season.WINTER, Seasons.season(LocalDate(2026, 1, 10)))
        assertEquals(Season.AUTUMN, Seasons.season(LocalDate(2026, 11, 14)))
        assertEquals(Season.SPRING, Seasons.season(LocalDate(2026, 4, 1)))
    }

    @Test fun festivals() {
        assertEquals("ostern", Seasons.festival(LocalDate(2026, 4, 2))?.id)
        assertEquals("halloween", Seasons.festival(LocalDate(2026, 10, 31))?.id)
        assertEquals("advent", Seasons.festival(LocalDate(2026, 12, 5))?.id)
        assertEquals("silvester", Seasons.festival(LocalDate(2027, 1, 1))?.id)
        assertNull(Seasons.festival(LocalDate(2026, 11, 14)))
    }
}
