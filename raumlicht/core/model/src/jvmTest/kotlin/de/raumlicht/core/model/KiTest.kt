package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

/** KI-Tutor: welche Codes gelten bis wann, was an den Server geht, wie Antworten und Störungen gelesen werden. */
class KiTest {
    // Schlüssel nur für Tests (Seed 0…31); die Codes sind KI-Pässe Nr. 2001 (bis 31.07.2027) und Nr. 2002 (bis 31.07.2026)
    private val pk = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val ki = "RL-04208-0000Z-8GNM5-HJFN6-SDW5Y-QPW2T-BSSPN-9DS5K-7Z3A1-PTFJ2-ACM3Q-GX1H4-ZM8EV-3GR33-K3N31-3VN22-58C7D-HP8Y1-1321W-HG25S-PNJ23-FSKM6-JYD9R-GJ"
    private val alt = "RL-04208-0000Z-90JRS-KVK76-JC5YR-K5AB8-66P5J-B9FQ0-J8M2Q-N9CCS-9YCEM-0XX2C-KCRBZ-8QGFP-J83ZY-RYKBJ-8WK56-H6461-JQTF0-1PN3P-K7Q9A-337T4-N8R7X-GE"
    private val pro = "RL-04202-0000F-MGKKT-CDRGB-BYPG5-8XEVQ-M0F02-94P76-M1MXZ-28QYX-86PN1-NEY0T-JNXA5-8S0GJ-HA3X2-C0GQW-AW2K1-BQN3M-4PV5F-ZHQBG-ZKMPE-KJSDT-6VARJ-0W"

    /** Solange Kasse und Server-Route fehlen, bleibt der KI-Tutor für alle anderen unsichtbar – beides trägt der Leitstand nach. */
    @Test fun nochNichtFreigegeben() {
        assertFalse(Ki.ENABLED); assertEquals("", Ki.CHECKOUT_URL); assertEquals("14,99 €", Ki.PRICE); assertEquals(10, Ki.DAILY); assertEquals(3, Ki.TRIALS)
        assertEquals("https://trainer.mathenachhilfe-online.com/api/raumlicht/ki", Ki.URL); assertEquals("https://trainer-test.mathenachhilfe-online.com/api/raumlicht/ki", Ki.TEST_URL)
    }

    @Test fun passGiltBisEinschliesslich31Juli() {
        val i = Ki.info(ki, pk)!!
        assertEquals(License.PRODUCT_KI, i.product); assertEquals(2001L, i.serial); assertEquals("31.07.2027", Ki.date(Ki.lastDay(i)))
        assertEquals(java.time.LocalDate.of(2027, 7, 31).toEpochDay(), Ki.lastDay(i))
        assertTrue(Ki.active(ki, Ki.lastDay(i), pk)); assertFalse("am 1. August ist Schluss", Ki.active(ki, Ki.lastDay(i) + 1, pk))
        assertFalse(Ki.active(alt, java.time.LocalDate.of(2026, 11, 18).toEpochDay(), pk)); assertEquals("31.07.2026", Ki.date(Ki.lastDay(Ki.info(alt, pk)!!)))
    }

