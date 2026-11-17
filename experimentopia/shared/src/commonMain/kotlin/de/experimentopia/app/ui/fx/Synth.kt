package de.experimentopia.app.ui.fx

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Alle Geräusche – im Code erzeugt, keine Audiodateien, keine Lizenzen. Blubbernd, gläsern, freundlich. */
/**
 * Effekte der Klang-Identität „Forscher-Labor“: TAP = Pizzicato-Zupfer, GLASS = Reagenzglas, BUBBLE/BLUBB = Blubbern, FIZZ = Zischen,
 * ZAP = Funken, DING = Messgerät, AHA = Entdeckung („Heureka“), WIN = richtig, SOFT = falsch (sanftes Fagott, nie ein Fehler-Ton).
 */
enum class Sound { TAP, POP, SPLASH, BUBBLE, DING, WIN, AHA, SOFT, BANG, FIZZ, ZAP, CLANK, WHOOSH, BLUBB, STAR, SWITCH, GLASS }

/** Klangfarben für Töne mit fester Höhe (Klangröhren, Gläser, Saite) */
enum class Timbre { FLUTE, BELL, PLUCK }

object Synth {
    const val SR = 44100
    private const val TAU = 2 * PI

    fun render(s: Sound): FloatArray = when (s) {
        // Pizzicato-Zupfer
        Sound.TAP -> buf(0.18).also { pluck(it, 0.0, 880.0, 0.17, 0.5) }
        Sound.SWITCH -> buf(0.08).also { noise(it, 0.0, 0.02, 0.5, false, 0.6); sweep(it, 0.0, 2200.0, 1800.0, 0.03, 0.3, square = true) }
        Sound.POP -> buf(0.12).also { sweep(it, 0.0, 400.0, 1400.0, 0.08, 0.6) }
        Sound.BUBBLE -> buf(0.18).also { sweep(it, 0.0, 500.0, 1600.0, 0.12, 0.45); sweep(it, 0.06, 700.0, 2000.0, 0.1, 0.25) }
        Sound.SPLASH -> buf(0.6).also { noise(it, 0.0, 0.55, 0.8, false, 0.25); sweep(it, 0.0, 300.0, 120.0, 0.2, 0.4) }
        // Messgerät: zwei kurze Pieptöne
        Sound.DING -> buf(0.32).also { beep(it, 0.0, 1760.0, 0.09, 0.45); beep(it, 0.15, 2349.0, 0.12, 0.45) }
        // Reagenzglas: hell, unharmonisch, kurz nachklingend
        Sound.GLASS -> buf(0.7).also { bell(it, 0.0, 3150.0, 0.6, 0.35); bell(it, 0.0, 4710.0, 0.35, 0.2); bell(it, 0.03, 3420.0, 0.5, 0.15) }
        Sound.STAR -> buf(0.5).also { bell(it, 0.0, 2093.0, 0.45, 0.35); bell(it, 0.05, 2637.0, 0.4, 0.25) }
        // Richtig: Pizzicato steigt auf, die Klarinette antwortet, ein Glas funkelt
        Sound.WIN -> buf(1.6).also {
            listOf(523.25, 659.25, 783.99).forEachIndexed { i, f -> pluck(it, i * 0.1, f, 0.5, 0.45) }
            reed(it, 0.32, 1046.5, 0.5, 0.35)
            bell(it, 0.4, 2093.0, 1.0, 0.18)
        }
        // Entdeckung („Heureka“): Glas-Glitzern steigt auf, oben ein Klarinetten-Triller
        Sound.AHA -> buf(1.2).also { listOf(784.0, 988.0, 1175.0, 1568.0, 1976.0).forEachIndexed { i, f -> bell(it, i * 0.06, f, 0.6, 0.2) }
            repeat(4) { k -> reed(it, 0.32 + k * 0.07, if (k % 2 == 0) 1568.0 else 1760.0, 0.065, 0.22) } }
        // Falsch – ganz sanft: zwei weiche Fagott-Töne abwärts („hmm“), nie ein harter Fehlerton
        Sound.SOFT -> buf(0.6).also { reed(it, 0.0, 164.8, 0.2, 0.4, low = true); reed(it, 0.22, 130.8, 0.32, 0.4, low = true) }
        Sound.BANG -> buf(1.0).also { noise(it, 0.0, 0.9, 1.0, false, 0.5); sweep(it, 0.0, 120.0, 35.0, 0.6, 0.9) }
        Sound.FIZZ -> buf(1.2).also { noise(it, 0.0, 1.15, 0.6, true, 0.9); repeat(10) { k -> sweep(it, k * 0.1, 900.0 + k * 80, 2400.0, 0.06, 0.15) } }
        Sound.ZAP -> buf(0.35).also { val r = Random(9); repeat(6) { k -> sweep(it, k * 0.04, r.nextDouble(800.0, 3000.0), r.nextDouble(200.0, 900.0), 0.05, 0.3, square = true) }; noise(it, 0.0, 0.3, 0.4, false, 0.8) }
        Sound.CLANK -> buf(0.4).also { bell(it, 0.0, 1850.0, 0.3, 0.35); bell(it, 0.0, 2770.0, 0.2, 0.25); noise(it, 0.0, 0.03, 0.5, false, 0.9) }
        Sound.WHOOSH -> buf(0.6).also { noise(it, 0.0, 0.55, 0.7, true, 0.15) }
        // Blubb „spricht“: drei blubbernde Glucker
        Sound.BLUBB -> buf(0.36).also { repeat(3) { k -> sweep(it, k * 0.11, 300.0 + k * 90, 900.0 + k * 150, 0.08, 0.45) } }
    }.also(::normalize)

