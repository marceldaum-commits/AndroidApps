// Prüft „← Alle Apps“ (Playwright): sichtbar im Garten (Handy + PC, hell + dunkel), Bildschirmfotos, Antippen führt im selben Tab
// zur App-Auswahl der Website (Test-Stufe: staging.). Aufruf: node web-alle-apps.cjs [<ordner für Fotos>] [<url>]
const { chromium, devices } = require('playwright');
const OUT = process.argv[2] || '.';
const BASE = process.argv[3] || 'http://127.0.0.1:8812/';
const now = Date.now();
const store = { profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: now - 864e5 }], beds: [], cards: [], logs: [], nextLogId: 1 };
let ok = true;
const check = (c, t, x) => { console.log((c ? '✔ ' : '✘ ') + t + (x !== undefined ? ' – ' + JSON.stringify(x) : '')); if (!c) ok = false; };
(async () => {
  const b = await chromium.launch();
  for (const [name, opts] of [['handy', { ...devices['iPhone 13'] }], ['pc', { viewport: { width: 1280, height: 800 } }], ['handy_dunkel', { ...devices['iPhone 13'], colorScheme: 'dark' }]]) {
    const ctx = await b.newContext(opts);
    await ctx.route('https://trainer*.mathenachhilfe-online.com/**', (r) => r.fulfill({ status: 204, body: '' }));
    let ziel = null;
    await ctx.route('https://staging.mathenachhilfe-online.com/**', (r) => { ziel = r.request().url(); r.fulfill({ status: 200, contentType: 'text/html', body: '<h1>Lern-Apps</h1>' }); });
    const p = await ctx.newPage();
    await p.addInitScript((s) => {
      if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1');
      localStorage.setItem('wg.garten', JSON.stringify(s));
      localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } }));
    }, store);
    await p.goto(BASE + '?modus=wasm');
    await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
    await p.waitForTimeout(3500);
    await p.screenshot({ path: `${OUT}/alle_apps_${name}.png` });
    // Knopf oben links antippen (Leiste ist 44 px hoch, Text beginnt links)
    await p.mouse.click(60, 22);
    await p.waitForTimeout(2500);
    check(ziel === 'https://staging.mathenachhilfe-online.com/lern-apps/?src=wissensgarten-kopf' && p.url().startsWith('https://staging.mathenachhilfe-online.com/lern-apps/'),
      `${name}: „← Alle Apps“ öffnet die App-Auswahl im selben Tab`, p.url());
    await ctx.close();
  }
  await b.close();
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
