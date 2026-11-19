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
    const q = card.q || ''; const a = card.a;
    let m;
    if ((m = RE_ZU.exec(q)) || (m = RE_SAGT.exec(q))) return { kind: 'zu', de: m[1], ziel: a, sprich: P.sprechbar(a) };
    if ((m = RE_BEDEUTET.exec(q))) return { kind: 'von', de: a, ziel: m[1], sprich: P.sprechbar(m[1]), vorher: P.sprechbar(m[1]) };
    if ((m = RE_FORMEN.exec(q))) return { kind: 'formen', sprich: m[1] + ', ' + P.sprechbar(a) };
    if (RE_VERB.test(q) && lang !== 'de') return { kind: 'formen', sprich: P.sprechbar(a) };
    if (q.includes('___')) {
      const satz = q.replace(/\s*\([^)]*\)/, '').replace('___', a);
      return { kind: 'luecke', sprich: lang === 'de' ? null : satz };
    }
    if (card.diktat) return { kind: 'diktat', sprich: a, vorher: a };
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
    if (card.diktat) return ['bauen'];
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
    else if ((lang === 'de' && k.kind === 'frage') || k.kind === 'diktat') t = card.a;
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

  // ------------------------------------------------------------------ „Warum?“ – Fehlerstelle und Regel
  /** Teilt die falsche Antwort in [gleich, anders, gleich]: gemeinsamer Anfang und gemeinsames Ende bleiben, die Mitte ist die Fehlerstelle. */
  P.diff = (wrong, right) => {
    const a = [...String(wrong)]; const b = [...String(right)];
    let i = 0; while (i < a.length && i < b.length && a[i] === b[i]) i++;
    let j = 0; while (j < a.length - i && j < b.length - i && a[a.length - 1 - j] === b[b.length - 1 - j]) j++;
    return { vor: a.slice(0, i).join(''), fehler: a.slice(i, a.length - j).join(''), nach: a.slice(a.length - j).join(''), fehlt: !a.slice(i, a.length - j).length };
  };
  const ohneAkzent = (t) => t.normalize('NFD').replace(/[̀-ͯ]/g, '');
  const ARTIKEL_SET = /^(der|die|das|ein|eine|le|la|les|l'|un|une|el|los|las|the|a|an)$/i;
  /** Eine Regel in einem Kindersatz: zuerst die Erklärung der Karte, sonst eine passende allgemeine Regel. */
  P.regel = (card, lang, wrong) => {
    const right = card.a; const w = String(wrong);
    if (w.toLowerCase() === right.toLowerCase()) {
      if (lang === 'de') return /^[A-ZÄÖÜ]/.test(right) ? 'Groß oder klein? Nomen und Satzanfänge schreibt man groß.' : 'Groß oder klein? Hier schreibt man klein.';
      return 'Groß oder klein – darauf kommt es hier an.';
    }
    if (ohneAkzent(w) === ohneAkzent(right) && w !== right) return 'Achte auf das kleine Zeichen über dem Buchstaben (Akzent) – é, è und ê klingen verschieden.';
    if (w.replace(/[.,!?;:]/g, '') === right.replace(/[.,!?;:]/g, '')) return 'Schau auf die Satzzeichen: Punkt, Komma, Frage- oder Ausrufezeichen.';
    const wa = w.split(' '); const ra = right.split(' ');
    if (wa.length > 1 && wa.length === ra.length && ARTIKEL_SET.test(wa[0]) && wa[0] !== ra[0] && wa.slice(1).join(' ') === ra.slice(1).join(' '))
      return 'Das Wort stimmt, aber der Begleiter (Artikel) nicht. Den lernt man am besten gleich mit.';
    const kind = P.classify(card, lang).kind;
    // Bei Vokabeln passt die Erklärung der Karte oft zu einer anderen falschen Antwort – nur zeigen, wenn sie das gewählte Wort nennt
    if (card.info && (!['zu', 'von'].includes(kind) || card.info.toLowerCase().includes(w.toLowerCase()))) return card.info;
    if (['zu', 'von'].includes(kind)) return 'Das ist ein anderes Wort. Schau dir das Bild an und lies die Buchstaben ganz genau.';
    return 'Vergleich die markierte Stelle mit dem, was du gehört oder gelernt hast.';
  };

  // ------------------------------------------------------------------ „Mein Schulbuch“: eigene Wortlisten
  P.MAX_WOERTER = 120;
  /**
   * Liest eine eingetippte Liste. Fremdsprachen: eine Zeile „deutsch = fremd“ (auch „-“, „;“, Tab).
   * Deutsch: ein Lernwort pro Zeile (wird als Diktat gespielt). Liefert { words, fehler[] } mit verständlichen Meldungen.
   */
  P.parseListe = (text, lang) => {
    const words = []; const fehler = []; const seen = new Set();
    const zeilen = String(text || '').split(/\r?\n/);
    zeilen.forEach((roh, i) => {
      const z = roh.trim(); if (!z) return;
      const nr = `Zeile ${i + 1}`;
      let de, ziel;
      if (lang === 'de') {
        ziel = z.replace(/\s+/g, ' ');
        if (/[=;\t]/.test(ziel)) { fehler.push(`${nr}: Für Deutsch bitte nur das Wort schreiben, ohne „=“.`); return; }
        de = ziel;
      } else {
        const m = /^(.+?)\s*(?:=|;|\t|\s-\s|\s–\s)\s*(.+)$/.exec(z);
        if (!m) { fehler.push(`${nr}: Bitte so schreiben: Hund = ${{ en: 'dog', fr: 'le chien', es: 'el perro', la: 'canis' }[lang] || 'dog'}`); return; }
        de = m[1].trim(); ziel = m[2].trim();
      }
      if (de.length > 60 || ziel.length > 60) { fehler.push(`${nr}: Das ist zu lang – höchstens 60 Zeichen.`); return; }
      if (!/\p{L}/u.test(ziel) || !/\p{L}/u.test(de)) { fehler.push(`${nr}: Da fehlt ein Wort.`); return; }
      const k = (de + '|' + ziel).toLowerCase();
      if (seen.has(k)) { fehler.push(`${nr}: „${ziel}“ steht schon in der Liste.`); return; }
      seen.add(k); words.push({ de, ziel });
    });
    if (words.length > P.MAX_WOERTER) fehler.push(`Höchstens ${P.MAX_WOERTER} Wörter pro Liste – teil sie lieber in Lektionen auf.`);
    if (!words.length && !fehler.length) fehler.push('Die Liste ist noch leer.');
    return { words: words.slice(0, P.MAX_WOERTER), fehler };
  };
  /** Kurze, stabile Kennung je Wort (damit die Wiederholung beim Bearbeiten erhalten bleibt). */
  P.wortId = (w) => { let h = 0; const t = (w.de + '|' + w.ziel).toLowerCase(); for (let i = 0; i < t.length; i++) h = (h * 31 + t.charCodeAt(i)) >>> 0; return 'w' + h.toString(36); };
  /** Macht aus einer eigenen Liste ein Paket im gewohnten Format. */
  P.listePack = (liste, lang) => ({
    id: 'eigen-' + liste.id, lang, title: liste.name, emoji: '📒', eigen: true,
    description: 'Deine eigene Liste', grades: [],
    cards: liste.words.map((w) => (lang === 'de'
      ? { id: P.wortId(w), q: 'Hör zu und bau das Wort!', a: w.ziel, img: '📒', diktat: true }
      : { id: P.wortId(w), q: `Was heißt „${w.de}“ auf ${P.LANGS[lang].name}?`, a: w.ziel, img: '📒' })),
  });

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
