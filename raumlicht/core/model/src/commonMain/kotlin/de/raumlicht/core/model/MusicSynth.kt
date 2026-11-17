package de.raumlicht.core.model

import kotlin.math.*

/**
 * Macht aus einem [Song] Klang – die gläserne Klangwelt von Raumlicht, vollständig errechnet (keine Aufnahmen).
 *
 * Instrumente: schwebende Flächen aus leicht verstimmten Sinustönen, angeschlagenes Glas (gedämpfte Schwinger mit
 * leicht unharmonischen Obertönen), Lichtschimmer (hohe, flimmernde Töne), Hauch (gefiltertes Rauschen) – dazu ein
 * Nachhall aus Kammfiltern und Allpässen ([Dsp.reverb]). Alles, was über das Ende hinausklingt (Töne wie Hall), läuft am Anfang
 * weiter – so ist die Schleife nahtlos.
 */
object MusicSynth {
    const val SR = 22050

    /** Lautheit (Effektivwert, 1.0 = Vollausschlag), auf die jedes Stück gebracht wird – so sind alle gleich laut. */
    internal const val TARGET_RMS = 0.15

    fun render(song: Song): ShortArray {
        val x = master(raw(song))
        return ShortArray(x.size) { (x[it] * 32767f).roundToInt().toShort() }
    }

    /** Alle Stimmen gemischt, mit Hall, noch nicht ausgesteuert. */
    internal fun raw(song: Song): FloatArray {
        val n = (song.seconds * SR).roundToInt()
        val direct = FloatArray(n)   // der tiefe Grund: fast ohne Hall, sonst wird es mulmig
        val room = FloatArray(n)     // alles andere klingt im Raum
        val spb = 60.0 / song.bpm
        for (voice in song.voices) for (note in voice.notes) {
            val start = (((note.beat * spb * SR).roundToInt() % n) + n) % n
            val hold = (note.length * spb * SR).toInt()
            val out = if (voice.instrument == Instrument.GRUND) direct else room
            place(voice.instrument, out, start, SpaceMusic.frequency(note.pitch), voice.gain * note.velocity, hold)
        }
        Dsp.reverb(room, direct, wet = 0.34, circular = true)
        for (i in 0 until n) direct[i] += room[i]
        return direct
    }

    /** Gleichanteil entfernen, auf einheitliche Lautheit bringen, Spitzen weich begrenzen (höchstens 0,9). */
    internal fun master(x: FloatArray): FloatArray {
        val n = x.size
        var sum = 0.0
        for (v in x) sum += v
        val mean = (sum / n).toFloat()
        var sq = 0.0
        for (i in 0 until n) { val v = x[i] - mean; x[i] = v; sq += v * v }
        val g = (TARGET_RMS / sqrt(sq / n).coerceAtLeast(1e-9)).toFloat()
        for (i in 0 until n) {
            val v = x[i] * g
            val a = abs(v)
            x[i] = if (a <= 0.5f) v else (0.5f + 0.4f * tanh((a - 0.5f) / 0.4f)) * sign(v)
        }
        return x
    }

