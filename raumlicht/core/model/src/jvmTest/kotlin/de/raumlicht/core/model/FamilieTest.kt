package de.raumlicht.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FamilieTest {
    private val data = mapOf("stars" to "1:3", "proCode" to "RL-X", "kiCode" to "", "kiDevice" to "abc", "elternKopplung" to "id.key", "sound" to "true")

    @Test fun nochAus() = assertFalse(Familie.ENABLED)

    @Test fun lernstandOhneRechteUndOhneGeraeteKennung() =
        assertEquals(mapOf("stars" to "1:3", "sound" to "true"), Familie.child(data))

    @Test fun rechteGetrenntUndNurGesetzte() = assertEquals(mapOf("proCode" to "RL-X"), Familie.rights(data))
}
