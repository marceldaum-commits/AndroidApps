package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

class FamilieTest {
    private val data = mapOf("stars" to "1:3", "proCode" to "RL-X", "kiCode" to "", "kiDevice" to "abc", "elternKopplung" to "id.key", "sound" to "true", "familie" to "geheim")
    private val fam = "A".repeat(22) + "." + "b".repeat(43) + "." + "C-_".repeat(7) + "D"
    private val kid = "k".repeat(22)

    @Test fun liveNochAus() = assertFalse(Familie.ENABLED)

    @Test fun lernstandOhneRechteGeraeteKennungUndFamilienLink() =
        assertEquals(mapOf("stars" to "1:3", "sound" to "true"), Familie.child(data))

    @Test fun rechteGetrenntUndNurGesetzte() = assertEquals(mapOf("proCode" to "RL-X"), Familie.rights(data))

    @Test fun linkHinUndZurueck() {
        val link = Familie.link(fam, Familie.TEST_SERVER)
        assertEquals("https://lernen-test.mathenachhilfe-online.com/raumlicht/#familie=$fam", link)
        assertEquals(fam, Familie.fromLink(" $link ")); assertEquals(fam, Familie.fromLink(fam)); assertEquals(fam, Familie.fromLink("$link&ziel=galton"))
        assertNull(Familie.fromLink("https://lernen.mathenachhilfe-online.com/raumlicht/")); assertNull(Familie.fromLink("#familie=kaputt")); assertNull(Familie.fromLink(""))
    }

    @Test fun zustandHinUndZurueck() {
        val s = Familie.State(fam, kid, "K7QX-2MFA", 4, "123", "Lea")
        assertEquals(s, Familie.State.decode(s.encode()))
        assertEquals(Familie.State(fam, kid, "K7QX-2MFA"), Familie.State.decode("$fam|$kid|K7QX-2MFA|x||"))
        assertNull(Familie.State.decode("")); assertNull(Familie.State.decode("kaputt|$kid|n|0||")); assertNull(Familie.State.decode("$fam|kurz|n|0||"))
    }

    @Test fun kinderListe() {
        assertEquals(listOf(Familie.Kid(kid, "Lea"), Familie.Kid("t".repeat(22), "")), Familie.kids("$kid\tLea\n" + "t".repeat(22) + "\nunsinn\tX\n"))
        assertEquals("Kind ohne Namen", Familie.Kid(kid, "").label); assertEquals("Lea Marie", Familie.cleanName(" Lea\tMarie "))
        assertEquals(20, Familie.cleanName("x".repeat(40)).length)
    }

    @Test fun jsonHinUndZurueckOhneRechte() {
        val json = Familie.json(data); assertFalse(json.contains("RL-X")); assertFalse(json.contains("geheim"))
        assertEquals(Familie.child(data), Familie.data(json)); assertNull(Familie.data("kaputt")); assertNull(Familie.data("[1]"))
        assertEquals(mapOf("stars" to "9"), Familie.data("""{"stars":"9","proCode":"FREMD"}"""))
    }

    @Test fun fortschrittErkannt() { assertTrue(Familie.hasProgress(data)); assertFalse(Familie.hasProgress(mapOf("sound" to "true", "stars" to ""))) }

    @Test fun rueckmeldungenVerstaendlich() { for (r in listOf("netz", "keine", "schluessel", "schreibrecht", "noch-nicht-live", "zu-gross", "zu-oft", "link", "baustein", "fehler"))
        assertTrue(r, Familie.problem(r).length > 20) }
}
