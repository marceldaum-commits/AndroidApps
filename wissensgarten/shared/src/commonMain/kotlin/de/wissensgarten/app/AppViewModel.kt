package de.wissensgarten.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.wissensgarten.core.data.DataContainer
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
const val SAFARI_BED = "Meine Safari"

class AppViewModel(
    private val c: DataContainer,
    /** Plattform (Widget, Erinnerungen, Updates) – null in Tests */
    var host: AppHost? = null,
    private val saved: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
) : ViewModel() {
    private val repo = c.repository
    val settings = c.settings
    val entitlements = c.entitlements

    val theme: StateFlow<ThemeMode> = settings.theme
    val onboarded: StateFlow<Boolean> = settings.onboarded

    private val _packs = MutableStateFlow<List<Pack>>(emptyList())
    val packs: StateFlow<List<Pack>> = _packs.asStateFlow()

    /** Aktuelle Zeit, minütlich aktualisiert – damit Pflanzen auch bei offener App welken. */
    private val _now = MutableStateFlow(de.wissensgarten.core.model.Platform.nowMs())
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

    // --- Abenteuer: Erklär-Szenen, Pfad, Gartenfreunde ---

    val adventureRev = settings.adventureRev

    fun introSeen(packId: String) = profile.value?.let { settings.introSeen(it.id, packId) } ?: true
    fun markIntroSeen(packId: String) { profile.value?.let { settings.markIntroSeen(it.id, packId) } }

    fun stations(bedId: String): List<de.wissensgarten.core.model.Station> {
        val b = beds.value.firstOrNull { it.bed.id == bedId } ?: return emptyList()
        return de.wissensgarten.core.model.AdventurePath.stations(b.cards.sortedBy { it.sortIndex }.map { it.toStudy() }, de.wissensgarten.core.model.Platform.nowMs())
    }

    /** 3 ruhige Karten vor dem Schlafen: bevorzugt heute gelernte, sonst durstige, sonst gewachsene. */
    fun startGoodNight(): Boolean {
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val today = Scheduler.localDay(now)
        val grown = beds.value.flatMap { b -> b.cards.filter { !it.memory.isSeed }.map { it.toStudy() } }
        val picked = (grown.filter { Scheduler.localDay(it.memory.lastReview) == today }.shuffled() +
            grown.filter { Scheduler.isThirsty(it.memory, now) } + grown.shuffled()).distinctBy { it.id }.take(3)
        if (picked.isEmpty()) return false
        session = picked; sessionCalm = true; mixPair = null
        profile.value?.let { settings.markGoodNight(it.id, today) }
        return true
    }

    /** Heutiges Fest und noch nicht eingesammelte Überraschungen (Index, Emoji). */
    fun festival() = de.wissensgarten.core.model.Seasons.festival(de.wissensgarten.core.model.Seasons.today())

    fun surprises(): List<Pair<Int, String>> {
        val p = profile.value ?: return emptyList()
        val f = festival() ?: return emptyList()
        val day = Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs())
        return f.items.mapIndexed { i, e -> i to e }.filter { (i, _) -> !settings.surpriseFound(p.id, day, i) }
    }

    fun collectSurprise(i: Int) {
        val p = profile.value ?: return
        settings.markSurprise(p.id, Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs()), i)
        addDew(5)
    }

    fun grownPlants() = beds.value.sumOf { b -> b.cards.count { !it.memory.isSeed } }
    fun chaptersRead() = profile.value?.let { settings.chaptersRead(it.id) } ?: emptySet()
    fun markChapterRead(n: Int) { profile.value?.let { settings.markChapterRead(it.id, n) } }
    fun newChapter(): de.wissensgarten.core.model.Chapter? {
        val read = chaptersRead()
        return de.wissensgarten.core.model.Story.unlocked(grownPlants()).firstOrNull { it.number !in read }
    }

    // --- Kreuzungen (Mischrunde) ---
    var mixPair: Pair<de.wissensgarten.core.model.Subject, de.wissensgarten.core.model.Subject>? = null
        private set(v) { field = v; saved[K_MIX] = v?.let { "${it.first.name},${it.second.name}" } }
    /** Gerade gezüchtete Kreuzung (für die Enthüllung) */
    var newHybrid: de.wissensgarten.core.model.Hybrid? = null
        private set

    private fun subjectOf(b: BedWithCards): de.wissensgarten.core.model.Subject =
        b.bed.packId?.let { pid -> packs.value.firstOrNull { it.id == pid }?.subjectInfo } ?: de.wissensgarten.core.model.Subject.EIGENE

    /** Zwei Beete aus verschiedenen Fächern mit gewachsenen Pflanzen – oder null. */
    fun mixCandidates(): Pair<BedWithCards, BedWithCards>? {
        val ok = beds.value.filter { b -> b.cards.count { !it.memory.isSeed } >= 4 }
        for (i in ok.indices) for (j in i + 1 until ok.size)
            if (subjectOf(ok[i]) != subjectOf(ok[j])) return ok[i] to ok[j]
        return null
    }

    fun startMixed(): Boolean {
        val (a, b) = mixCandidates() ?: return false
        val now = de.wissensgarten.core.model.Platform.nowMs()
        fun pick(x: BedWithCards) = x.cards.filter { !it.memory.isSeed }.map { it.toStudy() }
            .sortedBy { Scheduler.retrievability(it.memory, now) }
        session = de.wissensgarten.core.model.Hybrids.interleave(pick(a), pick(b))
        sessionCalm = false
        mixPair = subjectOf(a) to subjectOf(b)
        return session.isNotEmpty()
    }

    /** Am Ende jeder Runde aufgerufen. Bei Mischrunden mit ≥ 75 % gibt's eine Kreuzung. */
    fun sessionComplete(correct: Int, total: Int) {
        val pair = mixPair ?: return
        mixPair = null
        val p = profile.value ?: return
        if (total > 0 && correct.toDouble() / total >= de.wissensgarten.core.model.Hybrids.PASS_RATIO) {
            val h = de.wissensgarten.core.model.Hybrids.of(pair.first, pair.second)
            if (h.id !in settings.hybrids(p.id)) { settings.addHybrid(p.id, h.id); newHybrid = h }
        }
    }

    fun consumeNewHybrid(): de.wissensgarten.core.model.Hybrid? = newHybrid.also { newHybrid = null }
    fun hybrids() = profile.value?.let { p -> settings.hybrids(p.id).mapNotNull { de.wissensgarten.core.model.Hybrids.byId(it) } } ?: emptyList()

    // --- Familienbaum ---
    /** Kalenderwoche (Montag bis Sonntag, Ortszeit) */
    fun familyWeek(): Long = (Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs()) + 3).floorDiv(7L)
    fun familyClaimed() = settings.familyClaimedWeek() == familyWeek()
    fun claimFamilyTree() {
        if (familyClaimed()) return
        settings.setFamilyClaimedWeek(familyWeek())
        viewModelScope.launch { profiles.value.forEach { repo.addDew(it.id, 20) } }
    }

    fun goodNightDoneToday(): Boolean = profile.value?.let { settings.goodNightDay(it.id) == Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs()) } ?: true

    fun startStation(bedId: String, index: Int): Boolean {
        sessionCalm = false; mixPair = null
        val b = beds.value.firstOrNull { it.bed.id == bedId } ?: return false
        val cards = b.cards.sortedBy { it.sortIndex }.map { it.toStudy() }
        val st = stations(bedId).getOrNull(index) ?: return false
        session = de.wissensgarten.core.model.AdventurePath.sessionFor(st, cards, de.wissensgarten.core.model.Platform.nowMs())
        return session.isNotEmpty()
    }

    /** Freigeschaltete Freunde = geschaffte Truhen-Stationen (über alle Beete). */
    fun chestsEarned(): Int = de.wissensgarten.core.model.AdventurePath.chestsEarned(
        beds.value.map { b -> b.cards.sortedBy { it.sortIndex }.map { it.toStudy() } }, de.wissensgarten.core.model.Platform.nowMs(),
    )

    fun chestsOpened(): Int = profile.value?.let { settings.chestsOpened(it.id) } ?: 0

    /** Öffnet die nächste Truhe und liefert den neuen Freund. */
    fun openChest(): de.wissensgarten.core.model.Friend? {
        val p = profile.value ?: return null
        val opened = settings.chestsOpened(p.id)
        if (opened >= chestsEarned()) return null
        val f = de.wissensgarten.core.model.Friends.next(opened) ?: return null
        settings.setChestsOpened(p.id, opened + 1)
        addDew(15)
        return f
    }

    fun gardenFriends(): List<de.wissensgarten.core.model.Friend> {
        val p = profile.value ?: return emptyList()
        val owned = de.wissensgarten.core.model.Friends.earned(settings.chestsOpened(p.id))
        val chosen = settings.gardenFriends(p.id)
        return if (chosen == null) owned.takeLast(5) else owned.filter { it.id in chosen }
    }

    fun toggleGardenFriend(id: String) {
        val p = profile.value ?: return
        val current = gardenFriends().map { it.id }.toMutableSet()
        if (!current.remove(id)) { if (current.size >= 6) return; current += id }
        settings.setGardenFriends(p.id, current)
    }

    // --- Duell ---

    val duelHistory = c.duels.duels

    /** Duell, das als Nächstes gespielt bzw. angezeigt wird. */
    var currentDuel by androidx.compose.runtime.mutableStateOf<de.wissensgarten.core.model.Duel?>(null)
        private set

    fun newDuel(pack: Pack) { currentDuel = de.wissensgarten.core.model.Duels.new(pack) }

    fun showDuel(d: de.wissensgarten.core.model.Duel) { currentDuel = d }

    /** Liest einen Code (auch aus einer ganzen Nachricht). */
    // ------------------------------------------------------------ Klassen-Code & Arbeitsblatt

    /** Beet als Klassen-Code (nur eigene Beete; Fortschritt bleibt privat). */
    fun classSetOf(bedId: String): de.wissensgarten.core.model.ClassSet? {
        val b = beds.value.firstOrNull { it.bed.id == bedId } ?: return null
        return de.wissensgarten.core.model.ClassSet(b.bed.name, b.bed.emoji,
            b.cards.sortedBy { it.sortIndex }.map { de.wissensgarten.core.model.ClassCard(it.front, it.back, it.img, it.info) })
    }

    var pendingClassSet: de.wissensgarten.core.model.ClassSet? = null

    fun openClassCode(text: String): Boolean {
        pendingClassSet = de.wissensgarten.core.model.ClassCodes.decode(text) ?: return false
        return true
    }

    fun importClassSet(set: de.wissensgarten.core.model.ClassSet, then: (String) -> Unit) {
        val p = profile.value ?: return
        viewModelScope.launch {
            val id = repo.addOwnBed(p.id, set.title.ifBlank { "Klasse" }, set.emoji.ifBlank { "🏫" })
            repo.bedEntity(id)?.let { repo.addCards(it, set.cards) }
            then(id)
        }
    }

    /** Wissens-Safari: Fundstück als Karte ins Safari-Beet, Foto wird zum Merkbild. */
    fun addSafariFind(word: de.wissensgarten.core.model.Safari.Word, english: Boolean, photo: androidx.compose.ui.graphics.ImageBitmap?, then: () -> Unit = {}) {
        val p = profile.value ?: return
        viewModelScope.launch {
            val bedId = beds.value.firstOrNull { it.bed.packId == null && it.bed.name == SAFARI_BED }?.bed?.id
                ?: repo.addOwnBed(p.id, SAFARI_BED, "📷")
            val bed = repo.bedEntity(bedId) ?: return@launch
            val id = repo.addCards(bed, listOf(de.wissensgarten.core.model.Safari.card(word, english))).first()
            if (photo != null) de.wissensgarten.app.ui.play.Drawings.saveBitmap(id, photo)
            then()
        }
    }

    /** Zahlen und Beispiele für Postkarte und Urkunde eines Profils */
    suspend fun keepsakeData(profileId: String): de.wissensgarten.app.ui.share.KeepsakeData {
        val p = repo.profile(profileId)
        val bedList = repo.bedsOnce(profileId)
        val cards = bedList.flatMap { it.cards }
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val week = repo.profileLogsSince(profileId, now - 7 * Scheduler.DAY_MS)
        val byId = cards.associateBy { it.id }
        val learned = week.filter { it.rating >= 2 }.map { it.cardId }.distinct().mapNotNull { byId[it] }
            .filter { it.back.length <= 30 }.take(3)
            .map { c -> (c.img?.let { de.wissensgarten.app.ui.play.splitEmojis(it).firstOrNull() } ?: "🌱") to "${c.front.take(28).trimEnd('?')} → ${c.back}" }
        return de.wissensgarten.app.ui.share.KeepsakeData(
            name = p?.name ?: "", avatar = p?.avatar ?: "🌱",
            grown = cards.count { !it.memory.isSeed },
            trees = cards.count { de.wissensgarten.core.model.Stage.of(it.memory) == de.wissensgarten.core.model.Stage.TREE },
            reviewsTotal = repo.logCount(profileId), reviewsWeek = week.size,
            newThisWeek = cards.count { it.memory.reps in 1..2 && now - it.lastReview < 7 * Scheduler.DAY_MS },
            learned = learned,
            flowers = bedList.sortedBy { it.bed.sortIndex }.take(5).map { b ->
                val n = b.cards.size.coerceAtLeast(1)
                de.wissensgarten.app.widget.Flower(b.bed.hue, 1f - b.cards.count { Scheduler.isThirsty(it.memory, now) }.toFloat() / n, b.cards.any { !it.memory.isSeed })
            },
            beds = bedList.map { it.bed.name },
        )
    }

    fun safariCount() = beds.value.firstOrNull { it.bed.packId == null && it.bed.name == SAFARI_BED }?.cards?.size ?: 0

    fun addCardsTo(bedId: String, cards: List<de.wissensgarten.core.model.ClassCard>) {
        viewModelScope.launch { repo.bedEntity(bedId)?.let { repo.addCards(it, cards) } }
    }



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

    val weekLogs = repo.logsSince(de.wissensgarten.core.model.Platform.nowMs() - 7 * Scheduler.DAY_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Karten der laufenden Gieß-Runde (fest, damit sie während der Runde nicht springt). */
    var session: List<StudyCard> = emptyList()
        private set(v) { field = v; saved[K_SESSION] = ArrayList(v.map { it.id }) }

    /**
     * Beendet Android die App im Hintergrund, ist [session] danach leer. Die Karten-IDs liegen aber im
     * gespeicherten Zustand → hier mit dem aktuellen Lernstand aus der Datenbank wieder zusammensetzen.
     * Die Position in der Runde merkt sich der Bildschirm selbst (rememberSaveable).
     */
    fun restoreSession(all: List<BedWithCards>): List<StudyCard> {
        if (session.isNotEmpty()) return session
        val ids = saved.get<ArrayList<String>>(K_SESSION).orEmpty()
        if (ids.isEmpty()) return emptyList()
        val byId = all.flatMap { it.cards }.associateBy { it.id }
        session = ids.mapNotNull { byId[it]?.toStudy() }
        sessionCalm = saved.get<Boolean>(K_CALM) == true
        if (mixPair == null) mixPair = saved.get<String>(K_MIX)?.split(',')?.takeIf { it.size == 2 }?.let { (a, b) ->
            runCatching { de.wissensgarten.core.model.Subject.valueOf(a) to de.wissensgarten.core.model.Subject.valueOf(b) }.getOrNull()
        }
        return session
    }

    /** Liegt eine unterbrochene Runde im gespeicherten Zustand? */
    fun hasSavedSession(): Boolean = session.isEmpty() && !saved.get<ArrayList<String>>(K_SESSION).isNullOrEmpty()

    init {
        viewModelScope.launch {
            val all = c.catalog.all()
            _packs.value = all
            repo.syncPacks(all)
        }
        viewModelScope.launch { while (true) { delay(60_000); _now.value = de.wissensgarten.core.model.Platform.nowMs() } }
    }

    fun tick() { _now.value = de.wissensgarten.core.model.Platform.nowMs() }

    // ------------------------------------------------------------ App-Update

    private val _update = kotlinx.coroutines.flow.MutableStateFlow<de.wissensgarten.core.model.AppRelease?>(null)
    /** Neue Version, falls beim automatischen Nachsehen gefunden. */
    val availableUpdate: StateFlow<de.wissensgarten.core.model.AppRelease?> = _update

    /** Bei jedem Öffnen der App: höchstens alle 30 Minuten bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        val h = host ?: return
        if (!h.features.selfUpdate || !settings.autoUpdate.value) return
        val now = de.wissensgarten.core.model.Platform.nowMs()
        if (_update.value == null && now - settings.lastUpdateCheck < 30 * 60_000L) return
        viewModelScope.launch {
            runCatching { h.checkUpdate(installed) }.onSuccess { r ->
                val newer = r?.takeIf { de.wissensgarten.core.model.Updates.isNewer(it.version, installed) }
                // Nur „nichts Neues“ merken – ein gefundenes Update soll beim nächsten Start wieder angezeigt werden
                if (newer == null) settings.lastUpdateCheck = now
                _update.value = newer
            }
        }
    }

    /** Höchstens alle 20 Stunden neue Wissenspakete (nur Daten) nachladen – abschaltbar in den Einstellungen. */
    fun refreshPacksIfDue(force: Boolean = false) {
        val h = host ?: return
        if (!h.features.packUpdates || (!force && !settings.packUpdates.value)) return
        val now = de.wissensgarten.core.model.Platform.nowMs()
        if (!force && now - settings.lastPackCheck < 20 * 3_600_000L) return
        viewModelScope.launch {
            val known = c.catalog.all().associate { it.id to it.version }
            val n = runCatching { h.fetchPackUpdates(known) }.getOrNull() ?: return@launch
            settings.lastPackCheck = now
            if (n > 0) {
                val all = c.catalog.reload()
                _packs.value = all
                repo.syncPacks(all)
                h.toast(if (n == 1) "🐝 Brummi hat ein neues Wissenspaket mitgebracht!" else "🐝 Brummi hat $n neue Wissenspakete mitgebracht!")
            } else if (force) h.toast("Alle Wissenspakete sind aktuell 🌼")
        }
    }

    fun seedsLeft(p: ProfileEntity? = profile.value): Int =
        p?.let { SessionBuilder.seedsLeft(it.seedsPerDay, it.seedDay, it.seedsToday, de.wissensgarten.core.model.Platform.nowMs()) } ?: 0

    // ------------------------------------------------------------ Klassenarbeiten

    fun exams(): List<de.wissensgarten.core.model.Exam> = profile.value?.let { settings.exams(it.id) }.orEmpty()

    fun examPlans(): List<de.wissensgarten.core.model.ExamPlan> {
        val cards = beds.value.flatMap { b -> b.cards.map { it.toStudy() } }
        val now = de.wissensgarten.core.model.Platform.nowMs()
        return exams().filter { e -> beds.value.any { it.bed.id == e.bedId } }.map { de.wissensgarten.core.model.ExamPlanner.plan(it, cards, now) }
    }

    fun addExam(bedId: String, title: String, day: Long) {
        val p = profile.value ?: return
        @OptIn(kotlin.uuid.ExperimentalUuidApi::class) val e = de.wissensgarten.core.model.Exam(kotlin.uuid.Uuid.random().toString(), bedId, title.trim().ifBlank { "Klassenarbeit" }, day, de.wissensgarten.core.model.Platform.nowMs())
        settings.setExams(p.id, exams() + e)
    }

    fun removeExam(id: String) {
        val p = profile.value ?: return
        settings.setExams(p.id, exams().filterNot { it.id == id })
        c.tombstones.add(listOf(id))
    }

    fun startExamSession(examId: String): Boolean {
        val plan = examPlans().firstOrNull { it.exam.id == examId } ?: return false
        sessionCalm = false; mixPair = null
        session = plan.today
        return session.isNotEmpty()
    }

    /** Brummi-Radio: eine kurze Runde (höchstens 12 Karten) */
    fun radioCards(): List<StudyCard> = buildSession().take(12)

    fun buildSession(bedId: String? = null): List<StudyCard> {
        val cards = beds.value.filter { bedId == null || it.bed.id == bedId }.flatMap { b -> b.cards.map { it.toStudy() } }
        return SessionBuilder.build(cards, de.wissensgarten.core.model.Platform.nowMs(), seedsLeft())
    }

    /** Gute-Nacht-Runde aktiv? (ruhige Spielarten, leise) */
    var sessionCalm = false
        private set(v) { field = v; saved[K_CALM] = v }

    fun startSession(bedId: String? = null): Boolean {
        sessionCalm = false; mixPair = null
        session = buildSession(bedId)
        return session.isNotEmpty()
    }

    /** @param dew Tautropfen inkl. Serien-Bonus */
    fun review(card: StudyCard, rating: Rating, dew: Int) {
        val p = profile.value ?: return
        viewModelScope.launch { repo.review(p.id, card, rating, dew) }
        quest(de.wissensgarten.core.model.Quests.Kind.WATER)
        if (rating != Rating.AGAIN) quest(de.wissensgarten.core.model.Quests.Kind.RIGHT)
    }

    // ───── 0.18: Unkraut, Blitz, Rhythmus, Bienenstock, Tiere, Spaziergang, Tagesaufgaben, Film
    private fun pid() = profile.value?.id
    private fun today() = Scheduler.localDay(de.wissensgarten.core.model.Platform.nowMs())
    fun studyCards(): List<StudyCard> = beds.value.flatMap { b -> b.cards.map { it.toStudy() } }
    fun peers(): Map<String, List<String>> = beds.value.associate { b -> b.bed.id to b.cards.map { it.back } }
    fun weeds(): List<StudyCard> = pid()?.let { de.wissensgarten.core.model.Weeds.active(studyCards(), settings.weedsPulled(it)) }.orEmpty()
    fun weedCount() = pid()?.let { settings.weedCount(it) } ?: 0
    fun pullWeed(card: StudyCard) {
        val p = pid() ?: return
        review(card, Rating.GOOD, 3)
        settings.pullWeed(p, card.id, card.memory.lapses)
        quest(de.wissensgarten.core.model.Quests.Kind.WEED)
    }
    fun quest(kind: de.wissensgarten.core.model.Quests.Kind, n: Int = 1) { pid()?.let { settings.addQuestProgress(it, today(), kind.name, n) } }
    fun blitzDone(correct: Int) {
        addDew(de.wissensgarten.core.model.Blitz.dew(correct))
        quest(de.wissensgarten.core.model.Quests.Kind.BLITZ); quest(de.wissensgarten.core.model.Quests.Kind.RIGHT, correct)
    }
    fun rhythmDone(points: Int, right: Int) {
        addDew(de.wissensgarten.core.model.Rhythm.dew(points))
        quest(de.wissensgarten.core.model.Quests.Kind.RHYTHM); quest(de.wissensgarten.core.model.Quests.Kind.RIGHT, right)
    }
    fun totalReps() = beds.value.sumOf { b -> b.cards.sumOf { it.reps } }
    /** Aktuelle Serie und Rekord (Rekord wird gespeichert – Tiere bleiben für immer). */
    suspend fun streaks(): Pair<Int, Int> {
        val p = pid() ?: return 0 to 0
        val days = c.store.profileLogsSince(p, de.wissensgarten.core.model.Platform.nowMs() - 400 * Scheduler.DAY_MS).map { Scheduler.localDay(it.time) }.toSet()
        val cur = de.wissensgarten.core.model.Critters.streak(days, today())
        settings.setBestStreak(p, cur)
        return cur to settings.bestStreak(p)
    }
    private fun questsOf(p: String, day: Long): List<de.wissensgarten.core.model.Quests.Quest>? = settings.questList(p, day)?.mapNotNull { (k, n) ->
        runCatching { de.wissensgarten.core.model.Quests.Quest(de.wissensgarten.core.model.Quests.Kind.valueOf(k), n) }.getOrNull() }?.ifEmpty { null }
    fun questsToday(): List<de.wissensgarten.core.model.Quests.Quest> {
        val p = pid() ?: return emptyList()
        return questsOf(p, today()) ?: de.wissensgarten.core.model.Quests.forDay(today(), p, weeds().isNotEmpty()).also { l ->
            settings.setQuestList(p, today(), l.associate { it.kind.name to it.amount })
        }
    }
    fun questProgress(): Map<String, Int> = pid()?.let { settings.questProgress(it, today()) }.orEmpty()
    /** Erledigte Aufgaben dieser Woche (Mo–So) */
    fun questWeekDone(): Int {
        val p = pid() ?: return 0
        val t = today(); val w = de.wissensgarten.core.model.Quests.week(t)
        return (0..6).map { t - it }.filter { de.wissensgarten.core.model.Quests.week(it) == w }.sumOf { d ->
            val prog = settings.questProgress(p, d)
            questsOf(p, d).orEmpty().count { de.wissensgarten.core.model.Quests.done(it, prog) }
                .coerceAtMost(de.wissensgarten.core.model.Quests.PER_DAY)
        }
    }
    fun questChestOpened() = pid()?.let { settings.questChestWeek(it) == de.wissensgarten.core.model.Quests.week(today()) } ?: true
    fun openQuestChest() {
        val p = pid() ?: return
        settings.setQuestChestWeek(p, de.wissensgarten.core.model.Quests.week(today()))
        addDew(de.wissensgarten.core.model.Quests.CHEST_DEW)
    }
    fun filmFrames(): List<de.wissensgarten.core.model.GardenFilm.Frame> = de.wissensgarten.core.model.GardenFilm.frames(
        beds.value.flatMap { b -> b.cards.map { de.wissensgarten.core.model.GardenFilm.Plant(it.createdAt, it.lastReview, it.stability, it.reps) } },
        de.wissensgarten.core.model.Platform.nowMs())

    fun addDew(amount: Int) {
        val p = profile.value ?: return
        viewModelScope.launch { repo.addDew(p.id, amount) }
    }

    fun endSession() { session = emptyList(); saved[K_MIX] = null; tick(); refreshWidget() }

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
        host?.setReminder(on, hour)
    }


    fun refreshWidget() {
        host?.refreshWidget()
    }

    private companion object {
        const val K_SESSION = "session_ids"
        const val K_CALM = "session_calm"
        const val K_MIX = "session_mix"
    }
}
