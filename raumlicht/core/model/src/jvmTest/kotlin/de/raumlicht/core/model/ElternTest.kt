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

    /** Die Eltern-Seite steht: Koppeln ist für alle da, und der Link zeigt auf die Website (auf der Testseite auf deren Vorstufe). */
    @Test fun fuerAlle() { assertTrue(Eltern.ENABLED); assertTrue(Eltern.PAGE.startsWith("https://www.mathenachhilfe-online.com/")); assertTrue(Eltern.SERVER.startsWith("https://trainer.mathenachhilfe-online.com/")) }

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
        val geo = Levels.all
        val stars = geo.take(8).associate { it.id to 1 }
        var log = Eltern.add("", today - 20, min = 30, tasks = 9, right = 9); log = Eltern.add(log, today - 2, min = 12, tasks = 4, right = 1); log = Eltern.add(log, today, min = 8, tasks = 4, right = 3)
        val o = Json.parseToJsonElement(Eltern.report("Lena", stars, log, Plan(today + 5, setOf(1, 2)), today, java.time.Instant.parse("2026-11-18T15:40:07Z").toEpochMilli())).jsonObject
        assertEquals(setOf("format", "app", "appName", "name", "stand", "zeitraum", "lernzeitMin", "tageAktiv", "aufgaben", "richtigProzent", "staerken", "schwach", "fortschritt", "letzte", "verlauf", "klassenarbeit", "hinweis"), o.keys)
        assertEquals("1", o["format"]!!.jsonPrimitive.content); assertEquals("raumlicht", o["app"]!!.jsonPrimitive.content); assertEquals("Raumlicht", o["appName"]!!.jsonPrimitive.content)
        assertEquals("Lena", o["name"]!!.jsonPrimitive.content); assertEquals("2026-11-18T15:40:07Z", o["stand"]!!.jsonPrimitive.content)
        // nur die letzten 14 Tage: der Tag vor 20 Tagen zählt nicht
        assertEquals("20", o["lernzeitMin"]!!.jsonPrimitive.content); assertEquals("2", o["tageAktiv"]!!.jsonPrimitive.content); assertEquals("8", o["aufgaben"]!!.jsonPrimitive.content); assertEquals("50", o["richtigProzent"]!!.jsonPrimitive.content)
        assertEquals(emptyList<String>(), o["staerken"]!!.jsonArray.map { it.jsonObject["thema"]!!.jsonPrimitive.content }); assertEquals(listOf("vektoren"), o["schwach"]!!.jsonArray.map { it.jsonObject["thema"]!!.jsonPrimitive.content })
        assertEquals("33", o["schwach"]!!.jsonArray[0].jsonObject["prozent"]!!.jsonPrimitive.content)
        assertEquals(listOf("Vektoren und Geometrie"), o["fortschritt"]!!.jsonArray.map { it.jsonObject["titel"]!!.jsonPrimitive.content })
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

    // ── Sicherung des Lernstands ──
    private val state = mapOf("sterne" to "1-1=3;1-2=2", "proCode" to "RL-PRO", "kiCode" to "RL-KI", "lernMonate" to "2026-11:40:12:9", "ton" to "0", "diary" to "x".repeat(50),
        "kiDevice" to "geraet123", "elternKopplung" to "geheim", "elternGesendet" to "5", "elternSicherung" to "7", "sparsam" to "1", "mitschrift" to "…")

    @Test fun sicherungOhneGeraeteKennungenAberMitSchreibrecht() {
        val p = Eltern.Pairing.fresh("$id.$write.$key", "Lena")!!
        val json = Eltern.backup(p, "2.18.27", 1_795_016_400_000L, state)!!; val o = Json.parseToJsonElement(json).jsonObject
        assertEquals("1", o["format"]!!.jsonPrimitive.content); assertEquals("raumlicht", o["app"]!!.jsonPrimitive.content); assertEquals("2.18.27", o["v"]!!.jsonPrimitive.content)
        assertEquals(write, o["schreib"]!!.jsonPrimitive.content); assertTrue(Regex("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ").matches(o["stand"]!!.jsonPrimitive.content))
        val d = o["daten"]!!.jsonObject
        for (k in listOf("sterne", "proCode", "kiCode", "lernMonate", "ton", "diary")) assertEquals(k, state[k], d[k]!!.jsonPrimitive.content)
        for (k in listOf("kiDevice", "elternKopplung", "elternGesendet", "elternSicherung", "sparsam", "mitschrift")) assertNull(k, d[k])
        assertFalse("weder Geräte-Kennung noch Schlüssel in der Sicherung", json.contains("geraet123") || json.contains(key) || json.contains("geheim"))
        assertEquals("https://trainer.mathenachhilfe-online.com/eltern/v1/s/$id", Eltern.backupUrl(id)); assertEquals("https://trainer-test.mathenachhilfe-online.com/eltern/v1/s/$id", Eltern.backupUrl(id, Eltern.TEST_SERVER))
    }

    @Test fun rundlaufSicherungWiederherstellen() {
        val p = Eltern.Pairing.fresh("$id.$write.$key", "Lena")!!
        val r = Eltern.restore(Eltern.backup(p, "2.18.27", 1_795_016_400_000L, state)!!, id, key) as Eltern.Restore.Ok
        assertEquals(state.filterKeys(Eltern::saved), r.data); assertEquals(p.encode(), r.pairing.encode()); assertEquals(10, Eltern.germanDay(r.stand).length)
        assertTrue(Eltern.restore("""{"format":1,"app":"wissensgarten","schreib":"$write","daten":{}}""", id, key) is Eltern.Restore.OtherApp)
        for (bad in listOf("", "kein json", """{"format":2,"app":"raumlicht","schreib":"$write","daten":{}}""", """{"format":1,"app":"raumlicht","schreib":"kurz","daten":{}}""", """{"format":1,"app":"raumlicht","schreib":"$write"}"""))
            assertTrue(bad, Eltern.restore(bad, id, key) is Eltern.Restore.Broken)
        // Eine Sicherung kann keine Geräte-Kennung und keine fremde Kopplung einschleusen
        val evil = Eltern.restore("""{"format":1,"app":"raumlicht","schreib":"$write","daten":{"kiDevice":"x","elternKopplung":"y","sterne":"1-1=3"}}""", id, key) as Eltern.Restore.Ok
        assertEquals(mapOf("sterne" to "1-1=3"), evil.data)
    }

    @Test fun kopplungsLinkLesen() {
        assertEquals(id to key, Eltern.fromLink("  https://www.mathenachhilfe-online.com/eltern-bericht/#$id.$key \n")); assertEquals(id to key, Eltern.fromLink("$id.$key"))
        for (bad in listOf("", "https://www.mathenachhilfe-online.com/eltern-bericht/", "#$id", "#$id.${key.dropLast(1)}", "#$id.$write.$key", "#${id.dropLast(1)}!.$key")) assertNull(bad, Eltern.fromLink(bad))
    }

    @Test fun sicherungNurBeiAenderungUndNieZuGross() {
        assertEquals(Eltern.mark(state), Eltern.mark(state.entries.reversed().associate { it.key to it.value })); assertNotEquals(Eltern.mark(state), Eltern.mark(state + ("sterne" to "1-1=3")))
        val p = Eltern.Pairing.fresh("$id.$write.$key", "")!!
        assertEquals(16 + 1 + 24, Eltern.sealedLength("ab"))   // 2 + 16 Byte → 24 Zeichen base64url
        // zu groß: erst das Tagebuch weglassen …
        val big = Eltern.backup(p, "2.18.27", 0, state + ("diary" to "ä".repeat(300_000)))!!
        assertFalse(big.contains("diary")); assertTrue(big.contains("sterne")); assertTrue(Eltern.sealedLength(big) <= Eltern.MAX_BACKUP)
        // … und wenn es dann immer noch nicht passt, gar nichts senden
        assertNull(Eltern.backup(p, "2.18.27", 0, state + ("sterne" to "x".repeat(600_000))))
    }

    @Test fun monatssummenUndVerlaufImBericht() {
        var m = ""; m = Eltern.addMonth(m, 20_772L, min = 10, tasks = 4, right = 3); m = Eltern.addMonth(m, 20_772L, tasks = 4, right = 1); m = Eltern.addMonth(m, 20_772L - 30, min = 5)
        assertEquals("2026-10:5:0:0;2026-11:10:8:4", m); assertEquals(m, Eltern.months(m).joinToString(";") { "${it.month}:${it.min}:${it.tasks}:${it.right}" })
        assertEquals("2026-10:5:0:0;2026-11:10:8:4", Eltern.monthsFromLog("${20_772L - 30}:5:0:0;20770:4:3:2;20772:6:5:2")); assertEquals("", Eltern.monthsFromLog(""))
        assertTrue(Eltern.months("murks;2026-1:1:1:1;2026-11:a:1:1").isEmpty())
        var many = ""; for (i in 0 until 15) many = Eltern.addMonth(many, 20_772L - 31L * i, min = i + 1, tasks = 2, right = 1)
        val v = Json.parseToJsonElement(Eltern.report("", emptyMap(), "", null, 20_772L, 0, many)).jsonObject["verlauf"]!!.jsonArray
        assertEquals(12, v.size); val months = v.map { it.jsonObject["monat"]!!.jsonPrimitive.content }; assertEquals(months.sorted(), months); assertEquals("2026-11", months.last())
        assertEquals(setOf("monat", "min", "aufgaben", "richtigProzent"), v[0].jsonObject.keys); assertEquals("50", v[0].jsonObject["richtigProzent"]!!.jsonPrimitive.content)
        assertEquals(0, Json.parseToJsonElement(Eltern.report("", emptyMap(), "", null, 20_772L, 0)).jsonObject["verlauf"]!!.jsonArray.size)
    }

    @Test fun vermerkVomKindGeloest() {
        val note = Eltern.releaseNote(1_700_000_000_000L)
        val o = kotlinx.serialization.json.Json.parseToJsonElement(note) as kotlinx.serialization.json.JsonObject
        assertEquals(setOf("v", "status", "app", "appName", "stand"), o.keys); assertEquals("geloest", (o["status"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("raumlicht", (o["app"] as kotlinx.serialization.json.JsonPrimitive).content); assertTrue((o["stand"] as kotlinx.serialization.json.JsonPrimitive).content.matches(Regex("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ")))
        assertTrue("verschlüsselt (iv 16 + ct) höchstens 2000 Zeichen", Eltern.sealedLength(note) <= Eltern.MAX_RELEASE)
    }

    @Test fun vorgemerkterVermerk() {
        val p = Eltern.Pairing.decode("${"A".repeat(22)}.${"B".repeat(22)}.${"C".repeat(43)}.Lea")!!
        assertEquals("${"A".repeat(22)}.${"B".repeat(22)}.${"C".repeat(43)}", Eltern.pending(p)); assertEquals(Triple("A".repeat(22), "B".repeat(22), "C".repeat(43)), Eltern.parsePending(Eltern.pending(p)))
        assertNull(Eltern.parsePending("")); assertNull(Eltern.parsePending("a.b")); assertNull(Eltern.parsePending("a..c"))
        assertFalse("der Merker gehört in keine Sicherung", Eltern.saved("elternLoesen"))
    }
}
