// Service Worker: WebApp offline nutzbar. Bei neuer Version (@VERSION@) wird der Zwischenspeicher erneuert.
const CACHE = 'wissensgarten-@VERSION@';
self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(['./', 'index.html', 'wg-platform.js', 'wissensgarten.js', 'manifest.webmanifest', 'packs/index.json'])).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
// Erst Zwischenspeicher, sonst Netz (und dann merken) – so läuft alles auch ohne Internet weiter
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== location.origin) return;
  e.respondWith(caches.open(CACHE).then((c) => c.match(req).then((hit) => hit || fetch(req).then((res) => {
    if (res.ok) c.put(req, res.clone());
    return res;
  }).catch(() => (req.mode === 'navigate' ? c.match('index.html') : Response.error())))));
});
