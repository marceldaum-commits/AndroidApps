// Inhalte der Englisch-App. Reine Daten – geprüft von tools/test-inhalt.mjs.
// Regeln: britisches Englisch; jede Zeile mit deutscher Übersetzung (de); Bilder als Emoji (img).
// Hör-Aufgaben haben genau eine richtige Lösung. Deutsche Erklärungen vollständig, ohne Abkürzungen.
(function (root) {
  const C = {};

  C.characters = {
    ruby: { name: 'Ruby', emoji: '🦜', voice: { pitch: 1.35, rate: 0.9 } },
    mia: { name: 'Mia', emoji: '👧', voice: { pitch: 1.25, rate: 0.85 } },
    sam: { name: 'Sam', emoji: '👦', voice: { pitch: 1.05, rate: 0.85 } },
    ted: { name: 'Grandpa Ted', emoji: '👴', voice: { pitch: 0.8, rate: 0.8 } },
  };

  const colours = [
    { id: 'red', en: 'red', de: 'rot', css: '#e53935' },
    { id: 'blue', en: 'blue', de: 'blau', css: '#1e88e5' },
    { id: 'green', en: 'green', de: 'grün', css: '#43a047' },
    { id: 'yellow', en: 'yellow', de: 'gelb', css: '#fdd835' },
    { id: 'orange', en: 'orange', de: 'orange', css: '#fb8c00' },
    { id: 'purple', en: 'purple', de: 'lila', css: '#8e24aa' },
    { id: 'black', en: 'black', de: 'schwarz', css: '#263238' },
    { id: 'white', en: 'white', de: 'weiß', css: '#ffffff' },
  ];
  C.colours = colours;

  const numbers = ['one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten'];
  const zahlen = ['eins', 'zwei', 'drei', 'vier', 'fünf', 'sechs', 'sieben', 'acht', 'neun', 'zehn'];
  C.numbers = numbers;

  C.worlds = [
    {
      id: 1, title: 'Hello Harbour', de: 'Der Hafen', emoji: '⚓', stufe: 'gs',
      episodes: [
        {
          id: 'w1e1', kann: 'sich begrüßen, mit Namen vorstellen, nach dem Namen fragen und sich verabschieden', title: 'Hello!', de: 'Hallo sagen', emoji: '👋',
          scene: [
            { who: 'ruby', img: '🦜⚓', en: "Hello! I'm Ruby.", de: 'Hallo! Ich bin Ruby.' },
            { who: 'mia', img: '👧👋', en: "Hi, Ruby! I'm Mia.", de: 'Hi, Ruby! Ich bin Mia.' },
            { who: 'sam', img: '👦😃', en: "Hello! My name is Sam.", de: 'Hallo! Ich heiße Sam.' },
            { who: 'mia', img: '👧❓', en: "What's your name?", de: 'Wie heißt du?' },
            { who: 'ruby', img: '🦜🎉', en: 'Nice to meet you!', de: 'Schön, dich kennenzulernen!' },
          ],
          games: [
            {
              type: 'detective', title: 'Hör-Detektiv', rounds: 5,
              items: [
                { en: 'Hello!', de: 'Hallo!', img: '👋😃' },
                { en: 'Goodbye!', de: 'Auf Wiedersehen!', img: '👋🚢' },
                { en: 'Good morning!', de: 'Guten Morgen!', img: '🌅☕' },
                { en: 'Good night!', de: 'Gute Nacht!', img: '🌙🛏️' },
                { en: 'Thank you!', de: 'Danke!', img: '🎁😊' },
              ],
            },
            {
              type: 'echo', title: 'Papagei-Echo',
              lines: [
                { en: 'Hello!', de: 'Hallo!', img: '👋' },
                { en: 'My name is Ruby.', de: 'Ich heiße Ruby.', img: '🦜' },
                { en: "What's your name?", de: 'Wie heißt du?', img: '❓' },
                { en: 'Goodbye!', de: 'Auf Wiedersehen!', img: '🚢' },
              ],
            },
            {
              type: 'train', title: 'Satz-Zug',
              sentences: [
                { en: 'My name is Mia.', de: 'Ich heiße Mia.' },
                { en: "What's your name?", de: 'Wie heißt du?' },
                { en: 'Nice to meet you.', de: 'Schön, dich kennenzulernen.' },
              ],
            },
          ],
          mission: {
            title: 'Sag Mia Hallo', with: 'mia',
            steps: [
              { who: 'mia', en: "Hi! I'm Mia. What's your name?", de: 'Hi! Ich bin Mia. Wie heißt du?',
                options: [
                  { en: 'My name is {name}.', de: 'Ich heiße {name}.', ok: true },
                  { en: 'Goodbye!', de: 'Auf Wiedersehen!', reply: { en: 'Goodbye? But you are new here!', de: 'Auf Wiedersehen? Aber du bist doch neu hier!' } },
                  { en: 'Good night!', de: 'Gute Nacht!', reply: { en: "Good night? It's morning!", de: 'Gute Nacht? Es ist doch Morgen!' } },
                ] },
              { who: 'mia', en: 'Nice to meet you, {name}!', de: 'Schön, dich kennenzulernen, {name}!',
                options: [
                  { en: 'Nice to meet you too!', de: 'Ich freue mich auch!', ok: true },
                  { en: 'My name is Ruby.', de: 'Ich heiße Ruby.', reply: { en: "No, Ruby is the parrot! 😄", de: 'Nein, Ruby ist doch der Papagei! 😄' } },
                ] },
              { who: 'sam', en: 'Hello! I have to go now. Goodbye!', de: 'Hallo! Ich muss jetzt gehen. Auf Wiedersehen!',
                options: [
                  { en: 'Goodbye, Sam!', de: 'Auf Wiedersehen, Sam!', ok: true },
                  { en: 'Good morning, Sam!', de: 'Guten Morgen, Sam!', reply: { en: 'Morning? I am leaving! 👋', de: 'Morgen? Ich gehe doch! 👋' } },
                ] },
            ],
            end: { who: 'mia', en: 'See you soon!', de: 'Bis bald!' },
          },
          explain: {
            title: 'Ruby erklärt: Sich vorstellen',
            points: [
              '„Hello!“ und „Hi!“ heißen beide „Hallo!“. „Hi!“ sagt man eher unter Freunden.',
              'Den eigenen Namen sagst du so: „My name is …“ (Mein Name ist …) oder kürzer „I\'m …“ (Ich bin …). „I\'m“ ist die kurze Form von „I am“.',
              'Nach dem Namen fragen: „What\'s your name?“ (Wie heißt du?). „What\'s“ ist die kurze Form von „What is“.',
              'Zum Abschied sagst du „Goodbye!“ oder „Bye!“, abends „Good night!“.',
              'Aussprache-Tipp: Bei „name“ klingt das a wie „ej“ – also „nejm“.',
            ],
          },
          wg: 'gs-englisch-basics',
        },
        {
          id: 'w1e2', kann: 'bis 10 zählen, nach einer Menge fragen („How many …?“) und höflich etwas bestellen', title: 'Fish and numbers', de: 'Zahlen bis 10', emoji: '🐟',
          scene: [
            { who: 'ted', img: '👴🎣', en: "Hello! I'm Grandpa Ted. I'm a fisherman.", de: 'Hallo! Ich bin Opa Ted. Ich bin Fischer.' },
            { who: 'ted', img: '🐟🐟🐟', en: "Look! One, two, three fish.", de: 'Schau! Ein, zwei, drei Fische.' },
            { who: 'ruby', img: '⛵⛵⛵⛵⛵', en: 'How many boats can you see?', de: 'Wie viele Boote siehst du?' },
            { who: 'mia', img: '👧🖐️', en: 'I can see five boats.', de: 'Ich sehe fünf Boote.' },
            { who: 'ted', img: '👴😄', en: 'Well done!', de: 'Gut gemacht!' },
          ],
          games: [
            {
              type: 'detective', title: 'Hör-Detektiv', rounds: 6, mode: 'count',
              items: numbers.map((n, i) => ({ en: n, de: zahlen[i], count: i + 1 })),
            },
            {
              type: 'rubysays', title: 'Ruby says', rounds: 6, mode: 'count',
              targets: numbers.slice(0, 6).map((n, i) => ({ id: n, count: i + 1, en: n })),
            },
            {
              type: 'echo', title: 'Papagei-Echo',
              lines: [
                { en: 'One, two, three.', de: 'Eins, zwei, drei.', img: '1️⃣2️⃣3️⃣' },
                { en: 'Four, five, six.', de: 'Vier, fünf, sechs.', img: '4️⃣5️⃣6️⃣' },
                { en: 'Seven, eight, nine, ten.', de: 'Sieben, acht, neun, zehn.', img: '7️⃣8️⃣9️⃣🔟' },
                { en: 'How many fish?', de: 'Wie viele Fische?', img: '🐟❓' },
              ],
            },
          ],
          mission: {
            title: 'Fische kaufen bei Grandpa Ted', with: 'ted',
            steps: [
              { who: 'ted', en: 'Good morning! How many fish would you like?', de: 'Guten Morgen! Wie viele Fische möchtest du?',
                choose: 'number',
                options: [
                  { en: '{n} fish, please.', de: '{nFische}, bitte.', ok: true },
                ] },
              { who: 'ted', en: 'Here you are. {n} fish!', de: 'Bitte schön. {nFische}!',
                options: [
                  { en: 'Thank you!', de: 'Danke!', ok: true },
                  { en: 'Good night!', de: 'Gute Nacht!', reply: { en: "Ha ha! It's not night!", de: 'Ha ha! Es ist doch nicht Nacht!' } },
                ] },
              { who: 'ted', en: "You're welcome. Goodbye!", de: 'Gern geschehen. Auf Wiedersehen!',
                options: [
                  { en: 'Goodbye!', de: 'Auf Wiedersehen!', ok: true },
                  { en: 'Hello!', de: 'Hallo!', reply: { en: 'Hello again! 😄 But now: goodbye!', de: 'Hallo nochmal! 😄 Aber jetzt: Tschüss!' } },
                ] },
            ],
            end: { who: 'ted', en: 'Enjoy your fish!', de: 'Lass dir die Fische schmecken!' },
          },
          explain: {
            title: 'Ruby erklärt: Zahlen und Mehrzahl',
            points: [
              'Die Zahlen von 1 bis 10: one, two, three, four, five, six, seven, eight, nine, ten.',
              'Aussprache-Tipps: „two“ klingt wie „tu“, „three“ beginnt mit dem englischen th (Zungenspitze leicht zwischen die Zähne), „eight“ klingt wie „ejt“.',
              'Mehrzahl: Meistens hängt man ein -s an: one boat – two boats. „fish“ bleibt aber gleich: one fish – three fish.',
              'Nach der Menge fragen: „How many …?“ (Wie viele …?), zum Beispiel „How many fish?“.',
              'Höflich bitten: „…, please.“ (… bitte.). Bedanken: „Thank you!“, die Antwort darauf: „You\'re welcome.“ (Gern geschehen.)',
            ],
          },
          wg: 'gs-englisch-farben-zahlen',
        },
        {
          id: 'w1e3', kann: 'acht Farben nennen, nach der Farbe fragen („What colour is …?“) und Dinge beschreiben („The boat is red.“)', title: 'Colourful boats', de: 'Farben', emoji: '🎨',
          scene: [
            { who: 'mia', img: '👧🖌️', en: 'I paint the boats.', de: 'Ich male die Boote an.' },
            { who: 'mia', img: '🟥⛵', en: 'This boat is red.', de: 'Dieses Boot ist rot.' },
            { who: 'sam', img: '🟦⛵', en: 'My boat is blue.', de: 'Mein Boot ist blau.' },
            { who: 'ruby', img: '🌊❓', en: 'What colour is the sea?', de: 'Welche Farbe hat das Meer?' },
            { who: 'mia', img: '🌊🟦', en: 'The sea is blue!', de: 'Das Meer ist blau!' },
          ],
          games: [
            {
              type: 'rubysays', title: 'Ruby says', rounds: 6, mode: 'colour',
              targets: colours.slice(0, 6).map((c) => ({ id: c.id, en: c.en, css: c.css })),
            },
            {
              type: 'train', title: 'Satz-Zug',
              sentences: [
                { en: 'The boat is red.', de: 'Das Boot ist rot.' },
                { en: 'My boat is green.', de: 'Mein Boot ist grün.' },
                { en: 'What colour is the boat?', de: 'Welche Farbe hat das Boot?' },
              ],
            },
            {
              type: 'detective', title: 'Hör-Detektiv', rounds: 6, mode: 'colour',
              items: colours.map((c) => ({ en: c.en, de: c.de, css: c.css })),
            },
          ],
          mission: {
            title: 'Boote anmalen mit Mia', with: 'mia',
            steps: [
              { who: 'mia', en: 'Look at my boat! What colour is it?', de: 'Schau mein Boot an! Welche Farbe hat es?', show: { boat: 'green' },
                options: [
                  { en: "It's green.", de: 'Es ist grün.', ok: true },
                  { en: "It's red.", de: 'Es ist rot.', reply: { en: 'Hmm, look again!', de: 'Hmm, schau noch mal hin!' } },
                  { en: "It's blue.", de: 'Es ist blau.', reply: { en: 'Hmm, look again!', de: 'Hmm, schau noch mal hin!' } },
                ] },
              { who: 'mia', en: 'Now you! What colour is your boat?', de: 'Jetzt du! Welche Farbe hat dein Boot?', choose: 'colour',
                options: [
                  { en: 'My boat is {c}.', de: 'Mein Boot ist {cDe}.', ok: true },
                ] },
              { who: 'mia', en: 'Wow, {aC} boat! I like it!', de: 'Wow, ein Boot in {cDe}! Das gefällt mir!',
                options: [
                  { en: 'Thank you, Mia!', de: 'Danke, Mia!', ok: true },
                  { en: 'Goodbye!', de: 'Auf Wiedersehen!', reply: { en: 'Wait! Say thank you first! 😄', de: 'Warte! Sag erst danke! 😄' } },
                ] },
            ],
            end: { who: 'mia', en: 'Our boats are beautiful!', de: 'Unsere Boote sind wunderschön!' },
          },
          explain: {
            title: 'Ruby erklärt: Farben',
            points: [
              'Die Farben: red (rot), blue (blau), green (grün), yellow (gelb), orange (orange), purple (lila), black (schwarz), white (weiß).',
              'Nach der Farbe fragen: „What colour is …?“ (Welche Farbe hat …?). In britischem Englisch schreibt man „colour“, in amerikanischem „color“.',
              'Antworten: „It\'s green.“ (Es ist grün.) „It\'s“ ist die kurze Form von „It is“.',
              'Im Englischen steht die Farbe vor dem Ding: „a red boat“ (ein rotes Boot) – die Farbe ändert sich dabei nie, egal ob ein Boot oder viele Boote.',
              'Aussprache-Tipp: Bei „purple“ klingt das u wie ein kurzes „ö“.',
            ],
          },
          wg: 'gs-englisch-farben-zahlen',
        },
        { id: 'w1e4', title: 'How are you?', de: 'Wie geht es dir?', emoji: '😊', soon: true },
        { id: 'w1e5', title: 'How old are you?', de: 'Zahlen bis 20', emoji: '🎂', soon: true },
      ],
    },
    { id: 2, title: 'Family House', de: 'Familie, Körper, Kleidung', emoji: '🏠', stufe: 'gs', soon: true },
    { id: 3, title: 'School Street', de: 'Schule und Anweisungen', emoji: '🏫', stufe: 'gs', soon: true },
    { id: 4, title: 'Animal Park', de: 'Tiere, can und can\'t', emoji: '🦁', stufe: 'gs', soon: true },
    { id: 5, title: 'Market Square', de: 'Essen und Einkaufen', emoji: '🧺', stufe: 'gs', soon: true },
    { id: 6, title: 'Festival Fields', de: 'Wetter und Feste', emoji: '🎉', stufe: 'gs', soon: true },
    { id: 7, title: 'London Town', de: 'Weg erklären, there is / there are', emoji: '🚌', stufe: 'us', soon: true },
    { id: 8, title: 'Club Corner', de: 'Hobbys, Simple Present', emoji: '⚽', stufe: 'us', soon: true },
  ];

  C.findEpisode = (id) => {
    for (const w of C.worlds) for (const e of w.episodes || []) if (e.id === id) return { world: w, episode: e };
    return null;
  };

  /** Wörter eines Satzes für den Satz-Zug (Satzzeichen bleiben am Wort). */
  C.words = (sentence) => sentence.split(/\s+/).filter(Boolean);

  /** Name prüfen: 1–15 Buchstaben (auch Umlaute), Leerzeichen und Bindestrich. Liefert Fehlertext oder null. */
  C.checkName = (raw) => {
    const s = String(raw || '').trim();
    if (!s) return 'Schreib deinen Vornamen oder einen Spitznamen.';
    if (s.length > 15) return 'Der Name ist zu lang – höchstens 15 Buchstaben.';
    if (!/^[A-Za-zÄÖÜäöüßÀ-ÿ][A-Za-zÄÖÜäöüßÀ-ÿ \-]*$/.test(s)) return 'Bitte nur Buchstaben verwenden (keine Zahlen oder Zeichen).';
    return null;
  };

  /** Platzhalter in Missions-Texten füllen. */
  C.fill = (text, v) => {
    const col = colours.find((x) => x.id === v.c) || {};
    const map = {
      name: v.name || '…',
      n: v.n ? numbers[v.n - 1] : '',
      nFische: v.n ? (v.n === 1 ? 'Ein Fisch' : zahlen[v.n - 1].replace(/^./, (m) => m.toUpperCase()) + ' Fische') : '',
      c: col.en || '',
      cDe: col.de || '',
      aC: col.en ? (/^[aeiou]/.test(col.en) ? 'an ' : 'a ') + col.en : '',
    };
    // Am Satzanfang (Textbeginn oder nach . ! ?) wird groß geschrieben.
    return String(text).replace(/\{(\w+)\}/g, (m, k, off, all) => {
      let val = k in map ? map[k] : m;
      if (/(^|[.!?]\s+)$/.test(all.slice(0, off))) val = val.replace(/^./, (x) => x.toUpperCase());
      return val;
    });
  };

  /** Ansage im Spiel „Ruby says“ (eine Quelle für App und Vertonung). */
  C.rubySaysText = (mode, target, cmd) => {
    const core = mode === 'count' ? `touch ${target.en} fish.` : `touch the ${target.en} boat.`;
    return cmd ? 'Ruby says: ' + core : core.charAt(0).toUpperCase() + core.slice(1);
  };

  /** Ein einzelnes Wort zum Antippen in der Szene (ohne Satzzeichen am Rand). */
  C.tapWord = (w) => String(w).replace(/^[^\w']+|[^\w']+$/g, '');

  /**
   * Alle festen Sätze, die gesprochen werden – mit Sprecher. Grundlage für die Vertonung (tools/vertonen.py)
   * und für den Test, dass jede Ansage eine Datei hat. Sätze mit dem Namen des Kindes sind nicht dabei
   * (die liest die Gerätestimme).
   */
  C.allSpeech = () => {
    const out = new Map();
    const add = (who, text) => { if (text && !/\{name\}|…/.test(text)) out.set(who + '|' + text, { who, text }); };
    const fills = (t) => {
      if (/\{n\}|\{nFische\}/.test(t)) return numbers.map((x, i) => C.fill(t, { n: i + 1 }));
      if (/\{c\}|\{aC\}|\{cDe\}/.test(t)) return colours.map((c) => C.fill(t, { c: c.id }));
      return [t];
    };
    for (const n of numbers) add('ruby', n);
    for (const c of colours) add('ruby', c.en);
    for (const w of C.worlds) for (const e of w.episodes || []) {
      if (e.soon) continue;
      for (const s of e.scene) { add(s.who, s.en); for (const x of s.en.split(/\s+/)) add(s.who, C.tapWord(x)); }
      for (const g of e.games) {
        for (const it of g.items || []) add('ruby', it.en);
        for (const l of g.lines || []) add('ruby', l.en);
        for (const l of g.sentences || []) add('ruby', l.en);
        for (const t of g.targets || []) { add('ruby', C.rubySaysText(g.mode, t, true)); add('ruby', C.rubySaysText(g.mode, t, false)); }
      }
      const m = e.mission;
      for (const st of m.steps) {
        for (const t of fills(st.en)) add(st.who, t);
        for (const o of st.options) {
          for (const t of fills(o.en)) add('ruby', t);          // „🔊 Anhören“ beim Nachsprechen
          if (o.reply) add(st.who, o.reply.en);
        }
      }
      for (const t of fills(m.end.en)) add(m.end.who, t);
    }
    return [...out.values()];
  };

  C.zahlen = zahlen;
  root.CONTENT = C;
  if (typeof module !== 'undefined') module.exports = C;
})(typeof window !== 'undefined' ? window : globalThis);
