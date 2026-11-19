// Starttest + Durchspielen in Chromium und WebKit (iPhone/iPad-Ersatz). Aufruf: node tools/web-start.mjs [--fotos]
// Prüft: App startet, keine Fehler in der Konsole, alle 3 Episoden durchspielbar, Fortschritt bleibt, offline startet die App.
import { createServer } from 'node:http';
import { readFile, mkdir } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const { chromium, webkit, devices } = require('playwright');

const root = fileURLToPath(new URL('../webapp/', import.meta.url));
const shots = fileURLToPath(new URL('../screenshots/', import.meta.url));
const FOTOS = process.argv.includes('--fotos');
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json', '.webmanifest': 'application/manifest+json', '.svg': 'image/svg+xml', '.png': 'image/png' };
const server = createServer(async (req, res) => {
  let p = decodeURIComponent(new URL(req.url, 'http://x').pathname); if (p.endsWith('/')) p += 'index.html';
  const f = normalize(join(root, p)); if (!f.startsWith(root)) { res.writeHead(403); res.end(); return; }
  try { const b = await readFile(f); res.writeHead(200, { 'Content-Type': types[extname(f)] || 'application/octet-stream', 'Cache-Control': 'no-cache' }); res.end(b); }
  catch { res.writeHead(404); res.end('nicht da'); }
});
await new Promise((r) => server.listen(0, '127.0.0.1', r));
const base = `http://127.0.0.1:${server.address().port}/`;
if (FOTOS) await mkdir(shots, { recursive: true });

let failed = 0;
const ok = (c, msg) => { console.log((c ? '  ✅ ' : '  ❌ ') + msg); if (!c) failed++; };
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

async function playEpisode(page, id, shot) {
  await page.click(`[data-ep="${id}"]`);
  // Szene
  for (let i = 0; i < 8; i++) { if (!(await page.$('[data-again]'))) break; if (i === 1 && shot) await snap(page, shot + '_szene'); await page.click('[data-next]'); await wait(150); }
  for (let guard = 0; guard < 120; guard++) {
    await wait(250);
    if (await page.$('.reward')) break;
    const title = await page.$eval('.game-title', (e) => e.textContent).catch(() => '');
    if (shot && !shotDone.has(shot + title.slice(0, 6))) { shotDone.add(shot + title.slice(0, 6)); await snap(page, shot + '_' + title.replace(/[^A-Za-zÄÖÜäöü]+/g, '').slice(0, 14).toLowerCase()); }
    if (await page.$('.modal input')) { await snap(page, 'name_fragen'); await page.fill('.modal input', 'Max'); await page.click('.modal [data-ok]'); continue; } // Name wird erst in der Mission gefragt
    if (await page.$('.btn.green')) { await page.click('.btn.green'); continue; }                      // Zum Stempel
    if (await page.$('.picker button')) { await page.click('.picker button:nth-child(3)'); continue; } // Zahl/Farbe wählen
    if (await page.$('.card [data-quiet]')) { await page.click('.card [data-quiet]'); continue; }    // Mission: leise sagen
    if (await page.$('.opt[data-ok]')) { await page.click('.opt[data-ok]'); continue; }
    if (await page.$('[data-trick]')) { await wait(4600); continue; }                                  // Ruby hat nicht „Ruby says“ gesagt
    const okBtn = await page.$('.choice[data-ok]:not([disabled])'); if (okBtn) { await okBtn.click(); await wait(1200); continue; }
    const want = await page.$eval('[data-train]', (e) => e.dataset.want).catch(() => null);
    if (want && !(await page.$('.train.go'))) {
      const words = want.split(/\s+/);
      for (const w of words) {
        const btns = await page.$$('.pool .wagon:not(.used)');
        for (const b of btns) if ((await b.textContent()) === w) { await b.click(); break; }
      }
      await wait(2100); continue;
    }
    const nextBtn = await page.$('[data-next]:not([hidden])'); if (nextBtn && await nextBtn.isVisible()) { await nextBtn.click(); continue; }
    const quiet = await page.$('[data-quiet]'); if (quiet) { await quiet.click(); continue; }
  }
  const r = !!(await page.$('.reward'));
  if (r && shot) await snap(page, shot + '_stempel');
  return r;
}
const shotDone = new Set();
let snapN = 0; let curName = '';
async function snap(page, name) { if (!FOTOS) return; await wait(450); await page.screenshot({ path: `${shots}${curName}_${String(++snapN).padStart(2, '0')}_${name}.png` }); }

