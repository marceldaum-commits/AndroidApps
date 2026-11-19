// Sprachen, Wortschatz- und Regel-Pakete (Format des Wissensgartens) und Wiederholung in wachsenden Abständen.
// Reine Logik ohne Oberfläche – wird von der App, der Vertonung (tools/vertonen.py) und den Tests benutzt.
(function (root) {
  const P = {};

  // Sprachen der App. who = Stimme der Vertonung (null = keine freie Stimme vorhanden → nur lesen).
  // Emojis statt Flaggen (Flaggen fehlen auf Windows).
  P.LANGS = {
    en: { name: 'Englisch', emoji: '💂', who: 'ruby', thema: 'englisch', hallo: 'Hello!' },
    de: { name: 'Deutsch', emoji: '🥨', who: 'de', thema: 'deutsch', hallo: 'Hallo!' },
    fr: { name: 'Französisch', emoji: '🥐', who: 'fr', thema: 'franzoesisch', hallo: 'Bonjour !' },
    es: { name: 'Spanisch', emoji: '💃', who: 'es', thema: 'spanisch', hallo: '¡Hola!' },
    la: { name: 'Latein', emoji: '🏛️', who: null, thema: 'latein', hallo: 'Salve!' },
  };
  P.ORDER = ['en', 'de', 'fr', 'es', 'la'];

  const ZIELSPRACHE = '(?:Englisch|Französisch|Spanisch|Latein)';
  const RE_ZU = new RegExp('^Was heißt „(.+?)“(?: \\((.+?)\\))? auf ' + ZIELSPRACHE + '\\?$');
  const RE_SAGT = /^Wie (?:sagt|fragt) man[^:„]*:\s*„(.+?)“\??$/;
  const RE_BEDEUTET = /^Was bedeutet „(.+?)“\??$/;
  const RE_FORMEN = /^Simple Past und Past Participle von „(?:to )?(.+?)“\?$/;
  const RE_VERB = /^(?:Präsens von|Zu welchem Verb gehört|Presente de|Wie lautet)/;

  /** Gesprochene Form einer Antwort: Klammer-Zusätze weg, „/“ und „–“ als Pausen. */
  P.sprechbar = (t) => String(t).replace(/\s*\([^)]*\)/g, '').replace(/\s*\/\s*/g, ' or ').replace(/\s+–\s+/g, ', ').trim();

  /**
   * Was für eine Karte ist das? kind: zu (Deutsch → Zielsprache), von (Zielsprache → Deutsch), luecke (Lückensatz),
   * formen (Verbformen), frage (sonst). sprich = was nach der richtigen Antwort in der Zielsprache vorgelesen wird,
   * vorher = was vor der Antwort vorgelesen wird (Hörspiel).
   */
  P.classify = (card, lang) => {
    const q = card.q; const a = card.a;
    let m;
    if ((m = RE_ZU.exec(q)) || (m = RE_SAGT.exec(q))) return { kind: 'zu', de: m[1], ziel: a, sprich: P.sprechbar(a) };
    if ((m = RE_BEDEUTET.exec(q))) return { kind: 'von', de: a, ziel: m[1], sprich: P.sprechbar(m[1]), vorher: P.sprechbar(m[1]) };
    if ((m = RE_FORMEN.exec(q))) return { kind: 'formen', sprich: m[1] + ', ' + P.sprechbar(a) };
    if (RE_VERB.test(q) && lang !== 'de') return { kind: 'formen', sprich: P.sprechbar(a) };
    if (q.includes('___')) {
      const satz = q.replace(/\s*\([^)]*\)/, '').replace('___', a);
      return { kind: 'luecke', sprich: lang === 'de' ? null : satz };
    }
    if (lang === 'de') return { kind: 'frage', sprich: /^[\p{L} ,.!?-]{2,40}$/u.test(a) ? P.sprechbar(a) : null };
    return { kind: 'frage', sprich: null };
  };

  /** Alle Sätze aus Paketen, die vertont werden sollen: [{who, text}] */
  P.speech = (pakete) => {
    const out = new Map();
    for (const p of pakete) {
      const who = P.LANGS[p.lang] && P.LANGS[p.lang].who;
      if (!who) continue;
      for (const c of p.cards) {
        const k = P.classify(c, p.lang);
        for (const t of [k.sprich, k.vorher]) if (t) out.set(who + '|' + t, { who, text: t });
      }
    }
    return [...out.values()];
  };

  // ------------------------------------------------------------------ Spielarten je Karte
  const EINZELWORT = /^[\p{L}'’-]{2,10}$/u;
  /** Welche Spielarten passen zu dieser Karte? */
  P.modes = (card, lang) => {
    const k = P.classify(card, lang);
    const m = ['ballon'];
    if (k.kind === 'zu' && P.LANGS[lang].who) m.push('hoeren');
    if (P.bauteile(card, lang)) m.push('bauen');
    return m;
  };

  const ARTIKEL = /^(the|a|an|to|le|la|les|l'|un|une|el|los|las|der|die|das|ein|eine) (.+)$/i;
  /** Für das Buchstaben-Bauen: { vor: fester Teil (Artikel), wort: zu bauendes Wort } oder null. */
  P.bauteile = (card, lang) => {
    const k = P.classify(card, lang);
    let t = null;
    if (k.kind === 'zu') t = card.a;
    else if (k.kind === 'von' && EINZELWORT.test(card.a.trim())) t = card.a;
    else if (lang === 'de' && k.kind === 'frage') t = card.a;
    if (!t) return null;
    t = t.trim();
    if (EINZELWORT.test(t)) return { vor: '', wort: t };
    const m = ARTIKEL.exec(t);
    if (m && EINZELWORT.test(m[2])) return { vor: m[1] + ' ', wort: m[2] };
    return null;
  };

  /** Falsche Antworten: aus der Karte, sonst aus anderen Karten desselben Pakets (gleiche Art). */
  P.wrongs = (card, pack, n, feld) => {
    feld = feld || 'a';
    if (feld === 'a' && card.choices && card.choices.length >= n) return card.choices.slice(0, n);
    const own = feld === 'a' ? (card.choices || []) : [];
    const want = feld === 'de' ? P.classify(card, pack.lang).de : card.a;
    const pool = pack.cards.filter((c) => c !== card).map((c) => (feld === 'de' ? P.classify(c, pack.lang).de : c.a))
      .filter((x) => x && x !== want && !own.includes(x));
    const out = own.slice();
    while (out.length < n && pool.length) out.push(pool.splice(Math.floor(Math.random() * pool.length), 1)[0]);
    return out;
  };

  // ------------------------------------------------------------------ Wiederholung (Leitner-Kästen, wachsende Abstände)
  P.ABSTAND = [0, 1, 2, 4, 8, 16, 32];   // Tage bis zur nächsten Wiederholung je Kasten
  P.today = () => Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 864e5);
  P.key = (pack, card) => pack.id + '#' + card.id;
  /** Ergebnis eintragen: beim ersten Versuch richtig → ein Kasten weiter, sonst zurück in Kasten 1. */
  P.note = (store, pack, card, firstTry, day) => {
    day = day == null ? P.today() : day;
    const k = P.key(pack, card); const e = store[k] || { b: 0, d: day };
    e.b = firstTry ? Math.min(e.b + 1, P.ABSTAND.length - 1) : 1;
    e.d = day + P.ABSTAND[e.b];
    store[k] = e;
    return e;
  };
  /** Karten für eine Runde: fällige zuerst, dann höchstens `neu` neue, dann die am längsten nicht gesehenen. */
  P.pick = (store, pack, n, neu, day) => {
    day = day == null ? P.today() : day;
    const st = (c) => store[P.key(pack, c)];
    const faellig = pack.cards.filter((c) => st(c) && st(c).d <= day).sort((x, y) => st(x).d - st(y).d);
    const frisch = pack.cards.filter((c) => !st(c)).slice(0, neu);
    const rest = pack.cards.filter((c) => st(c) && st(c).d > day).sort((x, y) => st(x).d - st(y).d);
    return faellig.concat(frisch, rest).slice(0, n);
  };
  /** Wie viele Karten sitzen (Kasten 3 oder höher)? */
  P.mastered = (store, pack) => pack.cards.filter((c) => (store[P.key(pack, c)] || {}).b >= 3).length;

  root.PAKETE = P;
  if (typeof module !== 'undefined') module.exports = P;
})(typeof window !== 'undefined' ? window : globalThis);
