// Umzug auf die gemeinsame Adresse im echten Browser prüfen (2.13.0):
//   alte Adresse experimentopia-test.… → lernen-test.…/experimentopia/ mit dem Lernstand, über den echten Test-Server (trainer-test).
// Die beiden App-Adressen liefert dieses Skript selbst aus webapp/ (abgefangen), die Aufrufe an trainer-test gehen wirklich zum Server.
//   NODE_USE_ENV_PROXY=1 node tools/web-umzug.mjs        (nach ./gradlew :web:webapp)
import { createRequire } from 'node:module';
import path from 'node:path';
import fs from 'node:fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', 'webapp');
const ALT = 'https://experimentopia-test.mathenachhilfe-online.com/';
const NEU = 'https://lernen-test.mathenachhilfe-online.com/experimentopia/';
const SERVER = 'https://trainer-test.mathenachhilfe-online.com';
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
const typ = { html: 'text/html', js: 'text/javascript', json: 'application/json', wasm: 'application/wasm', ttf: 'font/ttf', png: 'image/png', webmanifest: 'application/manifest+json' };

const LERNSTAND = { 'experimentopia.ergebnisse': '{"x1":3,"x2":2}', 'experimentopia.forscherbuch': '[{"t":"Mein Schatten"}]', 'experimentopia.sterne': '17' };
const ELTERN = '{"id":"AAAAAAAAAAAAAAAAAAAAAA","write":"BBBBBBBBBBBBBBBBBBBBBB","key":"' + 'C'.repeat(43) + '","test":true}';

