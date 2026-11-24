// „Eltern-Handy koppeln“ in der WebApp – Rundlauf: koppeln, der Bericht wird im Browser verschlüsselt und hochgeladen, und mit dem Schlüssel aus dem Link lässt er sich wieder entschlüsseln.
// Der Server sieht nur Unlesbares. Aufheben löscht auf dem Server und im Browser.
// node test/web-eltern.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path'; import { webcrypto } from 'node:crypto';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const store = {}; const saves = {}; const seen = [];
const server = http.createServer((q, r) => {
  const m = /^\/eltern\/v1\/b\/([A-Za-z0-9_-]{22})$/.exec(q.url.split('?')[0]);
  if (m) { let body = ''; q.on('data', (d) => body += d); q.on('end', () => { seen.push({ method: q.method, id: m[1], schreib: q.headers['x-schreib'], type: q.headers['content-type'], cookie: q.headers.cookie, body });
    if (q.method === 'PUT') { if (store[m[1]] && store[m[1]].schreib !== q.headers['x-schreib']) { r.writeHead(403); return r.end(); } store[m[1]] = { schreib: q.headers['x-schreib'], body }; r.writeHead(204); return r.end(); }
    if (q.method === 'DELETE') { delete store[m[1]]; delete saves[m[1]]; r.writeHead(204); return r.end(); } r.writeHead(405); r.end(); }); return; }
  const sv = /^\/eltern\/v1\/s\/([A-Za-z0-9_-]{22})$/.exec(q.url.split('?')[0]);
  if (sv) { let body = ''; q.on('data', (d) => body += d); q.on('end', () => { const e = { method: q.method, art: 's', id: sv[1], schreib: q.headers['x-schreib'], type: q.headers['content-type'], cookie: q.headers.cookie, body }; seen.push(e);
    const end = (st, text) => { e.status = st; r.writeHead(st, text ? { 'Content-Type': 'application/json' } : {}); r.end(text); };
    if (q.method === 'PUT') { const own = (store[sv[1]] || saves[sv[1]] || {}).schreib; if (own && own !== q.headers['x-schreib']) return end(403); if (body.length > 512 * 1024) return end(413); saves[sv[1]] = { schreib: q.headers['x-schreib'], body }; return end(204); }
    if (q.method === 'GET') return saves[sv[1]] ? end(200, JSON.stringify({ d: saves[sv[1]].body, t: new Date().toISOString() })) : end(404);
    end(405); }); return; }
  let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64');
async function decrypt(body, key) { const [iv, ct] = body.split('.'); const k = await webcrypto.subtle.importKey('raw', b64(key), 'AES-GCM', false, ['decrypt']);
  return new TextDecoder().decode(await webcrypto.subtle.decrypt({ name: 'AES-GCM', iv: b64(iv) }, k, b64(ct))); }
for (const modus of ['wasm', 'js']) {
  seen.length = 0; for (const k of Object.keys(store)) delete store[k]; for (const k of Object.keys(saves)) delete saves[k];
  const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage();
  const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await page.addInitScript((u) => { window.__rlElternServer = u + 'eltern/v1/b/'; }, url);
  const open = async (q) => { await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 }); await page.waitForTimeout(3000); };
  await open(`?modus=${modus}&ziel=eltern`); const before = await page.screenshot({ path: `${out}/${modus}-1-vorher.png` });
  check(seen.length === 0 && await page.evaluate(() => !localStorage.getItem('raumlicht.elternKopplung')), `${modus}: vor dem Koppeln ist nichts gespeichert und nichts gesendet`);
  // Knopf „Eltern-Handy koppeln“: die breite türkise Fläche unter dem Namensfeld – von unten nach oben suchen
  const y = await page.evaluate(async (png) => { const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
    const k = bmp.width / innerWidth; for (let yy = bmp.height - 1; yy > 0; yy--) { let n = 0; const d = g.getImageData(0, yy, bmp.width, 1).data; for (let x = 0; x < bmp.width; x++) { const i = x * 4; if (d[i] < 120 && d[i + 1] > 190 && d[i + 2] > 220) n++; } if (n > bmp.width * 0.5) return yy / k; } return -1; }, before.toString('base64'));
  // ein kleiner Lernstand, der mit in die Sicherung soll – und eine Geräte-Kennung, die nicht hinein darf
  await page.evaluate(() => { localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=2'); localStorage.setItem('raumlicht.proCode', 'PRUEFCODE'); localStorage.setItem('raumlicht.lernMonate', '2026-10:30:9:6'); localStorage.setItem('raumlicht.kiDevice', 'geraet-alt'); });
  await page.mouse.click(200, y - 20); await page.waitForTimeout(3500); await page.screenshot({ path: `${out}/${modus}-2-gekoppelt.png` });
  const saved = await page.evaluate(() => localStorage.getItem('raumlicht.elternKopplung') || ''); const [id, schreib, key] = saved.split('.');
  check(/^[A-Za-z0-9_-]{22}\.[A-Za-z0-9_-]{22}\.[A-Za-z0-9_-]{43}\./.test(saved), `${modus}: Kopplungswerte erzeugt (22, 22 und 43 Zeichen)` + (y < 0 ? ' – Knopf nicht gefunden' : ''));
  const put = seen.find((s) => s.method === 'PUT' && s.art !== 's');
  check(!!put && put.id === id && put.schreib === schreib && /^text\/plain/.test(put.type || '') && !put.cookie && /^[A-Za-z0-9_-]{16}\.[A-Za-z0-9_-]+$/.test(put.body) && put.body.length < 24000, `${modus}: Bericht hochgeladen – „<iv>.<verschlüsselt>“, mit Schreib-Geheimnis, ohne Cookie`);
  check(!!put && !put.body.includes('raumlicht') && !put.body.includes(key) && !JSON.stringify(seen).includes(key), `${modus}: der Server sieht weder Klartext noch den Schlüssel`);
  let report = null; try { report = JSON.parse(await decrypt(put.body, key)); } catch (e) { console.log('     Entschlüsseln: ' + e.message); }
  check(!!report && report.format === 1 && report.app === 'raumlicht' && report.appName === 'Raumlicht' && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$/.test(report.stand) && Array.isArray(report.fortschritt) && report.fortschritt.length === 1 && report.fortschritt[0].titel === 'Vektoren und Geometrie' && report.fortschritt[0].von === 92,
    `${modus}: mit dem Schlüssel aus dem Link lässt sich der Bericht entschlüsseln und lesen`);
  let wrong = false; try { await decrypt(put.body, key.slice(0, 20) + (key[20] === 'A' ? 'B' : 'A') + key.slice(21)); } catch (e) { wrong = true; }
  check(wrong, `${modus}: mit einem falschen Schlüssel geht es nicht`);
  // Kopplung bleibt nach neu Laden; „aufheben“ prüfen wir über die Logik-Tests – hier nur, dass nichts doppelt hochlädt
  const n = seen.filter((s) => s.method === 'PUT').length; await open(`?modus=${modus}`); await page.waitForTimeout(1500);
  check(seen.filter((s) => s.method === 'PUT').length === n && (await page.evaluate(() => localStorage.getItem('raumlicht.elternKopplung') || '')) === saved, `${modus}: nach neu Laden bleibt die Kopplung, und es geht nicht gleich wieder ein Bericht raus (10-Minuten-Grenze)`);
  // ── Sicherung: ging zusammen mit dem Bericht raus, verschlüsselt, mit Schreibrecht im Inneren und ohne Geräte-Kennung ──
  check(report && Array.isArray(report.verlauf) && report.verlauf.length === 1 && report.verlauf[0].monat === '2026-10' && report.verlauf[0].min === 30 && report.verlauf[0].richtigProzent === 66, `${modus}: der Bericht nennt den Monatsverlauf`);
  const sput = seen.find((s) => s.method === 'PUT' && s.art === 's');
  check(!!sput && sput.id === id && sput.schreib === schreib && sput.status === 204 && /^text\/plain/.test(sput.type || '') && !sput.cookie && /^[A-Za-z0-9_-]{16}\.[A-Za-z0-9_-]+$/.test(sput.body) && sput.body.length < 512 * 1024, `${modus}: Sicherung hochgeladen – verschlüsselt, mit Schreib-Geheimnis, ohne Cookie`);
  let save = null; try { save = JSON.parse(await decrypt(sput.body, key)); } catch (e) { console.log('     Entschlüsseln: ' + e.message); }
  check(!!save && save.format === 1 && save.app === 'raumlicht' && save.schreib === schreib && save.daten.sterne === '1-1=3;1-2=2' && save.daten.proCode === 'PRUEFCODE' && !('kiDevice' in save.daten) && !('elternKopplung' in save.daten)
    && !JSON.stringify(save).includes('geraet-alt') && !JSON.stringify(save).includes(key), `${modus}: die Sicherung enthält den Lernstand und das Schreibrecht, aber weder Geräte-Kennung noch Schlüssel`);
  check(seen.filter((s) => s.method === 'PUT' && s.art === 's').length === 1, `${modus}: ohne Änderung keine zweite Sicherung`);
  check(errs.length === 0, errs.length ? `${modus}: Fehler: ` + errs[0] : `${modus}: keine Fehler`);
  await ctx.close();
  // ── Wiederherstellen in einem frischen Profil (wie ein neues Gerät) ──
  const ctx2 = await b.newContext({ viewport: { width: 400, height: 1500 }, serviceWorkers: 'block' }); const p2 = await ctx2.newPage(); const errs2 = []; p2.on('pageerror', (e) => errs2.push(e.message));
  await p2.addInitScript((u) => { window.__rlElternServer = u + 'eltern/v1/b/'; }, url);
  const open2 = async (q) => { await p2.goto(url + q); await p2.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 }); await p2.waitForTimeout(3000); };
  await open2(`?modus=${modus}&ziel=eltern`); const shot = await p2.screenshot({ path: `${out}/${modus}-3-neues-geraet.png` });
  // Knopf „Fortschritt wiederherstellen“: die breite grüne Fläche – von unten nach oben suchen; das Feld für den Link liegt direkt darüber
  const gy = await p2.evaluate(async (png) => { const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
    const k = bmp.width / innerWidth; for (let yy = bmp.height - 1; yy > 0; yy--) { let n = 0; const d = g.getImageData(0, yy, bmp.width, 1).data; for (let x = 0; x < bmp.width; x++) { const i = x * 4; if (d[i] < 150 && d[i + 1] > 215 && d[i + 2] > 110 && d[i + 2] < 205) n++; } if (n > bmp.width * 0.5) return yy / k; } return -1; }, shot.toString('base64'));
  const before2 = seen.length; await p2.mouse.click(200, gy - 85); await p2.waitForTimeout(600); await p2.keyboard.insertText(`${url}eltern-bericht/#${id}.${key}`); await p2.waitForTimeout(600);
  await p2.screenshot({ path: `${out}/${modus}-4-link-eingefuegt.png` }); await p2.mouse.click(200, gy - 20);
  await p2.waitForFunction(() => !!localStorage.getItem('raumlicht.elternKopplung'), null, { timeout: 20000 }).catch(() => {});
  await p2.waitForFunction(() => document.getElementById('start') && document.getElementById('start').style.display === 'none', null, { timeout: 120000 }).catch(() => {}); await p2.waitForTimeout(5000);
  await p2.screenshot({ path: `${out}/${modus}-5-wiederhergestellt.png` });
  const got = await p2.evaluate(() => ({ sterne: localStorage.getItem('raumlicht.sterne'), pro: localStorage.getItem('raumlicht.proCode'), monate: localStorage.getItem('raumlicht.lernMonate'), k: localStorage.getItem('raumlicht.elternKopplung'), dev: localStorage.getItem('raumlicht.kiDevice') }));
  check(seen.slice(before2).some((s) => s.method === 'GET' && s.art === 's' && s.id === id && !s.schreib && s.status === 200), `${modus}: neues Gerät holt die Sicherung ohne Schreibrecht` + (gy < 0 ? ' – Knopf nicht gefunden' : ''));
  check(got.sterne === '1-1=3;1-2=2' && got.pro === 'PRUEFCODE' && got.monate === '2026-10:30:9:6', `${modus}: Rundlauf – derselbe Lernstand ist wieder da (Sterne, Code, Monatssummen)`);
  check(got.k === `${id}.${schreib}.${key}.` && got.dev !== 'geraet-alt', `${modus}: das neue Gerät ist wieder gekoppelt, die alte Geräte-Kennung kam nicht mit`);
  check(seen.slice(before2).some((s) => s.method === 'PUT' && s.id === id && s.schreib === schreib), `${modus}: danach schreibt das neue Gerät erfolgreich weiter`);
  const foreign = await fetch(`${url}eltern/v1/s/${id}`, { method: 'PUT', headers: { 'X-Schreib': 'F'.repeat(22), 'Content-Type': 'text/plain' }, body: 'A'.repeat(16) + '.' + 'B'.repeat(40) });
  check(foreign.status === 403, `${modus}: ein fremdes Schreib-Geheimnis wird abgelehnt (403)`);
  check(errs2.length === 0, errs2.length ? `${modus}: Fehler nach dem Wiederherstellen: ` + errs2[0] : `${modus}: keine Fehler nach dem Wiederherstellen`);
  await ctx2.close();
}
await b.close(); server.closeAllConnections(); server.close(); process.exit(fail ? 1 : 0);
