@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.web

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import de.wissensgarten.app.AppViewModel
import de.wissensgarten.app.Routes
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.data.DataContainer
import de.wissensgarten.core.data.MemoryGardenStore
import de.wissensgarten.core.data.StoreState
import de.wissensgarten.core.model.Platform
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.web.res.Res
import de.wissensgarten.web.res.noto_color_emoji
import de.wissensgarten.web.res.dejavu_symbols
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import org.w3c.fetch.Response

private const val STORE_KEY = "wg.garten"
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

/** fetch ohne Optionen – ein leeres RequestInit() hätte unter Kotlin/JS „cache: null“ und scheitert im Browser */
private fun jsFetch(url: String): kotlin.js.Promise<Response> = js("fetch(url)")

private suspend fun fetchText(url: String): String {
    val r = jsFetch(url).await<Response>()
    if (!r.ok) error("$url: ${r.status}")
    return r.text().await<JsString>().toString()
}

/** Wissenspakete: dieselben JSON-Dateien wie in der Android-App, mitgeliefert unter packs/. */
private suspend fun packTexts(): List<String> = coroutineScope {
    val names = Json.parseToJsonElement(fetchText("packs/index.json")).let { it as kotlinx.serialization.json.JsonArray }
        .map { (it as kotlinx.serialization.json.JsonPrimitive).content }
    names.map { n -> async { runCatching { fetchText("packs/$n") }.getOrNull() } }.awaitAll().filterNotNull()
}

/** Plus-Pakete (vom Plus-Endpunkt geladen) liegen im Browser-Speicher. */
internal const val PLUS_PACKS_KEY = "wg.plusPakete"
private suspend fun plusPackTexts(): List<String> = runCatching {
    val raw = localStorage.getItem(PLUS_PACKS_KEY) ?: return emptyList()
    (Json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject).values.map { (it as kotlinx.serialization.json.JsonPrimitive).content }
}.getOrDefault(emptyList())

/** Wert von ?pro= lesen und aus der Adresse entfernen (der Code soll nicht im Verlauf/Lesezeichen bleiben). */
/** Freischalt-Code aus #pro=… (neu, landet nicht in Server-Protokollen) oder ?pro=… (alte Karten und Mails) – danach sofort aus der Adresse entfernen */
private fun takeProParam(): JsString? = js("(function () { var u = new URL(location.href); var v = u.searchParams.get('pro'); if (v) u.searchParams.delete('pro'); var h = (u.hash || '').replace(/^#/, ''); var hp = new URLSearchParams(h); var w = hp.get('pro'); if (w) { hp.delete('pro'); var rest = hp.toString(); u.hash = rest ? '#' + rest : ''; } var code = w || v; if (code) history.replaceState(null, '', u.pathname + u.search + u.hash); return code; })()")

private fun hideSplash(): Unit = js("(function () { var s = document.getElementById('splash'); if (s) { s.classList.add('weg'); setTimeout(function () { s.remove(); }, 600); } window.dispatchEvent(new Event('wg-ready')); })()")
/** Schritt ins Start-Protokoll (index.html, sichtbar mit ?diag=1) */
private fun diag(step: String): Unit = js("window.wgDiag && window.wgDiag(step)")
private fun diagOn(): Boolean = js("!!window.wgDiagOn")
/** Nur mit ?diag=1: Test-Einstieg für den Rundlauf der Eltern-Kopplung (Browser-Test webApp/tools/web-eltern.cjs) */
private fun onElternTest(koppeln: () -> Unit, aufheben: () -> Unit, wiederherstellen: (String) -> Unit): Unit =
    js("(function () { window.wgElternTest = function () { window.wgElternErgebnis = null; koppeln(); }; window.wgElternAufheben = function () { window.wgElternErgebnis = null; aufheben(); }; window.wgElternWiederherstellen = function (l) { window.wgElternErgebnis = null; wiederherstellen(l); }; })()")
