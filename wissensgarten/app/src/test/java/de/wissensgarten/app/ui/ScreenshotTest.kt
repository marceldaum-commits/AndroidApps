package de.wissensgarten.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import de.wissensgarten.app.data.Grade
import de.wissensgarten.app.data.GardenState
import de.wissensgarten.app.data.Scheduler
import de.wissensgarten.app.data.Session
import de.wissensgarten.app.data.StarterBeds
import de.wissensgarten.app.ui.theme.WissensgartenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rendert die Bildschirme mit einem „gelebten“ Garten, damit man das Design ohne Gerät sieht. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val now = 1_763_000_000_000L
    private val day = Scheduler.DAY_MS

    /** Garten mit Pflanzen in allen Stufen, manche durstig. */
    private fun demo(): GardenState {
        val beds = StarterBeds.all().mapIndexed { bi, bed ->
            bed.copy(cards = bed.cards.mapIndexed { i, c ->
                if (i >= 10 - bi * 3) return@mapIndexed c // Rest bleibt Samen
                var card = Scheduler.review(c, Grade.GOOD, now - 400 * day)
                var t = now - 400 * day
                repeat(i % 6) { t = Scheduler.dueAt(card); if (t < now) card = Scheduler.review(card, Grade.GOOD, t) }
                // jede dritte Pflanze lange nicht gegossen → welk
                if (i % 3 == 1) card = card.copy(lastReview = now - (card.stability * 4 * day).toLong())
                else card = card.copy(lastReview = now - (card.stability * 0.3 * day).toLong())
                card
            })
        }
        return GardenState(beds = beds, dewdrops = 128)
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { WissensgartenTheme(dark = dark) { androidx.compose.material3.Surface { content() } } }
        rule.mainClock.advanceTimeBy(2000)
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    @Test fun garden() = shot("1_garten") {
        GardenScreen(demo(), now, {}, {}, { _, _ -> })
    }

    @Test fun gardenDark() = shot("2_garten_dunkel", dark = true) {
        GardenScreen(demo(), now, {}, {}, { _, _ -> })
    }

    @Test fun bed() = shot("3_beet") {
        val s = demo()
        BedScreen(s.beds[0], now, {}, {}, { _, _ -> }, {}, {})
    }

    @Test fun review() = shot("4_giessen") {
        val s = demo()
        ReviewScreen(Session.build(s, now), s, { _, _ -> }, {})
    }
}
