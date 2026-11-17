@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.web

import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.NaturalVoicePack
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.app.ui.fx.VoiceOption
import de.wissensgarten.app.ui.fx.VoicePackState
import de.wissensgarten.app.ui.fx.VoiceRoute
import de.wissensgarten.core.data.VOICE_THORSTEN
import de.wissensgarten.core.model.Lautschrift
import de.wissensgarten.core.model.PiperIds
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ---- kleine JS-Aufrufe (siehe resources/wg-stimme.js)
private fun jsTtsAvailable(): Boolean = js("window.wg.ttsAvailable()")
private fun jsSpeak(text: String, lang: String, queue: Boolean, voiceId: String, rate: Double): Unit = js("window.wg.speak(text, lang, queue, voiceId, rate)")
private fun jsSpeaking(): Boolean = js("!!window.wg.speaking")
private fun jsStopSpeaking(): Unit = js("window.wg.stopSpeaking()")
private fun jsVoiceList(): String = js("window.wg.voiceList()")
private fun jsFetchText(url: String): kotlin.js.Promise<JsString?> = js("window.wg.fetchText(url)")
private fun jsThState(): String = js("window.wg.th.state")
private fun jsThPct(): Int = js("window.wg.th.pct")
private fun jsThWhy(): String = js("window.wg.th.why || ''")
private fun jsThProblem(): String = js("window.wg.th.problem || ''")
private fun jsThDownload(): Unit = js("{ window.wg.th.download(); }")
private fun jsThCancel(): Unit = js("window.wg.th.cancel()")
private fun jsThRemove(): Unit = js("{ window.wg.th.remove(); }")
private fun jsThPrepare(): Unit = js("{ window.wg.th.prepare().catch(function () {}); }")
private fun jsThUnload(): Unit = js("window.wg.th.unload()")
private fun jsThSpeak(spec: String, lengthScale: Double): Unit = js("window.wg.th.speak(spec, lengthScale)")
private fun jsThStop(): Unit = js("window.wg.th.stop()")
private fun jsThSpeaking(): Boolean = js("!!window.wg.th.speaking")
private fun jsThSupported(): Boolean = js("!!(window.wg && window.wg.th && window.wg.th.supported())")

/** Stimm-Paket „Thorsten“ im Browser: Zustand aus wg-stimme.js (Laden, Fortschritt, Fehler). */
class WebThorstenPack(private val onChanged: () -> Unit) : NaturalVoicePack {
    override val state = MutableStateFlow<VoicePackState>(VoicePackState.Missing)
    override val problem = MutableStateFlow<String?>(null)
    private val scope = MainScope()
    private var job: Job? = null

    init { watch() }

    /** Zustand aus dem Browser übernehmen – solange etwas im Gange ist */
    fun watch() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val js = jsThState()
                val before = state.value
                state.value = when (js) {
                    "loading" -> VoicePackState.Loading(jsThPct())
                    "ready" -> VoicePackState.Ready
                    "failed" -> VoicePackState.Failed(jsThWhy())
                    else -> VoicePackState.Missing
                }
                problem.value = jsThProblem().ifEmpty { null }
                if ((before == VoicePackState.Ready) != (state.value == VoicePackState.Ready)) onChanged()
                if (js != "loading" && js != "checking") break
                delay(200)
            }
        }
    }
    override fun download() { jsThDownload(); watch() }
    override fun cancel() { jsThCancel(); watch() }
    override fun remove() { jsThRemove(); watch() }
    fun refreshProblem() { problem.value = jsThProblem().ifEmpty { null } }
}

/**
 * Brummis Stimme im Browser: Stimmen des Browsers (nur solche, die auf dem Gerät sprechen) oder Thorsten.
 * Für Thorsten rechnet Kotlin die Laut-Nummern (Wörterbuch + tokens.txt, wie in der App), der Browser rechnet daraus mit
 * ONNX Runtime Web die Sprache und spielt sie per Web Audio ab.
 */
class WebVoice : Voice {
    override var onSpeaking: (Boolean) -> Unit = {}
    override val voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    private val scope = MainScope()
    private var voiceName = ""
    private var rate = 1f
    private var lexicon: Lautschrift? = null
    private var tokens: Map<Int, Int>? = null
    private var loadingLex = false
    private var watchJob: Job? = null

