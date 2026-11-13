package de.wissensgarten.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.wissensgarten.app.MainActivity
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.SessionBuilder
import kotlinx.coroutines.flow.first

/** Startbildschirm-Widget: zeigt, wie viele Pflanzen Durst haben. Antippen startet das Gießen. */
class GardenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = (context.applicationContext as WissensgartenApp).container
        val now = System.currentTimeMillis()
        val profiles = c.repository.profiles.first()
        val profile = profiles.firstOrNull { it.id == c.settings.activeProfileId.value } ?: profiles.firstOrNull()
        val cards = profile?.let { p -> c.database.dao().bedsWithCardsOnce(p.id).flatMap { it.cards } }.orEmpty()
        val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
        val seeds = profile?.let { p ->
            minOf(SessionBuilder.seedsLeft(p.seedsPerDay, p.seedDay, p.seedsToday, now), cards.count { it.memory.isSeed })
        } ?: 0
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WATER, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        provideContent { GlanceTheme { Content(thirsty, seeds, intent) } }
    }

    @Composable
    private fun Content(thirsty: Int, seeds: Int, intent: Intent) {
        val bg = if (thirsty > 0) Color(0xFFFFE3CF) else Color(0xFFDCEFD0)
        val fg = if (thirsty > 0) Color(0xFF5A2A0A) else Color(0xFF1E3D17)
        Column(
            modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(bg).padding(12.dp)
                .clickable(actionStartActivity(intent)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (thirsty > 0) "🥀" else "🌿", style = TextStyle(fontSize = 28.sp))
            Text(
                if (thirsty > 0) "$thirsty durstig" else "Alles grün",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(fg)),
            )
            Text(
                if (thirsty > 0) "Tippen zum Gießen" else if (seeds > 0) "$seeds Samen bereit" else "Bis morgen!",
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(fg)),
            )
        }
    }
}

class GardenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GardenWidget()
}
