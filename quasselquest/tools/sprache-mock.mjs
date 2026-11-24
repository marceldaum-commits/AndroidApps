// Mock-Server für /sprache/v1/auswerten (SPRACHE-API.md) – für Tests ohne echten Server.
// Modus per Aufruf von mock.modus('ok' | '413' | '415' | '429' | '503' | '500' | 'langsam' | 'schlecht'); Mitschnitt in mock.log.
export function neuerMock() {
  const mock = { modus: 'ok', log: [], antwort: { text: '', treffer: null } };
  mock.modus = (m) => { mock.modus_ = m; };
  mock.modus_ = 'ok';
  mock.handle = (req, res) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => {
      const body = Buffer.concat(chunks); const kopf = body.toString('latin1');
      const felder = [...kopf.matchAll(/name="([^"]+)"(?:; filename="([^"]+)")?/g)].map((m) => m[1]);
      const text = (n) => { const m = new RegExp('name="' + n + '"\\r\\n\\r\\n([^\\r]*)').exec(kopf); return m ? m[1] : null; };
      mock.log.push({ methode: req.method, url: req.url, felder, sprache: text('sprache'), ziel: text('ziel'), groesse: body.length,
        header: Object.fromEntries(Object.entries(req.headers).filter(([k]) => ['cookie', 'authorization', 'referer', 'origin', 'content-type', 'user-agent'].includes(k))) });
      const m = mock.modus_;
      const fehler = (code) => { res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ fehler: String(code) })); };
      if (['413', '415', '429', '503', '500'].includes(m)) return fehler(Number(m));
      const antworten = () => {
        const ziel = text('ziel') || '';
        const erkannt = m === 'schlecht' ? 'something else entirely' : ziel;
        res.writeHead(200, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ format: 1, text: erkannt.toLowerCase().replace(/[.!?,]/g, ''), dauer_ms: 120 }));
      };
      if (m === 'langsam') return setTimeout(antworten, 9500);
      antworten();
    });
  };
  return mock;
}
