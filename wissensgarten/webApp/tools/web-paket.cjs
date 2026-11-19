// Prüft den Deeplink …/#paket=<ID> (Playwright): Paket wird gepflanzt und das Beet geöffnet, der Hash verschwindet,
// ein zweiter Aufruf pflanzt nicht doppelt, „hashchange“ bei offener App wirkt. Aufruf: node web-paket.cjs [<ordner für Fotos>] [<url>]
const { chromium, devices } = require('playwright');
const OUT = process.argv[2] || '.';
const BASE = process.argv[3] || 'http://127.0.0.1:8812/';
const now = Date.now();
const store = { profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: now - 864e5 }], beds: [], cards: [], logs: [], nextLogId: 1 };
let ok = true;
const check = (c, t, x) => { console.log((c ? '✔ ' : '✘ ') + t + (x !== undefined ? ' – ' + JSON.stringify(x) : '')); if (!c) ok = false; };
const beete = (p) => p.evaluate(() => JSON.parse(localStorage.getItem('wg.garten') || '{}').beds || []);
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ ...devices['iPhone 13'] });
  await ctx.route('https://trainer*.mathenachhilfe-online.com/**', (r) => r.fulfill({ status: 204, body: '' }));
  const p = await ctx.newPage();
  await p.addInitScript((s) => {
    if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1');
    localStorage.setItem('wg.garten', JSON.stringify(s));
    localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } }));
  }, store);
  await p.goto(BASE + '?modus=wasm#paket=s1-mathe-gleichungen');
  await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
  await p.waitForTimeout(5000);
  let bs = await beete(p);
  check(bs.filter((x) => x.packId === 's1-mathe-gleichungen').length === 1, 'Paket wurde gepflanzt', bs.map((x) => x.packId));
  check(!p.url().includes('paket='), 'Hash ist aus der Adresse entfernt', p.url());
  await p.screenshot({ path: `${OUT}/paket_deeplink.png` });

  // Zweiter Aufruf: kein doppeltes Beet
  await p.goto(BASE + '?modus=wasm#paket=s1-mathe-gleichungen');
  await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
  await p.waitForTimeout(4000);
  bs = await beete(p);
  check(bs.filter((x) => x.packId === 's1-mathe-gleichungen').length === 1, 'zweiter Aufruf pflanzt nicht doppelt', bs.length);

  // Bei offener App: hashchange
  await p.evaluate(() => { location.hash = 'paket=gs-einmaleins'; });
  await p.waitForTimeout(4000);
  bs = await beete(p);
  check(bs.some((x) => x.packId === 'gs-einmaleins'), 'hashchange bei offener App pflanzt gs-einmaleins', bs.map((x) => x.packId));

  // Unbekannte ID: nichts passiert, kein Absturz
  await p.evaluate(() => { location.hash = 'paket=gibt-es-nicht'; });
  await p.waitForTimeout(2500);
  check((await beete(p)).length === bs.length, 'unbekannte Paket-ID pflanzt nichts');
  await ctx.close();
  await b.close();
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
