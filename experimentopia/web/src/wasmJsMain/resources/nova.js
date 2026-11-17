// Vorlesen mit der Stimme „Nova“ (KI-Stimme von OpenAI, vorab auf unserem Stimmen-Server vertont – der Browser spricht nie mit OpenAI).
// Jeder Satz kommt einmal als MP3 vom Stimmen-Server und bleibt im Browser-Speicher („experimentopia-nova“). Neue Sätze (z. B. mit Messwerten)
// lässt der Server auf Anfrage vertonen (/jetzt) – geschickt wird nur der Text, keine Angaben zum Kind.
(() => {
  const SERVER = 'https://stimmen.mathenachhilfe-online.com';
  const CACHE = 'experimentopia-nova';
  let turn = 0, playing = [];

  async function mp3(text, url) {
    let c = null;
    try { c = await caches.open(CACHE); const hit = await c.match(url); if (hit) return await hit.arrayBuffer(); } catch (e) { /* ohne Speicher geht es trotzdem */ }
    let r = await fetch(url).catch(() => null);
    if (!r || !r.ok) {
      if (text.length > 300) return null;
      const a = await fetch(SERVER + '/jetzt', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text, sprache: 'de' }) }).catch(() => null);
      if (!a || a.status !== 200) return null;
      const j = await a.json().catch(() => null);
      if (!j || typeof j.url !== 'string' || !j.url.startsWith(SERVER + '/')) return null;
      r = await fetch(j.url).catch(() => null);
      if (!r || !r.ok) return null;
    }
    const b = await r.arrayBuffer();
    try { if (c) await c.put(url, new Response(b.slice(0), { headers: { 'Content-Type': 'audio/mpeg' } })); } catch (e) {}
    return b;
  }

  window.ExNova = {
    /** Sätze (Zeilen) mit den passenden Adressen (Zeilen) nacheinander sprechen; fehlt ein Satz, meldet fail(i) ab welchem Satz die Notlösung liest */
    speak: (texts, urls, onStart, onEnd, onFail) => {
      const mine = ++turn; window.ExNova.stopAudio();
      const A = window.__exAudio;
      const t = texts.split('\n'), u = urls.split('\n');
      if (!A) { onFail(0); return; }
      const loads = t.map((x, i) => mp3(x, u[i]).then((b) => (b ? A.decodeAudioData(b) : null)).catch(() => null));
      let at = A.currentTime + 0.05, started = false;
      (async () => {
        for (let i = 0; i < loads.length; i++) {
          const buf = await loads[i];
          if (mine !== turn) return;
          if (!buf) { const wait = Math.max(0, (at - A.currentTime) * 1000); setTimeout(() => { if (mine === turn) onFail(i); }, wait); return; }
          const s = A.createBufferSource(); s.buffer = buf; s.connect(A.destination);
          at = Math.max(at, A.currentTime + 0.02); s.start(at); at += buf.duration + 0.15; playing.push(s);
          if (!started) { started = true; onStart(); }
        }
        const wait = Math.max(0, (at - A.currentTime) * 1000);
        setTimeout(() => { if (mine === turn) onEnd(); }, wait);
      })().catch(() => { if (mine === turn) onEnd(); });
    },
    stopAudio: () => { for (const s of playing) { try { s.stop(); } catch (e) {} } playing = []; },
    stop: () => { turn++; window.ExNova.stopAudio(); },
  };
})();
