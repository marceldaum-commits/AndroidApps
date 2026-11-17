package de.wissensgarten.app.ui.fx

import de.wissensgarten.core.model.Composer
import de.wissensgarten.core.model.Instrument
import de.wissensgarten.core.model.Note
import de.wissensgarten.core.model.Song
import kotlin.math.*

/**
 * Macht aus einem [Song] Klang: kleine, freundliche Instrumente und Natur-Geräusche, alles aus Sinus-Tönen und Rauschen
 * im Code gerechnet (keine Aufnahmen), dazu etwas Hall.
 * Töne, die über das Ende hinausklingen, laufen am Anfang weiter – so ist die Schleife nahtlos.
 */
object MusicSynth {
    const val SR = 22050
    private const val TAU = 2 * PI

    fun render(song: Song): ShortArray {
        val out = FloatArray((song.seconds * SR).roundToInt())
        for (voice in song.voices) for (note in voice.notes) addNote(out, song, voice.instrument, voice.gain, note)
        return finish(out)
    }

    /** Wie [render], gibt aber zwischendurch die Kontrolle ab – im Browser ruckelt so nichts, während die Musik entsteht. */
    suspend fun renderGently(song: Song, pause: suspend () -> Unit): ShortArray {
        val out = FloatArray((song.seconds * SR).roundToInt())
        var work = 0
        for (voice in song.voices) for (note in voice.notes) {
            work += addNote(out, song, voice.instrument, voice.gain, note)
            if (work > 12_000) { work = 0; pause() }
        }
        pause()
        reverb(out, pause)
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        pause()
        val res = ShortArray(out.size)
        var i = 0
        while (i < out.size) {
            val end = min(out.size, i + 100_000)
            while (i < end) { res[i] = softSample(out[i], peak); i++ }
            pause()
        }
        return res
    }

    private fun finish(out: FloatArray): ShortArray {
        reverb(out)
        return soften(out)
    }

    /** weich begrenzen und auf angenehme Lautstärke bringen */
    private fun soften(out: FloatArray): ShortArray {
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        return ShortArray(out.size) { i -> softSample(out[i], peak) }
    }

