package de.wissensgarten.content

import de.wissensgarten.core.model.Difficulty
import de.wissensgarten.core.model.GameMode
import de.wissensgarten.core.model.Games
import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import de.wissensgarten.core.model.Stage
import de.wissensgarten.core.model.StudyCard
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nachweis: Jede Schwierigkeitsstufe ist strikt schwerer als die vorige – gemessen an allen echten Karten.
 * 1. Stufen beim Spielen (Wachstum der Karte: Samen → Baum/Profi)
 * 2. Stufen der Inhalte (Klassen-Band: Grundschule → Experte)
 * Die Tabellen landen in SCHWIERIGKEIT.md (für Eltern und die Meldung).
 */
class DifficultyTest {
    private val packs: List<Pack> = File("src/main/assets/packs").listFiles { f -> f.extension == "json" }!!
        .sortedBy { it.name }.map { PackJson.parse(it.readText()) }

    data class StageRow(val stage: Stage, val free: Double, val options: Double, val similar: Double, val guess: Double, val intervalDays: Double)
    data class BandRow(val name: String, val packs: Int, val cards: Int, val grade: Double, val free: Double, val similar: Double, val answerLen: Double)

    /** Typische Stabilität (Tage bis 90 % Erinnerung ≈ Wiederholungsabstand) je Stufe – Mitte des Bereichs aus Stage.of. */
    private val stability = mapOf(Stage.SEED to 0.0, Stage.SPROUT to 1.0, Stage.SHOOT to 4.0, Stage.BUSH to 14.0, Stage.BLOOM to 40.0, Stage.TREE to 90.0)

    private fun stageRows(): List<StageRow> = Stage.entries.map { stage ->
        val tier = Difficulty.tier(stage)
        var free = 0; var n = 0; var optSum = 0.0; var optN = 0; var simSum = 0.0; var simN = 0; var guess = 0.0
        for (p in packs) {
            val answers = p.cards.map { it.a }
            for ((i, c) in p.cards.withIndex()) {
                val reps = if (stage == Stage.SEED) 0 else 1 + i % 5
                val card = StudyCard(p.id + c.id, p.id, c.q, c.a, c.choices, c.info, Memory(stability.getValue(stage), 5.0, 0, reps, 0))
                // Schüler ab Klasse 5 mit Tipp-Option; Spracherkennung zählt hier nicht (geräteabhängig)
                val mode = Games.choose(card, answers, simple = false, canSpeak = false, canType = true)
                val opts = Games.options(card, answers, stage)
                n++
                when (mode) {
                    GameMode.TYPE, GameMode.SPEAK, GameMode.FLIP -> { free++ }
                    GameMode.DISCOVER -> { guess += 1.0 } // Antwort wird gezeigt
                    GameMode.SWIPE -> { guess += 0.5 }
                    GameMode.LETTERS -> {}                // Antwort selbst zusammensetzen: nicht ratbar
                    else -> { val k = opts?.size ?: 4; guess += 1.0 / k }
                }
                if (opts != null && mode !in setOf(GameMode.TYPE, GameMode.SPEAK, GameMode.FLIP, GameMode.LETTERS)) {
                    optSum += opts.size; optN++
                    simSum += opts.filter { it != c.a }.maxOf { Difficulty.similarity(it, c.a) }; simN++
                }
            }
        }
        StageRow(stage, free.toDouble() / n, if (optN == 0) tier.options.toDouble() else optSum / optN,
            if (simN == 0) 1.0 else simSum / simN, guess / n, stability.getValue(stage))
    }

    private val bands = listOf("Grundschule" to 1..4, "Klasse 5–7" to 5..7, "Klasse 8–10" to 8..10, "Oberstufe/Abi" to 11..13, "Experte" to 14..14)

    private fun bandRows(): List<BandRow> = bands.map { (name, range) ->
        val ps = packs.filter { it.grades.min() in range }
        val cards = ps.flatMap { it.cards }
        val withChoices = cards.filter { !it.choices.isNullOrEmpty() }
        BandRow(name, ps.size, cards.size,
            if (ps.isEmpty()) 0.0 else ps.map { it.grades.average() }.average(),
            if (cards.isEmpty()) 0.0 else cards.count { it.choices.isNullOrEmpty() }.toDouble() / cards.size,
            if (withChoices.isEmpty()) 0.0 else withChoices.map { c -> c.choices!!.maxOf { Difficulty.similarity(it, c.a) } }.average(),
            if (cards.isEmpty()) 0.0 else cards.map { it.a.length }.average())
    }

