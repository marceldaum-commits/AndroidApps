package de.raumlicht.app.ui.fx

import de.raumlicht.core.model.Instrument
import de.raumlicht.core.model.Song
import de.raumlicht.core.model.SpaceMusic
import kotlin.math.*

/**
 * Macht aus einem [Song] Klang: weiche Synthesizer (Klangteppich, Arpeggio, Bass, Funkeln) mit Hall.
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
            val f = SpaceMusic.frequency(note.pitch)
            val start = (note.beat * spb * SR).roundToInt()
            val noteSec = note.length * spb
            val len = ((when (voice.instrument) {
                Instrument.PAD -> noteSec + 1.2; Instrument.ARP -> noteSec + 0.25
                Instrument.BASS -> noteSec + 0.1; Instrument.SPARKLE -> 1.8
            }) * SR).toInt()
            val amp = voice.gain * note.velocity
            for (i in 0 until len) {
                val t = i.toDouble() / SR
                out[(start + i) % n] += (amp * sample(voice.instrument, f, t, noteSec)).toFloat()
            }
        }
        echo(out, (spb * 0.75 * SR).toInt())
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        return ShortArray(n) { i -> (tanh(out[i] / peak * 1.2) / tanh(1.2) * 0.85 * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun sample(inst: Instrument, f: Double, t: Double, noteSec: Double): Double = when (inst) {
        // Klangteppich: drei leicht verstimmte Töne, langsam ein- und ausblenden
        Instrument.PAD -> {
            val env = min(1.0, t / 1.0) * (if (t > noteSec) exp(-(t - noteSec) * 3) else 1.0)
            env * (sin(TAU * f * t) + sin(TAU * f * 1.006 * t) + sin(TAU * f * 0.994 * t) + 0.25 * sin(TAU * 2 * f * t)) / 3.3
        }
        // Arpeggio: weicher „Sägezahn“ aus 4 Obertönen, kurz
        Instrument.ARP -> {
            val env = min(1.0, t / 0.005) * exp(-t * 6) * (if (t > noteSec) exp(-(t - noteSec) * 20) else 1.0)
            env * (1..4).sumOf { k -> sin(TAU * f * k * t) / k } * 0.6
        }
        Instrument.BASS -> {
            val env = min(1.0, t / 0.01) * (if (t > noteSec) exp(-(t - noteSec) * 30) else 1.0)
            env * (sin(TAU * f * t) + 0.35 * sin(TAU * 2 * f * t))
        }
        Instrument.SPARKLE -> min(1.0, t / 0.002) * exp(-t * 3) * (sin(TAU * f * t) + 0.3 * sin(TAU * 3.01 * f * t) * exp(-t * 8))
    }

    /** Echo im Takt (Delay) – typisch für Weltraum-Synth, kreisförmig für die Schleife. */
    private fun echo(x: FloatArray, d: Int) {
        val n = x.size
        val copy = x.copyOf()
        for (pass in 1..3) {
            val g = 0.3f / pass
            for (i in 0 until n) x[(i + d * pass) % n] += copy[i] * g
        }
    }
}
