'use strict';
// Prüfung der KI-Route ohne Netz:  node --test raumlicht/server
const test = require('node:test');
const assert = require('node:assert/strict');
const { raumlichtKi, speicherImRam, herkunftErlaubt, lizenz, nachrichten, SYSTEM } = require('./raumlicht-ki.js');

// Test-Schlüssel (Seed 0…31) und damit erzeugte Codes – der echte Schlüssel nimmt sie nicht
const PK = '03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8';
const KI_2027 = 'RL-04208-0000Z-8GNM5-HJFN6-SDW5Y-QPW2T-BSSPN-9DS5K-7Z3A1-PTFJ2-ACM3Q-GX1H4-ZM8EV-3GR33-K3N31-3VN22-58C7D-HP8Y1-1321W-HG25S-PNJ23-FSKM6-JYD9R-GJ';
const KI_ALT = 'RL-04208-0000Z-90JRS-KVK76-JC5YR-K5AB8-66P5J-B9FQ0-J8M2Q-N9CCS-9YCEM-0XX2C-KCRBZ-8QGFP-J83ZY-RYKBJ-8WK56-H6461-JQTF0-1PN3P-K7Q9A-337T4-N8R7X-GE';
const PRO = 'RL-04202-0000F-MGKKT-CDRGB-BYPG5-8XEVQ-M0F02-94P76-M1MXZ-28QYX-86PN1-NEY0T-JNXA5-8S0GJ-HA3X2-C0GQW-AW2K1-BQN3M-4PV5F-ZHQBG-ZKMPE-KJSDT-6VARJ-0W';
const GERAET = 'a'.repeat(32);

const frage = (mehr = {}) => ({ app: 'raumlicht', version: '2.19.0', art: 'erklaerung', aufgabe: { id: '3-2', kapitel: 3, titel: 'Ebene', auftrag: 'Stell die Ebene so ein, dass sie durch P geht.', tipp: 'Setz P ein.', formel: 'E: n · x = c', rechenweg: ['n · p ausrechnen', 'c = 6'] },
  eingabe: 'n = (0|0|1), c = 3', richtig: false, meldung: 'Knapp daneben.', frage: '', verlauf: [], geraet: GERAET, code: '', ...mehr });

/** KI-Dienst nachgestellt: merkt sich die Anfragen und antwortet wie die Messages-API */
function dienst(antwort = { content: [{ type: 'thinking', thinking: '' }, { type: 'text', text: ' Stell dir die Ebene als Tischplatte vor. ' }], stop_reason: 'end_turn' }, status = 200) {
  const gesehen = [];
  const fetch = async (url, init) => { gesehen.push({ url, init, body: JSON.parse(init.body) }); return { ok: status === 200, status, json: async () => antwort }; };
  return { fetch, gesehen };
}
const bau = (d, mehr = {}) => raumlichtKi({ apiKey: 'test', publicKeyHex: PK, speicher: speicherImRam(), fetch: d.fetch, jetzt: () => new Date('2026-11-18T12:00:00Z'), ...mehr });

test('Lizenz: nur KI-Codes dieser App, mit dem richtigen Schlüssel', () => {
  assert.deepEqual(lizenz(KI_2027, PK), { nummer: 2001, bisTag: 21030 });       // 31.07.2027
  assert.deepEqual(lizenz(KI_2027.toLowerCase().replace(/-/g, ' '), PK), { nummer: 2001, bisTag: 21030 });
  assert.equal(lizenz(PRO, PK), null, 'Pro-Code ist kein KI-Code');
  assert.equal(lizenz(KI_2027.replace('8GNM5', '8GNM6'), PK), null);
  assert.equal(lizenz(KI_2027, '1' + PK.slice(1)), null);
  assert.equal(lizenz('RL-123', PK), null); assert.equal(lizenz(null, PK), null); assert.equal(lizenz(KI_2027, ''), null);
});

test('Ohne Code: drei Proben je Gerät, dann 402', async () => {
  const d = dienst(); const ki = bau(d);
  for (const rest of [2, 1, 0]) assert.deepEqual(await ki(frage()), { status: 200, json: { text: 'Stell dir die Ebene als Tischplatte vor.', rest } });
  assert.deepEqual(await ki(frage()), { status: 402, json: { fehler: 'proben', rest: 0 } });
  assert.equal(d.gesehen.length, 3, 'die vierte Frage erreicht den KI-Dienst nicht');
  assert.equal((await ki(frage({ geraet: 'b'.repeat(32) }))).status, 200, 'anderes Gerät hat eigene Proben');
  assert.equal((await ki(frage({ geraet: 'kaputt' }))).status, 400);
});

test('Ohne Code: je Adresse höchstens zwölf Proben am Tag', async () => {
  const ki = bau(dienst());
  for (let i = 0; i < 12; i++) assert.equal((await ki(frage({ geraet: String(i).padStart(2, '0').repeat(16) }), { adresse: '203.0.113.7' })).status, 200);
  assert.equal((await ki(frage({ geraet: 'c'.repeat(32) }), { adresse: '203.0.113.7' })).status, 402);
  assert.equal((await ki(frage({ geraet: 'c'.repeat(32) }), { adresse: '203.0.113.8' })).json.rest, 2, 'die abgewiesene Frage hat keine Probe gekostet');
});

