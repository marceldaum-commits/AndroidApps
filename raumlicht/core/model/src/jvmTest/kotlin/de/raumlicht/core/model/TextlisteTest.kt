package de.raumlicht.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Textliste für das Tonstudio im „Format 1“ (Muster: quasselquest/VERTONUNG.md): alle festen Vorlese-Texte der App, nur Deutsch, nur Tempo normal.
 * Der Test schreibt `raumlicht/vorlesen/textliste.json` neu (nach jeder Inhaltsänderung mit `./gradlew :core:model:jvmTest` erzeugen und mit einchecken)
 * und prüft ihren Aufbau. Vorlesen selbst bleibt aus ([Vorlesen.ENABLED]).
 */
class TextlisteTest {
    private fun id(sprache: String, sprechtext: String): String {
        val n = Normalizer.normalize(sprechtext, Normalizer.Form.NFC).replace(Regex("\\s+"), " ").trim()
        return MessageDigest.getInstance("SHA-1").digest("$sprache|$n".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(20)
    }

    private fun build(): Pair<JsonObject, Int> {
        val seen = LinkedHashMap<String, String>()               // sprechtext → Text, wie er in der App steht
        (Vorlesen.raw() + Vorlesen.predictableRaw()).forEach { raw -> val s = Vorlesen.spoken(raw); if (s.isNotEmpty() && s.length <= Vorlesen.MAX_CHARS && s !in seen) seen[s] = raw }
        val eintraege = seen.map { (s, raw) ->
            val i = id("de", s)
            JsonObject(mapOf("id" to JsonPrimitive(i), "sprache" to JsonPrimitive("de"), "text" to JsonPrimitive(raw), "sprechtext" to JsonPrimitive(s),
                "rollen" to JsonArray(listOf(JsonPrimitive("lux"))),
                "fassungen" to JsonArray(listOf(JsonObject(mapOf("art" to JsonPrimitive("normal"), "speed" to JsonPrimitive(1.0), "datei" to JsonPrimitive("audio/server/de/$i.mp3")))))))
        }
        val root = JsonObject(mapOf(
            "format" to JsonPrimitive(1), "app" to JsonPrimitive("raumlicht"), "stand" to JsonPrimitive("2026-11-25"),
            "stimmen" to JsonObject(mapOf("de" to JsonObject(mapOf("stimme" to JsonNull, "hinweis" to JsonPrimitive("Stimme „de-1“, vom Leitstand/Marcel gewählt; Tempo normal 1,0, keine langsame Fassung (Raumlicht stellt das Tempo am Gerät ein)")))),
            ),
            "mp3" to JsonObject(mapOf("kanaele" to JsonPrimitive(1), "id3_comment" to JsonObject(mapOf("de" to JsonPrimitive("KI-generiert (synthetische Stimme)"))), "stille_rand_kuerzen" to JsonPrimitive(true))),
            "hinweis" to JsonPrimitive("Zahlen, Formeln und Symbole sind im sprechtext schon ausgeschrieben (Speech.clean). Vorlesen ist in der App noch aus."),
            "eintraege" to JsonArray(eintraege)))
        return root to eintraege.size
    }

    @Test fun textlisteSchreibenUndPruefen() {
        val (root, n) = build()
        val file = File("../../vorlesen/textliste.json").also { it.parentFile.mkdirs() }
        file.writeText(Json { prettyPrint = true; prettyPrintIndent = " " }.encodeToString(JsonObject.serializer(), root) + "\n")
        val e = (root["eintraege"] as JsonArray).map { it as JsonObject }
        assertTrue("mindestens ein paar hundert Texte", n > 300)
        assertEquals("ids eindeutig", n, e.map { (it["id"] as JsonPrimitive).content }.toSet().size)
        e.forEach { x ->
            val s = (x["sprechtext"] as JsonPrimitive).content
            assertTrue(s, !Regex("[0-9₀-₉²³⃗→⇒∫·×√°]").containsMatchIn(s))        // nichts Ungesprochenes: Ziffern und Symbole sind ausgeschrieben
        }
        assertTrue("Vorlesen bleibt aus", !Vorlesen.ENABLED)
    }
}
