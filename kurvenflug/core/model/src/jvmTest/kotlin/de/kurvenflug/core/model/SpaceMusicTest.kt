package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class SpaceMusicTest {
    @Test fun loopsAreShortAndNotesStayInside() {
        for (m in MusicMood.entries) {
            val s = SpaceMusic.compose(m)
            assertTrue(s.seconds in 15.0..30.0)
            s.voices.flatMap { it.notes }.forEach { n ->
                assertTrue(n.beat >= 0 && n.beat < s.beats)
                assertTrue(n.pitch in 30..100)
            }
        }
        assertEquals(440.0, SpaceMusic.frequency(69), 1e-9)
    }

    @Test fun flightIsFasterThanMenu() {
        assertTrue(SpaceMusic.compose(MusicMood.FLIGHT).bpm > SpaceMusic.compose(MusicMood.MENU).bpm)
    }
}