for (const [name, type, opts] of [
  ['chromium_handy', chromium, { viewport: { width: 390, height: 844 }, deviceScaleFactor: FOTOS ? 2 : 1, hasTouch: true, isMobile: true }],
  ['webkit_iphone', webkit, { ...devices['iPhone 13'] }],
  ['webkit_ipad', webkit, { ...devices['iPad (gen 7)'] }],
  ['chromium_pc', chromium, { viewport: { width: 1280, height: 800 }, deviceScaleFactor: FOTOS ? 2 : 1 }],
]) {
  console.log(`\n${name}`); curName = name; snapN = 0; shotDone.clear();
  const browser = await type.launch();
  const ctx = await browser.newContext(opts);
  const page = await ctx.newPage();
  const errors = [];
  // nur Fehler unserer App zählen – nicht die der Website, auf die „← Alle Apps“ führt
  page.on('pageerror', (e) => { if (page.url().startsWith(base)) errors.push(String(e)); });
  page.on('console', (m) => { if (m.type() === 'error' && page.url().startsWith(base)) errors.push(m.text()); });
  const t0 = Date.now();
  await page.goto(base + '?diag=1');
  await page.waitForSelector('[data-k]', { timeout: 10000 });
  ok(true, `Start in ${Date.now() - t0} ms (Klassenwahl sichtbar)`);
  if (name.endsWith('pc') || name.endsWith('ipad')) {
    await snap(page, 'start');
    await page.click('[data-k="4"]'); await page.click('[data-go]');
    await page.waitForSelector('.world'); await snap(page, 'hafen');
    ok(await playEpisode(page, 'w1e1', 'e1'), 'Episode 1 durchgespielt');
  } else {
    await snap(page, 'start');
    await page.fill('[data-name]', 'Lena1'); await page.click('[data-k="3"]'); await page.click('[data-go]');
    ok((await page.textContent('[data-err]')).includes('nur Buchstaben'), 'Unpassender Name → verständliche Meldung');
    await page.fill('[data-name]', 'Lena'); await page.click('[data-go]');
    await page.waitForSelector('.world'); await snap(page, 'hafen');
    for (const [id, s] of [['w1e1', 'e1'], ['w1e2', 'e2'], ['w1e3', 'e3']]) {
      ok(await playEpisode(page, id, FOTOS && name === 'chromium_handy' ? s : null), `Episode ${id} durchgespielt`);
      await page.click('[data-home]'); await page.waitForSelector('.world');
    }
    const st = await page.evaluate(() => window.englischTest.state());
    ok(Object.values(st.progress).filter((p) => p.done).length === 3, '3 Stempel gespeichert');
    ok(st.settings.recognition === 'device', 'Spracherkennung standardmäßig nur auf dem Gerät');
    ok(st.week && st.week.n === 3, 'Wochenziel gezählt (3 Episoden)');
    await page.reload(); await page.waitForSelector('.world');
    ok((await page.textContent('.pill')).includes('3'), 'Fortschritt nach Neuladen noch da');
    await snap(page, 'hafen_fertig');
    await page.click('[data-settings]'); await snap(page, 'einstellungen');
    await page.click('[data-parents]'); await page.fill('.modal input', '2015'); await page.click('.modal [data-ok]');
    ok((await page.textContent('.modal .err')).includes('Erwachsene'), 'Eltern-Tor hält Kinder ab');
    await page.fill('.modal input', '1985'); await page.click('.modal [data-ok]'); await page.waitForSelector('input[name=rec]');
    await snap(page, 'eltern');
    await page.click('[data-back]'); await page.click('[data-back]'); await page.waitForSelector('.world');
    await page.click('[data-allapps]').catch(() => {});
    await page.waitForURL(/lern-apps/, { timeout: 5000 }).then(() => ok(page.url().includes('src=englisch-kopf') && page.url().includes('staging.'), '„← Alle Apps“ → ' + page.url())).catch(() => ok(false, '„← Alle Apps“ öffnet die Übersicht'));
    // Offline: nach dem ersten Laden ohne Netz starten
    await page.goto(base);
    const sw = await page.evaluate(async () => { if (!navigator.serviceWorker) return 'kein SW'; const r = await navigator.serviceWorker.ready; return r.active ? 'aktiv' : 'nicht aktiv'; }).catch((e) => String(e));
    if (sw === 'aktiv') {
      await wait(500); await ctx.setOffline(true);
      await page.reload().catch(() => {});
      ok(!!(await page.waitForSelector('.world', { timeout: 6000 }).catch(() => null)), 'Startet offline');
      await ctx.setOffline(false);
    } else console.log('  ⚠️  Offline-Test übersprungen: Service Worker ' + sw);
  }
  const real = errors.filter((e) => !/net::ERR_INTERNET_DISCONNECTED|Failed to load resource|lern-apps/.test(e));
  ok(real.length === 0, 'Keine Fehler in der Konsole' + (real.length ? ': ' + real.join(' | ') : ''));
  await browser.close();
}
server.close();
console.log(failed ? `\n❌ ${failed} Prüfung(en) fehlgeschlagen` : '\n✅ Alles grün');
process.exit(failed ? 1 : 0);