    private fun pct(x: Double) = "${Math.round(x * 100)} %"
    private fun f2(x: Double) = String.format(java.util.Locale.GERMANY, "%.2f", x)
    private fun f1(x: Double) = String.format(java.util.Locale.GERMANY, "%.1f", x)

    @Test fun everyStageIsStrictlyHarder() {
        val rows = stageRows()
        val bandRows = bandRows()
        writeReport(rows, bandRows)
        val errors = mutableListOf<String>()
        rows.zipWithNext { a, b ->
            val t = "${a.stage.label} → ${b.stage.label}"
            if (b.guess >= a.guess) errors += "$t: Rate-Chance sinkt nicht (${f2(a.guess)} → ${f2(b.guess)})"
            if (b.free < a.free) errors += "$t: weniger freie Eingabe"
            if (b.options < a.options) errors += "$t: weniger Antwortmöglichkeiten"
            if (b.similar < a.similar - 1e-9) errors += "$t: Ablenker weniger ähnlich (${f2(a.similar)} → ${f2(b.similar)})"
            if (b.intervalDays <= a.intervalDays) errors += "$t: Abstand wächst nicht"
        }
        val last = rows.last()
        if (last.free < 0.9) errors += "Profi-Stufe: nur ${pct(last.free)} freie Eingabe (Ziel ≥ 90 %)"
        bandRows.zipWithNext { a, b ->
            val t = "${a.name} → ${b.name}"
            if (b.packs == 0) errors += "$t: keine Pakete auf der Stufe ${b.name}"
            if (b.grade <= a.grade) errors += "$t: Klassenstufe steigt nicht"
            if (b.free <= a.free) errors += "$t: Anteil freie Eingabe steigt nicht (${pct(a.free)} → ${pct(b.free)})"
            if (b.similar <= a.similar) errors += "$t: Ablenker werden nicht ähnlicher (${f2(a.similar)} → ${f2(b.similar)})"
            if (b.answerLen <= a.answerLen) errors += "$t: Antworten werden nicht länger (${f1(a.answerLen)} → ${f1(b.answerLen)})"
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    private fun writeReport(rows: List<StageRow>, bandRows: List<BandRow>) {
        val md = buildString {
            appendLine("# Schwierigkeit im Wissensgarten – Nachweis")
            appendLine()
            appendLine("Automatisch erzeugt von `content/src/test/java/de/wissensgarten/content/DifficultyTest.kt` aus allen ${packs.size} Paketen (${packs.sumOf { it.cards.size }} Karten).")
            appendLine("Der Test schlägt fehl, sobald eine Stufe nicht strikt schwerer ist als die vorige.")
            appendLine()
            appendLine("## 1. Beim Spielen: Je größer die Pflanze, desto schwerer die Frage")
            appendLine()
            appendLine("| Stufe | Pflanze | freie Eingabe | Ø Antwortmöglichkeiten | Ablenker-Ähnlichkeit | Rate-Chance | Wiederholungsabstand |")
            appendLine("|---|---|---|---|---|---|---|")
            for (r in rows) appendLine("| ${Difficulty.tier(r.stage).label} | ${r.stage.label} | ${pct(r.free)} | ${if (r.free >= 0.999) "keine Auswahl" else f1(r.options)} | ${if (r.free >= 0.999) "–" else f2(r.similar)} | ${pct(r.guess)} | ${if (r.intervalDays == 0.0) "neu" else "≈ ${r.intervalDays.toInt()} ${if (r.intervalDays == 1.0) "Tag" else "Tage"}"} |")
            appendLine()
            appendLine("Freie Eingabe = Antwort eintippen (ab Klasse 5) bzw. ohne Auswahl erinnern. Rate-Chance = Wahrscheinlichkeit, zufällig richtig zu liegen (Samen: Antwort wird gezeigt).")
            appendLine("Ablenker-Ähnlichkeit = wie ähnlich der ähnlichste falsche Vorschlag der richtigen Antwort ist (0 = ganz anders, 1 = gleich).")
            appendLine()
            appendLine("## 2. Inhalte: von der Grundschule bis zum Experten")
            appendLine()
            appendLine("| Stufe | Pakete | Karten | Ø Klasse | freie Eingabe | Ablenker-Ähnlichkeit | Ø Antwortlänge |")
            appendLine("|---|---|---|---|---|---|---|")
            for (b in bandRows) appendLine("| ${b.name} | ${b.packs} | ${b.cards} | ${if (b.grade >= 14) "Erw." else f1(b.grade)} | ${pct(b.free)} | ${f2(b.similar)} | ${f1(b.answerLen)} Zeichen |")
        }
        File("../SCHWIERIGKEIT.md").writeText(md)
    }
}
