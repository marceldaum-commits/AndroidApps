package de.raumlicht.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.data.Settings
import de.raumlicht.core.model.Levels
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Der gemeinsame App-Rahmen (derselbe für Android und Web): Wege durch die App, „zurück“, Sprünge von außen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class AppRootTest {
    @get:Rule val rule = createComposeRule()
    private val nav = Navigator(); private val stars = MutableStateFlow(mapOf<String, Int>()); private val routes = ArrayList<String>()
    private fun start() {
        val prefs = Settings(ApplicationProvider.getApplicationContext())
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            AppRoot(nav, prefs, stars, { id, s, _ -> stars.value = stars.value + (id to s) }, { 20_772L }, settings = { onBack -> androidx.compose.material3.Text("EINSTELLUNGEN", androidx.compose.ui.Modifier.testTag("einst")) },
                onRoute = { routes += it })
        } } } }
        rule.mainClock.advanceTimeBy(600)
    }
    private fun settle() { rule.mainClock.advanceTimeBy(700); rule.waitForIdle() }

    @Test fun walksThroughTheAppAndBack() {
        start()
        rule.onNodeWithTag("gebiet-GEOMETRIE").assertExists(); assertEquals(listOf(Routes.HOME), nav.stack)
        nav.go(Routes.level("1-1")); settle()
        rule.onNodeWithText(Levels.byId("1-1")!!.title, substring = true).assertExists()
        nav.go(Routes.level("1-1")); assertEquals(2, nav.stack.size)                                   // dasselbe Ziel zweimal: nur einmal offen
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000)); assertTrue(nav.back()); assertFalse(nav.back()); settle()                       // zweimal schnell „zurück“: nur eine Ebene
        rule.onNodeWithTag("gebiet-GEOMETRIE").assertExists()
        nav.go(Routes.BACKSTAGE); settle(); rule.onNodeWithText("Ballerbude").performScrollTo().performClick(); settle()
        assertEquals(listOf(Routes.HOME, Routes.BACKSTAGE, Routes.BALLOONS), nav.stack); rule.onNodeWithTag("ballern").assertExists()
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("einst").assertExists()
        nav.restart(Routes.DAILY); settle()                                                               // Sprung von außen (Widget): Startseite darunter
        assertEquals(listOf(Routes.HOME, Routes.DAILY), nav.stack); rule.onNodeWithText("Tagesaufgabe", substring = true).assertExists()
        assertTrue(routes.containsAll(listOf(Routes.HOME, "level/1-1", Routes.BACKSTAGE, Routes.BALLOONS, Routes.SETTINGS, Routes.DAILY)))
    }

    @Test fun everyDestinationOpens() {
        start()
        val all = listOf(Routes.DAILY, Routes.JAM_HOME, Routes.jam("MIX"), Routes.SHEET, Routes.BADGES, Routes.BACKSTAGE, Routes.EXAMS, Routes.exam("zelt", false), Routes.LAGE, Routes.DUEL, Routes.TRAINING,
            Routes.WORKSHOP, Routes.BUILDER, Routes.GUIDES, Routes.guide(de.raumlicht.core.model.Guides.all.first().id), Routes.LEARN, Routes.PLANNER, Routes.CLASS, Routes.MATCH, Routes.ride("huegel"), Routes.FLIGHT,
            Routes.GALTON, Routes.BALLOONS, Routes.CHANCE, Routes.BUILD, Routes.GHOST, Routes.RINGS, Routes.QUICK, Routes.DIARY, Routes.CALENDAR, Routes.level("16-3"))
        all.forEach { r -> nav.restart(r); settle(); assertEquals(r, listOf(Routes.HOME, r), nav.stack); rule.onNodeWithTag("gebiet-GEOMETRIE").assertDoesNotExist() }
        nav.restart("gibt/es/nicht"); settle(); settle(); assertEquals(listOf(Routes.HOME), nav.stack)       // unbekanntes Ziel: zurück zur Startseite
        assertTrue(Routes.isAction("level/3-2") && Routes.isAction("jam/MIX") && Routes.isAction("abi/zelt/1") && !Routes.isAction(Routes.HOME) && !Routes.isAction(Routes.EXAMS) && !Routes.isAction(Routes.BACKSTAGE))
    }
}
