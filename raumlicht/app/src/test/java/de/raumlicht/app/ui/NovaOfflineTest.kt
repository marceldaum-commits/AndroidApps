package de.raumlicht.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.raumlicht.app.ui.fx.NovaOffline
import de.raumlicht.app.ui.fx.NovaVoice
import de.raumlicht.app.ui.settings.OfflineVoice
import de.raumlicht.app.ui.settings.OfflineVoiceCard
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Vorlesen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Collections

/** „Alle Nova-Texte offline laden“: nur Fehlendes, zwei Qualitäten, Abbrechen und Weitermachen, Löschen – und die Wiedergabe nimmt die geladenen Dateien. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class NovaOfflineTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val mp3 = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()) + ByteArray(1000) { 7 }
    private val big = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()) + ByteArray(3000) { 9 }
    private val texts = listOf("Eins.", "Zwei.", "Drei.", "Vier.", "Fünf.", "Sechs."); private val keys = texts.map { Vorlesen.key(it) }
    private val asked: MutableList<String> = Collections.synchronizedList(ArrayList())

    @Before fun clean() { File(ctx.filesDir, "nova-offline").deleteRecursively(); File(ctx.cacheDir, "nova").deleteRecursively() }
    private fun wait(o: NovaOffline) { val end = System.currentTimeMillis() + 5000; Thread.sleep(60); while (o.loading && System.currentTimeMillis() < end) Thread.sleep(20); Thread.sleep(60) }

    @Test fun loadsOnlyWhatIsMissingInBothQualities() {
        // Server: Standard gibt es für alle außer dem sechsten Text (404), Bestqualität nur für die ersten vier
        val server = { url: String, _: String? -> asked += url; val k = url.substringAfterLast('/').removeSuffix(".mp3"); val i = keys.indexOf(k)
            when { url.contains("stand-raumlicht.math") -> 404 to ByteArray(0)
                url.contains("stand-raumlicht-test") -> 200 to """{"stimme":"nova","texte":6,"bytes":5015,"bytes_original":12012}""".toByteArray()
                url.contains("/original/") -> if (i in 0..3) 200 to big else 404 to ByteArray(0)
                else -> if (i in 0..4) 200 to mp3 else 404 to ByteArray(0) } }
        val o = NovaOffline(ctx, server) { keys }
        o.refresh(); Thread.sleep(300); assertEquals(6, o.total); assertEquals(0, o.standardDone); assertEquals(5015L, o.standardBytes); assertEquals(12012L, o.bestBytes)   // Live-Stand fehlt: Stand der Testadresse
        asked.clear(); o.start(best = false); wait(o)
        assertEquals(5, o.standardDone); assertEquals(5 * 1003L, o.standardStored); assertEquals(6, asked.size); assertEquals(6, o.done)       // der fehlende (404) wird still übersprungen
        // nochmal tippen: nur der eine fehlende wird wieder versucht
        asked.clear(); o.start(false); wait(o); assertEquals(listOf(Vorlesen.fileUrl(keys[5])), asked.toList())
        // Bestqualität: aus „original“, gibt es sie dort nicht, die normale Datei
        asked.clear(); o.start(best = true); wait(o)
        assertEquals(5, o.bestDone); assertEquals(4 * 3003L + 1003L, o.bestStored); assertEquals(6 + 2, asked.size)
        // die Wiedergabe nimmt die geladene Datei – Bestqualität zuerst, ganz ohne Netz
        val v = NovaVoice(ctx, fetch = { _, _ -> throw AssertionError("kein Netz nötig") })
        assertEquals(3003L, v.obtain("Eins.")!!.length()); assertEquals(1003L, v.obtain("Fünf.")!!.length())
        // löschen: beide Qualitäten weg
        o.delete(); Thread.sleep(300); assertEquals(0, o.standardDone); assertEquals(0, o.bestDone); assertEquals(0L, o.standardStored + o.bestStored)
        assertNull(NovaOffline.find(ctx, keys[0]))
    }

    @Test fun cancelStopsAndNextTapContinues() {
        var o: NovaOffline? = null
        val server = { url: String, _: String? -> asked += url; if (asked.size >= 2) o!!.cancel(); Thread.sleep(30); 200 to mp3 }
        o = NovaOffline(ctx, server) { keys }
        o.start(false); wait(o); assertFalse(o.loading); assertTrue(o.standardDone in 1..5)
        val before = o.standardDone; asked.clear()
        val rest = NovaOffline(ctx, { url, _ -> asked += url; 200 to mp3 }) { keys }; rest.start(false); wait(rest)
        assertEquals(6, rest.standardDone); assertEquals(6 - before, asked.size); assertNotNull(NovaOffline.find(ctx, keys[5]))
    }

    @Test fun buttonsInSettings() {
        var o by mutableStateOf(OfflineVoice(false, 0, 679, 0, 0, 0, 0, 63_975_168, null, false, {}, {}, {}))
        val did = ArrayList<String>()
        fun model(loading: Boolean = false, done: Int = 0, sd: Int = 0, bd: Int = 0, ss: Long = 0, bs: Long = 0, bestBytes: Long? = null, best: Boolean = false) =
            OfflineVoice(loading, done, 679, sd, bd, ss, bs, 63_975_168, bestBytes, best, { did += if (it) "best" else "standard" }, { did += "abbrechen" }, { did += "loeschen" })
        o = model()
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { OfflineVoiceCard(o) } } }
        rule.onNodeWithTag("offline-standard").assertTextEquals("Offline laden – Standard (ca. 61 MB)"); rule.onNodeWithTag("offline-best").assertTextEquals("Offline laden – Bestqualität")
        rule.onNodeWithTag("offline-loeschen").assertDoesNotExist()
        rule.onNodeWithTag("offline-best").performClick(); rule.onNodeWithTag("offline-standard").performClick(); assertEquals(listOf("best", "standard"), did)
        o = model(loading = true, done = 212); rule.onNodeWithTag("offline-stand").assertTextEquals("Lädt … 212 von 679"); rule.onNodeWithTag("offline-abbrechen").performClick()
        rule.onRoot().captureRoboImage("../screenshots/81_nova_offline.png")
        // abgebrochen: weiter laden (Rest), löschen möglich; Bestqualität mit Größe, sobald der Server sie meldet
        o = model(sd = 212, ss = 20_000_000, bestBytes = 204_000_000)
        rule.onNodeWithTag("offline-standard").assertTextEquals("Weiter laden – Standard (ca. 42 MB) · 212 von 679"); rule.onNodeWithTag("offline-best").assertTextEquals("Offline laden – Bestqualität (ca. 195 MB)")
        rule.onNodeWithTag("offline-loeschen").assertTextContains("Offline-Stimme löschen (19 MB)").performClick()
        o = model(sd = 679, ss = 63_975_168); rule.onNodeWithTag("offline-standard-fertig").assertTextEquals("✓ Standard ist vollständig geladen"); rule.onNodeWithTag("offline-standard").assertDoesNotExist()
        assertEquals(listOf("best", "standard", "abbrechen", "loeschen"), did)
    }
}
