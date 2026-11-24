// Recht-Auflage L1/L6 (V12) für Weg 2: Netzwerkmitschnitt im Browser (Chromium mit Fake-Mikrofon) gegen den Mock-Server.
// Aufruf: node tools/web-auswertung.mjs
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import { neuerMock } from './sprache-mock.mjs';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const root = fileURLToPath(new URL('../webapp/', import.meta.url));
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json', '.webmanifest': 'application/manifest+json', '.svg': 'image/svg+xml', '.png': 'image/png', '.mp3': 'audio/mpeg' };
const mock = neuerMock();
const server = createServer(async (req, res) => {
  const p = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  if (p.startsWith('/sprache/')) return mock.handle(req, res);
  const f = normalize(join(root, p.endsWith('/') ? p + 'index.html' : p)); if (!f.startsWith(root)) { res.writeHead(403); res.end(); return; }
  try { const b = await readFile(f); res.writeHead(200, { 'Content-Type': types[extname(f)] || 'application/octet-stream' }); res.end(b); } catch { res.writeHead(404); res.end(); }
});
await new Promise((r) => server.listen(0, '127.0.0.1', r));
const base = `http://127.0.0.1:${server.address().port}/`;
let failed = 0; const ok = (c, m) => { console.log((c ? '  ✅ ' : '  ❌ ') + m); if (!c) failed++; };
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream', '--autoplay-policy=no-user-gesture-required'] });

/** Neue Seite mit Vorgaben: Flag, Wahl der Eltern (oder keine), Modus des Mocks. Gibt die Anzahl Anfragen an /sprache/ zurück, die der BROWSER gesendet hat. */
async function neueSeite({ flag, wahl }) {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, permissions: ['microphone'] });
  const page = await ctx.newPage(); const anfragen = [];
  page.on('request', (r) => { if (r.url().includes('/sprache/')) anfragen.push({ methode: r.method(), url: r.url() }); });
  await page.addInitScript((f) => { window.__flag = f; }, flag);
  await page.route('**/version.js', async (route) => { const r = await route.fetch(); let t = await r.text(); t = t.replace('window.AUSWERTUNG_AKTIV = false;', 'window.AUSWERTUNG_AKTIV = ' + (flag ? 'true' : 'false') + ';').replace('window.FAMILIE_AKTIV = false;', 'window.FAMILIE_AKTIV = ' + (flag === 'familie' ? 'true' : 'false') + ';').replace('window.PRO_AKTIV = false;', 'window.PRO_AKTIV = ' + (flag === 'pro' ? 'true' : 'false') + ';'); await route.fulfill({ response: r, body: t }); });
  await page.goto(base + '?diag=1'); await page.waitForSelector('[data-k]');
  await page.fill('[data-name]', 'Lena'); await page.click('[data-k="3"]'); await page.click('[data-go]'); await page.waitForSelector('.world');
  if (wahl) await page.evaluate((w) => { const s = window.quasselTest.state(); s.settings.auswertung = w; window.APP_SETTINGS = s.settings; }, wahl);
  return { page, ctx, anfragen };
}
/** Spielt Episode 1 bis zum Papagei-Echo, nimmt einmal auf und gibt den Text des Mikrofon-Etiketts zurück. */
async function echoNachsprechen(page) {
  await page.click('[data-ep="w1e1"]');
  for (let i = 0; i < 8; i++) { if (!(await page.$('[data-again]'))) break; await page.click('[data-next]'); await wait(150); }
  for (let guard = 0; guard < 80; guard++) {
    await wait(250);
    const title = await page.$eval('.game-title', (e) => e.textContent).catch(() => '');
    if (title.includes('Papagei')) break;
    const okBtn = await page.$('.choice[data-ok]:not([disabled])'); if (okBtn) { await okBtn.click(); await wait(1200); continue; }
    const nextBtn = await page.$('[data-next]:not([hidden])'); if (nextBtn && await nextBtn.isVisible()) { await nextBtn.click(); continue; }
  }
  await page.click('[data-mic]'); await wait(1800); await page.click('[data-mic]');   // aufnehmen, stoppen
  await wait(1500);
  return { ml: await page.textContent('[data-ml]').catch(() => ''), sterne: (await page.textContent('[data-st]').catch(() => '')) || '', ruby: await page.textContent('[data-rubytext]').catch(() => '') };
}

