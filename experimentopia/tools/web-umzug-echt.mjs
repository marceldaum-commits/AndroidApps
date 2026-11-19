// Echter Rundlauf gegen die ausgelieferten Test-Server (keine Dateien von hier):
//   experimentopia-test.… mit Lernstand → lernen-test.…/experimentopia/ → Lernstand da → Umzugsdaten am Server gelöscht.
// Alle Anfragen des Browsers gehen über Node (fetch) an die echten Server – so gelten die Netzwerk-Regeln dieser Umgebung.
//   NODE_USE_ENV_PROXY=1 node tools/web-umzug-echt.mjs
import { createRequire } from 'node:module';
import path from 'node:path';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const ALT = 'https://experimentopia-test.mathenachhilfe-online.com/';
const NEU = 'https://lernen-test.mathenachhilfe-online.com/experimentopia/';
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
const LERNSTAND = { 'experimentopia.ergebnisse': '{"x1":3}', 'experimentopia.forscherbuch': '[{"t":"Rundlauf ' + new Date().toISOString() + '"}]', 'experimentopia.sterne': '7' };
const b = await chromium.launch();
const ctx = await b.newContext(); const log = [];
await ctx.addInitScript((seed) => { if (location.host.startsWith('experimentopia-test') && !sessionStorage.getItem('s')) { sessionStorage.setItem('s', '1'); for (const k in seed) localStorage.setItem(k, seed[k]); } }, LERNSTAND);
await ctx.route('**/*', async (rt) => {
  const r = rt.request(); const u = r.url();
  if (!u.startsWith('https://')) return rt.continue();
  const h = { ...r.headers() }; delete h['host'];
  try {
    const res = await fetch(u, { method: r.method(), headers: h, body: ['GET', 'HEAD', 'OPTIONS'].includes(r.method()) ? undefined : r.postDataBuffer() });
    const body = Buffer.from(await res.arrayBuffer());
    if (u.includes('/eltern/v1/s/')) log.push({ m: r.method(), status: res.status, url: u });
    const hd = {}; res.headers.forEach((v, k) => { if (!['content-encoding', 'content-length', 'transfer-encoding'].includes(k)) hd[k] = v; });
    await rt.fulfill({ status: res.status, headers: hd, body });
  } catch (e) { await rt.abort('failed'); }
});
const p = await ctx.newPage();
let weiter = '';
p.on('framenavigated', (f) => { if (f === p.mainFrame() && f.url().includes('#umzug=')) weiter = f.url(); });
await p.goto(ALT + '#thema=optik');
await p.waitForFunction(() => location.host.startsWith('lernen-test') && document.getElementById('start') && document.getElementById('start').dataset.fertig === '1', null, { timeout: 180000 });
await p.waitForTimeout(2000);
const st = await p.evaluate(() => ({ url: location.href, v: document.documentElement.dataset.version, ls: Object.fromEntries(Object.keys(localStorage).map((k) => [k, localStorage.getItem(k)])) }));
ok(st.url.startsWith(NEU), `angekommen: ${st.url} (Version ${st.v})`);
ok(/#umzug=[\w-]{22}\.[\w-]{43}\.[\w-]{22}&thema=optik$/.test(weiter), 'Weiterleitung im Format #umzug=<id>.<key>.<schreib>&thema=optik');
ok(Object.keys(LERNSTAND).every((k) => st.ls[k] === LERNSTAND[k]), 'Lernstand an der neuen Adresse vollständig');
const put = log.find((x) => x.m === 'PUT'), get = log.find((x) => x.m === 'GET'), del = log.find((x) => x.m === 'DELETE');
ok(put && put.status === 204, `PUT ${put && put.status} (${put && new URL(put.url).host})`);
ok(get && get.status === 200, `GET ${get && get.status}`);
ok(del && del.status === 204, `DELETE ${del && del.status}`);
const nach = put ? (await fetch(put.url)).status : 0; ok(nach === 404, `Umzugsdaten am Server gelöscht (GET danach ${nach})`);
await p.screenshot({ path: path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', 'screenshots', '95_umzug_echt_lernen_test.png') });
await b.close();
