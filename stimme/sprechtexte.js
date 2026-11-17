// Schritt 1: sammelt alle deutschen Sprechtexte der App (so, wie mathSpeak sie liefert) → stimme/build/sprechtexte.txt
// Aufruf (aus dem Repo-Wurzelordner, Web-App lokal auf Port 8765):  node stimme/sprechtexte.js
const { chromium } = require('playwright'); const fs = require('fs');
(async () => { const b = await chromium.launch(); const ctx = await b.newContext({ locale: 'de-DE', serviceWorkers: 'block' });
 await ctx.addInitScript(() => { window.__noWelcome = true; });
 const p = await ctx.newPage(); await p.goto('http://localhost:8765/'); await p.waitForTimeout(1200);
 const keys = fs.readFileSync(require('path').join(__dirname, '../i18n/uebersetzungen.tsv'), 'utf8').split('\n').filter(l => l && !l.startsWith('#')).map(l => l.split('\t')[0]);
 const out = await p.evaluate(keys => { const S_ = new Set(); const add = x => { if (x) S_.add(x); };
   for (const tp of Object.keys(GEN)) for (let l = 0; l < 6; l++) for (let i = 0; i < 400; i++) { const t = makeTask(tp, l);
     add(t.story ? mathSpeak(t.e[0].v, 'de') : taskSpeech(t)); add(t.hint && mathSpeak(t.hint, 'de')); (t.w || []).forEach(w => add(mathSpeak(w, 'de'))); add(mathSpeak('Richtig ist: ' + showAns(t.a) + (t.unit ? ' ' + t.unit : ''), 'de')); }
   for (const T of Object.values(THEORIE)) [T.k, ...T.l.flat()].forEach(x => add(mathSpeak(x, 'de')));
   keys.forEach(k => add(mathSpeak(k.replace(/\{\}/g, '7'), 'de')));
   ['Hallo, ich bin Blitz!'].forEach(add);
   return [...S_]; }, keys);
 fs.mkdirSync(require('path').join(__dirname, 'build'), { recursive: true }); fs.writeFileSync(require('path').join(__dirname, 'build/sprechtexte.txt'), out.join('\n')); console.log(out.length); await b.close(); })();
