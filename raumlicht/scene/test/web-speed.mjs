// Misst den Start der WebApp: node test/web-speed.mjs <webapp-Ordner> [alles-komprimiert|wie-server] [Ordner für ein Foto des Ladebilds]
// Liefert die Dateien wie der echte Server aus (gzip für .html und .wasm; mit „alles-komprimiert“ auch für .js, .ttf, .json)
// und bremst das Netz auf die Werte der Chrome-Entwicklerwerkzeuge: „Fast 4G“ (9 Mbit/s, 60 ms) und „Slow 4G“ (1,6 Mbit/s, 150 ms).
// Gemessen wird je Netz: erster Start (leerer Speicher) und zweiter Start (Service Worker hat alles) –
// Zeit bis zum ersten sichtbaren Inhalt, bis die App bedienbar ist, und die übertragenen Bytes.
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'fs'; import path from 'path'; import http from 'http'; import zlib from 'zlib';
const dir = path.resolve(process.argv[2]); const all = process.argv[3] === 'alles-komprimiert';
const types = { html: 'text/html', js: 'text/javascript', json: 'application/json', wasm: 'application/wasm', webmanifest: 'application/manifest+json', png: 'image/png', ttf: 'font/ttf', map: 'application/json' };
const zipped = new Map();
// Die Bremse sitzt im Testserver (alle Verbindungen teilen sich die Bandbreite, jede Anfrage wartet erst die Laufzeit ab) – so wird wirklich jedes Byte gebremst und gezählt.
let net = null, sent = 0; const queue = []; let pumping = false;
function pump() { if (pumping) return; pumping = true; const tick = 50;
  const step = () => { let budget = net ? net.down * tick / 1000 : Infinity; const active = queue.filter((j) => j.pos < j.data.length);
    if (!active.length) { pumping = false; queue.length = 0; return; }
    const share = Math.max(1, Math.floor(budget / active.length));
    for (const j of active) { const n = Math.min(share, j.data.length - j.pos); j.res.write(j.data.subarray(j.pos, j.pos + n)); j.pos += n; sent += n; if (j.pos >= j.data.length) j.res.end(); }
    setTimeout(step, tick); }; step(); }
const server = http.createServer((q, r) => { let f = path.join(dir, decodeURIComponent(q.url.split('?')[0])); if (fs.existsSync(f) && fs.statSync(f).isDirectory()) f = path.join(f, 'index.html');
  if (!fs.existsSync(f)) { r.writeHead(404); return r.end(); } const ext = f.split('.').pop(); const head = { 'Content-Type': types[ext] || 'application/octet-stream', 'Cache-Control': ext === 'wasm' ? 'max-age=31536000' : 'no-cache' };
  let data;
  if ((ext === 'html' || ext === 'wasm' || (all && ['js', 'ttf', 'json'].includes(ext))) && /gzip/.test(q.headers['accept-encoding'] || '')) {
    if (!zipped.has(f)) zipped.set(f, zlib.gzipSync(fs.readFileSync(f), { level: 6 })); head['Content-Encoding'] = 'gzip'; data = zipped.get(f); } else data = fs.readFileSync(f);
  head['Content-Length'] = data.length;
  setTimeout(() => { r.writeHead(200, head); queue.push({ res: r, data, pos: 0 }); pump(); }, net ? net.latency : 0); }).listen(8767);
const nets = { 'Fast 4G': { down: 9e6 / 8, latency: 60 }, 'Slow 4G': { down: 1.6e6 / 8, latency: 150 } };
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const s = (ms) => (ms / 1000).toFixed(1).replace('.', ',') + ' s'; const mb = (b) => (b / 1e6).toFixed(1).replace('.', ',') + ' MB';
console.log(`Ordner ${dir} · ${all ? 'alles komprimiert' : 'wie der Server heute (nur .html und .wasm komprimiert)'}`);
for (const [name, cfg] of Object.entries(nets)) {
  net = cfg; const ctx = await browser.newContext({ viewport: { width: 400, height: 860 } }); const page = await ctx.newPage();
  async function run(label) {
    sent = 0; const t0 = Date.now();
    await page.goto('http://localhost:8767/', { waitUntil: 'commit' });
    if (process.argv[4] && label === 'erster Start') setTimeout(() => page.screenshot({ path: `${process.argv[4]}/ladebild-${name.replace(' ', '')}.png` }).catch(() => {}), name === 'Fast 4G' ? 2500 : 9000);
    const first = await page.evaluate(() => new Promise((res) => { const got = performance.getEntriesByName('first-contentful-paint')[0]; if (got) return res(got.startTime);
      new PerformanceObserver((l) => { const e = l.getEntriesByName('first-contentful-paint')[0]; if (e) res(e.startTime); }).observe({ type: 'paint', buffered: true }); }));
    await page.waitForFunction(() => { const st = document.getElementById('start'); return !st || st.style.display === 'none' || st.dataset.fertig === '1'; }, null, { timeout: 600000, polling: 100 });
    const usable = Date.now() - t0; const atUsable = sent;
    console.log(`${name.padEnd(8)} ${label.padEnd(13)} erster Inhalt ${s(first).padStart(7)} · bedienbar ${s(usable).padStart(7)} · bis dahin übertragen ${mb(atUsable).padStart(8)}`);
  }
  await run('erster Start');
  // warten, bis der Service Worker alles gespeichert hat (im Hintergrund), dann zweiter Start
  await page.waitForFunction(() => navigator.serviceWorker.controller, null, { timeout: 600000 }).catch(() => {});
  let last = -1; for (let k = 0; k < 400; k++) { await page.waitForTimeout(2500); if (sent === last) break; last = sent; }
  console.log(`${name.padEnd(8)} insgesamt beim ersten Besuch übertragen (mit Speichern für später): ${mb(sent)}`);
  await run('zweiter Start');
  await ctx.close();
}
await browser.close(); server.close();
