package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class SpaceMusicTest {
    private val songs = MusicMood.entries.associateWith { SpaceMusic.compose(it) }
    private val pcm by lazy { songs.mapValues { MusicSynth.render(it.value) } }

    private fun rms(a: ShortArray) = sqrt(a.sumOf { it.toDouble() * it } / a.size)

    @Test fun loopsHaveTheRightLengthAndNotesStayInside() {
        for ((mood, s) in songs) {
            assertTrue("$mood: ${s.seconds} s", s.seconds in 40.0..70.0)
            val notes = s.voices.flatMap { it.notes }
            assertTrue(notes.isNotEmpty())
            notes.forEach { n ->
                assertTrue(n.beat >= 0 && n.beat < s.beats)
                assertTrue(n.pitch in 36..105)
                assertTrue(n.velocity > 0 && n.velocity <= 1.0)
            }
            // nichts brummt: kein Ton liegt tiefer als 60 Hz
            assertTrue(notes.minOf { SpaceMusic.frequency(it.pitch) } > 60.0)
        }
        assertEquals(440.0, SpaceMusic.frequency(69), 1e-9)
    }

    @Test fun flightMovesMoreThanMenuButNeverHectic() {
        val menu = songs.getValue(MusicMood.MENU); val flight = songs.getValue(MusicMood.FLIGHT)
        assertTrue(flight.bpm > menu.bpm)
        fun struck(s: Song) = s.voices.filter { it.instrument == Instrument.GLAS || it.instrument == Instrument.PERLE }.sumOf { it.notes.size } / s.seconds
        assertTrue(struck(flight) > 2 * struck(menu))
        assertTrue("Anschläge je Sekunde: ${struck(flight)}", struck(flight) < 3.0)
        assertTrue(struck(menu) < 1.0)
        assertTrue(menu.voices.none { it.instrument == Instrument.PERLE && it.notes.isNotEmpty() })
    }

    @Test fun motifReturnsInBothPieces() {
        assertTrue(SpaceMusic.MOTIF.size in 4..6)
        assertEquals(SpaceMusic.MOTIF.size, SpaceMusic.MOTIF_BEATS.size)
        assertEquals(SpaceMusic.MOTIF.sorted(), SpaceMusic.MOTIF)   // steigt auf wie ein Lichtstrahl
        for ((mood, s) in songs) {
            val times = s.voices.sumOf { v ->
                val p = v.notes.sortedBy { it.beat }.map { it.pitch }
                p.windowed(SpaceMusic.MOTIF.size).count { it == SpaceMusic.MOTIF }
            }
            assertTrue("$mood: Erkennungsmelodie $times-mal", times in 1..4)
        }
    }

    @Test fun piecesDiffer() {
        assertNotEquals(songs.getValue(MusicMood.MENU), songs.getValue(MusicMood.FLIGHT))
        assertFalse(pcm.getValue(MusicMood.MENU).contentEquals(pcm.getValue(MusicMood.FLIGHT)))
    }

    @Test fun renderIsDeterministicAndFast() {
        for ((mood, s) in songs) {
            val first = pcm.getValue(mood)
            val t0 = System.nanoTime()
            val again = MusicSynth.render(s)
            val ms = (System.nanoTime() - t0) / 1e6
            println("Rechenzeit $mood: ${"%.0f".format(ms)} ms für ${"%.1f".format(s.seconds)} s Musik")
            assertTrue(first.contentEquals(again))
            assertTrue("$mood braucht $ms ms", ms < 4000)
        }
    }

    @Test fun soundIsBalancedCleanAndLoopsSeamlessly() {
        val level = HashMap<MusicMood, Double>()
        for ((mood, a) in pcm) {
            val s = songs.getValue(mood)
            assertEquals(s.seconds * MusicSynth.SR, a.size.toDouble(), 1.0)
            val peak = a.maxOf { abs(it.toInt()) }
            val mean = a.sumOf { it.toDouble() } / a.size
            level[mood] = rms(a)
            // Nahtstelle: der Sprung vom letzten zum ersten Sample ist nicht größer als ein gewöhnlicher Schritt im Stück
            val step = (1 until a.size).sumOf { abs(a[it] - a[it - 1]).toDouble() } / (a.size - 1)
            val seam = abs(a[0] - a[a.size - 1])
            println("Klang $mood: Spitze ${"%.2f".format(peak / 32767.0)}, Effektivwert ${"%.3f".format(level.getValue(mood) / 32767)}, " +
                "Gleichanteil ${"%.1f".format(mean)}, Naht $seam (üblicher Schritt ${"%.0f".format(step)})")
            assertTrue("übersteuert: $peak", peak <= 0.9 * 32767 + 1)
            assertTrue("zu leise: $peak", peak > 0.3 * 32767)
            assertTrue("Gleichanteil $mean", abs(mean) < 20)
            assertTrue("Knacks an der Naht: $seam bei üblichem Schritt $step", seam <= 4 * step + 30)
            // … und es ist dort nicht still: die Schleife klingt über die Naht hinweg weiter
            val edge = rms(a.copyOfRange(0, 2000)) + rms(a.copyOfRange(a.size - 2000, a.size))
            assertTrue(edge > 0.2 * level.getValue(mood))
        }
        val ratio = level.getValue(MusicMood.MENU) / level.getValue(MusicMood.FLIGHT)
        assertTrue("Lautheit ungleich: $ratio", ratio in 0.85..1.18)
    }

    /** Zum Anhören und für die Analyse: build/klang/menu.wav, flight.wav sowie die Einzelstimmen (Mischungsverhältnis). */
    @Test fun writesWavFiles() {
        for ((mood, a) in pcm) {
            val f = Wav.write(mood.name.lowercase(), FloatArray(a.size) { a[it] / 32768f })
            assertTrue(f.length() > 44)
            val s = songs.getValue(mood)
            val all = sqrt(MusicSynth.raw(s).sumOf { it.toDouble() * it })
            println("Mischung $mood: " + s.voices.filter { it.notes.isNotEmpty() }.joinToString { v ->
                val part = sqrt(MusicSynth.raw(s.copy(voices = listOf(v))).sumOf { it.toDouble() * it })
                "${v.instrument} ${v.notes.size} Töne ${"%.0f".format(100 * part / all)} %"
            })
        }
    }
}

/** WAV-Dateien (Mono, 16 Bit) nach build/klang/ – nur für Tests. */
object Wav {
    fun write(name: String, x: FloatArray, sr: Int = MusicSynth.SR): File {
        val dir = File("build/klang").apply { mkdirs() }
        val data = ByteArray(x.size * 2)
        for (i in x.indices) {
            val v = (x[i].coerceIn(-1f, 1f) * 32767).toInt()
            data[2 * i] = v.toByte(); data[2 * i + 1] = (v shr 8).toByte()
        }
        fun i32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
        fun i16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
        val head = "RIFF".toByteArray() + i32(36 + data.size) + "WAVE".toByteArray() + "fmt ".toByteArray() + i32(16) + i16(1) + i16(1) +
            i32(sr) + i32(sr * 2) + i16(2) + i16(16) + "data".toByteArray() + i32(data.size)
        return File(dir, "$name.wav").apply { writeBytes(head + data) }
    }
}
