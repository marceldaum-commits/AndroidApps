@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
package de.wissensgarten.web

import androidx.compose.ui.graphics.ImageBitmap
import de.wissensgarten.app.*
import de.wissensgarten.app.ui.common.decodeImage
import de.wissensgarten.app.ui.common.encodePng
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.ui.share.CraftItem
import de.wissensgarten.core.data.KeyValueStore
import de.wissensgarten.core.data.MemoryKeyValueStore
import de.wissensgarten.core.model.Composer
import de.wissensgarten.core.model.MusicMood
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

// ---- kleine JS-Aufrufe (siehe resources/wg-platform.js)
private fun jsLoadSound(name: String, b64: String): Unit = js("window.wg.loadSound(name, b64)")
private fun jsPlay(name: String, rate: Double, vol: Double): Unit = js("window.wg.play(name, rate, vol)")
private fun jsMusicPlay(name: String, level: Double): Unit = js("window.wg.musicPlay(name, level)")
private fun jsMusicStop(): Unit = js("window.wg.musicStop()")
private fun jsMusicLevel(level: Double): Unit = js("window.wg.musicLevel(level)")
private fun jsTtsAvailable(): Boolean = js("window.wg.ttsAvailable()")
private fun jsSpeak(text: String, lang: String, queue: Boolean): Unit = js("window.wg.speak(text, lang, queue)")
private fun jsSpeaking(): Boolean = js("!!window.wg.speaking")
private fun jsStopSpeaking(): Unit = js("window.wg.stopSpeaking()")
private fun jsSrAvailable(): Boolean = js("window.wg.srAvailable()")
private fun jsListen(lang: String): Unit = js("window.wg.listen(lang)")
private fun jsSrState(): String = js("window.wg.sr.state")
private fun jsSrWords(): String = js("JSON.stringify(window.wg.sr.words)")
private fun jsCancelListen(): Unit = js("window.wg.cancelListen()")
internal fun jsToast(msg: String): Unit = js("window.wg.toast(msg)")
private fun jsShareText(text: String): Unit = js("window.wg.shareText(text)")
private fun jsSharePng(name: String, b64: String, text: String): Unit = js("window.wg.sharePng(name, b64, text)")
private fun jsPrintHtml(html: String): Unit = js("window.wg.printHtml(html)")
private fun jsStartTilt(): Unit = js("window.wg.startTilt()")
private fun jsTiltX(): Double = js("window.wg.tiltX")
private fun jsKeepAwake(on: Boolean): Unit = js("window.wg.keepAwake(on)")
private fun jsReload(): Unit = js("location.reload()")

/** Geräusche: im Code erzeugt (Synth), im Browser über WebAudio abgespielt. */
class WebSfx : Sfx {
    override var enabled = true
    init { Sound.entries.forEach { s -> jsLoadSound("sfx_${s.name}", Base64.encode(Synth.wav(Synth.render(s)))) } }
    override fun play(s: Sound, pitch: Float, volume: Float) { if (enabled) jsPlay("sfx_${s.name}", pitch.coerceIn(0.5f, 2f).toDouble(), volume.toDouble()) }
}

/** Hintergrundmusik: dieselben Stücke wie in der App (Composer + MusicSynth), als Schleife im Browser. */
class WebMusic : Music {
    private var enabled = true
    private var foreground = true
    private var ducked = false
    private var mood: MusicMood? = null
    private val loaded = mutableSetOf<MusicMood>()
    private fun level() = if (ducked) 0.06 else 0.16
    private fun load(m: MusicMood) {
        if (!loaded.add(m)) return
        val pcm = MusicSynth.render(Composer.compose(m))
        val f = FloatArray(pcm.size) { pcm[it] / 32768f }
        jsLoadSound("musik_${m.name}", Base64.encode(wavAt(f, MusicSynth.SR)))
    }
    private fun refresh() {
        val m = mood
        if (enabled && foreground && m != null) { load(m); jsMusicPlay("musik_${m.name}", level()) } else jsMusicStop()
    }
    override fun setEnabled(on: Boolean) { enabled = on; refresh() }
    override fun setMood(m: MusicMood) { mood = m; refresh() }
    override fun onForeground(on: Boolean) { foreground = on; refresh() }
    override fun duck(on: Boolean) { ducked = on; jsMusicLevel(level()) }
    override fun prepare(vararg moods: MusicMood) { /* erst bei Bedarf erzeugen – spart Ladezeit */ }

