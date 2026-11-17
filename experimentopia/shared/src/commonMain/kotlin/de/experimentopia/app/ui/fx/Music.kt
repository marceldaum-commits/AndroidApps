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
 * Statt Zufallstönen spielt jede Welt richtige, eingängige Melodien (8 Takte, Frage und Antwort, klarer Schluss) mit
 * Bass, Begleit-Akkorden auf den Nachschlägen und einem leichten Schlagzeug – fröhlich und hüpfend, nicht einlullend.
 * Jede Phrase ist ein ganzes kleines Stück; danach ist es kurz still, dann kommt das nächste (andere Melodie, andere Begleitung).
 */
object Composer {
    const val SR = 22050
    private const val TAU = 2 * PI
    private const val REST = -1

    /** Ein Stück: Melodie als (MIDI-Ton oder REST, Länge in Achteln), Akkorde je Takt als (Grundton, Moll?) */
    private class Tune(val melody: List<Pair<Int, Int>>, val chords: List<Pair<Int, Boolean>>)

    private fun n(vararg x: Int) = x.toList().chunked(2).map { it[0] to it[1] }
    private val C = 48 to false; private val F = 53 to false; private val G = 55 to false; private val Am = 57 to true; private val Dm = 50 to true
    private val Em = 52 to true; private val D = 50 to false; private val E = 52 to false; private val Bb = 46 to false

    /** „Forscher-Marsch“ – hüpfend, Dreiklang hoch, Antwort runter */
    private val MARSCH = Tune(n(64, 2, 67, 2, 72, 2, 67, 2, 69, 2, 67, 1, 65, 1, 64, 4, 65, 2, 69, 2, 74, 2, 72, 1, 71, 1, 72, 6, REST, 2,
        64, 2, 67, 2, 72, 2, 67, 2, 69, 2, 72, 2, 76, 2, 74, 2, 72, 2, 71, 2, 69, 2, 71, 2, 72, 6, REST, 2), listOf(C, F, G, C, C, F, G, C))
    /** „Treppauf“ – Tonleiter-Lauf als Frage, Sprung als Antwort */
    private val TREPPAUF = Tune(n(67, 1, 69, 1, 71, 1, 72, 1, 74, 2, 67, 2, 76, 3, 74, 1, 72, 4, 69, 1, 71, 1, 72, 1, 74, 1, 76, 2, 72, 2, 74, 6, REST, 2,
        67, 1, 69, 1, 71, 1, 72, 1, 74, 2, 79, 2, 77, 2, 76, 2, 74, 2, 72, 2, 69, 2, 74, 2, 72, 2, 71, 2, 72, 6, REST, 2), listOf(C, Am, F, G, C, F, G, C))
    /** „Geheimlabor“ – a-Moll, schleichend, aber mit Augenzwinkern (dorische Sexte) */
    private val GEHEIM = Tune(n(69, 2, 72, 1, 76, 1, 74, 2, 72, 2, 71, 2, 67, 2, 69, 4, 69, 1, 72, 1, 76, 2, 78, 2, 76, 2, 74, 2, 71, 2, 76, 4,
        69, 2, 72, 1, 76, 1, 74, 2, 72, 2, 71, 2, 74, 2, 72, 2, 71, 2, 69, 2, 67, 2, 64, 2, 67, 2, 69, 6, REST, 2), listOf(Am, G, D, E, Am, G, C, Am))
    /** „Sommerwiese“ – F-Dur, wiegend, mit kleinem Jodler-Sprung */
    private val WIESE = Tune(n(72, 2, 69, 1, 72, 1, 77, 2, 76, 2, 74, 2, 70, 2, 72, 4, 69, 1, 70, 1, 72, 2, 74, 2, 72, 1, 70, 1, 69, 2, 67, 2, 65, 4,
        72, 2, 69, 1, 72, 1, 77, 2, 79, 2, 81, 2, 79, 1, 77, 1, 76, 2, 72, 2, 74, 2, 77, 2, 76, 1, 74, 1, 72, 2, 77, 6, REST, 2), listOf(F, Bb, 48 to false, F, F, 48 to false, Bb, F))

