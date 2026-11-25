// KI-Tutor in der WebApp, gegen die echte Server-Funktion (raumlicht/server/raumlicht-ki.js) mit nachgestelltem KI-Dienst:
// Link aus der E-Mail (?ki=<Code>) wird gemerkt, nach einem Fehlversuch fragt „Erklär’s mir anders“ den Server, die Antwort erscheint.
// node test/web-ki.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path'; import { fileURLToPath } from 'url';
const { raumlichtKi } = require(path.join(path.dirname(fileURLToPath(import.meta.url)), '../../server/raumlicht-ki.js'));
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
// Die WebApp prüft Codes mit dem echten Schlüssel – hier gibt es keinen echten Code, also läuft der Test über die Proben (ohne Code) und einen abgelehnten Link.
const asked = []; const seen = [];
const ki = raumlichtKi({ apiKey: 'test', publicKeyHex: '03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8',
  fetch: async (u, init) => { asked.push(JSON.parse(init.body)); return { ok: true, status: 200, json: async () => ({ content: [{ type: 'text', text: 'Stell dir den Punkt als Adresse im Raum vor: erst nach vorn, dann zur Seite, dann nach oben.' }], stop_reason: 'end_turn' }) }; } });
const server = http.createServer((q, r) => {
  if (q.url.startsWith('/api/raumlicht/ki')) { let body = ''; q.on('data', (d) => body += d); q.on('end', async () => { let j = null; try { j = JSON.parse(body); } catch { }
    seen.push(j); const a = await ki(j, { adresse: '127.0.0.1' }); r.writeHead(a.status, { 'Content-Type': 'application/json' }); r.end(JSON.stringify(a.json)); }); return; }
  let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const ctx = await b.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
await page.addInitScript((u) => { window.__rlKiServer = u + 'api/raumlicht/ki'; }, url);
const errs = []; page.on('pageerror', (e) => errs.push(e.message)); page.on('console', (m) => { if (m.type() === 'error') errs.push(m.text()); });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const shot = (name) => page.screenshot({ path: `${out}/${name}.png` });
const open = async (q, name) => { await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }); await page.waitForTimeout(2800); return shot(name); };
const log = () => page.evaluate(() => localStorage.getItem('raumlicht.mitschrift') || '');
// 1. Link mit einem Code, den der echte Schlüssel nicht kennt: nichts gemerkt, die KI-Seite steht mit der Eingabe bereit
await open('?ki=RL04208ABCDE', '1-ki-seite');
check((await page.evaluate(() => localStorage.getItem('raumlicht.kiCode') || '')) === '' && /KI-Link: Code nicht erkannt/.test(await log()), 'falscher Code im KI-Link wird abgelehnt, die KI-Seite öffnet sich');
// 2. Aufgabe 1-2: vor dem ersten Versuch kein KI-Knopf (ein Tipper an seine Stelle fragt nichts)
const before = await open('?ziel=level/1-2', '2-aufgabe');
// 3. Fehlversuch („▶ Los“ unten rechts), dann „Erklär’s mir anders“: Die Stelle des Knopfs wird gesucht – er ist das einzige Gelbe rechts über der Formel
await page.mouse.click(310, 815); await page.waitForTimeout(6000); const wrong = await shot('3-fehlversuch');
check(Buffer.compare(before, wrong) !== 0, 'nach „Los“ sieht die Aufgabe anders aus (Rückmeldung)');
const spot = await page.evaluate(async (png) => {
  // das Bildschirmfoto im Browser auslesen (die Zeichenfläche selbst liegt gekapselt)
  const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
  const k = bmp.width / innerWidth; const d = g.getImageData(0, 0, bmp.width, bmp.height).data; let sx = 0, sy = 0, n = 0;
  // Schrift des Knopfs: Gelb (Q.Yellow) in der unteren Bildschirmhälfte, oberhalb der Regler
  // … und davon nur die oberste gelbe Zeile (darunter stehen die gelben Zahlen der Formel)
  let top = -1;
  for (let y = Math.floor(bmp.height * 0.45); y < Math.floor(bmp.height * 0.8) && (top < 0 || y < top + 16 * k); y++) for (let x = Math.floor(bmp.width * 0.25); x < bmp.width; x++) { const i = (y * bmp.width + x) * 4; if (d[i] > 230 && d[i + 1] > 190 && d[i + 1] < 235 && d[i + 2] < 120) { if (top < 0) top = y; sx += x; sy += y; n++; } }
  return n ? { x: sx / n / k, y: sy / n / k, n } : null; }, wrong.toString('base64'));
console.log('     Knopf gefunden bei', JSON.stringify(spot));
if (spot) { await page.mouse.click(spot.x, spot.y); await page.waitForTimeout(3500); }
await shot('4-ki-antwort');
check(await page.evaluate(() => { const e = document.querySelector('#ki-erzeugt [data-ki-erzeugt="true"]'); return !!e && e.getAttribute('aria-label') === 'KI-erzeugte Antwort' && !!e.getAttribute('aria-description') && e.dataset.kiErzeugt === 'true' && e.dataset.kiAnbieter === 'Anthropic' && e.dataset.kiHinweis === 'KI-erzeugt' && /^KI-erzeugt: ./.test(e.textContent); }),
  'jede KI-Antwort ist maschinenlesbar gekennzeichnet: data-ki-erzeugt="true", Anbieter, Hinweis „KI-erzeugt“ (Art. 50 Abs. 2)');
check(seen.length === 1 && seen[0] && seen[0].app === 'raumlicht' && seen[0].art === 'erklaerung' && seen[0].aufgabe.id === '1-2' && /^[0-9a-f]{32}$/.test(seen[0].geraet) && seen[0].code === '' && seen[0].eingabe.includes(' = '),
  'die WebApp fragt den Server: Erklärung zu Aufgabe 1-2 mit Eingabe und Geräte-Kennung' + (seen.length ? '' : ' (keine Anfrage angekommen)'));
check(asked.length === 1 && /<eingabe>/.test(asked[0].messages[0].content) && !JSON.stringify(asked[0]).includes(seen[0]?.geraet), 'der Server reicht Aufgabe und Eingabe an den KI-Dienst weiter – ohne Geräte-Kennung');
// 4. (nur mit Code-Datei als drittem Argument – der Code steht nicht im Repository) echter Pass im Link: gemerkt, und die nächste Frage geht mit dem Code an den Server
if (process.argv[4]) { const plain = fs.readFileSync(process.argv[4], 'utf8').trim().replace(/-/g, '');
  await page.goto('about:blank'); await open('#ki=' + plain, '5-pass-aktiv');   // neue Linkform hinter dem „#“; die alte „?ki=“ prüft Schritt 1
  check((await page.evaluate(() => localStorage.getItem('raumlicht.kiCode') || '')).replace(/-/g, '') === plain && /KI-Link: Code angenommen/.test(await log()), 'echter KI-Pass im Link „#ki=“ wird angenommen und gemerkt');
  check(!(await page.evaluate(() => location.href)).includes(plain.slice(0, 20)), 'der KI-Code steht danach nicht mehr in der Adresszeile'); }
check(errs.length === 0, errs.length ? 'Fehler: ' + errs.slice(0, 3).join(' | ') : 'keine Konsolenfehler');
await b.close(); server.close(); process.exit(fail ? 1 : 0);
