@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.web

import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.NovaGeraet
import de.wissensgarten.core.model.NovaOfflineSpeicher
import de.wissensgarten.core.model.NovaQualitaet
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
private fun jsOfflineList(q: String): Promise<JsString> = js("window.wg.nova.offlineList(q)")
private fun jsSecure(url: String, h: String, q: String): Promise<JsNumber> = js("window.wg.nova.secure(url, h, q)")
private fun jsText(url: String): Promise<JsString?> = js("window.wg.nova.text(url)")
private fun jsUsed(): Promise<JsNumber> = js("window.wg.nova.used()")
private fun jsClear(): Promise<JsAny?> = js("window.wg.nova.clear()")
/** Host, auf dem die WebApp läuft (für den Server-Stand der Offline-Größe) */
fun webHost(): String = js("location.hostname")

/**
 * Browser-Teil von Brummis Stimme „Nova“: MP3 vom Stimm-Server im Browser-Speicher (Cache Storage), Wiedergabe mit
 * HTMLAudioElement (playbackRate, preservesPitch) und als stille Notlösung eine Browser-Stimme, die auf dem Gerät spricht.
 * „Alle Nova-Texte offline laden“: eigene Speicher `wissensgarten-nova-offline` (Standard) und `wissensgarten-nova-original` (Bestqualität).
 */
class WebNova : NovaGeraet, NovaOfflineSpeicher {
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

    override suspend fun offlineHashes(q: NovaQualitaet): Set<String> = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(jsOfflineList(q.name).await<JsString>().toString()).toSet()
    }.getOrDefault(emptySet())
    override suspend fun sichern(url: String, h: String, q: NovaQualitaet): Int =
        runCatching { jsSecure(url, h, q.name).await<JsNumber>().toInt() }.getOrDefault(-1)
    override suspend fun text(url: String): String? = runCatching { jsText(url).await<JsString?>()?.toString() }.getOrNull()
    override suspend fun belegt(): Long = runCatching { jsUsed().await<JsNumber>().toDouble().toLong() }.getOrDefault(0L)
    override suspend fun loeschen() { runCatching { jsClear().await<JsAny?>() } }
}