    private class Style(val tunes: List<Tune>, val shift: Int, val bpm: Double, val lead: Int, val color: Int)
    private fun style(w: World) = when (w) {
        World.LABOR -> Style(listOf(MARSCH, TREPPAUF), 0, 118.0, 0, 0)
        World.PHYSIK -> Style(listOf(TREPPAUF, MARSCH), 2, 124.0, 1, 1)        // D-Dur, Glockenspiel, etwas flotter
        World.CHEMIE -> Style(listOf(GEHEIM, GEHEIM), 0, 108.0, 2, 2)          // a-Moll, gezupft, Blubbern
        World.BIOLOGIE -> Style(listOf(WIESE, MARSCH), 0, 112.0, 3, 3)         // F-Dur, Marimba und Flöte, Vögel
    }

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Ein ganzes kleines Stück (8 Takte + Ausklang) in der Welt [w]; [seed] wählt Melodie, Begleitung und Verzierungen */
    fun phrase(w: World, seed: Int): FloatArray {
        val s = style(w); val r = Random(seed)
        val tune = s.tunes[seed.mod(s.tunes.size)]
        val shift = s.shift + if (w == World.LABOR && r.nextBoolean()) 5 else 0   // mal in C, mal in F
        val beat = 60.0 / s.bpm; val eighth = beat / 2
        val bars = tune.chords.size
        val out = FloatArray(((bars * 4 * beat + 2.2) * SR).toInt())
        val noise = Random(seed * 31 + 7)
        val busyDrums = r.nextBoolean()
        val leadKind = if (r.nextDouble() < 0.3) (s.lead + 1) % 4 else s.lead   // manchmal spielt ein anderes Instrument die Melodie
        // Begleitung: Bass auf 1 und 3 (Grundton, Quinte), kurze Akkorde auf den Nachschlägen, leises Schlagzeug
        tune.chords.forEachIndexed { bar, (root, minor) ->
            val t0 = bar * 4 * beat
            val rt = root + shift
            bass(out, t0, hz(rt - 12), beat * 0.9, 0.55)
            bass(out, t0 + 2 * beat, hz(rt - 5), beat * 0.9, 0.45)
            if (bar % 2 == 1) bass(out, t0 + 3.5 * beat, hz(rt - 10), beat * 0.4, 0.3)                // kleiner Durchgang
            val chord = listOf(rt + 12, rt + 12 + (if (minor) 3 else 4), rt + 19)
            listOf(1, 3).forEach { b -> chord.forEach { m -> pluck(out, t0 + b * beat, hz(m), 0.16) } }
            if (busyDrums) chord.forEach { m -> pluck(out, t0 + 1.5 * beat, hz(m), 0.08) }
            // Schlagzeug
            kick(out, t0, 0.5); kick(out, t0 + 2 * beat, 0.42)
            snare(out, t0 + beat, 0.16, noise); snare(out, t0 + 3 * beat, 0.16, noise)
            for (e in 0 until 8) if (busyDrums || e % 2 == 1) hat(out, t0 + e * eighth, if (e % 2 == 1) 0.07 else 0.04, noise)
        }
        // Melodie
        var t = 0.0
        tune.melody.forEachIndexed { i, (m, len) ->
            if (m != REST) {
                val note = m + shift
                val accent = (t / beat).let { abs(it - kotlin.math.round(it)) < 1e-6 && kotlin.math.round(it).toInt() % 2 == 0 }
                lead(leadKind, out, t, hz(note), len * eighth * 0.9, if (accent) 0.5 else 0.42)
                // Verzierung: in der zweiten Hälfte manchmal eine Terz darunter (zweistimmig)
                if (t > bars * 2 * beat && r.nextDouble() < 0.35 && len >= 2) lead(leadKind, out, t, hz(note - 3), len * eighth * 0.9, 0.18)
            }
            t += len * eighth
        }
        // Farbtupfer der Welt in den Pausen am Ende von Takt 4 und 8
        listOf(4, 8).forEach { bar -> color(s.color, out, (bar * 4 - 0.5) * beat, r) }
        normalize(out, 0.55f)
        return out
    }

    /** Erkennungsmelodie: E-Piano steigt auf, Bass und Schlagzeug antworten, Glas funkelt, Blubb! */
    fun jingle(): FloatArray {
        val out = FloatArray((3.0 * SR).toInt())
        val e = 0.14
        listOf(67, 72, 76, 79).forEachIndexed { i, m -> epiano(out, i * e, hz(m), e * 0.95, 0.5) }
        epiano(out, 4 * e, hz(84), e * 3.0, 0.55); epiano(out, 4 * e, hz(76), e * 3.0, 0.3)
        bass(out, 4 * e, hz(36), 0.5, 0.6); kick(out, 4 * e, 0.5)
        glass(out, 1.25, hz(96), 0.3); glass(out, 1.38, hz(100), 0.2)
        bubble(out, 1.75, 0.45); bubble(out, 1.9, 0.3)
        normalize(out, 0.6f)
        return out
    }

    // ── Instrumente ──

