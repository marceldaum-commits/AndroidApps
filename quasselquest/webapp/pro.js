// Quasselquest Pro – Code-Prüfung offline (PRO.md): Ed25519 mit BigInt (läuft auch, wo WebCrypto kein Ed25519 kann), Crockford-Base32, Rechte.
// Vorbereitet und AUS: Solange window.PRO_AKTIV nicht true ist (version.js), ist nichts gesperrt und es gibt keine Eingabe.
// Code: Präfix „QQ-“ + Base32 von 73 Byte = Inhalt 9 B [1, App, Sprach-Byte, Serie(4), Tag(2)] + 64 B Ed25519-Signatur über die 9 B.
// Sprach-Byte: 1 Englisch, 2 Deutsch, 3 Französisch, 4 Latein, 5 Spanisch, 9 alle. Kaufschutz: Wissensgarten-Plus-Code (App 3, Produkt 1 oder 3) → Englisch + Deutsch.
(function (root) {
  const P = {};
  P.PUBLIC_KEY = '979eb515eb05b04dcc42e5fb776f108cdbdc86bb3c7cf28e05721fdde9e6b812';
  P.SPRACHEN = { 1: ['en'], 2: ['de'], 3: ['fr'], 4: ['la'], 5: ['es'], 9: ['en', 'de', 'fr', 'la', 'es'] };
  /* ───── Ed25519 (RFC 8032), Prüfung ───── */
  const p = 2n ** 255n - 19n; const L = 2n ** 252n + 27742317777372353535851937790883648493n;
  const mod = (a, m = p) => { const r = a % m; return r >= 0n ? r : r + m; };
  const pot = (b, e, m = p) => { let r = 1n; b = mod(b, m); while (e > 0n) { if (e & 1n) r = (r * b) % m; b = (b * b) % m; e >>= 1n; } return r; };
  const inv = (a) => pot(a, p - 2n);
  const d = mod(-121665n * inv(121666n));
  const I = pot(2n, (p - 1n) / 4n);
  const addiere = (A, B) => {   // erweiterte Koordinaten (X, Y, Z, T)
    const a = mod((A[1] - A[0]) * (B[1] - B[0])), b = mod((A[1] + A[0]) * (B[1] + B[0])), c = mod(2n * d * A[3] * B[3]), dd = mod(2n * A[2] * B[2]);
    const e = b - a, f = dd - c, g = dd + c, h = b + a;
    return [mod(e * f), mod(g * h), mod(f * g), mod(e * h)];
  };
  const mal = (Pt, n) => { let R = [0n, 1n, 1n, 0n], Q = Pt; while (n > 0n) { if (n & 1n) R = addiere(R, Q); Q = addiere(Q, Q); n >>= 1n; } return R; };
  const gleich = (A, B) => mod(A[0] * B[2]) === mod(B[0] * A[2]) && mod(A[1] * B[2]) === mod(B[1] * A[2]);
  const leZahl = (u) => { let n = 0n; for (let i = u.length - 1; i >= 0; i--) n = (n << 8n) + BigInt(u[i]); return n; };
  const punktLesen = (u) => {
    let y = leZahl(u); const x0 = (y >> 255n) & 1n; y &= (1n << 255n) - 1n; if (y >= p) return null;
    const xx = mod((y * y - 1n) * inv(d * y * y + 1n)); let x = pot(xx, (p + 3n) / 8n);
    if (mod(x * x - xx) !== 0n) x = mod(x * I); if (mod(x * x - xx) !== 0n) return null;
    if (x === 0n && x0 === 1n) return null; if ((x & 1n) !== x0) x = p - x;
    return [x, y, 1n, mod(x * y)];
  };
  const By = mod(4n * inv(5n)); const Bx0 = (() => { const xx = mod((By * By - 1n) * inv(d * By * By + 1n)); let x = pot(xx, (p + 3n) / 8n); if (mod(x * x - xx) !== 0n) x = mod(x * I); return (x & 1n) ? p - x : x; })();
  const BASIS = [Bx0, By, 1n, mod(Bx0 * By)];
  const hex = (s) => Uint8Array.from(s.match(/../g) || [], (x) => parseInt(x, 16));
  const sha512 = async (u) => new Uint8Array(await crypto.subtle.digest('SHA-512', u));
  /** Signatur prüfen: sig 64 B, nachricht, pk 32 B (alle Uint8Array). */
  P.verifiziere = async (sig, nachricht, pk) => {
    if (sig.length !== 64 || pk.length !== 32) return false;
    const A = punktLesen(pk), R = punktLesen(sig.subarray(0, 32)); const S = leZahl(sig.subarray(32)); if (!A || !R || S >= L) return false;
    const eing = new Uint8Array(64 + nachricht.length); eing.set(sig.subarray(0, 32)); eing.set(pk, 32); eing.set(nachricht, 64);
    const k = mod(leZahl(await sha512(eing)), L);
    return gleich(mal(BASIS, S), addiere(R, mal(A, k)));
  };
  /* ───── Code lesen ───── */
  const CROCK = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  P.bytes = (code) => {
    const t = String(code || '').trim().toUpperCase().replace(/^(QQ|WG)(?=[-\s0-9A-Z])/, '').replace(/[^0-9A-Z]/g, '').replace(/O/g, '0').replace(/[IL]/g, '1');
    const out = []; let puffer = 0, bits = 0;
    for (const z of t) { const v = CROCK.indexOf(z); if (v < 0) return null; puffer = (puffer << 5) | v; bits += 5; if (bits >= 8) { out.push((puffer >> (bits - 8)) & 255); bits -= 8; puffer &= (1 << bits) - 1; } }
    return out.length === 73 ? Uint8Array.from(out) : null;
  };
  /** Code prüfen → { ok, sprachen:[…], serie, app, art:'pro'|'plus', code } | { ok:false, grund: 'form' | 'signatur' | 'fremd' }. pk: für Tests austauschbar. */
  P.pruefe = async (code, pk) => {
    const b = P.bytes(code); if (!b || b[0] !== 1) return { ok: false, grund: 'form' };
    if (!(await P.verifiziere(b.subarray(9), b.subarray(0, 9), pk || hex(P.PUBLIC_KEY)))) return { ok: false, grund: 'signatur' };
    const serie = ((b[3] << 24) >>> 0) + (b[4] << 16) + (b[5] << 8) + b[6];
    if (b[1] === 7 && P.SPRACHEN[b[2]]) return { ok: true, art: 'pro', app: 7, sprachen: P.SPRACHEN[b[2]], serie, nummer: `L7.${b[2]}.${serie}` };
    if (b[1] === 3 && (b[2] === 1 || b[2] === 3)) return { ok: true, art: 'plus', app: 3, sprachen: ['en', 'de'], serie, nummer: `L3.${b[2]}.${serie}` };   // Kaufschutz Wissensgarten Plus
    return { ok: false, grund: 'fremd' };
  };
  /* ───── Rechte (nur auf dem Gerät, nicht im Familien-Stand) ───── */
  const KEY = 'quasselquest-pro';
  const lesen = () => { try { return JSON.parse(localStorage.getItem(KEY) || '[]'); } catch (e) { return []; } };
  P.aktiv = () => root.PRO_AKTIV === true;
  P.rechte = () => lesen();
  P.sprachenFrei = () => new Set(lesen().flatMap((r) => r.sprachen));
  /** Hat dieses Gerät Pro für die Sprache? Ohne Flag: immer ja (nichts ist gesperrt). */
  P.darf = (lang) => !P.aktiv() || P.sprachenFrei().has(lang);
  /** Code einlösen: prüft Signatur und Sperrliste (sperrlisteTest(nummer) → true = gesperrt), speichert nur Sprachen/Serie/Nummer. */
  P.einloesen = async (code, sperrlisteTest) => {
    const r = await P.pruefe(code); if (!r.ok) return r;
    if (sperrlisteTest && await sperrlisteTest(r.nummer)) return { ok: false, grund: 'gesperrt' };
    const liste = lesen().filter((x) => x.nummer !== r.nummer); liste.push({ nummer: r.nummer, art: r.art, sprachen: r.sprachen, t: Date.now() });
    try { localStorage.setItem(KEY, JSON.stringify(liste)); } catch (e) { return { ok: false, grund: 'speicher' }; }
    return r;
  };
  P.entfernen = () => { try { localStorage.removeItem(KEY); } catch (e) {} };
  root.PRO = P;
  if (typeof module !== 'undefined') module.exports = P;
})(typeof window !== 'undefined' ? window : globalThis);
