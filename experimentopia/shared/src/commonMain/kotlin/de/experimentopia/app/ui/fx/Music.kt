package de.experimentopia.app.ui.fx

import de.experimentopia.core.model.World
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Experimentopias eigene Musik – komplett im Code komponiert und erzeugt, keine Samples, keine Lizenzen.
 * Klang-Identität „verspieltes Forscher-Labor“: Pizzicato-Streicher, Klarinette, Fagott, Glas und Blubbern.
 * Jede Welt klingt eigen (Instrumente, Tonart, Tempo). Statt einer Schleife entsteht jedes Mal eine neue kurze Phrase,
 * danach ist es ein paar Sekunden still – so nervt nichts.
 */
object Composer {
    const val SR = 22050
    private const val TAU = 2 * PI

    private class Style(
        val root: Int, val scale: IntArray, val bpm: Double, val chords: IntArray,
        val melody: (FloatArray, Double, Double, Double, Double) -> Unit,
        val bass: (FloatArray, Double, Double, Double, Double) -> Unit,
        val color: (FloatArray, Double, Random) -> Unit,
        val low: Int,
    )

    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    private val DORIAN = intArrayOf(0, 2, 3, 5, 7, 9, 10)
    private val LYDIAN = intArrayOf(0, 2, 4, 6, 7, 9, 11)

    private fun style(w: World) = when (w) {
        // Labor: C-Dur, Klarinette über Pizzicato, Glas-Funkeln
        World.LABOR -> Style(60, MAJOR, 108.0, intArrayOf(0, 5, 3, 4, 0, 5, 1, 4), ::clarinet, ::pizz, { b, t, r -> if (r.nextBoolean()) glass(b, t, 2093.0 * listOf(1.0, 1.26, 1.5)[r.nextInt(3)], 0.12) else bubble(b, t, 0.18) }, 36)
        // Physik: G-Dur, Glas-Celesta wie ein Uhrwerk, Holzblock-Ticks
        World.PHYSIK -> Style(67, MAJOR, 116.0, intArrayOf(0, 3, 4, 0, 5, 3, 1, 4), { b, t, f, d, a -> glass(b, t, f, a * 0.9); glass(b, t, f * 2, a * 0.2) }, ::pizz, { b, t, _ -> tick(b, t, 0.22); tick(b, t + 0.14, 0.12) }, 43)
        // Chemie: D-dorisch, geheimnisvoll-schrullig, tiefe Klarinette, Fagott-Staccato, Blubbern
        World.CHEMIE -> Style(62, DORIAN, 96.0, intArrayOf(0, 6, 3, 0, 0, 6, 4, 0), { b, t, f, d, a -> clarinet(b, t, f / 2, d, a) }, { b, t, f, d, a -> bassoon(b, t, f, min(d, 0.35), a) }, { b, t, r -> bubble(b, t, 0.22); if (r.nextBoolean()) bubble(b, t + 0.09, 0.14) }, 38)
        // Biologie: F-lydisch, lebendig, Marimba, Fagott-Bögen, Vogel-Zwitschern
        World.BIOLOGIE -> Style(65, LYDIAN, 100.0, intArrayOf(0, 1, 4, 0, 3, 1, 4, 0), ::marimba, ::bassoon, { b, t, r -> chirp(b, t, 2600.0 + r.nextDouble(600.0), 0.1) }, 41)
    }

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
    private fun deg(s: Style, d: Int, octave: Int = 0): Int { val n = s.scale.size; val o = d.floorDiv(n); return s.root + 12 * (o + octave) + s.scale[d.mod(n)] }

    /** Rhythmen je Takt in Achteln (Summe 8) */
    private val RHYTHMS = listOf(intArrayOf(2, 2, 2, 2), intArrayOf(1, 1, 2, 2, 2), intArrayOf(3, 1, 2, 2), intArrayOf(2, 1, 1, 4), intArrayOf(4, 2, 2), intArrayOf(2, 2, 4), intArrayOf(1, 1, 1, 1, 4))

    /** Eine Phrase von 8 Takten in der Welt [w]; [seed] macht jede Phrase anders */
    fun phrase(w: World, seed: Int): FloatArray {
        val s = style(w); val r = Random(seed)
        val beat = 60.0 / s.bpm; val eighth = beat / 2
        val bars = 8
        val out = FloatArray(((bars * 4 * beat + 2.0) * SR).toInt())
        var d = 7 + s.chords[0]   // Melodie startet eine Oktave über dem Grundton
        for (bar in 0 until bars) {
            val t0 = bar * 4 * beat
            val chord = s.chords[bar]
            // Bass: Grundton auf 1, Quinte auf 3
            s.bass(out, t0, hz(deg(s, chord, -1)), beat * 1.6, 0.5)
            s.bass(out, t0 + 2 * beat, hz(deg(s, chord + 4, -1)), beat * 1.4, 0.38)
            // Melodie
            val rhythm = if (bar == bars - 1) intArrayOf(2, 6) else RHYTHMS[r.nextInt(RHYTHMS.size)]
            var t = t0
            rhythm.forEachIndexed { i, len ->
                val strong = i == 0
                d = when {
                    bar == bars - 1 && i == rhythm.lastIndex -> 7                                   // Schluss auf dem Grundton
                    strong -> listOf(chord, chord + 2, chord + 4, chord + 7).minBy { abs(it - d) }  // Akkordton
                    else -> (d + listOf(-2, -1, -1, 1, 1, 2)[r.nextInt(6)]).coerceIn(3, 13)       // kleine Schritte
                }
                val rest = !strong && r.nextDouble() < 0.12
                if (!rest) {
                    // Schrullige Vorschläge: manchmal ein schneller Nachbarton vor dem Hauptton
                    if (strong && r.nextDouble() < 0.25) s.melody(out, t - 0.05, hz(deg(s, d + 1)), 0.05, 0.25)
                    s.melody(out, t, hz(deg(s, d)), len * eighth * 0.92, if (strong) 0.55 else 0.42)
                }
                t += len * eighth
            }
            // Farbtupfer der Welt auf einer unbetonten Zählzeit
            if (r.nextDouble() < 0.45) s.color(out, t0 + (1 + 2 * r.nextInt(2)) * beat + eighth, r)
        }
        normalize(out, 0.55f)
        return out
    }

