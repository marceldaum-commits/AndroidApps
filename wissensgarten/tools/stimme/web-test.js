// Beweis im Headless-Chromium: Thorsten lädt (Modell-Download wird lokal bedient), ONNX Runtime Web rechnet Audio-Samples.
// Aufruf: node tools/stimme/web-test.js <basisUrl> <modell.onnx> <hoerprobe.txt> <ausgabeOrdner>
const { chromium } = require(process.env.PLAYWRIGHT || 'playwright');
const fs = require('fs');
const path = require('path');
const [base, modelPath, probePath, outDir] = process.argv.slice(2);
const MODEL_URL = 'https://huggingface.co/csukuangfj/vits-piper-de_DE-thorsten-medium/resolve/main/de_DE-thorsten-medium.onnx';

function wav(samples, rate) {
  const b = Buffer.alloc(44 + samples.length * 2);
  b.write('RIFF', 0); b.writeUInt32LE(36 + samples.length * 2, 4); b.write('WAVE', 8); b.write('fmt ', 12);
  b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22); b.writeUInt32LE(rate, 24); b.writeUInt32LE(rate * 2, 28);
  b.writeUInt16LE(2, 32); b.writeUInt16LE(16, 34); b.write('data', 36); b.writeUInt32LE(samples.length * 2, 40);
  samples.forEach((s, i) => b.writeInt16LE(Math.max(-32768, Math.min(32767, Math.round(s * 32767))), 44 + i * 2));
  return b;
}

(async () => {
  const browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] });
  const page = await browser.newPage();
  const requests = [];
  page.on('request', (r) => requests.push(r.url()));
  page.on('console', (m) => { if (m.type() === 'error' || m.type() === 'warning') console.log('[browser]', m.text().slice(0, 200)); });
  // Hugging Face lokal bedienen (gleiche Datei, gleiche Prüfsumme) – mit CORS-Kopf wie der echte Server
  await page.route(MODEL_URL, (route) => route.fulfill({ status: 200, body: fs.readFileSync(modelPath),
    headers: { 'Access-Control-Allow-Origin': '*', 'Content-Type': 'application/octet-stream' } }));
  await page.goto(base + '/index.html');
  await page.waitForFunction(() => window.wg && window.wg.th && window.wg.th.state !== 'checking');
  const startOrt = requests.filter((u) => u.includes('/ort/')).length;
  console.log('Start: Zustand', await page.evaluate(() => wg.th.state), '| ort-Dateien beim Start geladen:', startOrt);

  // 1) Laden wie über den Knopf „Thorsten laden“
  await page.evaluate(() => wg.th.download());
  await page.waitForFunction(() => wg.th.state !== 'loading', null, { timeout: 120000 });
  console.log('Nach Download:', await page.evaluate(() => ({ state: wg.th.state, why: wg.th.why })));

  // 2) Laut-Nummern aus dem Kotlin-Test (Wörterbuch + tokens.txt) rechnen lassen
  const lines = fs.readFileSync(probePath, 'utf8').trim().split('\n');
  fs.mkdirSync(outDir, { recursive: true });
  let n = 0;
  for (const line of lines.slice(0, 3)) {
    const [text, spec] = line.split('\t');
    const t0 = Date.now();
    const res = await page.evaluate(async ({ spec }) => {
      const all = [];
      for (const part of spec.split(';')) { const pcm = await wg.th.synth(part.split(',').map(Number), 1.0); all.push(...pcm); }
      let sq = 0, peak = 0; for (const x of all) { sq += x * x; peak = Math.max(peak, Math.abs(x)); }
      return { samples: all, rms: Math.sqrt(sq / all.length), peak };
    }, { spec });
    const ms = Date.now() - t0;
    const file = path.join(outDir, `probe${++n}.wav`);
    fs.writeFileSync(file, wav(res.samples, 22050));
    console.log(`„${text}“ → ${res.samples.length} Samples (${(res.samples.length / 22050).toFixed(2)} s Sprache) in ${ms} ms, RMS ${res.rms.toFixed(3)}, Spitze ${res.peak.toFixed(3)} → ${file}`);
  }
  // 3) Abspielen über Web Audio (wie in der App): speak → speaking → fertig
  await page.evaluate(({ spec }) => wg.th.speak(spec, 1 / 1.2), { spec: lines[0].split('\t')[1] });
  await page.waitForFunction(() => wg.th.speaking, null, { timeout: 5000 });
  await page.waitForFunction(() => !wg.th.speaking, null, { timeout: 60000 });
  console.log('Abspielen per Web Audio: fertig, letzte Samples', await page.evaluate(() => wg.th.lastSamples), '| Problem:', JSON.stringify(await page.evaluate(() => wg.th.problem)));
  console.log('ort-Dateien geladen (erst bei Bedarf):', requests.filter((u) => u.includes('/ort/')).map((u) => u.split('/').pop()).join(', '));
  // 4) Bleibt die Stimme nach Neuladen erhalten?
  await page.reload(); await page.waitForFunction(() => window.wg && wg.th && wg.th.state !== 'checking');
  console.log('Nach Neuladen:', await page.evaluate(() => wg.th.state));
  await browser.close();
})().catch((e) => { console.error('FEHLER', e); process.exit(1); });
