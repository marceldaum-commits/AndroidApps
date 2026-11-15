package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class TogetherTest {
    private val today = 20_500L * Weekly.DAY
    private val now = today + 10 * 3_600_000L

    @Test fun weekSummaryCountsDaysMinutesAndStuckTopics() {
        val plays = listOf(
            PlayRecord("2-1", 3, 1, 120, today - 2 * Weekly.DAY),
            PlayRecord("2-2", 3, 1, 100, today - 2 * Weekly.DAY),
            PlayRecord("2-3", 3, 1, 5_000, today),                       // Handy lag offen → höchstens 15 min
            PlayRecord("3-1", 0, 3, 300, today - Weekly.DAY),             // hängen geblieben …
            PlayRecord("3-2", 0, 4, 200, today),                          // … hier auch, nicht geschafft
            PlayRecord("3-1", 2, 2, 60, today),                           // 3-1 später geschafft
            PlayRecord("1-1", 3, 1, 60, today - 20 * Weekly.DAY),         // zu alt
        )
        val w = Weekly.summary(plays, now, today)
        assertEquals(7, w.days.size)
        assertEquals(2, w.days[4].first)                                 // vorgestern 2 geschafft
        assertEquals(3, w.activeDays)
        assertEquals((120 + 100 + 900 + 300 + 200 + 60) / 60, w.minutes)
        assertEquals(4, w.solved)
        assertEquals(listOf(3), w.needsHelp.map { it.number })          // 3-2 hängt noch
        assertEquals(listOf(2), w.strong.map { it.number })
        assertEquals(2, w.topics.first().first.number)
    }

    @Test fun challengeResultsAreParsedCheckedAndRanked() {
        fun r(name: String, attempts: List<Int>) = ChallengeResult("Klasse 7b", name, attempts, "15.11.2026 10:00",
            ChallengeResult.checkOf("Klasse 7b", name, attempts, "15.11.2026 10:00"))
        val chat = listOf(r("Mia", listOf(1, 1, 2)), r("Ben", listOf(1, 1, 1)), r("Ali", listOf(1, 2, 1)), r("Mia", listOf(1, 1, 2)))
            .joinToString("\n") { "Mein Ergebnis: " + Codec.encodeResult(it) + " 🚀" }
        val parsed = Codec.decodeResults(chat)
        assertEquals(4, parsed.size)
        assertTrue(parsed.all { it.valid })
        // Ben: 9 ⭐ – Ali und Mia je 8 ⭐ mit gleich vielen Versuchen
        assertEquals("Ben", ChallengeResult.ranking(parsed).first().name)
        assertEquals(setOf("Ali", "Mia"), ChallengeResult.ranking(parsed).drop(1).map { it.name }.toSet())
        assertEquals(3, ChallengeResult.ranking(parsed).size)            // Mia doppelt eingeschickt
        val forged = parsed.first().copy(name = "Tom", attempts = listOf(1, 1, 1))
        assertFalse(forged.valid)                                          // geändert → Prüfcode passt nicht
        assertEquals(forged, ChallengeResult.ranking(parsed + forged).last()) // und landet ganz unten
    }
}
