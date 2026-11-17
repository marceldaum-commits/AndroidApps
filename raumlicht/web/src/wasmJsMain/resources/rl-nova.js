// Lux spricht mit „Nova“ (KI-generierte Stimme): fertige MP3-Dateien von unserem Server abspielen und im Browser behalten.
// Der Browser spricht nur mit unserem Server – nie mit dem Dienst, der die Stimme erzeugt. Es gehen keine Angaben über den Nutzer mit.
(function () {
  const STORE = 'raumlicht-nova';
  let ctx = null, turn = 0, node = null;
  const N = window.__rlNova = { speaking: false };
  // Reste des früheren Stimm-Pakets (63 MB, bis 2.16) wegräumen
  try { if ('caches' in window) caches.delete('raumlicht-stimme'); } catch (e) {}
  function audio() {
    if (!ctx) { const C = window.AudioContext || window.webkitAudioContext; if (!C) return null; ctx = new C(); }
    if (ctx.state === 'suspended') ctx.resume().catch(function () {});
    return ctx;
  }
  // Browser erlauben Ton erst nach einer Berührung
  ['pointerdown', 'keydown', 'touchend'].forEach(function (e) { window.addEventListener(e, function () { if (ctx && ctx.state === 'suspended') ctx.resume().catch(function () {}); }, { passive: true }); });
  async function cached(url) {
    try { if (!('caches' in window) || !(await caches.has(STORE))) return null; const r = await (await caches.open(STORE)).match(url); return r ? await r.arrayBuffer() : null; } catch (e) { return null; }
  }
  async function keep(url, buf) {
    try { if ('caches' in window) await (await caches.open(STORE)).put(url, new Response(buf, { headers: { 'Content-Type': 'audio/mpeg' } })); } catch (e) {}
  }
  // Antwort 0 = keine Verbindung. Bei einem Aussetzer (keine Verbindung, Serverfehler) kurz warten und noch einmal versuchen.
  async function once(url, post) {
    try {
      const r = await fetch(url, post ? { method: 'POST', credentials: 'omit', referrerPolicy: 'no-referrer', headers: { 'Content-Type': 'application/json' }, body: post }
                                      : { credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer' });
      return { status: r.status, buf: r.status === 200 && !post ? await r.arrayBuffer() : null, json: r.status === 200 && post ? await r.json() : null };
    } catch (e) { return { status: 0, buf: null, json: null }; }
  }
  async function get(url, post) {
    const first = await once(url, post); if (first.status !== 0 && first.status < 500) return first;
    await new Promise(function (ok) { setTimeout(ok, N.retryPause); });
    return once(url, post);
  }
  N.retryPause = 700;
  // 1. gespeichert  2. fertige Datei vom Server  3. Server bitten, sie zu erzeugen (nur für Texte, die erst in der Aufgabe entstehen)
  // Ergebnis: die Datei – oder 'offline' (gar keine Verbindung) – oder null (Server erreichbar, aber keine Datei)
  async function obtain(server, url, live, body) {
    const off = await offlineHit(url); if (off) return off;                // offline geladen (Einstellungen), Bestqualität zuerst
    const hit = await cached(url); if (hit) return hit;
    if (navigator.onLine === false) return 'offline';                      // gar nicht erst fragen (kein Fehler in der Konsole)
    let r = await get(url);
    if (r.status === 0) return 'offline';
    if (r.status === 404 && live) {
      const a = await get(live, body);
      if (a.status === 0) return 'offline';
      if (a.status !== 200) return null;
      let u = a.json && typeof a.json.url === 'string' ? a.json.url : null;
      if (u && u.charAt(0) === '/') u = server + u;
      if (!u || u.indexOf(server + '/') !== 0) return null;
      r = await get(u);
      if (r.status === 0) return 'offline';
    }
    if (r.status !== 200 || !r.buf || r.buf.byteLength < 256) return null;
    keep(url, r.buf.slice(0));
    return r.buf;
  }
  // Anderes Sprechtempo bei gleicher Tonhöhe kann nur ein <audio>-Element (preservesPitch). Es wird bei der ersten Berührung „aufgeschlossen“,
  // sonst dürfte es später ohne Berührung nicht spielen. Klappt das Abspielen dort nicht, läuft der Satz im normalen Tempo über Web Audio.
  let el = null, blob = null;
  const QUIET = 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=';
  function unlock() { if (el) return; try { el = new Audio(); el.preload = 'auto'; el.src = QUIET; const p = el.play(); if (p && p.catch) p.catch(function () {}); } catch (e) { el = null; } }
  ['pointerdown', 'keydown', 'touchend'].forEach(function (e) { window.addEventListener(e, unlock, { passive: true }); });
  function quiet() {
    if (node) { try { node.onended = null; node.stop(); } catch (e) {} node = null; }
    if (el) { try { el.onended = null; el.pause(); } catch (e) {} }
    if (blob) { try { URL.revokeObjectURL(blob); } catch (e) {} blob = null; }
    N.speaking = false;
  }
  N.stop = function () { turn++; quiet(); };

  // ---- „Alle Nova-Texte offline laden“: die fertigen Dateien aller festen Texte in den Speicher des Browsers holen (Standard oder Bestqualität)
  const OFF = 'raumlicht-nova-offline';
  const O = N.offline = { loading: false, done: 0, total: 0, standardDone: 0, bestDone: 0, standardStored: 0, bestStored: 0, standardBytes: -1, bestBytes: -1 };
  let keys = null, cancelled = false;
  function best(url) { return url.replace('/nova/', '/nova/original/'); }
  async function offlineHit(url) {
    try { if (!('caches' in window) || !(await caches.has(OFF))) return null; const c = await caches.open(OFF); const r = (await c.match(best(url))) || (await c.match(url)); return r ? await r.arrayBuffer() : null; } catch (e) { return null; }
  }
  // Dateinamen aller festen Texte – aus der eigenen Liste, gerechnet wie auf dem Server
  async function loadKeys() {
    if (keys) return keys;
    const j = await (await fetch('vorlesen/texte.json', { cache: 'no-cache' })).json(); const out = [];
    for (const t of j.texte) {
      const d = new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode('nova|' + t.sprache + '|' + t.text)));
      out.push(Array.prototype.map.call(d, function (b) { return ('0' + b.toString(16)).slice(-2); }).join('').substring(0, 32));
    }
    keys = out; return keys;
  }
  function file(server, k, isBest) { return server + '/nova/' + (isBest ? 'original/' : '') + k.substring(0, 2) + '/' + k + '.mp3'; }
  async function count(s) {
    const ks = await loadKeys(); O.total = ks.length; let sd = 0, bd = 0, sb = 0, bb = 0;
    if (await caches.has(OFF)) {
      const c = await caches.open(OFF); const have = {};
      for (const q of await c.keys()) { const r = await c.match(q); have[q.url] = r ? (+r.headers.get('X-Bytes') || 0) : 0; }
      for (const k of ks) { const a = have[file(s, k, false)], b = have[file(s, k, true)]; if (a !== undefined) { sd++; sb += a; } if (b !== undefined) { bd++; bb += b; } }
    }
    O.standardDone = sd; O.bestDone = bd; O.standardStored = sb; O.bestStored = bb;
  }
  O.refresh = async function (server) {
    const s = window.__rlNovaServer || server;
    try { await count(s); } catch (e) {}
    if (navigator.onLine === false) return;                                  // offline: keine Abfrage (und kein Fehler in der Konsole)
    try {
      const r = await fetch(s + '/nova/stand-' + (window.__rlNovaHost || location.hostname) + '.json', { credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer' });
      if (r.ok) { const j = await r.json(); O.standardBytes = typeof j.bytes === 'number' ? j.bytes : -1; O.bestBytes = typeof j.bytes_original === 'number' ? j.bytes_original : -1; }
    } catch (e) {}
  };
  // Nur Fehlendes laden, vier Dateien gleichzeitig; nach einem Abbruch geht es beim nächsten Mal dort weiter
  O.start = async function (server, isBest) {
    if (O.loading) return; O.loading = true; cancelled = false;
    const s = window.__rlNovaServer || server;
    try {
      const ks = await loadKeys(); const c = await caches.open(OFF); const have = {};
      for (const q of await c.keys()) have[q.url] = true;
      const missing = ks.filter(function (k) { return !have[file(s, k, isBest)]; });
      O.total = ks.length; O.done = ks.length - missing.length;
      async function worker() {
        while (!cancelled && missing.length) {
          const k = missing.shift();
          try {
            let r = await get(file(s, k, isBest)), as = isBest;
            // Bestqualität fehlt (noch): die normale Datei als Standard ablegen – beim nächsten Tippen wird nur das Fehlende nachgeholt
            if (isBest && r.status === 404) { as = false; r = have[file(s, k, false)] ? { status: 0 } : await get(file(s, k, false)); }
            if (r.status === 200 && r.buf && r.buf.byteLength >= 256)               // 404 wird still übersprungen
              await c.put(file(s, k, as), new Response(r.buf, { headers: { 'Content-Type': 'audio/mpeg', 'X-Bytes': String(r.buf.byteLength) } }));
          } catch (e) {}
          O.done++;
        }
      }
      await Promise.all([worker(), worker(), worker(), worker()]);
      await count(s);
    } catch (e) {}
    O.loading = false;
  };
  O.cancel = function () { cancelled = true; };
  // Offline-Stimme löschen – beide Qualitäten
  O.remove = async function (server) { cancelled = true; try { await caches.delete(OFF); await count(window.__rlNovaServer || server); } catch (e) {} };
  async function paced(buf, volume, rate, mine) {
    if (!el) return false;
    try {
      blob = URL.createObjectURL(new Blob([buf], { type: 'audio/mpeg' }));
      el.src = blob; el.preservesPitch = true; el.webkitPreservesPitch = true; el.mozPreservesPitch = true;
      try { el.volume = volume; } catch (e) {}
      el.defaultPlaybackRate = rate; el.playbackRate = rate;
      el.onended = function () { if (mine === turn) N.speaking = false; };
      await el.play(); el.playbackRate = rate;
      if (mine !== turn) { quiet(); return true; }
      N.speaking = true; return true;
    } catch (e) { return false; }
  }
  // Antwort: 'ok' (spielt), 'offline' (kein Netz und nichts gespeichert – dann liest die Stimme des Geräts),
  // 'fehlt' (Server erreichbar, aber keine Datei – dieser Satz bleibt stumm), 'alt' (inzwischen kam ein neuer Satz)
  N.say = async function (server, url, live, body, volume, rate) {
    N.stop(); const mine = turn; rate = rate || 1;
    try {
      // für Tests: anderer Server (window.__rlNovaServer)
      const s = window.__rlNovaServer || server;
      if (s !== server) { url = s + url.substring(server.length); if (live) live = s + live.substring(server.length); }
      const buf = await obtain(s, url, live, body);
      if (mine !== turn) return 'alt';
      if (buf === 'offline') return 'offline';
      if (!buf) return 'fehlt';
      if (Math.abs(rate - 1) > 0.01 && await paced(buf.slice(0), volume, rate, mine)) { N.last = { url: url, rate: rate, seconds: el.duration || 0 }; return mine === turn ? 'ok' : 'alt'; }
      if (mine !== turn) return 'alt';
      const c = audio(); if (!c) return 'fehlt';
      const data = await c.decodeAudioData(buf.slice(0));
      if (mine !== turn) return 'alt';
      const src = c.createBufferSource(); src.buffer = data;
      const g = c.createGain(); g.gain.value = volume; src.connect(g); g.connect(c.destination);
      src.onended = function () { if (node === src) { node = null; N.speaking = false; } };
      node = src; N.speaking = true; N.last = { url: url, rate: 1, seconds: data.duration }; src.start();
      return 'ok';
    } catch (e) { return mine === turn ? 'fehlt' : 'alt'; }
  };
})();
