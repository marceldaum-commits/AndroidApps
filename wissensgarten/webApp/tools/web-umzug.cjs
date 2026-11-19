// Prüft den Umzug auf die gemeinsame Adresse (Playwright): „alt“ = http://127.0.0.1:8812, „neu“ = http://127.0.0.1:8813
// (zwei Ursprünge = zwei getrennte Browser-Speicher, beide liefern denselben Ordner webapp/). Der Sicherungs-Server
// trainer-test.…/eltern/v1/s/<id> wird im Test nachgebildet. Aufruf: node web-umzug.cjs [<ordner für Fotos>]
// Fälle: gelingt (mit Sprach-Beet, Bild und #paket=), offline, Ziel hat schon einen Garten, ohne Lernstand, Ziel nicht erreichbar.
const { chromium } = require('playwright');
const OUT = process.argv[2] || '.';
const ALT = 'http://127.0.0.1:8812/', NEU = 'http://127.0.0.1:8813/', TOT = 'http://127.0.0.1:8819/';
const now = Date.now();
const card = (id, bed, f, b) => ({ id, bedId: bed, profileId: 'p1', front: f, back: b, reps: 3, stability: 9, lastReview: now - 864e5, createdAt: now - 864e5 * 9 });
const garten = (name) => ({
  profiles: [{ id: 'p1', name, avatar: '🦊', level: 6, createdAt: now - 864e5 * 9 }],
  beds: [
    { id: 'b1', profileId: 'p1', name: 'Das kleine Einmaleins', emoji: '✖️', hue: 30, packId: 'gs-einmaleins', createdAt: now - 864e5 * 9 },
    { id: 'b2', profileId: 'p1', name: 'Englisch: Tenses', emoji: '⏰', hue: 200, packId: 's1-englisch-tenses', createdAt: now - 864e5 * 9 },
  ],
  cards: [card('c1', 'b1', '3 · 4', '12'), card('e1', 'b2', 'I ___ (go) yesterday.', 'went')],
  logs: [{ id: 1, profileId: 'p1', cardId: 'e1', time: now - 864e5, rating: 2, stabilityAfter: 9 }], nextLogId: 2,
});
const prefs = JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } });
let ok = true;
const check = (c, t, x) => { console.log((c ? '✔ ' : '✘ ') + t + (x !== undefined ? ' – ' + JSON.stringify(x).slice(0, 200) : '')); if (!c) ok = false; };

async function fall(b, name, { altDaten, neuDaten, offline, ziel = NEU, hash = '' }) {
  console.log('\n— ' + name);
  const ctx = await b.newContext();
  const server = new Map(); const calls = [];
  await ctx.route('https://trainer-test.mathenachhilfe-online.com/eltern/v1/s/**', async (r) => {
    const q = r.request(), id = q.url().split('/').pop();
    calls.push(q.method());
    if (offline) return r.abort('internetdisconnected');
    if (q.method() === 'PUT') { server.set(id, q.postData()); return r.fulfill({ status: 204, body: '' }); }
    if (q.method() === 'GET') return server.has(id) ? r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ d: server.get(id) }) }) : r.fulfill({ status: 404, body: '' });
    if (q.method() === 'DELETE') { server.delete(id); return r.fulfill({ status: 204, body: '' }); }
    return r.fulfill({ status: 405, body: '' });
  });
  await ctx.route('https://trainer*.mathenachhilfe-online.com/zaehler/**', (r) => r.fulfill({ status: 204, body: '' }));
  await ctx.addInitScript(({ ALT, NEU, ziel, altDaten, neuDaten, prefs }) => {
    const alt = location.origin + '/' === ALT, neu = location.origin + '/' === NEU;
    window.wgUmzugProbe = alt ? 'alt' : neu ? 'neu' : '';
    window.wgUmzugProbeZiel = ziel;
    if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1');
    const d = alt ? altDaten : neu ? neuDaten : null;
    if (d) { localStorage.setItem('wg.garten', JSON.stringify(d)); localStorage.setItem('wg.prefs.einstellungen', prefs); if (alt) localStorage.setItem('wg.bild.c1', 'iVBORw0KGgo='); }
  }, { ALT, NEU, ziel, altDaten, neuDaten, prefs });
  const p = await ctx.newPage();
  await p.goto(ALT + '?modus=wasm' + hash);
  await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
  await p.waitForTimeout(6000);
  const url = p.url();
  const speicher = await p.evaluate(() => { const o = {}; for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); o[k] = localStorage.getItem(k); } return o; });
  // alter Ursprung: Daten müssen dort immer bleiben
  const altP = await ctx.newPage(); await altP.goto(ALT + 'version.json');
  const altSpeicher = await altP.evaluate(() => ({ garten: localStorage.getItem('wg.garten'), fertig: localStorage.getItem('wg.umzug.fertig') }));
  await p.screenshot({ path: `${OUT}/umzug_${name.replace(/\W+/g, '_')}.png` });
  await ctx.close();
  return { url, speicher, altSpeicher, server, calls };
}

