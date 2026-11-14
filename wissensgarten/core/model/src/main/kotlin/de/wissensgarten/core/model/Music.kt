package de.wissensgarten.core.model

import kotlin.random.Random

/** Ein Ton: Beginn und Länge in Schlägen, MIDI-Tonhöhe, Lautstärke 0..1. */
data class Note(val beat: Double, val length: Double, val pitch: Int, val velocity: Double)

enum class Instrument { KALIMBA, MUSIC_BOX, PLUCK, PAD, BASS }

data class Track(val instrument: Instrument, val notes: List<Note>, val gain: Double)

/** Ein fertiges Stück: nahtlos wiederholbar ([beats] Schläge lang). */
data class Song(val name: String, val bpm: Double, val beats: Double, val voices: List<Track>) {
    val seconds get() = beats * 60.0 / bpm
}

enum class MusicMood { GARDEN, NIGHT, PLAY }

/**
 * Kleiner Komponist für Hintergrundmusik – im Code erzeugt, keine Lizenzen.
 * Einfache, freundliche Akkordfolgen, pentatonische Melodien mit Wiederholung (A A' B A), damit sie
 * im Ohr bleiben, aber nie schief klingen. Gleicher Startwert → gleiches Stück.
 */
object Composer {
    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    private val PENTA = intArrayOf(0, 2, 4, 7, 9)

    /** Akkorde als Stufen der Dur-Tonleiter (0 = I, 5 = vi, 3 = IV, 4 = V). */
    private val PROGRESSIONS = mapOf(
        MusicMood.GARDEN to listOf(0, 5, 3, 4),
        MusicMood.NIGHT to listOf(0, 3, 0, 4),
        MusicMood.PLAY to listOf(0, 4, 5, 3),
    )

    fun compose(mood: MusicMood, seed: Int = 7): Song {
        val rnd = Random(seed * 31 + mood.ordinal)
        val (key, bpm, inst) = when (mood) {
            MusicMood.GARDEN -> Triple(60, 92.0, Instrument.KALIMBA)    // C-Dur
            MusicMood.NIGHT -> Triple(65, 66.0, Instrument.MUSIC_BOX)   // F-Dur
            MusicMood.PLAY -> Triple(62, 108.0, Instrument.PLUCK)       // D-Dur
        }
        val prog = PROGRESSIONS.getValue(mood)
        val bars = 16
        val chords = List(bars) { prog[it % prog.size] }.toMutableList().also { it[bars - 1] = 4; it[bars - 2] = 3 } // Schluss V → zurück zu I
        fun chordTones(deg: Int) = listOf(0, 2, 4).map { scale(key, deg + it) }

        // Melodie: zwei Motive à 4 Takte, Form A A' B A
        val motifA = motif(rnd, mood)
        val motifB = motif(rnd, mood)
        val melody = mutableListOf<Note>()
        listOf(motifA, motifA, motifB, motifA).forEachIndexed { part, motif ->
            motif.forEach { (beat, len, step) ->
                val bar = part * 4 + (beat / 4).toInt()
                val tones = chordTones(chords[bar])
                // betonte Schläge auf Akkordtöne, sonst Pentatonik – klingt nie schief
                val strong = beat % 2.0 == 0.0
                var p = if (strong) nearest(tones.map { it + 12 }, key + 12 + PENTA[step % PENTA.size] + 12 * (step / PENTA.size))
                        else key + 12 + PENTA[step % PENTA.size] + 12 * (step / PENTA.size)
                if (part == 1 && beat >= 12) p = nearest(tones.map { it + 12 }, p + 2) // A' endet anders
                melody += Note(part * 16 + beat, len, p, if (strong) 0.9 else 0.7)
            }
        }
        // Letzter Ton: Grundton, damit die Wiederholung nach Hause kommt
        melody.removeAll { it.beat >= bars * 4 - 2 }
        melody += Note(bars * 4 - 2.0, 2.0, key + 12, 0.8)

        val pad = chords.flatMapIndexed { bar, deg -> chordTones(deg).map { Note(bar * 4.0, 4.0, it, 0.5) } }
        val bass = chords.flatMapIndexed { bar, deg ->
            val root = scale(key, deg) - 12
            when (mood) {
                MusicMood.NIGHT -> listOf(Note(bar * 4.0, 4.0, root, 0.6))
                MusicMood.GARDEN -> listOf(Note(bar * 4.0, 2.0, root, 0.7), Note(bar * 4.0 + 2, 2.0, root + 7, 0.5))
                MusicMood.PLAY -> (0 until 4).map { b -> Note(bar * 4.0 + b, 0.9, if (b % 2 == 0) root else root + 12, if (b == 0) 0.8 else 0.55) }
            }
        }
        val voices = listOf(
            Track(inst, melody, if (mood == MusicMood.NIGHT) 0.55 else 0.5),
            Track(Instrument.PAD, pad, if (mood == MusicMood.PLAY) 0.12 else 0.16),
            Track(Instrument.BASS, bass, if (mood == MusicMood.NIGHT) 0.22 else 0.28),
        )
        return Song(mood.name.lowercase(), bpm, bars * 4.0, voices)
    }

    /** Motiv: (Schlag im 4-Takt-Abschnitt, Länge, Stufe in der Pentatonik) */
    private fun motif(rnd: Random, mood: MusicMood): List<Triple<Double, Double, Int>> {
        val out = mutableListOf<Triple<Double, Double, Int>>()
        var beat = 0.0
        var step = rnd.nextInt(2, 6)
        val lengths = when (mood) {
            MusicMood.NIGHT -> listOf(1.0, 1.0, 2.0, 1.5)
            MusicMood.GARDEN -> listOf(0.5, 1.0, 1.0, 1.5, 0.5)
            MusicMood.PLAY -> listOf(0.5, 0.5, 1.0, 0.5)
        }
        while (beat < 16.0) {
            val len = lengths[rnd.nextInt(lengths.size)].coerceAtMost(16.0 - beat)
            // kleine Schritte, selten Sprünge; Pausen lassen Luft
            if (rnd.nextInt(10) < (if (mood == MusicMood.NIGHT) 2 else 1) && beat % 4.0 != 0.0) { beat += len; continue }
            step = (step + listOf(-1, 1, -1, 1, 2, -2, 0)[rnd.nextInt(7)]).coerceIn(0, 9)
            out += Triple(beat, len, step)
            beat += len
        }
        return out
    }

    fun scale(key: Int, degree: Int): Int {
        val oct = Math.floorDiv(degree, 7)
        return key + 12 * oct + MAJOR[Math.floorMod(degree, 7)]
    }

    private fun nearest(candidates: List<Int>, target: Int): Int {
        // Akkordtöne in allen Oktaven nahe dem Ziel
        val all = candidates.flatMap { c -> (-2..2).map { c + 12 * it } }
        return all.minBy { kotlin.math.abs(it - target) }
    }

    fun frequency(midi: Int) = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
}
