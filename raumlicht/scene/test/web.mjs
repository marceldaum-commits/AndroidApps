// Prüft die gebaute WebApp im Browser: node test/web.mjs <url> <outDir>
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
const [,, url, out] = process.argv;
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const problems = [];
for (const [name, vp] of [['handy', { width: 400, height: 860 }], ['pc', { width: 1280, height: 800 }]]) {
  const page = await browser.newPage({ viewport: vp, deviceScaleFactor: name === 'handy' ? 2 : 1 });
  page.on('console', (m) => { if (m.type() === 'error') problems.push(`${name}: ${m.text()}`); }); page.on('pageerror', (e) => problems.push(`${name}: ${e.message}`));
  page.on('requestfailed', (r) => problems.push(`${name}: nicht geladen ${r.url()}`));
  await page.goto(url); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 60000 }).catch(() => problems.push(`${name}: Startbildschirm bleibt stehen`));
  await page.waitForTimeout(1500); await page.screenshot({ path: `${out}/web-${name}-start.png` });
  // erste Aufgabe öffnen: der erste Kapitel-Knopf liegt unter der Kapitelüberschrift
  await page.mouse.click(42, name === 'handy' ? 316 : 250); await page.waitForTimeout(2500);
  const stage = await page.evaluate(() => { const f = document.getElementById('stage'); const r = f.getBoundingClientRect(); return { show: f.style.display, x: r.x, y: r.y, w: r.width, h: r.height, rl: !!(f.contentWindow && f.contentWindow.RL) }; });
  console.log(name, JSON.stringify(stage));
  await page.screenshot({ path: `${out}/web-${name}-aufgabe.png` });
  if (!(stage.show === 'block' && stage.w > 200 && stage.h > 200 && stage.rl)) problems.push(`${name}: 3D-Bühne nicht sichtbar ${JSON.stringify(stage)}`);
  if (name === 'pc') {   // Aufgabe 1-1 lösen: P = (4|2|0), dann „Los“ – der Stern muss im Browser-Speicher landen und einen Neustart überleben
    for (let k = 0; k < 4; k++) { await page.mouse.click(438, 710); await page.waitForTimeout(600); } for (let k = 0; k < 2; k++) { await page.mouse.click(841, 710); await page.waitForTimeout(600); }
    await page.mouse.click(681, 764); await page.waitForTimeout(1500);
    const saved = await page.evaluate(() => localStorage.getItem('raumlicht.sterne'));
    if (saved !== '1-1=3') problems.push(`pc: Fortschritt nicht gespeichert (${saved})`); else console.log('Aufgabe 1-1 gelöst, gespeichert:', saved);
    await page.screenshot({ path: `${out}/web-pc-geloest.png` });
    await page.reload(); await page.waitForTimeout(3000);
    const again = await page.evaluate(() => localStorage.getItem('raumlicht.sterne')); if (again !== '1-1=3') problems.push('pc: Fortschritt nach Neuladen weg');
    const v = await (await page.request.get(new URL('version.json', url).href)).json(); console.log('Version der WebApp:', JSON.stringify(v));
    const man = await (await page.request.get(new URL('manifest.webmanifest', url).href)).json(); if (man.display !== 'standalone' || man.icons.length < 2) problems.push('pc: Manifest unvollständig');
  }
  await page.close();
}
await browser.close();
console.log(problems.length ? problems.join('\n') : 'keine Konsolenfehler'); process.exit(problems.length ? 1 : 0);
