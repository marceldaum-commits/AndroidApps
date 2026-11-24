package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test

/** Zählung: nur erlaubte Felder, keine Kennung; Probestunden-Link mit Herkunft, Thema und Klasse */
class ZaehlungTest {
    @Test fun inhalt() {
        assertEquals("""{"app":"experimentopia","v":"2.11.0","ev":"start"}""", Zaehlung.inhalt("2.11.0", "start"))
        assertEquals("""{"app":"experimentopia","v":"2.11.0","ev":"probestunde_klick","ort":"klassenarbeit"}""",
            Zaehlung.inhalt("2.11.0", "probestunde_klick", "klassenarbeit", "physik-optik"))   // Thema wird bei Probestunden-Klicks nie gezählt (Recht V15)
        assertNull("unbekanntes Ereignis", Zaehlung.inhalt("1", "spion"))
        assertNull("unbekannter Ort", Zaehlung.inhalt("1", "start", "kinderzimmer"))
        assertEquals("unbekanntes Thema wird weggelassen", """{"app":"experimentopia","v":"1","ev":"start"}""", Zaehlung.inhalt("1", "start", thema = "mathe-geheim"))
        assertEquals("Version gesäubert", """{"app":"experimentopia","v":"2.0x","ev":"start"}""", Zaehlung.inhalt("2.0\"x", "start"))
        Topic.entries.forEach { assertTrue(ParentReport.slug(it) in Zaehlung.THEMEN) }
        assertEquals("https://trainer-test.mathenachhilfe-online.com/zaehler/v1/e", Zaehlung.url(true))
    }

    @Test fun alleApps() {
        assertEquals("https://www.mathenachhilfe-online.com/lern-apps/?src=experimentopia-kopf", Angebote.alleApps(false))
        assertEquals("https://staging.mathenachhilfe-online.com/lern-apps/?src=experimentopia-kopf", Angebote.alleApps(true))
    }
    @Test fun probestunde() {
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=experimentopia-klassenarbeit", Angebote.probestunde())
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=experimentopia-oberstufe_tipp", Angebote.probestunde("oberstufe_tipp"))
    }
}
