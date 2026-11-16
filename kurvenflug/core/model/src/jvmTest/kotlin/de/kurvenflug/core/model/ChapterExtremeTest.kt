package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Nachweis: Die Extrem-Prüfung jedes Kapitels ist messbar schwerer als jedes Level des Kapitels. */
class ChapterExtremeTest {
    @Test fun extremeIsHarderThanEveryLevelOfItsChapter() {
        val out = StringBuilder("| Kapitel | schwerstes Level (Gesamtwert) | Extrem-Prüfung | Zielzone | Hindernisse | Lösungsbereich Vorlage → Extrem |\n|---|---|---|---|---|---|\n")
        for (c in Levels.chapters) {
            val x = ChapterExtreme.forChapter(c.number)
            assertNotNull("Kapitel ${c.number}: keine Extrem-Prüfung", x); x!!
            assertEquals(x, ChapterExtreme.levelFor(x.id))
            assertTrue("${c.number}: nicht lösbar", Flight.fly(x, x.solution).success)
            x.params.forEach { p -> assertEquals("${c.number}/${p.key}", x.solution.getValue(p.key), p.snap(x.solution.getValue(p.key)), 1e-9) }
            val levels = c.levels.map { Difficulty.of(it) }
            val m = Difficulty.of(x)
            val hardest = levels.maxBy { it.score }
            assertTrue("${c.number} ${c.title}: Extrem ${m.score} nicht > ${hardest.score}", m.score > hardest.score)
            assertTrue("${c.number}: Lösungsbereich ${m.window} nicht kleiner als bei der Vorlage", m.window < Difficulty.window(ChapterExtreme.base(c)!!))
            assertTrue("${c.number}: Zielzone nicht enger", m.tolerance < levels.minOf { it.tolerance })
            fun f(v: Double, d: Int = 1) = String.format(java.util.Locale.GERMANY, "%.${d}f", v)
            out.appendLine("| ${c.emoji} ${c.title} | ${f(hardest.score)} | **${f(m.score)}** | ${f(levels.minOf { it.tolerance }, 2)} → ${f(m.tolerance, 2)} | ${m.obstacles} | ${f(Difficulty.window(ChapterExtreme.base(c)!!) * 100)} % → ${f(m.window * 100)} % |")
        }
        File("build").mkdirs(); File("build/kapitel-extrem.md").writeText(out.toString())
    }
}
