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
import de.raumlicht.core.model.Area
import de.raumlicht.core.model.Levels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Startbildschirm-Widget „Lernstand“: drei Balken für Geometrie, Analysis und Stochastik (Anteil der erreichten Sterne). */
class LearnWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stars = (context.applicationContext as RaumlichtApp).container.progress.all.first().associate { it.levelId to it.stars }
                ids.forEach { manager.updateAppWidget(it, views(context, percents(stars))) }
            } finally { pending.finish() }
        }
    }

    companion object {
        /** Prozent der möglichen Sterne je Gebiet, in der Reihenfolge von [Area] */
        fun percents(stars: Map<String, Int>): List<Int> = Area.entries.map { a ->
            val levels = Levels.chapters.filter { it.area == a }.flatMap { it.levels }
            if (levels.isEmpty()) 0 else levels.sumOf { stars[it.id] ?: 0 } * 100 / (levels.size * 3)
        }

        fun views(context: Context, p: List<Int>): RemoteViews = RemoteViews(context.packageName, R.layout.widget_learn).apply {
            listOf(R.id.learn_bar_0 to R.id.learn_value_0, R.id.learn_bar_1 to R.id.learn_value_1, R.id.learn_bar_2 to R.id.learn_value_2).forEachIndexed { i, (bar, value) ->
                setProgressBar(bar, 100, p[i], false); setTextViewText(value, "${p[i]} %")
            }
            val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            setOnClickPendingIntent(R.id.learn_root, PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, LearnWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, LearnWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}
