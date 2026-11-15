package de.wissensgarten.core.data

import de.wissensgarten.core.model.Pack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Freischaltungen. Heute ist alles gratis (ENFORCED = false).
 * Später: Google Play Billing anbinden, `plusActive` aus dem Kauf setzen und ENFORCED einschalten –
 * Pakete mit tier = "plus" sind dann nur mit Plus nutzbar. Die Oberfläche fragt nur `canUse`.
 */
interface Entitlements {
    val plusActive: StateFlow<Boolean>
    fun canUse(pack: Pack): Boolean
}

object Monetization {
    const val ENFORCED = false
}

class LocalEntitlements : Entitlements {
    override val plusActive = MutableStateFlow(false)
    override fun canUse(pack: Pack): Boolean = !Monetization.ENFORCED || !pack.isPlus || plusActive.value
}
