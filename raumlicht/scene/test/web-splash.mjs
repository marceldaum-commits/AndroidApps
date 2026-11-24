// Startanimation (Auftrag aller Lern-Apps, 22.11.2026): schwarzer Vollbild-Splash #05070f mit weißem Logo (96 px, atmet), steht inline im HTML, gibt das Ladebild mit Störungsanzeige nie auf;
// verschwindet, sobald die App bereit ist, bei einer Störung oder nach höchstens 4 s – danach kein Element, keine Klicksperre. reduced-motion: keine Bewegung.
// node test/web-splash.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const server = http.createServer((q, r) => { let p = decodeURIComponent(q.url.split('?')[0]); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : /\.m?js$/.test(f) ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } }); });
await new Promise((ok) => server.listen(0, ok)); const url = `http://localhost:${server.address().port}/`;
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
const html = fs.readFileSync(path.join(dir, 'index.html'), 'utf8'), man = JSON.parse(fs.readFileSync(path.join(dir, 'manifest.webmanifest'), 'utf8'));
check(man.background_color.toLowerCase() === '#05070f', 'Manifest: background_color #05070f');
check(/<div id="splash" aria-hidden="true"><svg[^>]*viewBox="0 0 2000 2000"[^>]*><path fill="currentColor"/.test(html) && !/<div id="splash"[^>]*>[^]*?<(img|link|script)[^>]*src=/.test(html.slice(html.indexOf('<div id="splash"'), html.indexOf('<div id="splash"') + 7000)), 'Splash steht inline im HTML (SVG, aria-hidden, ohne Netzabruf)');
for (const modus of ['wasm', 'js']) {
  // ── 1. normaler Start ──
  { const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
    const first = await (async () => { await page.goto(url + `?modus=${modus}`, { waitUntil: 'commit' }); await page.waitForSelector('#splash', { timeout: 10000 });
      return page.evaluate(() => { const s = document.getElementById('splash'), v = s.querySelector('svg'), cs = getComputedStyle(s), r = v.getBoundingClientRect(); return { bg: cs.backgroundColor, w: Math.round(r.width), h: Math.round(r.height), anim: getComputedStyle(v).animationName, hidden: s.getAttribute('aria-hidden'), z: cs.zIndex, fill: getComputedStyle(v.querySelector('path')).fill, mitte: Math.abs(r.left + r.width / 2 - innerWidth / 2) < 1 && Math.abs(r.top + r.height / 2 - innerHeight / 2) < 1 }; }); })();
    check(first.bg === 'rgb(5, 7, 15)' && Math.abs(first.w - 96) <= 12 && Math.abs(first.h - 96) <= 12 && first.hidden === 'true' && first.mitte, `${modus}: sofort schwarz (#05070f), Logo 96 px mittig, aria-hidden`);
    check(first.anim === 'atmen' && first.fill === 'rgb(255, 255, 255)', `${modus}: Logo ist weiß und atmet („atmen“, 1,1 s)`);
    await page.screenshot({ path: `${out}/${modus}-1-splash.png` });
    const t0 = Date.now(); await page.waitForFunction(() => document.getElementById('start') && document.getElementById('start').dataset.fertig === '1', null, { timeout: 180000 }); const bereit = Date.now() - t0;
    await page.waitForTimeout(150); const zoom = await page.evaluate(() => { const s = document.getElementById('splash'); return s ? { cls: s.className, anim: getComputedStyle(s.querySelector('svg')).animationName } : null; });
    if (zoom) await page.screenshot({ path: `${out}/${modus}-2-zoom.png` });
    check(!zoom || (zoom.cls === 'weg' && zoom.anim === 'zoomraus') || zoom.cls === '', `${modus}: ist die App bereit, zieht sich das Logo zusammen und zoomt heraus („zoomraus“)`);
    await page.waitForFunction(() => !document.getElementById('splash'), null, { timeout: 4000 }).then(() => check(true, `${modus}: danach ist das Element ganz weg`), () => check(false, `${modus}: Element blieb stehen`));
    const klick = await page.evaluate(() => { const e = document.elementFromPoint(innerWidth / 2, innerHeight / 2); return e && e.closest && !!e.closest('#splash'); });
    check(!klick, `${modus}: keine Klicksperre`); await page.screenshot({ path: `${out}/${modus}-3-app.png` });
    check(errs.length === 0, errs.length ? `${modus}: Fehler: ${errs[0]}` : `${modus}: keine Fehler`); await ctx.close(); }
  // ── 2. hängender Start: nach höchstens 4 s ist der Splash weg, das Ladebild (Fortschritt) darunter bleibt sichtbar ──
  { const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage();
    await page.route(/\.wasm$|raumlicht(-js)?\.js$/, async (r) => { await new Promise((ok) => setTimeout(ok, 12000)); r.continue().catch(() => {}); });
    const t0 = Date.now(); await page.goto(url + `?modus=${modus}`, { waitUntil: 'commit' }); await page.waitForSelector('#splash');
    await page.waitForFunction(() => { const s = document.getElementById('splash'); return !s || s.className === 'weg'; }, null, { timeout: 8000 }); const dauer = Date.now() - t0;
    check(dauer >= 3500 && dauer <= 5200, `${modus}: hängt der Start, gibt der Splash nach 4 s frei (${dauer} ms)`);
    await page.waitForTimeout(1300); const rest = await page.evaluate(() => { const st = document.getElementById('start'), r = st.getBoundingClientRect(), mid = document.elementFromPoint(innerWidth / 2, innerHeight / 2);
      return { splash: !!document.getElementById('splash'), sichtbar: getComputedStyle(st).display !== 'none' && r.width > 0, text: document.getElementById('start-text').textContent.length > 0, oben: !mid || !mid.closest('#splash') }; });
    check(!rest.splash && rest.sichtbar && rest.text && rest.oben, `${modus}: das Ladebild mit Fortschritt ist danach zu sehen`); await page.screenshot({ path: `${out}/${modus}-4-haengt.png` }); await ctx.close(); }
  // ── 3. Störung (Start schlägt fehl): der Splash gibt sofort frei, die Fehleranzeige ist zu sehen ──
  { const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block' }); const page = await ctx.newPage();
    await page.route(/raumlicht(-js)?\.js$/, (r) => r.abort()); const t0 = Date.now(); await page.goto(url + `?modus=${modus}`, { waitUntil: 'commit' });
    await page.waitForFunction(() => document.getElementById('start-text') && document.getElementById('start-text').className === 'fehler', null, { timeout: 20000 });
    await page.waitForFunction(() => !document.getElementById('splash'), null, { timeout: 4000 }).catch(() => {}); const dauer = Date.now() - t0;
    const fehler = await page.evaluate(() => { const t = document.getElementById('start-text'), r = t.getBoundingClientRect(), e = document.elementFromPoint(r.left + r.width / 2, r.top + 10); return { sichtbar: r.width > 0 && !!e && !e.closest('#splash'), neuladen: /Neu laden/.test(t.textContent) }; });
    check(!(await page.evaluate(() => !!document.getElementById('splash'))) && fehler.sichtbar && fehler.neuladen, `${modus}: bei einer Störung ist die Meldung mit „Neu laden“ sofort zu sehen (nach ${dauer} ms)`); await page.screenshot({ path: `${out}/${modus}-5-stoerung.png` }); await ctx.close(); }
}
// ── 4. weniger Bewegung ──
{ const ctx = await b.newContext({ viewport: { width: 400, height: 860 }, serviceWorkers: 'block', reducedMotion: 'reduce' }); const page = await ctx.newPage();
  await page.goto(url, { waitUntil: 'commit' }); await page.waitForSelector('#splash'); const r1 = await page.evaluate(() => getComputedStyle(document.querySelector('#splash svg')).animationName);
  await page.waitForFunction(() => document.getElementById('start') && document.getElementById('start').dataset.fertig === '1', null, { timeout: 180000 }); await page.waitForTimeout(100);
  const r2 = await page.evaluate(() => { const s = document.getElementById('splash'); return s ? { anim: getComputedStyle(s.querySelector('svg')).animationName, dauer: getComputedStyle(s).transitionDuration, cls: s.className } : null; });
  check(r1 === 'none' && (!r2 || (r2.anim === 'none' && r2.dauer === '0.2s')), 'weniger Bewegung: kein Atmen, kein Zoomen, nur 0,2 s Ausblenden'); await ctx.close(); }
await b.close(); server.close(); console.log(fail ? `${fail} FEHLER` : 'web-splash: alle Prüfungen bestanden'); process.exit(fail ? 1 : 0);
