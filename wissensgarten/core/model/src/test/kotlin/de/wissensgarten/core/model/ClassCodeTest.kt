package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class ClassCodeTest {
    private val set = ClassSet("Englisch Unit 3", "🇬", (1..30).map { ClassCard("word $it", "Wort $it", if (it % 3 == 0) "🐶" else null) })

    @Test fun roundTrip() {
        val code = ClassCodes.encode(set)
        assertEquals(set, ClassCodes.decode("Hier ist der Code: ${ClassCodes.LINK_BASE}$code danke!"))
    }

    @Test fun thirtyCardsFitIntoQr() = assertTrue(ClassCodes.link(set).length.toString(), ClassCodes.fitsQr(set))

    @Test fun brokenCodeIsNull() {
        val code = ClassCodes.encode(set)
        assertNull(ClassCodes.decode(code.dropLast(1) + if (code.last() == 'a') "b" else "a"))
        assertNull(ClassCodes.decode("WGK1-abc.1"))
        assertNull(ClassCodes.decode("nichts"))
    }
}
