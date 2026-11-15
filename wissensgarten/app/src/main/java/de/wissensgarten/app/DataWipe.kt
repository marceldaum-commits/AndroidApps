package de.wissensgarten.app

import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * „Alle Daten löschen“ (Eltern-Einstellungen): Profile, Lernstand, Einstellungen, Erinnerungen,
 * zwischengespeicherte Bilder – und, falls verbunden, die Kopie im versteckten Google-Drive-Ordner.
 * Danach startet die App neu, wie frisch installiert.
 */
object DataWipe {
    /** Alle SharedPreferences-Dateien der App */
    val PREFS = listOf("einstellungen", "duelle", "sync_geloescht", "geraete_sync")

    /** @return Fehlermeldung zum Drive (lokal wird trotzdem gelöscht) oder null */
    suspend fun wipeAll(app: WissensgartenApp): String? {
        val remote = app.sync.deleteRemote().exceptionOrNull()?.let { "Die Kopie im Google Drive konnte nicht gelöscht werden (offline?). Lösche sie notfalls in Drive → Einstellungen → Apps verwalten." }
        withContext(Dispatchers.IO) {
            runCatching { WorkManager.getInstance(app).cancelAllWork() }
            app.container.database.clearAllTables()
            PREFS.forEach { app.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
            app.cacheDir.deleteRecursively()
            app.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        }
        return remote
    }

    /** App sauber neu starten (alle Zustände im Speicher sind danach weg). */
    fun restart(context: Context) {
        val i = context.packageManager.getLaunchIntentForPackage(context.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(i)
        Runtime.getRuntime().exit(0)
    }
}
