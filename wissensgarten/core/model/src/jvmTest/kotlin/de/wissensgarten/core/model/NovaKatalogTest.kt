package de.wissensgarten.core.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wer darf an `/jetzt`? Und: warten statt ausweichen ([NovaAblauf]). */
class NovaKatalogTest {
    private fun pack(id: String, tier: String, vararg cards: PackCard) =
        Pack(id = id, version = 1, title = id, subject = "sachunterricht", grades = listOf(2), emoji = "🌱", tier = tier, cards = cards.toList())

    private val frei = pack("frei", Tier.FREE,
        PackCard("1", "Wie heißt die Hauptstadt von Italien?", "Rom", choices = listOf("Paris", "Madrid"), info = "Rom liegt am Tiber."))
    private val plus = pack("plus", Tier.PLUS, PackCard("p1", "Was ist ein Plus-Geheimnis?", "Das Plus-Wissen"))

    private fun sp(s: String) = Nova.normalisiert(Nova.sprechText(s, "de"))
    private fun card(front: String, back: String) = StudyCard("c", "b", front, back, null, null, Memory())

    private fun katalog() = NovaKatalog().apply {
        liste(listOf(Nova.Eintrag(sp("Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese."), "de")))
        pakete(listOf(frei, plus))
    }

    @Test fun festeTexteNachInhalt() {
        val k = katalog()
        assertEquals(NovaArt.OEFFENTLICH, k.art(sp("Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese."), "de"))
        assertEquals(NovaArt.OEFFENTLICH, k.art(sp("Wie heißt die Hauptstadt von Italien?"), "de"))   // freies Paket
        assertEquals(NovaArt.OEFFENTLICH, k.art(sp("Rom liegt am Tiber."), "de"))
        assertEquals(NovaArt.PLUS, k.art(sp("Was ist ein Plus-Geheimnis?"), "de"))
        // eigene Karte, Name: nie ans Netz
        assertEquals(NovaArt.PRIVAT, k.art(sp("Wie heißt meine Lehrerin?"), "de"))
        assertEquals(NovaArt.PRIVAT, k.art(sp("Lena"), "de"))
        // feste Zahlen-Sätze (Radio-Schluss …) sind öffentlich
        assertEquals(NovaArt.OEFFENTLICH, k.art(sp(Radio.summary(3, 5)), "de"))
    }

    @Test fun ohneListeUndPaketeUnbekannt() {
        assertEquals(NovaArt.UNBEKANNT, NovaKatalog().art("Lena", "de"))
        assertEquals(NovaArt.UNBEKANNT, NovaKatalog().art(Laufzeit.radioNochmal(card("x", "Lena"), true)!!, "x", "de"))
    }

    @Test fun laufzeitSatzNurMitOeffentlichenTeilen() {
        val k = katalog()
        val rom = card("Wie heißt die Hauptstadt von Italien?", "Rom")
        val rt = Laufzeit.radioNochmal(rom, true)!!
        assertEquals(listOf("Rom"), rt.teile)
        // als Paket-Satz vorab in der Liste (packLaufzeit) → öffentlich
        assertEquals(NovaArt.OEFFENTLICH, k.art(rt, sp(rt.text), "de"))
        // Brummi rät mit einer Antwort aus dem Paket – nicht vorab vertont, aber alle Teile öffentlich
        val rat = Laufzeit.lehrerRaet(rom, rom.copy(back = "Paris"), true)!!
        assertEquals(NovaArt.LAUFZEIT, k.art(rat, sp(rat.text), "de"))
        // eigene Karte in einem Paket-Beet (ausPaket = true, aber Inhalt unbekannt): nie an /jetzt
        val eigen = Laufzeit.radioNochmal(card("Wie heißt meine Lehrerin?", "Frau Müller"), true)!!
        assertEquals(NovaArt.PRIVAT, k.art(eigen, sp(eigen.text), "de"))
        // Plus-Inhalt in einem Laufzeit-Satz: nie an /jetzt
        val p = Laufzeit.radioKorrektur(Radio.Question(card("Was ist ein Plus-Geheimnis?", "Das Plus-Wissen"), Radio.Mode.FREE), true)!!
        assertEquals(NovaArt.PLUS, k.art(p, sp(p.text), "de"))
        // nur Zahlen: immer erlaubt
        val z = Laufzeit.palast(2, 9)
        assertEquals(NovaArt.LAUFZEIT, k.art(z, sp(z.text), "de"))
    }

    @Test fun vorabListeEnthaeltHaeufigeLaufzeitSaetze() {
        val texte = Nova.packLaufzeit(frei).map { it.text }
        assertTrue(sp("Hör noch mal: Rom. Das üben wir bald wieder.") in texte)
        assertTrue(sp("Fast! Die Antwort ist: Rom.") in texte)
        assertTrue(sp("Etwas Neues: Wie heißt die Hauptstadt von Italien? Die Antwort ist: Rom. Sag es mir nach!") in texte)
        val zahlen = Nova.zahlenTexte().map { it.text }
        assertTrue(sp(Radio.summary(12, 12)) in zahlen)
        assertTrue(sp(Laufzeit.bonusText(8, true)) in zahlen)
        assertTrue(sp(Laufzeit.kapitelWartetText(70)) in zahlen)
        assertTrue(sp(Laufzeit.kapitelOffenText(10)) in zahlen)
        assertEquals(zahlen.size, zahlen.distinct().size)
    }

