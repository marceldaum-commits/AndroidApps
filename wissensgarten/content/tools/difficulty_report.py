#!/usr/bin/env python3
"""Schwierigkeit je Stufe (Klassen-Band) messen – gleiche Kennzahlen wie DifficultyTest (Kotlin).
Aufruf: python3 content/tools/difficulty_report.py [pakete...]"""
import json, glob, sys, statistics as st

BANDS = [("Grundschule", 1, 4), ("Klasse 5–7", 5, 7), ("Klasse 8–10", 8, 10), ("Oberstufe/Abi", 11, 13), ("Experte", 14, 14)]

def lev(a, b):
    a, b = a.lower(), b.lower()
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return prev[-1]

def sim(a, b):
    m = max(len(a), len(b))
    return 1.0 if m == 0 else 1 - lev(a, b) / m

def band(p):
    lo = min(p["grades"])
    return next(name for name, a, b in BANDS if a <= lo <= b)

def metrics(packs):
    cards = [c for p in packs for c in p["cards"]]
    withc = [c for c in cards if c.get("choices")]
    return {
        "pakete": len(packs), "karten": len(cards),
        "klasse": st.mean(st.mean(p["grades"]) for p in packs),
        "frei": sum(1 for c in cards if not c.get("choices")) / len(cards),
        "aehnlich": st.mean(max(sim(c["a"], x) for x in c["choices"]) for c in withc) if withc else 1.0,
        "laenge": st.mean(len(c["a"]) for c in cards),
    }

if __name__ == "__main__":
    files = sys.argv[1:] or glob.glob("content/src/main/assets/packs/*.json")
    packs = [json.load(open(f)) for f in files]
    if len(sys.argv) > 1:  # einzelne Pakete
        for p in packs:
            m = metrics([p]); print(f"{p['id']:34} Karten {m['karten']:3}  frei {m['frei']:5.0%}  Ablenker-Ähnlichkeit {m['aehnlich']:.2f}  Antwortlänge {m['laenge']:4.1f}")
        sys.exit(0)
    print("| Stufe | Pakete | Karten | Ø Klasse | freie Eingabe | Ablenker-Ähnlichkeit | Ø Antwortlänge |\n|---|---|---|---|---|---|---|")
    for name, *_ in BANDS:
        ps = [p for p in packs if band(p) == name]
        if not ps: print(f"| {name} | 0 | – | – | – | – | – |"); continue
        m = metrics(ps)
        print(f"| {name} | {m['pakete']} | {m['karten']} | {m['klasse']:.1f} | {m['frei']:.0%} | {m['aehnlich']:.2f} | {m['laenge']:.1f} |")
