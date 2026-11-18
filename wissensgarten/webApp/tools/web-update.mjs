import { chromium } from 'playwright';
import { execSync } from 'child_process';
const T = process.argv[2];
const b = await chromium.launch(); const ctx = await b.newContext(); const p = await ctx.newPage();
const errs = []; p.on('pageerror', e => errs.push(String(e)));
const ver = () => p.evaluate(() => fetch('version.json', { cache: 'no-store' }).then(r => r.json()).then(v => v.version)).catch(() => '?');
await p.goto('http://127.0.0.1:8811/'); await p.waitForTimeout(25000);
await p.evaluate(() => localStorage.setItem('wg.testmarke', 'bleibt'));
await p.reload(); await p.waitForTimeout(20000);   // jetzt mit Service Worker
const swOld = await p.evaluate(() => !!navigator.serviceWorker.controller);
console.log('vorher:', await ver(), 'SW aktiv:', swOld);
// neue Version auf den „Server“ legen, Tab offen lassen
execSync(`rm -rf ${T}/live/* && cp -r ${T}/B/. ${T}/live/`);
// Tab verlassen und zurückkehren (visibilitychange)
const p2 = await ctx.newPage(); await p2.goto('about:blank'); await p.waitForTimeout(3000); await p.bringToFront();
await p.evaluate(() => { document.dispatchEvent(new Event('visibilitychange')); window.dispatchEvent(new Event('focus')); });
let toast = null;
for (let i = 0; i < 40; i++) {
  await p.waitForTimeout(1500);
  toast = await p.evaluate(() => { const t = document.getElementById('wg-toast'); return t ? t.textContent : null; }).catch(() => null);
  if (toast && toast.includes('Neue Version')) break;
}
console.log('nachher:', await ver(), '| Hinweis:', toast, '| Marke:', await p.evaluate(() => localStorage.getItem('wg.testmarke')));
console.log('Caches:', await p.evaluate(() => caches.keys()));
console.log('Fehler:', errs.slice(0, 3));
await b.close();
