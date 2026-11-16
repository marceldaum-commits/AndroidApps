package de.wissensgarten.app.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play-Variante: Updates über Google Play (In-App-Updates) – ohne den Store öffnen zu müssen.
 * - Normal „flexibel“: die neue Version lädt im Hintergrund; eingespielt wird, sobald die App in den Hintergrund geht
 *   oder beim nächsten Öffnen – so wird kein Kind mitten in einer Gieß-Runde unterbrochen.
 * - Kritische Versionen (in der Play Console Priorität ≥ 4 setzen): sofort („immediate“).
 */
object StoreUpdates {
    private const val REQUEST = 4711
    private const val CRITICAL_PRIORITY = 4
    private var manager: AppUpdateManager? = null
    private var downloaded = false
    private val listener = InstallStateUpdatedListener { s -> if (s.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }

    fun check(activity: Activity) {
        runCatching {
            val mgr = manager ?: AppUpdateManagerFactory.create(activity.applicationContext).also { manager = it; it.registerListener(listener) }
            mgr.appUpdateInfo.addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                when {
                    // Fertig geladen → jetzt einspielen (App startet kurz neu)
                    info.installStatus() == InstallStatus.DOWNLOADED -> mgr.completeUpdate()
                    // Abgebrochenes Sofort-Update fortsetzen
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        mgr.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE), REQUEST)
                    available && info.updatePriority() >= CRITICAL_PRIORITY && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                        mgr.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE), REQUEST)
                    available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                        mgr.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), REQUEST)
                }
            }
        }
    }

    /** App geht in den Hintergrund: ein fertig geladenes Update jetzt einspielen. */
    fun onBackground() { if (downloaded) runCatching { manager?.completeUpdate() } }
}
