// Wissensgarten WebApp – Brummis Stimme „Nova“ (KI-Stimme von OpenAI, vertont von unserem Stimm-Server).
// Die WebApp spricht nie mit OpenAI. Sie lädt fertige MP3-Dateien von stimmen.mathenachhilfe-online.com
// (ohne Cookies, ohne Referrer) und legt sie im Browser-Speicher ab (Cache Storage „wissensgarten-nova“).
// Ohne Internet liest still eine Stimme des Browsers, die auf dem Gerät spricht.
(function () {
  const wg = (window.wg = window.wg || {});

  // ---------------------------------------------------------------- Notlösung: Stimmen des Browsers
  // Nur Stimmen, die auf dem Gerät sprechen (localService) – Online-Stimmen würden den Text an fremde Server schicken.
  const localVoices = (lang) => ('speechSynthesis' in window ? speechSynthesis.getVoices() : [])
    .filter((v) => v.localService && v.lang && v.lang.toLowerCase().replace('_', '-').startsWith(lang));
  wg.ttsAvailable = () => {
    if (!('speechSynthesis' in window)) return false;
    const all = speechSynthesis.getVoices();
    return all.length === 0 || localVoices('de').length > 0;
  };
  let sayDone = null;
  /** Sprechen mit der Browser-Stimme; das Versprechen erfüllt sich, wenn sie fertig ist (oder abgebrochen wurde). */
  wg.say = (text, lang, rate) => new Promise((ok) => {
    if (!('speechSynthesis' in window)) { ok(null); return; }
    const l2 = lang.slice(0, 2).toLowerCase();
    const vs = localVoices(l2);
    const v = vs.find((x) => x.lang.replace('_', '-').toLowerCase() === lang.toLowerCase()) || vs[0];
    if (!v) { ok(null); return; }                     // keine Stimme auf dem Gerät für diese Sprache: lieber still als online
    wg.stopSpeaking();
    let done = false;
    const fin = () => { if (done) return; done = true; clearTimeout(t); if (sayDone === fin) sayDone = null; ok(null); };
    const t = setTimeout(fin, 60000);
    const u = new SpeechSynthesisUtterance(text); u.lang = v.lang; u.voice = v; u.pitch = 1.25; u.rate = rate || 0.95;
    u.onend = u.onerror = fin;
    sayDone = fin;
    speechSynthesis.speak(u);
  });
  wg.stopSpeaking = () => {
    if ('speechSynthesis' in window) speechSynthesis.cancel();
    if (sayDone) sayDone();
  };

  // ---------------------------------------------------------------- Nova
  const BASE = 'https://stimmen.mathenachhilfe-online.com';
  const STORE = 'wissensgarten-nova';
  const MAX_FILE = 5 * 1024 * 1024;
  const key = (h) => BASE + '/nova/' + h.slice(0, 2) + '/' + h + '.mp3';
  // Ohne Cookies und ohne Herkunftsangabe – der Server sieht nur, welche Datei geholt wird
  const OPTS = { credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store', mode: 'cors' };
  const mem = new Map();                              // Notlösung, falls der Browser-Speicher fehlt (z. B. ohne https)
  const nova = (wg.nova = {});

  // Alte Stimme „Thorsten“ (bis Version 2.0.2, ca. 63 MB) einmal aus dem Browser-Speicher löschen
  try { if ('caches' in window) caches.delete('wissensgarten-stimme').catch(() => {}); } catch (e) {}

  const timed = (ms) => { const c = new AbortController(); const t = setTimeout(() => c.abort(), ms); return [c.signal, () => clearTimeout(t)]; };

  nova.has = async (h) => {
    if (mem.has(h)) return true;
    try { const c = await caches.open(STORE); return !!(await c.match(key(h))); } catch (e) { return false; }
  };

  /** Datei holen und bei 200 speichern; liefert den HTTP-Status oder -1 (offline, Zeitüberschreitung, zu groß) */
  async function store(url, h) {
    const [signal, done] = timed(10000);
    try {
      const r = await fetch(url, Object.assign({}, OPTS, { signal }));
      if (r.status !== 200) return r.status;
      const b = await r.blob();
      if (!b.size || b.size > MAX_FILE) return -1;
      try {
        const c = await caches.open(STORE);
        await c.put(key(h), new Response(b, { headers: { 'Content-Type': 'audio/mpeg' } }));
      } catch (e) { mem.set(h, b); }
      return 200;
    } catch (e) { return -1; } finally { done(); }
  }
  nova.load = (url, h) => store(url, h);

  /** Laufzeit-Text einmalig vertonen lassen (POST /jetzt) und die gelieferte Datei speichern */
  nova.now = async (text, sprache, h) => {
    const [signal, done] = timed(15000);
    try {
      const r = await fetch(BASE + '/jetzt', Object.assign({}, OPTS, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: text, sprache: sprache }), signal,
      }));
      if (r.status !== 200) return r.status;
      const j = await r.json();
      if (!j || typeof j.url !== 'string' || !j.url.startsWith(BASE + '/')) return -1;   // nur vom eigenen Stimm-Server
      return (await store(j.url, h)) === 200 ? 200 : -1;
    } catch (e) { return -1; } finally { done(); }
  };

  let turn = 0, finish = null;
  /** Gespeicherte Datei abspielen – Tempo ohne Tonhöhen-Änderung. true = zu Ende gespielt (oder angehalten), false = ging nicht */
  nova.play = (h, rate) => new Promise((ok) => {
    nova.stop();
    const my = ++turn;
    (async () => {
      let blob = mem.get(h);
      if (!blob) { try { const c = await caches.open(STORE); const r = await c.match(key(h)); if (r) blob = await r.blob(); } catch (e) {} }
      if (my !== turn) { ok(true); return; }
      if (!blob) { ok(false); return; }
      const url = URL.createObjectURL(blob);
      const a = new Audio();
      let done = false;
      const fin = (v) => {
        if (done) return; done = true;
        if (finish === fin) finish = null;
        try { a.pause(); } catch (e) {}
        URL.revokeObjectURL(url);
        ok(v);
      };
      finish = fin;
      a.preservesPitch = true; a.mozPreservesPitch = true; a.webkitPreservesPitch = true;
      a.onended = () => fin(true);
      a.onerror = () => fin(false);
      a.src = url;
      a.defaultPlaybackRate = rate || 1; a.playbackRate = rate || 1;
      const p = a.play();
      if (p && p.catch) p.catch(() => fin(false));
    })();
  });
  nova.stop = () => { turn++; if (finish) finish(true); };
})();
