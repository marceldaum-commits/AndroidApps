#!/usr/bin/env python3
"""Baut die Server-Vertonung (Tonstudio, Branch qq-audio-1) in webapp/audio/index.json ein.
Quellen: webapp/vorlesen/textliste.json (id, text, rollen) und webapp/audio/server/pruefung.json (Status/Treffer je id).
Ergebnis: index.json mit Schlüssel '<figur>|<text>' → { n, s } (Pfade relativ zu webapp/audio/), 'unsicher' bei Treffer < 0,5.
Deutsch (noch ohne Stimme in der App) und Einträge ohne Datei werden nicht aufgenommen.
Aufruf: python3 tools/server-audio-einbauen.py"""
import json, os, sys, unicodedata, re
WEB = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp')
ohne_ton = set(sys.argv[1:]) or {'de'}
liste = json.load(open(os.path.join(WEB, 'vorlesen', 'textliste.json'), encoding='utf-8'))
pruef = json.load(open(os.path.join(WEB, 'audio', 'server', 'pruefung.json'), encoding='utf-8'))
norm = lambda t: re.sub(r'\s+', ' ', unicodedata.normalize('NFC', t)).strip()
saetze = {}; fehlt = []; unsicher = 0; groesse = {}; dateien = {}
for e in liste['eintraege']:
    if e['sprache'] in ohne_ton: continue
    n = f"server/{e['sprache']}/{e['id']}.mp3"; s = f"server/{e['sprache']}/{e['id']}-langsam.mp3"
    if not (os.path.exists(os.path.join(WEB, 'audio', n)) and os.path.exists(os.path.join(WEB, 'audio', s))):
        fehlt.append(e['id']); continue
    p = pruef.get(e['id'], {}); q = p.get('treffer', 0)
    eintrag = {'sprache': e['sprache'], 'n': n, 's': s, 'stimme': liste['stimmen'][e['sprache']]['stimme'], 'treffer': round(q, 2), 'gehoert': p.get('gehoert', '')}
    if q < 0.5: eintrag['unsicher'] = True; unsicher += 1
    else: dateien.setdefault(e['sprache'], set()).update([n, s])
    for who in e['rollen']:
        saetze[f"{who}|{norm(e['text'])}"] = eintrag
# Größe je Sprache für den Offline-Download (Audio + Lernpfad + Pakete), in Bytes
pk = json.load(open(os.path.join(WEB, 'pakete', 'index.json'), encoding='utf-8'))['pakete']
for l, fs in dateien.items():
    b = sum(os.path.getsize(os.path.join(WEB, 'audio', f)) for f in fs)
    lp = os.path.join(WEB, 'lernpfad', l + '.json'); b += os.path.getsize(lp) if os.path.exists(lp) else 0
    b += sum(os.path.getsize(os.path.join(WEB, 'pakete', p['id'] + '.json')) for p in pk if p['sprache'] == l)
    groesse[l] = b
for l in ('la', 'de'):   # ohne Ton: nur Lernpfad und Pakete
    b = sum(os.path.getsize(os.path.join(WEB, 'pakete', p['id'] + '.json')) for p in pk if p['sprache'] == l)
    lp = os.path.join(WEB, 'lernpfad', l + '.json'); b += os.path.getsize(lp) if os.path.exists(lp) else 0
    groesse[l] = b
index = {'format': 2, 'quelle': 'Tonstudio (Kokoro-82M) – Branch qq-audio-1', 'groessen': groesse, 'saetze': saetze}
json.dump(index, open(os.path.join(WEB, 'audio', 'index.json'), 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'), sort_keys=True)
print({l: round(b / 1e6, 1) for l, b in groesse.items()}, 'MB'); print(f'{len(saetze)} Schlüssel, {unsicher} Einträge unsicher, {len(fehlt)} ohne Datei')
