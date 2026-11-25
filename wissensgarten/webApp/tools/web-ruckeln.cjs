// Misst, wie flüssig die WebApp läuft (Playwright, Chromium, ohne Grafikkarte = ungünstiger Fall).
// Pro Bildschirm: Bilder pro Sekunde, 95. Perzentil der Bildabstände, Anteil der Zeit in langen Aufgaben (> 50 ms).
// Aufruf: node web-ruckeln.cjs [<modus: wasm|js>] [<url>] [<ohne Bremse: 1|0>] [<geräteprofil: handy|pc>]
// Die Messung ist ein Vergleichswert (vorher/nachher), keine Aussage über ein bestimmtes Gerät.
const { chromium, devices } = require('playwright');
const MODUS = process.argv[2] || 'wasm';
const BASE = process.argv[3] || 'http://127.0.0.1:8812/';
const OHNE_BREMSE = (process.argv[4] || '1') === '1';
const PROFIL = process.argv[5] || 'handy';
const now = Date.now();
const day = 864e5;
const mk = (n, pack, name, emoji, hue) => ({ id: 'b' + n, profileId: 'p1', name, emoji, hue, packId: pack, createdAt: now - day * 20 });
const packs = [['gs-einmaleins', 'Das kleine Einmaleins', '✖️', 30], ['gs-tiere', 'Tiere', '🐾', 120], ['s1-mathe-brueche-prozent', 'Brüche & Prozent', '➗', 200], ['s1-geschichte-neuzeit', 'Neuzeit', '🏰', 40], ['s1-bio-pflanzen', 'Pflanzen', '🌱', 100], ['gs-uhrzeit', 'Uhrzeit', '⏰', 260]];
const beds = packs.map((p, i) => mk(i + 1, p[0], p[1], p[2], p[3]));
const cards = [];
beds.forEach((b, bi) => { for (let i = 0; i < 12; i++) cards.push({ id: `c${bi}_${i}`, bedId: b.id, profileId: 'p1', front: `Frage ${i + 1} (${b.name})`, back: `Antwort ${i + 1}`, reps: 1 + (i % 6), stability: 2 + i * 1.5, lastReview: now - day * (1 + (i % 5)), createdAt: now - day * 19 }); });
const store = { profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: now - day * 20 }], beds, cards, logs: [], nextLogId: 1 };

const messen = (p, ms) => p.evaluate((ms) => new Promise((ok) => {
  const t = []; let last = performance.now(); const start = last; let lang = 0;
  let po = null;
  try { po = new PerformanceObserver((l) => { for (const e of l.getEntries()) lang += e.duration; }); po.observe({ entryTypes: ['longtask'] }); } catch (e) {}
  const tick = (n) => { t.push(n - last); last = n; if (n - start < ms) requestAnimationFrame(tick); else { if (po) po.disconnect(); t.shift(); t.sort((a, b) => a - b);
    const sum = t.reduce((a, b) => a + b, 0); ok({ fps: +(1000 * t.length / sum).toFixed(1), p50: +t[Math.floor(t.length * .5)].toFixed(0), p95: +t[Math.floor(t.length * .95)].toFixed(0), max: +t[t.length - 1].toFixed(0), langProzent: +(100 * lang / ms).toFixed(0) }); } };
  requestAnimationFrame(tick);
}), ms);

(async () => {
  const b = await chromium.launch();
  const dev = PROFIL === 'pc' ? { viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1 } : { ...devices['Pixel 7'] };
  const ctx = await b.newContext(dev);
  await ctx.route('https://trainer*.mathenachhilfe-online.com/**', (r) => r.fulfill({ status: 204, body: '' }));
  const p = await ctx.newPage();
  await p.addInitScript((s) => {
    if (sessionStorage.getItem('v')) return; sessionStorage.setItem('v', '1');
    localStorage.setItem('wg.garten', JSON.stringify(s));
    localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } }));
  }, store);
  const t0 = Date.now();
  await p.goto(BASE + `?modus=${MODUS}&leicht=aus` + (OHNE_BREMSE ? '&bremse=aus' : ''));
  await p.waitForFunction(() => window.wgReady, null, { timeout: 180000 });
  console.log(`Start bis „bereit“: ${((Date.now() - t0) / 1000).toFixed(1)} s (${MODUS}, ${PROFIL}, Bremse ${OHNE_BREMSE ? 'aus' : 'an'})`);
  await p.waitForTimeout(8000);   // Messfenster beginnt erst nach dem Start
  const vp = p.viewportSize();
  const szenen = [];
  szenen.push(['Garten, nichts angefasst', await messen(p, 6000)]);
  // Wischen im Garten
  const wisch = (async () => { for (let i = 0; i < 12; i++) { await p.mouse.move(vp.width / 2, vp.height * 0.7); await p.mouse.down(); await p.mouse.move(vp.width / 2, vp.height * 0.3, { steps: 8 }); await p.mouse.up(); await p.waitForTimeout(150); await p.mouse.move(vp.width / 2, vp.height * 0.3); await p.mouse.down(); await p.mouse.move(vp.width / 2, vp.height * 0.7, { steps: 8 }); await p.mouse.up(); await p.waitForTimeout(150); } })();
  szenen.push(['Garten, wischen', await messen(p, 6000)]); await wisch;
  const tabs = ['Garten', 'Samen', 'Laden', 'Mehr'];
  const nav = [[0.12, 0.96], [0.37, 0.96], [0.62, 0.96], [0.87, 0.96]];
  for (let i = 1; i < 4 && PROFIL === 'handy'; i++) {
    await p.mouse.click(vp.width * nav[i][0], vp.height * nav[i][1]); await p.waitForTimeout(2500);
    szenen.push([`Tab ${tabs[i]}`, await messen(p, 5000)]);
  }
  for (const [n, r] of szenen) console.log(`${n.padEnd(28)} ${String(r.fps).padStart(5)} B/s  p50 ${String(r.p50).padStart(3)} ms  p95 ${String(r.p95).padStart(3)} ms  max ${String(r.max).padStart(4)} ms  lange Aufgaben ${r.langProzent} %`);
  await b.close();
})();
