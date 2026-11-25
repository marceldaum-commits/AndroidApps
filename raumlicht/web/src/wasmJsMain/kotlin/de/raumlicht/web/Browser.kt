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
/**
 * Kennzeichnung jeder KI-Antwort als maschinenlesbares „KI-erzeugt“ (KI-Verordnung Art. 50 Abs. 2): Die App zeichnet auf einer Fläche, der Text steht nicht im Seitenmodell –
 * darum steht die letzte Antwort zusätzlich in einem versteckten Element (für Vorlese-Programme und Auswertungen lesbar).
 */
fun kiKennzeichnen(text: String): Unit = js("""{ var l = document.getElementById('ki-erzeugt'); if (!l) { l = document.createElement('div'); l.id = 'ki-erzeugt'; l.setAttribute('lang', 'de');
  l.style.cssText = 'position:absolute;width:1px;height:1px;margin:-1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap'; document.body.appendChild(l); }
  var e = document.createElement('div'); e.setAttribute('role', 'note'); e.setAttribute('aria-label', 'KI-erzeugte Antwort'); e.setAttribute('aria-description', 'Diese Antwort wurde von einer KI erzeugt und kann Fehler enthalten.');
  e.setAttribute('data-ki-erzeugt', 'true'); e.setAttribute('data-ki-anbieter', 'Anthropic'); e.setAttribute('data-ki-hinweis', 'KI-erzeugt');
  e.textContent = 'KI-erzeugt: ' + text; l.appendChild(e); while (l.children.length > 20) l.removeChild(l.firstChild); }""")
/** Kann die WebApp installiert werden? „installiert“ (läuft schon als App), „knopf“ (der Browser bietet es an), „ios“ (iPhone/iPad: von Hand über „Teilen“), sonst „nichts“ */
fun installState(): String = js("""(function () {
    var I = window.__rlInstall || {};
    if (I.done || (window.matchMedia && matchMedia('(display-mode: standalone)').matches) || navigator.standalone === true) return 'installiert';
    if (I.prompt) return 'knopf';
    if (/iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) return 'ios';
    return 'nichts'; })()""")
/** Fragt den Browser, ob er die WebApp installieren soll (zeigt dessen eigenes Fenster) */
fun installNow(): Unit = js("{ var I = window.__rlInstall; if (I && I.prompt) { var p = I.prompt; I.prompt = null; p.prompt(); if (p.userChoice) p.userChoice.then(function (c) { if (c && c.outcome === 'accepted') I.done = true; }); } }")
/** Zählaufruf abschicken: ohne Cookies, ohne auf die Antwort zu warten; Fehler bleiben ohne Folgen. „text/plain“, damit der Browser keine Vorab-Anfrage braucht. */
// Auf dem eigenen Rechner (automatische Prüfungen) wird nicht gezählt – sonst landeten Testläufe in der echten Statistik.
fun beacon(url: String, body: String): Unit = js("{ try { if ((location.hostname === 'localhost' || location.hostname === '127.0.0.1') && !window.__rlZaehleLokal) return; fetch(url, { method: 'POST', body: body, headers: { 'Content-Type': 'text/plain' }, credentials: 'omit', mode: 'no-cors', keepalive: true, referrerPolicy: 'no-referrer' }).catch(function () {}); } catch (e) {} }")
// ── „Eltern-Handy koppeln“: Zufallswerte, Verschlüsselung (AES-GCM 256, 12 Byte IV) und Hochladen – alles im Browser, der Schlüssel verlässt ihn nie ──
/** Frische Zufallswerte „id.schreib.key“ (16, 16 und 32 Byte, base64url ohne Auffüllzeichen) */
fun elternNeu(): String = js("""(function () { function r(n) { var a = crypto.getRandomValues(new Uint8Array(n)), s = ''; for (var i = 0; i < n; i++) s += String.fromCharCode(a[i]);
    return btoa(s).split('+').join('-').split('/').join('_').split('=').join(''); } return r(16) + '.' + r(16) + '.' + r(32); })()""")
