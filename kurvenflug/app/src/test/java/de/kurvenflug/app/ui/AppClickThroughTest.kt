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

/**
 * Tippt wie ein Kind durch die echte App: jeder Reiter, jede Kachel, jede Karte – hinein und wieder zurück.
 * Ein Knopf, der nichts tut, oder ein Absturz fällt hier auf.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class AppClickThroughTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val problems = mutableListOf<String>()

    private fun tick(ms: Long = 1200) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun onHome() = rule.onAllNodesWithText("Heute").fetchSemanticsNodes().isNotEmpty()
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; tick() }
    private fun tab(name: String) { rule.onAllNodesWithText(name).onLast().performClick(); tick() }

    /** Antippen, prüfen, dass ein neuer Bildschirm kommt, zurück, prüfen, dass die Startseite wieder da ist. */
    private fun visit(label: String, tabName: String, expect: String? = null) {
        tab(tabName)
        val nodes = rule.onAllNodesWithText(label, substring = true).fetchSemanticsNodes()
        if (nodes.isEmpty()) { problems += "„$label“ ($tabName): nicht gefunden"; return }
        val node = rule.onAllNodesWithText(label, substring = true).onFirst()
        runCatching { node.performScrollTo() }
        try { node.performClick() } catch (e: Throwable) { problems += "„$label“: Klick fehlgeschlagen – ${e.message?.take(120)}"; return }
        tick(1500)
        if (onHome()) problems += "„$label“ ($tabName): Startseite bleibt – Knopf tut nichts"
        expect?.let { if (rule.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isEmpty()) problems += "„$label“: erwartet „$it“" }
        back()
        if (!onHome()) { back() }
        if (!onHome()) problems += "„$label“: Zurück führt nicht zur Startseite"
    }

    @Test fun everyButtonLeadsSomewhereAndBack() {
        rule.mainClock.autoAdvance = false
        tick(2000)
        // Erststart: Klasse wählen, Geschichte
        rule.onNodeWithText("7").performClick(); tick()
        repeat(3) { rule.onAllNodesWithText("Weiter ›").onFirst().performClick(); tick(600) }
        rule.onNodeWithText("Klar helfe ich! 🚀").performClick(); tick()
        if (!onHome()) fail("Nach Klassen-Wahl und Geschichte keine Startseite")

        visit("Weiter geht's!", "Heute")
        visit("Tagesmission", "Heute")
        visit("Volts Schiff", "Heute", "Hangar")
        visit("Duell", "Spiele")
        visit("Sturm", "Spiele")
        visit("Detektiv", "Spiele")
        visit("Baukasten", "Spiele", "Baukasten")
        visit("Lernstand", "Ich", "Lernstand")
        visit("Abzeichen", "Ich")
        visit("Lexikon", "Ich", "Lexikon")
        visit("Werkstatt", "Ich", "Werkstatt")
        visit("Einstellungen", "Ich", "Einstellungen")
        visit("Hangar", "Ich", "Hangar")
        visit("Sticker", "Ich", "Sticker")
        visit("Crew", "Ich", "Crew")
        visit("Station", "Ich", "Raumstation")
        visit("Spieler", "Ich", "Spieler")
        visit("Für Eltern", "Ich", "Eltern")

        // Galaxie: Karte → Planet → Level
        tab("Galaxie")
        rule.onAllNodesWithText("Koordinaten").onFirst().performClick(); tick()
        if (rule.onAllNodesWithText("Navi-Computer", substring = true).fetchSemanticsNodes().isEmpty()) problems += "Galaxie-Karte: Planet öffnet kein Fenster"
        else {
            rule.onAllNodesWithText("1").onFirst().performClick(); tick(1500)
            if (onHome()) problems += "Galaxie-Karte: Level 1 startet nicht"
            back(); if (!onHome()) back()
        }
        // Galaxie: Liste → Kapitel aufklappen → Level
        tab("Galaxie")
        rule.onNodeWithText("📋 Liste").performClick(); tick()
        rule.onAllNodesWithText("Geraden").onFirst().performScrollTo().performClick(); tick()
        val lvl = rule.onAllNodesWithText("1").fetchSemanticsNodes()
        if (lvl.isEmpty()) problems += "Galaxie-Liste: Kapitel klappt nicht auf"
        // Klasse ändern
        tab("Ich")
        rule.onAllNodesWithText("🎓").onFirst().performScrollTo().performClick(); tick()
        if (rule.onAllNodesWithText("In welcher Klasse", substring = true).fetchSemanticsNodes().isEmpty()) problems += "Ich → Klasse: Auswahl erscheint nicht"
        else { rule.onNodeWithText("9").performClick(); tick() }

        if (problems.isNotEmpty()) fail("Gefundene Probleme:\n" + problems.joinToString("\n"))
    }
}
