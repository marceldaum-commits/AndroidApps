package de.raumlicht.core.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** Kurze Klänge der App – alle aus derselben gläsernen Tonwelt wie die Musik */
enum class Ton { TAP, TICK, LAUNCH, HIT, CELL, CRASH, WIN, WRONG, BEEP, REFRACT, CHAPTER, JINGLE }

/**
 * Die Kurzklänge von Raumlicht – im Code errechnet, keine Aufnahmen. Alle Töne liegen in D-Dur, der Tonart der Musik
 * ([SpaceMusic]), und sind aus denselben Bausteinen gebaut (angeschlagenes Glas, weiche Sinustöne, derselbe Nachhall).
 */
object Klang {
    const val SR = 22050
    private const val TAU = 2 * PI

    /** Grundton von [Ton.HIT]: d'' (587 Hz). Mit [beamPitch] ergeben mehrere Treffer zusammen einen D-Dur-Akkord mit Sexte und None. */
    const val HIT_MIDI = 74

    /** Stufen der Glas-Skala in Halbtönen über dem Grundton: d – e – fis – a – h (D-Dur-Pentatonik = D6/9-Akkord). */
    private val STEPS = intArrayOf(0, 2, 4, 7, 9)

    /** Der Klang als Mono-Samples in −1…1 (Spitze höchstens 0,9), ohne Knacks am Anfang und Ende */
    fun render(t: Ton): FloatArray = when (t) {
        // Glas-Tupfer auf d'''
        Ton.TAP -> buf(0.07).also {
            Dsp.ring(it, 0, hz(86), 1.0, 55.0, attack = 30)
            Dsp.ring(it, 0, hz(86) * 2.32, 0.3, 110.0, attack = 30)
            finish(it, 0.5, 0.02)
        }
        // Noch kleiner und höher (a'''), fast nur ein Anschlag
        Ton.TICK -> buf(0.035).also {
            Dsp.ring(it, 0, hz(93), 1.0, 120.0, attack = 20)
            Dsp.ring(it, 0, hz(93) * 2.32, 0.2, 250.0, attack = 20)
            finish(it, 0.3, 0.012)
        }
        // Lichtschweif: Sinus gleitet von d'' zu a''' (mit Quinte darüber), darauf drei Glasfunken d – e – a (Kern der Erkennungsmelodie)
        Ton.LAUNCH -> buf(0.30).also {
            var ph = 0.0
            for (i in it.indices) {
                val u = i.toDouble() / it.size
                ph += TAU * hz(74) * 3.0.pow(u) / SR
                it[i] = (sin(PI * u.pow(1.3)) * (sin(ph) + 0.35 * sin(1.5 * ph) + 0.15 * sin(2 * ph)) * 0.6).toFloat()
            }
            listOf(86, 88, 93).forEachIndexed { k, p -> Dsp.glass(it, at(0.05 + k * 0.07), hz(p), 0.3, 16.0, bright = 0.6) }
            finish(it, 0.7, 0.05)
        }
        // Klarer Glaston auf d'' mit Ausklang und etwas Raum
        Ton.HIT -> buf(0.5).also {
            Dsp.glass(it, 0, hz(HIT_MIDI), 1.0, 6.0, bright = 1.4)
            room(it, 0.25)
            finish(it, 0.8, 0.09)
        }
        // Weicher Doppelton a'' → d''' (Quarte aufwärts: „rastet ein“)
        Ton.CELL -> buf(0.15).also {
            Dsp.ring(it, 0, hz(81), 0.8, 30.0, attack = 90)
            Dsp.ring(it, 0, hz(81) * 2.32, 0.1, 60.0, attack = 90)
            Dsp.ring(it, at(0.055), hz(86), 1.0, 24.0, attack = 90)
            Dsp.ring(it, at(0.055), hz(86) * 2.32, 0.12, 50.0, attack = 90)
            finish(it, 0.55, 0.03)
        }
        // Dumpfes Klirren abwärts: fünf tiefe Glasscherben a' – fis' – e' – d' – a und ein kurzer, weicher Rauschstoß
        Ton.CRASH -> buf(0.35).also {
            listOf(69, 66, 64, 62, 57).forEachIndexed { k, p ->
                val s = at(0.01 + k * 0.05 + k * k * 0.004); val a = 1.0 - k * 0.11
                Dsp.ring(it, s, hz(p), a, 15.0, attack = 90)
                Dsp.ring(it, s, hz(p) * 2.32, a * 0.45, 26.0, attack = 90)
                Dsp.ring(it, s, hz(p) * 4.25, a * 0.2, 45.0, attack = 90)
            }
            Dsp.breath(it, 0, 700.0, 1.2, 0.22, at(0.03), at(0.006), at(0.1), seed = 7)
            finish(it, 0.6, 0.07)
        }
        // Aufsteigende Glasfigur d'' – e'' – a'' – d''': der Kern der Erkennungsmelodie, zur Oktave aufgelöst
        Ton.WIN -> buf(0.7).also {
            listOf(74, 76, 81, 86).forEachIndexed { k, p -> Dsp.glass(it, at(k * 0.085), hz(p), 0.7 + k * 0.1, 5.5) }
            Dsp.ring(it, at(3 * 0.085), hz(98), 0.18, 9.0)
            room(it, 0.25)
            finish(it, 0.8, 0.12)
        }
        // Zwei gedämpfte, leise Töne abwärts a' → fis' (kleine Terz) – reiner Sinus, weich ein- und ausgeblendet
        Ton.WRONG -> buf(0.3).also {
            soft(it, 0.0, 69, 1.0, 0.085, 0.014, 0.07)
            soft(it, 0.13, 66, 0.9, 0.08, 0.014, 0.085)
            finish(it, 0.4, 0.02)
        }
        // Neutraler, weicher Ton auf a''
        Ton.BEEP -> buf(0.1).also {
            soft(it, 0.0, 81, 1.0, 0.055, 0.008, 0.04)
            finish(it, 0.5, 0.01)
        }
        // Schillern: ein Ton knickt leicht ab (d''' → cis'''), dabei fächert sich das Licht in fünf flimmernde Glasfarben auf
        Ton.REFRACT -> buf(0.4).also {
            var ph = 0.0
            for (i in it.indices) {
                val u = i.toDouble() / it.size
                val bend = if (u < 0.25) 0.0 else if (u > 0.5) 1.0 else (u - 0.25) * 4
                ph += TAU * hz(86) * 2.0.pow(-bend / 12.0) / SR
                it[i] = (sin(PI * u.pow(0.7)) * sin(ph) * 0.35).toFloat()
            }
            listOf(88, 90, 93, 95, 98).forEachIndexed { k, p ->
                val s = at(0.04 + k * 0.035)
                Dsp.ring(it, s, hz(p), 0.3, 11.0, attack = 80)
                Dsp.ring(it, s, hz(p) * 1.012, 0.3, 11.0, attack = 80)
            }
            room(it, 0.20)
            finish(it, 0.6, 0.09)
        }
        // Erkennungsmelodie festlich: mit Oktave, getragen von einer Dmaj9-Fläche, Schlussakkord und Glanz aus hohen Funken
        Ton.CHAPTER -> buf(2.0).also {
            SpaceMusic.MOTIF.forEachIndexed { i, p ->
                val s = at(SpaceMusic.MOTIF_BEATS[i] * 0.26)
                Dsp.glass(it, s, hz(p), 0.75 + i * 0.06, 2.6)
                Dsp.glass(it, s, hz(p + 12), 0.28, 4.5, bright = 0.5)
            }
            for (p in listOf(62, 69, 76, 78)) {
                Dsp.tone(it, 0, hz(p) - 0.4, 0.07, at(1.1), at(0.5), at(0.8))
                Dsp.tone(it, 0, hz(p) + 0.4, 0.07, at(1.1), at(0.5), at(0.8), phase = 1.0)
            }
            val last = at(SpaceMusic.MOTIF_BEATS.last() * 0.26)
            Dsp.glass(it, last, hz(74), 0.4, 2.6); Dsp.glass(it, last, hz(78), 0.35, 2.6)
            listOf(93, 97, 100, 97, 102, 100, 105, 102).forEachIndexed { k, p ->
                val s = at(0.82 + k * 0.055)
                Dsp.ring(it, s, hz(p), 0.13, 8.0); Dsp.ring(it, s, hz(p) * 1.006, 0.13, 8.0)
            }
            room(it, 0.30)
            finish(it, 0.85, 0.3)
        }
        // Erkennungsmelodie schlicht: fünf Glastöne im Rhythmus der Musik, etwas Raum
        Ton.JINGLE -> buf(1.5).also {
            SpaceMusic.MOTIF.forEachIndexed { i, p -> Dsp.glass(it, at(SpaceMusic.MOTIF_BEATS[i] * 0.3), hz(p), 0.7 + i * 0.075, 3.0) }
            room(it, 0.25)
            finish(it, 0.75, 0.22)
        }
    }

