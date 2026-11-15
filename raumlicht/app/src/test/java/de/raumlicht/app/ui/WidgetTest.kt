package de.raumlicht.app.ui

import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.raumlicht.app.R
import de.raumlicht.app.widget.DailyWidget
import de.raumlicht.core.model.Daily
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class WidgetTest {
    private val day = 20_772L

    @Test fun textsFollowTheDailyAndProgress() {
        val open = DailyWidget.texts(day, mapOf("1-1" to 3, "1-2" to 2))
        assertEquals(Daily.forDay(day).title, open.title); assertFalse(open.done); assertEquals("★ 5", open.stars)
        assertEquals("heute neu – antippen ▶", open.status)
        val done = DailyWidget.texts(day, mapOf(Daily.id(day) to 3, "1-1" to 3))
        assertTrue(done.done); assertEquals("heute geschafft ✓  ★★★", done.status); assertEquals("★ 3", done.stars)   // Tagesgigs zählen nicht zu den Tour-Sternen
        assertFalse(DailyWidget.texts(day + 1, mapOf(Daily.id(day) to 3)).done)                                          // am nächsten Tag wieder offen
    }

    @Test fun remoteViewsRenderAndOpenTheDaily() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = FrameLayout(ctx)
        val v = DailyWidget.views(ctx, DailyWidget.texts(day, mapOf("1-1" to 3, "1-2" to 3, "2-1" to 2))).apply(ctx, root)
        assertEquals(Daily.forDay(day).title, v.findViewById<TextView>(R.id.widget_title).text.toString())
        assertTrue(v.findViewById<android.view.View>(R.id.widget_root).hasOnClickListeners())
        // Bild fürs README: so sieht das Widget auf dem Startbildschirm aus (4 × 1 Felder)
        root.setBackgroundColor(0xFF20242E.toInt()); root.setPadding(36, 36, 36, 36); root.addView(v)
        val activity = org.robolectric.Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        activity.setContentView(root, android.view.ViewGroup.LayoutParams(1080, 300))
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        root.captureRoboImage("../screenshots/37_widget.png")
    }
}
