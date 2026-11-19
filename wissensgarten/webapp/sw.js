// Service Worker: WebApp offline nutzbar. Bei neuer Version (2.3.0) wird der Zwischenspeicher erneuert.
const CACHE = 'wissensgarten-2.3.0';
self.addEventListener('install', (e) => {
  // Alles vorab speichern (Liste wird beim Bauen eingesetzt) – beim ersten Besuch kommt das meist schon aus dem Browser-Cache.
  // Einzeln, damit eine fehlende Datei nicht die ganze Installation verhindert.
  const files = ['./','composeResources/de.wissensgarten.web.res/font/dejavu_symbols.ttf','composeResources/de.wissensgarten.web.res/font/noto_color_emoji.ttf','icons/icon-192.png','icons/icon-512.png','icons/icon-maskable-512.png','index.html','js-reexport-symbols.mjs','lizenzen.txt','manifest.webmanifest','packs/aw-allgemeinwissen-mix.json','packs/aw-erste-hilfe.json','packs/aw-musik-kunst.json','packs/aw-redewendungen.json','packs/aw-weltall.json','packs/gs-einmaleins.json','packs/gs-englisch-basics.json','packs/gs-englisch-familie-koerper.json','packs/gs-englisch-farben-zahlen.json','packs/gs-formen-geometrie.json','packs/gs-geld.json','packs/gs-gesund-ernaehrung.json','packs/gs-koerper-natur.json','packs/gs-lesestart.json','packs/gs-mathe-groessen.json','packs/gs-pflanzen-wald.json','packs/gs-plus-minus-100.json','packs/gs-plus-minus-20.json','packs/gs-rechtschreibung.json','packs/gs-ritter-burgen.json','packs/gs-satzzeichen-satzarten.json','packs/gs-schriftlich-rechnen.json','packs/gs-tiere.json','packs/gs-uhrzeit.json','packs/gs-verkehr-deutschland.json','packs/gs-wetter-jahreszeiten.json','packs/gs-wortarten.json','packs/gs-zahlenraum-1000.json','packs/index.json','packs/s1-bio-oekologie.json','packs/s1-bio-organe.json','packs/s1-bio-pflanzen.json','packs/s1-bio-wirbeltiere.json','packs/s1-bio-zelle-mensch.json','packs/s1-chemie-elemente.json','packs/s1-chemie-grundbegriffe.json','packs/s1-chemie-saeuren-basen.json','packs/s1-deutsch-fremdwoerter.json','packs/s1-deutsch-grammatik.json','packs/s1-deutsch-stilmittel.json','packs/s1-deutsch-zeichensetzung.json','packs/s1-englisch-alltag.json','packs/s1-englisch-grundwortschatz.json','packs/s1-englisch-irregular-verbs.json','packs/s1-englisch-tenses.json','packs/s1-erdkunde-bundeslaender.json','packs/s1-erdkunde-europa.json','packs/s1-erdkunde-gebirge-fluesse.json','packs/s1-erdkunde-klima.json','packs/s1-erdkunde-welt.json','packs/s1-ethik-weltreligionen.json','packs/s1-franzoesisch-grundwortschatz.json','packs/s1-franzoesisch-verben.json','packs/s1-geschichte-20-jahrhundert.json','packs/s1-geschichte-antike-mittelalter.json','packs/s1-geschichte-ddr-einheit.json','packs/s1-geschichte-neuzeit.json','packs/s1-geschichte-steinzeit-aegypten.json','packs/s1-informatik-grundlagen.json','packs/s1-latein-grundwortschatz.json','packs/s1-mathe-algebra.json','packs/s1-mathe-brueche-prozent.json','packs/s1-mathe-geometrie-formeln.json','packs/s1-mathe-gleichungen.json','packs/s1-mathe-kopfrechnen.json','packs/s1-mathe-negative-zahlen.json','packs/s1-mathe-pythagoras-trigo.json','packs/s1-mathe-wahrscheinlichkeit.json','packs/s1-physik-einheiten-formeln.json','packs/s1-physik-strom.json','packs/s1-politik-grundgesetz.json','packs/s1-spanisch-grundwortschatz.json','packs/s1-wirtschaft-grundbegriffe.json','packs/versions.json','plus_katalog.json','skiko.mjs','skikod8.mjs','wg-platform.js','wg-stimme.js'];
  e.waitUntil(caches.open(CACHE).then((c) => Promise.all(files.map((f) => c.add(f).catch(() => {})))).then(() => self.skipWaiting()));
});
self.addEventListener('activate', (e) => {
  // Gespeicherte Nova-Sätze (wissensgarten-nova, -offline, -original) bleiben bei Updates stehen; alles andere (auch die frühere Offline-Stimme) wird gelöscht
  // Gemeinsame Adresse aller Lern-Apps: NUR eigene Caches („wissensgarten-…“) anfassen, nie die der anderen Apps
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k.startsWith('wissensgarten-') && k !== CACHE && !k.startsWith('wissensgarten-nova')).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});
// Erst Zwischenspeicher, sonst Netz (und dann merken) – so läuft alles auch ohne Internet weiter
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== location.origin) return;
  // Plus-Endpunkt nie zwischenspeichern: Liste muss frisch sein, Plus-Inhalte gehören nicht in den Cache
  if (new URL(req.url).pathname.includes('/wg-api/')) return;
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
