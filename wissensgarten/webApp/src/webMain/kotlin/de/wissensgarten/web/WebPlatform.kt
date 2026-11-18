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
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
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
private fun jsSafe(safe: Boolean): Unit = js("window.wgUpdate && window.wgUpdate.safe(safe)")
private fun jsInstallWatch(cb: (String) -> Unit): Unit = js("window.wg.install.onChange(function (s) { cb(s); })")
private fun jsInstallPrompt(): Unit = js("window.wg.install.prompt()")
private fun jsInstallPostpone(): Unit = js("window.wg.install.postpone()")

/** Geräusche: im Code erzeugt (Synth), im Browser über WebAudio abgespielt – dieselben Klänge wie in der App. */
class WebSfx : Sfx {
    override var enabled = true
    override var level = 0.65f
    private val made = mutableSetOf<Sound>()
    private fun ensure(s: Sound) { if (made.add(s)) jsLoadSound("sfx_${s.name}", Base64.encode(Synth.wav(Synth.render(s)))) }
    // Erst nach dem Start in kleinen Schritten erzeugen – der Garten erscheint so ohne Wartezeit
    init { kotlinx.coroutines.MainScope().launch { delay(1500); Sound.entries.forEach { ensure(it); delay(1) } } }
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!enabled || level <= 0f) return
        ensure(s)
        jsPlay("sfx_${s.name}", pitch.coerceIn(0.5f, 2f).toDouble(), (volume * level).toDouble())
    }
}

/**
 * Hintergrundmusik: dieselben Stücke wie in der App (Composer + MusicSynth), als Schleife im Browser.
 * Die Musik entsteht erst, wenn sie gebraucht wird, und in kleinen Schritten – so ruckelt nichts und der Start bleibt schnell.
 */
class WebMusic : Music {
    private val scope = kotlinx.coroutines.MainScope()
    private var enabled = true
    private var foreground = true
    private var ducked = false
    private var unlocked = false
    private var volume = 0.45f
    private var mood: MusicMood? = null
    private val loaded = mutableSetOf<MusicMood>()
    private val loading = mutableSetOf<MusicMood>()
    /** gleiche Lautstärke-Kurve wie Android (0.16 bei 50 %) */
    private fun level() = 0.32 * volume * (if (ducked) 0.35 else 1.0)
    private fun refresh() {
        val m = mood
        if (!(enabled && foreground && unlocked && volume > 0f && m != null)) { jsMusicStop(); return }
        if (m in loaded) { jsMusicPlay("musik_${m.name}", level()); return }
        if (!loading.add(m)) return
        scope.launch {
            val pcm = MusicSynth.renderGently(Composer.compose(m)) { delay(1) }
            jsLoadSound("musik_${m.name}", wavBase64(pcm, MusicSynth.SR))
            loaded += m; loading -= m
            if (mood == m) refresh()
        }
    }
    override fun setEnabled(on: Boolean) { enabled = on; refresh() }
    override fun setMood(m: MusicMood) { mood = m; refresh() }
    override fun onForeground(on: Boolean) { foreground = on; refresh() }
    override fun duck(on: Boolean) { ducked = on; jsMusicLevel(level()) }
    override fun prepare(vararg moods: MusicMood) { /* erst bei Bedarf erzeugen – spart Ladezeit */ }
    override fun setVolume(v: Float) { val was = volume; volume = v.coerceIn(0f, 1f); if (was == 0f || volume == 0f) refresh() else jsMusicLevel(level()) }
    override fun unlock() { if (!unlocked) { unlocked = true; refresh() } }

    /** 16-Bit-Mono-WAV als Base64 – in Häppchen, damit der Browser dazwischen weiterzeichnen kann */
    private suspend fun wavBase64(pcm: ShortArray, sr: Int): String {
        val bytes = ByteArray(44 + pcm.size * 2)
        fun str(at: Int, t: String) = t.forEachIndexed { k, c -> bytes[at + k] = c.code.toByte() }
        fun i32(at: Int, v: Int) { bytes[at] = v.toByte(); bytes[at + 1] = (v shr 8).toByte(); bytes[at + 2] = (v shr 16).toByte(); bytes[at + 3] = (v shr 24).toByte() }
        fun i16(at: Int, v: Int) { bytes[at] = v.toByte(); bytes[at + 1] = (v shr 8).toByte() }
        str(0, "RIFF"); i32(4, 36 + pcm.size * 2); str(8, "WAVE"); str(12, "fmt "); i32(16, 16); i16(20, 1); i16(22, 1)
        i32(24, sr); i32(28, sr * 2); i16(32, 2); i16(34, 16); str(36, "data"); i32(40, pcm.size * 2)
        var i = 0
        while (i < pcm.size) {
            val end = minOf(pcm.size, i + 200_000)
            while (i < end) { val v = pcm[i].toInt(); bytes[44 + 2 * i] = v.toByte(); bytes[45 + 2 * i] = (v shr 8).toByte(); i++ }
            delay(1)
        }
        val sb = StringBuilder(bytes.size / 3 * 4 + 4)
        val step = 300_000                                  // durch 3 teilbar: Stücke lassen sich einfach aneinanderhängen
        var at = 0
        while (at < bytes.size) { val to = minOf(bytes.size, at + step); sb.append(Base64.encode(bytes, at, to)); at = to; delay(1) }
        return sb.toString()
    }
}

