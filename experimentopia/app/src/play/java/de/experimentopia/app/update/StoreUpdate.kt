package de.experimentopia.app.update

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play-Fassung: Updates über den Store (Play In-App Updates).
 * Normale Versionen laden im Hintergrund („flexibel“) und werden beim nächsten App-Start eingespielt – nie mitten in einem Versuch.
 * Als dringend markierte Versionen (Priorität 4 oder 5 in der Play Console) verlangt der Store sofort.
 * Gefragt wird beim App-Start, bei normalen Versionen höchstens einmal am Tag.
 */
object StoreUpdate {
    private const val DAY = 24 * 60 * 60 * 1000L
    fun onStart(activity: ComponentActivity) {
        val manager = runCatching { AppUpdateManagerFactory.create(activity) }.getOrNull() ?: return
        val prefs = activity.getSharedPreferences("store_update", Context.MODE_PRIVATE)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            runCatching {
                when {
                    // fertig geladen (beim letzten Mal im Hintergrund): jetzt, direkt beim Start, einspielen
                    info.installStatus() == InstallStatus.DOWNLOADED -> manager.completeUpdate()
                    // ein dringendes Update wurde unterbrochen: fortsetzen
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        manager.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        val urgent = info.updatePriority() >= 4 && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        if (urgent) manager.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                        else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && System.currentTimeMillis() - prefs.getLong("asked", 0) > DAY) {
                            prefs.edit().putLong("asked", System.currentTimeMillis()).apply()
                            manager.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
                        }
                    }
                }
            }
        }
    }
}
