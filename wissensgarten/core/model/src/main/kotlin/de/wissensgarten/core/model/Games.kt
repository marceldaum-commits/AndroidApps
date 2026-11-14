package de.wissensgarten.core.model

import kotlin.random.Random

/** Wie eine Karte in der Gieß-Runde gespielt wird. */
enum class GameMode {
    /** Neue Karte: Merkbild + Antwort entdecken, dann kurz prüfen */
    DISCOVER,
    /** Antworten steigen als Ballons auf */
    BALLOONS,
    /** Antwort aus Buchstaben-Kacheln legen */
    LETTERS,
    /** Behauptung: stimmt / stimmt nicht wischen */
    SWIPE,
    /** Klassische Auswahl */
    CHOICE,
    /** Aufdecken + Selbsteinschätzung (eigene Karten ohne Auswahl) */
    FLIP,
}

/** Ergebnis eines Mini-Spiels – wird in eine Bewertung für die Vergessenskurve übersetzt. */
enum class GameResult(val rating: Rating) { PERFECT(Rating.GOOD), SHAKY(Rating.HARD), WRONG(Rating.AGAIN) }

object Games {
    private val LETTERS_OK = Regex("^[A-Za-zÄÖÜäöüßÀ-ÿ]{3,12}$")

    /** Antwortmöglichkeiten: eigene Ablenker der Karte, sonst Antworten anderer Karten aus dem Beet. */
    fun options(card: StudyCard, peers: List<String>): List<String>? {
        val given = card.choices.orEmpty().filter { it != card.back }
        val extra = peers.filter { it != card.back && it !in given }.distinct()
            .map { it to (it.hashCode() xor card.id.hashCode()) }.sortedBy { it.second }.map { it.first }
        val wrong = (given + extra).take(3)
        if (wrong.size < 2) return null
        val seed = card.id.hashCode() + card.memory.reps
        return (wrong + card.back).shuffled(Random(seed))
    }

    fun lettersPossible(answer: String, simple: Boolean): Boolean =
        LETTERS_OK.matches(answer) && (!simple || answer.length <= 8)

    fun balloonsPossible(options: List<String>) = options.all { it.length <= 22 }

    /**
     * Wählt das Spiel. Abwechslung entsteht über Karten-ID + Anzahl Wiederholungen,
     * damit dieselbe Karte beim nächsten Mal anders gespielt wird.
     */
    fun choose(card: StudyCard, peers: List<String>, simple: Boolean): GameMode {
        val opts = options(card, peers)
        if (card.memory.isSeed) return GameMode.DISCOVER
        if (opts == null) return GameMode.FLIP
        val pool = buildList {
            add(GameMode.CHOICE)
            add(GameMode.SWIPE)
            if (balloonsPossible(opts)) { add(GameMode.BALLOONS); add(GameMode.BALLOONS) }
            if (lettersPossible(card.back, simple)) { add(GameMode.LETTERS); add(GameMode.LETTERS) }
        }
        val pick = Math.floorMod(card.id.hashCode() * 31 + card.memory.reps * 7, pool.size)
        return pool[pick]
    }

    /** Buchstaben-Kacheln: Antwort gemischt (nie in richtiger Reihenfolge). */
    fun letterTiles(answer: String, seed: Int): List<Char> {
        val chars = answer.uppercase().toList()
        val rnd = Random(seed)
        repeat(10) {
            val s = chars.shuffled(rnd)
            if (s != chars) return s
        }
        return chars.reversed()
    }

    /** „Stimmt's?“: zeigt mit 50 % die richtige, sonst eine falsche Antwort. */
    fun swipeStatement(card: StudyCard, options: List<String>): Pair<String, Boolean> {
        val rnd = Random(card.id.hashCode() * 13 + card.memory.reps)
        val truth = rnd.nextBoolean()
        return if (truth) card.back to true else options.filter { it != card.back }.random(rnd) to false
    }

    /** Karten für eine Zuordnen-Bonusrunde (kurze Fragen und Antworten, alles verschieden). */
    fun matchPairs(cards: List<StudyCard>, count: Int = 4): List<StudyCard>? {
        val ok = cards.filter { it.front.length <= 28 && it.back.length <= 22 }
            .distinctBy { it.back }.distinctBy { it.front }
        return if (ok.size >= count) ok.take(count) else null
    }
}
