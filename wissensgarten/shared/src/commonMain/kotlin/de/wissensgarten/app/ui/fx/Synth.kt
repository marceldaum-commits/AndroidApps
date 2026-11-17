package de.wissensgarten.app.ui.fx

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Alle Spiel-Geräusche – Klang-Identität Wissensgarten: Holz (Marimba), Metallplättchen (Glockenspiel), Wasser.
 * Werden im Code erzeugt – keine Audiodateien, keine Lizenzen. Nichts Schrilles: hohe Töne klingen weich aus.
 */
enum class Sound { TAP, CORRECT, WRONG, WATER, GROW, LEVEL_UP, PLOP, FANFARE, DEW, CLAP,
    /** Pflanze lässt die Blätter hängen (leise, tröstlich – keine Strafe) */ WILT,
    /** Ernte: neue Blume, Baum ausgewachsen */ HARVEST,
    /** Neuer Gartenfreund (Sammelfigur) */ COLLECT,
    /** Erkennungsmelodie „Wis-sens-gar-ten“ */ JINGLE,
}

/** Kleiner Synthesizer: Marimba, Glockenspiel, Tonhöhen-Rutscher, Blubbern, Rauschen. */
object Synth {
    const val SR = 44100
    private const val TAU = 2 * PI

    /** MIDI-Ton → Hz */
    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun render(s: Sound): FloatArray = when (s) {
        // Antippen: kurzer, weicher Holz-Klick
        Sound.TAP -> buf(0.12).also { mallet(it, 0.0, hz(84), 0.1, 0.5) }
        Sound.PLOP -> buf(0.14).also { sweep(it, 0.0, 750.0, 170.0, 0.10, 0.7) }
        // Richtig: zwei Marimba-Töne nach oben, das Glockenspiel funkelt nach
        Sound.CORRECT -> buf(0.9).also {
            mallet(it, 0.00, hz(79), 0.5, 0.5)          // G5
            mallet(it, 0.08, hz(84), 0.7, 0.55)         // C6
            glock(it, 0.16, hz(91), 0.7, 0.16)          // G6, leise
        }
        // Falsch: sanft und tief, zwei weiche Holztöne nach unten – kein Brummen, kein Erschrecken
        Sound.WRONG -> buf(0.6).also {
            mallet(it, 0.00, hz(64), 0.45, 0.45)        // E4
            mallet(it, 0.13, hz(60), 0.5, 0.4)          // C4
        }
        Sound.WATER -> buf(0.75).also {
            noise(it, 0.0, 0.6, 0.10)
            val r = Random(7)
            repeat(11) { _ ->
                val start = r.nextDouble(0.0, 0.5)
                val f = r.nextDouble(450.0, 1100.0)
                sweep(it, start, f, f * 1.8, r.nextDouble(0.04, 0.08), r.nextDouble(0.18, 0.35))
            }
        }
        // Wachsen: drei Marimba-Töne nach oben (die Tonhöhe beim Abspielen steigt mit der Pflanzen-Stufe)
        Sound.GROW -> buf(0.8).also {
            listOf(67, 72, 76).forEachIndexed { i, m -> mallet(it, i * 0.07, hz(m), 0.5, 0.4) }
            glock(it, 0.21, hz(88), 0.5, 0.12)
        }
        Sound.LEVEL_UP -> buf(1.4).also {
            listOf(72, 76, 79, 84, 88).forEachIndexed { i, m -> mallet(it, i * 0.075, hz(m), 0.6, 0.38) }
            listOf(91, 96).forEachIndexed { i, m -> glock(it, 0.42 + i * 0.07, hz(m), 0.7, 0.12) }
        }
        Sound.FANFARE -> buf(2.2).also {
            listOf(
                0.00 to listOf(72, 76, 79),
                0.20 to listOf(74, 77, 81),
                0.40 to listOf(76, 79, 83),
                0.65 to listOf(84, 88, 91),
            ).forEach { (t, chord) -> chord.forEach { m -> mallet(it, t, hz(m), if (t > 0.6) 1.3 else 0.4, 0.22) } }
            glock(it, 0.65, hz(96), 1.2, 0.12)
        }
        // Klatschen: drei winzige Rausch-Stöße kurz hintereinander (so klingen echte Hände)
        Sound.CLAP -> buf(0.18).also { out ->
            val r = Random(11)
            for ((start, amp) in listOf(0.0 to 0.7, 0.008 to 0.9, 0.018 to 1.0)) {
                val s0 = (start * SR).toInt()
                var prev = 0.0
                for (i in 0 until (0.15 * SR).toInt()) {
                    if (s0 + i >= out.size) break
                    val t = i.toDouble() / SR
                    val w = r.nextDouble(-1.0, 1.0)
                    val hp = w - prev; prev = w                 // hell: Hochpass
                    out[s0 + i] += (amp * hp * exp(-t * (if (start < 0.015) 120 else 28))).toFloat()
                }
            }
        }
        Sound.DEW -> buf(0.7).also {
            glock(it, 0.0, hz(88), 0.5, 0.3)
            glock(it, 0.06, hz(93), 0.55, 0.22)
        }
        // Welken: leise, langsam nach unten gleitender Ton – wie ein kleiner Seufzer
        Sound.WILT -> buf(0.9).also {
            sweep(it, 0.0, 520.0, 330.0, 0.8, 0.3)
            mallet(it, 0.05, hz(62), 0.7, 0.25)
        }
        // Ernte: fröhlicher Marimba-Lauf nach oben, Glockenspiel-Funkeln, Wasser-Plopp
        Sound.HARVEST -> buf(1.6).also {
            listOf(72, 74, 76, 79, 81, 84).forEachIndexed { i, m -> mallet(it, i * 0.06, hz(m), 0.5, 0.35) }
            listOf(88, 91, 96).forEachIndexed { i, m -> glock(it, 0.4 + i * 0.09, hz(m), 0.9, 0.13) }
            sweep(it, 0.36, 500.0, 900.0, 0.08, 0.25)
        }
        // Neuer Freund: zauberhafter Glockenspiel-Lauf nach oben und ein schimmernder Akkord
        Sound.COLLECT -> buf(2.0).also {
            listOf(79, 81, 84, 86, 88, 91, 93, 96).forEachIndexed { i, m -> glock(it, i * 0.045, hz(m - 12), 0.6, 0.18) }
            listOf(72, 76, 79).forEach { m -> mallet(it, 0.42, hz(m), 1.2, 0.25) }
            glock(it, 0.42, hz(84), 1.4, 0.15)
        }
        // Erkennungsmelodie: „Wis-sens-gar-ten“ – vier Marimba-Töne, das Glockenspiel antwortet, Brummi summt vorbei
        Sound.JINGLE -> buf(2.6).also {
            listOf(0.00 to 67, 0.16 to 72, 0.32 to 76, 0.48 to 74).forEach { (t, m) -> mallet(it, t, hz(m), 0.6, 0.42) }
            listOf(0.72 to 79, 0.86 to 84).forEach { (t, m) -> glock(it, t, hz(m), 1.0, 0.2) }
            listOf(60, 64, 67).forEach { m -> mallet(it, 0.72, hz(m), 1.4, 0.18) }
            hum(it, 1.1, 1.3, 0.09)
        }
    }.also(::normalize)

