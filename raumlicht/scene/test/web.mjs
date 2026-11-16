// Prüft die gebaute WebApp im Browser: node test/web.mjs <url> <outDir>
// Die Oberfläche ist eine Zeichenfläche (kein HTML) – geklickt wird deshalb nach Position; die Prüfungen lesen Speicher, Bühne und Verlauf aus.
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
const [,, url, out] = process.argv;
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const problems = []; const ok = (name, good, info = '') => { console.log((good ? 'ok   ' : 'FAIL ') + name + (info ? '  ' + info : '')); if (!good) problems.push(name + ' ' + info); };
// Klickstellen je Bildschirmgröße
const at = { handy: { level1: [200, 208] /* Knopf „Tour starten“ – bleibt oben, auch wenn Kapitel dazukommen */, weiter: [200, 800] }, pc: { level1: [123, 625], weiter: [640, 745], plus1: [902, 245], plus2: [1074, 245], los: [1090, 306], mehr: [1112, 290], zurueck: [24, 24] } };
for (const [name, vp] of [['handy', { width: 400, height: 860 }], ['pc', { width: 1280, height: 800 }]]) {
  const page = await browser.newPage({ viewport: vp, deviceScaleFactor: name === 'handy' ? 2 : 1 }); const p = at[name];
  page.on('console', (m) => { if (m.type() === 'error') problems.push(`${name}: ${m.text()}`); }); page.on('pageerror', (e) => problems.push(`${name}: ${e.message}`));
  page.on('requestfailed', (r) => problems.push(`${name}: nicht geladen ${r.url()}`));
  const click = async ([x, y], ms = 900) => { await page.mouse.click(x, y); await page.waitForTimeout(ms); };
  const home = () => page.evaluate(() => window.__rlHome);
  const stage = () => page.evaluate(() => { const f = document.getElementById('stage'); const r = f.getBoundingClientRect(); return { show: f.style.display, w: r.width, h: r.height, rl: !!(f.contentWindow && f.contentWindow.RL) }; });
  await page.goto(url); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }).catch(() => problems.push(`${name}: Startbildschirm bleibt stehen`));
  await page.waitForTimeout(2500); await page.screenshot({ path: `${out}/web-${name}-start.png` });
  ok(`${name}: Startseite mit Bühne`, await home() === true && (await stage()).show === 'block');
  // Aufgabe 1-1 öffnen, Kapitel-Einführung durchklicken
  await click(p.level1, 2500); await page.screenshot({ path: `${out}/web-${name}-einfuehrung.png` });
  for (let k = 0; k < 3; k++) await click(p.weiter, 700);
  await page.waitForTimeout(1500); const s = await stage(); await page.screenshot({ path: `${out}/web-${name}-aufgabe.png` });
  ok(`${name}: Aufgabe offen, 3D-Bühne sichtbar`, await home() === false && s.show === 'block' && s.w > 200 && s.h > 200 && s.rl, JSON.stringify(s));
  if (name === 'pc') {
    // Bühne anfassen: ziehen dreht die Kamera, das Mausrad zoomt (Berührungen reicht die Oberfläche an die Bühne darunter weiter)
    const px = async () => (await page.screenshot({ clip: { x: 60, y: 100, width: 600, height: 500 } })).toString('base64');
    const before = await px(); await page.mouse.move(350, 300); await page.mouse.down(); await page.mouse.move(520, 340, { steps: 8 }); await page.mouse.up(); await page.waitForTimeout(1200);
    const turned = await px(); ok('pc: Ziehen dreht die Bühne', before !== turned);
    await page.mouse.wheel(0, 400); await page.waitForTimeout(1200); ok('pc: Mausrad zoomt', turned !== await px());
    // Aufgabe 1-1 lösen: P = (4|2|0), dann „Los“ – die Sterne müssen im Browser-Speicher landen und einen Neustart überleben
    for (let k = 0; k < 4; k++) await click(p.plus1, 350); for (let k = 0; k < 2; k++) await click(p.plus2, 350);
    await click(p.los, 6000); await page.screenshot({ path: `${out}/web-pc-geloest.png` });
    const saved = await page.evaluate(() => localStorage.getItem('raumlicht.sterne')); ok('pc: Aufgabe 1-1 gelöst und gespeichert', saved === '1-1=3', String(saved));
    // Zurück-Taste des Browsers schließt die Aufgabe (und verlässt nicht die Seite)
    await page.goBack(); await page.waitForTimeout(1500); ok('pc: Browser-Zurück führt zur Startseite', await home() === true && page.url().startsWith(url));
    // „Mehr“ → Liste aller Bereiche; der Pfeil oben links führt zurück
    await click(p.mehr, 1500); await page.screenshot({ path: `${out}/web-pc-mehr.png` }); ok('pc: „Mehr“ offen', await home() === false);
    await click(p.zurueck, 1500); ok('pc: Pfeil führt zurück', await home() === true);
    await page.reload(); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }); await page.waitForTimeout(2500);
    ok('pc: Fortschritt übersteht Neuladen', await page.evaluate(() => localStorage.getItem('raumlicht.sterne')) === '1-1=3'); await page.screenshot({ path: `${out}/web-pc-nachher.png` });
    const v = await (await page.request.get(new URL('version.json', url).href)).json(); console.log('Version der WebApp:', JSON.stringify(v));
    const man = await (await page.request.get(new URL('manifest.webmanifest', url).href)).json(); ok('pc: Manifest vollständig', man.display === 'standalone' && man.icons.length >= 2);
  }
  await page.close();
}
await browser.close();
console.log(problems.length ? '\n' + problems.join('\n') : 'WebApp in Ordnung, keine Konsolenfehler'); process.exit(problems.length ? 1 : 0);
