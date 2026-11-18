package de.raumlicht.web

// Brücke zum Browser: Speicher, Links, Ton, Stimme, Teilen, Verlauf. Die 3D-Bühne bindet das gemeinsame Modul selbst ein (Stage.wasmJs.kt).

/** Fehlersuche: Mit „?ohne=emoji,fenster,…“ in der Adresse lassen sich einzelne Teile abschalten (Knöpfe dafür auf der Diagnose-Seite) */
fun without(part: String): Boolean = js("(new URLSearchParams(location.search).get('ohne') || '').split(',').indexOf(part) >= 0")
/** Zeile in die Mitschrift für die Diagnose-Seite */
fun note(text: String): Unit = js("{ if (window.__rlNote) window.__rlNote(text); }")
fun storeGet(key: String): String? = js("localStorage.getItem(key)")
fun storeSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")
fun storeRemove(key: String): Unit = js("localStorage.removeItem(key)")
/** Alle Schlüssel mit diesem Anfang, durch Zeilenumbruch getrennt */
fun storeKeys(prefix: String): String = js("Object.keys(localStorage).filter((k) => k.startsWith(prefix)).join('\\n')")
/** Zufällige Kennung (32 Hex-Zeichen) aus dem Zufallsgenerator des Browsers */
fun randomId(): String = js("Array.from(crypto.getRandomValues(new Uint8Array(16)), (b) => b.toString(16).padStart(2, '0')).join('')")
/** JSON an den KI-Tutor schicken; [done] bekommt den Status (0 = keine Verbindung) und den Antworttext */
fun kiPost(url: String, body: String, done: (Int, String) -> Unit): Unit = js("""{
    const c = new AbortController(); const t = setTimeout(() => c.abort(), 45000);
    fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: body, signal: c.signal })
        .then((r) => r.text().then((x) => done(r.status, x)), () => done(0, '')).finally(() => clearTimeout(t));
}""")
fun hostName(): String = js("location.hostname")
/** Nur für die automatische Prüfung auf dem eigenen Rechner: anderer KI-Server (window.__rlKiServer), sonst null */
fun kiServer(): String? = js("((location.hostname === 'localhost' || location.hostname === '127.0.0.1') && window.__rlKiServer) || null")
fun openLink(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
/** Ladebild sanft ausblenden; spätestens jetzt wird auch die 3D-Bühne angefordert */
fun hideStartScreen(): Unit = js("{ const s = document.getElementById('start'); if (s && s.dataset.fertig !== '1') { s.dataset.fertig = '1'; s.style.opacity = '0'; s.style.pointerEvents = 'none'; setTimeout(() => { s.style.display = 'none'; }, 380); } if (window.__rlStage) window.__rlStage(); }")
/** Der Seite sagen, ob gerade die Startseite offen ist – nur dort (oder im Hintergrund) lädt sie sich nach einem Update selbst neu */
fun atHome(on: Boolean): Unit = js("{ window.__rlHome = on; if (on && window.__rlTry) window.__rlTry(); }")
/** Heutiger Tag in Ortszeit, gezählt seit dem 1.1.1970 */
fun todayLocal(): Double = js("Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 86400000)")

// ── Schrift: Dateien aus fonts/ holen und als Bytes an die Oberfläche geben
private fun fetchBytes(url: String, done: (JsAny?) -> Unit): Unit = js("{ fetch(url).then((r) => r.ok ? r.arrayBuffer() : Promise.reject()).then((b) => done(new Int8Array(b))).catch(() => done(null)); }")
private fun size(a: JsAny): Int = js("a.length")
private fun byteAt(a: JsAny, i: Int): Byte = js("a[i]")
/** Ruft [ready] mit dem Inhalt der Datei auf – oder mit null, wenn sie nicht zu laden war (die App läuft dann mit der Standardschrift weiter) */
fun loadFile(url: String, ready: (ByteArray?) -> Unit) = fetchBytes(url) { a -> ready(a?.let { arr -> ByteArray(size(arr)) { byteAt(arr, it) } }) }

// ── Ton: kurze Klänge aus dem Browser-Synthesizer (keine Dateien). Erst nach der ersten Berührung erlaubt der Browser Ton.
/** Kurzklang abspielen: [data] sind die im gemeinsamen Rechenkern erzeugten Samples (22050 Hz), [rate] die Abspielgeschwindigkeit (Tonhöhe) */
fun sfxKnown(name: String): Boolean = js("!!(window.__rlSfx && window.__rlSfx[name])")
fun sfxStore(name: String, data: JsAny): Unit = js("{ (window.__rlSfx || (window.__rlSfx = {}))[name] = { data: data, buf: null }; }")
fun sfxPlay(name: String, rate: Double, gain: Double): Unit = js(
    "{ try { const A = window.__rlAudio || (window.__rlAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state === 'suspended') A.resume(); const e = window.__rlSfx && window.__rlSfx[name]; if (!e) return; if (!e.buf) { e.buf = A.createBuffer(1, e.data.length, 22050); e.buf.copyToChannel(e.data, 0); } const s = A.createBufferSource(); s.buffer = e.buf; s.playbackRate.value = rate; const g = A.createGain(); g.gain.value = gain; s.connect(g); g.connect(A.destination); s.start(); } catch (e) {} }")
fun vibrate(ms: Int): Unit = js("{ if (navigator.vibrate) navigator.vibrate(ms); }")

// ── Stimme: Sprachausgabe des Browsers (rechnet auf dem Gerät bzw. im Browser, je nach System)
/** Vorgelesen wird nur mit einer deutschen Stimme, die auf dem Gerät eingebaut ist – Stimmen, die den Text an einen Server schicken, benutzt die App nicht
 *  (sonst stimmte „keine Daten an einen Server“ nicht). Solange der Browser seine Stimmen noch lädt, gilt: möglich. */
/** Stille Notlösung, wenn „Nova“ einen Satz nicht liefern kann: eine deutsche Stimme, die auf dem Gerät eingebaut ist (kein Versand an den Browser-Hersteller) */
fun speak(text: String, volume: Double, rate: Double = 1.0, voice: String = ""): Unit = js(
    "{ try { const u = new SpeechSynthesisUtterance(text); u.lang = 'de-DE'; u.volume = volume; u.rate = 0.96 * rate; const v = speechSynthesis.getVoices().filter((x) => x.localService && x.lang && x.lang.startsWith('de')); const pick = v.find((x) => x.name === voice) || v[0]; if (!pick) return; u.voice = pick; speechSynthesis.cancel(); speechSynthesis.speak(u); } catch (e) {} }")
fun speaking(): Boolean = js("('speechSynthesis' in window) && speechSynthesis.speaking")
fun stopSpeaking(): Unit = js("{ if ('speechSynthesis' in window) speechSynthesis.cancel(); }")

// ── Teilen: Teilen-Dialog des Systems, sonst Zwischenablage
fun shareText(text: String): Unit = js("{ if (navigator.share) navigator.share({ text: text }).catch(() => {}); else if (navigator.clipboard) navigator.clipboard.writeText(text).then(() => alert('In die Zwischenablage kopiert.')).catch(() => {}); }")

// ── Zurück-Taste des Browsers: Jede geöffnete Ebene bekommt einen Eintrag im Verlauf; „zurück“ schließt eine Ebene.
/** [depth] = Anzahl offener Ebenen; [onBack] wird gerufen, wenn der Nutzer im Browser zurückgeht */
fun syncHistory(depth: Int, onBack: () -> Unit): Unit = js(
    "{ window.__rlBack = onBack; if (window.__rlDepth === undefined) { window.__rlDepth = 1; window.addEventListener('popstate', () => { if (window.__rlSkip > 0) { window.__rlSkip--; return; } if (window.__rlDepth > 1) { window.__rlDepth--; window.__rlBack(); } }); window.__rlSkip = 0; } while (window.__rlDepth < depth) { history.pushState({ rl: window.__rlDepth }, ''); window.__rlDepth++; } if (window.__rlDepth > depth) { const n = window.__rlDepth - depth; window.__rlDepth = depth; window.__rlSkip += 1; history.go(-n); } }")

// ── Musik: dasselbe im Code erzeugte Stück wie in der Android-App, als Schleife über den Browser-Ton.
// Browser erlauben Ton erst nach der ersten Berührung – dann wird das Stück einmal berechnet und sanft eingeblendet. Im Hintergrund ist Ruhe.
fun newSamples(n: Int): JsAny = js("new Float32Array(n)")
fun putSample(b: JsAny, i: Int, v: Float): Unit = js("{ b[i] = v; }")
fun musicStore(mood: String, b: JsAny): Unit = js("{ window.__rlMusic.bufs[mood] = b; }")
/** Einmal beim Start: [render] berechnet ein Stück und legt es mit [musicStore] ab */
fun musicSetup(render: (String) -> Unit): Unit = js("""{
  const M = window.__rlMusic = { bufs: {}, want: null, vol: 0, cur: null, busy: false, render: render };
  M.apply = () => { const A = window.__rlAudio; if (!A || A.state !== 'running') return;
    if (M.cur && (M.cur.mood !== M.want || M.vol <= 0)) { const c = M.cur; M.cur = null; c.gain.gain.setTargetAtTime(0, A.currentTime, 0.25); setTimeout(() => { try { c.src.stop(); } catch (e) {} }, 1500); }
    if (!M.want || M.vol <= 0) return;
    if (M.cur) { M.cur.gain.gain.setTargetAtTime(M.vol, A.currentTime, 0.2); return; }
    const data = M.bufs[M.want];
    if (!data) { if (!M.busy) { M.busy = true; const w = M.want; setTimeout(() => { try { M.render(w); } catch (e) {} M.busy = false; if (M.bufs[w]) M.apply(); }, 30); } return; }
    const buf = A.createBuffer(1, data.length, 22050); buf.copyToChannel(data, 0);
    const src = A.createBufferSource(); src.buffer = buf; src.loop = true; const gain = A.createGain(); gain.gain.value = 0; gain.gain.setTargetAtTime(M.vol, A.currentTime, 0.6);
    src.connect(gain); gain.connect(A.destination); src.start(); M.cur = { mood: M.want, src: src, gain: gain }; };
  const wake = () => { try { const A = window.__rlAudio || (window.__rlAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state !== 'running') A.resume().then(M.apply); else M.apply(); } catch (e) {} };
  window.addEventListener('pointerdown', wake, true); window.addEventListener('keydown', wake, true);
  document.addEventListener('visibilitychange', () => { const A = window.__rlAudio; if (!A) return; if (document.hidden) A.suspend(); else A.resume().then(M.apply); });
}""")
/** Welches Stück laufen soll (null = keins) und wie laut (0..1) */
fun musicWant(mood: String?, vol: Double): Unit = js("{ const M = window.__rlMusic; if (!M) return; M.want = mood; M.vol = vol; M.apply(); }")

/** Wert aus der Adresszeile („?name=…“) holen und gleich wieder herausnehmen, damit er nicht versehentlich geteilt oder beim Neuladen wiederholt wird */
fun takeParam(name: String): String? = js("(function () { try { var u = new URL(location.href); var c = u.searchParams.get(name); if (c === null) return null; u.searchParams.delete(name); history.replaceState(history.state, '', u.pathname + u.search + u.hash); return c; } catch (e) { return null; } })()")
/** Freischalt-Link „?pro=<Code>“ */
fun takeProParam(): String? = takeParam("pro")
