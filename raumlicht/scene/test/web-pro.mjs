// Raumlicht Pro in der WebApp: Pro-Teile sind ohne Code gesperrt, freie Teile offen; der Link aus der E-Mail (?pro=<Code>) schaltet frei und bleibt gemerkt; ein falscher Code nicht.
// node test/web-pro.mjs <webappOrdner> <Datei mit einem gültigen Code> <Ordner für Bildschirmfotos>     (der Code steht nicht im Repository)
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, codeFile, out] = process.argv; const code = fs.readFileSync(codeFile, 'utf8').trim(); const plain = code.replace(/-/g, '');
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const ctx = await b.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
const errs = []; page.on('pageerror', (e) => errs.push(e.message)); page.on('console', (m) => { if (m.type() === 'error') errs.push(m.text()); });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const open = async (q, name) => { await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }); await page.waitForTimeout(2800); const buf = await page.screenshot({ path: `${out}/pro-${name}.png` }); return buf; };
const same = (a, b) => Buffer.compare(a, b) === 0;
const stored = () => page.evaluate(() => localStorage.getItem('raumlicht.proCode') || '');
const log = () => page.evaluate(() => localStorage.getItem('raumlicht.mitschrift') || '');
// 1. ohne Code: Aufgabe 9-5 (Pro) zeigt die Pro-Seite, Aufgabe 2-1 (frei) die Aufgabe
const locked = await open('?ziel=level/9-5', '1-gesperrt'); const free = await open('?ziel=level/2-1', '2-frei');
check((await stored()) === '', 'ohne Code ist nichts freigeschaltet');
// 2. falscher Code im Link: bleibt gesperrt
// (aus dem echten Code abgeleitet: ein Zeichen in der Mitte vertauscht – im Repository steht kein Code)
const wrong = plain.substring(0, 60) + (plain[60] === 'A' ? 'B' : 'A') + plain.substring(61);
await open('?pro=' + wrong, '3-falscher-code');
check((await stored()) === '' && /Freischalt-Link: Code nicht erkannt/.test(await log()), 'falscher Code im Link wird abgelehnt');
// 3. echter Code im Link (ohne Bindestriche, wie in der E-Mail): freigeschaltet und gemerkt
await open('?pro=' + plain, '4-freigeschaltet');
{ const st = await stored(), lg = await log(); if (!(st.replace(/-/g, '') === plain)) console.log('     gemerkt: ' + (st ? st.length + ' Zeichen' : 'nichts') + ' · Mitschrift: ' + lg.split('\n').filter((l) => /Freischalt/.test(l)).slice(-3).join(' | '));
  check(st.replace(/-/g, '') === plain && /Freischalt-Link: Code angenommen/.test(lg), 'echter Code im Link schaltet frei und wird gemerkt'); }
// 4. danach ist die Pro-Aufgabe offen – auch nach neu Laden; die Startseite zeigt das Abzeichen
const unlocked = await open('?ziel=level/9-5', '5-offen'); const home = await open('', '6-start');
check(!same(locked, unlocked), 'Aufgabe 9-5 sieht nach dem Freischalten anders aus als die Sperrseite');
check((await stored()).replace(/-/g, '') === plain, 'der Code bleibt nach dem Neuladen gemerkt');
check(errs.length === 0, errs.length ? 'Fehler: ' + errs.slice(0, 3).join(' | ') : 'keine Konsolenfehler');
await b.close(); server.close(); process.exit(fail ? 1 : 0);
