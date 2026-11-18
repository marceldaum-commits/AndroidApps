// Browser-Test „nie eine weiße Seite“ (WebKit wie Safari auf dem iPhone, sonst Chromium):
//   node tools/web-start.mjs            (nach ./gradlew :web:webapp; WebKit braucht ggf. PLAYWRIGHT_BROWSERS_PATH mit webkit)
// Prüft: normaler Start ohne Fehlermeldung · altes iOS ohne WasmGC → klare Meldung „mindestens iOS 18.2“ · fehlende Datei → „Neu laden“.
import { createRequire } from 'module'; import { spawn } from 'child_process'; import path from 'path';
const require = createRequire(import.meta.url);
const { webkit, chromium, devices } = require('playwright');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', 'webapp');
const server = spawn('python3', ['-m', 'http.server', '8792', '--bind', '127.0.0.1'], { cwd: root, stdio: 'ignore' });
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
try {
  await new Promise((r) => setTimeout(r, 800));
  let engine = webkit, name = 'WebKit';
  try { (await webkit.launch()).close(); } catch (e) { engine = chromium; name = 'Chromium (WebKit fehlt)'; }
  const b = await engine.launch();
  async function run(opts, init, route, wait) {
    const ctx = await b.newContext({ ...(engine === webkit ? devices['iPhone 13'] : {}), ...opts }); const p = await ctx.newPage();
    if (init) await p.addInitScript(init); if (route) await p.route(route, (r) => r.abort());
    await p.goto('http://localhost:8792/'); await p.waitForTimeout(wait);
    const r = await p.evaluate(() => document.getElementById('problem') ? document.getElementById('problem').innerText : null); await ctx.close(); return r;
  }
  ok((await run({}, null, null, 14000)) === null, `${name}: normaler Start ohne Fehlermeldung`);
  const alt = await run({ userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1' },
    () => { const v = WebAssembly.validate; WebAssembly.validate = (x) => x.length === 15 ? false : v(x); }, null, 6000);
  ok(!!alt && alt.includes('iOS 18.2') && alt.includes('Neu laden'), 'altes iOS (ohne WasmGC): Meldung „mindestens iOS 18.2“ + „Neu laden“');
  const fehlt = await run({}, null, '**/*.wasm', 8000);
  ok(!!fehlt && fehlt.includes('Neu laden') && fehlt.includes('Version'), 'Datei fehlt: Meldung mit Version und „Neu laden“ statt weißer Seite');
  // Pixeldichte auf 2 begrenzt (iPhone hat 3 → weniger Grafikspeicher) und Grafik-Verlust wird angezeigt
  const ctx = await b.newContext({ viewport: { width: 393, height: 852 }, deviceScaleFactor: 3 }); const p = await ctx.newPage();
  await p.goto('http://localhost:8792/'); await p.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 90000 }); await p.waitForTimeout(6000);
  const cv = await p.evaluate(() => { const c = document.getElementById('app').shadowRoot.querySelector('canvas'); return [devicePixelRatio, c.width]; });
  ok(cv[0] === 2 && cv[1] <= 393 * 2, `Pixeldichte begrenzt: ${cv[0]}, Zeichenfläche ${cv[1]} px breit`);
  await p.evaluate(() => { const c = document.getElementById('app').shadowRoot.querySelector('canvas'); const gl = c.getContext('webgl2') || c.getContext('webgl'); const x = gl && gl.getExtension('WEBGL_lose_context'); if (x) x.loseContext(); else c.dispatchEvent(new Event('webglcontextlost', { cancelable: true })); });
  await p.waitForTimeout(1500);
  ok(await p.evaluate(() => !!document.getElementById('problem') && document.getElementById('problem').innerText.includes('Neu laden')), 'Grafik-Verlust: Meldung mit „Neu laden“');
  await b.close();
} finally { server.kill(); }
