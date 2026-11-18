// Anonyme Nutzungszählung in der WebApp: genau ein Aufruf „start“ beim Öffnen – nur die erlaubten Felder, ohne Cookies; mit ausgeschaltetem Schalter gar keiner.
// node test/web-zaehler.mjs <webappOrdner>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const dir = process.argv[2]; const version = JSON.parse(fs.readFileSync(path.join(dir, 'version.json'), 'utf8')).version;
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const run = async (q, init) => { const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const calls = [];
  await ctx.addCookies([{ name: 'sitzung', value: 'geheim', domain: 'trainer.mathenachhilfe-online.com', path: '/', secure: true, sameSite: 'None' }]);
  await ctx.route(/trainer\.mathenachhilfe-online\.com/, (r) => { const q = r.request(); calls.push({ url: q.url(), method: q.method(), body: q.postData(), headers: q.headers() }); r.fulfill({ status: 204, body: '' }); });
  await page.addInitScript(() => { window.__rlZaehleLokal = true; });      // sonst zählt die WebApp auf dem eigenen Rechner nicht
  if (init) await page.addInitScript(init);
  await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 90000 }); await page.waitForTimeout(3000); await ctx.close(); return calls; };
{ const calls = await run('');
  check(calls.length === 1 && calls[0].method === 'POST' && calls[0].url === 'https://trainer.mathenachhilfe-online.com/zaehler/v1/e', 'beim Öffnen genau ein Zählaufruf' + ` (${calls.length})`);
  check(calls[0] && calls[0].body === JSON.stringify({ app: 'raumlicht', v: version, ev: 'start' }), 'Inhalt: nur App, Version und Ereignis – ' + (calls[0] || {}).body);
  check(calls[0] && /^text\/plain/.test(calls[0].headers['content-type'] || '') && !calls[0].headers.cookie && !calls[0].headers.referer, 'als einfacher Text, ohne Cookie und ohne Herkunftsadresse'); }
{ const calls = await run('?modus=js'); check(calls.length === 1 && /"ev":"start"/.test(calls[0].body || ''), 'auch die JavaScript-Fassung zählt genau einmal'); }
{ const calls = await run('', () => { localStorage.setItem('raumlicht.zaehlen', '0'); }); check(calls.length === 0, 'Schalter aus: kein einziger Aufruf'); }
{ const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const calls = [];
  await ctx.route(/trainer\.mathenachhilfe-online\.com/, (r) => { calls.push(r.request().postData()); r.fulfill({ status: 204, body: '' }); });
  await page.goto(url + '?ziel=level/1-2'); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 90000 }); await page.waitForTimeout(3000); await ctx.close();
  check(calls.length === 0, calls.join(' ') + ' ohne ausdrückliche Freigabe zählt die WebApp auf dem eigenen Rechner nicht (Testläufe bleiben aus der Statistik)'); }
{ const calls = await run('?ziel=level/1-2'); const evs = calls.map((c) => JSON.parse(c.body)); const lv = evs.find((e) => e.ev === 'level_start');
  check(!!lv && lv.thema === 'vektoren' && Object.keys(lv).sort().join() === 'app,ev,thema,v', 'Aufgabe geöffnet: „level_start“ nur mit Gebiet – ohne Aufgabe, ohne Sterne'); }
await b.close(); server.closeAllConnections(); server.close(); process.exit(fail ? 1 : 0);
