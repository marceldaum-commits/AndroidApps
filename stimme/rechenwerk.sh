#!/usr/bin/env bash
# Rechenwerk (onnxruntime-web, MIT) und Lautschrift-Wörterbuch in die Web-App legen → kopfrechnen/stimme/
# (Das Stimm-Modell selbst liegt auf dem gemeinsamen Stimmen-Server, siehe README.md.)
set -euo pipefail
cd "$(dirname "$0")"
ORT=1.20.1; OUT=../kopfrechnen/stimme
mkdir -p "$OUT"
TMP=$(mktemp -d); (cd "$TMP" && npm pack -q onnxruntime-web@$ORT >/dev/null && tar -xzf onnxruntime-web-$ORT.tgz)
cp "$TMP/package/dist/ort.wasm.min.js" "$TMP/package/dist/ort-wasm-simd-threaded.mjs" "$TMP/package/dist/ort-wasm-simd-threaded.wasm" "$OUT/"
curl -sSLf -o "$OUT/LICENSE-onnxruntime.txt" https://raw.githubusercontent.com/microsoft/onnxruntime/v$ORT/LICENSE
rm -rf "$TMP"
cp build/stimme-de.json "$OUT/"
ls -la "$OUT"
