package de.wissensgarten.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.wissensgarten.app.platform.AndroidHost
import kotlinx.coroutines.flow.MutableStateFlow

/** Android-Einstieg: hält die gemeinsame Oberfläche ([WissensgartenApp]) und verbindet Widget, Links und Lebenszyklus. */
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel((application as WissensgartenApp).container, null, createSavedStateHandle()) } }
    }
    private val host by lazy { AndroidHost(this) { flushUsage() } }
    private val waterRequest = MutableStateFlow(false)
    /** Duell-Code aus Link oder geteilter Nachricht */
    private val duelRequest = MutableStateFlow<String?>(null)

    private fun readDuelIntent(i: Intent?) {
        val text = i?.dataString ?: i?.getStringExtra(Intent.EXTRA_TEXT)
        if (text != null && (text.contains("WG1-") || text.contains("WGK1-"))) duelRequest.value = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Startbild in App-Farbe mit Logo (Android-12-Splash-API, rückwärtskompatibel) – kein weißes Aufblitzen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.host = host
        de.wissensgarten.app.ui.play.Drawings.store = host.images
        waterRequest.value = intent?.getBooleanExtra(EXTRA_WATER, false) == true
        readDuelIntent(intent)
        val startRoute = if (vm.onboarded.value) Routes.GARDEN else Routes.ONBOARDING
        setContent { WissensgartenApp(vm, host, startRoute, waterRequest, duelRequest) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_WATER, false)) waterRequest.value = true
        readDuelIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.tick()
        (application as WissensgartenApp).sync.syncSoon()
    }

    /** Gesunde Lernzeit: Vordergrund-Zeit pro Kalendertag mitzählen (nur lokal). */
    private var fgSince = 0L
    private fun flushUsage() {
        val t = System.currentTimeMillis()
        if (fgSince > 0 && t > fgSince) vm.settings.addUsage(de.wissensgarten.core.model.Scheduler.localDay(t), minOf(t - fgSince, 5 * 60_000L))
        fgSince = if (fgSince > 0) t else 0
    }

    override fun onStart() {
        super.onStart()
        fgSince = System.currentTimeMillis()
        // Bei jedem Öffnen (auch aus dem Hintergrund) nach neuen Versionen schauen
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
        vm.refreshPacksIfDue()
        vm.refreshPlusPacksIfDue()
        de.wissensgarten.app.update.StoreUpdates.check(this)
        (application as WissensgartenApp).music.onForeground(true)
    }

    override fun onStop() {
        super.onStop()
        flushUsage(); fgSince = 0
        de.wissensgarten.app.update.StoreUpdates.onBackground()
        (application as WissensgartenApp).music.onForeground(false)
        vm.refreshWidget()
        // Beim Verlassen der App gleich abgleichen, damit das andere Gerät den neuesten Stand bekommt
        (application as WissensgartenApp).sync.syncSoon(minGapMs = 5_000)
    }

    companion object {
        const val EXTRA_WATER = "water"
    }
}
