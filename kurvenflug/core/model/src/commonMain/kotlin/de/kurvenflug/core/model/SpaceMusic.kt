package de.kurvenflug.core.model

import kotlin.math.pow
import kotlin.random.Random

/**
 * Was gerade klingt: Menü ruhig, Flug spannend, Sturm treibend, Extrem noch treibender, still im Unterricht.
 * [variant] gibt jedem Kapitel bzw. Planeten seine eigene Variation (Tonart, Akkorde, Arpeggio, Tempo).
 */
data class MusicMood(val mode: Mode, val variant: Int = 0) {
    enum class Mode { MENU, FLIGHT, STORM, EXTREME }

    companion object {
        val MENU = MusicMood(Mode.MENU)
        val FLIGHT = MusicMood(Mode.FLIGHT)
        val entries = listOf(MENU, FLIGHT, MusicMood(Mode.STORM), MusicMood(Mode.EXTREME))
    }
}

enum class Instrument { PAD, ARP, BASS, SPARKLE, LEAD, KICK, HAT }

/** Ein Ton: Tonhöhe als MIDI-Nummer (60 = c'), Start und Länge in Schlägen. */
data class Note(val pitch: Int, val beat: Double, val length: Double, val velocity: Double = 1.0)
data class Voice(val instrument: Instrument, val gain: Double, val notes: List<Note>)
data class Song(val bpm: Int, val beats: Int, val voices: List<Voice>, val loop: Boolean = true) {
    val seconds get() = beats * 60.0 / bpm
}

/**
 * Kurvenflugs eigene Klangwelt: Synthwave im Weltraum – weite Flächen, Arpeggios, Hall, ein wiederkehrendes
 * „Kurven-Motiv“ (steigt wie eine Flugbahn und fällt wieder). Alles im Code komponiert, keine fremden Samples.
 * 16 Takte in zwei Teilen (A ohne, B mit Melodie), damit die Schleife nicht nervt; jede Variante klingt anders.
 */
