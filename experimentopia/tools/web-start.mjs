// Browser-Test „nie eine weiße Seite“ (WebKit wie Safari auf dem iPhone, sonst Chromium):
//   node tools/web-start.mjs            (nach ./gradlew :web:webapp; WebKit braucht ggf. PLAYWRIGHT_BROWSERS_PATH mit webkit)
// Prüft: Safari/WebKit startet Wasm (Abhilfe gegen WebKit-Fehler 329421) · altes iOS ohne WasmGC startet (JS) · fehlende Datei → „Neu laden“ ·
//        Chromium: Wasm-Fassung, ?modus=js geht, gescheitertes Wasm und weiße Fläche (Safari-27.2-Fehler) wechseln automatisch auf JS · Pixeldichte · Grafik-Verlust.
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
  async function modus(q, opts, init, route, wait) {
    const ctx = await b.newContext({ ...(engine === webkit ? devices['iPhone 13'] : {}), ...opts }); const p = await ctx.newPage();
    if (init) await p.addInitScript(init); if (route) await p.route(route, (r) => r.abort());
    await p.goto('http://localhost:8792/' + q); await p.waitForTimeout(wait);
    const r = await p.evaluate(() => ({ modus: window.__exModus, fertig: document.getElementById('start').dataset.fertig === '1', problem: document.getElementById('problem') ? document.getElementById('problem').innerText : null, addr: location.search + location.hash }));
    await ctx.close(); return r;
  }
  let r = await modus('', {}, null, null, 14000);
  ok(r.fertig && !r.problem && r.modus === 'wasm', `${name}: Start ohne Fehlermeldung, Fassung ${r.modus} (Wasm mit Safari-Abhilfe)`);
  // Freischalt-Link: neu „#pro=…“, alt „?pro=…“ – App startet, Code verschwindet sofort aus der Adresszeile
  for (const q of ['#pro=EX-TEST-1234', '?pro=EX-TEST-1234', '#pro=%E0%A4%A']) {
    const a = await modus(q, {}, null, null, 14000);
    ok(a.fertig && !a.problem && a.addr === '', `Freischalt-Link ${q}: startet, Adresse danach „${a.addr}“`);
  }
  r = await modus('', { userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1' },
    () => { const v = WebAssembly.validate; WebAssembly.validate = (x) => x.length === 15 ? false : v(x); }, null, 14000);
  ok(r.fertig && !r.problem && r.modus === 'js', `altes iOS ohne WasmGC: startet mit der JS-Fassung (${r.modus})`);
  r = await modus('', {}, null, '**/*.wasm', 9000);
  ok(!!r.problem && r.problem.includes('Neu laden') && r.problem.includes('Version'), 'Datei fehlt: Meldung mit Version und „Neu laden“ statt weißer Seite');
  if (engine !== webkit) {
    r = await modus('?modus=js', {}, null, null, 14000);
    ok(r.fertig && !r.problem && r.modus === 'js', 'Chromium mit ?modus=js: JS-Fassung startet');
    // Wasm-Hauptmodul kaputt (wie in Safari 27) → automatisch JS, für diese Version gemerkt
    const sw = await (await fetch('http://localhost:8792/sw.js')).text(), jsb = await (await fetch('http://localhost:8792/experimentopia-js.js')).text();
    const appWasm = sw.match(/'[0-9a-f]+\.wasm'/g).map((x) => x.replace(/'/g, '')).find((n) => !jsb.includes(n));
    const ctx = await b.newContext(); const p = await ctx.newPage();
    await p.route('**/' + appWasm, (rt) => rt.abort());
    await p.goto('http://localhost:8792/'); await p.waitForTimeout(20000);
    const f = await p.evaluate(() => ({ modus: window.__exModus, fertig: document.getElementById('start').dataset.fertig === '1', problem: !!document.getElementById('problem'), gemerkt: Object.keys(localStorage).filter((k) => k.startsWith('experimentopia.modus')).map((k) => localStorage.getItem(k)) }));
    ok(f.fertig && !f.problem && f.modus === 'js' && f.gemerkt.includes('js'), `Wasm gescheitert → automatisch JS und gemerkt (${JSON.stringify(f)})`);
    await ctx.close();
    // Safari 27.2 zeichnet nichts (weiße Fläche): als Safari ausgeben und weiße Pixel vortäuschen → automatisch JS
    const c2 = await b.newContext({ userAgent: 'Mozilla/5.0 (iPad; CPU OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/27.2 Mobile/15E148 Safari/604.1' });
    const p2 = await c2.newPage();
    await p2.addInitScript(() => { for (const C of [WebGLRenderingContext, WebGL2RenderingContext]) { const r = C.prototype.readPixels; C.prototype.readPixels = function (x, y, w, h, f, t, px) { if (!Object.keys(localStorage).some((k) => k.startsWith('experimentopia.modus')) && px && px.length >= 4) { px[0] = px[1] = px[2] = px[3] = 255; return; } return r.apply(this, arguments); }; } });
    await p2.goto('http://localhost:8792/'); await p2.waitForTimeout(25000);
    const w = await p2.evaluate(() => ({ modus: window.__exModus, fertig: document.getElementById('start').dataset.fertig === '1', problem: !!document.getElementById('problem'), gemerkt: Object.keys(localStorage).filter((k) => k.startsWith('experimentopia.modus')).map((k) => localStorage.getItem(k)) }));
    ok(w.fertig && !w.problem && w.modus === 'js' && w.gemerkt.includes('js'), `weiße Fläche auf Safari → automatisch JS (${JSON.stringify(w)})`);
    await c2.close();
  }
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
