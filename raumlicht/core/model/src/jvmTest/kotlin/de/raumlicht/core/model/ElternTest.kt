package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

/** „Eltern-Handy koppeln“: Kopplungswerte, Link, Lern-Protokoll und der Bericht – nur das, was angekündigt ist. */
class ElternTest {
    private val id = "A".repeat(22); private val write = "b-_".repeat(7) + "c"; private val key = "K9".repeat(21) + "x"

    @Test fun kopplungUndLink() {
        val p = Eltern.Pairing.fresh("$id.$write.$key", "  Lena.M  ")!!
        assertEquals(id, p.id); assertEquals(write, p.write); assertEquals(key, p.key); assertEquals("Lena M", p.name)
        assertEquals(p.encode(), Eltern.Pairing.decode(p.encode())!!.encode())
        assertEquals("https://www.mathenachhilfe-online.com/eltern-bericht/#$id.$key", Eltern.link(p)); assertEquals("https://staging.mathenachhilfe-online.com/eltern-bericht/#$id.$key", Eltern.link(p, Eltern.TEST_PAGE))
        assertFalse("das Schreib-Geheimnis steht nie im Link", Eltern.link(p).contains(write))
        assertEquals("https://trainer.mathenachhilfe-online.com/eltern/v1/b/$id", Eltern.uploadUrl(p)); assertFalse("der Schlüssel steht nie in der Adresse zum Server", Eltern.uploadUrl(p).contains(key))
        for (bad in listOf("", "a.b.c", "$id.$write", "$id.$write.${key.dropLast(1)}", "${id.dropLast(1)}!.$write.$key")) assertNull(bad, Eltern.Pairing.decode(bad))
        assertNotNull("der Link passt in den QR-Code", Qr.encode(Eltern.link(p, Eltern.TEST_PAGE)))
    }

    @Test fun lernProtokoll() {
        var log = ""
        log = Eltern.add(log, 100, min = 1); log = Eltern.add(log, 100, min = 1); log = Eltern.add(log, 100, tasks = 1, right = 1); log = Eltern.add(log, 100, tasks = 1); log = Eltern.add(log, 103, min = 5, tasks = 2, right = 2)
        assertEquals(listOf(Eltern.Day(100, 2, 2, 1), Eltern.Day(103, 5, 2, 2)), Eltern.log(log))
        assertEquals("nach 60 Tagen fällt ein Tag heraus", listOf(163L), Eltern.log(Eltern.add(log, 163, min = 1)).map { it.day })
        assertEquals(emptyList<Eltern.Day>(), Eltern.log("unsinn;1:2")); assertEquals("2026-11-18", Eltern.isoDay(java.time.LocalDate.of(2026, 11, 18).toEpochDay()))
        assertEquals("2026-11-18T15:40:07Z", Eltern.iso(java.time.Instant.parse("2026-11-18T15:40:07Z").toEpochMilli())); assertEquals("1970-01-01T00:00:00Z", Eltern.iso(0))
    }

    @Test fun berichtEnthaeltNurWasAngekuendigtIst() {
        val today = java.time.LocalDate.of(2026, 11, 18).toEpochDay()
        val geo = Levels.chapters.filter { it.area == Area.GEOMETRIE }.flatMap { it.levels }; val ana = Levels.chapters.filter { it.area == Area.ANALYSIS }.flatMap { it.levels }
        val stars = geo.take(8).associate { it.id to 1 } + ana.take(6).associate { it.id to 3 }
        var log = Eltern.add("", today - 20, min = 30, tasks = 9, right = 9); log = Eltern.add(log, today - 2, min = 12, tasks = 4, right = 1); log = Eltern.add(log, today, min = 8, tasks = 4, right = 3)
        val o = Json.parseToJsonElement(Eltern.report("Lena", stars, log, Plan(today + 5, setOf(1, 2)), today, java.time.Instant.parse("2026-11-18T15:40:07Z").toEpochMilli())).jsonObject
        assertEquals(setOf("format", "app", "appName", "name", "stand", "zeitraum", "lernzeitMin", "tageAktiv", "aufgaben", "richtigProzent", "staerken", "schwach", "fortschritt", "letzte", "klassenarbeit", "hinweis"), o.keys)
        assertEquals("1", o["format"]!!.jsonPrimitive.content); assertEquals("raumlicht", o["app"]!!.jsonPrimitive.content); assertEquals("Raumlicht", o["appName"]!!.jsonPrimitive.content)
        assertEquals("Lena", o["name"]!!.jsonPrimitive.content); assertEquals("2026-11-18T15:40:07Z", o["stand"]!!.jsonPrimitive.content)
        // nur die letzten 14 Tage: der Tag vor 20 Tagen zählt nicht
        assertEquals("20", o["lernzeitMin"]!!.jsonPrimitive.content); assertEquals("2", o["tageAktiv"]!!.jsonPrimitive.content); assertEquals("8", o["aufgaben"]!!.jsonPrimitive.content); assertEquals("50", o["richtigProzent"]!!.jsonPrimitive.content)
        assertEquals(listOf("analysis"), o["staerken"]!!.jsonArray.map { it.jsonObject["thema"]!!.jsonPrimitive.content }); assertEquals(listOf("vektoren"), o["schwach"]!!.jsonArray.map { it.jsonObject["thema"]!!.jsonPrimitive.content })
        assertEquals("33", o["schwach"]!!.jsonArray[0].jsonObject["prozent"]!!.jsonPrimitive.content)
        assertEquals(listOf("Vektoren und Geometrie", "Analysis", "Stochastik"), o["fortschritt"]!!.jsonArray.map { it.jsonObject["titel"]!!.jsonPrimitive.content })
        assertEquals("8", o["fortschritt"]!!.jsonArray[0].jsonObject["geschafft"]!!.jsonPrimitive.content); assertEquals(geo.size.toString(), o["fortschritt"]!!.jsonArray[0].jsonObject["von"]!!.jsonPrimitive.content)
        assertEquals(listOf("2026-11-16", "2026-11-18"), o["letzte"]!!.jsonArray.map { it.jsonObject["tag"]!!.jsonPrimitive.content })
        assertEquals("2026-11-23", o["klassenarbeit"]!!.jsonObject["tag"]!!.jsonPrimitive.content); assertEquals("vektoren", o["klassenarbeit"]!!.jsonObject["thema"]!!.jsonPrimitive.content)
        // keine Aufgaben-Kennungen, keine Codes, keine Geräte-Kennung
        val text = o.toString(); for (bad in listOf("\"id\"", "Bodenmarke", "RL-", "geraet", "kiCode", "proCode")) assertFalse(bad, text.contains(bad))
        assertTrue(text.length < 24_000)
        // ohne Namen, ohne Plan, ohne gespielte Aufgaben: die Pflichtfelder stehen trotzdem da
        val leer = Json.parseToJsonElement(Eltern.report("", emptyMap(), "", null, today, 0)).jsonObject
        assertFalse("name" in leer); assertFalse("klassenarbeit" in leer); assertFalse("richtigProzent" in leer); assertTrue(listOf("format", "app", "appName", "stand").all { it in leer })
    }

    @Test fun kommtBaldOhneDatumUndPreis() {
        assertTrue(Soon.topics.size in 3..8); for (t in Soon.topics) { for (bad in listOf("€", "Pro", "202", "Januar", "nächste Woche")) assertFalse(t.title + bad, (t.title + t.sub).contains(bad)) }
        assertEquals(Soon.topics.size, Soon.topics.map { it.title }.toSet().size)
    }
}
