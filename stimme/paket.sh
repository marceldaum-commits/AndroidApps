#!/usr/bin/env bash
# Schritt 4: Ordner „stimme/“ für unseren Server zusammenstellen → stimme/build/paket/
# Inhalt danach 1:1 hochladen, z. B. nach https://rechenblitz.mathenachhilfe-online.com/stimme/ (Adresse = CONFIG.voiceBase).
set -euo pipefail
cd "$(dirname "$0")"
OUT=build/paket; ORT=1.20.1
MODEL=de_DE-thorsten-medium.onnx
HF=https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium
rm -rf "$OUT" && mkdir -p "$OUT"
# Stimm-Modell (Aufnahmen CC0, Thorsten Müller) – einmalig von Hugging Face holen, danach liegt es bei uns
curl -sSLf -o "$OUT/$MODEL" "$HF/$MODEL"
curl -sSLf -o "$OUT/MODEL_CARD.txt" "$HF/MODEL_CARD"
# Rechenwerk onnxruntime-web (MIT) – nur der WebAssembly-Teil
TMP=$(mktemp -d); (cd "$TMP" && npm pack -q onnxruntime-web@$ORT >/dev/null && tar -xzf onnxruntime-web-$ORT.tgz)
cp "$TMP/package/dist/ort.wasm.min.js" "$TMP/package/dist/ort-wasm-simd-threaded.mjs" "$TMP/package/dist/ort-wasm-simd-threaded.wasm" "$OUT/"
curl -sSLf -o "$OUT/LICENSE-onnxruntime.txt" https://raw.githubusercontent.com/microsoft/onnxruntime/v$ORT/LICENSE || true
rm -rf "$TMP"
# Lautschrift-Wörterbuch (eigene Daten, siehe lautschrift.py)
cp build/stimme-de.json "$OUT/"
SIZE=$(stat -c %s "$OUT/$MODEL")
cat > "$OUT/stimme.json" <<JSON
{ "name": "Thorsten", "model": "$MODEL", "size": $SIZE, "dict": "stimme-de.json", "ort": "ort.wasm.min.js", "mb": $(( ( $(du -cb "$OUT"/*.onnx "$OUT"/*.wasm "$OUT"/*.json "$OUT"/*.js "$OUT"/*.mjs | tail -1 | cut -f1) + 999999 ) / 1000000 )) }
JSON
ls -la "$OUT"; cat "$OUT/stimme.json"
