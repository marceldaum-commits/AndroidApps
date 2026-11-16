package de.wissensgarten.core.data

import de.wissensgarten.core.model.License
import de.wissensgarten.core.model.PackJson
import org.junit.Assert.*
import org.junit.Test

class PlusEntitlementsTest {
    // Testschlüssel (Seed 0…31) und Codes aus pro/plus_code.py (Kauftag 16.11.2026)
    private val pk = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val year = "WG-041G2-0000F-MGKKZ-X11SR-53KYB-JN7RV-BFHF0-HKM5S-EGK3C-T9N7D-711Y9-7111X-SZD9W-05KGS-WKTH4-HK7N0-P5PJ3-BD3TB-BBDQ9-CQNKK-65JZV-NFXKH-Y6JR1-0P"
    private val forever = "WG-041G6-0000F-N0KKY-KAY2H-BSPZ4-APHE6-0KYWH-X60XC-QWN2Y-620E9-31QB0-A9H5E-9PZRM-ZSR85-0Z9TR-XZ754-HCPMX-GT3XF-ESVJQ-HZSDY-C1Z3Q-W9G7Y-9DSSK-0E"
    private val day0 = kotlinx.datetime.LocalDate(2026, 11, 16).toEpochDays().toLong()
    private var today = day0
    private val enforced = PlusConfig(pk, "https://shop/jahr", "https://shop/immer", purchaseAllowed = true)
    private fun ent(cfg: PlusConfig = enforced) = PlusEntitlements(Settings(MemoryKeyValueStore()), cfg) { today }
    private val plusPack = PackJson.parse("""{"id":"ob-x","version":1,"title":"X","subject":"mathe","grades":[12],"emoji":"🔢","tier":"plus","cards":[{"id":"1","q":"q","a":"a"}]}""")
    private val freePack = plusPack.copy(id = "s1-x", tier = "free")

    @Test fun withoutCheckoutNothingIsLocked() {
        val e = ent(enforced.copy(checkoutYear = "", checkoutForever = ""))
        assertTrue(e.canUse(plusPack)); assertTrue(e.canUseWeekReport()); assertEquals(Int.MAX_VALUE, e.maxProfiles())
        // Play-Fassung: kein Kaufweg → nie gesperrt
        assertTrue(ent(enforced.copy(purchaseAllowed = false)).canUse(plusPack))
    }

    @Test fun freeUserGetsFreePacksTwoProfilesNoReport() {
        val e = ent()
        assertTrue(e.canUse(freePack)); assertFalse(e.canUse(plusPack))
        assertEquals(2, e.maxProfiles()); assertFalse(e.canUseWeekReport())
    }

    @Test fun trialOncePerDeviceForSevenDays() {
        val e = ent()
        assertTrue(e.startTrial()); assertTrue(e.plusActive.value); assertTrue(e.canUse(plusPack)); assertEquals(6, e.maxProfiles())
        assertFalse("nur einmal", e.startTrial())
        today = day0 + 6; e.refresh(); assertTrue(e.plusActive.value)
        today = day0 + 7; e.refresh(); assertFalse("endet von allein", e.plusActive.value)
        assertNull("Test ist kein Code für den Server", e.code)
    }

    @Test fun yearCodeFromLinkWorksAndExpires() {
        val e = ent()
        assertEquals(PlusEntitlements.Redeem.OK, e.redeem("https://wissensgarten.mathenachhilfe-online.com/?pro=" + year.replace("-", "")))
        assertTrue(e.plusActive.value); assertEquals(License.PRODUCT_YEAR, e.status.value.product)
        assertNotNull(e.code)
        today = day0 + 365; e.refresh()
        assertFalse(e.plusActive.value); assertNull(e.code)
        assertEquals(PlusEntitlements.Redeem.EXPIRED, ent().redeem(year))
    }

    @Test fun foreverAndInvalidCodes() {
        val e = ent()
        assertEquals(PlusEntitlements.Redeem.INVALID, e.redeem("Hallo"))
        assertEquals(PlusEntitlements.Redeem.INVALID, e.redeem(year.replace('Q', 'R')))
        assertFalse(e.plusActive.value)
        assertEquals(PlusEntitlements.Redeem.OK, e.redeem("  ${forever.lowercase()}  "))
        today = day0 + 4000; e.refresh(); assertTrue(e.plusActive.value)
    }
}
