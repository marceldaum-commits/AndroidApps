package de.raumlicht.core.model

import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Sammelt alle Texte, die Lux vorlesen kann, nach build/sprechtexte.txt (je Zeile: Art, Tab, vorlesefertiger Text).
 * Daraus erzeugt `tools/stimme/lautschrift.py` die Lautschrift-Dateien für die natürliche Stimme (siehe dort).
 * „fest“ = Text steht wörtlich fest (wird als ganzer Satz umgesetzt), „wort“ = Beispiel für veränderliche Texte (liefert Wörter fürs Wörterbuch).
 */
class SpeechDumpTest {
    @Test fun dump() {
        val fixed = LinkedHashSet<String>(); val more = LinkedHashSet<String>(); val r = Random(7)
        fun wild(l: Level): Values = l.controls.associate { c -> c.key to c.start.indices.map { i -> r.nextInt(c.min[i], c.max[i] + 1) } }
        fun collect(l: Level, sure: Boolean) {
            val into = if (sure) fixed else more
            into += l.mission; into += l.tip
            runCatching { val out = l.evaluate(l.solution); into += out.message; l.finale?.invoke(l.solution)?.acts?.forEach { into += it.say } }
            runCatching { more += l.evaluate(l.start).message }
            repeat(40) { runCatching { val out = l.evaluate(wild(l)); more += out.message; more += out.message + " Tipp gefällig?" } }
            more += l.way; more += l.title
        }
        Levels.chapters.forEach { c -> fixed += c.intro; more += c.title; more += c.topic; c.levels.forEach { collect(it, true) } }
        Exams.all.forEach { e -> fixed += e.intro; more += e.title; e.parts.forEach { collect(it.level, true) }; for (g in 0..e.maxPoints) more += Exam.verdict(g, e.maxPoints) }
        ProGen.Kind.entries.forEach { k -> for (seed in 1L..60L) collect(ProGen.level(k, seed), false) }
        JamLevel.entries.forEach { more += it.title; more += it.hint }
        Gen.Kind.entries.forEach { k -> for (seed in 1L..40L) runCatching { collect(Gen.level(k, seed), false) } }
        fixed += listOf("Hallo, ich bin Lux!", "Hallo, ich bin Lux. So klinge ich jetzt.", "Geschafft!", "Der Schwung reicht nicht.", "Im Ziel, aber der Auftrag fehlt noch.")
        SpotKind.entries.forEach { fixed += it.title; more += it.rule }
        for (s in 0..100) more += Ghost.verdict(s)
        val text = buildString {
            fixed.map(Speech::clean).filter { it.isNotBlank() }.toSet().forEach { append("fest\t").append(it).append('\n') }
            more.map(Speech::clean).filter { it.isNotBlank() }.toSet().forEach { append("wort\t").append(it).append('\n') }
        }
        File("build").mkdirs(); File("build/sprechtexte.txt").writeText(text)
        check(fixed.size > 300) { "zu wenige Texte gesammelt: ${fixed.size}" }
    }
}
