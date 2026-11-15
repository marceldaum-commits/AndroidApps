package de.kurvenflug.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import de.kurvenflug.app.MainActivity
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tippt wie ein Kind durch die echte App: jeder Reiter, jede Kachel, jede Karte – hinein und wieder zurück.
 * Ein Knopf, der nichts tut, ein Absturz oder ein kaputtes „Zurück“ fällt hier auf.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class AppClickThroughTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val problems = mutableListOf<String>()
    private val log = File(System.getProperty("java.io.tmpdir"), "klicktest.log")
    private fun note(s: String) = log.appendText("${System.currentTimeMillis() / 1000 % 100000} $s\n")

    private fun tick(ms: Long = 400) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun onHome() = rule.onAllNodesWithText("Heute").fetchSemanticsNodes().isNotEmpty()
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; tick() }
    private fun tab(name: String) { rule.onAllNodesWithText(name).onLast().performClick(); tick() }

    private fun start() {
        rule.mainClock.autoAdvance = false
        tick(800)
        rule.onNodeWithText("7").performClick(); tick()
        repeat(3) { rule.onAllNodesWithText("Weiter ›").onFirst().performClick(); tick(300) }
        rule.onNodeWithText("Klar helfe ich! 🚀").performClick(); tick()
        if (!onHome()) fail("Nach Klassen-Wahl und Geschichte keine Startseite")
        note("start ok")
    }

    /** Antippen, prüfen, dass ein neuer Bildschirm kommt, zurück, prüfen, dass die Startseite wieder da ist. */
    private fun visit(label: String, tabName: String, expect: String? = null) {
        note("→ $label")
        tab(tabName)
        val all = rule.onAllNodesWithText(label, substring = true)
        if (all.fetchSemanticsNodes().isEmpty()) { problems += "„$label“ ($tabName): nicht gefunden"; return }
        val node = all.onFirst()
        runCatching { node.performScrollTo() }
        try { node.performClick() } catch (e: Throwable) { problems += "„$label“: Klick fehlgeschlagen – ${e.message?.take(120)}"; return }
        tick(800)
        if (onHome()) problems += "„$label“ ($tabName): Startseite bleibt – Knopf tut nichts"
        expect?.let { if (rule.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isEmpty()) problems += "„$label“: erwartet „$it“" }
        back()
        if (!onHome()) back()
        if (!onHome()) problems += "„$label“: Zurück führt nicht zur Startseite"
        note("  ok $label")
    }

    private fun report() { note("Probleme: $problems"); if (problems.isNotEmpty()) fail("Gefundene Probleme:\n" + problems.joinToString("\n")) }

    @Test fun todayAndGames() {
        start()
        visit("Weiter geht's!", "Heute")
        visit("Tagesmission", "Heute")
        visit("Volts Schiff", "Heute", "Hangar")
        visit("Duell", "Spiele")
        visit("Sturm", "Spiele")
        visit("Detektiv", "Spiele")
        visit("Baukasten", "Spiele", "Baukasten")
        // Zurück auf einem anderen Reiter: zu „Heute“, App bleibt offen
        tab("Spiele"); back()
        if (rule.onAllNodesWithText("Weiter geht's!").fetchSemanticsNodes().isEmpty()) problems += "Zurück im Reiter Spiele führt nicht zu Heute"
        report()
    }

    @Test fun meTab() {
        start()
        listOf("Lernstand" to "Lernstand", "Abzeichen" to null, "Lexikon" to "Lexikon", "Werkstatt" to "Werkstatt",
            "Einstellungen" to "Einstellungen", "Hangar" to "Hangar", "Sticker" to "Sticker", "Crew" to "Crew",
            "Station" to "Raumstation", "Spieler" to "Spieler", "Für Eltern" to "Eltern").forEach { (l, e) -> visit(l, "Ich", e) }
        tab("Ich")
        rule.onAllNodesWithText("🎓").onFirst().performScrollTo().performClick(); tick()
        if (rule.onAllNodesWithText("In welcher Klasse", substring = true).fetchSemanticsNodes().isEmpty()) problems += "Ich → Klasse: Auswahl erscheint nicht"
        else { rule.onNodeWithText("9").performClick(); tick() }
        report()
    }

    @Test fun galaxy() {
        start()
        tab("Galaxie")
        note("→ Karte Planet")
        rule.onAllNodesWithText("Koordinaten").onFirst().performClick(); tick()
        if (rule.onAllNodesWithText("Navi-Computer", substring = true).fetchSemanticsNodes().isEmpty()) problems += "Galaxie-Karte: Planet öffnet kein Fenster"
        else {
            rule.onAllNodesWithText("1").onFirst().performClick(); tick(800)
            if (onHome()) problems += "Galaxie-Karte: Level 1 startet nicht"
            back(); if (!onHome()) back()
        }
        note("→ Liste")
        tab("Galaxie")
        rule.onNodeWithText("📋 Liste").performClick(); tick()
        rule.onAllNodesWithText("Geraden").onFirst().performScrollTo().performClick(); tick()
        if (rule.onAllNodesWithText("Boss", substring = true).fetchSemanticsNodes().isEmpty()) problems += "Galaxie-Liste: Kapitel klappt nicht auf"
        report()
    }
}
