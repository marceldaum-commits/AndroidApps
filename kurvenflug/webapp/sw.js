// Kurvenflug 2.5.13 – Service Worker (automatisch erzeugt). Macht die WebApp offline spielbar.
const CACHE = "kurvenflug-2.5.13";
const SHARED = [
  "./",
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
  "kurvenflug.js.LICENSE.txt",
  "manifest.webmanifest"
];
const ONLY = {
  wasm: ["76e423315854966bfa64.wasm",
  "kurvenflug.js"],
  js: ["kurvenflug-compat.js"]
};
const MODE = new URL(location).searchParams.get("fassung") === "js" ? "js" : "wasm";
const FILES = SHARED.concat(ONLY[MODE]);
self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES.map(f => new Request(f, { cache: "no-cache" })))).then(() => self.skipWaiting()));
});
self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith("kurvenflug-") && k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", e => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== location.origin) return;
  // version.json immer frisch vom Server (Update-Prüfung); ohne Netz die gespeicherte
  if (url.pathname.endsWith("/version.json")) { e.respondWith(fetch(e.request).catch(() => caches.match(e.request, { ignoreSearch: true }))); return; }
  e.respondWith(caches.match(e.request, { ignoreSearch: true }).then(r => r || fetch(e.request).then(res => {
    // Dateien der anderen Fassung (Wechsel per Knopf) beim ersten Laden ebenfalls offline merken
    const p = url.pathname.split("/").pop();
    if (res.ok && (ONLY.wasm.includes(p) || ONLY.js.includes(p))) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); }
    return res;
  })));
});