    /** Ein Ton fester Höhe, z. B. für die Klangröhren */
    fun tone(hz: Double, timbre: Timbre, sec: Double = 0.9): FloatArray {
        val out = buf(sec)
        for (i in out.indices) {
            val t = i.toDouble() / SR
            out[i] = when (timbre) {
                // Flöte: weicher Einschwinger, etwas Hauch
                Timbre.FLUTE -> (min(1.0, t / 0.06) * exp(-t * 1.6) * (sin(TAU * hz * t) + 0.18 * sin(TAU * 2 * hz * t) + 0.05 * sin(TAU * 3 * hz * t))).toFloat()
                // Glas: Glocke mit unharmonischem Oberton
                Timbre.BELL -> (min(1.0, t / 0.002) * exp(-t * 3.2) * (sin(TAU * hz * t) + 0.35 * sin(TAU * 2.76 * hz * t) * exp(-t * 6))).toFloat()
                // Saite: gezupft, viele Obertöne, die schnell abklingen
                Timbre.PLUCK -> (min(1.0, t / 0.003) * (1..6).sumOf { k -> sin(TAU * k * hz * t) * exp(-t * (2.0 + k * 1.5)) / k }).toFloat()
            }
        }
        // Breath-Rauschen für die Flöte
        if (timbre == Timbre.FLUTE) noise(out, 0.0, min(sec, 0.25), 0.05, false, 0.4)
        normalize(out); return out
    }

    private fun buf(sec: Double) = FloatArray((sec * SR).toInt())

    /** Gezupfte Saite (Pizzicato) */
    private fun pluck(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt(); val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) { val t = i.toDouble() / SR
            out[s0 + i] += (amp * min(1.0, t / 0.002) * exp(-t * 9) * (1..5).sumOf { k -> sin(TAU * k * f * t) * exp(-t * 3.0 * k) / k }).toFloat() }
    }

    /** Rohrblatt: Klarinette (ungerade Obertöne) bzw. Fagott ([low], näselnd) */
    private fun reed(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double, low: Boolean = false) {
        val s0 = (start * SR).toInt(); val n = min(((dur + 0.08) * SR).toInt(), out.size - s0)
        for (i in 0 until n) { val t = i.toDouble() / SR; val ph = TAU * f * t
            val env = min(1.0, t / 0.03) * (if (t > dur) exp(-(t - dur) * 30) else 1.0)
            val w = if (low) 0.6 * sin(ph) + sin(2 * ph) + 0.7 * sin(3 * ph) + 0.3 * sin(4 * ph) else sin(ph) + 0.42 * sin(3 * ph) + 0.22 * sin(5 * ph)
            out[s0 + i] += (amp * env * w * 0.5).toFloat() }
    }

    /** Piepton eines Messgeräts */
    private fun beep(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt(); val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) { val t = i.toDouble() / SR
            out[s0 + i] += (amp * min(1.0, t / 0.003) * min(1.0, (dur - t) / 0.01) * sin(TAU * f * t)).toFloat() }
    }

    private fun bell(out: FloatArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = exp(-t * 5.5 / dur) * min(1.0, t / 0.003)
            out[s0 + i] += (amp * env * (sin(TAU * f * t) + 0.25 * sin(TAU * 2.0 * f * t) * exp(-t * 12))).toFloat()
        }
    }

    private fun sweep(out: FloatArray, start: Double, f0: Double, f1: Double, dur: Double, amp: Double, square: Boolean = false) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            phase += TAU * f0 * (f1 / f0).pow(t / dur) / SR
            val env = min(1.0, t / 0.004) * (1 - t / dur).pow(2)
            val w = if (square) kotlin.math.tanh(sin(phase) * 4) * 0.6 else sin(phase)
            out[s0 + i] += (amp * env * w).toFloat()
        }
    }

    private fun noise(out: FloatArray, start: Double, dur: Double, amp: Double, rising: Boolean, bright: Double) {
        val s0 = (start * SR).toInt()
        val n = min((dur * SR).toInt(), out.size - s0)
        val r = Random(3)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            lp += bright * (r.nextDouble(-1.0, 1.0) - lp)
            val env = if (rising) min(1.0, t / (dur * 0.4)) * (1 - t / dur).pow(0.6) else exp(-t * 6)
            out[s0 + i] += (amp * env * lp * 2).toFloat()
        }
    }

    private fun normalize(a: FloatArray) {
        val peak = a.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val g = 0.8f / peak
        for (i in a.indices) a[i] *= g
    }
}
