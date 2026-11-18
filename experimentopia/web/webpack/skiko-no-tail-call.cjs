// Abhilfe für einen Fehler in Safari 27.2 (macOS, iPadOS, iOS; WebKit-Fehler 329421): Erreicht ein Aufruf eine WebAssembly-Funktion über einen JavaScript-Endaufruf
// („tail call“), übergibt Safari für Kommazahlen mit dem Wert 0 oder 1 fälschlich NaN. Die Grafik-Bibliothek (Skiko) ruft alle ihre rund 1000 Skia-Funktionen genau so auf:
//     (...a) => (X = loadedWasm._["X"])(...a)
// Folge: Compose misst und zeichnet mit kaputten Zahlen – die Seite bleibt weiß, Farbverläufe scheitern mit „Can't wrap nullptr“.
// Dieser Lader schreibt jeden solchen Aufruf in einen try-Block um; dort ist er kein Endaufruf mehr:
//     (...a) => { try { return (X = loadedWasm._["X"])(...a); } catch (e) { throw e; } }
// (Idee: github.com/White-Wind-LLC/table, Pull Request 174.) Kann wieder weg, sobald Safari oder Skiko das selbst behoben haben.
const TAIL = /=>\s*\(([^()]*(?:loadedWasm\._\[|wasmExports\[)[^()]*)\)\(([^()]*)\)/g;
function patch(source) {
  let n = 0;
  const out = source.replace(TAIL, (_, target, args) => { n++; return `=>{try{return(${target})(${args})}catch(e){throw e}}`; });
  return { out, n };
}
module.exports = function (source) {
  const { out, n } = patch(source);
  // Passt das Muster nicht mehr (neue Skiko-Fassung), soll der Bau das laut sagen – sonst käme die weiße Seite unbemerkt zurück
  if (n < 900) throw new Error(`skiko-no-tail-call: nur ${n} Aufrufe umgeschrieben – hat sich das Format von skiko.mjs geändert?`);
  if (TAIL.test(out)) throw new Error('skiko-no-tail-call: es sind Endaufrufe übrig');
  return out;
};
module.exports.patch = patch;
