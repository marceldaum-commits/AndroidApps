// Familien-Speicher in der WebApp – Rundlauf mit dem unveränderten Baustein familie.js gegen einen nachgebauten Server /familie/v1/:
// Familie anlegen (Notfall-Kopie, Index, Stand – alles verschlüsselt, ohne Freischalt-Code), auf einem zweiten Gerät mit dem Familien-Link beitreten
// und den Stand holen, und bei zwei Ständen wird nichts still überschrieben. Ohne offenes Eltern-Tor ist die Seite nicht erreichbar.
// node test/web-familie.mjs <webappOrdner> <Ordner für Bildschirmfotos>
import { createRequire } from 'module'; const require = createRequire(import.meta.url); const { chromium } = require('playwright');
import http from 'http'; import fs from 'fs'; import path from 'path'; import { createHash } from 'node:crypto';
const [,, dir, out] = process.argv; fs.mkdirSync(out, { recursive: true });
const F = await import(path.resolve(dir, 'familie.js'));
const sha = (s) => createHash('sha256').update(s).digest('base64url');
const fam = new Map(); const seen = [];
const server = http.createServer((q, r) => {
  const u = q.url.split('?')[0]; const m = /^\/familie\/v1\/([\w-]{22})(\/.*)?$/.exec(u);
  if (m) { let body = ''; q.on('data', (d) => body += d); q.on('end', () => { const [, id, pfad = ''] = m, h = q.headers, e = fam.get(id); const log = { method: q.method, id, pfad, body, cookie: h.cookie, type: h['content-type'], match: h['if-match'] }; seen.push(log);
    const end = (st, obj) => { log.status = st; r.writeHead(st, { 'Content-Type': 'application/json' }); r.end(obj === undefined ? '' : JSON.stringify(obj)); };
    const darf = () => e && e.schreib === sha(h['x-schreib'] || '');
    if (pfad === '' && q.method === 'PUT') { if (!/^[\w-]{43}$/.test(h['x-notfall-pruef'] || '')) return end(400, { fehler: 'notfall_pruef' }); if (e) return end(409, { fehler: 'gibt_es_schon' });
      fam.set(id, { schreib: sha(h['x-schreib']), notfall: body, blobs: new Map() }); return end(201, {}); }
    if (!e) return end(404, { fehler: 'unbekannt' });
    if (pfad === '' && q.method === 'DELETE') { if (!darf()) return end(403, { fehler: 'schreibrecht' }); fam.delete(id); log.status = 204; r.writeHead(204); return r.end(); }
    if (pfad === '/protokoll') return end(200, { eintraege: [{ art: 'angelegt', t: '2026-11-19T15:31' }] });
    if (pfad === '/liste') return end(200, { blobs: [...e.blobs].map(([k, b]) => ({ pfad: k, rev: b.rev, t: b.t })) });
    if (/^\/(index|s\/[a-z][a-z0-9-]+\/([\w-]{22}|geraet-[\w-]+))$/.test(pfad)) { const b = e.blobs.get(pfad);
      if (q.method === 'GET') return b ? end(200, { d: b.d, rev: b.rev, t: b.t }) : end(404, {});
      if (q.method === 'PUT') { if (!darf()) return end(403, { fehler: 'schreibrecht' }); if (String(b ? b.rev : 0) !== String(h['if-match'])) return end(409, { fehler: 'veraltet', rev: b?.rev ?? 0, d: b?.d, t: b?.t });
        const nb = { d: body, rev: (b?.rev || 0) + 1, t: new Date().toISOString() }; e.blobs.set(pfad, nb); return end(200, { rev: nb.rev }); } }
    end(404, {}); }); return; }
  let p = decodeURIComponent(u); if (p === '/') p = '/index.html'; const f = path.join(dir, p);
  fs.readFile(f, (e, d) => { if (e) { r.writeHead(404); r.end(); } else { r.writeHead(200, { 'Content-Type': f.endsWith('.wasm') ? 'application/wasm' : f.endsWith('.js') || f.endsWith('.mjs') ? 'text/javascript' : f.endsWith('.html') ? 'text/html' : f.endsWith('.json') ? 'application/json' : 'application/octet-stream' }); r.end(d); } });
});
await new Promise((ok) => server.listen(0, ok)); const base = `http://localhost:${server.address().port}`; const url = base + '/';
const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
let fail = 0; const check = (ok, text) => { console.log((ok ? 'ok   ' : 'FAIL ') + text); if (!ok) fail++; };
// unterste breite Zeile in einer Knopf-Farbe (Rand eines Knopfs über fast die ganze Breite); „oben“ = oberste statt unterste
const zeile = (page, png, farbe, oben) => page.evaluate(async ([png, farbe, oben]) => { const bmp = await createImageBitmap(await (await fetch('data:image/png;base64,' + png)).blob()); const o = new OffscreenCanvas(bmp.width, bmp.height); const g = o.getContext('2d'); g.drawImage(bmp, 0, 0);
  const passt = (r, gr, bl) => farbe === 'cyan' ? r < 120 && gr > 190 && bl > 220 : r < 150 && gr > 220 && bl > 120 && bl < 200;
  const k = bmp.width / innerWidth; const ys = []; for (let yy = bmp.height - 1; yy > 0; yy--) { let n = 0; const d = g.getImageData(0, yy, bmp.width, 1).data; for (let x = 0; x < bmp.width; x++) { const i = x * 4; if (passt(d[i], d[i + 1], d[i + 2])) n++; } if (n > bmp.width * 0.5) ys.push(yy / k); }
  return ys.length ? (oben ? ys[ys.length - 1] : ys[0]) : -1; }, [png.toString('base64'), farbe, !!oben]);
