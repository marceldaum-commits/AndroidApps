// Schrittprotokoll des Starts: mit „?diag=1“ sichtbar, immer gespeichert; das Ladebild wird gemalt, bevor geladen wird; auf dem iPhone kommen die Kern-Dateien nacheinander;
// nach einem Abbruch mitten im Start sagt das Ladebild, wie weit Raumlicht beim letzten Mal kam.
// node test/web-diag.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium, devices } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const wasm = [];                                                                     // Beginn und Ende jeder Kern-Datei, wie der Server sie sieht
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); return; }
    r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' });
    if (f.endsWith('.wasm')) { const w = { name: p, start: Date.now(), end: 0 }; wasm.push(w); r.on('finish', () => { w.end = Date.now(); }); /* in Häppchen, damit sich gleichzeitiges Laden zeigen würde */
      let i = 0; const part = () => { if (i >= d.length) return r.end(); r.write(d.subarray(i, i + 262144)); i += 262144; setTimeout(part, 15); }; part(); }
    else r.end(d); }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const started = (page) => page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 90000 });
const iphone = { ...devices['iPhone 15'], serviceWorkers: 'block' };
// 1. iPhone mit ?diag=1: Protokoll sichtbar, Reihenfolge stimmt, Kern-Dateien nacheinander
{ const ctx = await b.newContext(iphone); const page = await ctx.newPage(); await page.goto(url + '?diag=1'); await started(page); await page.waitForTimeout(1500);
  await page.screenshot({ path: `${out}/1-schritte.png` });
  const text = await page.evaluate(() => (document.getElementById('schritte') || {}).textContent || ''); const at = (re) => text.search(re);
  check(/Schrittprotokoll/.test(text) && at(/Seite gelesen/) >= 0 && at(/Seite gelesen/) < at(/erstes Bild gemalt/) && at(/erstes Bild gemalt/) < at(/angekommen/) && at(/raumlicht\.js geladen/) > 0 && at(/Start geschafft/) > at(/angekommen/),
    '„?diag=1“ zeigt die Schritte in der richtigen Reihenfolge (erst malen, dann laden, dann starten)');
  check((text.match(/Kern .*angekommen/g) || []).length === 2 && /nacheinander/.test(text), 'beide Kern-Dateien sind einzeln vermerkt');
  check(wasm.length === 2 && wasm[1].start >= wasm[0].end, 'iPhone: die zweite Kern-Datei beginnt erst, wenn die erste ganz da ist' + (wasm.length === 2 ? ` (${wasm[1].start - wasm[0].end} ms danach)` : ` (${wasm.length} Abrufe)`));
  check(await page.evaluate(() => /schritt erstes Bild gemalt/.test(localStorage.getItem('raumlicht.mitschrift') || '')), 'die Schritte stehen auch in der gespeicherten Mitschrift');
  await ctx.close(); }
// 2. gewöhnlicher Browser ohne ?diag: kein Protokoll im Bild, Kern-Dateien gleichzeitig (schneller)
{ wasm.length = 0; const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); await page.goto(url); await started(page);
  check(await page.evaluate(() => !document.getElementById('schritte')), 'ohne „?diag=1“ ist kein Protokoll im Bild');
  check(wasm.length === 2 && wasm[1].start < wasm[0].end, 'sonst laden die Kern-Dateien gleichzeitig'); await ctx.close(); }
// 3. Abbruch mitten im Start: beim nächsten Öffnen steht auf dem Ladebild, wie weit es kam
{ const ctx = await b.newContext(iphone); const page = await ctx.newPage();
  await page.addInitScript(() => { if (!sessionStorage.getItem('einmal')) { sessionStorage.setItem('einmal', '1'); sessionStorage.setItem('rl-lauf', '1');
    localStorage.setItem('raumlicht.mitschrift', '10:00:00 info Start\n10:00:01 schritt erstes Bild gemalt, Laden beginnt (+40 ms)\n10:00:03 schritt Kern skiko.wasm angekommen (+2100 ms)'); } });
  await page.goto(url, { waitUntil: 'commit' }); await page.waitForSelector('#letzter', { timeout: 10000 }).catch(() => {}); await page.screenshot({ path: `${out}/2-abbruch-im-start.png` });
  const hint = await page.evaluate(() => (document.getElementById('letzter') || {}).textContent || '');
  check(/brach der Start hier ab: Kern skiko\.wasm angekommen/.test(hint), 'nach einem Abbruch im Start nennt das Ladebild den letzten Schritt'); await ctx.close(); }
await b.close(); server.closeAllConnections(); server.close(); process.exit(fail ? 1 : 0);
