'use strict';
/* ================= Natürliche Stimme „Thorsten“ (nur Deutsch) =================
   Piper-Stimme de_DE-thorsten-medium (Aufnahmen CC0, Thorsten Müller), gerechnet im Browser mit onnxruntime-web (MIT) –
   derselbe Code im Web und in der Android-App. Ohne espeak-ng: Die Lautschrift steckt in einem vorab erzeugten Wörterbuch
   (tools/stimme/lautschrift.py); Zahlen werden aus Bausteinen zusammengesetzt. Kommt in einem Satz ein unbekanntes Wort vor,
   liest die Gerätestimme diesen Satz.
   Alle Dateien liegen auf unserem Server (CONFIG.voiceBase) und werden erst geladen, wenn jemand „Thorsten“ wählt. */

// ---------- Zahlen als Wörter (dieselbe Funktion erzeugt die Liste fürs Wörterbuch) ----------
const NW_ONES = ['null', 'eins', 'zwei', 'drei', 'vier', 'fünf', 'sechs', 'sieben', 'acht', 'neun', 'zehn', 'elf', 'zwölf', 'dreizehn', 'vierzehn', 'fünfzehn', 'sechzehn', 'siebzehn', 'achtzehn', 'neunzehn'];
const NW_TENS = ['', '', 'zwanzig', 'dreißig', 'vierzig', 'fünfzig', 'sechzig', 'siebzig', 'achtzig', 'neunzig'];
// 0–999 als ein Wort; pre = Form vor „tausend“/„hundert“ („ein“ statt „eins“)
function nw999(n, pre = false) {
  if (n < 20) return n === 1 ? (pre ? 'ein' : 'eins') : NW_ONES[n];
  if (n < 100) { const u = n % 10, t = NW_TENS[Math.floor(n / 10)]; return u ? (u === 1 ? 'ein' : NW_ONES[u]) + 'und' + t : t; }
  const h = Math.floor(n / 100), r = n % 100;
  return (h === 1 ? '' : NW_ONES[h]) + 'hundert' + (r ? nw999(r, pre) : '');
}
// beliebige ganze Zahl → Wörter; „+“ verbindet Teile eines Wortes ohne Pause
function numWords(n) {
  if (n < 0) return 'minus ' + numWords(-n);
  if (n < 1000) return nw999(n);
  const parts = [];
  const bil = Math.floor(n / 1e9), mil = Math.floor(n / 1e6) % 1000, th = Math.floor(n / 1000) % 1000, r = n % 1000;
  if (bil) parts.push(bil === 1 ? 'eine Milliarde' : nw999(bil) + ' Milliarden');
  if (mil) parts.push(mil === 1 ? 'eine Million' : nw999(mil) + ' Millionen');
  let w = '';
  if (th) w = (th === 1 ? '' : nw999(th, true) + '+') + 'tausend';
  if (r) w += (w ? '+' : '') + nw999(r);
  if (w) parts.push(w);
  return parts.join(' ');
}
const NW_ORD = { 1: 'ersten', 3: 'dritten', 7: 'siebten', 8: 'achten' };
const ordWord = n => NW_ORD[n] || (n < 20 ? nw999(n) + 'ten' : nw999(n) + 'sten');
const digitsWords = s => [...s].map(d => NW_ONES[+d]).join(' ');
const MONTHS = 'Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember';
// Sprechtext (aus mathSpeak) → nur noch Wörter
function thorstenNormalize(s) {
  s = ' ' + s + ' ';
  s = s.replace(/(\d)\s?°C/g, '$1 Grad Celsius');
  s = s.replace(/(\d{1,2}):(\d\d)( Uhr)?/g, (m, h, mi, u) => `${numWords(+h)} Uhr${+mi ? ' ' + numWords(+mi) : ''}`);
  s = s.replace(new RegExp(`(\\d{1,2})\\. (${MONTHS})`, 'g'), (m, d, mo) => `${ordWord(+d)} ${mo}`);
  s = s.replace(/(\d+),(\d\d) Euro/g, (m, e, c) => +e ? `${numWords(+e)} Euro${+c ? ' ' + numWords(+c) : ''}` : `${numWords(+c)} Cent`);
  s = s.replace(/(\d{1,3}(?:\.\d{3})+)(?![\d])/g, m => m.replace(/\./g, ''));   // Tausenderpunkte
  s = s.replace(/(\d+),(\d+)/g, (m, a, b) => `${numWords(+a)} Komma ${digitsWords(b)}`);
  s = s.replace(/(\d)([a-z]{1,3})\b/g, (m, d, v) => d + ' ' + [...v].join(' '));   // „2x“ → „2 x“, „2ab“ → „2 a b“
  s = s.replace(/\d+/g, m => ' ' + numWords(+m) + ' ');
  return s.replace(/\s+/g, ' ').trim();
}

