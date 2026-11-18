// Rundlauf-Test „Eltern-Handy koppeln“ (Playwright, Wasm und JS):
// Profil mit etwas Lernstand anlegen → koppeln → der hochgeladene Bericht (PUT, abgefangen) lässt sich mit dem Schlüssel
// aus dem Eltern-Link wieder entschlüsseln und ist gültiges JSON (Format 1, nur erlaubte Felder) → Kopplung aufheben = DELETE,
// Werte lokal gelöscht. Ohne Kopplung wird nichts an /eltern/ gesendet.
// Aufruf: node web-eltern.cjs [<url>]   (Standard http://127.0.0.1:8812/ – dort gilt die Test-Stufe: trainer-test. / staging.)
const { chromium } = require('playwright');
const crypto = require('crypto');
const BASE = process.argv[2] || 'http://127.0.0.1:8812/';
let ok = true;
const check = (c, t, x) => { console.log((c ? '✔ ' : '✘ ') + t + (x !== undefined ? ' – ' + JSON.stringify(x).slice(0, 300) : '')); if (!c) ok = false; };
const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64');

const now = Date.now();
const profil = { id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: now - 864e5 * 20 };
const store = {
  profiles: [profil],
  beds: [{ id: 'b1', profileId: 'p1', name: 'Das kleine Einmaleins', emoji: '✖️', hue: 30, packId: 'gs-einmaleins', createdAt: now - 864e5 * 20 }],
  cards: Array.from({ length: 8 }, (_, i) => ({ id: 'c' + i, bedId: 'b1', profileId: 'p1', front: i + ' · 3', back: String(i * 3), reps: 2, stability: i < 4 ? 12 : 2, lastReview: now - 864e5, createdAt: now - 864e5 * 20 })),
  logs: Array.from({ length: 12 }, (_, i) => ({ id: i + 1, profileId: 'p1', cardId: 'c' + (i % 8), time: now - 864e5 * (i % 4), rating: i % 3 === 0 ? 0 : 2, stabilityAfter: 3 })),
  nextLogId: 13,
};

(async () => {
  for (const modus of ['wasm', 'js']) {
    const b = await chromium.launch();
    const ctx = await b.newContext();
    const calls = [];
    await ctx.route('**/eltern/v1/**', async (r) => { const q = r.request(); calls.push({ url: q.url(), method: q.method(), headers: q.headers(), body: q.postData() }); await r.fulfill({ status: 204, body: '' }); });
    await ctx.route('https://trainer*.mathenachhilfe-online.com/zaehler/**', (r) => r.fulfill({ status: 204, body: '' }));
    const p = await ctx.newPage();
    await p.addInitScript((s) => {
      if (sessionStorage.getItem('vorbereitet')) return;
      sessionStorage.setItem('vorbereitet', '1');
      localStorage.setItem('wg.garten', JSON.stringify(s));
      localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true } }));
    }, store);
    await p.goto(BASE + '?diag=1&modus=' + modus);
    await p.waitForFunction(() => window.wgReady && window.wgElternTest, null, { timeout: 120000 });
    await p.waitForTimeout(2000);
    check(calls.length === 0, `${modus}: ohne Kopplung nichts gesendet`, calls.length);
    await p.evaluate(() => window.wgElternTest());
    const res = await p.waitForFunction(() => window.wgElternErgebnis, null, { timeout: 30000 }).then((h) => h.jsonValue());
    const [status, link] = res.split(' ');
    check(status === '204', `${modus}: Hochladen 204`, res);
    const m = /^https:\/\/staging\.mathenachhilfe-online\.com\/eltern-bericht\/#([A-Za-z0-9_-]{22})\.([A-Za-z0-9_-]{43})$/.exec(link || '');
    check(!!m, `${modus}: Eltern-Link (Test-Stufe) mit id.key nach #`, link);
    const puts = calls.filter((c) => c.method === 'PUT');
    check(puts.length >= 1, `${modus}: PUT gesendet`, puts.length);
    if (m && puts.length) {
      const put = puts[puts.length - 1];
      check(put.url === 'https://trainer-test.mathenachhilfe-online.com/eltern/v1/b/' + m[1], `${modus}: richtige Adresse`, put.url);
      check(/^[A-Za-z0-9_-]{22}$/.test(put.headers['x-schreib'] || ''), `${modus}: X-Schreib`, put.headers['x-schreib']);
      check(!put.headers.cookie && !(put.url + put.body).includes(m[2]), `${modus}: kein Cookie, Schlüssel nicht gesendet`);
      check(/^text\/plain/.test(put.headers['content-type'] || ''), `${modus}: text/plain`);
      const [ivT, ctT] = put.body.split('.');
      check(ivT.length === 16 && put.body.length < 24 * 1024, `${modus}: IV 16 Zeichen, < 24 KB`, { iv: ivT.length, len: put.body.length });
      try {
        const ct = b64(ctT), d = crypto.createDecipheriv('aes-256-gcm', b64(m[2]), b64(ivT));
        d.setAuthTag(ct.subarray(ct.length - 16));
        const j = JSON.parse(Buffer.concat([d.update(ct.subarray(0, ct.length - 16)), d.final()]).toString('utf8'));
        check(j.format === 1 && j.app === 'wissensgarten' && j.appName === 'Wissensgarten' && /^\d{4}-\d\d-\d\dT/.test(j.stand), `${modus}: entschlüsselt, Pflichtfelder`, j);
        const erlaubt = ['format', 'app', 'appName', 'name', 'klasse', 'stand', 'zeitraum', 'lernzeitMin', 'tageAktiv', 'aufgaben', 'richtigProzent', 'staerken', 'schwach', 'fortschritt', 'letzte', 'klassenarbeit', 'hinweis'];
        check(Object.keys(j).every((k) => erlaubt.includes(k)), `${modus}: nur erlaubte Felder`, Object.keys(j));
        check(j.name === 'Testkind' && j.klasse === 3 && j.aufgaben > 0 && !JSON.stringify(j).includes('Lena'), `${modus}: Spitzname statt Profilname, Kennzahlen`, { name: j.name, klasse: j.klasse, aufgaben: j.aufgaben });
      } catch (e) { check(false, `${modus}: Entschlüsseln`, String(e)); }
    }
    await p.evaluate(() => window.wgElternAufheben());
    const r2 = await p.waitForFunction(() => window.wgElternErgebnis, null, { timeout: 30000 }).then((h) => h.jsonValue());
    const del = calls.filter((c) => c.method === 'DELETE');
    check(r2 === 'aufgehoben' && del.length === 1 && m && del[0].url.endsWith(m[1]), `${modus}: Kopplung aufheben → DELETE`, r2);
    const rest = await p.evaluate(() => localStorage.getItem('wg.prefs.einstellungen'));
    check(!/eltern_kopplung_p1"\s*:\s*\{[^}]*"v"\s*:\s*"[A-Za-z0-9_-]{10}/.test(rest), `${modus}: Werte lokal gelöscht`);
    await b.close();
  }
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
