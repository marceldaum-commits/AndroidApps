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
import com.github.takahirom.roborazzi.captureRoboImage
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
    private lateinit var prefs: Settings
    // Schlüssel und Code nur für Tests (Seed 0…31)
    private val PK = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val PASS = "RL-04202-0000F-MGKKT-CDRGB-BYPG5-8XEVQ-M0F02-94P76-M1MXZ-28QYX-86PN1-NEY0T-JNXA5-8S0GJ-HA3X2-C0GQW-AW2K1-BQN3M-4PV5F-ZHQBG-ZKMPE-KJSDT-6VARJ-0W"
    private fun start(pro: Boolean = false) {
        prefs = Settings(ApplicationProvider.getApplicationContext())
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            AppRoot(nav, prefs, stars, { id, s, _ -> stars.value = stars.value + (id to s) }, { 20_772L }, settings = { onBack -> androidx.compose.foundation.layout.Column { androidx.compose.material3.Text("EINSTELLUNGEN", androidx.compose.ui.Modifier.testTag("einst")); de.raumlicht.app.ui.settings.ProEntry() } },
                onRoute = { routes += it }, proEnabled = pro, proKey = PK)
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

    /** Abi-Pass: eingeschaltet sperrt er genau die Pro-Teile und zeigt die Abi-Pass-Seite; ein gültiger Code öffnet alles, ein falscher nichts. */
    @Test fun proGate() {
        Settings(ApplicationProvider.getApplicationContext()).setProCode("")
        start(pro = true)
        rule.onNodeWithTag("kapitel-7").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("schloss-7-3", useUnmergedTree = true).assertExists(); rule.onNodeWithTag("schloss-7-2", useUnmergedTree = true).assertDoesNotExist()
        nav.go(Routes.level("7-2")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertTextContains("Aufgabe 7-3", substring = true); rule.onNodeWithTag("pro-status").assertExists()
        rule.onNodeWithTag("pro-zurueck").performScrollTo().performClick(); settle(); assertEquals(listOf(Routes.HOME), nav.stack)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.BINOMIAL); settle(); rule.onNodeWithTag("pro-grund").assertTextContains("Binomial-Labor", substring = true); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.CHANCE); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist(); nav.back(); settle()            // Zufallsmaschine bleibt frei
        // über die Einstellungen zur Abi-Pass-Seite: Kauf-Knopf und Code-Eingabe
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("pro-eintrag").assertTextContains("Alles freischalten", substring = true).performClick(); settle()
        assertEquals(Routes.PRO, nav.top); rule.onNodeWithTag("pro-kaufen").assertExists()
        // falscher Code: freundlicher Hinweis, nichts geht auf
        rule.onNodeWithTag("pro-code").performScrollTo().performTextInput("RL-12345-ABCDE"); rule.onNodeWithTag("pro-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("pro-grund").assertTextContains("erkenne ich nicht", substring = true); rule.onNodeWithTag("pro-aktiv").assertDoesNotExist(); assertEquals("", prefs.proCode.value)
        rule.onRoot().captureRoboImage("../screenshots/76_abi_pass.png")
        // richtiger Code, mitten aus einer E-Mail kopiert
        rule.onNodeWithTag("pro-code").performTextClearance(); rule.onNodeWithTag("pro-code").performTextInput("Hallo! Dein Code:\n$PASS\nViel Erfolg!")
        rule.onNodeWithTag("pro-freischalten").performScrollTo().performClick(); settle()
        rule.onNodeWithTag("pro-aktiv").assertExists(); rule.onNodeWithTag("pro-kaufen").assertDoesNotExist(); assertEquals(PASS, prefs.proCode.value)
        rule.onRoot().captureRoboImage("../screenshots/77_abi_pass_frei.png")
        rule.onNodeWithTag("pro-zurueck").performScrollTo().performClick(); settle(); rule.onNodeWithTag("pro-eintrag").assertTextContains("Freigeschaltet", substring = true)
        // freigeschaltet: alles offen, Schlösser weg – auch nach einem Neustart (der Code wird neu geprüft)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist()
        assertEquals(PASS, Settings(ApplicationProvider.getApplicationContext()).proCode.value)
        rule.runOnUiThread { prefs.setProCode("") }
    }

    /** Ein gespeicherter Code, der nicht (mehr) zum Schlüssel passt, schaltet nichts frei */
    @Test fun fremderCodeSchaltetNichtsFrei() {
        Settings(ApplicationProvider.getApplicationContext()).setProCode(PASS.replace("MGKKT", "MGKKV"))
        start(pro = true)
        nav.go(Routes.level("7-3")); settle(); rule.onNodeWithTag("pro-grund").assertExists()
        rule.runOnUiThread { prefs.setProCode("") }
    }

    /** Die Schreibweise lässt sich in den Einstellungen umstellen und wird gemerkt; die Formelsammlung folgt ihr */
    @Test fun schreibweiseUmstellen() {
        Settings(ApplicationProvider.getApplicationContext()).setStrictVectors(false)
        prefs = Settings(ApplicationProvider.getApplicationContext())
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m -> Box(m) }) {
            AppRoot(nav, prefs, stars, { _, _, _ -> }, { 20_772L }, settings = { onBack -> de.raumlicht.app.ui.settings.SettingsPage(sound = true, onSound = {}, vibration = true, onVibration = {}, versionText = "Test", onBack = onBack) })
        } } } }
        rule.mainClock.advanceTimeBy(600)
        nav.go(Routes.SHEET); settle(); rule.onNodeWithText("Verbindungsvektor: AB⃗ = B − A (Spitze minus Fuß)").assertExists(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithText("📐 Strenge Vektorschreibweise").performScrollTo().performClick(); settle()
        assertTrue(prefs.strictVectors.value); assertTrue(Settings(ApplicationProvider.getApplicationContext()).strictVectors.value)
        // Vorlesen: Sprechtempo einstellbar und gemerkt, Hörprobe vorhanden
        rule.onNodeWithTag("sprechtempo-text").performScrollTo().assertTextEquals("🐢🐇 Sprechtempo: 100 %"); rule.onNodeWithTag("hoerprobe").assertExists()
        rule.runOnUiThread { prefs.setSpeechRate(120) }; settle(); rule.onNodeWithTag("sprechtempo-text").assertTextEquals("🐢🐇 Sprechtempo: 120 %")
        assertEquals(120, Settings(ApplicationProvider.getApplicationContext()).speechRate.value); rule.runOnUiThread { prefs.setSpeechRate(100) }
        rule.onNodeWithText("An: mit Ortsvektoren", substring = true).assertExists(); nav.back(); settle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SHEET); settle(); rule.onNodeWithText("Verbindungsvektor: AB⃗ = OB⃗ − OA⃗ (Spitze minus Fuß)").assertExists()
        rule.runOnUiThread { prefs.setStrictVectors(false) }
    }

    @Test fun proOffMeansEverythingOpen() {
        start()                                                                                                             // Auslieferungsstand
        nav.go(Routes.level("25-7")); settle(); rule.onNodeWithTag("pro-grund").assertDoesNotExist()
        rule.onNodeWithText(Levels.byId("25-7")!!.title, substring = true).assertExists()
        // kein Eintrag in den Einstellungen, kein Wort von einem Kauf
        nav.back(); settle(); org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1000))
        nav.go(Routes.SETTINGS); settle(); rule.onNodeWithTag("einst").assertExists(); rule.onNodeWithTag("pro-eintrag").assertDoesNotExist()
    }
}
