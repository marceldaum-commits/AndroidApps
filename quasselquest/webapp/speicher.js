// Zusammenführen von Lernständen (Familien-Speicher, Bauplan „Konflikte – nie still überschreiben“): rein, ohne Netz, mit Tests (tools/test-inhalt.mjs).
// Sicher zusammenführbar: Karten (Leitner), Tagessummen, Lernpfad, Episoden-Sterne, eigene Listen, Wochenzähler.
// Nicht automatisch: Insel-Positionen, Einstellungen, Name, Klasse – dort bleibt der Gerätestand; die Unterschiede werden gemeldet (Elternbereich fragt).
(function (root) {
  const S = {};
  const max = (a, b) => Math.max(a || 0, b || 0);
  const kartenEine = (a, b) => {
    if (!a) return b; if (!b) return a;
    if ((a.b || 0) !== (b.b || 0)) return (a.b || 0) > (b.b || 0) ? a : b;   // höherer Kasten gewinnt
    return (a.d || 0) >= (b.d || 0) ? a : b;                                  // sonst die spätere Fälligkeit
  };
  /** lokal und entfernt: Stand-Objekte (Format v3). Liefert { stand, abweichungen: ['insel', 'einstellungen', …] }. Ändert die Eingaben nicht. */
  S.zusammenfuehren = (lokal, entfernt) => {
    const l = JSON.parse(JSON.stringify(lokal || {})); const r = JSON.parse(JSON.stringify(entfernt || {}));
    const aus = Object.assign({}, l); const abw = [];
    aus.karten = {}; for (const k of new Set([...Object.keys(l.karten || {}), ...Object.keys(r.karten || {})])) aus.karten[k] = kartenEine((l.karten || {})[k], (r.karten || {})[k]);
    aus.tage = {};
    for (const lang of new Set([...Object.keys(l.tage || {}), ...Object.keys(r.tage || {})])) {
      const a = (l.tage || {})[lang] || {}; const b = (r.tage || {})[lang] || {}; aus.tage[lang] = {};
      for (const d of new Set([...Object.keys(a), ...Object.keys(b)])) aus.tage[lang][d] = [max((a[d] || [])[0], (b[d] || [])[0]), max((a[d] || [])[1], (b[d] || [])[1])];   // je Tag das Maximum, nicht doppelt zählen
    }
    aus.lp = {}; for (const k of new Set([...Object.keys(l.lp || {}), ...Object.keys(r.lp || {})])) aus.lp[k] = max((l.lp || {})[k], (r.lp || {})[k]);
    aus.progress = {};
    for (const k of new Set([...Object.keys(l.progress || {}), ...Object.keys(r.progress || {})])) {
      const a = (l.progress || {})[k]; const b = (r.progress || {})[k];
      aus.progress[k] = a && b ? { ...(a.at >= b.at ? a : b), done: !!(a.done || b.done), stars: max(a.stars, b.stars) } : (a || b);
    }
    aus.eigene = {};
    for (const lang of new Set([...Object.keys(l.eigene || {}), ...Object.keys(r.eigene || {})])) {
      const m = new Map(); for (const x of [...((r.eigene || {})[lang] || []), ...((l.eigene || {})[lang] || [])]) m.set(x.id, x);   // Vereinigung nach id, lokal gewinnt bei gleicher id
      aus.eigene[lang] = [...m.values()];
    }
    const la = l.week || { key: '', n: 0 }; const re = r.week || { key: '', n: 0 };
    aus.week = la.key === re.key ? { key: la.key, n: max(la.n, re.n) } : (la.key > re.key ? la : re);
    const gleich = (a, b) => JSON.stringify(a || null) === JSON.stringify(b || null);
    if (!gleich(l.insel, r.insel) && r.insel && Object.keys(r.insel).length) abw.push('insel');
    if (!gleich(l.settings, r.settings) && r.settings) abw.push('einstellungen');
    if (r.name && l.name !== r.name) abw.push('name');
    if (r.klasse && l.klasse !== r.klasse) abw.push('klasse');
    aus.hinweis = Object.assign({}, r.hinweis || {}, l.hinweis || {}); aus.tests = Object.assign({}, r.tests || {}, l.tests || {}); aus.introSeen = Object.assign({}, r.introSeen || {}, l.introSeen || {});
    return { stand: aus, abweichungen: abw };
  };
  root.SPEICHER = S;
  if (typeof module !== 'undefined') module.exports = S;
})(typeof window !== 'undefined' ? window : globalThis);
