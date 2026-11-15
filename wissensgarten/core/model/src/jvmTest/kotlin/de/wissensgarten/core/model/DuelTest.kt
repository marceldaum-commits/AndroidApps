package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class DuelTest {
    private val pack = Pack(
        "s1-test", 2, "Test", "mathe", listOf(5), "➗",
        cards = (1..30).map { PackCard("%03d".format(it), "$it + $it", "${it * 2}", if (it % 2 == 0) listOf("${it * 2 + 1}", "${it * 2 - 1}") else null) },
    )

    @Test fun sameSeedSameQuestions() {
        val a = Duels.questions(pack, 42)
        val b = Duels.questions(pack, 42)
        assertEquals(a, b)
        assertEquals(Duels.QUESTIONS, a.size)
        assertNotEquals(a.map { it.cardId }, Duels.questions(pack, 43).map { it.cardId })
    }

    @Test fun everyQuestionHasAnswerExactlyOnceAndEnoughOptions() {
        for (q in Duels.questions(pack, 7)) {
            assertEquals(1, q.options.count { it == q.answer })
            assertEquals(4, q.options.size)
            assertEquals(q.options.size, q.options.toSet().size)
        }
    }

    @Test fun questionsWorkForEveryRealisticPackSize() {
        // viele Seeds mit vielen ähnlich langen Antworten – darf nie abstürzen
        val big = pack.copy(cards = (1..60).map { PackCard("%03d".format(it), "q$it", "Antwort $it") })
        repeat(200) { seed -> assertEquals(Duels.QUESTIONS, Duels.questions(big, seed).size) }
    }

    @Test fun codeRoundTripsAndSurvivesSurroundingText() {
        val d = Duels.new(pack).withResult(DuelResult("Lena", "🦊", 8, 61_000, 980))
        val code = Duels.encode(d)
        assertTrue(code.startsWith("WG1-"))
        assertEquals(d, Duels.decode(code))
        assertEquals(d, Duels.decode("Hey! Schaffst du das?\n$code\nViel Glück 😄"))
    }

    @Test fun shareTextContainsClickableLinkThatDecodes() {
        val me = DuelResult("Lena", "🦊", 8, 61_000, 980)
        val d = Duels.new(pack).withResult(me)
        val text = Duels.shareText(d, "Test", me)
        assertTrue(text.contains("https://www.mathenachhilfe-online.com/duell/#WG1-"))
        assertEquals(d, Duels.decode(text))
        assertEquals(d, Duels.decode(Duels.link(d)))
    }

    @Test fun brokenCodeIsRejected() {
        val code = Duels.encode(Duels.new(pack))
        val i = code.length / 2
        val broken = code.substring(0, i) + (if (code[i] == 'A') 'B' else 'A') + code.substring(i + 1)
        assertNull(Duels.decode(broken))
        assertNull(Duels.decode("kein Code hier"))
    }

    @Test fun replayingReplacesOwnResultAndRankingIsByScore() {
        var d = Duels.new(pack)
        d = d.withResult(DuelResult("Lena", "🦊", 8, 60_000, 900))
        d = d.withResult(DuelResult("Ben", "🐸", 9, 70_000, 1000))
        d = d.withResult(DuelResult("lena", "🦊", 9, 50_000, 1100))
        assertEquals(2, d.results.size)
        assertEquals("lena", d.ranking.first().name)
        assertTrue(d.hasPlayed("BEN"))
    }

    @Test fun pointsRewardSpeed() {
        assertEquals(0, Duels.points(false, 1000))
        assertEquals(150, Duels.points(true, 0))
        assertEquals(100, Duels.points(true, 20_000))
        assertTrue(Duels.points(true, 3000) > Duels.points(true, 9000))
    }
}
