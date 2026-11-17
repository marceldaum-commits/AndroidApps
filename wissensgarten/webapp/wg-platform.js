// Wissensgarten WebApp – kleine Browser-Brücke (Ton, Sprache, Teilen, Speicher). Kein Tracking, keine fremden Server.
(function () {
  const wg = (window.wg = {});
  let ctx = null;
  const buffers = {};
  const pendingSounds = {};
  function audio() {
    if (!ctx) { try { ctx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { return null; } }
    if (ctx.state === 'suspended') ctx.resume();
    return ctx;
  }
  // Autoplay-Regeln: Ton erst nach dem ersten Antippen
  ['pointerdown', 'keydown', 'touchstart'].forEach((ev) => window.addEventListener(ev, () => { audio(); for (const n in pendingSounds) decode(n); }, { passive: true }));
  function b64ToBuf(b64) { const s = atob(b64); const a = new Uint8Array(s.length); for (let i = 0; i < s.length; i++) a[i] = s.charCodeAt(i); return a.buffer; }
  function decode(name) {
    const c = audio(); if (!c || buffers[name] || !pendingSounds[name]) return;
    const data = pendingSounds[name]; delete pendingSounds[name];
    c.decodeAudioData(b64ToBuf(data)).then((b) => { buffers[name] = b; if (music.want === name) music.start(); }).catch(() => {});
  }
  wg.loadSound = (name, b64wav) => { pendingSounds[name] = b64wav; if (ctx) decode(name); };
  wg.play = (name, rate, vol) => {
    const c = audio(); const b = buffers[name]; if (!c || !b) { decode(name); return; }
    const src = c.createBufferSource(); src.buffer = b; src.playbackRate.value = rate;
    const g = c.createGain(); g.gain.value = vol; src.connect(g).connect(c.destination); src.start();
  };
  // Hintergrundmusik in Schleife mit sanftem Ein-/Ausblenden
  const music = { want: null, src: null, gain: null, playing: null, level: 0.16,
    start() {
      const c = audio(); const b = buffers[this.want]; if (!c || !b) return;
      if (this.playing === this.want) { this.fade(this.level); return; }
      this.stop(0.4);
      const src = c.createBufferSource(); src.buffer = b; src.loop = true;
      const g = c.createGain(); g.gain.value = 0; src.connect(g).connect(c.destination); src.start();
      this.src = src; this.gain = g; this.playing = this.want; this.fade(this.level, 1.5);
    },
    fade(to, sec) { if (this.gain && ctx) { this.gain.gain.cancelScheduledValues(ctx.currentTime); this.gain.gain.linearRampToValueAtTime(to, ctx.currentTime + (sec || 0.6)); } },
    stop(sec) { const s = this.src, g = this.gain; if (!s) return; this.src = null; this.gain = null; this.playing = null;
      if (ctx && g) { g.gain.linearRampToValueAtTime(0, ctx.currentTime + (sec || 0.5)); setTimeout(() => { try { s.stop(); } catch (e) {} }, (sec || 0.5) * 1000 + 50); } },
  };
  wg.musicPlay = (name, level) => { music.want = name; music.level = level; decode(name); music.start(); };
  wg.musicStop = () => { music.want = null; music.stop(0.5); };
  wg.musicLevel = (level) => { music.level = level; music.fade(level); };

  // Vorlesen: siehe wg-stimme.js (Browser-Stimmen und die natürliche Stimme „Thorsten“)

  // Spracherkennung (Chrome, Edge, Safari)
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  wg.srAvailable = () => !!SR;
  wg.sr = { state: 'idle', words: [], rec: null };
  wg.listen = (lang) => {
    if (!SR) { wg.sr.state = 'error'; return; }
    try { if (wg.sr.rec) wg.sr.rec.abort(); } catch (e) {}
    const r = new SR(); r.lang = lang; r.maxAlternatives = 5; r.interimResults = false; r.continuous = false;
    wg.sr = { state: 'listening', words: [], rec: r };
    r.onresult = (e) => { const res = e.results[0]; const w = []; for (let i = 0; i < res.length; i++) w.push(res[i].transcript); wg.sr.words = w; wg.sr.state = 'done'; };
    r.onerror = (e) => { wg.sr.state = (e.error === 'no-speech') ? 'silence' : 'error'; };
    r.onend = () => { if (wg.sr.state === 'listening') wg.sr.state = 'silence'; };
    try { r.start(); } catch (e) { wg.sr.state = 'error'; }
  };
  wg.cancelListen = () => { try { if (wg.sr.rec) wg.sr.rec.abort(); } catch (e) {} wg.sr.state = 'idle'; };

  // Kurze Meldung unten am Bildschirm
  wg.toast = (msg) => {
    let t = document.getElementById('wg-toast');
    if (!t) { t = document.createElement('div'); t.id = 'wg-toast'; document.body.appendChild(t); }
    t.textContent = msg; t.className = 'show'; clearTimeout(wg._tt); wg._tt = setTimeout(() => { t.className = ''; }, 3500);
  };
  // Teilen: Teilen-Menü des Geräts, sonst in die Zwischenablage
  wg.shareText = (text) => {
    if (navigator.share) { navigator.share({ text }).catch(() => {}); return; }
    navigator.clipboard && navigator.clipboard.writeText(text).then(() => wg.toast('In die Zwischenablage kopiert – jetzt einfügen und verschicken. 📋'), () => wg.toast(text));
  };
  wg.sharePng = (name, b64, text) => {
    const blob = new Blob([new Uint8Array(b64ToBuf(b64))], { type: 'image/png' });
    const file = new File([blob], name, { type: 'image/png' });
    if (navigator.canShare && navigator.canShare({ files: [file] })) { navigator.share({ files: [file], text }).catch(() => {}); return; }
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = name; document.body.appendChild(a); a.click(); a.remove();
    wg.toast('Bild gespeichert: ' + name);
  };
  // Bastelbogen: druckfertige Seite in neuem Fenster
  wg.printHtml = (html) => {
    const w = window.open('', '_blank');
    if (!w) { wg.toast('Bitte Pop-ups für den Bastelbogen erlauben.'); return; }
    w.document.open(); w.document.write(html); w.document.close(); setTimeout(() => { try { w.focus(); w.print(); } catch (e) {} }, 400);
  };
  // Lagesensor (Kipp-Spiel)
  wg.tiltX = 0; wg.tiltOn = false;
  wg.startTilt = () => {
    const on = () => { if (wg.tiltOn) return; wg.tiltOn = true; window.addEventListener('devicemotion', (e) => { const g = e.accelerationIncludingGravity; if (g && g.x != null) wg.tiltX = g.x; }); };
    if (window.DeviceMotionEvent && typeof DeviceMotionEvent.requestPermission === 'function') DeviceMotionEvent.requestPermission().then((s) => { if (s === 'granted') on(); }).catch(() => {});
    else on();
  };
  wg.keepAwake = (on) => {
    if (!('wakeLock' in navigator)) return;
    if (on) navigator.wakeLock.request('screen').then((l) => { wg._lock = l; }).catch(() => {});
    else if (wg._lock) { wg._lock.release(); wg._lock = null; }
  };
  wg.visible = () => document.visibilityState === 'visible';
})();
