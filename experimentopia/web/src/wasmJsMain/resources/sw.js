// Service Worker: macht Experimentopia offline nutzbar und hält es aktuell. Jede Version hat ihren eigenen Speicher: Ändert sich diese Datei
// (neue Versionsnummer), lädt der Browser alles frisch, räumt den alten Speicher weg und übernimmt sofort – die Seite lädt sich dann selbst neu.
const CACHE = 'experimentopia-@VERSION@';
// Der App-Kern (.wasm): Der Dateiname ändert sich mit dem Inhalt, darum darf er aus dem Zwischenspeicher des Browsers kommen.
const WASM = @WASM@;
self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => Promise.all([c.addAll(WASM), c.addAll(['./', 'index.html', 'experimentopia.js', 'manifest.webmanifest', 'icon-192.png', 'version.json', 'nova.js', 'umzug.js', 'fonts/dejavu_sans.ttf', 'fonts/noto_color_emoji.ttf'].map((u) => new Request(u, { cache: 'reload' })))]))); });
// Aufräumen nur mit der Vorsilbe „experimentopia-“: Auf der gemeinsamen Adresse liegen auch die Speicher der anderen Lern-Apps
self.addEventListener('activate', (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.startsWith('experimentopia-') && k !== CACHE && k !== 'experimentopia-nova').map((k) => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;
  if (u.pathname.endsWith('version.json') || u.pathname.endsWith('diag.html') || u.searchParams.has('nc')) return;   // Versionsabgleich immer frisch aus dem Netz, nie gespeichert       // Fremdes (Impressum, Datenschutz) geht normal ins Netz
  // nur in den eigenen Speichern suchen (gemeinsame Adresse: nie Dateien einer anderen Lern-App ausliefern)
  e.respondWith(caches.open(CACHE).then((c) => c.match(e.request)).then((hit) => hit || caches.open('experimentopia-nova').then((c) => c.match(e.request))).then((hit) => hit || fetch(e.request).then((r) => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then((c) => c.put(e.request, copy)); } return r; })));
});
