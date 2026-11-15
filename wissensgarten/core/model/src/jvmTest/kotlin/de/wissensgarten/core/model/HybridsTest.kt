package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class HybridsTest {
    @Test fun sameSubjectsSameFlowerInAnyOrder() {
        val h1 = Hybrids.of(Subject.MATHE, Subject.MUSIK)
        val h2 = Hybrids.of(Subject.MUSIK, Subject.MATHE)
        assertEquals(h1, h2)
        assertTrue(h1.name.startsWith("Zahlennoten"))
        assertEquals(h1, Hybrids.byId(h1.id))
    }

    @Test fun interleavesAlternately() {
        assertEquals(listOf(1, 10, 2, 20, 3), Hybrids.interleave(listOf(1, 2, 3), listOf(10, 20), 4))
    }
}
