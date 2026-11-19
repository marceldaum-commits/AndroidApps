#!/usr/bin/env python3
"""Übernimmt geprüfte Korrekturen (JSON der Kartenprüfung) in webapp/pakete/*.json.
Ändert ein Feld nur, wenn der alte Wert noch genau so dasteht – sonst Meldung (nichts wird blind überschrieben)."""
import json, os, sys
WEB = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp', 'pakete')
ok = skip = 0
for datei in sys.argv[1:]:
    for k in json.load(open(datei, encoding='utf-8'))['korrekturen']:
        p = os.path.join(WEB, k['paket'] + '.json'); d = json.load(open(p, encoding='utf-8'))
        if k['karte'] == 'intro' or k['feld'] == 'intro':
            alt, neu = k['alt'], k['neu']
            treffer = [it for it in d.get('intro', []) if it.get('text') == alt or it == alt]
            if not treffer: print('⚠️  nicht gefunden (intro):', k['paket'], str(alt)[:60]); skip += 1; continue
            for it in treffer:
                if isinstance(neu, dict): it.update(neu)
                else: it['text'] = neu
        else:
            c = next((c for c in d['cards'] if c['id'] == k['karte']), None)
            if c is None or c.get(k['feld']) != k['alt']:
                print('⚠️  geändert/nicht gefunden:', k['paket'], k['karte'], k['feld'], '| jetzt:', str(c and c.get(k['feld']))[:60]); skip += 1; continue
            c[k['feld']] = k['neu']
        with open(p, 'w', encoding='utf-8') as fh: json.dump(d, fh, ensure_ascii=False, indent=2); fh.write('\n')
        ok += 1
print(f'{ok} übernommen, {skip} übersprungen')