    /** 16-Bit-Mono-WAV mit eigener Abtastrate */
    private fun wavAt(samples: FloatArray, sr: Int): ByteArray {
        val w = Synth.wav(samples)
        fun i32(at: Int, v: Int) { w[at] = v.toByte(); w[at + 1] = (v shr 8).toByte(); w[at + 2] = (v shr 16).toByte(); w[at + 3] = (v shr 24).toByte() }
        i32(24, sr); i32(28, sr * 2)
        return w
    }
}

/** Brummis Stimme: Sprachausgabe des Browsers. */
class WebVoice : Voice {
    override var onSpeaking: (Boolean) -> Unit = {}
    override val isReady get() = jsTtsAvailable()
    override fun speak(text: String, locale: Locale) {
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isNotEmpty() && isReady) { onSpeaking(true); jsSpeak(clean, locale.tag, false) }
    }
    override fun stop() { jsStopSpeaking(); onSpeaking(false) }
    override suspend fun speakAndWait(text: String, locale: Locale) {
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty() || !isReady) return
        onSpeaking(true); jsSpeak(clean, locale.tag, true)
        delay(150)
        var waited = 0
        while (jsSpeaking() && waited < 30_000) { delay(100); waited += 100 }
        onSpeaking(false)
    }
}

/** Spracherkennung des Browsers (Chrome, Edge, Safari). */
class WebListener : Listener {
    override var onLevel: (Float) -> Unit = {}
    override fun available() = jsSrAvailable()
    override suspend fun listen(locale: Locale): Heard {
        if (!available()) return Heard.Failed(-1)
        jsListen(locale.tag)
        var t = 0
        while (jsSrState() == "listening" && t < 15_000) { onLevel(((t / 100) % 6) / 10f + 0.2f); delay(100); t += 100 }
        onLevel(0f)
        return when (jsSrState()) {
            "done" -> Heard.Words(Json.parseToJsonElement(jsSrWords()).jsonArray.map { it.jsonPrimitive.content })
            "silence", "listening" -> { jsCancelListen(); Heard.Silence }
            else -> Heard.Failed(1)
        }
    }
    override fun cancel() = jsCancelListen()
    override fun release() = jsCancelListen()
}

/**
 * Schlüssel-Wert-Speicher im localStorage – ein Eintrag je Name, mit Typ-Angabe, damit Zahlen und Listen genau
 * so zurückkommen wie gespeichert.
 */
fun localStorageStore(name: String): KeyValueStore {
    val key = "wg.prefs.$name"
    val initial = mutableMapOf<String, Any>()
    runCatching {
        localStorage.getItem(key)?.let { txt ->
            Json.parseToJsonElement(txt).jsonObject.forEach { (k, v) ->
                val o = v.jsonObject; val t = o["t"]!!.jsonPrimitive.content; val x = o["v"]!!
                initial[k] = when (t) {
                    "b" -> x.jsonPrimitive.boolean
                    "i" -> x.jsonPrimitive.int
                    "l" -> x.jsonPrimitive.long
                    "set" -> x.jsonArray.map { it.jsonPrimitive.content }.toSet()
                    else -> x.jsonPrimitive.content
                }
            }
        }
    }
    return MemoryKeyValueStore(initial) { map ->
        val obj = buildJsonObject {
            map.forEach { (k, v) ->
                putJsonObject(k) {
                    when (v) {
                        is Boolean -> { put("t", "b"); put("v", v) }
                        is Int -> { put("t", "i"); put("v", v) }
                        is Long -> { put("t", "l"); put("v", v) }
                        is Set<*> -> { put("t", "set"); putJsonArray("v") { v.forEach { add(it.toString()) } } }
                        else -> { put("t", "s"); put("v", v.toString()) }
                    }
                }
            }
        }
        runCatching { localStorage.setItem(key, obj.toString()) }.onFailure { jsToast("Der Browser-Speicher ist voll – bitte ältere Daten löschen.") }
    }
}