console.log('\nL1: keine Anfrage ohne Wahl „Server“');
for (const [label, flag, wahl] of [['Flag an, keine Auswahl', true, null], ['Flag an, Auswahl (1) „Im Gerät“', true, 'geraet'], ['Flag an, Auswahl (3) „Aus“', true, 'aus'], ['Flag aus, „Server“ gespeichert', false, 'server']]) {
  mock.log.length = 0; mock.modus_ = 'ok';
  const { page, ctx, anfragen } = await neueSeite({ flag, wahl });
  const r = await echoNachsprechen(page); await wait(500);
  ok(anfragen.length === 0 && mock.log.length === 0, `${label}: 0 Anfragen an /sprache/ (Browser ${anfragen.length}, Server ${mock.log.length}) – Sterne: „${r.sterne.trim()}“`);
  ok(!/⭐/.test(r.sterne), `${label}: keine Sterne ohne Messung`);
  await ctx.close();
}

console.log('\nWahl (2) „Server“: Inhalt der Anfrage (Recht V12)');
{
  mock.log.length = 0; mock.modus_ = 'ok';
  const { page, ctx, anfragen } = await neueSeite({ flag: true, wahl: 'server' });
  const r = await echoNachsprechen(page); await wait(800);
  ok(anfragen.length === 1 && anfragen[0].methode === 'POST', `genau 1 Anfrage (${anfragen.length}), POST`);
  const m = mock.log[0] || {};
  ok(JSON.stringify(m.felder) === JSON.stringify(['sprache', 'ziel', 'audio']), 'nur die Felder sprache, ziel, audio: ' + JSON.stringify(m.felder));
  ok(m.sprache === 'en' && (m.ziel || '').length > 3, `Sprache „${m.sprache}“, Zielsatz „${m.ziel}“`);
  ok(!m.header.cookie && !m.header.authorization && !m.header.referer, 'kein Cookie, keine Autorisierung, kein Referrer: ' + JSON.stringify(Object.keys(m.header)));
  ok(m.groesse < 1000000, `Aufnahme unter 1 MB (${m.groesse} Bytes)`);
  ok(!/\?/.test(m.url), 'keine Parameter in der Adresse: ' + m.url);
  ok(/⭐/.test(r.sterne), `Sterne nach echter Übereinstimmung: „${r.sterne.trim()}“`);
  await ctx.close();
}

console.log('\nFehlerfälle: keine Sterne, neutraler Hinweis, kein Absturz');
for (const [modus, wartezeit] of [['429', 1500], ['503', 1500], ['500', 1500], ['413', 1500], ['langsam', 11000], ['schlecht', 1500]]) {
  mock.log.length = 0; mock.modus_ = modus;
  const { page, ctx, anfragen } = await neueSeite({ flag: true, wahl: 'server' });
  const fehler = []; page.on('pageerror', (e) => fehler.push(String(e)));
  await page.click('[data-ep="w1e1"]');
  for (let i = 0; i < 8; i++) { if (!(await page.$('[data-again]'))) break; await page.click('[data-next]'); await wait(150); }
  for (let guard = 0; guard < 80; guard++) { await wait(250); const t = await page.$eval('.game-title', (e) => e.textContent).catch(() => ''); if (t.includes('Papagei')) break; const b = await page.$('.choice[data-ok]:not([disabled])'); if (b) { await b.click(); await wait(1200); continue; } const n = await page.$('[data-next]:not([hidden])'); if (n && await n.isVisible()) await n.click(); }
  await page.click('[data-mic]'); await wait(1800); await page.click('[data-mic]'); await wait(wartezeit);
  const ml = await page.textContent('[data-ml]'); const st = await page.textContent('[data-st]');
  const erwartet = modus === 'schlecht' ? /noch einmal/ : /gerade nicht möglich/;
  ok(erwartet.test(ml) && !/⭐/.test(st) && fehler.length === 0, `Modus ${modus}: „${ml.trim().slice(0, 60)}“, keine Sterne, Seite läuft weiter (${anfragen.length} Anfrage)`);
  await ctx.close();
}