(async () => {
  const b = await chromium.launch();
  let r = await fall(b, 'gelingt', { altDaten: garten('Lena'), hash: '#paket=gs-tiere' });
  const g = JSON.parse(r.speicher['wg.garten'] || '{}');
  check(r.url.startsWith(NEU), 'Weiterleitung auf die neue Adresse', r.url);
  check(!r.url.includes('umzug='), 'umzug= ist aus der Adresse entfernt', r.url);
  check((g.profiles || [])[0]?.name === 'Lena', 'Profil ist umgezogen');
  check((g.beds || []).some((x) => x.id === 'b2') && (g.cards || []).some((x) => x.id === 'e1'), 'auch das ausgeblendete Sprach-Beet ist mitgekommen');
  check((g.logs || []).length === 1, 'Lernprotokoll ist mitgekommen');
  check(r.speicher['wg.bild.c1'] === 'iVBORw0KGgo=', 'selbst gemaltes Merkbild ist mitgekommen');
  check((g.beds || []).some((x) => x.packId === 'gs-tiere'), '#paket=gs-tiere wurde danach auf der neuen Adresse ausgeführt');
  check(r.server.size === 0 && r.calls.join() === 'PUT,GET,DELETE', 'Einmal-Sicherung: PUT, GET, DELETE – danach nichts mehr auf dem Server', r.calls);
  check(!!r.altSpeicher.garten && !!r.altSpeicher.fertig, 'alte Adresse behält ihre Daten und merkt sich den Umzug');
  check(!r.speicher['wg.umzug.offen'], 'kein offener Umzug mehr');

  r = await fall(b, 'offline', { altDaten: garten('Lena'), offline: true });
  check(r.url.startsWith(ALT), 'offline: bleibt auf der alten Adresse, App startet', r.url);
  check(!r.altSpeicher.fertig && !!r.altSpeicher.garten, 'offline: Daten bleiben, nächster Versuch beim nächsten Start');

  r = await fall(b, 'ziel hat schon daten', { altDaten: garten('Lena'), neuDaten: garten('Mia') });
  check(r.url.startsWith(NEU), 'Ziel mit Daten: Weiterleitung', r.url);
  check(JSON.parse(r.speicher['wg.garten']).profiles[0].name === 'Mia', 'Ziel mit Daten: vorhandener Garten bleibt unverändert');
  check(r.server.size === 0, 'Ziel mit Daten: Einmal-Sicherung trotzdem gelöscht');

  r = await fall(b, 'ohne lernstand', { altDaten: null });
  check(r.url.startsWith(NEU) && r.calls.length === 0, 'ohne Lernstand: direkt weiter, nichts gesendet', r.calls);

  r = await fall(b, 'ziel nicht erreichbar', { altDaten: garten('Lena'), ziel: TOT });
  check(r.url.startsWith(ALT) && r.calls.length === 0, 'Ziel nicht erreichbar: bleibt, nichts gesendet', r.url);

  await b.close();
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
