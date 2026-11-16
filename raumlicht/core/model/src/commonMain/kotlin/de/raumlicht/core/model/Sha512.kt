package de.raumlicht.core.model

/** SHA-512 (FIPS 180-4) in reinem Kotlin – gleich auf Android und im Browser. Für die Prüfung der Pro-Codes. */
internal object Sha512 {
    private val K = longArrayOf(
        0x428a2f98d728ae22L, 0x7137449123ef65cdL, -0x4a3f043013b2c4d1L, -0x164a245a7e762444L,
        0x3956c25bf348b538L, 0x59f111f1b605d019L, -0x6dc07d5b50e6b065L, -0x54e3a12a25927ee8L,
        -0x27f855675cfcfdbeL, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
        0x72be5d74f27b896fL, -0x7f214e01c4e9694fL, -0x6423f958da38edcbL, -0x3e640e8b3096d96cL,
        -0x1b64963e610eb52eL, -0x1041b879c7b0da1dL, 0xfc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
        0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
        -0x67c1aead11992055L, -0x57ce3992d24bcdf0L, -0x4ffcd8376704dec1L, -0x40a680384110f11cL,
        -0x391ff40cc257703eL, -0x2a586eb86cf558dbL, 0x6ca6351e003826fL, 0x142929670a0e6e70L,
        0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
        0x650a73548baf63deL, 0x766a0abb3c77b2a8L, -0x7e3d36d1b812511aL, -0x6d8dd37aeb7dcac5L,
        -0x5d40175eb30efc9cL, -0x57e599b443bdcfffL, -0x3db4748f2f07686fL, -0x3893ae5cf9ab41d0L,
        -0x2e6d17e62910ade8L, -0x2966f9dbaa9a56f0L, -0xbf1ca7aa88edfd6L, 0x106aa07032bbd1b8L,
        0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
        0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
        0x748f82ee5defb2fcL, 0x78a5636f43172f60L, -0x7b3787eb5e0f548eL, -0x7338fdf7e59bc614L,
        -0x6f410005dc9ce1d8L, -0x5baf9314217d4217L, -0x41065c084d3986ebL, -0x398e870d1c8dacd5L,
        -0x35d8c13115d99e64L, -0x2e794738de3f3df9L, -0x15258229321f14e2L, -0xa82b08011912e88L,
        0x6f067aa72176fbaL, 0xa637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
        0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
        0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L,
    )
    private val H0 = longArrayOf(
        0x6a09e667f3bcc908L, -0x4498517a7b3558c5L, 0x3c6ef372fe94f82bL, -0x5ab00ac5a0e2c90fL,
        0x510e527fade682d1L, -0x64fa9773d4c193e1L, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L,
    )

    private fun rotr(x: Long, n: Int) = (x ushr n) or (x shl (64 - n))

    fun hash(msg: ByteArray): ByteArray {
        val bitLen = msg.size.toLong() * 8
        val padLen = ((msg.size + 17 + 127) / 128) * 128
        val m = ByteArray(padLen)
        msg.copyInto(m)
        m[msg.size] = 0x80.toByte()
        for (i in 0 until 8) m[padLen - 1 - i] = (bitLen ushr (8 * i)).toByte()
        val h = H0.copyOf()
        val w = LongArray(80)
        for (block in 0 until padLen / 128) {
            for (t in 0 until 16) {
                var v = 0L
                for (b in 0 until 8) v = (v shl 8) or (m[block * 128 + t * 8 + b].toLong() and 0xff)
                w[t] = v
            }
            for (t in 16 until 80) {
                val s0 = rotr(w[t - 15], 1) xor rotr(w[t - 15], 8) xor (w[t - 15] ushr 7)
                val s1 = rotr(w[t - 2], 19) xor rotr(w[t - 2], 61) xor (w[t - 2] ushr 6)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]; var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (t in 0 until 80) {
                val t1 = hh + (rotr(e, 14) xor rotr(e, 18) xor rotr(e, 41)) + ((e and f) xor (e.inv() and g)) + K[t] + w[t]
                val t2 = (rotr(a, 28) xor rotr(a, 34) xor rotr(a, 39)) + ((a and b) xor (a and c) xor (b and c))
                hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        }
        val out = ByteArray(64)
        for (i in 0 until 8) for (b in 0 until 8) out[i * 8 + b] = (h[i] ushr (56 - 8 * b)).toByte()
        return out
    }
}
