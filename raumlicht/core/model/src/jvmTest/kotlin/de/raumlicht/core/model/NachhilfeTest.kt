package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Anonyme Zählung und Nachhilfe-Hinweis: was gesendet wird, wann der Hinweis erscheint und wann nicht. */
class NachhilfeTest {
    @Test fun zaehlaufrufEnthaeltNurDieFuenfFelder() {
        assertEquals("""{"app":"raumlicht","v":"2.18.22","ev":"start"}""", Zaehler.body("start", null, null, "2.18.22"))
        assertEquals("""{"app":"raumlicht","v":"2.18.22","ev":"probestunde_klick","ort":"lernstand","thema":"vektoren"}""", Zaehler.body("probestunde_klick", "lernstand", "vektoren", "2.18.22"))
        // nichts außerhalb der Listen – und nie etwas, das eine Person oder ein Gerät erkennbar macht
        assertNull(Zaehler.body("irgendwas", null, null, "1")); assertNull(Zaehler.body("start", "kinderzimmer", null, "1")); assertNull(Zaehler.body("start", null, "lena", "1")); assertNull(Zaehler.body("start", null, null, "x".repeat(21)))
        for (ev in Zaehler.EVENTS) for (ort in Zaehler.PLACES + null) for (thema in Zaehler.TOPICS + null) { val b = Zaehler.body(ev, ort, thema, "2.18.22")!!
            val keys = Regex(""""(\\w+)":""").findAll(b).map { it.groupValues[1] }.toSet(); assertTrue(b, keys.all { it in setOf("app", "v", "ev", "ort", "thema") })
            for (bad in listOf("geraet", "\"code\"", "RL-", "stern", "\"id\"", "name")) assertFalse(b, b.contains(bad, ignoreCase = true)) }
    }

    @Test fun linksTragenDieHerkunft() {
        assertEquals("https://www.mathenachhilfe-online.com/probestunde/?src=raumlicht-lernstand&thema=vektoren", Zaehler.link(Nachhilfe.TRIAL_URL, "lernstand", "vektoren"))
        assertEquals("https://www.mathenachhilfe-online.com/geschenke/?src=raumlicht-geschenk", Zaehler.link(Nachhilfe.GIFT_URL, "geschenk"))
        assertEquals("https://www.mathenachhilfe-online.com/?src=raumlicht-fusszeile", Zaehler.link(Nachhilfe.HOME_URL, "fusszeile"))
        assertTrue(Zaehler.link(Pro.CHECKOUT_URL, "gesperrt").endsWith("&src=raumlicht-gesperrt")); assertTrue(Zaehler.link(Ki.CHECKOUT_URL, "einstellungen").endsWith("?src=raumlicht-einstellungen"))
        assertEquals("vektoren", Zaehler.topic(Area.GEOMETRIE)); assertEquals("analysis", Zaehler.topic(Area.ANALYSIS)); assertTrue(Area.entries.all { Zaehler.topic(it) in Zaehler.TOPICS })
    }

    private val geo = Levels.chapters.filter { it.area == Area.GEOMETRIE }.flatMap { it.levels }
    private fun hint(stars: Map<String, Int>, today: Long = 100, state: String = "", on: Boolean = true) = Nachhilfe.hint(Learn.topics(stars), stars, today, state, on)

    @Test fun hinweisNurWennEinGebietErkennbarHakt() {
        assertNull("nichts gespielt", hint(emptyMap()))
        assertNull("zu wenig gespielt", hint(geo.take(5).associate { it.id to 1 }))
        assertNull("läuft gut", hint(geo.take(10).associate { it.id to 3 }))
        val weak = geo.take(8).associate { it.id to 1 }                               // acht Aufgaben, alle erst nach mehreren Versuchen: 33 % der Sterne
        val h = hint(weak)!!
        assertEquals("vektoren", h.topic); assertTrue(h.text.startsWith("Bei Vektoren hakt es noch?")); assertTrue(h.link.contains("src=raumlicht-lernstand&thema=vektoren"))
        // sachlich: kein Preis, kein Druck, keine Figur
        for (bad in listOf("€", "Euro", "jetzt", "nur noch", "Lux", "!", "Angebot endet")) assertFalse(bad, h.text.contains(bad))
        assertNull("abgeschaltet", hint(weak, on = false))
    }

