package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParentOffersTest {
    @Test fun hintNeedsTriggerAndRespectsPauseAndHide() {
        val d = 20_000L
        assertNull(ParentOffers.hint(d, stuck = false, examUntil = 0, hidden = false, lastShown = 0))
        assertEquals(ParentOffers.Reason.STUCK, ParentOffers.hint(d, stuck = true, examUntil = 0, hidden = false, lastShown = 0))
        assertEquals(ParentOffers.Reason.EXAM, ParentOffers.hint(d, stuck = true, examUntil = d + 3, hidden = false, lastShown = 0))
        assertEquals(ParentOffers.Reason.STUCK, ParentOffers.hint(d, true, 0, false, lastShown = d))          // am selben Tag sichtbar
        assertNull(ParentOffers.hint(d, true, 0, false, lastShown = d - 20))                                      // Pause
        assertEquals(ParentOffers.Reason.STUCK, ParentOffers.hint(d, true, 0, false, lastShown = d - 28))     // nach 4 Wochen wieder
        assertNull(ParentOffers.hint(d, true, d + 5, hidden = true, lastShown = 0))                               // ausgeblendet
        assertNull(ParentOffers.hint(d, false, examUntil = d - 1, hidden = false, lastShown = 0))                 // Klassenarbeit vorbei
    }

    @Test fun linksCarrySourceAndNoPrice() {
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=kurvenflug-elternbericht", ParentOffers.trialLesson())
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=kurvenflug-klassenarbeit", ParentOffers.trialLesson(ParentOffers.Reason.EXAM))
        assertTrue(ParentOffers.kidTip(9, 100, 0) && !ParentOffers.kidTip(8, 100, 0) && !ParentOffers.kidTip(10, 100, 95) && ParentOffers.kidTip(10, 100, 93))
        assertTrue("gratis" !in ParentOffers.KID_TIP_TEXT && "Probestunde" in ParentOffers.KID_TIP_TEXT)
        assertTrue("crashkurs_klick" !in UsageCount.EVENTS)
        assertTrue("src=kurvenflug-geschenk" in ParentOffers.GIFT && "src=kurvenflug-fusszeile" in ParentOffers.FOOTER)
        listOf(ParentOffers.GIFT, ParentOffers.FOOTER, ParentOffers.FOOTER_TEXT).forEach { assertTrue('€' !in it) }
    }

    @Test fun everyChapterHasAnAllowedTopic() {
        Levels.chapters.forEach { c -> val t = ParentOffers.TOPICS[c.number]; assertTrue(t != null && t in ParentOffers.ALLOWED_TOPICS, "Kapitel ${c.number} ohne Thema") }
    }

    @Test fun usageBodyOnlyKnownEventsNoIds() {
        assertNull(UsageCount.body("2.5.22", emptyMap()))
        val b = UsageCount.body("2.5.22", mapOf("start" to 1, "level_geschafft" to 300, "name:Lena" to 1, "theorie" to 0))!!
        assertEquals("{\"app\":\"kurvenflug\",\"v\":\"2.5.22\",\"e\":{\"level_geschafft\":50,\"start\":1}}", b)
        assertTrue(UsageCount.EVENTS.size <= 20)
        assertNull(UsageCount.body("2.5.32", mapOf("probestunde_klick" to 1)))   // Ereignisse mit Ort nie in der Sammelsendung
        assertEquals("{\"app\":\"kurvenflug\",\"v\":\"2.5.32\",\"ev\":\"probestunde_klick\",\"ort\":\"klassenarbeit\"}",   // Recht V15: kein Thema
            UsageCount.single("2.5.32", "probestunde_klick", "klassenarbeit", "lineare-funktionen"))
        assertNull(UsageCount.single("2.5.32", "start", "klassenarbeit")); assertNull(UsageCount.single("2.5.32", "pro_kasse", "irgendwo"))
        assertEquals("{\"app\":\"kurvenflug\",\"v\":\"2.5.32\",\"ev\":\"pro_kasse\",\"ort\":\"gesperrt\"}", UsageCount.single("2.5.32", "pro_kasse", "gesperrt", "kf-3"))
    }
}
