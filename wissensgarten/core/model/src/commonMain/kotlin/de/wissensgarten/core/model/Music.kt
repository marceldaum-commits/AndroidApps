package de.wissensgarten.core.model

import kotlin.math.pow
import kotlin.random.Random

/** Ein Ton: Beginn und Länge in Schlägen, MIDI-Tonhöhe, Lautstärke 0..1. */
data class Note(val beat: Double, val length: Double, val pitch: Int, val velocity: Double)

/**
 * Die Instrumente des Gartens – alle im Code erzeugt (siehe MusicSynth).
 * Melodie: Kalimba, Marimba, Glockenspiel, Spieluhr, Gitarre, Zupfklang. Begleitung: Klangteppich, Bass.
 * Natur (Tonhöhe ist hier nur ein Anhaltspunkt): Vögel, Grillen, Wind, Wassertropfen, Brummis Summen.
 */
enum class Instrument { KALIMBA, MUSIC_BOX, PLUCK, PAD, BASS, MARIMBA, GLOCKENSPIEL, GUITAR, BIRD, CRICKET, BREEZE, DROP, HUM;
    val isNature get() = this == BIRD || this == CRICKET || this == BREEZE || this == DROP || this == HUM
}

data class Track(val instrument: Instrument, val notes: List<Note>, val gain: Double)

/** Ein fertiges Stück: nahtlos wiederholbar ([beats] Schläge lang). */
data class Song(val name: String, val bpm: Double, val beats: Double, val voices: List<Track>) {
    val seconds get() = beats * 60.0 / bpm
}

/**
 * Stimmungen. Im Garten klingt jede Jahreszeit anders ([SPRING] … [WINTER]); [GARDEN] ist der Sommer-Klang als Rückfall.
 * Gieß-Runde [PLAY], Schlaflied [NIGHT], Feste ([ADVENT], [HALLOWEEN], [OSTERN]).
 */
enum class MusicMood { GARDEN, NIGHT, PLAY, ADVENT, HALLOWEEN, OSTERN, SPRING, SUMMER, AUTUMN, WINTER;
    companion object {
        fun of(season: Season) = when (season) {
            Season.SPRING -> SPRING; Season.SUMMER -> SUMMER; Season.AUTUMN -> AUTUMN; Season.WINTER -> WINTER
        }
    }
}

/**
 * Kleiner Komponist für die Garten-Musik – im Code erzeugt, keine fremden Aufnahmen, keine Lizenzen.
 *
 * Klang-Identität Wissensgarten: warm und natürlich – Kalimba, Marimba, Glockenspiel und leise Gitarre, dazu Natur
 * (Vögel, Wind, Wasser, Brummis Summen). Freundliche Akkordfolgen, pentatonische Melodien mit Wiederholung
 * (A A' B A C A), damit sie im Ohr bleiben, aber nie schief klingen. Im Garten folgt auf die Melodie eine
 * „Atempause“ nur mit Natur und leisem Teppich – so wird die Schleife nicht nervig. Gleicher Startwert → gleiches Stück.
 */
object Composer {
    val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    val MINOR = intArrayOf(0, 2, 3, 5, 7, 8, 10)
    private val PENTA_MAJOR = intArrayOf(0, 2, 4, 7, 9)
    private val PENTA_MINOR = intArrayOf(0, 3, 5, 7, 10)

    /** Begleitung: wie Bass und Gitarre spielen */
    enum class Comp { CALM, WALK, BOUNCE, STRUM }

    /**
     * Grundton (MIDI), Tempo, Melodie-Instrument, Antwort-Instrument (spielt Teil C und Glanzlichter),
     * Begleitung, Natur-Klänge, Takte Atempause nach der Melodie.
     */
    data class Style(
        val key: Int, val bpm: Double, val instrument: Instrument, val minor: Boolean = false,
        val answer: Instrument = Instrument.GLOCKENSPIEL, val comp: Comp = Comp.WALK,
        val nature: List<Instrument> = emptyList(), val breather: Int = 0,
        val progression: List<Int> = listOf(0, 5, 3, 4),
    )

