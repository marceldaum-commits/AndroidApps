package de.wissensgarten.core.data

import android.content.Context
import de.wissensgarten.core.data.db.GardenDatabase

/** Einfache Abhängigkeits-Sammlung – hier wird später z. B. ein Billing-Client ergänzt. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val database: GardenDatabase by lazy { GardenDatabase.create(app) }
    val tombstones: Tombstones by lazy { Tombstones(app) }
    val repository: GardenRepository by lazy { GardenRepository(database, tombstones = tombstones) }
    val catalog: PackCatalog by lazy { PackCatalog(app.assets) }
    val settings: Settings by lazy { Settings(app) }
    val entitlements: Entitlements by lazy { LocalEntitlements() }
    val duels: DuelHistory by lazy { DuelHistory(app) }
    val syncStore: SyncStore by lazy { SyncStore(app, database, settings, tombstones) }
}
