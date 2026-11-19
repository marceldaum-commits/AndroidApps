#!/usr/bin/env python3
"""Nimmt neue, selbst geschriebene und lektorierte Pakete auf: kopiert <id>.json nach webapp/pakete/ und trägt sie in index.json ein
(hinter die letzten Pakete derselben Sprache). Prüft Format, eindeutige IDs, genau eine richtige Antwort, keine Lehrwerksnamen.
Aufruf: python3 tools/neue-pakete.py <sprache> <datei.json> [...]"""
import json, os, re, sys
WEB = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp', 'pakete')
LEHRWERKE = re.compile(r"green line|camden town|english g\b|lighthouse|highlight\b|access\b|découvertes|decouvertes|à plus|a plus!|tous ensemble|cursus|prima\b|campus\b|pontes|felix\b|adelante|encuentros|vía rápida|via rapida", re.I)
lang = sys.argv[1]
idx = json.load(open(os.path.join(WEB, 'index.json')))
for f in sys.argv[2:]:
    p = json.load(open(f)); pid = p['id']
    assert re.fullmatch(r'[a-z0-9-]+', pid), pid
    roh = json.dumps(p, ensure_ascii=False); assert not LEHRWERKE.search(roh), (pid, LEHRWERKE.search(roh).group(0))
    ids = [c['id'] for c in p['cards']]; assert len(ids) == len(set(ids)), pid
    for c in p['cards']:
        assert c.get('q') and c.get('a') and c.get('img'), (pid, c.get('id'))
        ch = c.get('choices') or []
        assert c['a'] not in ch and len(set(ch)) == len(ch), (pid, c['id'])
    p.setdefault('tier', 'free'); p.setdefault('version', 1)
    json.dump(p, open(os.path.join(WEB, pid + '.json'), 'w'), ensure_ascii=False, indent=2); open(os.path.join(WEB, pid + '.json'), 'a').write('\n')
    eintrag = {'id': pid, 'sprache': lang, 'version': p['version'], 'title': p['title'], 'emoji': p['emoji'], 'grades': p['grades'], 'description': p['description'], 'karten': len(p['cards'])}
    alt = [i for i, m in enumerate(idx['pakete']) if m['id'] == pid]
    if alt: idx['pakete'][alt[0]] = eintrag
    else:
        pos = max([i for i, m in enumerate(idx['pakete']) if m['sprache'] == lang], default=len(idx['pakete']) - 1) + 1
        idx['pakete'].insert(pos, eintrag)
    print(pid, len(p['cards']), 'Karten')
json.dump(idx, open(os.path.join(WEB, 'index.json'), 'w'), ensure_ascii=False, indent=1); open(os.path.join(WEB, 'index.json'), 'a').write('\n')