/** Verschlüsselt [json] mit [key] und lädt „<iv>.<verschlüsselt>“ hoch; [done] bekommt den Status (0 = keine Verbindung oder Verschlüsselung nicht möglich) */
fun elternSenden(url: String, schreib: String, key: String, json: String, done: (Int) -> Unit): Unit = js("""{
    try {
        var b64 = function (u8) { var s = ''; for (var i = 0; i < u8.length; i++) s += String.fromCharCode(u8[i]); return btoa(s).split('+').join('-').split('/').join('_').split('=').join(''); };
        var bin = atob(key.split('-').join('+').split('_').join('/')), raw = new Uint8Array(bin.length); for (var i = 0; i < bin.length; i++) raw[i] = bin.charCodeAt(i);
        var iv = crypto.getRandomValues(new Uint8Array(12));
        crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt'])
            .then(function (k) { return crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, k, new TextEncoder().encode(json)); })
            .then(function (ct) { return fetch(url, { method: 'PUT', headers: { 'Content-Type': 'text/plain', 'X-Schreib': schreib }, body: b64(iv) + '.' + b64(new Uint8Array(ct)), credentials: 'omit', referrerPolicy: 'no-referrer' }); })
            .then(function (r) { done(r.status); }, function () { done(0); });
    } catch (e) { done(0); }
}""")
/** Wie [elternSenden], aber als Vermerk „vom Kind gelöst“ (Header X-Geloest: 1): Der Server löscht dabei sofort die Sicherung, markiert den Eintrag und lässt ihn nach 14 Tagen verfallen */
fun elternGeloest(url: String, schreib: String, key: String, json: String, done: (Int) -> Unit): Unit = js("""{
    try {
        var b64 = function (u8) { var s = ''; for (var i = 0; i < u8.length; i++) s += String.fromCharCode(u8[i]); return btoa(s).split('+').join('-').split('/').join('_').split('=').join(''); };
        var bin = atob(key.split('-').join('+').split('_').join('/')), raw = new Uint8Array(bin.length); for (var i = 0; i < bin.length; i++) raw[i] = bin.charCodeAt(i);
        var iv = crypto.getRandomValues(new Uint8Array(12));
        crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt'])
            .then(function (k) { return crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, k, new TextEncoder().encode(json)); })
            .then(function (ct) { return fetch(url, { method: 'PUT', headers: { 'Content-Type': 'text/plain', 'X-Schreib': schreib, 'X-Geloest': '1' }, body: b64(iv) + '.' + b64(new Uint8Array(ct)), credentials: 'omit', referrerPolicy: 'no-referrer' }); })
            .then(function (r) { done(r.status); }, function () { done(0); });
    } catch (e) { done(0); }
}""")
fun elternLoeschen(url: String, schreib: String, done: (Int) -> Unit): Unit = js("{ try { fetch(url, { method: 'DELETE', headers: { 'X-Schreib': schreib }, credentials: 'omit', referrerPolicy: 'no-referrer' }).then(function (r) { done(r.status); }, function () { done(0); }); } catch (e) { done(0); } }")
/** Holt die Sicherung und entschlüsselt sie mit [key]; [done] bekommt den Status (200 = gelesen, 404 = keine Sicherung, 0 = keine Verbindung, -1 = Schlüssel passt nicht) und den Klartext */
fun elternHolen(url: String, key: String, done: (Int, String) -> Unit): Unit = js("""{
    try {
        var un = function (t) { var bin = atob(t.split('-').join('+').split('_').join('/')), u = new Uint8Array(bin.length); for (var i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i); return u; };
        fetch(url, { credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store' }).then(function (r) {
            if (r.status !== 200) { done(r.status, ''); return; }
            return r.json().then(function (j) { var p = String(j.d || '').split('.');
                return crypto.subtle.importKey('raw', un(key), 'AES-GCM', false, ['decrypt']).then(function (k) { return crypto.subtle.decrypt({ name: 'AES-GCM', iv: un(p[0]) }, k, un(p[1])); })
                    .then(function (plain) { done(200, new TextDecoder().decode(plain)); }); }).catch(function () { done(-1, ''); });
        }, function () { done(0, ''); });
    } catch (e) { done(-1, ''); }
}""")
// ── Familien-Speicher: der unveränderte Baustein familie.js (Mathe-Trainer) wird erst bei Bedarf geladen; verschlüsselt wird im Browser ──
/**
 * Ein Aufruf an den Familien-Speicher. [was]: „neu“ (a = Spitzname) · „kinder“ (a = Familie) · „kind“ (a = Familie, b = Spitzname) ·
 * „sichern“ (a = Familie, b = Kind, c = Lernstand als JSON, d = Stand-Nummer) · „laden“ (a = Familie, b = Kind) · „protokoll“ (a = Familie) · „loeschen“ (a = Familie).
 * [done] bekommt „ok“ oder den Grund (netz, keine, konflikt, schluessel, …) und bis zu zwei Werte.
 */
