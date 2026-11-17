package de.wissensgarten.core.model

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ohne kotlinx-coroutines: die Fakes unterbrechen nie, also läuft alles sofort durch */
private fun <T> runBlocking(block: suspend () -> T): T {
    var r: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { r = it })
    return r!!.getOrThrow()
}

class NovaWegTest {
    /** Nachgebauter Speicher + Server: zählt, was die App anfragt */
    private class Fake(var status: Int = 404, var jetztStatus: Int = 403) : NovaQuelle {
        val speicher = HashSet<String>()
        val geladen = ArrayList<String>()
        val jetzt = ArrayList<Pair<String, String>>()
        override suspend fun imSpeicher(h: String) = h in speicher
        override suspend fun laden(url: String, h: String): Int {
            geladen += url
            if (status == 200) speicher += h
            return status
        }
        override suspend fun jetzt(text: String, sprache: String, h: String): Int {
            jetzt += text to sprache
            if (jetztStatus == 200) speicher += h
            return jetztStatus
        }
    }

    private var t = 1_000_000L
    private fun weg(f: Fake) = NovaWeg(f) { t }

    @Test fun ausDemSpeicherOhneNetz() = runBlocking {
        val f = Fake(); f.speicher += Nova.hash("Super!", "de")
        assertEquals(NovaWeg.Ergebnis.SPEICHER, weg(f).finde("Super!", "de", false))
        assertTrue(f.geladen.isEmpty())
    }

    @Test fun vorabVertonteDateiWirdGeladen() = runBlocking {
        val f = Fake(status = 200)
        val w = weg(f)
        assertEquals(NovaWeg.Ergebnis.NOVA, w.finde("Hallo Welt", "de", false))
        assertEquals(listOf("https://stimmen.mathenachhilfe-online.com/nova/cb/cb1e6d67882767042ebaa54d163e450c.mp3"), f.geladen)
        // zweites Mal aus dem Speicher
        assertEquals(NovaWeg.Ergebnis.SPEICHER, w.finde("Hallo Welt", "de", false))
        assertEquals(1, f.geladen.size)
    }

    @Test fun festerTextNieAnJetzt() = runBlocking {
        val f = Fake(status = 404, jetztStatus = 200)
        assertEquals(NovaWeg.Ergebnis.GERAET, weg(f).finde("Eine Frage aus einem Plus-Paket?", "de", laufzeit = false))
        assertTrue(f.jetzt.isEmpty())
    }

    @Test fun laufzeitTextNur404DannJetzt() = runBlocking {
        val f = Fake(status = 404, jetztStatus = 200)
        assertEquals(NovaWeg.Ergebnis.JETZT, weg(f).finde("Hör noch mal: Rom. Das üben wir bald wieder.", "de", laufzeit = true))
        assertEquals(listOf("Hör noch mal: Rom. Das üben wir bald wieder." to "de"), f.jetzt)
    }

    @Test fun jetztMit204OderFehlerGeraetestimme() = runBlocking {
        val f = Fake(status = 404, jetztStatus = 204)
        assertEquals(NovaWeg.Ergebnis.GERAET, weg(f).finde("3 von 5 Orten erinnert", "de", true))
        val g = Fake(status = 404, jetztStatus = 403)
        val w = weg(g)
        assertEquals(NovaWeg.Ergebnis.GERAET, w.finde("3 von 5 Orten erinnert", "de", true))
        // nach einem Fehler eine Weile kein neuer /jetzt-Versuch
        assertEquals(NovaWeg.Ergebnis.GERAET, w.finde("4 von 5 Orten erinnert", "de", true))
        assertEquals(1, g.jetzt.size)
    }

    @Test fun zuLangeLaufzeitTexteNichtAnJetzt() = runBlocking {
        val f = Fake(status = 404, jetztStatus = 200)
        assertEquals(NovaWeg.Ergebnis.GERAET, weg(f).finde("a".repeat(301), "de", true))
        assertTrue(f.jetzt.isEmpty())
    }

