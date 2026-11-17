package de.wissensgarten.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Läuft auf JVM (Liste, Android) UND im Browser (WebApp, Kotlin/Wasm): Der Sprechtext und damit der Dateiname müssen
 * überall gleich sein – sonst findet die WebApp die vorab vertonte Datei nicht und die Gerätestimme liest.
 */
class NovaGleichTest {
    private fun sp(s: String, l: String = "de") = Nova.normalisiert(Nova.sprechText(s, l))

    @Test fun emojisFallenWeg() {
        assertEquals("Neu! Schau dir das Bild gut an", sp("Neu! Schau dir das Bild gut an 👀"))
        assertEquals("Super!", sp("Super! 🌟"))
        assertEquals("Kipp das Handy!", sp("Kipp das Handy! 📱"))
        assertEquals("Klatsch mit!", sp("Klatsch mit! 👏"))
        assertEquals("Bonusrunde!", sp("Bonusrunde! 🧩"))
        assertEquals("Willkommen in meinem Zuhause! Richte die Räume ein, wie du magst.", sp("Willkommen in meinem Zuhause! Richte die Räume ein, wie du magst. 🐝"))
        assertEquals("Profi-Frage! Ganz ohne Hilfe", sp("Profi-Frage! Ganz ohne Hilfe 🌳"))
        assertEquals("plus 5 Bonus!", sp(Laufzeit.bonusText(5, false)))
        assertEquals("Stimmt das wirklich?", sp("Stimmt das wirklich? 🤔"))
        assertEquals("Hallo! Ich bin Brummi", sp("Hallo! Ich bin Brummi 🐝"))
        assertEquals("Ich höre zu!", sp("Ich höre zu! 👂"))
        assertEquals("Psst … ganz ruhig", sp("Psst … ganz ruhig 🌙"))
        assertEquals("Deine Pflanzen winken dir schon zu!", sp("Deine Pflanzen winken dir schon zu! 👋"))
        assertEquals("Das hast du alles selbst gegossen!", sp("Das hast du alles selbst gegossen! 🌻"))
    }

    @Test fun zeichenUndEinheiten() {
        assertEquals("a hoch 2 plus b hoch 2 gleich c hoch 2", sp("a² + b² = c²"))
        assertEquals("H2O", sp("H₂O"))
        assertEquals("50 Kilometer pro Stunde", sp("50 km/h"))
        assertEquals("20 Grad Celsius", sp("20 °C"))
        assertEquals("Na plus", sp("Na⁺"))
        assertEquals("3 mal 4 gleich 12", sp("3 · 4 = 12"))
        assertEquals("der Lehrer oder die Lehrerin", sp("der Lehrer / die Lehrerin"))
        assertEquals("1/2 Kuchen", sp("1/2 Kuchen"))
        assertEquals("Die Antwort ist: …", sp("Die Antwort ist: ___"))
    }

    @Test fun spracheGleich() {
        assertEquals("en", Nova.sprache("englisch", "What is the dog?"))
        assertEquals("de", Nova.sprache("englisch", "Wie heißt das auf Englisch?"))
        assertEquals("de", Nova.sprache("englisch", "Was ist the dog?"))
        assertEquals("fr", Nova.sprache("franzoesisch", "le chien"))
        assertEquals("de", Nova.sprache("franzoesisch", "der Hund"))
        assertEquals("es", Nova.sprache("spanisch", "el perro"))
        assertEquals("de", Nova.sprache("mathe", "the dog"))
    }

    @Test fun radioUndSprechspielLaufenImBrowser() {
        // früher: \\p{N} → im Browser „No such character class“
        assertEquals(true, Games.speakPossible("Hund"))
        assertEquals("ja klar", nurWorte("ja, klar!").trim().replace("  ", " "))
        val c = StudyCard("1", "b", "Wie heißt das Tier?", "Katze", listOf("Hund", "Maus", "Pferd"), null, Memory(stability = 3.0, reps = 1))
        assertEquals(Radio.Mode.FREE, Radio.question(c, listOf("Hund", "Maus")).mode)
    }

    @Test fun nameGleich() {
        assertEquals("cb1e6d67882767042ebaa54d163e450c", Nova.hash("Hallo Welt", "de"))
        assertEquals(Nova.hash("Grüße aus Köln", "de"), Nova.hash("Grüße  aus\nKöln ", "de"))
    }
}
