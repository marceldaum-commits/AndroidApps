package de.experimentopia.core.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/** Familien-Speicher: sicheres Zusammenführen zweier Stände, Texte (Recht A6/V8), Familien-Nummer zum Löschen */
class FamilieTest {
    private val j = Json
    private fun res(vararg r: ResultRow) = j.encodeToString(ListSerializer(ResultRow.serializer()), r.toList())
    private fun note(id: Long, e: String, at: Long, right: Boolean) = NoteRow(id, e, at, "g", "r", right, 1, right)
    private fun notes(vararg n: NoteRow) = j.encodeToString(ListSerializer(NoteRow.serializer()), n.toList())

    @Test fun ergebnisseVereinigen() {
        val a = mapOf("ergebnisse" to res(ResultRow("x1", 2, true, 3, 100, 500), ResultRow("x2", 1, false, 1, 200, 200)))
        val b = mapOf("ergebnisse" to res(ResultRow("x1", 3, false, 2, 50, 400), ResultRow("x3", 3, true, 1, 300, 300)))
        val m = j.decodeFromString(ListSerializer(ResultRow.serializer()), Familie.merge(a, b)["ergebnisse"]!!).associateBy { it.experimentId }
        assertEquals(setOf("x1", "x2", "x3"), m.keys)
        assertEquals(3, m["x1"]!!.stars)                 // meiste Sterne
        assertFalse(m["x1"]!!.firstGuessRight)            // die früheste erste Vermutung (50) war falsch
        assertEquals(3, m["x1"]!!.plays); assertEquals(50, m["x1"]!!.firstAt); assertEquals(500, m["x1"]!!.lastAt)
    }
    @Test fun forscherbuchOhneDoppelteUndMitNeuenNummern() {
        val a = mapOf("forscherbuch" to notes(note(2, "x1", 20, true), note(1, "x1", 10, false)))
        val b = mapOf("forscherbuch" to notes(note(5, "x1", 20, true), note(4, "x2", 30, true)))
        val l = j.decodeFromString(ListSerializer(NoteRow.serializer()), Familie.merge(a, b)["forscherbuch"]!!)
        assertEquals(listOf(30L, 20L, 10L), l.map { it.at })      // gleiche Antwort (Versuch + Zeit) nur einmal, neueste zuerst
        assertEquals(listOf(3L, 2L, 1L), l.map { it.id })          // eindeutige Nummern
    }
    @Test fun forscherbuchHoechstens500() {
        val many = notes(*(1..400).map { note(it.toLong(), "x1", it.toLong(), true) }.toTypedArray())
        val other = notes(*(401..800).map { note(it.toLong(), "x2", it.toLong(), true) }.toTypedArray())
        val l = j.decodeFromString(ListSerializer(NoteRow.serializer()), Familie.merge(mapOf("forscherbuch" to many), mapOf("forscherbuch" to other))["forscherbuch"]!!)
        assertEquals(500, l.size); assertEquals(800L, l.first().at)
    }
    @Test fun abzeichenLaufUndMonate() {
        val m = Familie.merge(mapOf("abzeichen" to "a,c", "laborlaeufe" to "4", "verlauf" to Monthly.encode(mapOf("2026-11" to MonthSum(30, 10, 6)))),
            mapOf("abzeichen" to "b,c", "laborlaeufe" to "9", "verlauf" to Monthly.encode(mapOf("2026-10" to MonthSum(20, 5, 5), "2026-11" to MonthSum(10, 12, 7)))))
        assertEquals("a,b,c", m["abzeichen"]); assertEquals("9", m["laborlaeufe"])
        val v = Monthly.decode(m["verlauf"]); assertEquals(setOf("2026-10", "2026-11"), v.keys); assertEquals(12, v["2026-11"]!!.aufgaben)
    }
    @Test fun nichtsGeht_verloren_undNichtLesbaresBleibtLokal() {
        val a = mapOf("ergebnisse" to res(ResultRow("x1", 1, true, 1, 1, 1)))
        assertEquals(a, Familie.merge(a, emptyMap())); assertEquals(a, Familie.merge(emptyMap(), a)); assertEquals(a, Familie.merge(a, a))
        assertEquals("kaputt", Familie.merge(mapOf("ergebnisse" to "kaputt"), mapOf("ergebnisse" to "[]"))["ergebnisse"])   // Fehler → lokal bleibt, nichts wird überschrieben
        assertTrue("Nur Teile des Lernstands, nie Einstellungen oder Rechte", Familie.merge(mapOf("procode" to "EX-1", "klang" to "x"), emptyMap()).isEmpty())
    }
    @Test fun bringtNeues() {
        val a = mapOf("abzeichen" to "a")
        assertFalse(Familie.bringtNeues(a, mapOf("abzeichen" to "a"))); assertFalse(Familie.bringtNeues(a, mapOf("abzeichen" to "a")))
        assertTrue(Familie.bringtNeues(a, mapOf("abzeichen" to "a,b")))
    }
    @Test fun textenachVorgabe() {
        assertTrue(Familie.PFLICHTTEXT.startsWith("Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert."))
        assertTrue(Familie.PFLICHTTEXT.endsWith("Jede Wiederherstellung seht ihr hier im Protokoll."))
        assertEquals("Einverstanden – Familie anlegen", Familie.KNOPF_ANLEGEN); assertEquals("Einverstanden – Familie beitreten", Familie.KNOPF_BEITRETEN)
        assertFalse("nirgends „nur das Handy kann öffnen“", Familie.PFLICHTTEXT.contains("nur das Handy"))
        assertTrue(Familie.SICHERHEIT.startsWith("Wer den Familien-Link oder den QR-Code hat"))
        assertEquals("Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.", Familie.FAMILIE_WEG)
    }
    @Test fun familienNummerZumLoeschen() {
        assertTrue(Familie.nummerGleich("k7qx-2mfa", "K7QX-2MFA")); assertTrue(Familie.nummerGleich(" K7QX 2MFA ", "K7QX-2MFA"))
        assertTrue(Familie.nummerGleich("K7QX-2MF1", "K7QX-2MFL"))      // I/L/1 und O/0 wie beim Server
        assertFalse(Familie.nummerGleich("K7QX-2MFB", "K7QX-2MFA")); assertFalse(Familie.nummerGleich("", ""))
    }
}

