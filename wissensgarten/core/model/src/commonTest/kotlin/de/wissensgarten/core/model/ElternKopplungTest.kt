package de.wissensgarten.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElternKopplungTest {
    private val day = 24L * 60 * 60 * 1000
    private val utc: (Long) -> Int = { 0 }
    private val jetzt = 20_400L * day + 15 * 3_600_000L   // Tag 20400, 15 Uhr UTC
    private val heute = 20_400L

    @Test fun kopplungHatRichtigeLaengen() {
        var n = 0
        val k = ElternKopplung.neu { size -> ByteArray(size) { (n++ % 251).toByte() } }
        assertEquals(22, k.id.length); assertEquals(22, k.schreib.length); assertEquals(43, k.key.length)
        assertTrue(ElternKopplung.gueltig(k))
        assertTrue(listOf(k.id, k.schreib, k.key).all { s -> s.all { it.isLetterOrDigit() || it == '-' || it == '_' } }, "base64url ohne Padding")
        assertEquals(32, ElternKopplung.b64urlDecode(k.key).size)
        assertEquals("https://www.mathenachhilfe-online.com/eltern-bericht/#${k.id}.${k.key}", ElternKopplung.elternLink(false, k))
        assertEquals("https://staging.mathenachhilfe-online.com/eltern-bericht/#${k.id}.${k.key}", ElternKopplung.elternLink(true, k))
        assertEquals("https://trainer-test.mathenachhilfe-online.com/eltern/v1/b/${k.id}", ElternKopplung.berichtUrl(true, k.id))
        assertEquals("https://trainer.mathenachhilfe-online.com/eltern/v1/b/${k.id}", ElternKopplung.berichtUrl(false, k.id))
    }

    private fun bericht(name: String? = null, antworten: List<ElternKopplung.Antwort> = emptyList()): JsonObject {
        val beete = listOf(
            ElternKopplung.Beet("b1", "Das kleine Einmaleins", "einmaleins", "Mathe"),
            ElternKopplung.Beet("b2", "Tiere im Wald", null, "Sachunterricht"),
        )
        val karten = (1..10).map { ElternKopplung.Karte("m$it", "b1", reps = 2, stability = if (it <= 4) 10.0 else 2.0) } +
            (1..6).map { ElternKopplung.Karte("s$it", "b2", reps = 1, stability = 1.0) }
        return ElternKopplung.bericht(name, 3, jetzt, heute, beete, karten, antworten,
            listOf(ElternKopplung.Arbeit(heute + 5, "Mathe: Einmaleins"), ElternKopplung.Arbeit(heute - 2, "alt")), "Lieber oft und kurz.", utc)
    }

    @Test fun pflichtfelderUndKeinePersonendaten() {
        val b = bericht()
        assertEquals(1, b["format"]!!.jsonPrimitive.int)
        assertEquals("wissensgarten", b["app"]!!.jsonPrimitive.content)
        assertEquals("Wissensgarten", b["appName"]!!.jsonPrimitive.content)
        assertTrue(b["stand"]!!.jsonPrimitive.content.startsWith("2025-11-08T15:00:00"), b["stand"].toString())
        assertNull(b["name"], "ohne Spitzname kein Name")
        val erlaubt = setOf("format", "app", "appName", "name", "klasse", "stand", "zeitraum", "lernzeitMin", "tageAktiv", "aufgaben",
            "richtigProzent", "staerken", "schwach", "fortschritt", "letzte", "klassenarbeit", "hinweis", "verlauf")
        assertTrue(erlaubt.containsAll(b.keys), "unbekannte Felder: ${b.keys - erlaubt}")
        assertEquals("Lena", bericht(name = "  Lena ")["name"]!!.jsonPrimitive.content)
    }

    @Test fun kennzahlenStaerkenUndProblemstellen() {
        val a = (1..8).map { ElternKopplung.Antwort("m$it", jetzt - (it % 3) * day, true) } +          // Einmaleins 8/8
            (1..6).map { ElternKopplung.Antwort("s$it", jetzt - day, it <= 2) } +                       // Sachunterricht 2/6
            listOf(ElternKopplung.Antwort("m1", jetzt - 10 * day, false))                               // älter als 7 Tage
        val b = bericht(antworten = a)
        assertEquals(14, b["aufgaben"]!!.jsonPrimitive.int)
        assertEquals(3, b["tageAktiv"]!!.jsonPrimitive.int)
        assertEquals(71, b["richtigProzent"]!!.jsonPrimitive.int)
        val st = b["staerken"] as JsonArray; val sw = b["schwach"] as JsonArray
        assertEquals("einmaleins", (st[0] as JsonObject)["thema"]!!.jsonPrimitive.content)
        assertEquals("Sachunterricht", (sw[0] as JsonObject)["titel"]!!.jsonPrimitive.content)
        assertEquals(33, (sw[0] as JsonObject)["prozent"]!!.jsonPrimitive.int)
        val f = (b["fortschritt"] as JsonArray)[0] as JsonObject
        assertEquals(4, f["geschafft"]!!.jsonPrimitive.int); assertEquals(10, f["von"]!!.jsonPrimitive.int)
        val ka = b["klassenarbeit"] as JsonObject
        assertEquals("Mathe: Einmaleins", ka["thema"]!!.jsonPrimitive.content)
        assertEquals(ElternKopplung.isoTag(heute + 5), ka["tag"]!!.jsonPrimitive.content)
        assertEquals(3, (b["letzte"] as JsonArray).size)
    }

    @Test fun berichtBleibtKlein() {
        val t = ElternKopplung.berichtText(bericht())
        assertTrue(t.encodeToByteArray().size < ElternKopplung.MAX_BYTES * 70 / 100)
        Json.parseToJsonElement(t)
    }

    @Test fun linkLesen() {
        val id = "A".repeat(22); val key = "b_-".repeat(14) + "c"
        assertEquals(id to key, ElternKopplung.ausLink("https://www.mathenachhilfe-online.com/eltern-bericht/#$id.$key"))
        assertEquals(id to key, ElternKopplung.ausLink("  $id.$key "))
        assertNull(ElternKopplung.ausLink("https://www.mathenachhilfe-online.com/eltern-bericht/#kaputt"))
    }

    @Test fun monatsverlauf() {
        val a = listOf(ElternKopplung.Antwort("x", jetzt, true), ElternKopplung.Antwort("x", jetzt, false), ElternKopplung.Antwort("x", jetzt - 40 * day, true))
        val m = ElternKopplung.monatsSummen(a, utc)
        assertEquals(2, m.size)
        // gespeicherter älterer Monat mit mehr Aufgaben bleibt, auch wenn die Rohdaten gekürzt wurden
        val alt = mapOf("2020-01" to ElternKopplung.Monat(50, 40), m.keys.first() to ElternKopplung.Monat(1, 1))
        val z = ElternKopplung.monateZusammen(alt, m)
        assertEquals(3, z.size); assertEquals(50, z["2020-01"]!!.aufgaben)
        val v = ElternKopplung.bericht(null, 3, jetzt, heute, emptyList(), emptyList(), emptyList(), emptyList(), null, utc, z)["verlauf"] as JsonArray
        assertEquals("2020-01", (v[0] as JsonObject)["monat"]!!.jsonPrimitive.content)
        assertEquals(80, (v[0] as JsonObject)["richtigProzent"]!!.jsonPrimitive.int)
    }

    @Test fun sicherungsHuelle() {
        val t = ElternKopplung.sicherung("2.2.15", jetzt, "S".repeat(22), kotlinx.serialization.json.JsonObject(emptyMap()))
        val j = Json.parseToJsonElement(t) as JsonObject
        assertEquals(setOf("format", "app", "v", "stand", "schreib", "daten"), j.keys)
        assertEquals("wissensgarten", j["app"]!!.jsonPrimitive.content)
    }
}
