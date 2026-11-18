// Safari 27.2 (WebKit-Fehler 329421): Aufrufe der Grafik-Bibliothek aus der Endaufruf-Stellung nehmen (siehe webApp/webpack/skiko-no-tail-call.cjs,
// übernommen aus Raumlicht 2.18.19). Gilt für die Wasm-Fassung (skiko.mjs); die JS-Fassung hat dieses Muster nicht.
(function () {
  const path = require('path'), fs = require('fs');
  let dir = __dirname, loader = null;
  for (let i = 0; i < 8 && !loader; i++) {
    const p = path.join(dir, 'webApp', 'webpack', 'skiko-no-tail-call.cjs');
    if (fs.existsSync(p)) loader = p;
    dir = path.dirname(dir);
  }
  if (!loader) throw new Error('skiko-no-tail-call.cjs nicht gefunden');
  config.module = config.module || {}; config.module.rules = config.module.rules || [];
  config.module.rules.push({ test: /skiko\.mjs$/, enforce: 'pre', use: [{ loader }] });
})();
