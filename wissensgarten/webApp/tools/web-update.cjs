// Prüft das automatische Update der WebApp im Browser (Playwright):
// alte Version laden (mit Service Worker), neue Version auf den „Server“ legen, Tab offen lassen,
// Tab verlassen und zurückkehren → neue Version muss ohne manuelles Neuladen laufen,
// Hinweis „Neue Version x.y.z geladen“ erscheint, Spielstand (localStorage) bleibt, Nova-Caches bleiben.
// Aufruf: node web-update.cjs <ordner>   (<ordner>/A = alte, <ordner>/B = neue WebApp, <ordner>/live = wird ausgeliefert, Server auf :8811)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const T = path.resolve(process.argv[2]);
  const live = path.join(T, 'live');
  const setLive = (src) => {
    for (const f of fs.readdirSync(live)) fs.rmSync(path.join(live, f), { recursive: true, force: true });
    fs.cpSync(path.join(T, src), live, { recursive: true });
  };
  setLive('A');
  const b = await chromium.launch();
  const ctx = await b.newContext();
  const p = await ctx.newPage();
  const errs = [];
  p.on('pageerror', (e) => errs.push(String(e)));
  const ver = () => p.evaluate(() => fetch('version.json?nc=' + Date.now(), { cache: 'no-store' }).then((r) => r.json()).then((v) => v.version)).catch(() => '?');

  await p.goto('http://127.0.0.1:8811/');
  await p.waitForTimeout(25000);
  await p.evaluate(() => { localStorage.setItem('wg.testmarke', 'bleibt'); return caches.open('wissensgarten-nova').then((c) => c.put('/nova-test', new Response('x'))); });
  await p.reload();
  await p.waitForTimeout(20000); // jetzt mit Service Worker
  await p.evaluate(() => { window.__alteSeite = true; });
  console.log('vorher:', await ver(), '| SW aktiv:', await p.evaluate(() => !!navigator.serviceWorker.controller));

  setLive('B'); // neue Version auf den Server, Tab bleibt offen
  const p2 = await ctx.newPage();
  await p2.goto('about:blank');
  await p.waitForTimeout(3000);
  await p.bringToFront();
  await p.evaluate(() => { document.dispatchEvent(new Event('visibilitychange')); window.dispatchEvent(new Event('focus')); });

  // Mitten in der Einrichtung (kein ungefährlicher Moment): Update ist erkannt, aber es wird noch NICHT neu geladen
  await p.waitForTimeout(8000);
  const wartet = await p.evaluate(() => !!(window.wgUpdate && window.wgUpdate.pending) && !!window.__alteSeite);
  console.log('Update erkannt, wartet auf ungefährlichen Moment:', wartet);
  // Ungefährlicher Moment, wie ihn die App auf dem Startbildschirm meldet (bzw. Tab kurz verlassen)
  const modus = process.argv[3] || 'start';
  if (modus === 'tab') { await p2.bringToFront(); await p.waitForTimeout(4000); await p.bringToFront(); }
  else await p.evaluate(() => window.wgUpdate.safe(true));
  let toast = null;
  for (let i = 0; i < 40; i++) {
    await p.waitForTimeout(1500);
    toast = await p.evaluate(() => { const t = document.getElementById('wg-toast'); return t ? t.textContent : null; }).catch(() => null);
    if (toast && toast.includes('Neue Version')) break;
  }
  const result = {
    nachher: await ver(),
    hinweis: toast, neuGeladen: await p.evaluate(() => !window.__alteSeite),
    spielstand: await p.evaluate(() => localStorage.getItem('wg.testmarke')),
    novaCache: await p.evaluate(() => caches.open('wissensgarten-nova').then((c) => c.match('/nova-test')).then((r) => !!r)),
    caches: await p.evaluate(() => caches.keys()),
    fehler: errs.slice(0, 3),
  };
  console.log(JSON.stringify(result, null, 1));
  await b.close();
  process.exit(result.hinweis && result.hinweis.includes('Neue Version') && result.spielstand === 'bleibt' && result.novaCache ? 0 : 1);
})();
