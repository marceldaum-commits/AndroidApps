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
        assertEquals("Paris".toList().sorted(), t.sorted())
        assertNotEquals("Paris".toList(), t)
    }

    @Test fun swipeShowsTrueOrFalseStatement() {
        val c = card("1", "Italien", "Rom")
        val o = Games.options(c, peers)!!
        val (shown, truth) = Games.swipeStatement(c, o)
        assertEquals(truth, shown == "Rom")
    }

    @Test fun calmModeOnlyQuietGames() {
        val modes = (1..30).map { Games.choose(card("c$it", "Land", "Paris", reps = 2), peers, false, calm = true) }.toSet()
        assertEquals(setOf(GameMode.CHOICE, GameMode.TEACH), modes)
    }

    @Test fun speakOnlyWhenAvailableAndShort() {
        val c = card("x", "Italien", "Rom")
        val withSpeech = (1..60).map { Games.choose(c.copy(id = "s$it"), peers, false, canSpeak = true) }
        val without = (1..60).map { Games.choose(c.copy(id = "s$it"), peers, false, canSpeak = false) }
        assertTrue(GameMode.SPEAK in withSpeech)
        assertFalse(GameMode.SPEAK in without)
        assertFalse(Games.speakPossible("A = π · r²"))
    }

    @Test fun spokenAnswersAreForgiving() {
        assertTrue(SpokenAnswer.matches("the apple", "apple"))
        assertTrue(SpokenAnswer.matches("Kopenhagn", "Kopenhagen"))
        assertTrue(SpokenAnswer.matches("la lune", "la lune"))
        assertFalse(SpokenAnswer.matches("Paris", "Rom"))
        assertFalse(SpokenAnswer.matches("", "Rom"))
    }

    @Test fun matchNeedsFourDistinctShortCards() {
        val cs = (1..5).map { card("$it", "F$it", "A$it") }
        assertEquals(4, Games.matchPairs(cs)!!.size)
        assertNull(Games.matchPairs(cs.take(3)))
    }

    @Test fun articlesAndCaseAreNotLeftToSpeech() {
        assertFalse(Games.speakPossible("die"))
        assertFalse(Games.speakPossible("la maison"))
        assertFalse(Games.speakPossible("der Hund"))
        assertTrue(Games.speakPossible("Hund"))
        assertEquals(setOf('V', 'o', 'g', 'e', 'l'), Games.letterTiles("Vogel", 1).toSet())
    }

    @Test fun typedAnswersForgiveSmallThingsButNotSpelling() {
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("  went  ", "went"))
        assertEquals(TypedAnswer.Verdict.ALMOST, TypedAnswer.check("Kopenhagn", "Kopenhagen"))
        assertEquals(TypedAnswer.Verdict.ALMOST, TypedAnswer.check("apple", "the apple"))
        assertEquals(TypedAnswer.Verdict.ALMOST, TypedAnswer.check("paris", "Paris"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("Rom", "Paris"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("", "Paris"))
        // Rechtschreibung: „fast“ reicht nicht
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("vogel", "Vogel", strictSpelling = true))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Vogel", "Vogel", strictSpelling = true))
        // kurze Antworten: kein Tippfehler-Bonus (sonst wäre „ist“ = „isst“)
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("ist", "isst"))
    }

    @Test fun typingOnlyWhenAllowedAndTypeable() {
        assertFalse(typePossible("A = π · r²"))
        assertTrue(typePossible("went – gone"))
        assertTrue(typePossible("3,50 €"))
        val c = card("x", "Italien", "Rom", reps = 3)
        val with = (1..60).map { Games.choose(c.copy(id = "t$it"), peers, false, canType = true) }
        val without = (1..60).map { Games.choose(c.copy(id = "t$it"), peers, false, canType = false) }
        assertTrue(GameMode.TYPE in with)
        assertFalse(GameMode.TYPE in without)
        // Keimling (leichteste Stufe): noch keine freie Eingabe
        val sprout = StudyCard("n", "b", "Italien", "Rom", null, null, Memory(1.0, 5.0, 0, 1, 0))
        assertFalse(GameMode.TYPE in (1..60).map { Games.choose(sprout.copy(id = "n$it"), peers, false, canType = true) })
    }

    @Test fun higherStagesGetMoreSimilarDistractorsAndMoreOptions() {
        val pool = listOf("Rom", "Bonn", "Born", "Bern", "Oslo", "Lima", "Kairo")
        fun opts(stab: Double) = Games.options(StudyCard("x", "b", "Hauptstadt der Schweiz?", "Berns", null, null, Memory(stab, 5.0, 0, 3, 0)), pool)!!
        val easy = opts(1.0); val hard = opts(90.0)
        assertEquals(3, easy.size); assertEquals(4, hard.size)
        val simEasy = easy.filter { it != "Berns" }.maxOf { Difficulty.similarity(it, "Berns") }
        val simHard = hard.filter { it != "Berns" }.minOf { Difficulty.similarity(it, "Berns") }
        assertTrue("schwer muss ähnlicher sein: $easy vs $hard", simHard > simEasy)
    }

    @Test fun profiStageMeansFreeRecall() {
        val tree = StudyCard("p", "b", "Italien", "Rom", null, null, Memory(90.0, 5.0, 0, 8, 0))
        val modes = (1..40).map { Games.choose(tree.copy(id = "p$it"), peers, false, canType = true) }.toSet()
        assertEquals(setOf(GameMode.TYPE), modes)
    }

    @Test fun numbersAreComparedByValueWithoutTypoBonus() {
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("0,5", "0,50"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("0,2462", "0,2461"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("5", "-5"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("1250", "1.250"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("1.5", "1,5"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Marshallplan", "Marshall-Plan"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("Marshallplan", "Marshall-Plan", strictSpelling = true))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("25", "25 %"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("25 €", "25 %"))
        assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("Paris", "12"))
    }
}