    @Test fun haeufigkeitsgrenzeUndAusblenden() {
        val weak = geo.take(8).associate { it.id to 1 }
        var state = ""
        assertTrue(Nachhilfe.isNew(state, "vektoren", 100)); state = Nachhilfe.shown(state, "vektoren", 100)
        assertNotNull("am selben Tag bleibt er stehen", hint(weak, 100, state)); assertFalse(Nachhilfe.isNew(state, "vektoren", 100))
        for (d in 101L..113L) assertNull("Tag $d: Ruhe", hint(weak, d, state))
        assertNotNull("nach 14 Tagen wieder", hint(weak, 114, state)); assertTrue(Nachhilfe.isNew(state, "vektoren", 114))
        state = Nachhilfe.hide(state, "vektoren"); for (d in listOf(114L, 200L, 9999L)) assertNull("ausgeblendet bleibt aus", hint(weak, d, state))
        // höchstens ein Hinweis: hakt es in zwei Gebieten, kommt nur das schwächere – und nach dem Ausblenden das andere
        val ana = Levels.chapters.filter { it.area == Area.ANALYSIS }.flatMap { it.levels }.take(8).associate { it.id to 1 }
        assertEquals("vektoren", hint(weak + ana)!!.topic); assertEquals("analysis", hint(weak + ana, 100, Nachhilfe.hide("", "vektoren"))!!.topic)
    }

    /** „Klausur bald“: nur 3 bis 14 Tage vorher, nur mit bekannter Adresse, je Klausur ausblendbar – und solange die Adresse fehlt, gar nicht. */
    @Test fun klausurHinweis() {
        val url = "https://www.mathenachhilfe-online.com/crashkurse/"
        val geo = Levels.chapters.filter { it.area == Area.GEOMETRIE }.map { it.number }.take(2).toSet()
        val plan = Plan(1000, geo)
        assertEquals("https://www.mathenachhilfe-online.com/produkt/easy-learning-crash-kurs/", Nachhilfe.CRASH_URL)
        assertEquals(Nachhilfe.CRASH_URL + "?src=raumlicht-klassenarbeit&thema=vektoren", Nachhilfe.exam(plan, 990, "", true)!!.link); assertNull("ohne Adresse kein Hinweis", Nachhilfe.exam(plan, 990, "", true, ""))
        for (d in listOf(986L, 990L, 997L)) assertNotNull("Tag $d", Nachhilfe.exam(plan, d, "", true, url))
        for (d in listOf(900L, 985L, 998L, 999L, 1000L, 1001L)) assertNull("Tag $d", Nachhilfe.exam(plan, d, "", true, url))
        val h = Nachhilfe.exam(plan, 990, "", true, url)!!
        assertEquals(10, h.days); assertEquals("$url?src=raumlicht-klassenarbeit&thema=vektoren", h.link); assertTrue(h.text.startsWith("In 10 Tagen ist deine Klausur."))
        for (bad in listOf("€", "Euro", "jetzt", "Lux", "!", "kostenlos")) assertFalse(bad, h.text.contains(bad))
        assertNull(Nachhilfe.exam(plan, 990, Nachhilfe.hide("", h.key), true, url)); assertNotNull("eine andere Klausur zeigt ihn wieder", Nachhilfe.exam(Plan(1001, geo), 990, Nachhilfe.hide("", h.key), true, url))
        assertNull(Nachhilfe.exam(plan, 990, "", false, url)); assertNull(Nachhilfe.exam(null, 990, "", true, url))
        // gemischte Gebiete: kein Thema im Link
        val mixed = Plan(1000, setOf(Levels.chapters.first { it.area == Area.GEOMETRIE }.number, Levels.chapters.first { it.area == Area.ANALYSIS }.number))
        assertEquals("$url?src=raumlicht-klassenarbeit", Nachhilfe.exam(mixed, 990, "", true, url)!!.link)
        assertNotNull(Zaehler.body("crashkurs_klick", "klassenarbeit", "vektoren", "1"))
    }
}
