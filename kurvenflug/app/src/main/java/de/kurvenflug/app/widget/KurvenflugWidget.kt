package de.kurvenflug.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.kurvenflug.app.KurvenflugApp
import de.kurvenflug.app.MainActivity
import de.kurvenflug.core.model.Played
import de.kurvenflug.core.model.WidgetInfo
import kotlinx.coroutines.flow.first

private val Space = Color(0xFF12163A)
private val Panel = Color(0xFF232A5C)
private val Cyan = Color(0xFF3DE8FF)
private val Green = Color(0xFF4CF59A)
private val Violet = Color(0xFFB18CFF)
private val Muted = Color(0xFFA9B0D6)
private val White = Color(0xFFF2F4FF)

/** Öffnet die App direkt in einem Level (Tagesmission, Auffrischen, nächstes Level). */
fun levelIntent(context: Context, id: String?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        // eigene Adresse je Ziel, sonst verwechselt Android die Knöpfe
        data = Uri.parse("kurvenflug://" + (id?.let { "level/$it" } ?: "start"))
    }

/**
 * Startbildschirm-Widget: Volt sagt, was heute dran ist; Knöpfe für Tagesmission und „Weiter geht's“,
 * darunter das fällige Auffrischen. Klein (2×2) zeigt nur Sterne und „Weiter“.
 * [fixed] nur für Bildschirmfotos – sonst wird der Spielstand vom Gerät gelesen.
 */
class KurvenflugWidget(private val fixed: WidgetInfo? = null) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(250.dp, 120.dp), DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val info = fixed ?: load(context)
        provideContent { Content(context, info) }
    }

    private suspend fun load(context: Context): WidgetInfo {
        val c = (context.applicationContext as KurvenflugApp).container
        val all = c.progress.all.first()
        val played = all.associate { it.levelId to Played(it.stars, it.solvedAt) }
        val cells = all.sumOf { Integer.bitCount(it.cellMask) }
        return WidgetInfo.of(played, cells, de.kurvenflug.core.model.CalDate.today().toEpochDay(), System.currentTimeMillis(), c.settings.grade.value)
    }

    @Composable
    private fun Content(context: Context, w: WidgetInfo) {
        val size = LocalSize.current
        val small = size.width < 200.dp
        val tall = size.height >= 170.dp
        fun open(id: String?): Action = actionStartActivity(levelIntent(context, id))
        Column(
            GlanceModifier.fillMaxSize().background(Space).cornerRadius(22.dp).padding(12.dp).clickable(open(null)),
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text("🤖", style = TextStyle(fontSize = if (small) 22.sp else 28.sp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text("KURVENFLUG", style = TextStyle(color = ColorProvider(Cyan), fontWeight = FontWeight.Bold, fontSize = 13.sp))
                    Text(if (small) "⭐ ${w.stars}  ⚡ ${w.cells}" else w.volt, maxLines = 2,
                        style = TextStyle(color = ColorProvider(White), fontSize = 12.sp))
                }
                if (!small) Text("⭐ ${w.stars}\n⚡ ${w.cells}", style = TextStyle(color = ColorProvider(Color(0xFFFFD84D)), fontWeight = FontWeight.Bold, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (small) {
                Button("🚀 Weiter", w.nextTitle ?: "Alles geschafft!", Cyan, open(w.nextId), GlanceModifier.fillMaxWidth())
            } else {
                Row(GlanceModifier.fillMaxWidth()) {
                    Button(if (w.dailyDone) "✅ Tagesmission" else "🛰️ Tagesmission", if (w.dailyDone) "heute geschafft" else w.dailyLabel,
                        if (w.dailyDone) Green else Violet, open(w.dailyId), GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    Button("🚀 Weiter", w.nextTitle ?: "Alles geschafft!", Cyan, open(w.nextId), GlanceModifier.defaultWeight())
                }
                if (tall && w.reviewId != null) {
                    Spacer(GlanceModifier.height(8.dp))
                    Button("🔁 Auffrischen", w.reviewTitle ?: "", Green, open(w.reviewId), GlanceModifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    private fun Button(title: String, sub: String, color: Color, action: Action, modifier: GlanceModifier) {
        Column(modifier.background(Panel).cornerRadius(16.dp).padding(horizontal = 10.dp, vertical = 8.dp).clickable(action)) {
            Text(title, maxLines = 1, style = TextStyle(color = ColorProvider(color), fontWeight = FontWeight.Bold, fontSize = 14.sp))
            Text(sub, maxLines = 1, style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp))
        }
    }

    companion object {
        /** Nach jedem Level aufrufen, damit das Widget gleich stimmt. */
        suspend fun refresh(context: Context) = runCatching { KurvenflugWidget().updateAll(context) }
    }
}

class KurvenflugWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KurvenflugWidget()
}
