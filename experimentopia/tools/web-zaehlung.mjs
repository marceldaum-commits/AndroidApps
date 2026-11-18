// Anonyme Zählung im Browser prüfen (Wasm und JS): genau ein „start“, text/plain, ohne Cookies, nur app/v/ev;
// Schalter aus → kein Aufruf. Zählaufrufe werden abgefangen (gehen nicht an den Server).
import { createRequire } from 'node:module';
import path from 'node:path';
import { spawn } from 'node:child_process';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', 'webapp');
const server = spawn('python3', ['-m', 'http.server', '8793', '--bind', '127.0.0.1'], { cwd: root, stdio: 'ignore' });
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
await new Promise((r) => setTimeout(r, 800));
try {
  const b = await chromium.launch();
  async function run(q, init) {
    const ctx = await b.newContext(); const p = await ctx.newPage(); const calls = [];
    if (init) await p.addInitScript(init);
    await p.route('**/zaehler/v1/e', async (rt) => { const r = rt.request(); calls.push({ url: r.url(), body: r.postData(), type: r.headers()['content-type'], cookie: r.headers()['cookie'] }); await rt.fulfill({ status: 204 }); });
    await p.goto('http://localhost:8793/' + q); await p.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 90000 }); await p.waitForTimeout(2500);
    await ctx.close(); return calls;
  }
  for (const q of ['', '?modus=js']) {
    const c = await run(q);
    const bodies = c.map((x) => JSON.parse(x.body));
    ok(c.length === 1 && bodies[0].ev === 'start' && bodies[0].app === 'experimentopia', `${q || 'Wasm'}: genau ein „start“ (${c.map((x) => x.body).join(' ')})`);
    ok(c.every((x) => (x.type || '').startsWith('text/plain') && !x.cookie), `${q || 'Wasm'}: text/plain, ohne Cookies`);
    ok(bodies.every((o) => Object.keys(o).every((k) => ['app', 'v', 'ev', 'ort', 'thema'].includes(k))), `${q || 'Wasm'}: nur app/v/ev/ort/thema – keine Kennung`);
    ok(c.every((x) => x.url.includes('trainer.') || x.url.includes('trainer-test.')), `${q || 'Wasm'}: Ziel ${c[0] && new URL(c[0].url).host}`);
  }
  const off = await run('', () => localStorage.setItem('experimentopia.zaehlung', '0'));
  ok(off.length === 0, `Schalter aus → kein Aufruf (${off.length})`);
  await b.close();
} finally { server.kill(); }
