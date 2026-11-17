// Kurvenflug 2.4.1 – Service Worker (automatisch erzeugt). Macht die WebApp offline spielbar.
const CACHE = "kurvenflug-2.4.1";
const FILES = [
  "./",
  "0629b2ab1fe972a98e2b.wasm",
  "bccfa839aa4b38489c76.wasm",
  "composeResources/de.kurvenflug.web.resources/files/emoji.ttf",
  "composeResources/de.kurvenflug.web.resources/files/symbols.ttf",
  "fonts-lizenzen.txt",
  "icons/apple-touch-icon.png",
  "icons/favicon-32.png",
  "icons/icon-192.png",
  "icons/icon-512.png",
  "icons/icon.svg",
  "icons/maskable-512.png",
  "index.html",
  "kf.js",
  "kurvenflug.js",
  "kurvenflug.js.LICENSE.txt",
  "manifest.webmanifest"
];
self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES.map(f => new Request(f, { cache: "no-cache" })))).then(() => self.skipWaiting()));
});
self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith("kurvenflug-") && k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", e => {
  if (e.request.method !== "GET" || new URL(e.request.url).origin !== location.origin) return;
  e.respondWith(caches.match(e.request, { ignoreSearch: true }).then(r => r || fetch(e.request)));
});
