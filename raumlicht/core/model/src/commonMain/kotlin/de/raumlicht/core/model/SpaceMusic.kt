package de.raumlicht.core.model

import kotlin.math.pow
import kotlin.random.Random

/** Wo Musik läuft: schwebend im Menü, mit perlender Bewegung beim Rechnen und Spielen. */
enum class MusicMood { MENU, FLIGHT }

/**
 * Die Instrumente der gläsernen Klangwelt (Klangerzeugung: [MusicSynth]).
 * FLAECHE schwebende Fläche · GRUND tiefer, weicher Grundton · GLAS angeschlagenes Glas · PERLE getupftes Glas ·
 * SCHIMMER hoher Lichtschimmer · HAUCH leiser Atem aus Rauschen
 */
enum class Instrument { FLAECHE, GRUND, GLAS, PERLE, SCHIMMER, HAUCH }

/** Ein Ton: Tonhöhe als MIDI-Nummer (60 = c'), Start und Länge in Schlägen. Glas und Perle klingen frei aus (Länge ohne Wirkung). */
data class Note(val pitch: Int, val beat: Double, val length: Double, val velocity: Double = 1.0)
data class Voice(val instrument: Instrument, val gain: Double, val notes: List<Note>)
data class Song(val bpm: Int, val beats: Int, val voices: List<Voice>) {
    val seconds get() = beats * 60.0 / bpm
}

/**
 * Komponiert die Musik von Raumlicht als nahtlose Schleife: Ambient in D-Dur mit lydischer Färbung (gis/cis über G),
 * „gläserne“ Harmonien (maj9, add9, m11, maj7♯11), lange Flächen, vereinzelte Glastöne, hoher Lichtschimmer.
 * Kein Schlagzeug, kein treibender Bass – ruhig genug zum Rechnen.
 *
 * Akkordfolge (je zwei Takte): Dmaj9 – A(add9)/Cis – Hm11 – Gmaj7♯11 – D(add9)/Fis – Gmaj9 – Em11 – A9sus4.
 */
