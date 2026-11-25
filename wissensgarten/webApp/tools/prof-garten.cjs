// CPU-Profil des Gartens (Playwright + CDP): wo geht die Hauptzeit hin? Aufruf: node prof-garten.cjs [url]
const { chromium, devices } = require('playwright');
const BASE = process.argv[2] || 'http://127.0.0.1:8812/';
const now = Date.now(), day = 864e5;
const packs = [['gs-einmaleins', 'Einmaleins', '✖️', 30], ['gs-tiere', 'Tiere', '🐾', 120], ['s1-mathe-brueche-prozent', 'Brüche', '➗', 200], ['s1-geschichte-neuzeit', 'Neuzeit', '🏰', 40], ['s1-bio-pflanzen', 'Pflanzen', '🌱', 100], ['gs-uhrzeit', 'Uhrzeit', '⏰', 260]];
const beds = packs.map((p, i) => ({ id: 'b' + (i + 1), profileId: 'p1', name: p[1], emoji: p[2], hue: p[3], packId: p[0], createdAt: now - day * 20 }));
const cards = []; beds.forEach((b, bi) => { for (let i = 0; i < 12; i++) cards.push({ id: `c${bi}_${i}`, bedId: b.id, profileId: 'p1', front: `F${i}`, back: `A${i}`, reps: 1 + (i % 6), stability: 2 + i * 1.5, lastReview: now - day * (1 + (i % 5)), createdAt: now - day * 19 }); });
const store = { profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: now - day * 20 }], beds, cards, logs: [], nextLogId: 1 };
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ ...devices['Pixel 7'] });
  await ctx.route('https://trainer*.mathenachhilfe-online.com/**', (r) => r.fulfill({ status: 204, body: '' }));
  const p = await ctx.newPage();
  await p.addInitScript((s) => { if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1'); localStorage.setItem('wg.garten', JSON.stringify(s));
    localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } })); }, store);
  await p.goto(BASE + '?modus=wasm&leicht=aus&bremse=aus');
  await p.waitForFunction(() => window.wgReady, null, { timeout: 180000 });
  await p.waitForTimeout(8000);
  const c = await ctx.newCDPSession(p);
  await c.send('Profiler.enable'); await c.send('Profiler.setSamplingInterval', { interval: 500 }); await c.send('Profiler.start');
  await p.waitForTimeout(6000);
  const { profile } = await c.send('Profiler.stop');
  const dt = profile.timeDeltas; const self = new Map(); const byId = new Map(profile.nodes.map((n) => [n.id, n]));
  profile.samples.forEach((id, i) => { const n = byId.get(id); const k = (n.callFrame.functionName || '(anonym)') + ' ' + (n.callFrame.url || '').split('/').pop() + ':' + n.callFrame.lineNumber; self.set(k, (self.get(k) || 0) + (dt[i] || 0)); });
  const tot = [...self.values()].reduce((a, b) => a + b, 0);
  console.log('Gesamt', (tot / 1000).toFixed(0), 'ms Stichproben in 6 s');
  [...self.entries()].sort((a, b) => b[1] - a[1]).slice(0, 25).forEach(([k, v]) => console.log(String((100 * v / tot).toFixed(1)).padStart(5) + ' %  ' + k.slice(0, 120)));
  await b.close();
})();
