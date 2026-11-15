package de.wissensgarten.core.model

import org.junit.Assert.*
import org.junit.Test

class SyllablesTest {
    private fun s(w: String) = Syllables.split(w).joinToString("-")

    @Test fun typicalPrimarySchoolWords() {
        val expected = listOf(
            "Ta-fel", "Som-mer", "Fens-ter", "Ka-ter", "Mut-ter", "Schu-le", "Zu-cker", "Tisch", "Ap-fel", "Ba-na-ne",
            "Gi-raf-fe", "El-tern", "Kin-der", "Blu-me", "Freun-de", "Mäd-chen", "Ei-mer", "Bau-er", "Feu-er", "Was-ser",
            "Schmet-ter-ling", "Erd-bee-re", "Kat-ze", "Bie-ne", "ge-hen", "Fah-rer", "Ok-to-ber", "Hund", "Hun-de",
            "Kas-ten", "Ra-ke-te", "Elefant".let { "E-le-fant" }, "Schild-krö-te", "Pin-sel", "Ro-se", "Au-to", "Teu-fel", "Ze-bra", "Zi-tro-ne", "Ad-ler", "Fa-brik", "Wes-pe", "Pfer-de",
        )
        val wrong = expected.filter { s(it.replace("-", "")) != it }.map { "${it.replace("-", "")} → ${s(it.replace("-", ""))} (erwartet $it)" }
        assertTrue(wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test fun keepsCaseAndPunctuation() {
        val seg = Syllables.segments("Die Giraffe, ja!")
        assertEquals("Die Gi raf fe ,  ja !".replace(" ", ""), seg.joinToString("") { it.first }.replace(" ", ""))
        assertEquals(listOf(0, 1, 2), seg.filter { it.first in setOf("Gi", "raf", "fe") }.map { it.second })
    }

    @Test fun shortAndOddWordsStayWhole() {
        assertEquals(listOf("ja"), Syllables.split("ja"))
        assertEquals(listOf("3/4"), Syllables.split("3/4"))
    }
}
