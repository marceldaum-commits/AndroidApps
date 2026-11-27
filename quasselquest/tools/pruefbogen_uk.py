#!/usr/bin/env python3
"""Prüfbogen für die Muttersprachler-Prüfung des Ukrainischen (uk.json und de-uk.json) → PRUEFBOGEN-UKRAINISCH.md"""
import json, os
B = os.path.dirname(os.path.abspath(__file__)) + '/../'
out = ['# Prüfbogen Ukrainisch (Muttersprachler-Prüfung)\n',
 'Bitte jede Zeile lesen und bei Fehlern einen Haken in „Korrektur nötig“ setzen und den besseren Text daneben schreiben. Geprüft werden: Rechtschreibung, Grammatik, Natürlichkeit, Übersetzung, kein Russismus.\n',
 'Teil A = Ukrainisch lernen (deutsche Kinder). Teil B = Deutsch lernen (ukrainische Kinder; hier sind die Erklärungen auf Ukrainisch).\n']
def teil(name, pfad, sprachfelder):
    lp = json.load(open(B + 'webapp/lernpfad/' + pfad, encoding='utf-8'))
    out.append(f'\n## {name}\n')
    for a in lp['abschnitte']:
        for e in a['einheiten']:
            out.append(f'\n### {e["id"]} – {e["titel"]}\n')
            out.append('| Typ | Text | Übersetzung / Hinweis | Korrektur nötig |\n|---|---|---|---|')
            for w in e['woerter']: out.append(f'| Wort | {w[0]} | {w[1]}{" – "+w[3] if len(w)>3 else ""} | ☐ |')
            for s in e['saetze']: out.append(f'| Satz | {s[0]} | {s[1]} | ☐ |')
            r = e['regel']; out.append(f'| Regel | {r["titel"]} | {r["text"]} | ☐ |')
            for b in r.get('beispiele', []): out.append(f'| Beispiel | {b[0]} | {b[1]} | ☐ |')
            for f, z, u in e['geschichte']['zeilen']: out.append(f'| Geschichte | {z} | {u} | ☐ |')
            for q in e['geschichte']['fragen']: out.append(f'| Frage | {q["q"]} | Antwort: {q["a"]} (falsch: {", ".join(q["choices"])}) | ☐ |')
            for x in e.get('aktionen', []): out.append(f'| Aktion | {x["sag"]} | {x["de"]} | ☐ |')
            for f in e.get('fehler', []): out.append(f'| Fehler-Aufgabe | {f["falsch"]} → {f["richtig"]} | Tipp: {f["tipp"]} | ☐ |')
teil('Teil A – Ukrainisch lernen (uk.json)', 'uk.json', None)
teil('Teil B – Deutsch für ukrainische Kinder (de-uk.json)', 'de-uk.json', None)
open(B + 'PRUEFBOGEN-UKRAINISCH.md', 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(out), 'Zeilen')
