package de.kurvenflug.core.model

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParentReportTest {
    private val day = Weekly.DAY
    private val today = 20_500L * day - 60 * 60_000L   // Mitternacht Ortszeit bei UTC+1
    private val now = today + 16 * 3_600_000L + 30 * 60_000L

    @Test fun formatMatchesSpec() {
        val plays = (0 until 6).map { PlayRecord("2-${it + 1}", if (it < 2) 0 else 3, 2, 300, today - it * day + 3_600_000L) } +
            (0 until 4).map { PlayRecord("15-${it + 1}", 3, 1, 120, today + 7_200_000L) } +
            listOf(PlayRecord("3-1", 0, 3, 400, today - 20 * day))   // älter als 14 Tage
        val j = Json.parseToJsonElement(ParentReport.json(plays, mapOf("2-1" to 3, "2-3" to 3), now, today, 60, 7, "Lena \"L\"")).jsonObject
        assertEquals(1, j["format"]!!.jsonPrimitive.int); assertEquals("kurvenflug", j["app"]!!.jsonPrimitive.content)
        assertEquals("KurvenFlug", j["appName"]!!.jsonPrimitive.content); assertEquals("Lena \"L\"", j["name"]!!.jsonPrimitive.content)
        assertEquals("2026-02-15T16:30:00+01:00".length, j["stand"]!!.jsonPrimitive.content.length)
        assertTrue(j["stand"]!!.jsonPrimitive.content.endsWith("T16:30:00+01:00"))
        assertEquals(10, j["aufgaben"]!!.jsonPrimitive.int); assertEquals(80, j["richtigProzent"]!!.jsonPrimitive.int)
        assertEquals(7, j["klasse"]!!.jsonPrimitive.int); assertEquals(6, j["tageAktiv"]!!.jsonPrimitive.int)
        val strong = j["staerken"]!!.jsonArray.map { it.jsonObject["thema"]!!.jsonPrimitive.content }
        assertEquals(listOf("brueche"), strong)
        val weak = j["schwach"]!!.jsonArray
        assertTrue(weak.isEmpty())   // Geraden 4/6 = 67 % → weder stark noch schwach
        val f = j["fortschritt"]!!.jsonArray.first().jsonObject
        assertTrue(f["titel"]!!.jsonPrimitive.content.startsWith("Kapitel "))
        assertTrue(j["letzte"]!!.jsonArray.first().jsonObject["tag"]!!.jsonPrimitive.content.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        // nur Themen aus der gemeinsamen Liste, keine Gerätedaten
        (j["staerken"]!!.jsonArray + j["schwach"]!!.jsonArray).forEach { assertTrue(it.jsonObject["thema"]!!.jsonPrimitive.content in ParentOffers.ALLOWED_TOPICS) }
        assertTrue(listOf("email", "geraet", "ort", "standort").none { it in j.keys })
    }

    @Test fun weakTopicGetsTip() {
        val plays = (0 until 5).map { PlayRecord("2-${it + 1}", if (it == 0) 3 else 0, 3, 200, today + it * 60_000L) }
        val j = Json.parseToJsonElement(ParentReport.json(plays, emptyMap(), now, today, 60, null, null)).jsonObject
        val w = j["schwach"]!!.jsonArray.single().jsonObject
        assertEquals("lineare-funktionen", w["thema"]!!.jsonPrimitive.content); assertEquals(20, w["prozent"]!!.jsonPrimitive.int)
        assertTrue("Δy" in w["tipp"]!!.jsonPrimitive.content)
        assertTrue("name" !in j && "klasse" !in j && "hinweis" !in j)
        val e = Json.parseToJsonElement(ParentReport.json(plays, emptyMap(), now, today, 60, null, null, examSoon = true)).jsonObject
        assertEquals("Bald steht eine Klassenarbeit an: Lineare Funktionen", e["hinweis"]!!.jsonPrimitive.content)
    }

    @Test fun monthHistoryGrowsAndNeverShrinks() {
        val utc: (Long) -> Int = { 0 }
        val nov = 20_773L * day   // 2026-11-15
        val oct = nov - 30 * day
        val first = MonthSums.merge(emptyList(), listOf(PlayRecord("2-1", 3, 1, 600, oct), PlayRecord("2-2", 0, 2, 600, oct), PlayRecord("2-3", 3, 1, 300, nov)), utc)
        assertEquals(listOf("2026-10", "2026-11"), first.map { it.month }); assertEquals(20, first[0].minutes); assertEquals(50, first[0].percent)
        // Oktober-Durchgänge aufgeräumt → Oktober bleibt, November wächst
        val second = MonthSums.merge(MonthSums.parse(MonthSums.format(first)), listOf(PlayRecord("2-3", 3, 1, 300, nov), PlayRecord("2-4", 3, 1, 300, nov)), utc)
        assertEquals(first[0], second[0]); assertEquals(2, second[1].tasks)
        val j = Json.parseToJsonElement(ParentReport.json(emptyList(), emptyMap(), now, today, 60, null, null, months = second)).jsonObject
        val v = j["verlauf"]!!.jsonArray
        assertEquals("2026-10", v.first().jsonObject["monat"]!!.jsonPrimitive.content); assertEquals(100, v.last().jsonObject["richtigProzent"]!!.jsonPrimitive.int)
        val many = (1..20).map { MonthSum("20${10 + it}-01", 1, 1, 1) }
        assertEquals(12, Json.parseToJsonElement(ParentReport.json(emptyList(), emptyMap(), now, today, 60, null, null, months = many)).jsonObject["verlauf"]!!.jsonArray.size)
    }

    @Test fun emptyIsValid() {
        val j = Json.parseToJsonElement(ParentReport.json(emptyList(), emptyMap(), now, today, 60, null, " ")).jsonObject
        assertEquals(0, j["aufgaben"]!!.jsonPrimitive.int); assertTrue("name" !in j)
        assertTrue(ParentReport.json(List(500) { PlayRecord("2-1", 3, 1, 60, today) }, emptyMap(), now, today, 60, 7, "x").length < 24_000)
    }
}
