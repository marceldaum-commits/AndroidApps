package de.wissensgarten.core.data

import de.wissensgarten.core.model.License
import de.wissensgarten.core.model.Pack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wissensgarten Plus. Frei bleiben alle Pakete bis Klasse 10 und alle Spielarten; Plus öffnet Abi-/Experten-Pakete,
 * monatlich neue Plus-Pakete, bis zu 6 Profile und den Wochenbericht.
 * Gesperrt wird erst, wenn [PlusConfig.enforced] (Kaufweg eingerichtet) – vorher ist alles frei.
 */
interface Entitlements {
    val plusActive: StateFlow<Boolean>
    fun canUse(pack: Pack): Boolean
}

/**
 * @param publicKey öffentlicher Ed25519-Schlüssel des Lizenz-Dienstes (Hex)
 * @param enforced Sperren aktiv (nur wenn ein Kaufweg existiert – Web/GitHub mit Kassen-URL; Play vorerst nie)
 */
data class PlusConfig(
    val publicKey: String = "",
    val checkoutYear: String = "",
    val checkoutForever: String = "",
    val purchaseAllowed: Boolean = false,
) {
    val enforced: Boolean get() = purchaseAllowed && publicKey.length == 64 && (checkoutYear.isNotBlank() || checkoutForever.isNotBlank())
}

/** Was gerade gilt – für die Plus-Seite im Eltern-Bereich. */
data class PlusStatus(
    val active: Boolean,
    val product: Int? = null,
    /** letzter gültiger Tag (Tage seit 1970), null = für immer / kein Code */
    val until: Long? = null,
    val trialUntil: Long? = null,
    val trialUsed: Boolean = false,
    val enforced: Boolean = false,
) {
    val viaTrial: Boolean get() = active && product == null
}

class PlusEntitlements(
    private val settings: Settings,
    val config: PlusConfig,
    private val today: () -> Long,
) : Entitlements {
    private val _status = MutableStateFlow(compute())
    val status: StateFlow<PlusStatus> = _status.asStateFlow()
    private val _active = MutableStateFlow(_status.value.active)
    override val plusActive: StateFlow<Boolean> = _active.asStateFlow()

    private fun compute(): PlusStatus {
        val d = today()
        val info = License.check(settings.plusCode, config.publicKey, d)
        val trial = settings.plusTrialUntil.takeIf { it >= 0 }
        val trialOn = trial != null && d <= trial
        return PlusStatus(
            active = info != null || trialOn,
            product = info?.product, until = info?.until,
            trialUntil = trial, trialUsed = trial != null, enforced = config.enforced,
        )
    }

    /** Neu berechnen (z. B. beim Öffnen der App – ein Jahres-Plus oder der Test kann abgelaufen sein). */
    fun refresh() { _status.value = compute(); _active.value = _status.value.active }

    /** Gültiger Code für Lizenz-Server-Abrufe (Plus-Pakete), sonst null. */
    val code: String? get() = settings.plusCode?.takeIf { License.check(it, config.publicKey, today()) != null }

    enum class Redeem { OK, EXPIRED, INVALID }

    /** Code aus Text, E-Mail oder Link übernehmen. */
    fun redeem(text: String): Redeem {
        val code = License.extract(text) ?: text.trim()
        val info = License.parse(code, config.publicKey) ?: return Redeem.INVALID
        if (info.until?.let { today() > it } == true) return Redeem.EXPIRED
        settings.plusCode = License.encode(License.decode(code)!!)
        refresh()
        return Redeem.OK
    }

    /** 7 Tage Plus gratis – einmal, ohne Zahlungsdaten, endet von allein. */
    fun startTrial(): Boolean {
        if (settings.plusTrialUntil >= 0) return false
        settings.plusTrialUntil = today() + TRIAL_DAYS - 1
        refresh()
        return true
    }

    fun forgetCode() { settings.plusCode = null; refresh() }

    /** Vorschauen (Plus-Paket noch nicht geladen) sind nie pflanzbar – das volle Paket kommt mit Code vom Server. */
    override fun canUse(pack: Pack): Boolean = !pack.preview && (!config.enforced || !pack.isPlus || _active.value)

    /** Profile: frei 2, mit Plus bis 6. Ohne Sperre (kein Kaufweg) unbegrenzt wie bisher. */
    fun maxProfiles(): Int = when { !config.enforced -> Int.MAX_VALUE; _active.value -> PLUS_PROFILES; else -> FREE_PROFILES }

    /** Wochenbericht: Plus (ohne Sperre frei). */
    fun canUseWeekReport(): Boolean = !config.enforced || _active.value

    companion object {
        const val TRIAL_DAYS = 7
        const val FREE_PROFILES = 2
        const val PLUS_PROFILES = 6
    }
}