    /** Marimba-Schlag: Grundton plus schnell verklingende hohe Obertöne (Holz) */
    private fun mallet(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val fade = if (t > dur - 0.03) (dur - t) / 0.03 else 1.0
            val v = sin(TAU * f * t) * exp(-t * 4.5 / dur.coerceAtLeast(0.3)) +
                0.3 * sin(TAU * f * 3.93 * t) * exp(-t * 18) + 0.08 * sin(TAU * f * 9.2 * t) * exp(-t * 40)
            out[s0 + i] += (amp * v * min(1.0, t / 0.0015) * fade).toFloat()
        }
    }

    /** Glockenspiel: Metallplättchen, hell und lang ausklingend */
    private fun glock(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val fade = if (t > dur - 0.05) (dur - t) / 0.05 else 1.0
            val v = sin(TAU * f * t) + 0.3 * sin(TAU * f * 2.76 * t) * exp(-t * 4) + 0.1 * sin(TAU * f * 5.4 * t) * exp(-t * 9)
            out[s0 + i] += (amp * v * exp(-t * 3.5 / dur) * min(1.0, t / 0.001) * fade).toFloat()
        }
    }

    /** Brummi summt kurz vorbei */
    private fun hum(out: FloatArray, start: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val swell = sin(PI * t / dur).pow(2)
            val f = 200.0 * (1 + 0.06 * sin(PI * t / dur) + 0.012 * sin(TAU * 6.5 * t))
            var v = 0.0
            for (k in 1..5) v += sin(TAU * f * k * t) / k
            out[s0 + i] += (amp * v * swell * (0.75 + 0.25 * sin(TAU * 31 * t))).toFloat()
        }
    }

    private fun buf(sec: Double) = FloatArray((sec * SR).toInt())

    /** Glockenton mit Obertönen und weichem Ausklang. */
    private fun bell(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = exp(-t * 5.5 / dur) * min(1.0, t / 0.003)
            val v = sin(TAU * f * t) + 0.4 * sin(TAU * 2.0 * f * t) * exp(-t * 12) + 0.15 * sin(TAU * 3.01 * f * t) * exp(-t * 20)
            out[s0 + i] += (amp * env * v).toFloat()
        }
    }

    /** Ton, der die Höhe ändert (Plopp, Blubbern, Bonk). */
    private fun sweep(out: FloatArray, start: Double, f0: Double, f1: Double, dur: Double, amp: Double, triangle: Boolean = false) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = f0 * (f1 / f0).pow(t / dur)
            phase += TAU * f / SR
            val env = min(1.0, t / 0.004) * (1 - t / dur).pow(2)
            val w = if (triangle) 2 / PI * kotlin.math.asin(sin(phase)) else sin(phase)
            out[s0 + i] += (amp * env * w).toFloat()
        }
    }

    /** Weiches Rauschen (Wasser). */
    private fun noise(out: FloatArray, start: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        val r = Random(3)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            lp += 0.08 * (r.nextDouble(-1.0, 1.0) - lp)
            val env = min(1.0, t / 0.05) * (1 - t / dur)
            out[s0 + i] += (amp * env * lp * 3).toFloat()
        }
    }

    private fun normalize(a: FloatArray) {
        val peak = a.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val g = 0.8f / peak
        for (i in a.indices) a[i] *= g
    }

    /** 16-Bit-Mono-WAV. */
    fun wav(samples: FloatArray): ByteArray {
        val data = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, s ->
            val v = (s.coerceIn(-1f, 1f) * 32767).toInt()
            data[2 * i] = (v and 0xff).toByte(); data[2 * i + 1] = ((v shr 8) and 0xff).toByte()
        }
        val out = ArrayList<Byte>(44 + data.size)
        fun str(s: String) { s.forEach { out += it.code.toByte() } }
        fun i32(v: Int) { out += v.toByte(); out += (v shr 8).toByte(); out += (v shr 16).toByte(); out += (v shr 24).toByte() }
        fun i16(v: Int) { out += v.toByte(); out += (v shr 8).toByte() }
        str("RIFF"); i32(36 + data.size); str("WAVE")
        str("fmt "); i32(16); i16(1); i16(1); i32(SR); i32(SR * 2); i16(2); i16(16)
        str("data"); i32(data.size); out.addAll(data.toList())
        return out.toByteArray()
    }
}
