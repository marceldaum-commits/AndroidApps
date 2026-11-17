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
  N.stop = function () { turn++; if (node) { try { node.onended = null; node.stop(); } catch (e) {} node = null; } N.speaking = false; };
  // Antwort: 'ok' (spielt), 'fehlt' (nicht zu bekommen – dann liest die Stimme des Geräts), 'alt' (inzwischen kam ein neuer Satz)
  N.say = async function (server, url, live, body, volume) {
    N.stop(); const mine = turn;
    try {
      // für Tests: anderer Server (window.__rlNovaServer)
      const s = window.__rlNovaServer || server;
      if (s !== server) { url = s + url.substring(server.length); if (live) live = s + live.substring(server.length); }
      const buf = await obtain(s, url, live, body);
      if (mine !== turn) return 'alt';
      const c = audio(); if (!buf || !c) return 'fehlt';
      const data = await c.decodeAudioData(buf.slice(0));
      if (mine !== turn) return 'alt';
      const src = c.createBufferSource(); src.buffer = data;
      const g = c.createGain(); g.gain.value = volume; src.connect(g); g.connect(c.destination);
      src.onended = function () { if (node === src) { node = null; N.speaking = false; } };
      node = src; N.speaking = true; N.last = { url: url, seconds: data.duration }; src.start();
      return 'ok';
    } catch (e) { return mine === turn ? 'fehlt' : 'alt'; }
  };
})();