    fun style(m: MusicMood) = when (m) {
        // Frühling: hüpfende Marimba, Glockenspiel antwortet, viele Vögel, Wassertropfen
        MusicMood.SPRING -> Style(62, 96.0, Instrument.MARIMBA, answer = Instrument.GLOCKENSPIEL, comp = Comp.BOUNCE,
            nature = listOf(Instrument.BIRD, Instrument.BIRD, Instrument.DROP, Instrument.HUM), breather = 8, progression = listOf(0, 3, 5, 4))
        // Sommer: Kalimba mit Gitarre, Brummi summt, ein paar Vögel
        MusicMood.SUMMER, MusicMood.GARDEN -> Style(60, 92.0, Instrument.KALIMBA, answer = Instrument.MARIMBA, comp = Comp.STRUM,
            nature = listOf(Instrument.BIRD, Instrument.HUM, Instrument.HUM), breather = 8)
        // Herbst: warme Gitarre, Marimba antwortet, Wind und Regentropfen
        MusicMood.AUTUMN -> Style(65, 84.0, Instrument.GUITAR, answer = Instrument.MARIMBA, comp = Comp.CALM,
            nature = listOf(Instrument.BREEZE, Instrument.DROP, Instrument.DROP), breather = 8, progression = listOf(5, 3, 0, 4))
        // Winter: Glockenspiel wie Schneeflocken, Spieluhr antwortet, leiser Wind
        MusicMood.WINTER -> Style(67, 72.0, Instrument.GLOCKENSPIEL, answer = Instrument.MUSIC_BOX, comp = Comp.CALM,
            nature = listOf(Instrument.BREEZE), breather = 6, progression = listOf(0, 3, 0, 4))
        // Schlaflied: Spieluhr, Grillen
        MusicMood.NIGHT -> Style(65, 66.0, Instrument.MUSIC_BOX, answer = Instrument.KALIMBA, comp = Comp.CALM,
            nature = listOf(Instrument.CRICKET), breather = 6, progression = listOf(0, 3, 0, 4))
        // Gieß-Runde: Marimba und Gitarre, schwungvoll, ohne Natur (Konzentration)
        MusicMood.PLAY -> Style(62, 108.0, Instrument.MARIMBA, answer = Instrument.KALIMBA, comp = Comp.BOUNCE, progression = listOf(0, 4, 5, 3))
        MusicMood.ADVENT -> Style(67, 76.0, Instrument.MUSIC_BOX, answer = Instrument.GLOCKENSPIEL, comp = Comp.CALM, breather = 4, progression = listOf(0, 3, 4, 0))
        MusicMood.HALLOWEEN -> Style(57, 100.0, Instrument.PLUCK, minor = true, answer = Instrument.MARIMBA, comp = Comp.BOUNCE,
            nature = listOf(Instrument.BREEZE))
        MusicMood.OSTERN -> Style(69, 112.0, Instrument.KALIMBA, answer = Instrument.GLOCKENSPIEL, comp = Comp.WALK,
            nature = listOf(Instrument.BIRD, Instrument.BIRD), breather = 4, progression = listOf(0, 3, 0, 4))
    }

    /** Takte mit Melodie (sechs Teile à 4 Takte: A A' B A C A) */
    const val MELODY_BARS = 24

