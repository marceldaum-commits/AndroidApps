'use strict';
/* ================= Vorlesen: Aufgaben, Lösungswege, Theorie und Blitz =================
   Stimmen des Geräts bzw. Browsers (Web Speech API). In der Android-Hülle wird – sobald vorhanden – das
   TextToSpeech-Plugin der Hülle benutzt (dieselben Einstellungen). Mathe wird ausgesprochen: „3/4“ → „3 Viertel“, „x²“ → „x hoch 2“, Einheiten als Wörter. */
const SPEECH_FRAC = { de: FRAC_NAMES };
const SPEECH = {
  de: { op: { '·': 'mal', '×': 'mal', ':': 'geteilt durch', '÷': 'geteilt durch', '−': 'minus', '+': 'plus', '=': 'gleich', '≈': 'ungefähr', '≙': 'entspricht', '→': ', also', '<': 'kleiner als', '>': 'größer als' },
    pow: 'hoch', root: 'Wurzel aus', pct: 'Prozent', euro: 'Euro', cent: 'Cent', over: 'durch', half: 'ein Halb', and: 'und', deg: 'Grad',
    units: { 'km/h': 'Kilometer pro Stunde', 'm/s': 'Meter pro Sekunde', 'mm²': 'Quadratmillimeter', 'cm²': 'Quadratzentimeter', 'dm²': 'Quadratdezimeter', 'm²': 'Quadratmeter', 'km²': 'Quadratkilometer',
      'cm³': 'Kubikzentimeter', 'dm³': 'Kubikdezimeter', 'm³': 'Kubikmeter', mm: 'Millimeter', cm: 'Zentimeter', dm: 'Dezimeter', km: 'Kilometer', m: 'Meter', kg: 'Kilogramm', g: 'Gramm', t: 'Tonnen',
      ml: 'Milliliter', l: 'Liter', h: 'Stunden', min: 'Minuten', s: 'Sekunden', ct: 'Cent' } },
  en: { op: { '·': 'times', '×': 'times', ':': 'divided by', '÷': 'divided by', '−': 'minus', '+': 'plus', '=': 'equals', '≈': 'is about', '≙': 'corresponds to', '→': ', so', '<': 'less than', '>': 'greater than' },
    pow: 'to the power of', root: 'the square root of', pct: 'percent', euro: 'euros', cent: 'cents', over: 'over', half: 'one half', and: 'and', deg: 'degrees',
    units: { 'km/h': 'kilometres per hour', 'm/s': 'metres per second', 'mm²': 'square millimetres', 'cm²': 'square centimetres', 'dm²': 'square decimetres', 'm²': 'square metres', 'km²': 'square kilometres',
      'cm³': 'cubic centimetres', 'dm³': 'cubic decimetres', 'm³': 'cubic metres', mm: 'millimetres', cm: 'centimetres', dm: 'decimetres', km: 'kilometres', m: 'metres', kg: 'kilograms', g: 'grams', t: 'tonnes',
      ml: 'millilitres', l: 'litres', h: 'hours', min: 'minutes', s: 'seconds', ct: 'cents' } },
  uk: { op: { '·': 'помножити на', '×': 'помножити на', ':': 'поділити на', '÷': 'поділити на', '−': 'мінус', '+': 'плюс', '=': 'дорівнює', '≈': 'приблизно', '≙': 'відповідає', '→': ', отже', '<': 'менше ніж', '>': 'більше ніж' },
    pow: 'у степені', root: 'корінь з', pct: 'відсотків', euro: 'євро', cent: 'центів', over: 'поділити на', half: 'одна друга', and: 'і', deg: 'градусів',
    units: { 'km/h': 'кілометрів на годину', 'км/год': 'кілометрів на годину', 'м/с': 'метрів за секунду', 'm/s': 'метрів за секунду', 'cm²': 'квадратних сантиметрів', 'м²': 'квадратних метрів', 'm²': 'квадратних метрів', 'см²': 'квадратних сантиметрів',
      'cm³': 'кубічних сантиметрів', 'м³': 'кубічних метрів', 'm³': 'кубічних метрів', mm: 'міліметрів', cm: 'сантиметрів', km: 'кілометрів', m: 'метрів', kg: 'кілограмів', g: 'грамів', t: 'тонн',
      ml: 'мілілітрів', l: 'літрів', h: 'годин', min: 'хвилин', s: 'секунд', ct: 'центів', 'см': 'сантиметрів', 'км': 'кілометрів', 'м': 'метрів', 'кг': 'кілограмів', 'г': 'грамів', 'л': 'літрів', 'год': 'годин', 'хв': 'хвилин', 'с': 'секунд' } },
};
const SUP_DIGITS = { '⁰': '0', '¹': '1', '²': '2', '³': '3', '⁴': '4', '⁵': '5', '⁶': '6', '⁷': '7', '⁸': '8', '⁹': '9', '⁻': '−', 'ˣ': 'x' };
const reEsc = s => s.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');
// Text (so, wie er auf dem Bildschirm steht) in sprechbare Sprache umwandeln
function mathSpeak(text, lang = I18N.lang) {
  const L = SPEECH[lang] || SPEECH.de, sp = ` ${L.op['−'] || 'minus'} `;
  let s = ' ' + String(text).replace(/<[^>]+>/g, ' ').replace(/&nbsp;/g, ' ') + ' ';
  // Einheiten nach Zahlen (längste zuerst), danach Hochzahlen – sonst würde „cm²“ zu „cm hoch 2“
  const units = Object.keys(L.units).sort((a, b) => b.length - a.length);
  s = s.replace(new RegExp(`(\\d|\\)|²|³)\\s?(${units.map(reEsc).join('|')})(?=[\\s.,;:!?)]|$)`, 'g'), (m, d, u) => `${d} ${L.units[u]}`);
  s = s.replace(/(\d)\s?°/g, `$1 ${L.deg}`);
  // Geld: „4,50 €“ und „€4.50“
  s = s.replace(/€\s?(\d[\d.,]*)/g, `$1 ${L.euro}`).replace(/(\d)\s?€/g, `$1 ${L.euro}`).replace(/€/g, L.euro);
  s = s.replace(/(\d)\s?%/g, `$1 ${L.pct}`).replace(/%/g, ` ${L.pct}`);
  // Brüche „3/4“ (nicht in Datumsangaben)
  s = s.replace(/(^|[^\d/])(\d+)\/(\d+)(?![\d/])/g, (m, pre, n, d) => {
    if (lang === 'de') { if (+n === 1 && +d === 2) return pre + L.half; const nm = FRAC_NAMES[+d]; return pre + (nm ? `${n} ${nm}` : `${n} ${L.over} ${d}`); }
    return pre + (+n === 1 && +d === 2 ? L.half : `${n} ${L.over} ${d}`);
  });
  // Hochzahlen: „x²“ → „x hoch 2“, „10⁻³“ → „10 hoch minus 3“
  s = s.replace(/[⁰¹²³⁴⁵⁶⁷⁸⁹⁻ˣ]+/g, m => ` ${L.pow} ${[...m].map(c => SUP_DIGITS[c]).join('').replace('−', sp.trim() + ' ')} `);
  s = s.replace(/√\s?\(/g, `${L.root} (`).replace(/√/g, `${L.root} `);
  // Punkte P(2|8): „P 2 und 8“
  s = s.replace(/\((−?\d[\d,.]*)\|(−?\d[\d,.]*)\)/g, ` $1 ${L.and} $2 `);
  // Rechenzeichen zwischen Zahlen/Klammern/x
  s = s.replace(/([\dx)²³]) ?([·×÷:]) ?(?=[\d(x√−])/g, (m, a, op) => `${a} ${L.op[op]} `);
  s = s.replace(/([·×÷])/g, m => ` ${L.op[m]} `);
  s = s.replace(/ ?([=≈≙→<>]) ?/g, (m, op) => ` ${L.op[op]} `);
  s = s.replace(/(^|[\s(=])−\s?(?=[\dx(])/g, `$1${sp}`).replace(/ − /g, sp);
  s = s.replace(/ \+ /g, ` ${L.op['+']} `);
  s = s.replace(/[()|]/g, ' ').replace(/\?/g, lang === 'de' ? ' wie viel? ' : '?');
  s = s.replace(/\s+/g, ' ').replace(/ ([.,;:!])/g, '$1');
  // Einzahl nach „1“: „1 Stunde“, „1 hour“
  if (lang === 'de') s = s.replace(/(^|[^\d,])1 (Stunde|Minute|Sekunde|Tonne)n\b/g, '$1eine $2');
  if (lang === 'en') s = s.replace(/(^|[^\d.])1 (hour|minute|second|metre|kilometre|centimetre|millimetre|gram|kilogram|tonne|litre|millilitre|euro|cent|degree)s\b/g, '$11 $2');
  return s.trim();
}
// mehrere Sätze ohne doppelte Punkte verbinden
const joinSay = parts => parts.map(x => String(x).trim().replace(/[.!?:]+$/, '')).filter(Boolean).join('. ');
// Deutscher Quelltext → so, wie er gerade angezeigt wird (übersetzt, mit Zahlen-Schreibweise der Sprache)
const shownText = de => I18N.lang === 'de' ? de : (typeof i18nNumbers === 'function' ? i18nNumbers(tr(de)) : tr(de));

const Voice = {
  plugin: () => window.Capacitor?.Plugins?.TextToSpeech || null,
  web: () => 'speechSynthesis' in window ? window.speechSynthesis : null,
  available() { return !!(this.plugin() || this.web()); },
  rate: () => S.settings.rate ?? 1,
  // Stimmen für die aktuelle Sprache: [{ id, name }]
  async voices() {
    const lang = locale().slice(0, 2).toLowerCase();
    const P = this.plugin();
    if (P) { try { const r = await P.getSupportedVoices(); return (r.voices || []).map((v, i) => ({ id: 'n' + i, name: v.name, lang: v.lang })).filter(v => (v.lang || '').toLowerCase().startsWith(lang)); } catch { return []; } }
    const W = this.web(); if (!W) return [];
    let list = W.getVoices();
    if (!list.length) list = await new Promise(r => { const t = setTimeout(() => r(W.getVoices()), 1200); W.addEventListener('voiceschanged', () => { clearTimeout(t); r(W.getVoices()); }, { once: true }); });
    return list.filter(v => v.lang.toLowerCase().replace('_', '-').startsWith(lang)).map(v => ({ id: v.voiceURI, name: v.name + (v.localService ? '' : ' ☁'), lang: v.lang }));
  },
  stop() { const P = this.plugin(); if (P) { try { P.stop(); } catch {} } const W = this.web(); if (W) W.cancel(); },
  // spricht fertigen Sprechtext; queue: hinten anhängen statt unterbrechen
  async speakRaw(text, { queue = false, force = false } = {}) {
    if ((!S.settings.speech && !force) || !text) return;
    const P = this.plugin(), rate = this.rate(), vid = S.settings.voice || '';
    if (P) {
      try { if (!queue) await P.stop();
        const o = { text, lang: locale(), rate, pitch: 1, volume: 1, category: 'ambient', queueStrategy: queue ? 1 : 0 };
        if (vid.startsWith('n')) o.voice = +vid.slice(1);
        await P.speak(o); } catch {}
      return;
    }
    const W = this.web(); if (!W) return;
    if (!queue) W.cancel();
    const u = new SpeechSynthesisUtterance(text); u.lang = locale(); u.rate = rate;
    if (vid) { const v = W.getVoices().find(x => x.voiceURI === vid); if (v) { u.voice = v; u.lang = v.lang; } }
    W.speak(u);
  },
  // deutscher Quelltext (wird übersetzt und mathematisch ausgesprochen)
  say(de, o) { this.speakRaw(mathSpeak(shownText(de)), o); },
};
// alte Schnittstelle: speak(text) für fertige Aufgaben-Sprechtexte
function speak(text, o) { Voice.speakRaw(text, o); }
