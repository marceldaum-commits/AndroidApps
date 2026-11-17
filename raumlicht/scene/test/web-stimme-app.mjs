// Durchgängig: Stimme laden, WebApp neu öffnen, Aufgabe aufrufen – die App liest den Auftrag mit Thorsten vor.
// node test/web-stimme-app.mjs <webappOrdner> <lokale Stimmdatei>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, local] = process.argv;
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = p === '/__stimme.onnx' ? local : path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--autoplay-policy=no-user-gesture-required'] }); const ctx = await b.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
const errs = []; page.on('pageerror', (e) => errs.push(e.message)); page.on('console', (m) => { if (m.type() === 'error') errs.push(m.text()); });
await page.addInitScript(() => { window.__rlStimmeUrl = location.origin + '/__stimme.onnx'; try { localStorage.setItem('raumlicht.stimmeAn', '1'); localStorage.setItem('raumlicht.stimmeThorsten', '1'); } catch (e) {} });
await page.goto(url); await page.waitForFunction(() => window.__rlThorsten && window.__rlThorsten.state !== 'checking', null, { timeout: 60000 });
await page.evaluate(() => { window.__rlThorsten.download(); }); await page.waitForFunction(() => window.__rlThorsten.state === 'ready', null, { timeout: 120000 });
// neu laden und direkt eine Aufgabe öffnen: Die App liest den Auftrag vor – mit Thorsten
await page.goto(url + '?ziel=level/5-2'); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 });
let got = null; for (let i = 0; i < 60 && !got; i++) { await page.waitForTimeout(500); got = await page.evaluate(() => window.__rlThorsten.lastSamples > 0 ? { n: window.__rlThorsten.lastSamples, peak: window.__rlThorsten.lastPeak, ready: window.__rlThorsten.ready, problem: window.__rlThorsten.problem } : null);
  if (!got && i === 8) await page.mouse.click(200, 300); }
console.log(got ? `Die App hat mit Thorsten vorgelesen: ${(got.n / 22050).toFixed(1)} s, Spitze ${got.peak.toFixed(2)}` : 'FAIL: Thorsten hat nicht gesprochen', await page.evaluate(() => JSON.stringify({ state: window.__rlThorsten.state, ready: window.__rlThorsten.ready, problem: window.__rlThorsten.problem, geraet: 'speechSynthesis' in window && speechSynthesis.speaking })));
console.log(errs.length ? 'Fehler: ' + errs.join(' | ') : 'keine Fehler'); await b.close(); server.close();
