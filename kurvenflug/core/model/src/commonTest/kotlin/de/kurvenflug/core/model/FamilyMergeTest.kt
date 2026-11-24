package de.kurvenflug.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FamilyMergeTest {
    @Test fun progressKeepsBestOfBoth() {
        val handy = "2-1|3|5|1|1000\n2-2|1|1|4|2000"
        val tablet = "2-2|3|2|2|1500\n7-1|2|0|1|3000"
        assertEquals("2-1|3|5|1|1000\n2-2|3|3|4|1500\n7-1|2|0|1|3000", FamilyMerge.progress(handy, tablet))
        // gleich bleibt gleich (kein Aufaddieren)
        assertEquals(FamilyMerge.progress(handy, null), FamilyMerge.progress(handy, handy))
        // ungeschafft (0) zählt nicht als frühestes Datum
        assertEquals("3-1|1|0|2|500", FamilyMerge.progress("3-1|0|0|2|0", "3-1|1|0|1|500"))
    }

    @Test fun playsAreUnitedWithoutDuplicates() {
        val a = "2-1|3|1|30|1000\n2-2|0|3|50|2000"
        val b = "2-2|0|3|50|2000\n7-1|2|1|20|1500"
        assertEquals("2-1|3|1|30|1000\n7-1|2|1|20|1500\n2-2|0|3|50|2000", FamilyMerge.plays(a, b))
        assertEquals("", FamilyMerge.plays(null, ""))
    }

    @Test fun monthsTakeMaximum() {
        assertEquals("2026-10|5|3|2\n2026-11|40|12|10", FamilyMerge.months("2026-11|30|12|9", "2026-10|5|3|2\n2026-11|40|10|10"))
    }

    @Test fun otherKeysNeverSilentlyOverwritten() {
        val r = FamilyMerge.kind(
            mapOf("progress" to "2-1|3|0|1|1000", "name" to "Mia", "grade" to "7", "skin" to "2"),
            mapOf("progress" to "2-2|2|0|1|2000", "name" to "Mia", "grade" to "8", "hat" to "1"))
        assertEquals(listOf("grade"), r.conflicts)
        assertEquals("7", r.merged["grade"])          // bis zur Rückfrage gilt der eigene Wert
        assertEquals("1", r.merged["hat"])             // nur auf einer Seite → übernommen
        assertEquals("2-1|3|0|1|1000\n2-2|2|0|1|2000", r.merged["progress"])
    }
}
