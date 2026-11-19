#!/usr/bin/env python3
"""Übernimmt Lektorats-Korrekturen in webapp/lernpfad/<sprache>.json. Aufruf: lernpfad-korrekturen.py <sprache> <korrekturen.json>
Jede Korrektur: {"einheit": "en-2-3", "pfad": "fehler.2" | "regel.text" | "geschichte.fragen.0" …, "neu": <Wert>}."""
import json, os, sys
lang, datei = sys.argv[1], sys.argv[2]
p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp', 'lernpfad', lang + '.json')
d = json.load(open(p, encoding='utf-8')); n = 0
for k in json.load(open(datei, encoding='utf-8'))['korrekturen']:
    e = next(e for a in d['abschnitte'] for e in a['einheiten'] if e['id'] == k['einheit'])
    teile = k['pfad'].split('.'); ziel = e
    for t in teile[:-1]: ziel = ziel[int(t)] if t.isdigit() else ziel[t]
    last = teile[-1]
    if last.isdigit(): ziel[int(last)] = k['neu']
    else: ziel[last] = k['neu']
    n += 1
open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=1) + '\n')
print(n, 'übernommen in', lang)
