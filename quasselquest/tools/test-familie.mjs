// Familien-Speicher in Quasselquest: Brücke + familie.js @ e9993ec gegen einen nachgebauten Server /familie/v1/ (nur zum Testen – den echten Server betreibt der Leitstand)
// und das Zusammenführen (speicher.js). Aufruf: node tools/test-familie.mjs
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
globalThis.window = globalThis;
const speicher = () => { const m = new Map(); return { getItem: (k) => (m.has(k) ? m.get(k) : null), setItem: (k, v) => m.set(k, String(v)), removeItem: (k) => m.delete(k) }; };
const geraetA = speicher(), geraetB = speicher();
globalThis.localStorage = geraetA;
globalThis.location = { origin: 'https://lernen-test.mathenachhilfe-online.com' };
globalThis.window.dispatchEvent = () => true;
globalThis.window.SPEICHER = require('../webapp/speicher.js');
const F = await import('../webapp/js/familie.js');
await import('../webapp/familie-bruecke.js');
const B = globalThis.window.FAMILIE;
let n = 0, fehler = 0; const ok = (b, t) => { n++; if (!b) { fehler++; console.error('FEHLER:', t); } };
/* ───────────── nachgebauter Server (nach ops/familie/SCHNITTSTELLE.md) ───────────── */
const GESPERRT = [], VERGEBEN = new Map();
const sha = async (s) => F.b64url(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s))));
function server() {
  const fam = new Map(); let uhr = 0; const log = [];
  const antwort = (status, obj) => ({ status, ok: status >= 200 && status < 300, json: async () => obj });
  const f = async (url, o = {}) => {
    const m = /\/familie\/v1\/([\w-]{22})(\/.*)?$/.exec(url); if (!m) return antwort(404, {});
    const [, id, pfad = ''] = m, meth = o.method || 'GET', h = o.headers || {}, e = fam.get(id);
    log.push(`${meth} ${pfad || '/'}`);
    const darf = async () => e && e.schreib === await sha(h['X-Schreib'] || '');
    if (pfad === '' && meth === 'PUT') { if (!/^[\w-]{43}$/.test(h['X-Notfall-Pruef'] || '')) return antwort(400, { fehler: 'notfall_pruef' }); if (e) return antwort(409, { fehler: 'gibt_es_schon' }); fam.set(id, { schreib: await sha(h['X-Schreib']), notfall: o.body, pruef: h['X-Notfall-Pruef'], blobs: new Map(), rechte: [] }); return antwort(201, {}); }
    if (!e) return antwort(404, { fehler: 'unbekannt' });
    if (pfad === '' && meth === 'DELETE') { if (!(await darf())) return antwort(403, {}); fam.delete(id); return antwort(204, null); }
    if (pfad === '/liste') return antwort(200, { blobs: [...e.blobs].map(([k, b]) => ({ pfad: k, rev: b.rev, t: b.t })) });
    if (pfad === '/rechte') return antwort(200, { rechte: e.rechte });
    if (pfad === '/protokoll') return antwort(200, { eintraege: [] });
    if (pfad === '/einloesen' && meth === 'POST') {
      if (!(await darf())) return antwort(403, { fehler: 'schreibrecht' });
      const nr = await F.codeNummer(JSON.parse(o.body).code); if (!nr) return antwort(404, { fehler: 'code_unbekannt' });
      if (GESPERRT.includes(nr)) return antwort(403, { fehler: 'code_gesperrt' });
      if (VERGEBEN.has(nr) && VERGEBEN.get(nr) !== id) return antwort(409, { fehler: 'code_vergeben' });
      VERGEBEN.set(nr, id); const recht = { app: 'mathetrainer', produkt: 'pass', codeNr: nr, bis: '2027-07-31', ki: true }; e.rechte = [...e.rechte.filter((r) => r.codeNr !== nr), recht];
      return antwort(200, { rechte: e.rechte, recht });
    }
    if (/^\/(index|s\/[a-z][a-z0-9-]+\/([\w-]{22}|geraet-[\w-]+))$/.test(pfad)) {
      const b = e.blobs.get(pfad);
      if (meth === 'GET') return b ? antwort(200, { d: b.d, rev: b.rev, t: b.t }) : antwort(404, { fehler: 'leer' });
      if (meth === 'PUT') {
        if (!(await darf())) return antwort(403, {});
        if (String(b ? b.rev : 0) !== String(h['If-Match'])) return antwort(409, { fehler: 'veraltet', rev: b?.rev ?? 0, d: b?.d, t: b?.t });
        const nb = { d: o.body, rev: (b?.rev || 0) + 1, t: ++uhr }; e.blobs.set(pfad, nb); return antwort(200, { rev: nb.rev });
      }
    }
    return antwort(404, {});
  };
  return { f, fam, log };
}
const S = server();
const stA = { v: 3, name: 'Lena', klasse: 3, karten: { 'a': { b: 2, d: 100 } }, tage: { en: { 1: [4, 3] } }, lp: { 'fr-1-1': 3 }, progress: { w1e1: { done: true, stars: 2, at: 1 } }, eigene: {}, week: { key: '2026-W48', n: 2 }, settings: { sound: true } };
const stB = { v: 3, name: 'Lena', klasse: 3, karten: { 'a': { b: 3, d: 50 }, 'c': { b: 1, d: 7 } }, tage: { en: { 1: [2, 2] } }, lp: { 'fr-1-1': 1, 'es-1-1': 2 }, progress: { w1e1: { done: true, stars: 3, at: 2 } }, eigene: {}, week: { key: '2026-W48', n: 3 }, settings: { sound: false } };

