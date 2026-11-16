package de.raumlicht.app.ui.fx

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Alle Geräusche – im Code erzeugt, keine Audiodateien, keine Lizenzen. Elektronisch, passend zum All. */

object Synth {
    const val SR = 44100
    private const val TAU = 2 * PI

    fun render(s: Sound): FloatArray = when (s) {
        Sound.TAP -> buf(0.05).also { sweep(it, 0.0, 1800.0, 1200.0, 0.035, 0.3) }
        Sound.TICK -> buf(0.03).also { sweep(it, 0.0, 2400.0, 2200.0, 0.02, 0.25, square = true) }
        // Start: aufsteigendes Rauschen + Ton
        Sound.LAUNCH -> buf(0.7).also {
            noise(it, 0.0, 0.65, 0.35, rising = true)
            sweep(it, 0.0, 180.0, 720.0, 0.6, 0.35)
        }
        Sound.HIT -> buf(0.7).also {
            bell(it, 0.0, 1318.5, 0.5, 0.35); bell(it, 0.06, 1975.5, 0.55, 0.3)
        }
        Sound.CELL -> buf(0.3).also { sweep(it, 0.0, 900.0, 2400.0, 0.12, 0.4, square = true); bell(it, 0.1, 2637.0, 0.2, 0.2) }
        Sound.CRASH -> buf(0.8).also {
            noise(it, 0.0, 0.75, 0.9, rising = false)
            sweep(it, 0.0, 140.0, 40.0, 0.5, 0.7)
        }
        Sound.WIN -> buf(1.6).also {
            listOf(523.25, 659.25, 783.99, 1046.5).forEachIndexed { i, f -> sweep(it, i * 0.09, f, f, 0.12, 0.25, square = true) }
            listOf(1046.5, 1318.5, 1568.0, 2093.0).forEach { f -> bell(it, 0.4, f, 1.1, 0.18) }
        }
        Sound.WRONG -> buf(0.35).also {
            sweep(it, 0.0, 330.0, 160.0, 0.28, 0.5, square = true)
        }
        // Volt „spricht“: kurze Roboter-Piepser
        Sound.BEEP -> buf(0.3).also {
            val r = Random(5)
            repeat(4) { i -> val f = r.nextDouble(700.0, 1400.0); sweep(it, i * 0.065, f, f * 1.1, 0.05, 0.25, square = true) }
        }
    }.also(::normalize)

    private fun buf(sec: Double) = FloatArray((sec * SR).toInt())

    private fun bell(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = exp(-t * 5.5 / dur) * min(1.0, t / 0.003)
            val v = sin(TAU * f * t) + 0.3 * sin(TAU * 2.0 * f * t) * exp(-t * 12)
            out[s0 + i] += (amp * env * v).toFloat()
        }
    }

    private fun sweep(out: FloatArray, start: Double, f0: Double, f1: Double, dur: Double, amp: Double, square: Boolean = false) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = f0 * (f1 / f0).pow(t / dur)
            phase += TAU * f / SR
            val env = min(1.0, t / 0.004) * (1 - t / dur).pow(2)
            // „Square“ weich gerundet – klingt nach Retro-Konsole, aber nicht schrill
            val w = if (square) kotlin.math.tanh(sin(phase) * 4) * 0.6 else sin(phase)
            out[s0 + i] += (amp * env * w).toFloat()
        }
    }

    private fun noise(out: FloatArray, start: Double, dur: Double, amp: Double, rising: Boolean) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        val r = Random(3)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val k = if (rising) 0.03 + 0.25 * t / dur else 0.3 * (1 - t / dur) + 0.02
            lp += k * (r.nextDouble(-1.0, 1.0) - lp)
            val env = if (rising) min(1.0, t / 0.3) * (1 - t / dur).pow(0.5) else exp(-t * 5)
            out[s0 + i] += (amp * env * lp * 3).toFloat()
        }
    }

    private fun normalize(a: FloatArray) {
        val peak = a.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val g = 0.97f / peak
        for (i in a.indices) a[i] *= g
    }

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