    private fun place(inst: Instrument, out: FloatArray, start: Int, f: Double, a: Double, hold: Int) {
        when (inst) {
            // Fläche: zwei um Bruchteile eines Hertz verstimmte, ungleich laute Töne schweben langsam gegeneinander (ohne je ganz
            // zu verlöschen), darüber leise Oktave und Duodezime
            Instrument.FLAECHE -> {
                val att = min(hold * 6 / 10, (1.8 * SR).toInt()); val rel = (2.6 * SR).toInt()
                val df = 0.28 + 0.07 * ((f * 7).toInt() % 5)
                Dsp.tone(out, start, f - df, a * 0.58, hold, att, rel, wrap = true)
                Dsp.tone(out, start, f + df, a * 0.42, hold, att, rel, wrap = true, phase = 1.0)
                Dsp.tone(out, start, 2 * f + 0.4, a * 0.16, hold, att, rel, wrap = true, phase = 2.0)
                Dsp.tone(out, start, 3 * f - 0.3, a * 0.05, hold, att, rel, wrap = true, phase = 0.5)
            }
            // Grund: weicher, tiefer Sinus mit etwas Oktave (damit ihn auch kleine Lautsprecher zeigen) – kein Puls
            Instrument.GRUND -> {
                val att = min(hold / 2, (1.2 * SR).toInt()); val rel = (2.0 * SR).toInt()
                Dsp.tone(out, start, f, a, hold, att, rel, wrap = true)
                Dsp.tone(out, start, 2 * f, a * 0.32, hold, att, rel, wrap = true, phase = 1.0)
                Dsp.tone(out, start, 3 * f, a * 0.09, hold, att, rel, wrap = true, phase = 2.0)
            }
            // Glas: angeschlagenes Weinglas – Grundton als langsam schwebendes Paar, Obertöne 2,32 / 4,25 / 6,63 verklingen schnell
            Instrument.GLAS -> Dsp.glass(out, start, f, a, 2.0 * sqrt(f / 660.0), wrap = true)
            // Perle: dasselbe Glas, nur getupft – weicherer Anschlag, kürzer, weniger Obertöne
            Instrument.PERLE -> {
                val d = 4.2 * sqrt(f / 880.0)
                Dsp.ring(out, start, f, a, d, attack = 110, wrap = true)
                Dsp.ring(out, start, f + 1.3, a * 0.4, d * 1.1, attack = 110, wrap = true)
                Dsp.ring(out, start, f * 2.32, a * 0.13, d * 3.0, attack = 110, wrap = true)
                Dsp.ring(out, start, f * 4.25, a * 0.04, d * 6.0, attack = 110, wrap = true)
            }
            // Schimmer: hohes Tonpaar, dessen Schwebung von selbst flimmert (4–7 Mal je Sekunde)
            Instrument.SCHIMMER -> {
                val att = min(hold / 2, (1.5 * SR).toInt()); val rel = (2.2 * SR).toInt()
                Dsp.tone(out, start, f, a * 0.56, hold, att, rel, wrap = true)
                Dsp.tone(out, start, f * 1.0032, a * 0.44, hold, att, rel, wrap = true, phase = 1.5)
                if (2 * f < 4600) Dsp.tone(out, start, f * 2.002, a * 0.1, hold, att, rel, wrap = true, phase = 0.7)
            }
            // Hauch: schmal gefiltertes Rauschen, das an- und abschwillt
            Instrument.HAUCH -> Dsp.breath(out, start, f, 7.0, a, hold, hold / 2, (1.6 * SR).toInt(), seed = start + f.toInt(), wrap = true)
        }
    }
}

/**
 * Die Bausteine der Klangerzeugung – gemeinsam für Musik ([MusicSynth]) und Kurzklänge ([Klang]).
 * Schwingungen laufen als Zweipol-Schwinger (eine Multiplikation je Sample statt eines Sinus-Aufrufs).
 */
internal object Dsp {
    const val SR = 22050
    private const val TAU = 2 * PI

    /**
     * Gedämpfte Schwingung amp · e^(−decay·t) · sin(2πft): ein angestoßener Körper, der ausklingt.
     * Läuft, bis sie auf ein Tausendstel abgeklungen ist (oder [seconds]); das letzte Achtel blendet ganz auf null.
     */
    fun ring(out: FloatArray, start: Int, f: Double, amp: Double, decay: Double, attack: Int = 66, wrap: Boolean = false, seconds: Double = 0.0) {
        val n = out.size
        if (f >= SR * 0.4 || amp == 0.0 || start >= n) return
        var len = ((if (seconds > 0) seconds else 6.9 / decay) * SR).toInt()
        if (!wrap) len = min(len, n - start)
        if (len <= 1) return
        val release = max(1, len / 8)
        val relStart = len - release
        val w = TAU * f / SR
        val r = exp(-decay / SR)
        val c1 = 2 * r * cos(w); val c2 = r * r
        var y1 = amp / r * sin(-w); var y2 = amp / (r * r) * sin(-2 * w)
        var idx = start
        for (i in 0 until len) {
            val y = c1 * y1 - c2 * y2
            y2 = y1; y1 = y
            val v = if (i < attack) y * i / attack else if (i >= relStart) y * (len - i) / release else y
            out[idx] += v.toFloat()
            if (++idx == n) idx = 0
        }
    }

