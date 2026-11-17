// Schritt 2: alle Wörter, die „Thorsten“ sprechen können muss → stimme/build/woerter.txt
// (Sprechtexte nach thorstenNormalize, dazu alle Zahlwörter 0–999, Ordnungszahlen, Buchstaben)
const fs = require('fs'), path = require('path');
global.window = {};
const src = fs.readFileSync(path.join(__dirname, '../kopfrechnen/js/thorsten.js'), 'utf8').replace(/^'use strict';/, '').split('const Thorsten =')[0];
eval(src + ';global.F = { numWords, thorstenNormalize, nw999, ordWord, MONTHS };');
const W = new Set();
const add = s => (s.match(/[A-Za-zÄÖÜäöüß+]+/g) || []).forEach(t => t.split('+').forEach(w => w && W.add(w)));
for (const line of fs.readFileSync(path.join(__dirname, 'build/sprechtexte.txt'), 'utf8').split('\n')) add(F.thorstenNormalize(line));
for (let n = 0; n < 1000; n++) { W.add(F.nw999(n)); W.add(F.nw999(n, true)); }
for (let d = 1; d <= 31; d++) W.add(F.ordWord(d));
F.MONTHS.split('|').forEach(m => W.add(m));
'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').forEach(c => W.add(c));
['tausend', 'eine', 'Million', 'Millionen', 'Milliarde', 'Milliarden', 'Komma', 'minus', 'Euro', 'Cent', 'Uhr', 'Grad', 'Celsius', 'Hallo', 'ich', 'bin', 'Blitz'].forEach(w => W.add(w));
fs.writeFileSync(path.join(__dirname, 'build/woerter.txt'), [...W].sort().join('\n'));
console.log(W.size + ' Wörter');
