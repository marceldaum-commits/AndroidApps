// Natürliche Stimme „Thorsten“ im Browser – nur auf Wunsch geladen.
// Rechnet mit onnxruntime-web (MIT, liegt in ort/) die Piper-Stimme „Thorsten“ (Aufnahmen CC0). Die Stimmdatei (63 MB) kommt einmal von Hugging Face
// und bleibt im Browser-Speicher („experimentopia-stimme“). Die Lautschrift kommt vorab erzeugt aus der App (stimme/*.tsv) – kein espeak-ng.
(() => {
  const URL_MODEL = 'https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/de_DE-thorsten-medium.onnx';
  const SIZE = 63201421, SHA = '1c305abe13dda2f50a9783da3e7645396833cdf2d09f24fa9a823c91293d65a6';
  const CACHE = 'experimentopia-stimme';
  let session = null, tokens = null, abort = null, turn = 0, playing = [];

  const hex = (b) => [...new Uint8Array(b)].map((x) => x.toString(16).padStart(2, '0')).join('');
  function loadScript(src) {
    return new Promise((ok, no) => { const s = document.createElement('script'); s.src = src; s.onload = ok; s.onerror = () => no(new Error('Der Rechenbaustein ließ sich nicht laden.')); document.head.appendChild(s); });
  }
  async function cachedModel() { try { const c = await caches.open(CACHE); const r = await c.match(URL_MODEL); return r ? await r.arrayBuffer() : null; } catch (e) { return null; } }

  async function start(buf) {
    if (!window.ort) await loadScript('ort/ort.wasm.min.js');
    ort.env.wasm.wasmPaths = new URL('ort/', location.href).href;
    ort.env.wasm.numThreads = 1;
    if (!tokens) {
      const t = await (await fetch('stimme/tokens.txt')).text();
      tokens = new Map();
      for (const line of t.split('\n')) { if (!line) continue; const i = line.lastIndexOf(' '); tokens.set(i === 0 ? ' ' : line.slice(0, i), parseInt(line.slice(i + 1), 10)); }
    }
    session = await ort.InferenceSession.create(new Uint8Array(buf), { executionProviders: ['wasm'] });
  }

  window.ExStimme = {
    /** Ist die Stimmdatei schon im Browser gespeichert? */
    installed: (done) => { cachedModel().then((b) => done(!!b)); },
    /** Laden (Fortschritt 0–99) und starten; done(Fehlertext oder null) */
    load: (progress, done) => {
      (async () => {
        let buf = await cachedModel();
        if (!buf) {
          abort = new AbortController();
          const r = await fetch(URL_MODEL, { signal: abort.signal });
          if (!r.ok) throw new Error('Der Server antwortet mit ' + r.status + '.');
          const reader = r.body.getReader(); const parts = []; let got = 0, shown = -1;
          for (;;) {
            const { done: end, value } = await reader.read(); if (end) break;
            parts.push(value); got += value.length;
            const p = Math.min(99, Math.floor(got * 100 / SIZE)); if (p !== shown) { shown = p; progress(p); }
          }
          const all = new Uint8Array(got); let o = 0; for (const p of parts) { all.set(p, o); o += p.length; }
          if (got !== SIZE || hex(await crypto.subtle.digest('SHA-256', all)) !== SHA) throw new Error('Die Datei ist unvollständig oder beschädigt angekommen.');
          buf = all.buffer;
          try { const c = await caches.open(CACHE); await c.put(URL_MODEL, new Response(all, { headers: { 'Content-Type': 'application/octet-stream' } })); } catch (e) { /* ohne Speicher: nur für diese Sitzung */ }
        }
        if (!session) await start(buf);
      })().then(() => done(null), (e) => done(e && e.name === 'AbortError' ? 'abgebrochen' : ((e && e.message) || 'Keine Verbindung. Bitte später noch einmal versuchen.')));
    },
    cancel: () => { if (abort) abort.abort(); },
    remove: (done) => { session = null; window.ExStimme.stop(); caches.delete(CACHE).then(() => done(), () => done()); },
    ready: () => !!session,
    /** Sätze in Lautschrift (getrennt durch Zeilenumbruch) sprechen; rate 0.6–1.6 */
    speak: (text, rate, volume, onStart, onEnd) => {
      const mine = ++turn; window.ExStimme.stopAudio();
      const A = window.__exAudio;
      if (!session || !A) { onEnd(); return; }
      const sentences = text.split('\n').filter((s) => s.trim());
      let t = A.currentTime + 0.05, started = false;
      (async () => {
        for (const ph of sentences) {
          if (mine !== turn) return;
          const ids = [1, 0];
          for (const ch of ph) { const id = tokens.get(ch); if (id !== undefined) ids.push(id, 0); }
          ids.push(2);
          const out = await session.run({
            input: new ort.Tensor('int64', BigInt64Array.from(ids.map((x) => BigInt(x))), [1, ids.length]),
            input_lengths: new ort.Tensor('int64', BigInt64Array.from([BigInt(ids.length)]), [1]),
            scales: new ort.Tensor('float32', Float32Array.from([0.667, 1 / rate, 0.8]), [3]),
          });
          if (mine !== turn) return;
          const pcm = out.output.data;
          const b = A.createBuffer(1, pcm.length, 22050); b.copyToChannel(pcm, 0);
          const s = A.createBufferSource(); s.buffer = b;
          const g = A.createGain(); g.gain.value = volume; s.connect(g); g.connect(A.destination);
          t = Math.max(t, A.currentTime + 0.02); s.start(t); t += b.duration + 0.12; playing.push(s);
          window.__exStimmeSamples = (window.__exStimmeSamples || 0) + pcm.length;   // für die Prüfung im Test
          if (!started) { started = true; onStart(); }
        }
        const wait = Math.max(0, (t - A.currentTime) * 1000);
        setTimeout(() => { if (mine === turn) onEnd(); }, wait);
      })().catch(() => { if (mine === turn) onEnd(); });
    },
    stopAudio: () => { for (const s of playing) { try { s.stop(); } catch (e) {} } playing = []; },
    stop: () => { turn++; window.ExStimme.stopAudio(); },
  };
})();
