#!/usr/bin/env python3
"""Bewertet die gespeicherten Prüf-Ergebnisse neu und markiert unsichere Aufnahmen (Treffer < 0,5).
Unsichere Aufnahmen spielt die App nicht ab (sie nimmt dann die Gerätestimme) – lieber keine als eine falsche Aussprache."""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vertonen as V
pfad = os.path.join(V.WEB, 'audio', 'index.json')
idx = json.load(open(pfad, encoding='utf-8'))
for k, e in idx['saetze'].items():
    t = k.split('|', 1)[1]
    g = re.sub(r'\s*\(small\)$', '', e.get('gehoert', ''))
    e['treffer'] = round(max(e.get('treffer', 0), V.treffer(t, g)), 2) if g else e.get('treffer', 1)
    if e['treffer'] < 0.5: e['unsicher'] = True
    else: e.pop('unsicher', None)
json.dump(idx, open(pfad, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'), sort_keys=True)
unklar = sorted(((v['treffer'], kk, v['gehoert'], v.get('unsicher')) for kk, v in idx['saetze'].items() if v['treffer'] < 0.99))
n_uns = sum(1 for x in unklar if x[3])
with open(os.path.join(V.HIER, '..', 'HOERPRUEFUNG.md'), 'w', encoding='utf-8') as fh:
    fh.write('# Hörprüfung der Piper-Vertonung (automatisch)\n\n')
    fh.write(f'{len(idx["saetze"])} Sätze. Prüfung mit Whisper base, nicht eindeutige noch einmal mit Whisper small.\n\n')
    fh.write(f'- **{n_uns} unsicher (Treffer unter 0,5): diese Aufnahmen spielt die App NICHT ab**, dort liest die Stimme des Geräts.\n')
    fh.write(f'- {len(unklar) - n_uns} teilweise erkannt (meist Prüf-Fehler bei Einzelwörtern oder Redewendungen) – werden abgespielt, bitte nachhören.\n')
    fh.write('- Gleichklänge (two/to, four/for …), Zahlen als Ziffern und zusammengeschriebene Wörter zählen als erkannt.\n\n')
    fh.write('| Treffer | Stimme | Satz | Gehört | Abgespielt? |\n|---|---|---|---|---|\n')
    for q, kk, g, u in unklar:
        who, t = kk.split('|', 1); fh.write(f'| {q:.2f} | {who} | {t} | {g} | {"nein" if u else "ja"} |\n')
print(f'{len(idx["saetze"])} Sätze, {len(unklar)} nicht eindeutig, davon {n_uns} unsicher (nicht abgespielt)')
