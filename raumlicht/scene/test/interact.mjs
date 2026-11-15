// Bedien-Test der echten Bühne: echte Zeiger-Ereignisse im Browser, geprüft wird, was die Bühne an die App meldet.
// node test/interact.mjs <scenes.json>
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'fs'; import path from 'path'; import { pathToFileURL } from 'url';
const states = Object.fromEntries(JSON.parse(fs.readFileSync(process.argv[2], 'utf8')).map((s) => [s.name, s.state]));
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage({ viewport: { width: 390, height: 520 }, hasTouch: false });
page.on('pageerror', (e) => { console.log('ERR', e.message); process.exitCode = 1; });
await page.addInitScript(() => { window.__ev = []; window.onSceneEvent = (e) => window.__ev.push(e); });
const url = pathToFileURL(path.resolve('../app/src/main/assets/scene/index.html')).href;
let failed = 0;
const check = (name, ok, info = '') => { console.log((ok ? 'ok   ' : 'FAIL ') + name + (info ? '  ' + info : '')); if (!ok) failed++; };
async function open(name) { await page.goto(url); await page.waitForFunction(() => window.RL); await page.evaluate((s) => { RL.pause(); RL.setState(s); RL.advance(3); window.__ev.length = 0; }, states[name]); }
const events = () => page.evaluate(() => window.__ev.splice(0));
const at = (p) => page.evaluate((p) => RL.project(p), p);
const step = () => page.evaluate(() => RL.advance(0.2));
async function tap(x, y) { await page.mouse.move(x, y); await page.mouse.down(); await page.mouse.up(); }
async function drag(x0, y0, x1, y1) { await page.mouse.move(x0, y0); await page.mouse.down(); for (let k = 1; k <= 8; k++) await page.mouse.move(x0 + (x1 - x0) * k / 8, y0 + (y1 - y0) * k / 8); await page.mouse.up(); }
const obj = (name, id) => states[name].objects.find((o) => o.id === id);

// 1 · Ballerbude: Ballon antippen, daneben tippen, Kamera bleibt fest
await open('ballerbude');
const balloons = states.ballerbude.objects.filter((o) => o.type === 'sphere');
for (const b of balloons.slice(0, 3)) { const [x, y] = await at(b.p); await tap(x, y); const ev = await events(); check(`Ballon ${b.id} antippen`, ev.some((e) => e.type === 'tap' && e.id === b.id), JSON.stringify(ev)); }
{ const b = balloons[0], [x, y] = await at(b.p); await tap(x + 4, y - 5); const ev = await events(); check('Ballon knapp neben der Mitte', ev.some((e) => e.type === 'tap' && e.id === b.id)); }
{ await tap(30, 40); const ev = await events(); check('daneben tippen meldet die Stelle', ev.some((e) => e.type === 'miss' && Math.abs(e.p[0] - 4) < 0.01), JSON.stringify(ev)); }
{ const before = await at([8, 0, 0]); await drag(60, 60, 220, 200); await step(); const after = await at([8, 0, 0]); const ev = await events();
  check('Wischen dreht die Bühne im Spiel nicht', Math.hypot(before[0] - after[0], before[1] - after[1]) < 1, `${before} → ${after}`); check('Wischen ist kein Treffer', !ev.some((e) => e.type === 'tap')); }
// zittriger Finger: ein paar Pixel Bewegung beim Tippen zählt trotzdem
{ const b = balloons[1], [x, y] = await at(b.p); await page.mouse.move(x, y); await page.mouse.down(); await page.mouse.move(x + 9, y + 7); await page.mouse.up(); const ev = await events(); check('Tippen mit zittrigem Finger', ev.some((e) => e.type === 'tap' && e.id === b.id), JSON.stringify(ev)); }

