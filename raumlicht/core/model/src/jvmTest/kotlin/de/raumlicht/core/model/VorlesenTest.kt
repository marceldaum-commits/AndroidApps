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

    /** Dateiname wie auf dem Server: Prüfwert aus der Absprache, SHA-256 gegen die Java-Fassung */
    @Test fun keyMatchesTheServer() {
        assertEquals("cb1e6d67882767042ebaa54d163e450c", Vorlesen.key("Hallo Welt"))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/cb/cb1e6d67882767042ebaa54d163e450c.mp3", Vorlesen.url("Hallo Welt"))
        val r = kotlin.random.Random(3)
        for (n in listOf(0, 1, 55, 56, 63, 64, 65, 119, 120, 1000)) { val b = r.nextBytes(n)
            assertEquals(java.security.MessageDigest.getInstance("SHA-256").digest(b).toList(), Sha256.digest(b).toList()) }
        Vorlesen.texts().take(50).forEach { t -> assertEquals(java.security.MessageDigest.getInstance("SHA-256").digest("nova|de|$t".toByteArray()).joinToString("") { "%02x".format(it) }.take(32), Vorlesen.key(t)) }
        // vorlesefertig: derselbe Text wie in der Liste
        assertEquals("Hallo Welt", Vorlesen.spoken("  Hallo\n Welt "))
    }

    @Test fun liveRequestCarriesOnlyTextAndLanguage() {
        assertEquals("""{"text":"Vektor a gleich 1, 2, 3","sprache":"de"}""", Vorlesen.liveBody("Vektor a gleich 1, 2, 3"))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/ab/abc.mp3", Vorlesen.liveUrl("""{"url":"https://stimmen.mathenachhilfe-online.com/nova/ab/abc.mp3"}"""))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/ab/abc.mp3", Vorlesen.liveUrl("""{"url":"/nova/ab/abc.mp3"}"""))
        assertEquals(null, Vorlesen.liveUrl("""{"url":"https://fremd.example/x.mp3"}""")); assertEquals(null, Vorlesen.liveUrl("kein json")); assertEquals(null, Vorlesen.liveUrl("{}"))
    }

    @Test fun tempoHasSixSteps() {
        assertEquals(listOf(75, 90, 100, 115, 130, 150), Vorlesen.TEMPOS)
        assertEquals(listOf("0,75×", "0,9×", "1,0×", "1,15×", "1,3×", "1,5×"), Vorlesen.TEMPOS.map(Vorlesen::tempoLabel))
        assertEquals(100, Vorlesen.tempo(100)); assertEquals(115, Vorlesen.tempo(120)); assertEquals(75, Vorlesen.tempo(10)); assertEquals(150, Vorlesen.tempo(999))
    }
}