    @Test fun saetzeNieMittenImSatz() {
        assertEquals(listOf("Eins. Zwei!", "Drei?"), Nova.saetze("Eins. Zwei! Drei?", 11))
        assertEquals(listOf("Ein langer Teil,", "noch ein Teil."), Nova.saetze("Ein langer Teil, noch ein Teil.", 20))
        assertTrue(Nova.saetze("x".repeat(30), 20).isEmpty())
    }

    // ---------------------------------------------------------------- warten statt ausweichen

    /** Server, der [ms] braucht und dann [status] liefert */
    private class Langsam(val ms: Long, val status: Int) : NovaQuelle {
        val speicher = HashSet<String>()
        override suspend fun imSpeicher(h: String) = h in speicher
        override suspend fun laden(url: String, h: String): Int { delay(ms); if (status == 200) speicher += h; return status }
        override suspend fun jetzt(text: String, sprache: String, h: String): Int { delay(ms); speicher += h; return 200 }
    }

    private fun ablauf(q: NovaQuelle, scope: kotlinx.coroutines.CoroutineScope) =
        NovaAblauf(NovaWeg(q), katalog(), scope, wartenMs = 600, hinweisMs = 80, jetztWartenMs = 1200)

    private val brummi = sp("Hallo, ich bin Brummi! So klinge ich, wenn ich dir vorlese.")

    @Test fun schnellOhneLadehinweis() = runBlocking {
        val an = ArrayList<Boolean>()
        val plan = ablauf(Langsam(10, 200), this).plan(brummi, "de", null) { an += it }
        assertEquals(1, plan.dateien.size)
        assertTrue(true !in an)
        assertNull(plan.meldung)
    }

    @Test fun langsamMitLadehinweisAberNova() = runBlocking {
        val an = ArrayList<Boolean>()
        val plan = ablauf(Langsam(250, 200), this).plan(brummi, "de", null) { an += it }
        assertEquals(1, plan.dateien.size)        // gewartet statt ausgewichen
        assertEquals(listOf(true, false), an)
    }

    @Test fun zuLangsamGeraetEinmalMeldenUndImHintergrundWeiterladen() = runBlocking {
        val q = Langsam(900, 200)
        val a = ablauf(q, this)
        val p1 = a.plan(brummi, "de", null)
        assertTrue(p1.dateien.isEmpty())
        assertEquals(NovaAblauf.MELDUNG, p1.meldung)
        val p2 = a.plan(sp("Rom"), "de", null)
        assertTrue(p2.dateien.isEmpty())
        assertNull(p2.meldung)                    // nur einmal je Sitzung
        delay(1500)
        assertTrue(Nova.hash(brummi, "de") in q.speicher)   // beim nächsten Mal da
        assertEquals(1, a.plan(brummi, "de", null).dateien.size)
    }

    /** Datei fehlt (404), der Server vertont den Satz neu: dafür etwas länger warten */
    private class Neu(val ms: Long) : NovaQuelle {
        val speicher = HashSet<String>()
        override suspend fun imSpeicher(h: String) = h in speicher
        override suspend fun laden(url: String, h: String) = 404
        override suspend fun jetzt(text: String, sprache: String, h: String): Int { delay(ms); speicher += h; return 200 }
    }

    @Test fun beimNeuVertonenLaengerWarten() = runBlocking {
        val rt = Laufzeit.lehrerRaet(card("Wie heißt die Hauptstadt von Italien?", "Rom"), card("x", "Paris"), true)!!
        val plan = ablauf(Neu(900), this).plan(sp(rt.text), "de", rt)
        assertEquals(NovaArt.LAUFZEIT, plan.art)
        assertEquals(NovaWeg.Ergebnis.JETZT, plan.ergebnis)      // 900 ms > 600 ms, aber < 1200 ms
        assertEquals(1, plan.dateien.size)
        // auch das hat Grenzen
        val p2 = ablauf(Neu(1600), this).plan(sp(rt.text), "de", rt)
        assertTrue(p2.dateien.isEmpty())
        assertEquals(NovaAblauf.MELDUNG, p2.meldung)
        delay(600)
    }

    @Test fun privatSofortGeraetOhneMeldung() = runBlocking {
        val gestartet = CompletableDeferred<Unit>()
        val q = object : NovaQuelle {
            override suspend fun imSpeicher(h: String) = false
            override suspend fun laden(url: String, h: String): Int { gestartet.complete(Unit); return 200 }
            override suspend fun jetzt(text: String, sprache: String, h: String): Int { gestartet.complete(Unit); return 200 }
        }
        val plan = ablauf(q, this).plan(sp("Frau Müller"), "de", null)
        assertTrue(plan.dateien.isEmpty())
        assertEquals(NovaArt.PRIVAT, plan.art)
        assertNull(plan.meldung)
        assertTrue(!gestartet.isCompleted)
    }

    @Test fun offlineSofortGeraetMitMeldung() = runBlocking {
        val plan = ablauf(Langsam(0, -1), this).plan(brummi, "de", null)
        assertTrue(plan.dateien.isEmpty())
        assertEquals(NovaWeg.Ergebnis.NETZ, plan.ergebnis)
        assertEquals(NovaAblauf.MELDUNG, plan.meldung)
    }
}
