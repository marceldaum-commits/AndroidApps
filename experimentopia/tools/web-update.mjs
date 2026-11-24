// Browser-Test: Aktualisiert sich die WebApp von selbst – ohne Datenverlust und nie mitten in einem Versuch?
// Aufruf (nach ./gradlew :web:webapp):  node tools/web-update.mjs   (braucht Playwright + Chromium und python3)
// Ablauf: Version A (= webapp/) läuft, auf dem „Server“ erscheint Version B (Kopie mit höherer Nummer),
// der Tab kehrt zurück → B wird geladen; mitten im Versuch wird erst nach dem Wechsel zur Startseite neu geladen.
import { createRequire } from 'module';
import { execSync, spawn } from 'child_process';
import fs from 'fs'; import path from 'path'; import os from 'os';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const srv = fs.mkdtempSync(path.join(os.tmpdir(), 'exup-'));
const A = path.join(srv, 'A'), B = path.join(srv, 'B'), cur = path.join(srv, 'cur');
execSync(`cp -r "${root}/webapp" "${A}" && cp -r "${root}/webapp" "${B}"`);
const vA = JSON.parse(fs.readFileSync(path.join(A, 'version.json'))).version;
const vB = vA.replace(/(\d+)$/, (n) => String(+n + 1));
// neue Version: andere Nummer, und eine spätere Änderungszeit (der Testserver vergleicht nur die Uhrzeit, der echte Server den Inhalt)
const later = Date.now() / 1000 + 120;
for (const f of ['version.json', 'sw.js', 'index.html']) { const p = path.join(B, f); fs.writeFileSync(p, fs.readFileSync(p, 'utf8').split(vA).join(vB)); fs.utimesSync(p, later, later); }
fs.symlinkSync(A, cur);
const server = spawn('python3', ['-m', 'http.server', '8790', '--bind', '127.0.0.1'], { cwd: srv, stdio: 'ignore' });
const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) process.exitCode = 1; };
try {
  await new Promise((r) => setTimeout(r, 800));
  const b = await chromium.launch(); const p = await b.newPage();
  const url = 'http://localhost:8790/cur/';
  await p.goto(url); await p.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 90000 });
  await p.waitForFunction(() => navigator.serviceWorker.controller, null, { timeout: 30000 });
  await p.evaluate(() => { localStorage.setItem('experimentopia.testmarke', 'bleibt'); return caches.open('experimentopia-nova').then((c) => c.put('/nova-test', new Response('x'))); });
  ok((await p.evaluate(() => fetch('version.json?nc=' + Date.now(), { cache: 'no-store' }).then((r) => r.json()))).version === vA, `läuft: ${vA}`);
  // Mitten im Versuch: neue Version liegt bereit, aber kein Neuladen
  await p.evaluate(() => { window.__exHome = false; });
  fs.unlinkSync(cur); fs.symlinkSync(B, cur);
  await p.evaluate(() => window.dispatchEvent(new Event('focus')));
  await p.waitForTimeout(6000);
  ok(await p.evaluate(() => typeof window.__exTry === 'function' && !document.getElementById('neu')) && p.url() === url, 'mitten im Versuch: kein Neuladen');
  // Zurück zur Startseite → lädt von selbst neu und zeigt den Hinweis
  const nav = p.waitForNavigation({ timeout: 30000 });
  await p.evaluate(() => { window.__exHome = true; window.__exTry(); });
  await nav;
  await p.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 90000 });
  const v = await p.evaluate(() => fetch('version.json?nc=' + Date.now(), { cache: 'no-store' }).then((r) => r.json()));
  ok(v.version === vB, `nach Rückkehr zur Startseite automatisch neu: ${v.version}`);
  const hint = await p.evaluate(() => { const n = document.getElementById('neu'); return n ? n.textContent : ''; });
  ok(hint.includes(vB), `Hinweis: „${hint}“`);
  ok(await p.evaluate(() => localStorage.getItem('experimentopia.testmarke')) === 'bleibt', 'Spielstand/Einstellungen (localStorage) erhalten');
  const ks = await p.evaluate(() => caches.keys());
  ok(!ks.includes('experimentopia-nova') && ks.includes('experimentopia-' + vB) && !ks.includes('experimentopia-' + vA), `Speicher aufgeräumt, alter Stimmen-Speicher (nova) gelöscht: ${ks.join(', ')}`);
  // Neuer Tab mit alter offener Version: nie hängen bleiben – beim nächsten Öffnen gilt die neue
  const p2 = await b.newPage(); await p2.goto(url); await p2.waitForFunction(() => document.getElementById('start').dataset.fertig === '1', null, { timeout: 90000 });
  ok(await p2.evaluate((v) => document.documentElement.innerHTML.includes('const CACHE') || document.getElementById('start-text').textContent.includes(v) || fetch('version.json?nc=1', { cache: 'no-store' }).then((r) => r.json()).then((j) => j.version === v), vB), `neuer Tab startet mit ${vB}`);
  await b.close();
} finally { server.kill(); fs.rmSync(srv, { recursive: true, force: true }); }