    /** Angeschlagenes Glas: Grundton als schwebendes Paar plus die leicht unharmonischen Obertöne eines Kelchs. */
    fun glass(out: FloatArray, start: Int, f: Double, amp: Double, decay: Double, bright: Double = 1.0, wrap: Boolean = false) {
        ring(out, start, f, amp, decay, wrap = wrap)
        ring(out, start, f + 0.9, amp * 0.45, decay * 1.1, wrap = wrap)
        ring(out, start, f * 2.32, amp * 0.22 * bright, decay * 2.5, wrap = wrap)
        ring(out, start, f * 4.25, amp * 0.10 * bright, decay * 5.0, wrap = wrap)
        if (f * 6.63 < 7000) ring(out, start, f * 6.63, amp * 0.04 * bright, decay * 9.0, wrap = wrap)
    }

    /** Gehaltener Sinuston mit weichem Ein- und Ausschwingen (S-Kurve): [hold] Samples bis zum Loslassen, dann [release]. */
    fun tone(out: FloatArray, start: Int, f: Double, amp: Double, hold: Int, attack: Int, release: Int, wrap: Boolean = false, phase: Double = 0.0) {
        val n = out.size
        if (f >= SR * 0.4 || start >= n) return
        var len = hold + release
        if (!wrap) len = min(len, n - start)
        val att = max(1, min(attack, hold))
        val w = TAU * f / SR
        val c1 = 2 * cos(w)
        var y1 = amp * sin(phase - w); var y2 = amp * sin(phase - 2 * w)
        var idx = start
        for (i in 0 until len) {
            val y = c1 * y1 - y2
            y2 = y1; y1 = y
            val v = if (i < att) { val s = i.toDouble() / att; y * s * s * (3 - 2 * s) }
            else if (i < hold) y
            else { val s = 1.0 - (i - hold).toDouble() / release; y * s * s * (3 - 2 * s) }
            out[idx] += v.toFloat()
            if (++idx == n) idx = 0
        }
    }

    /** Hauch: Rauschen durch einen schmalen Resonator bei [f] (Güte [q]), an- und abschwellend. */
    fun breath(out: FloatArray, start: Int, f: Double, q: Double, amp: Double, hold: Int, attack: Int, release: Int, seed: Int, wrap: Boolean = false) {
        val n = out.size
        if (start >= n) return
        var len = hold + release
        if (!wrap) len = min(len, n - start)
        val att = max(1, min(attack, hold))
        val w = TAU * f / SR
        val r = exp(-PI * (f / q) / SR)
        val c1 = 2 * r * cos(w); val c2 = r * r
        // Auf Effektivwert 1 bringen (Rauschen gleichverteilt in −1…1 hat die Varianz 1/3)
        val norm = 1.0 / sqrt((1 + c2) / ((1 - c2) * (1 + c2 * c2 - 2 * c2 * cos(2 * w))) / 3.0)
        var y1 = 0.0; var y2 = 0.0
        var rnd = seed * 747796405 + 12345
        var idx = start
        for (i in 0 until len) {
            rnd = rnd * 1664525 + 1013904223
            val x = (rnd ushr 8) * (1.0 / 8388608.0) - 1.0
            val y = x + c1 * y1 - c2 * y2
            y2 = y1; y1 = y
            val e = if (i < att) { val s = i.toDouble() / att; s * s * (3 - 2 * s) }
            else if (i < hold) 1.0
            else { val s = 1.0 - (i - hold).toDouble() / release; s * s * (3 - 2 * s) }
            out[idx] += (y * e * norm * amp).toFloat()
            if (++idx == n) idx = 0
        }
    }

    /** Zwölf lange, teilerfremde Verzögerungen (50–114 ms): dicht liegende Raumresonanzen, damit kein einzelner Ton bevorzugt wird. */
    private val COMB = intArrayOf(1109, 1223, 1361, 1481, 1601, 1741, 1867, 1999, 2113, 2251, 2371, 2503)
    private val ALLPASS = intArrayOf(277, 223, 173, 113)
    /** So weit (in Samples) atmen die Verzögerungen langsam hin und her. */
    private const val DRIFT = 9.0

