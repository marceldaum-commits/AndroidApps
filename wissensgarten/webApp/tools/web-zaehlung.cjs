// Prüft die anonyme Nutzungszählung der WebApp (Playwright, Wasm und JS):
// - beim Öffnen genau ein Aufruf „start“ an trainer.mathenachhilfe-online.com/zaehler/v1/e, text/plain, ohne Cookies
// - der Inhalt hat nur app, v, ev (ort, thema) – keine Kennung, kein Name, kein Lernstand
// - ist der Schalter „Anonyme Nutzungszählung“ aus (Einstellungen), wird nichts gesendet
// - der Freischalt-Link ?pro=<ungültig> zählt nichts
// Aufruf: node web-zaehlung.cjs [<url>]   (Standard http://127.0.0.1:8812/)
const { chromium } = require('playwright');
const BASE = process.argv[2] || 'http://127.0.0.1:8812/';
let ok = true;
const check = (cond, text, extra) => { console.log((cond ? '✔ ' : '✘ ') + text + (extra !== undefined ? ' – ' + JSON.stringify(extra) : '')); if (!cond) ok = false; };

async function open(modus, prep) {
  const b = await chromium.launch();
  const ctx = await b.newContext();
  const calls = [];
  await ctx.route('https://trainer.mathenachhilfe-online.com/**', async (r) => {
    const q = r.request();
    calls.push({ url: q.url(), method: q.method(), type: q.headers()['content-type'], cookie: q.headers()['cookie'] || null, body: q.postData() });
    await r.fulfill({ status: 204, body: '' });
  });
  const p = await ctx.newPage();
  if (prep) await p.addInitScript(prep);
  await p.goto(BASE + '?modus=' + modus);
  await p.waitForFunction(() => window.wgReady, null, { timeout: 120000 });
  await p.waitForTimeout(3000);
  return { b, p, calls };
}

(async () => {
  for (const modus of ['wasm', 'js']) {
    const { b, calls } = await open(modus);
    const start = calls.filter((c) => c.body && c.body.includes('"start"'));
    check(start.length === 1, `${modus}: ein Aufruf „start“`, calls.map((c) => c.body));
    if (start[0]) {
      const c = start[0];
      check(c.url === 'https://trainer.mathenachhilfe-online.com/zaehler/v1/e' && c.method === 'POST', `${modus}: richtiger Endpunkt`, c.url);
      check(/^text\/plain/.test(c.type || ''), `${modus}: text/plain`, c.type);
      check(!c.cookie, `${modus}: keine Cookies`);
      const j = JSON.parse(c.body);
      check(JSON.stringify(Object.keys(j).sort()) === '["app","ev","v"]' && j.app === 'wissensgarten', `${modus}: nur app, v, ev`, j);
    }
    await b.close();
  }
  // Schalter aus → nichts
  const off = await open('wasm', () => {
    try { localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ zaehlung: { t: 'b', v: false } })); } catch (e) {}
  });
  check(off.calls.length === 0, 'Schalter aus: kein Aufruf', off.calls.map((c) => c.body));
  await off.b.close();
  console.log(ok ? '\nAlle Prüfungen bestanden' : '\nPrüfungen fehlgeschlagen');
  process.exit(ok ? 0 : 1);
})();
