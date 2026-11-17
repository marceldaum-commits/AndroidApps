#!/usr/bin/env python3
"""Schritt 3: Lautschrift-Wörterbuch für die Stimme „Thorsten“ – Werkzeug für den Entwicklungsrechner, NICHT Teil der App.

Die Piper-Stimme braucht Laute statt Buchstaben. Das Umsetzen macht üblicherweise espeak-ng (GPL-3). Damit die App diesen
Baustein nicht enthält, werden die Laute hier vorab erzeugt und als reine Daten ausgeliefert (wie bei Raumlicht):
  build/stimme-de.json   { "w": { Wort: "Laute, durch | getrennt" } }
Die Laute werden erst in der App mit der .onnx.json des Modells auf unserem Server in Zahlen (IDs) übersetzt – so passt das
Wörterbuch zu jeder Fassung der Thorsten-Stimme (ältere ohne zusammengezogene Doppellaute, neuere mit).
Aufruf (aus dem Repo-Wurzelordner):
  python3 -m venv /tmp/piper && /tmp/piper/bin/pip install piper-tts==1.8.0
  /tmp/piper/bin/python stimme/lautschrift.py pfad/zu/de_DE-thorsten-medium.onnx.json
Nach jeder Änderung an Aufgaben- oder Sprechtexten neu laufen lassen (Schritte 1–3, siehe stimme/README.md).
"""
import json, os, sys, unicodedata
from piper.phonemize_espeak import EspeakPhonemizer

HERE = os.path.dirname(os.path.abspath(__file__))
cfg = json.load(open(sys.argv[1], encoding='utf-8'))
idmap = {k: v[0] for k, v in cfg['phoneme_id_map'].items()}
clusters = set(tuple(c) for c in cfg.get('vowel_clusters', []))
P = EspeakPhonemizer()

def phonemes(text):
    sents = P.phonemize(cfg['espeak']['voice'], text, clusters)
    return [p for s in sents for p in s]

TAIL = phonemes('Tisch')
# kleine Funktionswörter klingen im Satz unbetont – nur sie werden „im Satz“ umgesetzt, alles andere einzeln (betont)
SMALL = set('der die das den dem des ein eine einen einem einer eines und oder aber von zu zum zur bei mit für auf in im ins am an aus nach vor als wie so es er sie wir du ich ihr man sich ist sind hat bis um über unter durch'.split())
def word_ids(w):
    got = phonemes(w + ' Tisch') if w.lower() in SMALL else []
    if len(got) > len(TAIL) + 1 and got[-len(TAIL):] == TAIL and got[-len(TAIL) - 1] == ' ':
        ph = got[:-len(TAIL) - 1]
    else:
        ph = phonemes(w)
    ph = [p for p in ph if p not in '.,!?;:']
    while ph and ph[-1] == ' ': ph.pop()
    return [p for p in ph if p in idmap or all(c in idmap for c in p)]

words = [w for w in open(os.path.join(HERE, 'build/woerter.txt'), encoding='utf-8').read().split('\n') if w]
out = {}
for i, w in enumerate(words):
    ph = word_ids(w)
    if ph: out[w] = '|'.join(ph)
    if i % 500 == 0: print(i, '/', len(words), file=sys.stderr)
json.dump({'w': out, 'n': len(out), 'v': 2},
          open(os.path.join(HERE, 'build/stimme-de.json'), 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print(len(out), 'Wörter mit Lautschrift')