object SpaceMusic {
    fun frequency(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Akkorde als Halbtöne über dem Grundton (natürliches Moll) */
    private val I = listOf(0, 3, 7); private val IV = listOf(5, 8, 12); private val V5 = listOf(7, 10, 14); private val V = listOf(7, 11, 14)
    private val VI = listOf(-4, 0, 3); private val III = listOf(3, 7, 10); private val VII = listOf(-2, 2, 5)
    private val progressions = listOf(
        listOf(I, VI, III, VII), listOf(I, IV, VI, V), listOf(I, VII, VI, VII), listOf(I, III, VII, IV),
        listOf(VI, VII, I, I), listOf(I, V5, VI, IV), listOf(I, VI, IV, V), listOf(VI, III, VII, I),
    )
    private val arpPatterns = listOf(listOf(0, 1, 2, 1), listOf(0, 2, 1, 2), listOf(0, 1, 2, 3), listOf(2, 1, 0, 1), listOf(0, 2, 3, 1), listOf(0, 3, 1, 2))
    /** Tonarten (Grundton als MIDI): a, c, d, e, g, f, h, fis … */
    private val roots = listOf(57, 60, 62, 52, 55, 53, 59, 54, 56, 58, 61, 50)

    /** Das Kurven-Motiv in Moll-Pentatonik-Stufen: rauf wie eine Flugbahn, dann sanft runter. */
    val motif = listOf(0, 3, 5, 7, 10, 7, 5, 3)
    private val motifRhythm = listOf(0.0, 0.5, 1.0, 1.5, 2.5, 3.0, 3.5, 5.0)

    /** Erkennungsmelodie (ca. 2,5 s): das Motiv hell und schnell, mit Schlussakkord. Keine Schleife. */
    fun jingle(): Song {
        val root = 69
        val lead = motif.mapIndexed { i, s -> Note(root + s, i * 0.25, 0.3, 0.9) } + Note(root + 12, 2.0, 2.0, 1.0)
        val pad = (I.map { Note(root - 12 + it, 2.0, 2.0, 0.7) })
        val sparkle = listOf(Note(root + 24, 2.0, 1.0, 0.6), Note(root + 31, 2.25, 1.0, 0.5))
        return Song(132, 5, listOf(Voice(Instrument.LEAD, 0.35, lead), Voice(Instrument.PAD, 0.25, pad), Voice(Instrument.SPARKLE, 0.2, sparkle)), loop = false)
    }

    fun compose(m: MusicMood): Song {
        val v = m.variant
        val calm = m.mode == MusicMood.Mode.MENU
        val drive = m.mode == MusicMood.Mode.STORM || m.mode == MusicMood.Mode.EXTREME
        val extreme = m.mode == MusicMood.Mode.EXTREME
        val bpm = when (m.mode) {
            MusicMood.Mode.MENU -> 80 + (v % 4) * 2
            MusicMood.Mode.FLIGHT -> 98 + (v % 5) * 2
            MusicMood.Mode.STORM -> 118 + (v % 3) * 2
            MusicMood.Mode.EXTREME -> 128
        }
        val root = roots[floorModK(v.toLong() * 5 + m.mode.ordinal, roots.size.toLong()).toInt()]
        val progA = progressions[floorModK(v.toLong() + m.mode.ordinal, progressions.size.toLong()).toInt()]
        val progB = progressions[floorModK(v.toLong() * 3 + 1 + m.mode.ordinal, progressions.size.toLong()).toInt()]
        val pattern = arpPatterns[floorModK(v.toLong() * 7 + m.mode.ordinal, arpPatterns.size.toLong()).toInt()]
        val r = Random(1000 + v * 31 + m.mode.ordinal)
        val bars = 16
        val pad = ArrayList<Note>(); val arp = ArrayList<Note>(); val bass = ArrayList<Note>(); val sparkle = ArrayList<Note>()
        val lead = ArrayList<Note>(); val kick = ArrayList<Note>(); val hat = ArrayList<Note>()
        for (bar in 0 until bars) {
            val partB = bar >= 8
            val chord = (if (partB) progB else progA)[bar % 4].map { root + it }
            val b0 = bar * 4.0
            chord.forEach { pad += Note(it, b0, 4.0, if (partB) 0.75 else 0.85) }
            // Bass: im Menü lange Töne, im Flug Achtel, im Sturm/Extrem mit Oktavsprüngen
            when {
                calm -> bass += Note(chord[0] - 12, b0, 4.0)
                extreme -> for (i in 0 until 16) bass += Note(chord[0] - 12 + (if (i % 4 == 2) 12 else 0), b0 + i * 0.25, 0.22, if (i % 2 == 0) 1.0 else 0.65)
                else -> for (i in 0 until 8) bass += Note(chord[0] - 12, b0 + i * 0.5, 0.45, if (i % 2 == 0) 1.0 else 0.6)
            }
            // Arpeggio: Menü Achtel, sonst Sechzehntel; im B-Teil eine Oktave höher (Spannung)
            val step = if (calm) 0.5 else 0.25
            val notes = pattern.map { k -> if (k < 3) chord[k] + 12 else chord[0] + 24 }.map { if (partB && !calm && it + 12 <= 93) it + 12 else it }
            var t = 0.0; var i = 0
            while (t < 4.0 - 1e-9) { arp += Note(notes[i % notes.size], b0 + t, step * 0.9, if (i % 4 == 0) 1.0 else 0.65); t += step; i++ }
            // Funkeln: ab und zu ein hoher Akkordton
            repeat(if (calm) 2 else 3) { sparkle += Note(chord[r.nextInt(3)] + 24, b0 + r.nextInt(8) * 0.5, 0.5, 0.45 + r.nextDouble() * 0.45) }
            // Takt: nur Sturm/Extrem – Kick auf jedem Schlag, Hi-Hat dazwischen
            if (drive) for (beat in 0 until 4) {
                kick += Note(36, b0 + beat, 0.3, 1.0)
                hat += Note(90, b0 + beat + 0.5, 0.1, 0.6)
                if (extreme) { hat += Note(90, b0 + beat + 0.25, 0.05, 0.35); hat += Note(90, b0 + beat + 0.75, 0.05, 0.35) }
            }
        }
        // Melodie: das Kurven-Motiv im B-Teil (Takte 9–10 und 13–14), je Variante umgekehrt oder versetzt
        val scale = listOf(0, 3, 5, 7, 10, 12, 15, 17, 19, 22)
        val shape = if (v % 2 == 1) motif.reversed() else motif
        for (start in listOf(8, 12)) {
            val shift = if (start == 12) (if (v % 3 == 0) 2 else -2) else 0
            shape.forEachIndexed { i, s ->
                val deg = scale.indexOf(s).coerceAtLeast(0)
                val p = root + 12 + scale[(deg + shift).coerceIn(0, scale.lastIndex)]
                lead += Note(p, start * 4.0 + motifRhythm[i], if (i == shape.lastIndex) 2.5 else 0.45, if (i == 4) 1.0 else 0.8)
            }
        }
        return Song(bpm, bars * 4, buildList {
            add(Voice(Instrument.PAD, if (calm) 0.3 else 0.24, pad))
            add(Voice(Instrument.ARP, if (calm) 0.11 else if (drive) 0.17 else 0.15, arp))
            add(Voice(Instrument.BASS, 0.34, bass))
            add(Voice(Instrument.SPARKLE, 0.09, sparkle))
            add(Voice(Instrument.LEAD, if (calm) 0.16 else 0.14, lead))
            if (drive) { add(Voice(Instrument.KICK, 0.45, kick)); add(Voice(Instrument.HAT, 0.07, hat)) }
        })
    }

    /** Welche Musik zu welchem Level gehört: Kapitel = eigene Variation, Sturm/Extrem treibend. */
    fun forLevel(id: String, chapter: Int): MusicMood = when {
        id.startsWith("storm-") -> MusicMood(MusicMood.Mode.STORM, id.substringAfterLast('-').toIntOrNull()?.let { it / 4 } ?: 0)
        ChapterExtreme.isExtreme(id) -> MusicMood(MusicMood.Mode.EXTREME, chapter)
        Andromeda.isAndromeda(id) -> id.removePrefix(Andromeda.PREFIX).substringBefore('-').toIntOrNull().let { n ->
            if (n != null && Andromeda.tier(n) >= Tier.PRO) MusicMood(MusicMood.Mode.EXTREME, n) else MusicMood(MusicMood.Mode.FLIGHT, 100 + (n ?: 0)) }
        id.startsWith("boss-") -> MusicMood(MusicMood.Mode.STORM, chapter)
        chapter in 1..99 -> MusicMood(MusicMood.Mode.FLIGHT, chapter)
        else -> MusicMood(MusicMood.Mode.FLIGHT, 0)
    }
}
