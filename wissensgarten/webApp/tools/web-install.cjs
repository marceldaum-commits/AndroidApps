// Prüft die WebApp als installierbare App (PWA) im Browser (Playwright, Chromium):
//  1. Manifest: name, short_name, start_url/scope relativ, display standalone, Farben, Symbole 192/512 + maskable.
//  2. Offline: einmal laden (Service Worker speichert alles), dann offline neu laden → der Garten startet.
//  3. „App installieren“: ohne Angebot des Browsers kein Knopf; „beforeinstallprompt“ (Attrappe) → Knopf erscheint im Garten,
//     Antippen ruft prompt() auf; nach dem Installieren ist der Knopf weg.
//  4. Läuft schon installiert (display-mode: standalone) → kein Knopf.
//  5. iPhone (User-Agent) → Knopf „So geht's“ mit Anleitung; als Home-Bildschirm-App (navigator.standalone) → kein Knopf.
// Der Knopf wird in Compose (Canvas) gezeichnet – darum sucht der Test ihn über die Farbe im Bildschirmfoto.
// Aufruf: node web-install.cjs [<webapp-ordner>] [<ordner für Bildschirmfotos>]   (Standard: ../../webapp, Server auf :8812)
const { chromium } = require('playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(process.argv[2] || path.join(__dirname, '../../webapp'));
const SHOTS = process.argv[3] ? path.resolve(process.argv[3]) : null;
const PORT = 8812;
const BASE = `http://127.0.0.1:${PORT}/`;
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.wasm': 'application/wasm', '.json': 'application/json',
  '.webmanifest': 'application/manifest+json', '.png': 'image/png', '.svg': 'image/svg+xml', '.ttf': 'font/ttf', '.txt': 'text/plain', '.mp3': 'audio/mpeg' };
const IPHONE = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1';

const results = [];
const check = (name, ok, info) => { results.push({ name, ok: !!ok, info }); console.log(`${ok ? '✔' : '✘'} ${name}${info !== undefined ? ' – ' + JSON.stringify(info) : ''}`); };

// Kleiner Webserver für den WebApp-Ordner
const server = http.createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, BASE).pathname);
  if (p.endsWith('/')) p += 'index.html';
  const f = path.join(ROOT, p);
  if (!f.startsWith(ROOT) || !fs.existsSync(f) || !fs.statSync(f).isFile()) { res.writeHead(404); res.end(); return; }
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(f)] || 'application/octet-stream', 'Cache-Control': 'no-cache' });
  fs.createReadStream(f).pipe(res);
});

// Garten mit einem Profil, Einrichtung fertig – so zeigt die App gleich den Garten
const SEED = () => {
  if (localStorage.getItem('wg.garten')) return;
  localStorage.setItem('wg.garten', JSON.stringify({ profiles: [{ id: 'p1', name: 'Lena', avatar: '🦊', level: 3, createdAt: Date.now() }] }));
  localStorage.setItem('wg.prefs.einstellungen', JSON.stringify({ aktives_profil: { t: 's', v: 'p1' }, einrichtung_fertig: { t: 'b', v: true },
    erinnerung_gefragt: { t: 'b', v: true } }));
};

async function ready(page, ms = 90000) {
  await page.waitForFunction(() => !document.getElementById('splash'), null, { timeout: ms });
  await page.waitForTimeout(2500);
}

/** Bildschirmfoto im Browser auswerten: Mittelpunkt der größten Fläche in Knopf-Farbe (Moos-Grün #3F6B3A) unterhalb des Garten-Kopfs. */
async function findButton(page, helper, name) {
  const png = await page.screenshot();
  if (SHOTS) fs.writeFileSync(path.join(SHOTS, `install_${name}.png`), png);
  return helper.evaluate(async (b64) => {
    const img = new Image(); img.src = 'data:image/png;base64,' + b64; await img.decode();
    const c = document.createElement('canvas'); c.width = img.width; c.height = img.height;
    const g = c.getContext('2d'); g.drawImage(img, 0, 0);
    const d = g.getImageData(0, 0, c.width, c.height).data;
    const yFrom = Math.round(c.height * 0.25), yTo = Math.round(c.height * 0.6);
    let n = 0, sx = 0, sy = 0, minX = 1e9, maxX = 0, minY = 1e9, maxY = 0;
    for (let y = yFrom; y < yTo; y++) for (let x = Math.round(c.width * 0.5); x < c.width; x++) {
      const i = (y * c.width + x) * 4;
      if (Math.abs(d[i] - 0x3f) < 14 && Math.abs(d[i + 1] - 0x6b) < 14 && Math.abs(d[i + 2] - 0x3a) < 14) {
        n++; sx += x; sy += y; minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
      }
    }
    return n > 400 ? { x: Math.round(sx / n), y: Math.round(sy / n), w: maxX - minX, h: maxY - minY, n } : null;
  }, png.toString('base64'));
}

