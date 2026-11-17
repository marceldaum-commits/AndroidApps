package de.raumlicht.core.model

import org.junit.Test
import kotlin.random.Random

/** Wie lang werden Texte, die erst in der Aufgabe entstehen? (Der Stimmen-Server nimmt zur Laufzeit höchstens 300 Zeichen.) */
class VorlesenLaengeTest {
    @Test fun runtimeTextsFitTheLiveLimit() {
        val r = Random(11); val all = LinkedHashSet<String>()
        fun wild(l: Level): Values = l.controls.associate { c -> c.key to c.start.indices.map { i -> r.nextInt(c.min[i], c.max[i] + 1) } }
        fun collect(l: Level) { all += l.mission; all += l.tip
            runCatching { all += l.evaluate(l.solution).message; l.finale?.invoke(l.solution)?.acts?.forEach { all += it.say } }
            repeat(30) { runCatching { val m = l.evaluate(wild(l)).message; all += m; all += "$m Tipp gefällig? 💡" } } }
        Levels.all.forEach(::collect); Exams.all.forEach { e -> e.parts.forEach { collect(it.level) } }
        MoreGen.Kind.entries.forEach { k -> for (s in 1L..40L) collect(MoreGen.level(k, s)) }
        ProGen.Kind.entries.forEach { k -> for (s in 1L..40L) collect(ProGen.level(k, s)) }
        Gen.Kind.entries.forEach { k -> for (s in 1L..40L) runCatching { collect(Gen.level(k, s)) } }
        val spoken = all.map(Vorlesen::spoken).filter { it.isNotEmpty() }.toSet()
        val long = spoken.filter { it.length > Vorlesen.LIVE_MAX_CHARS }
        println("LAENGE: ${spoken.size} Texte, längster ${spoken.maxOf { it.length }}, über ${Vorlesen.LIVE_MAX_CHARS}: ${long.size}")
        long.take(3).forEach { println("LANG: ${it.length} $it") }
        // Stichprobe von Texten, die NICHT in der festen Liste stehen – zum Nachmessen der Laufzeit-Vertonung am Server (build/laufzeit-stichprobe.txt)
        val fixed = Vorlesen.texts().toSet(); val sample = spoken.filter { it !in fixed }.shuffled(Random(5)).take(40)
        java.io.File("build").mkdirs(); java.io.File("build/laufzeit-stichprobe.txt").writeText(sample.joinToString("\n"))
        println("LAENGE: davon nicht in der festen Liste: ${spoken.count { it !in fixed }}")
        org.junit.Assert.assertTrue("Diese Texte sind zu lang für die Laufzeit-Vertonung: ${long.take(2)}", long.isEmpty())
    }
}
