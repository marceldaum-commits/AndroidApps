package de.experimentopia.core.model

import org.junit.Assert.*
import org.junit.Test

/** Weg zur Probestunde (Recht V15): objektive Auslöser, nur Physik, Pausen, Wortlaut, Zählung ohne Thema */
class NachhilfeTest {
    private val tag = 86_400_000L
    private val now = 100 * tag
    private fun note(id: String, daysAgo: Int, right: Boolean) = NoteRow(experimentId = id, at = now - daysAgo * tag, guess = "", result = "", right = right, sure = 1, explainedFirstTry = right)
    private val themaOf: (String) -> Topic? = { Catalog.byId(it)?.topic }
    private val strom = Catalog.all.first { it.topic == Topic.STROM }.id
    private val saeure = Catalog.all.first { it.topic.subject != Subject.PHYSIK }.id

    @Test fun ausloeserNurBeiObjektiverSchwierigkeit() {
        val schwer = listOf(20, 17, 14, 9, 5, 2).map { note(strom, it, right = false) }
        assertEquals(listOf(Topic.STROM), Nachhilfe.schwierigeThemen(schwer, now, themaOf))
        // zu wenige Antworten
        assertTrue(Nachhilfe.schwierigeThemen(schwer.take(5), now, themaOf).isEmpty())
        // nicht über 2 Wochen: die älteste Antwort ist erst 10 Tage alt
        assertTrue(Nachhilfe.schwierigeThemen(listOf(10, 9, 8, 6, 3, 1).map { note(strom, it, false) }, now, themaOf).isEmpty())
        // 3 von 6 richtig = 50 % → noch kein Hinweis (nur unter 50 %)
        assertTrue(Nachhilfe.schwierigeThemen(listOf(20, 17, 14, 9, 5, 2).mapIndexed { i, d -> note(strom, d, right = i < 3) }, now, themaOf).isEmpty())
        // 2 von 6 richtig = 33 % → Hinweis
        assertEquals(1, Nachhilfe.schwierigeThemen(listOf(20, 17, 14, 9, 5, 2).mapIndexed { i, d -> note(strom, d, right = i < 2) }, now, themaOf).size)
        // nur Physik (Nachhilfe gibt es nur dort)
        assertTrue(Nachhilfe.schwierigeThemen(listOf(20, 17, 14, 9, 5, 2).map { note(saeure, it, false) }, now, themaOf).isEmpty())
        // zu alte Antworten zählen nicht
        assertTrue(Nachhilfe.schwierigeThemen(listOf(60, 50, 40, 35, 31, 30).map { note(strom, it, false) }, now, themaOf).isEmpty())
    }
    @Test fun pausen() {
        assertTrue(Nachhilfe.darfZeigen(null, now, Nachhilfe.PAUSE_THEMA_TAGE))
        assertFalse(Nachhilfe.darfZeigen(now - 27 * tag, now, Nachhilfe.PAUSE_THEMA_TAGE))
        assertTrue(Nachhilfe.darfZeigen(now - 28 * tag, now, Nachhilfe.PAUSE_THEMA_TAGE))
        assertFalse(Nachhilfe.darfZeigen(now - 6 * tag, now, Nachhilfe.PAUSE_TIPP_TAGE))
        assertTrue(Nachhilfe.darfZeigen(now - 7 * tag, now, Nachhilfe.PAUSE_TIPP_TAGE))
    }
    @Test fun oberstufeNurKlasse9bis10() {
        assertEquals(listOf(false, false, false, true, true, false), listOf(0, 5, 8, 9, 10, 11).map { Nachhilfe.tippFuerKlasse(it) })
    }
    @Test fun wortlautUndKeineWerbungFuerKinder() {
        assertEquals("Hinweis: In Strom hat Lea in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. " +
            "Wenn ihr persönliche Unterstützung möchtet: Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe.", Nachhilfe.elternHinweis("Strom", "Lea"))
        assertTrue(Nachhilfe.elternHinweis("Strom", null).contains("euer Kind"))
        // Kinder-Hilfesatz: kein Bezug auf Probestunde, Eltern-Bereich oder Außenlink
        listOf("probestunde", "elternbereich", "für eltern", "http", "kosten").forEach { assertFalse(it, Nachhilfe.KIND_HILFE.lowercase().contains(it)) }
        assertTrue(Nachhilfe.TIPP_OBERSTUFE.contains("kostenlose, unverbindliche Probestunde"))
        assertFalse(Nachhilfe.TIPP_OBERSTUFE.lowercase().contains("gratis"))
        listOf("€", "jetzt", "nur noch", "rabatt", "angst", "note").forEach { assertFalse(it, Nachhilfe.TIPP_OBERSTUFE.lowercase().contains(it)) }
    }
    @Test fun zaehlungOhneThema() {
        assertEquals("""{"app":"experimentopia","v":"2.14.0","ev":"probestunde_klick","ort":"elternhinweis"}""", Zaehlung.inhalt("2.14.0", Nachhilfe.ZAEHL_EREIGNIS, "elternhinweis", "physik-optik"))
        assertEquals("""{"app":"experimentopia","v":"2.14.0","ev":"probestunde_klick","ort":"oberstufe_tipp"}""", Zaehlung.inhalt("2.14.0", Nachhilfe.ZAEHL_EREIGNIS, "oberstufe_tipp", null))
    }
}
