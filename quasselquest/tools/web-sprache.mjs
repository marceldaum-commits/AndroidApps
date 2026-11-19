// Beleg: Die Spracherkennung läuft im Standardweg nur auf dem Gerät. Aufruf: node tools/web-sprache.mjs
// Prüft in Chromium und WebKit mit eingeschleusten Ersatz-Erkennern:
//  A) Browser kann „auf dem Gerät“ (processLocally)      → Start nur mit processLocally = true
//  B) Browser kann es nicht (z. B. Safari/iPad)            → Erkennung startet NIE (nur Lautstärke-Prüfung)
//  C) Browser behauptet es, setzt es aber nicht wirklich  → Erkennung startet NIE
//  D) Eltern erlauben ausdrücklich die Online-Erkennung    → erst dann Start ohne processLocally (nur bei B-Browsern)
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const { chromium, webkit } = createRequire(import.meta.url)('playwright');
const root = fileURLToPath(new URL('../webapp/', import.meta.url));
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json' };
const server = createServer(async (req, res) => {
  let p = decodeURIComponent(new URL(req.url, 'http://x').pathname); if (p.endsWith('/')) p += 'index.html';
  const f = normalize(join(root, p)); try { res.writeHead(200, { 'Content-Type': types[extname(f)] || 'application/octet-stream' }); res.end(await readFile(f)); } catch { res.writeHead(404); res.end(); }
});
await new Promise((r) => server.listen(0, '127.0.0.1', r));
const base = `http://127.0.0.1:${server.address().port}/?diag=1`;
let failed = 0; const ok = (c, m) => { console.log((c ? '  ✅ ' : '  ❌ ') + m); if (!c) failed++; };

// Ersatz-Erkenner: protokolliert jeden Start mit dem Wert von processLocally
const fake = (variant) => `(() => {
  window.__starts = [];
  class FakeSR { constructor() { this._pl = false; }
    start() { window.__starts.push({ processLocally: this.processLocally === true }); setTimeout(() => this.onend && this.onend(), 10); }
    abort() {} stop() {} }
  if ('${variant}' === 'local') Object.defineProperty(FakeSR.prototype, 'processLocally', { get() { return this._pl; }, set(v) { this._pl = v; }, configurable: true });
  if ('${variant}' === 'liar') Object.defineProperty(FakeSR.prototype, 'processLocally', { get() { return false; }, set(v) {}, configurable: true });
  window.SpeechRecognition = FakeSR; window.webkitSpeechRecognition = FakeSR;
})()`;

for (const [name, type] of [['Chromium', chromium], ['WebKit', webkit]]) {
  console.log(name);
  const b = await type.launch();
  for (const [variant, rec, expectStart, expectLocal, label] of [
    ['local', null, 1, true, 'A) kann auf dem Gerät, Standard → startet nur mit processLocally'],
    ['none', null, 0, null, 'B) kann es nicht (wie Safari), Standard → startet nicht'],
    ['liar', null, 0, null, 'C) setzt processLocally nicht wirklich, Standard → startet nicht'],
    ['none', 'browser', 1, false, 'D) Eltern erlauben Online-Erkennung → erst dann Start'],
    ['local', 'off', 0, null, 'E) Eltern schalten Erkennung aus → startet nicht'],
  ]) {
    const ctx = await b.newContext(); const page = await ctx.newPage();
    await page.addInitScript(fake(variant));
    await page.goto(base); await page.waitForSelector('[data-k]');
    const r = await page.evaluate(async (recPref) => {
      const T = window.quasselTest; const st = T.state();
      const before = st.settings.recognition;
      if (recPref) st.settings.recognition = recPref;
      await T.A.listen();
      return { before, starts: window.__starts };
    }, rec);
    const good = r.before === 'device' && r.starts.length === expectStart && (expectStart === 0 || r.starts.every((x) => x.processLocally === expectLocal));
    ok(good, `${label} (Standard „${r.before}“, Starts ${r.starts.length}${r.starts.length ? ', processLocally ' + r.starts[0].processLocally : ''})`);
    await ctx.close();
  }
  // F) Rückfall auf die Gerätestimme nimmt die passende Sprache (nicht immer Englisch)
  {
    const ctx = await b.newContext(); const page = await ctx.newPage();
    await page.addInitScript(() => {
      const mk = (lang, local) => ({ lang, localService: local, name: lang, voiceURI: lang, default: false });
      const voices = [mk('en-GB', true), mk('de-DE', true), mk('fr-FR', true), mk('es-ES', true), mk('es-US', false)];
      window.__said = [];
      Object.defineProperty(window, 'speechSynthesis', { value: { getVoices: () => voices, addEventListener() {}, cancel() {},
        speak(u) { window.__said.push(u.voice && u.voice.lang); setTimeout(() => u.onend && u.onend(), 5); } }, configurable: true });
      window.SpeechSynthesisUtterance = function (t) { this.text = t; };
    });
    await page.goto(base); await page.waitForSelector('[data-k]');
    const said = await page.evaluate(async () => {
      const A = window.quasselTest.A; window.quasselTest.state().settings.stimme = 'geraet';
      for (const who of ['ruby', 'de', 'fr', 'es']) await A.say('Test ' + who, { who });
      return window.__said;
    });
    ok(JSON.stringify(said) === JSON.stringify(['en-GB', 'de-DE', 'fr-FR', 'es-ES']), 'F) Gerätestimme in der richtigen Sprache: ' + said.join(', '));
    await ctx.close();
  }
  await b.close();
}
server.close();
console.log(failed ? `\n❌ ${failed} fehlgeschlagen` : '\n✅ Beleg: Ohne Zustimmung der Eltern startet die Erkennung nur, wenn sie auf dem Gerät läuft.');
process.exit(failed ? 1 : 0);