private fun setElternErgebnis(s: String): Unit = js("window.wgElternErgebnis = s")
private fun onVisibility(cb: (Boolean) -> Unit): Unit = js("document.addEventListener('visibilitychange', function () { cb(document.visibilityState === 'visible'); })")
private fun onBeforeUnload(cb: () -> Unit): Unit = js("window.addEventListener('pagehide', function () { cb(); })")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    diag("App: main() läuft")
    // Lernstand aus dem Browser-Speicher laden (nur auf diesem Gerät, kein Server)
    val raw = runCatching { localStorage.getItem(STORE_KEY) }.getOrNull()
    val saved = raw?.let { runCatching { json.decodeFromString(StoreState.serializer(), it) }.getOrNull() } ?: StoreState()
    // Lässt sich ein gespeicherter Lernstand nicht lesen, wird er nicht still überschrieben: Kopie bleibt erhalten
    if (raw != null && saved == StoreState() && raw.length > 2)
        runCatching { if (localStorage.getItem(STORE_KEY + ".unlesbar") == null) localStorage.setItem(STORE_KEY + ".unlesbar", raw) }
    val scope = MainScope()
    var saveJob: Job? = null
    var latest: StoreState? = null
    fun saveNow() {
        val s = latest ?: return
        latest = null
        // Protokoll auf ein Jahr begrenzen, damit der Speicher nicht vollläuft
        val cut = Platform.nowMs() - 400 * Scheduler.DAY_MS
        val slim = if (s.logs.size > 20_000) s.copy(logs = s.logs.filter { it.time >= cut }) else s
        runCatching { localStorage.setItem(STORE_KEY, json.encodeToString(StoreState.serializer(), slim)) }
            .onFailure { jsToast("Der Browser-Speicher ist voll – der Lernstand konnte nicht gesichert werden.") }
    }
    val store = MemoryGardenStore(saved) { s ->
        latest = s
        saveJob?.cancel()
        saveJob = scope.launch { delay(400); saveNow() }
    }
    val container = DataContainer(store, ::localStorageStore, ::packTexts, ::plusPackTexts, de.wissensgarten.app.plusConfig(purchaseAllowed = true),
        plusPreviews = { runCatching { fetchText("plus_katalog.json") }.getOrNull() })
    val host = WebHost()
    de.wissensgarten.app.ui.play.Drawings.store = host.images
    val vm = AppViewModel(container, host)

    // Gesunde Lernzeit: sichtbare Zeit pro Tag mitzählen (nur lokal)
    var fgSince = Platform.nowMs()
    fun flush() {
        val t = Platform.nowMs()
        if (fgSince > 0 && t > fgSince) container.settings.addUsage(Scheduler.localDay(t), minOf(t - fgSince, 5 * 60_000L))
        fgSince = if (fgSince > 0) t else 0
    }
    onVisibility { visible ->
        if (visible) { fgSince = Platform.nowMs(); vm.tick(); host.music.onForeground(true) }
        else { flush(); fgSince = 0; host.music.onForeground(false); saveNow() }
    }
    onBeforeUnload { saveNow() }
    val hostWithUsage = object : de.wissensgarten.app.AppHost by host { override fun flushUsage() = flush() }

    // Freischalt-Link aus der Kauf-E-Mail: …/?pro=<Code> – übernehmen und aus der Adresszeile entfernen
    val plusMessage = takeProParam()?.toString()?.let { text ->
        when (vm.redeemPlus(text, ort = "link")) {
            de.wissensgarten.core.data.PlusEntitlements.Redeem.OK -> "🌟 Wissensgarten Plus ist freigeschaltet – vielen Dank!"
            de.wissensgarten.core.data.PlusEntitlements.Redeem.EXPIRED -> "Dieser Plus-Code ist abgelaufen."
            de.wissensgarten.core.data.PlusEntitlements.Redeem.INVALID -> "Der Freischalt-Link ist leider ungültig. Bitte den Code im Eltern-Bereich eingeben."
        }
    }
    vm.refreshPlusPacksIfDue()
    diag("App: Daten bereit → Zeichenfläche anlegen")
    if (diagOn()) onElternTest(
        koppeln = {
            scope.launch {
                val pid = vm.settings.activeProfileId.value
                if (pid == null) { setElternErgebnis("kein Profil"); return@launch }
                val k = vm.settings.elternKopplung(pid) ?: vm.elternKoppeln(pid, "Testkind")
                if (k == null) { setElternErgebnis("keine Kopplung"); return@launch }
                delay(800)
                setElternErgebnis("${vm.elternHochladen(pid)} ${vm.elternLink(k)}")
            }
        },
        aufheben = {
            val pid = vm.settings.activeProfileId.value
            if (pid == null) setElternErgebnis("kein Profil") else vm.elternAufheben(pid) { ok -> setElternErgebnis(if (ok) "aufgehoben" else "lokal aufgehoben") }
        },
        wiederherstellen = { link ->
            scope.launch {
                when (val f = vm.sicherungPruefen(link)) {
                    is de.wissensgarten.app.AppViewModel.Fund.Fehler -> setElternErgebnis("fehler " + f.text)
                    is de.wissensgarten.app.AppViewModel.Fund.Gefunden -> { vm.sicherungEinspielen(f); setElternErgebnis("ok ${f.karten} ${f.name}") }
                }
            }
        },
    )

    ComposeViewport(document.body!!) {
        LaunchedEffect(Unit) {
            diag("App: erstes Compose-Bild")
            if (diagOn()) de.wissensgarten.app.ui.fx.skiaSelbsttest { diag(it) }
        }
        // Emoji-Schrift laden (Merkbilder!), danach erst die App zeigen
        val emoji by preloadFont(Res.font.noto_color_emoji)
        val symbols by preloadFont(Res.font.dejavu_symbols)
        val resolver = LocalFontFamilyResolver.current
        var ready by remember { mutableStateOf(false) }
        LaunchedEffect(emoji, symbols) {
            val e = emoji; val s = symbols
            if (e != null && s != null) {
                diag("App: Schriften geladen")
                resolver.preload(FontFamily(e)); resolver.preload(FontFamily(s)); ready = true; hideSplash()
                plusMessage?.let { delay(900); jsToast(it) }
            }
        }
        if (ready) {
            val start = remember { if (vm.onboarded.value) Routes.GARDEN else Routes.ONBOARDING }
            WissensgartenApp(vm, hostWithUsage, start)
        } else Box(
            // Gleiche Farben wie das HTML-Startbild – nie ein weißer Bildschirm
            Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFFBF6EC)), contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF3F6B3A), trackColor = androidx.compose.ui.graphics.Color(0xFFE6EEDD)) }
    }
}
