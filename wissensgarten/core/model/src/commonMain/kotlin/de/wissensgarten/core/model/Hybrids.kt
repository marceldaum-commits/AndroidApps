package de.wissensgarten.core.model

/**
 * Kreuzungs-Blumen: Wer zwei Fächer gemischt übt (verschränktes Lernen), züchtet eine Mischblüte.
 * Name und Farben ergeben sich fest aus den beiden Fächern.
 */
data class Hybrid(val id: String, val name: String, val a: Subject, val b: Subject)

object Hybrids {
    private val syllable = mapOf(
        Subject.DEUTSCH to "Wort", Subject.MATHE to "Zahlen", Subject.SACHUNTERRICHT to "Wiesen", Subject.ENGLISCH to "Lingo",
        Subject.FRANZOESISCH to "Bonjour", Subject.LATEIN to "Caesar", Subject.SPANISCH to "Fiesta", Subject.BIOLOGIE to "Zell",
        Subject.CHEMIE to "Blubber", Subject.PHYSIK to "Funken", Subject.INFORMATIK to "Pixel", Subject.GESCHICHTE to "Ritter",
        Subject.ERDKUNDE to "Globus", Subject.POLITIK to "Wahl", Subject.WIRTSCHAFT to "Taler", Subject.ETHIK to "Herz",
        Subject.MUSIK to "Noten", Subject.KUNST to "Farb", Subject.ALLGEMEIN to "Wunder", Subject.EIGENE to "Eigen",
    )
    private val endings = listOf("rose", "glocke", "lilie", "tulpe", "stern", "blüte", "nelke", "veilchen")

    /** Gleiche Fächerkombination → immer dieselbe Blume (Reihenfolge egal). */
    fun of(x: Subject, y: Subject): Hybrid {
        val (a, b) = listOf(x, y).sortedBy { it.ordinal }
        val end = endings[(a.ordinal * 7 + b.ordinal * 3) % endings.size]
        return Hybrid("${a.id}+${b.id}", syllable.getValue(a) + syllable.getValue(b).lowercase() + end, a, b)
    }

    fun byId(id: String): Hybrid? {
        val parts = id.split("+")
        if (parts.size != 2) return null
        return of(Subject.of(parts[0]), Subject.of(parts[1]))
    }

    /** Karten zweier Beete abwechselnd mischen (A, B, A, B …). */
    fun <T> interleave(a: List<T>, b: List<T>, perSide: Int = 4): List<T> {
        val x = a.take(perSide); val y = b.take(perSide)
        return (0 until maxOf(x.size, y.size)).flatMap { i -> listOfNotNull(x.getOrNull(i), y.getOrNull(i)) }
    }

    const val PASS_RATIO = 0.75
}