    @Test fun datum() {
        for (d in listOf("1970-01-01", "2000-02-29", "2024-02-29", "2026-11-18", "2027-12-31", "2029-07-31", "2100-03-01")) {
            val day = java.time.LocalDate.parse(d)
            assertEquals(day.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")), Ki.date(day.toEpochDay()))
        }
    }

    @Test fun kiUndProSindGetrennteCodes() {
        assertNull("Pro-Code ist kein KI-Code", Ki.info(pro, pk)); assertFalse("KI-Code schaltet Pro nicht frei", Pro.active(ki, pk))
        assertNull("mit dem Test-Schlüssel erzeugt – der echte nimmt ihn nicht", Ki.info(ki)); assertNull(Ki.info("", pk)); assertNull(Ki.info(null, pk))
        assertNull(Ki.info(ki.substring(0, 20) + (if (ki[20] == 'A') 'B' else 'A') + ki.substring(21), pk))
    }

    @Test fun codeAusLinkUndMail() {
        val plain = ki.replace("-", "")
        assertEquals(ki, Ki.fromText(plain, pk)); assertEquals(ki, Ki.fromText(plain.removePrefix("RL"), pk)); assertEquals(ki, Ki.fromText(plain.lowercase(), pk))
        assertEquals(ki, Ki.fromText("Hallo!\nDein KI-Abi-Pass:\n$ki\nViel Erfolg!", pk)); assertEquals(ki, Ki.fromText("https://raumlicht.mathenachhilfe-online.com/?ki=$plain", pk))
        assertEquals("abgelaufene Codes werden erkannt – die Seite sagt dann, dass sie abgelaufen sind", alt, Ki.fromText(alt, pk))
        assertNull(Ki.fromText(pro, pk)); assertNull(Ki.fromText("", pk)); assertNull(Ki.fromText("RL-12345", pk)); assertNull(Ki.fromText(plain))
    }

    @Test fun anfrage() {
        val level = Levels.byId("1-2")!!
        val history = (1..9).map { Ki.Turn(if (it % 2 == 0) "lux" else "schueler", "Satz $it") }
        val o = Json.parseToJsonElement(Ki.request(level, level.start, "Knapp daneben.", Ki.Kind.QUESTION, "  " + "x".repeat(400), history, ki, "abc123", "2.19.0")).jsonObject
        assertEquals("raumlicht", o["app"]!!.jsonPrimitive.content); assertEquals("2.19.0", o["version"]!!.jsonPrimitive.content); assertEquals("rueckfrage", o["art"]!!.jsonPrimitive.content)
        val a = o["aufgabe"]!!.jsonObject
        assertEquals("1-2", a["id"]!!.jsonPrimitive.content); assertEquals(level.mission, a["auftrag"]!!.jsonPrimitive.content); assertEquals(level.tip, a["tipp"]!!.jsonPrimitive.content)
        assertEquals(level.way, a["rechenweg"]!!.jsonArray.map { it.jsonPrimitive.content }); assertTrue(a["formel"]!!.jsonPrimitive.content.isNotBlank())
        assertEquals(Ki.input(level, level.start), o["eingabe"]!!.jsonPrimitive.content); assertEquals("Knapp daneben.", o["meldung"]!!.jsonPrimitive.content)
        assertEquals("Fragen werden auf 300 Zeichen gekürzt", 300, o["frage"]!!.jsonPrimitive.content.length)
        assertEquals("nur die letzten sechs Wortwechsel gehen mit", (4..9).map { "Satz $it" }, o["verlauf"]!!.jsonArray.map { it.jsonObject["text"]!!.jsonPrimitive.content })
        assertEquals("abc123", o["geraet"]!!.jsonPrimitive.content); assertEquals(ki, o["code"]!!.jsonPrimitive.content)
        assertEquals("erklaerung", Json.parseToJsonElement(Ki.request(level, level.start, "", Ki.Kind.EXPLAIN, "", emptyList(), "", "abc", "1")).jsonObject["art"]!!.jsonPrimitive.content)
    }

    /** Jede Aufgabe lässt sich in Worte fassen – ohne Absturz und ohne leere Eingabe */
    @Test fun eingabeInWorten() {
        for (l in Levels.all) { assertTrue(l.id, Ki.input(l, l.start).contains(" = ")); assertTrue(l.id, Ki.formula(l, l.start).isNotBlank()) }
        val l = Levels.all.first { it.controls.any { c -> c.size == 3 } }; assertTrue(Ki.input(l, l.start), Regex("""\(-?\d+\|-?\d+\|-?\d+\)""").containsMatchIn(Ki.input(l, l.start)))
    }

    @Test fun antwortUndStoerungen() {
        Ki.answer(200, """{"text":"  Stell dir die Ebene als Tischplatte vor. ","rest":7}""").let { assertEquals(200, it.status); assertEquals("Stell dir die Ebene als Tischplatte vor.", it.text); assertEquals(7, it.rest) }
        Ki.answer(402, """{"fehler":"proben","rest":0}""").let { assertEquals("", it.text); assertEquals(0, it.rest) }
        Ki.answer(502, "<html>Bad Gateway</html>").let { assertEquals("", it.text); assertNull(it.rest) }; Ki.answer(0, "").let { assertEquals("", it.text); assertNull(it.rest) }
        assertTrue(Ki.problem(402, false).contains("3 Proben")); assertTrue(Ki.problem(402, true).contains("gilt nicht mehr")); assertTrue(Ki.problem(429, true).contains("Morgen"))
        assertTrue(Ki.problem(0, false).contains("Internet")); assertTrue(Ki.problem(502, true).contains("später")); assertTrue(Ki.problem(404, false).contains("später"))
    }

    @Test fun datenschutzSatzNenntAllesWasGesendetWird() {
        for (w in listOf("trainer.mathenachhilfe-online.com", "Anthropic", "Aufgabe", "Eingabe", "Frage", "KI-Code", "Geräte-Kennung", "keine Namen", "keine Lernstände")) assertTrue(w, Ki.PRIVACY.contains(w))
    }
}
