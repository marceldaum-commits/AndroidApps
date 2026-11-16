'use strict';
/* =====================================================================
   Rechenblitz – Rechen-Engine
   Zufall (auch reproduzierbar), Zahlen-Darstellung, Themen und
   Aufgaben-Generatoren. Jede Aufgabe bringt ihren Lösungsweg (w) mit.
   ===================================================================== */

/* ---------- Zufall ---------- */
let RNG = Math.random;
const ENGINE = { weakFact: null, seeded: false };
const rand = () => RNG();
const rnd = (a, b) => a + Math.floor(rand() * (b - a + 1));
const pick = arr => arr[Math.floor(rand() * arr.length)];
const coin = () => rand() < .5;
const sign = () => coin() ? 1 : -1;
const shuffle = arr => { for (let i = arr.length - 1; i > 0; i--) { const j = Math.floor(rand() * (i + 1)); [arr[i], arr[j]] = [arr[j], arr[i]]; } return arr; };
function seeded(seed) {   // mulberry32
  let a = seed >>> 0;
  return () => { a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
}
function withSeed(seed, fn) { const old = RNG, os = ENGINE.seeded; RNG = seeded(seed); ENGINE.seeded = true; try { return fn(); } finally { RNG = old; ENGINE.seeded = os; } }
const hashStr = s => { let h = 2166136261; for (const c of s) { h ^= c.charCodeAt(0); h = Math.imul(h, 16777619); } return h >>> 0; };

/* ---------- Zahlen ---------- */
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const gcd = (a, b) => { a = Math.abs(a); b = Math.abs(b); while (b) [a, b] = [b, a % b]; return a || 1; };
const lcm = (a, b) => a / gcd(a, b) * b;
const Q = (n, d = 1) => { if (d < 0) { n = -n; d = -d; } const g = gcd(n, d); return { n: n / g, d: d / g }; };
const fi = v => (v < 0 ? '−' : '') + (Math.abs(v) >= 10000 ? Math.abs(v).toLocaleString('de-DE') : Math.abs(v));
const par = v => v < 0 ? `(${fi(v)})` : fi(v);
function fmtDec(i, k) {
  if (!k) return fi(i);
  const s = String(Math.abs(i)).padStart(k + 1, '0'), frac = s.slice(-k).replace(/0+$/, '');
  return (i < 0 ? '−' : '') + s.slice(0, -k) + (frac ? ',' + frac : '');
}
const fmtMoney = c => `${Math.floor(c / 100)},${String(c % 100).padStart(2, '0')}`;
function decOfQ(q) { for (let k = 0; k <= 8; k++) { const p = 10 ** k; if (p % q.d === 0) return fmtDec(q.n * (p / q.d), k); } return `${q.n}/${q.d}`; }
const fq = (n, d) => { const q = Q(n, d); return q.d === 1 ? fi(q.n) : `${fi(q.n)}/${q.d}`; };
const fr = (n, d) => `${fi(n)}/${d}`;                       // Bruch ungekürzt anzeigen
// Lösung → so, wie man sie eintippt
function canon(a) {
  const { n, d } = a.q;
  let s = a.kind === 'dec' ? decOfQ(a.q) : d === 1 ? String(n) : `${n}/${d}`;
  if (a.money && s.includes(',')) s = s.replace(/,(\d)$/, ',$10');   // 38,7 → 38,70
  return s.replace('−', '-').replace(/\./g, '');
}
const showAns = a => canon(a).replace('-', '−');
function parseInput(s) {
  const neg = s.startsWith('-'), b = neg ? s.slice(1) : s, sg = neg ? -1 : 1;
  if (!b) return null;
  if (b.includes('/')) {
    const [x, y] = b.split('/');
    if (!/^\d+$/.test(x) || !/^\d+$/.test(y) || +y === 0) return null;
    return { q: Q(sg * +x, +y), frac: true, reduced: gcd(+x, +y) === 1 && +y !== 1 };
  }
  if (!/^\d*,?\d*$/.test(b) || b === ',') return null;
  const [ip, fp = ''] = b.split(',');
  return { q: Q(sg * parseInt((ip || '0') + fp, 10), 10 ** fp.length) };
}

/* ---------- Bausteine einer Aufgabe ---------- */
const N = (v, paren) => ({ t: 'n', v, paren });       // ganze Zahl
const D = (i, k) => ({ t: 'd', i, k });               // Dezimalzahl i/10^k
const M = c => ({ t: 'm', c });                       // Geldbetrag in Cent
const F = (n, d) => ({ t: 'f', n, d });               // Bruch
const O = v => ({ t: 'o', v });                       // Rechenzeichen
const T = v => ({ t: 'x', v });                       // kleiner Text
const U = v => ({ t: 'u', v });                       // Einheit
const S_ = v => ({ t: 's', v });                      // normaler Text
const P = (b, e) => ({ t: 'p', b, e });               // Potenz
const W = v => ({ t: 'r', v });                       // Wurzel
const Gap = () => ({ t: 'g' });                       // Lücke
const K = v => ({ t: 'k', v });                       // Klammer
const V = c => ({ t: 'v', c });                       // c·x
const Eq = (v = '=') => ({ t: 'q', v });              // = oder ≈
const Story = v => ({ t: 't', v });                   // Textaufgabe
const I = v => ({ q: Q(v), kind: 'int' });
const Fr = (n, d) => ({ q: Q(n, d), kind: 'frac' });
const De = (n, d) => ({ q: Q(n, d), kind: 'dec' });
const Mo = c => ({ q: Q(c, 100), kind: 'dec', money: true });   // Geldbetrag: Lösung mit 2 Nachkommastellen
const SUPS = '⁰¹²³⁴⁵⁶⁷⁸⁹';
const coefTxt = c => c === 1 ? '' : c === -1 ? '−' : fi(c);

function tokHtml(t) {
  switch (t.t) {
    case 'n': return `<span>${t.paren ? par(t.v) : fi(t.v)}</span>`;
    case 'd': return `<span>${fmtDec(t.i, t.k)}</span>`;
    case 'm': return `<span>${fmtMoney(t.c)}</span>`;
    case 'f': return `<span class="frac"><span>${t.n}</span><span>${t.d}</span></span>`;
    case 'o': return `<span class="o">${t.v}</span>`;
    case 'x': return `<span class="tx">${esc(t.v)}</span>`;
    case 'u': return `<span class="ux">${esc(t.v)}</span>`;
    case 's': return `<span>${esc(t.v)}</span>`;
    case 'p': return `<span>${par(t.b)}<sup>${t.e}</sup></span>`;
    case 'r': return `<span class="root">√<span class="rad">${t.v}</span></span>`;
    case 'g': return '<span class="gapbox">?</span>';
    case 'k': return `<span class="br">${t.v}</span>`;
    case 'v': return `<span>${coefTxt(t.c)}<i class="var">x</i></span>`;
    case 'q': return `<span class="eq">${t.v}</span>`;
    case 't': return `<span class="story">${esc(t.v)}</span>`;
  }
}
function tokText(t) {
  switch (t.t) {
    case 'n': return t.paren ? par(t.v) : fi(t.v);
    case 'd': return fmtDec(t.i, t.k);
    case 'm': return fmtMoney(t.c);
    case 'f': return `${t.n}/${t.d}`;
    case 'p': return par(t.b) + String(t.e).split('').map(c => SUPS[c]).join('');
    case 'r': return `√${t.v}`;
    case 'g': return '?';
    case 'v': return coefTxt(t.c) + 'x';
    default: return t.v;
  }
}
const FRAC_NAMES = { 2: 'Halbe', 3: 'Drittel', 4: 'Viertel', 5: 'Fünftel', 6: 'Sechstel', 7: 'Siebtel', 8: 'Achtel', 9: 'Neuntel', 10: 'Zehntel', 11: 'Elftel', 12: 'Zwölftel', 20: 'Zwanzigstel', 25: 'Fünfundzwanzigstel', 100: 'Hundertstel' };
function tokSpeech(t) {
  switch (t.t) {
    case 'n': return (t.v < 0 ? 'minus ' : '') + Math.abs(t.v);
    case 'd': return fmtDec(t.i, t.k).replace('−', 'minus ');
    case 'm': return `${Math.floor(t.c / 100)} Euro ${t.c % 100 || ''}`;
    case 'f': return t.n === 1 && t.d === 2 ? 'ein Halb' : FRAC_NAMES[t.d] ? `${t.n} ${FRAC_NAMES[t.d]}` : `${t.n} durch ${t.d}`;
    case 'o': return { '+': 'plus', '−': 'minus', '·': 'mal', ':': 'geteilt durch' }[t.v];
    case 'x': case 'u': case 's': return t.v.replace('%', ' Prozent').replace('€', ' Euro').replace('ggT', 'größter gemeinsamer Teiler von').replace('kgV', 'kleinstes gemeinsames Vielfaches von');
    case 'p': return t.e === 2 ? `${tokSpeech(N(t.b))} zum Quadrat` : `${tokSpeech(N(t.b))} hoch ${t.e}`;
    case 'r': return `Wurzel aus ${t.v}`;
    case 'g': return 'wie viel';
    case 'k': return t.v === '(' ? 'Klammer auf' : 'Klammer zu';
    case 'v': return `${t.c === 1 ? '' : t.c} x`;
    case 'q': return t.v === '=' ? 'gleich' : 'ungefähr';
    case 't': return t.v;
  }
}
const taskText = t => t.e.map(tokText).join(' ').replace(/\( /g, '(').replace(/ \)/g, ')');
const taskSpeech = t => t.e.map(tokSpeech).join(' ') + (t.hint ? '. ' + t.hint : '');

/* ---------- Themen ---------- */
const GROUPS = ['Grundrechnen', 'Brüche & Co.', 'Anwenden', 'Gemischt'];
const TOPICS = {
  add: { g: '+', name: 'Plus', grp: 0 }, sub: { g: '−', name: 'Minus', grp: 0 }, mul: { g: '·', name: 'Mal', grp: 0 },
  div: { g: ':', name: 'Geteilt', grp: 0 }, gap: { g: '□', name: 'Lücke', grp: 0 },
  doppel: { g: '×2', name: 'Verdoppeln', grp: 0 }, geld: { g: '€', name: 'Geld', grp: 0 },
  frac: { g: '½', name: 'Brüche', grp: 1 }, dec: { g: '0,5', name: 'Dezimal', grp: 1 }, pct: { g: '%', name: 'Prozent', grp: 1 },
  neg: { g: '−3', name: 'Negativ', grp: 1 }, pow: { g: 'x²', name: 'Potenzen', grp: 1 },
  term: { g: '3x', name: 'Terme', grp: 1 }, linear: { g: 'f(x)', name: 'Funktionen', grp: 1 }, zehner: { g: '10ⁿ', name: 'Zehnerpotenzen', grp: 1 },
  order: { g: '( )', name: 'Klammern', grp: 2 }, round: { g: '≈', name: 'Runden', grp: 2 }, unit: { g: 'cm', name: 'Einheiten', grp: 2 },
  divis: { g: 'ggT', name: 'Teiler', grp: 2 }, eq: { g: 'x=', name: 'Gleichung', grp: 2 },
  zeit: { g: '🕐', name: 'Uhr & Zeit', grp: 2 }, geo: { g: '📐', name: 'Geometrie', grp: 2 }, folge: { g: '🔢', name: 'Zahlenfolgen', grp: 2 },
  dreisatz: { g: '⇄', name: 'Dreisatz', grp: 2 }, zins: { g: '🏦', name: 'Zinsen', grp: 2 }, wahr: { g: '🎲', name: 'Wahrscheinlichkeit', grp: 2 }, stat: { g: 'Ø', name: 'Statistik', grp: 2 }, kreis: { g: 'π', name: 'Kreis', grp: 2 },
  word: { g: '📖', name: 'Textaufgaben', grp: 3, wide: true }, mix: { g: '★', name: 'Alles gemischt', grp: 3, wide: true },
};
const LEVEL_DESC = {
  add: ['Zahlenraum 10', 'Zahlenraum 20 mit Zehnerübergang', 'Bis 100: zweistellig + einstellig', 'Bis 100: zweistellig + zweistellig', 'Zahlenraum 1000'],
  sub: ['Zahlenraum 10', 'Zahlenraum 20 mit Zehnerübergang', 'Bis 100: zweistellig − einstellig', 'Bis 100: zweistellig − zweistellig', 'Zahlenraum 1000'],
  mul: ['Kernaufgaben (·1, ·2, ·5, ·10)', 'Kleines Einmaleins', 'Großes Einmaleins (11–19)', 'Zweistellig · einstellig', 'Zweistellig · zweistellig'],
  div: ['Kernaufgaben (:1, :2, :5, :10)', 'Kleines Einmaleins rückwärts', 'Großes Einmaleins rückwärts', 'Bis 900 : einstellig', 'Bis 625 : zweistellig'],
  gap: ['Plus-Lücken bis 20', 'Plus- und Minus-Lücken bis 100', 'Mal-Lücken im Einmaleins', 'Geteilt-Lücken im Einmaleins', 'Lücken bis 1000 und zweistellig mal'],
  frac: ['Gleichnamige Brüche addieren & subtrahieren', 'Kürzen und Bruchteile von Zahlen', 'Ungleichnamige Brüche addieren & subtrahieren', 'Brüche multiplizieren & dividieren', 'Brüche und ganze Zahlen gemischt'],
  dec: ['Eine Nachkommastelle: plus & minus', 'Zwei Nachkommastellen: plus & minus', 'Mal & geteilt durch 10, 100, 1000', 'Dezimalzahlen multiplizieren', 'Dezimalzahlen dividieren & Brüche umwandeln'],
  pct: ['50 %, 25 % und 10 % von …', 'Weitere Prozentsätze von …', 'Prozentsatz gesucht', 'Grundwert gesucht', 'Preise erhöhen & reduzieren'],
  neg: ['Plus mit negativen Zahlen bis 10', 'Minus ins Negative', 'Plus & minus mit Klammern bis 20', 'Mal & geteilt mit Vorzeichen', 'Drei Zahlen & größere Zahlen'],
  pow: ['Quadratzahlen bis 10²', 'Quadratzahlen & Wurzeln bis 12', 'Quadratzahlen & Wurzeln bis 20', 'Zweier-, Zehner- & Kubikpotenzen', 'Potenzen & Wurzeln kombinieren'],
  order: ['Punkt vor Strich', 'Klammern zuerst', 'Punkt vor Strich mit Geteilt', 'Klammern und Punkt vor Strich', 'Lange Rechenketten'],
  round: ['Auf Zehner runden', 'Auf Hunderter runden', 'Auf Tausender oder Zehntel runden', 'Überschlag: plus & minus', 'Überschlag: mal'],
  unit: ['m, cm, kg, g, € und ct', 'km, Liter, Stunden, Minuten', 'Mit Kommazahlen umrechnen', 'Flächen und Rauminhalte', 'Gemischte Angaben (1 h 15 min …)'],
  divis: ['Division mit Rest', 'Quersumme', 'Größter gemeinsamer Teiler', 'Kleinstes gemeinsames Vielfaches', 'Primfaktoren & Anzahl der Teiler'],
  eq: ['x + a = b', 'a · x = b und x : a = b', 'Zwei Schritte: a · x + b = c', 'Mit negativen Zahlen & Klammern', 'x auf beiden Seiten'],
  doppel: ['Verdoppeln & halbieren bis 20', 'Verdoppeln & halbieren bis 100', 'Verdoppeln & halbieren bis 1000', 'Halbieren mit Komma', 'Vierfach, Viertel, dreimal verdoppeln'],
  geld: ['Cent plus & minus', 'Euro und Cent umrechnen, Rückgeld', 'Euro-Beträge plus & minus', 'Rückgeld und Mal mit Komma', 'Preise vergleichen (Dreisatz)'],
  zeit: ['Viertelstunden, Stunden, Wochen', 'Minuten zwischen zwei Uhrzeiten', 'Über die volle Stunde, Filmende', 'Kalender, Tage und Sekunden', 'Kommastunden, Tempo und Laufzeiten'],
  geo: ['Umfang von Quadrat und Rechteck', 'Fläche von Quadrat und Rechteck', 'Winkel im Dreieck, Seite aus Fläche', 'Dreieck, Quader, Viereck, Würfel', 'Pythagoras, Vielecke, Trapez, Volumen'],
  folge: ['Plus und minus in kleinen Schritten', 'Größere Schritte und Verdoppeln', 'Abwechselnd, wachsende Abstände, mal 3', 'Quadratzahlen, Halbieren, Fibonacci, Lücke', 'Negative Zahlen und knifflige Regeln'],
  term: ['Termwert berechnen', 'Gleichartige Glieder zusammenfassen', 'Klammern ausmultiplizieren', 'Termwert mit x² und negativen Zahlen', 'Binomische Formeln'],
  linear: ['Funktionswert berechnen', 'Funktionswert & y-Achsenabschnitt', 'Steigung aus zwei Punkten', 'Nullstelle', 'Schnittpunkt zweier Geraden'],
  zehner: ['Zehnerpotenzen ausrechnen', 'Zahl mal Zehnerpotenz', 'Kommazahl mal Zehnerpotenz', 'Große Zahlen: Exponent gesucht', 'Kleine Zahlen: negativer Exponent'],
  dreisatz: ['Preise (proportional)', 'Verbrauch und Strecke', 'Antiproportional: Pumpen', 'Preise mit Komma', 'Antiproportional: Vorräte'],
  zins: ['Jahreszinsen', 'Zinssatz gesucht', 'Kapital gesucht', 'Zinsen für Monate', 'Zinseszins'],
  wahr: ['Würfel und Münze', 'Kugeln ziehen', 'Gegenereignis, Prozent, Erwartung', 'Zweistufig mit Zurücklegen', 'Ohne Zurücklegen, mindestens einmal'],
  stat: ['Mittelwert', 'Median (ungerade Anzahl)', 'Spannweite', 'Median (gerade Anzahl)', 'Fehlender Wert für einen Durchschnitt'],
  kreis: ['Umfang aus dem Durchmesser', 'Umfang aus dem Radius', 'Kreisfläche', 'Kreisfläche aus dem Durchmesser', 'Volumen eines Zylinders'],
  word: ['Klasse 1–2: plus & minus', 'Klasse 3–4: mal, geteilt, Geld', 'Klasse 5–6: Komma, Brüche, Flächen', 'Klasse 7–8: Prozent, Dreisatz, Tempo', 'Mehrere Schritte'],
  mix: [1, 2, 3, 4, 5].map(n => `Alle Themen zufällig gemischt – Stufe ${n}`),
};
// Welche Themen (und Stufen) zu welcher Klassenstufe passen
const CLASS_TOPICS = {
  1: [['add', 0], ['sub', 0], ['gap', 0], ['doppel', 0], ['geld', 0], ['folge', 0]],
  2: [['add', 1], ['sub', 1], ['mul', 0], ['div', 0], ['gap', 0], ['word', 0], ['doppel', 1], ['geld', 1], ['zeit', 0], ['folge', 0]],
  3: [['add', 2], ['sub', 2], ['mul', 1], ['div', 1], ['gap', 1], ['order', 0], ['unit', 0], ['word', 0], ['doppel', 2], ['geld', 1], ['zeit', 1], ['geo', 0], ['folge', 1]],
  4: [['add', 3], ['sub', 3], ['mul', 2], ['div', 2], ['gap', 2], ['round', 0], ['unit', 1], ['order', 1], ['word', 1], ['doppel', 2], ['geld', 2], ['zeit', 2], ['geo', 1], ['folge', 1]],
  5: [['add', 4], ['sub', 4], ['mul', 3], ['div', 3], ['gap', 3], ['round', 1], ['frac', 0], ['dec', 0], ['divis', 0], ['order', 2], ['word', 1], ['doppel', 3], ['geld', 3], ['zeit', 3], ['geo', 2], ['folge', 2], ['stat', 0], ['dreisatz', 0]],
  6: [['mul', 4], ['frac', 1], ['frac', 2], ['dec', 1], ['dec', 2], ['divis', 2], ['unit', 2], ['round', 2], ['neg', 0], ['word', 2], ['doppel', 4], ['geld', 4], ['zeit', 4], ['geo', 3], ['folge', 3], ['stat', 1], ['dreisatz', 1], ['wahr', 0]],
  7: [['frac', 3], ['dec', 3], ['pct', 0], ['pct', 1], ['neg', 2], ['eq', 0], ['order', 3], ['unit', 3], ['divis', 3], ['word', 2], ['geo', 3], ['folge', 3], ['stat', 2], ['dreisatz', 2], ['dreisatz', 3], ['wahr', 1], ['term', 0], ['term', 1], ['zins', 0]],
  8: [['frac', 4], ['dec', 4], ['pct', 2], ['neg', 3], ['eq', 1], ['eq', 2], ['pow', 0], ['round', 3], ['order', 4], ['word', 3], ['geo', 4], ['folge', 4], ['stat', 3], ['dreisatz', 4], ['wahr', 2], ['term', 2], ['term', 3], ['zins', 1], ['zins', 2], ['linear', 0], ['linear', 1], ['kreis', 0], ['kreis', 1]],
  9: [['pct', 3], ['pow', 1], ['pow', 2], ['eq', 3], ['neg', 4], ['unit', 4], ['round', 4], ['word', 3], ['geo', 4], ['folge', 4], ['stat', 4], ['wahr', 3], ['term', 4], ['zins', 3], ['linear', 2], ['linear', 3], ['kreis', 2], ['kreis', 3], ['zehner', 0], ['zehner', 1], ['zehner', 2]],
  10: [['pct', 4], ['pow', 3], ['pow', 4], ['eq', 4], ['divis', 4], ['word', 4], ['folge', 4], ['wahr', 4], ['zins', 4], ['linear', 4], ['kreis', 4], ['zehner', 3], ['zehner', 4]],
};
function classLevel(k, topic) { // empfohlene Stufe einer Klassenstufe (höchste genannte)
  for (let c = k; c >= 1; c--) { const hits = CLASS_TOPICS[c].filter(([t]) => t === topic); if (hits.length) return Math.max(...hits.map(h => h[1])); }
  return null;
}
function classTopicSet(k) { const s = new Set(['mix']); for (const c of [k - 1, k]) (CLASS_TOPICS[c] || []).forEach(([t]) => s.add(t)); return s; }

/* ---------- Lösungswege ---------- */
const places = n => { const out = []; let m = 1; while (m * 10 <= n) m *= 10; for (; m >= 1; m /= 10) { const d = Math.floor(n / m) % 10; if (d) out.push(d * m); } return out; };
function addW(a, b) {
  const s = a + b;
  if (b < 10) {
    const r = 10 - a % 10;
    if (a % 10 && b > r) return [`Zerlege ${b} in ${r} + ${b - r}, um zum Zehner zu springen:`, `${a} + ${r} = ${a + r}`, `${a + r} + ${b - r} = ${s}`];
    return [`${a} + ${b} = ${s}`, `Probe: ${s} − ${b} = ${a}`];
  }
  const parts = places(b); if (parts.length === 1) return [`${a} + ${b} = ${s}`, `Probe: ${s} − ${b} = ${a}`];
  const out = [`Zerlege ${b} in ${parts.join(' + ')}:`]; let cur = a;
  for (const p of parts) { out.push(`${cur} + ${p} = ${cur + p}`); cur += p; }
  return out;
}
function subW(a, b) {
  const s = a - b;
  if (b < 10) {
    const r = a % 10;
    if (r && b > r) return [`Zerlege ${b} in ${r} + ${b - r}, um zum Zehner zu springen:`, `${a} − ${r} = ${a - r}`, `${a - r} − ${b - r} = ${s}`];
    return [`${a} − ${b} = ${s}`, `Probe: ${s} + ${b} = ${a}`];
  }
  const parts = places(b); if (parts.length === 1) return [`${a} − ${b} = ${s}`, `Probe: ${s} + ${b} = ${a}`];
  const out = [`Zerlege ${b} in ${parts.join(' + ')}:`]; let cur = a;
  for (const p of parts) { out.push(`${cur} − ${p} = ${cur - p}`); cur -= p; }
  return out;
}
function mulW(a, b) {
  const p = a * b, big = Math.max(a, b), sm = Math.min(a, b);
  if (big <= 10) {
    if (sm === 1) return [`Mal 1 ändert nichts: ${p}`];
    if (big === 10) return [`Mal 10: eine Null anhängen → ${p}`];
    if (sm === 2) return [`Mal 2 ist das Doppelte: ${big} + ${big} = ${p}`];
    if (sm === 5 || big === 5) { const x = sm === 5 ? big : sm; return [`5 · ${x} ist die Hälfte von 10 · ${x}:`, `10 · ${x} = ${10 * x}, die Hälfte ist ${p}`]; }
    if (big === 9) return [`9 · ${sm} = 10 · ${sm} − ${sm}`, `= ${10 * sm} − ${sm} = ${p}`];
    return [`Zerlege ${big} in 5 + ${big - 5}:`, `5 · ${sm} = ${5 * sm}`, `${big - 5} · ${sm} = ${(big - 5) * sm}`, `${5 * sm} + ${(big - 5) * sm} = ${p}`];
  }
  if (sm <= 10) {
    const tn = big - big % 10, o = big % 10;
    if (!o) return [`${big / 10} · ${sm} = ${big / 10 * sm}, dann eine Null anhängen → ${p}`];
    return [`Zerlege ${big} in ${tn} + ${o}:`, `${tn} · ${sm} = ${tn * sm}`, `${o} · ${sm} = ${o * sm}`, `${tn * sm} + ${o * sm} = ${p}`];
  }
  const tn = sm - sm % 10, o = sm % 10;
  if (!o) return [`${big} · ${sm / 10} = ${big * sm / 10}, dann eine Null anhängen → ${p}`];
  return [`Zerlege ${sm} in ${tn} + ${o}:`, `${big} · ${tn} = ${big * tn}`, `${big} · ${o} = ${big * o}`, `${big * tn} + ${big * o} = ${p}`];
}
function divW(a, b) {
  const q = a / b;
  if (q <= 10) return [`Welche Zahl mal ${b} ergibt ${a}?`, `${q} · ${b} = ${a}`, `Also ${a} : ${b} = ${q}`];
  const qt = q - q % 10, qo = q % 10;
  if (!qo) return [`${a} : ${b} = ${q}`, `Probe: ${q} · ${b} = ${a}`];
  return [`Zerlege ${a} in ${qt * b} + ${qo * b}:`, `${qt * b} : ${b} = ${qt}`, `${qo * b} : ${b} = ${qo}`, `${qt} + ${qo} = ${q}`];
}
function redW(n, d) {
  const g = gcd(n, d);
  if (d / g === 1) return n === d ? [`${fr(n, d)} = 1`] : [`${fr(n, d)} = ${n / d}`];
  return g > 1 ? [`Kürzen mit ${g}: ${fr(n, d)} = ${fr(n / g, d / g)}`] : [];
}
function fracAddW(n1, d1, n2, d2, op) {
  const L = lcm(d1, d2), a = n1 * L / d1, b = n2 * L / d2, r = op === '+' ? a + b : a - b, out = [`Gemeinsamer Nenner: ${L}`];
  if (d1 !== L) out.push(`${fr(n1, d1)} = ${fr(a, L)}`);
  if (d2 !== L) out.push(`${fr(n2, d2)} = ${fr(b, L)}`);
  out.push(`${fr(a, L)} ${op} ${fr(b, L)} = ${fr(r, L)}`);
  return out.concat(redW(r, L));
}
function pctW(p, m) {
  const r = p * m / 100;
  const known = { 50: [2, 1, 'die Hälfte'], 25: [4, 1, 'ein Viertel'], 10: [10, 1, 'ein Zehntel'], 20: [5, 1, 'ein Fünftel'], 75: [4, 3, 'drei Viertel'], 1: [100, 1, 'ein Hundertstel'] }[p];
  if (known) { const [d, n, name] = known; return n === 1 ? [`${p} % ist ${name}:`, `${fi(m)} : ${d} = ${fi(r)}`] : [`${p} % ist ${name}:`, `${fi(m)} : ${d} = ${fi(m / d)}`, `${fi(m / d)} · ${n} = ${fi(r)}`]; }
  if (m % 10 === 0) {
    if (p % 10 === 0) return [`10 % von ${fi(m)} = ${fi(m / 10)}`, `${p} % = ${p / 10} · ${fi(m / 10)} = ${fi(r)}`];
    if (p === 5 && m % 20 === 0) return [`10 % von ${fi(m)} = ${fi(m / 10)}`, `5 % ist die Hälfte davon: ${fi(r)}`];
    if (p === 15 && m % 20 === 0) return [`10 % = ${fi(m / 10)} und 5 % = ${fi(m / 20)}`, `15 % = ${fi(m / 10)} + ${fi(m / 20)} = ${fi(r)}`];
  }
  return [`${p} % von ${fi(m)} = ${fi(m)} · ${p} : 100`, `= ${fi(m * p)} : 100 = ${fi(r)}`];
}
const sgnTerm = v => v >= 0 ? ` + ${fi(v)}` : ` − ${fi(-v)}`;
const divisors = n => { const out = []; for (let i = 1; i <= n; i++) if (n % i === 0) out.push(i); return out; };

/* ---------- Aufgaben-Generatoren ---------- */
const ADD = [
  () => { const a = rnd(0, 10); return [a, rnd(0, 10 - a)]; },
  () => { const a = rnd(2, 9); return [a, rnd(11 - a, 9)]; },
  () => { const a = rnd(11, 89); return [a, rnd(2, Math.min(9, 100 - a))]; },
  () => { const a = rnd(11, 89); return [a, rnd(11, 100 - a)]; },
  () => { const a = rnd(100, 899); return [a, rnd(11, 999 - a)]; },
];
const MUL = [
  () => [rnd(1, 10), pick([1, 2, 5, 10])],
  () => [rnd(2, 10), rnd(2, 10)],
  () => [rnd(11, 19), rnd(2, 9)],
  () => [rnd(12, 99), rnd(2, 9)],
  () => [rnd(11, 25), rnd(11, 25)],
];
const properFrac = d => { let n; do n = rnd(1, d - 1); while (gcd(n, d) !== 1); return n; };
const noTrail = (lo, hi) => { let v; do v = rnd(lo, hi); while (v % 10 === 0); return v; };
const factKey = (a, b) => a <= 10 && b <= 10 && a >= 1 && b >= 1 ? `${Math.min(a, b)}x${Math.max(a, b)}` : null;
function tablePair(l) {      // im kleinen Einmaleins schwache Aufgaben bevorzugen
  if (l === 1 && !ENGINE.seeded && ENGINE.weakFact && rand() < .4) { const w = ENGINE.weakFact(); if (w) return w; }
  return MUL[l]();
}
const NAMES = ['Lena', 'Tim', 'Mia', 'Ben', 'Emma', 'Noah', 'Sofia', 'Elias', 'Hannah', 'Luca', 'Mila', 'Paul', 'Ella', 'Finn', 'Leonie', 'Jonas', 'Aylin', 'Mats', 'Zoe', 'Can'];

function dblW(x) { const p = places(x); return p.length > 1 ? [`Zerlege ${x} in ${p.join(' + ')}:`, `Doppelt: ${p.map(v => 2 * v).join(' + ')} = ${2 * x}`] : [`${x} + ${x} = ${2 * x}`]; }
function hlfW(x) { const t = Math.floor(x / 10) * 10, e = x - t; return x <= 20 ? [`${x / 2} + ${x / 2} = ${x}`] : t && e && t % 20 === 0 ? [`Zerlege ${x} in ${t} + ${e}:`, `Hälfte: ${t / 2} + ${e / 2} = ${x / 2}`] : t && e ? [`Zerlege ${x} in ${t - 10} + ${e + 10}:`, `Hälfte: ${(t - 10) / 2} + ${(e + 10) / 2} = ${x / 2}`] : [`${x / 2} + ${x / 2} = ${x}`]; }
/* weitere Textaufgaben (16.11.2026) – je Stufe eine Liste, word() wählt sie etwa zur Hälfte */
const WORD_MORE = [
  [
    (n, n2, W_) => { const a = rnd(6, 15), b = rnd(2, a - 1); return W_(`Auf dem Baum sitzen ${a} Vögel. ${b} fliegen weg. Wie viele Vögel sitzen noch auf dem Baum?`, I(a - b), '', [`Wegfliegen heißt minus: ${a} − ${b} = ${a - b}`]); },
    (n, n2, W_) => { const a = rnd(3, 10), b = rnd(2, 9); return W_(`${n} hat ${a} rote und ${b} blaue Perlen. Wie viele Perlen sind das zusammen?`, I(a + b), '', [`Zusammen heißt plus: ${a} + ${b} = ${a + b}`]); },
    (n, n2, W_) => { const a = rnd(6, 14), b = rnd(2, 5); return W_(`Auf einem Teller liegen ${a} Kekse. ${n} isst ${b} davon. Wie viele Kekse liegen noch auf dem Teller?`, I(a - b), '', [`${a} − ${b} = ${a - b}`]); },
    (n, n2, W_) => { const a = rnd(3, 9), b = rnd(2, 9); return W_(`Im Stall stehen ${a} Pferde und ${b} Kühe. Wie viele Tiere sind es zusammen?`, I(a + b), '', [`${a} + ${b} = ${a + b}`]); },
    (n, n2, W_) => { const a = rnd(5, 9), b = rnd(1, 4); return W_(`${n} ist ${a} Jahre alt. ${n2} ist ${b} Jahre älter. Wie alt ist ${n2}?`, I(a + b), 'Jahre', [`Älter heißt plus: ${a} + ${b} = ${a + b}`]); },
    (n, n2, W_) => { const k = rnd(2, 4); return W_(`Ein Hase hat 4 Beine. Wie viele Beine haben ${k} Hasen zusammen?`, I(4 * k), '', [`${Array(k).fill(4).join(' + ')} = ${4 * k}`]); },
  ],
  [
    (n, n2, W_) => { const r = rnd(6, 12); return W_(`Ein Bus hat ${r} Sitzreihen mit je 4 Plätzen. Wie viele Kinder können sitzen?`, I(4 * r), '', [`${r} · 4 = ${4 * r}`]); },
    (n, n2, W_) => { const p = rnd(2, 5), w = rnd(3, 9); return W_(`${n} spart jede Woche ${p} €. Wie viel Geld hat ${n} nach ${w} Wochen?`, I(p * w), '€', [`${w} Wochen mit je ${p} €: ${w} · ${p} = ${w * p}`]); },
    (n, n2, W_) => { const c = pick([6, 8, 10, 12]), k = rnd(3, 9); return W_(`In einer Packung sind ${c} Stifte. Wie viele Stifte sind in ${k} Packungen?`, I(c * k), '', [`${k} · ${c} = ${k * c}`]); },
    (n, n2, W_) => { const c = pick([4, 5, 6]); let t; do t = rnd(13, 40); while (t % c === 0); const b = Math.ceil(t / c); return W_(`${t} Kinder fahren Boot. In ein Boot passen ${c} Kinder. Wie viele Boote braucht man mindestens?`, I(b), '', [`${t} : ${c} = ${Math.floor(t / c)} Rest ${t % c}`, `Die ${t % c} übrigen Kinder brauchen noch ein Boot: ${Math.floor(t / c)} + 1 = ${b}`]); },
    (n, n2, W_) => { const p = rnd(4, 8), k = rnd(2, 5), q = p + rnd(2, 4); return W_(`Ein Kinoticket für Kinder kostet ${p} €, für Erwachsene ${q} €. Was zahlen ${k} Kinder und ein Erwachsener zusammen?`, I(k * p + q), '€', [`Kinder: ${k} · ${p} = ${k * p}`, `${k * p} + ${q} = ${k * p + q}`]); },
    (n, n2, W_) => { const b = rnd(6, 15), c = rnd(2, 5), a = b + c + rnd(3, 20); return W_(`${n} hat ${a} €. Ein Buch kostet ${b} €, ein Stift ${c} €. Wie viel Geld bleibt übrig?`, I(a - b - c), '€', [`Ausgaben: ${b} + ${c} = ${b + c}`, `${a} − ${b + c} = ${a - b - c}`]); },
  ],
  [
    (n, n2, W_) => { const a = rnd(3, 6), b = rnd(2, 5), p = pick([8, 10, 12, 15]); return W_(`Ein Zimmer ist ${a} m lang und ${b} m breit. Ein Quadratmeter Teppich kostet ${p} €. Was kostet der Teppich für das ganze Zimmer?`, I(a * b * p), '€', [`Fläche: ${a} · ${b} = ${a * b} m²`, `${a * b} · ${p} = ${a * b * p} €`]); },
    (n, n2, W_) => { const d = pick([3, 4, 5]), t = d * rnd(4, 20); return W_(`${n} hat ${t} € gespart und gibt 1/${d} davon für ein Spiel aus. Wie viel kostet das Spiel?`, I(t / d), '€', [`1/${d} von ${t} € = ${t} : ${d} = ${t / d} €`]); },
    (n, n2, W_) => { const s = 60 * rnd(7, 15) + 5 * rnd(0, 11), d = 5 * rnd(14, 30), e = s + d, hm = m => `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}`; return W_(`Ein Zug fährt um ${hm(s)} Uhr ab und kommt um ${hm(e)} Uhr an. Wie viele Minuten dauert die Fahrt?`, I(d), 'min', [`Bis ${Math.ceil(s / 60)}:00 Uhr: ${60 - s % 60} min`, `Dann bis ${hm(e)} Uhr: ${e - Math.ceil(s / 60) * 60} min`, `${60 - s % 60} + ${e - Math.ceil(s / 60) * 60} = ${d} min`]); },
    (n, n2, W_) => { const t = 4 * rnd(30, 120); return W_(`An einer Schule sind ${t} Kinder. 3/4 davon kommen zu Fuß. Wie viele Kinder sind das?`, I(t * 3 / 4), '', [`1/4 von ${t} = ${t / 4}`, `3/4 = 3 · ${t / 4} = ${t * 3 / 4}`]); },
    (n, n2, W_) => { const k = pick([2, 3]), a = k * rnd(2, 5), b = k * rnd(2, 4), c = k * rnd(1, 3); return W_(`Holzwürfel haben ${k} cm Kantenlänge. Wie viele passen in eine Kiste, die ${a} cm lang, ${b} cm breit und ${c} cm hoch ist?`, I(a / k * b / k * c / k), '', [`Pro Kante: ${a} : ${k} = ${a / k}, ${b} : ${k} = ${b / k}, ${c} : ${k} = ${c / k}`, `${a / k} · ${b / k} · ${c / k} = ${a / k * b / k * c / k}`]); },
    (n, n2, W_) => { const g = 4 * rnd(25, 100), p = pick([2, 3, 6, 8]); return W_(`Ein Rezept für 4 Personen braucht ${g} g Mehl. Wie viel Mehl braucht man für ${p} Personen?`, I(g / 4 * p), 'g', [`Für 1 Person: ${g} : 4 = ${g / 4} g`, `Für ${p} Personen: ${p} · ${g / 4} = ${g / 4 * p} g`]); },
    (n, n2, W_) => { const m = pick([200, 250, 400]), a = rnd(3, 12); return W_(`${n} läuft ${a} Runden. Eine Runde ist ${m} m lang. Wie viele Meter läuft ${n}?`, I(a * m), 'm', [`${a} · ${m} = ${fi(a * m)}`]); },
    (n, n2, W_) => { const d = pick([8, 10, 12, 16]), e = rnd(2, d - 2); return W_(`Ein Kuchen wird in ${d} gleich große Stücke geteilt. ${n} und ${n2} essen zusammen ${e} Stücke. Welcher Bruchteil des Kuchens wurde gegessen?`, Fr(e, d), '', [`${e} von ${d} Stücken: ${fr(e, d)}`, ...redW(e, d)], 'als gekürzter Bruch'); },
    (n, n2, W_) => { const [f, g, k] = pick([[15, 25, 6], [10, 2, 5], [20, 25, 8], [15, 3, 5], [10, 25, 4], [20, 4, 5]]); return W_(`Eine Flasche enthält ${fmtDec(f, 1)} l Saft. Wie viele Gläser mit ${fmtDec(g, g < 10 ? 1 : 2)} l kann man damit füllen?`, I(k), '', [`${fmtDec(f, 1)} l : ${fmtDec(g, g < 10 ? 1 : 2)} l = ${k}`, `Probe: ${k} · ${fmtDec(g, g < 10 ? 1 : 2)} l = ${fmtDec(f, 1)} l`]); },
    (n, n2, W_) => { const a = rnd(3, 12), b = rnd(2, 8), k = pick([2, 3, 4]); return W_(`Ein Beet ist ${a} m lang und ${b} m breit. Auf jeden Quadratmeter kommen ${k} Blumen. Wie viele Blumen braucht man?`, I(a * b * k), '', [`Fläche: ${a} · ${b} = ${a * b} m²`, `${a * b} · ${k} = ${a * b * k}`]); },
    (n, n2, W_) => { const m = rnd(7, 18), a = m + rnd(-3, 3), b = m + rnd(-3, 3), c = 3 * m - a - b; return W_(`${n} hat in drei Tests ${a}, ${b} und ${c} Punkte. Wie viele Punkte sind das im Durchschnitt?`, I(m), '', ['Durchschnitt = Summe : Anzahl', `${a} + ${b} + ${c} = ${3 * m}`, `${3 * m} : 3 = ${m}`]); },
  ],
  [
    (n, n2, W_) => { const p = 5 * rnd(4, 30), x = p * 4 / 5; return W_(`Ein T-Shirt kostet nach 20 % Rabatt noch ${x} €. Was hat es vorher gekostet?`, I(p), '€', ['Nach 20 % Rabatt bleiben 80 % übrig.', `80 % = ${x} € → 10 % = ${decOfQ(Q(x, 8))} €`, `100 % = ${p} €`]); },
    (n, n2, W_) => { const t = pick([20, 25, 50]), k = rnd(1, t - 1); return W_(`In einer Klasse mit ${t} Kindern haben ${k} ein Haustier. Wie viel Prozent sind das?`, I(k * 100 / t), '%', [`${k} von ${t} = ${fr(k, t)}`, `Erweitern auf 100: ${fr(k * 100 / t, 100)} = ${k * 100 / t} %`]); },
    (n, n2, W_) => { const l = 4 * rnd(1, 6); return W_(`Eine Saftschorle wird aus 3 Teilen Saft und 1 Teil Wasser gemischt. Wie viele Liter Saft braucht man für ${l} Liter Schorle?`, I(l * 3 / 4), 'l', [`Zusammen 3 + 1 = 4 Teile`, `1 Teil: ${l} : 4 = ${l / 4} l`, `Saft: 3 · ${l / 4} = ${l * 3 / 4} l`]); },
    (n, n2, W_) => { const a = 20 * rnd(2, 20), q = pick([10, 20, 25, 50]), b = a * (100 + q) / 100; return W_(`Der Preis steigt von ${a} € auf ${b} €. Um wie viel Prozent ist er gestiegen?`, I(q), '%', [`Erhöhung: ${b} − ${a} = ${b - a} €`, `${b - a} von ${a} = ${fr(b - a, a)} = ${q} %`]); },
    (n, n2, W_) => { const l = 5 * rnd(4, 10), w = rnd(5, 15), d = rnd(1, 3); return W_(`Ein Schwimmbecken ist ${l} m lang, ${w} m breit und ${d} m tief. Wie viele Kubikmeter Wasser passen hinein?`, I(l * w * d), 'm³', ['Volumen = Länge · Breite · Tiefe', `${l} · ${w} · ${d} = ${l * w * d}`]); },
    (n, n2, W_) => { const q = pick([10, 20, 5]), p = (q === 5 ? 100 : 50) * rnd(4, 20); return W_(`Ein Handy kostet ${p} €. Der Preis steigt um ${q} %. Was kostet es jetzt?`, I(p + p * q / 100), '€', [`${q} % von ${p} € = ${p * q / 100} €`, `${p} + ${p * q / 100} = ${p + p * q / 100} €`]); },
    (n, n2, W_) => { const v = pick([12, 15, 20]); let d; do d = v * pick([20, 30, 40, 60, 80, 90, 120]) / 60; while (d % 1); return W_(`${n} fährt mit dem Fahrrad ${v} km pro Stunde. Wie viele Minuten braucht ${n} für ${d} km?`, I(d / v * 60), 'min', [`In 60 Minuten: ${v} km`, `${d} km = ${fq(d, v)} von ${v} km`, `${fq(d, v)} · 60 = ${d / v * 60} min`]); },
    (n, n2, W_) => { const r = rnd(4, 12), h = rnd(2, 7); let h2; do h2 = rnd(2, 9); while (h2 === h); return W_(`Ein Maler streicht in ${h} Stunden ${r * h} m². Wie viele m² schafft er in ${h2} Stunden?`, I(r * h2), 'm²', [`In 1 Stunde: ${r * h} : ${h} = ${r} m²`, `In ${h2} Stunden: ${h2} · ${r} = ${r * h2} m²`]); },
    (n, n2, W_) => { const k = 100 * rnd(2, 30), z = pick([1, 2, 3, 5]); return W_(`Auf einem Sparkonto liegen ${k} €. Im Jahr gibt es ${z} % Zinsen. Wie viele Euro Zinsen sind das nach einem Jahr?`, I(k * z / 100), '€', [`1 % von ${k} € = ${k / 100} €`, `${z} % = ${z} · ${k / 100} = ${k * z / 100} €`]); },
    (n, n2, W_) => { const v = 5 * rnd(10, 26), h = rnd(2, 4); return W_(`Ein Auto fährt in ${h} Stunden ${v * h} km. Wie viele Kilometer fährt es im Durchschnitt pro Stunde?`, I(v), 'km', [`${v * h} : ${h} = ${v}`]); },
    (n, n2, W_) => { const c = rnd(2, 12); return W_(`Eine Wanderkarte hat den Maßstab 1 : 25000. Auf der Karte ist ein Weg ${c} cm lang. Wie viele Meter ist er in Wirklichkeit?`, I(c * 250), 'm', ['1 cm auf der Karte = 25000 cm = 250 m', `${c} · 250 = ${fi(c * 250)} m`]); },
  ],
  [
    (n, n2, W_) => { const g = rnd(5, 15), c = pick([5, 9, 10]), m = 10 * rnd(5, 30), t = g * 100 + c * m; return W_(`Ein Handytarif kostet ${g} € im Monat plus ${c} Cent pro Minute. ${n} bezahlt in einem Monat ${fmtMoney(t)} €. Wie viele Minuten hat ${n} telefoniert?`, I(m), 'min', [`Ohne Grundgebühr: ${fmtMoney(t)} € − ${g} € = ${fmtMoney(t - g * 100)} €`, `${t - g * 100} ct : ${c} ct = ${m} min`]); },
    (n, n2, W_) => { const dv = pick([1, 2]), [a, b, c] = pick([[3, 4, 5], [6, 8, 10], [5, 12, 13], [9, 12, 15]]).map(v => v / dv); return W_(`Eine Leiter ist ${fmtDec(c * 10, 1)} m lang. Ihr Fuß steht ${fmtDec(a * 10, 1)} m von der Wand entfernt. Wie hoch reicht die Leiter an der Wand?`, De(b * 10, 10), 'm', ['Satz des Pythagoras: Höhe² = Leiter² − Abstand²', `${fmtDec(c * 10, 1)}² − ${fmtDec(a * 10, 1)}² = ${decOfQ(Q(c * c * 100 - a * a * 100, 100))}`, `Wurzel: ${fmtDec(b * 10, 1)} m`]); },
    (n, n2, W_) => { const s = pick([50, 100, 200, 300]), h = rnd(3, 6); return W_(`Eine Bakterienkultur verdoppelt sich jede Stunde. Am Anfang sind es ${s} Bakterien. Wie viele sind es nach ${h} Stunden?`, I(s * 2 ** h), '', [`Nach ${h} Stunden: ${s} · 2${String(h).split('').map(c => SUPS[c]).join('')}`, `= ${s} · ${2 ** h} = ${fi(s * 2 ** h)}`]); },
    (n, n2, W_) => { const p = 1000 * rnd(10, 40), q = pick([10, 15, 20, 25]); return W_(`Ein neues Auto kostet ${fi(p)} €. Nach einem Jahr ist es ${q} % weniger wert. Wie viel ist es dann noch wert?`, I(p * (100 - q) / 100), '€', [`${q} % von ${fi(p)} € = ${fi(p * q / 100)} €`, `${fi(p)} − ${fi(p * q / 100)} = ${fi(p * (100 - q) / 100)} €`]); },
    (n, n2, W_) => { const a = rnd(2, 9), b = rnd(1, 20), x = rnd(2, 15); return W_(`${n} denkt sich eine Zahl, nimmt sie mal ${a} und addiert ${b}. Das Ergebnis ist ${a * x + b}. Welche Zahl hat sich ${n} gedacht?`, I(x), '', [`Gleichung: ${a}x + ${b} = ${a * x + b}`, `${a}x = ${a * x} → x = ${x}`]); },
    (n, n2, W_) => { const k = pick([20, 24, 25, 30]), bus = k * rnd(8, 20), e = rnd(3, 9); return W_(`Eine Klasse mit ${k} Kindern macht einen Ausflug. Der Bus kostet ${bus} € für alle, der Eintritt ${e} € pro Kind. Wie viel muss jedes Kind bezahlen?`, I(bus / k + e), '€', [`Bus pro Kind: ${bus} : ${k} = ${bus / k} €`, `${bus / k} + ${e} = ${bus / k + e} €`]); },
    (n, n2, W_) => { const x = rnd(5, 30); return W_(`${n} hat 3-mal so viele Murmeln wie ${n2}. Zusammen haben sie ${4 * x} Murmeln. Wie viele Murmeln hat ${n}?`, I(3 * x), '', [`Zusammen sind es 3 + 1 = 4 gleiche Teile.`, `1 Teil: ${4 * x} : 4 = ${x}`, `${n}: 3 · ${x} = ${3 * x}`]); },
    (n, n2, W_) => { const r = pick([20, 25, 40, 50]), t = rnd(5, 30), v = 2 * r * t; return W_(`Ein Becken fasst ${fi(v)} Liter. Pro Minute fließen ${r} Liter hinein. Nach wie vielen Minuten ist es halb voll?`, I(t), 'min', [`Halb voll: ${fi(v)} : 2 = ${fi(v / 2)} Liter`, `${fi(v / 2)} : ${r} = ${t} min`]); },
    (n, n2, W_) => { const t = 5 * rnd(4, 6), g = pick([2, 3]); return W_(`In einer Klasse sind ${t} Kinder. ${g}/5 davon sind Mädchen. Wie viele Jungen sind in der Klasse?`, I(t * (5 - g) / 5), '', [`Ein Fünftel: ${t} : 5 = ${t / 5}`, `Jungen: ${5 - g}/5 = ${5 - g} · ${t / 5} = ${t * (5 - g) / 5}`]); },
    (n, n2, W_) => { const b = rnd(3, 20); return W_(`Ein Rechteck ist doppelt so lang wie breit. Sein Umfang ist ${6 * b} cm. Wie lang ist es?`, I(2 * b), 'cm', ['Umfang = 2 · (Länge + Breite) = 2 · (2 · Breite + Breite) = 6 · Breite', `Breite: ${6 * b} : 6 = ${b} cm`, `Länge: 2 · ${b} = ${2 * b} cm`]); },
    (n, n2, W_) => { const a = rnd(6, 12), b = rnd(6, 12), y = rnd(2, 10); return W_(`${n} ist ${a} Jahre alt, ${n2} ist ${b}. In wie vielen Jahren sind beide zusammen ${a + b + 2 * y} Jahre alt?`, I(y), 'Jahre', [`Heute zusammen: ${a} + ${b} = ${a + b}`, `Pro Jahr kommen 2 Jahre dazu.`, `(${a + b + 2 * y} − ${a + b}) : 2 = ${y}`]); },
  ],
];
const GEN = {
  add(l) { const [a, b] = ADD[l](); return { e: [N(a), O('+'), N(b)], a: I(a + b), w: addW(a, b) }; },
  sub(l) { const [x, y] = ADD[l](); return { e: [N(x + y), O('−'), N(x)], a: I(y), w: subW(x + y, x) }; },
  mul(l) { let [a, b] = tablePair(l); if (coin()) [a, b] = [b, a]; return { e: [N(a), O('·'), N(b)], a: I(a * b), w: mulW(a, b), fact: factKey(a, b) }; },
  div(l) { const [x, y] = tablePair(l); return { e: [N(x * y), O(':'), N(y)], a: I(x), w: divW(x * y, y), fact: factKey(x, y) }; },

  gap(l) {
    const mk = (e, ans, w, fact) => ({ e, a: I(ans), noEq: true, hint: 'Welche Zahl fehlt?', w, fact });
    if (l <= 1) {
      const [x, y] = ADD[l === 0 ? 1 : 3](), s = x + y;
      switch (rnd(0, 2)) {
        case 0: return mk([N(x), O('+'), Gap(), Eq(), N(s)], y, [`Rechne rückwärts: ${s} − ${x} = ${y}`]);
        case 1: return mk([Gap(), O('+'), N(y), Eq(), N(s)], x, [`Rechne rückwärts: ${s} − ${y} = ${x}`]);
        default: return mk([N(s), O('−'), Gap(), Eq(), N(x)], y, [`Wie viel fehlt von ${x} bis ${s}?`, `${s} − ${x} = ${y}`]);
      }
    }
    if (l === 2) {
      const [x, y] = tablePair(1);
      return coin() ? mk([N(x), O('·'), Gap(), Eq(), N(x * y)], y, [`Umkehraufgabe: ${x * y} : ${x} = ${y}`], factKey(x, y))
        : mk([Gap(), O('·'), N(y), Eq(), N(x * y)], x, [`Umkehraufgabe: ${x * y} : ${y} = ${x}`], factKey(x, y));
    }
    if (l === 3) {
      const [x, y] = tablePair(1);
      return coin() ? mk([N(x * y), O(':'), Gap(), Eq(), N(x)], y, [`${x * y} geteilt durch welche Zahl ergibt ${x}?`, `${x * y} : ${x} = ${y}`], factKey(x, y))
        : mk([Gap(), O(':'), N(y), Eq(), N(x)], x * y, [`Umkehraufgabe: ${x} · ${y} = ${x * y}`], factKey(x, y));
    }
    if (coin()) {
      const [x, y] = ADD[4](), s = x + y;
      return coin() ? mk([N(x), O('+'), Gap(), Eq(), N(s)], y, [`Rechne rückwärts: ${s} − ${x} = ${y}`])
        : mk([Gap(), O('−'), N(x), Eq(), N(y)], s, [`Rechne rückwärts: ${y} + ${x} = ${s}`]);
    }
    const [x, y] = MUL[3]();
    return mk([N(x), O('·'), Gap(), Eq(), N(x * y)], y, [`Umkehraufgabe: ${x * y} : ${x} = ${y}`]);
  },

  frac(l) {
    const H = 'als gekürzter Bruch';
    if (l === 0) {
      const d = rnd(3, 12);
      if (coin()) { const a = rnd(1, d - 1), b = rnd(1, d - 1); return { e: [F(a, d), O('+'), F(b, d)], a: Fr(a + b, d), hint: H, w: ['Gleicher Nenner – nur die Zähler addieren:', `${fr(a, d)} + ${fr(b, d)} = ${fr(a + b, d)}`, ...redW(a + b, d)] }; }
      const a = rnd(2, d - 1), b = rnd(1, a - 1);
      return { e: [F(a, d), O('−'), F(b, d)], a: Fr(a - b, d), hint: H, w: ['Gleicher Nenner – nur die Zähler subtrahieren:', `${fr(a, d)} − ${fr(b, d)} = ${fr(a - b, d)}`, ...redW(a - b, d)] };
    }
    if (l === 1) {
      if (coin()) { const d = rnd(2, 9), n = properFrac(d), k = rnd(2, 6); return { e: [T('Kürze'), F(n * k, d * k)], a: Fr(n, d), hint: 'so weit wie möglich', w: [`${n * k} und ${d * k} sind beide durch ${k} teilbar.`, `Zähler und Nenner durch ${k} teilen: ${fr(n, d)}`] }; }
      const d = pick([2, 3, 4, 5, 6, 8, 10]), n = properFrac(d), m = d * rnd(1, 10);
      return { e: [F(n, d), T('von'), N(m)], a: I(n * m / d), w: [`${fr(n, d)} von ${m}: erst durch ${d} teilen, dann mal ${n}.`, `${m} : ${d} = ${m / d}`, ...(n > 1 ? [`${m / d} · ${n} = ${n * m / d}`] : [])] };
    }
    if (l === 2) {
      let d1, d2;
      do { d1 = pick([2, 3, 4, 5, 6, 8, 10, 12]); d2 = pick([2, 3, 4, 5, 6, 8, 10, 12]); } while (d1 === d2 || lcm(d1, d2) > 24);
      let n1 = properFrac(d1), n2 = properFrac(d2);
      if (coin() && n1 * d2 !== n2 * d1) {
        if (n1 * d2 < n2 * d1) { [n1, n2] = [n2, n1]; [d1, d2] = [d2, d1]; }
        return { e: [F(n1, d1), O('−'), F(n2, d2)], a: Fr(n1 * d2 - n2 * d1, d1 * d2), hint: H, w: fracAddW(n1, d1, n2, d2, '−') };
      }
      return { e: [F(n1, d1), O('+'), F(n2, d2)], a: Fr(n1 * d2 + n2 * d1, d1 * d2), hint: H, w: fracAddW(n1, d1, n2, d2, '+') };
    }
    if (l === 3) {
      const d1 = rnd(2, 9), d2 = rnd(2, 9), n1 = properFrac(d1), n2 = properFrac(d2);
      return coin()
        ? { e: [F(n1, d1), O('·'), F(n2, d2)], a: Fr(n1 * n2, d1 * d2), hint: H, w: ['Zähler mal Zähler, Nenner mal Nenner:', `${fr(n1, d1)} · ${fr(n2, d2)} = ${fr(n1 * n2, d1 * d2)}`, ...redW(n1 * n2, d1 * d2)] }
        : { e: [F(n1, d1), O(':'), F(n2, d2)], a: Fr(n1 * d2, d1 * n2), hint: H, w: [`Mit dem Kehrwert von ${fr(n2, d2)} malnehmen:`, `${fr(n1, d1)} · ${fr(d2, n2)} = ${fr(n1 * d2, d1 * n2)}`, ...redW(n1 * d2, d1 * n2)] };
    }
    const d = rnd(2, 9), n = properFrac(d), k = rnd(2, 9);
    switch (rnd(0, 3)) {
      case 0: return { e: [N(k), O('·'), F(n, d)], a: Fr(k * n, d), hint: H, w: [`Nur den Zähler mal ${k} nehmen:`, `${k} · ${fr(n, d)} = ${fr(k * n, d)}`, ...redW(k * n, d)] };
      case 1: { const g = rnd(1, 5); return { e: [N(g), O('−'), F(n, d)], a: Fr(g * d - n, d), hint: H, w: [`${g} als Bruch: ${g} = ${fr(g * d, d)}`, `${fr(g * d, d)} − ${fr(n, d)} = ${fr(g * d - n, d)}`, ...redW(g * d - n, d)] }; }
      case 2: return { e: [F(n, d), O(':'), N(k)], a: Fr(n, d * k), hint: H, w: [`Durch ${k} teilen heißt: Nenner mal ${k}.`, `${fr(n, d)} : ${k} = ${fr(n, d * k)}`, ...redW(n, d * k)] };
      default: {
        let d1, d2; do { d1 = rnd(2, 12); d2 = rnd(2, 12); } while (d1 === d2 || lcm(d1, d2) > 60);
        const n1 = properFrac(d1), n2 = properFrac(d2);
        return { e: [F(n1, d1), O('+'), F(n2, d2)], a: Fr(n1 * d2 + n2 * d1, d1 * d2), hint: H, w: fracAddW(n1, d1, n2, d2, '+') };
      }
    }
  },

  dec(l) {
    if (l === 0) {
      let a = noTrail(11, 99), b = noTrail(11, 99);
      if (coin()) return { e: [D(a, 1), O('+'), D(b, 1)], a: De(a + b, 10), w: [`In Zehnteln rechnen: ${a} + ${b} = ${a + b} Zehntel`, `${a + b} Zehntel = ${fmtDec(a + b, 1)}`] };
      if (a === b) a += 1; if (a < b) [a, b] = [b, a];
      return { e: [D(a, 1), O('−'), D(b, 1)], a: De(a - b, 10), w: [`In Zehnteln rechnen: ${a} − ${b} = ${a - b} Zehntel`, `${a - b} Zehntel = ${fmtDec(a - b, 1)}`] };
    }
    if (l === 1) {
      const x = D(noTrail(101, 999), 2), y = D(noTrail(11, 99), 1), vx = x.i, vy = y.i * 10;
      const head = ['Komma unter Komma – in Hundertsteln rechnen:', `${fmtDec(x.i, 2)} = ${vx} Hundertstel, ${fmtDec(y.i, 1)} = ${vy} Hundertstel`];
      if (coin() || vx === vy) { const e = coin() ? [x, O('+'), y] : [y, O('+'), x]; return { e, a: De(vx + vy, 100), w: [...head, `${vx} + ${vy} = ${vx + vy} Hundertstel = ${fmtDec(vx + vy, 2)}`] }; }
      return vx > vy ? { e: [x, O('−'), y], a: De(vx - vy, 100), w: [...head, `${vx} − ${vy} = ${vx - vy} Hundertstel = ${fmtDec(vx - vy, 2)}`] }
        : { e: [y, O('−'), x], a: De(vy - vx, 100), w: [...head, `${vy} − ${vx} = ${vy - vx} Hundertstel = ${fmtDec(vy - vx, 2)}`] };
    }
    if (l === 2) {
      const k = rnd(1, 2), xi = noTrail(11, 999), f = pick([10, 100, 1000]), z = Math.log10(f), st = z > 1 ? `${z} Stellen` : 'eine Stelle';
      return coin() ? { e: [D(xi, k), O('·'), N(f)], a: De(xi * f, 10 ** k), w: [`Mal ${f}: Komma um ${st} nach rechts`, `${fmtDec(xi, k)} · ${f} = ${decOfQ(Q(xi * f, 10 ** k))}`] }
        : { e: [D(xi, k), O(':'), N(f)], a: De(xi, 10 ** k * f), w: [`Geteilt durch ${f}: Komma um ${st} nach links`, `${fmtDec(xi, k)} : ${f} = ${decOfQ(Q(xi, 10 ** k * f))}`] };
    }
    if (l === 3) {
      if (rand() < .3) { const a = rnd(1, 9), b = rnd(1, 9); return { e: [D(a, 1), O('·'), D(b, 1)], a: De(a * b, 100), w: [`Ohne Komma rechnen: ${a} · ${b} = ${a * b}`, `Zusammen zwei Nachkommastellen: ${fmtDec(a * b, 2)}`] }; }
      const a = noTrail(11, 99), b = rnd(2, 9);
      return { e: [D(a, 1), O('·'), N(b)], a: De(a * b, 10), w: [`Ohne Komma rechnen: ${a} · ${b} = ${a * b}`, `Eine Nachkommastelle zurück: ${fmtDec(a * b, 1)}`] };
    }
    switch (rnd(0, 2)) {
      case 0: { const q = noTrail(11, 99), b = rnd(2, 9); return { e: [D(q * b, 1), O(':'), N(b)], a: De(q, 10), w: [`In Zehnteln rechnen: ${q * b} Zehntel : ${b} = ${q} Zehntel`, `${q} Zehntel = ${fmtDec(q, 1)}`] }; }
      case 1: { const b = rnd(2, 9), q = rnd(2, 12); return { e: [D(q * b, 1), O(':'), D(b, 1)], a: I(q), w: ['Beide Zahlen mal 10 – das Ergebnis bleibt gleich:', `${q * b} : ${b} = ${q}`] }; }
      default: {
        const d = pick([2, 4, 5, 8, 10, 20, 25]), n = properFrac(d);
        let k = 1; while ((10 ** k) % d) k++; const m = 10 ** k / d;
        return { e: [F(n, d)], a: De(n, d), hint: 'als Dezimalzahl', w: [`Erweitern auf ${10 ** k}: ${fr(n, d)} = ${fr(n * m, 10 ** k)}`, `${fr(n * m, 10 ** k)} = ${fmtDec(n * m, k)}`] };
      }
    }
  },

  pct(l) {
    const mult = (p, max) => { const step = 100 / gcd(p, 100); return step * rnd(1, Math.max(1, Math.floor(max / step))); };
    if (l <= 1) { const p = l === 0 ? pick([50, 25, 10]) : pick([1, 5, 20, 75, 30, 40, 15, 60]), m = mult(p, l === 0 ? 200 : 400); return { e: [N(p), T('% von'), N(m)], a: I(p * m / 100), w: pctW(p, m) }; }
    const p = pick([10, 20, 25, 50, 75, 5, 40, 30]), m = mult(p, 300), part = p * m / 100;
    if (l === 2) return { e: [N(part), T('von'), N(m)], a: I(p), unit: '%', hint: 'Wie viel Prozent sind das?', w: [`Anteil als Bruch: ${fr(part, m)}`, `${fr(part, m)} = ${fr(p, 100)}`, `${fr(p, 100)} = ${p} %`] };
    if (l === 3) return { e: [N(p), T('% sind'), N(part)], a: I(m), hint: 'Wie viel sind 100 %?', w: 100 % p === 0 ? [`${p} % passen ${100 / p}-mal in 100 %.`, `${part} · ${100 / p} = ${m}`] : [`1 % = ${part} : ${p}`, `100 % = ${part} : ${p} · 100 = ${m}`] };
    const up = coin(), q = pick([10, 20, 25, 50, 5, 15, 30]), price = mult(q, 400), dlt = price * q / 100;
    return { e: [N(price), U('€'), O(up ? '+' : '−'), N(q), U('%')], a: I(price + (up ? dlt : -dlt)), unit: '€', hint: up ? 'Preis wird erhöht' : 'Preis wird reduziert', w: [...pctW(q, price), up ? `${price} € + ${dlt} € = ${price + dlt} €` : `${price} € − ${dlt} € = ${price - dlt} €`] };
  },

  neg(l) {
    const plusW = (a, b) => b < 0 ? [`Plus eine negative Zahl heißt minus: ${fi(a)} + ${par(b)} = ${fi(a)} − ${fi(-b)}`, `= ${fi(a + b)}`] : [`Starte bei ${fi(a)} und gehe ${b} nach rechts.`, `${fi(a)} + ${b} = ${fi(a + b)}`];
    const minusW = (a, b) => b < 0 ? [`Minus eine negative Zahl heißt plus: ${fi(a)} − ${par(b)} = ${fi(a)} + ${fi(-b)}`, `= ${fi(a - b)}`] : [`Starte bei ${fi(a)} und gehe ${b} nach links.`, `${fi(a)} − ${b} = ${fi(a - b)}`];
    const signW = (a, b, r, op) => [`Vorzeichen: ${a < 0 ? 'minus' : 'plus'} ${op} ${b < 0 ? 'minus' : 'plus'} ergibt ${r < 0 ? 'minus' : 'plus'}.`, `${Math.abs(a)} ${op === 'mal' ? '·' : ':'} ${Math.abs(b)} = ${Math.abs(r)}`, `Ergebnis: ${fi(r)}`];
    if (l === 0) { let a, b; do { a = rnd(-10, 10); b = rnd(-10, 10); } while (a >= 0 && b >= 0); return { e: [N(a), O('+'), N(b, true)], a: I(a + b), w: plusW(a, b) }; }
    if (l === 1) { const a = rnd(-10, 10), b = rnd(2, 15); return { e: [N(a), O('−'), N(b)], a: I(a - b), w: minusW(a, b) }; }
    if (l === 2) { const a = rnd(-20, 20), b = rnd(-20, -1); return coin() ? { e: [N(a), O('+'), N(b, true)], a: I(a + b), w: plusW(a, b) } : { e: [N(a), O('−'), N(b, true)], a: I(a - b), w: minusW(a, b) }; }
    if (l === 3) {
      let a, b; do { a = rnd(1, 10) * sign(); b = rnd(1, 10) * sign(); } while (a > 0 && b > 0);
      return coin() ? { e: [N(a), O('·'), N(b, true)], a: I(a * b), w: signW(a, b, a * b, 'mal') } : { e: [N(a * b), O(':'), N(b, true)], a: I(a), w: signW(a * b, b, a, 'geteilt durch') };
    }
    switch (rnd(0, 2)) {
      case 0: { const a = rnd(-60, 60), b = rnd(-60, 60); return { e: [N(a), O('−'), N(b, true)], a: I(a - b), w: minusW(a, b) }; }
      case 1: { const a = rnd(2, 15) * sign(), b = rnd(2, 9) * sign(); return { e: [N(a), O('·'), N(b, true)], a: I(a * b), w: signW(a, b, a * b, 'mal') }; }
      default: { const a = rnd(-20, 20), b = rnd(-20, 20), c = rnd(-20, 20); return { e: [N(a), O('−'), N(b, true), O('+'), N(c, true)], a: I(a - b + c), w: ['Von links nach rechts rechnen:', `${fi(a)} − ${par(b)} = ${fi(a - b)}`, `${fi(a - b)} + ${par(c)} = ${fi(a - b + c)}`] }; }
    }
  },

  pow(l) {
    const sq = (lo, hi) => { const n = rnd(lo, hi); return { e: [P(n, 2)], a: I(n * n), w: [`${n}² = ${n} · ${n} = ${n * n}`] }; };
    const rt = (lo, hi) => { const n = rnd(lo, hi); return { e: [W(n * n)], a: I(n), w: [`Welche Zahl mal sich selbst ergibt ${n * n}?`, `${n} · ${n} = ${n * n}, also √${n * n} = ${n}`] }; };
    if (l === 0) return sq(1, 10);
    if (l === 1) return coin() ? sq(1, 12) : rt(1, 12);
    if (l === 2) return coin() ? sq(11, 20) : rt(11, 20);
    if (l === 3) {
      const [b, e] = pick([[2, rnd(0, 10)], [10, rnd(0, 6)], [rnd(1, 6), 3], [3, rnd(0, 5)], [5, rnd(0, 4)]]), r = b ** e;
      const w = e === 0 ? ['Jede Zahl hoch 0 ergibt 1.'] : e === 1 ? [`Hoch 1 ändert nichts: ${b}`] : b === 10 ? [`10 hoch ${e} ist eine 1 mit ${e} Nullen: ${fi(r)}`] : [`${b} ${e}-mal mit sich selbst malnehmen:`, `${Array(e).fill(b).join(' · ')} = ${fi(r)}`];
      return { e: [P(b, e)], a: I(r), w };
    }
    switch (rnd(0, 3)) {
      case 0: { const a = rnd(1, 12), b = rnd(1, 12); return { e: [P(a, 2), O('+'), P(b, 2)], a: I(a * a + b * b), w: [`Erst die Potenzen: ${a}² = ${a * a}, ${b}² = ${b * b}`, `${a * a} + ${b * b} = ${a * a + b * b}`] }; }
      case 1: { const a = rnd(3, 15), b = rnd(1, a - 1); return { e: [P(a, 2), O('−'), P(b, 2)], a: I(a * a - b * b), w: [`Erst die Potenzen: ${a}² = ${a * a}, ${b}² = ${b * b}`, `${a * a} − ${b * b} = ${a * a - b * b}`] }; }
      case 2: { const x = rnd(1, 15), y = rnd(1, 15); return { e: [W(x * x), O('+'), W(y * y)], a: I(x + y), w: [`√${x * x} = ${x} und √${y * y} = ${y}`, `${x} + ${y} = ${x + y}`] }; }
      default: { const x = rnd(2, 12), b = rnd(2, 9); return { e: [W(x * x), O('·'), N(b)], a: I(x * b), w: [`√${x * x} = ${x}`, `${x} · ${b} = ${x * b}`] }; }
    }
  },

  order(l) {
    const PV = 'Punkt vor Strich: ';
    if (l === 0) {
      const a = rnd(1, 20), b = rnd(2, 10), c = rnd(2, 10);
      return coin() ? { e: [N(a), O('+'), N(b), O('·'), N(c)], a: I(a + b * c), w: [`${PV}zuerst ${b} · ${c} = ${b * c}`, `${a} + ${b * c} = ${a + b * c}`] }
        : { e: [N(b), O('·'), N(c), O('+'), N(a)], a: I(b * c + a), w: [`${PV}zuerst ${b} · ${c} = ${b * c}`, `${b * c} + ${a} = ${b * c + a}`] };
    }
    if (l === 1) {
      if (coin()) { const a = rnd(1, 9), b = rnd(1, 9), c = rnd(2, 9); return { e: [K('('), N(a), O('+'), N(b), K(')'), O('·'), N(c)], a: I((a + b) * c), w: [`Klammer zuerst: ${a} + ${b} = ${a + b}`, `${a + b} · ${c} = ${(a + b) * c}`] }; }
      const b = rnd(2, 9), c = rnd(2, 9), a = b * c + rnd(0, 30);
      return { e: [N(a), O('−'), N(b), O('·'), N(c)], a: I(a - b * c), w: [`${PV}zuerst ${b} · ${c} = ${b * c}`, `${a} − ${b * c} = ${a - b * c}`] };
    }
    if (l === 2) {
      switch (rnd(0, 2)) {
        case 0: { const c = rnd(2, 9), b = c * rnd(2, 10), a = rnd(1, 50); return { e: [N(a), O('+'), N(b), O(':'), N(c)], a: I(a + b / c), w: [`${PV}zuerst ${b} : ${c} = ${b / c}`, `${a} + ${b / c} = ${a + b / c}`] }; }
        case 1: { const a = rnd(3, 9), b = rnd(3, 9), c = rnd(2, 5), d = rnd(1, Math.floor(a * b / c)); return { e: [N(a), O('·'), N(b), O('−'), N(c), O('·'), N(d)], a: I(a * b - c * d), w: [`${PV}${a} · ${b} = ${a * b} und ${c} · ${d} = ${c * d}`, `${a * b} − ${c * d} = ${a * b - c * d}`] }; }
        default: { const b = rnd(1, 15), a = b + rnd(1, 10), c = rnd(2, 9); return { e: [K('('), N(a), O('−'), N(b), K(')'), O('·'), N(c)], a: I((a - b) * c), w: [`Klammer zuerst: ${a} − ${b} = ${a - b}`, `${a - b} · ${c} = ${(a - b) * c}`] }; }
      }
    }
    if (l === 3) {
      if (coin()) { const a = rnd(2, 9), b = rnd(1, 12), c = rnd(1, 12), d = rnd(1, a * (b + c)); return { e: [N(a), O('·'), K('('), N(b), O('+'), N(c), K(')'), O('−'), N(d)], a: I(a * (b + c) - d), w: [`Klammer zuerst: ${b} + ${c} = ${b + c}`, `Dann Punkt vor Strich: ${a} · ${b + c} = ${a * (b + c)}`, `${a * (b + c)} − ${d} = ${a * (b + c) - d}`] }; }
      const c = rnd(2, 9), s = c * rnd(2, 12), a = rnd(1, s - 1), b = s - a, d = rnd(1, 40);
      return { e: [K('('), N(a), O('+'), N(b), K(')'), O(':'), N(c), O('+'), N(d)], a: I(s / c + d), w: [`Klammer zuerst: ${a} + ${b} = ${s}`, `Dann Punkt vor Strich: ${s} : ${c} = ${s / c}`, `${s / c} + ${d} = ${s / c + d}`] };
    }
    switch (rnd(0, 2)) {
      case 0: { const a = rnd(2, 15), b = rnd(2, 15), d = rnd(1, 9), c = d + rnd(1, 9); return { e: [K('('), N(a), O('+'), N(b), K(')'), O('·'), K('('), N(c), O('−'), N(d), K(')')], a: I((a + b) * (c - d)), w: [`Beide Klammern zuerst: ${a} + ${b} = ${a + b} und ${c} − ${d} = ${c - d}`, `${a + b} · ${c - d} = ${(a + b) * (c - d)}`] }; }
      case 1: { const a = rnd(2, 12), b = rnd(2, 9), c = rnd(2, 9), d = rnd(2, 9), e = rnd(1, a * b + c * d); return { e: [N(a), O('·'), N(b), O('+'), N(c), O('·'), N(d), O('−'), N(e)], a: I(a * b + c * d - e), w: [`${PV}${a} · ${b} = ${a * b} und ${c} · ${d} = ${c * d}`, `${a * b} + ${c * d} = ${a * b + c * d}`, `${a * b + c * d} − ${e} = ${a * b + c * d - e}`] }; }
      default: { const c = rnd(1, 9), b = c + rnd(1, 6), d = rnd(2, 9), a = (b - c) * d + rnd(0, 60); return { e: [N(a), O('−'), K('('), N(b), O('−'), N(c), K(')'), O('·'), N(d)], a: I(a - (b - c) * d), w: [`Klammer zuerst: ${b} − ${c} = ${b - c}`, `Dann Punkt vor Strich: ${b - c} · ${d} = ${(b - c) * d}`, `${a} − ${(b - c) * d} = ${a - (b - c) * d}`] }; }
    }
  },

  round(l) {
    const r = (x, m) => Math.round(x / m) * m;
    const NM = { 10: ['Zehner', 'Einerstelle'], 100: ['Hunderter', 'Zehnerstelle'], 1000: ['Tausender', 'Hunderterstelle'] };
    const rw = (x, m) => { const dig = Math.floor(x % m / (m / 10)); return [`Entscheidend ist die ${NM[m][1]}: ${dig}`, dig >= 5 ? `${dig} ist 5 oder mehr → aufrunden` : `${dig} ist kleiner als 5 → abrunden`, `${fi(x)} ≈ ${fi(r(x, m))}`]; };
    const task = (x, m) => ({ e: [T('Runde'), N(x), T(`auf ${NM[m][0]}`)], a: I(r(x, m)), eqSign: '≈', w: rw(x, m) });
    if (l === 0) { let x; do x = rnd(11, 999); while (x % 10 === 0); return task(x, 10); }
    if (l === 1) { let x; do x = rnd(101, 9999); while (x % 100 === 0); return task(x, 100); }
    if (l === 2) {
      if (coin()) { let x; do x = rnd(1001, 99999); while (x % 1000 === 0); return task(x, 1000); }
      const i = noTrail(101, 9999), h = i % 10, ri = Math.round(i / 10);
      return { e: [T('Runde'), D(i, 2), T('auf Zehntel')], a: De(ri, 10), eqSign: '≈', w: [`Entscheidend ist die Hundertstelstelle: ${h}`, h >= 5 ? `${h} ist 5 oder mehr → aufrunden` : `${h} ist kleiner als 5 → abrunden`, `${fmtDec(i, 2)} ≈ ${fmtDec(ri, 1)}`] };
    }
    if (l === 3) {
      let a = rnd(101, 999), b = rnd(101, 999); const minus = coin();
      if (minus && r(a, 100) < r(b, 100)) [a, b] = [b, a];
      const ra = r(a, 100), rb = r(b, 100);
      return { e: [T('Überschlag:'), N(a), O(minus ? '−' : '+'), N(b)], a: I(minus ? ra - rb : ra + rb), eqSign: '≈', hint: 'Runde beide Zahlen auf Hunderter', w: [`${a} ≈ ${ra} und ${b} ≈ ${rb}`, `${ra} ${minus ? '−' : '+'} ${rb} = ${minus ? ra - rb : ra + rb}`] };
    }
    const a = rnd(12, 99), b = rnd(12, 99), ra = r(a, 10), rb = r(b, 10);
    return { e: [T('Überschlag:'), N(a), O('·'), N(b)], a: I(ra * rb), eqSign: '≈', hint: 'Runde beide Zahlen auf Zehner', w: [`${a} ≈ ${ra} und ${b} ≈ ${rb}`, `${ra} · ${rb} = ${fi(ra * rb)}`] };
  },

  unit(l) {
    const conv = (pairs, maxX) => {
      const [from, to, f] = pick(pairs);
      if (coin()) { const x = rnd(1, maxX); return { e: [N(x), U(from)], a: I(x * f), unit: to, w: [`1 ${from} = ${fi(f)} ${to}`, `${x} · ${fi(f)} = ${fi(x * f)}`] }; }
      const y = rnd(1, maxX); return { e: [N(y * f), U(to)], a: I(y), unit: from, w: [`1 ${from} = ${fi(f)} ${to}`, `${fi(y * f)} : ${fi(f)} = ${y}`] };
    };
    if (l === 0) return conv([['m', 'cm', 100], ['cm', 'mm', 10], ['kg', 'g', 1000], ['€', 'ct', 100], ['m', 'dm', 10]], 20);
    if (l === 1) return conv([['km', 'm', 1000], ['h', 'min', 60], ['min', 's', 60], ['Tage', 'h', 24], ['l', 'ml', 1000], ['t', 'kg', 1000]], 12);
    if (l === 2) {
      if (coin()) {
        const [from, to, f] = pick([['m', 'cm', 100], ['km', 'm', 1000], ['kg', 'g', 1000], ['l', 'ml', 1000], ['h', 'min', 60], ['€', 'ct', 100]]), i = noTrail(11, 99);
        return { e: [D(i, 1), U(from)], a: De(i * f, 10), unit: to, w: [`1 ${from} = ${fi(f)} ${to}`, `${fmtDec(i, 1)} · ${fi(f)} = ${decOfQ(Q(i * f, 10))}`] };
      }
      const [from, to, f] = pick([['g', 'kg', 1000], ['m', 'km', 1000], ['ml', 'l', 1000], ['cm', 'm', 100], ['ct', '€', 100]]);
      let x; do x = rnd(11, f * 3 - 1); while (x % f === 0 || x % 10 === 0 && x % 100 === 0);
      return { e: [N(x), U(from)], a: De(x, f), unit: to, w: [`1 ${to} = ${fi(f)} ${from}`, `${fi(x)} : ${fi(f)} = ${decOfQ(Q(x, f))}`] };
    }
    if (l === 3) return conv([['m²', 'dm²', 100], ['dm²', 'cm²', 100], ['m²', 'cm²', 10000], ['ha', 'm²', 10000], ['l', 'cm³', 1000], ['dm³', 'l', 1], ['cm³', 'ml', 1], ['m³', 'dm³', 1000], ['a', 'm²', 100]], 9);
    switch (rnd(0, 4)) {
      case 0: { const h = rnd(1, 4), mi = 5 * rnd(1, 11); return { e: [N(h), U('h'), N(mi), U('min')], a: I(h * 60 + mi), unit: 'min', w: [`${h} h = ${h * 60} min`, `${h * 60} + ${mi} = ${h * 60 + mi} min`] }; }
      case 1: { const m = rnd(1, 9), c = rnd(1, 99); return { e: [N(m), U('m'), N(c), U('cm')], a: I(m * 100 + c), unit: 'cm', w: [`${m} m = ${m * 100} cm`, `${m * 100} + ${c} = ${m * 100 + c} cm`] }; }
      case 2: { const k = rnd(1, 9), g = rnd(5, 995); return { e: [N(k), U('kg'), N(g), U('g')], a: I(k * 1000 + g), unit: 'g', w: [`${k} kg = ${k * 1000} g`, `${k * 1000} + ${g} = ${fi(k * 1000 + g)} g`] }; }
      case 3: { const h = rnd(1, 4), q = pick([15, 30, 45]), mi = h * 60 + q; return { e: [N(mi), U('min')], a: De(mi, 60), unit: 'h', w: [`${mi} min = ${h} h ${q} min`, `${q} min = ${fmtDec(q * 100 / 60, 2)} h → ${decOfQ(Q(mi, 60))} h`] }; }
      default: { const x = pick([[25, 2, 15], [5, 1, 30], [75, 2, 45]]); return { e: [D(x[0], x[1]), U('h')], a: I(x[2]), unit: 'min', w: ['1 h = 60 min', `${fmtDec(x[0], x[1])} · 60 = ${x[2]} min`] }; }
    }
  },

  divis(l) {
    if (l === 0) {
      const b = rnd(2, 9), q = rnd(2, 10), r = rnd(1, b - 1), a = b * q + r;
      return { e: [N(a), O(':'), N(b), Eq(), N(q), T('Rest'), Gap()], a: I(r), noEq: true, hint: 'Wie groß ist der Rest?', w: [`${q} · ${b} = ${q * b}`, `${a} − ${q * b} = ${r}`, `Also ${a} : ${b} = ${q} Rest ${r}`] };
    }
    if (l === 1) {
      const x = rnd(100, 99999), dg = String(x).split('').map(Number), s = dg.reduce((p, c) => p + c, 0);
      return { e: [T('Quersumme von'), N(x)], a: I(s), w: [`Alle Ziffern addieren: ${dg.join(' + ')} = ${s}`, s % 3 === 0 ? `Tipp: ${s} ist durch 3 teilbar – also auch ${fi(x)}.` : `Tipp: ${s} ist nicht durch 3 teilbar – also auch ${fi(x)} nicht.`] };
    }
    if (l === 2) {
      const g = rnd(2, 12); let x, y; do { x = rnd(1, 9); y = rnd(1, 9); } while (x === y || gcd(x, y) !== 1);
      const a = g * x, b = g * y;
      return { e: [S_(`ggT(${a}, ${b})`)], a: I(g), w: [`Teiler von ${a}: ${divisors(a).join(', ')}`, `Teiler von ${b}: ${divisors(b).join(', ')}`, `Der größte gemeinsame Teiler ist ${g}.`] };
    }
    if (l === 3) {
      let a, b; do { a = rnd(2, 15); b = rnd(2, 15); } while (a === b || lcm(a, b) > 120);
      const L = lcm(a, b), mult = n => Array.from({ length: L / n }, (_, i) => n * (i + 1)).slice(-6);
      return { e: [S_(`kgV(${a}, ${b})`)], a: I(L), w: [`Vielfache von ${a}: …, ${mult(a).join(', ')}`, `Vielfache von ${b}: …, ${mult(b).join(', ')}`, `Das kleinste gemeinsame Vielfache ist ${L}.`] };
    }
    if (coin()) {
      const pr = [3, 5, 7, 11, 13]; let p, q; do { p = pick(pr); q = pick(pr); } while (p * q > 200); if (p > q) [p, q] = [q, p];
      const n = p * q, before = [2, 3, 5, 7, 11].filter(x => x < p);
      return { e: [T('kleinster Primfaktor von'), N(n)], a: I(p), w: [...(before.length ? [`${n} ist nicht durch ${before.join(', ')} teilbar.`] : []), `${n} = ${p} · ${q}`, `Der kleinste Primfaktor ist ${p}.`] };
    }
    const n = rnd(12, 100), dv = divisors(n);
    return { e: [T('Anzahl der Teiler von'), N(n)], a: I(dv.length), w: [`Teiler von ${n}: ${dv.join(', ')}`, `Das sind ${dv.length} Teiler.`] };
  },

  eq(l) {
    const mk = (e, x, w) => ({ e, a: I(x), noEq: true, pre: 'x =', w });
    if (l === 0) {
      const a = rnd(1, 30);
      if (coin()) { const x = rnd(1, 30); return mk([V(1), O('+'), N(a), Eq(), N(x + a)], x, [`Auf beiden Seiten ${a} abziehen:`, `x = ${x + a} − ${a} = ${x}`]); }
      const x = rnd(a + 1, a + 30); return mk([V(1), O('−'), N(a), Eq(), N(x - a)], x, [`Auf beiden Seiten ${a} addieren:`, `x = ${x - a} + ${a} = ${x}`]);
    }
    if (l === 1) {
      const a = rnd(2, 10), x = rnd(2, 12);
      return coin() ? mk([V(a), Eq(), N(a * x)], x, [`Beide Seiten durch ${a} teilen:`, `x = ${a * x} : ${a} = ${x}`]) : mk([V(1), O(':'), N(a), Eq(), N(x)], x * a, [`Beide Seiten mal ${a} nehmen:`, `x = ${x} · ${a} = ${x * a}`]);
    }
    if (l === 2) {
      const a = rnd(2, 9), x = rnd(1, 12), b = rnd(1, 20);
      return coin() ? mk([V(a), O('+'), N(b), Eq(), N(a * x + b)], x, [`− ${b} auf beiden Seiten: ${a}x = ${a * x}`, `: ${a} auf beiden Seiten: x = ${x}`])
        : mk([V(a), O('−'), N(b), Eq(), N(a * x - b)], x, [`+ ${b} auf beiden Seiten: ${a}x = ${a * x}`, `: ${a} auf beiden Seiten: x = ${x}`]);
    }
    if (l === 3) {
      let x; do x = rnd(-10, 10); while (!x);
      const a = rnd(2, 9), b = rnd(1, 9);
      if (coin()) { const c = a * x + b; return mk([V(a), O('+'), N(b), Eq(), N(c)], x, [`− ${b} auf beiden Seiten: ${a}x = ${fi(c - b)}`, `: ${a} auf beiden Seiten: x = ${fi(x)}`]); }
      return mk([N(a), K('('), V(1), O('+'), N(b), K(')'), Eq(), N(a * (x + b))], x, [`: ${a} auf beiden Seiten: x + ${b} = ${fi(x + b)}`, `− ${b} auf beiden Seiten: x = ${fi(x)}`]);
    }
    const a = rnd(3, 9), c = rnd(1, a - 1); let x; do x = rnd(-8, 12); while (!x);
    const b = rnd(-20, 20), d = (a - c) * x + b;
    const side = (k, v) => [V(k), ...(v ? [O(v > 0 ? '+' : '−'), N(Math.abs(v))] : [])];
    return mk([...side(a, b), Eq(), ...side(c, d)], x, [`− ${c}x auf beiden Seiten: ${a - c === 1 ? '' : a - c}x${b ? sgnTerm(b) : ''} = ${fi(d)}`, ...(b ? [`${b > 0 ? '− ' + b : '+ ' + (-b)} auf beiden Seiten: ${a - c === 1 ? '' : a - c}x = ${fi(d - b)}`] : []), ...(a - c > 1 ? [`: ${a - c} auf beiden Seiten: x = ${fi(x)}`] : [])]);
  },

  word(l) {
    const n = pick(NAMES); let n2; do n2 = pick(NAMES); while (n2 === n);
    const W_ = (story, a, unit, w, hint) => ({ e: [Story(story)], a, unit, w, noEq: true, story: true, hint });
    if (rand() < .5) return pick(WORD_MORE[l])(n, n2, W_);   // neue Geschichten
    if (l === 0) {
      switch (rnd(0, 3)) {
        case 0: { const a = rnd(3, 12), b = rnd(2, 8); return W_(`${n} hat ${a} Murmeln und bekommt ${b} geschenkt. Wie viele Murmeln hat ${n} jetzt?`, I(a + b), '', [`Dazubekommen heißt plus: ${a} + ${b} = ${a + b}`]); }
        case 1: { const a = rnd(8, 20), b = rnd(2, a - 1); return W_(`Im Bus sitzen ${a} Kinder. An der Haltestelle steigen ${b} aus. Wie viele Kinder sitzen noch im Bus?`, I(a - b), '', [`Aussteigen heißt minus: ${a} − ${b} = ${a - b}`]); }
        case 2: { const a = rnd(3, 12), b = a + rnd(2, 8); return W_(`${n} hat ${a} Sticker, ${n2} hat ${b} Sticker. Wie viele Sticker hat ${n2} mehr?`, I(b - a), '', [`Unterschied: ${b} − ${a} = ${b - a}`]); }
        default: { const p = rnd(1, 4), z = pick([5, 10]); return W_(`Ein Eis kostet ${p} €. ${n} bezahlt mit ${z} €. Wie viel Geld bekommt ${n} zurück?`, I(z - p), '€', [`Rückgeld = bezahlt − Preis`, `${z} − ${p} = ${z - p}`]); }
      }
    }
    if (l === 1) {
      switch (rnd(0, 4)) {
        case 0: { const p = rnd(2, 9), k = rnd(2, 9); return W_(`Ein Heft kostet ${p} €. ${n} kauft ${k} Hefte. Wie viel muss ${n} bezahlen?`, I(p * k), '€', [`${k} Hefte zu je ${p} €: ${k} · ${p} = ${k * p}`]); }
        case 1: { const k = rnd(2, 9), c = rnd(2, 9); return W_(`${k * c} Bonbons werden gerecht an ${k} Kinder verteilt. Wie viele Bonbons bekommt jedes Kind?`, I(c), '', [`Gerecht verteilen heißt teilen: ${k * c} : ${k} = ${c}`]); }
        case 2: { const r = rnd(3, 9), c = rnd(4, 9); return W_(`Im Klassenzimmer stehen ${r} Reihen mit je ${c} Stühlen. Wie viele Stühle sind es zusammen?`, I(r * c), '', [`${r} Reihen mit je ${c}: ${r} · ${c} = ${r * c}`]); }
        case 3: { const p = rnd(5, 20), d = rnd(3, 7); return W_(`${n} liest jeden Tag ${p} Seiten. Wie viele Seiten liest ${n} in ${d} Tagen?`, I(p * d), '', [`${d} Tage mit je ${p} Seiten: ${d} · ${p} = ${d * p}`]); }
        default: { const k = rnd(2, 6); return W_(`Eine Schulstunde dauert 45 Minuten. Wie viele Minuten dauern ${k} Schulstunden?`, I(45 * k), 'min', [`${k} · 45 = ${45 * k}`]); }
      }
    }
    if (l === 2) {
      switch (rnd(0, 3)) {
        case 0: { const c = rnd(69, 199), k = rnd(2, 5); return W_(`Ein Liter Milch kostet ${fmtMoney(c)} €. Wie viel kosten ${k} Liter?`, Mo(c * k), '€', [`In Cent rechnen: ${k} · ${c} ct = ${c * k} ct`, `${c * k} ct = ${fmtMoney(c * k)} €`]); }
        case 1: { const d = pick([6, 8, 10, 12]), e = rnd(1, d - 1); return W_(`Eine Pizza wird in ${d} gleich große Stücke geschnitten. ${n} isst ${e} Stücke. Welcher Bruchteil der Pizza ist übrig?`, Fr(d - e, d), '', [`Übrig sind ${d - e} von ${d} Stücken: ${fr(d - e, d)}`, ...redW(d - e, d)], 'als gekürzter Bruch'); }
        case 2: { const a = rnd(3, 15), b = rnd(2, 12); return W_(`Ein Rechteck ist ${a} cm lang und ${b} cm breit. Wie groß ist seine Fläche?`, I(a * b), 'cm²', ['Fläche = Länge · Breite', `${a} · ${b} = ${a * b}`]); }
        default: { const a = rnd(5, 40), b = rnd(3, 25); return W_(`Ein Garten ist ${a} m lang und ${b} m breit. Wie lang wird ein Zaun rundherum?`, I(2 * (a + b)), 'm', ['Umfang = 2 · (Länge + Breite)', `2 · (${a} + ${b}) = 2 · ${a + b} = ${2 * (a + b)}`]); }
      }
    }
    if (l === 3) {
      switch (rnd(0, 3)) {
        case 0: { const q = pick([10, 20, 25, 50]), p = 20 * rnd(2, 12), d = p * q / 100; return W_(`Eine Jacke kostet ${p} € und wird um ${q} % reduziert. Was kostet sie jetzt?`, I(p - d), '€', [...pctW(q, p).slice(0, -1), `${q} % von ${p} € = ${d} €`, `${p} € − ${d} € = ${p - d} €`]); }
        case 1: { const tot = pick([20, 25, 50]), k = rnd(1, tot - 1), pc = k * 100 / tot; return W_(`Von ${tot} Schülerinnen und Schülern kommen ${k} mit dem Fahrrad. Wie viel Prozent sind das?`, I(pc), '%', [`Anteil: ${fr(k, tot)}`, `Erweitern auf 100: ${fr(k, tot)} = ${fr(pc, 100)} = ${pc} %`]); }
        case 2: { const v = 10 * rnd(6, 16), h = rnd(2, 5); return W_(`Ein Zug fährt ${v} km pro Stunde. Wie weit kommt er in ${h} Stunden?`, I(v * h), 'km', [`Weg = Tempo · Zeit`, `${v} · ${h} = ${v * h}`]); }
        default: { const c = 5 * rnd(10, 30), a = rnd(2, 5); let b; do b = rnd(3, 9); while (b === a); return W_(`${a} Hefte kosten ${fmtMoney(a * c)} €. Wie viel kosten ${b} Hefte?`, Mo(b * c), '€', [`Dreisatz: erst den Preis für 1 Heft`, `${fmtMoney(a * c)} € : ${a} = ${fmtMoney(c)} €`, `${b} · ${fmtMoney(c)} € = ${fmtMoney(b * c)} €`]); }
      }
    }
    switch (rnd(0, 4)) {
      case 0: { const c = rnd(30, 60), k = rnd(3, 8), z = k * c < 500 ? 5 : 10; return W_(`${n} kauft ${k} Brötchen zu je ${c} Cent und bezahlt mit ${z} €. Wie viel Cent Rückgeld bekommt ${n}?`, I(z * 100 - k * c), 'ct', [`Preis: ${k} · ${c} ct = ${k * c} ct`, `${z} € = ${z * 100} ct`, `${z * 100} − ${k * c} = ${z * 100 - k * c} ct`]); }
      case 1: { const a = rnd(6, 12), m = rnd(3, 4), y = rnd(2, 10); return W_(`${n} ist ${a} Jahre alt. Die Mutter ist ${m}-mal so alt. Wie alt ist die Mutter in ${y} Jahren?`, I(a * m + y), 'Jahre', [`Mutter heute: ${m} · ${a} = ${a * m}`, `In ${y} Jahren: ${a * m} + ${y} = ${a * m + y}`]); }
      case 2: { const v = rnd(5, 9), d = 50 * rnd(3, 12); return W_(`Ein Auto verbraucht ${v} Liter auf 100 km. Wie viele Liter braucht es für ${d} km?`, De(v * d, 100), 'l', [`Für 1 km: ${v} : 100 Liter`, `Für ${d} km: ${v} · ${d} : 100 = ${decOfQ(Q(v * d, 100))}`]); }
      case 3: { const p = 20 * rnd(5, 20), q = pick([10, 20, 25]), r = 5 * rnd(1, 8), up = p * q / 100; return W_(`Ein Fahrrad kostet ${p} €. Zuerst wird es um ${q} % teurer, dann um ${r} € billiger. Was kostet es am Ende?`, I(p + up - r), '€', [`${q} % von ${p} € = ${up} €`, `${p} + ${up} = ${p + up} €`, `${p + up} − ${r} = ${p + up - r} €`]); }
      default: { const b = rnd(2, 8), a = rnd(2, 6), t = b * rnd(1, 4); return W_(`${a} Personen brauchen ${t} Tage, um einen Zaun zu streichen. Wie viele Tage brauchen ${b} Personen?`, I(a * t / b), 'Tage', [`Mehr Personen – weniger Tage (antiproportional).`, `Arbeit insgesamt: ${a} · ${t} = ${a * t} Personentage`, `${a * t} : ${b} = ${fq(a * t, b)}`]); }
    }
  },

  /* ---------- neue Themen (16.11.2026, Marcel: „sehr erweitern mit vielen neuen Aufgaben“) ---------- */
  doppel(l) {
    const dbl = x => ({ e: [T('das Doppelte von'), N(x)], a: I(2 * x), w: dblW(x) });
    const hlf = x => ({ e: [T('die Hälfte von'), N(x)], a: I(x / 2), w: hlfW(x) });
    if (l === 0) return coin() ? dbl(rnd(1, 10)) : hlf(2 * rnd(1, 10));
    if (l === 1) return coin() ? dbl(rnd(11, 50)) : hlf(2 * rnd(6, 50));
    if (l === 2) return coin() ? dbl(rnd(51, 499)) : hlf(pick([2 * rnd(51, 499), 10 * rnd(11, 99)]));
    if (l === 3) {
      if (coin()) { const x = 2 * rnd(5, 49) + 1; return { e: [T('die Hälfte von'), N(x)], a: De(x, 2), w: [`${x} = ${x - 1} + 1`, `Hälfte von ${x - 1} = ${(x - 1) / 2}, Hälfte von 1 = 0,5`, `${(x - 1) / 2} + 0,5 = ${fmtDec(x * 5, 1)}`] }; }
      const i = noTrail(11, 99); return { e: [T('das Doppelte von'), D(i, 1)], a: De(2 * i, 10), w: [`Doppelt so viel: ${fmtDec(i, 1)} + ${fmtDec(i, 1)} = ${fmtDec(2 * i, 1)}`] };
    }
    switch (rnd(0, 3)) {
      case 0: { const x = rnd(12, 99); return { e: [T('das Vierfache von'), N(x)], a: I(4 * x), w: ['Vierfach = zweimal verdoppeln', `${x} → ${2 * x} → ${4 * x}`] }; }
      case 1: { const x = 4 * rnd(15, 99); return { e: [T('ein Viertel von'), N(x)], a: I(x / 4), w: ['Ein Viertel = zweimal halbieren', `${x} → ${x / 2} → ${x / 4}`] }; }
      case 2: { const x = rnd(6, 40); return { e: [T('dreimal verdoppeln:'), N(x)], a: I(8 * x), w: [`${x} → ${2 * x} → ${4 * x} → ${8 * x}`, `Dreimal verdoppeln heißt mal 8.`] }; }
      default: { const x = 8 * rnd(13, 99); return { e: [T('dreimal halbieren:'), N(x)], a: I(x / 8), w: [`${x} → ${x / 2} → ${x / 4} → ${x / 8}`, `Dreimal halbieren heißt geteilt durch 8.`] }; }
    }
  },

  geld(l) {
    const ct = c => [M(c), U('€')];
    if (l === 0) {
      const a = 5 * rnd(2, 12), b = 5 * rnd(2, 10);
      return coin() ? { e: [N(a), U('ct'), O('+'), N(b), U('ct')], a: I(a + b), unit: 'ct', w: addW(a, b) }
        : { e: [N(a + b), U('ct'), O('−'), N(a), U('ct')], a: I(b), unit: 'ct', w: subW(a + b, a) };
    }
    if (l === 1) {
      switch (rnd(0, 2)) {
        case 0: { const e = rnd(1, 9), c = 10 * rnd(1, 9); return { e: [N(e), U('€'), N(c), U('ct')], a: I(e * 100 + c), unit: 'ct', w: [`${e} € = ${e * 100} ct`, `${e * 100} + ${c} = ${e * 100 + c} ct`] }; }
        case 1: { const c = 10 * rnd(11, 99); return { e: [N(c), U('ct')], a: Mo(c), unit: '€', w: ['100 ct = 1 €', `${c} ct = ${fmtMoney(c)} €`] }; }
        default: { const p = rnd(2, 18), z = p < 10 ? 10 : 20; return { e: [Story(`Du kaufst etwas für ${p} € und bezahlst mit einem ${z}-€-Schein. Wie viel Rückgeld bekommst du?`)], a: I(z - p), unit: '€', noEq: true, story: true, w: [`Rückgeld = bezahlt − Preis`, `${z} − ${p} = ${z - p}`] }; }
      }
    }
    if (l === 2) {
      const a = rnd(105, 899), b = rnd(105, 899);
      if (coin()) return { e: [...ct(a), O('+'), ...ct(b)], a: Mo(a + b), unit: '€', w: [`In Cent rechnen: ${a} ct + ${b} ct = ${a + b} ct`, `${a + b} ct = ${fmtMoney(a + b)} €`] };
      const [x, y] = a > b ? [a, b] : [b, a];
      return { e: [...ct(x), O('−'), ...ct(y)], a: Mo(x - y), unit: '€', w: [`In Cent rechnen: ${x} ct − ${y} ct = ${x - y} ct`, `${x - y} ct = ${fmtMoney(x - y)} €`] };
    }
    if (l === 3) {
      switch (rnd(0, 2)) {
        case 0: { const p = rnd(111, 1899), z = p < 500 ? 5 : p < 1000 ? 10 : 20; return { e: [Story(`Du kaufst für ${fmtMoney(p)} € ein und bezahlst mit ${z} €. Wie viel Rückgeld bekommst du?`)], a: Mo(z * 100 - p), unit: '€', noEq: true, story: true, w: [`Ergänze von ${fmtMoney(p)} € bis ${z} €:`, `${z * 100} ct − ${p} ct = ${z * 100 - p} ct = ${fmtMoney(z * 100 - p)} €`] }; }
        case 1: { const c = 5 * rnd(20, 180), k = rnd(2, 6); return { e: [N(k), O('·'), ...ct(c)], a: Mo(k * c), unit: '€', w: [`In Cent: ${k} · ${c} ct = ${k * c} ct`, `${k * c} ct = ${fmtMoney(k * c)} €`] }; }
        default: { const a = rnd(99, 599), b = rnd(99, 599), c = rnd(99, 599); return { e: [...ct(a), O('+'), ...ct(b), O('+'), ...ct(c)], a: Mo(a + b + c), unit: '€', w: [`${a} + ${b} + ${c} = ${a + b + c} ct`, `= ${fmtMoney(a + b + c)} €`] }; }
      }
    }
    switch (rnd(0, 2)) {
      case 0: { const p = 10 * rnd(8, 45); return { e: [Story(`500 g kosten ${fmtMoney(p)} €. Wie viel kostet 1 kg?`)], a: Mo(2 * p), unit: '€', noEq: true, story: true, w: ['1 kg = 2 · 500 g', `2 · ${fmtMoney(p)} € = ${fmtMoney(2 * p)} €`] }; }
      case 1: { const u = 5 * rnd(8, 60), k = 3, m = rnd(4, 9); return { e: [Story(`${k} Stück kosten ${fmtMoney(k * u)} €. Wie viel kosten ${m} Stück?`)], a: Mo(m * u), unit: '€', noEq: true, story: true, w: [`1 Stück: ${fmtMoney(k * u)} € : ${k} = ${fmtMoney(u)} €`, `${m} Stück: ${m} · ${fmtMoney(u)} € = ${fmtMoney(m * u)} €`] }; }
      default: { const g = pick([250, 200, 125]), p = 5 * rnd(10, 60), f = 1000 / g; return { e: [Story(`${g} g Käse kosten ${fmtMoney(p)} €. Wie viel kostet 1 kg?`)], a: Mo(f * p), unit: '€', noEq: true, story: true, w: [`1 kg = 1000 g = ${f} · ${g} g`, `${f} · ${fmtMoney(p)} € = ${fmtMoney(f * p)} €`] }; }
    }
  },

  zeit(l) {
    const hm = m => `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}`;
    const S2 = (story, a, unit, w) => ({ e: [Story(story)], a: I(a), unit, w, noEq: true, story: true });
    if (l === 0) {
      switch (rnd(0, 3)) {
        case 0: { const [n, m] = pick([['eine halbe Stunde', 30], ['eine Viertelstunde', 15], ['eine Dreiviertelstunde', 45], ['eine ganze Stunde', 60]]); return S2(`Wie viele Minuten sind ${n}?`, m, 'min', [`1 Stunde = 60 Minuten`, `${n[0].toUpperCase() + n.slice(1)} = ${m} Minuten`]); }
        case 1: { const a = rnd(1, 8), b = a + rnd(1, 4); return S2(`Von ${a} Uhr bis ${b} Uhr: Wie viele Stunden sind das?`, b - a, 'h', [`${b} − ${a} = ${b - a}`]); }
        case 2: { const w = rnd(2, 4); return S2(`Wie viele Tage haben ${w} Wochen?`, 7 * w, 'Tage', ['1 Woche = 7 Tage', `${w} · 7 = ${7 * w}`]); }
        default: { const a = rnd(6, 10), d = rnd(1, 3); return S2(`Es ist ${a} Uhr. Wie spät ist es in ${d} Stunden?`, a + d, 'Uhr', [`${a} + ${d} = ${a + d}`]); }
      }
    }
    if (l === 1) {
      const h = rnd(7, 17), m1 = 5 * rnd(0, 8), d = 5 * rnd(3, 11), s = h * 60 + m1;
      return S2(`Von ${hm(s)} Uhr bis ${hm(s + d)} Uhr: Wie viele Minuten sind das?`, d, 'min', s % 60 + d > 60 ? [`Bis ${h + 1}:00 Uhr: ${60 - m1} Minuten`, `Dann noch ${(s + d) % 60} Minuten`, `${60 - m1} + ${(s + d) % 60} = ${d}`] : [`${(s + d) % 60 || 60} − ${m1} = ${d}`]);
    }
    if (l === 2) {
      if (coin()) {
        const s = 60 * rnd(7, 18) + 5 * rnd(1, 11), d = 5 * rnd(13, 30), e = s + d;
        return S2(`Von ${hm(s)} Uhr bis ${hm(e)} Uhr: Wie viele Minuten sind das?`, d, 'min', [`Bis zur vollen Stunde ${Math.ceil(s / 60)}:00 Uhr: ${60 - s % 60} min`, `Dann bis ${hm(e)} Uhr: ${e - Math.ceil(s / 60) * 60} min`, `${60 - s % 60} + ${e - Math.ceil(s / 60) * 60} = ${d} min`]);
      }
      const s = 60 * rnd(14, 19) + 5 * rnd(0, 11), d = 5 * rnd(15, 30), e = s + d;
      return S2(`Ein Film beginnt um ${hm(s)} Uhr und dauert ${d} Minuten. Wie viele Minuten nach ${Math.floor(e / 60)} Uhr ist er zu Ende?`, e % 60, 'min', [`${d} min = ${Math.floor(d / 60)} h ${d % 60} min`, `${hm(s)} Uhr + ${d} min = ${hm(e)} Uhr`]);
    }
    if (l === 3) {
      switch (rnd(0, 2)) {
        case 0: { const [mn, len] = pick([['März', 31], ['Mai', 31], ['April', 30], ['Juni', 30], ['Oktober', 31], ['November', 30]]), a = rnd(len - 10, len - 1), b = rnd(2, 12), nx = { März: 'April', Mai: 'Juni', April: 'Mai', Juni: 'Juli', Oktober: 'November', November: 'Dezember' }[mn];
          return S2(`Wie viele Tage sind es vom ${a}. ${mn} bis zum ${b}. ${nx}?`, len - a + b, 'Tage', [`Der ${mn} hat ${len} Tage.`, `Bis zum Monatsende: ${len} − ${a} = ${len - a}`, `${len - a} + ${b} = ${len - a + b}`]); }
        case 1: { const d = rnd(2, 6), h = rnd(1, 23); return S2(`Wie viele Stunden sind ${d} Tage und ${h} Stunden?`, 24 * d + h, 'h', ['1 Tag = 24 Stunden', `${d} · 24 = ${24 * d}`, `${24 * d} + ${h} = ${24 * d + h}`]); }
        default: { const m = rnd(1, 9), s = 5 * rnd(1, 11); return S2(`Wie viele Sekunden sind ${m} Minuten und ${s} Sekunden?`, 60 * m + s, 's', ['1 Minute = 60 Sekunden', `${m} · 60 = ${60 * m}`, `${60 * m} + ${s} = ${60 * m + s}`]); }
      }
    }
    switch (rnd(0, 2)) {
      case 0: { const [i, k, m] = pick([[225, 2, 135], [175, 2, 105], [35, 1, 210], [15, 1, 90], [45, 1, 270], [125, 2, 75]]); return { e: [D(i, k), U('h')], a: I(m), unit: 'min', w: ['1 h = 60 min', `${fmtDec(i, k)} · 60 = ${m}`] }; }
      case 1: { const p = pick([[4, 30], [5, 15], [6, 0], [4, 45], [5, 30]]), km = pick([4, 6, 8, 10]), sec = (p[0] * 60 + p[1]) * km;
        return { ...S2(`Eine Läuferin braucht für 1 km ${p[0]} min ${p[1]} s. Wie viele Minuten braucht sie für ${km} km?`, 0, 'min', [`1 km: ${p[0] * 60 + p[1]} s`, `${km} km: ${km} · ${p[0] * 60 + p[1]} s = ${sec} s`, `${sec} s : 60 = ${decOfQ(Q(sec, 60))} min`]), a: De(sec, 60) }; }
      default: { const v = pick([40, 60, 80, 90, 120]), d = v * pick([1, 2, 3]) / 2 + (v / 4) * rnd(0, 1) * 2; const mins = d / v * 60; return S2(`Ein Zug fährt ${v} km pro Stunde. Wie viele Minuten braucht er für ${d} km?`, mins, 'min', [`In 60 Minuten: ${v} km`, `${d} km sind ${fq(d, v)} von ${v} km`, `${fq(d, v)} · 60 min = ${mins} min`]); }
    }
  },

  geo(l) {
    const S2 = (story, a, unit, w) => ({ e: [Story(story)], a: I(a), unit, w, noEq: true, story: true });
    if (l === 0) {
      if (coin()) { const s = rnd(2, 12); return S2(`Ein Quadrat hat die Seitenlänge ${s} cm. Wie groß ist sein Umfang?`, 4 * s, 'cm', ['Umfang Quadrat = 4 · Seite', `4 · ${s} = ${4 * s}`]); }
      const a = rnd(3, 12), b = rnd(2, 9); return S2(`Ein Rechteck ist ${a} cm lang und ${b} cm breit. Wie groß ist sein Umfang?`, 2 * (a + b), 'cm', ['Umfang = 2 · (Länge + Breite)', `2 · (${a} + ${b}) = ${2 * (a + b)}`]);
    }
    if (l === 1) {
      switch (rnd(0, 2)) {
        case 0: { const s = rnd(2, 12); return S2(`Ein Quadrat hat die Seitenlänge ${s} cm. Wie groß ist seine Fläche?`, s * s, 'cm²', ['Fläche Quadrat = Seite · Seite', `${s} · ${s} = ${s * s}`]); }
        case 1: { const a = rnd(3, 15), b = rnd(2, 10); return S2(`Ein Rechteck ist ${a} m lang und ${b} m breit. Wie groß ist seine Fläche?`, a * b, 'm²', ['Fläche = Länge · Breite', `${a} · ${b} = ${a * b}`]); }
        default: { const s = rnd(3, 15); return S2(`Ein Quadrat hat den Umfang ${4 * s} cm. Wie lang ist eine Seite?`, s, 'cm', ['Ein Quadrat hat 4 gleich lange Seiten.', `${4 * s} : 4 = ${s}`]); }
      }
    }
    if (l === 2) {
      switch (rnd(0, 2)) {
        case 0: { const a = 5 * rnd(6, 16), b = 5 * rnd(4, Math.min(20, 34 - a / 5)); return S2(`In einem Dreieck sind zwei Winkel ${a}° und ${b}° groß. Wie groß ist der dritte Winkel?`, 180 - a - b, '°', ['Alle Winkel im Dreieck zusammen: 180°', `180 − ${a} − ${b} = ${180 - a - b}`]); }
        case 1: { const a = 5 * rnd(4, 32); return S2(`Zwei Nebenwinkel ergeben zusammen 180°. Einer ist ${a}° groß. Wie groß ist der andere?`, 180 - a, '°', [`180 − ${a} = ${180 - a}`]); }
        default: { const b = rnd(3, 12), a = rnd(3, 15); return S2(`Ein Rechteck hat die Fläche ${a * b} cm² und ist ${b} cm breit. Wie lang ist es?`, a, 'cm', ['Länge = Fläche : Breite', `${a * b} : ${b} = ${a}`]); }
      }
    }
    if (l === 3) {
      switch (rnd(0, 3)) {
        case 0: { const g = 2 * rnd(2, 10), h = rnd(3, 12); return S2(`Ein Dreieck hat die Grundseite ${g} cm und die Höhe ${h} cm. Wie groß ist seine Fläche?`, g * h / 2, 'cm²', ['Fläche Dreieck = Grundseite · Höhe : 2', `${g} · ${h} : 2 = ${g * h / 2}`]); }
        case 1: { const a = rnd(2, 8), b = rnd(2, 6), c = rnd(2, 5); return S2(`Ein Quader ist ${a} cm lang, ${b} cm breit und ${c} cm hoch. Wie groß ist sein Volumen?`, a * b * c, 'cm³', ['Volumen = Länge · Breite · Höhe', `${a} · ${b} · ${c} = ${a * b * c}`]); }
        case 2: { const a = 5 * rnd(14, 24), b = 5 * rnd(14, 24), c = 5 * rnd(12, Math.min(26, (360 - a - b - 60) / 5)); return S2(`In einem Viereck sind drei Winkel ${a}°, ${b}° und ${c}° groß. Wie groß ist der vierte Winkel?`, 360 - a - b - c, '°', ['Alle Winkel im Viereck zusammen: 360°', `360 − ${a} − ${b} − ${c} = ${360 - a - b - c}`]); }
        default: { const s = rnd(2, 9); return S2(`Ein Würfel hat die Kantenlänge ${s} cm. Wie groß ist seine Oberfläche?`, 6 * s * s, 'cm²', ['Ein Würfel hat 6 gleiche Quadrate.', `Ein Quadrat: ${s} · ${s} = ${s * s}`, `6 · ${s * s} = ${6 * s * s}`]); }
      }
    }
    switch (rnd(0, 3)) {
      case 0: { const [x, y, z] = pick([[3, 4, 5], [6, 8, 10], [5, 12, 13], [8, 15, 17], [9, 12, 15], [12, 16, 20]]); return S2(`Ein rechtwinkliges Dreieck hat die Katheten ${x} cm und ${y} cm. Wie lang ist die Hypotenuse?`, z, 'cm', ['Satz des Pythagoras: a² + b² = c²', `${x}² + ${y}² = ${x * x} + ${y * y} = ${z * z}`, `√${z * z} = ${z}`]); }
      case 1: { const n = rnd(5, 10); return S2(`Wie groß ist die Summe aller Innenwinkel in einem ${n}-Eck?`, (n - 2) * 180, '°', ['Innenwinkelsumme = (Ecken − 2) · 180°', `(${n} − 2) · 180 = ${(n - 2) * 180}`]); }
      case 2: { const a = rnd(4, 12), c = a + 2 * rnd(1, 4), h = rnd(2, 9); return S2(`Ein Trapez hat die parallelen Seiten ${a} cm und ${c} cm und die Höhe ${h} cm. Wie groß ist seine Fläche?`, (a + c) / 2 * h, 'cm²', ['Fläche Trapez = (a + c) : 2 · h', `(${a} + ${c}) : 2 = ${(a + c) / 2}`, `${(a + c) / 2} · ${h} = ${(a + c) / 2 * h}`]); }
      default: { const s = rnd(2, 10); return S2(`Ein Würfel hat die Kantenlänge ${s} cm. Wie groß ist sein Volumen?`, s ** 3, 'cm³', ['Volumen Würfel = Kante · Kante · Kante', `${s} · ${s} · ${s} = ${s ** 3}`]); }
    }
  },

  folge(l) {
    const mk = (seq, ans, w, gapAt) => {
      const shown = seq.map((v, i) => i === gapAt ? '?' : fi(v)).join(', ') + (gapAt == null ? ', ?' : '');
      return { e: [S_(shown)], a: I(ans), noEq: true, hint: gapAt == null ? 'Welche Zahl kommt als Nächstes?' : 'Welche Zahl fehlt?', w };
    };
    const ar = (s, d, n = 5) => Array.from({ length: n }, (_, i) => s + i * d);
    if (l === 0) { const d = pick([1, 2, 2, 3, 5, 10]), down = coin() && d < 5, s = down ? rnd(4 * d + 1, 20 + 4 * d) : rnd(0, 15), dd = down ? -d : d, q = ar(s, dd, 4); return mk(q, s + 4 * dd, [`Immer ${down ? '−' : '+'} ${d}`, `${q[3]} ${down ? '−' : '+'} ${d} = ${s + 4 * dd}`]); }
    if (l === 1) {
      if (coin()) { const d = rnd(3, 12) * (coin() ? 1 : -1), s = d > 0 ? rnd(1, 50) : rnd(60, 100), q = ar(s, d, 4); return mk(q, s + 4 * d, [`Immer ${d > 0 ? '+' : '−'} ${Math.abs(d)}`, `${q[3]} ${d > 0 ? '+' : '−'} ${Math.abs(d)} = ${s + 4 * d}`]); }
      const s = rnd(1, 6), q = [s, 2 * s, 4 * s, 8 * s]; return mk(q, 16 * s, ['Immer verdoppeln (· 2)', `${8 * s} · 2 = ${16 * s}`]);
    }
    if (l === 2) {
      switch (rnd(0, 2)) {
        case 0: { const a = rnd(3, 9), b = rnd(1, a - 1), s = rnd(1, 20), q = [s, s + a, s + a - b, s + 2 * a - b, s + 2 * a - 2 * b]; return mk(q, s + 3 * a - 2 * b, [`Abwechselnd + ${a} und − ${b}`, `${q[4]} + ${a} = ${s + 3 * a - 2 * b}`]); }
        case 1: { const s = rnd(1, 10), q = [s, s + 1, s + 3, s + 6, s + 10]; return mk(q, s + 15, ['Die Abstände wachsen: + 1, + 2, + 3, + 4, …', `${s + 10} + 5 = ${s + 15}`]); }
        default: { const s = rnd(1, 4), q = [s, 3 * s, 9 * s, 27 * s]; return mk(q, 81 * s, ['Immer mal 3', `${27 * s} · 3 = ${81 * s}`]); }
      }
    }
    if (l === 3) {
      switch (rnd(0, 3)) {
        case 0: { const s = rnd(1, 5), q = [s, s + 1, s + 2, s + 3].map(x => x * x); return mk(q, (s + 4) ** 2, ['Quadratzahlen', `${s + 4} · ${s + 4} = ${(s + 4) ** 2}`]); }
        case 1: { const e = rnd(4, 7), q = [e, e - 1, e - 2, e - 3].map(k => 2 ** k * 3); return mk(q, 3 * 2 ** (e - 4), ['Immer halbieren (: 2)', `${q[3]} : 2 = ${3 * 2 ** (e - 4)}`]); }
        case 2: { let a = rnd(1, 4), b = rnd(a, 6); const q = [a, b]; while (q.length < 6) q.push(q[q.length - 1] + q[q.length - 2]); return mk(q, q[4] + q[5], ['Jede Zahl ist die Summe der beiden davor', `${q[4]} + ${q[5]} = ${q[4] + q[5]}`]); }
        default: { const d = rnd(4, 15), s = rnd(10, 60), q = ar(s, d, 5), g = rnd(1, 3); return mk(q, q[g], [`Immer + ${d}`, `${q[g - 1]} + ${d} = ${q[g]}`], g); }
      }
    }
    switch (rnd(0, 3)) {
      case 0: { const s = rnd(1, 5) * (coin() ? 1 : -1), q = [s, -2 * s, 4 * s, -8 * s]; return mk(q, 16 * s, ['Immer mal (−2)', `${fi(-8 * s)} · (−2) = ${fi(16 * s)}`]); }
      case 1: { const s = rnd(1, 5), q = [s]; while (q.length < 5) q.push(q[q.length - 1] * 2 + 1); return mk(q, q[4] * 2 + 1, ['Immer · 2, dann + 1', `${q[4]} · 2 + 1 = ${q[4] * 2 + 1}`]); }
      case 2: { const d = rnd(3, 9), s = rnd(5, 20), q = ar(s, -d, 6); return mk(q, s - 6 * d, [`Immer − ${d} (auch unter null)`, `${fi(q[5])} − ${d} = ${fi(s - 6 * d)}`]); }
      default: { const a = rnd(2, 4), s = rnd(1, 9), q = [s, s + a, s + 3 * a, s + 6 * a, s + 10 * a]; return mk(q, s + 15 * a, [`Die Abstände wachsen: + ${a}, + ${2 * a}, + ${3 * a}, + ${4 * a}, …`, `${s + 10 * a} + ${5 * a} = ${s + 15 * a}`]); }
    }
  },

  /* ---------- Klasse 5–10 (16.11.2026, Leitstand/Marcel: „sehr viel Inhalt“) ---------- */
  term(l) {
    const tm = (c, v = 'x') => c === 1 ? v : c === -1 ? `−${v}` : `${fi(c)}${v}`;
    const join = cs => cs.map((c, i) => i === 0 ? tm(c) : `${c < 0 ? '−' : '+'} ${tm(Math.abs(c))}`).join(' ');
    if (l === 0) {
      const a = rnd(2, 9), b = rnd(1, 15), x = rnd(2, 10), minus = coin() && a * x > b;
      return { e: [S_(`${a}x ${minus ? '−' : '+'} ${b}`), T(`(x = ${x})`)], hint: 'Setze die Zahl für x ein', a: I(minus ? a * x - b : a * x + b), w: [`x durch ${x} ersetzen: ${a} · ${x} ${minus ? '−' : '+'} ${b}`, `${a * x} ${minus ? '−' : '+'} ${b} = ${minus ? a * x - b : a * x + b}`] };
    }
    if (l === 1) {
      const cs = [rnd(2, 12), rnd(2, 12) * (coin() ? 1 : -1), rnd(1, 9) * (coin() ? 1 : -1)]; const s = cs.reduce((p, c) => p + c, 0);
      if (!s) cs[0]++;
      const sum = cs.reduce((p, c) => p + c, 0);
      return { e: [S_(`${join(cs)} =`)], a: I(sum), unit: 'x', noEq: true, hint: 'Fasse zusammen', w: ['Gleichartige Glieder: nur die Zahlen vor x rechnen', `${cs.map((c, i) => i ? (c < 0 ? `− ${-c}` : `+ ${c}`) : fi(c)).join(' ')} = ${fi(sum)}`, `Also ${tm(sum)}`] };
    }
    if (l === 2) {
      const a = rnd(2, 9) * (coin() ? 1 : -1), b = rnd(1, 12) * (coin() ? 1 : -1);
      return { e: [S_(`${fi(a)} · (x ${b < 0 ? '−' : '+'} ${Math.abs(b)}) = ${tm(a)} +`)], a: I(a * b), noEq: true, hint: 'Klammer ausmultiplizieren', w: [`Jedes Glied in der Klammer mal ${fi(a)}:`, `${fi(a)} · x = ${tm(a)}`, `${fi(a)} · ${par(b)} = ${fi(a * b)}`] };
    }
    if (l === 3) {
      const x = rnd(-5, 5) || 3, a = rnd(1, 4), b = rnd(-6, 6) || 2, c = rnd(-9, 9), v = a * x * x + b * x + c;
      return { e: [S_(`${a === 1 ? '' : a}x² ${b < 0 ? '−' : '+'} ${tm(Math.abs(b)).replace(/^1x$/, 'x')} ${c < 0 ? '−' : '+'} ${Math.abs(c)}`), T(`(x = ${fi(x)})`)], hint: 'Setze die Zahl für x ein', a: I(v), w: [`x² = ${par(x)}² = ${x * x}`, `${a} · ${x * x} ${b < 0 ? '−' : '+'} ${Math.abs(b)} · ${par(x)} ${c < 0 ? '−' : '+'} ${Math.abs(c)}`, `= ${fi(a * x * x)} ${b * x < 0 ? '−' : '+'} ${Math.abs(b * x)} ${c < 0 ? '−' : '+'} ${Math.abs(c)} = ${fi(v)}`] };
    }
    const k = rnd(2, 9);
    switch (rnd(0, 2)) {
      case 0: return { e: [S_(`(x + ${k})² = x² +`), Gap(), S_(`x + ${k * k}`)], a: I(2 * k), noEq: true, hint: '1. binomische Formel', w: ['(a + b)² = a² + 2ab + b²', `2 · x · ${k} = ${2 * k}x`] };
      case 1: return { e: [S_(`(x − ${k})² = x² −`), Gap(), S_(`x + ${k * k}`)], a: I(2 * k), noEq: true, hint: '2. binomische Formel', w: ['(a − b)² = a² − 2ab + b²', `2 · x · ${k} = ${2 * k}x`] };
      default: return { e: [S_(`(x + ${k}) · (x − ${k}) = x² −`)], a: I(k * k), noEq: true, hint: '3. binomische Formel', w: ['(a + b)(a − b) = a² − b²', `${k}² = ${k * k}`] };
    }
  },

  linear(l) {
    const fx = (m, b) => `f(x) = ${m === 1 ? '' : m === -1 ? '−' : fi(m)}x${b ? (b < 0 ? ` − ${-b}` : ` + ${b}`) : ''}`;
    if (l === 0) { const m = rnd(2, 6), b = rnd(0, 9), x = rnd(1, 10); return { e: [S_(fx(m, b)), T(`f(${x}) =`)], a: I(m * x + b), noEq: true, w: [`x = ${x} einsetzen: ${m} · ${x}${b ? ' + ' + b : ''}`, `= ${m * x + b}`] }; }
    if (l === 1) {
      const m = rnd(-5, 5) || 2, b = rnd(-9, 9), x = rnd(-6, 6);
      return coin() ? { e: [S_(fx(m, b)), T(`f(${fi(x)}) =`)], a: I(m * x + b), noEq: true, w: [`x = ${fi(x)} einsetzen: ${fi(m)} · ${par(x)}${b ? (b < 0 ? ' − ' + -b : ' + ' + b) : ''}`, `= ${fi(m * x + b)}`] }
        : { e: [S_(fx(m, b))], a: I(b), noEq: true, pre: 'y =', hint: 'Wo schneidet der Graph die y-Achse?', w: ['Auf der y-Achse ist x = 0.', `f(0) = ${fi(b)}`] };
    }
    if (l === 2) {
      const m = rnd(-4, 4) || 3, x1 = rnd(-3, 3), dx = rnd(1, 4), y1 = rnd(-5, 5), x2 = x1 + dx, y2 = y1 + m * dx;
      return { e: [S_(`P(${fi(x1)}|${fi(y1)}),  Q(${fi(x2)}|${fi(y2)})`)], a: I(m), noEq: true, pre: 'm =', hint: 'Wie groß ist die Steigung der Geraden durch P und Q?', w: ['m = (y₂ − y₁) : (x₂ − x₁)', `(${fi(y2)} − ${par(y1)}) : (${fi(x2)} − ${par(x1)}) = ${fi(y2 - y1)} : ${dx} = ${fi(m)}`] };
    }
    if (l === 3) {
      const m = rnd(1, 6) * (coin() ? 1 : -1), x0 = rnd(-6, 6), b = -m * x0;
      return { e: [S_(fx(m, b))], a: I(x0), noEq: true, pre: 'x =', hint: 'Wo ist die Nullstelle?', w: ['Nullstelle: f(x) = 0', `${fx(m, b).slice(7)} = 0`, `${m === 1 ? '' : fi(m)}x = ${fi(-b)} → x = ${fi(x0)}`] };
    }
    const m1 = rnd(1, 4), m2 = -rnd(1, 4), x0 = rnd(-4, 5), b1 = rnd(-6, 6), b2 = (m1 - m2) * x0 + b1;
    return { e: [S_(`${fx(m1, b1)};  g(x) = ${m2 === -1 ? '−' : fi(m2)}x${b2 ? (b2 < 0 ? ` − ${-b2}` : ` + ${b2}`) : ''}`)], a: I(x0), noEq: true, pre: 'x =', hint: 'Wo schneiden sich die Geraden?', w: ['Gleichsetzen: f(x) = g(x)', `${m1 === 1 ? '' : m1}x${b1 ? (b1 < 0 ? ' − ' + -b1 : ' + ' + b1) : ''} = ${fi(m2)}x${b2 ? (b2 < 0 ? ' − ' + -b2 : ' + ' + b2) : ''}`, `${m1 - m2}x = ${fi(b2 - b1)} → x = ${fi(x0)}`] };
  },

  wahr(l) {
    const F_ = (n, d) => Fr(n, d), S2 = (story, a, w, hint) => ({ e: [Story(story)], a, w, noEq: true, story: true, hint });
    if (l === 0) {
      switch (rnd(0, 2)) {
        case 0: { const z = rnd(1, 6); return S2(`Du würfelst einmal mit einem normalen Würfel. Wie groß ist die Wahrscheinlichkeit für eine ${z}?`, F_(1, 6), ['Ein Würfel hat 6 gleich wahrscheinliche Zahlen.', `Günstig: 1 von 6 → 1/6`], 'als Bruch'); }
        case 1: return S2('Du würfelst einmal. Wie groß ist die Wahrscheinlichkeit für eine gerade Zahl?', F_(1, 2), ['Gerade Zahlen: 2, 4, 6 → 3 von 6', '3/6 = 1/2'], 'als gekürzter Bruch');
        default: return S2('Du wirfst eine Münze. Wie groß ist die Wahrscheinlichkeit für Kopf?', F_(1, 2), ['Zwei Seiten: Kopf oder Zahl', 'Günstig: 1 von 2 → 1/2'], 'als Bruch');
      }
    }
    if (l === 1) {
      const r = rnd(1, 9), b = rnd(1, 9), g = rnd(0, 5), t = r + b + g;
      return S2(`In einer Tüte sind ${r} rote${g ? `, ${b} blaue und ${g} grüne` : ` und ${b} blaue`} Kugeln. Du ziehst blind eine Kugel. Wie groß ist die Wahrscheinlichkeit für Rot?`, F_(r, t), [`Alle Kugeln: ${r} + ${b}${g ? ' + ' + g : ''} = ${t}`, `Günstig: ${r} rote → ${fr(r, t)}`, ...redW(r, t)], 'als gekürzter Bruch');
    }
    if (l === 2) {
      switch (rnd(0, 2)) {
        case 0: { const p = 5 * rnd(1, 19); return S2(`Die Wahrscheinlichkeit für Regen ist ${p} %. Wie groß ist die Wahrscheinlichkeit, dass es nicht regnet (in %)?`, I(100 - p), ['Gegenereignis: 100 % − p', `100 − ${p} = ${100 - p}`]); }
        case 1: { const k = pick([4, 5, 10, 20]), f = rnd(1, k - 1); return { ...S2(`Ein Glücksrad hat ${k} gleich große Felder, ${f} davon sind Gewinne. Wie groß ist die Gewinnchance in Prozent?`, I(f * 100 / k), [`${f} von ${k} = ${fr(f, k)}`, `${fr(f, k)} = ${f * 100 / k} %`]), unit: '%' }; }
        default: { const n = pick([60, 120, 300, 600]); return S2(`Du würfelst ${n}-mal. Wie oft erwartest du ungefähr eine Sechs?`, I(n / 6), ['Wahrscheinlichkeit für eine 6: 1/6', `${n} · 1/6 = ${n / 6}`]); }
      }
    }
    if (l === 3) {
      switch (rnd(0, 3)) {
        case 0: return S2('Du wirfst zwei Münzen. Wie groß ist die Wahrscheinlichkeit, dass beide Kopf zeigen?', F_(1, 4), ['Pfadregel: Wahrscheinlichkeiten entlang des Pfades multiplizieren', '1/2 · 1/2 = 1/4'], 'als Bruch');
        case 1: return S2('Du würfelst zweimal. Wie groß ist die Wahrscheinlichkeit für zwei Sechsen?', F_(1, 36), ['Pfadregel: 1/6 · 1/6', '= 1/36'], 'als Bruch');
        case 2: { const s = pick([[2, 1], [3, 2], [4, 3], [5, 4], [6, 5], [7, 6], [8, 5], [9, 4], [10, 3], [11, 2], [12, 1]]); return S2(`Du würfelst mit zwei Würfeln. Wie groß ist die Wahrscheinlichkeit für die Augensumme ${s[0]}?`, F_(s[1], 36), ['Es gibt 6 · 6 = 36 gleich wahrscheinliche Paare.', `Augensumme ${s[0]}: ${s[1]} Paare → ${fr(s[1], 36)}`, ...redW(s[1], 36)], 'als gekürzter Bruch'); }
        default: { const r = rnd(2, 6), b = rnd(2, 6), t = r + b; return S2(`In einer Urne sind ${r} rote und ${b} blaue Kugeln. Du ziehst zweimal mit Zurücklegen. Wie groß ist die Wahrscheinlichkeit für zweimal Rot?`, F_(r * r, t * t), [`Einmal Rot: ${fr(r, t)}`, `Zweimal (Pfadregel): ${fr(r, t)} · ${fr(r, t)} = ${fr(r * r, t * t)}`, ...redW(r * r, t * t)], 'als gekürzter Bruch'); }
      }
    }
    const r = rnd(2, 6), b = rnd(2, 6), t = r + b;
    return coin() ? S2(`In einer Urne sind ${r} rote und ${b} blaue Kugeln. Du ziehst zwei Kugeln ohne Zurücklegen. Wie groß ist die Wahrscheinlichkeit für zweimal Rot?`, F_(r * (r - 1), t * (t - 1)), [`Erste Kugel rot: ${fr(r, t)}`, `Dann sind noch ${r - 1} rote von ${t - 1} übrig: ${fr(r - 1, t - 1)}`, `${fr(r, t)} · ${fr(r - 1, t - 1)} = ${fr(r * (r - 1), t * (t - 1))}`, ...redW(r * (r - 1), t * (t - 1))], 'als gekürzter Bruch')
      : S2(`In einer Urne sind ${r} rote und ${b} blaue Kugeln. Du ziehst zweimal mit Zurücklegen. Wie groß ist die Wahrscheinlichkeit für mindestens einmal Blau?`, F_(t * t - r * r, t * t), ['Gegenereignis: zweimal Rot', `P(zweimal Rot) = ${fr(r * r, t * t)}`, `1 − ${fr(r * r, t * t)} = ${fr(t * t - r * r, t * t)}`, ...redW(t * t - r * r, t * t)], 'als gekürzter Bruch');
  },

  zins(l) {
    const S2 = (story, a, unit, w) => ({ e: [Story(story)], a, unit, w, noEq: true, story: true });
    if (l === 0) { const k = 100 * rnd(2, 50), p = pick([1, 2, 3, 4, 5]); return S2(`Wie viele Euro Zinsen bringen ${fi(k)} € in einem Jahr bei ${p} % Zinsen?`, I(k * p / 100), '€', ['Zinsen = Kapital · p : 100', `${fi(k)} · ${p} : 100 = ${fi(k * p / 100)}`]); }
    if (l === 1) { const k = 100 * rnd(5, 50), p = rnd(1, 6); return S2(`${fi(k)} € bringen in einem Jahr ${fi(k * p / 100)} € Zinsen. Wie hoch ist der Zinssatz in Prozent?`, I(p), '%', ['p = Zinsen · 100 : Kapital', `${fi(k * p / 100)} · 100 : ${fi(k)} = ${p}`]); }
    if (l === 2) { const k = 100 * rnd(5, 80), p = pick([2, 4, 5]); return S2(`Bei ${p} % Zinsen gibt es in einem Jahr ${fi(k * p / 100)} € Zinsen. Wie groß ist das Kapital?`, I(k), '€', ['Kapital = Zinsen · 100 : p', `${fi(k * p / 100)} · 100 : ${p} = ${fi(k)}`]); }
    if (l === 3) { const k = 1200 * rnd(1, 10), p = pick([1, 2, 3, 5]), m = pick([1, 2, 3, 4, 6, 8, 9]); return S2(`${fi(k)} € liegen ${m} Monate zu ${p} % Jahreszins auf dem Konto. Wie viele Euro Zinsen gibt es?`, I(k * p / 100 * m / 12), '€', [`Jahreszinsen: ${fi(k)} · ${p} : 100 = ${fi(k * p / 100)} €`, `Für ${m} Monate: ${fi(k * p / 100)} · ${m} : 12 = ${fi(k * p / 100 * m / 12)} €`]); }
    const k = 1000 * rnd(1, 10), p = pick([5, 10]), k1 = k * (100 + p) / 100, k2 = k1 * (100 + p) / 100;
    return S2(`${fi(k)} € werden 2 Jahre lang mit ${p} % verzinst, die Zinsen bleiben auf dem Konto (Zinseszins). Wie viel Geld ist es nach 2 Jahren?`, Mo(Math.round(k2 * 100)), '€', [`Nach 1 Jahr: ${fi(k)} · ${fmtDec(100 + p, 2)} = ${fi(k1)} €`, `Nach 2 Jahren: ${fi(k1)} · ${fmtDec(100 + p, 2)} = ${decOfQ(Q(Math.round(k2 * 100), 100))} €`]);
  },

  stat(l) {
    const list = a => a.join(', ');
    if (l === 0) { const n = rnd(3, 5), m = rnd(4, 20); const a = Array.from({ length: n - 1 }, () => m + rnd(-4, 4)); a.push(n * m - a.reduce((p, c) => p + c, 0)); if (a.some(v => v < 0)) return GEN.stat(0); return { e: [T('Mittelwert von'), S_(list(a))], a: I(m), w: ['Mittelwert = Summe : Anzahl', `${a.join(' + ')} = ${n * m}`, `${n * m} : ${n} = ${m}`] }; }
    if (l === 1) { const a = Array.from({ length: pick([5, 7]) }, () => rnd(1, 30)), s = [...a].sort((x, y) => x - y); return { e: [T('Median von'), S_(list(a))], a: I(s[(s.length - 1) / 2]), w: [`Der Größe nach ordnen: ${list(s)}`, `Der mittlere Wert ist ${s[(s.length - 1) / 2]}.`] }; }
    if (l === 2) { const a = Array.from({ length: rnd(5, 7) }, () => rnd(2, 60)), mx = Math.max(...a), mn = Math.min(...a); return { e: [T('Spannweite von'), S_(list(a))], a: I(mx - mn), w: ['Spannweite = größter − kleinster Wert', `${mx} − ${mn} = ${mx - mn}`] }; }
    if (l === 3) { let s; do s = Array.from({ length: pick([4, 6]) }, () => rnd(1, 40)).sort((x, y) => x - y); while ((s[s.length / 2 - 1] + s[s.length / 2]) % 2); const a = shuffle([...s]), med = (s[s.length / 2 - 1] + s[s.length / 2]) / 2; return { e: [T('Median von'), S_(list(a))], a: I(med), w: [`Ordnen: ${list(s)}`, 'Gerade Anzahl: Mittelwert der beiden mittleren Werte', `(${s[s.length / 2 - 1]} + ${s[s.length / 2]}) : 2 = ${med}`] }; }
    const n = 4, m = rnd(6, 15), a = [m + rnd(-3, 3), m + rnd(-3, 3), m + rnd(-3, 3)], x = (n + 1) * m - a.reduce((p, c) => p + c, 0) - (m + 2);
    const have = [...a, m + 2];
    if (x < 1 || x > 30) return GEN.stat(4);
    return { e: [Story(`Die Noten-Punkte von vier Tests sind ${list(have)}. Wie viele Punkte braucht man im fünften Test für einen Durchschnitt von ${m}?`)], a: I(x), noEq: true, story: true, w: [`Summe für Durchschnitt ${m} bei 5 Tests: 5 · ${m} = ${5 * m}`, `Bisher: ${have.join(' + ')} = ${have.reduce((p, c) => p + c, 0)}`, `${5 * m} − ${have.reduce((p, c) => p + c, 0)} = ${x}`] };
  },

  kreis(l) {
    const S2 = (story, a, unit, w) => ({ e: [Story(story)], a, unit, w, noEq: true, story: true, hint: 'Rechne mit π ≈ 3,14' });
    if (l === 0) { const d = rnd(2, 20); return S2(`Ein Kreis hat den Durchmesser ${d} cm. Wie groß ist sein Umfang?`, De(314 * d, 100), 'cm', ['Umfang = π · d', `3,14 · ${d} = ${decOfQ(Q(314 * d, 100))}`]); }
    if (l === 1) { const r = rnd(1, 15); return S2(`Ein Kreis hat den Radius ${r} cm. Wie groß ist sein Umfang?`, De(628 * r, 100), 'cm', ['Umfang = 2 · π · r', `2 · 3,14 · ${r} = ${decOfQ(Q(628 * r, 100))}`]); }
    if (l === 2) { const r = rnd(1, 12); return S2(`Ein Kreis hat den Radius ${r} cm. Wie groß ist seine Fläche?`, De(314 * r * r, 100), 'cm²', ['Fläche = π · r²', `3,14 · ${r}² = 3,14 · ${r * r} = ${decOfQ(Q(314 * r * r, 100))}`]); }
    if (l === 3) { const d = 2 * rnd(1, 10); return S2(`Ein Kreis hat den Durchmesser ${d} m. Wie groß ist seine Fläche?`, De(314 * (d / 2) ** 2, 100), 'm²', [`Radius = Durchmesser : 2 = ${d / 2}`, `Fläche = 3,14 · ${d / 2}² = ${decOfQ(Q(314 * (d / 2) ** 2, 100))}`]); }
    const r = rnd(1, 6), h = rnd(2, 10); return S2(`Ein Zylinder hat den Radius ${r} cm und die Höhe ${h} cm. Wie groß ist sein Volumen?`, De(314 * r * r * h, 100), 'cm³', ['Volumen = π · r² · h', `3,14 · ${r * r} · ${h} = ${decOfQ(Q(314 * r * r * h, 100))}`]);
  },

  zehner(l) {
    const P10 = k => P(10, k);
    if (l === 0) { const k = rnd(2, 6); return { e: [P10(k)], a: I(10 ** k), w: [`10 hoch ${k}: eine 1 mit ${k} Nullen`, `= ${fi(10 ** k)}`] }; }
    if (l === 1) { const a = rnd(2, 9), k = rnd(2, 6); return { e: [N(a), O('·'), P10(k)], a: I(a * 10 ** k), w: [`${a} mit ${k} Nullen`, `= ${fi(a * 10 ** k)}`] }; }
    if (l === 2) { const i = noTrail(11, 99), k = rnd(3, 7); return { e: [D(i, 1), O('·'), P10(k)], a: I(i * 10 ** (k - 1)), w: [`Komma um ${k} Stellen nach rechts`, `${fmtDec(i, 1)} · 10${String(k).split('').map(c => SUPS[c]).join('')} = ${fi(i * 10 ** (k - 1))}`] }; }
    if (l === 3) { const i = noTrail(11, 99), k = rnd(3, 7), v = i * 10 ** (k - 1); return { e: [S_(`${fi(v)} = ${fmtDec(i, 1)} · 10ˣ`)], a: I(k), noEq: true, pre: 'x =', w: [`Das Komma wandert von ${fi(v)} bis ${fmtDec(i, 1)} um ${k} Stellen nach links`, `Also 10${String(k).split('').map(c => SUPS[c]).join('')}`] }; }
    const i = noTrail(11, 99), k = rnd(2, 5), v = fmtDec(i, k + 1);
    return { e: [S_(`${v} = ${fmtDec(i, 1)} · 10ˣ`)], a: I(-k), noEq: true, pre: 'x =', w: [`Das Komma wandert von ${v} bis ${fmtDec(i, 1)} um ${k} Stellen nach rechts`, `Kleine Zahl → negativer Exponent: x = −${k}`] };
  },

  dreisatz(l) {
    const S2 = (story, a, unit, w) => ({ e: [Story(story)], a, unit, w, noEq: true, story: true });
    if (l === 0) { const u = rnd(2, 9), a = rnd(2, 5); let b; do b = rnd(2, 9); while (b === a); return S2(`${a} Hefte kosten ${a * u} €. Wie viel kosten ${b} Hefte?`, I(b * u), '€', [`1 Heft: ${a * u} : ${a} = ${u} €`, `${b} Hefte: ${b} · ${u} = ${b * u} €`]); }
    if (l === 1) { const u = rnd(12, 22), a = rnd(2, 6); let b; do b = rnd(2, 9); while (b === a); return S2(`Ein Auto fährt mit ${a} Litern Benzin ${a * u} km. Wie weit kommt es mit ${b} Litern?`, I(b * u), 'km', [`Mit 1 Liter: ${a * u} : ${a} = ${u} km`, `Mit ${b} Litern: ${b} · ${u} = ${b * u} km`]); }
    if (l === 2) { const p = rnd(2, 6); let q; do q = rnd(2, 10); while (q === p); const W = lcm(p, q) * rnd(1, 4), t = W / p; return S2(`${p} Pumpen leeren ein Becken in ${t} Stunden. Wie lange brauchen ${q} Pumpen?`, I(p * t / q), 'h', ['Mehr Pumpen – weniger Zeit (antiproportional)', `1 Pumpe: ${p} · ${t} = ${p * t} h`, `${q} Pumpen: ${p * t} : ${q} = ${p * t / q} h`]); }
    if (l === 3) { const u = 5 * rnd(3, 40), a = rnd(2, 8); let b; do b = rnd(2, 12); while (b === a); return S2(`${a} kg Äpfel kosten ${fmtMoney(a * u)} €. Wie viel kosten ${b} kg?`, Mo(b * u), '€', [`1 kg: ${fmtMoney(a * u)} € : ${a} = ${fmtMoney(u)} €`, `${b} kg: ${b} · ${fmtMoney(u)} € = ${fmtMoney(b * u)} €`]); }
    const k = rnd(3, 8); let k2; do k2 = rnd(2, 12); while (k2 === k); const W = lcm(k, k2) * rnd(1, 3), d = W / k; return S2(`Ein Futtervorrat reicht für ${k} Pferde ${d} Tage. Wie lange reicht er für ${k2} Pferde?`, I(k * d / k2), 'Tage', ['Mehr Pferde – kürzere Zeit (antiproportional)', `Für 1 Pferd: ${k} · ${d} = ${k * d} Tage`, `Für ${k2} Pferde: ${k * d} : ${k2} = ${k * d / k2} Tage`]);
  },
};
const REAL_TOPICS = Object.keys(GEN);
function makeTask(topic, lvl) {
  const tp = topic === 'mix' ? pick(REAL_TOPICS) : topic;
  const t = GEN[tp](lvl);
  t.topic = tp; t.lvl = lvl;
  return t;
}
const keyOf = t => `${t.topic}|${taskText(t)}`;
