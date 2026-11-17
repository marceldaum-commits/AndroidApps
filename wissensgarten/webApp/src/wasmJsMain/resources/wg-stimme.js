// Wissensgarten WebApp – Brummis Stimme: Stimmen des Browsers (nur solche auf dem Gerät) und die natürliche Stimme „Thorsten“.
// Thorsten rechnet ganz im Browser (ONNX Runtime Web, MIT). Die Stimmdatei (63 MB, CC0) kommt nur auf Wunsch einmal von unserem Stimm-Server
// und liegt danach im Browser-Speicher (Cache Storage). Die Rechen-Bausteine (ort/…) werden erst geladen, wenn Thorsten spricht.
// Kein Text verlässt das Gerät.
(function () {
  const wg = (window.wg = window.wg || {});

  // ---------------------------------------------------------------- Stimmen des Browsers
  // Nur Stimmen, die auf dem Gerät sprechen (localService) – Online-Stimmen würden den Text an einen Server schicken.
  const localVoices = (lang) => ('speechSynthesis' in window ? speechSynthesis.getVoices() : [])
    .filter((v) => v.localService && v.lang && v.lang.toLowerCase().replace('_', '-').startsWith(lang));
  wg.voiceList = () => JSON.stringify(localVoices('de').map((v) => ({ id: v.voiceURI, label: v.name })));
  wg.speaking = false;
  wg.ttsAvailable = () => {
    if (!('speechSynthesis' in window)) return false;
    const all = speechSynthesis.getVoices();
    return all.length === 0 || localVoices('de').length > 0;
  };
  wg.speak = (text, lang, queue, voiceId, rate) => {
    if (!('speechSynthesis' in window)) return;
    const l2 = lang.slice(0, 2).toLowerCase();
    const vs = localVoices(l2);
    let v = (l2 === 'de' && voiceId) ? vs.find((x) => x.voiceURI === voiceId) : null;
    v = v || vs.find((x) => x.lang.replace('_', '-').toLowerCase() === lang.toLowerCase()) || vs[0];
    if (!v) return;                                  // keine Stimme auf dem Gerät für diese Sprache: lieber still als online
    if (!queue) speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text); u.lang = v.lang; u.voice = v; u.pitch = 1.25; u.rate = rate || 0.95;
    u.onstart = () => { wg.speaking = true; }; u.onend = u.onerror = () => { wg.speaking = speechSynthesis.speaking && speechSynthesis.pending; };
    wg.speaking = true; speechSynthesis.speak(u);
  };
  wg.stopSpeaking = () => { if ('speechSynthesis' in window) speechSynthesis.cancel(); wg.speaking = false; };

  // Kleine Textdateien der WebApp holen (Lautschrift-Wörterbuch, gzip)
  wg.fetchText = (url) => fetch(url).then(async (r) => {
    if (!r.ok) return null;
    const buf = new Uint8Array(await r.arrayBuffer());
    if (buf.length > 1 && buf[0] === 0x1f && buf[1] === 0x8b) {
      if (!window.DecompressionStream) return null;
      return new Response(new Blob([buf]).stream().pipeThrough(new DecompressionStream('gzip'))).text();
    }
    return new TextDecoder().decode(buf);
  }).catch(() => null);

  // ---------------------------------------------------------------- Thorsten
  // Merkname im Browser-Speicher (bleibt gleich, damit schon geladene Stimmen erhalten bleiben)
  const MODEL_URL = 'https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/de_DE-thorsten-medium.onnx';
  // Quelle: unser eigener Stimm-Server. Dort liegt die reine Piper-Datei; die 127 Bytes mit den sherpa-Angaben hängen wir an –
  // das Ergebnis ist Byte für Byte dieselbe geprüfte Datei wie in der Android-App (gleiche Prüfsumme).
  const SRC_URL = 'https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx';
  const SRC_SIZE = 63201294;
  const TAIL_B64 = 'chIKCm1vZGVsX3R5cGUSBHZpdHNyEAoHY29tbWVudBIFcGlwZXJyEgoIbGFuZ3VhZ2USBkdlcm1hbnILCgV2b2ljZRICZGVyDwoKaGFzX2VzcGVhaxIBMXIPCgpuX3NwZWFrZXJzEgExchQKC3NhbXBsZV9yYXRlEgUyMjA1MA==';
  const SIZE = 63201421;
  const SHA256 = '1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6';
  const STORE = 'wissensgarten-stimme';              // eigener Speicher – der Service Worker lässt ihn bei Updates stehen
  const RATE = 22050;                                // Abtastrate des Modells (laut Modell-Daten)
  const th = (wg.th = { state: 'checking', pct: 0, why: '', problem: '', speaking: false, ready: false, turn: 0, lastSamples: 0, lastPeak: 0 });
  th.supported = () => 'caches' in window && 'WebAssembly' in window && !!(window.crypto && crypto.subtle) && typeof BigInt64Array !== 'undefined';
  let abort = null, session = null, ctx = null, sources = [];

  th.check = async () => {
    if (!th.supported()) { th.state = 'unsupported'; return; }
    try { const c = await caches.open(STORE); th.state = (await c.match(MODEL_URL)) ? 'ready' : 'missing'; } catch (e) { th.state = 'missing'; }
  };
  const hex = (buf) => Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('');
  th.download = async () => {
    if (th.state === 'loading' || th.state === 'ready' || !th.supported()) return;
    th.state = 'loading'; th.pct = 0; th.why = '';
    const ctl = new AbortController(); abort = ctl;
    try {
      // ohne Cookies und ohne Herkunftsangabe – unser Stimm-Server sieht nur IP-Adresse und Zeitpunkt
      const res = await fetch(SRC_URL, { signal: ctl.signal, credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store' });
      if (!res.ok || !res.body) throw new Error('Der Server antwortet mit ' + res.status + '.');
      const reader = res.body.getReader(); const buf = new Uint8Array(SIZE); let got = 0;
      for (;;) {
        const r = await reader.read(); if (r.done) break;
        if (got + r.value.length > SRC_SIZE) throw new Error('Die Datei ist größer als erwartet.');
        buf.set(r.value, got); got += r.value.length; th.pct = Math.min(99, Math.floor(got * 100 / SRC_SIZE));
      }
      if (got !== SRC_SIZE) throw new Error('Die Datei ist unvollständig angekommen.');
      const tail = Uint8Array.from(atob(TAIL_B64), (ch) => ch.charCodeAt(0)); buf.set(tail, SRC_SIZE);
      if (hex(await crypto.subtle.digest('SHA-256', buf)) !== SHA256) throw new Error('Die Datei ist beschädigt angekommen.');
      const c = await caches.open(STORE);
      await c.put(MODEL_URL, new Response(buf, { headers: { 'Content-Type': 'application/octet-stream', 'Content-Length': String(SIZE) } }));
      try { if (navigator.storage && navigator.storage.persist) navigator.storage.persist(); } catch (e) {}
      th.state = 'ready'; th.problem = '';
    } catch (e) {
      if (ctl.signal.aborted) { th.state = 'missing'; th.why = ''; }
      else {
        th.state = 'failed';
        th.why = (e && e.name === 'QuotaExceededError') ? 'Im Browser ist nicht genug Platz für die Stimme.'
          : (e && e.message && !/fetch|network|load/i.test(e.message)) ? e.message : 'Keine Verbindung. Bitte später noch einmal versuchen.';
      }
    }
    abort = null;
  };
  th.cancel = () => { if (abort) abort.abort(); };
  th.remove = async () => { th.cancel(); th.unload(); try { await caches.delete(STORE); } catch (e) {} th.state = 'missing'; th.problem = ''; };

  function loadScript(src) {
    return new Promise((ok, fail) => { const s = document.createElement('script'); s.src = src; s.onload = ok; s.onerror = () => fail(new Error(src)); document.head.appendChild(s); });
  }
  // Rechen-Bausteine und Stimmdatei erst jetzt laden – nicht beim Start der WebApp
  th.prepare = () => {
    if (session) return session;
    session = (async () => {
      if (!window.ort) await loadScript('ort/ort.wasm.min.js');
      ort.env.wasm.wasmPaths = new URL('ort/', location.href).href;
      ort.env.wasm.numThreads = self.crossOriginIsolated ? Math.min(4, navigator.hardwareConcurrency || 1) : 1;
      const c = await caches.open(STORE); const r = await c.match(MODEL_URL);
      if (!r) throw new Error('Stimmdatei fehlt');
      const s = await ort.InferenceSession.create(new Uint8Array(await r.arrayBuffer()), { executionProviders: ['wasm'], graphOptimizationLevel: 'all' });
      th.ready = true; th.problem = '';
      return s;
    })();
    session.catch((e) => { session = null; th.ready = false; th.problem = 'Thorsten ließ sich in diesem Browser nicht starten – Brummi liest mit der Browser-Stimme.'; console.warn('Thorsten', e); });
    return session;
  };
  th.unload = () => { th.stop(); const p = session; session = null; th.ready = false; if (p) p.then((s) => s.release && s.release()).catch(() => {}); };

  function audio() {
    if (!ctx) { try { ctx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { return null; } }
    if (ctx.state === 'suspended') ctx.resume();
    return ctx;
  }
  ['pointerdown', 'keydown', 'touchstart'].forEach((ev) => window.addEventListener(ev, () => { if (ctx && ctx.state === 'suspended') ctx.resume(); }, { passive: true }));
  function silence() { sources.forEach((s) => { try { s.stop(); } catch (e) {} }); sources = []; }

  /** Eine Folge von Laut-Nummern (Piper: ^ _ Laut _ … $) rechnen – liefert die Audio-Samples (Float32Array). */
  th.synth = async (ids, lengthScale) => {
    const s = await th.prepare();
    const feeds = {
      input: new ort.Tensor('int64', BigInt64Array.from(ids, (x) => BigInt(x)), [1, ids.length]),
      input_lengths: new ort.Tensor('int64', BigInt64Array.from([BigInt(ids.length)]), [1]),
      scales: new ort.Tensor('float32', Float32Array.from([0.667, lengthScale, 0.8]), [3]),
    };
    const out = await s.run(feeds);
    const pcm = out[s.outputNames[0]].data;
    let peak = 0; for (let i = 0; i < pcm.length; i++) { const a = Math.abs(pcm[i]); if (a > peak) peak = a; }
    th.lastSamples = pcm.length; th.lastPeak = peak;
    return pcm;
  };

  /** Sätze sprechen: „1,0,25,0,…,2;1,0,…“ (je Satz Laut-Nummern). Rechnet Satz für Satz und spielt, während der nächste entsteht. */
  th.speak = (spec, lengthScale) => {
    const my = ++th.turn; silence(); th.speaking = true;
    (async () => {
      try {
        const c = audio(); let at = 0;
        for (const part of spec.split(';')) {
          if (!part) continue;
          const pcm = await th.synth(part.split(',').map(Number), lengthScale);
          if (my !== th.turn) return;
          if (!c) continue;
          const b = c.createBuffer(1, pcm.length, RATE); b.copyToChannel(pcm, 0);
          const src = c.createBufferSource(); src.buffer = b; src.connect(c.destination);
          at = Math.max(at, c.currentTime + 0.03); src.start(at); at += b.duration + 0.15;
          sources.push(src);
        }
        const until = Date.now() + (c ? Math.max(0, at - c.currentTime) * 1000 : 0) + 3000;   // Sicherheitsnetz, falls der Ton angehalten ist
        if (c) while (my === th.turn && c.currentTime < at - 0.1 && Date.now() < until) await new Promise((ok) => setTimeout(ok, 100));
      } catch (e) { console.warn('Thorsten', e); }
      if (my === th.turn) { th.speaking = false; sources = []; }
    })();
  };
  th.stop = () => { th.turn++; silence(); th.speaking = false; };

  th.check();
})();
