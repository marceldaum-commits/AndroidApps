// Aussprache-Auswertung (Weg 2: über unseren Server in Deutschland) – Client, Stand 24.11.2026.
// Vorbereitet und AUS: Solange window.AUSWERTUNG_AKTIV nicht true ist (version.js), gibt es weder Auswahl noch einen einzigen Aufruf.
// Regeln (Recht V12): nur wenn die Eltern „Über unseren Server“ gewählt haben; nur Aufnahme, Zielsatz und Sprache werden gesendet
// (keine Kennung, keine Cookies, kein Gerätename, kein Referrer); Aufnahme nur im Arbeitsspeicher, höchstens 15 s und 1 MB;
// Deutsch und Latein werden nie gesendet; nach 8 s Abbruch; bei Fehler/Überlastung keine Sterne, neutraler Hinweis.
// Schnittstelle: SPRACHE-API.md
(function () {
  const W = {};
  W.ENDPUNKT = '/sprache/v1/auswerten';
  W.SPRACHEN = ['en', 'fr', 'es'];          // L6: de und la bleiben gesperrt
  W.MAX_SEKUNDEN = 15; W.MAX_BYTES = 1000000; W.ZEITLIMIT_MS = 8000;
  const S = () => window.APP_SETTINGS || {};
  W.aktiv = () => window.AUSWERTUNG_AKTIV === true;
  /** 'geraet' | 'server' | 'aus' – die Wahl der Eltern. Server ist NIE voreingestellt. */
  W.wahl = () => (['geraet', 'server', 'aus'].includes(S().auswertung) ? S().auswertung : 'aus');
  /** Soll für diese Sprache der Server gefragt werden? */
  W.serverFuer = (lang) => W.aktiv() && W.wahl() === 'server' && W.SPRACHEN.includes(lang);

  const norm = (t) => String(t).normalize('NFC').toLowerCase().replace(/[’`]/g, "'").replace(/[^\p{L}\p{N}' ]+/gu, ' ').replace(/\s+/g, ' ').trim();
  /** Anteil der Zielwörter, die im Erkannten vorkommen (Reihenfolge egal, jedes Wort einmal). */
  W.treffer = (ziel, erkannt) => {
    const z = norm(ziel).split(' ').filter(Boolean); const rest = norm(erkannt).split(' ').filter(Boolean);
    if (!z.length) return 0; let n = 0;
    for (const w of z) { const i = rest.indexOf(w); if (i >= 0) { n++; rest.splice(i, 1); } }
    return n / z.length;
  };
  /** Sterne nach echter Übereinstimmung: ★★★ ab 90 %, ★★ ab 70 %, ★ ab 50 %. Französisch großzügiger (Server trifft dort seltener). */
  W.sterne = (lang, t) => { const g = lang === 'fr' ? [0.8, 0.6, 0.4] : [0.9, 0.7, 0.5]; return t >= g[0] ? 3 : t >= g[1] ? 2 : t >= g[2] ? 1 : 0; };

  /** Aufnahme (Blob) → WAV mono 16 kHz 16 bit, höchstens MAX_SEKUNDEN. */
  W.zuWav = async (blob) => {
    const AC = window.AudioContext || window.webkitAudioContext; const OAC = window.OfflineAudioContext || window.webkitOfflineAudioContext;
    const c = new AC(); let buf;
    try { const ab = await await_ab(blob); buf = await new Promise((ok, no) => { const p = c.decodeAudioData(ab, ok, no); if (p && p.then) p.then(ok, no); }); } catch (e) { buf = null; }
    try { c.close(); } catch (e) {}
    if (!buf) throw new Error('decode');
    const sek = Math.min(buf.duration, W.MAX_SEKUNDEN); const n = Math.max(1, Math.ceil(sek * 16000));
    const off = new OAC(1, n, 16000); const src = off.createBufferSource(); src.buffer = buf; src.connect(off.destination); src.start(0);
    const out = (await off.startRendering()).getChannelData(0);
    const wav = new DataView(new ArrayBuffer(44 + out.length * 2)); const w = (o, s) => { for (let i = 0; i < s.length; i++) wav.setUint8(o + i, s.charCodeAt(i)); };
    w(0, 'RIFF'); wav.setUint32(4, 36 + out.length * 2, true); w(8, 'WAVEfmt '); wav.setUint32(16, 16, true); wav.setUint16(20, 1, true); wav.setUint16(22, 1, true);
    wav.setUint32(24, 16000, true); wav.setUint32(28, 32000, true); wav.setUint16(32, 2, true); wav.setUint16(34, 16, true); w(36, 'data'); wav.setUint32(40, out.length * 2, true);
    for (let i = 0; i < out.length; i++) wav.setInt16(44 + i * 2, Math.max(-1, Math.min(1, out[i])) * 32767, true);
    return new Blob([wav], { type: 'audio/wav' });
  };
  // Blob → ArrayBuffer (ältere Safari-Versionen haben blob.arrayBuffer() nicht überall)
  function await_ab(blob) { return blob.arrayBuffer ? blob.arrayBuffer() : new Promise((ok) => { const r = new FileReader(); r.onload = () => ok(r.result); r.readAsArrayBuffer(blob); }); }

  /**
   * Schickt die Aufnahme zur Auswertung. Liefert immer ein Objekt:
   *   { status: 'ok', treffer, sterne, text } | { status: 'nicht_moeglich', grund }
   * Ohne Auswahl „Server“ (oder bei de/la) wird NICHTS gesendet: { status: 'nicht_gefragt' }.
   */
  W.pruefen = async (lang, ziel, blob) => {
    if (!W.serverFuer(lang) || !blob) return { status: 'nicht_gefragt' };
    let wav;
    try { wav = await W.zuWav(blob); } catch (e) { return { status: 'nicht_moeglich', grund: 'aufnahme' }; }
    if (wav.size > W.MAX_BYTES) return { status: 'nicht_moeglich', grund: 'zu_gross' };
    const fd = new FormData(); fd.append('sprache', lang); fd.append('ziel', String(ziel).slice(0, 300)); fd.append('audio', wav, 'aufnahme.wav');
    const ac = typeof AbortController !== 'undefined' ? new AbortController() : null; const t = setTimeout(() => ac && ac.abort(), W.ZEITLIMIT_MS);
    try {
      const r = await fetch(W.ENDPUNKT, { method: 'POST', body: fd, credentials: 'omit', referrerPolicy: 'no-referrer', cache: 'no-store', mode: 'same-origin', signal: ac ? ac.signal : undefined });
      if (!r.ok) return { status: 'nicht_moeglich', grund: 'http_' + r.status };
      const j = await r.json(); const text = String(j.text || '');
      const tr = typeof j.treffer === 'number' ? j.treffer : W.treffer(ziel, text);
      return { status: 'ok', treffer: tr, sterne: W.sterne(lang, tr), text };
    } catch (e) { return { status: 'nicht_moeglich', grund: e && e.name === 'AbortError' ? 'zeit' : 'netz' }; } finally { clearTimeout(t); }
  };
  window.AUSWERTUNG = W;
})();
