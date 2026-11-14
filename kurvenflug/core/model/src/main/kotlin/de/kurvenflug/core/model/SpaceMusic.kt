package de.kurvenflug.core.model

import kotlin.math.pow
import kotlin.random.Random

/** Wo Musik läuft: ruhig im Menü, treibender im Level. */
enum class MusicMood { MENU, FLIGHT }

enum class Instrument { PAD, ARP, BASS, SPARKLE }

/** Ein Ton: Tonhöhe als MIDI-Nummer (60 = c'), Start und Länge in Schlägen. */
data class Note(val pitch: Int, val beat: Double, val length: Double, val velocity: Double = 1.0)
data class Voice(val instrument: Instrument, val gain: Double, val notes: List<Note>)
data class Song(val bpm: Int, val beats: Int, val voices: List<Voice>) {
    val seconds get() = beats * 60.0 / bpm
}

/**
 * Komponiert elektronische Weltraum-Musik als nahtlose Schleife – synthwave-artig, nicht kindlich.
 * Akkordfolge in Moll (Am – F – C – G), darüber ein Arpeggio und funkelnde Glöckchen.
 */
object SpaceMusic {
    fun frequency(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private val chords = listOf(
        listOf(57, 60, 64),   // a-Moll
        listOf(53, 57, 60),   // F-Dur
        listOf(48, 52, 55),   // C-Dur
        listOf(55, 59, 62),   // G-Dur
    )

    fun compose(mood: MusicMood): Song {
        val bpm = if (mood == MusicMood.MENU) 84 else 104
        val bars = 8
        val beats = bars * 4
        val r = Random(if (mood == MusicMood.MENU) 3 else 9)
        val pad = ArrayList<Note>(); val arp = ArrayList<Note>(); val bass = ArrayList<Note>(); val sparkle = ArrayList<Note>()
        for (bar in 0 until bars) {
            val c = chords[bar % chords.size]
            val b0 = bar * 4.0
            c.forEach { pad += Note(it, b0, 4.0, 0.8) }
            // Bass: Grundton eine Oktave tiefer, im Level als treibende Achtel
            if (mood == MusicMood.MENU) bass += Note(c[0] - 12, b0, 4.0)
            else for (i in 0 until 8) bass += Note(c[0] - 12, b0 + i * 0.5, 0.45, if (i % 2 == 0) 1.0 else 0.6)
            // Arpeggio auf/ab, im Menü Achtel, im Level Sechzehntel
            val pattern = listOf(0, 1, 2, 1) .map { c[it] + 12 }
            val step = if (mood == MusicMood.MENU) 0.5 else 0.25
            var t = 0.0; var i = 0
            while (t < 4.0 - 1e-9) {
                arp += Note(pattern[i % pattern.size], b0 + t, step * 0.9, if (i % 4 == 0) 1.0 else 0.7)
                t += step; i++
            }
            // Funkeln: ab und zu ein hoher Akkordton
            repeat(if (mood == MusicMood.MENU) 2 else 3) {
                sparkle += Note(c[r.nextInt(3)] + 24, b0 + r.nextInt(8) * 0.5, 0.5, 0.5 + r.nextDouble() * 0.5)
            }
        }
        return Song(bpm, beats, listOf(
            Voice(Instrument.PAD, 0.28, pad),
            Voice(Instrument.ARP, if (mood == MusicMood.MENU) 0.12 else 0.16, arp),
            Voice(Instrument.BASS, 0.35, bass),
            Voice(Instrument.SPARKLE, 0.1, sparkle),
        ))
    }
}
