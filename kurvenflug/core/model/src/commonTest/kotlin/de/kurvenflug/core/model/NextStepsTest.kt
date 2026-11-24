package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NextStepsTest {
    @Test fun linksToFreeUpperSchoolApps() {
        assertEquals(listOf("Mathe-Trainer", "Raumlicht"), NextSteps.apps.map { it.name })
        NextSteps.apps.forEach {
            assertTrue(it.url.startsWith("https://") && it.url.endsWith("src=kurvenflug-weiter"), it.url)
            assertTrue(".mathenachhilfe-online.com/" in it.url, it.url)
            // kein Preis, kein Kaufhinweis
            listOf("€", "kaufen", "Pro", "Preis").forEach { w -> assertTrue(w !in it.what + it.topics, "$w in ${it.name}") }
        }
    }

    /** Recht V10b: Raumlicht nur auf preisfreie Einstiege (nicht „erleben“, nicht Pro-Ziele) */
    @Test fun raumlichtOnlyPriceFreeEntry() {
        val url = NextSteps.apps.first { it.name == "Raumlicht" }.url
        val frei = listOf("ziel=galton", "ziel=zufall", "ziel=rotation", "ziel=raumflug", "ziel=level/3-1", "ziel=level/5-1", "ziel=level/5-6")
        assertTrue(frei.any { it in url }, url)
    }
}