    fun compose(mood: MusicMood, seed: Int = 7): Song {
        val rnd = Random(seed * 31 + mood.ordinal)
        val st = style(mood)
        val key = st.key
        val steps = if (st.minor) MINOR else MAJOR
        val penta = if (st.minor) PENTA_MINOR else PENTA_MAJOR
        val prog = st.progression
        val bars = MELODY_BARS + st.breather
        val chords = MutableList(bars) { prog[it % prog.size] }.also {
            it[MELODY_BARS - 1] = 4; it[MELODY_BARS - 2] = 3                        // Schluss der Melodie: IV → V → zurück zu I
            if (st.breather > 0) { it[MELODY_BARS] = 0; it[bars - 1] = 4 }          // Atempause beginnt zu Hause, endet auf V
        }
        fun chordTones(deg: Int) = listOf(0, 2, 4).map { scale(key, deg + it, steps) }

        val calm = mood == MusicMood.NIGHT || mood == MusicMood.ADVENT || mood == MusicMood.WINTER
        val motifA = motif(rnd, calm)
        val motifB = motif(rnd, calm)
        val motifC = motif(rnd, calm)
        // hohe Tonarten eine Oktave tiefer, damit nichts schrill wird; Glockenspiel klingt von Natur aus hoch
        val lift = (if (key >= 67) 0 else 12) - (if (st.instrument == Instrument.GLOCKENSPIEL) 12 else 0)
        fun pentaPitch(step: Int) = key + lift + penta[step % penta.size] + 12 * (step / penta.size)
        val lead = mutableListOf<Note>()
        val answer = mutableListOf<Note>()
        listOf(motifA, motifA, motifB, motifA, motifC, motifA).forEachIndexed { part, motif ->
            val target = if (part == 4) answer else lead          // Teil C: das Antwort-Instrument ist dran
            val shift = if (part == 4 && st.answer == Instrument.GLOCKENSPIEL) -12 else 0
            motif.forEach { (beat, len, step) ->
                val bar = part * 4 + (beat / 4).toInt()
                val tones = chordTones(chords[bar]).map { it + lift }
                val strong = beat % 2.0 == 0.0
                var p = if (strong) nearest(tones, pentaPitch(step)) else pentaPitch(step)
                if (part == 1 && beat >= 12) p = nearest(tones, p + 2)   // A' endet anders
                target += Note(part * 16 + beat, len, p + shift, if (strong) 0.9 else 0.7)
            }
        }
        // Letzter Melodie-Ton: Grundton, damit die Musik nach Hause kommt
        val end = MELODY_BARS * 4.0
        lead.removeAll { it.beat >= end - 2 }
        lead += Note(end - 2.0, 2.0, key + lift, 0.8)
        // Glanzlichter: in Teil B ein paar lange Antwort-Töne über der Melodie
        for (bar in 8 until 12) if (bar % 2 == 0) answer += Note(bar * 4.0 + 2, 2.0, chordTones(chords[bar])[2] + 12 + (if (st.answer == Instrument.GLOCKENSPIEL) -12 else 0), 0.5)

        // Klangteppich: in der Atempause leiser
        val pad = chords.flatMapIndexed { bar, deg -> chordTones(deg).map { Note(bar * 4.0, 4.0, it, if (bar >= MELODY_BARS) 0.35 else 0.5) } }
        val bass = mutableListOf<Note>()
        val guitar = mutableListOf<Note>()
        chords.forEachIndexed { bar, deg ->
            val root = scale(key, deg, steps) - 12
            val fifth = scale(key, deg + 4, steps) - 12
            val b = bar * 4.0
            if (bar >= MELODY_BARS) { bass += Note(b, 4.0, root, 0.4); return@forEachIndexed }
            when (st.comp) {
                Comp.CALM -> bass += Note(b, 4.0, root, 0.6)
                Comp.WALK -> { bass += Note(b, 2.0, root, 0.7); bass += Note(b + 2, 2.0, fifth, 0.5) }
                Comp.BOUNCE -> (0 until 4).forEach { k -> bass += Note(b + k, 0.9, if (k % 2 == 0) root else root + 12, if (k == 0) 0.8 else 0.55) }
                Comp.STRUM -> {
                    bass += Note(b, 2.0, root, 0.6); bass += Note(b + 2, 2.0, fifth, 0.45)
                    // Gitarre: sanftes Zupfmuster aus Akkordtönen (Grundton – Quinte – Terz – Quinte)
                    val t = chordTones(deg)
                    listOf(t[0], t[2], t[1] + 12, t[2]).forEachIndexed { k, p -> guitar += Note(b + k, 1.0, p, if (k == 0) 0.55 else 0.4) }
                }
            }
        }
        // Herbst und Winter: Gitarre bzw. Spieluhr tupfen leise Akkordtöne in der Atempause
        if (st.breather > 0) for (bar in MELODY_BARS until bars) if (bar % 2 == 0) {
            val t = chordTones(chords[bar])
            answer += Note(bar * 4.0 + 1, 2.0, t[1] + lift - (if (st.answer == Instrument.GLOCKENSPIEL) 12 else 0), 0.35)
        }

        val nature = natureTracks(st, bars, key, rnd)
        val voices = listOfNotNull(
            Track(st.instrument, lead, if (calm) 0.55 else 0.5),
            Track(st.answer, answer, 0.38).takeIf { answer.isNotEmpty() },
            Track(Instrument.PAD, pad, if (st.comp == Comp.BOUNCE) 0.11 else 0.15),
            Track(Instrument.BASS, bass, if (calm) 0.22 else 0.27),
            Track(Instrument.GUITAR, guitar, 0.2).takeIf { guitar.isNotEmpty() },
        ) + nature
        return Song(mood.name.lowercase(), st.bpm, bars * 4.0, voices)
    }

