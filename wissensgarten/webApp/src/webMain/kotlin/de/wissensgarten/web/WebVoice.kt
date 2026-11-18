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

private fun jsNovaPruefung(sprech: (JsString, JsString) -> JsString, paket: (JsString) -> JsString): Unit =
    js("{ window.wg = window.wg || {}; window.wg.novaSprech = function (t, l) { return sprech(t, l); }; window.wg.novaPaket = function (p) { return paket(p); }; }")

/**
 * Prüfhilfe (nur lesend, ohne Netz): Sprechtext und Dateiname so, wie die WebApp sie im Browser berechnet –
 * `wg.novaSprech(text, sprache)` und `wg.novaPaket(paketJson)` (leer = feste Texte). Damit prüft der Klicktest, dass
 * Browser und Vorlese-Liste (JVM) exakt dieselben Namen haben.
 */
fun novaPruefungEinrichten() = runCatching {
    fun eintrag(t: String, l: String) = "{\"text\":${kotlinx.serialization.json.JsonPrimitive(t)},\"sprache\":\"$l\",\"h\":\"${de.wissensgarten.core.model.Nova.hash(t, l)}\"}"
    jsNovaPruefung(
        { t, l -> val s = l.toString(); eintrag(de.wissensgarten.core.model.Nova.normalisiert(de.wissensgarten.core.model.Nova.sprechText(t.toString(), s)), s).toJsString() },
        { p ->
            val n = de.wissensgarten.core.model.Nova
            val list = if (p.toString().isBlank()) n.festeTexte() else de.wissensgarten.core.model.PackJson.parse(p.toString()).let { n.packTexte(it) + n.packLaufzeit(it) }
            list.joinToString(",", "[", "]") { eintrag(it.text, it.sprache) }.toJsString()
        },
    )
}

/**
 * Browser-Teil von Brummis Stimme „Nova“: MP3 vom Stimm-Server im Browser-Speicher (Cache Storage), Wiedergabe mit
 * HTMLAudioElement (playbackRate, preservesPitch) und als stille Notlösung eine Browser-Stimme, die auf dem Gerät spricht.
 * „Alle Nova-Texte offline laden“: eigene Speicher `wissensgarten-nova-offline` (Standard) und `wissensgarten-nova-original` (Bestqualität).
 */
/**
 * Wie runCatching, aber ein Abbruch (Coroutine beendet, z. B. weil Brummi schon den nächsten Satz sagt) bleibt ein Abbruch.
 * Früher wurde er als „ging nicht“ gelesen – dann sprang die Gerätestimme für den abgebrochenen Satz ein.
 */
private inline fun <T> sicher(block: () -> T): Result<T> = try { Result.success(block()) }
    catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
    catch (e: Throwable) { Result.failure(e) }

class WebNova : NovaGeraet, NovaOfflineSpeicher {
    override suspend fun imSpeicher(h: String): Boolean = sicher { jsHas(h).await<JsBoolean>().toBoolean() }.getOrDefault(false)
    override suspend fun laden(url: String, h: String): Int = sicher { jsLoad(url, h).await<JsNumber>().toInt() }.getOrDefault(-1)
    override suspend fun jetzt(text: String, sprache: String, h: String): Int =
        sicher { jsNow(text, sprache, h).await<JsNumber>().toInt() }.getOrDefault(-1)

    override suspend fun abspielen(h: String, tempo: Float): Boolean =
        sicher { jsPlay(h, tempo.toDouble()).await<JsBoolean>().toBoolean() }.getOrDefault(false)
    override fun audioStopp() { runCatching { jsStopAudio() } }

    override val geraetBereit: Boolean get() = runCatching { jsTtsAvailable() }.getOrDefault(false)
    override suspend fun geraetSprechen(text: String, locale: Locale, tempo: Float) {
        sicher { jsSay(text, locale.tag, 0.95 * tempo).await<JsAny?>() }
    }
    override fun geraetStopp() { runCatching { jsStopSpeaking() } }

    override suspend fun offlineHashes(q: NovaQualitaet): Set<String> = sicher {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(jsOfflineList(q.name).await<JsString>().toString()).toSet()
    }.getOrDefault(emptySet())
    override suspend fun sichern(url: String, h: String, q: NovaQualitaet): Int =
        sicher { jsSecure(url, h, q.name).await<JsNumber>().toInt() }.getOrDefault(-1)
    override suspend fun text(url: String): String? = sicher { jsText(url).await<JsString?>()?.toString() }.getOrNull()
    /** Die Vorlese-Liste liegt neben der WebApp */
    override suspend fun liste(): String? = text("vorlesen/texte.json")
    override suspend fun belegt(): Long = sicher { jsUsed().await<JsNumber>().toDouble().toLong() }.getOrDefault(0L)
    override suspend fun loeschen() { sicher { jsClear().await<JsAny?>() } }
}
