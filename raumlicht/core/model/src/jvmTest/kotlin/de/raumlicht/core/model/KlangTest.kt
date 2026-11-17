package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

class KlangTest {
    /** Gewünschte Länge je Klang in Sekunden (erlaubt: ±30 %). */
    private val seconds = mapOf(
        Ton.TAP to 0.07, Ton.TICK to 0.035, Ton.LAUNCH to 0.3, Ton.HIT to 0.5, Ton.CELL to 0.15, Ton.CRASH to 0.35,
        Ton.WIN to 0.7, Ton.WRONG to 0.3, Ton.BEEP to 0.1, Ton.REFRACT to 0.4, Ton.CHAPTER to 2.0, Ton.JINGLE to 1.5,
    )
    private val sounds = Ton.entries.associateWith { Klang.render(it) }
    private fun peak(a: FloatArray) = a.maxOf { abs(it) }

    @Test fun sampleRateMatchesMusic() = assertEquals(MusicSynth.SR, Klang.SR)

    @Test fun everySoundHasItsLength() {
        assertEquals(Ton.entries.toSet(), seconds.keys)
        for ((t, a) in sounds) {
            val sec = a.size.toDouble() / Klang.SR
            val want = seconds.getValue(t)
            assertTrue("$t dauert $sec s statt $want s", sec in want * 0.7..want * 1.3)
        }
        assertTrue(sounds.getValue(Ton.TAP).size <= 0.08 * Klang.SR)
        assertTrue(sounds.getValue(Ton.TICK).size <= 0.04 * Klang.SR)
        assertTrue(sounds.getValue(Ton.TICK).size < sounds.getValue(Ton.TAP).size)
    }

    @Test fun everySoundStartsAndEndsSilentlyAndNeverClips() {
        for ((t, a) in sounds) {
            val p = peak(a)
            assertTrue("$t übersteuert: $p", p <= 0.9f)
            assertTrue("$t ist zu leise: $p", p >= 0.25f)
            assertTrue("$t hat nicht endliche Werte", a.all { it.isFinite() })
            assertTrue("$t knackt am Anfang: ${a.first()}", abs(a.first()) < 0.01f)
            assertTrue("$t knackt am Ende: ${a.last()}", abs(a.last()) < 0.01f)
            // auch die ersten 0,25 ms und die letzten 0,5 ms sind leise – der Klang wächst aus der Stille und endet in ihr
            assertTrue("$t beginnt hart", (0 until 6).all { abs(a[it]) < 0.1f * p })
            assertTrue("$t endet hart", (a.size - 11 until a.size).all { abs(a[it]) < 0.05f * p })
            val mean = a.sumOf { it.toDouble() } / a.size
            assertTrue("$t hat Gleichanteil $mean", abs(mean) < 0.004)
            // kein Sprung mittendrin: größter Schritt zwischen zwei Samples bleibt klein gegen die Spitze
            val jump = (1 until a.size).maxOf { abs(a[it] - a[it - 1]) }
            assertTrue("$t springt um $jump", jump < 1.3f * p)
        }
    }

    @Test fun wrongIsGentlerThanWinAndTickSmallerThanTap() {
        assertTrue(peak(sounds.getValue(Ton.WRONG)) <= 0.45f)
        assertTrue(peak(sounds.getValue(Ton.WRONG)) < 0.6f * peak(sounds.getValue(Ton.WIN)))
        assertTrue(peak(sounds.getValue(Ton.TICK)) < peak(sounds.getValue(Ton.TAP)))
        fun energy(t: Ton) = sounds.getValue(t).sumOf { it.toDouble() * it }
        assertTrue(energy(Ton.WRONG) < energy(Ton.WIN))
        assertTrue(energy(Ton.TICK) < energy(Ton.TAP))
    }

    @Test fun soundsAreDeterministic() {
        for (t in Ton.entries) assertTrue(Klang.render(t).contentEquals(sounds.getValue(t)))
    }

    @Test fun beamPitchIsAPentatonicGlassChordInTheKeyOfTheMusic() {
        val allowed = setOf(0, 2, 4, 7, 9)   // d – e – fis – a – h über dem Grundton d
        assertEquals(1f, Klang.beamPitch(0), 1e-6f)
        assertEquals(62 % 12, Klang.HIT_MIDI % 12)   // der Grundton von HIT ist ein d, der Grundton der Musik
        val steps = (0..11).map { i ->
            val p = Klang.beamPitch(i)
            assertTrue("beamPitch($i) = $p", p in 0.5f..2.0f)
            assertEquals(p, Klang.beamPitch(i), 0f)
            val semis = 12 * ln(p.toDouble()) / ln(2.0)
            assertEquals("beamPitch($i) liegt nicht auf einem Halbton", semis.roundToInt().toDouble(), semis, 1e-4)
            assertTrue("beamPitch($i): Stufe ${semis.roundToInt()}", ((semis.roundToInt() % 12) + 12) % 12 in allowed)
            semis.roundToInt()
        }
        assertEquals(listOf(0, 2, 4, 7, 9), steps.take(5))            // die ersten fünf Strahlen: aufsteigend
        assertEquals(10, steps.take(10).toSet().size)                  // zehn verschiedene Töne, dann von vorn
        assertEquals(steps[0], steps[10]); assertEquals(steps[1], steps[11])
        // Frequenzverhältnisse nachgerechnet: Quinte und große Terz stimmen (gleichstufig) auf wenige Cent
        assertEquals(1.5, Klang.beamPitch(3).toDouble(), 0.003)
        assertEquals(1.25, Klang.beamPitch(2).toDouble(), 0.012)
        assertEquals(0.5, Klang.beamPitch(5).toDouble(), 1e-6)
        // beliebig große und negative i sind erlaubt
        for (i in listOf(-7, -1, 12, 99, 1_000_003, Int.MAX_VALUE, Int.MIN_VALUE)) assertTrue(Klang.beamPitch(i) in 0.5f..2.0f)
    }

    /** Zum Anhören: je Klang eine Datei in build/klang/, dazu die Lichtshow – sechs Treffer nacheinander und zusammen als Akkord. */
    @Test fun writesWavFiles() {
        for ((t, a) in sounds) assertTrue(Wav.write(t.name.lowercase(), a).length() > 44)
        val hit = sounds.getValue(Ton.HIT)
        val show = FloatArray(Klang.SR * 4)
        for (i in 0 until 6) {
            val rate = Klang.beamPitch(i)
            for (start in listOf((i * 0.3 * Klang.SR).toInt(), (2.4 * Klang.SR).toInt())) {
                var k = 0
                while (k * rate < hit.size - 1 && start + k < show.size) {
                    val x = k * rate; val j = x.toInt(); val f = x - j
                    show[start + k] += 0.25f * (hit[j] * (1 - f) + hit[j + 1] * f)
                    k++
                }
            }
        }
        assertTrue(peak(show) < 0.95f)
        Wav.write("lichtshow", show)
    }
}
