package de.wissensgarten.core.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

/** Schlüssel nur für Tests (Seed 0…31); Codes erzeugt mit pro/plus_code.py. */
class LicenseTest {
    private val pk = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val year = "WG-041G2-0000F-MGKKZ-X11SR-53KYB-JN7RV-BFHF0-HKM5S-EGK3C-T9N7D-711Y9-7111X-SZD9W-05KGS-WKTH4-HK7N0-P5PJ3-BD3TB-BBDQ9-CQNKK-65JZV-NFXKH-Y6JR1-0P"
    private val forever = "WG-041G6-0000F-N0KKY-KAY2H-BSPZ4-APHE6-0KYWH-X60XC-QWN2Y-620E9-31QB0-A9H5E-9PZRM-ZSR85-0Z9TR-XZ754-HCPMX-GT3XF-ESVJQ-HZSDY-C1Z3Q-W9G7Y-9DSSK-0E"
    private val oldYear = "WG-041G2-0000F-NGGMV-9YJ3C-1FGF3-DRVGB-15ZD4-04AH5-HEAAS-4W5QG-ME6GH-5K2CE-7KYWK-RANB9-K59BK-J8GNT-5QTET-9NZ33-6CTTC-6HB6D-E4W2W-H2KCZ-QZV0Z-06"
    private val otherApp = "WG-040G2-0000F-P0KKV-NMENF-6PHWV-Y1RNW-1E1MK-51CVR-CZBX1-BCAHY-V85ZX-BXXM1-TFJ4K-HZFEK-CJSAS-A3TGN-CW0R7-HZY99-GZ5EK-31MZG-04ZHF-XHR3H-VN5B2-GE"
    private fun day(y: Int, m: Int, d: Int) = LocalDate(y, m, d).toEpochDays().toLong()
    private val today = day(2026, 11, 16)

    @Test fun sha512KnownAnswer() {
        fun hex(b: ByteArray) = b.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hex(Sha512.hash("abc".encodeToByteArray())))
    }

    @Test fun yearPlusIsValidFor365Days() {
        val i = License.check(year, pk, today)!!
        assertEquals(License.PRODUCT_YEAR, i.product); assertEquals(1001L, i.serial)
        assertEquals(day(2027, 11, 15), i.until)
        assertNotNull(License.check(year, pk, day(2027, 11, 15)))
        assertNull(License.check(year, pk, day(2027, 11, 16)))
        assertNull("abgelaufenes Jahres-Plus", License.check(oldYear, pk, today))
        assertNotNull("Signatur bleibt echt", License.parse(oldYear, pk))
    }

    @Test fun foreverNeverExpires() {
        val i = License.check(forever, pk, day(2040, 1, 1))!!
        assertEquals(License.PRODUCT_FOREVER, i.product); assertNull(i.until)
    }

    @Test fun codeFoundInMailOrLinkAndTypos() {
        assertNotNull(License.check(License.extract("Hallo!\nDein Code:\n$year\nViel Spaß"), pk, today))
        val link = "https://wissensgarten.mathenachhilfe-online.com/?pro=" + year.replace("-", "")
        assertEquals(year, License.extract(link))
        assertNotNull(License.check(year.lowercase(), pk, today))
        assertNull("Duell-Code ist kein Plus-Code", License.extract("WG1-ABCDEF"))
    }

    @Test fun forgedOrForeignCodesFail() {
        val bad = year.substring(0, 20) + (if (year[20] == 'A') 'B' else 'A') + year.substring(21)
        assertNull(License.check(bad, pk, today))
        assertNull(License.check(year, "1" + pk.drop(1), today))
        assertNull("Code einer anderen App", License.check(otherApp, pk, today))
        assertNull(License.check("", pk, today)); assertNull(License.check("WG-123", pk, today))
    }
}
