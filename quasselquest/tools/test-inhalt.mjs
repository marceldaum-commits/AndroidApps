// Inhalts- und Logiktests: node --test tools/test-inhalt.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const dir = new URL('../webapp/', import.meta.url);
const C = require(new URL('content.js', dir).pathname);
globalThis.window = globalThis; // audio.js erwartet window
const A = require(new URL('audio.js', dir).pathname);
const P = require(new URL('pakete.js', dir).pathname);
const PIDX = JSON.parse(readFileSync(new URL('pakete/index.json', dir), 'utf8'));
const PACKS = PIDX.pakete.map((m) => Object.assign(JSON.parse(readFileSync(new URL('pakete/' + m.id + '.json', dir), 'utf8')), { lang: m.sprache }));

const playable = C.worlds.flatMap((w) => (w.episodes || []).filter((e) => !e.soon));

test('es gibt spielbare Episoden mit Szene, drei Spielen, Mission und Erklärung', () => {
  assert.ok(playable.length >= 3);
  for (const e of playable) {
    assert.ok(e.scene.length >= 4, e.id);
    assert.equal(e.games.length, 3, e.id);
    assert.ok(e.mission.steps.length >= 2, e.id);
    assert.ok(e.explain.points.length >= 3, e.id);
    assert.ok(e.kann && e.kann.length > 20, `${e.id}: „Das kann mein Kind jetzt“ fehlt`);
  }
});

test('jede englische Zeile hat eine deutsche Übersetzung und endet mit Satzzeichen', () => {
  for (const e of playable) {
    const lines = [...e.scene, ...e.games.flatMap((g) => g.lines || g.sentences || [])];
    for (const l of lines) {
      assert.ok(l.en && l.de, `${e.id}: ${JSON.stringify(l)}`);
      assert.match(l.en, /[.!?]$/, `${e.id}: ${l.en}`);
    }
  }
});

test('Spielarten sind gemischt: jede Episode hat mindestens zwei verschiedene', () => {
  for (const e of playable) assert.ok(new Set(e.games.map((g) => g.type)).size >= 2, e.id);
  const all = new Set(playable.flatMap((e) => e.games.map((g) => g.type)));
  for (const t of ['detective', 'echo', 'train', 'rubysays']) assert.ok(all.has(t), t);
});

test('Hör-Detektiv: alle Antworten unterscheidbar (genau eine richtige Lösung)', () => {
  for (const e of playable) for (const g of e.games.filter((x) => x.type === 'detective')) {
    const keys = g.items.map((i) => i.en.toLowerCase());
    assert.equal(new Set(keys).size, keys.length, `${e.id} doppelte Wörter`);
    const pics = g.items.map((i) => i.img || i.css || i.count);
    assert.equal(new Set(pics).size, pics.length, `${e.id} doppelte Bilder`);
    assert.ok(g.items.length >= 4, e.id);
  }
});

test('Ruby says: Ziele eindeutig', () => {
  for (const e of playable) for (const g of e.games.filter((x) => x.type === 'rubysays')) {
    const k = g.targets.map((t) => t.id); assert.equal(new Set(k).size, k.length, e.id);
  }
});

test('Satz-Zug: mindestens 3 Wörter, Wörter eines Satzes nicht doppelt (sonst wäre die Lösung nicht eindeutig)', () => {
  for (const e of playable) for (const g of e.games.filter((x) => x.type === 'train')) for (const s of g.sentences) {
    const w = C.words(s.en); assert.ok(w.length >= 3, s.en); assert.equal(new Set(w).size, w.length, s.en);
  }
});

test('Mission: jeder Schritt hat genau eine richtige Antwort, falsche haben eine freundliche Reaktion', () => {
  for (const e of playable) for (const st of e.mission.steps) {
    assert.equal(st.options.filter((o) => o.ok).length, 1, `${e.id}: ${st.en}`);
    for (const o of st.options.filter((x) => !x.ok)) assert.ok(o.reply && o.reply.en && o.reply.de, o.en);
  }
});

