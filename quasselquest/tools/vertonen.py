#!/usr/bin/env python3
"""Vertont alle festen englischen Sätze mit freien Piper-Stimmen (vorab, offline nutzbar).

Aufruf (Piper in einer eigenen Umgebung):
    python3 -m venv /tmp/piper && /tmp/piper/bin/pip install piper-tts
    /tmp/piper/bin/python tools/vertonen.py --stimmen /pfad/zu/den/onnx-dateien

Ergebnis:
    webapp/audio/piper/<hash>.mp3   (normal und langsam)
    webapp/audio/index.json         (Satz → Dateien; die App liest nur diese Liste)
    webapp/vorlesen/texte.json      (alle Sätze im Format des Stimm-Servers – für die spätere Nova-Vertonung)

Nur Stimmen mit geprüfter Lizenz (siehe ../LIZENZEN.md). Stimmen, die auf „lessac“ aufbauen, sind ausgeschlossen
(Daten nur für nicht-kommerzielle Forschung).
Bereits vorhandene Dateien werden nicht neu erzeugt; nicht mehr gebrauchte werden gelöscht.
"""
import argparse, hashlib, json, os, subprocess, sys, tempfile, unicodedata, re, wave

HIER = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.normpath(os.path.join(HIER, '..', 'webapp'))

# Figur → Stimme. length_scale > 1 = langsamer.
# tonhoehe: Faktor für die Tonhöhe (rubberband, Formanten bleiben), 1 = unverändert.
# Gewählt nach Hörtest mit Whisper (tools/hoertest.py): cori-medium spricht einzelne Wörter deutlich besser als cori-high;
# alba (schottisch) ließ „boat“ wie „boot“ klingen; norman war für einzelne Wörter kaum verständlich;
# Mia stärker als 1.08 höher gestimmt wurde undeutlich.
STIMMEN = {
    'ruby': {'modell': 'en_GB-cori-medium', 'tempo': 1.05, 'tonhoehe': 1.0},
    'mia':  {'modell': 'en_GB-cori-medium', 'tempo': 1.0, 'tonhoehe': 1.08},
    'sam':  {'modell': 'en_US-john-medium', 'tempo': 1.0, 'tonhoehe': 1.12},
    'ted':  {'modell': 'en_US-john-medium', 'tempo': 1.15, 'tonhoehe': 0.92},
    # Sprachpakete (Lizenzen geprüft 19.11.2026, siehe LIZENZEN.md); Latein: keine freie Stimme → wird nicht vorgelesen
    'de':   {'modell': 'de_DE-thorsten-medium', 'tempo': 1.0, 'tonhoehe': 1.0, 'sprache': 'de'},
    'fr':   {'modell': 'fr_FR-siwis-medium', 'tempo': 1.0, 'tonhoehe': 1.0, 'sprache': 'fr'},
    'es':   {'modell': 'es_ES-davefx-medium', 'tempo': 1.0, 'tonhoehe': 1.0, 'sprache': 'es'},
}
EINZELWORT = 1.15   # einzelne Wörter etwas langsamer – kurze Wörter werden sonst leicht verschluckt
LANGSAM = 1.5   # Faktor für die Schildkröte 🐢


def norm(text):
    return re.sub(r'\s+', ' ', unicodedata.normalize('NFC', text)).strip()


def datei(modell, tempo, ton, text):
    return hashlib.sha256(f'piper|{modell}|{tempo}|{ton}|{norm(text)}'.encode('utf-8')).hexdigest()[:20] + '.mp3'


def nova_hash(text, sprache='en'):
    """Gleiche Kennung wie der Wissensgarten (Nova.hash): sha256("nova|<sprache>|<text>"), die ersten 32 Zeichen."""
    return hashlib.sha256(f'nova|{sprache}|{norm(text)}'.encode('utf-8')).hexdigest()[:32]


def saetze():
    js = ("const C=require(process.argv[1]);const P=require(process.argv[2]);const fs=require('fs');const d=process.argv[3];"
          "const idx=JSON.parse(fs.readFileSync(d+'index.json'));"
          "const packs=idx.pakete.map(m=>Object.assign(JSON.parse(fs.readFileSync(d+m.id+'.json')),{lang:m.sprache}));"
          "const lp=P.LERNPFAD.map(l=>JSON.parse(fs.readFileSync(d+'../lernpfad/'+l+'.json')));"
          "const lpPacks=lp.flatMap(x=>x.abschnitte.flatMap(a=>a.einheiten.map(e=>P.lernPack(e,x.sprache))));"
          "process.stdout.write(JSON.stringify(C.allSpeech().concat(P.speech(packs),P.speech(lpPacks),...lp.map(P.lernSpeech))))")
    out = subprocess.check_output(['node', '-e', js, os.path.join(WEB, 'content.js'), os.path.join(WEB, 'pakete.js'),
                                   os.path.join(WEB, 'pakete') + os.sep])
    return json.loads(out)


