package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class GamesTest {
    private fun card(id: String, q: String, a: String, choices: List<String>? = null, reps: Int = 2) =
        StudyCard(id, "b", q, a, choices, null, if (reps == 0) Memory() else Memory(3.0, 5.0, 0, reps, 0))

    private val peers = listOf("Paris", "Rom", "Madrid", "Berlin", "Wien")

    @Test fun seedsAreDiscovered() {
        assertEquals(GameMode.DISCOVER, Games.choose(card("1", "Frankreich", "Paris", reps = 0), peers, false))
    }

    @Test fun ownCardsWithoutOptionsAreFlipped() {
        assertEquals(GameMode.FLIP, Games.choose(card("1", "Mein Satz", "eine lange eigene Antwort"), emptyList(), false))
    }

    @Test fun optionsContainAnswerOnceWithPeerDistractors() {
        val o = Games.options(card("1", "Italien", "Rom"), peers)!!
        assertEquals(4, o.size)
        assertEquals(1, o.count { it == "Rom" })
    }

    @Test fun modesVaryAcrossCardsAndRepetitions() {
        val modes = (1..40).map { Games.choose(card("k$it", "Land $it", "Paris", reps = it % 5 + 1), peers, false) }.toSet()
        assertTrue("zu wenig Abwechslung: $modes", modes.size >= 3)
    }

    @Test fun lettersOnlyForShortSingleWords() {
        assertTrue(Games.lettersPossible("Paris", false))
        assertFalse(Games.lettersPossible("went – gone", false))
        assertFalse(Games.lettersPossible("Kopenhagen", true)) // Grundschule: max. 8 Buchstaben
        assertFalse(Games.lettersPossible("H2O", false))
    }

    @Test fun tilesAreShuffledButComplete() {
        val t = Games.letterTiles("Paris", 3)
        assertEquals("PARIS".toList().sorted(), t.sorted())
        assertNotEquals("PARIS".toList(), t)
    }

    @Test fun swipeShowsTrueOrFalseStatement() {
        val c = card("1", "Italien", "Rom")
        val o = Games.options(c, peers)!!
        val (shown, truth) = Games.swipeStatement(c, o)
        assertEquals(truth, shown == "Rom")
    }

    @Test fun matchNeedsFourDistinctShortCards() {
        val cs = (1..5).map { card("$it", "F$it", "A$it") }
        assertEquals(4, Games.matchPairs(cs)!!.size)
        assertNull(Games.matchPairs(cs.take(3)))
    }
}
