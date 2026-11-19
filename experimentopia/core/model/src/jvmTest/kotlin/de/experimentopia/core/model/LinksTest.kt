package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test

/** Direkt-Links und Querverweise: eindeutig, stabil, keine toten oder Kauf-Links */
class LinksTest {
    @Test fun namenEindeutig() {
        val names = Topic.entries.map { Links.name(it) }
        assertEquals(names.size, names.toSet().size)
        names.forEach { assertTrue(it, it.matches(Regex("[a-z]+"))) }
        Links.PLANNED.keys.forEach { assertFalse("angekündigter Name ist schon vergeben: $it", it in names) }
    }

    @Test fun linksLesen() {
        Topic.entries.forEach { t -> assertEquals(Links.Target(t.subject, t), Links.target("#thema=" + Links.name(t))) }
        assertEquals(Links.Target(Subject.PHYSIK, Topic.OPTIK), Links.target("https://experimentopia.mathenachhilfe-online.com/#thema=optik"))
        assertEquals(Links.Target(Subject.PHYSIK, Topic.OPTIK), Links.target("?thema=OPTIK"))
        assertEquals(Links.Target(Subject.PHYSIK, null), Links.target("#thema=energie"))
        assertEquals(Subject.CHEMIE, Links.target("#thema=chemie-saeuren-laugen")!!.subject)
        assertEquals(Topic.SAEURE, Links.target("#thema=chemie-saeuren-laugen")!!.topic)
        assertEquals(Topic.STROM, Links.target("#thema=physik-elektrizitaet")!!.topic)
        assertNull("unbekannt → Startseite", Links.target("#thema=quatsch"))
        assertNull(Links.target("#pro=EX-1"))
        assertEquals("https://lernen-test.mathenachhilfe-online.com/experimentopia/#thema=optik", Links.url(Topic.OPTIK, true))
        assertEquals(Links.Target(Subject.PHYSIK, Topic.OPTIK), Links.target("https://lernen.mathenachhilfe-online.com/experimentopia/#thema=optik"))
    }

    @Test fun querverweise() {
        Topic.entries.mapNotNull { Links.cross(it) }.forEach { c ->
            listOf(c.url(false), c.url(true)).forEach { u ->
                assertTrue(u, u.startsWith("https://") && u.contains(".mathenachhilfe-online.com/"))
                assertFalse("keine Kaufseite: $u", u.contains("pro") || u.contains("kasse") || u.contains("preis"))
            }
            assertTrue("Test-Stufe nutzt die Test-Adresse", c.url(true).contains("-test."))
            assertTrue("erklärender Satz", c.sentence.length in 40..200 && c.sentence.endsWith("."))
        }
        assertTrue(Topic.entries.count { Links.cross(it) != null } >= 6)
    }
}
