// Stellt nach: Seite lädt mit Höhe 0 (WebView noch nicht eingepasst), bekommt erst danach ihre Größe.
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import path from 'path'; import { pathToFileURL } from 'url'; import fs from 'fs';
const flag = process.argv[2] ?? '';
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage({ viewport: { width: 384, height: 480 }, deviceScaleFactor: 1 });
const logs = []; page.on('console', m => logs.push(m.text())); page.on('pageerror', e => logs.push('ERR ' + e.message));
await page.addInitScript(() => { window.__zero = true; const d = Object.getOwnPropertyDescriptor(window, 'innerHeight');
  Object.defineProperty(window, 'innerHeight', { get() { return window.__zero ? 0 : d.get.call(window); } });
  window.onSceneEvent = (e) => { if (e.type !== 'ready') console.log('EVENT ' + JSON.stringify(e)); }; });
await page.goto(pathToFileURL(path.resolve('../app/src/main/assets/scene/index.html')).href + flag);
await page.waitForFunction(() => window.RL);
const d = await page.evaluate(() => { window.__zero = false; window.dispatchEvent(new Event('resize')); RL.pause();
  RL.setState({ bounds: { min: [0, 0, 0], max: [8, 8, 6] }, objects: [{ id: 'lux', type: 'drone', p: [4, 4, 2] }, { id: 't', type: 'point', style: 'target', p: [2, 5, 3], color: 'green' }] });
  RL.advance(1.5); return RL.snapshot(); });
const out = '/tmp/claude-0/-home-user-AndroidApps/0897751d-eebe-5f60-9390-b94118e175e2/scratchpad/zero' + (flag ? '-nohdr' : '') + '.png';
fs.writeFileSync(out, Buffer.from(d.split(',')[1], 'base64')); console.log(out, 'bytes', fs.statSync(out).size, logs.slice(0, 5).join(' | '));
await browser.close();
