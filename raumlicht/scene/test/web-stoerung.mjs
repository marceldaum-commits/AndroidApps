// Nie mehr eine weiße Seite: Grafik-Ausfall, hängender Start und ein vom Gerät beendeter Tab zeigen eine sichtbare Meldung (mit Version und „Neu laden“); iPhones zeichnen mit höchstens doppelter Schärfe.
// node test/web-stoerung.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium, devices } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const version = JSON.parse(fs.readFileSync(path.join(dir, 'version.json'), 'utf8')).version;
let hang = false;
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  if (hang && /\.wasm$/.test(p)) return;                                              // Start hängt: der App-Kern kommt nie an
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const started = (page) => page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 });
const iphone = { ...devices['iPhone 15'], serviceWorkers: 'block' };

// 1. iPhone (dreifache Schärfe): Raumlicht zeichnet mit doppelter
{ const ctx = await b.newContext(iphone); const page = await ctx.newPage(); await page.goto(url); await started(page); await page.waitForTimeout(2500);
  check(await page.evaluate(() => window.devicePixelRatio) === 2, 'iPhone: höchstens doppelte Schärfe (Gerät 3 → benutzt 2)');
  check(await page.evaluate(() => !document.getElementById('absturz') && !document.getElementById('stoerung')), 'normaler Start: keine Meldung');
  // 2. Grafik-Kontext der Oberfläche geht verloren und kommt nicht wieder → Meldung mit Version und „Neu laden“
  const lost = await page.evaluate(() => { const a = document.getElementById('app'); const c = (a.shadowRoot && a.shadowRoot.querySelector('canvas')) || a.querySelector('canvas');
    const gl = c.getContext('webgl2') || c.getContext('webgl'); const e = gl && gl.getExtension('WEBGL_lose_context'); if (!e) return false; e.loseContext(); return true; });
  await page.waitForTimeout(4500); await page.screenshot({ path: `${out}/1-grafik-ausfall.png` });
  const text = await page.evaluate(() => (document.getElementById('stoerung') || {}).textContent || '');
  check(lost && /Anzeige ist ausgefallen/.test(text) && text.includes('Version ' + version) && /Neu laden/.test(text), 'Grafik-Ausfall: Meldung mit Version und „Neu laden“' + (lost ? '' : ' (Kontext ließ sich nicht entziehen)'));
  await Promise.all([page.waitForNavigation(), page.click('#stoerung button')]); await started(page); await page.waitForTimeout(1500);
  check(await page.evaluate(() => !document.getElementById('stoerung') && !document.getElementById('absturz')), '„Neu laden“ startet Raumlicht frisch – ohne Absturz-Hinweis (ordentlich verlassen)');
  await ctx.close(); }

// 3. Das Gerät hat die Seite beendet (kein ordentliches Verlassen): Hinweis beim nächsten Start; nach dem zweiten Mal Sparmodus (einfache Schärfe), Mitschrift bleibt für die Diagnose
{ const ctx = await b.newContext(iphone); const page = await ctx.newPage(); await page.goto(url); await started(page); await page.waitForTimeout(1500);
  const crash = async () => { /* wie ein beendeter Tab: der Merker bleibt stehen */ await page.evaluate(() => { const keep = () => sessionStorage.setItem('rl-lauf', '1'); window.addEventListener('pagehide', keep); document.addEventListener('visibilitychange', keep); window.addEventListener('unload', keep); }); await page.reload(); await started(page); await page.waitForTimeout(1500); };
  await crash(); const first = await page.evaluate(() => (document.getElementById('absturz') || {}).textContent || '');
  await page.screenshot({ path: `${out}/2-absturz-hinweis.png` });
  check(/hatte Raumlicht beendet/.test(first) && await page.evaluate(() => window.devicePixelRatio) === 2, 'nach einem Absturz: Hinweis, Schärfe bleibt');
  await crash(); const second = await page.evaluate(() => (document.getElementById('absturz') || {}).textContent || '');
  check(/Sparmodus/.test(second) && await page.evaluate(() => window.devicePixelRatio) === 1, 'nach dem zweiten Absturz in Folge: Sparmodus mit einfacher Schärfe');
  check(await page.evaluate(() => /Start /.test(localStorage.getItem('raumlicht.mitschrift.absturz') || '')), 'die Mitschrift vor dem Absturz bleibt für die Diagnose erhalten');
  await page.goto(url + 'diagnose.html'); await page.waitForTimeout(1500);
  check(await page.evaluate(() => /vor dem letzten Absturz/.test(document.getElementById('log').textContent)), 'die Diagnose-Seite zeigt sie');
  await page.goto(url + '?mit=schaerfe'); await started(page);
  check(await page.evaluate(() => localStorage.getItem('raumlicht.sparsam') === null), '„?mit=schaerfe“ hebt den Sparmodus wieder auf');
  await ctx.close(); }

// 4. Der Start hängt ohne Fehler: nach der Wartezeit eine Meldung mit Version und „Neu laden“ (kein endloser Ladebalken)
{ hang = true; const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); await page.addInitScript(() => { window.__rlHangMs = 3000; });
  await page.goto(url, { waitUntil: 'commit' }).catch(() => {}); await page.waitForTimeout(8000); await page.screenshot({ path: `${out}/3-start-haengt.png` });
  const text = await page.evaluate(() => { const t = document.getElementById('start-text'); return t.className + ' ' + t.textContent; });
  check(/^fehler /.test(text) && /hängen geblieben/.test(text) && text.includes('Version ' + version) && /Neu laden/.test(text), 'hängender Start: Meldung mit Version und „Neu laden“');
  await ctx.close(); hang = false; }
await b.close(); server.closeAllConnections(); server.close(); process.exit(fail ? 1 : 0);
