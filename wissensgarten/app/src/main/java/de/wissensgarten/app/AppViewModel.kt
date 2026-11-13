package de.wissensgarten.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wissensgarten.app.widget.GardenWidget
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.SessionBuilder
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/** Verbindet Datenbank, Pakete und Einstellungen mit der Oberfläche. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as WissensgartenApp).container
    private val repo = c.repository
    val settings = c.settings
    val entitlements = c.entitlements

    val theme: StateFlow<ThemeMode> = settings.theme
    val onboarded: StateFlow<Boolean> = settings.onboarded

    private val _packs = MutableStateFlow<List<Pack>>(emptyList())
    val packs: StateFlow<List<Pack>> = _packs.asStateFlow()

    /** Aktuelle Zeit, minütlich aktualisiert – damit Pflanzen auch bei offener App welken. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    val profiles: StateFlow<List<ProfileEntity>> =
        repo.profiles.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val profile: StateFlow<ProfileEntity?> = combine(profiles, settings.activeProfileId) { ps, id ->
        ps.firstOrNull { it.id == id } ?: ps.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val beds: StateFlow<List<BedWithCards>> = profile.map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.beds(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Gekaufte Artikel des aktiven Profils. */
    val inventory: StateFlow<List<de.wissensgarten.core.data.db.InventoryEntity>> =
        profile.map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.inventory(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Aufgestellte Deko. */
    val activeDecos: StateFlow<Set<String>> = inventory.map { inv -> inv.filter { it.active }.map { it.itemId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun buy(itemId: String, onResult: (de.wissensgarten.core.data.GardenRepository.BuyResult) -> Unit) {
        val p = profile.value ?: return
        viewModelScope.launch { onResult(repo.buy(p.id, itemId)) }
    }

    fun setDecoActive(itemId: String, active: Boolean) {
        val p = profile.value ?: return
        viewModelScope.launch { repo.setDecoActive(p.id, itemId, active) }
    }

    fun setBedSpecies(bedId: String, species: String) = viewModelScope.launch { repo.setBedSpecies(bedId, species) }

    // --- Duell ---

    val duelHistory = c.duels.duels

    /** Duell, das als Nächstes gespielt bzw. angezeigt wird. */
    var currentDuel by androidx.compose.runtime.mutableStateOf<de.wissensgarten.core.model.Duel?>(null)
        private set

    fun newDuel(pack: Pack) { currentDuel = de.wissensgarten.core.model.Duels.new(pack) }

    fun showDuel(d: de.wissensgarten.core.model.Duel) { currentDuel = d }

    /** Liest einen Code (auch aus einer ganzen Nachricht). */
    fun openDuelCode(text: String): Boolean {
        val d = de.wissensgarten.core.model.Duels.decode(text) ?: return false
        currentDuel = d
        return true
    }

    /** Ergebnis anhängen, speichern, Tautropfen gutschreiben. @return neuer Stand und verdiente Tropfen */
    fun finishDuel(correct: Int, millis: Long, score: Int): Pair<de.wissensgarten.core.model.Duel, Int> {
        val p = profile.value
        val d0 = currentDuel ?: return de.wissensgarten.core.model.Duels.new(packs.value.first()) to 0
        val me = de.wissensgarten.core.model.DuelResult(p?.name ?: "Ich", p?.avatar ?: "🌱", correct, millis, score)
        val wasLeader = d0.results.isNotEmpty() && d0.results.all { score > it.score }
        val d = d0.withResult(me)
        currentDuel = d
        c.duels.save(d)
        var dew = 0
        if (p != null && c.duels.claimReward(p.id, d)) {
            dew = correct * 2 + if (wasLeader) 10 else 0
            viewModelScope.launch { repo.addDew(p.id, dew) }
        }
        return d to dew
    }

    val allBeds: StateFlow<List<BedWithCards>> =
        repo.allBeds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekLogs = repo.logsSince(System.currentTimeMillis() - 7 * Scheduler.DAY_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Karten der laufenden Gieß-Runde (fest, damit sie während der Runde nicht springt). */
    var session: List<StudyCard> = emptyList()
        private set

    init {
        viewModelScope.launch {
            val all = c.catalog.all()
            _packs.value = all
            repo.syncPacks(all)
        }
        viewModelScope.launch { while (true) { delay(60_000); _now.value = System.currentTimeMillis() } }
    }

    fun tick() { _now.value = System.currentTimeMillis() }

    fun seedsLeft(p: ProfileEntity? = profile.value): Int =
        p?.let { SessionBuilder.seedsLeft(it.seedsPerDay, it.seedDay, it.seedsToday, System.currentTimeMillis()) } ?: 0

    fun buildSession(bedId: String? = null): List<StudyCard> {
        val cards = beds.value.filter { bedId == null || it.bed.id == bedId }.flatMap { b -> b.cards.map { it.toStudy() } }
        return SessionBuilder.build(cards, System.currentTimeMillis(), seedsLeft())
    }

    fun startSession(bedId: String? = null): Boolean {
        session = buildSession(bedId)
        return session.isNotEmpty()
    }

    /** @param dew Tautropfen inkl. Serien-Bonus */
    fun review(card: StudyCard, rating: Rating, dew: Int) {
        val p = profile.value ?: return
        viewModelScope.launch { repo.review(p.id, card, rating, dew) }
    }

    fun endSession() { tick(); refreshWidget() }

    // --- Einrichtung & Profile ---

    fun finishOnboarding(name: String, avatar: String, level: Int, packIds: Set<String>, ownBeds: List<Pair<String, String>>) {
        viewModelScope.launch {
            val p = repo.createProfile(name, avatar, level)
            plantInto(p.id, packIds, ownBeds)
            settings.setActiveProfile(p.id)
            settings.setOnboarded(true)
        }
    }

    fun addProfile(name: String, avatar: String, level: Int, packIds: Set<String>, ownBeds: List<Pair<String, String>>) {
        viewModelScope.launch {
            val p = repo.createProfile(name, avatar, level)
            plantInto(p.id, packIds, ownBeds)
            settings.setActiveProfile(p.id)
            refreshWidget()
        }
    }

    private suspend fun plantInto(profileId: String, packIds: Set<String>, ownBeds: List<Pair<String, String>>) {
        val all = c.catalog.all()
        packIds.mapNotNull { id -> all.firstOrNull { it.id == id } }.forEach { repo.plantPack(profileId, it) }
        ownBeds.forEach { (name, emoji) -> repo.addOwnBed(profileId, name, emoji) }
    }

    fun switchProfile(id: String) { settings.setActiveProfile(id); refreshWidget() }

    fun updateProfile(p: ProfileEntity) = viewModelScope.launch { repo.updateProfile(p) }

    fun deleteProfile(id: String) = viewModelScope.launch {
        repo.deleteProfile(id)
        if (settings.activeProfileId.value == id) settings.setActiveProfile(profiles.value.firstOrNull { it.id != id }?.id)
        refreshWidget()
    }

    // --- Beete & Karten ---

    fun plantPack(pack: Pack) {
        val p = profile.value ?: return
        viewModelScope.launch { repo.plantPack(p.id, pack); refreshWidget() }
    }

    fun addOwnBed(name: String, emoji: String, then: (String) -> Unit = {}) {
        val p = profile.value ?: return
        viewModelScope.launch { then(repo.addOwnBed(p.id, name, emoji)) }
    }

    fun deleteBed(id: String) = viewModelScope.launch { repo.deleteBed(id); refreshWidget() }

    fun addCard(bedId: String, front: String, back: String, info: String?) {
        val bed = beds.value.firstOrNull { it.bed.id == bedId }?.bed ?: return
        viewModelScope.launch { repo.addCard(bed, front, back, info) }
    }

    fun deleteCard(id: String) = viewModelScope.launch { repo.deleteCard(id) }

    fun setTheme(t: ThemeMode) = settings.setTheme(t)

    fun setReminder(on: Boolean, hour: Int = settings.reminderHour.value) {
        settings.setReminder(on, hour)
        val ctx = getApplication<Application>()
        if (on) de.wissensgarten.app.notify.Reminders.schedule(ctx, hour) else de.wissensgarten.app.notify.Reminders.cancel(ctx)
    }


    fun refreshWidget() {
        viewModelScope.launch { runCatching { GardenWidget().updateAll(getApplication()) } }
    }
}
