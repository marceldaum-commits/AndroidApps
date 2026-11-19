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
    const art = (c) => P.classify(c, pack.lang).kind + (c.satz != null ? 's' : '');
    const gleich = pack.cards.filter((c) => c !== card && art(c) === art(card));
    const quelle = gleich.length >= n ? gleich : pack.cards.filter((c) => c !== card);
    const pool = [...new Set(quelle.map((c) => (feld === 'de' ? P.classify(c, pack.lang).de : c.a)))]
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

  // ------------------------------------------------------------------ Klassenarbeits-Generalprobe
  /** Alle Schreibweisen, die als richtig gelten: „thank you (thanks)“ → thank you | thanks; „got – got/gotten“ → beide Formen. */
  P.akzeptiert = (a) => {
    const out = new Set(); const t = String(a).trim();
    out.add(t.replace(/\s*\([^)]*\)/g, '').trim());
    for (const m of t.matchAll(/\(([^)]+)\)/g)) out.add(m[1].trim());
    if (t.includes('/')) {
      const ganz = t.replace(/\s*\([^)]*\)/g, '');
      const teile = ganz.split(/\s*–\s*|\s*,\s*/);
      const varianten = teile.map((x) => x.split('/').map((y) => y.trim()));
      let combos = [[]];
      for (const v of varianten) combos = combos.flatMap((c) => v.map((y) => c.concat(y)));
      for (const c of combos) out.add(c.join(ganz.includes('–') ? ' – ' : ', '));
    }
    return [...out].filter(Boolean);
  };
  const norm = (t, de) => { let x = String(t).normalize('NFC').replace(/[’`]/g, "'").replace(/\s+/g, ' ').replace(/\s*[–-]\s*/g, ' – ').trim(); if (x.length > 1) x = x.replace(/[.!?]$/, ''); return de ? x : x.toLowerCase(); };
  /** Prüft eine getippte Antwort. Deutsch: Groß/klein zählt. Liefert { ok, fast, hinweis } – „fast“ = nur Akzent anders. */
  P.pruefeTipp = (eingabe, card, lang) => {
    const de = lang === 'de'; const e = norm(eingabe, de);
    if (!e) return { ok: false, hinweis: 'Keine Antwort.' };
    for (const a of P.akzeptiert(card.a)) if (norm(a, de) === e) return { ok: true };
    const oa = (t) => t.normalize('NFD').replace(/[̀-ͯ]/g, '');
    for (const a of P.akzeptiert(card.a)) {
      if (oa(norm(a, de)) === oa(e)) return { ok: false, fast: true, hinweis: 'Fast! Nur der Akzent fehlt oder ist falsch – in der Klassenarbeit zählt er mit.' };
      if (de && norm(a, false) === norm(e, false)) return { ok: false, fast: true, hinweis: 'Fast! Achte auf Groß- und Kleinschreibung.' };
    }
    return { ok: false };
  };
  /** Lernplan bis zum Test: jeden Tag ein Paket (reihum), am letzten Tag die Generalprobe. */
  P.lernplan = (packIds, heute, testTag) => {
    const tage = []; const n = Math.max(0, testTag - heute);
    for (let d = 0; d < n; d++) tage.push({ tag: heute + d, paket: d === n - 1 && n > 1 ? null : packIds[d % packIds.length], probe: d === n - 1 && n > 1 });
    return tage;
  };

  // ------------------------------------------------------------------ Lernpfad „Neu lernen“ (siehe LERNPFAD.md)
  P.LERNPFAD = ['en', 'fr', 'es'];
  P.LEKTIONEN = [
    { t: 'Neue Wörter', e: '🌱', sub: 'Anschauen, hören, erkennen' },
    { t: 'Wörter üben', e: '🎯', sub: 'Selbst sagen und bauen' },
    { t: 'Sätze bauen', e: '🚂', sub: 'Regel entdecken, Sätze legen' },
    { t: 'Hören & Sprechen', e: '🎤', sub: 'Zuhören und nachsprechen', stimme: true },
    { t: 'Geschichte', e: '📖', sub: 'Eine kleine Geschichte verstehen' },
  ];
  /** Lektionen einer Einheit (ohne Stimme fällt „Hören & Sprechen“ weg). */
  P.lektionenFuer = (lang) => P.LEKTIONEN.map((l, nr) => ({ ...l, nr })).filter((l) => !l.stimme || P.LANGS[lang].who);
  /** Karten einer Einheit im Paket-Format: Wörter in beide Richtungen, Sätze in beide Richtungen, Fragen zur Geschichte. */
  P.lernPack = (e, lang) => {
    const L = P.LANGS[lang]; const cards = [];
    e.woerter.forEach(([z, de, img, info], i) => {
      cards.push({ id: 'w' + i + 'v', q: `Was bedeutet „${z}“?`, a: de, img, info, wort: i });
      cards.push({ id: 'w' + i + 'z', q: `Was heißt „${de}“ auf ${L.name}?`, a: z, img, info, wort: i });
    });
    e.saetze.forEach(([z, de, img], i) => {
      cards.push({ id: 's' + i + 'z', q: `Wie sagt man: „${de}“`, a: z, img, satz: i });
      cards.push({ id: 's' + i + 'v', q: `Was bedeutet „${z}“?`, a: de, img, satz: i });
    });
    ((e.geschichte && e.geschichte.fragen) || []).forEach((f, i) => cards.push({ id: 'g' + i, q: f.q, a: f.a, choices: f.choices, img: '📖', frage: i }));
    return { id: 'lp-' + e.id, lang, title: e.titel, emoji: e.emoji, cards, einheit: e };
  };
  /** Satz in Bausteine für den Satz-Zug: Wörter; einzelne Satzzeichen (? ! : ;) hängen am Wort davor. */
  P.zugTeile = (satz) => {
    const out = [];
    for (const t of String(satz).trim().split(/\s+/)) {
      if (/^[?!:;»«]+$/.test(t) && out.length) out[out.length - 1] += ' ' + t; else out.push(t);
    }
    return out;
  };
  const mischen = (a, rnd) => { a = a.slice(); for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(rnd() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  /**
   * Die Schritte einer Lektion. Schritt = { mode, card } | { mode:'memory', cards } | { mode:'neu' } | { mode:'regel' }
   * | { mode:'sprechen', text, de, img } | { mode:'geschichte' }. Spielarten wechseln sich ab, damit es nie eintönig wird.
   */
  P.lektion = (pack, nr, rnd) => {
    rnd = rnd || Math.random;
    const e = pack.einheit; const stimme = !!P.LANGS[pack.lang].who;
    const C = (id) => pack.cards.find((c) => c.id === id);
    const W = e.woerter.map((_, i) => i); const S = e.saetze.map((_, i) => i);
    const steps = [];
    const abwechselnd = (cards, modes) => { let last = ''; for (const c of cards) {
      const ok = modes.filter((m) => P.modes(c, pack.lang).includes(m) || m === 'zug');
      const pick = ok.filter((m) => m !== last); const m = (pick.length ? pick : ok)[Math.floor(rnd() * (pick.length ? pick.length : ok.length))] || 'ballon';
      last = m; steps.push({ mode: m, card: c });
    } };
    if (nr === 0) {
      steps.push({ mode: 'neu' });
      const ws = mischen(W, rnd); const memo = ws.slice(0, 3); const rest = ws.slice(3);
      abwechselnd(rest.slice(0, Math.ceil(rest.length / 2)).map((i) => C('w' + i + 'v')), stimme ? ['ballon', 'hoeren'] : ['ballon']);
      steps.push({ mode: 'memory', cards: memo.map((i) => C('w' + i + 'v')) });
      abwechselnd(rest.slice(Math.ceil(rest.length / 2)).map((i) => C('w' + i + 'v')), stimme ? ['hoeren', 'ballon'] : ['ballon']);
    } else if (nr === 1) {
      abwechselnd(mischen(W, rnd).map((i) => C('w' + i + 'z')), stimme ? ['bauen', 'ballon', 'hoeren'] : ['bauen', 'ballon']);
      abwechselnd(mischen(S, rnd).slice(0, 2).map((i) => C('s' + i + 'v')), ['ballon']);
    } else if (nr === 2) {
      steps.push({ mode: 'regel' });
      const ss = mischen(S, rnd);
      ss.forEach((i, k) => { steps.push({ mode: 'zug', card: C('s' + i + 'z') }); if (k === 1) steps.push({ mode: 'memory', cards: mischen(W, rnd).slice(0, 3).map((w) => C('w' + w + 'z')) }); });
      abwechselnd(mischen(S, rnd).slice(0, 2).map((i) => C('s' + i + 'v')), stimme ? ['hoeren'] : ['ballon']);
    } else if (nr === 3) {
      const ss = mischen(S, rnd); const ws = mischen(W, rnd).slice(0, 2);
      ss.forEach((i, k) => {
        steps.push({ mode: 'sprechen', text: e.saetze[i][0], de: e.saetze[i][1], img: e.saetze[i][2] });
        if (k < 2) steps.push({ mode: 'hoeren', card: C('s' + ss[(k + 2) % ss.length] + 'v') });
      });
      ws.forEach((i) => steps.push({ mode: 'sprechen', text: e.woerter[i][0], de: e.woerter[i][1], img: e.woerter[i][2] }));
    } else {
      steps.push({ mode: 'geschichte' });
      (e.geschichte.fragen || []).forEach((_, i) => steps.push({ mode: 'ballon', card: C('g' + i) }));
      abwechselnd(mischen(S, rnd).slice(0, 2).map((i) => C('s' + i + 'z')), ['zug', 'ballon']);
    }
    return steps;
  };
  /** Alle Texte des Lernpfads, die vertont werden: Wörter, Sätze, Beispiele der Regeln, Geschichten (mit der Stimme der Figur). */
  P.lernSpeech = (lp) => {
    const out = new Map(); const who = P.LANGS[lp.sprache].who; if (!who) return [];
    const add = (w, t) => { if (t) out.set(w + '|' + t, { who: w, text: t }); };
    for (const a of lp.abschnitte) for (const e of a.einheiten) {
      e.woerter.forEach(([z]) => add(who, P.sprechbar(z)));
      e.saetze.forEach(([z]) => add(who, z));
      (e.regel.beispiele || []).forEach(([z]) => add(who, z.includes('→') ? null : z));
      e.geschichte.zeilen.forEach(([f, z]) => add((lp.figuren[f] || {}).who || who, z));
    }
    return [...out.values()];
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
