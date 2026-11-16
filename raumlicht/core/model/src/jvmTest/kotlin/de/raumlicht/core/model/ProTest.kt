package de.raumlicht.core.model

import org.junit.Assert.*
import org.junit.Test

/** Regeln für den Abi-Pass: Was frei bleibt, was dazugehört – und dass im Auslieferungsstand nichts gesperrt ist. */
class ProTest {
    @Test fun ausgeliefertIstAllesFrei() = assertFalse("Pro darf erst eingeschaltet werden, wenn man es kaufen kann – und dann müssen die Texte „keine Käufe“ angepasst sein", Pro.ENABLED)

    @Test fun grenzeFreiPro() {
        // Kapitel 1–6 komplett frei, danach je Kapitel genau die ersten zwei
        Levels.chapters.forEach { c -> val free = c.levels.count { Pro.freeLevel(it.id) }
            assertEquals("Kapitel ${c.number}", if (c.number <= 6) c.levels.size else 2, free)
            if (c.number > 6) assertEquals(listOf(true, true, false), c.levels.take(3).map { Pro.freeLevel(it.id) }) }
        assertEquals(Levels.chapters.filter { it.number <= 6 }.sumOf { it.levels.size } + 19 * 2, Levels.all.count { Pro.freeLevel(it.id) })
        assertFalse(Pro.locked("level/1-6")); assertFalse(Pro.locked("level/7-2")); assertTrue(Pro.locked("level/7-3")); assertTrue(Pro.locked("level/25-7"))
        // Tagesaufgabe, Spiele und Grundfunktionen bleiben frei
        listOf("home", "tagesaufgabe", "jam", "spickzettel", "abzeichen", "backstage", "abi", "achterbahn/huegel", "raumflug", "galton", "ballerbude", "zufall", "einstellungen").forEach { assertFalse(it, Pro.locked(it)) }
        // Üben: nur „Einstieg“ frei
        assertFalse(Pro.locked("jam/MIX/EINSTIEG")); assertTrue(Pro.locked("jam/MIX/ABI")); assertTrue(Pro.locked("jam/PUNKTE/PROFI")); assertTrue(Pro.locked("jam/MIX"))
        // Prüfungsaufgaben: die erste frei
        assertEquals(1, Exams.all.count { Pro.freeExam(it.id) }); assertFalse(Pro.locked("abi/${Exams.all[0].id}/0")); assertTrue(Pro.locked("abi/${Exams.all[3].id}/1"))
        Pro.areas.keys.forEach { assertTrue(it, Pro.locked(it)) }
    }

    @Test fun texte() {
        assertEquals("Aufgabe 7-3 „${Levels.byId("7-3")!!.title}“", Pro.what("level/7-3")); assertEquals("Üben in der Stufe „Profi“", Pro.what("jam/MIX/PROFI")); assertEquals("Binomial-Labor", Pro.what("binomial"))
        assertTrue(Pro.benefits.first().contains("${Levels.all.size} Aufgaben")); assertTrue(Pro.free.isNotEmpty())
    }
}
