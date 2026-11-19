// Offline-Speicher der WebApp. Die Versionsnummer wechselt mit jeder Auslieferung (prüft tools/test-inhalt.mjs).
const VERSION = '0.4.4';
const CACHE = 'quasselquest-' + VERSION;
const FILES = ['./', 'index.html', 'app.css', 'version.js', 'content.js', 'pakete.js', 'audio.js', 'app.js', 'datenschutz.html', 'datenblatt-schule.html',
  'manifest.webmanifest', 'audio/index.json', 'pakete/index.json', 'lernpfad/en.json', 'lernpfad/fr.json', 'lernpfad/es.json', 'lizenzen.txt', 'icons/icon.svg', 'icons/icon-180.png', 'icons/icon-192.png', 'icons/icon-512.png'];

self.addEventListener('install', (e) => {
  // Programmdateien und alle vorab vertonten Sätze (audio/index.json) offline speichern
  e.waitUntil(caches.open(CACHE).then(async (c) => {
    await c.addAll(FILES.map((f) => new Request(f, { cache: 'reload' })));
    const idx = await (await c.match('audio/index.json')).json();
    const mp3 = new Set();
    // Grundausstattung: nur Englisch (Hafen-Welt) sofort offline – andere Sprachen landen beim ersten Abspielen im Speicher
    // (Marcel 19.11.: Download-Knopf je Sprache folgt). So bleibt der erste Besuch klein.
    for (const e of Object.values(idx.saetze || {})) if (String(e.stimme || '').startsWith('en_')) { mp3.add('audio/piper/' + e.n); mp3.add('audio/piper/' + e.s); }
    const pk = await (await c.match('pakete/index.json')).json();
    await c.addAll((pk.pakete || []).map((p) => 'pakete/' + p.id + '.json'));
    await c.addAll([...mp3]);
  }));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.startsWith('quasselquest-') && k !== CACHE).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});
// Die App sagt Bescheid, wenn gerade keine Runde läuft – erst dann wird die neue Version aktiv.
self.addEventListener('message', (e) => { if (e.data === 'skipWaiting') self.skipWaiting(); });
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== self.location.origin) return;
  // Nur im eigenen Speicher suchen (caches.open(CACHE).match): Unter lernen.… liegen alle Apps auf derselben Adresse –
  // caches.match würde auch Dateien anderer Apps finden (z. B. deren index.html).
  if (req.url.endsWith('version.json')) { e.respondWith(fetch(req, { cache: 'no-store' }).catch(() => caches.open(CACHE).then((c) => c.match(req)))); return; }
  e.respondWith(caches.open(CACHE).then((c) => c.match(req, { ignoreSearch: req.mode === 'navigate' }).then((hit) => hit || fetch(req).then((res) => {
    // Vertonte Sätze, Pakete und Lernpfade beim ersten Laden merken – danach offline verfügbar
    if (res.ok && /\/(audio\/piper|pakete|lernpfad)\//.test(new URL(req.url).pathname)) c.put(req, res.clone()).catch(() => {});
    return res;
  }).catch(() => (req.mode === 'navigate' ? c.match('index.html') : Response.error())))));
});
