#!/usr/bin/env python3
"""Prüft Wissenspakete nach PACK_FORMAT.md (+ Emoji-Version ≤ 11, Größe ≤ 1 MB).
Aufruf: python3 content/tools/check_pack.py content/src/main/assets/packs/*.json   (braucht: pip install emoji)"""
import json, re, sys, os
import emoji

SUBJECTS = {"deutsch","mathe","sachunterricht","englisch","franzoesisch","latein","spanisch","biologie","chemie","physik",
            "informatik","geschichte","erdkunde","politik","wirtschaft","ethik","musik","kunst","allgemeinwissen"}
OK_EXTRA = {"️", "⃣"}

def emoji_errors(s, where):
    errs = []
    if any(c.isascii() and c.isalpha() for c in s): errs.append(f"{where}: Buchstaben im Merkbild")
    if "‍" in s: errs.append(f"{where}: ZWJ-Kombination")
    if any(0x1F1E6 <= ord(c) <= 0x1F1FF for c in s): errs.append(f"{where}: Flagge")
    if any(0x1F3FB <= ord(c) <= 0x1F3FF for c in s): errs.append(f"{where}: Hautfarbe")
    for e in emoji.emoji_list(s):
        v = emoji.EMOJI_DATA.get(e["emoji"], {}).get("E", 99)
        if v > 11: errs.append(f"{where}: {e['emoji']} ist Emoji {v} (nur ≤ 11 erlaubt)")
    rest = emoji.replace_emoji(s, "")
    if rest.strip(" " + "".join(OK_EXTRA)): errs.append(f"{where}: kein reines Emoji: {rest!r}")
    return errs

def check(path):
    errs = []
    raw = open(path, encoding="utf-8").read()
    if len(raw.encode()) > 1_000_000: errs.append("größer als 1 MB")
    try: p = json.loads(raw)
    except Exception as e: return [f"kein gültiges JSON: {e}"]
    pid = p.get("id", "")
    if os.path.basename(path) != f"{pid}.json": errs.append("Dateiname ≠ id")
    if not re.fullmatch(r"(gs|s1|ob|aw)-[a-z0-9-]+", pid): errs.append("id-Format")
    if not isinstance(p.get("version"), int) or p["version"] < 1: errs.append("version")
    if p.get("subject") not in SUBJECTS: errs.append(f"Fach {p.get('subject')}")
    g = p.get("grades") or []
    if not g or any(x not in range(1, 15) for x in g): errs.append("grades")
    if p.get("tier") not in ("free", "plus"): errs.append("tier")
    for k in ("title", "description", "emoji"):
        if not str(p.get(k, "")).strip(): errs.append(f"{k} fehlt")
    if len(p.get("title", "")) > 32: errs.append("Titel zu lang")
    errs += emoji_errors(p.get("emoji", ""), "Paket-emoji")
    intro = p.get("intro") or []
    if not 3 <= len(intro) <= 6: errs.append("intro: 3–6 Szenen")
    for i, s in enumerate(intro):
        if not s.get("text") or len(s["text"]) > 170: errs.append(f"intro {i}: Text leer/zu lang ({len(s.get('text',''))})")
        errs += emoji_errors(s.get("img", ""), f"intro {i}")
    cards = p.get("cards") or []
    if not 15 <= len(cards) <= 60: errs.append(f"Kartenzahl {len(cards)}")
    ids, qa = set(), set()
    primary = all(x <= 4 for x in g)
    for c in cards:
        cid = c.get("id", "?")
        if cid in ids: errs.append(f"{cid}: doppelte id")
        ids.add(cid)
        q, a = c.get("q", ""), c.get("a", "")
        if not q.strip() or not a.strip(): errs.append(f"{cid}: leer")
        if len(q) > 120: errs.append(f"{cid}: Frage > 120 Zeichen")
        if len(a) > 80: errs.append(f"{cid}: Antwort > 80 Zeichen")
        if len(c.get("info", "") or "") > 160: errs.append(f"{cid}: info > 160 Zeichen")
        if (q, a) in qa: errs.append(f"{cid}: doppelte Karte")
        qa.add((q, a))
        img = c.get("img") or ""
        if not img.strip(): errs.append(f"{cid}: img fehlt")
        elif len(img) > 24: errs.append(f"{cid}: img zu lang")
        else: errs += emoji_errors(img, cid)
        ch = c.get("choices")
        if ch is not None:
            if not 2 <= len(ch) <= 3: errs.append(f"{cid}: 2–3 choices")
            if a in ch: errs.append(f"{cid}: Antwort in choices")
            if len(set(ch)) != len(ch): errs.append(f"{cid}: doppelte choices")
        elif primary: errs.append(f"{cid}: Grundschule braucht choices")
    return errs

bad = 0
for f in sys.argv[1:]:
    e = check(f)
    if e:
        bad += 1
        print(f"✗ {os.path.basename(f)}"); [print("   ", x) for x in e[:40]]
print(f"{len(sys.argv) - 1 - bad}/{len(sys.argv) - 1} Pakete in Ordnung")
sys.exit(1 if bad else 0)