    /**
     * Tonhöhen-Faktor für die Lichtshow: der i-te Laserstrahl/die i-te Ebene klingt als i-ter Ton eines Glas-Akkords.
     * i = 0…4: d – e – fis – a – h aufwärts ab dem Grundton (Faktor 1 … 1,68), i = 5…9: dieselben Töne eine Oktave
     * tiefer (0,5 … 0,84); danach wiederholt es sich. Gleichstufig gestimmt wie die Musik. Ergebnis zwischen 0,5 und 2,0.
     */
    fun beamPitch(i: Int): Float {
        val k = ((i % 10) + 10) % 10
        return 2.0.pow((STEPS[k % 5] - if (k >= 5) 12 else 0) / 12.0).toFloat()
    }

    private fun hz(midi: Int) = SpaceMusic.frequency(midi)
    private fun at(sec: Double) = (sec * SR).toInt()
    private fun buf(sec: Double) = FloatArray(at(sec))

    /** Weicher, gehaltener Ton: Sinus mit einem Hauch Oktave. */
    private fun soft(out: FloatArray, start: Double, midi: Int, amp: Double, hold: Double, attack: Double, release: Double) {
        Dsp.tone(out, at(start), hz(midi), amp, at(hold), at(attack), at(release))
        Dsp.tone(out, at(start), hz(midi) * 2, amp * 0.1, at(hold), at(attack), at(release))
    }

    /** Derselbe Nachhall wie in der Musik, hier kürzer und leiser. */
    private fun room(x: FloatArray, wet: Double) = Dsp.reverb(x.copyOf(), x, wet, t60 = 1.1)

    /** Gleichanteil weg, Anfang (1,5 ms) und Ende ([fadeOut] Sekunden) weich auf null, dann auf die gewünschte Spitze bringen. */
    private fun finish(x: FloatArray, peak: Double, fadeOut: Double) {
        val n = x.size
        var mean = 0.0
        for (v in x) mean += v
        mean /= n
        val fi = 33; val fo = at(fadeOut).coerceIn(1, n)
        var top = 1e-9
        for (i in 0 until n) {
            var v = x[i] - mean
            if (i < fi) v *= smooth(i.toDouble() / fi)
            if (i >= n - fo) v *= smooth((n - 1 - i).toDouble() / fo)
            x[i] = v.toFloat()
            if (abs(v) > top) top = abs(v)
        }
        val g = (peak / top).toFloat()
        for (i in 0 until n) x[i] *= g
    }

    private fun smooth(s: Double) = s * s * (3 - 2 * s)
}