    @Test fun offlineStillGeraetUndPause() = runBlocking {
        val f = Fake(status = -1)
        val w = weg(f)
        assertFalse(w.offline)
        assertEquals(NovaWeg.Ergebnis.GERAET, w.finde("Super!", "de", true))
        assertTrue(w.offline)
        assertTrue(f.jetzt.isEmpty())
        // während der Pause keine Anfragen
        assertEquals(NovaWeg.Ergebnis.GERAET, w.finde("Klasse!", "de", false))
        assertEquals(1, f.geladen.size)
        t += NovaWeg.NETZ_PAUSE_MS + 1
        f.status = 200
        assertEquals(NovaWeg.Ergebnis.NOVA, w.finde("Klasse!", "de", false))
    }

    @Test fun fehlendeDateiNichtStaendigNeuAnfragen() = runBlocking {
        val f = Fake(status = 404)
        val w = weg(f)
        repeat(3) { w.finde("Super!", "de", false) }
        assertEquals(1, f.geladen.size)
        t += NovaWeg.FEHLT_MS + 1
        w.finde("Super!", "de", false)
        assertEquals(2, f.geladen.size)
    }

    @Test fun andereSprachenUndUnbekannte() = runBlocking {
        val f = Fake(status = 200)
        assertEquals(NovaWeg.Ergebnis.NOVA, weg(f).finde("the dog", "en", false))
        assertEquals(NovaWeg.Ergebnis.GERAET, weg(f).finde("canis", "la", false))
    }

    @Test fun sprechtempoStufen() {
        assertEquals(listOf(75, 90, 100, 115, 130, 150), Sprechtempo.STUFEN)
        assertEquals(100, Sprechtempo.stufe(100))
        assertEquals(75, Sprechtempo.stufe(50))     // alter Wert 50 %
        assertEquals(115, Sprechtempo.stufe(120))   // alter Wert 120 %
        assertEquals(150, Sprechtempo.stufe(150))
        assertEquals(130, Sprechtempo.stufe(140))
        assertEquals(1.15f, Sprechtempo.faktor(115))
        assertEquals("1,0×", Sprechtempo.label(100))
        assertEquals("0,75×", Sprechtempo.label(75))
        assertEquals("1,15×", Sprechtempo.label(115))
        assertEquals("0,9×", Sprechtempo.label(90))
    }

    /**
     * „Nie Namen an /jetzt“: Laufzeit-Texte gibt es nur über [Laufzeit]. Dessen Bausteine nehmen keinen freien Text (String) an –
     * nur Zahlen, Ja/Nein und Karten (die nur aus Wissenspaketen verwendet werden). Ein Profilname kann so nicht hineingeraten.
     */
    @Test fun laufzeitTexteKoennenKeineNamenEnthalten() {
        val allowed = setOf(Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Radio.Question::class.java, StudyCard::class.java)
        val builders = Laufzeit::class.java.declaredMethods.filter { it.returnType == LaufzeitText::class.java && java.lang.reflect.Modifier.isPublic(it.modifiers) }
        assertTrue(builders.size >= 10)
        builders.forEach { m -> m.parameterTypes.forEach { p -> assertTrue("${m.name} nimmt ${p.simpleName}", p in allowed) } }
    }

    @Test fun eigeneKartenNieAnDenServer() {
        val card = StudyCard("x", "eigenes-beet", "Wie heißt meine Lehrerin?", "Frau Müller", null, null, Memory())
        val q = Radio.Question(card, Radio.Mode.SELF)
        assertNull(Laufzeit.radioFrage(q, ausPaket = false))
        assertNull(Laufzeit.radioAufloesung(q, ausPaket = false))
        assertNull(Laufzeit.radioKorrektur(q, ausPaket = false))
        assertNull(Laufzeit.radioSpaeter(q, ausPaket = false))
        assertNull(Laufzeit.radioNochmal(card, ausPaket = false))
        // aus einem Paket: einheitlich formuliert
        assertEquals("Hör noch mal: Frau Müller. Das üben wir bald wieder.", Laufzeit.radioNochmal(card, ausPaket = true)!!.text)
        // freie Antworten (FREE) sind feste Pakettexte – nie Laufzeit
        assertNull(Laufzeit.radioFrage(Radio.Question(card, Radio.Mode.FREE), ausPaket = true))
    }
}
