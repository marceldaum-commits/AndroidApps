package de.wissensgarten.content

import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.PackJson
import de.wissensgarten.core.model.Subject
import de.wissensgarten.core.model.Tier
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Prüft jedes Wissenspaket auf die Regeln aus PACK_FORMAT.md.
 * Läuft bei jedem Build mit – fehlerhafte Pakete fallen sofort auf.
 */
class PackValidationTest {
    private val dir = File("src/main/assets/packs")
    private val packs: List<Pair<String, Pack>> by lazy {
        dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }.map { it.name to PackJson.parse(it.readText()) }
    }

    @Test fun thereArePacksForEveryStage() {
        assertTrue("zu wenige Pakete", packs.size >= 30)
        for (band in listOf(1..4, 5..10, 11..13, 14..14)) {
            assertTrue("keine Pakete für $band", packs.any { (_, p) -> p.grades.any { it in band } })
        }
    }

    @Test fun everyPackFollowsTheFormat() {
        val errors = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        for ((file, p) in packs) {
            fun err(msg: String) { errors += "$file: $msg" }
            if (file != "${p.id}.json") err("Dateiname passt nicht zur id")
            if (!ids.add(p.id)) err("doppelte Paket-id")
            if (!Regex("^(gs|s1|ob|aw)-[a-z0-9-]+$").matches(p.id)) err("id-Format")
            if (p.version < 1) err("version < 1")
            if (Subject.entries.none { it.id == p.subject }) err("unbekanntes Fach ${p.subject}")
            if (p.grades.isEmpty() || p.grades.any { it !in 1..14 }) err("grades ungültig")
            if (p.tier != Tier.FREE && p.tier != Tier.PLUS) err("tier ungültig")
            if (p.title.isBlank() || p.description.isBlank() || p.emoji.isBlank()) err("Titel/Beschreibung/Emoji fehlt")
            if (p.cards.size !in 15..60) err("Kartenzahl ${p.cards.size}")
            if (p.intro.size !in 3..5) err("intro: 3–5 Erklär-Szenen nötig")
            p.intro.forEach { if (it.text.isBlank() || it.text.length > 170 || it.img.isBlank()) err("intro-Szene ungültig") }
            if (p.cards.any { it.img.isNullOrBlank() }) err("Merkbild (img) fehlt bei mindestens einer Karte")
            val cardIds = mutableSetOf<String>()
            val questions = mutableSetOf<String>()
            for (c in p.cards) {
                if (!cardIds.add(c.id)) err("doppelte Karten-id ${c.id}")
                if (c.q.isBlank() || c.a.isBlank()) err("leere Karte ${c.id}")
                if (c.q.length > 120) err("Frage zu lang ${c.id}")
                if (c.a.length > 80) err("Antwort zu lang ${c.id}")
                c.img?.let { if (it.isBlank() || it.length > 24 || it.any { ch -> ch in 'a'..'z' || ch in 'A'..'Z' }) err("${c.id}: img ungültig") }
                c.choices?.let { ch ->
                    if (ch.size !in 2..3) err("${c.id}: 2–3 Ablenker nötig")
                    if (c.a in ch) err("${c.id}: Antwort steht in choices")
                    if (ch.toSet().size != ch.size) err("${c.id}: doppelte choices")
                }
                // gleiche Frage mit gleicher Antwort doppelt?
                if (!questions.add(c.q + "→" + c.a)) err("doppelte Karte ${c.id}")
            }
            if (p.grades.all { it <= 4 } && p.cards.any { it.choices.isNullOrEmpty() }) err("Grundschule: alle Karten brauchen choices")
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }
}