    private fun lead(kind: Int, b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = when (kind) {
        0 -> epiano(b, t0, f, d, a)
        1 -> { glock(b, t0, f, a * 0.8); epiano(b, t0, f, d, a * 0.35) }
        2 -> pluckLead(b, t0, f, d, a)
        else -> { marimba(b, t0, f, a * 0.8); flute(b, t0, f, d, a * 0.35) }
    }

    /** E-Piano (FM): warmer Anschlag, glockiger Beginn, verklingt weich */
    private fun epiano(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.35) { t ->
        val env = min(1.0, t / 0.004) * exp(-t * 2.2) * (if (t > d) exp(-(t - d) * 14) else 1.0)
        val idx = 1.6 * exp(-t * 6)
        a * env * sin(TAU * f * t + idx * sin(TAU * f * t)) * 0.8
    }

    /** Glockenspiel: hell und klar */
    private fun glock(b: FloatArray, t0: Double, f: Double, a: Double) = add(b, t0, 1.0) { t ->
        a * min(1.0, t / 0.002) * (sin(TAU * 2 * f * t) * exp(-t * 3.5) + 0.25 * sin(TAU * 5.4 * f * t) * exp(-t * 9)) * 0.7
    }

    /** Gezupfte Melodie (wie eine Mandoline) */
    private fun pluckLead(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.3) { t ->
        val env = min(1.0, t / 0.002) * exp(-t * 4.0) * (if (t > d) exp(-(t - d) * 12) else 1.0)
        a * env * ((1..5).sumOf { k -> sin(TAU * k * f * t) * exp(-t * 3.0 * k) / k }) * 0.8
    }

    /** Flöte: weich, leichtes Vibrato */
    private fun flute(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.1) { t ->
        val env = min(1.0, t / 0.05) * (if (t > d) exp(-(t - d) * 25) else 1.0)
        val ph = TAU * f * t + 0.03 * sin(TAU * 5.5 * t) * min(1.0, t / 0.3) * 6
        a * env * (sin(ph) + 0.12 * sin(2 * ph))
    }

    /** Marimba: Holz, Grundton plus schnell verklingender Oberton */
    private fun marimba(b: FloatArray, t0: Double, f: Double, a: Double) = add(b, t0, 0.8) { t ->
        a * min(1.0, t / 0.002) * (sin(TAU * f * t) * exp(-t * 5.0) + 0.3 * sin(TAU * 4 * f * t) * exp(-t * 20))
    }

    /** Begleit-Akkordton: kurz gezupft */
    private fun pluck(b: FloatArray, t0: Double, f: Double, a: Double) = add(b, t0, 0.35) { t ->
        a * min(1.0, t / 0.003) * exp(-t * 11) * (sin(TAU * f * t) + 0.4 * sin(TAU * 2 * f * t) * exp(-t * 20))
    }

    /** Bass: runder, gezupfter Ton */
    private fun bass(b: FloatArray, t0: Double, f: Double, d: Double, a: Double) = add(b, t0, d + 0.1) { t ->
        val env = min(1.0, t / 0.005) * exp(-t * 3.0) * (if (t > d) exp(-(t - d) * 30) else 1.0)
        a * env * (sin(TAU * f * t) + 0.35 * sin(TAU * 2 * f * t) * exp(-t * 8))
    }

    private fun kick(b: FloatArray, t0: Double, a: Double) {
        var ph = 0.0
        add(b, t0, 0.18) { t -> ph += TAU * (45.0 + 90.0 * exp(-t * 35)) / SR; a * exp(-t * 18) * sin(ph) }
    }

    private fun snare(b: FloatArray, t0: Double, a: Double, r: Random) =
        add(b, t0, 0.15) { t -> a * exp(-t * 28) * ((r.nextDouble() * 2 - 1) * 0.8 + 0.4 * sin(TAU * 190 * t)) }

    private fun hat(b: FloatArray, t0: Double, a: Double, r: Random) {
        var last = 0.0
        add(b, t0, 0.04) { t -> val x = r.nextDouble() * 2 - 1; val y = x - last; last = x; a * exp(-t * 90) * y }
    }

    private fun color(kind: Int, b: FloatArray, t: Double, r: Random) = when (kind) {
        0 -> glass(b, t, 2093.0 * listOf(1.0, 1.26, 1.5)[r.nextInt(3)], 0.12)
        1 -> { glass(b, t, 2637.0, 0.1); glass(b, t + 0.12, 3136.0, 0.08) }
        2 -> { bubble(b, t, 0.22); bubble(b, t + 0.1, 0.15) }
        else -> chirp(b, t, 2600.0 + r.nextDouble(600.0), 0.1)
    }

    /** Glas: unharmonischer Oberton wie ein angeschlagenes Reagenzglas */
    private fun glass(b: FloatArray, t0: Double, f: Double, a: Double) = add(b, t0, 1.2) { t ->
        a * min(1.0, t / 0.002) * (sin(TAU * f * t) * exp(-t * 3.0) + 0.3 * sin(TAU * 2.76 * f * t) * exp(-t * 8))
    }

    /** Blubb: aufsteigendes Glucksen */
    private fun bubble(b: FloatArray, t0: Double, a: Double) {
        var ph = 0.0; val dur = 0.11
        add(b, t0, dur) { t -> ph += TAU * (450.0 * (4.0).pow(t / dur)) / SR; a * min(1.0, t / 0.004) * (1 - t / dur).pow(2) * sin(ph) }
    }

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
