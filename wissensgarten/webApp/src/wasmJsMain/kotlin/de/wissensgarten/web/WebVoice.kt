@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.web

import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.NovaGeraet
import kotlinx.coroutines.await
import kotlin.js.Promise

// ---- kleine JS-Aufrufe (siehe resources/wg-stimme.js)
private fun jsHas(h: String): Promise<JsBoolean> = js("window.wg.nova.has(h)")
private fun jsLoad(url: String, h: String): Promise<JsNumber> = js("window.wg.nova.load(url, h)")
private fun jsNow(text: String, sprache: String, h: String): Promise<JsNumber> = js("window.wg.nova.now(text, sprache, h)")
private fun jsPlay(h: String, rate: Double): Promise<JsBoolean> = js("window.wg.nova.play(h, rate)")
private fun jsStopAudio(): Unit = js("window.wg.nova.stop()")
private fun jsTtsAvailable(): Boolean = js("window.wg.ttsAvailable()")
private fun jsSay(text: String, lang: String, rate: Double): Promise<JsAny?> = js("window.wg.say(text, lang, rate)")
private fun jsStopSpeaking(): Unit = js("window.wg.stopSpeaking()")

/**
 * Browser-Teil von Brummis Stimme „Nova“: MP3 vom Stimm-Server im Browser-Speicher (Cache Storage), Wiedergabe mit
 * HTMLAudioElement (playbackRate, preservesPitch) und als stille Notlösung eine Browser-Stimme, die auf dem Gerät spricht.
 */
class WebNova : NovaGeraet {
    override suspend fun imSpeicher(h: String): Boolean = runCatching { jsHas(h).await<JsBoolean>().toBoolean() }.getOrDefault(false)
    override suspend fun laden(url: String, h: String): Int = runCatching { jsLoad(url, h).await<JsNumber>().toInt() }.getOrDefault(-1)
    override suspend fun jetzt(text: String, sprache: String, h: String): Int =
        runCatching { jsNow(text, sprache, h).await<JsNumber>().toInt() }.getOrDefault(-1)

    override suspend fun abspielen(h: String, tempo: Float): Boolean =
        runCatching { jsPlay(h, tempo.toDouble()).await<JsBoolean>().toBoolean() }.getOrDefault(false)
    override fun audioStopp() { runCatching { jsStopAudio() } }

    override val geraetBereit: Boolean get() = runCatching { jsTtsAvailable() }.getOrDefault(false)
    override suspend fun geraetSprechen(text: String, locale: Locale, tempo: Float) {
        runCatching { jsSay(text, locale.tag, 0.95 * tempo).await<JsAny?>() }
    }
    override fun geraetStopp() { runCatching { jsStopSpeaking() } }
}