    /**
     * Nachhall („Licht im Raum“): zwölf parallele Kammfilter mit Höhendämpfung (abwechselnd gepolt), dahinter vier Allpässe,
     * die jedes Echo verwischen; 10 ms Vorverzögerung, Tiefen unter etwa 120 Hz bleiben draußen. Nachhallzeit etwa [t60] Sekunden.
     * [wet] = Lautstärke des Halls im Verhältnis zum trockenen Klang (Effektivwert).
     *
     * Die Verzögerungen atmen ganz langsam (0,13–0,57 Hz, ±0,4 ms). Ohne das hätte der Raum feste Resonanzen, und reine,
     * lange Töne (Flächen, Glas) würden je nach Tonhöhe zufällig lauter oder fast ausgelöscht; so schwebt der Hall stattdessen.
     *
     * Addiert den Hall von [src] auf [dst] (zwei verschiedene Felder!).
     * [circular]: der Hall wird mit dem Ende des Stücks „vorgewärmt“ und das Atmen geht in der Stücklänge genau auf –
     * so klingt er über die Nahtstelle der Schleife hinweg weiter.
     */
    fun reverb(src: FloatArray, dst: FloatArray, wet: Double, t60: Double = 2.4, damp: Double = 0.3, circular: Boolean = false) {
        val n = src.size
        val m = COMB.size
        val cb = Array(m) { FloatArray(COMB[it] + 2 * DRIFT.toInt() + 4) }
        val ci = IntArray(m); val cf = FloatArray(m)
        // Rückkopplung je Kamm so, dass alle gleich lang ausklingen; Ausgang so gewichtet, dass alle gleich viel beitragen
        val fb = FloatArray(m) { 10.0.pow(-3.0 * COMB[it] / (SR * t60)).toFloat() }
        val cg = FloatArray(m) { (wet * sqrt((1.0 - fb[it] * fb[it]) / m) * (if (it % 2 == 0) 1 else -1)).toFloat() }
        val ab = Array(ALLPASS.size) { FloatArray(ALLPASS[it]) }
        val ai = IntArray(ALLPASS.size)
        val pre = SR / 100
        val dp = damp.toFloat(); val keep = 1f - dp
        var lp = 0f
        val warm = if (circular) min(n, (2.5 * t60 * SR).toInt()) else 0
        // Atmen: je Kamm ein langsamer Sinus (als Schwinger gerechnet); in der Schleife mit ganzzahlig vielen Perioden
        val first = if (circular) n - warm else 0
        val lc = DoubleArray(m); val l1 = DoubleArray(m); val l2 = DoubleArray(m)
        for (k in 0 until m) {
            val rate = 0.13 + 0.04 * k
            val w = if (circular) TAU * max(1.0, round(rate * n / SR)) / n else TAU * rate / SR
            lc[k] = 2 * cos(w); l1[k] = DRIFT * sin(w * (first - 1) + k); l2[k] = DRIFT * sin(w * (first - 2) + k)
        }
        for (j in -warm until n) {
            val i = if (j < 0) j + n else j
            var p = i - pre
            if (p < 0 && circular) p += n
            val x = if (p < 0) 0f else src[p]
            lp += 0.0336f * (x - lp)
            val inp = x - lp
            var acc = 0f
            for (k in 0 until m) {
                val lfo = lc[k] * l1[k] - l2[k]
                l2[k] = l1[k]; l1[k] = lfo
                val b = cb[k]; val ix = ci[k]; val size = b.size
                var pos = ix - (COMB[k] + DRIFT + 1 + lfo).toFloat()
                if (pos < 0) pos += size
                val i0 = pos.toInt(); val fr = pos - i0
                val i1 = if (i0 + 1 >= size) 0 else i0 + 1
                val y = b[if (i0 >= size) 0 else i0].let { it + (b[i1] - it) * fr }
                val fl = y * keep + cf[k] * dp
                cf[k] = fl
                b[ix] = inp + fl * fb[k]
                ci[k] = if (ix + 1 == size) 0 else ix + 1
                acc += y * cg[k]
            }
            for (k in ab.indices) {
                val b = ab[k]; val ix = ai[k]
                val y = b[ix] - 0.5f * acc      // echter Allpass: verwischt, ohne einzelne Tonhöhen zu bevorzugen
                b[ix] = acc + 0.5f * y
                acc = y
                ai[k] = if (ix + 1 == b.size) 0 else ix + 1
            }
            if (j >= 0) dst[i] += acc
        }
    }
}
