package de.experimentopia.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.text.Normalizer

/** Tonstudio: Texte in kurze, saubere Stücke; SHA-256 (Sicherung) wie Java; Vorlesen zurzeit aus */
class TonstudioTest {
    @Test fun sha256WieJava() {
        val md = MessageDigest.getInstance("SHA-256")
        listOf("", "a", "x".repeat(55), "y".repeat(56), "z".repeat(64), "Ä Ö Ü ß – Blubb", "q".repeat(1000)).forEach { s ->
            assertEquals(s, md.digest(s.encodeToByteArray()).toList(), Sha256.digest(s.encodeToByteArray()).toList())
        }
    }

    @Test fun stueckeSindKurzUndVollstaendig() {
        val all = ReadTexts.all()
        all.forEach { t ->
            val p = Tonstudio.parts(t)
            p.forEach { assertTrue("zu lang: $it", it.length <= Tonstudio.MAX); assertTrue("leer in $t", it.isNotBlank()) }
            assertEquals(Tonstudio.normalize(Speech.clean(t)), p.joinToString(" "))
        }
    }

    @Test fun listeIstSauber() {
        val c = Tonstudio.catalog()
        assertTrue(c.size > 1000)
        c.forEach { t ->
            assertTrue("NFC: $t", Normalizer.isNormalized(t, Normalizer.Form.NFC))
            assertTrue("ohne Emojis/Markup: $t", t.none { Character.isSurrogate(it) || it in "*#_<>[]{}`" })
        }
        if (Tonstudio.ENABLED) assertTrue(Tonstudio.json().startsWith("{\"format\":1,\"app\":\"experimentopia\",\"texte\":[{\"text\":"))
        else assertEquals("""{"format":1,"app":"experimentopia","texte":[]}""", Tonstudio.json())   // Vorlesen aus: nichts vertonen lassen
    }
}

class TypoTest {
    @Test fun zahlUndEinheitBleibenZusammen() {
        assertEquals("im Salzwasser (5 %)?", Typo.keep("im Salzwasser (5 %)?"))
        assertEquals("14 kg und 2 m und 20 °C", Typo.keep("14 kg und 2 m und 20 °C"))
        assertEquals("Feld 2 geht", Typo.keep("Feld 2 geht"))
        // Vorlesen und Textliste bleiben gleich
        assertEquals(Speech.clean("5 % Salz"), Speech.clean(Typo.keep("5 % Salz")))
    }
}

class TonstudioTempoTest {
    @Test fun stufen() {
        assertEquals(listOf(0.8f, 1.0f), Tonstudio.SPEEDS)
        assertEquals(0.8f, Tonstudio.speed(0.75f)); assertEquals(1.0f, Tonstudio.speed(1.15f)); assertEquals(1.0f, Tonstudio.speed(1.0f))
    }
}
