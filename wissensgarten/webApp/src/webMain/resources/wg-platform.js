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

  // Vorlesen: siehe wg-stimme.js (Stimme „Nova“ vom Stimm-Server, Browser-Stimme als Notlösung)

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
    // Nicht in <body>: dort hängt Compose ein Shadow-DOM an, normale Kinder von <body> werden dann nicht gezeigt
    if (!t) { t = document.createElement('div'); t.id = 'wg-toast'; t.setAttribute('role', 'status'); document.documentElement.appendChild(t); }
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

  // „App installieren“ (PWA). Kein Knopf, wenn die WebApp schon installiert läuft oder der Browser es nicht kann.
  // - Chrome/Edge/Android: „beforeinstallprompt“ abfangen, merken und beim Antippen prompt() aufrufen
  // - iPhone/iPad: es gibt kein solches Ereignis – die App zeigt eine kurze Anleitung (Teilen → Zum Home-Bildschirm)
  // Dieses Skript läuft vor der App (im <head>), damit kein frühes Ereignis verloren geht.
  const inst = (wg.install = { deferred: null, installed: false, listeners: [] });
  const LATER = 'wg.installSpaeter';
  const LATER_MS = 14 * 24 * 3600 * 1000;           // „Später“ im Garten: 2 Wochen Ruhe (in den Einstellungen bleibt der Knopf)
  const mq = (q) => { try { return window.matchMedia(q).matches; } catch (e) { return false; } };
  inst.standalone = () => mq('(display-mode: standalone)') || mq('(display-mode: fullscreen)') || mq('(display-mode: minimal-ui)') ||
    mq('(display-mode: window-controls-overlay)') || navigator.standalone === true;
  inst.ios = () => /iPhone|iPad|iPod/.test(navigator.userAgent || '') || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  inst.mode = () => {
    if (inst.installed || inst.standalone()) return 'none';
    if (inst.deferred) return 'prompt';
    if (inst.ios()) return 'ios';
    return 'none';
  };
  inst.later = () => { try { const t = +localStorage.getItem(LATER); return !!t && Date.now() - t < LATER_MS; } catch (e) { return false; } };
  inst.state = () => inst.mode() + (inst.later() ? ':spaeter' : '');
  const notify = () => { const s = inst.state(); inst.listeners.forEach((cb) => { try { cb(s); } catch (e) {} }); };
  inst.onChange = (cb) => { inst.listeners.push(cb); cb(inst.state()); };
  inst.postpone = () => { try { localStorage.setItem(LATER, String(Date.now())); } catch (e) {} notify(); };
  inst.prompt = () => {
    const d = inst.deferred;
    if (!d) return;
    inst.deferred = null;                              // ein gemerktes Angebot lässt sich nur einmal zeigen
    try {
      Promise.resolve(d.prompt()).catch(() => {});
      Promise.resolve(d.userChoice).then((c) => { if (c && c.outcome === 'accepted') inst.installed = true; notify(); }).catch(notify);
    } catch (e) { notify(); }
    notify();
  };
  window.addEventListener('beforeinstallprompt', (e) => { e.preventDefault(); inst.deferred = e; notify(); });
  window.addEventListener('appinstalled', () => { inst.installed = true; inst.deferred = null; notify(); });
  try { window.matchMedia('(display-mode: standalone)').addEventListener('change', notify); } catch (e) {}
})();
