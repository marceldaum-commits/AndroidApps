package de.raumlicht.app

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hält fest, warum die App zuletzt unerwartet zuging – nur auf dem Gerät, nichts wird verschickt.
 * Zu sehen in den Einstellungen; von dort lässt sich der Bericht kopieren und weitergeben.
 */
object Diagnose {
    private fun file(ctx: Context) = File(ctx.filesDir, "abbruch.txt")
    private fun now() = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date())

    /** Beim Start der App einhängen: fängt Abstürze im App-Code ab, schreibt sie auf und lässt Android dann wie gewohnt weitermachen */
    fun install(ctx: Context) {
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { file(ctx).writeText(report("Absturz im App-Code", "${e.javaClass.name}: ${e.message}\n" + e.stackTraceToString().take(3500))) }
            before?.uncaughtException(t, e)
        }
    }
    /** Etwas Bemerkenswertes festhalten, das kein Absturz war (z. B. Bühne neu gestartet) */
    fun note(ctx: Context, what: String) { runCatching { file(ctx).writeText(report(what, "")) } }
    fun report(title: String, detail: String) = "Raumlicht ${BuildConfig.VERSION_NAME} vom ${BuildConfig.VERSION_DATE} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\n${now()} – $title\n$detail".trim()

    /** In Worten, warum Android den Prozess beendet hat (nur die Fälle, die auf ein Problem hindeuten) */
    fun reasonText(reason: Int): String? = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "Absturz im App-Code"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "Absturz in einer Maschinencode-Bibliothek (3D-Bühne oder Sprachausgabe)"
        ApplicationExitInfo.REASON_ANR -> "App hat zu lange nicht reagiert"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "Vom System beendet: zu wenig Arbeitsspeicher"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Vom System beendet: zu hoher Verbrauch"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "App konnte nicht starten"
        ApplicationExitInfo.REASON_SIGNALED -> "Vom System abgebrochen"
        else -> null
    }

    /** Der letzte unerwartete Abbruch als Text – oder null, wenn alles in Ordnung war */
    fun last(ctx: Context): String? {
        val own = runCatching { file(ctx).takeIf { it.isFile }?.readText() }.getOrNull()
        val system = if (Build.VERSION.SDK_INT >= 30) runCatching {
            ctx.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(ctx.packageName, 0, 5).firstNotNullOfOrNull { i ->
                reasonText(i.reason)?.let { r -> "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(i.timestamp))} – $r${i.description?.let { d -> " ($d)" } ?: ""}" } }
        }.getOrNull() else null
        return listOfNotNull(own, system?.let { "Android meldet: $it" }).joinToString("\n\n").ifBlank { null }
    }
    fun clear(ctx: Context) { runCatching { file(ctx).delete() } }
}