const Thorsten = {
  st: 'off', pct: 0, dict: null, ids: null, session: null, man: null, tok: 0, src: [],
  base: () => (CONFIG.voiceBase || '').replace(/\/?$/, '/'),
  can: () => !!CONFIG.voiceBase && I18N.lang === 'de' && typeof WebAssembly === 'object',
  // gibt es die Stimme auf dem Server? (kleine Datei stimme.json, wird nur in den Einstellungen abgefragt)
  async manifest() {
    if (this.man !== null) return this.man;
    if (!this.can()) return (this.man = false);
    const url = this.base() + 'stimme.json'; let c = null; try { c = await caches.open('rb-stimme'); } catch {}
    try { const r = await fetch(url, { cache: 'no-cache' }); this.man = r.ok ? await r.clone().json() : false; if (r.ok && c) c.put(url, r).catch(() => {}); }
    catch { try { const hit = c && await c.match(url); this.man = hit ? await hit.json() : false; } catch { this.man = false; } }   // offline: gespeicherte Angaben
    return this.man;
  },
  async cached() { try { const c = await caches.open('rb-stimme'); return !!(await c.match(this.base() + this.man.model)); } catch { return false; } },
  async fetchCached(url, onProg) {
    let c = null; try { c = await caches.open('rb-stimme'); const hit = await c.match(url); if (hit) return hit.arrayBuffer(); } catch {}
    const r = await fetch(url); if (!r.ok) throw new Error('HTTP ' + r.status);
    const total = +r.headers.get('content-length') || 0, rd = r.body.getReader(), parts = []; let got = 0;
    for (;;) { const { done, value } = await rd.read(); if (done) break; parts.push(value); got += value.length; if (onProg && total) onProg(got / total); }
    const blob = new Blob(parts); if (c) { try { await c.put(url, new Response(blob)); } catch {} }
    return blob.arrayBuffer();
  },
  loadScript: src => new Promise((ok, no) => { if (window.ort) return ok(); const s = document.createElement('script'); s.src = src; s.onload = ok; s.onerror = () => no(new Error('ort')); document.head.appendChild(s); }),
  async load(onProg = () => {}) {
    if (this.st === 'ready') return true;
    if (this.loading) return this.loading;
    this.loading = (async () => {
      try {
        this.st = 'loading'; const m = await this.manifest(); if (!m) throw new Error('keine Stimme');
        const B = this.base();
        // auch das Rechenwerk kommt aus dem Gerätespeicher (offline, kein zweiter Download)
        const blobUrl = async (f, type) => URL.createObjectURL(new Blob([await this.fetchCached(B + f)], { type }));
        const [js, mjs, wasm] = await Promise.all([blobUrl(m.ort, 'text/javascript'), blobUrl('ort-wasm-simd-threaded.mjs', 'text/javascript'), blobUrl('ort-wasm-simd-threaded.wasm', 'application/wasm')]);
        await this.loadScript(js);
        ort.env.wasm.wasmPaths = { mjs, wasm }; ort.env.wasm.numThreads = 1;
        const [dict, model] = await Promise.all([this.fetchCached(B + m.dict).then(b => JSON.parse(new TextDecoder().decode(b))), this.fetchCached(B + m.model, p => { this.pct = p; onProg(p); })]);
        this.dict = dict.w; this.idmap = dict.ids; this.scales = dict.scales;
        this.session = await ort.InferenceSession.create(new Uint8Array(model), { executionProviders: ['wasm'] });
        this.st = 'ready'; return true;
      } catch (e) { this.st = 'error'; this.err = String(e.message || e); return false; } finally { this.loading = null; }
    })();
    return this.loading;
  },
  word(w) { if (!w) return []; const d = this.dict; return d[w] ?? d[w.toLowerCase()] ?? d[w[0].toUpperCase() + w.slice(1).toLowerCase()]; },
  // Satz → Phonem-IDs (Piper: BOS, PAD, je Laut + PAD, EOS); null, wenn ein Wort fehlt
  sentenceIds(text) {
    const M = this.idmap, out = [M['^'], M._];
    const push = id => { out.push(id, M._); };
    const tokens = thorstenNormalize(text).match(/[A-Za-zÄÖÜäöüß+]+|[.,!?;:]/g) || [];
    for (let i = 0; i < tokens.length; i++) {
      const t = tokens[i];
      if (/^[.,!?;:]$/.test(t)) { if (out.length > 2 && out[out.length - 2] === M[' ']) out.splice(-2, 2); push(M[t]); push(M[' ']); continue; }
      for (const part of t.split('+').filter(Boolean)) {
        let p = this.word(part);
        if (p === undefined && part.length <= 3 && /^[a-zA-Z]+$/.test(part)) p = [...part].map(ch => this.word(ch)).every(x => x !== undefined) ? [...part].flatMap((ch, j) => [...(j ? [M[' ']] : []), ...this.word(ch)]) : undefined;
        if (p === undefined) return null;
        p.forEach(push);
      }
      push(M[' ']);
    }
    while (out.length > 2 && out[out.length - 2] === M[' ']) out.splice(-2, 2);
    if (out[out.length - 2] !== M['.'] && out[out.length - 2] !== M['?'] && out[out.length - 2] !== M['!']) push(M['.']);
    out.push(M.$);
    return out.length > 4 ? out : [];
  },
  async synth(ids, rate) {
    const n = ids.length, input = new ort.Tensor('int64', BigInt64Array.from(ids.map(BigInt)), [1, n]);
    const res = await this.session.run({ input, input_lengths: new ort.Tensor('int64', BigInt64Array.from([BigInt(n)]), [1]), scales: new ort.Tensor('float32', Float32Array.from([this.scales[0], 1 / rate, this.scales[1]]), [3]) });
    return res.output.data;
  },
  // Text in Sätze teilen; true, wenn Thorsten alles sprechen kann (sonst übernimmt die Gerätestimme)
  plan(text) { return text.split(/(?<=[.!?])\s+/).filter(x => x.trim()).map(s => this.sentenceIds(s)); },
  stop() { this.tok++; this.src.forEach(s => { try { s.stop(); } catch {} }); this.src = []; this.endAt = 0; },
  // beim Start still nachladen, wenn die Stimme schon auf dem Gerät gespeichert ist
  async warm() { if (this.st !== 'off' || !(await this.manifest()) || !(await this.cached())) return; this.load(); },
  async speak(text, rate = 1, queue = false) {
    const plan = this.plan(text); if (plan.some(p => p === null)) return false;
    if (!queue) this.stop(); const tok = this.tok; Sound.init(); const c = Sound.ctx; if (!c) return false;
    let at = Math.max(c.currentTime + .05, queue ? this.endAt || 0 : 0);
    for (const ids of plan) {
      if (!ids.length) continue;
      const pcm = await this.synth(ids, rate); if (tok !== this.tok) return true;
      const buf = c.createBuffer(1, pcm.length, 22050); buf.copyToChannel(pcm, 0);
      const s = c.createBufferSource(), g = c.createGain(); g.gain.value = 1.4; s.buffer = buf; s.connect(g).connect(c.destination);
      at = Math.max(at, c.currentTime + .02); s.start(at); at += buf.duration + .12; this.endAt = at; this.src.push(s);
      s.onended = () => { this.src = this.src.filter(x => x !== s); };
    }
    return true;
  },
};
