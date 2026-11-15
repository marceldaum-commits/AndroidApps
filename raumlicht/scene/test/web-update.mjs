// Prüft das automatische Update der WebApp: Seite läuft mit Version A, auf dem Server erscheint Version B –
// die Seite muss sich ohne Zutun aktualisieren (auf der Startseite sofort, in einer Aufgabe erst beim Verlassen).
// node test/web-update.mjs <webapp-Ordner>
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'fs'; import path from 'path'; import http from 'http'; import os from 'os';
const src = path.resolve(process.argv[2]); const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rl-web-')); fs.cpSync(src, dir, { recursive: true });
const types = { html: 'text/html', js: 'text/javascript', json: 'application/json', wasm: 'application/wasm', webmanifest: 'application/manifest+json', png: 'image/png', ttf: 'font/ttf', map: 'application/json' };
const server = http.createServer((q, r) => { let f = path.join(dir, decodeURIComponent(q.url.split('?')[0])); if (f.endsWith(path.sep) || fs.existsSync(f) && fs.statSync(f).isDirectory()) f = path.join(f, 'index.html');
  if (!fs.existsSync(f)) { r.writeHead(404); return r.end(); } r.writeHead(200, { 'Content-Type': types[f.split('.').pop()] || 'application/octet-stream', 'Cache-Control': 'max-age=3600' }); fs.createReadStream(f).pipe(r); }).listen(8766);
const old = JSON.parse(fs.readFileSync(path.join(dir, 'version.json'), 'utf8')).version;
function publish(v) { for (const f of ['index.html', 'sw.js', 'version.json']) fs.writeFileSync(path.join(dir, f), fs.readFileSync(path.join(dir, f), 'utf8').split(old).join(v)); }
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await (await browser.newContext({ viewport: { width: 1280, height: 800 } })).newPage();
let failed = 0; const check = (name, ok, info = '') => { console.log((ok ? 'ok   ' : 'FAIL ') + name + (info ? '  ' + info : '')); if (!ok) failed++; };
const shown = () => page.evaluate(() => document.documentElement.dataset.version);
const cached = () => page.evaluate(() => caches.keys().then((k) => k.join(',')));
const ask = () => page.evaluate(() => navigator.serviceWorker.getRegistration().then((r) => r.update()));
await page.goto('http://localhost:8766/'); await page.waitForFunction(() => navigator.serviceWorker.controller, null, { timeout: 60000 }); await page.waitForTimeout(2500);
check('erste Version läuft und ist gespeichert', await shown() === old && (await cached()) === 'raumlicht-' + old, `${await shown()} / ${await cached()}`);

// 1 · neue Version, Nutzer ist auf der Startseite → lädt sich von selbst neu
publish('9.9.1'); await ask();
await page.waitForFunction(() => document.documentElement.dataset.version === '9.9.1', null, { timeout: 60000 }).catch(() => {});
await page.waitForTimeout(2500);
check('Startseite: neue Version erscheint ohne Zutun', await shown() === '9.9.1', await shown());
check('alter Speicher ist weggeräumt', (await cached()) === 'raumlicht-9.9.1', await cached());
check('die App startet nach dem Update', await page.evaluate(() => document.getElementById('start').style.display === 'none'));

// 2 · neue Version, Nutzer steckt in einer Aufgabe → kein Neuladen mittendrin, erst beim Zurückgehen
await page.mouse.click(42, 250); await page.waitForTimeout(2000);
check('Aufgabe ist offen', await page.evaluate(() => document.getElementById('stage').style.display === 'block'));
fs.writeFileSync(path.join(dir, 'version.json'), '{ "version": "9.9.2" }'); for (const f of ['index.html', 'sw.js']) fs.writeFileSync(path.join(dir, f), fs.readFileSync(path.join(dir, f), 'utf8').split('9.9.1').join('9.9.2'));
await ask(); await page.waitForFunction(() => caches.keys().then((k) => k.includes('raumlicht-9.9.2')), null, { timeout: 60000 }).catch(() => {}); await page.waitForTimeout(3000);
check('in der Aufgabe wird nicht neu geladen', await shown() === '9.9.1' && await page.evaluate(() => document.getElementById('stage').style.display === 'block'), await shown());
await page.mouse.click(50, 32);                                                           // „‹ zurück“
await page.waitForFunction(() => document.documentElement.dataset.version === '9.9.2', null, { timeout: 60000 }).catch(() => {}); await page.waitForTimeout(1500);
check('zurück auf der Startseite: Update kommt von selbst', await shown() === '9.9.2', await shown());

// 3 · offline: die App startet aus dem Speicher
await page.context().setOffline(true); await page.reload().catch(() => {}); await page.waitForTimeout(4000);
check('offline startet die App', await shown() === '9.9.2' && await page.evaluate(() => document.getElementById('start').style.display === 'none'));
await browser.close(); server.close(); fs.rmSync(dir, { recursive: true, force: true });
console.log(failed ? `${failed} Fehler` : 'automatisches Update funktioniert'); process.exit(failed ? 1 : 0);
