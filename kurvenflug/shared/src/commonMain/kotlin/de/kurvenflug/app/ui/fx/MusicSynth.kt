package de.kurvenflug.app.ui.fx

import de.kurvenflug.core.model.Instrument
import de.kurvenflug.core.model.Song
import de.kurvenflug.core.model.SpaceMusic
import kotlin.math.*
import kotlin.random.Random

/**
 * Macht aus einem [Song] Klang: weiche Synthesizer (Klangteppich, Arpeggio, Bass, Funkeln, Melodie, Kick, Hi-Hat),
 * Takt-Echo und ein weiter Hall-Raum. Bei Schleifen laufen Töne über das Ende hinaus am Anfang weiter – nahtlos.
 * Alles im Code erzeugt – Android und Browser klingen gleich.
 */
object MusicSynth {
    const val SR = 22050
    private const val TAU = 2 * PI

    /** Ganzes Stück auf einmal (Android, im Hintergrund-Thread). */
    fun render(song: Song): ShortArray {
        val out = FloatArray(length(song))
        parts(song).forEach { v -> addVoice(out, song, v) }
        echo(out, (60.0 / song.bpm * 0.75 * SR).toInt(), song.loop)
        reverb(out, song.loop)
        return toShort(out)
    }

    /**
     * Gleiches Ergebnis wie [render], aber in Häppchen mit Pausen dazwischen – im Browser gibt es nur einen Thread,
     * so bleibt die Oberfläche flüssig. [pause] z. B. `delay(1)`.
     */
    suspend fun renderInSlices(song: Song, pause: suspend () -> Unit): ShortArray {
        val out = FloatArray(length(song))
        parts(song).forEach { v -> addVoice(out, song, v); pause() }
        echo(out, (60.0 / song.bpm * 0.75 * SR).toInt(), song.loop); pause()
        reverb(out, song.loop, pause)
        return toShort(out)
    }

    /** Stimmen in kleine Notengruppen teilen – auf Android und im Browser gleich (gleicher Klang, auch beim Rauschen) */
    private fun parts(song: Song) = song.voices.flatMap { v -> v.notes.chunked(3).map { v.copy(notes = it) } }

    private fun length(song: Song): Int = (song.seconds * SR).roundToInt().let { if (song.loop) it else it + (2.5 * SR).toInt() }

    private fun toShort(out: FloatArray): ShortArray {
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        return ShortArray(out.size) { i -> (tanh(out[i] / peak * 1.2) / tanh(1.2) * 0.85 * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Eine Stimme in [out] hineinmischen (Rauschen der Hi-Hat mit festem Startwert je Stimme – überall gleich). */
    private fun addVoice(out: FloatArray, song: Song, voice: de.kurvenflug.core.model.Voice) {
        val n = out.size
        val spb = 60.0 / song.bpm
        val noise = Random(7)
        for (note in voice.notes) {
            val f = SpaceMusic.frequency(note.pitch)
            val start = (note.beat * spb * SR).roundToInt()
            val noteSec = note.length * spb
            val len = ((when (voice.instrument) {
                Instrument.PAD -> noteSec + 1.2; Instrument.ARP -> noteSec + 0.25; Instrument.LEAD -> noteSec + 0.6
                Instrument.BASS -> noteSec + 0.1; Instrument.SPARKLE -> 1.8; Instrument.KICK -> 0.35; Instrument.HAT -> 0.08
            }) * SR).toInt()
            val amp = voice.gain * note.velocity
            for (i in 0 until len) {
                val t = i.toDouble() / SR
                val s = if (voice.instrument == Instrument.HAT) noise.nextDouble(-1.0, 1.0) * exp(-t * 60) else sample(voice.instrument, f, t, noteSec)
                val idx = start + i
                if (song.loop) out[idx % n] += (amp * s).toFloat() else if (idx < n) out[idx] += (amp * s).toFloat()
            }
        }
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
        // Melodie: runder Synth-Lead mit sanftem Vibrato (setzt nach kurzer Zeit ein)
        Instrument.LEAD -> {
            val env = min(1.0, t / 0.03) * (if (t > noteSec) exp(-(t - noteSec) * 6) else 1.0)
            val vib = 1 + 0.004 * sin(TAU * 5.5 * t) * min(1.0, t / 0.4)
            env * (sin(TAU * f * vib * t) + 0.3 * sin(TAU * 2 * f * vib * t) + 0.12 * sin(TAU * 3 * f * vib * t)) / 1.4
        }
        Instrument.BASS -> {
            val env = min(1.0, t / 0.01) * (if (t > noteSec) exp(-(t - noteSec) * 30) else 1.0)
            env * (sin(TAU * f * t) + 0.35 * sin(TAU * 2 * f * t))
        }
        Instrument.SPARKLE -> min(1.0, t / 0.002) * exp(-t * 3) * (sin(TAU * f * t) + 0.3 * sin(TAU * 3.01 * f * t) * exp(-t * 8))
        // Kick: Sinus, der schnell von 120 auf 45 Hz fällt
        Instrument.KICK -> { val fr = 45 + 75 * exp(-t * 30); exp(-t * 9) * sin(TAU * fr * t) }
        Instrument.HAT -> 0.0
    }

    /** Echo im Takt (Delay) – typisch für Weltraum-Synth. */
    private fun echo(x: FloatArray, d: Int, loop: Boolean) {
        val n = x.size
        val copy = x.copyOf()
        for (pass in 1..3) {
            val g = 0.28f / pass
            for (i in 0 until n) { val j = i + d * pass; if (loop) x[j % n] += copy[i] * g else if (j < n) x[j] += copy[i] * g }
        }
    }

    /** Weiter Hall-Raum (vier Kammfilter + zwei Allpässe). Bei Schleifen zweimal durchlaufen, damit der Nachhall nahtlos ist. */
    private fun reverb(x: FloatArray, loop: Boolean) = reverbImpl(x, loop) {}
    private suspend fun reverb(x: FloatArray, loop: Boolean, pause: suspend () -> Unit) {
        // gleiche Rechnung, aber Kammfilter für Kammfilter mit Pause dazwischen
        val wet = FloatArray(x.size)
        combDelays.forEach { d -> comb(x, wet, d, loop); pause() }
        allpass(x, wet)
    }
    private val combDelays = intArrayOf(1116, 1188, 1277, 1356).map { it / 2 }

    private inline fun reverbImpl(x: FloatArray, loop: Boolean, between: () -> Unit) {
        val wet = FloatArray(x.size)
        combDelays.forEach { d -> comb(x, wet, d, loop); between() }
        allpass(x, wet)
    }

    private fun comb(x: FloatArray, wet: FloatArray, d: Int, loop: Boolean) {
        val n = x.size
        val buf = FloatArray(d); var p = 0; var lp = 0f
        repeat(if (loop) 2 else 1) { round ->
            for (i in 0 until n) {
                val y = buf[p]
                lp = y * 0.6f + lp * 0.4f
                buf[p] = x[i] + lp * 0.8f
                p = (p + 1) % d
                if (round == (if (loop) 1 else 0)) wet[i] += y * 0.25f
            }
        }
    }

    private fun allpass(x: FloatArray, wet: FloatArray) {
        val n = x.size
        for (d in intArrayOf(556, 441).map { it / 2 }) {
            val buf = FloatArray(d); var p = 0
            for (i in 0 until n) { val b = buf[p]; val y = -wet[i] + b; buf[p] = wet[i] + b * 0.5f; p = (p + 1) % d; wet[i] = y }
        }
        for (i in 0 until n) x[i] += wet[i] * 0.35f
    }
}
