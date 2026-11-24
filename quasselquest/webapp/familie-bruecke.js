// Brücke zwischen der App (klassische Skripte) und familie.js (Modul): Familien-Speicher für Quasselquest.
// Alles hinter dem Eltern-Tor; ohne Kopplung bleibt alles wie bisher (ein Profil im Browser, nichts geht zum Server).
// Recht A6/V8: Einwilligung (Pflichttext) vor dem ersten Hochladen, nichts vorausgewählt, Abbrechen = nichts hochladen.
import * as F from './js/familie.js';

const APP = 'quasselquest';
const KEY = 'quasselquest-familie';
const basis = () => location.origin;
const lesen = () => { try { return JSON.parse(localStorage.getItem(KEY) || 'null'); } catch (e) { return null; } };
const schreiben = (o) => { try { if (o) localStorage.setItem(KEY, JSON.stringify(o)); else localStorage.removeItem(KEY); } catch (e) {} };
const zugang = (z, f) => F.familie(z.fam, { basis: basis(), f: f || undefined });

const B = {};
B.APP = APP;
B.gekoppelt = () => !!(lesen() && lesen().fam && lesen().kindId);
B.status = () => { const z = lesen(); return z && z.fam ? { gekoppelt: !!z.kindId, nummer: F.familienNummer(z.fam.famId), letzte: z.letzte || null, kindName: z.kindName || '' } : { gekoppelt: false }; };
B.link = () => { const z = lesen(); return z && z.fam ? F.familieLink(z.fam, APP, basis()) : null; };
B.notfallBlatt = () => { const z = lesen(); return z && z.fam ? F.notfallBlatt(z.fam, APP, basis()) : null; };
B.linkLesen = (text) => F.familieLesen(text);

/** Familie anlegen (nach „Einverstanden“) und den Stand hochladen. */
B.anlegen = async (state, f) => {
  const fam = F.neueFamilie(); const fa = F.familie(fam, { basis: basis(), f });
  const r = await fa.anlegen(); if (!r.ok) return r;
  const { index, kind } = F.kindHinzu(F.neuerIndex(), { name: state.name || '', klasse: String(state.klasse || '') });
  const ri = await fa.indexSichern(F.appMerken(index, APP), 0); if (!ri.ok) return ri;
  schreiben({ fam, kindId: kind.kindId, kindName: kind.name, rev: 0, einverstanden: new Date().toISOString() });
  return B.sichern(state, f);
};
/** Beitreten per Link: liefert die Kinder der Familie (zur Auswahl). */
B.beitretenVorbereiten = async (linkText, f) => {
  const fam = F.familieLesen(linkText); if (!fam) return { ok: false, grund: 'ungueltig' };
  const fa = F.familie(fam, { basis: basis(), f }); const r = await fa.indexLaden(); if (!r.ok) return r;
  return { ok: true, fam, kinder: (r.daten && r.daten.kinder) || [], index: r.daten, rev: r.rev };
};
/** Kind wählen (kindId) oder neues Kind (kindId null) – danach wird der Stand geladen und zusammengeführt. */
B.beitreten = async (vorb, kindId, state, f) => {
  const fa = F.familie(vorb.fam, { basis: basis(), f }); let kind = (vorb.kinder || []).find((k) => k.kindId === kindId);
  if (!kind) {
    const n = F.kindHinzu(vorb.index || F.neuerIndex(), { name: state.name || '', klasse: String(state.klasse || '') }); kind = n.kind;
    const ri = await fa.indexSichern(F.appMerken(n.index, APP), vorb.rev); if (!ri.ok) return ri;
  } else if (!(vorb.index.apps || []).includes(APP)) { await fa.indexSichern(F.appMerken(vorb.index, APP), vorb.rev); }
  schreiben({ fam: vorb.fam, kindId: kind.kindId, kindName: kind.name, rev: 0, einverstanden: new Date().toISOString() });
  return B.laden(state, f);
};
/** Stand vom Server holen und mit dem Gerätestand zusammenführen → { ok, stand, abweichungen } (die App übernimmt `stand`). */
B.laden = async (state, f) => {
  const z = lesen(); if (!z) return { ok: false, grund: 'nicht-gekoppelt' };
  const r = await zugang(z, f).standLaden(APP, z.kindId);
  if (!r.ok) { if (r.grund === 'keine') return B.sichern(state, f); return r; }
  const m = window.SPEICHER.zusammenfuehren(state, r.daten); z.rev = r.rev; schreiben(z);
  return { ok: true, stand: m.stand, abweichungen: m.abweichungen, geladen: true };
};
/** Stand sichern; bei Konflikt (anderes Gerät war schneller) zusammenführen und noch einmal sichern. */
B.sichern = async (state, f) => {
  const z = lesen(); if (!z) return { ok: false, grund: 'nicht-gekoppelt' };
  const fa = zugang(z, f); let r = await fa.standSichern(APP, z.kindId, state, z.rev || 0); let stand = state; let abw = [];
  if (!r.ok && r.grund === 'konflikt' && r.daten) {
    const m = window.SPEICHER.zusammenfuehren(state, r.daten); stand = m.stand; abw = m.abweichungen;
    r = await fa.standSichern(APP, z.kindId, stand, r.rev);
  }
  if (r.ok) { z.rev = r.rev; z.letzte = new Date().toISOString(); schreiben(z); return { ok: true, stand, abweichungen: abw }; }
  if (r.grund === 'familie-weg') { schreiben(null); }
  return r;
};
/** Familie löschen (Familien-Nummer muss stimmen). Lokal bleibt der Lernstand; nur die Schlüssel verschwinden. */
B.loeschen = async (nummerEingabe, f) => {
  const z = lesen(); if (!z) return { ok: false, grund: 'nicht-gekoppelt' };
  if (F.nummerNormal(nummerEingabe) !== F.nummerNormal(F.familienNummer(z.fam.famId))) return { ok: false, grund: 'nummer' };
  const r = await zugang(z, f).loeschen(); if (r.ok || r.grund === 'familie-weg' || r.status === 404) { schreiben(null); return { ok: true }; }
  return r;
};
B.vergessen = () => schreiben(null);
// Sperrliste erstatteter/gesperrter Codes (nur mit Pro-Flag): laden, wenn online, und offline vor jeder Prüfung nutzen
const SPERR = 'quasselquest.sperrliste';
B.sperrlisteLaden = (f) => F.sperrlisteLaden({ basis: basis(), f, schluessel: SPERR });
B.codeGesperrt = (nr) => F.codeGesperrt(nr, { schluessel: SPERR });

window.FAMILIE = B;
window.dispatchEvent(new Event('familie-bereit'));