EMOJI = re.compile('[\U0001F000-\U0001FAFF\u2600-\u27BF\uFE0F]')


def sprechtext(text):
    """Was die Stimme liest: ohne Emojis (sonst liest sie „grinning face“)."""
    return re.sub(r'\s+', ' ', EMOJI.sub('', text)).strip()


def sprich(voice, text, tempo, ton, ziel):
    with tempfile.NamedTemporaryFile(suffix='.wav') as w:
        with wave.open(w.name, 'wb') as wf:
            voice.synthesize_wav(sprechtext(text), wf, syn_config=SynCfg(length_scale=tempo))
        filt = ('rubberband=pitch=%s:formant=preserved,' % ton) if ton != 1.0 else ''
        # leise Ränder kürzen, Lautstärke angleichen, mono 22 kHz, MP3 40 kbit/s
        subprocess.check_call(['ffmpeg', '-v', 'error', '-y', '-i', w.name,
                               '-af', filt + 'silenceremove=start_periods=1:start_threshold=-50dB,areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,loudnorm=I=-18:TP=-2,apad=pad_dur=0.12',
                               '-ac', '1', '-ar', '22050', '-c:a', 'libmp3lame', '-b:a', '40k', ziel])


# ---------------------------------------------------------------- Hörprüfung (Whisper, nur beim Bauen)
ZIFFERN = {str(i): w for i, w in enumerate('zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty'.split())}
# Gleich klingende Wörter zählen als richtig erkannt (die Erkennung kann sie nicht unterscheiden)
GLEICHKLANG = {'two': {'to', 'too'}, 'four': {'for'}, 'eight': {'ate'}, 'one': {'won'}, 'meet': {'meat'}, 'see': {'sea', 'c'},
               'red': {'read'}, 'colour': {'color'}, 'hi': {'high'}, 'i': {'eye', 'aye'}, 'here': {'hear'}, 'you': {'u'},
               'wow': {'whoa', 'woah'}, 'bye': {'by', 'buy'}, 'hmm': {'hm', 'mm', 'mmm'}, "you're": {'your'}, 'ha': {'hah', 'huh'}}


def woerter(t):
    t = t.lower().replace('’', "'")
    t = unicodedata.normalize('NFD', t)
    t = ''.join(ch for ch in t if unicodedata.category(ch) != 'Mn')   # Akzente egal (é = e)
    return [ZIFFERN.get(w, w) for w in re.sub(r"[^a-z0-9ß' ]", ' ', t).split()]


def treffer(soll, ist):
    iw = woerter(ist); ist_s = set(iw); w = woerter(sprechtext(soll))
    if not w: return 1.0
    if ''.join(w) == ''.join(iw): return 1.0          # zusammengesetzte Wörter: „Ausrufe Satz“ = „Ausrufesatz“
    return sum(1 for x in w if x in ist_s or (GLEICHKLANG.get(x, set()) & ist_s)) / len(w)


class Pruefer:
    def __init__(self):
        from faster_whisper import WhisperModel
        import numpy as np
        self.np = np; self.W = WhisperModel; self.m = {}

    def hoere(self, mp3, sprache='en'):
        name = 'base.en' if sprache == 'en' else 'base'
        if name not in self.m:
            self.m[name] = self.W(name, device='cpu', compute_type='int8')
        raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', mp3, '-f', 's16le', '-ac', '1', '-ar', '16000', '-'])
        a = self.np.frombuffer(raw, self.np.int16).astype(self.np.float32) / 32768.0
        return ' '.join(x.text for x in self.m[name].transcribe(a, language=sprache, beam_size=1)[0]).strip()


def varianten(text, basis):
    """Reihenfolge der Versuche: wie geschrieben, etwas langsamer, mit Satzzeichen (bei Einzelwörtern), noch langsamer."""
    einzel = ' ' not in text.strip()
    v = [(text, basis), (text, basis * 1.15)]
    if einzel:
        w = C_ZEICHEN(text)
        v += [(w + '.', basis * 1.15), (w[:1].upper() + w[1:] + '!', basis * 1.3), (w + '.', basis * 1.4)]
    v.append((text, basis * 1.3))
    return [(t, round(x, 3)) for t, x in v]