object SpaceMusic {
    fun frequency(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /**
     * Die Erkennungsmelodie von Raumlicht: a' – d'' – e'' – a'' – cis''' – steigt auf wie ein Lichtstrahl
     * (Quarte, Sekunde, Quarte, große Terz) und endet offen auf der großen Septime. Kehrt in beiden Stücken wieder
     * und ist der Kern der Kurzklänge JINGLE, CHAPTER und WIN ([Klang]).
     */
    val MOTIF: List<Int> = listOf(69, 74, 76, 81, 85)

    /** Der Rhythmus der Erkennungsmelodie: Einsatz jedes Tons in Schlägen (kurz – kurz – lang – kurz – lang). */
    val MOTIF_BEATS: List<Double> = listOf(0.0, 0.5, 1.0, 2.0, 2.5)

    /** Die Erkennungsmelodie als Noten ab Schlag [beat], leicht anschwellend. */
    fun motif(beat: Double, velocity: Double = 1.0): List<Note> =
        MOTIF.mapIndexed { i, p -> Note(p, beat + MOTIF_BEATS[i], 2.0, velocity * (0.72 + 0.07 * i)) }

    private class Chord(val bass: Int, val pad: IntArray, val glass: IntArray)

    private val CHORDS = listOf(
        Chord(50, intArrayOf(57, 62, 64, 66, 69), intArrayOf(74, 76, 78, 81, 85)),   // Dmaj9 (Fläche Dadd9, die Septime cis''' bringt das Glas)
        Chord(49, intArrayOf(57, 59, 64, 69, 73), intArrayOf(73, 76, 81, 83, 88)),   // A(add9)/Cis
        Chord(47, intArrayOf(57, 62, 64, 66, 71), intArrayOf(74, 78, 81, 83, 88)),   // Hm11
        Chord(43, intArrayOf(59, 62, 66, 69, 73), intArrayOf(71, 76, 78, 81, 85)),   // Gmaj7♯11 (lydisch; Glas ohne d, das sich am cis reiben würde)
        Chord(42, intArrayOf(57, 62, 64, 69, 74), intArrayOf(74, 76, 81, 86, 90)),   // D(add9)/Fis
        Chord(43, intArrayOf(59, 62, 66, 69, 71), intArrayOf(74, 79, 81, 83, 86)),   // Gmaj9
        Chord(40, intArrayOf(55, 59, 62, 66, 69), intArrayOf(74, 76, 79, 81, 86)),   // Em11
        Chord(45, intArrayOf(57, 59, 62, 64, 67), intArrayOf(74, 76, 79, 81, 83)),   // A9sus4 – führt zurück nach D
    )

    private const val CHORD_BEATS = 8.0
    private const val BEATS = 64

    /** Perlmuster im Spiel: Achtel je Takt als Stufen des Glas-Akkords, −1 = Pause. Zwei Takte je Akkord, zweite Runde abgewandelt. */
    private val PEARLS = listOf(
        intArrayOf(0, 2, 3, 1, 2, 4, -1, 2), intArrayOf(1, 3, 4, 2, 3, -1, 1, -1),
        intArrayOf(0, 3, 2, 4, -1, 2, 1, 3), intArrayOf(4, 2, 3, 1, 2, -1, 0, -1),
    )

    fun compose(mood: MusicMood): Song {
        val menu = mood == MusicMood.MENU
        val r = Random(if (menu) 11 else 23)
        val pad = ArrayList<Note>(); val ground = ArrayList<Note>(); val glass = ArrayList<Note>()
        val pearl = ArrayList<Note>(); val shimmer = ArrayList<Note>(); val breath = ArrayList<Note>()
        val theme = ArrayList<Note>()
        fun wrap(b: Double) = ((b % BEATS) + BEATS) % BEATS

        // Wo die Erkennungsmelodie leise wiederkehrt (Akkord → Schlag im Akkord); dort schweigt das übrige Glas
        // (nur über Akkorden, deren Fläche kein cis'' direkt unter dem d'' der Melodie hält)
        val motifAt = if (menu) mapOf(0 to 2.0, 5 to 1.5) else mapOf(2 to 4.0, 5 to 4.0)

        CHORDS.forEachIndexed { c, ch ->
            val b0 = c * CHORD_BEATS
            ground += Note(ch.bass, b0, CHORD_BEATS - 0.5)
            // Fläche: die Töne setzen nacheinander ein und überlappen den nächsten Akkord – es fließt, statt zu wechseln
            ch.pad.forEachIndexed { i, p ->
                val late = i * 0.3 + r.nextDouble() * 0.3
                pad += Note(p, b0 + late, CHORD_BEATS - late + 0.4, if (i == ch.pad.lastIndex) 0.92 else 0.84)
            }
            // Lichtschimmer: ein hoher Akkordton, der langsam auf- und abblendet
            val high = ch.glass[1 + r.nextInt(4)].let { if (it + 12 <= 95) it + 12 else it }
            shimmer += Note(high, b0 + 1.0 + r.nextDouble() * 2.0, 4.0, 0.7 + r.nextDouble() * 0.3)
            if (c % 2 == 0) breath += Note(ch.pad.last() + 12, wrap(b0 - 1.5), 3.0, 1.0)

            motifAt[c]?.let { theme += motif(b0 + it) }

            if (menu) {
                // Vereinzelte Glastöne ohne festes Raster – manchmal mit leisem Nachklang eines zweiten Akkordtons
                var last = -1
                val times = if (c in motifAt) listOf(6.6) else List(3) { 0.6 + it * 2.5 + r.nextDouble() * 1.3 }
                for (t in times) {
                    var k = r.nextInt(5); if (k == last) k = (k + 2) % 5
                    last = k
                    val v = 0.5 + r.nextDouble() * 0.4
                    glass += Note(ch.glass[k], b0 + t, 2.0, v)
                    if (r.nextDouble() < 0.4) glass += Note(ch.glass[(k + 2) % 5], b0 + t + 0.34, 2.0, v * 0.55)
                }
            } else {
                // Perlendes Glas in ruhigen Achteln; im Takt der Erkennungsmelodie und am Ende jeder Runde dünnt es aus
                for (half in 0..1) {
                    val bar = c * 2 + half
                    val pattern = PEARLS[half + if (c >= 4) 2 else 0]
                    val motifBar = c in motifAt && half == 1
                    val thin = bar % 8 == 7
                    for (e in 0 until 8) {
                        val k = pattern[e]
                        if (k < 0 || (motifBar && e > 0) || (thin && e > 3)) continue
                        val accent = when (e) { 0 -> 0.85; 4 -> 0.7; else -> 0.48 }
                        pearl += Note(ch.glass[k], b0 + half * 4 + e * 0.5, 0.5, accent + r.nextDouble() * 0.12)
                    }
                }
                // dazu ein einzelner, länger klingender Glaston je Akkord
                if (c !in motifAt) glass += Note(ch.glass[2 + r.nextInt(3)], b0 + 5.0 + r.nextInt(3) * 0.5, 2.0, 0.6 + r.nextDouble() * 0.3)
            }
        }

        return Song(if (menu) 60 else 72, BEATS, listOf(
            Voice(Instrument.FLAECHE, if (menu) 0.1 else 0.085, pad),
            Voice(Instrument.GRUND, if (menu) 0.06 else 0.055, ground),
            Voice(Instrument.GLAS, if (menu) 0.22 else 0.16, glass),
            Voice(Instrument.GLAS, if (menu) 0.19 else 0.18, theme),
            Voice(Instrument.PERLE, 0.15, pearl),
            Voice(Instrument.SCHIMMER, 0.045, shimmer),
            Voice(Instrument.HAUCH, 0.011, breath),
        ))
    }
}
