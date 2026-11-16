package de.kurvenflug.app

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kurvenflug.core.model.CalDate
import de.kurvenflug.core.model.License
import de.kurvenflug.core.model.Pro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Kaufweg für Kurvenflug Pro: Google Play (Play-Fassung, Pflicht dort) oder Code von der Website (Web, GitHub-APK).
 * Gesperrt wird nur, solange ein Kaufweg wirklich angeboten wird ([available]).
 */
interface ProShop {
    /** Kauf wird angeboten (Play: Produkt im Play Store angelegt; Website: Schlüssel und Kasse eingetragen) */
    val available: StateFlow<Boolean>
    /** Preis zum Anzeigen (Play: von Google, Website: brutto inkl. USt.) */
    val price: StateFlow<String?>
    /** Über Google Play gekauft */
    val purchased: StateFlow<Boolean>
    /** Website-Codes werden angenommen (nicht in der Play-Fassung – dort nur Google Play) */
    val acceptsCodes: Boolean
    /** Öffentlicher Schlüssel des Lizenz-Dienstes (Codes prüfen) */
    val publicKey: String
    fun buy()
    /** Play: Käufe neu abfragen */
    fun restore() {}
}

/** Web und GitHub-APK: Kasse auf der Website öffnen, Code kommt per E-Mail bzw. über den Link zurück. */
class CodeShop(override val publicKey: String, private val checkoutUrl: String, private val open: (String) -> Unit) : ProShop {
    override val available: StateFlow<Boolean> = MutableStateFlow(publicKey.length == 64 && checkoutUrl.isNotBlank())
    override val price: StateFlow<String?> = MutableStateFlow(Pro.WEB_PRICE)
    override val purchased: StateFlow<Boolean> = MutableStateFlow(false)
    override val acceptsCodes = true
    override fun buy() = open(checkoutUrl)
}

/** Was gerade gilt: Pro freigeschaltet? Werden Pro-Inhalte gesperrt (Kaufweg angeboten)? */
data class ProState(val pro: Boolean = false, val locking: Boolean = false, val license: License.Info? = null, val viaPlay: Boolean = false)

val LocalPro = staticCompositionLocalOf { ProState() }

@Composable
fun rememberProState(vm: AppState, shop: ProShop?): ProState {
    if (shop == null) return ProState()
    val available by shop.available.collectAsStateWithLifecycle()
    val purchased by shop.purchased.collectAsStateWithLifecycle()
    val code by vm.settings.proCode.collectAsStateWithLifecycle()
    val offered by vm.settings.proOffered.collectAsStateWithLifecycle()
    LaunchedEffect(available) { if (available) vm.settings.markProOffered() }
    val today = remember { CalDate.today().toEpochDay() }
    val license = remember(code, shop.publicKey) { if (shop.acceptsCodes) License.check(code, shop.publicKey, today) else null }
    return ProState(pro = purchased || license != null, locking = available || offered, license = license, viaPlay = purchased)
}
