package de.raumlicht.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.raumlicht.app.MainActivity
import de.raumlicht.app.R
import de.raumlicht.app.RaumlichtApp
import de.raumlicht.core.model.Daily
import de.raumlicht.core.model.Levels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Was das Widget zeigt – getrennt vom Android-Teil, damit es sich testen lässt. */
data class WidgetText(val title: String, val status: String, val stars: String, val done: Boolean, val caption: String = "RAUMLICHT · TAGESAUFGABE")

/**
 * Startbildschirm-Widget „Tagesaufgabe“: heutige Aufgabe, ob sie schon geschafft ist, und die Sterne der Tour.
 * Ein Tipp öffnet den Tagesaufgabe. Das Widget liest nur den Spielstand auf dem Gerät – kein Netz, keine Daten nach außen.
 */
class DailyWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stars = (context.applicationContext as RaumlichtApp).container.progress.all.first().associate { it.levelId to it.stars }
                val text = texts(java.time.LocalDate.now().toEpochDay(), stars, de.raumlicht.core.model.Plan.decode((context.applicationContext as RaumlichtApp).container.settings.plan.value))
                ids.forEach { manager.updateAppWidget(it, views(context, text)) }
            } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_OPEN = "raumlicht.open"
        const val OPEN_DAILY = "tagesaufgabe"

        fun texts(day: Long, stars: Map<String, Int>, plan: de.raumlicht.core.model.Plan? = null): WidgetText {
            val level = Daily.forDay(day); val got = stars[level.id] ?: 0
            val tour = Levels.all.sumOf { stars[it.id] ?: 0 }
            return WidgetText(level.title, if (got > 0) "heute geschafft ✓  " + "★".repeat(got) else "heute neu – antippen ▶", "★ $tour", got > 0,
                plan?.let { de.raumlicht.core.model.Planner.headline(it, day, stars) }?.uppercase() ?: "RAUMLICHT · TAGESAUFGABE")
        }

        fun views(context: Context, t: WidgetText): RemoteViews = RemoteViews(context.packageName, R.layout.widget_daily).apply {
            setTextViewText(R.id.widget_title, t.title)
            setTextViewText(R.id.widget_caption, t.caption)
            setTextViewText(R.id.widget_status, t.status)
            setTextColor(R.id.widget_status, if (t.done) 0xFF5CFFA1.toInt() else 0xFFFF8A3D.toInt())
            setTextViewText(R.id.widget_emoji, if (t.done) "✅" else "🔦")
            setTextViewText(R.id.widget_stars, t.stars)
            val open = Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN, OPEN_DAILY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }

        /** Nach jedem gelösten Level aufrufen, damit das Widget sofort stimmt. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, DailyWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, DailyWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}
