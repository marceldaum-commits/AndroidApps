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
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("apple", "the apple"))
        assertEquals("Groß/klein zählt außer in Rechtschreibung nicht", TypedAnswer.Verdict.RIGHT, TypedAnswer.check("paris", "Paris"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Rubisco", "RuBisCO"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Blauwal", "Der Blauwal"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Kiew", "Kyjiw (Kiew)"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Kyjiw", "Kyjiw (Kiew)"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Reykjavik", "Reykjavík"))
        assertEquals("Umlaut zählt", TypedAnswer.Verdict.WRONG, TypedAnswer.check("Bar", "Bär"))
        assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Gewaltenteilung", "Gewaltenteilung."))
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

    @Test fun choiceCardsWithPhraseAnswersAreNotTyped() {
        val tree = Memory(90.0, 5.0, 0, 8, 0)
        val phrase = StudyCard("w", "b", "Wann verdoppelt sich das Geld bei 6 %?", "Nach etwa 12 Jahren", listOf("Nach etwa 6 Jahren", "Nach etwa 20 Jahren"), null, tree)
        assertFalse(GameMode.TYPE in (1..40).map { Games.choose(phrase.copy(id = "w$it"), peers, false, canType = true) })
        val word = StudyCard("v", "b", "Hauptstadt Italiens?", "Rom", listOf("Paris", "Madrid"), null, tree)
        assertTrue(GameMode.TYPE in (1..40).map { Games.choose(word.copy(id = "v$it"), peers, false, canType = true) })
    }

    @Test fun formulaParenthesesAreNotAlternatives() {
        org.junit.Assert.assertEquals(listOf("cos(x)"), TypedAnswer.alternatives("cos(x)"))
        org.junit.Assert.assertEquals(TypedAnswer.Verdict.WRONG, TypedAnswer.check("x", "cos(x)"))
        org.junit.Assert.assertEquals(listOf("Kyjiw (Kiew)", "Kyjiw", "Kiew"), TypedAnswer.alternatives("Kyjiw (Kiew)"))
    }

    private val R = TypedAnswer.Verdict.RIGHT
    private val W = TypedAnswer.Verdict.WRONG

    @Test fun numberWordsCountLikeDigits() {
        assertEquals(R, TypedAnswer.check("vier", "4", question = "Wie viele Fälle gibt es im Deutschen?"))
        assertEquals(R, TypedAnswer.check("Acht", "8"))
        assertEquals(R, TypedAnswer.check("eight", "8"))
        assertEquals(R, TypedAnswer.check("einundzwanzig", "21"))
        assertEquals(R, TypedAnswer.check("twenty-one", "21"))
        assertEquals(R, TypedAnswer.check("zwanzig", "20"))
        assertEquals(R, TypedAnswer.check("null", "0 °C"))
        assertEquals(R, TypedAnswer.check("acht Tage", "8 Tage"))
        assertEquals(R, TypedAnswer.check("tausend", "1 000"))
        assertEquals(W, TypedAnswer.check("sieben", "8"))
        // steht das Zahlwort schon in der Frage, ist es keine Antwort
        assertEquals(W, TypedAnswer.check("six", "6", question = "Welche Zahl ist „six“?"))
        // umgekehrt nicht: „eight“ ist das gesuchte Wort, „8“ reicht nicht
        assertEquals(W, TypedAnswer.check("8", "eight", question = "Was heißt „acht“ auf Englisch?"))
    }

    @Test fun fractionsDecimalsAndPercentAreTheSameValue() {
        assertEquals(R, TypedAnswer.check("0,5", "1/2"))
        assertEquals(R, TypedAnswer.check("0.5", "1/2"))
        assertEquals(R, TypedAnswer.check("½", "1/2"))
        assertEquals(R, TypedAnswer.check("50 %", "1/2"))
        assertEquals(R, TypedAnswer.check("1/2", "0,5 (1/2)"))
        assertEquals(R, TypedAnswer.check("0,75", "3/4", question = "1/2 + 1/4 = ?"))
        assertEquals(R, TypedAnswer.check("0,25", "25 %", question = "Wie wahrscheinlich ist das?"))
        assertEquals(W, TypedAnswer.check("1/3", "0,333"))
        // verlangt die Frage eine Schreibweise, zählt nur diese
        assertEquals(W, TypedAnswer.check("0,75", "3/4", question = "0,75 als vollständig gekürzter Bruch?"))
        assertEquals(W, TypedAnswer.check("4/6", "2/3", question = "Kürze 12/18 vollständig."))
        assertEquals(R, TypedAnswer.check("2/3", "2/3", question = "Kürze 12/18 vollständig."))
        assertEquals(W, TypedAnswer.check("17/5", "3 2/5", question = "Schreibe 17/5 als gemischte Zahl."))
        assertEquals(W, TypedAnswer.check("1/8", "0,125", question = "1/8 als Dezimalzahl?"))
        assertEquals(R, TypedAnswer.check("40", "40 %", question = "2/5 in Prozent?"))
        assertEquals(W, TypedAnswer.check("0,4", "40 %", question = "2/5 in Prozent?"))
    }

    @Test fun unitsMayBeWrittenOut() {
        assertEquals(R, TypedAnswer.check("50 Prozent", "50 %"))
        assertEquals(R, TypedAnswer.check("100 Grad", "100 °C"))
        assertEquals(R, TypedAnswer.check("100 °C", "100 °C"))
        assertEquals(R, TypedAnswer.check("7 °C", "7 Grad"))
        assertEquals(R, TypedAnswer.check("5 Zentimeter", "5 cm"))
        assertEquals(R, TypedAnswer.check("5", "5 cm"))
        assertEquals(R, TypedAnswer.check("2,50 Euro", "2,50 €"))
        assertEquals(R, TypedAnswer.check("100 ct", "100 Cent"))
        assertEquals(R, TypedAnswer.check("20", "20 m/s"))
        assertEquals(W, TypedAnswer.check("5 m", "5 cm"))
        assertEquals(W, TypedAnswer.check("1 km", "1000 m", question = "1 km = ? m"))
    }

    @Test fun listsInAnyOrderUnlessOrderIsAsked() {
        assertEquals(R, TypedAnswer.check("Remus und Romulus", "Romulus und Remus", question = "Wie heißen die Zwillinge der römischen Gründungssage?"))
        assertEquals(R, TypedAnswer.check("Haken, Appel", "Appel und Haken"))
        assertEquals(R, TypedAnswer.check("Blau, Gelb, Rot", "Gelb, Rot, Blau", question = "Welche drei Grundfarben hat der Farbkreis nach Itten?"))
        assertEquals(W, TypedAnswer.check("Vilnius, Riga, Tallinn", "Tallinn, Riga, Vilnius", question = "Hauptstädte der baltischen Staaten (in dieser Reihenfolge)?"))
        assertEquals(W, TypedAnswer.check("Kern, Mantel, Kruste", "Kruste, Mantel, Kern", question = "Wie heißen die drei Schalen der Erde von außen nach innen?"))
        assertEquals(W, TypedAnswer.check("Drang und Sturm", "Sturm und Drang", question = "Epoche von Schillers „Die Räuber“"))
        assertEquals(W, TypedAnswer.check("Romulus", "Romulus und Remus"))
        assertTrue(SpokenAnswer.matches("deimos und phobos", "Phobos und Deimos", "Wie heißen die beiden Monde des Mars?"))
        assertFalse(SpokenAnswer.matches("gold rot schwarz", "Schwarz, Rot, Gold", "Welche Farben hat die deutsche Flagge (von oben nach unten)?"))
    }

    @Test fun alternativesInParenthesesAndOtherEquivalentForms() {
        assertEquals(listOf("Nomen (Namenwort / Substantiv)", "Nomen", "Namenwort", "Substantiv"), TypedAnswer.alternatives("Nomen (Namenwort / Substantiv)"))
        assertEquals(R, TypedAnswer.check("Substantiv", "Nomen (Namenwort / Substantiv)", strictSpelling = true))
        assertEquals(listOf("Δx · Δp ≥ h / (4π)"), TypedAnswer.alternatives("Δx · Δp ≥ h / (4π)"))
        assertEquals(1, TypedAnswer.alternatives("Münster (mit Frankreich) und Osnabrück (mit Schweden)").size)
        assertEquals(R, TypedAnswer.check("I am full", "I'm full."))
        assertEquals(R, TypedAnswer.check("did not see", "didn't see"))
        assertEquals(R, TypedAnswer.check("read", "to read"))
        assertEquals(R, TypedAnswer.check("Schweiz", "In der Schweiz"))
        assertEquals(W, TypedAnswer.check("Aus der Schweiz", "In der Schweiz"))
        assertEquals(R, TypedAnswer.check("CO2", "CO₂"))
        assertTrue(SpokenAnswer.matches("rufen", "rufen (schreien)"))
        assertTrue(SpokenAnswer.matches("schreien", "rufen (schreien)"))
        assertTrue(SpokenAnswer.matches("did not see", "didn't see"))
        assertTrue(SpokenAnswer.matches("didn t see", "didn't see"))
        assertTrue(SpokenAnswer.matches("halb 8", "halb acht"))
        assertTrue(SpokenAnswer.matches("schweiz", "In der Schweiz"))
        assertTrue(SpokenAnswer.matches("einstein", "Albert Einstein", "Wer entwickelte die Relativitätstheorie?"))
        assertTrue(SpokenAnswer.matches("da vinci", "Leonardo da Vinci", "Wer malte die „Mona Lisa“?"))
        assertFalse(SpokenAnswer.matches("albert", "Albert Einstein", "Wer entwickelte die Relativitätstheorie?"))
        assertFalse(SpokenAnswer.matches("einstein", "Albert Einstein", "Wer entwickelte die Relativitätstheorie? (Vor- und Nachname)"))
    }

    @Test fun ambiguousChoiceQuestionsAreNotAskedFreely() {
        val tree = Memory(90.0, 5.0, 0, 8, 0)
        val c = StudyCard("n", "b", "Welches Wort ist ein Nomen?", "Hund", listOf("laufen", "rot", "schnell"), null, tree)
        val modes = (1..40).map { Games.choose(c.copy(id = "n$it"), peers, false, canSpeak = true, canType = true) }.toSet()
        assertFalse(GameMode.TYPE in modes)
        assertFalse(GameMode.SPEAK in modes)
    }

    @Test fun germanAcceptsAlternativesButStaysStrict() {
        org.junit.Assert.assertEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Voraussage", "Vorhersage (Voraussage)", strictSpelling = true))
        org.junit.Assert.assertNotEquals(TypedAnswer.Verdict.RIGHT, TypedAnswer.check("Vorrausage", "Vorhersage (Voraussage)", strictSpelling = true))
    }
}
