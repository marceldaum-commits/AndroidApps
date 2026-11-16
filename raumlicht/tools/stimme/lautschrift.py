#!/usr/bin/env python3
"""Erzeugt die Lautschrift-Dateien für Lux' natürliche Stimme – ein Werkzeug für den Entwicklungsrechner, NICHT Teil der App.

Warum: Die Stimme „Thorsten“ (Piper) braucht Lautschrift statt Buchstaben. Das Umsetzen erledigt üblicherweise espeak-ng (GPL-3).
Damit die App diesen Baustein nicht enthalten muss, wird die Lautschrift hier vorab erzeugt und als Daten mitgeliefert:
  saetze.tsv   feste Texte der App, als ganzer Satz umgesetzt (beste Betonung):   Text <Tab> Laute Satz 1 | Laute Satz 2 …
  woerter.tsv  Wörterbuch für veränderliche Texte:                               wort <Tab> Laute
Unbekannte Wörter liest in der App die Stimme des Handys.

Aufruf (aus raumlicht/):
  ./gradlew :core:model:jvmTest --tests '*SpeechDumpTest*'          # sammelt die Texte nach core/model/build/sprechtexte.txt
  python tools/stimme/lautschrift.py                                 # braucht: pip install piper-tts  (bringt espeak-ng mit)
Nach jeder Änderung an Aufgaben- oder Sprechtexten neu laufen lassen; ein Test (StimmeTest) meldet fehlende Texte.
"""
import os, re, sys, glob
import piper
from piper.phonemize_espeak import EspeakPhonemizer

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, 'app/src/main/assets/stimme')
P = EspeakPhonemizer(os.path.join(os.path.dirname(piper.__file__), 'espeak-ng-data'))
WORD = re.compile(r"[A-Za-zÄÖÜäöüß]+")

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

fixed, texts = [], []
for line in open(os.path.join(ROOT, 'core/model/build/sprechtexte.txt'), encoding='utf-8'):
    kind, text = line.rstrip('\n').split('\t', 1)
    texts.append(text)
    if kind == 'fest': fixed.append(text)

# Wörter: aus allen gesammelten Texten und – als weites Netz – aus allen Texten im Quellcode der gemeinsamen Module
words = set()
for t in texts: words.update(w.lower() for w in WORD.findall(t))
for path in glob.glob(os.path.join(ROOT, 'core/model/src/commonMain/**/*.kt'), recursive=True) + glob.glob(os.path.join(ROOT, 'shared/src/commonMain/**/*.kt'), recursive=True):
    for lit in re.findall(r'"((?:[^"\\\n]|\\.)*)"', open(path, encoding='utf-8').read()):
        if ' ' in lit or re.fullmatch(r"[A-ZÄÖÜ][a-zäöüß]+", lit): words.update(w.lower() for w in WORD.findall(lit))
words.update('abcdefghijklmnopqrstuvwxyzäöü')
words.update(['komma', 'minus', 'tausend', 'eintausend', 'plus', 'gleich', 'hoch', 'mal', 'durch', 'prozent', 'grad', 'strich'])
words = sorted(w for w in words if len(w) <= 40)

os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, 'saetze.tsv'), 'w', encoding='utf-8') as f:
    for t in sorted(set(fixed)): f.write(t + '\t' + '|'.join(sentences(t)) + '\n')
with open(os.path.join(OUT, 'woerter.tsv'), 'w', encoding='utf-8') as f:
    for w in words:
        ph = alone(w) if len(w) == 1 else in_context(w)
        if ph: f.write(w + '\t' + ph + '\n')
    for n in range(0, 1000): f.write(str(n) + '\t' + alone(str(n)) + '\n')
print(len(set(fixed)), 'feste Texte,', len(words), 'Wörter, 1000 Zahlen →', OUT)