console.log('\nL6: Deutsch und Latein werden nie gesendet');
{
  mock.log.length = 0; mock.modus_ = 'ok';
  const { page, ctx, anfragen } = await neueSeite({ flag: true, wahl: 'server' });
  const r = await page.evaluate(async () => {
    const blob = new Blob([new Uint8Array(2000)], { type: 'audio/webm' }); const out = {};
    for (const l of ['de', 'la', 'it', 'uk']) out[l] = (await window.AUSWERTUNG.pruefen(l, 'Hallo', blob)).status;
    return out;
  });
  ok(Object.values(r).every((s) => s === 'nicht_gefragt') && anfragen.length === 0, 'de/la (und it/uk, noch nicht freigegeben): nicht_gefragt, 0 Anfragen ' + JSON.stringify(r));
  await ctx.close();
}
console.log('\nFamilien-Speicher (Oberfläche, Server nicht live): Karte nur mit Flag, Einwilligung vor dem Hochladen, freundliche Meldung');
{
  for (const flag of [false, 'familie']) {
    const { page, ctx } = await neueSeite({ flag, wahl: null }); const fam = [];
    page.on('request', (r) => { if (r.url().includes('/familie/v1/')) fam.push(r.method()); });
    await page.route('**/familie/v1/**', (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ fehler: 'noch_nicht_live' }) }));
    const tor = async () => { if (await page.$('.tor')) { const z = await page.getAttribute('.tor', 'data-loesung'); for (const c of String(z)) await page.click(`.tor-taste[data-z="${c}"]`); await page.click('.tor-taste[data-ok]'); } };
    await page.click('[data-settings]'); await tor(); await page.waitForSelector('[data-parents]'); await page.click('[data-parents]'); await tor();
    await page.waitForSelector('[data-offline]', { timeout: 8000 });
    const gefunden = !!(await page.$('[data-familie-karte]'));
    if (flag === false) { ok(!gefunden && fam.length === 0, 'Flag aus: keine Familien-Karte, 0 Aufrufe'); await ctx.close(); continue; }
    ok(gefunden, 'Flag an: Karte „Familie“ im Elternbereich');
    await page.click('[data-neu]'); const t = await page.textContent('[data-fpflicht]');
    ok(/verschlüsselt auf unserem Server in Deutschland/.test(t) && fam.length === 0, 'Pflichttext sichtbar, vor „Einverstanden“ 0 Aufrufe');
    await page.click('[data-fbereich] [data-ok]'); await wait(1200);
    ok(/noch nicht freigeschaltet/.test(await page.textContent('[data-fmeld]')) && fam.length >= 1, 'Server nicht live: freundliche Meldung, Stand bleibt auf dem Gerät');
    await ctx.close();
  }
}
console.log('\nPro (Sperre je Sprache, Schloss-Text): nur mit Flag');
for (const flag of [false, 'pro']) {
  const { page, ctx } = await neueSeite({ flag, wahl: null });
  await page.click('[data-lang="fr"]'); await page.waitForSelector('[data-lp-e="fr-1-1"]'); await wait(400);
  const zu = async (id) => page.$eval(`[data-lp-e="${id}"]`, (e) => e.classList.contains('pro-zu'));
  if (!flag) { ok(!(await zu('fr-2-2')), 'Flag aus: nichts gesperrt (fr-2-2 offen)'); await ctx.close(); continue; }
  ok(!(await zu('fr-1-3')) && !(await zu('fr-2-1')) && (await zu('fr-2-2')) && (await zu('fr-3-1') === false), 'Flag an: Abschnitt 1 frei, ab Abschnitt 2 nur die erste Einheit frei, fr-2-2 gesperrt');
  await page.click('[data-lp-e="fr-2-2"]', { force: true }); await page.waitForSelector('.modal-bg [data-ok]');
  const t = await page.textContent('.modal-bg');
  ok(t.replace('🔒', '').replace('OK', '').trim() === 'Das gehört zu Quasselquest Pro.' && !/€|Preis|Eltern|kaufen|Shop/i.test(t), 'Schloss-Text in der Kinderansicht genau: „' + t.replace('OK', '').trim() + '“ (kein Preis, kein Kaufknopf, kein Hinweis auf Eltern)');
  await page.click('.modal-bg [data-ok]');
  ok(!(await page.$('[data-lp-e="fr-2-2"].gewaehlt')) && !(await page.$('.lp-lektionen')), 'Gesperrte Einheit öffnet sich nicht');
  await ctx.close();
}
await browser.close(); server.close();
console.log(failed ? `\n❌ ${failed} Prüfung(en) fehlgeschlagen` : '\n✅ Alles grün (L1/L6)');
process.exit(failed ? 1 : 0);
