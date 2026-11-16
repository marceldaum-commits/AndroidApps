package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class UnderstandTest {
    @Test fun liveCalcShowsHitForTheSolution() {
        var n = 0
        for (l in Levels.all) {
            val line = Understand.live(l, l.solution) ?: continue
            n++
            if (line.last() in "✓↑↓↕↔") assertTrue("${l.id}: Lösung muss ✓ zeigen: $line", line.endsWith("✓"))
        }
        println("Live-Rechnung in $n von ${Levels.all.size} Leveln")
        assertTrue(n > Levels.all.size / 2)
    }

    @Test fun liveCalcPointsTheRightWayForStartValues() {
        for (l in Levels.all) {
            val line = Understand.live(l, l.start) ?: continue
            assertTrue("${l.id}: $line", line.isNotBlank())
        }
    }

    @Test fun checksHaveThreeDifferentOptions() {
        var n = 0
        for (l in Levels.all) {
            val c = Understand.check(l) ?: continue
            n++
            assertEquals(l.id, 3, c.options.distinct().size)
            assertTrue(c.correct in 0..2)
            assertEquals("gleich bei jedem Öffnen", c, Understand.check(l))
        }
        println("Rechen-Frage in $n von ${Levels.all.size} Leveln")
        assertTrue(n > Levels.all.size / 2)
    }

    @Test fun trigExampleFromMarcel() {
        val l = Levels.all.first { it.kind == Kind.LASER && it.triangle != null && it.mirrors.isEmpty() }
        val c = Understand.check(l)!!
        assertTrue(c.options[c.correct], c.options[c.correct].startsWith("tan α ="))
        println(l.id + ": " + Understand.live(l, l.solution) + " / " + c)
    }
}

class UnderstandCoverage {
    @Test fun printMissing() {
        Levels.all.filter { Understand.check(it) == null || Understand.live(it, it.solution) == null }.forEach {
            println("FEHLT ${it.id} ${it.kind} params=${it.params.map { p -> p.key }} live=${Understand.live(it, it.solution) != null} check=${Understand.check(it) != null} tri=${it.triangle != null} mirrors=${it.mirrors.size} ${it.title}")
        }
    }
}

/** Keine Rechen-Frage darf zwei gleichwertige Antworten haben (z. B. „4 − 0 · 6“ und „4 + 0 · 6“). */
class UnderstandUnambiguous {
    @Test fun noTwoOptionsWithTheSameValue() {
        val levels = Levels.all + (20_000L..20_400L).map { Daily.forDay(it) } + (1..10).flatMap { Andromeda.chapter(it).levels }
        for (l in levels) {
            val c = Understand.check(l) ?: continue
            for (o in c.options) {
                assertFalse("${l.id}: ${c.options}", Regex("""· \(?0\)?( |$)|(^|[ (])0 ·|[−+] 0\)²""").containsMatchIn(o))
            }
        }
    }
}
