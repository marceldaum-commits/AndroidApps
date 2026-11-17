#!/usr/bin/env python3
"""Erzeugt das Lautschrift-Wörterbuch für Brummis natürliche Stimme „Thorsten“ – ein Werkzeug für den Entwicklungsrechner, NICHT Teil der App.

Warum: Die Stimme „Thorsten“ (Piper) braucht Lautschrift statt Buchstaben. Das Umsetzen erledigt üblicherweise espeak-ng (GPL-3).
Damit App und WebApp diesen Baustein nicht enthalten müssen, wird die Lautschrift hier vorab erzeugt und als Daten mitgeliefert:
  content/src/main/assets/stimme/woerter.tsv.gz   wort <Tab> Laute   (gzip; Zahlen 0–999 inklusive)
  content/src/main/assets/stimme/tokens.txt       Zeichenliste der Stimme (Laut → Nummer)
Texte mit einem Wort, das hier fehlt, liest in der App die Stimme des Geräts.

Woher die Wörter kommen (nur deutsche Texte – Antworten in Englisch/Französisch/Spanisch liest ohnehin die Gerätestimme):
  * alle freien Wissenspakete  content/src/main/assets/packs/*.json   (Titel, Beschreibung, Einführung, Frage, Antwort, info)
  * Plus-Pakete, falls vorhanden (nur die Wörter, die Pakete selbst kommen nicht ins Repo): --plus <Ordner>
  * Brummis Sätze und Oberflächen-Texte: Zeichenketten im Quellcode von shared/ und core/model/

Aufruf (aus wissensgarten/):
  pip install piper-tts                                  # bringt espeak-ng mit (nur hier am Rechner)
  python3 tools/stimme/lautschrift.py --plus /pfad/zu/wissensgarten-plus
Danach `./gradlew :content:test` – der Test StimmeAbdeckungTest meldet, wenn viele Wörter der Pakete fehlen.
"""
import argparse, glob, gzip, json, os, re, shutil
import piper
from piper.phonemize_espeak import EspeakPhonemizer

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, 'content/src/main/assets/stimme')
WORD = re.compile(r"[^\W\d_]+")
GERMAN_HINT = re.compile(r"\b(der|die|das|ein|eine|ist|was|wie|welche[rs]?|heißt|und|auf|von|mit|nicht)\b|[äöüß]", re.I)
FOREIGN = {'englisch', 'franzoesisch', 'spanisch'}
# Wörter, die die App beim Aufbereiten selbst einsetzt (SprechText.clean) oder für Zahlen braucht (Lautschrift.number)
EXTRA = ('zum Beispiel das heißt unter anderem und so weiter beziehungsweise circa vor nach Christus Nummer Seite vieles mehr et cetera '
         'minus plus gleich mal durch Prozent Grad Celsius Euro komma hundert tausend eintausend').split()

ap = argparse.ArgumentParser()
ap.add_argument('--plus', default='/home/user/website_backup/wissensgarten-plus', help='Ordner mit Plus-Paketen (nur Wörter werden übernommen)')
args = ap.parse_args()

def pack_texts(path):
    p = json.load(open(path, encoding='utf-8'))
    subject = p.get('subject')
    out = [p.get('title', ''), p.get('description', '')] + [s.get('text', '') for s in p.get('intro', [])]
    for c in p.get('cards', []): out += [c.get('q', ''), c.get('a', ''), c.get('info') or '']
    # wie Voice.localeFor: in Sprach-Fächern nur Texte, die erkennbar deutsch sind
    return [t for t in out if t and (subject not in FOREIGN or GERMAN_HINT.search(t))]

