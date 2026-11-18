// Vorlesen ist abgeschaltet (seit 2.18.14): Auch wer „Lux spricht“ früher eingeschaltet hatte, löst keinen Abruf am Stimmen-Server aus, und die ausgelieferte Textliste ist leer.
// node test/web-still.mjs <webappOrdner>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const dir = process.argv[2];
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const ctx = await b.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
const voice = []; await ctx.route(/stimmen\.mathenachhilfe-online\.com/, (r) => { voice.push(r.request().url()); r.abort(); });
const spoken = []; await page.addInitScript(() => { localStorage.setItem('raumlicht.stimmeAn', '1'); if (window.speechSynthesis) { const s = speechSynthesis.speak.bind(speechSynthesis); speechSynthesis.speak = (u) => { (window.__gesprochen = window.__gesprochen || []).push(u.text); }; } });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const open = async (q) => { await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }); await page.waitForTimeout(3500); };
await open('?ziel=level/1-2'); await page.mouse.click(200, 400); await page.mouse.click(310, 815); await page.waitForTimeout(6000);      // Aufgabe mit Sprechblase, ein Fehlversuch mit Rückmeldung
await open('?ziel=einstellungen');
check(voice.length === 0, 'kein Abruf am Stimmen-Server' + (voice.length ? ': ' + voice[0] : ''));
check((await page.evaluate(() => (window.__gesprochen || []).length)) === 0, 'die Stimme des Geräts spricht nicht');
const list = JSON.parse(fs.readFileSync(path.join(dir, 'vorlesen/texte.json'), 'utf8'));
check(list.format === 1 && list.app === 'raumlicht' && Array.isArray(list.texte) && list.texte.length === 0, 'vorlesen/texte.json ist eine leere Liste');
await b.close(); server.close(); process.exit(fail ? 1 : 0);
