package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

class PlanTest {
    private val chapters = Levels.chapters
    private fun ch(n: Int) = chapters.first { it.number == n }

    @Test fun gradeRangeAndSubjectAreReadFromTheChapter() {
        assertEquals(5..6, ch(1).gradeRange)
        assertEquals(8..8, ch(7).gradeRange)
        assertEquals(7..10, ch(4).gradeRange)
        assertEquals(Subject.MATH, ch(2).subject)
        assertEquals(Subject.PHYSICS, ch(11).subject)
    }

    @Test fun everyChapterLandsInExactlyOneSectionForEveryGrade() {
        for (g in Plan.grades) {
            val s = Plan.sections(chapters, g)
            assertEquals("Klasse $g", chapters.map { it.number }.sorted(), s.flatMap { it.chapters }.map { it.number }.sorted())
            assertTrue(s.first().title.startsWith("Deine Klasse"))
        }
        val seven = Plan.sections(chapters, 7)
        assertTrue(ch(2) in seven[0].chapters)                                   // Geraden 7–8
        assertTrue(ch(1) in seven.first { it.title == "Zum Wiederholen" }.chapters) // Koordinaten 5–6
        assertTrue(ch(14) in seven.first { it.title == "Für später" }.chapters)   // Trigonometrie 9–10
    }

    @Test fun withoutGradeAllChaptersInOrderAndFilterBySubject() {
        assertEquals(chapters, Plan.sections(chapters, null).single().chapters)
        assertTrue(Plan.sections(chapters, null, Subject.PHYSICS).single().chapters.all { it.subject == Subject.PHYSICS })
        assertTrue(Plan.sections(chapters, 6, Subject.MATH).flatMap { it.chapters }.none { it.subject == Subject.PHYSICS })
    }

    @Test fun continueWhereYouLeftOff() {
        // nichts gespielt, Klasse 9 → erstes Level eines Klasse-9-Kapitels
        val first = Plan.next(chapters, 9, emptyMap())!!
        assertTrue(9 in chapters.first { it.number == first.chapter }.gradeRange)
        assertEquals(1, first.number)
        // zuletzt in Parabeln gespielt → dort weiter, auch wenn anderes älter ist
        val p = mapOf("1-1" to Played(3, 100), "3-1" to Played(2, 200))
        assertEquals("3-2", Plan.next(chapters, 5, p)!!.id)
        // Kapitel fertig → weiter in der eigenen Klasse
        val done = ch(3).levels.associate { it.id to Played(3, 300) }
        assertEquals(9, chapters.first { it.number == Plan.next(chapters, 9, done)!!.chapter }.gradeRange.let { if (9 in it) 9 else -1 })
        // alles geschafft → nichts mehr
        assertNull(Plan.next(chapters, 7, Levels.all.associate { it.id to Played(3, 1) }))
    }
}