// Gerät A legt die Familie an
let r = await B.anlegen(stA, S.f);
ok(r.ok, 'Familie anlegen und ersten Stand sichern: ' + JSON.stringify(r.grund));
const stA1 = B.status(); ok(stA1.gekoppelt && /^[0-9A-Z]{4}-[0-9A-Z]{4}$/.test(stA1.nummer), 'Familien-Nummer ' + stA1.nummer);
const link = B.link(); ok(/\/quasselquest\/#familie=[\w-]{22}\.[\w-]{43}\.[\w-]{22}$/.test(link), 'Link: ' + link.slice(0, 60) + '…');
const blobs = [...[...S.fam.values()][0].blobs.values()].map((b) => b.d).join('|');
ok(!/Lena|karten|fr-1-1/.test(blobs) && !blobs.includes(B.link().split('.')[1]), 'Beim Server liegt kein Klartext (nur verschlüsselt)');
// V40 Option A: Index und Stand enthalten weder Spitzname noch Klasse – wir entschlüsseln beides mit dem Familien-Schlüssel und sehen nach
const famA = JSON.parse(localStorage.getItem('quasselquest-familie')).fam; const faA = F.familie(famA, { basis: 'https://lernen-test.mathenachhilfe-online.com', f: S.f });
const idx = (await faA.indexLaden()).daten; ok(JSON.stringify(idx.kinder.map((k) => [k.name, k.klasse, !!k.farbe])) === JSON.stringify([['Profil 1', '', true]]) && !/Lena/.test(JSON.stringify(idx)), 'Index: nur „Profil 1“ und Symbol, kein Spitzname, keine Klasse: ' + JSON.stringify(idx.kinder));
const kid = (await faA.standLaden('quasselquest', JSON.parse(localStorage.getItem('quasselquest-familie')).kindId)).daten; ok(kid.name === '' && !/Lena/.test(JSON.stringify(kid)), 'Gesicherter Stand enthält den Namen nicht');
ok(/PUT \/$/.test(S.log[0]) && S.log.some((l) => l.includes('/s/quasselquest/')), 'Server bekam: Anlegen, Index, Stand');

// Gerät B tritt per Link bei, wählt das Kind und führt zusammen
const linkA = B.link(); const kennA = B.status().nummer;
globalThis.localStorage = geraetB;
ok(!B.gekoppelt(), 'Gerät B ist vor dem Beitritt nicht gekoppelt');
const v = await B.beitretenVorbereiten(linkA, S.f); ok(v.ok && v.kinder.length === 1 && v.kinder[0].name === 'Profil 1', 'Beitritt: „Profil 1“ zur Auswahl (kein Spitzname)');
r = await B.beitreten(v, v.kinder[0].kindId, stB, S.f);
ok(r.stand.name === 'Lena', 'Der Name bleibt der des Geräts'); ok(r.ok && r.stand.karten.a.b === 3 && r.stand.karten.c && r.stand.lp['fr-1-1'] === 3 && r.stand.lp['es-1-1'] === 2 && r.stand.tage.en[1][0] === 4 && r.stand.progress.w1e1.stars === 3 && r.stand.week.n === 3, 'Zusammenführen beim Beitritt (höherer Kasten, Maximum je Einheit/Tag, Sterne)');
ok(r.abweichungen.includes('einstellungen') && r.stand.settings.sound === false, 'Einstellungen unterscheiden sich → Gerätestand bleibt, Hinweis ' + JSON.stringify(r.abweichungen));
ok(B.status().nummer === kennA, 'Gleiche Familien-Nummer auf beiden Geräten');
r = await B.sichern(r.stand, S.f); ok(r.ok, 'Gerät B sichert');

// Konflikt: Gerät A sichert mit altem Stand-Rev → 409 → Zusammenführen → erneut sichern
globalThis.localStorage = geraetA;
r = await B.sichern({ ...stA, lp: { ...stA.lp, 'la-1-1': 5 } }, S.f);
ok(r.ok && r.stand.lp['la-1-1'] === 5 && r.stand.lp['es-1-1'] === 2 && r.stand.karten.c, 'Konflikt (409) wird zusammengeführt, nie still überschrieben');

// Löschen
r = await B.loeschen('0000-0000', S.f); ok(!r.ok && r.grund === 'nummer', 'Löschen mit falscher Familien-Nummer wird abgelehnt');
const nummer = B.status().nummer.toLowerCase();
r = await B.loeschen(nummer, S.f); ok(r.ok && !B.gekoppelt(), 'Löschen mit richtiger Nummer (Groß/Klein egal): lokal nur die Schlüssel weg');
globalThis.localStorage = geraetB;
r = await B.laden(stB, S.f); ok(!r.ok && r.grund === 'familie-weg', 'Anderes Gerät: „Diese Familie gibt es nicht mehr“ (grund ' + r.grund + ')');

// Ungültiger Link
r = await B.beitretenVorbereiten('https://example.org/#familie=kaputt', S.f); ok(!r.ok && r.grund === 'ungueltig', 'Ungültiger Link');

// Zusammenführen: rein
const M = globalThis.window.SPEICHER.zusammenfuehren;
const a0 = JSON.stringify(stA); M(stA, stB); ok(JSON.stringify(stA) === a0, 'Zusammenführen verändert die Eingaben nicht');
ok(M({ eigene: { en: [{ id: 'x', n: 1 }] } }, { eigene: { en: [{ id: 'y', n: 2 }] } }).stand.eigene.en.length === 2, 'Eigene Listen: Vereinigung nach id');
ok(M({ tage: { en: { 5: [3, 3] } } }, { tage: { en: { 5: [3, 3] } } }).stand.tage.en[5][0] === 3, 'Tagessummen werden nicht doppelt gezählt');

console.log(fehler ? `❌ ${fehler} von ${n} Prüfungen fehlgeschlagen` : `✅ Familien-Speicher: ${n} Prüfungen bestanden`);
process.exit(fehler ? 1 : 0);
