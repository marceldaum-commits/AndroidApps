package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.Composer
import de.wissensgarten.core.model.Instrument
import de.wissensgarten.core.model.Song
import kotlin.math.*

/**
 * Macht aus einem [Song] Klang: kleine, freundliche Instrumente aus Sinus-Tönen, etwas Hall.
 * Töne, die über das Ende hinausklingen, laufen am Anfang weiter – so ist die Schleife nahtlos.
 */
object MusicSynth {
    const val SR = 22050
    private const val TAU = 2 * PI

    fun render(song: Song): ShortArray {
        val n = (song.seconds * SR).roundToInt()
        val out = FloatArray(n)
        val spb = 60.0 / song.bpm
        for (voice in song.voices) for (note in voice.notes) {
            val f = Composer.frequency(note.pitch)
            val start = (note.beat * spb * SR).roundToInt()
            val noteSec = note.length * spb
            val ring = ringTime(voice.instrument, noteSec)
            val len = (ring * SR).toInt()
            val amp = voice.gain * note.velocity
            for (i in 0 until len) {
                val t = i.toDouble() / SR
                val v = sample(voice.instrument, f, t, noteSec)
                out[(start + i) % n] += (amp * v).toFloat()
            }
        }
        reverb(out)
        // weich begrenzen und auf angenehme Lautstärke bringen
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        return ShortArray(n) { i -> (tanh(out[i] / peak * 1.2) / tanh(1.2) * 0.85 * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun ringTime(inst: Instrument, noteSec: Double) = when (inst) {
        Instrument.KALIMBA -> 1.6
        Instrument.MUSIC_BOX -> 2.2
        Instrument.PLUCK -> 0.7
        Instrument.PAD -> noteSec + 0.8
        Instrument.BASS -> noteSec + 0.15
    }

    private fun sample(inst: Instrument, f: Double, t: Double, noteSec: Double): Double {
        val attack = { a: Double -> min(1.0, t / a) }
        return when (inst) {
            // Kalimba: warmer Grundton, kurz „klingelnder“ unharmonischer Oberton
            Instrument.KALIMBA -> attack(0.004) * (sin(TAU * f * t) * exp(-t * 2.8) + 0.25 * sin(TAU * f * 5.4 * t) * exp(-t * 18))
            // Spieluhr: hell, glockig, lang ausklingend
            Instrument.MUSIC_BOX -> attack(0.002) * (sin(TAU * f * t) + 0.35 * sin(TAU * f * 3.0 * t) * exp(-t * 4) +
                0.15 * sin(TAU * f * 5.2 * t) * exp(-t * 9)) * exp(-t * 1.9) * 0.8
            // Zupfklang: wenige Obertöne, kurz
            Instrument.PLUCK -> attack(0.003) * (sin(TAU * f * t) + 0.5 * sin(TAU * 2 * f * t) * exp(-t * 8) +
                0.25 * sin(TAU * 3 * f * t) * exp(-t * 14)) * exp(-t * 5.5)
            // Klangteppich: zwei leicht verstimmte Töne, weich ein- und ausblenden
            Instrument.PAD -> {
                val env = min(1.0, t / 0.7) * (if (t > noteSec) exp(-(t - noteSec) * 4) else 1.0)
                env * (sin(TAU * f * t) + sin(TAU * f * 1.004 * t) + 0.3 * sin(TAU * f * 2.002 * t)) / 2.3
            }
            Instrument.BASS -> {
                val env = attack(0.01) * exp(-t * 1.2) * (if (t > noteSec) exp(-(t - noteSec) * 25) else 1.0)
                env * (sin(TAU * f * t) + 0.3 * sin(TAU * 2 * f * t))
            }
        }
    }

    /** Einfacher Hall: zwei kreisförmige Echos (passt zur Schleife), sanft gedämpft. */
    private fun reverb(x: FloatArray) {
        val n = x.size
        for ((delaySec, gain) in listOf(0.113 to 0.22f, 0.271 to 0.16f)) {
            val d = (delaySec * SR).toInt()
            val copy = x.copyOf()
            var lp = 0f
            for (pass in 0 until 2) for (i in 0 until n) {
                lp = lp * 0.6f + copy[(i - d + n) % n] * 0.4f   // Höhen etwas dämpfen
                x[i] += lp * gain / (pass + 1)
            }
        }
    }
}
