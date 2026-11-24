package de.wissensgarten.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AngeboteTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Test fun zaehlInhaltEnthaeltNurErlaubteFelder() {
        val j = Zaehlung.inhalt("2.2.13", "probestunde_klick", "elternbericht", "einmaleins")!!
        assertEquals("""{"app":"wissensgarten","v":"2.2.13","ev":"probestunde_klick","ort":"elternbericht","thema":"einmaleins"}""", j)
        // Thema, das der Zähl-Server nicht kennt: Ereignis zählt, Thema fällt weg
        assertEquals("""{"app":"wissensgarten","v":"2.2.13","ev":"nachhilfe_hinweis","ort":"elternbericht"}""", Zaehlung.inhalt("2.2.13", "nachhilfe_hinweis", "elternbericht", "brueche"))
        // keine Kennungen, Gerätedaten, Namen oder Lernstände – nur diese fünf Schlüssel
        val keys = Regex("\"(\\w+)\":").findAll(j).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("app", "v", "ev", "ort", "thema"), keys)
        assertEquals("""{"app":"wissensgarten","v":"2.2.13","ev":"start"}""", Zaehlung.inhalt("2.2.13", "start"))
    }

    @Test fun ungueltigeWerteWerdenNichtGesendet() {
        assertNull(Zaehlung.inhalt("1", "irgendwas"))
        assertNull(Zaehlung.inhalt("1", "start", ort = "kinderbereich"))
        assertEquals("""{"app":"wissensgarten","v":"1","ev":"start"}""", Zaehlung.inhalt("1", "start", thema = "Lena"))
        // Version wird gekürzt und bereinigt
        assertEquals("""{"app":"wissensgarten","v":"12345678901234567890","ev":"start"}""", Zaehlung.inhalt("12345678901234567890xyz", "start"))
        assertEquals("""{"app":"wissensgarten","v":"2x","ev":"start"}""", Zaehlung.inhalt("2\"},{\"x", "start"))
    }

    @Test fun linksTragenHerkunft() {
        // V15: nur Herkunft, nie Thema oder Klasse
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=wissensgarten-elternbericht",
            Angebote.probestunde("elternbericht", "brueche", 6))
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=wissensgarten-klassenarbeit",
            Angebote.probestunde("klassenarbeit", null, 14))
        assertEquals("https://www.mathenachhilfe-online.com/geschenke/?src=wissensgarten-geschenk", Angebote.geschenk)
        assertEquals("https://www.mathenachhilfe-online.com/produkt/einzelnachhilfe-neukundenpaket/?src=wissensgarten-elternbericht&thema=deutsch&klasse=3",
            Angebote.neukundenpaket("elternbericht", "deutsch", 3))
        assertEquals("https://www.mathenachhilfe-online.com/produkt/easy-learning-crash-kurs/?src=wissensgarten-klassenarbeit&klasse=7",
            Angebote.crashkurs(null, 7))
        assertTrue(Angebote.crashkursPasst(5)); assertFalse(Angebote.crashkursPasst(4)); assertFalse(Angebote.crashkursPasst(null)); assertFalse(Angebote.crashkursPasst(14))
        assertEquals("https://www.mathenachhilfe-online.com/?src=wissensgarten-fusszeile", Angebote.fusszeile)
    }

    @Test fun themenZuordnung() {
        assertEquals("einmaleins", Angebote.thema("gs-einmaleins", "mathe"))
        assertEquals("brueche", Angebote.thema("s1-mathe-brueche-prozent", "mathe"))
        assertEquals("grundrechenarten", Angebote.thema("gs-plus-minus-20", "mathe"))
        assertEquals("englisch", Angebote.thema("gs-englisch-tiere", "englisch"))
        assertNull(Angebote.thema("s1-geschichte-antike", "geschichte"))
        assertNull(Angebote.thema(null, "eigene"))
    }

    private fun antworten(thema: String, richtig: Int, falsch: Int, vorTagen: Int = 1) =
        List(richtig) { NachhilfeHinweis.Antwort(thema, now - vorTagen * day, true) } + List(falsch) { NachhilfeHinweis.Antwort(thema, now - vorTagen * day, false) }

    @Test fun hinweisNurWennEsHakt() {
        assertNull(NachhilfeHinweis.pruefe(antworten("brueche", 3, 5), now, emptyMap(), emptySet()), "weniger als 10 Antworten")
        assertNull(NachhilfeHinweis.pruefe(antworten("brueche", 7, 3), now, emptyMap(), emptySet()), "70 % richtig")
        assertNull(NachhilfeHinweis.pruefe(antworten("brueche", 5, 5), now, emptyMap(), emptySet()), "genau 50 % ist nicht darunter")
        assertNull(NachhilfeHinweis.pruefe(antworten("brueche", 2, 10, vorTagen = 20), now, emptyMap(), emptySet()), "zu alt")
        val h = NachhilfeHinweis.pruefe(antworten("brueche", 5, 6), now, emptyMap(), emptySet())
        assertNotNull(h); assertEquals("brueche", h.thema); assertEquals(45, h.prozent); assertTrue(h.neu)
        assertNull(NachhilfeHinweis.pruefe(antworten("brueche", 6, 5), now, emptyMap(), emptySet()), "54 % richtig – nicht unter 50 %")
    }

    @Test fun hoechstensEinHinweisDerSchwaechste() {
        val h = NachhilfeHinweis.pruefe(antworten("brueche", 5, 6) + antworten("englisch", 2, 9), now, emptyMap(), emptySet())
        assertEquals("englisch", h!!.thema)
    }

    @Test fun haeufigkeitsgrenzeUndAusblenden() {
        val a = antworten("brueche", 5, 6)
        // heute schon gezeigt → bleibt stehen, wird aber nicht neu gezählt
        assertEquals(false, NachhilfeHinweis.pruefe(a, now, mapOf("brueche" to now - 3_600_000), emptySet())!!.neu)
        // vor 5 Tagen gezeigt → Pause
        assertNull(NachhilfeHinweis.pruefe(a, now, mapOf("brueche" to now - 5 * day), emptySet()))
        // vor 15 und vor 27 Tagen gezeigt → noch Pause (höchstens 1× in 4 Wochen je Thema)
        assertNull(NachhilfeHinweis.pruefe(a, now, mapOf("brueche" to now - 15 * day), emptySet()))
        assertNull(NachhilfeHinweis.pruefe(a, now, mapOf("brueche" to now - 27 * day), emptySet()))
        // vor 29 Tagen gezeigt → wieder erlaubt
        assertTrue(NachhilfeHinweis.pruefe(a, now, mapOf("brueche" to now - 29 * day), emptySet())!!.neu)
        // ausgeblendet → nie wieder
        assertNull(NachhilfeHinweis.pruefe(a, now, emptyMap(), setOf("brueche")))
    }

    @Test fun wortlautVonRechtV15() {
        assertEquals("Hinweis: In Bruchrechnung hat Mia in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. " +
            "Wenn ihr persönliche Unterstützung möchtet: Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe.", NachhilfeHinweis.text("brueche", "Mia"))
        assertTrue(NachhilfeHinweis.text("einmaleins", "Ben").startsWith("Hinweis: Im Einmaleins hat Ben"))
        assertEquals("Probestunde anfragen", NachhilfeHinweis.KNOPF)
        assertEquals("Solche Hinweise ausblenden", NachhilfeHinweis.AUSBLENDEN)
    }

    @Test fun klassenarbeitDreiBisVierzehnTage() {
        val ex = listOf(Exam("a", "b", "Mathe", 102), Exam("c", "b", "Deutsch", 110), Exam("d", "b", "Englisch", 130))
        assertEquals("c", NachhilfeHinweis.klassenarbeit(ex, 100, emptySet())!!.id)
        assertNull(NachhilfeHinweis.klassenarbeit(ex, 100, setOf("c")))
        assertEquals("d", NachhilfeHinweis.klassenarbeit(ex, 120, emptySet())!!.id)
    }
}
