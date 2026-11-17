package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class ComposerTest {
    private val major = setOf(0, 2, 4, 5, 7, 9, 11)

    @Test fun allNotesStayInKeyAndInsideTheLoop() {
        for (mood in MusicMood.entries) {
            val s = Composer.compose(mood)
            val st = Composer.style(mood)
            val allowed = (if (st.minor) Composer.MINOR else Composer.MAJOR).toSet()
            s.voices.flatMap { it.notes }.forEach { n ->
                assertTrue("$mood: ${n.pitch} nicht in der Tonart", Math.floorMod(n.pitch - st.key, 12) in allowed)
                assertTrue("$mood: Ton außerhalb der Schleife", n.beat >= 0 && n.beat < s.beats)
                assertTrue(n.pitch in 24..96)
            }
        }
    }

    @Test fun melodyEndsOnTonicSoTheLoopComesHome() {
        val s = Composer.compose(MusicMood.GARDEN)
        val last = s.voices[0].notes.maxBy { it.beat }
        assertEquals(0, Math.floorMod(last.pitch - 60, 12))
        val h = Composer.compose(MusicMood.HALLOWEEN)
        assertEquals(0, Math.floorMod(h.voices[0].notes.maxBy { it.beat }.pitch - 57, 12))
    }

    @Test fun lengthsAreKidFriendly() {
        for (mood in MusicMood.entries) {
            val sec = Composer.compose(mood).seconds
            assertTrue("$mood: $sec s", sec in 40.0..115.0)
        }
    }

    @Test fun sameSeedSameSong() = assertEquals(Composer.compose(MusicMood.PLAY, 3), Composer.compose(MusicMood.PLAY, 3))

    @Test fun melodyHasEnoughNotesButAlsoAir() {
        val notes = Composer.compose(MusicMood.GARDEN).voices[0].notes
        assertTrue(notes.size in 30..120)
    }

    @Test fun everySeasonSoundsDifferentAndHasNature() {
        val seasons = listOf(MusicMood.SPRING, MusicMood.SUMMER, MusicMood.AUTUMN, MusicMood.WINTER)
        assertEquals("jede Jahreszeit hat ihr eigenes Melodie-Instrument", 4, seasons.map { Composer.style(it).instrument }.toSet().size)
        for (m in seasons) {
            val s = Composer.compose(m)
            assertTrue("$m ohne Natur", s.voices.any { it.instrument.isNature && it.notes.isNotEmpty() })
        }
        assertTrue("Gieß-Runde ohne Natur (Konzentration)", Composer.compose(MusicMood.PLAY).voices.none { it.instrument.isNature })
        assertEquals(MusicMood.WINTER, MusicMood.of(Season.WINTER))
    }

    @Test fun breatherIsMelodyFreeSoTheLoopNeverNags() {
        for (m in MusicMood.entries) {
            val st = Composer.style(m)
            if (st.breather == 0) continue
            val s = Composer.compose(m)
            val melodyEnd = Composer.MELODY_BARS * 4.0
            assertTrue("$m: Melodie in der Atempause", s.voices[0].notes.all { it.beat < melodyEnd })
            assertEquals(s.beats, (Composer.MELODY_BARS + st.breather) * 4.0, 0.0)
        }
    }
}
