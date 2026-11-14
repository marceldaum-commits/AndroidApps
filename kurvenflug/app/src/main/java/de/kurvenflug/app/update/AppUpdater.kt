package de.kurvenflug.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import de.kurvenflug.core.model.AppRelease
import de.kurvenflug.core.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Holt neue Versionen aus GitHub Releases und übergibt sie an den Android-Installer.
 * Nur für Installationen außerhalb des Play Stores – dort übernimmt der Store die Updates.
 * Kontakt zu GitHub nur auf Knopfdruck; es werden keine Daten gesendet außer der normalen Anfrage.
 */
object AppUpdater {
    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000; readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "Kurvenflug-App")
    }

    /** Neueste veröffentlichte Version (oder Fehler). */
    suspend fun check(): Result<AppRelease?> = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(Updates.API)
            if (c.responseCode != 200) error("GitHub antwortet mit ${c.responseCode}")
            Updates.latest(c.inputStream.bufferedReader().use { it.readText() })
        }
    }

    private fun apkFile(ctx: Context) = File(ctx.cacheDir, "updates").apply { mkdirs() }.resolve("kurvenflug-update.apk")

    /** Lädt die APK herunter; [onProgress] bekommt 0..1. */
    suspend fun download(ctx: Context, r: AppRelease, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val f = apkFile(ctx)
            var c = open(r.apkUrl)
            // GitHub leitet auf einen Download-Server um – Weiterleitungen notfalls von Hand folgen
            var hops = 0
            while (c.responseCode in 300..399 && hops++ < 5) c = open(c.getHeaderField("Location"))
            if (c.responseCode != 200) error("Download fehlgeschlagen (${c.responseCode})")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: r.sizeBytes
            c.inputStream.use { input ->
                f.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024); var done = 0L
                    while (true) {
                        coroutineContext.ensureActive()   // Bildschirm verlassen → Download abbrechen
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            f
        }
    }

    /** Darf die App Updates installieren? (Android fragt das einmalig ab) */
    fun canInstall(ctx: Context) = ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Öffnet den Android-Installer. Updates behalten alle Daten (gleiche Signatur). */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