/** Bilder (selbst gemalte Merkbilder) als PNG im localStorage. */
object WebImages : ImageStore {
    private fun k(name: String) = "wg.bild.$name"
    override fun save(name: String, image: ImageBitmap) {
        runCatching { localStorage.setItem(k(name), Base64.encode(image.encodePng())) }.onFailure { jsToast("Das Bild passt nicht mehr in den Browser-Speicher.") }
    }
    override fun load(name: String) = localStorage.getItem(k(name))?.let { runCatching { decodeImage(Base64.decode(it)) }.getOrNull() }
    override fun exists(name: String) = localStorage.getItem(k(name)) != null
    override fun delete(name: String) = localStorage.removeItem(k(name))
}

/** Web-Seite der Plattform-Brücke. */
class WebHost : AppHost {
    override val features = Features(selfUpdate = false, safari = false, reminders = false, sync = false, qrScan = false,
        tilt = true, keepsakes = false, widget = false, label = "WebApp")
    override val sfx = WebSfx()
    override val music = WebMusic()
    override val voice = WebVoice().also { v -> v.onSpeaking = { music.duck(it) } }
    override val listener = WebListener()
    override val images = WebImages

    override fun toast(message: String) = jsToast(message)
    override fun shareText(text: String) = jsShareText(text)
    override fun shareImage(image: ImageBitmap, fileName: String, text: String) = jsSharePng(fileName, Base64.encode(image.encodePng()), text)
    override fun printCraftSheet(bedName: String, emoji: String, items: List<CraftItem>) = jsPrintHtml(CraftHtml.page(bedName, emoji, items))
    override suspend fun wipeAll(): String? {
        runCatching {
            val keys = (0 until localStorage.length).mapNotNull { localStorage.key(it) }.filter { it.startsWith("wg.") }
            keys.forEach { localStorage.removeItem(it) }
        }
        return null
    }
    override fun restart() = jsReload()
    override fun keepScreenOn(on: Boolean) = jsKeepAwake(on)
    override fun tilt(): Flow<Float> = flow {
        jsStartTilt()
        while (true) { emit(jsTiltX().toFloat()); delay(16) }
    }
}

/** Bastelbogen als druckbare HTML-Seite (A4: links Frage, rechts Antwort – in der Mitte falten). */
object CraftHtml {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    fun page(bed: String, emoji: String, items: List<CraftItem>): String = buildString {
        append("<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><title>Bastelbogen – ${esc(bed)}</title><style>")
        append("@page{size:A4;margin:12mm}body{font-family:system-ui,sans-serif;color:#22261f}h1{color:#3f6b3a;font-size:20px;margin:0}")
        append("p.h{color:#777;font-size:11px;margin:4px 0 10px}table{width:100%;border-collapse:collapse}td{width:50%;height:40mm;border:1px dashed #bbb;")
        append("vertical-align:top;padding:8px 10px;font-size:14px}td.a{border-left:2px dashed #8bc34a;color:#2e7d32}small{display:block;color:#9e9e9e;font-size:9px}")
        append(".e{font-size:30px}tr{page-break-inside:avoid}</style></head><body>")
        append("<h1>${esc(emoji)} ${esc(bed)} – Lernkarten zum Basteln</h1><p class=\"h\">Ausschneiden ✂, in der Mitte falten – vorne die Frage, hinten die Antwort. Wissensgarten 🐝</p><table>")
        items.forEach { c ->
            append("<tr><td><small>FRAGE</small>")
            c.img?.let { append("<div class=\"e\">${esc(it)}</div>") }
            append(esc(c.front)).append("</td><td class=\"a\"><small>ANTWORT</small>").append(esc(c.back)).append("</td></tr>")
        }
        append("</table></body></html>")
    }
}