    private fun softSample(v: Float, peak: Float): Short = (tanh(v / peak * 1.2) / tanh(1.2) * 0.85 * Short.MAX_VALUE).toInt().toShort()
    /** @return Anzahl gerechneter Werte (für [renderGently]) */
    private fun addNote(out: FloatArray, song: Song, inst: Instrument, gain: Double, note: Note): Int {
        val n = out.size
        val spb = 60.0 / song.bpm
        val f = Composer.frequency(note.pitch)
        val start = (note.beat * spb * SR).roundToInt()
        val noteSec = note.length * spb
        val len = (ringTime(inst, noteSec) * SR).toInt()
        val amp = gain * note.velocity
        val variant = ((note.beat * 4).roundToInt() * 2654435761L).and(0x7fffffff).toInt()   // kleine Unterschiede je Vogel/Tropfen
        if (inst == Instrument.BREEZE) { breeze(out, start, len, noteSec, amp, variant); return len }
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            out[(start + i) % n] += (amp * sample(inst, f, t, noteSec, variant)).toFloat()
        }
        return len
    }

    private fun ringTime(inst: Instrument, noteSec: Double) = when (inst) {
        Instrument.KALIMBA -> 1.6
        Instrument.MUSIC_BOX -> 2.2
        Instrument.PLUCK -> 0.7
        Instrument.PAD -> noteSec + 0.8
        Instrument.BASS -> noteSec + 0.15
        Instrument.MARIMBA -> 1.2
        Instrument.GLOCKENSPIEL -> 2.0
        Instrument.GUITAR -> 1.4
        Instrument.BIRD -> 0.5
        Instrument.CRICKET -> 1.1
        Instrument.BREEZE -> noteSec
        Instrument.DROP -> 0.18
        Instrument.HUM -> noteSec
    }

    private fun sample(inst: Instrument, f: Double, t: Double, noteSec: Double, variant: Int): Double {
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
            // Marimba: Holzklang – Grundton plus die typischen hohen Obertöne (≈ 4- und 10-fach), die schnell verklingen
            Instrument.MARIMBA -> attack(0.0015) * (sin(TAU * f * t) * exp(-t * 3.6) +
                0.32 * sin(TAU * f * 3.93 * t) * exp(-t * 16) + 0.1 * sin(TAU * f * 9.2 * t) * exp(-t * 34))
            // Glockenspiel: Metallplättchen – helle, unharmonische Obertöne, langer Ausklang
            Instrument.GLOCKENSPIEL -> attack(0.001) * (sin(TAU * f * t) + 0.3 * sin(TAU * f * 2.76 * t) * exp(-t * 3) +
                0.12 * sin(TAU * f * 5.40 * t) * exp(-t * 7) + 0.05 * sin(TAU * f * 8.93 * t) * exp(-t * 12)) * exp(-t * 2.3) * 0.7
            // Gitarre (Nylon): viele Obertöne, die höheren verklingen schneller
            Instrument.GUITAR -> {
                var v = 0.0
                for (k in 1..6) v += sin(TAU * f * k * t + k) / k.toDouble().pow(1.4) * exp(-t * (2.2 + 1.3 * k))
                attack(0.002) * v * 0.9
            }
            // Vogel: zwei bis drei schnelle, nach oben gezogene Pfiffe
            Instrument.BIRD -> {
                val calls = 2 + variant % 2
                val each = 0.11
                val k = (t / (each + 0.03)).toInt()
                val local = t - k * (each + 0.03)
                if (k >= calls || local > each) 0.0 else {
                    val base = 2300.0 + (variant % 7) * 180.0
                    // Tonhöhe steigt während des Pfiffs um 45 %, dazu ein feines Trillern
                    val ph = TAU * base * (local + 0.225 * local * local / each) + 0.6 * sin(TAU * 38 * local)
                    sin(ph) * sin(PI * local / each).pow(2) * (if (k == calls - 1) 0.8 else 1.0)
                }
            }
            // Grille: hohes Zirpen in kleinen Paketen
            Instrument.CRICKET -> {
                val pulse = (t * 28).toInt()
                val inPulse = (t * 28) - pulse
                val on = pulse % 6 < 3 && inPulse < 0.55
                if (!on) 0.0 else sin(TAU * 4300.0 * t) * sin(PI * inPulse / 0.55) * exp(-t * 0.8)
            }
            // Wassertropfen: kurzer, nach oben gleitender Plopp
            Instrument.DROP -> {
                val fr = 650.0 + (variant % 5) * 90.0
                val ph = TAU * fr * (t + 9.0 * t * t)
                sin(ph) * attack(0.002) * exp(-t * 32)
            }
            // Brummi fliegt vorbei: weiches Summen mit Flügel-Zittern, kommt und geht
            Instrument.HUM -> {
                val swell = sin(PI * (t / noteSec).coerceIn(0.0, 1.0)).pow(2)
                val fr = 190.0 * (1 + 0.03 * sin(TAU * t / noteSec) + 0.012 * sin(TAU * 6.5 * t))
                var v = 0.0
                for (k in 1..5) v += sin(TAU * fr * k * t) / k
                v * swell * (0.75 + 0.25 * sin(TAU * 31 * t))
            }
            Instrument.BREEZE -> 0.0
        }
    }

    /** Wind: weiches Rauschen, das langsam an- und abschwillt (mit kleinem Zustand, daher eigene Schleife). */
    private fun breeze(out: FloatArray, start: Int, len: Int, noteSec: Double, amp: Double, variant: Int) {
        val n = out.size
        var seed = variant.toLong() or 1L
        var lp1 = 0.0; var lp2 = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            val white = ((seed ushr 33).toDouble() / (1L shl 31).toDouble()) * 2 - 1
            // zwei Tiefpässe: dunkles, sanftes Rauschen; Böen ändern die Helligkeit
            val gust = 0.5 + 0.5 * sin(TAU * t / (noteSec * 0.6) + variant)
            lp1 += (0.02 + 0.03 * gust) * (white - lp1)
            lp2 += 0.05 * (lp1 - lp2)
            val swell = sin(PI * (t / noteSec)).pow(2)
            out[(start + i) % n] += (amp * 6.0 * lp2 * swell).toFloat()
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

    /** Derselbe Hall in Häppchen (Browser) – das Ergebnis ist Wert für Wert gleich. */
    private suspend fun reverb(x: FloatArray, pause: suspend () -> Unit) {
        val n = x.size
        for ((delaySec, gain) in listOf(0.113 to 0.22f, 0.271 to 0.16f)) {
            val d = (delaySec * SR).toInt()
            val copy = x.copyOf()
            var lp = 0f
            for (pass in 0 until 2) {
                var i = 0
                while (i < n) {
                    val end = min(n, i + 100_000)
                    while (i < end) { lp = lp * 0.6f + copy[(i - d + n) % n] * 0.4f; x[i] += lp * gain / (pass + 1); i++ }
                    pause()
                }
            }
        }
    }
}
