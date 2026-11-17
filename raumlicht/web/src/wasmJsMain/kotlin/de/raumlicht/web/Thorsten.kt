@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.raumlicht.web

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Lautschrift
import de.raumlicht.core.model.PiperIds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- kleine JS-Aufrufe (siehe resources/rl-stimme.js)
private fun jsState(): String = js("(window.__rlThorsten ? window.__rlThorsten.state : 'unsupported')")
private fun jsPct(): Int = js("(window.__rlThorsten ? window.__rlThorsten.pct : 0)")
private fun jsWhy(): String = js("(window.__rlThorsten && window.__rlThorsten.why) || ''")
private fun jsProblem(): String = js("(window.__rlThorsten && window.__rlThorsten.problem) || ''")
private fun jsDownload(): Unit = js("{ window.__rlThorsten.download(); }")
private fun jsCancel(): Unit = js("{ window.__rlThorsten.cancel(); }")
private fun jsRemove(): Unit = js("{ window.__rlThorsten.remove(); }")
private fun jsPrepare(): Unit = js("{ window.__rlThorsten.prepare().catch(function () {}); }")
private fun jsUnload(): Unit = js("{ if (window.__rlThorsten) window.__rlThorsten.unload(); }")
private fun jsSpeak(spec: String, lengthScale: Double, volume: Double): Unit = js("{ window.__rlThorsten.speak(spec, lengthScale, volume); }")
private fun jsStop(): Unit = js("{ if (window.__rlThorsten) window.__rlThorsten.stop(); }")
private fun jsSpeaking(): Boolean = js("!!(window.__rlThorsten && window.__rlThorsten.speaking)")
private fun jsFetchText(url: String): kotlin.js.Promise<JsString?> = js("fetch(url).then(function (r) { return r.ok ? r.text() : null; }).catch(function () { return null; })")

/**
 * Die natürliche Stimme „Thorsten“ in der WebApp – dieselbe Stimme und dieselbe Lautschrift wie in der Android-App.
 * Kotlin setzt den Text in Laut-Nummern um ([Lautschrift], [PiperIds]); der Browser rechnet daraus die Sprache (rl-stimme.js, ONNX Runtime Web).
 * Die Stimmdatei wird nur auf Wunsch geladen (eigener Server), geprüft und im Browser gespeichert; danach geht es ohne Internet.
 */
