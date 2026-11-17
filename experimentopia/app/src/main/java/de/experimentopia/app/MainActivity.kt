package de.experimentopia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.experimentopia.app.ui.AppRoot
import de.experimentopia.app.ui.fx.LocalBuzzer
import de.experimentopia.app.ui.fx.LocalSfx
import de.experimentopia.app.ui.fx.LocalVibration
import de.experimentopia.app.ui.fx.LocalMusic
import de.experimentopia.app.ui.fx.LocalReader
import de.experimentopia.app.ui.settings.UpdateBanner
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import de.experimentopia.app.update.UpdateSection
import de.experimentopia.app.update.UpdateState

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExperimentopiaApp
        if (savedInstanceState == null) de.experimentopia.app.update.StoreUpdate.onStart(this)
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val audio by vm.settings.audio.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(audio) { app.sfx.volume = audio.sfxVol }
            val view = LocalView.current
            val buzzer = remember(view) { AndroidBuzzer(view) }
            ExperimentopiaTheme {
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration, LocalBuzzer provides buzzer,
                    LocalMusic provides app.music, LocalReader provides app.reader) {
                    Surface(Modifier.fillMaxSize(), color = E.Night) {
                        BackHandler(enabled = vm.nav.canBack) { vm.nav.back() }
                        val upd by vm.availableUpdate.collectAsStateWithLifecycle()
                        val autoUpd by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                        AppRoot(vm.nav, vm.state, BuildConfig.VERSION_NAME, BuildConfig.VERSION_DATE,
                            settingsTop = { if (BuildConfig.SELF_UPDATE) UpdateSection(BuildConfig.VERSION_NAME, upd?.let { UpdateState.Available(it) } ?: UpdateState.Idle, autoUpd, vm.settings::setAutoUpdate) },
                            banner = upd?.let { u -> { open -> UpdateBanner(u.version, open) } })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as ExperimentopiaApp).music.pause(false)
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }

    override fun onStop() {
        super.onStop()
        // im Hintergrund: keine Musik, kein Vorlesen
        val app = application as ExperimentopiaApp
        app.music.pause(true); app.reader.stop()
    }
}
