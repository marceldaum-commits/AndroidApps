// Vorlesen mit der Stimme „Nova“ (KI-Stimme von OpenAI, vorab auf unserem Stimmen-Server vertont – der Browser spricht nie mit OpenAI).
// Jeder Satz kommt einmal als MP3 vom Stimmen-Server und bleibt im Browser-Speicher („experimentopia-nova“). Neue Sätze (z. B. mit Messwerten)
// lässt der Server auf Anfrage vertonen (/jetzt) – geschickt wird nur der Text, keine Angaben zum Kind.
(() => {
  const SERVER = 'https://stimmen.mathenachhilfe-online.com';
  const CACHE = 'experimentopia-nova';
  let turn = 0;

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

  // Abgespielt über <audio>: so geht schneller/langsamer bei gleicher Tonhöhe (preservesPitch)
  let current = null;
  function play(buf, speed, mine) {
    return new Promise((done) => {
      if (mine !== turn) { done(); return; }
      const url = URL.createObjectURL(new Blob([buf], { type: 'audio/mpeg' }));
      const a = new Audio(url);
      a.preservesPitch = true; a.mozPreservesPitch = true; a.webkitPreservesPitch = true;
      a.playbackRate = speed;
      const end = () => { URL.revokeObjectURL(url); if (current === a) current = null; done(); };
      a.onended = end; a.onerror = end; a.onpause = end;
      current = a;
      a.play().catch(end);
    });
  }

  window.ExNova = {
    /** Sätze (Zeilen) mit den passenden Adressen (Zeilen) nacheinander sprechen; fehlt ein Satz, meldet fail(i), ab welchem Satz die Notlösung liest */
    speak: (texts, urls, speed, onStart, onEnd, onFail) => {
      const mine = ++turn; window.ExNova.stopAudio();
      const t = texts.split('\n'), u = urls.split('\n');
      const loads = t.map((x, i) => mp3(x, u[i]).catch(() => null));
      (async () => {
        for (let i = 0; i < loads.length; i++) {
          const buf = await loads[i];
          if (mine !== turn) return;
          if (!buf) { onFail(i); return; }
          if (i === 0) onStart();
          await play(buf, speed, mine);
          if (mine !== turn) return;
          if (i < loads.length - 1) await new Promise((r) => setTimeout(r, 120));
        }
        if (mine === turn) onEnd();
      })().catch(() => { if (mine === turn) onEnd(); });
    },
    stopAudio: () => { if (current) { const a = current; current = null; try { a.pause(); } catch (e) {} } },
    stop: () => { turn++; window.ExNova.stopAudio(); },
  };
})();
