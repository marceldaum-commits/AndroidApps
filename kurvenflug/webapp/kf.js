// Kurvenflug WebApp – kleine Helfer für Ton, Teilen, Fotos und die Zurück-Taste.
// Kein Tracking: der Spielstand bleibt im Browser. Einzige Verbindung nach außen: die Vorlese-Stimme Nova von unserem
// Server stimmen.mathenachhilfe-online.com (nur wenn Vorlesen an ist, siehe unten).
(function () {
  'use strict';
  var KF = window.KF = {};

  // ------------------------------------------------------------ Schnellstart: große Dateien sofort parallel laden
  // window.KF_FILES (vom Build in index.html eingesetzt): [{u: Pfad, s: Bytes, t: Typ}]. Wir laden alles gleich los,
  // zählen die Bytes für den Fortschrittsbalken und reichen die Daten an die App weiter, sobald sie danach fragt.
  // ?direkt=1: ohne unseren Vorlade-Strom (Test: Liegt es am fetch-/ReadableStream-Weg?)
  var files = /[?&]direkt=1/.test(location.search) ? [] : (window.KF_FILES || []), total = files.reduce(function (a, f) { return a + f.s; }, 0), loaded = 0, pending = {};
  function progress() {
    var bar = document.getElementById('kf-bar'), txt = document.getElementById('kf-pct');
    if (!bar || !total) return;
    var p = Math.min(1, loaded / total);
    bar.style.width = (4 + 96 * p) + '%';
    if (txt) txt.textContent = p < 1 ? 'Lade … ' + Math.round(p * 100) + ' %' : 'Starte die Triebwerke …';
    // alles geladen: stürzt der Tab jetzt beim Übersetzen ab, nimmt der nächste Start die Kompatibilitäts-Fassung (index.html)
    if (p >= 1 && window.kfMode === 'wasm') { try { if ((localStorage.getItem('kf.start') || '').indexOf('wasm|startet') === 0 && window.kfStage) window.kfStage('geladen'); } catch (e) {} }
  }
  // Erst das Ladebild malen lassen, dann die Dateien NACHEINANDER holen (nicht parallel – schont Speicher und Leitung,
  // iOS hat bei parallelem Laden und Übersetzen den Tab beendet). Die Daten fließen als Strom weiter, damit der Browser das
  // Wasm schon während des Ladens übersetzen kann (instantiateStreaming). Wir zählen nur mit.
  var realFetch = window.fetch.bind(window);   // vor dem Umleiten unten merken – sonst wartet der Abruf auf sich selbst
  var chain = new Promise(function (r) { requestAnimationFrame(function () { requestAnimationFrame(function () { setTimeout(r, 30); }); }); });
  files.forEach(function (f) {
    var finished;
    var bodyDone = new Promise(function (r) { finished = r; });
    pending[f.u] = chain.then(function () { return realFetch(f.u); }).then(function (r) {
      if (!r.ok || !r.body) { finished(); return r; }
      var reader = r.body.getReader();
      var body = new ReadableStream({
        start: function (ctrl) {   // gleich alles abholen (nicht auf die App warten), die App liest später mit
          (function pump() {
            reader.read().then(function (x) {
              if (x.done) { ctrl.close(); finished(); return; }
              loaded += x.value.length; progress(); ctrl.enqueue(x.value); pump();
            }, function (e) { ctrl.error(e); finished(); });
          })();
        }
      });
      return new Response(body, { status: 200, headers: { 'Content-Type': f.t } });
    }, function (e) { finished(); throw e; });
    chain = chain.then(function () { return bodyDone; });   // nächste Datei erst, wenn diese ganz da ist
  });
  var origFetch = window.fetch.bind(window);
  window.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : (input && input.url) || '';
    var path = url.replace(location.origin + location.pathname.replace(/[^/]*$/, ''), '').replace(/^\.\//, '').split('?')[0];
    var hit = pending[path];
    if (hit) { delete pending[path]; return hit; }
    return origFetch(input, init);
  };

  // ------------------------------------------------------------ Ton (Web Audio, erst nach dem ersten Tippen)
  var ctx = null, unlocked = false, unlockCbs = [], waiting = [];
  KF.bufData = []; var buffers = [];
  // Ton-Ausgabe erst nach dem ersten Tippen anlegen (iOS: kein AudioContext vor einer Berührung)
  function audio() {
    if (!ctx && unlocked) { var C = window.AudioContext || window.webkitAudioContext; if (!C) return null; try { ctx = new C(); } catch (e) { return null; } }
    return ctx;
  }
  function unlock() {
    if (!unlocked) unlocked = true;
    var c = audio(); if (!c) return;
    if (c.state === 'suspended') c.resume();
    if (waiting.length) { var w = waiting; waiting = []; w.forEach(function (id) { KF.bufDone(id); }); }
    if (unlockCbs.length) { var cbs = unlockCbs; unlockCbs = []; cbs.forEach(function (f) { try { f(); } catch (e) { console.warn(e); } }); }
  }
  ['pointerdown', 'keydown', 'touchend'].forEach(function (t) { window.addEventListener(t, unlock, { capture: true, passive: true }); });
  KF.onUnlock = function (f) { if (unlocked && ctx) f(); else unlockCbs.push(f); };
  KF.bufNew = function (n, rate) { KF.bufData.push(new Float32Array(n)); buffers.push({ rate: rate, buf: null }); return KF.bufData.length - 1; };
  KF.bufDone = function (id) {
    var c = audio(); if (!c) { waiting.push(id); return; }   // vor dem ersten Tippen: Daten aufheben
    var d = KF.bufData[id], b = c.createBuffer(1, d.length, buffers[id].rate);
    b.getChannelData(0).set(d); buffers[id].buf = b; KF.bufData[id] = null;
  };
  KF.play = function (id, rate, vol) {
    var c = audio(); if (!c || !unlocked || !buffers[id] || !buffers[id].buf) return;
    var s = c.createBufferSource(), g = c.createGain();
    s.buffer = buffers[id].buf; s.playbackRate.value = rate; g.gain.value = vol;
    s.connect(g).connect(c.destination); s.start();
  };
  var music = null;
  KF.music = function (id, vol) {
    var c = audio(); if (!c) return;
    if (music && music.id === id) return;
    if (music) { var old = music; old.g.gain.setTargetAtTime(0, c.currentTime, 0.15); setTimeout(function () { try { old.s.stop(); } catch (e) {} }, 800); music = null; }
    if (id < 0 || !buffers[id] || !buffers[id].buf) return;
    var s = c.createBufferSource(), g = c.createGain();
    s.buffer = buffers[id].buf; s.loop = true; g.gain.value = 0; g.gain.setTargetAtTime(vol, c.currentTime, 0.5);
    s.connect(g).connect(c.destination); s.start(); music = { id: id, s: s, g: g };
  };
  // Im Hintergrund still
  document.addEventListener('visibilitychange', function () {
    if (!ctx) return;
    if (document.hidden) ctx.suspend(); else if (unlocked) ctx.resume();
  });

  // ------------------------------------------------------------ Teilen
  function toast(msg) {
    var t = document.createElement('div'); t.className = 'kf-toast'; t.textContent = msg;
    // an <html>, nicht an <body>: <body> trägt die Compose-Oberfläche als Shadow-DOM, dort eingehängtes wäre unsichtbar
    document.documentElement.appendChild(t); setTimeout(function () { t.remove(); }, 2600);
  }
  KF.toast = toast;

  // ------------------------------------------------------------ Eltern-Handy koppeln (Ende-zu-Ende verschlüsselt)
  // id, schreib und key entstehen hier per crypto.getRandomValues und liegen nur in diesem Browser. Der Link für das Eltern-Handy
  // trägt id und key nach „#“ – dieser Teil geht nie an einen Server. Hochgeladen wird nur „iv.ciphertext“ (AES-GCM-256).
  var E = { id: 'kf.eltern.id', w: 'kf.eltern.schreib', k: 'kf.eltern.key', n: 'kf.eltern.name', t: 'kf.eltern.zuletzt' };
  function lsGet(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function lsSet(k, v) { try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) {} }
  function b64u(bytes) { var s = ''; for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]); return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, ''); }
  function unb64u(t) { t = t.replace(/-/g, '+').replace(/_/g, '/'); while (t.length % 4) t += '='; var s = atob(t), b = new Uint8Array(s.length); for (var i = 0; i < s.length; i++) b[i] = s.charCodeAt(i); return b; }
  function rnd(n) { var b = new Uint8Array(n); crypto.getRandomValues(b); return b64u(b); }
  // Test-Stufe (…-test, localhost) → trainer-test / staging; sonst trainer / www
  var testStage = /-test\.|localhost|127\.0\.0\.1/.test(location.hostname);
  var ELTERN_API = (testStage ? 'https://trainer-test.mathenachhilfe-online.com' : 'https://trainer.mathenachhilfe-online.com') + '/eltern/v1/b/';
  var SICHERUNG_API = ELTERN_API.replace('/eltern/v1/b/', '/eltern/v1/s/');
  var ELTERN_PAGE = (testStage ? 'https://staging.mathenachhilfe-online.com' : 'https://www.mathenachhilfe-online.com') + '/eltern-bericht/#';
  KF.elternLink = function () { var id = lsGet(E.id), k = lsGet(E.k); return id && k ? ELTERN_PAGE + id + '.' + k : ''; };
  KF.elternNick = function () { return lsGet(E.n) || ''; };
  KF.elternPair = function (nick) {
    if (!lsGet(E.id)) { lsSet(E.id, rnd(16)); lsSet(E.w, rnd(16)); lsSet(E.k, rnd(32)); lsSet(E.t, null); }
    lsSet(E.n, nick ? String(nick).slice(0, 30) : null);
    pendingFirst = true;
    return KF.elternLink();
  };
  KF.elternUnpair = function () {
    var id = lsGet(E.id), w = lsGet(E.w);
    if (id && w) fetch(ELTERN_API + id, { method: 'DELETE', headers: { 'X-Schreib': w }, credentials: 'omit', keepalive: true }).catch(function () {});
    [E.id, E.w, E.k, E.n, E.t].forEach(function (k) { lsSet(k, null); });
    readyBody = null; reportJson = null; readyBackup = null; backupPlain = null; sentBackup = null;
  };
  var reportJson = null, readyBody = null, pendingFirst = false, encrypting = null;
  function encryptMax(json, max) {
    var k = lsGet(E.k); if (!k || !window.crypto || !crypto.subtle) return Promise.resolve(null);
    var iv = new Uint8Array(12); crypto.getRandomValues(iv);
    return crypto.subtle.importKey('raw', unb64u(k), { name: 'AES-GCM' }, false, ['encrypt'])
      .then(function (key) { return crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, new TextEncoder().encode(json)); })
      .then(function (ct) { var body = b64u(iv) + '.' + b64u(new Uint8Array(ct)); return body.length <= max ? body : null; })
      .catch(function () { return null; });
  }
  function encrypt(json) {
    var k = lsGet(E.k); if (!k || !window.crypto || !crypto.subtle) return Promise.resolve(null);
    var iv = new Uint8Array(12); crypto.getRandomValues(iv);
    return crypto.subtle.importKey('raw', unb64u(k), { name: 'AES-GCM' }, false, ['encrypt'])
      .then(function (key) { return crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, new TextEncoder().encode(json)); })
      .then(function (ct) { var body = b64u(iv) + '.' + b64u(new Uint8Array(ct)); return body.length <= 24 * 1024 ? body : null; })
      .catch(function () { return null; });
  }
  function upload(force) {
    var id = lsGet(E.id), w = lsGet(E.w); if (!id || !w || !readyBody) return;
    var last = +(lsGet(E.t) || 0);
    if (!force && Date.now() - last < 10 * 60 * 1000) return;   // höchstens alle 10 Minuten
    var body = readyBody; lsSet(E.t, String(Date.now()));
    fetch(ELTERN_API + id, { method: 'PUT', headers: { 'X-Schreib': w, 'Content-Type': 'text/plain' }, body: body, credentials: 'omit', keepalive: true })
      .then(function (r) { if (!r.ok) lsSet(E.t, String(last)); }, function () { lsSet(E.t, String(last)); });
    // Sicherung des Lernstands – nur wenn sie sich seit der letzten Sendung geändert hat
    if (readyBackup && backupPlain !== sentBackup) {
      var plain = backupPlain;
      fetch(SICHERUNG_API + id, { method: 'PUT', headers: { 'X-Schreib': w, 'Content-Type': 'text/plain' }, body: readyBackup, credentials: 'omit', keepalive: readyBackup.length < 60000 })
        .then(function (r) { if (r.ok) sentBackup = plain; }, function () {});
    }
  }

  // ------------------------------------------------------------ Verschlüsselte Sicherung des Lernstands (über die Kopplung)
  // „daten“ = alle Kurvenflug-Einträge im Browser (Fortschritt, Durchgänge, Monatssummen, Einstellungen, Pro-Code) – ohne die
  // technischen Merker und ohne die Kopplungs-Schlüssel selbst (schreib steht eigens im verschlüsselten Klartext).
  var backupPlain = null, readyBackup = null, sentBackup = null;
  function techKey(k) { return /^kf\.(eltern\.|start$|wasmGescheitert$|modus$|opt\.|lastVersion$|diag$|letzterWechsel$)/.test(k); }
  function gameData(withPlays) {
    var d = {};
    try { Object.keys(localStorage).forEach(function (k) { if (k.indexOf('kf.') === 0 && !techKey(k) && (withPlays || !/^kf\.plays\./.test(k))) d[k] = localStorage.getItem(k); }); } catch (e) {}
    return d;
  }
  function isoNow() {
    var d = new Date(), o = -d.getTimezoneOffset(), p = function (n) { return (n < 10 ? '0' : '') + n; };
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds()) +
      (o < 0 ? '-' : '+') + p(Math.floor(Math.abs(o) / 60)) + ':' + p(Math.abs(o) % 60);
  }
  function prepareBackup() {
    var w = lsGet(E.w); if (!w) return Promise.resolve();
    var data = gameData(true);
    var json = JSON.stringify({ format: 1, app: 'kurvenflug', v: window.KF_VER || '', stand: isoNow(), schreib: w, daten: data });
    if (json.length > 380 * 1024) {   // zu groß (≤ 512 KB nach Verschlüsselung): ohne Einzelergebnisse, Monatssummen bleiben
      data = gameData(false); json = JSON.stringify({ format: 1, app: 'kurvenflug', v: window.KF_VER || '', stand: isoNow(), schreib: w, daten: data });
    }
    var compare = JSON.stringify(data);
    if (compare === backupPlain && readyBackup) return Promise.resolve();
    return encryptMax(json, 512 * 1024).then(function (b) { if (b) { readyBackup = b; backupPlain = compare; } });
  }
  KF.elternHasProgress = function () { try { return Object.keys(localStorage).some(function (k) { return /^kf\.progress\./.test(k) && localStorage.getItem(k); }); } catch (e) { return false; } };
  // Wiederherstellen auf einem neuen Gerät: Link einfügen → Sicherung holen, entschlüsseln, (nach Rückfrage) übernehmen.
  // Ergebnis an cb: ok | vorhanden (Lernstand da – erst nachfragen) | keine | andere_app | link | fehler
  KF.elternRestore = function (link, force, cb) {
    var m = /#([A-Za-z0-9_-]{22})\.([A-Za-z0-9_-]{43})/.exec(String(link || ''));
    if (!m) { cb('link'); return; }
    var id = m[1], key = m[2];
    fetch(SICHERUNG_API + id, { credentials: 'omit', cache: 'no-store' }).then(function (r) {
      if (r.status === 404) throw 'keine';
      if (!r.ok) throw 'fehler';
      return r.json();
    }).then(function (j) {
      var parts = String(j.d || '').split('.');
      return crypto.subtle.importKey('raw', unb64u(key), { name: 'AES-GCM' }, false, ['decrypt'])
        .then(function (k) { return crypto.subtle.decrypt({ name: 'AES-GCM', iv: unb64u(parts[0]) }, k, unb64u(parts[1] || '')); })
        .then(function (plain) { return JSON.parse(new TextDecoder().decode(plain)); }, function () { throw 'link'; });
    }).then(function (b) {
      if (!b || b.app !== 'kurvenflug' || !b.daten || !b.schreib) throw 'andere_app';
      if (!force && KF.elternHasProgress()) { cb('vorhanden'); return; }
      try {
        Object.keys(localStorage).forEach(function (k) { if (k.indexOf('kf.') === 0 && !techKey(k)) localStorage.removeItem(k); });
        Object.keys(b.daten).forEach(function (k) { if (k.indexOf('kf.') === 0 && !techKey(k)) localStorage.setItem(k, b.daten[k]); });
      } catch (e) { throw 'fehler'; }
      lsSet(E.id, id); lsSet(E.k, key); lsSet(E.w, b.schreib); lsSet(E.t, null);
      cb('ok');
      setTimeout(function () { location.reload(); }, 1800);
    }).catch(function (e) { cb(typeof e === 'string' ? e : 'fehler'); });
  };
  // Neuer Bericht aus der App: gleich verschlüsseln, damit er beim Verlassen sofort gesendet werden kann
  KF.elternReport = function (json) {
    if (!lsGet(E.id)) return;
    reportJson = json;
    encrypting = Promise.all([encrypt(json), prepareBackup()]).then(function (r) { var b = r[0]; if (b && json === reportJson) { readyBody = b; if (pendingFirst) { pendingFirst = false; upload(true); } } });
  };
  function elternLeave() { if (lsGet(E.id)) upload(false); }
  window.addEventListener('pagehide', elternLeave);
  document.addEventListener('visibilitychange', function () { if (document.hidden) elternLeave(); });

  // ------------------------------------------------------------ Anonyme Nutzungszählung (nur wenn Eltern sie einschalten)
  // Die App liefert beim Verlassen/Verstecken der Seite die Ereignisliste (Text); hier wird sie einmal per fetch (keepalive) geschickt –
  // ohne Cookies (credentials: 'omit'; sendBeacon würde Cookies mitschicken), ohne Kennung. Ist der Zähler nicht erreichbar, geht es still ins Leere.
  var leaveFn = null;
  KF.onLeave = function (f) { leaveFn = f; };
  function leave() {
    if (!leaveFn) return;
    try {
      var b = leaveFn();
      if (b) fetch('https://trainer.mathenachhilfe-online.com/zaehler/v1/e', { method: 'POST', credentials: 'omit', keepalive: true, headers: { 'Content-Type': 'text/plain' }, body: b }).catch(function () {});
    } catch (e) {}
  }
  window.addEventListener('pagehide', leave);
  document.addEventListener('visibilitychange', function () { if (document.hidden) leave(); });
  KF.share = function (text, title) {
    if (navigator.share) { navigator.share({ title: title, text: text }).catch(function () {}); return; }
    if (navigator.clipboard) navigator.clipboard.writeText(text).then(function () { toast('In die Zwischenablage kopiert 📋'); }, function () { window.prompt(title, text); });
    else window.prompt(title, text);
  };

  // ------------------------------------------------------------ Foto der Flugbahn
  KF.photo = function (spec, title) {
    var size = 1080, H = 1350, top = size * 0.18, cv = document.createElement('canvas'); cv.width = size; cv.height = H;
    var g = cv.getContext('2d'); g.fillStyle = '#0d112e'; g.fillRect(0, 0, size, H);
    var seed = 7; function rnd() { seed = (seed * 16807) % 2147483647; return seed / 2147483647; }
    for (var i = 0; i < 120; i++) { g.fillStyle = 'rgba(255,255,255,' + (0.25 + rnd() * 0.6) + ')'; g.beginPath(); g.arc(rnd() * size, rnd() * H, 1 + rnd() * 2.5, 0, 7); g.fill(); }
    function px(x, y) { return [(x / 1000 + 1) / 2 * size, top + (1 - y / 1000) / 2 * size]; }
    var d = {}; spec.split('\n').forEach(function (l) { d[l[0]] = l.substring(1).trim(); });
    g.strokeStyle = 'rgba(120,200,255,0.16)'; g.lineWidth = 2;
    for (var k = -9; k <= 9; k++) { var a = px(k / 10 * 1000, -1000), b = px(k / 10 * 1000, 1000); g.beginPath(); g.moveTo(a[0], a[1]); g.lineTo(b[0], b[1]); g.stroke(); a = px(-1000, k / 10 * 1000); b = px(1000, k / 10 * 1000); g.beginPath(); g.moveTo(a[0], a[1]); g.lineTo(b[0], b[1]); g.stroke(); }
    function circles(s, col, min) { (s || '').split(' ').filter(Boolean).forEach(function (c) { var v = c.split(',').map(Number), p = px(v[0], v[1]); g.fillStyle = col; g.beginPath(); g.arc(p[0], p[1], Math.max(min, v[2] / 2000 * size), 0, 7); g.fill(); }); }
    circles(d.A, 'rgb(140,110,100)', 2); circles(d.T, 'rgb(76,245,154)', 14);
    var pts = (d.P || '').split(' ').filter(Boolean).map(function (c) { var v = c.split(',').map(Number); return px(v[0], v[1]); });
    if (pts.length > 1) [[26, 'rgba(61,232,255,0.27)'], [9, 'rgb(61,232,255)']].forEach(function (w) {
      g.lineWidth = w[0]; g.strokeStyle = w[1]; g.lineCap = 'round'; g.lineJoin = 'round'; g.beginPath(); g.moveTo(pts[0][0], pts[0][1]);
      pts.forEach(function (p) { g.lineTo(p[0], p[1]); }); g.stroke(); });
    var stars = +d.S || 0;
    // App-Logo (wie icons/icon.svg) links neben dem Schriftzug
    (function (x, y, w) { var cr = 18, k = w / (108 - 2 * cr); function X(v) { return x + (v - cr) * k; } function Y(v) { return y + (v - cr) * k; }
      g.fillStyle = '#0b1026'; g.beginPath(); if (g.roundRect) g.roundRect(x, y, w, w, w * 0.22); else g.rect(x, y, w, w); g.fill();
      g.strokeStyle = '#38E8FF'; g.lineWidth = 3.5 * k; g.lineCap = 'round'; g.beginPath(); g.moveTo(X(30), Y(76)); g.quadraticCurveTo(X(46), Y(26), X(76), Y(62)); g.stroke();
      g.fillStyle = '#FF5CA8'; g.beginPath(); g.arc(X(76), Y(62), 7 * k, 0, 7); g.fill();
      g.fillStyle = '#FFD54F'; g.beginPath(); g.moveTo(X(50), Y(38)); g.lineTo(X(58), Y(43)); g.lineTo(X(50), Y(48)); g.lineTo(X(52), Y(43)); g.closePath(); g.fill();
      g.fillStyle = '#fff'; g.beginPath(); g.arc(X(36), Y(38), 1.5 * k, 0, 7); g.fill(); g.beginPath(); g.arc(X(70), Y(34), 1.2 * k, 0, 7); g.fill();
    })(size * 0.06, size * 0.025, size * 0.085);
    g.fillStyle = 'rgb(61,232,255)'; g.font = 'bold ' + (size * 0.06) + 'px sans-serif'; g.fillText('KURVENFLUG', size * 0.16, size * 0.09);
    g.fillStyle = '#fff'; g.font = (size * 0.042) + 'px sans-serif'; g.fillText(title + '  ' + '★'.repeat(stars) + '☆'.repeat(3 - stars), size * 0.06, size * 0.15);
    g.fillStyle = 'rgb(169,176,214)'; g.font = (size * 0.04) + 'px sans-serif'; g.fillText(d.F || '', size * 0.06, top + size + size * 0.045);
    cv.toBlob(function (blob) {
      var file = new File([blob], 'kurvenflug.png', { type: 'image/png' });
      if (navigator.canShare && navigator.canShare({ files: [file] })) { navigator.share({ files: [file], text: 'Meine Flugbahn in Kurvenflug: ' + title + ' 🚀' }).catch(function () {}); return; }
      var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'kurvenflug.png'; document.body.appendChild(a); a.click(); a.remove();
      setTimeout(function () { URL.revokeObjectURL(a.href); }, 5000); toast('Foto gespeichert 📸');
    }, 'image/png');
  };

  // ------------------------------------------------------------ Zurück (Browser-Knopf, Wischgeste, Esc)
  var backHandler = null;
  KF.onBack = function (f) {
    backHandler = f;
    history.replaceState({ kf: 'base' }, '');
    history.pushState({ kf: 'app' }, '');
    window.addEventListener('popstate', function () {
      if (backHandler && backHandler()) history.pushState({ kf: 'app' }, '');
      else history.back();   // ganz oben: Browser darf die Seite verlassen
    });
    window.addEventListener('keydown', function (e) { if (e.key === 'Escape' && backHandler) backHandler(); });
  };
  // ------------------------------------------------------------ Vorlesen mit der KI-Stimme Nova
  // Fertige Ansagen (MP3) von unserem Server stimmen.mathenachhilfe-online.com, zwischengespeichert im Browser (Cache „kf-nova“).
  // Texte mit Zahlen zur Laufzeit vertont unser Server auf Anfrage (/jetzt) – ohne Angaben zum Kind. Die App spricht nie mit OpenAI.
  // Notlösung (offline): eine deutsche Stimme des Browsers, die auf dem Gerät rechnet.
  var synth = window.speechSynthesis || null, speakCb = null, novaTurn = 0, novaEl = null, novaSpeed = 1;
  KF.novaSpeed = function (v) { novaSpeed = v; if (novaEl) novaEl.playbackRate = v; };
  function speaking(b) { if (speakCb) try { speakCb(b); } catch (e) {} }
  KF.onSpeaking = function (f) { speakCb = f; };
  function deviceSay(text) {
    if (!synth) { speaking(false); return; }
    var vs = synth.getVoices().filter(function (v) { return /^de([-_]|$)/i.test(v.lang) && v.localService; });
    if (!vs.length) { speaking(false); return; }
    var u = new SpeechSynthesisUtterance(text); u.lang = 'de-DE'; u.voice = vs[0]; u.rate = Math.min(2, 1.1 * novaSpeed); u.pitch = 1.08;
    u.onstart = function () { speaking(true); }; u.onend = u.onerror = function () { speaking(false); };
    synth.cancel(); synth.speak(u);
  }
  function hush() {
    novaTurn++;
    if (novaEl) { try { novaEl.pause(); URL.revokeObjectURL(novaEl.src); } catch (e) {} novaEl = null; }
    if (synth) synth.cancel();
  }
  KF.hush = function () { hush(); speaking(false); };
  function cached(url) {
    if (!window.caches) return fetch(url, { credentials: 'omit' }).then(function (r) { return r.ok ? r.arrayBuffer() : null; });
    return caches.open('kf-nova').then(function (c) {
      // offline gespeicherte Bestqualität (nova/original/…) zuerst
      return c.match(url.replace('/nova/', '/nova/original/')).then(function (b) { return b || c.match(url); }).then(function (hit) {
        if (hit) return hit.arrayBuffer();
        return fetch(url, { credentials: 'omit' }).then(function (r) {
          if (!r.ok) return null;
          return c.put(url, r.clone()).catch(function () {}).then(function () { return r.arrayBuffer(); });
        });
      });
    });
  }
  function live(text) {
    return fetch('https://stimmen.mathenachhilfe-online.com/jetzt', { method: 'POST', credentials: 'omit',
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: text, sprache: 'de' }) })
      .then(function (r) { return r.status === 200 ? r.json() : null; })
      .then(function (j) { return j && typeof j.url === 'string' && j.url.indexOf('https://stimmen.mathenachhilfe-online.com/nova/') === 0 ? j.url : null; });
  }
  var unreachCb = null; KF.onUnreach = function (f) { unreachCb = f; };
  function one(url, text, ask) {   // eine Ansage: Zwischenspeicher/Server, sonst (falls erlaubt) frisch vertonen
    return cached(url).then(function (b) {
      if (b || !ask) return b;
      return live(text).then(function (u) { return u ? cached(u) : null; });
    });
  }
  function playChain(bufs, i, my, text) {
    var a = new Audio(URL.createObjectURL(new Blob([bufs[i]], { type: 'audio/mpeg' })));
    a.preservesPitch = true; a.mozPreservesPitch = true; a.webkitPreservesPitch = true; a.playbackRate = novaSpeed;
    a.onended = a.onerror = function () {
      URL.revokeObjectURL(a.src);
      if (novaEl !== a) return;
      if (i + 1 < bufs.length && my === novaTurn) playChain(bufs, i + 1, my, text); else { novaEl = null; speaking(false); }
    };
    novaEl = a; speaking(true);
    return a.play().catch(function () { if (novaEl === a) { novaEl = null; fallback(text); } });
  }
  function fallback(text) { if (unreachCb) try { unreachCb(); } catch (e) {} deviceSay(text); }
  // items: Zeilen „Adresse⇥Text⇥ja/nein“ – erst der ganze Text, bei langen Texten danach dieselben Worte in ganzen Sätzen.
  // Gerätestimme nur, wenn Nova wirklich nichts liefern kann (kein Netz, Server sagt nein).
  KF.nova = function (items) {
    hush(); var my = novaTurn;
    var it = items.split('\n').filter(Boolean).map(function (l) { var p = l.split('\t'); return { url: p[0], text: p[1], ask: p[2] === '1' }; });
    var whole = it[0], parts = it.slice(1);
    one(whole.url, whole.text, whole.ask).then(function (b) {
      if (b) return [b];
      if (!parts.length) return null;
      return Promise.all(parts.map(function (p) { return one(p.url, p.text, p.ask); }))
        .then(function (bs) { return bs.every(Boolean) ? bs : null; });
    }).then(function (bufs) {
      if (my !== novaTurn) return;
      if (!bufs) { fallback(whole.text); return; }
      return playChain(bufs, 0, my, whole.text);
    }).catch(function () { if (my === novaTurn) fallback(whole.text); });
  };
  // „Alle Nova-Texte offline laden“: dieselben Dateien im Cache „kf-nova“ (Cache Storage, keine Obergrenze beim manuellen Laden)
  var NOVA = 'https://stimmen.mathenachhilfe-online.com/nova/', offAbort = false;
  function novaUrl(h, best) { return NOVA + (best ? 'original/' : '') + h.slice(0, 2) + '/' + h + '.mp3'; }
  function size(r) { var n = +(r.headers.get('content-length') || 0); return n ? Promise.resolve(n) : r.blob().then(function (b) { return b.size; }); }
  // hashes: Dateinamen der festen Texte (je Zeile) → cb(Standard gespeichert, Bestqualität gespeichert, Bytes beide, Server Standard, Server Best; -1 = unbekannt)
  KF.offStat = function (hashes, cb) {
    var hs = hashes.split('\n').filter(Boolean), host = /^(localhost|127\.)/.test(location.hostname) ? 'kurvenflug.mathenachhilfe-online.com' : location.hostname;
    var server = fetch(NOVA + 'stand-' + host + '.json', { credentials: 'omit', cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : null; }).then(function (j) { return j ? [j.bytes || -1, j.bytes_original || -1] : [-1, -1]; }, function () { return [-1, -1]; });
    var local = !window.caches ? Promise.resolve([0, 0, 0]) : caches.open('kf-nova').then(function (c) {
      return c.keys().then(function (ks) {
        var have = {}; ks.forEach(function (k) { have[k.url] = k; });
        var std = hs.map(function (h) { return novaUrl(h, false); }).filter(function (u) { return have[u]; });
        var best = hs.map(function (h) { return novaUrl(h, true); }).filter(function (u) { return have[u]; });
        return Promise.all(std.concat(best).map(function (u) { return c.match(u).then(function (r) { return r ? size(r) : 0; }); }))
          .then(function (sz) { return [std.length, best.length, sz.reduce(function (a, b) { return a + b; }, 0)]; });
      });
    });
    Promise.all([local, server]).then(function (x) { cb(x[0][0], x[0][1], x[0][2], x[1][0], x[1][1]); }, function () { cb(0, 0, 0, -1, -1); });
  };
  // Nur fehlende Dateien, 4 gleichzeitig; 404 still überspringen; progress(fertig, alle), done(Fehlerzahl, abgebrochen)
  // best: Bestqualität (nova/original/…); liefert original/ 404, wird die normale Datei als Standard gespeichert
  // (so holt ein späteres Tippen die Bestqualität nach, sobald der Server sie hat)
  KF.offLoad = function (hashes, best, progress, done) {
    offAbort = false;
    if (!window.caches) { done(-1, false); return; }
    caches.open('kf-nova').then(function (c) {
      return c.keys().then(function (ks) {
        var have = {}; ks.forEach(function (k) { have[k.url] = 1; });
        var todo = hashes.split('\n').filter(Boolean).map(function (h) { return novaUrl(h, best); }).filter(function (u) { return !have[u]; });
        var i = 0, fin = 0, fail = 0, n = todo.length;
        progress(0, n);
        if (!n) { done(0, false); return; }
        return new Promise(function (resolve) {
          function next() {
            if (offAbort || i >= n) { if (fin >= Math.min(i, n) ) resolve(); return; }
            var u = todo[i++];
            var key = u;
            fetch(u, { credentials: 'omit' }).then(function (r) {
              if (r.status === 404 && best) { key = u.replace('/nova/original/', '/nova/'); return have[key] ? null : fetch(key, { credentials: 'omit' }); }
              return r;
            }).then(function (r) {
              if (!r || r.status === 404) return;
              if (!r.ok) { fail++; return; }
              return c.put(key, r);
            }).catch(function () { fail++; }).then(function () { fin++; progress(fin, n); if (fin >= n || (offAbort && fin >= i)) resolve(); else next(); });
          }
          for (var k = 0; k < 4; k++) next();
        }).then(function () { done(fail, offAbort); });
      });
    }).catch(function () { done(-1, offAbort); });
  };
  KF.offCancel = function () { offAbort = true; };
  KF.offClear = function (cb) { if (window.caches) caches.delete('kf-nova').then(function () { cb(); }, function () { cb(); }); else cb(); };

  // Reste der früheren Offline-Stimme (2.0–2.3) aus dem Browser-Speicher räumen
  if (window.caches) caches.delete('kf-stimme').catch(function () {});
})();