test('Mit Pass: zehn KI-Punkte am Tag, am nächsten Tag neu', async () => {
  let jetzt = new Date('2026-11-18T12:00:00Z');
  const ki = bau(dienst(), { jetzt: () => jetzt });
  for (let i = 9; i >= 0; i--) assert.equal((await ki(frage({ code: KI_2027, art: 'rueckfrage', frage: 'Warum c = 6?' }))).json.rest, i);
  assert.deepEqual(await ki(frage({ code: KI_2027 })), { status: 429, json: { fehler: 'tageslimit', rest: 0 } });
  jetzt = new Date('2026-11-18T23:30:00Z');                                      // in Deutschland schon der 19.
  assert.equal((await ki(frage({ code: KI_2027 }))).json.rest, 9);
});

test('Pass: gilt bis einschließlich 31. Juli, danach 402; falscher Code 402', async () => {
  let jetzt = new Date('2027-07-31T21:00:00Z');                                  // 23 Uhr deutscher Zeit
  const ki = bau(dienst(), { jetzt: () => jetzt });
  assert.equal((await ki(frage({ code: KI_2027 }))).status, 200);
  jetzt = new Date('2027-07-31T22:30:00Z');                                      // 1. August, 0:30 Uhr
  assert.deepEqual(await ki(frage({ code: KI_2027 })), { status: 402, json: { fehler: 'abgelaufen', rest: 0 } });
  assert.deepEqual(await bau(dienst())(frage({ code: KI_ALT })), { status: 402, json: { fehler: 'abgelaufen', rest: 0 } });
  assert.deepEqual(await bau(dienst())(frage({ code: PRO })), { status: 402, json: { fehler: 'code', rest: 0 } });
});

test('Fällt der KI-Dienst aus oder lehnt er ab, kostet das nichts', async () => {
  const speicher = speicherImRam();
  assert.deepEqual(await bau(dienst({}, 529), { speicher })(frage()), { status: 502, json: { fehler: 'ki' } });
  assert.deepEqual(await bau(dienst({ content: [], stop_reason: 'end_turn' }), { speicher })(frage()), { status: 502, json: { fehler: 'ki' } });
  const abgelehnt = await bau(dienst({ content: [], stop_reason: 'refusal' }), { speicher })(frage());
  assert.equal(abgelehnt.status, 200); assert.equal(abgelehnt.json.rest, 3); assert.match(abgelehnt.json.text, /Aufgabe/);
  assert.equal((await bau(dienst(), { speicher })(frage())).json.rest, 2, 'erst die erste echte Antwort kostet eine Probe');
});

test('Unbrauchbare Anfragen: 400, ohne den KI-Dienst zu fragen', async () => {
  const d = dienst(); const ki = bau(d);
  for (const b of [null, 'text', {}, frage({ app: 'anders' }), frage({ art: 'plaudern' }), frage({ aufgabe: null }), frage({ aufgabe: { auftrag: '' } }), frage({ art: 'rueckfrage', frage: ' ' })]) assert.equal((await ki(b)).status, 400);
  assert.equal(d.gesehen.length, 0);
});

test('Was an den KI-Dienst geht', async () => {
  const d = dienst(); const ki = bau(d);
  await ki(frage({ art: 'rueckfrage', frage: 'Warum   nicht c = 3?', verlauf: [{ rolle: 'lux', text: 'Erste Erklärung' }, { rolle: 'schueler', text: 'Hm.' }] }));
  const { url, init, body } = d.gesehen[0];
  assert.equal(url, 'https://api.anthropic.com/v1/messages');
  assert.equal(init.headers['x-api-key'], 'test'); assert.equal(init.headers['anthropic-version'], '2026-10-21');
  assert.equal(body.model, 'claude-opus-5-5'); assert.equal(body.system, SYSTEM); assert.deepEqual(body.output_config, { effort: 'low' });
  assert.deepEqual(body.messages.map((m) => m.role), ['user', 'assistant', 'user']);
  assert.match(body.messages[0].content, /Auftrag: Stell die Ebene/); assert.match(body.messages[0].content, /<eingabe>\nn = \(0\|0\|1\), c = 3/); assert.match(body.messages[0].content, /2\. c = 6/);
  assert.equal(body.messages[2].content, 'Hm.\n\n<frage>\nWarum nicht c = 3?\n</frage>');
  assert.ok(!JSON.stringify(body).includes(GERAET), 'die Geräte-Kennung geht nicht an den KI-Dienst');
  // überlange Eingaben werden gekürzt
  const lang = nachrichten(frage({ frage: 'x'.repeat(5000), art: 'rueckfrage', verlauf: Array.from({ length: 20 }, (_, i) => ({ rolle: i % 2 ? 'lux' : 'schueler', text: 'y'.repeat(5000) })) }));
  assert.ok(JSON.stringify(lang).length < 12000); assert.equal(lang[0].role, 'user'); assert.equal(lang[lang.length - 1].role, 'user');
});

test('Herkunft: nur die WebApp-Adressen (die Android-App schickt die Live-Adresse)', () => {
  assert.ok(herkunftErlaubt('https://raumlicht.mathenachhilfe-online.com')); assert.ok(herkunftErlaubt('https://raumlicht-test.mathenachhilfe-online.com'));
  assert.ok(!herkunftErlaubt('https://example.com')); assert.ok(!herkunftErlaubt(undefined));
});
