// Umzug auf die gemeinsame Adresse: Die alte Adresse verschlüsselt den Lernstand, legt ihn als Einmal-Sicherung ab und leitet weiter; die neue holt ihn,
// spielt ihn nur ein, wenn dort noch nichts liegt, und löscht die Sicherung. Dazu: Raumlicht räumt unter der gemeinsamen Adresse nur eigene Zwischenspeicher weg.
// Drei Server auf dem eigenen Rechner: alte Adresse, neue Adresse (Unterordner /raumlicht/) und der Sicherungs-Server.
// node test/web-umzug.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const TYPE = (f) => f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') || f.endsWith('.webmanifest') ? 'application/json' : 'application/octet-stream';
const files = (prefix) => http.createServer((q, r) => { if (q.method === 'POST' && q.url.split('?')[0] === '/zaehler/v1/e') { let b = ''; q.on('data', (d) => b += d); q.on('end', () => { zaehl.push({ prefix, body: b, type: q.headers['content-type'], cookie: q.headers.cookie }); r.writeHead(204); r.end(); }); return; }
  if (neuWeg && prefix !== '/') { r.writeHead(503); return r.end('noch nicht da'); } let p = decodeURIComponent(q.url.split('?')[0]); if (!p.startsWith(prefix)) { r.writeHead(404); return r.end(); } p = '/' + p.slice(prefix.length); if (p === '/') p = '/index.html';
  const f = path.join(dir, p); fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': TYPE(f) }); r.end(d); } }); });
const store = {}; const seen = []; const zaehl = []; let kaputt = false, neuWeg = false;
const sicherung = http.createServer((q, r) => { const cors = { 'Access-Control-Allow-Origin': q.headers.origin || '*', 'Access-Control-Allow-Methods': 'GET, PUT, DELETE, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type, X-Schreib' };
  const m = /^\/eltern\/v1\/s\/([A-Za-z0-9_-]{22})$/.exec(q.url.split('?')[0]); let body = ''; q.on('data', (d) => body += d); q.on('end', () => {
    if (q.method === 'OPTIONS') { r.writeHead(204, cors); return r.end(); }
    const e = { method: q.method, url: q.url, id: m && m[1], schreib: q.headers['x-schreib'], type: q.headers['content-type'], cookie: q.headers.cookie, origin: q.headers.origin, body }; seen.push(e);
    const end = (st, text) => { e.status = st; r.writeHead(st, { ...cors, ...(text ? { 'Content-Type': 'application/json' } : {}) }); r.end(text); };
    if (!m) return end(404); if (kaputt) return end(500);
    if (q.method === 'PUT') { if (store[m[1]] && store[m[1]].schreib !== q.headers['x-schreib']) return end(403); if (!/^[A-Za-z0-9_-]{16}\.[A-Za-z0-9_-]+$/.test(body)) return end(400); store[m[1]] = { schreib: q.headers['x-schreib'], body }; return end(204); }
    if (q.method === 'GET') return store[m[1]] ? end(200, JSON.stringify({ d: store[m[1]].body, t: new Date().toISOString() })) : end(404, '{"fehler":"keine_sicherung"}');
    if (q.method === 'DELETE') { if (store[m[1]] && store[m[1]].schreib !== q.headers['x-schreib']) return end(403); delete store[m[1]]; return end(204); }
    end(405); }); });
const sA = files('/'), sB = files('/raumlicht/'); await Promise.all([sA, sB, sicherung].map((s) => new Promise((ok) => s.listen(0, ok))));
const ALT = `http://localhost:${sA.address().port}/`, NEU = `http://localhost:${sB.address().port}/raumlicht/`, SRV = `http://localhost:${sicherung.address().port}/eltern/v1/s/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const neu = async (sw = 'block') => { const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: sw }); const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await ctx.addInitScript(([altPort, ziel, server]) => { window.__rlUmzugTest = { alt: location.port === altPort, ziel, server, zaehle: true }; }, [String(sA.address().port), NEU, SRV]); return { ctx, page, errs }; };
const gestartet = (page) => page.waitForFunction(() => document.getElementById('start') && document.getElementById('start').style.display === 'none', null, { timeout: 120000 });
const stand = (page) => page.evaluate(() => { const o = {}; for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k.startsWith('raumlicht.')) o[k] = localStorage.getItem(k); } return o; });
const fuellen = async (page, wo, daten) => { await page.goto(wo + 'diagnose.html'); await page.evaluate((d) => { for (const k in d) localStorage.setItem(k, d[k]); }, daten); };
const DATEN = { 'raumlicht.sterne': '1-1=3;1-2=2;18-3=3', 'raumlicht.proCode': 'PRUEFCODE', 'raumlicht.lernMonate': '2026-10:30:9:6', 'raumlicht.ton': '0', 'raumlicht.mitschrift': 'nur dieser Browser' };

for (const modus of ['wasm', 'js']) {
  // ── 1 · Umzug gelingt: Lernstand kommt an, Ziel und Raute-Teile bleiben erhalten, die Sicherung ist danach gelöscht ──
  seen.length = 0; zaehl.length = 0; for (const k in store) delete store[k]; kaputt = false;
  { const { ctx, page, errs } = await neu(); await fuellen(page, ALT, DATEN);
    await page.goto(ALT + `?modus=${modus}&ziel=einstellungen#anker=1`); await page.waitForURL((u) => u.href.startsWith(NEU), { timeout: 30000 }).catch(() => {}); await gestartet(page).catch(() => {}); await page.waitForTimeout(2500);
    await page.screenshot({ path: `${out}/umzug-${modus}-1-angekommen.png` });
    const url = page.url(), da = await stand(page), put = seen.find((s) => s.method === 'PUT');
    // „?ziel=“ nimmt die App nach dem Öffnen selbst aus der Adresse – dass es ankam, zeigt der offene Bildschirm (nicht die Startseite)
    const offen = await page.evaluate(() => window.__rlHome === false);
    check(url === NEU + `?modus=${modus}#anker=1` && offen, `${modus}: nach dem Umzug an der neuen Adresse – Ziel geöffnet, Raute-Teil erhalten, „umzug“ aus der Adresse entfernt` + (url === NEU + `?modus=${modus}#anker=1` && offen ? '' : ' – ' + url + ' Ziel offen: ' + offen));
    check(da['raumlicht.sterne'] === DATEN['raumlicht.sterne'] && da['raumlicht.proCode'] === 'PRUEFCODE' && da['raumlicht.lernMonate'] === DATEN['raumlicht.lernMonate'] && da['raumlicht.ton'] === '0', `${modus}: Lernstand, Freischalt-Code und Einstellungen sind an der neuen Adresse da`);
    check(da['raumlicht.mitschrift'] !== 'nur dieser Browser', `${modus}: Merker des alten Browsers ziehen nicht mit`);
    check(!!put && put.status === 204 && /^text\/plain/.test(put.type || '') && !put.cookie && /^[A-Za-z0-9_-]{16}\.[A-Za-z0-9_-]+$/.test(put.body) && !put.body.includes('PRUEFCODE') && !put.body.includes('sterne'), `${modus}: abgelegt wird nur Verschlüsseltes – ohne Cookie, ohne Klartext`);
    check(seen.filter((s) => s.method === 'GET' && s.status === 200).length === 1 && seen.some((s) => s.method === 'DELETE' && s.status === 204 && s.schreib === put?.schreib) && Object.keys(store).length === 0, `${modus}: die neue Adresse holt die Sicherung einmal und löscht sie sofort`);
    check(seen.every((s) => !/[A-Za-z0-9_-]{43}/.test(s.url) && !(s.method !== 'PUT' && /[A-Za-z0-9_-]{43}/.test(s.body))), `${modus}: der Schlüssel geht an keinen Server`);
    check(errs.length === 0, errs.length ? `${modus}: Fehler: ` + errs[0] : `${modus}: keine Fehler beim Umzug`);
    // alte Adresse noch einmal: Dort liegt der Stand weiter (Rückweg) – das Ziel hat schon Daten und wird nicht überschrieben
    await page.evaluate(() => localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=3;1-3=3;18-3=3'));                          // an der neuen Adresse weitergelernt
    await page.goto(ALT + `?modus=${modus}`); await page.waitForURL((u) => u.href.startsWith(NEU), { timeout: 30000 }).catch(() => {}); await gestartet(page).catch(() => {}); await page.waitForTimeout(2000);
    const da2 = await stand(page);
    check(da2['raumlicht.sterne'] === '1-1=3;1-2=3;1-3=3;18-3=3' && Object.keys(store).length === 0 && page.url() === NEU + `?modus=${modus}`, `${modus}: Ziel hat schon einen Lernstand → nichts überschrieben, Sicherung trotzdem gelöscht`);
    // Zählpunkt „umzug“: genau einmal, anonym, an die Zähl-Adresse der neuen Herkunft – und nicht beim zweiten Besuch, wo nichts übernommen wurde
    { const o = zaehl.length === 1 ? JSON.parse(zaehl[0].body) : null;
      check(!!o && zaehl[0].prefix === '/raumlicht/' && Object.keys(o).sort().join() === 'app,ev,v' && o.app === 'raumlicht' && o.ev === 'umzug' && /^\d+\.\d+\.\d+$/.test(o.v) && /^text\/plain/.test(zaehl[0].type || '') && !zaehl[0].cookie && !zaehl[0].body.includes('PRUEFCODE') && !zaehl[0].body.includes('sterne'),
        `${modus}: Zählpunkt „umzug“ – einmal, nur App, Ereignis und Version, ohne Kennung und Lernstand, an die neue Herkunft` + (zaehl.length !== 1 ? ` (${zaehl.length} Aufrufe)` : '')); }
    await ctx.close(); }

  // ── 2 · Server nicht erreichbar: nicht weiterleiten, Raumlicht läuft unter der alten Adresse; beim nächsten Start klappt es ──
  seen.length = 0; for (const k in store) delete store[k]; kaputt = true;
  { const { ctx, page, errs } = await neu(); await fuellen(page, ALT, DATEN);
    await page.goto(ALT + `?modus=${modus}`); await gestartet(page).catch(() => {}); await page.waitForTimeout(1500);
    check(page.url().startsWith(ALT) && (await stand(page))['raumlicht.sterne'] === DATEN['raumlicht.sterne'] && await page.evaluate(() => document.getElementById('start').style.display === 'none'), `${modus}: Ablegen scheitert → keine Weiterleitung, Raumlicht startet unter der alten Adresse, Lernstand unberührt`);
    kaputt = false; await page.goto(ALT + `?modus=${modus}`); await page.waitForURL((u) => u.href.startsWith(NEU), { timeout: 30000 }).catch(() => {}); await gestartet(page).catch(() => {}); await page.waitForTimeout(2000);
    check(page.url() === NEU + `?modus=${modus}` && (await stand(page))['raumlicht.sterne'] === DATEN['raumlicht.sterne'], `${modus}: beim nächsten Start gelingt der Umzug`);
    check(errs.length === 0, errs.length ? `${modus}: Fehler: ` + errs[0] : `${modus}: keine Fehler beim gescheiterten und wiederholten Umzug`);
    await ctx.close(); }

  // ── 2b · Neue Adresse noch nicht erreichbar (Fehlerseite): gar nicht erst ablegen, nicht weiterleiten – Raumlicht läuft unter der alten Adresse ──
  seen.length = 0; for (const k in store) delete store[k]; neuWeg = true;
  { const { ctx, page, errs } = await neu(); await fuellen(page, ALT, DATEN);
    await page.goto(ALT + `?modus=${modus}`); await gestartet(page).catch(() => {}); await page.waitForTimeout(1500);
    check(page.url().startsWith(ALT) && seen.length === 0 && (await stand(page))['raumlicht.sterne'] === DATEN['raumlicht.sterne'] && await page.evaluate(() => document.getElementById('start').style.display === 'none'),
      `${modus}: neue Adresse antwortet mit einem Fehler → nichts abgelegt, keine Weiterleitung, Raumlicht startet unter der alten Adresse`);
    check(errs.length === 0, errs.length ? `${modus}: Fehler: ` + errs[0] : `${modus}: keine Fehler, solange die neue Adresse fehlt`);
    await ctx.close(); } neuWeg = false;

  // ── 2b · Zähl-Schalter aus: Der Umzug klappt, „umzug“ wird NICHT gesendet ──
  seen.length = 0; zaehl.length = 0; for (const k in store) delete store[k]; kaputt = false;
  { const { ctx, page, errs } = await neu(); await fuellen(page, ALT, { ...DATEN, 'raumlicht.zaehlen': '0' });
    await page.goto(ALT + `?modus=${modus}`); await page.waitForURL((u) => u.href.startsWith(NEU), { timeout: 30000 }).catch(() => {}); await gestartet(page).catch(() => {}); await page.waitForTimeout(2500);
    check(zaehl.length === 0 && (await stand(page))['raumlicht.sterne'] === DATEN['raumlicht.sterne'], `${modus}: Zähl-Schalter aus → Umzug klappt, „umzug“ wird nicht gesendet`); await ctx.close(); }
  // ── 3 · Nichts mitzunehmen: gleich hinüber, ohne Sicherung; und die neue Adresse allein fragt den Server nie ──
  seen.length = 0; for (const k in store) delete store[k];
  { const { ctx, page } = await neu(); await page.goto(ALT + `?modus=${modus}&ziel=galton`); await page.waitForURL((u) => u.href.startsWith(NEU), { timeout: 30000 }).catch(() => {}); await gestartet(page).catch(() => {}); await page.waitForTimeout(1500);
    check(page.url() === NEU + `?modus=${modus}` && await page.evaluate(() => window.__rlHome === false) && seen.length === 0, `${modus}: ohne Lernstand geht es direkt zur neuen Adresse (Ziel geöffnet) – nichts wird abgelegt`);
    await page.goto('about:blank'); await page.goto(NEU + `?modus=${modus}`); await gestartet(page).catch(() => {}); await page.waitForTimeout(1000);
    check(seen.length === 0 && page.url() === NEU + `?modus=${modus}`, `${modus}: die neue Adresse allein fragt den Sicherungs-Server nicht`);
    await page.goto('about:blank');
    // erfundener oder alter Umzugs-Link: keine Sicherung da → Start wie gewohnt, Adresse bereinigt
    await page.goto(NEU + `?modus=${modus}#umzug=${'A'.repeat(22)}.${'B'.repeat(43)}.${'C'.repeat(22)}`); await gestartet(page).catch(() => {}); await page.waitForTimeout(1000);
    check(page.url() === NEU + `?modus=${modus}` && Object.keys(await stand(page)).every((k) => !/sterne|proCode/.test(k)), `${modus}: Umzugs-Link ohne Sicherung → normaler Start, nichts eingespielt, Adresse bereinigt`);
    await ctx.close(); }
}

// ── 4 · Gemeinsame Adresse: Raumlicht räumt nur eigene Zwischenspeicher weg – beim Update und beim Notfall-Knopf der Diagnose-Seite ──
{ const { ctx, page } = await neu('allow'); await page.goto(NEU + 'diagnose.html');
  await page.evaluate(async () => { const c = await caches.open('andere-app-1'); await c.put('/andere/', new Response('fremd')); const d = await caches.open('raumlicht-0.0.1'); await d.put('./alt', new Response('alt')); });
  await page.goto(NEU); await gestartet(page).catch(() => {}); await page.evaluate(() => navigator.serviceWorker.ready); await page.waitForTimeout(4000);
  let ks = await page.evaluate(() => caches.keys());
  check(ks.includes('andere-app-1') && !ks.includes('raumlicht-0.0.1') && ks.some((k) => /^raumlicht-\d/.test(k)), 'Service Worker: alter eigener Speicher weg, fremder Speicher bleibt (' + ks.join(', ') + ')');
  check((await page.evaluate(() => caches.open('andere-app-1').then((c) => c.match('/andere/')).then((r) => r && r.text()))) === 'fremd', 'Service Worker: Inhalt des fremden Speichers unverändert');
  await page.goto(NEU + 'diagnose.html'); await page.click('#reset'); await page.waitForURL((u) => !u.href.includes('diagnose'), { timeout: 20000 }).catch(() => {}); await page.waitForTimeout(1500);
  ks = await page.evaluate(() => caches.keys());
  check(ks.includes('andere-app-1'), 'Diagnose-Seite „zurücksetzen“: fremder Speicher bleibt (' + ks.join(', ') + ')');
  await ctx.close(); }
await b.close(); for (const s of [sA, sB, sicherung]) { s.closeAllConnections(); s.close(); } process.exit(fail ? 1 : 0);
