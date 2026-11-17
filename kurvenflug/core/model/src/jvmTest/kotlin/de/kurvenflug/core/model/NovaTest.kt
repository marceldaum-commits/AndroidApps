package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class NovaTest {
    @Test fun checkValueFromLeitstand() {
        assertEquals("cb1e6d67882767042ebaa54d163e450c", Nova.hash("Hallo Welt", "de"))
        assertEquals(Nova.hash("Hallo Welt"), Nova.hash("  Hallo \n  Welt "))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/cb/cb1e6d67882767042ebaa54d163e450c.mp3", Nova.url("Hallo Welt"))
    }

    @Test fun sha256LikeJava() {
        listOf("", "abc", "ä".repeat(100), "x".repeat(55), "x".repeat(56), "x".repeat(64), Speech.PROBE).forEach { s ->
            val java = MessageDigest.getInstance("SHA-256").digest(s.encodeToByteArray()).joinToString("") { "%02x".format(it) }
            assertEquals(s, java, Sha256.hex(s.encodeToByteArray()))
        }
    }

    @Test fun listHasVoltsLinesAndSpokenForm() {
        val texts = VorlesenListe.texts(listOf(File("../../shared/src/commonMain/kotlin")))
        assertTrue(texts.size > 1000)
        // Volt-Sätze aus der Oberfläche, gesprochen wie in der App
        assertTrue(Speech.clean("Volltreffer beim ersten Versuch! Genial berechnet! 🤯") in texts)
        assertTrue(Speech.clean("Alles Wichtige zum Nachlesen – das kostet keine Sterne! 💡") in texts)
        assertTrue(texts.none { '$' in it || "{" in it })
        assertTrue(texts.all { it.length <= VorlesenListe.MAX })
        // Theorie am Stück und Rechen-Stern genau so, wie die App sie vorliest
        val l = Levels.byId("2-5")!!
        assertTrue(Speech.clean(ReadTexts.theoryAll(Theories.of(l, Levels.chapters.first { it.number == 2 })!!)) in texts)
    }

    @Test fun liveRules() {
        assertTrue(Nova.maySend("Schon 24 Sterne gesammelt.", setOf("Mia", "Ben")))
        assertFalse(Nova.maySend("Super, Mia! Weiter so.", setOf("Mia")))
        assertFalse(Nova.maySend("super, mia!", setOf("Mia")))
        assertTrue(Nova.maySend("Die Miau-Katze", setOf("Mia")))       // nur ganze Wörter
        assertFalse(Nova.maySend("x".repeat(301), emptySet()))
        assertEquals("""{"text": "Er sagt \"Hallo\"", "sprache": "de"}""", Nova.liveBody("Er sagt \"Hallo\""))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/ac/acddd8626f25b43663f8efcd3fe60a1f.mp3",
            Nova.liveUrl("""{"url": "https://stimmen.mathenachhilfe-online.com/nova/ac/acddd8626f25b43663f8efcd3fe60a1f.mp3", "neu": true}"""))
        assertNull(Nova.liveUrl("""{"url": "https://elsewhere.example/x.mp3"}"""))
        // Prüfwert aus dem Test gegen den echten Server (17.11.)
        assertEquals("acddd8626f25b43663f8efcd3fe60a1f", Nova.hash("Schon 24 Sterne gesammelt. Weiter so, Commander!"))
    }

    @Test fun speeds() {
        assertEquals(listOf("0,75", "0,9", "1,0", "1,15", "1,3", "1,5"), Nova.SPEEDS.map(Nova::speedLabel))
        assertEquals(100, Nova.nearestSpeed(104)); assertEquals(150, Nova.nearestSpeed(400))
    }

    @Test fun offlineList() {
        assertEquals(188464128L, Nova.standBytes("""{"stimme": "nova", "texte": 1163, "bytes": 188464128, "zu_lang": 0}"""))
        val texts = VorlesenListe.texts(listOf(File("../../shared/src/commonMain/kotlin")))
        val back = Nova.listTexts(VorlesenListe.json(texts))
        assertEquals(texts, back)   // auch Anführungszeichen, Umlaute, Sonderfälle
        assertEquals(listOf("Er sagt \"Hallo\" – à"), Nova.listTexts(VorlesenListe.json(listOf("Er sagt \"Hallo\" – à"))))
    }
}