    /** Erkennungsmelodie: Klarinette steigt auf, Pizzicato antwortet, Glas funkelt, Blubb! */
    fun jingle(): FloatArray {
        val out = FloatArray((3.2 * SR).toInt())
        val e = 0.16
        listOf(67, 72, 76, 79).forEachIndexed { i, m -> clarinet(out, i * e, hz(m), e * 0.95, 0.5) }
        clarinet(out, 4 * e, hz(77), e * 0.6, 0.35); clarinet(out, 4.6 * e, hz(76), e * 2.2, 0.55)
        listOf(60, 64, 67, 72).forEachIndexed { i, m -> pizz(out, 1.2 + i * 0.11, hz(m), 0.5, 0.6) }
        glass(out, 1.66, hz(96), 0.35); glass(out, 1.78, hz(100), 0.22)
        bubble(out, 2.15, 0.45); bubble(out, 2.3, 0.3)
        normalize(out, 0.6f)
        return out
    }

    // ── Instrumente ──

    /** Pizzicato: gezupfte Saite mit hölzernem Körper */
    private fun pizz(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, 0.7) { t ->
        val env = min(1.0, t / 0.002) * exp(-t * 7.0)
        a * env * ((1..6).sumOf { k -> sin(TAU * k * f * t) * exp(-t * (2.0 * k)) / k } + 0.3 * sin(TAU * 220.0 * t) * exp(-t * 40))
    }

    /** Klarinette: ungerade Obertöne, weicher Einsatz, leichtes Vibrato */
    private fun clarinet(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.12) { t ->
        val env = min(1.0, t / 0.035) * (if (t > d) exp(-(t - d) * 30) else 1.0)
        val vib = 1 + (if (t > 0.18) 0.004 * sin(TAU * 5.2 * t) else 0.0)
        val ph = TAU * f * vib * t
        a * env * (sin(ph) + 0.42 * sin(3 * ph) + 0.22 * sin(5 * ph) + 0.1 * sin(7 * ph)) * 0.6
    }

    /** Fagott: tief, näselnd, Obertöne 2 und 3 betont */
    private fun bassoon(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.1) { t ->
        val env = min(1.0, t / 0.04) * (if (t > d) exp(-(t - d) * 25) else 1.0)
        val ph = TAU * f * t
        a * env * (0.6 * sin(ph) + 1.0 * sin(2 * ph) + 0.7 * sin(3 * ph) + 0.35 * sin(4 * ph) + 0.2 * sin(5 * ph)) * 0.35
    }

    /** Marimba: Holz, Grundton plus schnell verklingender vierfacher Oberton */
    private fun marimba(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, 0.9) { t ->
        val env = min(1.0, t / 0.002)
        a * env * (sin(TAU * f * t) * exp(-t * 4.5) + 0.35 * sin(TAU * 4 * f * t) * exp(-t * 18))
    }

    /** Glas: unharmonischer Oberton wie ein angeschlagenes Reagenzglas */
    private fun glass(b: FloatArray, t0: Double, f: Double, a: Double) = add(b, t0, 1.2) { t ->
        val env = min(1.0, t / 0.002)
        a * env * (sin(TAU * f * t) * exp(-t * 3.0) + 0.3 * sin(TAU * 2.76 * f * t) * exp(-t * 8))
    }

    /** Blubb: aufsteigendes Glucksen */
    private fun bubble(b: FloatArray, t0: Double, a: Double) {
        var ph = 0.0; val dur = 0.11
        add(b, t0, dur) { t -> ph += TAU * (450.0 * (4.0).pow(t / dur)) / SR; a * min(1.0, t / 0.004) * (1 - t / dur).pow(2) * sin(ph) }
    }

    /** Holzblock-Tick (Uhrwerk) */
    private fun tick(b: FloatArray, t0: Double, a: Double) = add(b, t0, 0.05) { t -> a * exp(-t * 90) * sin(TAU * 1800 * t) }

    /** Vogel-Zwitschern: kurzer, schneller Gleitton */
    private fun chirp(b: FloatArray, t0: Double, f: Double, a: Double) {
        var ph = 0.0
        add(b, t0, 0.09) { t -> ph += TAU * f * (1 + 0.5 * sin(TAU * 18 * t)) / SR; a * sin(TAU * t / 0.18) * sin(ph) }
    }

    private inline fun add(b: FloatArray, t0: Double, dur: Double, f: (Double) -> Double) {
        val s0 = (t0 * SR).toInt().coerceAtLeast(0); val n = min((dur * SR).toInt(), b.size - s0)
        for (i in 0 until n) b[s0 + i] += f(i.toDouble() / SR).toFloat()
    }

    private fun normalize(a: FloatArray, peak: Float) {
        val m = a.maxOf { abs(it) }.coerceAtLeast(1e-6f); val g = peak / m
        for (i in a.indices) a[i] *= g
    }
}
