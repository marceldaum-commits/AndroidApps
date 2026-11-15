package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import de.kurvenflug.core.model.CalDate as LocalDate

class HomeTest {
    @Test fun stationRoomsCostStardustAndCrewMovesIn() {
        val stars = mapOf("1-1" to 3, "daily-5" to 2, "rescue-1" to 3) + (1..10).associate { "2-$it" to 3 }
        assertEquals(38, Station.earned(stars))
        assertTrue(Station.canUnlock(Station.rooms[1], stars, emptySet()))      // Labor 30
        assertFalse(Station.canUnlock(Station.rooms[2], stars, emptySet()))     // Garten 60
        assertEquals(8, Station.balance(stars, setOf(1)))
        assertFalse(Station.canUnlock(Station.rooms[0], stars, emptySet()))     // Brücke ist schon offen
        // Crew verteilt sich auf offene Räume
        assertEquals(listOf(1, 3), Station.residents(0, setOf(1, 2, 3), setOf(1)).map { it.n })
        assertEquals(listOf(2), Station.residents(1, setOf(1, 2, 3), setOf(1)).map { it.n })
    }

    @Test fun seasonsComeEveryYear() {
        assertEquals(LocalDate.of(2026, 4, 5), Seasons.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Seasons.easter(2027))
        assertEquals(SeasonEvent.HALLOWEEN, Seasons.active(LocalDate.of(2026, 10, 31)))
        assertEquals(SeasonEvent.ADVENT, Seasons.active(LocalDate.of(2026, 12, 24)))
        assertEquals(SeasonEvent.ADVENT, Seasons.active(LocalDate.of(2027, 1, 3)))
        assertEquals(2026, Seasons.year(LocalDate.of(2027, 1, 3), SeasonEvent.ADVENT))
        assertEquals(SeasonEvent.EASTER, Seasons.active(LocalDate.of(2026, 4, 1)))
        assertEquals(SeasonEvent.SUMMER, Seasons.active(LocalDate.of(2026, 8, 1)))
        assertNull(Seasons.active(LocalDate.of(2026, 11, 15)))
    }

    @Test fun seasonLevelsAreSolvableAndRebuiltFromId() {
        for (s in SeasonEvent.entries) for (y in 2026..2030) {
            val l = Seasons.levels(s, y)
            assertEquals(5, l.size)
            l.forEach {
                assertTrue(it.id, Flight.fly(it, it.solution).success)
                assertEquals(it, Seasons.levelFor(it.id))
            }
        }
        val l = Seasons.levels(SeasonEvent.HALLOWEEN, 2026)
        assertEquals(l[1].id, Seasons.next(l[0].id)!!.id)
        assertNull(Seasons.next(l[4].id))
        assertTrue(Seasons.done(SeasonEvent.HALLOWEEN, 2026, l.associate { it.id to 1 }))
    }
}