// Brummis Stimme „Nova“ (mit Browser-Stimme als Notlösung): siehe WebVoice.kt

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
        tilt = true, keepsakes = false, widget = false, label = "WebApp", plusPurchase = true)
    override val sfx = WebSfx()
    override val music = WebMusic()
    override val voice = run {
        val scope = kotlinx.coroutines.MainScope()
        val nova = WebNova()
        novaPruefungEinrichten()
        // „Alle Nova-Texte offline laden“: eigene Liste und Server-Stand des Hosts, auf dem die WebApp läuft
        de.wissensgarten.app.ui.fx.NovaVoice(nova, scope, de.wissensgarten.app.ui.fx.NovaOfflineManager(nova, scope, "vorlesen/texte.json", webHost()))
    }.also { v -> v.onSpeaking = { music.duck(it) } }
    override val listener = WebListener()
    override val images = WebImages

    // „App installieren“ (PWA): Stand kommt aus wg-platform.js („prompt“, „ios“ oder „none“, dazu „:spaeter“)
    private val install = kotlinx.coroutines.flow.MutableStateFlow(InstallOffer())
    override val installOffer: kotlinx.coroutines.flow.StateFlow<InstallOffer> = install
    init {
        runCatching {
            jsInstallWatch { s ->
                val mode = when (s.substringBefore(':')) { "prompt" -> InstallMode.PROMPT; "ios" -> InstallMode.IOS_GUIDE; else -> InstallMode.NONE }
                install.value = InstallOffer(mode, later = s.endsWith(":spaeter"))
            }
        }
    }
    override fun requestInstall() = jsInstallPrompt()
    override fun postponeInstall() = jsInstallPostpone()

    override fun toast(message: String) = jsToast(message)
    override fun setSafeToUpdate(safe: Boolean) = jsSafe(safe)

    // Plus-Pakete: gleicher Server wie die WebApp (relativ), Code im Header – nichts landet in Adresse oder Protokoll
    override suspend fun fetchPlusPacks(code: String, known: Map<String, Int>): Int {
        val store = runCatching {
            (kotlinx.serialization.json.Json.parseToJsonElement(kotlinx.browser.localStorage.getItem(PLUS_PACKS_KEY) ?: "{}") as kotlinx.serialization.json.JsonObject)
                .mapValues { (it.value as kotlinx.serialization.json.JsonPrimitive).content }.toMutableMap()
        }.getOrDefault(mutableMapOf())
        val n = de.wissensgarten.core.data.PackUpdates(
            get = { name -> fetchLicensed("wg-api/plus/$name", code).await<JsString?>()?.toString() },
            save = { name, text -> store[name] = text },
        ).fetch(known)
        if (n > 0) runCatching {
            kotlinx.browser.localStorage.setItem(PLUS_PACKS_KEY, kotlinx.serialization.json.JsonObject(store.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) }).toString())
        }.onFailure { jsToast("Der Browser-Speicher ist voll – Plus-Pakete konnten nicht gespeichert werden.") }
        return n
    }
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
    @androidx.compose.runtime.Composable
    override fun WelcomeRestore(onRestored: (Boolean) -> Unit) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            androidx.compose.material3.Text("Dein Fortschritt bleibt nur in diesem Browser – kein Konto, keine Werbung.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            de.wissensgarten.app.ui.settings.LegalLinks()
        }
    }
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

/** GET mit Lizenz-Header; null bei Fehler (403/404/offline). */
private fun fetchLicensed(url: String, code: String): kotlin.js.Promise<JsString?> =
    js("fetch(url, { headers: { 'X-Lizenz': code }, cache: 'no-store' }).then(function (r) { return r.ok ? r.text() : null; }).catch(function () { return null; })")
