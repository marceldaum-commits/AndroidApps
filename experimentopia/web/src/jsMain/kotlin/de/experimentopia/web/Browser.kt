package de.experimentopia.web

// Brücke zum Browser – Fassung für Kotlin/JS (Ersatz für Safari 27, das Kotlin/Wasm nicht verarbeitet). Gleiche Funktionen wie wasmJsMain/Browser.kt.

fun storeGet(key: String): String? = js("localStorage.getItem(key)")
fun storeSet(key: String, value: String): Unit = js("(function () { try { localStorage.setItem(key, value); } catch (e) {} })()")
fun storeRemove(key: String): Unit = js("localStorage.removeItem(key)")
fun openLink(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
fun nowMs(): Double = js("Date.now()")
/** Heutiger Tag in Ortszeit, gezählt seit dem 1.1.1970 */
fun todayLocal(): Double = js("Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 86400000)")
/** Beginn des heutigen Tages (Ortszeit) in Millisekunden */
fun dayStartLocal(): Double = js("(function () { var d = new Date(); d.setHours(0, 0, 0, 0); return d.getTime(); })()")
/** Ladebild sanft ausblenden */
fun hideStartScreen(): Unit = js("(function () { var s = document.getElementById('start'); if (s && s.dataset.fertig !== '1') { s.dataset.fertig = '1'; s.style.opacity = '0'; s.style.pointerEvents = 'none'; setTimeout(function () { s.style.display = 'none'; }, 380); } })()")
/** Der Seite sagen, ob gerade die Startseite offen ist – nur dort (oder im Hintergrund) lädt sie sich nach einem Update selbst neu */
fun atHome(on: Boolean): Unit = js("(function () { window.__exHome = on; if (on && window.__exTry) window.__exTry(); })()")

// ── Schrift: Dateien holen und als Bytes an die Oberfläche geben
private fun fetchBytes(url: String, done: (dynamic) -> Unit): Unit = js("(function () { fetch(url).then(function (r) { return r.ok ? r.arrayBuffer() : Promise.reject(); }).then(function (b) { return done(new Int8Array(b)); }).catch(function () { return done(null); }); })()")
fun loadFile(url: String, ready: (ByteArray?) -> Unit) = fetchBytes(url) { a -> ready(a?.unsafeCast<ByteArray>()) }

// ── Ton: dieselben im Code erzeugten Klänge wie in der Android-App, abgespielt über WebAudio.
// Browser erlauben Ton erst nach der ersten Berührung – vorher wird still nichts gespielt.
fun audioReady(): Boolean = js("(function () { try { if (!window.__exAudio && navigator.userActivation && !navigator.userActivation.hasBeenActive) return false; var A = window.__exAudio || (window.__exAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state === 'suspended') A.resume(); return A.state === 'running'; } catch (e) { return false; } })()")
fun hasBuffer(key: String): Boolean = js("!!(window.__exBufs && window.__exBufs[key])")
fun storeBuffer(key: String, data: dynamic, rate: Int): Unit = js("(function () { var A = window.__exAudio; var buf = A.createBuffer(1, data.length, rate); buf.copyToChannel(data, 0); (window.__exBufs || (window.__exBufs = {}))[key] = buf; })()")
/** Klang-Daten (im Code erzeugt) als WebAudio-Puffer ablegen */
fun storePcm(key: String, pcm: FloatArray, rate: Int) = storeBuffer(key, pcm, rate)   // FloatArray ist in Kotlin/JS schon ein Float32Array
fun playBuffer(key: String, rate: Double, gain: Double): Unit = js("(function () { try { var A = window.__exAudio; var b = window.__exBufs[key]; if (!A || !b) return; var s = A.createBufferSource(); s.buffer = b; s.playbackRate.value = rate; var g = A.createGain(); g.gain.value = gain; s.connect(g); g.connect(A.destination); s.start(); } catch (e) {} })()")
/** Ton wecken, sobald der Nutzer das erste Mal tippt */
fun wakeAudioOnTouch(): Unit = js("(function () { var w = function () { try { var A = window.__exAudio || (window.__exAudio = new (window.AudioContext || window.webkitAudioContext)()); if (A.state !== 'running') A.resume(); } catch (e) {} }; window.addEventListener('pointerdown', w, true); window.addEventListener('keydown', w, true); document.addEventListener('visibilitychange', function () { var A = window.__exAudio; if (!A) return; if (document.hidden) A.suspend(); else A.resume(); }); })()")
fun vibrate(ms: Int): Unit = js("(function () { if (navigator.vibrate) navigator.vibrate(ms); })()")

// ── Zurück-Taste des Browsers: Jede geöffnete Ebene bekommt einen Eintrag im Verlauf; „zurück“ schließt eine Ebene.
fun syncHistory(depth: Int, onBack: () -> Unit): Unit = js("(function () { window.__exBack = onBack; if (window.__exDepth === undefined) { window.__exDepth = 1; window.addEventListener('popstate', function () { if (window.__exSkip > 0) { window.__exSkip--; return; } if (window.__exDepth > 1) { window.__exDepth--; window.__exBack(); } }); window.__exSkip = 0; } while (window.__exDepth < depth) { history.pushState({ ex: window.__exDepth }, ''); window.__exDepth++; } if (window.__exDepth > depth) { var n = window.__exDepth - depth; window.__exDepth = depth; window.__exSkip += 1; history.go(-n); } })()")

// ── Musik: eigener Lautstärke-Regler, die Phrase wird in Kotlin komponiert und hier abgespielt
fun musicPlay(key: String, gain: Double): Double = js("(function () { try { var A = window.__exAudio; var b = window.__exBufs[key]; if (!A || !b) return 0; if (!window.__exMusic) { window.__exMusic = A.createGain(); window.__exMusic.connect(A.destination); } window.__exMusic.gain.value = gain; if (window.__exMusicSrc) { try { window.__exMusicSrc.stop(); } catch (e) {} } var s = A.createBufferSource(); s.buffer = b; s.connect(window.__exMusic); s.start(); window.__exMusicSrc = s; return b.duration; } catch (e) { return 0; } })()")
fun musicGain(gain: Double): Unit = js("(function () { if (window.__exMusic) window.__exMusic.gain.setTargetAtTime(gain, window.__exAudio.currentTime, 0.15); })()")
fun musicStop(): Unit = js("(function () { try { if (window.__exMusicSrc) { var g = window.__exMusic; var A = window.__exAudio; g.gain.setTargetAtTime(0, A.currentTime, 0.08); var s = window.__exMusicSrc; setTimeout(function () { try { s.stop(); } catch (e) {} }, 300); window.__exMusicSrc = null; } } catch (e) {} })()")
fun pageHidden(): Boolean = js("document.hidden")

// ── Vorlesen mit den Stimmen des Browsers
fun canSpeak(): Boolean = js("('speechSynthesis' in window)")
fun browserSpeak(text: String, voice: String, rate: Double, volume: Double, onStart: () -> Unit, onEnd: () -> Unit): Unit = js("(function () { try { speechSynthesis.cancel(); var u = new SpeechSynthesisUtterance(text); u.lang = 'de-DE'; u.rate = rate; u.volume = volume; var all = speechSynthesis.getVoices(); var v = all.find(function (x) { return x.voiceURI === voice; }) || all.filter(function (x) { return x.lang && x.lang.toLowerCase().startsWith('de'); }).sort(function (a, b) { return b.localService - a.localService; })[0]; if (v) u.voice = v; u.onstart = function () { return onStart(); }; u.onend = function () { return onEnd(); }; u.onerror = function () { return onEnd(); }; speechSynthesis.speak(u); } catch (e) { onEnd(); } })()")
fun browserStop(): Unit = js("(function () { if ('speechSynthesis' in window) speechSynthesis.cancel(); })()")

// ── Nova (nova.js wird beim ersten Vorlesen geladen)
fun novaScript(done: (Boolean) -> Unit): Unit = js("(function () { if (window.ExNova) { done(true); return; } var s = document.createElement('script'); s.src = 'nova.js'; s.onload = function () { return done(true); }; s.onerror = function () { return done(false); }; document.head.appendChild(s); })()")
fun novaSpeak(texts: String, urls: String, speed: Double, onStart: () -> Unit, onEnd: () -> Unit, onFail: (Int) -> Unit): Unit = js("window.ExNova.speak(texts, urls, speed, onStart, onEnd, onFail)")
fun novaInfo(host: String, urls: String, done: (Double, Double, Double, Int) -> Unit): Unit = js("window.ExNova.info(host, urls, done)")
fun novaLoadAll(urls: String, originals: String, progress: (Int, Int) -> Unit, finished: (String) -> Unit): Unit = js("window.ExNova.loadAll(urls, originals, progress, finished)")
fun novaCancelAll(): Unit = js("(function () { if (window.ExNova) window.ExNova.cancelAll(); })()")
fun novaClear(done: () -> Unit): Unit = js("window.ExNova.clear(done)")
/** Hat der Nutzer schon einmal getippt/geklickt? (vorher blockiert der Browser jeden Ton) */
fun userActivated(): Boolean = js("(function () { try { return !navigator.userActivation || navigator.userActivation.hasBeenActive; } catch (e) { return true; } })()")
/** 0 = nichts anzeigen (schon installiert oder Browser kann es nicht), 1 = Installations-Dialog möglich, 2 = iPhone/iPad (Anleitung) */
fun installState(): Int = js("""(function () { try {
  if ((window.matchMedia && matchMedia('(display-mode: standalone)').matches) || navigator.standalone === true) return 0;
  if (window.__exBip) return 1;
  var ios = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  return ios ? 2 : 0; } catch (e) { return 0; } })()""")
fun installPrompt(done: (Boolean) -> Unit): Unit = js("(function () { var e = window.__exBip; if (!e) { done(false); return; } e.prompt(); e.userChoice.then(function (c) { window.__exBip = null; done(c && c.outcome === 'accepted'); }, function () { done(false); }); })()")
fun onInstallChanged(cb: () -> Unit): Unit = js("(function () { window.__exInstallChanged = cb; if (window.matchMedia) { try { matchMedia('(display-mode: standalone)').addEventListener('change', cb); } catch (e) {} } })()")
/** Schrittprotokoll (index.html): wo der Start gerade ist – bleibt nach einem Absturz sichtbar */
fun step(s: String): Unit = js("(function () { if (window.exStep) window.exStep(s); })()")
fun pageHost(): String = js("location.host")
fun novaStop(): Unit = js("(function () { if (window.ExNova) window.ExNova.stop(); })()")
/** Den Speicher der alten Offline-Stimme (bis 2.1, ca. 63 MB) freigeben */
fun dropOldVoice(): Unit = js("(function () { try { if (window.caches) caches.delete('experimentopia-stimme'); } catch (e) {} })()")

// ── Forscher-Pass: Freischalt-Link „…/#pro=EX…“ (neu, landet nicht in Server-Logs) oder „…/?pro=EX…“ (alte Karten und Mails) lesen und die Adresse danach aufräumen
fun locationSearch(): String = js("(function () { var a = (window.location.search || '') + ' ' + (window.location.hash || ''); try { return decodeURIComponent(a); } catch (e) { return a; } })()")
fun clearSearch(): Unit = js("(function () { try { history.replaceState(history.state, '', location.pathname); } catch (e) {} })()")

// ── Eltern-Kopplung: sichere Zufallszahlen, HTTP zum Eltern-Server, Teilen
/** [n] Zufallsbytes aus crypto.getRandomValues als Hex-Text */
fun randomHex(n: Int): String = js("(function () { var a = new Uint8Array(n); crypto.getRandomValues(a); var s = ''; for (var i = 0; i < a.length; i++) s += (a[i] < 16 ? '0' : '') + a[i].toString(16); return s; })()")
/** PUT/DELETE mit X-Schreib; done(Status) – 0 bei Netzfehler */
fun httpSend(method: String, url: String, write: String, body: String, done: (Int) -> Unit): Unit = js("(function () { try { var o = { method: method, headers: { 'X-Schreib': write }, cache: 'no-store' }; if (body) { o.body = body; o.headers['Content-Type'] = 'text/plain'; } fetch(url, o).then(function (r) { done(r.status); }).catch(function () { done(0); }); } catch (e) { done(0); } })()")
/** Teilen-Menü des Geräts, sonst in die Zwischenablage; done("geteilt" | "kopiert" | "") */
fun shareLink(title: String, url: String, done: (String) -> Unit): Unit = js("(function () { var copy = function () { try { navigator.clipboard.writeText(url).then(function () { done('kopiert'); }, function () { done(''); }); } catch (e) { done(''); } }; if (navigator.share) { navigator.share({ title: title, url: url }).then(function () { done('geteilt'); }, function (e) { if (e && e.name === 'AbortError') done(''); else copy(); }); } else copy(); })()")
/** Zeitzone: Minuten östlich von UTC */
fun tzOffsetMin(): Int = js("-new Date().getTimezoneOffset()")
/** GET (Sicherung holen); done(Status, Text) – 0 bei Netzfehler */
fun httpGet(url: String, done: (Int, String) -> Unit): Unit = js("(function () { try { fetch(url, { cache: 'no-store' }).then(function (r) { return r.text().then(function (t) { done(r.status, t); }); }).catch(function () { done(0, ''); }); } catch (e) { done(0, ''); } })()")
/** Alle Schlüssel im Browser-Speicher, die mit [prefix] beginnen (durch Zeilenumbruch getrennt) */
fun storeKeys(prefix: String): String = js("(function () { var r = []; for (var i = 0; i < localStorage.length; i++) { var k = localStorage.key(i); if (k && k.indexOf(prefix) === 0) r.push(k); } return r.join('\\n'); })()")
fun reloadPage(): Unit = js("location.reload()")
/** Anonyme Zählung: POST text/plain, ohne Cookies, auch beim Schließen der Seite (keepalive); Fehler werden still verschluckt */
fun zaehlenSenden(url: String, body: String): Unit = js("(function () { try { fetch(url, { method: 'POST', body: body, headers: { 'Content-Type': 'text/plain' }, credentials: 'omit', keepalive: true, mode: 'no-cors', cache: 'no-store' }).catch(function () {}); } catch (e) {} })()")
