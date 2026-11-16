package de.raumlicht.app.ui.stage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

// Browser: Die 3D-Bühne ist ein eigenes Fenster-Element (iframe „stage“ mit derselben scene.js wie in der Android-App).
// Die Oberfläche lässt an der richtigen Stelle Platz und meldet dem Element, wohin es gehört.

private fun place(x: Double, y: Double, w: Double, h: Double, show: Boolean): Unit = js(
    "{ const f = document.getElementById('stage'); if (!f) return; f.style.display = show ? 'block' : 'none'; f.style.left = x + 'px'; f.style.top = y + 'px'; f.style.width = w + 'px'; f.style.height = h + 'px'; }")
private fun stageReady(): Boolean = js("(() => { const f = document.getElementById('stage'); return !!(f && f.contentWindow && f.contentWindow.RL); })()")
private fun stageEval(code: String): Unit = js("{ const w = document.getElementById('stage').contentWindow; if (w && w.RL) w.eval(code); }")
/** Meldungen der Bühne (bereit, gezogen, getippt …) an [handler] weiterreichen; null hängt wieder ab */
private fun listen(handler: ((String) -> Unit)?): Unit = js(
    "{ const w = document.getElementById('stage').contentWindow; window.__rlStage = handler; const hook = () => { const s = document.getElementById('stage').contentWindow; if (s) s.onSceneEvent = (e) => { if (window.__rlStage) window.__rlStage(JSON.stringify(e)); }; }; hook(); document.getElementById('stage').addEventListener('load', hook); }")
private fun pixelRatio(): Double = js("window.devicePixelRatio || 1")
private fun later(ms: Int, f: () -> Unit): Unit = js("{ setTimeout(f, ms); }")

@Composable
actual fun PlatformStage(controller: StageController, modifier: Modifier) {
    DisposableEffect(controller) {
        var alive = true
        listen { json -> if (alive) controller.onEvent(json) }
        controller.attach { cmd -> stageEval(cmd) }
        // Die Bühne ist meist schon geladen und meldet „bereit“ dann nicht noch einmal – wir fragen nach und geben Bescheid
        fun probe(tries: Int) { if (!alive) return; if (stageReady()) controller.onEvent("{\"type\":\"ready\"}") else if (tries > 0) later(120) { probe(tries - 1) } }
        probe(100)
        onDispose { alive = false; controller.detach(); listen(null); place(0.0, 0.0, 0.0, 0.0, false) }
    }
    Box(modifier.onGloballyPositioned { c -> val o = c.positionInWindow(); val d = pixelRatio(); place(o.x / d, o.y / d, c.size.width / d, c.size.height / d, true) })
}