texts = []
files = sorted(glob.glob(os.path.join(ROOT, 'content/src/main/assets/packs/*.json')))
plus = sorted(glob.glob(os.path.join(args.plus, '*.json'))) if os.path.isdir(args.plus) else []
plus = [f for f in plus if os.path.basename(f) != 'versions.json']
for f in files + plus: texts += pack_texts(f)
for path in glob.glob(os.path.join(ROOT, 'shared/src/commonMain/**/*.kt'), recursive=True) + glob.glob(os.path.join(ROOT, 'core/model/src/commonMain/**/*.kt'), recursive=True):
    for lit in re.findall(r'"((?:[^"\\\n]|\\.)*)"', open(path, encoding='utf-8').read()):
        lit = re.sub(r'\$\{[^}]*\}|\$[A-Za-z_]\w*|\\[nt]', ' ', lit)
        if ' ' in lit.strip() or re.fullmatch(r"[A-ZÄÖÜ][a-zäöüß]+", lit): texts.append(lit)

keys = set(w.lower() for w in EXTRA)
for t in texts:
    for w in WORD.findall(''.join(c if c.isalpha() else ' ' for c in t)):   # nur Buchstaben – wie \p{L} in der App
        if len(w) > 40: continue
        keys.add(w if (len(w) >= 2 and w.isupper()) else w.lower())   # Abkürzungen wie „DNA“ bleiben groß (werden buchstabiert)
keys.update('abcdefghijklmnopqrstuvwxyzäöüß')

P = EspeakPhonemizer(os.path.join(os.path.dirname(piper.__file__), 'espeak-ng-data'))
def sentences(text):
    return [''.join(s).strip() for s in P.phonemize('de', text) if ''.join(s).strip()]
def alone(word):
    return ' '.join(sentences(word)).rstrip('.').strip()
TAIL = alone('Tisch')
def in_context(word):
    """Laute eines Wortes, wie es mitten im Satz klingt (kleine Wörter wie „der“, „bei“ bleiben dort unbetont)."""
    got = ' '.join(sentences(word + ' Tisch'))
    if got.endswith(' ' + TAIL) and ' ' not in got[:-len(TAIL) - 1].strip(): return got[:-len(TAIL) - 1].strip()
    return alone(word)

VOWEL = re.compile('[AEIOUÄÖÜ]')
def acronym(w):
    """Abkürzung in Großbuchstaben: kurze (bis 3) oder ohne Selbstlaut buchstabieren (A-T-P), längere wie NATO liest espeak selbst.
    Römische Zahlen (II, IV …) kennt espeak („römisch zwei“)."""
    if re.fullmatch('[IVXLCDM]+', w) or (len(w) > 3 and VOWEL.search(w)): return alone(w)
    return alone(' '.join(w))

# großgeschriebene Wörter, die es auch klein gibt („ICH“, „BALD“), sind nur betont – die liest die Stimme wie das kleine Wort
keys = {w for w in keys if not (w.isupper() and len(w) >= 2 and w.lower() in keys)}
lines = []
for w in sorted(keys):
    ph = acronym(w) if (len(w) >= 2 and w.isupper()) else alone(w) if len(w) == 1 else in_context(w)
    if ph and '\t' not in ph: lines.append(w + '\t' + ph)
for n in range(0, 1000): lines.append(str(n) + '\t' + alone(str(n)))
os.makedirs(OUT, exist_ok=True)
data = ('\n'.join(lines) + '\n').encode('utf-8')
with open(os.path.join(OUT, 'woerter.tsv.gz'), 'wb') as raw:
    with gzip.GzipFile(fileobj=raw, mode='wb', compresslevel=9, mtime=0, filename='') as f: f.write(data)   # mtime=0: gleiche Eingabe → gleiche Datei
tokens = os.path.join(ROOT, '../raumlicht/app/src/main/assets/stimme/tokens.txt')
if not os.path.exists(os.path.join(OUT, 'tokens.txt')) and os.path.exists(tokens): shutil.copy(tokens, os.path.join(OUT, 'tokens.txt'))
print(len(files), 'freie +', len(plus), 'Plus-Pakete,', len(texts), 'Texte →', len(keys), 'Wörter + 1000 Zahlen;',
      len(data) // 1024, 'KB roh,', os.path.getsize(os.path.join(OUT, 'woerter.tsv.gz')) // 1024, 'KB gepackt →', OUT)
