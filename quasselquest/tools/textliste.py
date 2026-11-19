#!/usr/bin/env python3
"""Textliste für die Vertonung auf dem Server (Kokoro-82M, Leitstand). Format: siehe ../VERTONUNG.md.
Aufruf: python3 tools/textliste.py  → webapp/vorlesen/textliste.json"""
import hashlib, json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vertonen as V  # saetze(), sprechtext(), norm() – dieselbe Textaufbereitung wie bei der bisherigen Vertonung

# Vorschlag bis zu Marcels Wahl (Hörproben: webapp/hoerproben/). Tempo: EN 1.5 (Marcel), FR/ES 1.25 (bei 1.5 schlecht verständlich)
STIMME = {'en': 'bf_emma', 'fr': 'ff_siwis', 'es': 'ef_dora', 'it': 'if_sara'}
TEMPO = {'en': 1.5, 'fr': 1.25, 'es': 1.25, 'it': 1.25}
LANGSAM = 0.75  # langsame Fassung (🐢), Kokoro speed
KOKORO_LANG = {'en': 'en-gb', 'fr': 'fr-fr', 'es': 'es', 'it': 'it'}

def sprache(who):
    return V.STIMMEN[who].get('sprache', 'en')

eintraege = {}
for s in V.saetze():
    l = sprache(s['who'])
    if l not in STIMME: continue
    sprech = V.sprechtext(s['text'])
    sid = hashlib.sha1(f'{l}|{V.norm(sprech)}'.encode()).hexdigest()[:20]
    e = eintraege.setdefault(sid, {'id': sid, 'sprache': l, 'text': s['text'], 'sprechtext': sprech, 'rollen': []})
    if s['who'] not in e['rollen']: e['rollen'].append(s['who'])
liste = sorted(eintraege.values(), key=lambda e: (e['sprache'], e['id']))
for e in liste:
    l = e['sprache']; einzel = ' ' not in e['sprechtext'].strip()
    e['fassungen'] = [
        {'art': 'normal', 'speed': TEMPO[l] if not einzel else round(TEMPO[l] / 1.15, 2), 'datei': f'audio/kokoro/{e["id"]}.mp3'},
        {'art': 'langsam', 'speed': LANGSAM, 'datei': f'audio/kokoro/{e["id"]}-langsam.mp3'},
    ]
out = {'format': 1, 'app': 'quasselquest', 'modell': 'Kokoro-82M (hexgrad, Apache-2.0)',
       'stimmen': {l: {'stimme': STIMME[l], 'kokoro_lang': KOKORO_LANG[l]} for l in STIMME},
       'mp3': {'kanaele': 1, 'abtastrate': 24000, 'bitrate': '48k', 'id3_comment': 'KI-generiert (synthetische Stimme, Kokoro-82M)',
               'stille_rand_kuerzen': True, 'lautheit_lufs': -18, 'vorlauf_s': 0.12},
       'eintraege': liste}
ziel = os.path.join(V.WEB, 'vorlesen', 'textliste.json'); os.makedirs(os.path.dirname(ziel), exist_ok=True)
json.dump(out, open(ziel, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
from collections import Counter
print(dict(Counter(e['sprache'] for e in liste)), len(liste), 'Texte →', ziel)
