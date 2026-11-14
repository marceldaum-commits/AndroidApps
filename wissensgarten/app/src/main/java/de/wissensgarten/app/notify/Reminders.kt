package de.wissensgarten.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import de.wissensgarten.app.MainActivity
import de.wissensgarten.app.R
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.SessionBuilder
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Tägliche Gieß-Erinnerung. Meldet sich nur, wenn wirklich etwas zu tun ist –
 * keine Benachrichtigung, wenn alles grün ist.
 */
object Reminders {
    private const val WORK = "giess-erinnerung"
    const val CHANNEL = "giessen"
    private const val NOTIFICATION_ID = 17

    fun schedule(context: Context, hour: Int, replace: Boolean = true) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(hour, System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK)

    /** Millisekunden bis zur nächsten vollen Stunde [hour]. */
    fun delayUntil(hour: Int, now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis - now
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Gieß-Erinnerung", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Einmal am Tag, wenn Pflanzen Durst haben" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Text für die Erinnerung, oder null wenn nichts zu tun ist. */
    data class Message(val title: String, val text: String)

    fun message(entries: List<Triple<String, Int, Int>>): Message? {
        // entries: Name, durstig, Samen bereit
        val thirsty = entries.filter { it.second > 0 }
        if (thirsty.isNotEmpty()) {
            val total = thirsty.sumOf { it.second }
            val text = if (entries.size == 1) "$total ${if (total == 1) "Pflanze wartet" else "Pflanzen warten"} aufs Gießen. Ein paar Minuten reichen!"
            else thirsty.joinToString(" · ") { "${it.first}: ${it.second}" } + " – jetzt gießen!"
            return Message(if (total == 1) "💧 Eine Pflanze freut sich auf Wasser" else "💧 $total Pflanzen freuen sich auf Wasser", text)
        }
        val seeds = entries.filter { it.third > 0 }
        if (seeds.isNotEmpty()) {
            return Message("🌱 Neue Samen warten", if (entries.size == 1) "Säe heute ein paar neue – dein Garten wächst!" else seeds.joinToString(" · ") { it.first } + ": heute noch säen?")
        }
        return null
    }

    fun show(context: Context, m: Message) {
        if (!canNotify(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WATER, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_leaf)
            .setColor(0xFF3F6B3A.toInt())
            .setContentTitle(m.title)
            .setContentText(m.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(m.text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n) } catch (_: SecurityException) { }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as WissensgartenApp).container
        if (!c.settings.reminderOn.value) return Result.success()
        val now = System.currentTimeMillis()
        val dao = c.database.dao()
        val entries = c.repository.profiles.first().map { p ->
            val cards = dao.bedsWithCardsOnce(p.id).flatMap { it.cards }
            val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
            val seeds = minOf(SessionBuilder.seedsLeft(p.seedsPerDay, p.seedDay, p.seedsToday, now), cards.count { it.memory.isSeed })
            // Samen nur erwähnen, wenn heute noch nichts gesät wurde
            val today = Scheduler.localDay(now)
            Triple(p.name, thirsty, if (p.seedDay == today && p.seedsToday > 0) 0 else seeds)
        }
        Reminders.message(entries)?.let { Reminders.show(applicationContext, it) }
        return Result.success()
    }
}
