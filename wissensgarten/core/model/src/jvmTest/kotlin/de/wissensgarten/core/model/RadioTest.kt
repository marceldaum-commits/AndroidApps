package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class RadioTest {
    private val known = Memory(3.0, 5.0, 1, 2, 0)
    private fun card(q: String, a: String, choices: List<String>? = null, m: Memory = known) = StudyCard("c-$a", "b", q, a, choices, null, m, null)

    @Test fun picksModeByAnswer() {
        assertEquals(Radio.Mode.LEARN, Radio.question(card("dog?", "Hund", m = Memory()), emptyList()).mode)
        assertEquals(Radio.Mode.FREE, Radio.question(card("dog?", "Hund"), emptyList()).mode)
        val choice = Radio.question(card("1/2 + 1/4?", "3/4", listOf("2/6", "1/8", "2/4")), emptyList())
        assertEquals(Radio.Mode.CHOICE, choice.mode)
        assertTrue("3/4" in choice.options)
        assertEquals(Radio.Mode.SELF, Radio.question(card("Was ist Photosynthese?", "Pflanzen machen aus Licht, Wasser und CO2 Zucker"), emptyList()).mode)
    }

    @Test fun understandsFreeAnswersWithArticlesAndSmallErrors() {
        val q = Radio.question(card("dog?", "Hund"), emptyList())
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("der Hund"), q))
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("Hunt", "Hund"), q))
        assertTrue(Radio.interpret(listOf("Katze"), q) is Radio.Reply.Wrong)
    }

    @Test fun understandsLettersAndOptionText() {
        val q = Radio.Question(card("1/2 + 1/4?", "3/4"), Radio.Mode.CHOICE, listOf("2/6", "3/4", "1/8"))
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("B"), q))
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("Antwort be"), q))
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("die zweite"), q))
        assertTrue(Radio.interpret(listOf("a"), q) is Radio.Reply.Wrong)
        assertEquals(Radio.Reply.Unclear, Radio.interpret(listOf("hmm"), q))
    }

    @Test fun controlWordsWin() {
        val q = Radio.question(card("dog?", "Hund"), emptyList())
        assertEquals(Radio.Reply.Stop, Radio.interpret(listOf("stopp"), q))
        assertEquals(Radio.Reply.Repeat, Radio.interpret(listOf("nochmal bitte"), q))
        assertEquals(Radio.Reply.DontKnow, Radio.interpret(listOf("weiß nicht"), q))
        assertEquals(Radio.Reply.Unclear, Radio.interpret(emptyList(), q))
    }

    @Test fun selfRatingYesNo() {
        val q = Radio.Question(card("Erkläre X", "lange Antwort mit vielen Wörtern"), Radio.Mode.SELF)
        assertEquals(Radio.Reply.Yes, Radio.interpret(listOf("ja klar"), q))
        assertEquals(Radio.Reply.No, Radio.interpret(listOf("nö"), q))
        assertEquals(Rating.GOOD, Radio.rating(Radio.Reply.Yes, q.mode))
        assertEquals(Rating.AGAIN, Radio.rating(Radio.Reply.No, q.mode))
    }

    @Test fun promptReadsOptions() {
        val q = Radio.Question(card("Hauptstadt?", "Paris"), Radio.Mode.CHOICE, listOf("Rom", "Paris"))
        assertEquals("Hauptstadt? A: Rom. B: Paris.", Radio.prompt(q))
    }

    @Test fun answerThatIsAControlWordStillCounts() {
        val q = Radio.question(card("Gegenteil von Anfang?", "Ende"), emptyList())
        assertEquals(Radio.Reply.Correct, Radio.interpret(listOf("Ende"), q))
        assertEquals(Radio.Reply.Stop, Radio.interpret(listOf("stopp"), q))
    }
}
