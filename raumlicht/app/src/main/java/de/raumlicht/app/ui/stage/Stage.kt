package de.raumlicht.app.ui.stage

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.raumlicht.core.model.Beam
import de.raumlicht.core.model.V3
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fernbedienung für die 3D-Bühne (assets/scene). Die Bühne zeichnet nur; gerechnet und geprüft wird in core/model.
 * Befehle, die vor dem Laden ankommen, werden gesammelt und danach ausgeführt.
 */
class StageController {
    private var web: WebView? = null
    private var ready = false
    private var state: String? = null
    private val queue = ArrayList<String>()

    /** Griff wurde in der Bühne gezogen: neue (ganzzahlige) Position */
    var onDrag: ((id: String, p: List<Int>) -> Unit)? = null
    /** Zuletzt gesetzter Zustand (für Tests) */
    val lastState get() = state
    val log = ArrayList<String>()

    fun setState(json: String) { state = json; if (ready) run("RL.setState($json)") }
    fun fire(b: Beam, ms: Int) = send("RL.fire({from:${js(b.from)},to:${js(b.to)},color:'${b.color}',ms:$ms,hit:${b.hit}})")
    fun burst(p: V3, color: String) = send("RL.burst(${js(p)},'$color')")
    fun party(seconds: Double = 4.0) = send("RL.party($seconds)")
    fun camera(az: Double? = null, el: Double? = null, dist: Double? = null) =
        send("RL.setCamera({${listOfNotNull(az?.let { "az:$it" }, el?.let { "el:$it" }, dist?.let { "dist:$it" }).joinToString(",")}})")

    private fun js(v: V3) = "[${v.x},${v.y},${v.z}]"
    private fun send(cmd: String) { log += cmd; if (ready) run(cmd) else queue += cmd }
    private fun run(cmd: String) { web?.evaluateJavascript(cmd, null) }

    internal fun attach(w: WebView) { web = w; ready = false }
    internal fun detach() { web = null; ready = false }
    internal fun onEvent(json: String) {
        val o = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return
        when (o["type"]?.jsonPrimitive?.content) {
            "ready" -> { ready = true; state?.let { run("RL.setState($it)") }; queue.forEach(::run); queue.clear() }
            "drag" -> onDrag?.invoke(o.getValue("id").jsonPrimitive.content, o.getValue("p").jsonArray.map { it.jsonPrimitive.int })
        }
    }
    internal fun pause(on: Boolean) { if (ready) run(if (on) "RL.pause()" else "RL.resume()") }
}

/** Wie die Bühne gezeichnet wird – in Tests durch ein Standbild ersetzt (Robolectric hat kein WebGL). */
val LocalStageRenderer = staticCompositionLocalOf<@Composable (StageController, Modifier) -> Unit> { { c, m -> WebStage(c, m) } }

@Composable
fun Stage(controller: StageController, modifier: Modifier = Modifier) = LocalStageRenderer.current(controller, modifier)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebStage(controller: StageController, modifier: Modifier) {
    val owner = LocalLifecycleOwner.current
    val main = Handler(Looper.getMainLooper())
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0xFF05071A.toInt())
                settings.javaScriptEnabled = true
                settings.allowContentAccess = false
                settings.setSupportZoom(false)
                isVerticalScrollBarEnabled = false; isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                // Nur die mitgelieferte Szene – keine Navigation ins Netz
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                }
                addJavascriptInterface(object { @JavascriptInterface fun event(json: String) { main.post { controller.onEvent(json) } } }, "Android")
                controller.attach(this)
                loadUrl("file:///android_asset/scene/index.html")
            }
        },
        onRelease = { controller.detach(); it.destroy() },
    )
    DisposableEffect(owner, controller) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) controller.pause(true) else if (e == Lifecycle.Event.ON_START) controller.pause(false)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}
