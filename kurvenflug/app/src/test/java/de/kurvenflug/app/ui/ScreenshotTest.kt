package de.kurvenflug.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.kurvenflug.app.ui.flight.IntroOverlay
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.home.HomeScreen
import de.kurvenflug.app.ui.settings.SettingsScreen
import de.kurvenflug.app.ui.settings.UpdateState
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.core.model.Levels
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rendert die Bildschirme ohne Gerät – für das README und zum Prüfen vor jeder Auslieferung. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private val someStars = Levels.all.take(9).mapIndexed { i, l -> l.id to (3 - i % 3) }.toMap() +
        Levels.chapters[1].levels.take(3).associate { it.id to 3 }

    private fun level(id: String, values: Map<String, Double>? = null, prepare: (ComposeContentTestRule) -> Unit = {}, name: String, settle: Long = 1500) {
        val l = Levels.byId(id)!!
        shot(name, settle, prepare) {
            LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, Q.Cyan, false, {}, { _, _, _ -> }, {}, {}, values)
        }
    }

    @Test fun home() = shot("01_start") {
        HomeScreen(someStars.values.sum(), Levels.all.size * 3, 7, 0, {}, Levels.chapters, someStars, {}, {},
            daily = de.kurvenflug.core.model.Daily.forDay(20_500), dailiesDone = 4,
            bossStars = mapOf(1 to 2), onBoss = {}, badges = 3, stormBest = 7, onDuel = {}, onStorm = {}, onBadges = {})
    }

    @Test fun intro() = shot("03_volt_erklaert", prepare = { it.onNodeWithText("Weiter ›").performClick() }) {
        IntroOverlay(Levels.chapters[1]) {}
    }

    @Test fun coordinates() = level("1-2", name = "04_koordinaten", values = mapOf("x" to -5.0, "y" to 0.0))

    @Test fun lineAim() = level("2-7", name = "05_gerade_zielen", values = mapOf("m" to -1.0, "b" to 2.0))

    @Test fun lineMiss() = level("2-6", name = "06_gerade_daneben", values = mapOf("m" to 1.0, "b" to 0.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun lineFlying() = level("2-6", name = "07_gerade_flug", values = mapOf("m" to 0.5, "b" to 1.0), settle = 900,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun parabolaWin() = level("3-6", name = "08_parabel_geschafft", values = mapOf("a" to -0.25, "d" to 0.0, "e" to 5.0), settle = 4500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun throwHelp() = level("4-5", name = "09_wurf_berg", values = mapOf("angle" to 60.0, "v" to 12.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun throwMoon() = level("4-6", name = "10_wurf_mond", values = mapOf("angle" to 35.0, "v" to 5.5), settle = 1500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun settings() = shot("11_einstellungen") {
        SettingsScreen(sound = true, onSound = {}, vibration = true, onVibration = {}, autoUpdate = true, onAutoUpdate = {}, installed = "0.2.0", update = UpdateState.Idle, onBack = {})
    }

    @Test fun sine() = level("5-4", name = "12_sinus", values = mapOf("a" to 2.0, "b" to 1.0, "d" to -1.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun exponential() = level("5-7", name = "13_zerfall", values = mapOf("b" to 0.75))

    @Test fun slingshot() = level("6-4", name = "14_schwerkraft_schleuder", values = mapOf("angle" to 0.0, "v" to 3.5), settle = 1600,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun slingshotHelp() = level("6-3", name = "15_schleuder_ganze_bahn", values = mapOf("angle" to 70.0, "v" to 7.0),
        prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun daily() {
        val l = de.kurvenflug.core.model.Daily.forDay(20_501)
        shot("16_tagesmission") {
            LevelScreen(l, de.kurvenflug.core.model.Chapter(0, "Tagesmission", "Jeden Tag neu", "🛰️", emptyList(), listOf(l)),
                Q.Pink, false, {}, { _, _, _ -> }, null, {})
        }
    }

    @Test fun rendezvous() = level("7-4", name = "17_rendezvous", values = mapOf("x" to 1.5, "y" to 5.5), settle = 1000,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun rendezvousMiss() = level("7-2", name = "18_rendezvous_daneben", values = mapOf("x" to 2.0, "y" to 1.0), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun bossIntro() = shot("19_boss") {
        de.kurvenflug.app.ui.modes.BossScreen(de.kurvenflug.core.model.Bosses.forChapter(3)!!, Q.Cyan, { _, _ -> }, {})
    }

    @Test fun storm() = shot("20_asteroidensturm", prepare = { it.onNodeWithText("Los! 🚀").performClick() }) {
        de.kurvenflug.app.ui.modes.StormScreen(12, Q.Cyan, { false }, {}, seed = 5)
    }

    @Test fun duelMenu() = shot("21_duell") { de.kurvenflug.app.ui.modes.DuelScreen(Q.Cyan, null, {}, {}) }

    @Test fun duelHandover() = shot("22_duell_zu_zweit", prepare = { it.onNodeWithText("Zu zweit an diesem Handy").performClick() }) {
        // fester Startwert, damit das Bild bei jedem Lauf gleich bleibt
        de.kurvenflug.app.ui.modes.DuelScreen(Q.Cyan, null, {}, {}, localSeed = 1_234_567L)
    }

    @Test fun badges() = shot("23_abzeichen") {
        de.kurvenflug.app.ui.modes.BadgesScreen(de.kurvenflug.core.model.Stats(stars = 40, perfect = 12, chaptersDone = 2, cells = 30, stormBest = 11, duelsPlayed = 2), {})
    }

    @Test fun dragMode() = level("3-4", name = "24_kurve_ziehen", values = mapOf("d" to 1.0, "e" to 2.0),
        prepare = { it.onNodeWithText("✋ Ziehen").performClick() })

    @Test fun builder() {
        val spec = de.kurvenflug.core.model.LevelSpec.of(Levels.byId("2-7")!!).copy(title = "Gürtel-Chaos", task = "Schaffst du das?")
        shot("25_baukasten") { de.kurvenflug.app.ui.maker.BuilderScreen(spec, Q.Cyan, {}, {}, {}) }
    }

    @Test fun builderHome() {
        val mine = listOf(de.kurvenflug.core.model.Codec.encodeLevel(de.kurvenflug.core.model.LevelSpec.of(Levels.byId("3-5")!!).copy(title = "Lenas Bogen")))
        shot("26_baukasten_start") { de.kurvenflug.app.ui.maker.BuilderHome(mine, {}, {}, {}, {}, {}, {}, {}) }
    }

    @Test fun challengeComposer() = shot("27_klassen_challenge", prepare = { r ->
        r.onAllNodesWithText("1")[1].performClick(); r.onAllNodesWithText("2")[1].performClick()
    }) { de.kurvenflug.app.ui.maker.ChallengeComposer(emptyList()) {} }

    @Test fun detective() = shot("28_formel_detektiv") {
        de.kurvenflug.app.ui.maker.DetectiveScreen(3, Q.Cyan, {}, {}, seed = 4)
    }

    @Test fun anti() = level("8-5", name = "29_antiproportional", values = mapOf("k" to 12.0), settle = 1200,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun pythagoras() = level("9-3", name = "30_pythagoras", values = mapOf("r" to 7.6), settle = 3500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun laser() = level("11-2", name = "31_laser", values = mapOf("angle" to 4.0), settle = 1500,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun ramp() = level("12-1", name = "32_energie_rampe", values = mapOf("h" to 8.0), settle = 1100,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun log() = level("10-5", name = "33_logarithmus", values = mapOf("b" to 3.0, "d" to -2.0), prepare = { it.onNodeWithText("👁 Bahn").performClick() })

    @Test fun motion() = level("13-3", name = "34_weg_zeit", values = mapOf("v" to 0.5, "s0" to 5.0), settle = 1300,
        prepare = { it.onNodeWithText("🚀 Start").performClick() })

    @Test fun codex() = shot("35_lexikon") { de.kurvenflug.app.ui.collection.CodexScreen(32) {} }

    @Test fun workshop() = shot("36_werkstatt") {
        de.kurvenflug.app.ui.collection.WorkshopScreen(30, 22, 1, Triple(1, 2, 2), {}, { _, _, _ -> }, {})
    }
}
