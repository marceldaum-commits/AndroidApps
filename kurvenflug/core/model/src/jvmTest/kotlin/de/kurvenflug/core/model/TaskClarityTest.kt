package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Prüfung „Aufgaben verständlich“ (Auftrag 17.11.): über alle festen Level und die Zufallsgeneratoren mit vielen Seeds.
 * Fängt Formfehler ab, die beim Lesen stören („y = 0x − 6“, „+ 0“, „Wellen-berg“) und Aufgaben ohne erkennbare Frage.
 */
class TaskClarityTest {
    private val all: List<Level> by lazy {
        ReadTexts.fixedLevels() + Gen.kinds.flatMap { k -> (0..Tier.MAX).flatMap { t -> (1L..60L).map { Gen.level(k, it * 977 + t, t) } } } +
            (0..400).map { Daily.forDay(20_000L + it) } + SeasonEvent.entries.flatMap { Seasons.levels(it, 2026) }
    }

    @Test fun noOddFormatting() {
        val odd = Regex("""(^|[^0-9,])0x|[+−] 0(?![,0-9])|Wellen-|\+ −|− −""")
        val bad = all.filter { odd.containsMatchIn(it.task.substringBefore(" 🔥 Extrem:")) }.map { "${it.id}: ${it.task}" }.distinct()
        assertTrue("Unschöne Schreibweise:\n" + bad.take(20).joinToString("\n"), bad.isEmpty())
    }

    @Test fun lineText() {
        assertEquals("y = −6", linText(0.0, -6.0)); assertEquals("y = 2,25x", linText(2.25, 0.0)); assertEquals("y = −x + 3", linText(-1.0, 3.0))
        assertEquals("y = x − 4,5", linText(1.0, -4.5)); assertEquals("y = 0", linText(0.0, 0.0))
    }

    /** Jede Aufgabe sagt, was zu tun ist: eine Frage, eine Aufforderung oder ein Ziel, das man einstellt. */
    @Test fun everyTaskAsksSomething() {
        val asks = Regex("""\?|!|Stell|stell|Flieg|flieg|Finde|Wähl|Bring|Lies|Feuere|Halte|Nutz|Lass|Schwing|Schieb|Verschieb|Zieh|treff|Ziel|Station|Durch|durch|Landeplätze|landen""")
        val bad = all.filter { !asks.containsMatchIn(it.task) }.map { "${it.id}: ${it.task}" }.distinct()
        assertTrue("Ohne erkennbare Frage:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
