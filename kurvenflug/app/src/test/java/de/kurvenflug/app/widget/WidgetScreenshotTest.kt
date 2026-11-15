package de.kurvenflug.app.widget

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import de.kurvenflug.core.model.Learning
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.Played
import de.kurvenflug.core.model.WidgetInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rendert das Widget so, wie Android es auf dem Startbildschirm zeichnet (RemoteViews) – fürs README. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class WidgetScreenshotTest {
    private val day = 20_500L
    private val now = day * Learning.DAY_MS + 3_600_000L
    private val info = WidgetInfo.of(
        Levels.chapters.first { it.number == 2 }.levels.take(5).associate { it.id to Played(2, now - 3 * Learning.DAY_MS) },
        7, day, now, 7,
    )

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun render(name: String, w: Int, h: Int) = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val rv = KurvenflugWidget(info).compose(ctx, size = DpSize(w.dp, h.dp))
        val density = ctx.resources.displayMetrics.density
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Color.rgb(40, 52, 70)) }
        val view: View = rv.apply(ctx, frame)
        frame.addView(view, FrameLayout.LayoutParams((w * density).toInt(), (h * density).toInt()))
        val pad = (16 * density).toInt()
        frame.setPadding(pad, pad, pad, pad)
        val spec = View.MeasureSpec.makeMeasureSpec(((w + 32) * density).toInt(), View.MeasureSpec.EXACTLY)
        val hspec = View.MeasureSpec.makeMeasureSpec(((h + 32) * density).toInt(), View.MeasureSpec.EXACTLY)
        frame.measure(spec, hspec); frame.layout(0, 0, frame.measuredWidth, frame.measuredHeight)
        val bmp = android.graphics.Bitmap.createBitmap(frame.measuredWidth, frame.measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
        frame.draw(android.graphics.Canvas(bmp))
        java.io.File("../screenshots/$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun wide() {
        assertTrue(info.reviewId != null && !info.dailyDone)
        render("47_widget", 320, 190)
    }

    @Test fun small() { render("48_widget_klein", 150, 150) }
}
