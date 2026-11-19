// Service Worker: macht Raumlicht offline nutzbar und hält es aktuell. Jede Version hat ihren eigenen Speicher: Ändert sich diese Datei
// (neue Versionsnummer), lädt der Browser alles frisch, räumt den alten Speicher weg und übernimmt sofort – die Seite lädt sich dann selbst neu.
const CACHE = 'raumlicht-2.18.38';
// Der App-Kern (.wasm): Der Dateiname ändert sich mit dem Inhalt, darum darf er aus dem Zwischenspeicher des Browsers kommen –
// die Seite hat ihn gerade erst geladen, so wird er beim ersten Besuch nicht doppelt übertragen. Danach startet die App auch ohne Netz.
const WASM = ['a621341e12e6bc7fd2f5.wasm', 'bccfa839aa4b38489c76.wasm'];
// Alles andere beim Installieren am Zwischenspeicher des Browsers vorbei laden ('reload'): wirklich die neue Fassung
self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => Promise.all([c.addAll(WASM), c.addAll(['./', 'index.html', 'raumlicht.js', 'scene/index.html', 'scene/scene.js', 'rl-nova.js', 'familie.js', 'manifest.webmanifest', 'icon-192.png', 'version.json', 'fonts/dejavu_sans.ttf', 'fonts/noto_color_emoji.ttf', 'diagnose.html'].map((u) => new Request(u, { cache: 'reload' })))]))); });
// Schon gehörte und offline geladene Sätze der Stimme „Nova“ (raumlicht-nova, raumlicht-nova-offline) bleiben bei Updates stehen.
// Aufgeräumt werden NUR eigene Speicher (Präfix „raumlicht-“): Unter der gemeinsamen Adresse liegen daneben die Speicher der anderen Lern-Apps.
self.addEventListener('activate', (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k.startsWith('raumlicht-') && k !== CACHE && k !== 'raumlicht-nova' && k !== 'raumlicht-nova-offline').map((k) => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;       // Fremdes (Impressum, Datenschutz) geht normal ins Netz
  // erst Speicher, sonst Netz – und was aus dem Netz kommt, wird für später behalten (z. B. die .wasm-Dateien mit wechselnden Namen)
  // nur im eigenen Speicher suchen – nie in dem einer anderen App unter derselben Adresse; und nur Anfragen aus dem eigenen Ordner
  if (!u.href.startsWith(self.registration.scope)) return;
  e.respondWith(caches.open(CACHE).then((c) => c.match(e.request).then((hit) => hit || fetch(e.request).then((r) => { if (r.ok) c.put(e.request, r.clone()); return r; }))));
});