// zu zweit: beide tippen gleichzeitig auf verschiedene Ballons – beide Treffer zählen
{ const [a, b] = [balloons[0], balloons[2]]; const pa = await at(a.p), pb = await at(b.p);
  await page.evaluate(([pa, pb]) => { const c = document.getElementById('c'); const ev = (t, id, p) => c.dispatchEvent(new PointerEvent(t, { pointerId: id, clientX: p[0], clientY: p[1], bubbles: true, pointerType: 'touch', isPrimary: id === 11 }));
    ev('pointerdown', 11, pa); ev('pointerdown', 12, pb); ev('pointerup', 12, pb); ev('pointerup', 11, pa); }, [pa, pb]);
  const ev = await events(); check('zwei Finger gleichzeitig: beide Treffer', ev.filter((e) => e.type === 'tap').map((e) => e.id).sort().join() === [a.id, b.id].sort().join(), JSON.stringify(ev)); }

// 2 · Baumeister: Stützpunkt an der Kugel greifen und hochziehen
await open('baumeister');
for (const id of ['h1', 'h3']) { const h = obj('baumeister', id), [x, y] = await at(h.p), [, y2] = await at([h.p[0], h.p[1], h.p[2] + 2]);
  await drag(x, y, x, y2); const ev = await events(); const d = ev.filter((e) => e.type === 'drag' && e.id === id);
  check(`Stützpunkt ${id} zwei hochziehen`, d.length > 0 && d[d.length - 1].p[2] === h.p[2] + 2 && d[d.length - 1].p[1] === h.p[1], JSON.stringify(d.slice(-1))); }
{ const h = obj('baumeister', 'h2'), [x, y] = await at(h.p), [, y2] = await at([h.p[0], h.p[1], h.p[2] - 3]); await drag(x, y, x + 40, y2); const ev = await events(); const d = ev.filter((e) => e.type === 'drag' && e.id === 'h2');
  check('Stützpunkt schräg ziehen bleibt auf seiner Stelle', d.length > 0 && d.every((e) => e.p[1] === h.p[1] && e.p[0] === h.p[0]) && d[d.length - 1].p[2] === h.p[2] - 3, JSON.stringify(d.slice(-1))); }
{ const before = await at([8, 0, 0]); await drag(200, 430, 320, 470); await step(); const after = await at([8, 0, 0]); check('Wischen neben den Punkten dreht nichts', Math.hypot(before[0] - after[0], before[1] - after[1]) < 1); }

// 3 · Aufgabe mit Griff: in der Bodenebene ziehen; daneben wischen dreht die Bühne
await open('1-2');
{ const h = states['1-2'].objects.find((o) => o.type === 'handle'); const [x, y] = await at(h.p), [x2, y2] = await at([h.p[0] + 2, h.p[1] + 1, h.p[2]]);
  await drag(x, y, x2, y2); const ev = await events(); const d = ev.filter((e) => e.type === 'drag');
  check('Griff in der Aufgabe ziehen', d.length > 0 && d[d.length - 1].p[0] === h.p[0] + 2 && d[d.length - 1].p[1] === h.p[1] + 1, JSON.stringify(d.slice(-1)));
  // Prüfpunkt am Rand des Raums – die Mitte ist das Drehzentrum und bewegt sich beim Drehen nicht
  const before = await at([8, 0, 0]); await drag(40, 60, 200, 90); await step(); await step(); const after = await at([8, 0, 0]); check('Wischen dreht die Bühne in Aufgaben', Math.hypot(before[0] - after[0], before[1] - after[1]) > 5);
  // zwei Finger zum Zoomen sind in Aufgaben kein Tipp
  await page.evaluate(() => { const c = document.getElementById('c'); const ev = (t, id, x, y) => c.dispatchEvent(new PointerEvent(t, { pointerId: id, clientX: x, clientY: y, bubbles: true, pointerType: 'touch' }));
    ev('pointerdown', 21, 100, 200); ev('pointerdown', 22, 250, 300); ev('pointerup', 22, 250, 300); ev('pointerup', 21, 100, 200); });
  const ev2 = await events(); check('Zwei-Finger-Griff in Aufgaben ist kein Tipp', !ev2.some((e) => e.type === 'tap' || e.type === 'miss'), JSON.stringify(ev2)); }

await browser.close();
console.log(failed ? `${failed} Bedienfehler` : 'alle Bedienproben bestanden'); process.exit(failed ? 1 : 0);
