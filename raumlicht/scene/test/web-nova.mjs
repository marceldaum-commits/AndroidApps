// Stimme „Nova“ in der WebApp: Die App holt zu einem Auftrag die fertige Datei vom Stimmen-Server, lässt Laufzeit-Texte erzeugen und meldet „fehlt“, wenn nichts geht.
// node test/web-nova.mjs <webappOrdner> <irgendeine MP3-Datei>      (der Stimmen-Server wird hier nachgestellt – Chromium hat im Test kein Internet)
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, mp3] = process.argv; const sound = fs.readFileSync(mp3);
const seen = []; let made = false;
const cors = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'content-type' };
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]);
  if (p.startsWith('/__s/')) { p = p.substring(4);
    if (q.method === 'OPTIONS') { r.writeHead(204, cors); return r.end(); }
    if (q.method === 'POST' && p === '/jetzt') { let body = ''; q.on('data', (d) => body += d); q.on('end', () => { seen.push('POST ' + body); made = true; r.writeHead(200, { ...cors, 'Content-Type': 'application/json' }); r.end(JSON.stringify({ url: '/nova/zz/laufzeit.mp3', neu: true })); }); return; }
    seen.push('GET ' + p);
    const ok = p === '/nova/zz/laufzeit.mp3' ? made : !p.includes('/00/');                 // Dateien unter /00/ gibt es (noch) nicht
    if (ok) { r.writeHead(200, { ...cors, 'Content-Type': 'audio/mpeg' }); r.end(sound); } else { r.writeHead(404, cors); r.end(); }
    return; }
  if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--autoplay-policy=no-user-gesture-required'] });
const ctx = await b.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
const errs = []; page.on('pageerror', (e) => errs.push(e.message)); page.on('console', (m) => { if (m.type() === 'error' && !/404|ERR_/.test(m.text())) errs.push(m.text()); });
await page.addInitScript(() => { window.__rlNovaServer = location.origin + '/__s'; try { localStorage.setItem('raumlicht.stimmeAn', '1'); } catch (e) {} });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
// 1. Aufgabe öffnen: Die App liest den Auftrag vor – mit der fertigen Datei, deren Name in der Liste der festen Texte steht
const list = JSON.parse(fs.readFileSync(path.join(dir, 'vorlesen/texte.json'), 'utf8')); const crypto = await import('crypto');
const keys = new Set(list.texte.map((t) => crypto.createHash('sha256').update('nova|de|' + t.text).digest('hex').substring(0, 32)));
await page.goto(url + '?ziel=level/5-2'); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 });
let last = null; for (let i = 0; i < 40 && !last; i++) { await page.waitForTimeout(500); last = await page.evaluate(() => window.__rlNova && window.__rlNova.last || null); if (!last && i === 8) await page.mouse.click(200, 300); }
check(!!last && last.seconds > 0.3, 'die App liest einen Auftrag mit Nova vor' + (last ? ` (${last.seconds.toFixed(1)} s)` : ''));
const first = seen.find((s) => s.startsWith('GET /nova/')); const key = first ? first.split('/').pop().replace('.mp3', '') : '';
check(keys.has(key) && first === `GET /nova/${key.substring(0, 2)}/${key}.mp3`, 'abgerufen wird genau die Datei aus der Liste der festen Texte: ' + first);
check(!seen.some((s) => s.startsWith('POST')), 'für feste Texte geht nichts an /jetzt');
// 2. derselbe Satz noch einmal: kommt aus dem Speicher des Browsers
const S = 'https://stimmen.mathenachhilfe-online.com'; const before = seen.length;
const again = await page.evaluate(([s, u]) => window.__rlNova.say(s, s + u, '', '', 0.5), [S, first.substring(4)]);
check(again === 'ok' && seen.length === before, 'schon gehörte Sätze kommen aus dem Speicher (kein neuer Abruf)');
// 2b. Sprechtempo: dieselbe Datei, schneller abgespielt, Tonhöhe bleibt
await page.mouse.click(200, 300);
const fast = await page.evaluate(([s, u]) => window.__rlNova.say(s, s + u, '', '', 0.5, 1.3).then((r) => ({ r, last: window.__rlNova.last })), [S, first.substring(4)]);
check(fast.r === 'ok' && fast.last.rate === 1.3 && seen.length === before, 'Sprechtempo 1,3×: gleiche Datei aus dem Speicher, schneller abgespielt (Tonhöhe bleibt)');
// 3. Laufzeit-Text: Datei fehlt → Server erzeugt sie → abspielen; gesendet werden nur Text und Sprache
const body = JSON.stringify({ text: 'Fast! Vektor a gleich 1, 2, 3.', sprache: 'de' });
const live = await page.evaluate(([s, bd]) => window.__rlNova.say(s, s + '/nova/00/fehltnoch.mp3', s + '/jetzt', bd, 0.5), [S, body]);
check(live === 'ok' && seen.includes('POST ' + body) && seen.includes('GET /nova/zz/laufzeit.mp3'), 'Laufzeit-Text: /jetzt bekommt nur Text und Sprache, die neue Datei wird gespielt');
// 4. zu langer Text (kein /jetzt) und Server weg: „fehlt“ – dann liest die Stimme des Geräts
const none = await page.evaluate(([s]) => window.__rlNova.say(s, s + '/nova/00/gibtsnicht.mp3', '', '', 0.5), [S]);
check(none === 'fehlt', 'ohne Datei und ohne /jetzt meldet Nova „fehlt“');
await page.evaluate(() => { window.__rlNovaServer = 'http://localhost:9'; });
const off = await page.evaluate(([s]) => window.__rlNova.say(s, s + '/nova/ab/offline.mp3', s + '/jetzt', '{}', 0.5), [S]);
check(off === 'fehlt', 'Server nicht erreichbar: Nova meldet „fehlt“');
check(errs.length === 0, errs.length ? 'Fehler: ' + errs.join(' | ') : 'keine Konsolenfehler');
await b.close(); server.close(); process.exit(fail ? 1 : 0);
