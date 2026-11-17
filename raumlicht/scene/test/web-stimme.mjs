// Prüft die natürliche Stimme „Thorsten“ in der gebauten WebApp: laden (mit Prüfsumme), im Browser speichern, einen Satz rechnen.
// node test/web-stimme.mjs <webappOrdner> [<lokale Stimmdatei>]   – ohne zweite Angabe wird vom echten Server geladen (braucht Netz)
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, local] = process.argv;
const types = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm', '.json': 'application/json', '.ttf': 'font/ttf', '.png': 'image/png', '.tsv': 'text/plain; charset=utf-8', '.txt': 'text/plain; charset=utf-8' };
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html';
  const f = p === '/__stimme.onnx' && local ? local : path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': types[path.extname(f)] || 'application/octet-stream', 'Content-Length': d.length }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const problems = []; const ok = (name, good, info = '') => { console.log((good ? 'ok   ' : 'FAIL ') + name + (info ? '  ' + info : '')); if (!good) problems.push(name); };
const launch = { args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--autoplay-policy=no-user-gesture-required'] };
if (!local && process.env.HTTPS_PROXY) launch.proxy = { server: process.env.HTTPS_PROXY, bypass: 'localhost' };
const browser = await chromium.launch(launch); const ctx = await browser.newContext({ viewport: { width: 400, height: 860 }, ignoreHTTPSErrors: true });
const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
if (local) await page.addInitScript(() => { window.__rlStimmeUrl = location.origin + '/__stimme.onnx'; });
await page.goto(url); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 });
await page.waitForFunction(() => window.__rlThorsten && window.__rlThorsten.state !== 'checking', null, { timeout: 20000 });
ok('am Anfang ist keine Stimme geladen und kein Rechen-Baustein da', await page.evaluate(() => window.__rlThorsten.state === 'missing' && !window.ort));
const t0 = Date.now(); await page.evaluate(() => { window.__rlThorsten.download(); });
await page.waitForFunction(() => window.__rlThorsten.state !== 'loading', null, { timeout: 420000 });
const st = await page.evaluate(() => ({ state: window.__rlThorsten.state, why: window.__rlThorsten.why }));
ok('Stimme geladen und Prüfsumme stimmt', st.state === 'ready', `${((Date.now() - t0) / 1000).toFixed(1)} s ${st.why}`);
// Laut-Nummern für einen festen Satz – wie PiperIds.encode
const tokens = new Map(fs.readFileSync(path.join(dir, 'stimme/tokens.txt'), 'utf8').split('\n').filter(Boolean).map((l) => { const i = l.lastIndexOf(' '); return [l.slice(0, i), Number(l.slice(i + 1))]; }));
const line = fs.readFileSync(path.join(dir, 'stimme/saetze.txt'), 'utf8').split('\n').find((l) => l.startsWith('Hallo, ich bin Lux. So klinge ich jetzt.\t'));
ok('der Probesatz hat Lautschrift', !!line);
const sentences = line.split('\t')[1].split('|').filter(Boolean).map((s) => { const out = [1, 0]; for (const ch of s) { if (tokens.has(ch)) { out.push(tokens.get(ch), 0); } } out.push(2); return out; });
const t1 = Date.now();
const res = await page.evaluate(async (ids) => { const out = []; for (const s of ids) { const pcm = await window.__rlThorsten.synth(s, 1.0); let peak = 0, sum = 0; for (let i = 0; i < pcm.length; i++) { const a = Math.abs(pcm[i]); if (a > peak) peak = a; sum += pcm[i] * pcm[i]; } out.push({ n: pcm.length, peak: peak, rms: Math.sqrt(sum / pcm.length) }); } return out; }, sentences).catch((e) => { errs.push(String(e)); return []; });
const sec = res.reduce((a, r) => a + r.n, 0) / 22050;
ok('Thorsten rechnet den Satz im Browser', res.length === sentences.length && res.every((r) => r.n > 8000 && r.peak > 0.05 && r.peak <= 1.5 && r.rms > 0.01), `${sec.toFixed(1)} s Sprache in ${((Date.now() - t1) / 1000).toFixed(1)} s gerechnet (erster Satz mit Start des Bausteins) · ${JSON.stringify(res.map((r) => ({ s: +(r.n / 22050).toFixed(2), spitze: +r.peak.toFixed(2) })))}`);
const t2 = Date.now(); await page.evaluate(async (ids) => { await window.__rlThorsten.synth(ids, 1.0); }, sentences[0]);
ok('zweiter Durchlauf (Baustein läuft schon)', true, `${((Date.now() - t2) / 1000).toFixed(1)} s für ${(res[0].n / 22050).toFixed(1)} s Sprache`);
await page.reload(); await page.waitForFunction(() => window.__rlThorsten && window.__rlThorsten.state !== 'checking', null, { timeout: 30000 });
ok('nach dem Neuladen ist die Stimme noch da', await page.evaluate(() => window.__rlThorsten.state === 'ready'));
await page.evaluate(() => window.__rlThorsten.remove()); ok('Entfernen räumt den Speicher', await page.evaluate(async () => window.__rlThorsten.state === 'missing' && !(await caches.has('raumlicht-stimme'))));
if (errs.length) { console.log('Fehler: ' + errs.join(' | ')); problems.push('Fehler in der Seite'); }
await browser.close(); server.close();
console.log(problems.length ? `${problems.length} Problem(e)` : 'Thorsten im Browser funktioniert');
process.exit(problems.length ? 1 : 0);
