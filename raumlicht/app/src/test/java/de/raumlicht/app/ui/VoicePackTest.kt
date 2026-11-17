package de.raumlicht.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.settings.NaturalVoiceCard
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Natürliche Stimme: Download mit Prüfsumme, Abbruch, Entfernen und die Wahl zwischen den Stimmen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoicePackTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val data = ByteArray(300_000) { (it * 7 % 251).toByte() }
    private val sum = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    private fun server(bytes: ByteArray, code: Int = 200, onRead: () -> Unit = {}) = { _: String -> object : HttpURLConnection(URL("https://beispiel.invalid/")) {
        override fun connect() {}; override fun disconnect() {}; override fun usingProxy() = false
        override fun getResponseCode() = code
        override fun getInputStream() = object : ByteArrayInputStream(bytes) { override fun read(b: ByteArray): Int { onRead(); return super.read(b) } }
    } as HttpURLConnection }
    private fun wait(l: VoicePackLoader) { val end = System.currentTimeMillis() + 10_000; while (l.state is PackState.Loading && System.currentTimeMillis() < end) Thread.sleep(20) }

    @Test fun downloadIsCheckedAndCanBeRemoved() {
        var ready = 0
        val l = VoicePackLoader(ctx, server(data), { ready++ }, data.size.toLong(), sum)
        assertEquals(PackState.Missing, l.state); assertFalse(VoicePack.installed(ctx, data.size.toLong()))
        l.start(); wait(l)
        assertEquals(PackState.Ready, l.state); assertEquals(1, ready); assertTrue(VoicePack.installed(ctx, data.size.toLong()))
        // die mitgelieferten Hilfsdaten liegen daneben: Zeichenliste und deutsches Lautschrift-Wörterbuch
        val dir = VoicePack.dir(ctx); assertTrue(java.io.File(dir, "tokens.txt").length() > 500); assertTrue(java.io.File(dir, "saetze.tsv").length() > 10_000); assertTrue(java.io.File(dir, "woerter.tsv").length() > 10_000); assertFalse("kein espeak-ng mehr", java.io.File(dir, "espeak-ng-data").exists()); assertFalse(VoicePack.matches(VoicePack.model(ctx), data.size.toLong(), "00"))
        assertEquals(PackState.Ready, VoicePackLoader(ctx, server(data), size = data.size.toLong(), sha256 = sum).state)     // beim nächsten Start gleich da
        l.remove(); assertEquals(PackState.Missing, l.state); assertFalse(dir.exists())
    }

    @Test fun damagedOrFailedDownloadsAreRejected() {
        val broken = VoicePackLoader(ctx, server(data.copyOf().also { it[5] = 1 }), size = data.size.toLong(), sha256 = sum); broken.start(); wait(broken)
        assertEquals(PackState.Failed("Die Datei ist unvollständig oder beschädigt angekommen."), broken.state)
        assertFalse(VoicePack.model(ctx).exists()); assertFalse(java.io.File(VoicePack.dir(ctx), VoicePack.MODEL + ".teil").exists())   // nichts Halbes bleibt liegen
        val short = VoicePackLoader(ctx, server(data.copyOf(1000)), size = data.size.toLong(), sha256 = sum); short.start(); wait(short); assertTrue(short.state is PackState.Failed)
        val gone = VoicePackLoader(ctx, server(data, 404), size = data.size.toLong(), sha256 = sum); gone.start(); wait(gone)
        assertEquals(PackState.Failed("Der Server antwortet mit 404."), gone.state)
        lateinit var slow: VoicePackLoader
        slow = VoicePackLoader(ctx, server(data) { slow.cancel() }, size = data.size.toLong(), sha256 = sum); slow.start(); wait(slow)
        assertEquals(PackState.Missing, slow.state)                                                                             // abgebrochen ist kein Fehler
        broken.start(); wait(broken); assertTrue(broken.state is PackState.Failed)                                              // noch einmal versuchen geht
    }

    @Test fun routerPicksTheNaturalVoiceOnlyWhenWanted() {
        val heard = ArrayList<String>(); var natural = false
        val r = VoiceRouter(Speaker { heard += "handy:$it" }, Speaker { heard += "thorsten:$it" }) { natural }
        r.say("a"); natural = true; r.say("b"); r.sayIfQuiet("c"); natural = false; r.sayIfQuiet("d")
        assertEquals(listOf("handy:a", "thorsten:b", "thorsten:c", "handy:d"), heard)
        // ohne Paket bleibt die natürliche Stimme stumm und meldet sich nicht als bereit
        val n = NaturalVoice(ctx); n.load(); assertFalse(n.ready); n.say("nichts"); n.sayIfQuiet("nichts"); n.stop(); n.unload()
    }

    @Test fun settingsCardFollowsTheState() {
        var state by androidx.compose.runtime.mutableStateOf<PackState>(PackState.Missing); var active by androidx.compose.runtime.mutableStateOf(false); val did = ArrayList<String>()
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { NaturalVoiceCard(state, active, null, { did += "laden" }, { did += "abbrechen" }, { did += "entfernen" }, { did += "nutzen:$it" }) } } }
        rule.onNodeWithTag("stimme-status").assertTextContains("Einmaliger Download von 63 MB", substring = true); rule.onNodeWithText("Stimme laden (63 MB)").performClick()
        state = PackState.Loading(42); rule.onNodeWithTag("stimme-status").assertTextEquals("Lädt … 42 %"); rule.onNodeWithTag("stimme-abbrechen").performClick()
        state = PackState.Failed("Keine Verbindung."); rule.onNodeWithTag("stimme-status").assertTextEquals("Das hat nicht geklappt: Keine Verbindung."); rule.onNodeWithTag("stimme-laden").assertExists()
        state = PackState.Ready; rule.onNodeWithText("Einschalten").performClick()
        active = true; rule.onNodeWithTag("stimme-status").assertTextContains("ganz ohne Internet", substring = true); rule.onNodeWithText("Handy-Stimme nehmen").performClick()
        rule.onNodeWithTag("stimme-entfernen").performClick()
        assertEquals(listOf("laden", "abbrechen", "nutzen:true", "nutzen:false", "entfernen"), did)
    }

    @Test fun stageRebuildsWhenItsRendererIsGone() {
        val c = de.raumlicht.app.ui.stage.StageController(); c.setState("{\"objects\":[]}")
        assertEquals(0, c.generation); c.rendererGone(); assertEquals(1, c.generation)
        assertEquals("{\"objects\":[]}", c.lastState)                                         // der letzte Zustand bleibt für den Neustart erhalten
        c.fire(de.raumlicht.core.model.Beam(de.raumlicht.core.model.V3(0, 0, 0), de.raumlicht.core.model.V3(1, 1, 1), "pink", true), 100)   // Befehle ohne Bühne stürzen nicht ab
    }

    @Test fun lastProblemIsShownAndCanBeDismissed() {
        de.raumlicht.app.Diagnose.clear(ctx); assertNull(de.raumlicht.app.Diagnose.last(ctx))
        de.raumlicht.app.Diagnose.note(ctx, "3D-Bühne vom System beendet (zu wenig Speicher) – wurde neu gestartet")
        val text = de.raumlicht.app.Diagnose.last(ctx)!!; assertTrue(text, text.startsWith("Raumlicht ") && text.contains("zu wenig Speicher") && text.contains("Android "))
        assertEquals("Vom System beendet: zu wenig Arbeitsspeicher", de.raumlicht.app.Diagnose.reasonText(android.app.ApplicationExitInfo.REASON_LOW_MEMORY))
        assertNull(de.raumlicht.app.Diagnose.reasonText(android.app.ApplicationExitInfo.REASON_USER_REQUESTED))   // vom Nutzer geschlossen ist kein Problem
        var seen = 0
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { de.raumlicht.app.ui.settings.SettingsScreen(true, {}, true, {}, true, {}, true, {}, "2.4.1", de.raumlicht.app.ui.settings.UpdateState.Idle, {}, lastProblem = text, onProblemSeen = { seen++ }) } } }
        rule.onNodeWithTag("abbruch-bericht").performScrollTo().assertTextContains("zu wenig Speicher", substring = true)
        rule.onNodeWithTag("abbruch-kopieren").performScrollTo().performClick(); rule.onNodeWithText("✓ Kopiert").assertExists()
        rule.onNodeWithTag("abbruch-weg").performClick(); assertEquals(1, seen)
        de.raumlicht.app.Diagnose.clear(ctx); assertNull(de.raumlicht.app.Diagnose.last(ctx))
    }

    /** Die Datei vom eigenen Server bekommt die Angaben für den Sprach-Baustein angehängt – geprüft wird davor und danach */
    @Test fun angabenWerdenAngehaengt() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>(); VoicePack.dir(ctx).deleteRecursively()
        fun sha(b: ByteArray) = java.security.MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
        val source = ByteArray(40_000) { (it * 31 % 251).toByte() }; val tail = VoicePack.TAIL; val whole = source + tail
        assertEquals(127, tail.size); assertTrue(String(tail, Charsets.ISO_8859_1).contains("sample_rate")); assertEquals(VoicePack.SIZE, VoicePack.SOURCE_SIZE + tail.size)
        val l = VoicePackLoader(ctx, server(source), size = whole.size.toLong(), sha256 = sha(whole), tail = tail, sourceSha256 = sha(source)); l.start(); wait(l)
        assertEquals(PackState.Ready, l.state); assertArrayEquals(whole, VoicePack.model(ctx).readBytes())
        // falsche Quelle: wird abgelehnt, obwohl die Größe stimmt
        VoicePack.dir(ctx).deleteRecursively()
        val bad = VoicePackLoader(ctx, server(source.copyOf().also { it[7] = 9 }), size = whole.size.toLong(), sha256 = sha(whole), tail = tail, sourceSha256 = sha(source)); bad.start(); wait(bad)
        assertTrue(bad.state is PackState.Failed); assertFalse(VoicePack.model(ctx).exists())
        assertTrue(VoicePack.URL.startsWith("https://stimmen.mathenachhilfe-online.com/"))
    }
}
