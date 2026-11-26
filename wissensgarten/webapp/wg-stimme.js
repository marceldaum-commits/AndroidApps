// Wissensgarten WebApp – Brummis KI-generierte Stimme (vertont auf unserem eigenen Server).
// Die WebApp lädt fertige MP3-Dateien von stimmen.mathenachhilfe-online.com
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
  const STORE = 'wissensgarten-nova';                 // automatischer Zwischenspeicher
  // „Alle Nova-Texte offline laden“: eigene Speicher, damit nichts davon versehentlich weggeräumt wird
  const OFFLINE = { STANDARD: 'wissensgarten-nova-offline', BEST: 'wissensgarten-nova-original' };
  const ALL = [OFFLINE.BEST, OFFLINE.STANDARD, STORE];   // Reihenfolge beim Abspielen: Bestqualität zuerst
  const MAX_FILE = 5 * 1024 * 1024;
  const key = (h) => BASE + '/nova/' + h.slice(0, 2) + '/' + h + '.mp3';
  // Ohne Cookies und ohne Herkunftsangabe – der Server sieht nur, welche Datei geholt wird
  const OPTS = { credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store', mode: 'cors' };
  const mem = new Map();                              // Notlösung, falls der Browser-Speicher fehlt (z. B. ohne https)
  const nova = (wg.nova = {});

  // Frühere Offline-Stimme (bis Version 2.0.2, ca. 63 MB) einmal aus dem Browser-Speicher löschen
  try { if ('caches' in window) caches.delete('wissensgarten-stimme').catch(() => {}); } catch (e) {}

  // Sicher offline (Flugmodus): gar nicht erst anfragen – sonst meldet der Browser jeden Versuch als Fehler in der Konsole
  const offline = () => navigator.onLine === false;
  const timed = (ms) => { const c = new AbortController(); const t = setTimeout(() => c.abort(), ms); return [c.signal, () => clearTimeout(t)]; };

  async function find(h) {
    for (const name of ALL) {
      try { if (await caches.has(name)) { const r = await (await caches.open(name)).match(key(h)); if (r) return r; } } catch (e) {}
    }
    return null;
  }
  nova.has = async (h) => mem.has(h) || !!(await find(h));

  /** Datei holen und bei 200 speichern; liefert den HTTP-Status oder -1 (offline, Zeitüberschreitung, zu groß) */
  async function store(url, h, cacheName) {
    if (offline()) return -1;
    const [signal, done] = timed(10000);
    try {
      const r = await fetch(url, Object.assign({}, OPTS, { signal }));
      if (r.status !== 200) return r.status;
      const b = await r.blob();
      if (!b.size || b.size > MAX_FILE) return -1;
      try {
        const c = await caches.open(cacheName || STORE);
        await c.put(key(h), new Response(b, { headers: { 'Content-Type': 'audio/mpeg', 'X-Groesse': String(b.size) } }));
      } catch (e) { if (!cacheName) mem.set(h, b); else return -1; }
      return 200;
    } catch (e) { return -1; } finally { done(); }
  }
  nova.load = (url, h) => store(url, h);

  /** Früher: Laufzeit-Text über POST /jetzt vertonen lassen. Der Dienst ist abgeschaltet (20.11.2026) – nie mehr anfragen.
   *  404 = „gibt es nicht“: Die App liest dann still mit der Gerätestimme, ohne Fehlermeldung. */
  nova.now = async () => 404;

  // Autoplay-Sperre: Vor der ersten Berührung lässt der Browser nichts abspielen. Dann springt NICHT die Gerätestimme ein –
  // die Nova-Datei wartet und startet beim ersten Antippen (innerhalb der Berührung, damit der Browser es erlaubt).
  let pendingStart = null;
  const onGesture = () => { const p = pendingStart; pendingStart = null; if (p) p(); };
  ['pointerdown', 'pointerup', 'touchend', 'keydown', 'click'].forEach((ev) =>
    document.addEventListener(ev, onGesture, { capture: true, passive: true }));
  nova.wartetAufTippen = () => pendingStart !== null;

  let turn = 0, finish = null;
  /** Gespeicherte Datei abspielen – Tempo ohne Tonhöhen-Änderung. true = zu Ende gespielt (oder angehalten), false = ging nicht */
  nova.play = (h, rate) => new Promise((ok) => {
    nova.stop();
    const my = ++turn;
    (async () => {
      let blob = mem.get(h);
      if (!blob) { try { const r = await find(h); if (r) blob = await r.blob(); } catch (e) {} }
      if (my !== turn) { ok(true); return; }
      if (!blob) { ok(false); return; }
      const url = URL.createObjectURL(blob);
      const a = new Audio();
      let done = false;
      const start = () => {
        if (done) return;
        const p = a.play();
        if (p && p.catch) p.catch((e) => {
          if (done) return;
          if (e && e.name === 'NotAllowedError') pendingStart = start;   // auf das erste Antippen warten
          else fin(false);
        });
      };
      const fin = (v) => {
        if (done) return; done = true;
        if (finish === fin) finish = null;
        if (pendingStart === start) pendingStart = null;
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
      start();
    })();
  });
  nova.stop = () => { turn++; if (finish) finish(true); };

  // ---------------------------------------------------------------- Alle Nova-Texte offline laden
  const hashOf = (url) => { const m = /([0-9a-f]{32})\.mp3$/.exec(url); return m ? m[1] : null; };
  /** Namen (h) im Offline-Speicher der Qualität q ('STANDARD' | 'BEST') – als JSON-Liste */
  nova.offlineList = async (q) => {
    try {
      if (!(await caches.has(OFFLINE[q]))) return '[]';
      const ks = await (await caches.open(OFFLINE[q])).keys(); return JSON.stringify(ks.map((r) => hashOf(r.url)).filter(Boolean));
    }
    catch (e) { return '[]'; }
  };
  /** Datei dauerhaft sichern: Standard aus dem Zwischenspeicher übernehmen, sonst laden. HTTP-Status oder -1 */
  nova.secure = async (url, h, q) => {
    try {
      const c = await caches.open(OFFLINE[q]);
      if (await c.match(key(h))) return 200;
      if (q === 'STANDARD') {
        const auto = await caches.open(STORE); const r = await auto.match(key(h));
        if (r) {
          const b = await r.blob();
          await c.put(key(h), new Response(b, { headers: { 'Content-Type': 'audio/mpeg', 'X-Groesse': String(b.size) } }));
          await auto.delete(key(h));
          return 200;
        }
      }
    } catch (e) { return -1; }
    return store(url, h, OFFLINE[q]);
  };
  /** Kleine Textdatei (Liste, Server-Stand) – null bei Fehler */
  nova.text = async (url) => {
    if (offline()) return null;
    const [signal, done] = timed(20000);
    try { const r = await fetch(url, Object.assign({}, OPTS, { signal })); return r.ok ? await r.text() : null; }
    catch (e) { return null; } finally { done(); }
  };
  /** Belegter Platz aller Nova-Dateien in Bytes */
  nova.used = async () => {
    let sum = 0;
    for (const name of ALL) {
      try {
        if (!(await caches.has(name))) continue;
        const c = await caches.open(name);
        for (const req of await c.keys()) {
          const r = await c.match(req); if (!r) continue;
          const g = Number(r.headers.get('X-Groesse'));
          sum += g > 0 ? g : (await r.blob()).size;
        }
      } catch (e) {}
    }
    return sum;
  };
  /** Alle Nova-Dateien löschen (beide Offline-Fassungen und Zwischenspeicher) */
  nova.clear = async () => { mem.clear(); for (const name of ALL) { try { await caches.delete(name); } catch (e) {} } return true; };
})();
