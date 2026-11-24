package de.kurvenflug.core.model

import kotlin.test.*

/** Läuft auf JVM und im Browser (Wasm): gleiche Prüfung überall. Schlüssel nur für Tests (Seed 0…31). */
class LicenseTest {
    private val pk = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val pro = "KF-040G2-0000F-MGKKV-QD10E-XXXJN-05Q3B-DPG8M-0VWZR-KNQ5R-DR4ND-G4XD5-BKQZV-8V162-V9YKD-2DF1D-3MTYY-ZRFHM-74P0G-PVYQF-V8ZE0-CQJ6C-SNV78-NR1SN-GG"
    private val school = "KF-040G4-0000Z-8GNM1-A9EZ4-AXTAB-MCAXS-SWFCE-D6SX4-91C0F-EZ3XX-35AQ9-3B0XQ-KQGCF-G88TC-K8MG9-KH2TA-HAJ9D-PSNK5-X1WTF-S8CMN-PV7XH-ZXEC6-VPASZ-0M"
    private val otherApp = "KF-04102-00000-2GKKZ-EZRJ5-47T54-4PF62-V69GZ-KZJMN-M8YBV-23D36-FMS0H-7323D-K1A56-61FG8-27JSP-FZYGS-ZD2N4-VE9YP-N7PRK-Q2B56-3TT1Y-KV6TP-T9NA2-0R"
    private val today = CalDate.of(2026, 11, 16).toEpochDay()

    @Test fun sha512KnownAnswer() {
        fun hex(b: ByteArray) = b.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hex(Sha512.hash("abc".encodeToByteArray())))
        assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            hex(Sha512.hash(ByteArray(0))))
    }

    @Test fun validProCode() {
        val i = License.check(pro, pk, today)
        assertNotNull(i); assertEquals(License.PRODUCT_PRO, i.product); assertEquals(1001L, i.serial); assertNull(i.until)
    }

    @Test fun codeFoundInsideEmailOrLink() {
        val mail = "Hallo! Dein Code lautet:\n$pro\nViel Spaß!"
        assertNotNull(License.check(License.extract(mail), pk, today))
        val link = "https://lernen.mathenachhilfe-online.com/kurvenflug/?pro=" + pro.replace("-", "")
        assertNotNull(License.check(License.extract(link), pk, today))
        // Kleinbuchstaben und Verwechsler O/0, I/1 beim Abtippen
        assertNotNull(License.check(pro.lowercase(), pk, today))
    }

    @Test fun schoolLicenseExpires() {
        val i = License.check(school, pk, today)
        assertNotNull(i); assertEquals(License.PRODUCT_SCHOOL, i.product)
        assertEquals(CalDate.of(2027, 7, 31).toEpochDay(), i.until)
        assertNotNull(License.check(school, pk, CalDate.of(2027, 7, 31).toEpochDay()))
        assertNull(License.check(school, pk, CalDate.of(2027, 8, 1).toEpochDay()))
    }

    @Test fun forgedOrForeignCodesFail() {
        // ein Zeichen verändert
        val bad = pro.substring(0, 20) + (if (pro[20] == 'A') 'B' else 'A') + pro.substring(21)
        assertNull(License.check(bad, pk, today))
        // anderer Schlüssel
        assertNull(License.check(pro, "1" + pk.drop(1), today))
        // Code einer anderen App
        assertNull(License.check(otherApp, pk, today))
        // kein Schlüssel eingetragen → nie gültig
        assertNull(License.check(pro, "", today))
        assertNull(License.check("KF-12345", pk, today))
        assertNull(License.check(null, pk, today))
    }

    @Test fun encodeDecodeRoundTrip() {
        val bytes = ByteArray(73) { (it * 37 + 5).toByte() }
        assertContentEquals(bytes, License.decode(License.encode(bytes)))
    }
}
