package de.wissensgarten.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Klickt die neuen Spiele so durch, wie ein Kind es tun würde, und prüft jede Reaktion. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h900dp-xhdpi")
class InteractionTest {
    @get:Rule val rule = createComposeRule()

    private val cards = (1..10).map { i ->
        StudyCard("c$i", "b", "Frage $i?", "Antwort $i", listOf("Falsch ${i}a", "Falsch ${i}b", "Falsch ${i}c"), null,
            Memory(2.0, 5.0, 0, 3, if (i <= 3) 3 else 0), "🌱")
    }
    private val peers = mapOf("b" to cards.map { it.back })

    private fun show(content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { WissensgartenTheme(ThemeMode.LIGHT) { Surface { content() } } }
        step(800)
    }
    private fun step(ms: Long = 300) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun has(t: String) = rule.onAllNodesWithText(t, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun current() = cards.first { has(it.front) }
    private fun tap(t: String) = rule.onAllNodesWithText(t, substring = false).filter(hasClickAction()).onFirst().performClick()

    @Test fun weedPullByTapsAndWrongAnswer() {
        val right = mutableListOf<String>(); val wrong = mutableListOf<String>(); var closed = false
        show { de.wissensgarten.app.ui.play.WeedScreen(cards.take(3), peers, 4, { right += it.id }, { wrong += it.id }) { closed = true } }
        // 1. richtig + dreimal tippen
        val c1 = current(); tap(c1.back); step()
        assertTrue(has("Nach oben ziehen"))
        repeat(3) { rule.onNodeWithText("Nach oben ziehen", substring = true).performClick(); step(100) }
        assertEquals(listOf(c1.id), right)
        step(1200)
        // 2. falsch → Lösung + Weiter
        val c2 = current(); assertNotEquals(c1, c2)
        tap("Falsch ${c2.id.drop(1)}a"); step()
        assertEquals(listOf(c2.id), wrong)
        assertTrue(has("Weiter"))
        tap("Weiter ▶"); step()
        // 3. richtig + nach oben wischen
        val c3 = current(); tap(c3.back); step()
        rule.onNodeWithText("Nach oben ziehen", substring = true).performTouchInput { swipeUp(startY = bottom + 300f, endY = top - 600f, durationMillis = 300) }
        step(100); step(1200)
        assertEquals(2, right.size)
        assertTrue(has("2 von 3"))
        tap("Zurück in den Garten"); assertTrue(closed)
    }

    @Test fun blitzCountsAndEnds() {
        var result = -1
        show { de.wissensgarten.app.ui.play.BlitzScreen(cards, peers, { result = it }) {} }
        tap("Los geht's! ⚡"); step()
        repeat(5) { tap(current().back); step(700) }
        tap("Falsch ${current().id.drop(1)}b"); step(200)
        assertTrue(has("5 richtig"))
        step(61_000)
        assertEquals(5, result)
        assertTrue(has("richtig! 🌈"))
    }

    @Test fun rhythmAlwaysCountsRightAnswers() {
        var pts = -1; var right = -1
        show { de.wissensgarten.app.ui.play.RhythmScreen(cards, peers, { p, r -> pts = p; right = r }) {} }
        tap("Musik ab! 🎶"); step()
        repeat(Rhythm.QUESTIONS) { tap(current().back); step(200); step(1000) }
        assertEquals(Rhythm.QUESTIONS, right)
        assertTrue(pts >= Rhythm.QUESTIONS * 2)
        assertTrue(has("Punkte! 🎉"))
    }

    @Test fun hiveChoiceShowsImmediately() {
        val saved = mutableMapOf<String, Int>()
        show { de.wissensgarten.app.ui.garden.HiveScreen(60, { saved[it] ?: -1 }, { r, i -> saved[r] = i }) {} }
        rule.onNodeWithText("🍯 Honigtopf").performClick(); step()
        assertTrue(has("Eingerichtet mit 🍯 Honigtopf"))
        assertEquals(1, saved["eingang"])
        // gesperrter Raum: keine Auswahl
        assertFalse(has("🏆 Pokal"))
    }

    @Test fun lociPlaceWalkAndFinish() {
        var placed: Map<Int, String> = emptyMap(); val answers = mutableListOf<Boolean>(); var walked = false
        show {
            de.wissensgarten.app.ui.garden.LociScreen(cards.take(4), cards.associateBy { it.id }, emptyMap(), peers,
                { placed = it }, { _, ok -> answers += ok }, { walked = true }) {}
        }
        rule.onNodeWithText("Brunnen").performClick(); step()
        assertTrue(has("Erst unten eine Karte"))
        rule.onNodeWithText("Antwort 1", substring = true).performClick(); step()
        rule.onNodeWithText("Brunnen").performClick(); step()
        assertTrue(has("Stell dir vor"))
        rule.onNodeWithText("Antwort 2", substring = true).performClick(); step()
        rule.onNodeWithText("Teich").performClick(); step()
        assertEquals(mapOf(2 to "c1", 3 to "c2"), placed)
        rule.onNodeWithText("Spaziergang starten", substring = true).performClick(); step(1200)
        assertTrue(has("Brunnen. Was war hier?"))
        tap("Antwort 1"); step(); step(2000)
        assertTrue(has("Teich. Was war hier?"))
        tap("Falsch 2a"); step(); step(2000)
        assertEquals(listOf(true, false), answers)
        assertTrue(walked)
        assertTrue(has("1 von 2 Orten"))
        rule.onNodeWithText("Orte ändern").performClick(); step()
        assertTrue(has("Spaziergang starten"))
    }

    @Test fun questChestOpens() {
        var opened = false
        show {
            de.wissensgarten.app.ui.garden.QuestCard(listOf(Quests.Quest(Quests.Kind.WATER, 5)), mapOf("WATER" to 5), 10, false) { opened = true }
        }
        rule.onNodeWithText("Öffnen!").performClick(); step()
        assertTrue(opened)
    }

    @Test fun critterTalks() {
        show { de.wissensgarten.app.ui.garden.CrittersCard(0, 8) }
        assertTrue(has("schlafen"))
        rule.onNodeWithText("🦔").performClick(); step()
        assertTrue(has("Ivo raschelt"))
    }

    @Test fun filmPlaysAndShares() {
        var share = false
        val d = Scheduler.DAY_MS; val now = System.currentTimeMillis()
        val frames = GardenFilm.frames((0 until 12).map { GardenFilm.Plant(now - 30 * d + it * d, now - d, 10.0, 3) }, now)
        show { de.wissensgarten.app.ui.share.GardenFilmScreen(frames, "Lena", { share = true }) {} }
        assertTrue(has("Schau, wie alles wächst"))
        step(10_000)
        assertTrue(has("selbst gegossen"))
        rule.onNodeWithText("💌 An Oma schicken").performClick(); step()
        assertTrue(share)
        rule.onNodeWithText("▶ Nochmal abspielen").performClick(); step()
        assertTrue(has("Schau, wie alles wächst"))
    }

    @Test fun restOverlayBlocksGardenUnderneath() {
        var under = 0; var more = false
        show {
            androidx.compose.foundation.layout.Box {
                androidx.compose.material3.Button(onClick = { under++ }) { androidx.compose.material3.Text("Garten-Knopf") }
                de.wissensgarten.app.ui.parents.RestOverlay("Lena") { more = true }
            }
        }
        rule.onNodeWithText("Garten-Knopf").performClick(); step()
        assertEquals(0, under)
        rule.onNodeWithText("15 Minuten", substring = true).performClick(); step()
        assertTrue(more)
    }
}
