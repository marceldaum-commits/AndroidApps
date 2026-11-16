package de.experimentopia.web

// Brücke zum Browser: Speicher, Links, Ton, Vibration, Verlauf, Ladebild.

fun storeGet(key: String): String? = js("localStorage.getItem(key)")
fun storeSet(key: String, value: String): Unit = js("{ try { localStorage.setItem(key, value); } catch (e) {} }")
fun storeRemove(key: String): Unit = js("localStorage.removeItem(key)")
fun openLink(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
fun nowMs(): Double = js("Date.now()")
/** Heutiger Tag in Ortszeit, gezählt seit dem 1.1.1970 */
fun todayLocal(): Double = js("Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 86400000)")
/** Beginn des heutigen Tages (Ortszeit) in Millisekunden */
fun dayStartLocal(): Double = js("{ const d = new Date(); d.setHours(0, 0, 0, 0); return d.getTime(); }")
/** Ladebild sanft ausblenden */
fun hideStartScreen(): Unit = js("{ const s = document.getElementById('start'); if (s && s.dataset.fertig !== '1') { s.dataset.fertig = '1'; s.style.opacity = '0'; s.style.pointerEvents = 'none'; setTimeout(() => { s.style.display = 'none'; }, 380); } }")
/** Der Seite sagen, ob gerade die Startseite offen ist – nur dort (oder im Hintergrund) lädt sie sich nach einem Update selbst neu */
fun atHome(on: Boolean): Unit = js("{ window.__exHome = on; if (on && window.__exTry) window.__exTry(); }")

// ── Schrift: Dateien holen und als Bytes an die Oberfläche geben
private fun fetchBytes(url: String, done: (JsAny?) -> Unit): Unit = js("{ fetch(url).then((r) => r.ok ? r.arrayBuffer() : Promise.reject()).then((b) => done(new Int8Array(b))).catch(() => done(null)); }")
private fun size(a: JsAny): Int = js("a.length")
private fun byteAt(a: JsAny, i: Int): Byte = js("a[i]")
fun loadFile(url: String, ready: (ByteArray?) -> Unit) = fetchBytes(url) { a -> ready(a?.let { arr -> ByteArray(size(arr)) { byteAt(arr, it) } }) }

// ── Ton: dieselben im Code erzeugten Klänge wie in der Android-App, abgespielt über WebAudio.
// Browser erlauben Ton erst nach der ersten Berührung – vorher wird still nichts gespielt.
fun audioReady(): Boolean = js("{ try { const A = window.__exAudio || (window.__exAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state === 'suspended') A.resume(); return A.state === 'running'; } catch (e) { return false; } }")
fun hasBuffer(key: String): Boolean = js("!!(window.__exBufs && window.__exBufs[key])")
fun newSamples(n: Int): JsAny = js("new Float32Array(n)")
fun putSample(b: JsAny, i: Int, v: Float): Unit = js("{ b[i] = v; }")
fun storeBuffer(key: String, data: JsAny, rate: Int): Unit = js("{ const A = window.__exAudio; const buf = A.createBuffer(1, data.length, rate); buf.copyToChannel(data, 0); (window.__exBufs || (window.__exBufs = {}))[key] = buf; }")
fun playBuffer(key: String, rate: Double, gain: Double): Unit = js("{ try { const A = window.__exAudio; const b = window.__exBufs[key]; if (!A || !b) return; const s = A.createBufferSource(); s.buffer = b; s.playbackRate.value = rate; const g = A.createGain(); g.gain.value = gain; s.connect(g); g.connect(A.destination); s.start(); } catch (e) {} }")
/** Ton wecken, sobald der Nutzer das erste Mal tippt */
fun wakeAudioOnTouch(): Unit = js("{ const w = () => { try { const A = window.__exAudio || (window.__exAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state !== 'running') A.resume(); } catch (e) {} }; window.addEventListener('pointerdown', w, true); window.addEventListener('keydown', w, true); document.addEventListener('visibilitychange', () => { const A = window.__exAudio; if (!A) return; if (document.hidden) A.suspend(); else A.resume(); }); }")
fun vibrate(ms: Int): Unit = js("{ if (navigator.vibrate) navigator.vibrate(ms); }")

// ── Zurück-Taste des Browsers: Jede geöffnete Ebene bekommt einen Eintrag im Verlauf; „zurück“ schließt eine Ebene.
fun syncHistory(depth: Int, onBack: () -> Unit): Unit = js(
    "{ window.__exBack = onBack; if (window.__exDepth === undefined) { window.__exDepth = 1; window.addEventListener('popstate', () => { if (window.__exSkip > 0) { window.__exSkip--; return; } if (window.__exDepth > 1) { window.__exDepth--; window.__exBack(); } }); window.__exSkip = 0; } while (window.__exDepth < depth) { history.pushState({ ex: window.__exDepth }, ''); window.__exDepth++; } if (window.__exDepth > depth) { const n = window.__exDepth - depth; window.__exDepth = depth; window.__exSkip += 1; history.go(-n); } }")