    /** Natur: verstreute, nie gleichmäßige Ereignisse – in der Atempause etwas dichter. */
    private fun natureTracks(st: Style, bars: Int, key: Int, rnd: Random): List<Track> {
        val beats = bars * 4.0
        val quietFrom = MELODY_BARS * 4.0
        return st.nature.groupingBy { it }.eachCount().map { (inst, weight) ->
            val notes = mutableListOf<Note>()
            // mittlerer Abstand in Schlägen (Melodie-Teil / Atempause)
            val (gap, gapQuiet, len) = when (inst) {
                Instrument.BIRD -> Triple(14.0, 4.0, 1.0)
                Instrument.CRICKET -> Triple(10.0, 5.0, 2.0)
                Instrument.BREEZE -> Triple(20.0, 10.0, 10.0)
                Instrument.DROP -> Triple(9.0, 3.0, 0.5)
                Instrument.HUM -> Triple(48.0, 20.0, 8.0)
                else -> Triple(16.0, 8.0, 1.0)
            }
            var b = rnd.nextDouble(0.0, gap)
            while (b < beats - 0.5) {
                val v = rnd.nextDouble(0.55, 1.0)
                notes += Note(kotlin.math.floor(b * 4) / 4, len, key + 24, v)
                val g = (if (b >= quietFrom) gapQuiet else gap) / weight
                b += g * rnd.nextDouble(0.6, 1.5)
            }
            val gain = when (inst) {
                Instrument.BIRD -> 0.075; Instrument.CRICKET -> 0.035; Instrument.BREEZE -> 0.07
                Instrument.DROP -> 0.08; Instrument.HUM -> 0.05; else -> 0.05
            }
            Track(inst, notes, gain)
        }
    }

    /** Motiv: (Schlag im 4-Takt-Abschnitt, Länge, Stufe in der Pentatonik) */
    private fun motif(rnd: Random, calm: Boolean): List<Triple<Double, Double, Int>> {
        val out = mutableListOf<Triple<Double, Double, Int>>()
        var beat = 0.0
        var step = rnd.nextInt(2, 6)
        val lengths = if (calm) listOf(1.0, 1.0, 2.0, 1.5) else listOf(0.5, 1.0, 1.0, 1.5, 0.5)
        while (beat < 16.0) {
            val len = lengths[rnd.nextInt(lengths.size)].coerceAtMost(16.0 - beat)
            // kleine Schritte, selten Sprünge; Pausen lassen Luft
            if (rnd.nextInt(10) < (if (calm) 2 else 1) && beat % 4.0 != 0.0) { beat += len; continue }
            step = (step + listOf(-1, 1, -1, 1, 2, -2, 0)[rnd.nextInt(7)]).coerceIn(0, 9)
            out += Triple(beat, len, step)
            beat += len
        }
        return out
    }

    fun scale(key: Int, degree: Int, steps: IntArray = MAJOR): Int {
        val oct = degree.floorDiv(7)
        return key + 12 * oct + steps[degree.mod(7)]
    }

    private fun nearest(candidates: List<Int>, target: Int): Int {
        val all = candidates.flatMap { c -> (-2..2).map { c + 12 * it } }
        return all.minBy { kotlin.math.abs(it - target) }
    }

    fun frequency(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
}