test('Platzhalter werden richtig gefüllt (Großschreibung, a/an, Einzahl)', () => {
  assert.equal(C.fill('{n} fish, please.', { n: 3 }), 'Three fish, please.');
  assert.equal(C.fill('Here you are. {n} fish!', { n: 1 }), 'Here you are. One fish!');
  assert.equal(C.fill('{nFische}, bitte.', { n: 1 }), 'Ein Fisch, bitte.');
  assert.equal(C.fill('{nFische}, bitte.', { n: 7 }), 'Sieben Fische, bitte.');
  assert.equal(C.fill('Wow, {aC} boat!', { c: 'orange' }), 'Wow, an orange boat!');
  assert.equal(C.fill('Wow, {aC} boat!', { c: 'red' }), 'Wow, a red boat!');
  assert.equal(C.fill('My boat is {c}.', { c: 'purple' }), 'My boat is purple.');
  assert.equal(C.fill('Nice to meet you, {name}!', { name: 'Lena' }), 'Nice to meet you, Lena!');
  // Kein Platzhalter bleibt übrig
  for (const e of playable) for (const st of e.mission.steps) for (const o of st.options) for (let n = 1; n <= 10; n++) for (const c of C.colours) {
    const t = C.fill(o.en, { name: 'Lena', n, c: c.id }) + C.fill(o.de, { name: 'Lena', n, c: c.id }) + C.fill(st.en, { name: 'Lena', n, c: c.id });
    assert.doesNotMatch(t, /\{|\}/, t);
  }
});

test('Namensprüfung gibt verständliche Rückmeldung', () => {
  assert.equal(C.checkName('Lena'), null);
  assert.equal(C.checkName('Jan-Ole'), null);
  assert.equal(C.checkName('Ömer'), null);
  assert.match(C.checkName(''), /Vornamen/);
  assert.match(C.checkName('Lena123'), /nur Buchstaben/);
  assert.match(C.checkName('Abcdefghijklmnopq'), /zu lang/);
});

test('Spracherkennung wird großzügig gewertet (Ziffern = Zahlwörter, Satzzeichen egal)', () => {
  assert.equal(A.similarity('Three fish, please.', ['3 fish please']), 1);
  assert.ok(A.similarity("What's your name?", ['whats your name']) >= 0.99);
  assert.ok(A.similarity('My name is Lena.', ['my name is Lina']) >= 0.5);
  assert.ok(A.similarity('Goodbye!', ['hello']) < 0.5);
});

test('Version und Datum stimmen überall überein', () => {
  const vjs = readFileSync(new URL('version.js', dir), 'utf8');
  const v = /APP_VERSION = '([^']+)'/.exec(vjs)[1];
  const d = /APP_VERSION_DATE = '([^']+)'/.exec(vjs)[1];
  const json = JSON.parse(readFileSync(new URL('version.json', dir), 'utf8'));
  assert.equal(json.version, v); assert.equal(json.datum, d);
  assert.match(readFileSync(new URL('sw.js', dir), 'utf8'), new RegExp(`VERSION = '${v.replace(/\./g, '\\.')}'`));
  assert.match(readFileSync(new URL('../CHANGELOG.md', dir), 'utf8'), new RegExp(`## ${v.replace(/\./g, '\\.')} .*\\(${d.replace(/\./g, '\\.')}\\)`));
});