    private val pack: WebThorstenPack? = if (thorstenSupported()) WebThorstenPack { sync() } else null
    override val natural: NaturalVoicePack? get() = pack

    init {
        // Die Stimmenliste kommt im Browser oft erst nach einem Moment
        scope.launch { repeat(20) { val l = readVoices(); if (l.isNotEmpty()) { voices.value = l; return@launch }; delay(500) } }
    }

    private fun readVoices(): List<VoiceOption> = runCatching {
        Json.parseToJsonElement(jsVoiceList()).jsonArray.map { val o = it.jsonObject; VoiceOption(o["id"]!!.jsonPrimitive.content, o["label"]!!.jsonPrimitive.content) }
    }.getOrDefault(emptyList())

    override val isReady get() = jsTtsAvailable() || thorstenUsable()

    override fun setVoice(name: String) { voiceName = name; sync() }
    override fun setRate(percent: Int) { rate = VoiceRoute.rate(percent) }

    /** Thorsten nur vorbereiten, wenn gewählt und geladen: Wörterbuch holen, Rechen-Bausteine im Hintergrund starten */
    private fun sync() {
        val want = voiceName == VOICE_THORSTEN && pack?.state?.value == VoicePackState.Ready
        if (!want) { jsThUnload(); return }
        jsThPrepare()
        if (lexicon != null || loadingLex) return
        loadingLex = true
        scope.launch {
            runCatching {
                val t = jsFetchText("stimme/tokens.txt").await<JsString?>()?.toString()
                val w = jsFetchText("stimme/woerter.txt").await<JsString?>()?.toString()
                if (t != null && w != null) { tokens = PiperIds.tokens(t.lineSequence()); lexicon = Lautschrift(w.lineSequence()) }
            }
            loadingLex = false
        }
    }

    private fun thorstenUsable() = pack != null && voiceName == VOICE_THORSTEN && pack.state.value == VoicePackState.Ready &&
        jsThProblem().isEmpty() && lexicon != null && tokens != null

    /** Laut-Nummern je Satz („1,0,…;1,0,…“) für Thorsten – oder null, dann spricht die Browser-Stimme */
    private fun natural(text: String, locale: Locale): String? {
        if (!thorstenUsable()) return null
        val map = tokens ?: return null
        val s = VoiceRoute.natural(text, locale, voiceName, lexicon) ?: return null
        return s.joinToString(";") { PiperIds.encode(it, map).joinToString(",") }
    }

    private fun busy() = jsSpeaking() || jsThSpeaking()
    /** Musik leiser, solange gesprochen wird – auch bei Thorsten */
    private fun started() {
        onSpeaking(true)
        watchJob?.cancel()
        watchJob = scope.launch { delay(300); var t = 0; while (busy() && t < 120_000) { delay(150); t += 150 }; pack?.refreshProblem(); onSpeaking(false) }
    }

    private fun say(text: String, locale: Locale, queue: Boolean): Boolean {
        natural(text, locale)?.let { spec -> jsStopSpeaking(); jsThSpeak(spec, 1.0 / rate); started(); return true }
        jsThStop()
        val clean = Voice.stripEmoji(text).trim()
        if (clean.isEmpty() || !jsTtsAvailable()) return false
        jsSpeak(clean, locale.tag, queue, if (locale.language == "de" && voiceName != VOICE_THORSTEN) voiceName else "", 0.95 * rate)
        started(); return true
    }

    override fun speak(text: String, locale: Locale) { say(text, locale, queue = false) }
    override fun stop() { watchJob?.cancel(); jsStopSpeaking(); jsThStop(); onSpeaking(false) }
    override suspend fun speakAndWait(text: String, locale: Locale) {
        if (!say(text, locale, queue = true)) return
        delay(150)
        var waited = 0
        while (busy() && waited < 60_000) { delay(100); waited += 100 }
    }

    companion object {
        fun thorstenSupported() = runCatching { jsThSupported() }.getOrDefault(false)
    }
}
