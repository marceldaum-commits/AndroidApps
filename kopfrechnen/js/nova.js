'use strict';
/* ================= Vorlesen mit der KI-Stimme „Nova“ =================
   Rechenblitz liest nur noch mit Nova vor (Vorgabe Marcel 17.11.). Die App schickt nie etwas an OpenAI:
   1. Jeder Text hat eine feste Adresse auf unserem Stimmen-Server:
      https://stimmen.mathenachhilfe-online.com/nova/<h[0:2]>/<h>.mp3, h = SHA-256("nova|" + sprache + "|" + text_normalisiert), erste 32 Hex-Zeichen.
      Feste Texte (Theorie, Blitz) stehen in vorlesen/texte.json und sind dort vorab vertont.
   2. Fehlt die Datei (z. B. eine neue Rechenaufgabe), fragt die App unseren Server: POST /jetzt {text, sprache} (höchstens 300 Zeichen).
      Er lässt den Text einmalig vertonen und antwortet mit {url}. Antwort 204 oder kein Netz → still die Gerätestimme.
   Es werden keine Namen, Konten oder Geräte-Kennungen übertragen. Tempo (1,25) legt der Server fest. */
// Sprechtempo aus den Einstellungen (1,0 = Server-Tempo); Stufen wie bei allen Apps
const RATES = [0.75, 0.9, 1, 1.15, 1.3, 1.5];
const speechRate = () => RATES.includes(S.settings.rate) ? S.settings.rate : 1;
const novaNorm = s => String(s).normalize('NFC').replace(/\s+/g, ' ').trim();
const Nova = {
  miss: new Set(), pending: new Map(), audio: null,
  cfg: () => CONFIG.nova || {},
  base: () => (Nova.cfg().base || '').replace(/\/?$/, '/'),
  can: () => !!Nova.cfg().base && !!(window.crypto && crypto.subtle),
  // In der Android-App normales fetch (nicht über die Capacitor-Brücke)
  get: (url, o) => (window.CapacitorWebFetch || fetch)(url, o),
  async hash(lang, text) {
    const d = await crypto.subtle.digest('SHA-256', new TextEncoder().encode('nova|' + lang + '|' + novaNorm(text)));
    return [...new Uint8Array(d)].map(b => b.toString(16).padStart(2, '0')).join('').slice(0, 32);
  },
  async url(lang, text) { const h = await this.hash(lang, text); return `${this.base()}${h.slice(0, 2)}/${h}.mp3`; },
  // keine Namen an den Server: Texte mit einem Profilnamen liest die Gerätestimme
  private(text) { try { return Object.values(ROOT.profiles || {}).some(p => p && p.name && p.name.length > 1 && text.includes(p.name)); } catch { return false; } },
  async cachePut(url, blob) { try { const c = await caches.open('rb-nova'); await c.put(url, new Response(blob, { headers: { 'Content-Type': 'audio/mpeg' } })); } catch {} },
  // Audiodatei holen: Gerätespeicher → feste Adresse → /jetzt; null = nicht vorhanden (dann Gerätestimme)
  async src(text, lang) {
    const key = lang + '|' + novaNorm(text);
    if (!this.can() || this.miss.has(key) || this.private(text)) return null;
    if (this.pending.has(key)) return this.pending.get(key);
    const job = (async () => {
      const url = await this.url(lang, text);
      try { const c = await caches.open('rb-nova'); const hit = await c.match(url); if (hit) return hit.blob(); } catch {}
      try {
        let r = await this.get(url);
        if (!r.ok && r.status === 404 && this.cfg().jetzt && novaNorm(text).length <= 300) {
          const j = await this.get(this.cfg().jetzt, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text: novaNorm(text), sprache: lang }) });
          if (j.status !== 200) { this.miss.add(key); return null; }   // 204: gibt es nicht → Gerätestimme
          const u = (await j.json()).url; if (!u) { this.miss.add(key); return null; }
          r = await this.get(new URL(u, this.cfg().jetzt).href);
        }
        if (!r.ok) { this.miss.add(key); return null; }
        const b = await r.blob(); this.cachePut(url, b); return b;
      } catch { return null; }   // offline: diesmal Gerätestimme, später wieder versuchen
    })();
    this.pending.set(key, job); job.finally(() => setTimeout(() => this.pending.delete(key), 60000));
    return job;
  },
  // vorab holen (z. B. die nächste Aufgabe in der Blitzrunde)
  prefetch(text, lang = I18N.lang) { if (text) this.src(text, lang); },
  stop() { if (this.audio) { try { this.audio.pause(); } catch {} this.audio = null; } },
  // true = mit Nova gesprochen (oder abgebrochen); false = Gerätestimme übernehmen
  async play(text, lang, cancelled = () => false) {
    const blob = await this.src(text, lang); if (!blob) return false;
    if (cancelled()) return true;
    const a = new Audio(URL.createObjectURL(blob)); a.preservesPitch = true; a.playbackRate = speechRate(); this.audio = a;   // Sprechtempo, gleiche Tonhöhe
    try { await a.play(); } catch { this.audio = null; return false; }
    await new Promise(res => { a.onended = a.onerror = a.onpause = res; });
    URL.revokeObjectURL(a.src); if (this.audio === a) this.audio = null;
    return true;
  },
};
// ---------- Alle festen Nova-Texte offline laden (Vorgabe Marcel 17.11.) ----------
// Nur die eingestellte Sprache, nur fehlende Dateien, 4 parallel; Abbrechen und später weitermachen; 404 still überspringen.
// Gespeichert im Cache-Speicher der App (Web: Cache Storage, Android: derselbe Speicher im privaten App-Bereich der WebView).
const NovaOffline = {
  busy: false, stopFlag: false,
  host: () => (typeof IS_NATIVE !== 'undefined' && IS_NATIVE) ? 'rechenblitz.mathenachhilfe-online.com' : location.hostname,
  async texts(lang = I18N.lang) { try { const j = await (await fetch('vorlesen/texte.json')).json(); return (j.texte || []).filter(t => t.sprache === lang).map(t => t.text); } catch { return []; } },
  // Größe aller Dateien laut Server (live, wird kleiner, wenn der Server neu kodiert)
  async stand() { try { const r = await Nova.get(`${Nova.base()}stand-${this.host()}.json`, { cache: 'no-cache' }); return r.ok ? await r.json() : null; } catch { return null; } },
  async cache() { try { return await caches.open('rb-nova'); } catch { return null; } },
  // was liegt schon auf dem Gerät: have (beliebige Qualität) / best (Bestqualität) dieser Sprache, total, bytes (alle Nova-Dateien)
  async status(lang = I18N.lang) {
    const c = await this.cache(), list = await this.texts(lang); let have = 0, best = 0, bytes = 0;
    if (c) { for (const t of list) { const r = await c.match(await Nova.url(lang, t)); if (r) { have++; if (r.headers.get('X-Nova-Q') === 'best') best++; } }
      for (const k of await c.keys()) { try { const r = await c.match(k); bytes += (await r.blob()).size; } catch {} } }
    return { have, best, total: list.length, bytes };
  },
  // geschätzte Download-Größe (MB) für die fehlenden Dateien dieser Sprache – Standard (40 kbit/s) und Bestqualität (128 kbit/s)
  async estimate(lang = I18N.lang) {
    const [st, s] = await Promise.all([this.stand(), this.status(lang)]);
    if (!st || !st.texte) return { ...s, std: null, best: null };
    const mb = (bytes, n) => bytes ? Math.max(1, Math.round(bytes / st.texte * n / 1e6)) : null;
    return { ...s, std: mb(st.bytes, s.total - s.have), bestMb: mb(st.bytes_original || st.bytes, s.total - s.best) };
  },
  stop() { this.stopFlag = true; },
  async load(onProg = () => {}, q = 'std', lang = I18N.lang) {
    if (this.busy) return false; this.busy = true; this.stopFlag = false;
    try {
      const c = await this.cache(); if (!c) return false;
      const list = await this.texts(lang), todo = [];
      for (const t of list) { const u = await Nova.url(lang, t), r = await c.match(u); if (!r || (q === 'best' && r.headers.get('X-Nova-Q') !== 'best')) todo.push(u); }
      let done = list.length - todo.length; onProg(done, list.length);
      const fetchOne = async u => {
        if (q === 'best') {   // Bestqualität: …/nova/original/…; gibt es die nicht (404), die normale Datei
          try { const r = await Nova.get(u.replace(Nova.base(), Nova.base() + 'original/')); if (r.ok) return [await r.blob(), 'best']; } catch {}
        }
        const r = await Nova.get(u); return r.ok ? [await r.blob(), q === 'best' ? 'best' : 'std'] : null;   // ohne Original: normale Datei gilt als beste verfügbare
      };
      const worker = async () => {
        while (todo.length && !this.stopFlag) {
          const u = todo.shift();
          try { const got = await fetchOne(u); if (got) await c.put(u, new Response(got[0], { headers: { 'Content-Type': 'audio/mpeg', 'X-Nova-Q': got[1] } })); } catch {}   // 404/Fehler: still überspringen
          done++; onProg(done, list.length);
        }
      };
      await Promise.all([worker(), worker(), worker(), worker()]);
      return !this.stopFlag;
    } finally { this.busy = false; }
  },
  async clear() { try { await caches.delete('rb-nova'); } catch {} Nova.pending.clear(); },
};
// Speicher der früheren Zusatzstimme (bis 3.24.x) vom Gerät räumen
try { caches.delete('rb-stimme'); } catch {}
