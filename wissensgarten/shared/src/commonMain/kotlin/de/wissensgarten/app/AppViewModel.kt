package de.wissensgarten.app

import de.wissensgarten.app.ui.common.encodePng
import kotlinx.coroutines.sync.withLock
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
    private val _plusPreviews = MutableStateFlow<List<Pack>>(emptyList())
    /** Plus-Pakete, die noch nicht geladen sind (nur Vorschau für die Samenhandlung) */
    val plusPreviews: StateFlow<List<Pack>> = _plusPreviews.asStateFlow()

    /** Vorschauen nie ins Lernen geben und nie Beete damit „aktualisieren“ – nur volle Pakete. */
    private suspend fun setPacks(all: List<Pack>) {
        val full = all.filter { !it.preview }
        _packs.value = full
        _plusPreviews.value = all.filter { it.preview }
        repo.syncPacks(full)
    }

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

    /** Ausgeblendete Sprach-Beete des aktiven Profils (→ Hinweis auf Quasselquest im Garten) */
    val sprachBeete: StateFlow<Int> = profile.map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(0) else repo.ausgeblendeteBeete(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

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

    /** Die letzten 14 Tage – für den Nachhilfe-Hinweis im Wochenbericht (Eltern-Tor) */
    val hintLogs = repo.logsSince(de.wissensgarten.core.model.Platform.nowMs() - 14 * Scheduler.DAY_MS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            setPacks(all)
        }
        viewModelScope.launch { while (true) { delay(60_000); _now.value = de.wissensgarten.core.model.Platform.nowMs() } }
    }

    fun tick() { _now.value = de.wissensgarten.core.model.Platform.nowMs(); c.entitlements.refresh() }

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

    // ------------------------------------------------------------ Wissensgarten Plus

    val plusStatus = c.entitlements.status

    /** Code aus Eingabe, E-Mail oder Link übernehmen. */
    /** Code einlösen – ort: „eingabe“ (Eltern tippen ihn ein) oder „link“ (Freischalt-Link ?pro=…) */
    fun redeemPlus(text: String, ort: String = "eingabe"): de.wissensgarten.core.data.PlusEntitlements.Redeem {
        val r = c.entitlements.redeem(text)
        if (r == de.wissensgarten.core.data.PlusEntitlements.Redeem.OK) { refreshPlusPacksIfDue(force = true); zaehle("code_eingeloest", ort) }
        return r
    }

    /**
     * Anonyme Nutzungszählung (abschaltbar im Elternbereich): nur App, Version, Ereignis, Ort, Thema –
     * keine Kennung, kein Name, kein Lernstand. Ist der Schalter aus, wird nichts gesendet.
     */
    fun zaehle(ereignis: String, ort: String? = null, thema: String? = null) {
        if (!c.settings.countUsage.value) return
        val body = de.wissensgarten.core.model.Zaehlung.inhalt(AppInfo.VERSION, ereignis, ort, thema) ?: return
        host?.let { it.sendCount(de.wissensgarten.core.model.Zaehlung.url(it.testStage), body) }
    }

    fun startPlusTrial(): Boolean = c.entitlements.startTrial()

    /** Plus-Pakete vom Plus-Endpunkt holen (nur mit gültigem Code, höchstens alle 20 h). */
    fun refreshPlusPacksIfDue(force: Boolean = false) {
        val h = host ?: return
        val code = c.entitlements.code ?: return
        val now = de.wissensgarten.core.model.Platform.nowMs()
        if (!force && now - settings.lastPlusPackCheck < 20 * 3_600_000L) return
        viewModelScope.launch {
            val known = c.catalog.installedVersions()
            val n = runCatching { h.fetchPlusPacks(code, known) }.getOrNull() ?: return@launch
            settings.lastPlusPackCheck = now
            if (n > 0) {
                val all = c.catalog.reload()
                setPacks(all)
                h.toast(if (n == 1) "🌟 Ein neues Plus-Paket ist da!" else "🌟 $n neue Plus-Pakete sind da!")
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
            val known = c.catalog.installedVersions()
            val n = runCatching { h.fetchPackUpdates(known) }.getOrNull() ?: return@launch
            settings.lastPackCheck = now
            if (n > 0) {
                val all = c.catalog.reload()
                setPacks(all)
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

    fun endSession() {
        session = emptyList(); saved[K_MIX] = null; tick(); refreshWidget()
        c.settings.activeProfileId.value?.let { pid -> viewModelScope.launch { runCatching { verlaufAktualisieren(pid) } } }
        elternBald()
    }

    // ---------------- Eltern-Handy koppeln (nur hinter dem Eltern-Tor; ohne Kopplung wird nichts gesendet) ----------------
    private val EK = de.wissensgarten.core.model.ElternKopplung

    /** Neue Kopplung für ein Profil anlegen und gleich den ersten Bericht hochladen. null = Gerät kann keine sicheren Zufallszahlen. */
    fun elternKoppeln(profileId: String, spitzname: String?): de.wissensgarten.core.model.ElternKopplung.Kopplung? {
        val h = host ?: return null
        val k = de.wissensgarten.core.model.ElternKopplung.Kopplung(h.secureRandomB64(16) ?: return null, h.secureRandomB64(16) ?: return null, h.secureRandomB64(32) ?: return null)
        if (!EK.gueltig(k)) return null
        c.settings.setElternKopplung(profileId, k, spitzname)
        viewModelScope.launch { elternHochladen(profileId) }
        return k
    }

    fun elternLink(k: de.wissensgarten.core.model.ElternKopplung.Kopplung) = EK.elternLink(host?.testStage == true, k)

    /** Kopplung aufheben: Bericht auf dem Server löschen, Werte lokal löschen (auch wenn der Server gerade nicht erreichbar ist) */
    fun elternAufheben(profileId: String, onDone: (Boolean) -> Unit = {}) {
        val k = c.settings.elternKopplung(profileId) ?: return onDone(true)
        val h = host
        c.settings.setElternKopplung(profileId, null)
        viewModelScope.launch {
            val st = h?.elternDelete(EK.berichtUrl(h.testStage, k.id), k.schreib) ?: -1
            onDone(st == 204 || st == 404)
        }
    }

    /** Nach einer Lernrunde: Bericht hochladen, höchstens alle 10 Minuten (später nachgeholt) */
    private var elternJob: kotlinx.coroutines.Job? = null
    fun elternBald() {
        val pid = c.settings.activeProfileId.value ?: return
        if (c.settings.elternKopplung(pid) == null) return
        elternJob?.cancel()
        elternJob = viewModelScope.launch {
            val warten = (c.settings.elternUpload(pid) + EK.ABSTAND_MS - de.wissensgarten.core.model.Platform.nowMs()).coerceAtLeast(5_000)
            kotlinx.coroutines.delay(warten)
            elternHochladen(pid)
        }
    }

    /** Bericht bauen, verschlüsseln (auf dem Gerät) und hochladen. Ergebnis: HTTP-Status (204 = ok), −1 = Fehler/keine Kopplung */
    private val elternMutex = kotlinx.coroutines.sync.Mutex()
    suspend fun elternHochladen(profileId: String): Int = elternMutex.withLock { elternHochladenJetzt(profileId) }
    private suspend fun elternHochladenJetzt(profileId: String): Int {
        val h = host ?: return -1
        val k = c.settings.elternKopplung(profileId) ?: return -1
        verlaufAktualisieren(profileId)
        val json = elternBerichtText(profileId) ?: return -1
        val st = h.elternPut(EK.berichtUrl(h.testStage, k.id), k.schreib, k.key, json)
        if (st in 200..299) {
            c.settings.setElternUpload(profileId, de.wissensgarten.core.model.Platform.nowMs())
            sicherungHochladen(profileId)
        }
        return st
    }

    /** Monatssummen aus den Einzelergebnissen nachführen (dauerhaft, auch wenn alte Einzelergebnisse später gekürzt werden) */
    suspend fun verlaufAktualisieren(profileId: String) {
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val logs = repo.profileLogsSince(profileId, now - 400 * Scheduler.DAY_MS)
        val neu = EK.monatsSummen(logs.map { de.wissensgarten.core.model.ElternKopplung.Antwort(it.cardId, it.time, it.rating != de.wissensgarten.core.model.Rating.AGAIN.ordinal) })
        c.settings.setMonatsVerlauf(profileId, EK.monateZusammen(c.settings.monatsVerlauf(profileId), neu))
    }

    // ---------------- Verschlüsselte Sicherung über die Kopplung ----------------
    private val sJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Sicherung nur bei Änderung hochladen (gleicher Schlüssel und gleiches Schreibrecht wie der Bericht). Ergebnis: Status oder 0 = unverändert */
    suspend fun sicherungHochladen(profileId: String): Int {
        val h = host ?: return -1
        val k = c.settings.elternKopplung(profileId) ?: return -1
        val (daten, _) = sicherungDaten(profileId) ?: return -1
        val stand = daten.toString().let { "${it.length}:${it.hashCode()}" }
        if (stand == c.settings.elternSicherungStand(profileId)) return 0
        val text = EK.sicherung(AppInfo.VERSION, de.wissensgarten.core.model.Platform.nowMs(), k.schreib, daten)
        val st = h.elternPut(EK.sicherungUrl(h.testStage, k.id), k.schreib, k.key, text)
        if (st in 200..299) c.settings.setElternSicherungStand(profileId, stand)
        return st
    }

    /**
     * Der Lernstand eines Profils für die Sicherung: Profil, Beete, Karten, Einkäufe, Fortschritt (Kapitel, Truhen, Klassenarbeiten),
     * Einzelergebnisse der letzten 90 Tage, Monatssummen, Plus-Code und – solange es klein bleibt – selbst gemalte Merkbilder.
     * Ergebnis: Daten und Zahl der weggelassenen Bilder.
     */
    suspend fun sicherungDaten(profileId: String): Pair<kotlinx.serialization.json.JsonObject, Int>? {
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val all = c.snapshots.snapshot()
        if (all.profiles.none { it.id == profileId }) return null
        // savedAt bleibt 0: sonst wäre jede Sicherung „geändert“ (hochgeladen wird nur bei Änderung)
        fun snap(logTage: Long) = de.wissensgarten.core.model.SyncSnapshot(
            savedAt = 0,
            profiles = all.profiles.filter { it.id == profileId },
            beds = all.beds.filter { it.profileId == profileId },
            cards = all.cards.filter { it.profileId == profileId },
            inventory = all.inventory.filter { it.profileId == profileId },
            logs = all.logs.filter { it.profileId == profileId && it.time >= now - logTage * Scheduler.DAY_MS },
            extras = all.extras.filterKeys { it == profileId },
        )
        val verlauf = sJson.encodeToJsonElement(
            kotlinx.serialization.builtins.MapSerializer(kotlinx.serialization.serializer<String>(), de.wissensgarten.core.model.ElternKopplung.Monat.serializer()),
            c.settings.monatsVerlauf(profileId))
        fun bauen(logTage: Long, bilder: Map<String, String>) = kotlinx.serialization.json.buildJsonObject {
            put("lernstand", sJson.parseToJsonElement(de.wissensgarten.core.model.Sync.encodeText(snap(logTage))))
            put("verlauf", verlauf)
            c.entitlements.code?.let { put("plusCode", kotlinx.serialization.json.JsonPrimitive(it)) }
            if (bilder.isNotEmpty()) put("bilder", kotlinx.serialization.json.JsonObject(bilder.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) }))
        }
        // Zu groß? Erst Einzelergebnisse kürzen (90 → 30 Tage → keine), Monatssummen bleiben
        var tage = 90L
        var ohne = bauen(tage, emptyMap())
        while (ohne.toString().length > EK.SICHERUNG_MAX_KLARTEXT && tage > 0) { tage = if (tage > 30) 30 else 0; ohne = bauen(tage, emptyMap()) }
        // Selbst gemalte Merkbilder nur, solange alles unter der Grenze bleibt
        val bilder = LinkedHashMap<String, String>()
        var weg = 0
        var groesse = ohne.toString().length
        val b64 = kotlin.io.encoding.Base64.Default
        for (card in all.cards.filter { it.profileId == profileId }) {
            if (!de.wissensgarten.app.ui.play.Drawings.has(card.id)) continue
            val bytes = runCatching { de.wissensgarten.app.ui.play.Drawings.load(card.id)?.encodePng() }.getOrNull()
            val t = bytes?.let { b64.encode(it) }
            if (t == null || groesse + t.length + card.id.length + 8 > EK.SICHERUNG_MAX_KLARTEXT) { weg++; continue }
            bilder[card.id] = t; groesse += t.length + card.id.length + 8
        }
        return bauen(tage, bilder) to weg
    }

    /** Ergebnis der Prüfung eines Kopplungs-Links auf einem (neuen) Gerät */
    sealed class Fund {
        data class Fehler(val text: String) : Fund()
        data class Gefunden(val id: String, val key: String, val schreib: String, val daten: kotlinx.serialization.json.JsonObject,
                            val name: String, val karten: Int, val stand: String, val bilder: Int, val hierSchonLernstand: Boolean) : Fund()
    }

    /** Kopplungs-Link prüfen: Sicherung holen und entschlüsseln (noch nichts überschreiben) */
    suspend fun sicherungPruefen(link: String): Fund {
        val h = host ?: return Fund.Fehler("Das geht hier leider nicht.")
        val (id, key) = EK.ausLink(link) ?: return Fund.Fehler("Das ist kein vollständiger Kopplungs-Link. Er endet mit „#…“ – bitte ganz kopieren.")
        val (st, text) = h.elternGetDecrypt(EK.sicherungUrl(h.testStage, id), key)
        if (st == 404) return Fund.Fehler("Keine Sicherung gefunden. Sie entsteht erst nach der ersten Lernrunde auf dem gekoppelten Gerät.")
        if (st != 200) return Fund.Fehler("Der Server ist gerade nicht erreichbar. Bitte später noch einmal versuchen.")
        if (text == null) return Fund.Fehler("Die Sicherung ließ sich nicht öffnen – stimmt der Link?")
        val j = runCatching { sJson.parseToJsonElement(text) as kotlinx.serialization.json.JsonObject }.getOrNull()
            ?: return Fund.Fehler("Die Sicherung ist beschädigt.")
        val app = (j["app"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        if (app != "wissensgarten") return Fund.Fehler("Dieser Link gehört zu einer anderen App.")
        val schreib = (j["schreib"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return Fund.Fehler("Die Sicherung ist unvollständig.")
        val daten = j["daten"] as? kotlinx.serialization.json.JsonObject ?: return Fund.Fehler("Die Sicherung ist unvollständig.")
        val snap = runCatching { de.wissensgarten.core.model.Sync.decodeText(daten["lernstand"].toString()) }.getOrNull()
            ?: return Fund.Fehler("Die Sicherung stammt aus einer neueren Version. Bitte erst den Wissensgarten aktualisieren.")
        if (snap.format > de.wissensgarten.core.model.Sync.FORMAT) return Fund.Fehler("Die Sicherung stammt aus einer neueren Version. Bitte erst den Wissensgarten aktualisieren.")
        val hier = c.snapshots.snapshot().let { s -> s.cards.isNotEmpty() || s.logs.isNotEmpty() }
        return Fund.Gefunden(id, key, schreib, daten, snap.profiles.firstOrNull()?.name ?: "", snap.cards.size,
            (j["stand"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
            (daten["bilder"] as? kotlinx.serialization.json.JsonObject)?.size ?: 0, hier)
    }

    /**
     * Sicherung einspielen – NUR nach Rückfrage. Vorhandener Lernstand wird nicht überschrieben, sondern zusammengeführt
     * (wie beim Geräte-Sync: nichts geht verloren). Danach ist dieses Gerät wieder gekoppelt und schreibt weiter.
     */
    suspend fun sicherungEinspielen(f: Fund.Gefunden): String {
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val remote = de.wissensgarten.core.model.Sync.decodeText(f.daten["lernstand"].toString())
        val local = c.snapshots.snapshot()
        val merged = de.wissensgarten.core.model.Sync.merge(local, remote, null, now)
        c.snapshots.apply(merged, local)
        // Profil im zusammengeführten Stand finden (gleiche ID oder zugeordnet)
        val rp = remote.profiles.firstOrNull()?.id
        val pid = rp?.let { merged.aliases[it] ?: it }?.takeIf { p -> merged.profiles.any { it.id == p } } ?: merged.profiles.firstOrNull()?.id
        // Merkbilder
        val b64 = kotlin.io.encoding.Base64.Default
        (f.daten["bilder"] as? kotlinx.serialization.json.JsonObject)?.forEach { (cardId, v) ->
            val bytes = runCatching { b64.decode((v as kotlinx.serialization.json.JsonPrimitive).content) }.getOrNull() ?: return@forEach
            de.wissensgarten.app.ui.common.decodeImage(bytes)?.let { de.wissensgarten.app.ui.play.Drawings.saveImage(cardId, it) }
        }
        // Plus-Code (nur wenn hier noch nicht aktiv)
        (f.daten["plusCode"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { code ->
            if (c.entitlements.code == null) runCatching { c.entitlements.redeem(code) }
        }
        if (pid != null) {
            (f.daten["verlauf"])?.let { v -> runCatching {
                sJson.decodeFromJsonElement(kotlinx.serialization.builtins.MapSerializer(kotlinx.serialization.serializer<String>(), de.wissensgarten.core.model.ElternKopplung.Monat.serializer()), v)
            }.getOrNull() }?.let { c.settings.setMonatsVerlauf(pid, EK.monateZusammen(c.settings.monatsVerlauf(pid), it)) }
            c.settings.setElternKopplung(pid, de.wissensgarten.core.model.ElternKopplung.Kopplung(f.id, f.schreib, f.key), c.settings.elternName(pid))
            c.settings.setActiveProfile(pid)
        }
        c.settings.setOnboarded(true)
        c.settings.syncChanged()
        tick(); refreshWidget()
        // Weiter schreiben (Bericht + Sicherung) – zeigt zugleich, dass das Schreibrecht passt
        if (pid != null) viewModelScope.launch { elternHochladen(pid) }
        return "Fortschritt von ${f.name.ifBlank { "eurem Kind" }} wiederhergestellt 🌱"
    }

    /** Der Bericht als JSON-Text (vor der Verschlüsselung) – auch für Tests */
    suspend fun elternBerichtText(profileId: String): String? {
        val p = repo.profile(profileId) ?: return null
        val now = de.wissensgarten.core.model.Platform.nowMs()
        val beds = repo.bedsOnce(profileId)
        val packOf = packs.value.associateBy { it.id }
        val beete = beds.map { b ->
            val pack = b.bed.packId?.let(packOf::get)
            de.wissensgarten.core.model.ElternKopplung.Beet(b.bed.id, b.bed.name, de.wissensgarten.core.model.Angebote.thema(b.bed.packId, pack?.subject),
                pack?.subjectInfo?.label ?: de.wissensgarten.core.model.Subject.EIGENE.label)
        }
        val karten = beds.flatMap { b -> b.cards.map { de.wissensgarten.core.model.ElternKopplung.Karte(it.id, it.bedId, it.reps, it.stability) } }
        val logs = repo.profileLogsSince(profileId, now - 15 * Scheduler.DAY_MS)
        val antworten = logs.map { de.wissensgarten.core.model.ElternKopplung.Antwort(it.cardId, it.time, it.rating != de.wissensgarten.core.model.Rating.AGAIN.ordinal) }
        val subj = beds.associate { it.bed.id to (it.bed.packId?.let(packOf::get)?.subject ?: "eigene") }
        val infos = beds.flatMap { b -> b.cards.map { it.id to de.wissensgarten.core.model.WeekReport.CardInfo(it.front, subj[b.bed.id] ?: "eigene", it.stability, it.lapses) } }.toMap()
        val tipp = de.wissensgarten.core.model.WeekReport.build(logs.map { de.wissensgarten.core.model.WeekReport.Entry(it.cardId, it.time, it.rating) }, infos, now).tip
        val arbeiten = c.settings.exams(profileId).map { e -> de.wissensgarten.core.model.ElternKopplung.Arbeit(e.day, e.title) }
        val b = EK.bericht(c.settings.elternName(profileId), p.level.takeIf { it in 1..13 }, now, Scheduler.localDay(now), beete, karten, antworten, arbeiten, tipp,
            verlauf = c.settings.monatsVerlauf(profileId))
        return EK.berichtText(b)
    }

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

    /** Ziel eines Deeplinks …/#paket=<ID> (andere Lern-Apps verlinken so auf unsere Pakete) */
    sealed interface PaketZiel {
        data class Beet(val bedId: String, val titel: String) : PaketZiel
        data class Pflanzen(val pack: Pack) : PaketZiel
        data object Plus : PaketZiel
        data object Unbekannt : PaketZiel
    }

    fun paketZiel(id: String): PaketZiel {
        val pack = packs.value.firstOrNull { it.id == id } ?: return PaketZiel.Unbekannt
        beds.value.firstOrNull { it.bed.packId == id }?.let { return PaketZiel.Beet(it.bed.id, pack.title) }
        if (!de.wissensgarten.core.model.Angebot.angeboten(pack)) return PaketZiel.Unbekannt
        return if (entitlements.canUse(pack)) PaketZiel.Pflanzen(pack) else PaketZiel.Plus
    }

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
