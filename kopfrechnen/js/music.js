'use strict';
/* ================= Rechenblitz-Musik: selbst komponiert, im Code erzeugt (Web Audio, keine Samples) =================
   Klang-Identität: hell, schnell, Chiptune. Erkennungsmotiv „Blitz-Zickzack“: E – H – A – E (hoch, runter, hoch – wie ein Blitz).
   Vier Stücke, alle mit Grundton E: Menü (E-Dur, 104 BPM), Spiel (E-Moll, 126), Blitzrunde (E-Moll, 152), Dungeon (E-Phrygisch, 108).
   Jede Schleife ändert sich: Melodie aus Rhythmus-Bausteinen mit Zufall, Pausen-Takte, Breakdown ohne Schlagzeug, ab und zu eine Pause.
   Es wird nichts geladen – die Töne entstehen erst, wenn Musik läuft. Start nie vor der ersten Berührung. */
const MOTIF = [0, 7, 5, 12];   // Blitz-Zickzack, Halbtöne über dem Grundton
const TRACKS = {
  //            Akkorde: [Grundton (MIDI, Bass-Lage), Terz-Abstand 4 = Dur / 3 = Moll]
  menu:    { bpm: 104, root: 64, scale: [0, 2, 4, 6, 7, 9, 11], chords: [[40, 4], [37, 3], [45, 4], [47, 4]], lead: 'pulse', vol: .9,
             kick: 'x.......x.......', snare: '............x...', hat: '..x...x...x...x.', bass: 'half', arp: 'slow', dens: .55 },
  game:    { bpm: 126, root: 64, scale: [0, 2, 3, 5, 7, 8, 10], chords: [[40, 3], [36, 4], [38, 4], [40, 3]], lead: 'pulse', vol: 1,
             kick: 'x...x...x...x...', snare: '....x.......x...', hat: 'x.x.x.x.x.x.x.x.', bass: 'eighth', arp: 'b', dens: .7 },
  blitz:   { bpm: 152, root: 64, scale: [0, 2, 3, 5, 7, 8, 10], chords: [[40, 3], [36, 4], [43, 4], [38, 4]], lead: 'square', vol: 1,
             kick: 'x...x...x...x.x.', snare: '....x.......x..x', hat: 'xxxxxxxxxxxxxxxx', bass: 'octave', arp: 'always', dens: .8 },
  dungeon: { bpm: 108, root: 64, scale: [0, 1, 3, 5, 7, 8, 10], chords: [[40, 3], [41, 4], [40, 3], [38, 4]], lead: 'tri', vol: .9,
             kick: 'x.....x...x.....', snare: '........x.......', hat: '....x.......x...', bass: 'drone', arp: 'slow', dens: .5 },
};
const RHYTHMS = [   // [Startschritt, Länge] in Sechzehnteln
  [[0, 2], [3, 1], [4, 2], [6, 2], [8, 4], [12, 2], [14, 2]],
  [[0, 1], [1, 1], [2, 2], [4, 4], [10, 2], [12, 4]],
  [[0, 3], [3, 3], [6, 2], [8, 2], [10, 2], [12, 4]],
  [[0, 4], [8, 6]],
  [[0, 2], [2, 2], [4, 2], [6, 2], [8, 8]],
  [[2, 2], [4, 2], [7, 3], [10, 2], [12, 2], [14, 2]],
];

