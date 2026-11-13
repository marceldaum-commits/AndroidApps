package de.wissensgarten.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Hält den Garten im Speicher und speichert ihn als JSON-Datei. */
class GardenRepository private constructor(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<GardenState> = _state.asStateFlow()

    private fun load(): GardenState =
        runCatching { json.decodeFromString<GardenState>(file.readText()) }
            .getOrElse { GardenState(beds = StarterBeds.all()) }

    @Synchronized
    private fun update(change: (GardenState) -> GardenState) {
        val next = change(_state.value)
        _state.value = next
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(GardenState.serializer(), next))
        tmp.renameTo(file)
    }

    fun addBed(name: String, emoji: String) = update { s ->
        val hue = listOf(330f, 45f, 200f, 280f, 15f, 160f)[s.beds.size % 6]
        s.copy(beds = s.beds + Bed(UUID.randomUUID().toString(), name.trim(), emoji, hue))
    }

    fun deleteBed(bedId: String) = update { s -> s.copy(beds = s.beds.filterNot { it.id == bedId }) }

    fun addCard(bedId: String, front: String, back: String) = update { s ->
        s.mapBed(bedId) { it.copy(cards = it.cards + Card(UUID.randomUUID().toString(), front.trim(), back.trim())) }
    }

    fun deleteCard(bedId: String, cardId: String) = update { s ->
        s.mapBed(bedId) { b -> b.copy(cards = b.cards.filterNot { it.id == cardId }) }
    }

    fun review(bedId: String, cardId: String, grade: Grade, now: Long = System.currentTimeMillis()) = update { s ->
        val wasSeed = s.beds.firstOrNull { it.id == bedId }?.cards?.firstOrNull { it.id == cardId }?.isSeed == true
        val today = now / Scheduler.DAY_MS
        val seedsToday = if (s.seedDay == today) s.seedsToday else 0
        s.mapBed(bedId) { b ->
            b.copy(cards = b.cards.map { if (it.id == cardId) Scheduler.review(it, grade, now) else it })
        }.copy(
            dewdrops = s.dewdrops + if (grade == Grade.AGAIN) 1 else 3,
            seedDay = today,
            seedsToday = seedsToday + if (wasSeed) 1 else 0,
        )
    }

    private fun GardenState.mapBed(bedId: String, f: (Bed) -> Bed) =
        copy(beds = beds.map { if (it.id == bedId) f(it) else it })

    companion object {
        @Volatile private var instance: GardenRepository? = null
        fun get(context: Context): GardenRepository =
            instance ?: synchronized(this) {
                instance ?: GardenRepository(File(context.applicationContext.filesDir, "garten.json"))
                    .also { instance = it }
            }
    }
}

/** Eine Gieß-Runde: zuerst die durstigsten Pflanzen, dann ein paar neue Samen. */
data class SessionItem(val bedId: String, val card: Card)

object Session {
    const val SEEDS_PER_DAY = 10

    fun build(state: GardenState, now: Long, bedId: String? = null): List<SessionItem> {
        val beds = state.beds.filter { bedId == null || it.id == bedId }
        val thirsty = beds.flatMap { b -> b.cards.filter { Scheduler.isThirsty(it, now) }.map { SessionItem(b.id, it) } }
            .sortedBy { Scheduler.retrievability(it.card, now) }
        val today = now / Scheduler.DAY_MS
        val seedsLeft = SEEDS_PER_DAY - if (state.seedDay == today) state.seedsToday else 0
        val seeds = beds.flatMap { b -> b.cards.filter { it.isSeed }.map { SessionItem(b.id, it) } }
            .take(seedsLeft.coerceAtLeast(0))
        return thirsty + seeds
    }

    fun thirstyCount(state: GardenState, now: Long): Int =
        state.beds.sumOf { b -> b.cards.count { Scheduler.isThirsty(it, now) } }
}