const neu = async (h, hooks = true) => { const ctx = await b.newContext({ viewport: { width: 400, height: h }, serviceWorkers: 'block' }); const page = await ctx.newPage(); const errs = []; page.on('pageerror', (e) => errs.push(e.message));
  await page.addInitScript(([u, hooks]) => { window.__rlFamilieServer = u; if (hooks) window.__rlTorOffen = true; }, [base, hooks]);
  const open = async (q) => { await page.goto(url + q); await page.waitForFunction(() => document.getElementById('start').style.display === 'none', null, { timeout: 120000 }); await page.waitForTimeout(3000); };
  return { ctx, page, errs, open }; };
const st = (page) => page.evaluate(() => localStorage.getItem('raumlicht.familie') || '');
for (const modus of ['wasm', 'js']) {
  fam.clear(); seen.length = 0;
  // ── ohne offenes Eltern-Tor: „?ziel=familie“ zeigt das Tor, nichts wird angelegt ──
  const Z = await neu(860, false); await Z.open(`?modus=${modus}&ziel=familie`); const zu = await Z.page.screenshot({ path: `${out}/${modus}-0-tor.png` });
  await Z.page.mouse.click(200, 430); await Z.page.waitForTimeout(1500);
  check(seen.length === 0 && !(await st(Z.page)) && !Z.page.url().includes('ziel='), `${modus}: ohne Eltern-Tor ist die Familien-Seite nicht erreichbar, nichts geht raus`); await Z.ctx.close();
  // ── Gerät A: Familie anlegen ──
  const A = await neu(1100); await A.page.addInitScript(() => { if (!localStorage.getItem('raumlicht.sterne')) { localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=2'); localStorage.setItem('raumlicht.proCode', 'PRUEFCODE'); localStorage.setItem('raumlicht.kiDevice', 'geraet-alt'); } });
  await A.open(`?modus=${modus}&ziel=familie`); const a1 = await A.page.screenshot({ path: `${out}/${modus}-1-vorher.png` });
  check(seen.length === 0 && !(await st(A.page)), `${modus}: vor dem Anlegen ist nichts gespeichert und nichts gesendet`);
  // erst „Einverstanden“ (einheitlicher Eltern-Hinweis), dann „Familie anlegen“
  const ye = await zeile(A.page, a1, 'cyan'); await A.page.mouse.click(200, ye - 20); await A.page.waitForTimeout(1500); const a1b = await A.page.screenshot({ path: `${out}/${modus}-1b-einverstanden.png` });
  check(seen.length === 0 && !(await st(A.page)), `${modus}: „Einverstanden“ allein sendet nichts`);
  const ya = await zeile(A.page, a1b, 'cyan'); await A.page.mouse.click(200, ya - 20); await A.page.waitForTimeout(4000); await A.page.screenshot({ path: `${out}/${modus}-2-angelegt.png` });
  const sa = await st(A.page); const [famText, kid, nummer, rev] = sa.split('|'); const [famId, famKey, schreib] = (famText || '').split('.');
  check(/^[\w-]{22}\.[\w-]{43}\.[\w-]{22}\|[\w-]{22}\|[0-9A-Z]{4}-[0-9A-Z]{4}\|1\|/.test(sa), `${modus}: Familie angelegt, Kind und Familien-Nummer gemerkt, Stand 1 gesichert` + (ya < 0 ? ' – Knopf nicht gefunden' : ''));
  const e = fam.get(famId); const puts = seen.filter((s) => s.method === 'PUT');
  check(!!e && /^[\w-]{43}\.[\w-]{16}\.[\w-]{64}$/.test(e.notfall) && puts.length === 3 && seen.some((s) => s.pfad === '/protokoll') && puts.every((p) => /^text\/plain/.test(p.type || '') && !p.cookie) && puts[1].pfad === '/index' && puts[2].pfad === `/s/raumlicht/${kid}` && puts[2].match === '0',
    `${modus}: Notfall-Kopie, Index und Stand hochgeladen – als Text, ohne Cookie, mit If-Match`);
  const alles = JSON.stringify(seen) + JSON.stringify([...fam].map(([k, v]) => [k, v.notfall, [...v.blobs]]));
  check(!alles.includes(famKey) && !alles.includes('PRUEFCODE') && !alles.includes('1-1=3') && !alles.includes('geraet-alt') && [...e.blobs.values()].every((x) => /^[\w-]{16}\.[\w-]+$/.test(x.d)), `${modus}: der Server sieht weder Schlüssel noch Lernstand noch Freischalt-Code`);
  const N = F.familie({ famId, famKey, schreib }, { basis: base, f: fetch }); const s1 = await N.standLaden('raumlicht', kid); const i1 = await N.indexLaden();
  check(s1.ok && s1.rev === 1 && s1.daten.sterne === '1-1=3;1-2=2' && !('proCode' in s1.daten) && !('kiDevice' in s1.daten) && !('familie' in s1.daten) && i1.ok && i1.daten.kinder.length === 1 && i1.daten.kinder[0].kindId === kid && i1.daten.apps.includes('raumlicht'),
    `${modus}: mit dem Familien-Link lässt sich der Stand lesen – ohne Freischalt-Code und ohne Geräte-Kennung; Index nennt Kind und App`);
  check(N.nummer === nummer, `${modus}: Familien-Nummer stimmt mit dem Baustein überein (${nummer})`);
  const n1 = seen.filter((s) => s.method === 'PUT').length; await A.open(`?modus=${modus}`); await A.page.waitForTimeout(1500);
  check(seen.filter((s) => s.method === 'PUT').length === n1 && await st(A.page) === sa, `${modus}: nach neu Laden bleibt die Familie, ohne Änderung keine zweite Sicherung`);
  // ── Gerät B (frisch): mit dem Familien-Link öffnen, beitreten, Kind wählen, Stand holen ──
  const B = await neu(1100); await B.open(`?modus=${modus}#familie=${famText}`); const b1 = await B.page.screenshot({ path: `${out}/${modus}-3-neues-geraet.png` });
  check(!(await B.page.evaluate(() => location.href)).includes(famKey), `${modus}: der Schlüssel verschwindet sofort aus der Adresszeile`);
  const yeb = await zeile(B.page, b1, 'cyan'); await B.page.mouse.click(200, yeb - 20); await B.page.waitForTimeout(1500); const b1b = await B.page.screenshot({ path: `${out}/${modus}-3b-einverstanden.png` });
  const yb = await zeile(B.page, b1b, 'gruen'); await B.page.mouse.click(200, yb - 20); await B.page.waitForTimeout(3000); const b2 = await B.page.screenshot({ path: `${out}/${modus}-4-kind-waehlen.png` });
  const yk = await zeile(B.page, b2, 'cyan', true); await B.page.mouse.click(200, yk + 20);
  await B.page.waitForFunction(() => localStorage.getItem('raumlicht.sterne') === '1-1=3;1-2=2', null, { timeout: 20000 }).catch(() => {}); await B.page.waitForTimeout(3000); await B.page.screenshot({ path: `${out}/${modus}-5-geholt.png` });
  const sb = await st(B.page);
  check(await B.page.evaluate(() => localStorage.getItem('raumlicht.sterne')) === '1-1=3;1-2=2' && sb.startsWith(`${famText}|${kid}|${nummer}|1|`) && !(await B.page.evaluate(() => localStorage.getItem('raumlicht.proCode'))),
    `${modus}: zweites Gerät tritt mit dem Link bei und holt den Lernstand – ohne Freischalt-Code` + (yb < 0 || yk < 0 ? ' – Knopf nicht gefunden' : ''));
  // ── zwei Stände: B lernt weiter und sichert (Stand 2); A hat inzwischen anders gelernt → nichts wird still überschrieben ──
  await B.page.evaluate(() => localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=2;1-3=3')); await B.open(`?modus=${modus}`); await B.page.waitForTimeout(2500);
  const s2 = await N.standLaden('raumlicht', kid); check(s2.ok && s2.rev === 2 && s2.daten.sterne === '1-1=3;1-2=2;1-3=3', `${modus}: nach dem Lernen sichert das zweite Gerät von selbst (Stand 2)`);
  await A.page.evaluate(() => localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=3')); await A.open(`?modus=${modus}&ziel=familie`); await A.page.waitForTimeout(2500); await A.page.screenshot({ path: `${out}/${modus}-6-zwei-staende.png` });
  const s3 = await N.standLaden('raumlicht', kid); const k409 = seen.filter((s) => s.status === 409 && s.pfad.startsWith('/s/'));
  check(k409.length === 1 && k409[0].match === '1' && s3.rev === 2 && s3.daten.sterne === '1-1=3;1-2=2;1-3=3' && await A.page.evaluate(() => localStorage.getItem('raumlicht.sterne')) === '1-1=3;1-2=3',
    `${modus}: veralteter Stand wird abgewiesen – weder Server noch Gerät werden still überschrieben, die Eltern werden gefragt`);
  // ── die Familie wird (anderswo) gelöscht: Gerät B merkt es beim nächsten Sichern, trennt sich und behält seinen Lernstand ──
  check((await N.loeschen()).ok && !fam.has(famId), `${modus}: Familie gelöscht (mit Schreibrecht)`);
  await B.page.evaluate(() => localStorage.setItem('raumlicht.sterne', '1-1=3;1-2=2;1-3=3;1-4=1')); await B.open(`?modus=${modus}`); await B.page.waitForTimeout(2500);
  check(!(await st(B.page)) && await B.page.evaluate(() => localStorage.getItem('raumlicht.sterne')) === '1-1=3;1-2=2;1-3=3;1-4=1', `${modus}: gibt es die Familie nicht mehr, trennt sich das Gerät – der Lernstand bleibt`);
  check(A.errs.length + B.errs.length === 0, A.errs.length + B.errs.length ? `${modus}: Fehler: ` + (A.errs[0] || B.errs[0]) : `${modus}: keine Fehler`);
  await A.ctx.close(); await B.ctx.close();
}
await b.close(); server.close(); console.log(fail ? `${fail} FEHLER` : 'web-familie: alle Prüfungen bestanden'); process.exit(fail ? 1 : 0);
