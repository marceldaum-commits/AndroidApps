// Kurvenflug WebApp – kleine Helfer für Ton, Teilen, Fotos und die Zurück-Taste.
// Keine Verbindungen nach außen, kein Tracking: alles bleibt im Browser. Einzige Ausnahme, nur auf ausdrücklichen Wunsch:
// das Stimm-Paket „Thorsten“ fürs Vorlesen kommt einmalig von unserem Server stimmen.mathenachhilfe-online.com (siehe unten).
(function () {
  'use strict';
  var KF = window.KF = {};

  // ------------------------------------------------------------ Schnellstart: große Dateien sofort parallel laden
  // window.KF_FILES (vom Build in index.html eingesetzt): [{u: Pfad, s: Bytes, t: Typ}]. Wir laden alles gleich los,
  // zählen die Bytes für den Fortschrittsbalken und reichen die Daten an die App weiter, sobald sie danach fragt.
  var files = window.KF_FILES || [], total = files.reduce(function (a, f) { return a + f.s; }, 0), loaded = 0, pending = {};
  function progress() {
    var bar = document.getElementById('kf-bar'), txt = document.getElementById('kf-pct');
    if (!bar || !total) return;
    var p = Math.min(1, loaded / total);
    bar.style.width = (4 + 96 * p) + '%';
    if (txt) txt.textContent = p < 1 ? 'Lade … ' + Math.round(p * 100) + ' %' : 'Starte die Triebwerke …';
  }
  // Jede Datei wird sofort angefragt; die Daten fließen als Strom weiter, damit der Browser das Wasm schon
  // während des Ladens übersetzen kann (instantiateStreaming). Wir zählen nur mit.
  files.forEach(function (f) {
    pending[f.u] = fetch(f.u).then(function (r) {
      if (!r.ok || !r.body) return r;
      var reader = r.body.getReader();
      var body = new ReadableStream({
        start: function (ctrl) {   // gleich alles abholen (nicht auf die App warten), die App liest später mit
          (function pump() {
            reader.read().then(function (x) {
              if (x.done) { ctrl.close(); return; }
              loaded += x.value.length; progress(); ctrl.enqueue(x.value); pump();
            }, function (e) { ctrl.error(e); });
          })();
        }
      });
      return new Response(body, { status: 200, headers: { 'Content-Type': f.t } });
    });
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
  var ctx = null, unlocked = false, unlockCbs = [];
  KF.bufData = []; var buffers = [];
  function audio() {
    if (!ctx) { var C = window.AudioContext || window.webkitAudioContext; if (!C) return null; ctx = new C(); }
    return ctx;
  }
  function unlock() {
    var c = audio(); if (!c) return;
    if (c.state === 'suspended') c.resume();
    if (!unlocked) { unlocked = true; unlockCbs.forEach(function (f) { try { f(); } catch (e) { console.warn(e); } }); }
  }
  ['pointerdown', 'keydown', 'touchend'].forEach(function (t) { window.addEventListener(t, unlock, { capture: true, passive: true }); });
  KF.onUnlock = function (f) { if (unlocked) f(); else unlockCbs.push(f); };
  KF.bufNew = function (n, rate) { KF.bufData.push(new Float32Array(n)); buffers.push({ rate: rate, buf: null }); return KF.bufData.length - 1; };
  KF.bufDone = function (id) {
    var c = audio(); if (!c) return;
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
    document.body.appendChild(t); setTimeout(function () { t.remove(); }, 2600);
  }
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
    g.fillStyle = 'rgb(61,232,255)'; g.font = 'bold ' + (size * 0.06) + 'px sans-serif'; g.fillText('KURVENFLUG', size * 0.06, size * 0.09);
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
  // ------------------------------------------------------------ Vorlesen
  // a) Stimmen des Browsers: nur deutsche Stimmen, die auf dem Gerät rechnen (localService) – so geht kein Text ins Netz.
  // b) „Thorsten“: natürliche Piper-Stimme, rechnet im Browser (ONNX Runtime Web, MIT-Lizenz). Die Lautschrift kommt fertig aus der App
  //    (stimme/*.tsv, vorab erzeugt) – kein espeak-ng. Stimmdatei nur auf Wunsch von unserem Server (Adresse und Prüfsumme aus der App), geprüft,
  //    danach im Browser-Speicher (Cache „kf-stimme“, auch offline nutzbar, jederzeit entfernbar).
  var synth = window.speechSynthesis || null, speakCb = null;
  function speaking(b) { if (speakCb) try { speakCb(b); } catch (e) {} }
  function deVoices() {
    if (!synth) return [];
    return synth.getVoices().filter(function (v) { return /^de([-_]|$)/i.test(v.lang) && v.localService; });
  }
  KF.voices = function () { return deVoices().map(function (v) { return v.name; }).join('\n'); };
  KF.onVoices = function (f) { if (synth) synth.addEventListener('voiceschanged', function () { f(); }); };
  KF.onSpeaking = function (f) { speakCb = f; };
  KF.say = function (text, name, rate) {
    natStop(); if (!synth) return;
    synth.cancel();
    var vs = deVoices(), u = new SpeechSynthesisUtterance(text), v = vs.filter(function (x) { return x.name === name; })[0] || vs[0];
    u.lang = 'de-DE'; if (v) u.voice = v; u.rate = rate; u.pitch = 1.08;
    u.onstart = function () { speaking(true); }; u.onend = u.onerror = function () { speaking(false); };
    synth.speak(u);
  };
  KF.hush = function () { if (synth) synth.cancel(); natStop(); speaking(false); };

  var PACK = { cache: 'kf-stimme', key: 'stimme/thorsten-piper.onnx', old: 'stimme/thorsten.onnx',
    helpers: ['stimme/saetze.tsv', 'stimme/woerter.tsv', 'stimme/tokens.txt', 'ort/ort.wasm.min.mjs', 'ort/ort-wasm-simd-threaded.mjs', 'ort/ort-wasm-simd-threaded.wasm'] };
  function abs(p) { return new URL(p, location.href).href; }
  KF.packHas = function (cb) {
    if (!window.caches) { cb(false); return; }
    caches.open(PACK.cache).then(function (c) { c.delete(abs(PACK.old)); return c.match(abs(PACK.key)); }).then(function (r) { cb(!!r); }, function () { cb(false); });
  };
  var packAbort = null;
  KF.packLoad = function (url, size, sha, progress, done) {
    PACK.url = url; PACK.size = size; PACK.sha = sha;
    if (!window.caches || !window.crypto || !crypto.subtle) { done('Dieser Browser kann die Stimme nicht speichern.'); return; }
    packAbort = new AbortController(); var signal = packAbort.signal;
    caches.open(PACK.cache).then(function (c) {
      // erst die kleinen Helfer aus der App (Lautschrift, Rechenkern) – damit Thorsten später auch offline spricht
      return c.addAll(PACK.helpers.map(function (h) { return new Request(abs(h), { cache: 'no-cache' }); })).then(function () {
        return fetch(PACK.url, { signal: signal, credentials: 'omit' });
      }).then(function (r) {
        if (!r.ok || !r.body) throw new Error('Der Server antwortet mit ' + r.status + '.');
        var buf = new Uint8Array(PACK.size), got = 0, shown = -1, rd = r.body.getReader();
        function pump() {
          return rd.read().then(function (x) {
            if (x.done) return;
            if (got + x.value.length > PACK.size) throw new Error('Die Datei ist größer als erwartet.');
            buf.set(x.value, got); got += x.value.length;
            var p = Math.min(99, Math.floor(got * 100 / PACK.size)); if (p !== shown) { shown = p; progress(p); }
            return pump();
          });
        }
        return pump().then(function () {
          if (got !== PACK.size) throw new Error('Die Datei ist unvollständig angekommen.');
          return crypto.subtle.digest('SHA-256', buf);
        }).then(function (h) {
          var hex = Array.from(new Uint8Array(h)).map(function (b) { return ('0' + b.toString(16)).slice(-2); }).join('');
          if (hex !== PACK.sha) throw new Error('Die Datei ist beschädigt angekommen.');
          return c.put(abs(PACK.key), new Response(buf, { headers: { 'Content-Type': 'application/octet-stream' } }));
        });
      });
    }).then(function () {
      if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(function () {});
      done('');
    }, function (e) {
      if (signal.aborted) done('abgebrochen');
      else done(e && e.message && !/fetch|network/i.test(e.message) ? e.message : 'Keine Verbindung. Bitte später noch einmal versuchen.');
    });
  };
  KF.packCancel = function () { if (packAbort) packAbort.abort(); };
  KF.packRemove = function () { natStop(); sessP = null; if (window.caches) caches.delete(PACK.cache).catch(function () {}); };

  var sessP = null, natTurn = 0, natSrc = [];
  function session() {
    if (sessP) return sessP;
    sessP = import(abs('ort/ort.wasm.min.mjs')).then(function (ort) {
      ort.env.wasm.wasmPaths = abs('ort/');
      ort.env.wasm.numThreads = window.crossOriginIsolated ? Math.min(4, navigator.hardwareConcurrency || 1) : 1;
      return caches.open(PACK.cache).then(function (c) { return c.match(abs(PACK.key)); }).then(function (r) {
        if (!r) throw new Error('Stimm-Paket fehlt');
        return r.arrayBuffer();
      }).then(function (b) { return ort.InferenceSession.create(new Uint8Array(b)); }).then(function (s) { return { ort: ort, s: s }; });
    });
    sessP.catch(function () { sessP = null; });
    return sessP;
  }
  KF.natWarm = function () { session().catch(function (e) { console.warn('Stimme', e); }); };
  function natStop() { natTurn++; natSrc.forEach(function (s) { try { s.stop(); } catch (e) {} }); natSrc = []; }
  // ids: Sätze (je eine Zeile, Nummern mit Komma) – berechnet in der App aus der Lautschrift
  KF.natSay = function (ids, rate, fail) {
    natStop(); if (synth) synth.cancel();
    var my = natTurn, sents = ids.split('\n').filter(Boolean).map(function (l) { return l.split(',').map(Number); });
    var c = audio(); if (!c) { fail(); return; }
    session().then(function (x) {
      if (my !== natTurn) return;
      var t = 0, chain = Promise.resolve(); speaking(true);
      sents.forEach(function (s) {
        chain = chain.then(function () {
          if (my !== natTurn) return;
          return x.s.run({
            input: new x.ort.Tensor('int64', BigInt64Array.from(s.map(BigInt)), [1, s.length]),
            input_lengths: new x.ort.Tensor('int64', BigInt64Array.from([BigInt(s.length)]), [1]),
            scales: new x.ort.Tensor('float32', Float32Array.from([0.667, 1 / rate, 0.8]), [3])
          }).then(function (r) {
            if (my !== natTurn) return;
            var d = r.output.data, b = c.createBuffer(1, d.length, 22050); b.getChannelData(0).set(d);
            var src = c.createBufferSource(); src.buffer = b; src.connect(c.destination);
            t = Math.max(t, c.currentTime + 0.03); src.start(t); t += b.duration + 0.15; natSrc.push(src);
            var end = t; src.onended = function () { if (my === natTurn && end >= t) speaking(false); };
          });
        });
      });
      return chain;
    }).catch(function (e) { console.warn('Stimme', e); if (my === natTurn) { speaking(false); fail(); } });
  };
  KF.text = function (u, cb) { fetch(u).then(function (r) { return r.ok ? r.text() : ''; }).then(cb, function () { cb(''); }); };
})();