fun familieRuf(server: String, was: String, a: String, b: String, c: String, d: String, done: (String, String, String) -> Unit): Unit = js("""{
    var fertig = false, ende = function (s, x, y) { if (s === 'familie-weg') s = 'weg'; if (!fertig) { fertig = true; done(String(s), String(x == null ? '' : x), String(y == null ? '' : y)); } };
    try {
        var laden = window.__rlFamilieModul || (window.__rlFamilieModul = new Function('u', 'return import(u)')(new URL('familie.js', document.baseURI).href));
        laden.then(function (F) {
            var APP = 'raumlicht', teile = function (t) { var p = String(t).split('.'); return { famId: p[0], famKey: p[1], schreib: p[2] }; };
            var zugang = function (fam) { return F.familie(fam, { basis: server, f: function (u, o) { o = o || {}; o.credentials = 'omit'; o.referrerPolicy = 'no-referrer'; return fetch(u, o); } }); };
            var zeilen = function (index) { return (index.kinder || []).map(function (k) { return k.kindId + '\t' + String(k.name || '').split('\t').join(' ').split('\n').join(' '); }).join('\n'); };
            var index = function (A) { return A.indexLaden().then(function (r) { return r.ok ? { index: r.daten, rev: r.rev } : r.grund === 'keine' ? { index: F.neuerIndex(), rev: 0 } : { grund: r.grund }; }); };
            if (was === 'neu') { var fam = F.neueFamilie(), A = zugang(fam);
                return A.anlegen().then(function (r) { if (!r.ok) return ende(r.grund);
                    var k = F.kindHinzu(F.appMerken(F.neuerIndex(), APP), { name: a });
                    return A.indexSichern(k.index, 0).then(function (s) { if (!s.ok) return ende(s.grund); ende('ok', fam.famId + '.' + fam.famKey + '.' + fam.schreib + '|' + k.kind.kindId, A.nummer); }); }); }
            var B = zugang(teile(a));
            if (was === 'kinder') return index(B).then(function (i) { if (i.grund) return ende(i.grund); ende('ok', zeilen(i.index), B.nummer); });
            if (was === 'kind') return index(B).then(function (i) { if (i.grund) return ende(i.grund);
                var k = F.kindHinzu(F.appMerken(i.index, APP), { name: b });
                return B.indexSichern(k.index, i.rev).then(function (s) { if (!s.ok) return ende(s.grund); ende('ok', k.kind.kindId, B.nummer); }); });
            if (was === 'sichern') return B.standSichern(APP, b, JSON.parse(c), Number(d) || 0).then(function (r) {
                if (r.ok) return ende('ok', r.rev); if (r.grund === 'konflikt') return ende('konflikt', r.rev, r.daten ? JSON.stringify(r.daten) + '\n' + (r.stand || '') : ''); ende(r.grund); });
            if (was === 'laden') return B.standLaden(APP, b).then(function (r) { if (!r.ok) return ende(r.grund); ende('ok', JSON.stringify(r.daten), r.rev + '|' + (r.stand || '')); });
            if (was === 'protokoll') return B.protokoll().then(function (r) { if (!r.ok) return ende(r.grund);
                ende('ok', ((r.daten && r.daten.eintraege) || []).map(function (e) { return String(e.art || '').split('\t').join(' ') + '\t' + String(e.t || ''); }).join('\n')); });
            if (was === 'loeschen') return B.loeschen().then(function (r) { ende(r.ok ? 'ok' : r.grund); });
            ende('fehler');
        }).catch(function () { ende('baustein'); });
    } catch (e) { ende('baustein'); }
}""")
/** Nur für die automatische Prüfung auf dem eigenen Rechner: anderer Familien-Server (window.__rlFamilieServer), sonst null */
fun familieServer(): String? = js("((location.hostname === 'localhost' || location.hostname === '127.0.0.1') && window.__rlFamilieServer) || null")
/** Nur für die automatische Prüfung auf dem eigenen Rechner: Eltern-Tor offen (window.__rlTorOffen) – auf jeder echten Adresse immer false */
fun familieTorFrei(): Boolean = js("(location.hostname === 'localhost' || location.hostname === '127.0.0.1') && window.__rlTorOffen === true")
fun reloadPage(): Unit = js("location.reload()")
/** Nur für die automatische Prüfung auf dem eigenen Rechner: anderer Eltern-Server (window.__rlElternServer), sonst null */
fun elternServer(): String? = js("((location.hostname === 'localhost' || location.hostname === '127.0.0.1') && window.__rlElternServer) || null")
fun hostName(): String = js("location.hostname")
/** Läuft die WebApp auf der Test-Stufe? Alte Testadresse (raumlicht-test.…) oder die gemeinsame (lernen-test.…/raumlicht/) – am Hostnamen erkannt */
fun isTestSite(): Boolean = js("location.hostname === 'raumlicht-test.mathenachhilfe-online.com' || location.hostname.indexOf('lernen-test.') === 0")
/** Nur für die automatische Prüfung auf dem eigenen Rechner: anderer KI-Server (window.__rlKiServer), sonst null */
fun kiServer(): String? = js("((location.hostname === 'localhost' || location.hostname === '127.0.0.1') && window.__rlKiServer) || null")
/** Zur App-Auswahl der Website: im Browser im selben Tab; läuft die WebApp installiert (eigenes Fenster), im normalen Browser */
fun openApps(url: String): Unit = js("{ var allein = (window.matchMedia && matchMedia('(display-mode: standalone)').matches) || navigator.standalone === true; if (window.__rlAppsZiel) { window.__rlAppsZiel(url, allein); return; } if (allein) window.open(url, '_blank', 'noopener'); else location.assign(url); }")
fun openLink(url: String): Unit = js("window.open(url, '_blank', 'noopener')")
/** Ladebild sanft ausblenden; spätestens jetzt wird auch die 3D-Bühne angefordert */
fun hideStartScreen(): Unit = js("{ const s = document.getElementById('start'); if (s && s.dataset.fertig !== '1') { s.dataset.fertig = '1'; if (window.__rlSchritt) window.__rlSchritt('Oberfläche sichtbar – Start geschafft'); s.style.opacity = '0'; s.style.pointerEvents = 'none'; setTimeout(() => { s.style.display = 'none'; }, 380); } if (window.__rlStage) window.__rlStage(); }")
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
/** Zwischenlösung, solange es keine fertigen Sprachdateien gibt: eine deutsche Stimme, die auf dem Gerät eingebaut ist (kein Versand an den Browser-Hersteller) */
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

/**
 * Wert aus der Adresszeile holen und gleich wieder herausnehmen, damit er nicht versehentlich geteilt oder beim Neuladen wiederholt wird.
 * Gelesen wird zuerst hinter dem „#“ („#name=…“ – dieser Teil geht nie an einen Server und landet in keinem Protokoll), sonst „?name=…“ (alte Karten und E-Mails).
 */
fun takeParam(name: String): String? = js("""(function () { try {
    var u = new URL(location.href), h = new URLSearchParams(u.hash.charAt(0) === '#' ? u.hash.slice(1) : u.hash), c = h.get(name), q = u.searchParams.get(name);
    if (c === null && q === null) return null;
    if (c !== null) { h.delete(name); var rest = h.toString(); u.hash = rest ? '#' + rest : ''; }
    u.searchParams.delete(name); history.replaceState(history.state, '', u.pathname + u.search + u.hash); return c !== null ? c : q; } catch (e) { return null; } })()""")
/** Freischalt-Link „#pro=<Code>“ (früher „?pro=<Code>“) */
fun takeProParam(): String? = takeParam("pro")
