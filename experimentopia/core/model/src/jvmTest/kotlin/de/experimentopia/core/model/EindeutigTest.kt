package de.experimentopia.core.model

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Zwei Wege, aber nur einer zählt“ darf es nicht geben: Jede Frage hat genau eine richtige Antwort, und keine zwei Antworten
 * meinen dasselbe (gleicher Text, gleiche Zahl mit gleicher Einheit, Bruch/Prozent/Wort für denselben Anteil).
 */
class EindeutigTest {
    private val words = mapOf("die hälfte" to 0.5, "halb so" to 0.5, "doppelt" to 2.0, "ein viertel" to 0.25, "ein drittel" to 1.0 / 3, "ein neuntel" to 1.0 / 9, "gleich" to 1.0)

    /** Zahl + Einheit (oder Anteil) einer Antwort, falls sie eine Größe ist */
    private fun value(t: String): Pair<Double, String>? {
        val s = t.replace(Typo.NBSP, ' ').lowercase()
        Regex("""^(?:etwa |ca\. |rund )?(\d+(?:,\d+)?)\s*(%|[a-zµ°Ω/³²]+)?$""").find(s.trim())?.let { m ->
            val v = m.groupValues[1].replace(',', '.').toDouble(); val u = m.groupValues[2]
            return if (u == "%") v / 100 to "anteil" else v to u
        }
        Regex("""^(\d+)\s*/\s*(\d+)$""").find(s.trim())?.let { return it.groupValues[1].toDouble() / it.groupValues[2].toDouble() to "anteil" }
        return null
    }

    @Test fun keineDoppeltenOderGleichwertigenAntworten() {
        val bad = mutableListOf<String>()
        Catalog.all.forEach { e ->
            listOf("Vermutung" to e.predict, "Knobelfrage" to e.transfer).forEach { (name, q) ->
                val texts = q.choices.map { it.text.replace(Typo.NBSP, ' ').trim() }   // Groß/klein zählt (Erbanlagen AA, Aa, aa)
                if (texts.toSet().size != texts.size) bad += "${e.id} $name: doppelte Antwort $texts"
                val vals = q.choices.mapNotNull { value(it.text) }
                if (vals.toSet().size != vals.size) bad += "${e.id} $name: gleichwertige Größen ${q.choices.map { it.text }}"
            }
            if (e.why.map { it.trim().lowercase() }.toSet().size != e.why.size) bad += "${e.id}: doppelte Begründung"
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun reaktionenVerlangenAusdruecklichDieKleinstenZahlen() {
        Catalog.all.filter { it.setup is ReactionSetup }.forEach { e ->
            assertTrue("${e.id}: Auftrag muss „kleinste ganze Zahlen“ nennen (Vielfache wären sonst auch richtig)", e.task.contains("kleinsten ganzen Zahlen"))
        }
    }
}
