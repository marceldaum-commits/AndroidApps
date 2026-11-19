package de.wissensgarten.app.ui

import de.wissensgarten.app.ui.common.ParentGate
import de.wissensgarten.app.ui.onboarding.recommendedPacks
import de.wissensgarten.core.model.PackJson
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ParentGateTest {
    @Test fun tasksAreForAdultsAndWrittenInWords() {
        val r = Random(1)
        repeat(200) {
            val t = ParentGate.task(r)
            assertTrue("zu leicht: ${t.a}×${t.b}", t.answer >= 78)
            assertTrue(t.answer.toString().length <= ParentGate.MAX_DIGITS)
            assertFalse("Ziffern in „${t.words}“", t.words.any { it.isDigit() })
            assertTrue(t.words.contains(" mal "))
        }
        assertEquals("siebzehn mal acht", ParentGate.Task(17, 8).words)
    }

    @Test fun gateStaysOpenTenMinutes() {
        ParentGate.close()
        assertFalse(ParentGate.isOpen(1000))
        ParentGate.open(1000)
        assertTrue(ParentGate.isOpen(1000 + 9 * 60_000))
        assertFalse(ParentGate.isOpen(1000 + 11 * 60_000))
        ParentGate.close()
    }

    @Test fun quickStartPicksTwoFittingPacksFromDifferentSubjects() {
        val packs = File("../content/src/main/assets/packs").listFiles()!!.filter { it.extension == "json" }.map { PackJson.parse(it.readText()) }
        for (level in 1..13) {
            val ids = recommendedPacks(packs, level)
            assertTrue("Klasse $level: ${ids.size}", ids.size in 1..2)
            val chosen = packs.filter { it.id in ids }
            assertTrue(chosen.all { level in it.grades })
            assertEquals(chosen.size, chosen.map { it.subject }.toSet().size)
        }
    }
}
