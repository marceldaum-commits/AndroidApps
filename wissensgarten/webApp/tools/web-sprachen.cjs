// Prüft 2.2.19 (Playwright, Wasm und JS): Ein Garten mit einem Sprach-Beet (s1-englisch-tenses) und einem Mathe-Beet.
// Das Sprach-Beet ist ausgeblendet (Hinweis auf Quasselquest, Bildschirmfoto), seine Daten bleiben aber vollständig im Speicher.
// Aufruf: node web-sprachen.cjs [<ordner für Fotos>] [<url>]
const { chromium, devices } = require('playwright');
const OUT = process.argv[2] || '.';
const BASE = process.argv[3] || 'http://127.0.0.1:8812/';
const now = Date.now();
const card = (id, bed, f, b) => ({ id, bedId: bed, profileId: 'p1', front: f, back: b, reps: 3, stability: 9, lastReview: now - 864e5, createdAt: now - 864e5 * 9 });
const store = {
  profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 6, createdAt: now - 864e5 * 9 }],
  beds: [
    { id: 'b1', profileId: 'p1', name: 'Das kleine Einmaleins', emoji: '✖️', hue: 30, packId: 'gs-einmaleins', createdAt: now - 864e5 * 9 },
    { id: 'b2', profileId: 'p1', name: 'Englisch: Tenses', emoji: '⏰', hue: 200, packId: 's1-englisch-tenses', createdAt: now - 864e5 * 9 },
  ],
  cards: [card('c1', 'b1', '3 · 4', '12'), card('c2', 'b1', '6 · 7', '42'), card('e1', 'b2', 'I ___ (go) yesterday.', 'went'), card('e2', 'b2', 'She ___ (be) here.', 'is')],
  logs: [{ id: 1, profileId: 'p1', cardId: 'e1', time: now - 864e5, rating: 2, stabilityAfter: 9 }],
  nextLogId: 2,
};
let ok = true;
const check = (c, t, x) => { console.log((c ? '✔ ' : '✘ ') + t + (x !== undefined ? ' – ' + JSON.stringify(x) : '')); if (!c) ok = false; };
(async () => {
  const b = await chromium.launch();
  for (const modus of ['wasm', 'js']) {
    const ctx = await b.newContext({ ...devices['iPhone 13'] });
    await ctx.route('https://trainer*.mathenachhilfe-online.com/**', (r) => r.fulfill({ status: 204, body: '' }));
    const p = await ctx.newPage();
    await p.addInitScript((s) => {
      if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1');
      localStorage.setItem('wg.garten', JSON.stringify(s));
      localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } }));
    }, store);
    await p.goto(BASE + '?modus=' + modus);
    await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
    await p.waitForTimeout(5000);
    await p.screenshot({ path: `${OUT}/sprachen_hinweis_${modus}.png` });
    // Etwas auslösen, das speichert (Sichtbarkeitswechsel), dann prüfen, dass das Sprach-Beet samt Karten und Log noch da ist
    await p.evaluate(() => document.dispatchEvent(new Event('visibilitychange')));
    await p.waitForTimeout(1500);
    const g = await p.evaluate(() => JSON.parse(localStorage.getItem('wg.garten') || '{}'));
    check((g.beds || []).some((x) => x.id === 'b2'), `${modus}: Sprach-Beet ist weiter gespeichert`);
    check((g.cards || []).filter((x) => x.bedId === 'b2').length === 2, `${modus}: seine 2 Karten sind weiter gespeichert`);
    check((g.logs || []).some((x) => x.cardId === 'e1'), `${modus}: sein Lernprotokoll ist weiter gespeichert`);
    // Deeplink auf das Sprachpaket pflanzt nichts Neues
    await p.evaluate(() => { location.hash = 'paket=s1-englisch-alltag'; });
    await p.waitForTimeout(2500);
    const g2 = await p.evaluate(() => JSON.parse(localStorage.getItem('wg.garten') || '{}'));
    check(!(g2.beds || []).some((x) => x.packId === 's1-englisch-alltag'), `${modus}: #paket= pflanzt kein Sprachpaket`);
    await ctx.close();
  }
  await b.close();
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
