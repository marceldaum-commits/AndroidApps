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
        assertNull(ParentOffers.hint(d, true, 0, false, lastShown = d - 3))                                       // Pause
        assertEquals(ParentOffers.Reason.STUCK, ParentOffers.hint(d, true, 0, false, lastShown = d - 7))      // nach einer Woche wieder
        assertNull(ParentOffers.hint(d, true, d + 5, hidden = true, lastShown = 0))                               // ausgeblendet
        assertNull(ParentOffers.hint(d, false, examUntil = d - 1, hidden = false, lastShown = 0))                 // Klassenarbeit vorbei
    }

    @Test fun linksCarrySourceAndNoPrice() {
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=kurvenflug-eltern&thema=kf-9&klasse=9", ParentOffers.trialLesson(9, 9))
        assertTrue(ParentOffers.trialLesson(3, null).endsWith("thema=kf-3"))
        assertTrue("src=kurvenflug-geschenk" in ParentOffers.GIFT && "src=kurvenflug-fusszeile" in ParentOffers.FOOTER)
        listOf(ParentOffers.GIFT, ParentOffers.FOOTER, ParentOffers.FOOTER_TEXT).forEach { assertTrue('€' !in it) }
    }

    @Test fun usageBodyOnlyKnownEventsNoIds() {
        assertNull(UsageCount.body("2.5.21", "wasm", emptyMap()))
        val b = UsageCount.body("2.5.21", "wasm", mapOf("start" to 1, "level_geschafft" to 3, "name:Lena" to 1, "theorie" to 0))!!
        assertEquals("{\"app\":\"kurvenflug\",\"v\":\"2.5.21\",\"f\":\"wasm\",\"e\":{\"level_geschafft\":3,\"start\":1}}", b)
    }
}
