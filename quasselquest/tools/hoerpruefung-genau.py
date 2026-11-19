#!/usr/bin/env python3
"""Zweite, genauere Hörprüfung (Whisper small) nur für die Sätze, die beim Vertonen nicht eindeutig erkannt wurden.
Aktualisiert treffer/gehoert in webapp/audio/index.json und schreibt HOERPRUEFUNG.md neu."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vertonen as V
from faster_whisper import WhisperModel
import numpy as np, subprocess
pfad = os.path.join(V.WEB, 'audio', 'index.json')
idx = json.load(open(pfad, encoding='utf-8'))
m = WhisperModel('small', device='cpu', compute_type='int8')
def hoere(mp3, lang):
    raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', mp3, '-f', 's16le', '-ac', '1', '-ar', '16000', '-'])
    a = np.frombuffer(raw, np.int16).astype(np.float32) / 32768.0
    return ' '.join(x.text for x in m.transcribe(a, language=lang, beam_size=3)[0]).strip()
n = besser = 0
for k, e in idx['saetze'].items():
    if e.get('treffer', 1) >= 0.99: continue
    who, text = k.split('|', 1); lang = V.STIMMEN[who].get('sprache', 'en'); n += 1
    g = hoere(os.path.join(V.WEB, 'audio', 'piper', e['n']), lang)
    q = V.treffer(text, g)
    if q > e['treffer']:
        e['treffer'] = round(q, 2); e['gehoert'] = g + ' (small)'; besser += 1
json.dump(idx, open(pfad, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'), sort_keys=True)
unklar = sorted(((v['treffer'], kk, v['gehoert']) for kk, v in idx['saetze'].items() if v['treffer'] < 0.99))
with open(os.path.join(V.HIER, '..', 'HOERPRUEFUNG.md'), 'w', encoding='utf-8') as fh:
    fh.write('# Hörprüfung der Piper-Vertonung (automatisch)\n\n')
    fh.write(f'{len(idx["saetze"])} Sätze. Erste Prüfung mit Whisper base, nicht eindeutige danach noch einmal mit Whisper small.\n')
    fh.write(f'Danach nicht eindeutig: {len(unklar)} (Gleichklänge wie two/to zählen als erkannt). Einzelwörter erkennt die Prüfung schlechter als ein Mensch;\n')
    fh.write('die Liste ist zum Nachhören auf einem echten Gerät gedacht.\n\n| Treffer | Stimme | Satz | Gehört |\n|---|---|---|---|\n')
    for q, kk, g in unklar:
        who, t = kk.split('|', 1); fh.write(f'| {q:.2f} | {who} | {t} | {g} |\n')
print(f'{n} nachgeprüft, {besser} jetzt besser erkannt, {len(unklar)} bleiben unklar')
