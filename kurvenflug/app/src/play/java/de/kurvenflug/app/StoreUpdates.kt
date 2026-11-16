package de.kurvenflug.app

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play-Variante: Updates direkt in der App über die Play In-App Updates API – ohne den Store zu öffnen.
 * - Normal „flexibel“: Google Play fragt einmal nach, lädt im Hintergrund, man spielt weiter.
 *   Fertig geladen wird beim nächsten Wechsel in den Hintergrund installiert – kein Kind wird mitten im Level unterbrochen.
 * - Wichtige Versionen (Priorität ≥ 4 in der Play Console bzw. älter als 30 Tage): „sofort“ beim Start.
 */
class StoreUpdates(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private var downloaded = false
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val listener = InstallStateUpdatedListener { s -> if (s.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }

    fun onStart() {
        manager.registerListener(listener)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> downloaded = true
                // Ein „sofort“-Update wurde unterbrochen → fortsetzen
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> start(info, AppUpdateType.IMMEDIATE)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    val important = info.updatePriority() >= 4 || (info.clientVersionStalenessDays() ?: 0) >= 30
                    val type = if (important && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                    if (info.isUpdateTypeAllowed(type)) start(info, type)
                }
            }
        }
    }

    fun onStop() {
        // Im Hintergrund still installieren (Google: ohne Neustart-Bildschirm, wenn die App nicht sichtbar ist)
        if (downloaded) runCatching { manager.completeUpdate() }
        runCatching { manager.unregisterListener(listener) }
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build()) }
    }
}
