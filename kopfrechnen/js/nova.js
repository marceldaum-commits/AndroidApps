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
// Reste der früheren Stimme „Thorsten“ vom Gerät räumen (bis 3.24.x)
try { caches.delete('rb-stimme'); } catch {}
