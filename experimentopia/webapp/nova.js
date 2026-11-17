// Vorlesen mit der Stimme „Nova“ (KI-Stimme von OpenAI, vorab auf unserem Stimmen-Server vertont – der Browser spricht nie mit OpenAI).
// Jeder Satz kommt einmal als MP3 vom Stimmen-Server und bleibt im Browser-Speicher („experimentopia-nova“). Neue Sätze (z. B. mit Messwerten)
// lässt der Server auf Anfrage vertonen (/jetzt) – geschickt wird nur der Text, keine Angaben zum Kind.
(() => {
  const SERVER = 'https://stimmen.mathenachhilfe-online.com';
  const CACHE = 'experimentopia-nova';
  let turn = 0, bulk = 0;

  /** Was schon gespeichert ist: Bytes aller Sätze und wie viele der festen Sätze da sind */
  async function saved(list) {
    try {
      const c = await caches.open(CACHE); const keys = await c.keys(); let bytes = 0;
      for (const k of keys) { const r = await c.match(k); const x = r && r.headers.get('X-Bytes'); bytes += x ? parseInt(x, 10) : (r ? (await r.blob()).size : 0); }
      const set = new Set(keys.map((k) => k.url));
      return { bytes, have: list.filter((u) => set.has(u)).length };
    } catch (e) { return { bytes: 0, have: 0 }; }
  }

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
    try { if (c) await c.put(url, new Response(b.slice(0), { headers: { 'Content-Type': 'audio/mpeg', 'X-Bytes': String(b.byteLength) } })); } catch (e) {}
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
    /** Größe aller Sätze laut Server (-1 = unbekannt) und was schon im Browser liegt; done(total, saved, vorhandeneAdressen) */
    info: (host, urls, done) => {
      (async () => {
        let total = -1;
        try { const r = await fetch(SERVER + '/nova/stand-' + host + '.json', { cache: 'no-store' }); if (r.ok) { const j = await r.json(); if (typeof j.bytes === 'number') total = j.bytes; } } catch (e) {}
        const s = await saved(urls.split('\n'));
        done(total, s.bytes, s.have);
      })();
    },
    /** Alle fehlenden Sätze laden: 4 gleichzeitig, 404 still überspringen; progress(fertig, alle), finished(Hinweis oder '') */
    loadAll: (urls, progress, finished) => {
      bulk++; const mine = bulk;
      (async () => {
        const list = urls.split('\n');
        let c; try { c = await caches.open(CACHE); } catch (e) { finished('Dieser Browser erlaubt keinen Speicher (privates Fenster?).'); return; }
        const have = new Set((await c.keys()).map((k) => k.url));
        const todo = list.filter((u) => !have.has(u));
        let n = list.length - todo.length, failed = 0, next = 0;
        progress(n, list.length);
        async function worker() {
          while (next < todo.length && mine === bulk) {
            const u = todo[next++];
            try {
              const r = await fetch(u);
              if (r.ok) { const b = await r.arrayBuffer(); await c.put(u, new Response(b, { headers: { 'Content-Type': 'audio/mpeg', 'X-Bytes': String(b.byteLength) } })); }
              else if (r.status !== 404) failed++;
            } catch (e) { failed++; }
            n++; if (n % 10 === 0 || n === list.length) progress(n, list.length);
          }
        }
        await Promise.all([0, 1, 2, 3].map(worker));
        if (mine !== bulk) { finished('Angehalten – beim nächsten Tippen geht es dort weiter.'); return; }
        finished(failed > 0 ? 'Einige Sätze kamen nicht an (keine Verbindung?). Nochmal tippen lädt den Rest.' : '');
      })();
    },
    cancelAll: () => { bulk++; },
    clear: (done) => { bulk++; try { caches.delete(CACHE).then(() => done(), () => done()); } catch (e) { done(); } },
    stopAudio: () => { if (current) { const a = current; current = null; try { a.pause(); } catch (e) {} } },
    stop: () => { turn++; window.ExNova.stopAudio(); },
  };
})();
