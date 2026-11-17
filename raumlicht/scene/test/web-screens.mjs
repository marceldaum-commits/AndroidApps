// Öffnet jeden Bildschirm der gebauten WebApp über den Direkt-Link ?ziel=… und prüft: keine Konsolenfehler, keine Ausnahme, die Oberfläche zeichnet etwas (kein leeres/weißes Bild).
// node test/web-screens.mjs <url> <outDir>          (RL_VIEWS="klein:320x640,ipad:820x1180" prüft jeden Bildschirm in diesen Größen)
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import { inflateSync } from 'zlib';
// Anteil weißer Bildpunkte und Zahl verschiedener Farben eines PNG (8 Bit RGB/RGBA, ohne Zeilensprung) – ohne fremde Bibliothek
function look(buf) {
  let o = 8, w = 0, h = 0, type = 0; const idat = [];
  while (o < buf.length) { const len = buf.readUInt32BE(o), kind = buf.toString('ascii', o + 4, o + 8), d = buf.subarray(o + 8, o + 8 + len);
    if (kind === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); type = d[9]; } if (kind === 'IDAT') idat.push(d); o += 12 + len; }
  const bpp = type === 6 ? 4 : 3, raw = inflateSync(Buffer.concat(idat)), stride = w * bpp, cur = Buffer.alloc(stride), prev = Buffer.alloc(stride); const seen = new Set(); let white = 0, count = 0;
  for (let y = 0; y < h; y++) { const f = raw[y * (stride + 1)], row = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
    for (let x = 0; x < stride; x++) { const a = x >= bpp ? cur[x - bpp] : 0, b = prev[x], c = x >= bpp ? prev[x - bpp] : 0; let v = row[x];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1; else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      cur[x] = v & 255; }
    if (y % 7 === 0) for (let x = 0; x < w; x += 5) { const i = x * bpp, k = (cur[i] >> 4) << 8 | (cur[i + 1] >> 4) << 4 | (cur[i + 2] >> 4); seen.add(k); count++; if (k === 0xfff) white++; }
    cur.copy(prev); }
  return { colors: seen.size, white: white / count };
}
const [,, url, out] = process.argv;
const routes = ['start', 'einstellungen', 'abi-pass', 'tagesaufgabe', 'jam', 'jam/MIX/EINSTIEG', 'jam/MIX/ABI', 'jam/MIX/PROFI', 'spickzettel', 'abzeichen', 'backstage', 'abi', 'abi/zelt/0', 'abi/bogen/0', 'abi/trommel/1', 'lage', 'duell', 'zweite-chance', 'werkstatt',
  'baukasten', 'schritte', 'schritte/weg-durchstoss', 'lernstand', 'klausur', 'klasse', 'zu-zweit', 'raumflug', 'galton', 'ballerbude', 'zufall', 'binomial', 'baumeister', 'geisterfahrt', 'ringflug', 'pflichtteil', 'fehler-tagebuch', 'kalender',
  'achterbahn/huegel', 'achterbahn/ln', 'achterbahn/ebuckel', 'level/1-1', 'level/5-3', 'level/9-5', 'level/12-3', 'level/13-2', 'level/14-4', 'level/16-3', 'level/17-5', 'level/20-4', 'level/22-3', 'level/23-5', 'level/24-2', 'level/25-7', 'gibt-es-nicht'];
// Nur bestimmte Bildschirme: RL_ROUTES="start,galton"
if (process.env.RL_ROUTES) { const only = process.env.RL_ROUTES.split(','); routes.splice(0, routes.length, ...only); }
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const problems = []; let n = 0;
// Eigene Bildschirmgrößen für die gründliche Prüfung: RL_VIEWS="klein:320x640,ipad:820x1180" – dann jeder Bildschirm in jeder Größe
const own = (process.env.RL_VIEWS || '').split(',').filter(Boolean).map((v) => { const [name, size] = v.split(':'); const [w, h] = size.split('x').map(Number); return [name, { width: w, height: h }]; });
for (const [name, vp] of own.length ? own : [['handy', { width: 400, height: 860 }], ['pc', { width: 1280, height: 800 }]]) {
  for (const r of routes) {
    if (!own.length && name === 'pc' && n % 3 !== 0) { n++; continue; } n++;   // am PC jede dritte: der Aufbau ist derselbe, nur breiter
    const page = await browser.newPage({ viewport: vp, deviceScaleFactor: 1 }); const errs = [];
    page.on('console', (m) => { if (m.type() === 'error') errs.push(m.text()); }); page.on('pageerror', (e) => errs.push(e.message));
    await page.goto(url + '?ziel=' + encodeURIComponent(r));
    await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }).catch(() => errs.push('Startbildschirm bleibt stehen'));
    await page.waitForTimeout(2600);
    const file = `${out}/bild-${name}-${r.replace(/\//g, '_')}.png`; const buf = await page.screenshot({ path: file });
    const pic = look(buf);
    if (pic.colors < 12) errs.push(`Bild fast einfarbig (${pic.colors} Farben)`); if (pic.white > 0.6) errs.push('Bild überwiegend weiß');
    const note = await page.evaluate(() => (localStorage.getItem('raumlicht.mitschrift') || '').split('\n').filter((l) => / (error|warn|fehler) /i.test(l) || /Exception|gescheitert/.test(l)).join(' | '));
    if (note) errs.push('Mitschrift: ' + note.slice(0, 300));
    console.log((errs.length ? 'FAIL ' : 'ok   ') + name + ' ' + r + (errs.length ? '  ' + errs.join(' ; ').slice(0, 400) : '')); if (errs.length) problems.push(name + ' ' + r);
    await page.close();
  }
}
await browser.close();
console.log(problems.length ? `${problems.length} Bildschirme mit Problemen` : 'Alle Bildschirme in Ordnung');
process.exit(problems.length ? 1 : 0);
