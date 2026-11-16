#!/usr/bin/env python3
"""
Kurvenflug Pro – Codes erzeugen (für den Lizenz-Dienst / WooCommerce-Anbindung).

  python3 pro_code.py keygen GEHEIM.key           # neues Schlüsselpaar; GEHEIM.key NIE ins Repo, ab nach Bitwarden
  python3 pro_code.py pro    GEHEIM.key 1001      # Pro-Code Nr. 1001 (Einmalkauf)
  python3 pro_code.py school GEHEIM.key 2001 2027-07-31   # Schullizenz bis einschließlich 31.7.2027: eine Lehrkraft +
                                                          # alle Schüler ihrer Klassen, Unterricht und zu Hause, beliebig viele Geräte

Ausgabe: der Code und der Freischalt-Link für die E-Mail. Den öffentlichen Schlüssel (64 Hex-Zeichen) trägt man in
kurvenflug/pro/pro.properties ein. Format siehe core/model/.../License.kt. Braucht: pip install cryptography
"""
import sys, datetime
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization

APP_KURVENFLUG = 1
ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
DAY0 = datetime.date(2020, 1, 1)
LINK = "https://kurvenflug.mathenachhilfe-online.com/?pro="


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
    return "KF-" + "-".join(s[i:i + 5] for i in range(0, len(s), 5))


def load(path: str) -> Ed25519PrivateKey:
    return Ed25519PrivateKey.from_private_bytes(bytes.fromhex(open(path).read().strip()))


def public_hex(k: Ed25519PrivateKey) -> str:
    return k.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw).hex()


def make(k: Ed25519PrivateKey, product: int, serial: int, day: datetime.date, app: int = APP_KURVENFLUG) -> str:
    d = (day - DAY0).days
    payload = bytes([1, app, product]) + serial.to_bytes(4, "big") + d.to_bytes(2, "big")
    return b32(payload + k.sign(payload))


def main(a):
    if len(a) >= 2 and a[0] == "keygen":
        k = Ed25519PrivateKey.generate()
        raw = k.private_bytes(serialization.Encoding.Raw, serialization.PrivateFormat.Raw, serialization.NoEncryption())
        open(a[1], "w").write(raw.hex() + "\n")
        print("Geheimer Schlüssel gespeichert in", a[1], "(nicht ins Repo!)")
        print("Öffentlicher Schlüssel für pro.properties:", public_hex(k))
    elif len(a) >= 3 and a[0] == "pro":
        c = make(load(a[1]), 1, int(a[2]), datetime.date.today())
        print(c); print(LINK + c.replace("-", ""))
    elif len(a) >= 4 and a[0] == "school":
        c = make(load(a[1]), 2, int(a[2]), datetime.date.fromisoformat(a[3]))
        print(c); print(LINK + c.replace("-", ""))
    else:
        print(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
