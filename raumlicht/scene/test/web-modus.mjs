// Zwei Fassungen der WebApp: „wasm“ (Vorgabe) und „js“ (Safari/WebKit, Browser ohne WebAssembly-GC, oder nach einem Startfehler der Wasm-Fassung – gemerkt je Version).
// node test/web-modus.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium, devices } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
let asked = [], breakKern = false;
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p); asked.push(p);
  // „Wasm-Hauptmodul kaputt“: der Kotlin-Kern (nicht die Grafik-Datei) kommt als Unsinn an
  if (breakKern && /\.wasm$/.test(p) && !fs.readFileSync(path.join(dir, 'raumlicht-js.js'), 'utf8').includes(p.slice(1))) { r.writeHead(200, { 'Content-Type': 'application/wasm' }); return r.end(Buffer.from('kein wasm')); }
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const started = (page) => page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 });
const run = async (name, q, opts = {}) => { asked = []; const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block', ...opts }); const page = await ctx.newPage();
  const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await page.goto(url + q); await started(page); await page.waitForTimeout(3500); const shot = await page.screenshot({ path: `${out}/${name}.png` });
  const r = { modus: await page.evaluate(() => window.__rlModus), js: asked.includes('/raumlicht-js.js'), wasmJs: asked.includes('/raumlicht.js'), kerne: asked.filter((p) => p.endsWith('.wasm')).length, errs, shot, page, ctx,
    log: await page.evaluate(() => localStorage.getItem('raumlicht.mitschrift') || '') }; return r; };
const bunt = (page) => page.evaluate(async (png) => { const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
  const d = g.getImageData(0, 0, bmp.width, bmp.height).data; const seen = new Set(); let white = 0; for (let i = 0; i < d.length; i += 4 * 37) { seen.add((d[i] >> 4) + ',' + (d[i + 1] >> 4) + ',' + (d[i + 2] >> 4)); if (d[i] > 245 && d[i + 1] > 245 && d[i + 2] > 245) white++; }
  return { farben: seen.size, weiss: white / (d.length / (4 * 37)) }; }, null);
const picture = async (r) => r.page.evaluate(async (png) => { const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
  const d = g.getImageData(0, 0, bmp.width, bmp.height).data; const seen = new Set(); let white = 0, n = 0; for (let i = 0; i < d.length; i += 4 * 37) { n++; seen.add((d[i] >> 4) + ',' + (d[i + 1] >> 4) + ',' + (d[i + 2] >> 4)); if (d[i] > 245 && d[i + 1] > 245 && d[i + 2] > 245) white++; }
  return { farben: seen.size, weiss: white / n }; }, r.shot.toString('base64'));
// 1. gewöhnlicher Browser: Wasm-Fassung
{ const r = await run('1-auto-chromium', ''); const p = await picture(r);
  check(r.modus === 'wasm' && r.wasmJs && !r.js && r.kerne === 2, 'Chromium, modus=auto: Wasm-Fassung'); check(p.farben > 20 && p.weiss < 0.5 && !r.errs.length, `Startseite hat ein Bild (${p.farben} Farbtöne)`); await r.ctx.close(); }
// 2. ?modus=js: JavaScript-Fassung, ohne den Kotlin-Kern zu laden – und sie zeigt dasselbe Bild
{ const r = await run('2-modus-js', '?modus=js'); const p = await picture(r);
  check(r.modus === 'js' && r.js && !r.wasmJs && r.kerne === 1, 'modus=js: JavaScript-Fassung, nur die Grafik-Datei wird geladen' + ` (${r.kerne} .wasm)`);
  check(p.farben > 20 && p.weiss < 0.5 && !r.errs.length, `JavaScript-Fassung hat ein Bild (${p.farben} Farbtöne)` + (r.errs.length ? ' – Fehler: ' + r.errs[0] : ''));
  check(/Fassung: js – gewünscht/.test(r.log) && /Oberfläche gestartet/.test(r.log), 'Mitschrift nennt die Fassung, die Oberfläche ist gestartet'); await r.ctx.close(); }
// 3. JavaScript-Fassung: eine Aufgabe läuft (Bühne, Regler, „Los“ ändert das Bild)
{ const r = await run('3-js-aufgabe', '?modus=js&ziel=level/1-2'); await r.page.mouse.click(310, 815); await r.page.waitForTimeout(6000); const after = await r.page.screenshot({ path: `${out}/3-js-aufgabe-los.png` });
  check(Buffer.compare(r.shot, after) !== 0 && !r.errs.length, 'JavaScript-Fassung: Aufgabe öffnet sich und reagiert auf „Los“' + (r.errs.length ? ' – Fehler: ' + r.errs[0] : '')); await r.ctx.close(); }
// 4. iPhone und Safari auf dem Mac: von selbst die JavaScript-Fassung; mit ?modus=wasm lässt sich vergleichen
{ const r = await run('4-iphone', '', { ...devices['iPhone 15'], serviceWorkers: 'block' }); check(r.modus === 'js' && r.js && !r.wasmJs && /Safari\/WebKit/.test(r.log), 'iPhone, modus=auto: JavaScript-Fassung'); await r.ctx.close(); }
{ const r = await run('5-safari-mac', '', { userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/27.2 Safari/605.1.15' }); check(r.modus === 'js', 'Safari 27.2 (Mac/iPad im Desktop-Modus), modus=auto: JavaScript-Fassung'); await r.ctx.close(); }
{ const r = await run('6-iphone-wasm', '?modus=wasm', { ...devices['iPhone 15'], serviceWorkers: 'block' }); check(r.modus === 'wasm' && r.wasmJs, 'iPhone mit ?modus=wasm: Wasm-Fassung zum Vergleichen'); await r.ctx.close(); }
// 5. Wasm-Hauptmodul lässt sich nicht verarbeiten: merken, von selbst mit der JavaScript-Fassung neu laden
{ breakKern = true; const r = await run('7-kern-kaputt', '');
  check(r.modus === 'js' && r.js && await r.page.evaluate(() => Object.keys(localStorage).some((k) => /^raumlicht\.modus\./.test(k) && localStorage.getItem(k) === 'js')), 'Wasm-Fassung startet nicht → gemerkt und von selbst die JavaScript-Fassung geladen');
  const p = await picture(r); check(p.farben > 20, 'danach ist ein Bild da'); await r.ctx.close(); breakKern = false; }
await b.close(); server.closeAllConnections(); server.close(); process.exit(fail ? 1 : 0);
