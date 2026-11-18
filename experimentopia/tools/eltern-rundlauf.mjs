// Rundlauf-Test Eltern-Kopplung gegen den Test-Server: versiegelter Bericht aus Kotlin (ParentsRoundtripExport) →
// PUT → GET → mit WebCrypto entschlüsseln (wie die Eltern-Seite) → fremdes X-Schreib (403) → DELETE → GET (404).
// Aufruf: RUNDLAUF=/tmp/r.json ./gradlew :core:model:jvmTest --tests '*ParentsRoundtripExport*' && node tools/eltern-rundlauf.mjs /tmp/r.json
import { readFileSync } from 'node:fs';
const r = JSON.parse(readFileSync(process.argv[2], 'utf8'));
const base = 'https://trainer-test.mathenachhilfe-online.com/eltern/v1/b/' + r.id;
const b64 = (s) => Buffer.from(s.replace(/-/g, '+').replace(/_/g, '/'), 'base64');
let ok = true; const check = (name, cond, extra = '') => { console.log((cond ? '✅ ' : '❌ ') + name + (extra ? ' ' + extra : '')); ok &&= cond; };
const hdr = (w) => ({ 'X-Schreib': w, 'Content-Type': 'text/plain', Origin: 'https://experimentopia-test.mathenachhilfe-online.com' });
let res = await fetch(base, { method: 'PUT', headers: hdr(r.write), body: r.body });
check('PUT Bericht', res.status === 204, `(${res.status} ${res.status === 204 ? '' : await res.text()})`);
res = await fetch(base); const raw = await res.text(); let got = raw.trim(); try { const j = JSON.parse(raw); if (j && typeof j.d === 'string') got = j.d; } catch (e) {}
check('GET liefert denselben verschlüsselten Text', res.status === 200 && got === r.body, `(${res.status})`);
const [iv, ct] = got.split('.');
const key = await crypto.subtle.importKey('raw', b64(r.key), 'AES-GCM', false, ['decrypt']);
const plain = new TextDecoder().decode(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: b64(iv) }, key, b64(ct)));
check('WebCrypto entschlüsselt', plain === r.json);
const rep = JSON.parse(plain);
check('Pflichtfelder', rep.format === 1 && rep.app === 'experimentopia' && rep.appName === 'Experimentopia' && !!rep.stand, JSON.stringify({ aufgaben: rep.aufgaben, richtigProzent: rep.richtigProzent, lernzeitMin: rep.lernzeitMin }));
res = await fetch(base, { method: 'PUT', headers: hdr('A'.repeat(22)), body: r.body });
check('fremdes Schreibrecht abgelehnt', res.status === 403, `(${res.status})`);
res = await fetch(base, { method: 'DELETE', headers: hdr(r.write) });
check('DELETE', res.status === 204, `(${res.status})`);
res = await fetch(base);
check('danach gelöscht', res.status === 404, `(${res.status})`);
console.log('Eltern-Link (Test):', r.parentUrl.replace(/#.*/, '#…'));
process.exit(ok ? 0 : 1);
