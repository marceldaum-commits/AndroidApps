#!/usr/bin/env python3
"""Textliste „uk“ (Ukrainisch) für das Tonstudio. Eigene Datei, damit die eingefrorene Kokoro-Liste (textliste.json) unverändert bleibt.
Stimme: Piper uk_UA-ukrainian_tts-medium, Sprecherin „tetiana“ (Speaker 2), Tempo 1,0 / langsam 0,8. Format wie VERTONUNG.md (format 1).
Aufruf: python3 tools/textliste_uk.py  → webapp/vorlesen/textliste-uk.json"""
import hashlib, json, os, re, sys, unicodedata
WEB = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp')

def norm(t):
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFC', t)).strip()

def sprechbar(t):
    t = re.sub(r'\s*\([^)]*\)', '', t)
    t = re.sub(r'\s*/\s*', ', ', t)
    return norm(t.replace('…', ''))

lp = json.load(open(os.path.join(WEB, 'lernpfad', 'uk.json'), encoding='utf-8'))
texte = {}
def add(t, rolle):
    s = sprechbar(t)
    if not s or '→' in s: return
    sid = hashlib.sha1(f'uk|{norm(s)}'.encode()).hexdigest()[:20]
    e = texte.setdefault(sid, {'id': sid, 'sprache': 'uk', 'text': t, 'sprechtext': s, 'rollen': []})
    if rolle not in e['rollen']: e['rollen'].append(rolle)
for a in lp['abschnitte']:
    for e in a['einheiten']:
        for w in e['woerter']: add(w[0], 'tetiana')
        for s in e['saetze']: add(s[0], 'tetiana')
        for b in e['regel'].get('beispiele', []): add(b[0], 'tetiana')
        for f, z, _ in e['geschichte']['zeilen']: add(z, 'tetiana')
        for x in e.get('aktionen', []): add(x['sag'], 'tetiana')
        for fe in e.get('fehler', []): add(fe['richtig'], 'tetiana')
liste = sorted(texte.values(), key=lambda e: e['id'])
for e in liste:
    einzel = ' ' not in e['sprechtext']
    e['fassungen'] = [
        {'art': 'normal', 'speed': 1.0 if not einzel else round(1.0 / 1.15, 2), 'datei': f'audio/server/uk/{e["id"]}.mp3'},
        {'art': 'langsam', 'speed': 0.8, 'datei': f'audio/server/uk/{e["id"]}-langsam.mp3'},
    ]
kommentar = 'KI-generiert (synthetische Stimme, Piper uk_UA-ukrainian_tts-medium)'
out = {'format': 1, 'app': 'quasselquest', 'modell': 'Piper uk_UA-ukrainian_tts-medium (Datensatz ukrainian-tts-datasets, Yehor Smoliakov, Apache-2.0)',
       'stimmen': {'uk': {'stimme': 'tetiana', 'piper_modell': 'uk_UA-ukrainian_tts-medium', 'speaker_id': 2, 'hinweis': 'Sprecherin 2 „tetiana“ (Recht und Marcel freigegeben)'}},
       'mp3': {'kanaele': 1, 'abtastrate': 22050, 'bitrate': '48k', 'id3_comment': {'uk': kommentar}, 'stille_rand_kuerzen': True, 'lautheit_lufs': -18, 'vorlauf_s': 0.12,
               'wasserzeichen': 'Perth, falls das Tonstudio es für alle Stimmen anwendet (Recht V36a) – bitte bestätigen'},
       'eintraege': liste}
ziel = os.path.join(WEB, 'vorlesen', 'textliste-uk.json')
json.dump(out, open(ziel, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(liste), 'Texte ukrainisch →', ziel, '· sha256', hashlib.sha256(open(ziel, 'rb').read()).hexdigest()[:16])
