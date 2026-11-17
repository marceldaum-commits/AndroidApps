// Lux spricht mit „Nova“ (KI-generierte Stimme): fertige MP3-Dateien von unserem Server abspielen und im Browser behalten.
// Der Browser spricht nur mit unserem Server – nie mit dem Dienst, der die Stimme erzeugt. Es gehen keine Angaben über den Nutzer mit.
(function () {
  const STORE = 'raumlicht-nova';
  let ctx = null, turn = 0, node = null;
  const N = window.__rlNova = { speaking: false };
  // Reste der früheren Stimme „Thorsten“ (63 MB) wegräumen
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
  async function get(url) {
    const r = await fetch(url, { credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer' });
    return { status: r.status, buf: r.status === 200 ? await r.arrayBuffer() : null };
  }
  // 1. gespeichert  2. fertige Datei vom Server  3. Server bitten, sie zu erzeugen (nur für Texte, die erst in der Aufgabe entstehen)
  async function obtain(server, url, live, body) {
    const hit = await cached(url); if (hit) return hit;
    let r = await get(url);
    if (r.status === 404 && live) {
      const a = await fetch(live, { method: 'POST', credentials: 'omit', referrerPolicy: 'no-referrer', headers: { 'Content-Type': 'application/json' }, body: body });
      if (a.status !== 200) return null;
      const j = await a.json(); let u = j && typeof j.url === 'string' ? j.url : null;
      if (u && u.charAt(0) === '/') u = server + u;
      if (!u || u.indexOf(server + '/') !== 0) return null;
      r = await get(u);
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
  // Antwort: 'ok' (spielt), 'fehlt' (nicht zu bekommen – dann liest die Stimme des Geräts), 'alt' (inzwischen kam ein neuer Satz)
  N.say = async function (server, url, live, body, volume, rate) {
    N.stop(); const mine = turn; rate = rate || 1;
    try {
      // für Tests: anderer Server (window.__rlNovaServer)
      const s = window.__rlNovaServer || server;
      if (s !== server) { url = s + url.substring(server.length); if (live) live = s + live.substring(server.length); }
      const buf = await obtain(s, url, live, body);
      if (mine !== turn) return 'alt';
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
