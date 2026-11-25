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
globalThis.UK_AKTIV = true;   // Ukrainisch im Test mitprüfen (in der App bis zur Freigabe AUS)
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
    for (const f of [e.n, e.s]) assert.ok(existsSync(new URL('audio/' + f, dir)), f);
  }
  assert.deepEqual(fehlt, [], 'Bitte tools/server-audio-einbauen.py laufen lassen');
});

test('Keine Verbindung zu OpenAI/Stimm-Server im Code (Marcel 20.11.2026)', () => {
  for (const f of ['app.js', 'audio.js', 'offline.js', 'sw.js', 'version.js', 'pakete.js', 'content.js', 'index.html']) {
    const t = readFileSync(new URL(f, dir), 'utf8');
    assert.doesNotMatch(t, /openai|stimmen\.mathenachhilfe|NOVA_FREIGEGEBEN|novaUrl|\/jetzt/i, f);
  }
  assert.ok(!existsSync(new URL('vorlesen/texte.json', dir)), 'alte Nova-Liste entfernt');
});

test('Eltern-Tor hat keinen Vorlese-Knopf (keine Gerätestimme, Marcel 21.11.2026)', () => {
  const app = readFileSync(new URL('app.js', dir), 'utf8');
  assert.doesNotMatch(app, /data-vorlesen|geraetSagen/);
});

