'use strict';
/* ================= KI-Stimme „Nova“ (vorab vertont) =================
   Die festen Vorlese-Texte (Theorie, Blitz-Sätze) stehen in vorlesen/texte.json (beim Bauen erzeugt, android-app/vorlesen.js).
   Unser Stimmen-Werk vertont sie (OpenAI-Stimme „nova“) und legt sie auf unserem Server ab:
   https://stimmen.mathenachhilfe-online.com/nova/<h[0:2]>/<h>.mp3, h = SHA-256("nova|" + sprache + "|" + text_normalisiert), erste 32 Hex-Zeichen.
   Die App schickt nie etwas an OpenAI – sie lädt nur fertige MP3-Dateien von unserem Server. Fehlt eine Datei, liest still die nächste Stimme. */
const novaNorm = s => String(s).normalize('NFC').replace(/\s+/g, ' ').trim();
const Nova = {
  set: null, miss: new Set(), audio: null,
  base: () => (CONFIG.nova && CONFIG.nova.base || '').replace(/\/?$/, '/'),
  can: () => !!(CONFIG.nova && CONFIG.nova.base),
  get: (url, o) => (window.CapacitorWebFetch || fetch)(url, o),
  // Liste der vertonten Texte (liegt in der App selbst)
  async list() {
    if (this.set) return this.set;
    this.set = new Set();
    try { const j = await (await fetch('vorlesen/texte.json')).json(); for (const t of j.texte || []) this.set.add(t.sprache + '|' + novaNorm(t.text)); } catch {}
    return this.set;
  },
  async hash(lang, text) {
    const d = await crypto.subtle.digest('SHA-256', new TextEncoder().encode('nova|' + lang + '|' + novaNorm(text)));
    return [...new Uint8Array(d)].map(b => b.toString(16).padStart(2, '0')).join('').slice(0, 32);
  },
  async url(lang, text) { const h = await this.hash(lang, text); return `${this.base()}${h.slice(0, 2)}/${h}.mp3`; },
  async file(url) {
    let c = null; try { c = await caches.open('rb-nova'); const hit = await c.match(url); if (hit) return hit.blob(); } catch {}
    const r = await this.get(url); if (!r.ok) throw new Error('HTTP ' + r.status);
    const b = await r.blob(); if (c) { try { await c.put(url, new Response(b, { headers: { 'Content-Type': 'audio/mpeg' } })); } catch {} }
    return b;
  },
  stop() { if (this.audio) { try { this.audio.pause(); } catch {} this.audio = null; } },
  // spielt den Text, wenn es ihn vertont gibt; false = nicht vorhanden → nächste Stimme
  async play(text, lang, rate = 1, cancelled = () => false) {
    if (!this.can() || !crypto.subtle) return false;
    const key = lang + '|' + novaNorm(text);
    if (this.miss.has(key) || !(await this.list()).has(key)) return false;
    let blob; try { blob = await this.file(await this.url(lang, text)); } catch { this.miss.add(key); return false; }
    if (cancelled()) return true;
    const a = new Audio(URL.createObjectURL(blob)); a.playbackRate = rate; a.preservesPitch = true; this.audio = a;
    try { await a.play(); } catch { this.audio = null; return false; }
    await new Promise(res => { a.onended = a.onerror = a.onpause = res; });
    URL.revokeObjectURL(a.src); if (this.audio === a) this.audio = null;
    return true;
  },
};
