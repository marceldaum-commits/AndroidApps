// Service Worker: macht Experimentopia offline nutzbar und hält es aktuell. Jede Version hat ihren eigenen Speicher: Ändert sich diese Datei
// (neue Versionsnummer), lädt der Browser alles frisch, räumt den alten Speicher weg und übernimmt sofort – die Seite lädt sich dann selbst neu.
const CACHE = 'experimentopia-2.3.3';
// Der App-Kern (.wasm): Der Dateiname ändert sich mit dem Inhalt, darum darf er aus dem Zwischenspeicher des Browsers kommen.
const WASM = ['96a548a1a76e9f228fc2.wasm', 'bccfa839aa4b38489c76.wasm'];
self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => Promise.all([c.addAll(WASM), c.addAll(['./', 'index.html', 'experimentopia.js', 'manifest.webmanifest', 'icon-192.png', 'version.json', 'nova.js', 'fonts/dejavu_sans.ttf', 'fonts/noto_color_emoji.ttf'].map((u) => new Request(u, { cache: 'reload' })))]))); });
self.addEventListener('activate', (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE && k !== 'experimentopia-nova').map((k) => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;       // Fremdes (Impressum, Datenschutz) geht normal ins Netz
  e.respondWith(caches.match(e.request).then((hit) => hit || fetch(e.request).then((r) => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then((c) => c.put(e.request, copy)); } return r; })));
});