def C_ZEICHEN(t):
    return t.rstrip('.!?')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--stimmen', required=True, help='Ordner mit <modell>.onnx und <modell>.onnx.json')
    ap.add_argument('--ohne-pruefung', action='store_true', help='keine Hörprüfung (schneller, nur zum Ausprobieren)')
    args = ap.parse_args()
    from piper import PiperVoice, SynthesisConfig as SynCfg  # noqa: E402

    ordner = os.path.join(WEB, 'audio', 'piper'); os.makedirs(ordner, exist_ok=True)
    alt_pfad = os.path.join(WEB, 'audio', 'index.json')
    alt = json.load(open(alt_pfad, encoding='utf-8')).get('saetze', {}) if os.path.exists(alt_pfad) else {}
    pruefer = None if args.ohne_pruefung else Pruefer()
    stimmen = {}
    index = {'format': 1, 'stimmen': {w: s['modell'] for w, s in STIMMEN.items()}, 'saetze': {}}
    bericht = []; gebraucht = set(); neu = 0
    liste = saetze()
    for i, s in enumerate(liste):
        st = STIMMEN[s['who']]; key = s['who'] + '|' + norm(s['text'])
        if st['modell'] not in stimmen:
            stimmen[st['modell']] = PiperVoice.load(os.path.join(args.stimmen, st['modell'] + '.onnx'))
        vorher = alt.get(key)
        if vorher and vorher.get('stimme') == st['modell'] and vorher.get('treffer', 1) >= 0.99 and all(os.path.exists(os.path.join(ordner, vorher[a])) for a in ('n', 's')):
            index['saetze'][key] = vorher; gebraucht.update([vorher['n'], vorher['s']]); continue
        basis = st['tempo'] * (EINZELWORT if ' ' not in s['text'].strip() else 1.0)
        bestes = None
        for form, tempo in varianten(s['text'], basis):
            f = datei(st['modell'], tempo, st['tonhoehe'], form)
            p = os.path.join(ordner, f)
            if not os.path.exists(p):
                sprich(stimmen[st['modell']], form, tempo, st['tonhoehe'], p); neu += 1
            gehört = pruefer.hoere(p, st.get('sprache', 'en')) if pruefer else ''
            q = treffer(s['text'], gehört) if pruefer else 1.0
            if bestes is None or q > bestes[0]:
                bestes = (q, form, tempo, f, gehört)
            if q >= 0.99:
                break
        q, form, tempo, f, gehört = bestes
        fs = datei(st['modell'], round(tempo * LANGSAM, 3), st['tonhoehe'], form)
        if not os.path.exists(os.path.join(ordner, fs)):
            sprich(stimmen[st['modell']], form, round(tempo * LANGSAM, 3), st['tonhoehe'], os.path.join(ordner, fs)); neu += 1
        index['saetze'][key] = {'n': f, 's': fs, 'stimme': st['modell'], 'gehoert': gehört, 'treffer': round(q, 2)}
        gebraucht.update([f, fs])
        if (i + 1) % 25 == 0:
            print(f'{i + 1}/{len(liste)} …', file=sys.stderr)
    for f in os.listdir(ordner):
        if f.endswith('.mp3') and f not in gebraucht:
            os.remove(os.path.join(ordner, f))
    with open(alt_pfad, 'w', encoding='utf-8') as fh:
        json.dump(index, fh, ensure_ascii=False, separators=(',', ':'), sort_keys=True)
    # Für die spätere Nova-Vertonung (erst nach Marcels Go): gleiches Format wie wissensgarten/webapp/vorlesen/texte.json
    os.makedirs(os.path.join(WEB, 'vorlesen'), exist_ok=True)
    texte = sorted({(STIMMEN[s['who']].get('sprache', 'en'), norm(sprechtext(s['text']))) for s in liste})
    with open(os.path.join(WEB, 'vorlesen', 'texte.json'), 'w', encoding='utf-8') as fh:
        json.dump({'format': 1, 'app': 'quasselquest', 'texte': [{'text': t, 'sprache': l, 'nova': nova_hash(t, l)} for l, t in texte]},
                  fh, ensure_ascii=False, indent=0)
    unklar = sorted(((v['treffer'], k, v['gehoert']) for k, v in index['saetze'].items() if v['treffer'] < 0.99))
    with open(os.path.join(HIER, '..', 'HOERPRUEFUNG.md'), 'w', encoding='utf-8') as fh:
        fh.write('# Hörprüfung der Piper-Vertonung (automatisch, Whisper base.en)\n\n')
        fh.write(f'{len(index["saetze"])} Sätze, davon {len(unklar)} nicht eindeutig erkannt (Gleichklänge wie two/to zählen als erkannt).\n')
        fh.write('Einzelwörter erkennt die Prüfung schlechter als ein Mensch; die Liste ist zum Nachhören gedacht.\n\n')
        fh.write('| Treffer | Figur | Satz | Gehört |\n|---|---|---|---|\n')
        for q, k, g in unklar:
            who, t = k.split('|', 1)
            fh.write(f'| {q:.2f} | {who} | {t} | {g} |\n')
    groesse = sum(os.path.getsize(os.path.join(ordner, f)) for f in gebraucht)
    print(f'{len(liste)} Sätze, {len(gebraucht)} Dateien ({neu} neu), {groesse / 1e6:.1f} MB, {len(unklar)} nicht eindeutig')
