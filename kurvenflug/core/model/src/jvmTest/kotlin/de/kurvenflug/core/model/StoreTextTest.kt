package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Die Store-Beschreibung für die Zeit mit Pro nennt genau das, was Pro freischaltet (Pro.kt) – und nicht mehr „keine Käufe“. */
class StoreTextTest {
    private val withPro = File("../../store/de-DE/beschreibung-mit-pro.txt").readText()

    @Test fun listsEveryPerk() {
        Pro.perks.forEach { p -> assertTrue("fehlt: ${p.title}", p.title.substringBefore(" ") in withPro) }
        assertTrue("${Pro.FREE_PROFILES} Spieler frei", "frei: ${Pro.FREE_PROFILES}" in withPro)
        assertTrue("ab Planet ${Pro.FREE_PLANETS + 1}" in withPro)
        // Seit 2.6.0 kein Pro-Schulstoff mehr: Kapitel- und Levelzahl stimmen mit der App
        assertTrue("${Levels.all.size} LEVEL IN ${Levels.chapters.size} KAPITELN" in withPro)
        assertTrue("in jedem der ${Levels.chapters.size} Kapitel" in withPro)
        assertFalse("Oberstufe:" in withPro)
    }

    @Test fun noFreeOnlyClaims() {
        assertFalse("keine Käufe" in withPro); assertTrue("kein Abo" in withPro)
        assertTrue("Beschreibung zu lang für Play (4000)", withPro.length <= 4000)
    }
}