test('Service Worker kennt alle Dateien, die index.html lädt', () => {
  const sw = readFileSync(new URL('sw.js', dir), 'utf8');
  const html = readFileSync(new URL('index.html', dir), 'utf8');
  for (const m of html.matchAll(/(?:src|href)="([^"#:]+)"/g)) assert.ok(sw.includes(`'${m[1]}'`), m[1]);
});

test('Spracherkennung ist standardmäßig nur auf dem Gerät', () => {
  const app = readFileSync(new URL('app.js', dir), 'utf8');
  assert.match(app, /recognition: 'device'/);
  assert.doesNotMatch(app, /recognition: 'browser'/);
});

test('Jeder feste Satz ist vorab vertont (normal und langsam) und die Dateien gibt es', () => {
  const idx = JSON.parse(readFileSync(new URL('audio/index.json', dir), 'utf8')).saetze;
  const norm = (t) => t.normalize('NFC').replace(/\s+/g, ' ').trim();
  const fehlt = [];
  for (const s of C.allSpeech().concat(P.speech(PACKS))) {
    const e = idx[s.who + '|' + norm(s.text)];
    if (!e) { fehlt.push(s.who + '|' + s.text); continue; }
    for (const f of [e.n, e.s]) assert.ok(existsSync(new URL('audio/piper/' + f, dir)), f);
  }
  assert.deepEqual(fehlt, [], 'Bitte tools/vertonen.py laufen lassen');
});

test('Nova ist vorbereitet, aber nicht freigeschaltet (erst nach Marcels Go)', () => {
  assert.match(readFileSync(new URL('version.js', dir), 'utf8'), /NOVA_FREIGEGEBEN = false/);
  const t = JSON.parse(readFileSync(new URL('vorlesen/texte.json', dir), 'utf8'));
  assert.equal(t.app, 'quasselquest');
  assert.ok(t.texte.length >= 100 && t.texte.every((x) => ['en', 'de', 'fr', 'es'].includes(x.sprache) && /^[0-9a-f]{32}$/.test(x.nova)));
});

test('Pakete: alle 5 Sprachen vorhanden, jede Karte hat Frage, Antwort und Merkbild', () => {
  assert.deepEqual([...new Set(PIDX.pakete.map((p) => p.sprache))].sort(), ['de', 'en', 'es', 'fr', 'la']);
  for (const p of PACKS) for (const c of p.cards) { assert.ok(c.q && c.a, p.id + '/' + c.id); assert.ok(c.img, p.id + '/' + c.id + ' ohne Merkbild'); }
  // Deutsch nur Sprach-Teil: keine Stilmittel/Literatur
  assert.ok(!PIDX.pakete.some((p) => /stilmittel|literatur|epoche/.test(p.id)));
});

test('Pakete: falsche Antworten enthalten nie die richtige; genug Auswahl für jede Karte', () => {
  for (const p of PACKS) for (const c of p.cards) {
    const w = P.wrongs(c, p, 3);
    assert.ok(w.length >= 2, p.id + '/' + c.id);
    assert.ok(!w.includes(c.a), p.id + '/' + c.id);
    const k = P.classify(c, p.lang);
    if (P.modes(c, p.lang).includes('hoeren')) { const wd = P.wrongs(c, p, 3, 'de'); assert.ok(wd.length >= 2 && !wd.includes(k.de), p.id + '/' + c.id); }
  }
});

test('Buchstaben-Bau ergibt genau die Antwort', () => {
  let n = 0;
  for (const p of PACKS) for (const c of p.cards) {
    const t = P.bauteile(c, p.lang); if (!t) continue; n++;
    assert.equal(t.vor + t.wort, c.a.trim(), p.id + '/' + c.id);
    assert.ok(t.wort.length <= 10);
  }
  assert.ok(n >= 150, 'genug Karten zum Bauen: ' + n);
});

test('Wiederholung: richtig → längerer Abstand, falsch → morgen wieder; fällige Karten zuerst', () => {
  const store = {}; const p = PACKS[0]; const c = p.cards[0]; const day = 1000;
  assert.equal(P.note(store, p, c, true, day).d, day + 1);
  assert.equal(P.note(store, p, c, true, day + 1).d, day + 1 + 2);
  assert.equal(P.note(store, p, c, true, day + 3).d, day + 3 + 4);
  assert.equal(P.note(store, p, c, false, day + 7).b, 1);
  assert.equal(store[P.key(p, c)].d, day + 8);
  const pick = P.pick(store, p, 8, 5, day + 8);
  assert.equal(pick[0], c, 'fällige Karte kommt zuerst');
  assert.equal(pick.length, 6, '1 fällige + höchstens 5 neue');
});

test('Unsichere Aufnahmen sind markiert und bleiben die Ausnahme (unter 15 %)', () => {
  const idx = JSON.parse(readFileSync(new URL('audio/index.json', dir), 'utf8')).saetze;
  const all = Object.values(idx); const uns = all.filter((e) => e.unsicher);
  assert.ok(uns.length / all.length < 0.15, `${uns.length} von ${all.length} unsicher`);
  assert.match(readFileSync(new URL('audio.js', dir), 'utf8'), /!e\.unsicher/);
});

test('„Warum?“: Fehlerstelle wird genau markiert, Regel passt zur Art des Fehlers', () => {
  assert.deepEqual(P.diff('le pére', 'le père'), { vor: 'le p', fehler: 'é', nach: 're', fehlt: false });
  assert.equal(P.diff('Bine', 'Biene').fehlt, true);
  assert.match(P.regel({ a: 'Vogel' }, 'de', 'vogel'), /groß/);
  assert.match(P.regel({ a: 'le père' }, 'fr', 'le pére'), /Akzent/);
  assert.match(P.regel({ a: 'le père' }, 'fr', 'la père'), /Artikel/);
  assert.match(P.regel({ a: 'Heute regnet es.' }, 'de', 'Heute regnet es'), /Satzzeichen/);
  assert.equal(P.regel({ a: 'Biene', info: 'Langes i schreibt man meistens ie.' }, 'de', 'Bine'), 'Langes i schreibt man meistens ie.');
  // Für jede falsche Auswahl jeder Karte gibt es eine Regel und eine Markierung
  for (const p of PACKS) for (const c of p.cards) for (const w of P.wrongs(c, p, 3)) {
    assert.ok(P.regel(c, p.lang, w).length > 3);
    const d = P.diff(w, c.a); assert.equal(d.vor + d.fehler + d.nach, w);
  }
});

test('Generalprobe: Antworten werden fair geprüft (Alternativen ja, Akzent und Großschreibung als „Fast!“)', () => {
  assert.deepEqual(P.akzeptiert('thank you (thanks)'), ['thank you', 'thanks']);
  assert.equal(P.pruefeTipp('  Thanks ', { a: 'thank you (thanks)' }, 'en').ok, true);
  const fr = P.pruefeTipp('le pere', { a: 'le père' }, 'fr');
  assert.equal(fr.ok, false); assert.equal(fr.fast, true); assert.match(fr.hinweis, /Akzent/);
  const de = P.pruefeTipp('vogel', { a: 'Vogel' }, 'de');
  assert.equal(de.ok, false); assert.match(de.hinweis, /Groß/);
  assert.equal(P.pruefeTipp('Hund', { a: 'Vogel' }, 'de').fast, undefined);
  // Jede Antwort jedes Pakets wird als richtig erkannt, wenn man sie genau abtippt
  for (const p of PACKS) for (const c of p.cards) for (const a of P.akzeptiert(c.a)) assert.equal(P.pruefeTipp(a, c, p.lang).ok, true, `${p.id}: ${a}`);
});

test('Lernplan: jeden Tag ein Paket, am letzten Tag vor dem Test die Generalprobe', () => {
  const plan = P.lernplan(['a', 'b'], 100, 105);
  assert.equal(plan.length, 5);
  assert.deepEqual(plan.map((t) => t.paket), ['a', 'b', 'a', 'b', null]);
  assert.equal(plan[4].probe, true);
  assert.deepEqual(P.lernplan(['a'], 100, 101), [{ tag: 100, paket: 'a', probe: false }]);
  assert.deepEqual(P.lernplan(['a'], 100, 100), []);
});
