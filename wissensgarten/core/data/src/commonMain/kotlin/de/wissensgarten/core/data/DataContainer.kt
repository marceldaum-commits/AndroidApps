package de.wissensgarten.core.data

/**
 * Alles, was die gemeinsame Oberfläche an Daten braucht – auf Android und im Web gleich.
 * [prefs] liefert einen Schlüssel-Wert-Speicher je Name (wie SharedPreferences-Dateien).
 */
open class DataContainer(
    val store: GardenStore,
    prefs: (String) -> KeyValueStore,
    packTexts: suspend () -> List<String>,
    downloadedPacks: suspend () -> List<String> = { emptyList() },
    plus: PlusConfig = PlusConfig(),
) {
    val tombstones: Tombstones = Tombstones(prefs("sync_geloescht"))
    val repository: GardenRepository = GardenRepository(store, tombstones = tombstones)
    val catalog: PackCatalog = PackCatalog(packTexts, downloadedPacks)
    val settings: Settings = Settings(prefs("einstellungen"))
    val entitlements: PlusEntitlements = PlusEntitlements(settings, plus) {
        de.wissensgarten.core.model.Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs())
    }
    val duels: DuelHistory = DuelHistory(prefs("duelle"))
}
