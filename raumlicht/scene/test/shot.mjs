// Rendert Szenen-Zustände zu PNGs: node test/shot.mjs <states.json> <outDir> [width height]
import { createRequire } from 'module'; const { chromium } = createRequire(import.meta.url)('playwright');
import fs from 'fs'; import path from 'path'; import { pathToFileURL } from 'url';
const [,, statesFile, outDir, W = '400', H = '470'] = process.argv;
const states = JSON.parse(fs.readFileSync(statesFile, 'utf8'));
fs.mkdirSync(outDir, { recursive: true });
const browser = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage({ viewport: { width: +W, height: +H }, deviceScaleFactor: 2 });
page.on('console', m => { if (m.type() === 'error') console.log('JS:', m.text()); }); page.on('pageerror', e => console.log('ERR', e.message));
const url = pathToFileURL(path.resolve('../app/src/main/assets/scene/index.html')).href;
for (const s of states) {
  await page.goto(url); await page.waitForFunction(() => window.RL);
  await page.evaluate((s) => { RL.pause(); if (s.camera) RL.setCamera(s.camera); RL.setState(s.state); RL.advance(s.settle ?? 2.5);
    for (const a of s.actions ?? []) { if (a.fire) RL.fire(a.fire); if (a.party) RL.party(a.party); if (a.state) RL.setState(a.state); RL.advance(a.wait ?? 1); } return RL.snapshot(); }, s).then((d) => fs.writeFileSync(path.join(outDir, s.name + '.png'), Buffer.from(d.split(',')[1], 'base64')));
  console.log('ok', s.name);
}
await browser.close();
