import json, sys, re, os, subprocess
import numpy as np
def load(f):
    raw = subprocess.check_output(['ffmpeg','-v','error','-i',f,'-f','s16le','-ac','1','-ar','16000','-'])
    return np.frombuffer(raw, np.int16).astype(np.float32) / 32768.0
from faster_whisper import WhisperModel
web = sys.argv[1]
idx = json.load(open(os.path.join(web, 'audio/index.json')))
m = WhisperModel('base.en', device='cpu', compute_type='int8')
digits = {str(i): w for i, w in enumerate('zero one two three four five six seven eight nine ten'.split())}
def norm(s):
    s = s.lower().replace("’", "'")
    ws = re.sub(r"[^a-z0-9' ]", ' ', s).split()
    return [digits.get(w, w) for w in ws]
bad = []; n = 0
for key, e in idx['saetze'].items():
    who, text = key.split('|', 1)
    for art in ('n',):
        segs, _ = m.transcribe(load(os.path.join(web, 'audio/piper', e[art])), language='en', beam_size=1, vad_filter=False)
        got = ' '.join(s.text for s in segs)
        want = norm(text); have = set(norm(got))
        hit = sum(1 for w in want if w in have) / max(1, len(want)); n += 1
        if hit < 0.75: bad.append((who, text, got.strip(), round(hit, 2)))
print(f'{n} Dateien geprüft, {len(bad)} auffällig')
for b in bad: print(' ', b)