/** Brücke zu familie.js: Aufrufe und Antworten als JSON */
class FamilieJsonTest {
    private val fam = FamilieJson.Fam("A".repeat(22), "B".repeat(43), "C".repeat(22))
    @Test fun antwortenLesen() {
        val a = FamilieJson.parse("""{"ok":true,"rev":3,"stand":"2026-11-24T10:00:00Z","daten":{"ergebnisse":"[]","abzeichen":"a,b"}}""")
        assertTrue(a.ok); assertEquals(3L, a.rev); assertEquals(mapOf("ergebnisse" to "[]", "abzeichen" to "a,b"), a.daten); assertNull(a.index)
        val i = FamilieJson.parse("""{"ok":true,"rev":1,"daten":{"v":1,"kinder":[{"kindId":"K1","name":"Lea"},{"kindId":"K2","name":""}],"apps":["experimentopia"]}}""")
        assertEquals(listOf(FamilieJson.Kind("K1", "Lea"), FamilieJson.Kind("K2", "")), i.kinder); assertTrue(i.index!!.contains("experimentopia")); assertTrue(i.daten.isEmpty())
        val f = FamilieJson.parse("""{"ok":true,"fam":{"famId":"${fam.famId}","famKey":"${fam.famKey}","schreib":"${fam.schreib}"},"nummer":"K7QX-2MFA"}""")
        assertEquals(fam, f.fam); assertEquals("K7QX-2MFA", f.nummer)
        val e = FamilieJson.parse("""{"ok":false,"grund":"familie-weg"}"""); assertFalse(e.ok); assertEquals("familie-weg", e.grund)
        assertFalse(FamilieJson.parse("kaputt").ok); assertEquals("fehler", FamilieJson.parse(null).grund)
    }
    @Test fun aufrufeBauen() {
        val s = FamilieJson.standSichern(fam, FamilieJson.basis(true), "KIND", mapOf("b" to "2", "a" to "1"), 4)
        assertTrue(s, s.contains("\"basis\":\"https://lernen-test.mathenachhilfe-online.com\"") && s.contains("\"rev\":4") && s.indexOf("\"a\":\"1\"") < s.indexOf("\"b\":\"2\""))
        assertEquals(fam, FamilieJson.famOrNull(fam.encode())); assertNull(FamilieJson.famOrNull("{}")); assertNull(FamilieJson.famOrNull(null))
        assertEquals("https://lernen.mathenachhilfe-online.com", FamilieJson.basis(false))
    }
    @Test fun fehlertexteVerstaendlich() {
        listOf("netz", "noch-nicht-live", "gibt-es-schon", "familie-weg", "schreibrecht", "zu-gross", "zu-oft", "schluessel", "keine", "xyz").forEach { assertTrue(FamilieJson.grundText(it).length > 10) }
        assertEquals(Familie.FAMILIE_WEG, FamilieJson.grundText("familie-weg"))
    }
}
