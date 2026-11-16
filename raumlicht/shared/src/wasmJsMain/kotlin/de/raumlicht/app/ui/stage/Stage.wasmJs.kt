package de.raumlicht.app.ui.stage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

// Browser: Die 3D-Bühne ist ein eigenes Fenster-Element (iframe „stage“ mit derselben scene.js wie in der Android-App). Es liegt UNTER der Oberfläche.
// Die Oberfläche lässt an der richtigen Stelle ein durchsichtiges Fenster, meldet dem Element, wohin es gehört, und reicht Berührungen weiter.
// So liegt alles, was die App über die Bühne zeichnet (Einblendungen, Konfetti), auch im Browser darüber.

private fun place(x: Double, y: Double, w: Double, h: Double, show: Boolean): Unit = js(
    "{ const f = document.getElementById('stage'); if (!f) return; f.style.display = show ? 'block' : 'none'; f.style.left = x + 'px'; f.style.top = y + 'px'; f.style.width = w + 'px'; f.style.height = h + 'px'; }")
private fun stageReady(): Boolean = js("(() => { const f = document.getElementById('stage'); return !!(f && f.contentWindow && f.contentWindow.RL); })()")
private fun stageEval(code: String): Unit = js("{ const w = document.getElementById('stage').contentWindow; if (w && w.RL) w.eval(code); }")
/** Meldungen der Bühne (bereit, gezogen, getippt …) an [handler] weiterreichen; null hängt wieder ab */
private fun listen(handler: ((String) -> Unit)?): Unit = js(
    "{ const w = document.getElementById('stage').contentWindow; window.__rlStage = handler; const hook = () => { const s = document.getElementById('stage').contentWindow; if (s) s.onSceneEvent = (e) => { if (window.__rlStage) window.__rlStage(JSON.stringify(e)); }; }; hook(); document.getElementById('stage').addEventListener('load', hook); }")
private fun pixelRatio(): Double = js("window.devicePixelRatio || 1")
private fun later(ms: Int, f: () -> Unit): Unit = js("{ setTimeout(f, ms); }")
/** Eine Berührung an die Zeichenfläche der Bühne weitergeben (x, y in Bildpunkten der Seite, gemessen ab der linken oberen Ecke der Bühne) */
private fun touch(type: String, id: Int, x: Double, y: Double, finger: Boolean): Unit = js(
    "{ const w = document.getElementById('stage').contentWindow; const c = w && w.document.getElementById('c'); if (c) c.dispatchEvent(new w.PointerEvent(type, { pointerId: id, clientX: x, clientY: y, bubbles: true, cancelable: true, isPrimary: true, pointerType: finger ? 'touch' : 'mouse', buttons: type === 'pointerup' ? 0 : 1 })); }")

/** Mausrad an die Bühne weitergeben (zoomen) */
private fun wheel(dy: Double): Unit = js("{ const w = document.getElementById('stage').contentWindow; const c = w && w.document.getElementById('c'); if (c) c.dispatchEvent(new w.WheelEvent('wheel', { deltaY: dy, bubbles: true, cancelable: true })); }")

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
    Box(modifier.onGloballyPositioned { c -> val o = c.positionInWindow(); val d = pixelRatio(); place(o.x / d, o.y / d, c.size.width / d, c.size.height / d, true) }
        .drawBehind { drawRect(Color.Black, blendMode = BlendMode.Clear) }      // Fenster zur Bühne darunter
        .pointerInput(Unit) {
            val down = HashMap<Long, Pair<Double, Double>>()       // gedrückte Zeiger mit letzter Stelle – damit keiner „hängen bleibt“
            awaitPointerEventScope { while (true) {
                val ev = awaitPointerEvent(); val d = pixelRatio(); val seen = HashSet<Long>()
                if (ev.type == PointerEventType.Scroll) { ev.changes.firstOrNull()?.let { c -> wheel(c.scrollDelta.y.toDouble()); c.consume() }; continue }
                for (c in ev.changes) {
                    val id = c.id.value; val x = c.position.x / d; val y = c.position.y / d; val finger = c.type == PointerType.Touch; seen += id
                    when {
                        c.changedToDown() -> { down[id] = x to y; touch("pointerdown", id.toInt(), x, y, finger) }
                        c.changedToUp() -> { if (down.remove(id) != null) touch("pointerup", id.toInt(), x, y, finger) }
                        c.pressed && id in down -> { down[id] = x to y; touch("pointermove", id.toInt(), x, y, finger) }
                    }
                    c.consume()
                }
                // Zeiger, die ohne „losgelassen“ verschwinden (Fenster gewechselt, Geste abgebrochen)
                down.keys.filter { it !in seen }.forEach { id -> val (x, y) = down.remove(id)!!; touch("pointercancel", id.toInt(), x, y, false) }
            } }
        })
}

private fun hasMouse(): Boolean = js("window.matchMedia && window.matchMedia('(pointer: fine)').matches")
actual val orbitHint: String get() = if (hasMouse()) "ziehen: drehen · Mausrad: zoomen" else "☝ drehen · ✌ zoomen"