test('Offline-Download je Sprache: Größen und Dateien stimmen', () => {
  const idx = JSON.parse(readFileSync(new URL('audio/index.json', dir), 'utf8'));
  for (const l of ['en', 'fr', 'es']) assert.ok(idx.groessen[l] > 1e6, 'Größe ' + l);
  const sw = readFileSync(new URL('sw.js', dir), 'utf8');
  assert.match(sw, /OFFLINE/); assert.match(sw, /k !== OFFLINE/); assert.match(sw, /offline\.js/);
  assert.match(readFileSync(new URL('index.html', dir), 'utf8'), /offline\.js/);
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

test('Buchstaben-Bau nur, wenn das Wort eindeutig aus der Aufgabe folgt (keine Auswahlfragen, keine Reime)', () => {
  for (const p of PACKS) for (const c of p.cards) {
    if (!P.modes(c, p.lang).includes('bauen')) continue;
    const k = P.classify(c, p.lang); const b = P.bauFrage(c, p.lang);
    assert.ok(b, `${p.id}: „${c.q}“ hat Bauen, aber keine Bau-Aufgabe`);
    assert.doesNotMatch(b.text, /^(Welche[rs]? |Was reimt)/, `${p.id}: „${b.text}“ ist eine Auswahlfrage`);
    if (/richtig geschrieben|Wie schreibt man/.test(c.q)) assert.ok(b.hoer, `${p.id}: „${c.q}“ braucht das vorgesprochene Wort`);
    if (k.kind === 'diktat') assert.ok(k.vorher);
  }
});

const LP = P.LERNPFAD.map((l) => JSON.parse(readFileSync(new URL('lernpfad/' + l + '.json', dir), 'utf8')));
test('Lernpfad: jede Einheit vollständig, Bedeutungen eindeutig, Figuren vorhanden, Lektionen spielbar', () => {
  for (const lp of LP) for (const a of lp.abschnitte) for (const e of a.einheiten) {
    const wo = `${lp.sprache} ${e.id}`;
    assert.ok(e.woerter.length >= 6 && e.saetze.length >= 3 && e.regel.beispiele.length >= 2 && e.geschichte.zeilen.length >= 3, wo + ' unvollständig');
    for (const w of e.woerter) assert.ok(w[0] && w[1] && w[2], wo + ' Wort ohne Bild/Übersetzung: ' + w[0]);
    const de = e.woerter.map((w) => w[1].toLowerCase()); assert.equal(new Set(de).size, de.length, wo + ': zwei Wörter mit gleicher Bedeutung');
    const zi = e.woerter.map((w) => w[0].toLowerCase()); assert.equal(new Set(zi).size, zi.length, wo + ': Wort doppelt');
    for (const [f] of e.geschichte.zeilen) assert.ok(lp.figuren[f], wo + ': unbekannte Figur ' + f);
    for (const q of e.geschichte.fragen) assert.ok(!q.choices.includes(q.a) && q.choices.length >= 2, wo + ': Frage ' + q.q);
    for (const x of e.aktionen || []) {
      assert.ok(x.sag && x.de && x.ding && lp.figuren[x.an] && x.anzahl >= 1 && x.anzahl <= 5, wo + ': Aktion ' + JSON.stringify(x));
    }
    for (const f of e.fehler || []) {
      const t = P.fehlerTeile(f); assert.equal(t.filter((x) => x.falsch).length, 1, wo + ': genau ein falsches Wort in „' + f.falsch + '“');
      const idx = t.findIndex((x) => x.falsch); const r = t.map((x, i) => (i === idx ? x.t.replace(f.wort, f.korrektur) : x.t)).join(' ');
      assert.equal(r, f.richtig, wo + ': Korrektur ergibt nicht den richtigen Satz');
      assert.ok(!f.choices.includes(f.korrektur) && f.korrektur !== f.wort, wo + ': Auswahl bei „' + f.falsch + '“');
    }
    const pk = P.lernPack(e, lp.sprache);
    for (const l of P.lektionenFuer(lp.sprache)) {
      const st = P.lektion(pk, l.nr, () => 0.42);
      assert.ok(st.length >= 4, wo + ' Lektion ' + l.nr + ' zu kurz');
      for (const s of st) if (s.card) assert.ok(s.card.a, wo + ' leere Karte');
    }
    for (const c of pk.cards) for (const w of P.wrongs(c, pk, 3)) assert.notEqual(w, c.a, wo + ': falsche Antwort = richtige');
  }
});

test('Service Worker sucht nur im eigenen Speicher (gemeinsame Adresse lernen.…)', () => {
  const sw = readFileSync(new URL('sw.js', dir), 'utf8');
  assert.doesNotMatch(sw, /caches\.match\(/, 'caches.match findet auch Dateien anderer Apps');
  assert.match(sw, /caches\.open\(CACHE\)\.then\(\(c\) => c\.match/);
});

test('Abwechslung: Runden-Spiele ziehen ohne Wiederholung (nie dieselben 2 Aufgaben abwechselnd)', () => {
  for (let s = 1; s <= 200; s++) {
    let x = s; const rnd = () => { x = (x * 16807) % 2147483647; return x / 2147483647; };
    const r = P.ziehen(['a', 'b', 'c', 'd', 'e'], 5, rnd); assert.equal(new Set(r).size, 5);
    const r2 = P.ziehen(['a', 'b', 'c'], 7, rnd);
    for (let i = 1; i < r2.length; i++) assert.notEqual(r2[i], r2[i - 1], 'gleiche Aufgabe direkt nacheinander');
  }
  for (const e of playable) for (const g of e.games) {
    const pool = g.items || g.targets; if (!pool) continue;
    const runden = g.rounds || 5;
    const r = P.ziehen(pool, runden);
    assert.equal(new Set(r).size, Math.min(pool.length, runden), `${e.id}/${g.title}: ${runden} Runden aus ${pool.length} Aufgaben`);
    assert.ok(pool.length >= Math.min(runden, 5), `${e.id}/${g.title}: Pool zu klein (${pool.length})`);
  }
  for (const e of playable) for (const g of e.games) {
    const pool = g.lines || g.sentences; if (!pool) continue;
    assert.ok(pool.length >= 5, `${e.id}/${g.title}: nur ${pool.length} Sätze – mindestens 5 verschiedene je Runde`);
  }
});

test('Abwechslung: Lektionen im Lernpfad und Paket-Runden wiederholen keine Aufgabe direkt nacheinander', () => {
  for (const l of P.LERNPFAD) {
    const lp = JSON.parse(readFileSync(new URL('lernpfad/' + l + '.json', dir), 'utf8'));
    for (const a of lp.abschnitte) for (const e of a.einheiten) {
      const pk = P.lernPack(e, l);
      for (const le of P.lektionenFuer(l)) for (let s = 1; s <= 5; s++) {
        let x = s * 7919; const rnd = () => { x = (x * 16807) % 2147483647; return x / 2147483647; };
        const st = P.lektion(pk, le.nr, rnd);
        const ids = st.map((q) => q.card ? q.card.id : (q.fehler ? q.fehler.falsch : (q.aktion ? q.aktion.sag : (q.text || q.mode))));
        for (let i = 1; i < ids.length; i++) assert.notEqual(ids[i], ids[i - 1], `${e.id} Lektion ${le.nr}: „${ids[i]}“ doppelt hintereinander`);
        const karten = st.filter((q) => q.card).map((q) => q.card.id);
        assert.ok(new Set(karten).size >= Math.min(karten.length, 4), `${e.id} Lektion ${le.nr}: zu wenig verschiedene Aufgaben`);
      }
    }
  }
});

test('Ehrliche Rückmeldung: keine Sterne und kein „klang super“ fürs bloße Aufnehmen', () => {
  const app = readFileSync(new URL('app.js', dir), 'utf8');
  assert.doesNotMatch(app, /klang super/i);
  assert.doesNotMatch(app, /Super gesprochen/);
  assert.doesNotMatch(app, /done\([123],/, 'Papagei-Echo vergibt Sterne ohne Messung');
});

test('Startanimation: Splash inline im HTML (kein Netzabruf), Manifest-Hintergrund #05070f, reduced-motion', () => {
  const html = readFileSync(new URL('index.html', dir), 'utf8');
  assert.match(html, /<div id="splash"[^>]*aria-hidden="true"><svg/);
  assert.match(html, /#05070f/); assert.match(html, /prefers-reduced-motion/); assert.match(html, /setTimeout\(ende, ?4000\)|setTimeout\(ende,4000\)/);
  const splash = html.slice(html.indexOf('id="splash"'), html.indexOf('</div>', html.indexOf('id="splash"')));
  assert.doesNotMatch(splash.replace('xmlns="http://www.w3.org/2000/svg"', ''), /https?:|href=|src=|url\(/, 'Splash ohne Netzabruf (xmlns ist nur ein Name, kein Abruf)');
  assert.equal(JSON.parse(readFileSync(new URL('manifest.webmanifest', dir), 'utf8')).background_color, '#05070f');
  assert.match(readFileSync(new URL('app.js', dir), 'utf8'), /splashEnde/);
});
