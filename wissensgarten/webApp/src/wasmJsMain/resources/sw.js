// Service Worker: WebApp offline nutzbar. Bei neuer Version (@VERSION@) wird der Zwischenspeicher erneuert.
const CACHE = 'wissensgarten-@VERSION@';
self.addEventListener('install', (e) => {
  // Alles vorab speichern (Liste wird beim Bauen eingesetzt) – beim ersten Besuch kommt das meist schon aus dem Browser-Cache.
  // Einzeln, damit eine fehlende Datei nicht die ganze Installation verhindert.
  const files = '@PRECACHE@';
  e.waitUntil(caches.open(CACHE).then((c) => Promise.all(files.map((f) => c.add(f).catch(() => {})))).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
// Erst Zwischenspeicher, sonst Netz (und dann merken) – so läuft alles auch ohne Internet weiter
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== location.origin) return;
  // Seitenaufruf: erst Netz (frische Version), ohne Internet aus dem Zwischenspeicher
  if (req.mode === 'navigate') {
    e.respondWith(fetch(req).then((res) => { if (res.ok) caches.open(CACHE).then((c) => c.put('index.html', res.clone())); return res; })
      .catch(() => caches.open(CACHE).then((c) => c.match('index.html'))));
    return;
  }
  e.respondWith(caches.open(CACHE).then((c) => c.match(req).then((hit) => hit || fetch(req).then((res) => {
    if (res.ok) c.put(req, res.clone());
    return res;
  }).catch(() => (req.mode === 'navigate' ? c.match('index.html') : Response.error())))));
});
