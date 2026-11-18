package de.experimentopia.core.model

/**
 * AES-256-GCM in reinem Kotlin (gleich auf Android, Wasm und JS – ohne asynchrone Browser-Schnittstelle).
 * Ausgabe wie WebCrypto: Geheimtext und dahinter 16 Byte Prüfwert. Geprüft gegen javax.crypto ([CryptoTest]).
 */
object AesGcm {
    private val SBOX = IntArray(256).also { s ->
        // S-Box aus dem Inversen in GF(2^8) und der affinen Abbildung
        var p = 1; var q = 1
        do {
            p = p xor (p shl 1) xor (if (p and 0x80 != 0) 0x1B else 0); p = p and 0xFF
            q = q xor (q shl 1); q = q xor (q shl 2); q = q xor (q shl 4); q = q and 0xFF; if (q and 0x80 != 0) q = q xor 0x09
            val x = q xor rotl8(q, 1) xor rotl8(q, 2) xor rotl8(q, 3) xor rotl8(q, 4)
            s[p] = (x xor 0x63) and 0xFF
        } while (p != 1)
        s[0] = 0x63
    }
    private fun rotl8(x: Int, n: Int) = ((x shl n) or (x ushr (8 - n))) and 0xFF
    private fun xt(x: Int) = ((x shl 1) xor (if (x and 0x80 != 0) 0x1B else 0)) and 0xFF

    /** Rundenschlüssel für AES-256 (15 × 16 Byte) */
    private fun expand(key: ByteArray): IntArray {
        require(key.size == 32) { "AES-256 braucht 32 Byte Schlüssel" }
        val w = IntArray(240)
        for (i in 0 until 32) w[i] = key[i].toInt() and 0xFF
        var rcon = 1
        var i = 32
        val t = IntArray(4)
        while (i < 240) {
            for (k in 0..3) t[k] = w[i - 4 + k]
            if (i % 32 == 0) {
                val a = t[0]; t[0] = SBOX[t[1]] xor rcon; t[1] = SBOX[t[2]]; t[2] = SBOX[t[3]]; t[3] = SBOX[a]
                rcon = xt(rcon)
            } else if (i % 32 == 16) for (k in 0..3) t[k] = SBOX[t[k]]
            for (k in 0..3) { w[i] = w[i - 32] xor t[k]; i++ }
        }
        return w
    }

    private fun encryptBlock(w: IntArray, input: ByteArray): ByteArray {
        val s = IntArray(16) { (input[it].toInt() and 0xFF) xor w[it] }
        val t = IntArray(16)
        for (round in 1..14) {
            for (i in 0 until 16) s[i] = SBOX[s[i]]
            // ShiftRows (spaltenweise abgelegt: Index = 4·Spalte + Zeile)
            for (c in 0..3) for (r in 0..3) t[4 * c + r] = s[4 * ((c + r) % 4) + r]
            if (round < 14) for (c in 0..3) {
                val a0 = t[4 * c]; val a1 = t[4 * c + 1]; val a2 = t[4 * c + 2]; val a3 = t[4 * c + 3]
                val all = a0 xor a1 xor a2 xor a3
                s[4 * c] = a0 xor all xor xt(a0 xor a1)
                s[4 * c + 1] = a1 xor all xor xt(a1 xor a2)
                s[4 * c + 2] = a2 xor all xor xt(a2 xor a3)
                s[4 * c + 3] = a3 xor all xor xt(a3 xor a0)
            } else t.copyInto(s)
            for (i in 0 until 16) s[i] = s[i] xor w[16 * round + i]
        }
        return ByteArray(16) { s[it].toByte() }
    }

    /** Multiplikation in GF(2^128) nach GCM-Norm (Bit 0 = höchstes Bit des ersten Bytes) */
    private fun gmul(x: ByteArray, y: ByteArray): ByteArray {
        val z = ByteArray(16); val v = y.copyOf()
        for (i in 0 until 128) {
            if ((x[i / 8].toInt() shr (7 - i % 8)) and 1 == 1) for (k in 0 until 16) z[k] = (z[k].toInt() xor v[k].toInt()).toByte()
            val lsb = v[15].toInt() and 1
            for (k in 15 downTo 1) v[k] = (((v[k].toInt() and 0xFF) ushr 1) or ((v[k - 1].toInt() and 1) shl 7)).toByte()
            v[0] = ((v[0].toInt() and 0xFF) ushr 1).toByte()
            if (lsb == 1) v[0] = (v[0].toInt() xor 0xE1).toByte()
        }
        return z
    }

    /** Verschlüsseln: [iv] 12 Byte, Ergebnis = Geheimtext ‖ Prüfwert (16 Byte) */
    fun encrypt(key: ByteArray, iv: ByteArray, plain: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
        require(iv.size == 12) { "GCM hier nur mit 12-Byte-IV" }
        val w = expand(key)
        val h = encryptBlock(w, ByteArray(16))
        val j0 = ByteArray(16).also { iv.copyInto(it); it[15] = 1 }
        val out = ByteArray(plain.size + 16)
        val ctr = j0.copyOf()
        var off = 0
        while (off < plain.size) {
            for (k in 15 downTo 12) { ctr[k] = (ctr[k] + 1).toByte(); if (ctr[k].toInt() != 0) break }
            val ks = encryptBlock(w, ctr)
            for (k in 0 until minOf(16, plain.size - off)) out[off + k] = (plain[off + k].toInt() xor ks[k].toInt()).toByte()
            off += 16
        }
        // GHASH über AAD, Geheimtext und Längen
        var y = ByteArray(16)
        fun absorb(data: ByteArray, n: Int) {
            var o = 0
            while (o < n) { val blk = ByteArray(16); for (k in 0 until minOf(16, n - o)) blk[k] = data[o + k]; for (k in 0 until 16) blk[k] = (blk[k].toInt() xor y[k].toInt()).toByte(); y = gmul(blk, h); o += 16 }
        }
        absorb(aad, aad.size); absorb(out, plain.size)
        val lens = ByteArray(16)
        val aBits = aad.size.toLong() * 8; val cBits = plain.size.toLong() * 8
        for (k in 0..7) { lens[7 - k] = (aBits ushr (8 * k)).toByte(); lens[15 - k] = (cBits ushr (8 * k)).toByte() }
        absorb(lens, 16)
        val ek = encryptBlock(w, j0)
        for (k in 0 until 16) out[plain.size + k] = (y[k].toInt() xor ek[k].toInt()).toByte()
        return out
    }
}

/** Base64 für Adressen (RFC 4648 §5): - und _ statt + und /, ohne „=“ am Ende */
object B64u {
    private const val A = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    fun encode(b: ByteArray): String = buildString {
        var i = 0
        while (i < b.size) {
            val n = minOf(3, b.size - i)
            var v = 0
            for (k in 0 until 3) v = (v shl 8) or (if (k < n) b[i + k].toInt() and 0xFF else 0)
            for (k in 0..n) append(A[(v shr (18 - 6 * k)) and 63])
            i += 3
        }
    }
    fun decode(s: String): ByteArray? {
        val out = ArrayList<Byte>(); var v = 0; var bits = 0
        for (ch in s) { val d = A.indexOf(ch); if (d < 0) return null; v = (v shl 6) or d; bits += 6; if (bits >= 8) { bits -= 8; out.add((v shr bits).toByte()) } }
        return out.toByteArray()
    }
}
