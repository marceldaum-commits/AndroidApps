// Übernimmt die Sprachpakete aus dem Wissensgarten (Entscheidung Marcel 19.11.2026: Sprachen ziehen nach Quasselquest um).
// Aufruf: node tools/pakete-uebernehmen.mjs   → webapp/pakete/<id>.json (unverändert) + webapp/pakete/index.json
// Prüft dabei: Format, eindeutige Karten-IDs, genau eine richtige Antwort, keine Namen von Schulbüchern (Urheberrecht).
import { readFileSync, writeFileSync, mkdirSync, readdirSync, unlinkSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const QUELLE = fileURLToPath(new URL('../../wissensgarten/content/src/main/assets/packs/', import.meta.url));
const ZIEL = fileURLToPath(new URL('../webapp/pakete/', import.meta.url));

// Deutsch: nur der Sprach-Teil (Rechtschreibung, Grammatik, Wortschatz). Literatur, Epochen, Stilmittel bleiben im Wissensgarten.
export const PAKETE = {
  en: ['gs-englisch-farben-zahlen', 'gs-englisch-basics', 'gs-englisch-familie-koerper', 's1-englisch-grundwortschatz', 's1-englisch-alltag', 's1-englisch-irregular-verbs', 's1-englisch-tenses'],
  de: ['gs-lesestart', 'gs-wortarten', 'gs-satzzeichen-satzarten', 'gs-rechtschreibung', 's1-deutsch-grammatik', 's1-deutsch-zeichensetzung', 's1-deutsch-fremdwoerter', 'aw-redewendungen'],
  fr: ['s1-franzoesisch-grundwortschatz', 's1-franzoesisch-verben'],
  la: ['s1-latein-grundwortschatz'],
  es: ['s1-spanisch-grundwortschatz'],
};
// Titel von Lehrwerken – dürfen nicht vorkommen (keine Listen aus Schulbüchern übernehmen)
const LEHRWERKE = /green line|camden town|english g\b|lighthouse|highlight\b|access\b|découvertes|decouvertes|à plus|a plus!|tous ensemble|cursus|prima\b|campus\b|pontes|felix\b|adelante|encuentros|vía rápida|via rapida/i;  // „¡Vamos!“ allein ist ein normales Wort, kein Hinweis

const fehler = [];
mkdirSync(ZIEL, { recursive: true });
for (const f of readdirSync(ZIEL)) if (f.endsWith('.json')) unlinkSync(ZIEL + f);
const index = { format: 1, quelle: 'wissensgarten', pakete: [] };
for (const [sprache, ids] of Object.entries(PAKETE)) {
  for (const id of ids) {
    const raw = readFileSync(QUELLE + id + '.json', 'utf8');
    const p = JSON.parse(raw);
    if (LEHRWERKE.test(raw)) fehler.push(`${id}: enthält den Namen eines Lehrwerks`);
    const seen = new Set();
    for (const c of p.cards) {
      if (seen.has(c.id)) fehler.push(`${id}/${c.id}: doppelte Karten-ID`); seen.add(c.id);
      if (!c.q || !c.a) fehler.push(`${id}/${c.id}: Frage oder Antwort fehlt`);
      // Groß/klein zählt (Rechtschreibung!)
      if (c.choices && c.choices.some((x) => x.trim() === c.a.trim())) fehler.push(`${id}/${c.id}: Antwort steht auch bei den falschen`);
    }
    writeFileSync(ZIEL + id + '.json', raw);
    index.pakete.push({ id, sprache, version: p.version, title: p.title, emoji: p.emoji, grades: p.grades, description: p.description, karten: p.cards.length });
  }
}
writeFileSync(ZIEL + 'index.json', JSON.stringify(index, null, 1));
if (fehler.length) { console.error(fehler.join('\n')); process.exit(1); }
console.log(`${index.pakete.length} Pakete, ${index.pakete.reduce((s, p) => s + p.karten, 0)} Karten übernommen – keine Lehrwerk-Namen gefunden.`);