const b = await chromium.launch();
async function lauf({ alt = {}, neu = {}, neuWeg = false, start = ALT + '#thema=optik' } = {}) {
  const ctx = await b.newContext(); const log = { put: [], get: [], del: [] };
  await ctx.addInitScript(({ alt, neu }) => {
    const seed = location.host.startsWith('experimentopia-test') ? alt : location.host.startsWith('lernen-test') ? neu : {};
    if (!sessionStorage.getItem('geseedet')) { sessionStorage.setItem('geseedet', '1'); for (const k in seed) localStorage.setItem(k, seed[k]); }
  }, { alt, neu });
  // App-Dateien für alte und neue Adresse aus webapp/
  await ctx.route(/^https:\/\/(experimentopia-test\.mathenachhilfe-online\.com\/|lernen-test\.mathenachhilfe-online\.com\/experimentopia\/)/, async (rt) => {
    const u = new URL(rt.request().url());
    if (neuWeg && u.host.startsWith('lernen-test')) return rt.abort('connectionrefused');
    let p = u.pathname.replace(/^\/experimentopia\//, '/').replace(/^\//, '') || 'index.html';
    if (p.endsWith('/')) p += 'index.html';
    const f = path.join(root, p);
    if (!fs.existsSync(f)) return rt.fulfill({ status: 404, body: 'fehlt' });
    await rt.fulfill({ status: 200, contentType: typ[p.split('.').pop()] || 'application/octet-stream', body: fs.readFileSync(f) });
  });
  // Test-Server wirklich ansprechen (mit Origin der Seite, CORS-Antwort durchreichen)
  await ctx.route(SERVER + '/eltern/**', async (rt) => {
    const r = rt.request(), h = r.headers(), m = r.method();
    const hdr = {}; if (h['x-schreib']) hdr['X-Schreib'] = h['x-schreib']; if (h['content-type']) hdr['Content-Type'] = h['content-type']; hdr['Origin'] = h['origin'] || '';
    const res = await fetch(r.url(), { method: m, headers: hdr, body: m === 'PUT' ? r.postData() : undefined });
    const body = await res.text();
    ({ PUT: log.put, GET: log.get, DELETE: log.del })[m]?.push({ url: r.url(), status: res.status, origin: hdr.Origin });
    await rt.fulfill({ status: res.status, body, headers: { 'access-control-allow-origin': res.headers.get('access-control-allow-origin') || '', 'content-type': res.headers.get('content-type') || 'text/plain' } });
  });
  const p = await ctx.newPage();
  p.on('framenavigated', (f) => { if (f === p.mainFrame() && f.url().includes('#umzug=')) log.weiter = f.url(); });
  await p.goto(start);
  await p.waitForFunction(() => document.getElementById('start') && document.getElementById('start').dataset.fertig === '1', null, { timeout: 120000 });
  await p.waitForTimeout(1500);
  const st = await p.evaluate(() => ({ url: location.href, ls: Object.fromEntries(Object.keys(localStorage).map((k) => [k, localStorage.getItem(k)])) }));
  return { ctx, p, st, log };
}
const lernstand = (ls) => Object.keys(LERNSTAND).every((k) => ls[k] === LERNSTAND[k]);

try {
  // 1) Normalfall: Lernstand + Eltern-Kopplung ziehen um, #thema bleibt, Sicherung danach gelöscht
  {
    const { ctx, p, st, log } = await lauf({ alt: { ...LERNSTAND, exEltern: ELTERN, exSchritte: '[]', 'exModus2.12.0': 'js' } });
    ok(st.url.startsWith(NEU), `weitergeleitet nach ${st.url}`);
    ok(!st.url.includes('umzug='), 'Schlüssel nicht mehr in der Adresse');
    ok(lernstand(st.ls), 'Lernstand am neuen Ort vollständig');
    ok(!Object.keys(st.ls).some((k) => /umzugneu|zaehl/.test(k)), 'kein eigener Zähl-Schlüssel auf dem Gerät (Recht V45c)');
    ok(st.ls['experimentopia.eltern'] === ELTERN && !('exEltern' in st.ls), 'Eltern-Kopplung unter neuem Namen experimentopia.eltern');
    ok(!Object.keys(st.ls).some((k) => /^ex[A-Z]/.test(k)), `keine Namen ohne Vorsilbe (${Object.keys(st.ls).join(', ')})`);
    ok(log.put.length === 1 && log.put[0].status < 300 && log.put[0].origin === ALT.slice(0, -1), `Einmal-Sicherung hochgeladen (${log.put.map((x) => x.status)}) von ${log.put[0]?.origin}`);
    ok(log.get.length === 1 && log.get[0].status === 200 && log.get[0].origin === NEU.replace('/experimentopia/', ''), `am neuen Ort geholt (${log.get.map((x) => x.status)})`);
    ok(log.del.length === 1 && log.del[0].status < 300, `danach am Server gelöscht (${log.del.map((x) => x.status)})`);
    ok(/#umzug=[\w-]{22}\.[\w-]{43}\.[\w-]{22}&thema=optik$/.test(log.weiter || ''), `Format #umzug=<id>.<key>.<schreib>&thema=… (${(log.weiter || '').replace(/umzug=[^&]+/, 'umzug=…')})`);
    const nach = await fetch(log.put[0].url); ok(nach.status === 404, `Server hat nichts mehr (${nach.status})`);
    const body = await (await fetch(log.put[0].url.replace('/s/', '/s/'), { method: 'GET' })).status;
    ok(body === 404, 'nichts Lesbares bleibt zurück');
    const hatThema = await p.evaluate(() => location.hash); ok(!hatThema.includes('umzug'), `Adresse danach „${hatThema}“`);
    await p.screenshot({ path: path.resolve(root, '..', 'screenshots', '94_umzug_angekommen.png') });
    // erneut die alte Adresse öffnen: keine zweite Sicherung, Daten bleiben
    const p2 = await ctx.newPage(); await p2.goto(ALT);
    await p2.waitForFunction(() => document.getElementById('start') && document.getElementById('start').dataset.fertig === '1', null, { timeout: 120000 });
    const st2 = await p2.evaluate(() => ({ url: location.href, ls: Object.fromEntries(Object.keys(localStorage).map((k) => [k, localStorage.getItem(k)])) }));
    ok(st2.url.startsWith(NEU) && lernstand(st2.ls) && log.put.length === 1, `zweites Öffnen der alten Adresse: wieder am neuen Ort, keine zweite Sicherung (PUT ${log.put.length})`);
    await ctx.close();
  }
  // 2) Am neuen Ort ist schon ein Lernstand: nichts überschreiben, Sicherung trotzdem löschen
  {
    const eigen = { 'experimentopia.sterne': '99' };
    const { ctx, st, log } = await lauf({ alt: LERNSTAND, neu: eigen });
    ok(st.url.startsWith(NEU) && st.ls['experimentopia.sterne'] === '99' && !st.ls['experimentopia.ergebnisse'], 'vorhandener Lernstand am neuen Ort bleibt unangetastet');
    ok(log.del.length === 1 && log.del[0].status < 300, 'Sicherung trotzdem gelöscht');
    await ctx.close();
  }
  // 3) Neue Adresse noch nicht erreichbar: kein Hochladen, App startet am alten Ort mit Lernstand
  {
    const { ctx, st, log } = await lauf({ alt: LERNSTAND, neuWeg: true });
    ok(st.url.startsWith(ALT) && lernstand(st.ls) && log.put.length === 0, `neue Adresse weg → bleibt am alten Ort, nichts hochgeladen (${st.url})`);
    await ctx.close();
  }
  // 4) Leerer Speicher (neues Kind): direkt weiter, nichts hochladen
  {
    const { ctx, st, log } = await lauf({ start: ALT + '?modus=js' });
    ok(st.url.startsWith(NEU + '?modus=js') && log.put.length === 0, `leer → direkt weiter, ?modus bleibt, nichts hochgeladen (${st.url})`);
    await ctx.close();
  }
  // 5) ?umzug=nein: bleibt am alten Ort (Notbremse für Tests)
  {
    const { ctx, st, log } = await lauf({ alt: LERNSTAND, start: ALT + '?umzug=nein' });
    ok(st.url.startsWith(ALT) && log.put.length === 0, '?umzug=nein → bleibt am alten Ort');
    await ctx.close();
  }
  // 6) Gemeinsame Adresse: fremde Speicher (andere Lern-App) überleben unseren Service Worker
  {
    const ctx = await b.newContext();
    await ctx.route(/^https:\/\/lernen-test\.mathenachhilfe-online\.com\//, async (rt) => {
      const u = new URL(rt.request().url());
      if (!u.pathname.startsWith('/experimentopia/')) return rt.fulfill({ status: 200, contentType: 'text/html', body: '<!doctype html><title>andere App</title>' });
      let p = u.pathname.replace(/^\/experimentopia\//, '') || 'index.html';
      const f = path.join(root, p); if (!fs.existsSync(f)) return rt.fulfill({ status: 404, body: '' });
      await rt.fulfill({ status: 200, contentType: typ[p.split('.').pop()] || 'application/octet-stream', body: fs.readFileSync(f) });
    });
    const p = await ctx.newPage();
    await p.goto('https://lernen-test.mathenachhilfe-online.com/kurvenflug/');
    await p.evaluate(async () => { const c = await caches.open('kurvenflug-2.6.0'); await c.put('/kurvenflug/x', new Response('fremd')); localStorage.setItem('kurvenflug.stand', 'fremd'); });
    await p.goto(NEU);
    await p.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 120000 });
    const sw = await p.evaluate(async () => { for (let i = 0; i < 60; i++) { const ks = await caches.keys(); if (ks.some((k) => /^experimentopia-\d/.test(k)) && navigator.serviceWorker.controller) return ks; await new Promise((r) => setTimeout(r, 500)); } return await caches.keys(); });
    const reg = await p.evaluate(async () => (await navigator.serviceWorker.getRegistration('./'))?.scope);
    ok(sw.includes('kurvenflug-2.6.0') && sw.some((k) => /^experimentopia-\d/.test(k)), `fremder Speicher bleibt, eigener angelegt (${sw.join(', ')})`);
    ok(reg === NEU, `Service Worker nur für ${reg}`);
    ok(await p.evaluate(() => localStorage.getItem('kurvenflug.stand')) === 'fremd', 'fremder Lernstand unangetastet');
    await ctx.close();
  }
} finally { await b.close(); }
