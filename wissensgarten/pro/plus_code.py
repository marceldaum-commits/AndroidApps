#!/usr/bin/env python3
"""
Wissensgarten Plus – Codes erzeugen (für den Lizenz-Dienst / WooCommerce-Anbindung). Gemeinsames Format aller Lern-Apps.

  python3 plus_code.py jahr  GEHEIM.key 1001 [2026-11-16]   # Plus Jahr Nr. 1001, ab Kaufdatum 365 Tage gültig
  python3 plus_code.py immer GEHEIM.key 1002                # Plus für immer

GEHEIM.key enthält den geheimen Ed25519-Schlüssel als Hex – NIE ins Repo (nur Bitwarden / Lizenz-Dienst).
Ausgabe: Code und Freischalt-Link. Format siehe core/model/.../License.kt. Braucht: pip install cryptography
"""
import sys, datetime
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

APP_WISSENSGARTEN = 3
PRODUCT_JAHR, PRODUCT_IMMER = 1, 3
ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
DAY0 = datetime.date(2020, 1, 1)
LINK = "https://wissensgarten.mathenachhilfe-online.com/?pro="


def b32(data: bytes) -> str:
    bits, buf, out = 0, 0, []
    for b in data:
        buf = (buf << 8) | b; bits += 8
        while bits >= 5:
            out.append(ALPHABET[(buf >> (bits - 5)) & 31]); bits -= 5
        buf &= (1 << bits) - 1
    if bits:
        out.append(ALPHABET[(buf << (5 - bits)) & 31])
    s = "".join(out)
    return "WG-" + "-".join(s[i:i + 5] for i in range(0, len(s), 5))


def make(k: Ed25519PrivateKey, product: int, serial: int, day: datetime.date, app: int = APP_WISSENSGARTEN) -> str:
    d = (day - DAY0).days
    payload = bytes([1, app, product]) + serial.to_bytes(4, "big") + d.to_bytes(2, "big")
    return b32(payload + k.sign(payload))


def main(a):
    if len(a) >= 3 and a[0] in ("jahr", "immer"):
        k = Ed25519PrivateKey.from_private_bytes(bytes.fromhex(open(a[1]).read().strip()))
        day = datetime.date.fromisoformat(a[3]) if len(a) >= 4 else datetime.date.today()
        c = make(k, PRODUCT_JAHR if a[0] == "jahr" else PRODUCT_IMMER, int(a[2]), day)
        print(c); print(LINK + c.replace("-", ""))
    else:
        print(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
