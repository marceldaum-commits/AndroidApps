// Offline-Download je Sprache (Marcel 20.11.2026): Eltern laden die vorab vertonten Sätze, Pakete und den Lernpfad einer Sprache
// einmalig herunter ("am besten im WLAN"), können sie wieder löschen, und bei einer neuen Vertonung wird automatisch aktualisiert.
// Gespeichert wird im Browser-Speicher "quasselquest-offline" (bleibt bei App-Updates erhalten). Englisch ist immer offline.
(function () {
  const O = {}; const CACHE = 'quasselquest-offline'; const KEY = 'quasselquest-offline-sprachen';
  const lesen = () => { try { return JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { return {}; } };
  const schreiben = (o) => { try { localStorage.setItem(KEY, JSON.stringify(o)); } catch (e) {} };
  O.unterstuetzt = () => 'caches' in window && 'serviceWorker' in navigator;
  O.geladen = (lang) => !!lesen()[lang];
  O.version = (lang) => lesen()[lang] || null;
  let indexCache = null;
  const audioIndex = () => indexCache || (indexCache = fetch('audio/index.json').then((r) => r.json()).catch(() => ({ saetze: {}, groessen: {} })));
  /** Ungefähre Größe in Bytes (aus audio/index.json). */
  O.groesse = async (lang) => ((await audioIndex()).groessen || {})[lang] || 0;
  /** Alle Dateien einer Sprache. */
  O.dateien = async (lang) => {
    const idx = await audioIndex(); const set = new Set();
    for (const e of Object.values(idx.saetze || {})) if (e.sprache === lang && !e.unsicher) { set.add('audio/' + e.n); set.add('audio/' + e.s); }
    const pk = await fetch('pakete/index.json').then((r) => r.json()).catch(() => ({ pakete: [] }));
    for (const p of pk.pakete || []) if (p.sprache === lang) set.add('pakete/' + p.id + '.json');
    if (window.PAKETE && window.PAKETE.LERNPFAD.includes(lang)) set.add('lernpfad/' + lang + '.json');
    return [...set];
  };
  const absolut = (u) => new URL(u, location.href).href;
  /** Lädt eine Sprache. onFortschritt(fertig, gesamt). Abbrechen: O.abbrechen = true. */
  O.laden = async (lang, onFortschritt) => {
    if (!O.unterstuetzt()) throw new Error('nicht unterstützt');
    O.abbrechen = false;
    const c = await caches.open(CACHE); const urls = await O.dateien(lang); let fertig = 0;
    onFortschritt && onFortschritt(0, urls.length);
    let i = 0;
    const arbeiter = async () => {
      while (i < urls.length && !O.abbrechen) {
        const u = urls[i++];
        try {
          if (!(await c.match(absolut(u)))) { const r = await fetch(u); if (!r.ok) throw new Error(String(r.status)); await c.put(absolut(u), r); }
        } catch (e) { O.abbrechen = true; O.fehler = e; }
        fertig++; onFortschritt && onFortschritt(fertig, urls.length);
      }
    };
    O.fehler = null;
    await Promise.all([arbeiter(), arbeiter(), arbeiter(), arbeiter()]);
    if (O.abbrechen) throw O.fehler || new Error('abgebrochen');
    const o = lesen(); o[lang] = window.APP_VERSION; schreiben(o);
    return urls.length;
  };
  /** Löscht eine Sprache aus dem Offline-Speicher und gibt den Platz frei. */
  O.loeschen = async (lang) => {
    const c = await caches.open(CACHE); const urls = new Set((await O.dateien(lang)).map(absolut));
    for (const k of await c.keys()) if (urls.has(k.url) || new RegExp('/audio/server/' + lang + '/').test(k.url)) await c.delete(k);
    const o = lesen(); delete o[lang]; schreiben(o);
  };
  /** Nach einem App-Update: geladene Sprachen auf den neuen Stand bringen (fehlende laden, nicht mehr gebrauchte löschen). Nur mit Netz. */
  O.abgleichen = async () => {
    if (!O.unterstuetzt() || navigator.onLine === false) return;
    const o = lesen(); const langs = Object.keys(o).filter((l) => o[l] !== window.APP_VERSION);
    for (const l of langs) {
      try {
        await O.laden(l);
        const c = await caches.open(CACHE); const soll = new Set((await O.dateien(l)).map(absolut));
        for (const k of await c.keys()) if (new RegExp('/audio/server/' + l + '/').test(k.url) && !soll.has(k.url)) await c.delete(k);
      } catch (e) {}
    }
  };
  /** Englisch gehört zur Grundausstattung (immer offline): nach dem ersten Start im Hintergrund laden, einmalig. */
  O.grundausstattung = async () => {
    if (!O.unterstuetzt() || navigator.onLine === false || O.geladen('en')) return;
    try { await O.laden('en'); } catch (e) {}
  };
  window.OFFLINE = O;
})();
