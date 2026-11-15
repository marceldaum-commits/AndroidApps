package de.kurvenflug.core.data

import kotlinx.coroutines.flow.Flow

/** Bestes Ergebnis pro Level und Spieler. */
data class LevelProgress(
    val profileId: Int = 0,
    val levelId: String,
    val stars: Int,
    /** Energiezellen, die in diesem Level schon eingesammelt wurden (Bit je Zelle) */
    val cellMask: Int = 0,
    val attempts: Int = 0,
    val solvedAt: Long = 0,
)

/**
 * Ein Durchgang für die Eltern-Ansicht: geschafft ([stars] > 0) oder hängen geblieben (stars = 0 nach mehreren Versuchen).
 * Nur Level-Kennung, Sterne, Versuche, Dauer und Zeitpunkt – nichts Persönliches.
 */
data class Play(
    val id: Long = 0,
    val profileId: Int,
    val levelId: String,
    val stars: Int,
    val attempts: Int,
    val seconds: Int,
    val at: Long,
)

/** Spielstand des aktuellen Spielers – Android: Room-Datenbank, Browser: lokaler Speicher. */
interface ProgressStore {
    val all: Flow<List<LevelProgress>>
    fun plays(since: Long): Flow<List<Play>>
    suspend fun record(levelId: String, stars: Int, cells: Set<Int>, attempts: Int, seconds: Int = 0)
    suspend fun stuck(levelId: String, attempts: Int, seconds: Int)
    suspend fun clear()
    suspend fun clear(profileId: Int)
}

/** Einfacher Schlüssel-Wert-Speicher – Android: SharedPreferences, Browser: localStorage. */
interface Prefs {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getString(key: String, default: String?): String?
    fun getStringSet(key: String, default: Set<String>): Set<String>
    fun keys(): Set<String>
    fun edit(): Editor

    interface Editor {
        fun putBoolean(key: String, v: Boolean): Editor
        fun putInt(key: String, v: Int): Editor
        fun putLong(key: String, v: Long): Editor
        fun putString(key: String, v: String?): Editor
        fun putStringSet(key: String, v: Set<String>): Editor
        fun remove(key: String): Editor
        fun apply()
    }
}
