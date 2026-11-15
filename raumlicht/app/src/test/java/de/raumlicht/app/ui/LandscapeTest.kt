package de.raumlicht.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.stage.LocalStageRenderer
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Querformat (Tablet oder gedrehtes Handy): Bühne links groß, Formel und Regler rechts – und alles bleibt bedienbar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land-xhdpi")
class LandscapeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun levelInLandscapePutsStageBesideControls() {
        val l = Levels.byId("5-1")!!; var solved = 0
        rule.setContent { RaumlichtTheme { Surface(color = Q.Space) { CompositionLocalProvider(LocalStageRenderer provides { _, m ->
            val bmp = javaClass.classLoader!!.getResourceAsStream("stage/5-1-fertig.png")?.use { BitmapFactory.decodeStream(it) }
            if (bmp != null) Image(bmp.asImageBitmap(), null, m, contentScale = ContentScale.Crop) else Box(m)
        }) { LevelScreen(l, Levels.chapters.first { it.number == 5 }, false, {}, { _, _ -> solved++ }, {}, {}, l.solution) } } } }
        rule.mainClock.advanceTimeBy(1500)
        val go = rule.onNodeWithText("▶ Los").fetchSemanticsNode().boundsInRoot
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Regler stehen rechts neben der Bühne", go.left > root.width * 0.5f && go.bottom <= root.bottom)
        rule.onNodeWithText("▶ Los").performClick(); rule.mainClock.advanceTimeBy(5000)
        assertTrue(solved == 1)
        rule.onRoot().captureRoboImage("../screenshots/48_querformat.png")
    }
}
