package de.raumlicht.web

// Brücke zum Browser: 3D-Bühne (iframe mit derselben scene.js wie in der Android-App), Speicher und Links.

/** Bühne an die Stelle legen, an der die Oberfläche Platz für sie lässt (CSS-Pixel) – oder verstecken */
fun placeStage(x: Double, y: Double, w: Double, h: Double, show: Boolean): Unit = js(
    "{ const f = document.getElementById('stage'); f.style.display = show ? 'block' : 'none'; f.style.left = x + 'px'; f.style.top = y + 'px'; f.style.width = w + 'px'; f.style.height = h + 'px'; }")

/** Befehl an die Bühne (RL.setState …). Ist sie noch nicht geladen, wartet der Befehl. */
fun stageRun(code: String): Unit = js(
    "{ const w = document.getElementById('stage').contentWindow; const go = () => { if (w && w.RL) { w.eval(code); return true; } return false; }; if (!go()) { window.__rlq = window.__rlq || []; window.__rlq.push(code); if (!window.__rlt) window.__rlt = setInterval(() => { const w2 = document.getElementById('stage').contentWindow; if (w2 && w2.RL) { clearInterval(window.__rlt); window.__rlt = null; window.__rlq.splice(0).forEach((c) => w2.eval(c)); } }, 100); } }")

fun storeGet(key: String): String? = js("localStorage.getItem(key)")
fun storeSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")
fun openLink(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
fun hideStartScreen(): Unit = js("{ const s = document.getElementById('start'); if (s) s.style.display = 'none'; }")
/** Der Seite sagen, ob gerade die Startseite offen ist – nur dort (oder im Hintergrund) lädt sie sich nach einem Update selbst neu */
fun atHome(on: Boolean): Unit = js("{ window.__rlHome = on; if (on && window.__rlTry) window.__rlTry(); }")
fun pixelRatio(): Double = js("window.devicePixelRatio || 1")

// Schrift laden: zwei Dateien aus fonts/ holen und als Bytes an die Oberfläche geben
private fun fetchBytes(url: String, done: (JsAny?) -> Unit): Unit = js("{ fetch(url).then((r) => r.arrayBuffer()).then((b) => done(new Int8Array(b))).catch(() => done(null)); }")
private fun size(a: JsAny): Int = js("a.length")
private fun byteAt(a: JsAny, i: Int): Byte = js("a[i]")
private fun bytes(a: JsAny) = ByteArray(size(a)) { byteAt(a, it) }
/** Ruft [ready] auf, sobald beide Schnitte da sind. Klappt das Laden nicht, bleibt die Standardschrift – die App startet trotzdem. */
fun loadFonts(ready: (ByteArray, ByteArray) -> Unit) {
    fetchBytes("fonts/dejavu_sans.ttf") { a -> fetchBytes("fonts/dejavu_sans_bold.ttf") { b -> if (a != null && b != null) ready(bytes(a), bytes(b)) else hideStartScreen() } }
}
