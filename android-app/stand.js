// Erzeugt kopfrechnen/stand.json: Version, Hüllen-Nummer und SHA-256 jeder Datei.
// Die Android-App lädt damit neue Inhalte selbst (GitHub, Ersatz: eigener Server) – ohne Neuinstallation.
// Aufruf im Repo-Stamm oder hier:  node android-app/stand.js          (schreibt die Datei)
//                                  node android-app/stand.js --check  (Fehler, wenn stand.json veraltet ist)
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const root = path.join(__dirname, '..', 'kopfrechnen'), out = path.join(root, 'stand.json');
const SKIP = new Set(['stand.json', 'sw.js']);   // sw.js braucht die App nicht
const files = {};
(function walk(dir) {
  for (const n of fs.readdirSync(dir).sort()) {
    const f = path.join(dir, n), rel = path.relative(root, f).split(path.sep).join('/');
    if (n.startsWith('.')) continue;
    if (fs.statSync(f).isDirectory()) walk(f);
    else if (!SKIP.has(rel)) files[rel] = crypto.createHash('sha256').update(fs.readFileSync(f)).digest('hex');
  }
})(root);
const version = fs.readFileSync(path.join(root, 'js/changelog.js'), 'utf8').match(/APP_VERSION = '([\d.]+)'/)[1];
const shell = +fs.readFileSync(path.join(__dirname, 'SHELL'), 'utf8').trim();
const id = crypto.createHash('sha256').update(JSON.stringify([version, shell, files])).digest('hex').slice(0, 16);
const json = `{"app":"rechenblitz","version":"${version}","shell":${shell},"id":"${id}","files":{\n${Object.entries(files).map(([k, v]) => `"${k}":"${v}"`).join(',\n')}\n}}\n`;
if (process.argv.includes('--check')) {
  const cur = fs.existsSync(out) ? fs.readFileSync(out, 'utf8') : '';
  if (cur !== json) { console.error('kopfrechnen/stand.json ist veraltet – bitte „node android-app/stand.js“ ausführen und mit committen.'); process.exit(1); }
  console.log(`stand.json aktuell: ${version} · Hülle ${shell} · ${id}`);
} else { fs.writeFileSync(out, json); console.log(`stand.json: ${version} · Hülle ${shell} · ${Object.keys(files).length} Dateien · ${id}`); }
