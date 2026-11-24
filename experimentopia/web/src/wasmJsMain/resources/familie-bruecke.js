// Brücke zwischen der App und der gemeinsamen Bibliothek familie.js (unverändert, Quelle mathe-trainer).
// Die App ruft window.ExFamilie.call(op, argsJson) und bekommt einen JSON-Text zurück. Nichts hier verlässt das Gerät außer dem,
// was familie.js selbst tut (verschlüsselte Blobs an /familie/v1/ auf unserem Server). Kein anderer Netzabruf.
(function (w) {
  'use strict';
  var mod = null;
  function F() { return mod || (mod = import('./familie.js')); }
  var APP = 'experimentopia';
  // Antwort schlank halten: nur, was die App braucht
  function schlank(r) {
    var o = { ok: !!r.ok, grund: r.grund || '', status: r.status || 0 };
    if (r.rev !== undefined) o.rev = r.rev;
    if (r.stand) o.stand = r.stand;
    if (r.daten !== undefined && r.daten !== null) o.daten = r.daten;
    return o;
  }
  var OPS = {
    neu: async function (f) { var fam = f.neueFamilie(); return { ok: true, fam: fam, nummer: f.familienNummer(fam.famId) }; },
    lesen: async function (f, a) { var fam = f.familieLesen(a.text || ''); return { ok: !!fam, fam: fam || null, nummer: fam ? f.familienNummer(fam.famId) : '' }; },
    kindId: async function (f) { return { ok: true, kindId: f.neueKindId() }; },
    blatt: async function (f, a) { var b = f.notfallBlatt(a.fam, APP, a.seite); return { ok: true, link: b.link, nummer: b.nummer, blatt: b.text }; },
    anlegen: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).anlegen()); },
    indexNeu: async function (f, a) {
      var i = f.appMerken(f.neuerIndex(), APP); var r = f.kindHinzu(i, { name: a.name || '' });
      return { ok: true, index: r.index, kindId: r.kind.kindId };
    },
    indexLaden: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).indexLaden()); },
    indexSichern: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).indexSichern(f.appMerken(a.index, APP), a.rev || 0)); },
    kindHinzu: async function (f, a) { var r = f.kindHinzu(a.index, { name: a.name || '' }); return { ok: true, index: r.index, kindId: r.kind.kindId }; },
    standLaden: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).standLaden(APP, a.kind)); },
    standSichern: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).standSichern(APP, a.kind, a.daten, a.rev || 0)); },
    loeschen: async function (f, a) { return schlank(await f.familie(a.fam, { basis: a.basis }).loeschen()); }
  };
  w.ExFamilie = {
    call: function (op, argsJson) {
      return F().then(function (f) {
        if (!OPS[op]) return JSON.stringify({ ok: false, grund: 'ungueltig' });
        return OPS[op](f, JSON.parse(argsJson || '{}')).then(function (r) { return JSON.stringify(r); });
      }).catch(function (e) { return JSON.stringify({ ok: false, grund: 'fehler', meldung: String(e && e.message || e).slice(0, 120) }); });
    }
  };
})(window);
