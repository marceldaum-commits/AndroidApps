package de.wissensgarten.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuerverweiseTest {
    @Test fun gleichungenVerweistAufMatheTrainerUndKurvenFlug() {
        val l = Querverweise.fuer("s1-mathe-gleichungen")
        assertEquals(listOf("Mathe-Trainer", "KurvenFlug"), l.map { it.app })
        assertEquals("https://trainer.mathenachhilfe-online.com/?src=wissensgarten-theorie#/u/k7-gleichungen", l[0].url)
        assertEquals("https://kurvenflug.mathenachhilfe-online.com/?src=wissensgarten-theorie#kapitel=terme", l[1].url)
    }

    @Test fun teststufe() {
        assertEquals("https://rechenblitz-test.mathenachhilfe-online.com/?thema=mul&stufe=2&modus=training&src=wissensgarten-theorie",
            Querverweise.fuer("gs-einmaleins", test = true).single().url)
        assertEquals("https://trainer-test.mathenachhilfe-online.com/?src=wissensgarten-theorie#/g/analysis", Querverweise.fuer("ob-mathe-analysis", true).single().url)
        assertEquals("https://raumlicht-test.mathenachhilfe-online.com/?ziel=galton&src=wissensgarten-theorie", Querverweise.fuer("s1-mathe-wahrscheinlichkeit", true).last().url)
    }

    @Test fun kopfrechnenQuadratzahlenImRechenblitz() {
        val l = Querverweise.fuer("s1-mathe-kopfrechnen")
        assertEquals(listOf("Mathe-Trainer", "Rechenblitz"), l.map { it.app })
        assertEquals("https://rechenblitz.mathenachhilfe-online.com/?thema=pow&stufe=3&modus=training&src=wissensgarten-theorie", l[1].url)
    }

    @Test fun alleLinksSindHttpsUndTragenSrc() {
        for (p in Querverweise.pakete) for (l in Querverweise.fuer(p)) {
            assertTrue(l.url.startsWith("https://") && ".mathenachhilfe-online.com/" in l.url, l.url)
            assertTrue("src=wissensgarten-theorie" in l.url, l.url)
            assertTrue(" " !in l.url, l.url)
        }
        assertTrue(Querverweise.fuer("gs-tiere").isEmpty())
    }

    @Test fun eigenerDeeplink() {
        assertEquals("https://lernen.mathenachhilfe-online.com/wissensgarten/#paket=gs-einmaleins", Querverweise.paketLink("gs-einmaleins"))
        assertEquals("https://lernen-test.mathenachhilfe-online.com/wissensgarten/#paket=gs-einmaleins", Querverweise.paketLink("gs-einmaleins", true))
    }
}
