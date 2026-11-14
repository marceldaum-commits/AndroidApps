package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class ComposerTest {
    private val major = setOf(0, 2, 4, 5, 7, 9, 11)

    @Test fun allNotesStayInKeyAndInsideTheLoop() {
        for (mood in MusicMood.entries) {
            val s = Composer.compose(mood)
            val key = when (mood) { MusicMood.GARDEN -> 60; MusicMood.NIGHT -> 65; MusicMood.PLAY -> 62 }
            s.voices.flatMap { it.notes }.forEach { n ->
                assertTrue("$mood: ${n.pitch} nicht in der Tonart", Math.floorMod(n.pitch - key, 12) in major)
                assertTrue("$mood: Ton außerhalb der Schleife", n.beat >= 0 && n.beat < s.beats)
                assertTrue(n.pitch in 24..96)
            }
        }
    }

    @Test fun melodyEndsOnTonicSoTheLoopComesHome() {
        val s = Composer.compose(MusicMood.GARDEN)
        val last = s.voices[0].notes.maxBy { it.beat }
        assertEquals(0, Math.floorMod(last.pitch - 60, 12))
    }

    @Test fun lengthsAreKidFriendly() {
        for (mood in MusicMood.entries) {
            val sec = Composer.compose(mood).seconds
            assertTrue("$mood: $sec s", sec in 30.0..65.0)
        }
    }

    @Test fun sameSeedSameSong() = assertEquals(Composer.compose(MusicMood.PLAY, 3), Composer.compose(MusicMood.PLAY, 3))

    @Test fun melodyHasEnoughNotesButAlsoAir() {
        val notes = Composer.compose(MusicMood.GARDEN).voices[0].notes
        assertTrue(notes.size in 30..120)
    }
}
