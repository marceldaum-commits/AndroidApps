package de.experimentopia.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.experimentopia.app.ui.experiment.Outcome
import de.experimentopia.core.data.JournalEntry
import de.experimentopia.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as ExperimentopiaApp).container
    val settings = c.settings

    /** Sterne je Versuch */
    val stars: StateFlow<Map<String, Int>> = c.progress.results.map { l -> l.filter { Catalog.byId(it.experimentId) != null }.associate { it.experimentId to it.stars } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val journal: StateFlow<List<JournalEntry>> = c.progress.journal.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<Stats> = combine(c.progress.results, c.progress.journal, settings.freeLabRuns) { res, j, runs ->
        val r = res.filter { Catalog.byId(it.experimentId) != null }
        val ids = r.map { it.experimentId }.toSet()
        Stats(
            done = r.size,
            stars = r.sumOf { it.stars },
            hits = r.count { it.firstGuessRight },
            ahas = r.count { !it.firstGuessRight },
            explained = j.filter { it.explainedFirstTry }.map { it.experimentId }.toSet().size,
            islandsDone = Island.entries.count { i -> Catalog.of(i).all { it.id in ids } },
            freeLabRuns = runs,
            dailies = j.filter { it.daily }.map { it.at / 86_400_000L }.toSet().size,
            calibrated = j.count { Calibration.matches(Sure.entries.getOrElse(it.sure) { Sure.ETWAS }, it.right) },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    fun newBadges(s: Stats) = Badges.earned(s).filter { it.id !in settings.badgesSeen }
    fun markBadgesSeen(ids: Collection<String>) { settings.badgesSeen = settings.badgesSeen + ids }

    fun today(): Long = java.time.LocalDate.now().toEpochDay()
    fun daily() = Daily.forDay(today())
    fun dailyDoneToday(j: List<JournalEntry>): Boolean {
        val start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return j.any { it.daily && it.at >= start && it.experimentId == daily().id }
    }

    fun record(e: Experiment, o: Outcome, daily: Boolean) {
        viewModelScope.launch { c.progress.record(e.id, o.stars, o.guess, o.result, o.right, o.sure.ordinal, o.explainedFirstTry, daily) }
    }

    fun reset() { viewModelScope.launch { c.progress.reset(); settings.resetProgress() } }

    fun locked(e: Experiment) = Pro.locked(e, settings.pro.value)
    fun locked(l: Lab) = Pro.locked(l, settings.pro.value)

    // ------------------------------------------------------------ App-Update
    private val _update = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _update
    private companion object { const val CHECK_EVERY_MS = 20 * 60 * 60_000L }

    /** Beim Start höchstens alle 20 Stunden bei GitHub nachsehen (abschaltbar). */
    fun checkForUpdateIfDue(installed: String) {
        if (!BuildConfig.SELF_UPDATE || !settings.autoUpdate.value) return
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < CHECK_EVERY_MS) return
        viewModelScope.launch {
            de.experimentopia.app.update.AppUpdater.check().onSuccess { r ->
                settings.lastUpdateCheck = now
                _update.value = r?.takeIf { Updates.isNewer(it.version, installed) }
            }
        }
    }
}
