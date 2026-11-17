package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

/** Die Liste für den Stimmen-Server: vollständig, im vereinbarten Format, jeder Text vorlesefertig. */
class VorlesenTest {
    @Test fun listHasTheAgreedFormat() {
        val root = Json.parseToJsonElement(Vorlesen.json()).jsonObject
        assertEquals(1, root["format"]!!.jsonPrimitive.content.toInt()); assertEquals("raumlicht", root["app"]!!.jsonPrimitive.content)
        val texts = root["texte"]!!.jsonArray.map { it.jsonObject }
        assertTrue("zu wenige Texte: ${texts.size}", texts.size > 600)
        assertTrue(texts.all { it["sprache"]!!.jsonPrimitive.content == "de" })
        val all = texts.map { it["text"]!!.jsonPrimitive.content }
        assertEquals("jeder Text nur einmal", all.size, all.toSet().size)
        all.forEach { t ->
            assertTrue("zu lang: $t", t.length <= Vorlesen.MAX_CHARS)
            assertEquals("Leerraum: $t", Vorlesen.normalize(t), t)
            assertEquals("nicht NFC: $t", Normalizer.normalize(t, Normalizer.Form.NFC), t)
            // vorlesefertig: keine Formelzeichen und kein Markup mehr
            assertTrue("Zeichen, die keine Stimme lesen kann: $t", t.none { it in "⃗→·√²³∫≈≤≥±|*_#<>{}[]" })
        }
        // bekannte feste Texte sind dabei
        assertTrue("Hallo, ich bin Lux!" in all); assertTrue(all.any { it.startsWith("Hi, ich bin Lux, deine Lichtdrohne.") })
    }
}
