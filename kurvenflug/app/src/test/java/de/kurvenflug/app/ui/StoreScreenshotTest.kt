package de.kurvenflug.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.home.HomeScreen
import de.kurvenflug.app.ui.theme.KurvenflugTheme
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.Levels
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Bildschirmfotos für den Play-Store-Eintrag: 1080 × 2160 (Google erlaubt höchstens 2 : 1). Landen in store/screenshots/.
 * Reihenfolge = Reihenfolge im Store.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}, content: @Composable () -> Unit) {
        rule.setContent { KurvenflugTheme { Surface(color = Q.Space) { content() } } }
        rule.mainClock.advanceTimeBy(1500)
        prepare(rule)
        rule.mainClock.advanceTimeBy(settle)
        rule.onRoot().captureRoboImage("../store/screenshots/$name.png")
    }

    private fun level(id: String, values: Map<String, Double>, name: String, settle: Long = 1500, prepare: (ComposeContentTestRule) -> Unit = {}) {
        val l = Levels.byId(id)!!
        shot(name, settle, prepare) { LevelScreen(l, Levels.chapters.first { it.number == l.chapter }, Q.Cyan, false, {}, { _, _, _ -> }, {}, {}, values) }
    }

    private val stars = Levels.chapters.filter { it.number in setOf(1, 15, 8, 2, 16) }.flatMap { it.levels }.associate { it.id to 3 }

    @Test fun s1Home() = shot("01_start") {
        HomeScreen(stars.values.sum(), Levels.all.size * 3, 12, 0, {}, Levels.chapters, stars, {}, {},
            daily = de.kurvenflug.core.model.Daily.forDay(20_500), dailiesDone = 6, grade = 7,
            continueLevel = Levels.byId("13-2"), partsFound = 5, onHangar = {})
    }
    @Test fun s2Trig() = level("14-2", mapOf("angle" to 10.0), "02_trigonometrie")
    @Test fun s3CalcStar() = level("14-2", mapOf("angle" to 13.0), "03_rechen_stern", settle = 3000, prepare = {
        it.onNodeWithText("🚀 Start").performClick(); it.mainClock.advanceTimeBy(5000)
        it.onNodeWithText("tan α = 4 : 17").performScrollTo().performClick()
    })
    @Test fun s4Galaxy() = shot("04_galaxie") {
        HomeScreen(stars.values.sum(), Levels.all.size * 3, 12, 0, {}, Levels.chapters, stars, {}, {},
            grade = 7, continueLevel = Levels.byId("13-2"), initialTab = 1, partsFound = 5, onHangar = {})
    }
    @Test fun s5Integral() = level("21-4", mapOf("A" to 10.0), "05_oberstufe_integral", settle = 5000, prepare = { it.onNodeWithText("🚀 Start").performClick() })
    @Test fun s6Throw() = level("4-6", mapOf("angle" to 35.0, "v" to 5.5), "06_wurf_mond", settle = 1500)
    @Test fun s7Hangar() = shot("07_aurora") {
        de.kurvenflug.app.ui.story.HangarScreen(de.kurvenflug.core.model.Story.found(Levels.chapters, stars), {})
    }
    @Test fun s8Stickers() = shot("08_sticker") {
        de.kurvenflug.app.ui.collection.StickerAlbum(setOf("rocket", "star", "moon", "comet*", "planet", "alien", "robot", "unicorn*", "sat", "s_halloween")) {}
    }
}
