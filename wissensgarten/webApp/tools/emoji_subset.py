#!/usr/bin/env python3
"""Emoji-Schrift für die WebApp verkleinern: nur Emojis, die in Paketen und im Code vorkommen (+ Grundvorrat).
Aufruf (im Ordner wissensgarten/): python3 webApp/tools/emoji_subset.py /usr/share/fonts/truetype/noto/NotoColorEmoji.ttf
Braucht: pip install fonttools. Lizenz der Schrift: SIL Open Font License 1.1 (Noto Color Emoji, Google)."""
import glob, os, re, sys
from fontTools import subset
from fontTools.ttLib import TTFont

src = sys.argv[1] if len(sys.argv) > 1 else "/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf"
out = "webApp/src/wasmJsMain/composeResources/font/noto_color_emoji.ttf"
text = ""
for pat in ["content/src/main/assets/packs/*.json", "shared/src/**/*.kt", "core/**/src/**/*.kt", "app/src/main/**/*.kt"]:
    for f in glob.glob(pat, recursive=True):
        text += open(f, encoding="utf-8").read()
cmap = TTFont(src)["cmap"].getBestCmap()
cps = {ord(c) for c in text if ord(c) in cmap and ord(c) > 0x7F}
# Grundvorrat: Smileys (für eigene Karten)
for a, b in [(0x1F600, 0x1F64F)]:
    cps |= {c for c in range(a, b + 1) if c in cmap}
cps |= {0x200D, 0xFE0F, 0x20E3} | {c for c in range(0x1F3FB, 0x1F400) if c in cmap} | {c for c in range(0x1F1E6, 0x1F200) if c in cmap}
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
