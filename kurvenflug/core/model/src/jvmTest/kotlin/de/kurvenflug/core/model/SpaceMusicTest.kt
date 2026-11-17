package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class SpaceMusicTest {
    @Test fun loopsHaveTwoPartsAndNotesStayInside() {
        for (m in MusicMood.entries + (1..30).map { MusicMood(MusicMood.Mode.FLIGHT, it) }) {
            val s = SpaceMusic.compose(m)
            assertTrue("${m}: ${s.seconds} s", s.seconds in 25.0..50.0)
            s.voices.flatMap { it.notes }.forEach { n ->
                assertTrue(n.beat >= 0 && n.beat < s.beats)
                assertTrue(n.pitch in 30..100)
            }
            assertTrue("Melodie fehlt", s.voices.any { it.instrument == Instrument.LEAD && it.notes.isNotEmpty() })
        }
        assertEquals(440.0, SpaceMusic.frequency(69), 1e-9)
    }

    @Test fun tempoRisesWithTension() {
        val bpm = MusicMood.entries.map { SpaceMusic.compose(it).bpm }
        assertEquals(bpm, bpm.sorted())
        assertTrue(SpaceMusic.compose(MusicMood(MusicMood.Mode.EXTREME)).voices.any { it.instrument == Instrument.KICK })
        assertTrue(SpaceMusic.compose(MusicMood.FLIGHT).voices.none { it.instrument == Instrument.KICK })
    }

    @Test fun everyChapterHasItsOwnVariation() {
        val songs = Levels.chapters.map { SpaceMusic.compose(SpaceMusic.forLevel(it.levels.first().id, it.number)) }
        val fingerprints = songs.map { s -> s.bpm.toString() + s.voices.joinToString { v -> v.notes.take(40).joinToString { it.pitch.toString() } } }
        assertEquals(Levels.chapters.size, fingerprints.toSet().size)
    }

    @Test fun cuesFollowTheGame() {
        assertEquals(MusicMood.Mode.STORM, SpaceMusic.forLevel("storm-12-5", 0).mode)
        assertEquals(MusicMood.Mode.EXTREME, SpaceMusic.forLevel(ChapterExtreme.id(3), 3).mode)
        assertEquals(MusicMood.Mode.FLIGHT, SpaceMusic.forLevel("2-1", 2).mode)
        assertTrue(SpaceMusic.jingle().seconds < 3.0 && !SpaceMusic.jingle().loop)
    }
}
