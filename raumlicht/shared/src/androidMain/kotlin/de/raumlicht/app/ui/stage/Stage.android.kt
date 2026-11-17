package de.raumlicht.app.ui.stage

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Android: Die 3D-Bühne läuft in einer Web-Ansicht (assets/scene). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformStage(controller: StageController, modifier: Modifier) {
    val owner = LocalLifecycleOwner.current
    val main = Handler(Looper.getMainLooper())
    // Nach einem Verlust des Darstellungsprozesses entsteht die Web-Ansicht neu (die alte ist unbrauchbar)
    androidx.compose.runtime.key(controller.generation) { AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // Wichtig: feste Größe ansagen. Ohne das rechnet die Web-Ansicht mit „so hoch wie der Inhalt“,
                // die Seite hat dann intern die Höhe 0 und die Bühne bleibt unsichtbar.
                layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(0xFF05071A.toInt())
                settings.javaScriptEnabled = true
                settings.allowContentAccess = false
                settings.setSupportZoom(false)
                isVerticalScrollBarEnabled = false; isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                // Nur die mitgelieferte Szene – keine Navigation ins Netz
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                    // Beendet Android den Darstellungsprozess der Bühne (zu wenig Speicher oder ein Fehler darin), würde ohne diese Antwort
                    // die ganze App geschlossen. So bleibt die App offen und die Bühne wird frisch aufgebaut.
                    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                        stageNote("3D-Bühne vom System beendet (${if (detail.didCrash()) "Fehler in der Darstellung" else "zu wenig Speicher"}) – wurde neu gestartet")
                        main.post { controller.rendererGone() }
                        return true
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(m: android.webkit.ConsoleMessage): Boolean {
                        if (m.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) controller.report("${m.message()} (Zeile ${m.lineNumber()})")
                        return true
                    }
                }
                addJavascriptInterface(object { @JavascriptInterface fun event(json: String) { main.post { controller.onEvent(json) } } }, "Android")
                controller.attach { cmd -> evaluateJavascript(cmd, null) }
                loadUrl("file:///android_asset/scene/index.html")
            }
        },
        onRelease = { controller.detach(); it.destroy() },
    ) }
    // Meldet sich die Bühne nicht, sagen wir das – statt nur schwarz zu bleiben
    // Gezählt wird nur die Zeit, in der die App wirklich vorn ist: Im Hintergrund ruht die Bühne, das ist kein Fehler.
    // Meldet sie sich später doch, nimmt der Controller den Hinweis von selbst zurück.
    androidx.compose.runtime.LaunchedEffect(controller, controller.generation) {
        var waited = 0
        while (!controller.isReady && waited < 10_000) {
            kotlinx.coroutines.delay(500)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) waited += 500
        }
        if (!controller.isReady) controller.reportSlowStart()
    }
    controller.problem?.let { msg ->
        androidx.compose.material3.Text("⚠ $msg", modifier.then(Modifier.padding(16.dp)), color = androidx.compose.ui.graphics.Color(0xFFFF8A3D), fontSize = 12.sp)
    }
    DisposableEffect(owner, controller) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) controller.pause(true) else if (e == Lifecycle.Event.ON_START) controller.pause(false)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}


actual val orbitHint: String = "☝ drehen · ✌ zoomen"
