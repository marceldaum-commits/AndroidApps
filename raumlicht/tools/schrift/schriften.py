#!/usr/bin/env python3
"""Kürzt die Schriften der WebApp auf die Zeichen, die Raumlicht wirklich benutzt – ein Werkzeug für den Entwicklungsrechner.

Die volle Emoji-Schrift hat 10,8 MB; gebraucht werden rund 100 Emojis. Läuft automatisch vor jedem Bau der WebApp (Gradle-Aufgabe :web:schriften,
nur wenn sich Quelltexte geändert haben) – ein neues Emoji im Text landet so von selbst in der Schrift. Braucht: pip install fonttools.
Von Hand (aus raumlicht/):
    python tools/schrift/schriften.py            # schreibt web/src/wasmJsMain/resources/fonts/*.ttf neu
    python tools/schrift/schriften.py --pruefen  # ändert nichts; schlägt fehl, wenn ein benutztes Zeichen in den Schriften fehlt (läuft auch auf GitHub) Quellen: Noto Color Emoji (SIL OFL 1.1), DejaVu Sans (frei).
"""
import glob, os, re, sys
from fontTools.ttLib import TTFont
from fontTools import subset

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, 'web/src/wasmJsMain/resources/fonts')
# die vollständigen Schriften liegen im Repo (tools/schrift/quelle) – so entsteht überall dasselbe Ergebnis
EMOJI_SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'quelle/NotoColorEmoji.ttf')
TEXT_SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'quelle/DejaVuSans.ttf')

# alle Zeichen aus den Texten im Quellcode (Kotlin der gemeinsamen Module und der WebApp)
used = set()
for pattern in ['core/model/src/commonMain/**/*.kt', 'shared/src/commonMain/**/*.kt', 'shared/src/wasmJsMain/**/*.kt', 'web/src/wasmJsMain/**/*.kt']:
    for path in glob.glob(os.path.join(ROOT, pattern), recursive=True):
        src = open(path, encoding='utf-8').read()
        src = re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), src)
        used.update(ord(c) for c in src)
emoji_all = set(TTFont(EMOJI_SRC, lazy=True).getBestCmap())
text_all = set(TTFont(TEXT_SRC, lazy=True).getBestCmap())
special = {c for c in used if c > 0x7E}
# Emojis: alles, was die Emoji-Schrift kann und kein gewöhnliches Schriftzeichen ist; dazu Bausteine für zusammengesetzte Emojis
emoji = {c for c in special if c in emoji_all and c > 0x2000} | {0x200D, 0xFE0F, 0x20E3}
# Textschrift: Mathe- und Sonderzeichen (ₓ, →, √, ≤ …) plus Grundalphabet als Rückfall. Zahlen und Rechenzeichen können in Eingaben überall vorkommen.
text = {c for c in special if c in text_all} | set(range(0x20, 0x7F)) | set(range(0xA0, 0x180)) | set(range(0x2070, 0x209D)) | set(range(0x2190, 0x2200)) | set(range(0x2200, 0x2300)) & text_all
text &= text_all
missing = sorted(c for c in special if c not in emoji_all and c not in text_all and c not in (0x20D7, 0xFE0F, 0x200D))

def cut(src, dst, chars):
    opts = subset.Options(); opts.layout_features = ['*']; opts.name_IDs = ['*']; opts.notdef_outline = True; opts.glyph_names = False; opts.drop_tables += ['FFTM']
    font = subset.load_font(src, opts); s = subset.Subsetter(opts); s.populate(unicodes=chars); s.subset(font); subset.save_font(font, dst, opts)

if '--pruefen' in sys.argv:
    have_e = set(TTFont(os.path.join(OUT, 'noto_color_emoji.ttf'), lazy=True).getBestCmap()); have_t = set(TTFont(os.path.join(OUT, 'dejavu_sans.ttf'), lazy=True).getBestCmap())
    gaps = sorted(c for c in special if c not in (0x200D, 0xFE0F, 0x20E3) and (c in emoji and c not in have_e) or (c in text and c not in have_t and c not in have_e))
    if gaps: sys.exit('In den gekürzten Schriften fehlen: ' + ' '.join(f'{chr(c)} (U+{c:04X})' for c in gaps) + ' – tools/schrift/schriften.py neu laufen lassen')
    print(f'Schriften vollständig: {len(emoji)} Emojis, {len(text)} Textzeichen'); sys.exit(0)

os.makedirs(OUT, exist_ok=True)
cut(EMOJI_SRC, os.path.join(OUT, 'noto_color_emoji.ttf'), emoji)
cut(TEXT_SRC, os.path.join(OUT, 'dejavu_sans.ttf'), text)
for f in ('noto_color_emoji.ttf', 'dejavu_sans.ttf'): print(f, os.path.getsize(os.path.join(OUT, f)) // 1024, 'KB')
print(len(emoji), 'Emojis,', len(text), 'Textzeichen')
if missing: print('Hinweis – in keiner der beiden Schriften vorhanden (zeigt die Standardschrift oder ein Kästchen):', ' '.join(f'{chr(c)} U+{c:04X}' for c in missing))
