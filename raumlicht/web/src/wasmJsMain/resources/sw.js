// Service Worker: macht Raumlicht offline nutzbar. Jede Version hat ihren eigenen Speicher – nach einem Update wird alles frisch geladen.
const CACHE = 'raumlicht-@VERSION@';
self.addEventListener('install', (e) => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then((c) => c.addAll(['./', 'index.html', 'raumlicht.js', 'scene/index.html', 'scene/scene.js', 'manifest.webmanifest', 'icon-192.png', 'version.json', 'fonts/dejavu_sans.ttf', 'fonts/dejavu_sans_bold.ttf']))); });
self.addEventListener('activate', (e) => { e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', (e) => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;       // Fremdes (Impressum, Datenschutz) geht normal ins Netz
  // erst Speicher, sonst Netz – und was aus dem Netz kommt, wird für später behalten (z. B. die .wasm-Dateien mit wechselnden Namen)
  e.respondWith(caches.match(e.request).then((hit) => hit || fetch(e.request).then((r) => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then((c) => c.put(e.request, copy)); } return r; })));
});
