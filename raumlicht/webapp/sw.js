// Service Worker: macht Raumlicht offline nutzbar und hält es aktuell. Jede Version hat ihren eigenen Speicher: Ändert sich diese Datei
// (neue Versionsnummer), lädt der Browser alles frisch, räumt den alten Speicher weg und übernimmt sofort – die Seite lädt sich dann selbst neu.
const CACHE = 'raumlicht-2.17.2';
// Der App-Kern (.wasm): Der Dateiname ändert sich mit dem Inhalt, darum darf er aus dem Zwischenspeicher des Browsers kommen –
// die Seite hat ihn gerade erst geladen, so wird er beim ersten Besuch nicht doppelt übertragen. Danach startet die App auch ohne Netz.
const WASM = ['64721b48df661e0e4775.wasm', 'bccfa839aa4b38489c76.wasm'];
// Alles andere beim Installieren am Zwischenspeicher des Browsers vorbei laden ('reload'): wirklich die neue Fassung
self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => Promise.all([c.addAll(WASM), c.addAll(['./', 'index.html', 'raumlicht.js', 'scene/index.html', 'scene/scene.js', 'rl-nova.js', 'manifest.webmanifest', 'icon-192.png', 'version.json', 'fonts/dejavu_sans.ttf', 'fonts/noto_color_emoji.ttf', 'diagnose.html'].map((u) => new Request(u, { cache: 'reload' })))]))); });
// Schon gehörte Sätze der Stimme „Nova“ (raumlicht-nova) bleiben bei Updates stehen
self.addEventListener('activate', (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE && k !== 'raumlicht-nova').map((k) => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;       // Fremdes (Impressum, Datenschutz) geht normal ins Netz
  // erst Speicher, sonst Netz – und was aus dem Netz kommt, wird für später behalten (z. B. die .wasm-Dateien mit wechselnden Namen)
  e.respondWith(caches.match(e.request).then((hit) => hit || fetch(e.request).then((r) => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then((c) => c.put(e.request, copy)); } return r; })));
});
