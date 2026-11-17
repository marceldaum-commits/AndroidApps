// Update von der Vorversion ohne Datenverlust: Die alte WebApp läuft mit Spielstand und Einstellungen, auf dem Server erscheint die neue –
// danach müssen Spielstand, Abi-Pass-Code und Einstellungen noch da sein, die alte Stimm-Datei weggeräumt und die Konsole fehlerfrei sein.
// node test/web-vorversion.mjs <alter webapp-Ordner> <neuer webapp-Ordner>
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'fs'; import path from 'path'; import http from 'http'; import os from 'os';
const [,, oldDir, newDir] = process.argv; const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rl-vor-')); fs.cpSync(path.resolve(oldDir), dir, { recursive: true });
const types = { html: 'text/html', js: 'text/javascript', mjs: 'text/javascript', json: 'application/json', wasm: 'application/wasm', webmanifest: 'application/manifest+json', png: 'image/png', ttf: 'font/ttf', map: 'application/json', txt: 'text/plain' };
const server = http.createServer((q, r) => { let f = path.join(dir, decodeURIComponent(q.url.split('?')[0])); if (f.endsWith(path.sep) || fs.existsSync(f) && fs.statSync(f).isDirectory()) f = path.join(f, 'index.html');
  if (!fs.existsSync(f)) { r.writeHead(404); return r.end(); } r.writeHead(200, { 'Content-Type': types[f.split('.').pop()] || 'application/octet-stream', 'Cache-Control': 'no-cache' }); fs.createReadStream(f).pipe(r); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const version = (d) => JSON.parse(fs.readFileSync(path.join(d, 'version.json'), 'utf8')).version; const from = version(dir), to = version(path.resolve(newDir));
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await (await browser.newContext({ viewport: { width: 400, height: 860 } })).newPage();
const errs = []; let watch = false; page.on('pageerror', (e) => { if (watch) errs.push(e.message); }); page.on('console', (m) => { if (watch && m.type() === 'error') errs.push(m.text()); });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const started = () => page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 90000 });
// 1. Vorversion: einmal starten (Service Worker richtet sich ein), dann Spielstand und Einstellungen wie nach längerer Nutzung
await page.goto(url); await started(); await page.waitForFunction(() => navigator.serviceWorker.controller || navigator.serviceWorker.ready.then(() => true), null, { timeout: 60000 });
const data = { 'raumlicht.sterne': '1-1=3;1-2=2;5-3=1;abi-zelt-a=3', 'raumlicht.jamBest': '17', 'raumlicht.lageBest': '9', 'raumlicht.proCode': 'RL-TEST-CODE', 'raumlicht.stimmeAn': '1',
  'raumlicht.stimmeName': 'Anna', 'raumlicht.sprechtempo': '120', 'raumlicht.musik': '0', 'raumlicht.ton': '1', 'raumlicht.vibration': '0', 'raumlicht.lautstaerke': '65', 'raumlicht.musikLaut': '40', 'raumlicht.tonLaut': '80',
  'raumlicht.strengeVektoren': '1', 'raumlicht.skin': 'gold', 'raumlicht.plan': 'x', 'raumlicht.diary': 'y' };
await page.evaluate(async (d) => { for (const k in d) localStorage.setItem(k, d[k]); const c = await caches.open('raumlicht-stimme'); await c.put('/alte-stimme.onnx', new Response(new Uint8Array(5000))); }, data);
await page.reload(); await started(); await page.waitForTimeout(1500);
const seenOld = await page.evaluate(() => ({ nova: !!window.__rlNova, sterne: localStorage.getItem('raumlicht.sterne') }));
check(!seenOld.nova && seenOld.sterne === data['raumlicht.sterne'], `Vorversion ${from} läuft mit Spielstand`);
// 2. Die neue Version erscheint auf dem Server; die Seite holt sie sich (spätestens nach ein paar Aufrufen)
fs.rmSync(dir, { recursive: true, force: true }); fs.cpSync(path.resolve(newDir), dir, { recursive: true }); watch = true;
let now = null; for (let i = 0; i < 8 && !now; i++) { await page.goto(url + '?n=' + i).catch(() => {}); await started().catch(() => {}); await page.waitForTimeout(3500);
  now = await page.evaluate(() => window.__rlNova ? true : null).catch(() => null); }
const shown = await page.evaluate(() => fetch('version.json', { cache: 'no-store' }).then((r) => r.json()).then((j) => j.version));
check(!!now && shown === to, `Update auf ${to} ist angekommen`);
// 3. Nichts verloren: Spielstand, Abi-Pass-Code, Einstellungen
const after = await page.evaluate((keys) => Object.fromEntries(keys.map((k) => [k, localStorage.getItem(k)])), Object.keys(data));
const kept = ['sterne', 'jamBest', 'lageBest', 'proCode', 'stimmeAn', 'musik', 'ton', 'vibration', 'lautstaerke', 'musikLaut', 'tonLaut', 'strengeVektoren', 'skin', 'plan', 'diary'].map((k) => 'raumlicht.' + k);
const lost = kept.filter((k) => after[k] !== data[k]);
check(lost.length === 0, lost.length ? 'verloren oder verändert: ' + lost.join(', ') : 'Spielstand, Abi-Pass-Code und Einstellungen unverändert (' + kept.length + ' Einträge)');
// 4. Alte Stimm-Datei weg, neuer Regler steht auf Standard (das alte Sprechtempo galt für eine andere Stimme)
const cachesNow = await page.evaluate(() => caches.keys());
check(!cachesNow.includes('raumlicht-stimme'), 'alte Stimm-Datei (63 MB) ist weggeräumt');
// 5. Einstellungen öffnen und eine Aufgabe: ohne Fehler, Spielstand bleibt
await page.goto(url + '?ziel=einstellungen'); await started(); await page.waitForTimeout(2500);
await page.goto(url + '?ziel=level/1-1'); await started(); await page.waitForTimeout(2500);
check((await page.evaluate(() => localStorage.getItem('raumlicht.sterne'))) === data['raumlicht.sterne'], 'Spielstand bleibt auch nach Einstellungen und Aufgabe');
const real = errs.filter((e) => !/stimmen\.mathenachhilfe|ERR_|Failed to load resource/.test(e));
check(real.length === 0, real.length ? 'Konsolenfehler: ' + real.slice(0, 4).join(' | ') : 'keine Konsolenfehler nach dem Update');
await browser.close(); server.close(); process.exit(fail ? 1 : 0);
