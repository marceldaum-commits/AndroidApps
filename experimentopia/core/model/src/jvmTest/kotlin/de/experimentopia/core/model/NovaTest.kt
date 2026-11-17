package de.experimentopia.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.text.Normalizer

class NovaTest {
    @Test fun pruefwertDesServers() = assertEquals("cb1e6d67882767042ebaa54d163e450c", Nova.hash("Hallo Welt"))
    @Test fun leerraumZaehltNicht() = assertEquals(Nova.hash("Hallo Welt"), Nova.hash("  Hallo \n Welt "))
    @Test fun adresse() = assertEquals("https://stimmen.mathenachhilfe-online.com/nova/cb/cb1e6d67882767042ebaa54d163e450c.mp3", Nova.url("Hallo Welt"))

    @Test fun zaehlstand() {
        assertEquals(217802112L, Nova.bytesOf("""{"stimme": "nova", "texte": 2301, "bytes": 217802112, "zu_lang": 0}"""))
        assertEquals(null, Nova.bytesOf("{}"))
        val both = """{"bytes_original": 300, "bytes": 100}"""
        assertEquals(100L, Nova.bytesOf(both)); assertEquals(300L, Nova.bytesOriginalOf(both))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/original/cb/cb1e6d67882767042ebaa54d163e450c.mp3", Nova.originalUrl("Hallo Welt"))
        assertEquals(208L, Nova.mb(217802112L)); assertEquals(1L, Nova.mb(10))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/stand-experimentopia.mathenachhilfe-online.com.json", Nova.standUrl(Nova.APP_HOST))
    }
    @Test fun sha256WieJava() {
        val md = MessageDigest.getInstance("SHA-256")
        listOf("", "a", "x".repeat(55), "y".repeat(56), "z".repeat(64), "Ä Ö Ü ß – Blubb", "q".repeat(1000)).forEach { s ->
            assertEquals(s, md.digest(s.encodeToByteArray()).toList(), Sha256.digest(s.encodeToByteArray()).toList())
        }
    }

    @Test fun stueckeSindKurzUndVollstaendig() {
        val all = ReadTexts.all()
        all.forEach { t ->
            val p = Nova.parts(t)
            p.forEach { assertTrue("zu lang: $it", it.length <= Nova.MAX); assertTrue("leer in $t", it.isNotBlank()) }
            assertEquals(Nova.normalize(Speech.clean(t)), p.joinToString(" "))
        }
    }

    @Test fun listeIstSauber() {
        val c = Nova.catalog()
        assertTrue(c.size > 1000)
        c.forEach { t ->
            assertTrue("NFC: $t", Normalizer.isNormalized(t, Normalizer.Form.NFC))
            assertTrue("ohne Emojis/Markup: $t", t.none { Character.isSurrogate(it) || it in "*#_<>[]{}`" })
        }
        assertTrue(Nova.json().startsWith("{\"format\":1,\"app\":\"experimentopia\",\"texte\":[{\"text\":"))
    }
}
