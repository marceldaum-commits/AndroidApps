// Familien-Speicher gegen den echten Test-Server (lernen-test.…/familie/v1/) mit der echten familie.js und der Brücke der App:
// anlegen → Index → Stand sichern (verschlüsselt) → zweites Gerät lädt → Konflikt (409) → löschen → „familie-weg“.
// Alle Anfragen des Browsers gehen über Node (fetch), die App-Dateien kommen aus webapp/.
//   NODE_USE_ENV_PROXY=1 node tools/web-familie-echt.mjs      (nach ./gradlew :web:webapp)
import { createRequire } from 'node:module';
import path from 'node:path';
import fs from 'node:fs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', 'webapp');
const NEU = 'https://lernen-test.mathenachhilfe-online.com/experimentopia/';
const BASIS = 'https://lernen-test.mathenachhilfe-online.com';
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
const typ = { html: 'text/html', js: 'text/javascript', json: 'application/json', wasm: 'application/wasm', ttf: 'font/ttf', png: 'image/png', webmanifest: 'application/manifest+json' };
const b = await chromium.launch(); const ctx = await b.newContext(); const netz = [];
await ctx.route(/^https:\/\/lernen-test\.mathenachhilfe-online\.com\/experimentopia\//, async (rt) => {
  const u = new URL(rt.request().url()); let p = u.pathname.replace(/^\/experimentopia\//, '') || 'index.html'; const f = path.join(root, p);
  if (!fs.existsSync(f)) return rt.fulfill({ status: 404, body: '' });
  await rt.fulfill({ status: 200, contentType: typ[p.split('.').pop()] || 'application/octet-stream', body: fs.readFileSync(f) });
});
await ctx.route(BASIS + '/familie/**', async (rt) => {
  const r = rt.request(), h = { ...r.headers() }; delete h['host'];
  const res = await fetch(r.url(), { method: r.method(), headers: h, body: ['GET', 'HEAD', 'OPTIONS'].includes(r.method()) ? undefined : r.postDataBuffer() });
  const body = Buffer.from(await res.arrayBuffer()); netz.push({ m: r.method(), pfad: new URL(r.url()).pathname.replace(/^\/familie\/v1\/[\w-]{22}/, '/familie/v1/<id>'), s: res.status, body: body.toString('latin1') });
  const hd = {}; res.headers.forEach((v, k) => { if (!['content-encoding', 'content-length', 'transfer-encoding'].includes(k)) hd[k] = v; });
  await rt.fulfill({ status: res.status, headers: hd, body });
});
const p = await ctx.newPage();
await p.goto(NEU + 'version.json');            // gleiche Herkunft, ohne die App zu starten
await p.setContent('<script src="familie-bruecke.js"></script>', { waitUntil: 'load' }).catch(() => {});
await p.goto(NEU); await p.waitForFunction(() => window.ExFamilie, null, { timeout: 60000 });
// Service Worker abwarten: Er darf /familie/v1/ nie zwischenspeichern (Prüfung unten)
await p.waitForFunction(() => navigator.serviceWorker.controller || false, null, { timeout: 60000 }).catch(() => {});
const call = (op, a) => p.evaluate(async ([op, a]) => JSON.parse(await window.ExFamilie.call(op, JSON.stringify(a))), [op, a]);
const neu = await call('neu', {}); ok(neu.ok && /^[\w-]{22}$/.test(neu.fam.famId) && /^[0-9A-Z]{4}-[0-9A-Z]{4}$/.test(neu.nummer), `neue Familie ${neu.nummer}`);
const fam = neu.fam;
const an = await call('anlegen', { fam, basis: BASIS }); ok(an.ok, `anlegen (mit Notfall-Kopie) → ${an.ok ? 'ok' : an.grund + ' ' + (an.meldung || '')}`);
if (!an.ok) { await b.close(); process.exit(1); }
const an2 = await call('anlegen', { fam, basis: BASIS }); ok(!an2.ok && an2.grund === 'gibt-es-schon', `zweites Anlegen → ${an2.grund}`);
const idx = await call('indexNeu', { name: 'Lea' }); const is = await call('indexSichern', { fam, basis: BASIS, index: idx.index, rev: 0 }); ok(is.ok && is.rev === 1, `Index gesichert (rev ${is.rev})`);
const il = await call('indexLaden', { fam, basis: BASIS }); ok(il.ok && il.daten.kinder[0].name === 'Lea' && il.daten.apps.includes('experimentopia'), 'Index von „Gerät B“ geladen und entschlüsselt');
const daten = { ergebnisse: '[{"experimentId":"x1","stars":3,"firstGuessRight":true,"plays":1,"firstAt":1,"lastAt":1}]', abzeichen: 'a,b' };
const s1 = await call('standSichern', { fam, basis: BASIS, kind: idx.kindId, daten, rev: 0 }); ok(s1.ok && s1.rev === 1, `Stand gesichert (rev ${s1.rev})`);
const l1 = await call('standLaden', { fam, basis: BASIS, kind: idx.kindId }); ok(l1.ok && l1.daten.abzeichen === 'a,b' && l1.rev === 1, 'Stand geladen, identisch');
const s2 = await call('standSichern', { fam, basis: BASIS, kind: idx.kindId, daten: { ...daten, abzeichen: 'a' }, rev: 0 }); ok(!s2.ok && s2.grund === 'konflikt' && s2.rev === 1 && s2.daten.abzeichen === 'a,b', `veraltetes rev → ${s2.grund} mit aktuellem Stand`);
const s3 = await call('standSichern', { fam, basis: BASIS, kind: idx.kindId, daten: { ...daten, abzeichen: 'a,b,c' }, rev: 1 }); ok(s3.ok && s3.rev === 2, 'nach Zusammenführen: rev 2');
const fremd = await call('standLaden', { fam: { ...fam, famKey: 'x'.repeat(43) }, basis: BASIS, kind: idx.kindId }); ok(!fremd.ok && fremd.grund === 'schluessel', `falscher Schlüssel → ${fremd.grund}`);
const k = await call('standLaden', { fam, basis: BASIS, kind: 'z'.repeat(22) }); ok(!k.ok && k.grund === 'keine', `anderes Kind ohne Stand → ${k.grund}`);
const blob = netz.filter((x) => x.m === 'GET' && x.pfad.includes('/s/experimentopia/')).map((x) => x.body).join('');
ok(blob.length > 0 && !/abzeichen|ergebnisse|experimentId|Lea/.test(blob), `Server sieht nur Zufallszeichen (${blob.length} Zeichen, kein Klartext)`);
const bl = await call('blatt', { fam, seite: BASIS }); ok(bl.link.startsWith(BASIS + '/experimentopia/#familie=') && bl.blatt.includes(neu.nummer), 'Notfall-Blatt mit Link und Familien-Nummer');
const lo = await call('loeschen', { fam, basis: BASIS }); ok(lo.ok, 'Familie gelöscht');
const nach = await call('standLaden', { fam, basis: BASIS, kind: idx.kindId }); ok(!nach.ok && nach.grund === 'familie-weg', `nach dem Löschen anderes Gerät → ${nach.grund} („Diese Familie gibt es nicht mehr.“)`);
const gespeichert = await p.evaluate(async () => { const out = []; for (const k of await caches.keys()) for (const r of await (await caches.open(k)).keys()) out.push(r.url); return out; });
ok(!gespeichert.some((u) => u.includes('/familie/')), `Service Worker speichert nichts von /familie/v1/ (${gespeichert.length} Dateien im Speicher)`);
await b.close();
