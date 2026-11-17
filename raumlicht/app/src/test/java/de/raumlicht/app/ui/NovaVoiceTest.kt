package de.raumlicht.app.ui

import androidx.test.core.app.ApplicationProvider
import de.raumlicht.app.ui.fx.NovaVoice
import de.raumlicht.app.ui.fx.Speaker
import de.raumlicht.core.model.Vorlesen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/** Stimme „Nova“: Datei aus dem Speicher, sonst vom Server, sonst erzeugen lassen – und wenn nichts geht, liest die Stimme des Geräts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NovaVoiceTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mp3 = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()) + ByteArray(600) { 7 }
    private val asked = ArrayList<Pair<String, String?>>()
    private val played = ArrayList<File>(); private val device = ArrayList<String>(); private val ducked = ArrayList<Boolean>()
    private var finish: (() -> Unit)? = null

    @Before fun clean() { File(ctx.cacheDir, "nova").deleteRecursively() }

    private fun voice(server: (String, String?) -> Pair<Int, ByteArray>) = NovaVoice(ctx, Speaker { device += it }, { ducked += it },
        fetch = { url, body -> asked += url to body; server(url, body) }, play = { f, _, done -> played += f; finish = done; {} })
    private fun settle() { Thread.sleep(150); shadowOf(android.os.Looper.getMainLooper()).idle() }

    @Test fun fixedTextComesFromTheServerAndIsKept() {
        val v = voice { url, _ -> if (url == Vorlesen.url("Hallo Welt")) 200 to mp3 else 404 to ByteArray(0) }
        v.say("Hallo  Welt"); settle()
        assertEquals(listOf(Vorlesen.url("Hallo Welt") to null), asked); assertEquals(1, played.size); assertEquals("cb1e6d67882767042ebaa54d163e450c.mp3", played[0].name)
        assertEquals(listOf(true), ducked); finish!!(); settle(); assertEquals(listOf(true, false), ducked)
        // zweites Mal: aus dem Speicher, ohne Netz
        v.say("Hallo Welt"); settle(); assertEquals(1, asked.size); assertEquals(2, played.size); assertTrue(device.isEmpty())
    }

    @Test fun runtimeTextIsRequestedWithNothingButTextAndLanguage() {
        val text = "Fast! Vektor a gleich 1, 2, 3."; val made = "${Vorlesen.SERVER}/nova/zz/neu.mp3"
        val v = voice { url, body -> when { body != null -> 200 to """{"url": "$made", "neu": true}""".toByteArray(); url == made -> 200 to mp3; else -> 404 to ByteArray(0) } }
        v.say(text); settle()
        assertEquals(listOf(Vorlesen.url(text) to null, Vorlesen.LIVE to """{"text":"$text","sprache":"de"}""", made to null), asked)
        assertEquals(1, played.size); assertTrue(device.isEmpty())
    }

    @Test fun deviceVoiceStepsInQuietly() {
        // offline
        voice { _, _ -> 0 to ByteArray(0) }.say("Geschafft!"); settle(); assertEquals(listOf("Geschafft!"), device); assertTrue(played.isEmpty())
        // Server lehnt ab (204) · zu langer Text wird gar nicht erst geschickt · Fehlerseite statt MP3 landet nicht im Speicher
        asked.clear(); voice { _, body -> if (body != null) 204 to ByteArray(0) else 404 to ByteArray(0) }.say("Nochmal."); settle(); assertEquals(2, asked.size); assertEquals(2, device.size)
        asked.clear(); val long = "a ".repeat(200).trim(); voice { _, _ -> 404 to ByteArray(0) }.say(long); settle(); assertEquals(1, asked.size); assertEquals(3, device.size)
        val v = voice { _, _ -> 200 to "<html>Fehler</html>".toByteArray() }; assertNull(v.obtain("Hallo Welt")); assertNull(File(ctx.cacheDir, "nova").listFiles()?.firstOrNull { it.name.endsWith(".mp3") })
        // Antwort mit fremder Adresse wird nicht geholt
        asked.clear(); voice { _, body -> if (body != null) 200 to """{"url":"https://fremd.example/x.mp3"}""".toByteArray() else 404 to ByteArray(0) }.say("Hm."); settle(); assertEquals(2, asked.size)
    }

    @Test fun newSentenceCutsTheOldOneAndOffMeansSilent() {
        var stopped = 0
        val v = NovaVoice(ctx, null, { ducked += it }, fetch = { _, _ -> 200 to mp3 }, play = { f, _, _ -> played += f; { stopped++ } })
        v.say("Eins."); settle(); v.say("Zwei."); settle(); assertEquals(2, played.size); assertEquals(1, stopped)
        v.stop(); assertEquals(2, stopped); assertEquals(false, ducked.last())
        v.enabled = false; v.say("Drei."); settle(); assertEquals(2, played.size)
        assertNotNull(v.obtain("Eins."))
    }

    @Test fun oldSentencesAreRemovedWhenTheStoreGrows() {
        val dir = File(ctx.cacheDir, "nova").apply { mkdirs() }
        repeat(5) { i -> File(dir, "$i.mp3").apply { writeBytes(ByteArray(1000)); setLastModified(1_000_000L * (i + 1)) } }
        NovaVoice.trim(ctx, maxBytes = 2500); assertEquals(listOf("3.mp3", "4.mp3"), dir.listFiles()!!.map { it.name }.sorted())
    }
}
