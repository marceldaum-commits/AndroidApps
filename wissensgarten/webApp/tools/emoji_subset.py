#!/usr/bin/env python3
"""Emoji-Schrift für die WebApp verkleinern: nur Emojis, die in Paketen und im Code vorkommen (+ Grundvorrat).
Aufruf (im Ordner wissensgarten/): python3 webApp/tools/emoji_subset.py /usr/share/fonts/truetype/noto/NotoColorEmoji.ttf
Braucht: pip install fonttools. Lizenz der Schrift: SIL Open Font License 1.1 (Noto Color Emoji, Google)."""
import glob, os, re, sys
from fontTools import subset
from fontTools.ttLib import TTFont

src = sys.argv[1] if len(sys.argv) > 1 else "/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf"
out = "webApp/src/webMain/composeResources/font/noto_color_emoji.ttf"
text = ""
for pat in ["content/src/main/assets/packs/*.json", "shared/src/**/*.kt", "core/**/src/**/*.kt", "app/src/main/**/*.kt", "webApp/src/**/*.kt"]:
    for f in glob.glob(pat, recursive=True):
        text += open(f, encoding="utf-8").read()
cmap = TTFont(src)["cmap"].getBestCmap()
cps = {ord(c) for c in text if ord(c) in cmap and ord(c) > 0x7F}
# Grundvorrat: Smileys (für eigene Karten)
for a, b in [(0x1F600, 0x1F64F)]:
    cps |= {c for c in range(a, b + 1) if c in cmap}
# Nur, was wirklich vorkommt (+ Smileys): keine Hautfarben-/Flaggen-Vollsätze – das hielt die Schrift bei 6 MB, jetzt ca. 2 MB.
cps |= {0xFE0F, 0x20E3}
opts = subset.Options()
opts.layout_features = ["*"]
opts.notdef_outline = True
opts.name_IDs = ["*"]
opts.drop_tables = []
font = TTFont(src)
s = subset.Subsetter(opts)
s.populate(unicodes=sorted(cps))
s.subset(font)
font.save(out)
print(f"{len(cps)} Zeichen → {out} ({os.path.getsize(out) // 1024} KB)")

# Zweite Schrift: Pfeile, Mathe- und Sonderzeichen (DejaVu Sans, freie Lizenz) für alles, was keine Emoji ist
sym_src = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
sym_out = "webApp/src/webMain/composeResources/font/dejavu_symbols.ttf"
sym_cmap = TTFont(sym_src)["cmap"].getBestCmap()
sym = {ord(c) for c in text if ord(c) > 0x7F and ord(c) in sym_cmap and ord(c) not in cps}
sym |= {c for r in [(0x2190, 0x21FF), (0x2200, 0x22FF), (0x2070, 0x209F), (0x2150, 0x218F), (0x0370, 0x03FF), (0x00A0, 0x017F), (0x2010, 0x205E), (0x25A0, 0x25FF)] for c in range(r[0], r[1] + 1) if c in sym_cmap}
f2 = TTFont(sym_src)
s2 = subset.Subsetter(opts)
s2.populate(unicodes=sorted(sym))
s2.subset(f2)
f2.save(sym_out)
print(f"{len(sym)} Sonderzeichen → {sym_out} ({os.path.getsize(sym_out) // 1024} KB)")
