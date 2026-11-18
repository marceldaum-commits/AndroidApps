// Offline-Speicher der WebApp. Die Versionsnummer wechselt mit jeder Auslieferung (prüft tools/test-inhalt.mjs).
const VERSION = '0.1.0';
const CACHE = 'englisch-' + VERSION;
const FILES = ['./', 'index.html', 'app.css', 'version.js', 'content.js', 'audio.js', 'app.js', 'datenschutz.html',
  'manifest.webmanifest', 'icons/icon.svg', 'icons/icon-180.png', 'icons/icon-192.png', 'icons/icon-512.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(FILES.map((f) => new Request(f, { cache: 'reload' })))));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.startsWith('englisch-') && k !== CACHE).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});
// Die App sagt Bescheid, wenn gerade keine Runde läuft – erst dann wird die neue Version aktiv.
self.addEventListener('message', (e) => { if (e.data === 'skipWaiting') self.skipWaiting(); });
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== self.location.origin) return;
  if (req.url.endsWith('version.json')) { e.respondWith(fetch(req, { cache: 'no-store' }).catch(() => caches.match(req))); return; }
  e.respondWith(caches.match(req, { ignoreSearch: req.mode === 'navigate' }).then((hit) => hit || fetch(req).catch(() =>
    req.mode === 'navigate' ? caches.match('index.html') : Response.error())));
});
