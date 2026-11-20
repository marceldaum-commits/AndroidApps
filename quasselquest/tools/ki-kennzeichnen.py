#!/usr/bin/env python3
"""KI-VO Art. 50 Abs. 2: kennzeichnet alle vorhandenen Audiodateien maschinenlesbar als KI-generiert (ID3-Kommentar),
ohne den Ton neu zu kodieren (ffmpeg -c copy). Bereits gekennzeichnete Dateien werden übersprungen.
Aufruf: python3 tools/ki-kennzeichnen.py"""
import os, subprocess, sys
ORDNER = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'webapp', 'audio', 'piper')
KOMMENTAR = 'KI-generiert (synthetische Stimme, Piper TTS)'
neu = alt = 0
for f in sorted(os.listdir(ORDNER)):
    if not f.endswith('.mp3'): continue
    p = os.path.join(ORDNER, f)
    tags = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format_tags=comment', '-of', 'default=nw=1:nk=1', p], capture_output=True, text=True).stdout
    if 'KI-generiert' in tags: alt += 1; continue
    tmp = p + '.tmp.mp3'
    subprocess.check_call(['ffmpeg', '-v', 'error', '-y', '-i', p, '-map_metadata', '-1', '-c', 'copy', '-id3v2_version', '3',
                           '-metadata', 'comment=' + KOMMENTAR, '-metadata', 'encoded_by=Quasselquest (KI-generierte Stimme)', tmp])
    os.replace(tmp, p); neu += 1
print(f'{neu} gekennzeichnet, {alt} waren es schon')