const Music = {
  scene: 'home', cur: null, timer: null, gain: null, noise: null,
  st: null,   // Zustand des laufenden Stücks
  on: () => musicOn(),
  level: () => (S.settings.musicVol ?? 50) / 100,
  out() {   // eigener Lautstärke-Regler für Musik
    const c = Sound.ctx; if (!c) return null;
    if (!this.gain) { this.gain = c.createGain(); this.gain.gain.value = 0; this.gain.connect(Sound.bus); }
    if (!this.noise) { const n = c.sampleRate, b = c.createBuffer(1, n, n), d = b.getChannelData(0); let s = 7; for (let i = 0; i < n; i++) { s = (s * 16807) % 2147483647; d[i] = s / 1073741823.5 - 1; } this.noise = b; }
    return this.gain;
  },
  // Welche Musik gehört zum Bildschirm? (null = Stille)
  want(id) {
    if (id === 'game' && typeof G !== 'undefined' && G) {
      if (G.assign || G.cfg?.quiet || G.mode === 'test') return null;   // Aufträge von Lehrkräften, Einstufungstest: ohne Musik
      return G.mode === 'blitz' ? 'blitz' : G.mode === 'dungeon' ? 'dungeon' : G.cfg?.calm ? 'menu' : 'game';
    }
    if (id === 'duel') return 'blitz';
    if (id === 'home') return 'menu';
    return this.cur;   // Ergebnis: Stück läuft aus und geht ins Menü über
  },
  setScene(id) { this.scene = id; this.update(); },
  update() {
    const t = this.on() && !document.hidden && Sound.ctx && Sound.ctx.state === 'running' ? this.want(this.scene) : null;
    if (t === this.cur && (t === null || this.timer)) { this.applyVol(); return; }
    this.stop(); if (t) this.start(t);
  },
  applyVol() { if (this.gain && Sound.ctx) this.gain.gain.setTargetAtTime(this.cur ? .26 * this.level() : 0, Sound.ctx.currentTime, .15); },
  start(name) {
    const g = this.out(); if (!g) return;
    const c = Sound.ctx, tr = TRACKS[name];
    this.cur = name;
    this.st = { tr, step: 0, bar: 0, loop: 0, t: c.currentTime + .12, seed: (Date.now() & 0xffff) || 1, prev: tr.root + 7, sect: 'A', rest: 0 };
    this.applyVol();
    this.timer = setInterval(() => this.tick(), 40); this.tick();
  },
  stop() {
    if (this.timer) clearInterval(this.timer); this.timer = null; this.cur = null;
    if (this.gain && Sound.ctx) { const c = Sound.ctx; this.gain.gain.cancelScheduledValues(c.currentTime); this.gain.gain.setTargetAtTime(0, c.currentTime, .08);
      const old = this.gain; this.gain = null; setTimeout(() => { try { old.disconnect(); } catch {} }, 800); }
  },
  rnd() { const s = this.st; s.seed = (s.seed * 16807) % 2147483647; return s.seed / 2147483647; },
  tick() {
    const c = Sound.ctx, s = this.st; if (!c || !s) return;
    if (c.state !== 'running') return;
    if (s.t < c.currentTime - .25) s.t = c.currentTime + .05;   // nach einer Pause des Tabs nicht nachholen
    const dt = 60 / s.tr.bpm / 4;
    while (s.t < c.currentTime + .22) { this.play(s, s.t, dt); s.t += dt; this.advance(s); }
  },
  advance(s) {
    if (++s.step < 16) return;
    s.step = 0; s.bar++;
    if (s.rest > 0) s.rest--;
    if (s.bar % 8 === 0) {   // eine Schleife = 8 Takte
      s.loop++;
      s.sect = s.loop % 4 === 3 ? 'B' : 'A';            // jede 4. Schleife: Breakdown ohne Schlagzeug
      if (s.loop % 5 === 4) s.rest = 1;                  // ab und zu ein Takt Pause – keine Endlosschleife ohne Atem
      if (s.loop % 2 === 0) s.seed = (s.seed * 7 + 13) % 2147483647 || 1;   // neue Melodie-Variante
    }
  },
  play(s, t, dt) {
    const tr = s.tr, st = s.step, bar = s.bar % 8, ch = tr.chords[bar % 4], root = ch[0], third = ch[1];
    const triad = [0, third, 7], out = this.gain; if (!out) return;
    if (s.rest > 0) return;
    const drums = s.sect === 'A' || bar >= 6;
    // Schlagzeug
    if (drums && tr.kick[st] === 'x') this.kick(t);
    if (drums && tr.snare[st] === 'x') this.snare(t, .5);
    if (tr.hat[st] === 'x') this.hat(t, st % 4 === 0 ? .32 : .2);
    // Bass
    const b = tr.bass, bassNote = (n, len) => this.voice(n, t, dt * len, 'triangle', .55, out);
    if (b === 'half' && (st === 0 || st === 8)) bassNote(root, 6); else if (b === 'half' && st === 12) bassNote(root + 7, 3);
    if (b === 'eighth' && st % 2 === 0) bassNote(root + (st % 8 === 4 ? 12 : 0), 1.6);
    if (b === 'octave') bassNote(root + (st % 2 ? 12 : 0), .9);
    if (b === 'drone' && st === 0) bassNote(root, 14); else if (b === 'drone' && st === 14 && bar % 2) bassNote(root + 1, 2);
    // Arpeggio (leise, im Hintergrund)
    const arpOn = tr.arp === 'always' || (tr.arp === 'b' && (s.sect === 'B' || bar >= 4)) || (tr.arp === 'slow' && st % 4 === 2);
    if (arpOn && (tr.arp === 'slow' || st % 1 === 0)) { const n = root + 24 + triad[(st >> (tr.arp === 'slow' ? 2 : 0)) % 3]; this.voice(n, t, dt * (tr.arp === 'slow' ? 2.5 : .8), 'square', .1, out, .25); }
    // Melodie: im ersten Takt jeder Schleife das Blitz-Motiv, sonst aus Rhythmus-Bausteinen
    if (s.sect === 'B' && bar < 6) return;
    if (st === 0) s.rh = bar === 0 ? MOTIF.map((m, i) => [i * 2, i === 3 ? 6 : 2, m]) : RHYTHMS[Math.floor(this.rnd() * RHYTHMS.length)];
    const ev = s.rh && s.rh.find(e => e[0] === st); if (!ev) return;
    if (bar !== 0 && this.rnd() > tr.dens + .2) return;   // manche Töne auslassen: Luft in der Melodie
    let n;
    if (bar === 0) n = tr.root + ev[2];
    else {
      const strong = st % 4 === 0, pool = [];
      for (let o = -1; o <= 1; o++) for (const d of tr.scale) { const m = tr.root + d + 12 * o; if (Math.abs(m - s.prev) <= 7 && m >= tr.root - 3 && m <= tr.root + 17) pool.push(m); }
      const chordTones = pool.filter(m => [0, third, 7].includes(((m - root) % 12 + 12) % 12));
      const src = strong && chordTones.length ? chordTones : pool;
      n = src[Math.floor(this.rnd() * src.length)] ?? tr.root;
    }
    s.prev = n;
    const lead = tr.lead === 'tri' ? 'triangle' : 'square';
    this.voice(n, t, dt * ev[1] * .9, lead, tr.lead === 'pulse' ? .2 : .17, out, tr.lead === 'pulse' ? .55 : .7, true);
  },
  voice(midi, t, dur, type, vol, out, bright = 1, vib = false) {
    const c = Sound.ctx, o = c.createOscillator(), g = c.createGain(), f = c.createBiquadFilter();
    o.type = type; o.frequency.setValueAtTime(440 * 2 ** ((midi - 69) / 12), t);
    if (vib && dur > .3) { const l = c.createOscillator(), lg = c.createGain(); l.frequency.value = 5.5; lg.gain.value = 4; l.connect(lg).connect(o.frequency); l.start(t + .15); l.stop(t + dur + .05); }
    f.type = 'lowpass'; f.frequency.value = 1200 + 4200 * bright;
    g.gain.setValueAtTime(.0001, t); g.gain.exponentialRampToValueAtTime(vol, t + .008); g.gain.setTargetAtTime(vol * .6, t + .03, .08); g.gain.setTargetAtTime(.0001, t + Math.max(.02, dur - .03), .03);
    o.connect(f).connect(g).connect(out); o.start(t); o.stop(t + dur + .25);
  },
  kick(t) { const c = Sound.ctx, o = c.createOscillator(), g = c.createGain(); o.frequency.setValueAtTime(140, t); o.frequency.exponentialRampToValueAtTime(42, t + .12);
    g.gain.setValueAtTime(.7, t); g.gain.exponentialRampToValueAtTime(.001, t + .18); o.connect(g).connect(this.gain); o.start(t); o.stop(t + .2); },
  hit(t, vol, freq, len) { const c = Sound.ctx, s = c.createBufferSource(), f = c.createBiquadFilter(), g = c.createGain(); s.buffer = this.noise;
    f.type = 'highpass'; f.frequency.value = freq; g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(.001, t + len);
    s.connect(f).connect(g).connect(this.gain); s.start(t, Math.random() * .5, len + .02); },
  snare(t, v) { this.hit(t, v * .5, 1400, .12); },
  hat(t, v) { this.hit(t, v * .25, 7000, .035); },
  // Erkennungsmelodie (z. B. beim ersten Start und wenn eine Stufe geschafft ist) – läuft über die Effekte
  sting() { if (!S.settings.sound) return; MOTIF.forEach((m, i) => { Sound.tone(76 + m, i * .075, i === 3 ? .45 : .12, 'square', .07); Sound.tone(88 + m, i * .075, .08, 'triangle', .03); }); Sound.zap(.3); },
};

// Erst nach der ersten Berührung: Audio starten, Erkennungsmelodie, dann Musik
(() => {
  let first = true;
  const go = () => {
    Sound.init();
    if (first && Sound.ctx) { first = false; if (S.settings.sound) setTimeout(() => Music.sting(), 120); }
    setTimeout(() => Music.update(), first ? 0 : 50);
  };
  addEventListener('pointerdown', go, { capture: true, passive: true });
  addEventListener('keydown', go, { capture: true });
  document.addEventListener('visibilitychange', () => Music.update());
})();
