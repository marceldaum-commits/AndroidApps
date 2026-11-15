package de.wissensgarten.app.flow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.app.MainActivity
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.data.db.CardEntity
import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.Quests
import de.wissensgarten.core.model.Scheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Klickt sich wie ein Kind durch die neuen Bereiche der echten App (MainActivity, echte Datenbank)
 * und prüft, dass jede Aktion die erwartete Reaktion auslöst.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h900dp-xhdpi")
class AppFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val app get() = ApplicationProvider.getApplicationContext<WissensgartenApp>()
    private lateinit var pid: String
    private lateinit var cards: List<CardEntity>

    @Before fun setUp() = runBlocking {
        val c = app.container
        val p = c.repository.createProfile("Lena", "🦊", 7)
        pid = p.id
        c.repository.plantPack(p.id, c.catalog.all().first { it.id == "s1-englisch-irregular-verbs" })
        val now = System.currentTimeMillis()
        val all = c.database.dao().allCards().filter { it.profileId == p.id }.sortedBy { it.sortIndex }
        // 20 Karten gelernt, die ersten 3 oft vergessen (= Unkraut), ein paar durstig
        c.database.dao().updateCards(all.take(20).mapIndexed { i, card ->
            card.withMemory(Memory(stability = if (i < 3) 1.0 else 4.0 + i, difficulty = 5.0,
                lastReview = now - (if (i % 4 == 0) 6 else 1) * Scheduler.DAY_MS, reps = 3, lapses = if (i < 3) 3 else 0))
        })
        cards = c.database.dao().allCards().filter { it.profileId == p.id }
        c.settings.setActiveProfile(p.id)
        c.settings.setOnboarded(true)
        rule.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
        step(2500)
    }

    @After fun tearDown() { scenario.close() }

    private fun step(ms: Long = 400) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun has(text: String, substring: Boolean = true) =
        rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun openTile(title: String) {
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(title, substring = true))
        rule.onNodeWithContentDescription(title, substring = true).performScrollTo().performClick()
        step(800)
    }

    /** Antwort zur gerade gezeigten Frage aus der Datenbank suchen und antippen. */
    private fun answerCurrent(): String {
        val card = cards.filter { it.reps > 0 }.firstOrNull { has(it.front) } ?: error("Keine bekannte Frage sichtbar")
        rule.onAllNodesWithText(card.back, useUnmergedTree = false).filter(hasClickAction()).onFirst().performClick()
        return card.id
    }

    @Test fun weedsCanBeAnsweredAndPulled() {
        openTile("Unkraut-Jäten")
        assertTrue("Unkraut-Bildschirm", has("Unkraut-Jäten"))
        repeat(3) { n ->
            answerCurrent(); step(300)
            assertTrue("Ziehen-Hinweis nach richtiger Antwort ($n)", has("Nach oben ziehen"))
            // dreimal auf das Unkraut tippen
            repeat(3) { rule.onNodeWithText("Nach oben ziehen", substring = true).performClick(); step(100) }
            assertTrue("Weg damit ($n)", has("Weg damit"))
            step(1200)
        }
        assertTrue("Ende-Bildschirm", has("Beet ist sauber"))
        assertEquals(3, app.container.settings.weedCount(pid))
        rule.onNodeWithText("Zurück in den Garten").performClick(); step(800)
        assertFalse("Unkraut-Kachel weg", rule.onAllNodesWithContentDescription("Unkraut-Jäten", substring = true).fetchSemanticsNodes().isNotEmpty())
        val prog = app.container.settings.questProgress(pid, Scheduler.localDay(System.currentTimeMillis()))
        assertEquals(3, prog[Quests.Kind.WEED.name]); assertEquals(3, prog[Quests.Kind.WATER.name])
    }

    @Test fun weedWrongAnswerShowsSolutionAndContinues() {
        openTile("Unkraut-Jäten")
        val card = cards.first { has(it.front) }
        rule.onAllNodes(hasClickAction() and !hasText(card.back) and hasText("–", substring = true)).onFirst().performClick(); step(300)
        assertTrue(has("Weiter"))
        rule.onNodeWithText("Weiter ▶").performClick(); step(300)
        assertTrue("nächste Frage", cards.any { it.id != card.id && has(it.front) })
    }

    @Test fun blitzRunsSixtySecondsAndRewards() {
        val dew0 = runBlocking { app.container.database.dao().profile(pid)!!.dewdrops }
        openTile("Blitzrunde")
        rule.onNodeWithText("Los geht's! ⚡").performClick(); step(300)
        repeat(4) { answerCurrent(); step(300) }
        assertTrue(has("4 richtig"))
        step(61_000)
        assertTrue("Regenbogen-Ende", has("richtig! 🌈"))
        step(500)
        val dew1 = runBlocking { app.container.database.dao().profile(pid)!!.dewdrops }
        assertEquals(dew0 + 2, dew1)
        rule.onNodeWithText("Zurück in den Garten").performClick(); step(800)
        assertTrue(has("Mehr entdecken"))
    }

    @Test fun rhythmCountsRightAnswers() {
        openTile("Rhythmus")
        rule.onNodeWithText("Musik ab! 🎶").performClick(); step(300)
        repeat(8) { answerCurrent(); step(200); step(1000) }
        assertTrue("Ende", has("Punkte! 🎉"))
        assertTrue("alle getroffen", has("8 von 8"))
    }

    @Test fun hiveFurnishes() {
        openTile("Bienenstock")
        rule.onNodeWithText("🍯 Honigtopf").performClick(); step(300)
        assertTrue(has("Eingerichtet mit 🍯 Honigtopf"))
        assertEquals(1, app.container.settings.hiveFurniture(pid, "eingang"))
    }

    @Test fun lociPlaceAndWalk() {
        openTile("Spaziergang")
        // Ort ohne Karte → Hinweis
        rule.onNodeWithText("Brunnen").performClick(); step(200)
        assertTrue(has("Erst unten eine Karte"))
        val first = cards.filter { it.reps > 0 }.sortedWith(compareByDescending<CardEntity> { it.lapses }.thenBy { it.stability }).first()
        rule.onNodeWithText(first.back, substring = true).performClick(); step(200)
        rule.onNodeWithText("Brunnen").performClick(); step(300)
        assertTrue("Merkgeschichte", has("Stell dir vor"))
        assertEquals(first.id, app.container.settings.loci(pid)[2])
        rule.onNodeWithText("Spaziergang starten", substring = true).performClick(); step(1200)
        assertTrue(has("Was war hier?"))
        rule.onAllNodesWithText(first.back).filter(hasClickAction()).onFirst().performClick(); step(300)
        assertTrue(has("Genau!"))
        step(2000)
        assertTrue("Ende", has("Orten erinnert"))
    }

    @Test fun filmAndQuestsAndCritters() {
        assertTrue("Tagesaufgaben sichtbar", rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Heutige Aufgaben")).let { has("Heutige Aufgaben") })
        assertTrue("Bewohner-Karte", has("Garten-Bewohner"))
        openTile("Garten-Film")
        assertTrue(has("Mein Garten-Film"))
        step(10_000)
        assertTrue(has("selbst gegossen"))
        rule.onNodeWithContentDescription("Zurück").performClick(); step(500)
        assertTrue(has("Mehr entdecken"))
    }
}
