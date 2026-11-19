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
const URL_ARG = (process.argv.find((x) => x.startsWith('--url=')) || '').slice(6);
const base = URL_ARG ? (URL_ARG.endsWith('/') ? URL_ARG : URL_ARG + '/') : `http://127.0.0.1:${server.address().port}/`;
if (URL_ARG) console.log('Prüfe gegen ' + base);
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

async function playPackRound(page, packId, shot) {
  if (packId) await page.click(`[data-pack="${packId}"]`);
  for (let guard = 0; guard < 80; guard++) {
    await wait(250);
    if (await page.$('.reward')) break;
    if (await page.$('[data-next]') && !(await page.$('.progress'))) { await page.click('[data-next]'); continue; } // Einführung
    const title = await page.$eval('.game-title', (e) => e.textContent).catch(() => '');
    if (shot && !shotDone.has(shot + title.slice(0, 5))) { shotDone.add(shot + title.slice(0, 5)); await snap(page, shot + '_' + title.replace(/[^A-Za-zÄÖÜäöü]+/g, '').slice(0, 12).toLowerCase()); }
    const b = await page.$('.balloon[data-ok]:not([disabled])'); if (b) { await b.click({ force: true }); await wait(1500); continue; }
    const o = await page.$('.opt[data-ok]:not([disabled])'); if (o) { await o.click(); await wait(1500); continue; }
    const tiles = await page.$$('.tile[data-pos]:not([disabled])');
    if (tiles.length) {
      const n = await page.$$eval('.slot', (x) => x.length);
      for (let p = 0; p < n; p++) { const t = await page.$(`.tile[data-pos="${p}"]:not([disabled])`); if (t) await t.click(); }
      await wait(1600); continue;
    }
    const mems = await page.$$('.mem:not(.up)');
    if (mems.length) {
      const pairs = await page.$$eval('.mem', (x) => x.map((e) => e.dataset.pair));
      for (const pr of [...new Set(pairs)]) { const two = await page.$$(`.mem[data-pair="${pr}"]:not(.up)`); for (const t of two) await t.click(); await wait(250); }
      await wait(1800); continue;
    }
    if (guard % 10 === 9) console.log('    … hängt bei:', (await page.evaluate(() => document.querySelector('#app').innerText)).replace(/\s+/g, ' ').slice(0, 160));
  }
  const ok = !!(await page.$('.reward'));
  if (ok && shot) await snap(page, shot + '_ende');
  return ok;
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
    const st = await page.evaluate(() => window.quasselTest.state());
    ok(Object.values(st.progress).filter((p) => p.done).length === 3, '3 Stempel gespeichert');
    ok(st.settings.recognition === 'device', 'Spracherkennung standardmäßig nur auf dem Gerät');
    const played = await page.evaluate(() => window.quasselTest.A.played || 0);
    ok(played > 20, `Vertonte Sätze abgespielt (${played} Mal)`);
    ok(!(await page.$('[data-novoice]')), 'Kein Ersatz-Text nötig (alle Sätze vertont)');
    ok(st.week && st.week.n === 3, 'Wochenziel gezählt (3 Episoden)');
    await page.reload(); await page.waitForSelector('.world');
    ok((await page.textContent('.pill')).includes('3'), 'Fortschritt nach Neuladen noch da');
    await snap(page, 'hafen_fertig');
    // Sprachen: je eine Runde Wörter & Regeln
    for (const [lang, pack] of [['en', 'gs-englisch-basics'], ['de', 'gs-rechtschreibung'], ['fr', 's1-franzoesisch-grundwortschatz'], ['es', 's1-spanisch-grundwortschatz'], ['la', 's1-latein-grundwortschatz']]) {
      await page.click(`[data-lang="${lang}"]`); await page.waitForSelector(`[data-pack="${pack}"]`);
      if (FOTOS && name === 'chromium_handy') await snap(page, 'sprache_' + lang);
      ok(await playPackRound(page, pack, FOTOS && name === 'chromium_handy' && (lang === 'fr' || lang === 'de') ? 'paket_' + lang : null), `Paket-Runde ${lang} (${pack}) durchgespielt`);
      await page.click('[data-home]'); await page.waitForSelector('.langbar');
    }
    // Mein Schulbuch: eigene Liste anlegen (erst mit Fehler), spielen – Englisch und Deutsch (Diktat)
    for (const [lang, bad, good] of [['en', 'Hund dog\nKatze = cat', 'Hund = dog\nKatze = cat\nMaus = mouse\nBaum = tree\nHaus = house'], ['de', 'Hund = dog', 'Fahrrad\nVogel\nStraße\nBlume']]) {
      await page.click(`[data-lang="${lang}"]`); await page.waitForSelector('[data-mine]'); await page.click('[data-mine]');
      await page.click('[data-new]'); await page.fill('[data-name]', 'Lektion 1'); await page.fill('[data-text]', bad); await page.click('[data-save]');
      ok((await page.textContent('[data-err]')).length > 5, `Mein Schulbuch ${lang}: falsche Zeile → Meldung „${(await page.textContent('[data-err]')).slice(0, 50)}“`);
      await page.fill('[data-text]', good); await page.click('[data-save]'); await page.waitForSelector('[data-play]');
      if (FOTOS && name === 'chromium_handy') await snap(page, 'schulbuch_' + lang);
      await page.click('[data-play]');
      ok(await playPackRound(page, null, FOTOS && name === 'chromium_handy' && lang === 'de' ? 'schulbuch_runde_de' : null), `Mein Schulbuch ${lang}: Runde gespielt`);
      await page.click('[data-home]'); await page.waitForSelector('[data-new]'); await page.click('[data-back]'); await page.waitForSelector('.langbar');
    }
    const st2 = await page.evaluate(() => window.quasselTest.state());
    ok(Object.keys(st2.karten).length >= 20, `Wiederholung gespeichert (${Object.keys(st2.karten).length} Karten)`);
    await page.click('[data-lang="en"]'); await page.waitForSelector('.world');
    // Zählung + Nachhilfe-Hinweis (nur Elternbereich)
    const calls = [];
    page.on('request', (r) => { if (r.url().includes('/zaehler/v1/e')) try { calls.push(JSON.parse(r.postData())); } catch (e) {} });
    await page.route('**/zaehler/v1/e', (r) => r.fulfill({ status: 204, body: '' }));   // in WebKit nicht immer wirksam – gezählt wird über 'request'
    await page.evaluate(() => { const st = window.quasselTest.state(); const d = window.PAKETE.today(); st.tage.fr = { [d]: [12, 4] }; });
    ok(!(await page.$('.hint-card')), 'Kein Nachhilfe-Hinweis im Kinderbereich');
    await page.click('[data-settings]'); await snap(page, 'einstellungen');
    await page.click('[data-kids]'); ok((await page.textContent('#app')).includes('kein Konto'), 'Datenschutz in Kindersprache erreichbar');
    if (FOTOS && name === 'chromium_handy') await snap(page, 'daten_kinder');
    await page.click('[data-back]'); await page.waitForSelector('[data-parents]');
    await page.click('[data-parents]'); await page.fill('.modal input', '2015'); await page.click('.modal [data-ok]');
    ok((await page.textContent('.modal .err')).includes('Erwachsene'), 'Eltern-Tor hält Kinder ab');
    await page.fill('.modal input', '1985'); await page.click('.modal [data-ok]'); await page.waitForSelector('input[name=rec]');
    ok(!!(await page.$('.hint-card')) && (await page.getAttribute('[data-probe]', 'href')).includes('src=quasselquest-elternbericht&thema=franzoesisch'), 'Nachhilfe-Hinweis im Elternbereich (Französisch hakt), Link mit src/thema/klasse');
    for (let w = 0; w < 30 && !calls.some((c) => c.ev === 'nachhilfe_hinweis'); w++) await page.waitForTimeout(200);   // WebKit meldet Anfragen verzögert
    ok(calls.some((c) => c.ev === 'elternbereich') && calls.some((c) => c.ev === 'nachhilfe_hinweis' && c.thema === 'franzoesisch') && calls.every((c) => Object.keys(c).every((k) => ['app', 'v', 'ev', 'ort', 'thema'].includes(k)) && c.app === 'quasselquest'), 'Zählung: nur erlaubte Felder (' + calls.map((c) => c.ev).join(', ') + ')');
    if (FOTOS && name === 'chromium_handy') await snap(page, 'eltern_hinweis');
    await page.click('[data-hide]'); await page.waitForSelector('input[name=rec]');
    ok(!(await page.$('.hint-card')), 'Hinweis ausgeblendet');
    await page.click('[data-sw="zaehlung"]'); await page.waitForTimeout(1500); const n0 = calls.length;
    await page.click('[data-back]'); await page.click('[data-parents]'); await page.fill('.modal input', '1985'); await page.click('.modal [data-ok]'); await page.waitForSelector('input[name=rec]'); await page.waitForTimeout(1500);
    ok(calls.length === n0, 'Zählung aus → kein Aufruf');
    await page.click('[data-sw="zaehlung"]');
    await snap(page, 'eltern');
    await page.click('[data-back]'); await page.click('[data-back]'); await page.waitForSelector('.world');
    await page.click('[data-allapps]').catch(() => {});
    await page.waitForURL(/lern-apps/, { timeout: 5000 }).then(() => ok(page.url().includes('src=quasselquest-kopf') && page.url().includes('staging.'), '„← Alle Apps“ → ' + page.url())).catch(() => ok(false, '„← Alle Apps“ öffnet die Übersicht'));
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
