package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class AdventureTest {
    private val now = 1_700_000_000_000L
    private fun card(i: Int, learned: Boolean) =
        StudyCard("c$i", "b", "q$i", "a$i", null, null, if (learned) Memory(10.0, 5.0, now, 2, 0) else Memory())

    @Test fun stationsAreChunkedAndOnlyOneIsCurrent() {
        val cards = (0 until 17).map { card(it, it < 7) }
        val s = AdventurePath.stations(cards, now)
        assertEquals(4, s.size)
        assertEquals(StationState.DONE, s[0].state)
        assertEquals(StationState.CURRENT, s[1].state)
        assertTrue(s.drop(2).all { it.state == StationState.LOCKED })
        assertEquals(listOf(false, false, true, false), s.map { it.chest })
    }

    @Test fun chestsCountOnlyFinishedChestStations() {
        val bedA = (0 until 15).map { card(it, true) }      // Station 3 = Truhe, geschafft
        val bedB = (0 until 15).map { card(it, it < 10) }   // Station 3 noch nicht
        assertEquals(1, AdventurePath.chestsEarned(listOf(bedA, bedB), now))
    }

    @Test fun stationSessionHasSeedsAndThirstyOnly() {
        val cards = (0 until 5).map { card(it, it < 2) }
        val st = AdventurePath.stations(cards, now)[0]
        assertEquals(3, AdventurePath.sessionFor(st, cards, now).size)
    }

    @Test fun friendsComeInFixedOrder() {
        assertEquals(24, Friends.all.size)
        assertEquals(24, Friends.all.map { it.id }.toSet().size)
        assertEquals("marie", Friends.earned(1).single().id)
        assertEquals("schnecki", Friends.next(1)!!.id)
        assertNull(Friends.next(24))
    }
}