(async () => {
  await new Promise((r) => server.listen(PORT, '127.0.0.1', r));
  const browser = await chromium.launch();
  const helper = await browser.newPage();
  let exit = 0;
  try {
    // ---- 1. Manifest
    const m = JSON.parse(fs.readFileSync(path.join(ROOT, 'manifest.webmanifest'), 'utf8'));
    check('Manifest: name/short_name', m.name && m.short_name, { name: m.name, short_name: m.short_name });
    check('Manifest: start_url und scope relativ', m.start_url === './' && m.scope === './', { start_url: m.start_url, scope: m.scope });
    check('Manifest: display standalone', m.display === 'standalone');
    check('Manifest: Farben', /^#[0-9a-f]{6}$/i.test(m.background_color) && /^#[0-9a-f]{6}$/i.test(m.theme_color), { bg: m.background_color, theme: m.theme_color });
    const pngSize = (f) => { const b = fs.readFileSync(path.join(ROOT, f)); return `${b.readUInt32BE(16)}x${b.readUInt32BE(20)}`; };
    const icon = (size, purpose) => (m.icons || []).find((i) => i.sizes === size && (i.purpose || 'any').split(' ').includes(purpose));
    for (const [size, purpose] of [['192x192', 'any'], ['512x512', 'any'], ['512x512', 'maskable']]) {
      const i = icon(size, purpose);
      check(`Manifest: Symbol ${size} ${purpose}`, i && fs.existsSync(path.join(ROOT, i.src)) && pngSize(i.src) === size, i && i.src);
    }
    const html = fs.readFileSync(path.join(ROOT, 'index.html'), 'utf8');
    check('index.html verlinkt Manifest und Apple-Symbol', html.includes('rel="manifest"') && html.includes('apple-touch-icon'));

    // ---- 2. Offline
    {
      const ctx = await browser.newContext({ viewport: { width: 412, height: 900 } });
      const p = await ctx.newPage();
      const errs = []; p.on('pageerror', (e) => errs.push(String(e)));
      await p.goto(BASE); await ready(p);
      // Warten, bis der Service Worker alles vorab gespeichert hat
      const pre = await p.evaluate(async () => {
        const reg = await navigator.serviceWorker.ready;
        for (let i = 0; i < 120 && !navigator.serviceWorker.controller; i++) await new Promise((r) => setTimeout(r, 500));
        const txt = await (await fetch('sw.js')).text();
        const list = eval(txt.match(/const files = (\[[^\]]*\])/)[1]);
        let missing = list.length;
        for (let i = 0; i < 120 && missing > 0; i++) {
          const c = await caches.open((await caches.keys()).find((k) => /^wissensgarten-\d/.test(k)) || 'x');
          const got = await Promise.all(list.map((f) => c.match(new URL(f, location.href).href)));
          missing = got.filter((x) => !x).length;
          if (missing) await new Promise((r) => setTimeout(r, 500));
        }
        return { aktiv: !!reg.active, kontrolliert: !!navigator.serviceWorker.controller, dateien: list.length, fehlen: missing };
      });
      check('Service Worker hat alles gespeichert', pre.aktiv && pre.kontrolliert && pre.fehlen === 0, pre);
      await ctx.setOffline(true);
      await p.reload();
      let ok = true; try { await ready(p, 60000); } catch (e) { ok = false; }
      const online = await p.evaluate(() => navigator.onLine);
      if (SHOTS) await p.screenshot({ path: path.join(SHOTS, 'install_offline.png') });
      check('Offline neu geladen: App startet', ok && !online && errs.length === 0, { offline: !online, fehler: errs.slice(0, 2) });
      await ctx.close();
    }

    // ---- 3. Chrome/Edge/Android: beforeinstallprompt
    {
      const ctx = await browser.newContext({ viewport: { width: 412, height: 900 } });
      await ctx.addInitScript(SEED);
      const p = await ctx.newPage();
      await p.goto(BASE); await ready(p);
      check('Ohne Angebot des Browsers: Zustand „none“', (await p.evaluate(() => wg.install.state())) === 'none');
      check('Ohne Angebot des Browsers: kein Knopf', !(await findButton(p, helper, 'ohne_angebot')));
      await p.evaluate(() => {
        window.__prompted = 0;
        const e = new Event('beforeinstallprompt', { cancelable: true });
        e.prompt = () => { window.__prompted++; return Promise.resolve(); };
        e.userChoice = new Promise((r) => { window.__choose = r; });
        window.dispatchEvent(e);
        window.__defaultPrevented = e.defaultPrevented;
      });
      await p.waitForTimeout(1500);
      check('beforeinstallprompt abgefangen (preventDefault, gemerkt)', await p.evaluate(() => window.__defaultPrevented && wg.install.state() === 'prompt'));
      const btn = await findButton(p, helper, 'garten_knopf');
      check('Knopf „Installieren“ erscheint im Garten', btn, btn);
      if (btn) {
        await p.mouse.click(btn.x, btn.y);
        await p.waitForTimeout(500);
        check('Antippen ruft prompt() auf', (await p.evaluate(() => window.__prompted)) === 1);
        await p.evaluate(() => { window.__choose({ outcome: 'accepted', platform: 'web' }); window.dispatchEvent(new Event('appinstalled')); });
        await p.waitForTimeout(1500);
        check('Nach dem Installieren: Knopf weg', (await p.evaluate(() => wg.install.state())) === 'none' && !(await findButton(p, helper, 'nach_installieren')));
      }
      await ctx.close();
    }

    // ---- 4. Schon installiert: display-mode standalone
    {
      const ctx = await browser.newContext({ viewport: { width: 412, height: 900 } });
      await ctx.addInitScript(SEED);
      await ctx.addInitScript(() => {
        const orig = window.matchMedia.bind(window);
        window.matchMedia = (q) => (/display-mode:\s*standalone/.test(q)
          ? { matches: true, media: q, onchange: null, addEventListener() {}, removeEventListener() {}, addListener() {}, removeListener() {}, dispatchEvent() { return false; } }
          : orig(q));
      });
      const p = await ctx.newPage();
      await p.goto(BASE); await ready(p);
      await p.evaluate(() => { const e = new Event('beforeinstallprompt', { cancelable: true }); e.prompt = () => Promise.resolve(); e.userChoice = Promise.resolve({ outcome: 'dismissed' }); window.dispatchEvent(e); });
      await p.waitForTimeout(1500);
      check('display-mode standalone: kein Knopf', (await p.evaluate(() => wg.install.state())) === 'none' && !(await findButton(p, helper, 'standalone')));
      await ctx.close();
    }

    // ---- 5. iPhone (Safari): Anleitung statt Browser-Dialog
    for (const homescreen of [false, true]) {
      const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, userAgent: IPHONE, isMobile: true, hasTouch: true });
      await ctx.addInitScript(SEED);
      if (homescreen) await ctx.addInitScript(() => Object.defineProperty(Navigator.prototype, 'standalone', { get: () => true }));
      const p = await ctx.newPage();
      await p.goto(BASE); await ready(p);
      const state = await p.evaluate(() => wg.install.state());
      if (homescreen) {
        check('iPhone vom Home-Bildschirm (navigator.standalone): kein Knopf', state === 'none' && !(await findButton(p, helper, 'iphone_homescreen')), state);
      } else {
        check('iPhone: Zustand „ios“', state === 'ios', state);
        const btn = await findButton(p, helper, 'iphone_knopf');
        check('iPhone: Knopf „So geht\'s“ erscheint', btn, btn);
        if (btn) {
          const before = await p.screenshot();
          await p.touchscreen.tap(btn.x, btn.y);
          await p.waitForTimeout(1200);
          const after = await p.screenshot();
          if (SHOTS) fs.writeFileSync(path.join(SHOTS, 'install_iphone_anleitung.png'), after);
          // Der Dialog (mit abgedunkeltem Hintergrund) verändert einen großen Teil des Bildes
          const changed = await helper.evaluate(async ([a, b]) => {
            const load = async (s) => { const i = new Image(); i.src = 'data:image/png;base64,' + s; await i.decode(); const c = document.createElement('canvas');
              c.width = i.width; c.height = i.height; const g = c.getContext('2d'); g.drawImage(i, 0, 0); return g.getImageData(0, 0, c.width, c.height).data; };
            const x = await load(a), y = await load(b); let diff = 0;
            for (let k = 0; k < x.length; k += 4) if (Math.abs(x[k] - y[k]) + Math.abs(x[k + 1] - y[k + 1]) + Math.abs(x[k + 2] - y[k + 2]) > 40) diff++;
            return diff / (x.length / 4);
          }, [before.toString('base64'), after.toString('base64')]);
          check('iPhone: Antippen öffnet die Anleitung', changed > 0.3, { geaendert: Math.round(changed * 100) + ' %' });
        }
      }
      await ctx.close();
    }
  } catch (e) {
    console.error(e); exit = 1;
  }
  await browser.close();
  server.close();
  const bad = results.filter((r) => !r.ok);
  console.log(bad.length ? `\n${bad.length} von ${results.length} Prüfungen fehlgeschlagen` : `\nAlle ${results.length} Prüfungen bestanden`);
  process.exit(bad.length || exit ? 1 : 0);
})();
