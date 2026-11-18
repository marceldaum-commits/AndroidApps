// Inhalts- und Logiktests: node --test tools/test-inhalt.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const dir = new URL('../webapp/', import.meta.url);
const C = require(new URL('content.js', dir).pathname);
globalThis.window = globalThis; // audio.js erwartet window
const A = require(new URL('audio.js', dir).pathname);

const playable = C.worlds.flatMap((w) => (w.episodes || []).filter((e) => !e.soon));

test('es gibt spielbare Episoden mit Szene, drei Spielen, Mission und Erklärung', () => {
  assert.ok(playable.length >= 3);
  for (const e of playable) {
    assert.ok(e.scene.length >= 4, e.id);
    assert.equal(e.games.length, 3, e.id);
    assert.ok(e.mission.steps.length >= 2, e.id);
    assert.ok(e.explain.points.length >= 3, e.id);
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
