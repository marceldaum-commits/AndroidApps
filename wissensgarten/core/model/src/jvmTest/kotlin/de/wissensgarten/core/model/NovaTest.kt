package de.wissensgarten.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class NovaTest {
    @Test fun pruefwertDesServers() {
        assertEquals("cb1e6d67882767042ebaa54d163e450c", Nova.hash("Hallo Welt", "de"))
        assertEquals("https://stimmen.mathenachhilfe-online.com/nova/cb/cb1e6d67882767042ebaa54d163e450c.mp3", Nova.url(Nova.hash("Hallo Welt", "de")))
    }

    @Test fun leerraumUndNfcZaehlenNicht() {
        val h = Nova.hash("Hallo Welt", "de")
        assertEquals(h, Nova.hash("  Hallo\n\tWelt ", "de"))
        assertEquals(h, Nova.hash("Hallo  Welt", "de"))
        // „ä“ zusammengesetzt (a + ¨) und fertig sind derselbe Text
        assertEquals(Nova.hash("Bär", "de"), Nova.hash("Bär", "de"))
        assertFalse(h == Nova.hash("Hallo Welt", "en"))
    }

    @Test fun sha256WieJava() {
        val r = java.util.Random(7)
        for (n in listOf(0, 1, 55, 56, 63, 64, 65, 119, 120, 1000)) {
            val b = ByteArray(n).also(r::nextBytes)
            val ref = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
            assertEquals(ref, Sha256.hex(b))
        }
    }

    @Test fun sprache() {
        assertEquals("en", Nova.sprache("englisch", "the dog"))
        assertEquals("de", Nova.sprache("englisch", "Wie heißt Hund auf Englisch?"))
        assertEquals("de", Nova.sprache("englisch", "Was bedeutet dog?"))
        assertEquals("fr", Nova.sprache("franzoesisch", "le chien"))
        assertEquals("es", Nova.sprache("spanisch", "¿Cómo te llamas?"))
        assertEquals("de", Nova.sprache("mathe", "the dog"))
        assertEquals("de", Nova.sprache(null, "dog"))
    }

    @Test fun sprechTextOhneEmojisMitWoertern() {
        assertEquals("Du bist on fire!", Nova.sprechText("🔥 Du bist on fire!"))
        assertEquals("Super!", Nova.sprechText("Super! 🌟"))
        assertEquals("Nomen sind Namenwörter, wie ein Namensschild: Hund, Schule.", Nova.sprechText("Nomen sind Namenwörter, wie ein Namensschild 🏷️: Hund, Schule."))
        assertEquals("10 mal 10 gleich 100.", Nova.sprechText("10 × 10 = 100."))
        assertEquals("ten minus four equals?", Nova.sprechText("ten − four = ?", "en"))
        assertEquals("A gleich π mal r hoch 2", Nova.sprechText("A = π · r²"))
        assertEquals("u gleich 2 mal (a plus b)", Nova.sprechText("u = 2 · (a + b)"))
        assertEquals("1918 Weimarer Republik, 1933 Diktatur", Nova.sprechText("1918 Weimarer Republik · 1933 Diktatur"))
        assertEquals("Punkt S(2, 3)", Nova.sprechText("Punkt S(2|3)"))
        assertEquals("Wasser ist H2O.", Nova.sprechText("Wasser ist H₂O."))
        assertEquals("Na plus und Cl minus", Nova.sprechText("Na⁺ und Cl⁻"))
        assertEquals("Wurzel aus 25 gleich 5", Nova.sprechText("√25 = 5"))
        assertEquals("Betrag von minus 7 gleich 7", Nova.sprechText("|−7| = 7"))
        assertEquals("Es kostet 6 Euro, das sind 25 Prozent.", Nova.sprechText("Es kostet 6 €, das sind 25 %."))
        assertEquals("Wasser kocht bei 100 Grad Celsius, ein rechter Winkel hat 90 Grad.", Nova.sprechText("Wasser kocht bei 100 °C, ein rechter Winkel hat 90°."))
        assertEquals("Er fährt 50 Kilometer pro Stunde auf 3 Quadratmeter.", Nova.sprechText("Er fährt 50 km/h auf 3 m²."))
        assertEquals("der Lehrer oder die Lehrerin", Nova.sprechText("der Lehrer / die Lehrerin"))
        assertEquals("I oder he oder she oder it", Nova.sprechText("I/he/she/it"))
        assertEquals("Die Hälfte ist 1/2, und x durch 3 auch.", Nova.sprechText("Die Hälfte ist 1/2, und x/3 auch."))
        assertEquals("Ergänze: „Mama sagt, … wir gleich essen.“", Nova.sprechText("Ergänze: „Mama sagt, ___ wir gleich essen.“"))
        assertEquals("aqua, Aquarium", Nova.sprechText("aqua → Aquarium"))
        assertEquals("(minus 3) hoch 2 gleich 9", Nova.sprechText("(−3)² = 9"))
        assertEquals("Lurch (Amphibie)?", Nova.sprechText("Lurch (Amphibie)?"))
        assertEquals("z. B. Ei – Raupe – Puppe", Nova.sprechText("z. B. Ei – Raupe – Puppe"))
    }

    @Test fun packTexteOhneAuswahlAntworten() {
        val p = Pack("p", 1, "T", "mathe", listOf(3), "🧮", description = "D",
            intro = listOf(IntroScene("🍎", "Hallo 🐝"), IntroScene("🍐", "Los geht's!")),
            cards = listOf(PackCard("1", "2 + 2 = ?", "4", choices = listOf("5", "6"), info = "Zwei und zwei."), PackCard("2", "Was ist rot?", "Tomate")))
        val t = Nova.packTexte(p).map { it.text }
        assertTrue(t.containsAll(listOf("Hallo", "Los geht's!", "Hallo Los geht's!", "2 plus 2 gleich?", "4", "Zwei und zwei.", "To – ma – te", "Tomate")))
        assertFalse(t.contains("5")); assertFalse(t.contains("6"))
    }
}
