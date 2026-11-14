package de.wissensgarten.app.ui.fx

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Alle Spiel-Geräusche. Werden beim ersten Start im Code erzeugt – keine Audiodateien, keine Lizenzen. */
enum class Sound { TAP, CORRECT, WRONG, WATER, GROW, LEVEL_UP, PLOP, FANFARE, DEW, CLAP }

/** Kleiner Synthesizer: Glocken, Tonhöhen-Rutscher, Blubbern, Rauschen. */
object Synth {
    const val SR = 44100
    private const val TAU = 2 * PI

    fun render(s: Sound): FloatArray = when (s) {
        Sound.TAP -> buf(0.05).also { sweep(it, 0.0, 1500.0, 1000.0, 0.035, 0.3) }
        Sound.PLOP -> buf(0.14).also { sweep(it, 0.0, 750.0, 170.0, 0.10, 0.7) }
        Sound.CORRECT -> buf(0.7).also {
            bell(it, 0.00, 1046.5, 0.45, 0.35)
            bell(it, 0.07, 1318.5, 0.50, 0.35)
            bell(it, 0.14, 1568.0, 0.55, 0.32)
        }
        Sound.WRONG -> buf(0.32).also {
            sweep(it, 0.0, 260.0, 140.0, 0.24, 0.55, triangle = true)
            sweep(it, 0.0, 130.0, 90.0, 0.24, 0.25, triangle = true)
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
        Sound.GROW -> buf(0.6).also {
            sweep(it, 0.0, 330.0, 990.0, 0.25, 0.22)
            bell(it, 0.20, 1568.0, 0.35, 0.3)
        }
        Sound.LEVEL_UP -> buf(1.2).also {
            listOf(523.25, 659.25, 783.99, 1046.5, 1318.5).forEachIndexed { i, f -> bell(it, i * 0.075, f, 0.6, 0.3) }
            listOf(2093.0, 2637.0, 3136.0).forEachIndexed { i, f -> bell(it, 0.4 + i * 0.05, f, 0.5, 0.08) }
        }
        Sound.FANFARE -> buf(2.0).also {
            listOf(
                0.00 to listOf(523.25, 659.25, 783.99),
                0.20 to listOf(587.33, 739.99, 880.0),
                0.40 to listOf(659.25, 830.61, 987.77),
                0.65 to listOf(1046.5, 1318.5, 1568.0),
            ).forEach { (t, chord) -> chord.forEach { f -> bell(it, t, f, if (t > 0.6) 1.2 else 0.35, 0.2) } }
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
        Sound.DEW -> buf(0.5).also {
            bell(it, 0.0, 1760.0, 0.3, 0.3)
            bell(it, 0.06, 2637.0, 0.35, 0.25)
        }
    }.also(::normalize)

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
        val out = ByteArrayOutputStream()
        fun str(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun i32(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
        fun i16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        str("RIFF"); i32(36 + data.size); str("WAVE")
        str("fmt "); i32(16); i16(1); i16(1); i32(SR); i32(SR * 2); i16(2); i16(16)
        str("data"); i32(data.size); out.write(data)
        return out.toByteArray()
    }
}
