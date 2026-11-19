// Ton, Stimme und Mikrofon. Alle Klänge werden im Code erzeugt (Web Audio) – keine fremden Dateien, keine Lizenzfragen.
(function () {
  const A = (window.AUDIO = {});
  const S = () => window.APP_SETTINGS || {};

  // ------------------------------------------------------------------ Klänge
  let ctx = null;
  function ac() {
    if (!ctx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return null;
      ctx = new AC();
    }
    if (ctx.state === 'suspended') ctx.resume().catch(() => {});
    return ctx;
  }
  /** Muss bei der ersten Berührung aufgerufen werden (iOS gibt Ton erst nach einer Geste frei). */
  A.unlock = () => {
    const c = ac(); if (!c) return;
    const b = c.createBuffer(1, 1, 22050); const s = c.createBufferSource(); s.buffer = b; s.connect(c.destination); s.start(0);
  };
  function tone(freq, start, dur, type, vol, slide) {
    const c = ac(); if (!c) return;
    const t = c.currentTime + start;
    const o = c.createOscillator(); const g = c.createGain();
    o.type = type || 'sine'; o.frequency.setValueAtTime(freq, t);
    if (slide) o.frequency.exponentialRampToValueAtTime(slide, t + dur);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol || 0.2, t + 0.015);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(c.destination); o.start(t); o.stop(t + dur + 0.05);
  }
  function noise(start, dur, vol, freq) {
    const c = ac(); if (!c) return;
    const t = c.currentTime + start;
    const n = Math.floor(c.sampleRate * dur); const b = c.createBuffer(1, n, c.sampleRate); const d = b.getChannelData(0);
    for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / n);
    const s = c.createBufferSource(); s.buffer = b;
    const f = c.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = freq || 1200; f.Q.value = 2;
    const g = c.createGain(); g.gain.value = vol || 0.2;
    s.connect(f); f.connect(g); g.connect(c.destination); s.start(t);
  }
  const SFX = {
    tap: () => tone(660, 0, 0.06, 'triangle', 0.12),
    pop: () => { tone(500, 0, 0.09, 'sine', 0.2, 900); },
    right: () => { tone(784, 0, 0.12, 'triangle', 0.18); tone(1046, 0.1, 0.22, 'triangle', 0.18); },
    soft: () => { tone(330, 0, 0.16, 'sine', 0.12, 260); },
    whoosh: () => noise(0, 0.25, 0.12, 900),
    stamp: () => { noise(0, 0.12, 0.35, 300); tone(1318, 0.12, 0.15, 'sine', 0.1); tone(1568, 0.2, 0.25, 'sine', 0.1); },
    squawk: () => { tone(1400, 0, 0.08, 'sawtooth', 0.06, 900); tone(1300, 0.09, 0.1, 'sawtooth', 0.06, 700); },
    fanfare: () => { [523, 659, 784, 1046].forEach((f, i) => tone(f, i * 0.11, 0.3, 'triangle', 0.15)); },
    choo: () => { noise(0, 0.18, 0.15, 700); noise(0.22, 0.18, 0.15, 700); tone(880, 0.45, 0.35, 'square', 0.05); tone(698, 0.45, 0.35, 'square', 0.04); },
  };
  A.sfx = (name) => {
    if (S().sound === false) return;
    try { (SFX[name] || SFX.tap)(); } catch (e) {}
  };
  A.vibrate = (ms) => {
    if (S().vibrate === false) return;
    try { if (navigator.vibrate) navigator.vibrate(ms || 30); } catch (e) {}
  };

  // ------------------------------------------------------------------ Stimme
  // Prototyp: Stimme des Geräts. Nur Stimmen, die auf dem Gerät sprechen (localService) – Online-Stimmen
  // würden den Text an fremde Server schicken. Später: vorab vertonte Dateien (audio/…mp3), diese hier nur als Rückfall.
  let voicesCache = [];
  function loadVoices() { try { voicesCache = speechSynthesis.getVoices() || []; } catch (e) { voicesCache = []; } }
  if ('speechSynthesis' in window) {
    loadVoices();
    try { speechSynthesis.addEventListener('voiceschanged', loadVoices); } catch (e) { speechSynthesis.onvoiceschanged = loadVoices; }
  }
  // Sprache der Gerätestimme je Figur/Stimme der Vertonung
  const LANG_OF = { ruby: 'en', mia: 'en', sam: 'en', ted: 'en', de: 'de', fr: 'fr', es: 'es' };
  const PREF = { en: [/^en-GB$/i, /^en-(IE|AU|NZ)$/i, /^en-US$/i], de: [/^de-DE$/i, /^de-(AT|CH)$/i], fr: [/^fr-FR$/i, /^fr-(BE|CH|CA)$/i], es: [/^es-ES$/i, /^es-/i] };
  /** Lokale Gerätestimme für eine Sprache (nur Stimmen, die auf dem Gerät sprechen). */
  A.deviceVoice = (lang) => {
    lang = lang || 'en';
    const vs = voicesCache.filter((v) => v.localService && new RegExp('^' + lang + '[-_]', 'i').test(v.lang));
    for (const re of PREF[lang] || []) { const v = vs.find((x) => re.test(x.lang.replace('_', '-'))); if (v) return v; }
    return vs[0] || null;
  };
  A.englishVoice = () => A.deviceVoice('en');
  A.canSpeak = (lang) => 'speechSynthesis' in window && !!A.deviceVoice(lang || 'en');
  A.langOf = (who) => LANG_OF[who] || 'en';
  // ------------------------------------------------------------------ Vorab vertonte Sätze
  // Standard: freie Piper-Stimmen (audio/index.json + audio/piper/*.mp3, mit der App offline gespeichert).
  // Nova (KI-Stimme vom Stimm-Server) ist vorbereitet, aber erst nach Marcels Go eingeschaltet (window.NOVA_FREIGEGEBEN).
  // Gespielt wird über Web Audio: Nach der ersten Berührung darf das auch iOS jederzeit, ohne neue Geste.
  const norm = (t) => String(t).normalize('NFC').replace(/\s+/g, ' ').trim();
  const sprechtext = (t) => norm(String(t).replace(/[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}]/gu, ''));
  let index = null;
  A.ready = fetch('audio/index.json', { cache: 'no-cache' }).then((r) => (r.ok ? r.json() : null))
    .then((j) => { index = j && j.saetze ? j.saetze : null; }).catch(() => { index = null; });
  const fileFor = (text, who, slow) => {
    if (!index) return null;
    const e = index[(who || 'ruby') + '|' + norm(text)] || index['ruby|' + norm(text)];
    // Unsichere Aufnahmen (Hörprüfung nicht bestanden) nicht abspielen – lieber die Gerätestimme als eine falsche Aussprache
    return e && !e.unsicher ? 'audio/piper/' + (slow ? e.s : e.n) : null;
  };
  A.hasRecording = (text, who) => !!fileFor(text, who, false);
  A.voiceMode = () => {
    const m = S().stimme || 'frei';
    return m === 'nova' && !window.NOVA_FREIGEGEBEN ? 'frei' : m;   // Nova erst nach Freigabe
  };
  const buffers = new Map();
  function decode(c, ab) {
    return new Promise((ok, no) => { const p = c.decodeAudioData(ab, ok, no); if (p && p.then) p.then(ok, no); });
  }
  async function bufferFor(url) {
    if (buffers.has(url)) return buffers.get(url);
    const c = ac(); if (!c) return null;
    const r = await fetch(url, url.startsWith('http') ? { credentials: 'omit', referrerPolicy: 'no-referrer', mode: 'cors' } : {});
    if (!r.ok) return null;
    const b = await decode(c, await r.arrayBuffer());
    if (buffers.size > 120) buffers.delete(buffers.keys().next().value);
    buffers.set(url, b);
    return b;
  }
  // Nova (vorbereitet): gleiche Kennung wie im Wissensgarten – sha256("nova|en|<text>"), die ersten 32 Zeichen
  const NOVA = 'https://stimmen.mathenachhilfe-online.com/nova/';
  async function novaUrl(text) {
    if (!(window.crypto && crypto.subtle)) return null;
    const d = await crypto.subtle.digest('SHA-256', new TextEncoder().encode('nova|en|' + sprechtext(text)));
    const h = Array.from(new Uint8Array(d)).map((x) => x.toString(16).padStart(2, '0')).join('').slice(0, 32);
    return NOVA + h.slice(0, 2) + '/' + h + '.mp3';
  }
  A.novaUrl = novaUrl;

  let speakDone = null; let current = null;
  A.stop = () => {
    try { if ('speechSynthesis' in window) speechSynthesis.cancel(); } catch (e) {}
    if (current) { try { current.stop(); } catch (e) {} current = null; }
    if (speakDone) speakDone();
  };
  function playBuffer(b) {
    return new Promise((ok) => {
      const c = ac(); const src = c.createBufferSource(); src.buffer = b;
      const g = c.createGain(); g.gain.value = 1; src.connect(g); g.connect(c.destination);
      let done = false;
      const fin = () => { if (done) return; done = true; clearTimeout(t); if (current === src) current = null; if (speakDone === fin) speakDone = null; ok(true); };
      const t = setTimeout(fin, b.duration * 1000 + 1500);
      src.onended = fin; speakDone = fin; current = src;
      src.start(0); A.played = (A.played || 0) + 1;
    });
  }
  function deviceSay(text, opts) {
    return new Promise((ok) => {
      const lang = A.langOf(opts.who);
      if (!A.canSpeak(lang)) { ok(false); return; }
      const v = A.deviceVoice(lang);
      let done = false;
      const fin = () => { if (done) return; done = true; clearTimeout(t); if (speakDone === fin) speakDone = null; ok(true); };
      const t = setTimeout(fin, 15000);
      const u = new SpeechSynthesisUtterance(sprechtext(text));
      u.voice = v; u.lang = v.lang;
      u.pitch = opts.pitch || 1; u.rate = (opts.slow ? 0.6 : 0.9) * (S().slow ? 0.75 : 1);
      u.onend = u.onerror = fin;
      speakDone = fin;
      try { speechSynthesis.speak(u); } catch (e) { fin(); }
    });
  }
  /**
   * Englisch sprechen: opts.who = Figur (ruby, mia, sam, ted), opts.slow = langsam (🐢).
   * Reihenfolge: (Nova, wenn freigegeben und gewählt) → vorab vertonte Datei → Stimme des Geräts.
   * Erfüllt sich mit true, wenn etwas zu hören war, sonst false.
   */
  A.say = async (text, opts) => {
    opts = opts || {};
    if (S().voice === false) return false;
    A.stop();
    const slow = !!opts.slow || !!S().slow;
    try {
      if (A.voiceMode() === 'nova' && navigator.onLine !== false) {
        const b = await bufferFor(await novaUrl(text)).catch(() => null);
        if (b) return playBuffer(b);
      }
      if (A.voiceMode() !== 'geraet') {
        const f = fileFor(text, opts.who, slow);
        if (f) { const b = await bufferFor(f); if (b) return playBuffer(b); }
      }
    } catch (e) {}
    return deviceSay(text, Object.assign({}, opts, { slow }));
  };
  /** Kann dieser Satz hörbar gemacht werden (Datei oder Gerätestimme)? */
  A.canHear = (text, who) => S().voice !== false && (A.hasRecording(text, who) || A.canSpeak(A.langOf(who)));

  // ------------------------------------------------------------------ Mikrofon (nur auf Knopfdruck)
  // Die Aufnahme bleibt im Arbeitsspeicher, wird abgespielt und verworfen. Nichts wird gespeichert oder gesendet.
  A.micSupported = () => !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder);
  let rec = null;
  /**
   * Aufnahme starten. onLevel(0..1) bekommt laufend die Lautstärke.
   * Liefert ein Objekt mit stop() → Promise<{url, peak, ms}>.
   */
  A.record = async (onLevel) => {
    if (!A.micSupported()) throw new Error('nomic');
    const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
    const chunks = [];
    let mr;
    try { mr = new MediaRecorder(stream); } catch (e) { stream.getTracks().forEach((t) => t.stop()); throw e; }
    mr.ondataavailable = (e) => { if (e.data && e.data.size) chunks.push(e.data); };
    let peak = 0; let loud = 0; let raf = 0; let an = null;
    const c = ac();
    if (c) {
      try {
        const src = c.createMediaStreamSource(stream); an = c.createAnalyser(); an.fftSize = 512; src.connect(an);
        const buf = new Uint8Array(an.fftSize);
        const loop = () => {
          an.getByteTimeDomainData(buf);
          let sum = 0; for (let i = 0; i < buf.length; i++) { const x = (buf[i] - 128) / 128; sum += x * x; }
          const lvl = Math.min(1, Math.sqrt(sum / buf.length) * 4);
          peak = Math.max(peak, lvl); if (lvl > 0.12) loud++;
          if (onLevel) onLevel(lvl);
          raf = requestAnimationFrame(loop);
        };
        loop();
      } catch (e) {}
    }
    const t0 = Date.now();
    mr.start();
    const handle = {
      stop: () => new Promise((ok) => {
        cancelAnimationFrame(raf);
        mr.onstop = () => {
          stream.getTracks().forEach((t) => t.stop());
          const blob = new Blob(chunks, { type: mr.mimeType || 'audio/webm' });
          ok({ url: chunks.length ? URL.createObjectURL(blob) : null, peak, loudFrames: loud, ms: Date.now() - t0 });
        };
        try { mr.stop(); } catch (e) { stream.getTracks().forEach((t) => t.stop()); ok({ url: null, peak, loudFrames: loud, ms: Date.now() - t0 }); }
      }),
    };
    rec = handle;
    // Sicherheitsnetz: nie länger als 8 Sekunden aufnehmen
    setTimeout(() => { if (rec === handle && mr.state === 'recording') handle.stop(); }, 8000);
    return handle;
  };
  A.playUrl = (url) => new Promise((ok) => {
    if (!url) { ok(); return; }
    const a = new Audio(url);
    const fin = () => { try { URL.revokeObjectURL(url); } catch (e) {} ok(); };
    a.onended = fin; a.onerror = fin;
    a.play().catch(fin);
  });

  // ------------------------------------------------------------------ Spracherkennung (optional)
  // Nur wenn die Eltern es nicht abgeschaltet haben. Wo der Browser es kann, ausschließlich auf dem Gerät (processLocally).
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  A.recognitionMode = () => {
    if (!SR) return 'none';
    const pref = S().recognition || 'device';      // 'device' (Standard) | 'browser' (nur mit Zustimmung der Eltern) | 'off'
    if (pref === 'off') return 'none';
    const local = 'processLocally' in SR.prototype;
    if (pref === 'device') return local ? 'device' : 'none';
    return local ? 'device' : 'browser';
  };
  /** Einmal zuhören (max. 6 s). Liefert Liste möglicher Texte oder [] (nicht verstanden/abgebrochen/nicht verfügbar). */
  A.listen = (lang) => new Promise((ok) => {
    const mode = A.recognitionMode();
    if (mode === 'none') { ok(null); return; }
    let r;
    try { r = new SR(); } catch (e) { ok(null); return; }
    r.lang = ({ en: 'en-GB', fr: 'fr-FR', es: 'es-ES', de: 'de-DE' })[lang || 'en'] || 'en-GB'; r.interimResults = false; r.maxAlternatives = 5; r.continuous = false;
    if (mode === 'device') {
      // Streng: Bestätigt der Browser „nur auf dem Gerät“ nicht, wird gar nicht erst zugehört (sonst könnte Ton an den Hersteller gehen).
      try { r.processLocally = true; } catch (e) {}
      if (r.processLocally !== true) { ok(null); return; }
    }
    let done = false;
    const fin = (v) => { if (done) return; done = true; clearTimeout(t); try { r.abort(); } catch (e) {} ok(v); };
    const t = setTimeout(() => fin([]), 6000);
    r.onresult = (e) => {
      const alts = [];
      for (let i = 0; i < e.results.length; i++) for (let j = 0; j < e.results[i].length; j++) alts.push(e.results[i][j].transcript);
      fin(alts);
    };
    r.onerror = (e) => fin(e && (e.error === 'not-allowed' || e.error === 'service-not-allowed' || e.error === 'language-not-supported') ? null : []);
    r.onend = () => fin([]);
    try { r.start(); } catch (e) { fin(null); }
  });

  /** Wie ähnlich ist das Gehörte dem Zielsatz? 0..1 (Anteil der Zielwörter, die vorkommen). Großzügig für Kinderstimmen. */
  A.similarity = (target, alts) => {
    const digits = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten'];
    const norm = (s) => String(s).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[’']/g, '').replace(/[^a-z0-9 ]/g, ' ').split(/\s+/).filter(Boolean)
      .map((w) => (/^\d+$/.test(w) && +w <= 10 ? digits[+w] : w));
    const want = norm(target); if (!want.length) return 0;
    let best = 0;
    for (const a of alts || []) {
      const got = new Set(norm(a)); let hit = 0;
      for (const w of want) if (got.has(w)) hit++;
      best = Math.max(best, hit / want.length);
    }
    return best;
  };
  if (typeof module !== 'undefined') module.exports = A;
})();