class WebThorsten(private val prefs: WebPrefs,
                  /** Thorsten kann einen wartenden Satz doch nicht sprechen (unbekanntes Wort) – dann liest ihn die Stimme des Geräts */
                  private val onFallback: (String) -> Unit = {}) {
    /** checking · unsupported · missing · loading · ready · failed */
    var state by mutableStateOf(jsState()); private set
    var percent by mutableStateOf(0); private set
    var why by mutableStateOf(""); private set
    var problem by mutableStateOf(""); private set
    /** vom Nutzer eingeschaltet (gemerkt) */
    var chosen by mutableStateOf(prefs.voiceNatural); private set
    private var lexicon: Lautschrift? = null
    private var tokens: Map<Int, Int>? = null
    private var loadingLex = false
    /** Satz, der kam, während die Lautschrift noch lud – wird gesprochen, sobald sie da ist (sonst ginge nach dem Start der erste Satz verloren) */
    private var waiting: Triple<String, Double, Double>? = null
    private val scope = MainScope()

    init { watch(); }

    /** Zustand aus dem Browser übernehmen – solange etwas im Gange ist */
    private fun watch() { scope.launch { while (true) {
        val s = jsState(); val before = state; state = s; percent = jsPct(); why = jsWhy(); problem = jsProblem()
        if (s == "ready" && before != "ready") sync()
        if (s != "loading" && s != "checking") break
        delay(200) } } }

    /** Thorsten nur vorbereiten, wenn gewählt und geladen: Lautschrift holen, Rechen-Bausteine im Hintergrund starten */
    private fun sync() {
        if (!(chosen && state == "ready")) { jsUnload(); return }
        jsPrepare()
        if (lexicon != null || loadingLex) return
        loadingLex = true
        scope.launch {
            runCatching {
                val t = jsFetchText("stimme/tokens.txt").await<JsString?>()?.toString()
                val s = jsFetchText("stimme/saetze.txt").await<JsString?>()?.toString()
                val w = jsFetchText("stimme/woerter.txt").await<JsString?>()?.toString()
                if (t != null && s != null && w != null) { tokens = PiperIds.tokens(t.lineSequence()); lexicon = Lautschrift(s.lineSequence(), w.lineSequence()) }
            }
            loadingLex = false
            waiting?.let { (clean, volume, rate) -> waiting = null; if (!say(clean, volume, rate)) onFallback(clean) }
        }
    }
    fun download() { jsDownload(); watch() }
    fun cancel() { jsCancel(); watch() }
    fun remove() { use(false); jsRemove(); watch() }
    fun use(on: Boolean) { chosen = on; prefs.voiceNatural = on; if (!on) { jsStop(); waiting = null }; sync() }
    val active get() = chosen && state == "ready"
    private fun usable() = active && jsProblem().isEmpty() && lexicon != null && tokens != null
    val speaking get() = jsSpeaking()
    fun stop() { waiting = null; jsStop() }

    /** Liest [clean] (schon sprechfertig) mit Thorsten vor. false = geht gerade nicht (nicht geladen, noch nicht bereit, unbekanntes Wort) – dann liest die Stimme des Geräts. */
    fun say(clean: String, volume: Double, rate: Double): Boolean {
        problem = jsProblem()
        if (!usable()) {
            if (!active || jsProblem().isNotEmpty()) return false
            // geladen und eingeschaltet, aber die Lautschrift kommt gerade erst: den Satz merken statt ihn der Geräte-Stimme zu geben
            sync(); if (loadingLex) { waiting = Triple(clean, volume, rate); return true }
            return false
        }
        val laute = lexicon?.of(clean) ?: return false
        val map = tokens ?: return false
        jsSpeak(laute.joinToString(";") { PiperIds.encode(it, map).joinToString(",") }, 1.0 / rate.coerceIn(0.5, 2.0), volume.coerceIn(0.0, 1.0))
        return true
    }
}

/** Natürliche Stimme in den Einstellungen der WebApp: einmal laden (freiwillig), ein- und ausschalten, wieder entfernen – wie in der Android-App */
@Composable
fun ThorstenCard(th: WebThorsten, onProbe: () -> Unit) {
    if (th.state == "unsupported" || th.state == "checking") return
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("✨ Natürliche Stimme „Thorsten“", style = MaterialTheme.typography.bodyLarge)
        when (th.state) {
            "ready" -> {
                Text(th.problem.ifEmpty { if (th.active) "Lux spricht mit der natürlichen Stimme – ganz ohne Internet." else "Geladen. Schalte sie ein, damit Lux so spricht." }, Modifier.testTag("stimme-status"),
                    style = MaterialTheme.typography.bodySmall, color = if (th.problem.isNotEmpty()) Q.Orange else if (th.active) Q.Green else Q.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ th.use(!th.active); if (th.active) onProbe() }, Modifier.testTag("stimme-nutzen")) { Text(if (th.active) "Geräte-Stimme nehmen" else "Einschalten") }
                    OutlinedButton({ th.remove() }, Modifier.testTag("stimme-entfernen")) { Text("Entfernen (63 MB frei)") }
                }
            }
            "loading" -> {
                Text("Lädt … ${th.percent} %", Modifier.testTag("stimme-status"), style = MaterialTheme.typography.bodySmall, color = Q.Cyan)
                LinearProgressIndicator({ th.percent / 100f }, Modifier.fillMaxWidth())
                OutlinedButton({ th.cancel() }, Modifier.testTag("stimme-abbrechen")) { Text("Abbrechen") }
            }
            else -> {
                Text(if (th.state == "failed") "Das hat nicht geklappt: ${th.why}" else "Klingt viel weniger nach Automat und rechnet in deinem Browser. Einmaliger Download von 63 MB von unserem Server (am besten im WLAN), danach ohne Internet.",
                    Modifier.testTag("stimme-status"), style = MaterialTheme.typography.bodySmall, color = if (th.state == "failed") Q.Orange else Q.Muted)
                OutlinedButton({ th.download() }, Modifier.testTag("stimme-laden")) { Text("Stimme laden (63 MB)") }
            }
        }
    }
}
