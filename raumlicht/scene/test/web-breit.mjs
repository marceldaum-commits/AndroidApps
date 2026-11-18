// Breite Bildschirme (PC): Die Startseite teilt sich auf – links die Bühne, rechts Gebiete, Kapitel und Aufgaben mit Namen. Auf dem Handy bleibt alles wie gehabt.
// node test/web-breit.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
// Anteil der Bildpunkte in einem Ausschnitt, die deutlich heller sind als der dunkle Hintergrund – so lässt sich „hier steht etwas“ von „hier ist leer“ unterscheiden
const filled = (page, x, y, w, h) => page.evaluate(async ([x, y, w, h]) => { const c = document.querySelector('canvas'); const s = await createImageBitmap(c, 0, 0, c.width, c.height); const k = c.width / innerWidth;
  const o = new OffscreenCanvas(Math.round(w * k), Math.round(h * k)); const g = o.getContext('2d'); g.drawImage(s, -x * k, -y * k); const d = g.getImageData(0, 0, o.width, o.height).data; let n = 0;
  for (let i = 0; i < d.length; i += 4) if (d[i] + d[i + 1] + d[i + 2] > 330) n++; return n / (d.length / 4); }, [x, y, w, h]).catch(() => -1);
for (const [w, h, name] of [[1920, 870, 'pc'], [1366, 700, 'laptop']]) for (const modus of ['wasm', 'js']) {
  const ctx = await b.newContext({ viewport: { width: w, height: h }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await page.goto(url + '?modus=' + modus); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 }); await page.waitForTimeout(5000);
  const before = await page.screenshot({ path: `${out}/breit-${name}-${modus}-1-start.png` });
  const side = Math.min(640, Math.max(440, w * 0.4)); const left = w - side;
  // „Analysis“ ist der mittlere der drei Gebiets-Knöpfe oben in der rechten Spalte
  await page.mouse.click(left + side / 2, 92); await page.waitForTimeout(2500); const after = await page.screenshot({ path: `${out}/breit-${name}-${modus}-2-analysis.png` });
  check(Buffer.compare(before, after) !== 0, `${name}/${modus}: Gebiet in der rechten Spalte lässt sich wählen`);
  // erste Aufgabe des Kapitels antippen (Zeile unter dem Kapitelkopf): die Aufgabe öffnet sich
  await page.mouse.click(left + side / 2, h - 30); await page.waitForTimeout(600);
  check(errs.length === 0, errs.length ? `${name}/${modus}: Fehler: ` + errs[0] : `${name}/${modus}: keine Fehler`);
  await ctx.close();
}
// Handy: unverändert einspaltig (die Kapitel-Zeile zum Wischen liegt über die ganze Breite)
{ const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await page.goto(url); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 }); await page.waitForTimeout(4000); await page.screenshot({ path: `${out}/breit-handy.png` });
  check(errs.length === 0, 'Handy: keine Fehler'); await ctx.close(); }
await b.close(); server.close(); process.exit(fail ? 1 : 0);
