// „App installieren“ in den Einstellungen der WebApp: Knopf, wenn der Browser es anbietet; Anleitung auf dem iPhone; nichts, wenn schon installiert.
// node test/web-install.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium, devices } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') || f.endsWith('.webmanifest') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const state = (page) => page.evaluate(() => { const I = window.__rlInstall || {};
  if (I.done || matchMedia('(display-mode: standalone)').matches || navigator.standalone === true) return 'installiert';
  if (I.prompt) return 'knopf'; if (/iPad|iPhone|iPod/.test(navigator.userAgent)) return 'ios'; return 'nichts'; });
const run = async (name, opts, init) => { const ctx = await b.newContext(opts); const page = await ctx.newPage(); if (init) await page.addInitScript(init);
  await page.goto(url + '?ziel=einstellungen'); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }); await page.waitForTimeout(3000);
  return { page, ctx, shot: await page.screenshot({ path: `${out}/${name}.png` }) }; };
// 1. gewöhnlicher Browser ohne Angebot: keine Karte
const plain = await run('1-ohne', { viewport: { width: 400, height: 860 } });
check(await state(plain.page) === 'nichts', 'ohne Angebot des Browsers: kein Installations-Knopf');
// 2. der Browser bietet die Installation an: Knopf erscheint, Antippen ruft das Fenster des Browsers, danach verschwindet die Karte
await plain.page.evaluate(() => { const e = new Event('beforeinstallprompt'); e.prompt = () => { window.__gefragt = true; }; e.userChoice = Promise.resolve({ outcome: 'accepted' }); window.dispatchEvent(e); });
await plain.page.waitForTimeout(1500); const offered = await plain.page.screenshot({ path: `${out}/2-knopf.png` });
check(await state(plain.page) === 'knopf' && Buffer.compare(plain.shot, offered) !== 0, 'Angebot des Browsers: Karte „App installieren“ erscheint');
// Knopf suchen: die breite türkise Fläche im oberen Drittel
await plain.page.mouse.click(200, 205); await plain.page.waitForTimeout(1500); await plain.page.screenshot({ path: `${out}/3-nach-klick.png` });
check(await plain.page.evaluate(() => window.__gefragt === true), 'Antippen öffnet die Installations-Frage des Browsers');
check(await state(plain.page) === 'installiert', 'nach dem Installieren verschwindet die Karte');
await plain.ctx.close();
// 3. iPhone: Anleitung statt Knopf
const ios = await run('4-iphone', { ...devices['iPhone 13'] });
check(await state(ios.page) === 'ios', 'iPhone: Anleitung „Teilen → Zum Home-Bildschirm“'); await ios.ctx.close();
// 4. schon installiert (läuft als App): keine Karte
const app = await run('5-installiert', { viewport: { width: 400, height: 860 } }, () => { const m = window.matchMedia.bind(window); window.matchMedia = (q) => /standalone/.test(q) ? { matches: true, addEventListener() {}, removeEventListener() {} } : m(q); });
check(await state(app.page) === 'installiert' && Buffer.compare(app.shot, plain.shot) === 0, 'als App gestartet: keine Karte'); await app.ctx.close();
const mf = JSON.parse(fs.readFileSync(path.join(dir, 'manifest.webmanifest'), 'utf8'));
check(mf.display === 'standalone' && mf.start_url && mf.id && mf.icons.some((i) => i.sizes === '192x192') && mf.icons.some((i) => i.sizes === '512x512') && fs.existsSync(path.join(dir, 'icon-512.png')), 'Manifest: Name, Start, Vollbild, Symbole 192 und 512');
await b.close(); server.close(); process.exit(fail ? 1 : 0);
