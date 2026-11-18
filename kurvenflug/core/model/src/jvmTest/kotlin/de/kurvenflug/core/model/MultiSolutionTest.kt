package de.kurvenflug.core.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Mehrere richtige Lösungen (Auftrag 18.11.): Der Flug akzeptiert jede Einstellung, die trifft. Die Rechenweg-Kontrolle darf
 * deshalb keine Zahl als „falsch“ werten, die zu einer anderen treffenden Lösung gehört – und die Lösungsanzeige sagt,
 * wenn das Kind anders (aber richtig) getroffen hat.
 */
class MultiSolutionTest {
    private val scan = MultiSolutionScan()

    @Test fun walkthroughNeverRejectsAnotherRightAnswer() {
        val bad = ArrayList<String>()
        (ReadTexts.fixedLevels() + (0..60).map { Daily.forDay(20_775L + it) }).forEach { l ->
            val steps = runCatching { Walkthrough.of(l) }.getOrNull() ?: return@forEach
            val sols = scan.clusters(l) ?: return@forEach
            if (sols.size < 2) return@forEach
            val fixedSoFar = HashMap<String, Double>()   // Werte, die frühere Rechenweg-Schritte schon festgelegt haben
            steps.filter { it.sets != null }.forEach { st ->
                val key = st.sets!!; val step = l.params.first { it.key == key }.step
                fun near(k: String, a: Double, b: Double) = kotlin.math.abs(a - b) <= 1.5 * l.params.first { it.key == k }.step
                val others = sols.filter { sol -> fixedSoFar.all { (k, v) -> sol[k]?.let { near(k, it, v) } ?: true } }
                    .mapNotNull { it[key] }.filter { kotlin.math.abs(it - st.answer) > 1.5 * step }
                fixedSoFar[key] = st.answer
                val wrongButRight = st.choices.filter { c -> c != st.answer && others.any { kotlin.math.abs(it - c) <= 1.5 * step } }
                if (wrongButRight.isNotEmpty()) bad += "${l.id}: Rechenweg wertet $key = ${wrongButRight.map { num(it) }} als falsch, trifft aber auch"
            }
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun ownWayIsNamed() {
        val l = Levels.byId("4-3")!!
        assertNull(ReadTexts.ownWay(l, l.solution))
        val other = l.solution + ("angle" to 63.0)
        assertTrue(Flight.fly(l, other).success)                       // der steile Wurf trifft auch …
        assertTrue(ReadTexts.ownWay(l, other)!!.contains("auch richtig"))   // … und wird ausdrücklich als richtig genannt
    }
}
