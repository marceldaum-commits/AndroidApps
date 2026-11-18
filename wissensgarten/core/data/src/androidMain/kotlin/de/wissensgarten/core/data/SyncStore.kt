package de.wissensgarten.core.data

import android.content.Context
import de.wissensgarten.core.data.db.*
import de.wissensgarten.core.model.*
import java.io.File

/**
 * Übersetzt zwischen Datenbank/Einstellungen und dem Sync-Format. Der eigentliche Abgleich
 * (Zusammenführen) steckt in [Sync.merge], der Transport (Google Drive) in der App.
 */
class SyncStore(
    context: Context,
    private val dao: GardenStore,
    private val settings: Settings,
    private val tombstones: Tombstones,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val baseFile = File(context.filesDir, "sync_basis.gz")

    /** Stand der letzten erfolgreichen Synchronisierung (für die Drei-Wege-Zusammenführung). */
    fun base(): SyncSnapshot? = runCatching { Sync.decode(baseFile.readBytes()) }.getOrNull()
    fun saveBase(s: SyncSnapshot) { baseFile.writeBytes(Sync.encode(s)) }
    fun forgetBase() { baseFile.delete() }

    private val io = SnapshotIO(dao, settings, tombstones, clock)

    suspend fun snapshot(): SyncSnapshot = io.snapshot(base()?.aliases.orEmpty())

    /**
     * Schreibt einen zusammengeführten Stand in Datenbank und Einstellungen (siehe [SnapshotIO.apply]).
     */
    suspend fun apply(s: SyncSnapshot, taken: SyncSnapshot) = io.apply(s, taken)

    // ---- Abgleich in zwei Schritten absichern: erst „vorgemerkt“, nach Erfolg zur Basis

    private val pendingFile = File(context.filesDir, "sync_vorgemerkt.gz")
    private val pendingTakenFile = File(context.filesDir, "sync_vorgemerkt_eigen.gz")

    /** Vor dem Hochladen merken, was hochgeladen wird – falls die App mittendrin beendet wird. */
    fun savePending(merged: SyncSnapshot, taken: SyncSnapshot) {
        pendingTakenFile.writeBytes(Sync.encode(taken))
        pendingFile.writeBytes(Sync.encode(merged))
    }
    fun pending(): Pair<SyncSnapshot, SyncSnapshot>? = runCatching {
        Sync.decode(pendingFile.readBytes()) to Sync.decode(pendingTakenFile.readBytes())
    }.getOrNull()
    fun clearPending() { pendingFile.delete(); pendingTakenFile.delete() }
}
