package de.raumlicht.core.model

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * „Zwei Wege, aber nur einer wird angenommen“ darf es nicht geben: Wo mehrere Antworten dieselbe Gerade oder Ebene beschreiben, muss die Kontrolle jede einstellbare annehmen.
 *
 * Geprüft werden alle festen Aufgaben, Prüfungsteile, Schritt-Verfahren und die Übungs-Vorlagen (je 40 Zufalls-Fassungen):
 * - **Ebenen** (Regler n⃗, meist mit c): Jedes ganzzahlige Vielfache der gekürzten Gleichung – auch mit umgedrehten Vorzeichen – beschreibt dieselbe Ebene.
 *   Jedes davon, das sich einstellen lässt, muss angenommen werden; und die Fassung mit umgedrehten Vorzeichen muss sich einstellen lassen.
 * - **Richtungen** (Regler d⃗): Jedes positive Vielfache der gekürzten Richtung, das sich einstellen lässt, muss angenommen werden.
 *   Die Gegenrichtung ist bei einem Strahl auf ein Ziel wirklich falsch; wo sie abgelehnt wird, steht die Aufgabe in [oneWay] und sagt im Auftrag, wohin es gehen soll.
 */
class MehrdeutigTest {
    private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)
    private fun levels(): List<Pair<String, Level>> = buildList {
        Levels.all.forEach { add("Tour" to it) }
        Exams.all.forEach { e -> e.parts.forEach { add("Prüfung" to it.level) } }
        Guides.all.forEach { e -> e.parts.forEach { add("Schritte" to it.level) } }
        Gen.Kind.entries.forEach { k -> for (s in 1L..40L) runCatching { Gen.level(k, s) }.getOrNull()?.let { add("Üben ${k.name}" to it) } }
        MoreGen.Kind.entries.forEach { k -> for (s in 1L..40L) runCatching { MoreGen.level(k, s) }.getOrNull()?.let { add("Üben ${k.name}" to it) } }
        ProGen.Kind.entries.forEach { k -> for (s in 1L..40L) runCatching { ProGen.level(k, s) }.getOrNull()?.let { add("Üben ${k.name}" to it) } }
    }
    private fun inRange(c: Control, v: List<Int>) = v.indices.all { v[it] in c.min[it]..c.max[it] }

    @Test fun everyEquivalentAnswerIsAccepted() {
        val problems = ArrayList<String>(); val oneWaySeen = sortedSetOf<String>(); var planes = 0; var dirs = 0; var checked = 0
        for ((group, l) in levels()) {
            val n = l.controls.firstOrNull { it.kind == ControlKind.VECTOR && it.name == "n⃗" }
            val d = l.controls.firstOrNull { it.kind == ControlKind.VECTOR && it.name == "d⃗" }
            if (n != null && l.id in oneWay) {
                // Normalenvektor mit vorgegebener Seite: Der Auftrag muss die Seite nennen, und jedes einstellbare positive Vielfache zählt
                assertTrue("${l.id}: Der Auftrag nennt nicht, in welche Richtung der Vektor zeigen soll", oneWay.getValue(l.id) in l.mission)
                val sn = l.solution.getValue(n.key); val g = sn.fold(0, ::gcd).coerceAtLeast(1); dirs++
                for (m in 1..6) { val v = sn.map { it / g * m }; if (!inRange(n, v)) continue
                    checked++; if (!l.evaluate(mapOf(n.key to v)).success) problems += "$group ${l.id}: Vektor mit Faktor $m ($v) wird abgelehnt" }
            } else if (n != null && l.controls.all { it === n || it.key == "c" }) {
                planes++
                val c = l.controls.firstOrNull { it.key == "c" }
                val sn = l.solution.getValue(n.key); val sc = c?.let { l.solution.getValue(it.key)[0] }
                val g = (sn + listOfNotNull(sc)).fold(0, ::gcd).coerceAtLeast(1)
                var negativeSettable = false
                for (m in (-6..6).filter { it != 0 }) {
                    val vn = sn.map { it / g * m }; val vc = sc?.let { it / g * m }
                    if (!inRange(n, vn) || (c != null && vc !in c.min[0]..c.max[0])) continue
                    if (m < 0) negativeSettable = true
                    checked++
                    val v = l.solution + (n.key to vn) + (if (c != null) mapOf(c.key to listOf(vc!!)) else emptyMap())
                    if (!l.evaluate(v).success) problems += "$group ${l.id}: Ebene mit Faktor $m (n⃗ = $vn${vc?.let { ", c = $it" } ?: ""}) wird abgelehnt"
                }
                if (!negativeSettable) problems += "$group ${l.id}: die Gleichung mit umgedrehten Vorzeichen lässt sich nicht einstellen (n⃗ = ${sn.map { -it / g }})"
            } else if (d != null && l.controls.size == 1) {
                dirs++
                val sd = l.solution.getValue(d.key); val g = sd.fold(0, ::gcd).coerceAtLeast(1)
                for (m in 1..6) { val v = sd.map { it / g * m }; if (!inRange(d, v)) continue
                    checked++; if (!l.evaluate(mapOf(d.key to v)).success) problems += "$group ${l.id}: Richtung mit Faktor $m ($v) wird abgelehnt" }
                val back = sd.map { -it / g }
                if (inRange(d, back) && !l.evaluate(mapOf(d.key to back)).success) { val name = if (group.startsWith("Üben")) group else l.id
                    oneWaySeen += name
                    oneWay[name]?.let { hint -> assertTrue("$name: Der Auftrag nennt das Ziel nicht („$hint“ fehlt): ${l.mission}", hint in l.mission) } }
            }
        }
        println("MEHRDEUTIG: $planes Ebenen-Aufgaben, $dirs Richtungs-Aufgaben, $checked gleichwertige Antworten geprüft; nur eine Richtung zählt bei: $oneWaySeen")
        problems.distinct().take(40).forEach { println("MEHRDEUTIG-PROBLEM: $it") }
        assertTrue("Gleichwertige Antworten werden abgelehnt oder lassen sich nicht einstellen:\n" + problems.distinct().take(20).joinToString("\n"), problems.isEmpty())
        assertTrue("Neue Aufgabe, bei der die Gegenrichtung abgelehnt wird – Auftrag prüfen und hier eintragen: ${oneWaySeen - oneWay.keys}", oneWay.keys.containsAll(oneWaySeen))
    }

    /**
     * Aufgaben, bei denen nur eine Seite zählt – zu Recht, weil der Auftrag sie nennt. Dahinter das Wort, das im Auftrag stehen muss.
     * (Bei 10-4 und 19-3 lässt sich die Gegenrichtung gar nicht einstellen; der Auftrag sagt „in den Raum hinein“ bzw. „nach oben“ – das prüft [directionTasksSayWhichWay].)
     */
    private val oneWay = mapOf(
        "5-4" to "nach unten",          // Normalenvektor, der vom Spot zur Leinwand zeigt
        "Üben AIM" to "zum Ring",       // ein Strahl soll ein Ziel treffen – die Gegenrichtung verfehlt es wirklich
    )

    @Test fun directionTasksSayWhichWay() {
        assertTrue("in den Raum hinein" in Levels.byId("10-4")!!.mission); assertTrue("nach oben" in Levels.byId("19-3")!!.mission)
        // weitere Aufgaben mit zwei mathematischen Lösungen, bei denen der Auftrag die gemeinte nennt
        assertTrue("positive" in Levels.byId("14-1")!!.mission || "positiv" in Levels.byId("14-1")!!.mission); assertTrue("Tiefpunkt" in Levels.byId("14-3")!!.mission)
    }
}
