package de.wissensgarten.app.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play-Variante: Updates über Google Play (In-App-Updates, „flexibel“).
 * Die neue Version lädt im Hintergrund; installiert wird erst beim nächsten Öffnen –
 * so wird kein Kind mitten in einer Gieß-Runde unterbrochen.
 */
object StoreUpdates {
    private const val REQUEST = 4711

    fun check(activity: Activity) {
        runCatching {
            val mgr = AppUpdateManagerFactory.create(activity)
            mgr.appUpdateInfo.addOnSuccessListener { info ->
                when {
                    // Beim letzten Mal fertig geladen → jetzt installieren (App startet kurz neu)
                    info.installStatus() == InstallStatus.DOWNLOADED -> mgr.completeUpdate()
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                        mgr.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), REQUEST)
                }
            }
        }
    }
}
