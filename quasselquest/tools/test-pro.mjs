// Quasselquest Pro: Ed25519-Prüfung (RFC 8032 + Gegenprobe mit Node-Krypto), Code-Format, Rechte. Aufruf: node tools/test-pro.mjs
import { createRequire } from 'node:module';
import { generateKeyPairSync, sign, createPublicKey } from 'node:crypto';
const require = createRequire(import.meta.url);
globalThis.window = undefined; const m = new Map(); globalThis.localStorage = { getItem: (k) => (m.has(k) ? m.get(k) : null), setItem: (k, v) => m.set(k, String(v)), removeItem: (k) => m.delete(k) };
const PRO = require('../webapp/pakete.js') && require('../webapp/pro.js');
let n = 0, f = 0; const ok = (b, t) => { n++; if (!b) { f++; console.error('FEHLER:', t); } else console.log('  ✅ ' + t); };
const hex = (s) => Uint8Array.from(s.match(/../g) || [], (x) => parseInt(x, 16));
// RFC 8032, Abschnitt 7.1
ok(await PRO.verifiziere(hex('e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b'), new Uint8Array(0), hex('d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a')), 'RFC 8032 Test 1 (leere Nachricht)');
ok(await PRO.verifiziere(hex('92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00'), hex('72'), hex('3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c')), 'RFC 8032 Test 2');
ok(!(await PRO.verifiziere(hex('92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00'), hex('73'), hex('3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c'))), 'veränderte Nachricht wird abgelehnt');
// Gegenprobe: Node signiert, unsere Prüfung verifiziert (zufällige Schlüssel)
let alle = true;
for (let i = 0; i < 8; i++) {
  const { privateKey, publicKey } = generateKeyPairSync('ed25519'); const pk = new Uint8Array(publicKey.export({ format: 'der', type: 'spki' }).subarray(12));
  const msg = crypto.getRandomValues(new Uint8Array(9)); const sig = new Uint8Array(sign(null, msg, privateKey));
  const gut = await PRO.verifiziere(sig, msg, pk); const bad = sig.slice(); bad[10] ^= 1;
  if (!gut || (await PRO.verifiziere(bad, msg, pk))) alle = false;
}
ok(alle, 'Node-Krypto signiert, unsere Prüfung stimmt zu und lehnt Verfälschtes ab (8 Schlüssel)');
// Code: Test-Schlüssel, Codes für Sprachen, Plus, falsche App
const { privateKey, publicKey } = generateKeyPairSync('ed25519'); const pk = new Uint8Array(publicKey.export({ format: 'der', type: 'spki' }).subarray(12));
const CROCK = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const b32 = (u) => { let s = '', puf = 0, bits = 0; for (const b of u) { puf = (puf << 8) | b; bits += 8; while (bits >= 5) { s += CROCK[(puf >> (bits - 5)) & 31]; bits -= 5; } puf &= (1 << bits) - 1; } if (bits) s += CROCK[(puf << (5 - bits)) & 31]; return s; };
const code = (app, sp, serie) => { const pay = Uint8Array.from([1, app, sp, (serie >>> 24) & 255, (serie >> 16) & 255, (serie >> 8) & 255, serie & 255, 0, 5]); const sig = new Uint8Array(sign(null, pay, privateKey)); const t = b32(Uint8Array.from([...pay, ...sig])); return 'QQ-' + t.match(/.{1,5}/g).join('-'); };
let r = await PRO.pruefe(code(7, 3, 42), pk); ok(r.ok && r.sprachen.join() === 'fr' && r.nummer === 'L7.3.42' && r.art === 'pro', 'QQ-Code Französisch: ' + r.nummer);
r = await PRO.pruefe(code(7, 9, 70000).toLowerCase().replace(/-/g, ' '), pk); ok(r.ok && r.sprachen.length === 5, 'Code „alle Sprachen“, Groß/Klein und Leerzeichen egal');
r = await PRO.pruefe(code(3, 1, 7), pk); ok(r.ok && r.art === 'plus' && r.sprachen.join() === 'en,de', 'Kaufschutz: Wissensgarten-Plus → Englisch + Deutsch');
r = await PRO.pruefe(code(5, 1, 7), pk); ok(!r.ok && r.grund === 'fremd', 'Code einer anderen App wird abgelehnt');
const ganz = code(7, 1, 1); const kaputt = ganz.slice(0, 52) + (ganz[52] === "A" ? "B" : "A") + ganz.slice(53);; /* mitten im Code ein Zeichen ändern (das letzte trägt nur Füll-Bits) */ r = await PRO.pruefe(kaputt, pk); ok(!r.ok && r.grund === 'signatur', 'verfälschter Code → Signatur ungültig');
r = await PRO.pruefe('QQ-ABC', pk); ok(!r.ok && r.grund === 'form', 'zu kurzer Code → Form');
r = await PRO.pruefe(code(7, 1, 1)); ok(!r.ok && r.grund === 'signatur', 'mit dem echten öffentlichen Schlüssel ist ein Test-Code ungültig');
// Rechte
globalThis.PRO_AKTIV = false; ok(PRO.darf('fr') === true, 'Flag aus: nichts gesperrt');
globalThis.PRO_AKTIV = true; ok(PRO.darf('fr') === false, 'Flag an, kein Code: Französisch gesperrt');
PRO.pruefe0 = PRO.pruefe; PRO.pruefe = (c) => PRO.pruefe0(c, pk);
r = await PRO.einloesen(code(7, 3, 42)); ok(r.ok && PRO.darf('fr') && !PRO.darf('es'), 'eingelöst: nur Französisch frei');
r = await PRO.einloesen(code(7, 5, 43), async (nr) => nr === 'L7.5.43'); ok(!r.ok && r.grund === 'gesperrt' && !PRO.darf('es'), 'gesperrter Code (Sperrliste) wird nicht freigeschaltet');
console.log(f ? `❌ ${f} von ${n} fehlgeschlagen` : `✅ Pro: ${n} Prüfungen bestanden`); process.exit(f ? 1 : 0);
