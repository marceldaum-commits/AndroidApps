package de.raumlicht.app

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.raumlicht.app.ui.AppRoot
import de.raumlicht.app.ui.Navigator
import de.raumlicht.app.ui.Routes
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.home.HomeScreen
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.settings.SettingsScreen
import de.raumlicht.app.ui.settings.UpdateState
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Levels
import de.raumlicht.core.model.Badges
import de.raumlicht.core.model.Chapter
import de.raumlicht.core.model.Daily
import de.raumlicht.core.model.Gen
import de.raumlicht.core.model.JamTopic
import de.raumlicht.core.model.Sheet
import de.raumlicht.app.ui.home.*
import de.raumlicht.core.model.*
import de.raumlicht.app.ui.home.JamPicker
import de.raumlicht.app.ui.home.SheetScreen
import androidx.compose.runtime.saveable.rememberSaveable
import de.raumlicht.core.model.MusicMood

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    /** Wunsch-Ziel von außen (Widget): wird einmal angesteuert und dann gelöscht */
    private val openRoute = mutableStateOf<String?>(null)

    private fun readIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra(de.raumlicht.app.widget.DailyWidget.EXTRA_OPEN) == de.raumlicht.app.widget.DailyWidget.OPEN_DAILY) openRoute.value = Routes.DAILY
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()     // Startbild in App-Farbe, bis der erste Bildschirm gezeichnet ist
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as RaumlichtApp
        app.music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        if (savedInstanceState == null) { readIntent(intent); de.raumlicht.app.update.StoreUpdate.onStart(this) }
        setContent {
            val sound by vm.settings.sound.collectAsStateWithLifecycle()
            val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
            val music by vm.settings.music.collectAsStateWithLifecycle()
            LaunchedEffect(sound) { app.sfx.enabled = sound }
            LaunchedEffect(music) { app.music.setEnabled(music) }
            val volume by vm.settings.volume.collectAsStateWithLifecycle()
            LaunchedEffect(volume) { app.sfx.master = volume / 100f; app.music.setVolume(volume / 100f); app.voice.volume = volume / 100f; app.natural.volume = volume / 100f }
            val voice by vm.settings.voice.collectAsStateWithLifecycle()
            LaunchedEffect(voice) { app.voice.enabled = voice; app.natural.enabled = voice; if (!voice) { app.voice.stop(); app.natural.stop() } }
            RaumlichtTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                CompositionLocalProvider(LocalSfx provides app.sfx, LocalVibration provides vibration, de.raumlicht.app.ui.fx.LocalSpeaker provides app.speaker,
                    de.raumlicht.app.ui.fx.LocalBuzzer provides remember(view) { de.raumlicht.app.ui.fx.Buzzer { b, on -> with(de.raumlicht.app.ui.fx.Haptic) { view.buzzNow(b, on) } } },
                    de.raumlicht.app.ui.fx.LocalShareText provides { text -> runCatching { startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text), "Teilen")) } },
                    de.raumlicht.app.ui.fx.LocalShareCard provides { title, points, verdict -> de.raumlicht.app.ui.home.ShareCard.share(this@MainActivity, title, points, verdict) }) {
                    Surface(Modifier.fillMaxSize(), color = Q.Space) { Nav() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RaumlichtApp).music.onForeground(true)
        de.raumlicht.app.widget.DailyWidget.refresh(this)   // z. B. nach Mitternacht: neuer Tagesaufgabe
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as RaumlichtApp).music.onForeground(false); (application as RaumlichtApp).voice.stop(); (application as RaumlichtApp).natural.stop(); super.onStop() }

    @Composable
    private fun Nav() {
        val nav = remember { Navigator() }
        // Systemtaste „Zurück“: eine Ebene schließen; auf der Startseite verlässt sie die App wie gewohnt
        androidx.activity.compose.BackHandler(enabled = nav.canGoBack) { nav.back() }
        val update by vm.availableUpdate.collectAsStateWithLifecycle()
        // Wunsch-Ziel von außen (Widget)
        val wanted by openRoute
        LaunchedEffect(wanted) { wanted?.let { r -> nav.restart(r); openRoute.value = null } }
        AppRoot(nav, vm.settings, vm.stars, vm::record, vm::today,
            updateVersion = update?.version,
            // Abi-Pass über die Website gibt es nur in der GitHub-Fassung; im Play Store wäre Google Play Billing Pflicht – dort bleibt vorerst alles frei
            proEnabled = de.raumlicht.core.model.Pro.ENABLED && BuildConfig.SELF_UPDATE,
            // Musik passend zum Bildschirm: ruhig im Tourplan, treibend auf der Bühne
            onRoute = { route -> (application as RaumlichtApp).music.setMood(if (Routes.isAction(route)) MusicMood.FLIGHT else MusicMood.MENU) },
            onPlanChanged = { de.raumlicht.app.widget.DailyWidget.refresh(this@MainActivity) },
            settings = { onBack ->
                val sound by vm.settings.sound.collectAsStateWithLifecycle()
                val vibration by vm.settings.vibration.collectAsStateWithLifecycle()
                var problem by remember { mutableStateOf(Diagnose.last(this@MainActivity)) }
                val music by vm.settings.music.collectAsStateWithLifecycle()
                val auto by vm.settings.autoUpdate.collectAsStateWithLifecycle()
                SettingsScreen(music, vm.settings::setMusic, sound, vm.settings::setSound, vibration, vm.settings::setVibration, auto, vm.settings::setAutoUpdate,
                    BuildConfig.VERSION_NAME, update?.let { UpdateState.Available(it) } ?: UpdateState.Idle, onBack, onReset = vm::resetAll, voice = vm.settings.voice.collectAsStateWithLifecycle().value, onVoice = vm.settings::setVoice,
                    lastProblem = problem, onProblemSeen = { Diagnose.clear(this@MainActivity); problem = null },
                    voiceControls = { val app = application as RaumlichtApp; val v = app.voice
                        var natural by remember { mutableStateOf(vm.settings.voiceNatural) }
                        LaunchedEffect(app.voicePack.state) { natural = vm.settings.voiceNatural }
                        de.raumlicht.app.ui.settings.NaturalVoiceCard(app.voicePack.state, natural && app.natural.ready, app.natural.problem,
                            onLoad = { app.voicePack.start() }, onCancel = { app.voicePack.cancel() },
                            onRemove = { app.natural.unload(); app.voicePack.remove(); vm.settings.voiceNatural = false; natural = false },
                            onUse = { on -> vm.settings.voiceNatural = on; natural = on; if (on) { app.natural.load(); app.speaker.say("Hallo, ich bin Lux. So klinge ich jetzt.") } else app.natural.stop() })
                        if (!(natural && app.natural.ready)) de.raumlicht.app.ui.settings.VoiceControls(v.position, vm.settings.voiceBright,
                        onNext = { v.nextVoice()?.let { vm.settings.voiceName = it } }, onBright = { vm.settings.voiceBright = it; v.setBright(it) }) }, voiceAvailable = (application as RaumlichtApp).voice.let { !it.checked || it.available }, volume = vm.settings.volume.collectAsStateWithLifecycle().value, onVolume = vm.settings::setVolume)
            })
    }
}
