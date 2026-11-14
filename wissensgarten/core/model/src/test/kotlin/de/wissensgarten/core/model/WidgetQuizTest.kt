package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class WidgetQuizTest {
    private fun card(i: Int, seed: Boolean = false) = StudyCard("c$i", "b", "Frage $i", "Antwort $i", null, null,
        if (seed) Memory() else Memory(stability = 1.0, difficulty = 5.0, lastReview = 1, reps = 1, lapses = 0), null)

    @Test fun twoOptionsOneCorrect() {
        repeat(30) { s ->
            val q = WidgetQuiz.pick((1..5).map { card(it) }, 10L * 86_400_000, Random(s))!!
            assertEquals(2, q.options.size)
            assertEquals(q.card.back, q.options[q.correct])
            assertNotEquals(q.options[0], q.options[1])
        }
    }

    @Test fun seedsAreSkipped() = assertNull(WidgetQuiz.pick(listOf(card(1, true), card(2, true)), 0L))
}
