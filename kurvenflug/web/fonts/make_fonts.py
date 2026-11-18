#!/usr/bin/env python3
"""
Baut die Schriften der WebApp neu (nur nötig, wenn neue Emojis oder Sonderzeichen in die App kommen –
der Gradle-Check `checkWebFonts` meldet das).

  pip install fonttools brotli && python3 web/fonts/make_fonts.py

Quellen (frei, aus dem npm-Verzeichnis):
  - Noto Color Emoji (@fontsource/noto-color-emoji), SIL Open Font License 1.1
  - DejaVu Sans (dejavu-fonts-ttf), Bitstream-Vera-/DejaVu-Lizenz (frei)
Die Schriften werden auf genau die benutzten Zeichen zugeschnitten.
"""
import glob, io, json, os, sys, tarfile, urllib.request
from fontTools.ttLib import TTFont
from fontTools import subset

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))   # kurvenflug/
OUT = os.path.join(ROOT, 'web/src/commonMain/composeResources/files')
HERE = os.path.dirname(os.path.abspath(__file__))

def npm(pkg):
    meta = json.load(urllib.request.urlopen(f'https://registry.npmjs.org/{pkg}/latest'))
    return tarfile.open(fileobj=io.BytesIO(urllib.request.urlopen(meta['dist']['tarball']).read()))

def used_chars():
    cps = set()
    for f in glob.glob(os.path.join(ROOT, '**/src/*Main/**/*.kt'), recursive=True) + glob.glob(os.path.join(ROOT, 'app/src/main/**/*.kt'), recursive=True):
        for ch in open(f, encoding='utf-8').read():
            if ord(ch) > 0xFF: cps.add(ord(ch))
    return sorted(cps)

def cut(font, cps, out):
    o = subset.Options(); o.layout_features = ['*']; o.drop_tables += ['SVG ']; o.notdef_outline = True
    s = subset.Subsetter(o); s.populate(unicodes=cps); s.subset(font)
    font.flavor = None; font.save(out)
    print(out, os.path.getsize(out), 'Bytes')

cps = used_chars()
def texty(c):
    return (0x2B0 <= c <= 0x2FF or 0x2070 <= c <= 0x209F or 0x2190 <= c <= 0x22FF or c in (0x2713, 0x2717, 0x2605, 0x2606, 0x25CF, 0x221A))
dejavu_tar = npm('dejavu-fonts-ttf')
dv = TTFont(dejavu_tar.extractfile('package/ttf/DejaVuSans.ttf'))
dcm = dv.getBestCmap()
emoji_tar = npm('@fontsource/noto-color-emoji')
emoji = TTFont(emoji_tar.extractfile('package/files/noto-color-emoji-emoji-400-normal.woff2'))
ecm = emoji.getBestCmap()
in_emoji = [c for c in cps if c in ecm]
cut(emoji, [c for c in in_emoji if not (texty(c) and c in dcm)] + [0xFE0F, 0x200D, 0x20], os.path.join(OUT, 'emoji.ttf'))

# Schlichte Zeichen (Pfeile, Mathe, Haken, Sterne, Hochzahlen) als Text – sonst zeigt die Emoji-Schrift bunte Kästchen
def texty(c):
    return (0x2B0 <= c <= 0x2FF or 0x2070 <= c <= 0x209F or 0x2190 <= c <= 0x22FF or c in (0x2713, 0x2717, 0x2605, 0x2606, 0x25CF, 0x221A))
rest = [c for c in cps if c in dcm and (c not in ecm or texty(c))]
cut(dv, rest + [0x20], os.path.join(OUT, 'symbols.ttf'))
missing = [c for c in cps if c not in ecm and c not in dcm]
if missing: print('Ohne Schrift (bitte im Code ersetzen):', ''.join(map(chr, missing)), file=sys.stderr)

open(os.path.join(HERE, 'chars.txt'), 'w', encoding='utf-8').write(''.join(map(chr, cps)) + '\n')
lic = os.path.join(ROOT, 'web/src/webMain/resources/fonts-lizenzen.txt')
with open(lic, 'w', encoding='utf-8') as f:
    f.write('Schriften in Kurvenflug (Web)\n\nNoto Color Emoji – SIL Open Font License 1.1\n\n')
    f.write(emoji_tar.extractfile('package/LICENSE').read().decode('utf-8'))
    f.write('\n\n----------------------------------------\nDejaVu Sans\n\n')
    f.write(dejavu_tar.extractfile('package/LICENSE').read().decode('utf-8'))
