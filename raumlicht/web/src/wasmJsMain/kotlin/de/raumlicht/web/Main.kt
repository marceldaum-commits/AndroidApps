package de.raumlicht.web

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import de.raumlicht.app.ui.AppRoot
import de.raumlicht.app.ui.Navigator
import de.raumlicht.app.ui.Routes
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.settings.SettingsPage
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.app.ui.theme.RaumlichtTheme
import de.raumlicht.core.model.Speech
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // eigener Bereich für die Oberfläche: Compose blendet alles andere in seinem Element aus – die 3D-Bühne liegt deshalb daneben, nicht darin
    ComposeViewport(document.getElementById("app")!!) { WebApp() }
}

/** Kurze Klänge aus dem Browser-Synthesizer – dieselben Anlässe wie in der Android-App */
private class WebSfx(private val prefs: WebPrefs) : SoundPlayer {
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!prefs.sound.value) return
        val g = 0.16 * volume * prefs.volume.value / 100.0; if (g <= 0) return
        val p = pitch.toDouble()
        when (s) {
            Sound.TAP -> beep("triangle", 520 * p, 420 * p, 0.05, g * 0.7)
            Sound.TICK -> beep("square", 880 * p, 880 * p, 0.03, g * 0.35)
            Sound.LAUNCH -> beep("sawtooth", 220 * p, 880 * p, 0.28, g * 0.6)
            Sound.HIT -> beep("triangle", 660 * p, 1320 * p, 0.14, g)
            Sound.CELL -> beep("sine", 440 * p, 660 * p, 0.12, g)
            Sound.CRASH -> beep("sawtooth", 200 * p, 60 * p, 0.3, g)
            Sound.WIN -> { beep("triangle", 523 * p, 784 * p, 0.18, g); beep("triangle", 784 * p, 1047 * p, 0.34, g * 0.8) }
            Sound.WRONG -> beep("square", 240 * p, 150 * p, 0.22, g * 0.6)
            Sound.BEEP -> beep("sine", 990 * p, 990 * p, 0.08, g * 0.7)
        }
    }
}

@Composable
private fun WebApp() {
    val prefs = remember { WebPrefs() }; val stars = remember { WebStars() }; val nav = remember { Navigator() }
    val vibration by prefs.vibration.collectAsState()
    // Schriften als Ergänzung zur Standardschrift: Mathe-Zeichen (x₃, ✓, √ …) und Emojis. Die Oberfläche startet erst, wenn beide da sind –
    // schon gezeichneter Text würde sie sonst nicht mehr übernehmen. Fehlt eine Datei, läuft die App trotzdem (dann mit Kästchen statt Zeichen).
    val resolver = LocalFontFamilyResolver.current
    var pending by remember { mutableStateOf(2) }; var fonts by remember { mutableStateOf(listOf<Pair<String, ByteArray>>()) }
    LaunchedEffect(Unit) { listOf("DejaVuSans" to "fonts/dejavu_sans.ttf", "NotoColorEmoji" to "fonts/noto_color_emoji.ttf").forEach { (name, url) -> loadFile(url) { b -> if (b != null) fonts = fonts + (name to b) else pending-- } } }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(fonts.size, pending) {
        if (fonts.size < pending) return@LaunchedEffect
        fonts.forEach { (name, bytes) -> runCatching { resolver.preload(FontFamily(Font(name, bytes))) } }
        ready = true; hideStartScreen()
    }
    if (!ready) return
    // Zurück-Taste des Browsers schließt eine Ebene
    LaunchedEffect(nav.stack.size) { syncHistory(nav.stack.size) { nav.back() } }

    val sfx = remember { WebSfx(prefs) }
    val speaker = remember { object : Speaker {
        override fun say(text: String) { if (prefs.voice.value && prefs.volume.value > 0) Speech.clean(text).takeIf { it.isNotEmpty() }?.let { speak(it, prefs.volume.value / 100.0) } }
        override fun sayIfQuiet(text: String) { if (!speaking()) say(text) }
    } }
    CompositionLocalProvider(LocalSfx provides sfx, LocalVibration provides vibration, LocalSpeaker provides speaker,
        LocalBuzzer provides Buzzer { b, on -> if (on) vibrate(when (b) { Buzz.TICK -> 8; Buzz.HIT -> 18; Buzz.CRASH -> 40 }) },
        LocalShareText provides { text -> shareText(text) }) {
        RaumlichtTheme { Surface(Modifier.fillMaxSize(), color = Q.Space) {
            AppRoot(nav, prefs, stars.all, stars::record, { todayLocal().toLong() },
                onRoute = { atHome(it == Routes.HOME) },
                settings = { onBack ->
                    val music by prefs.music.collectAsState(); val sound by prefs.sound.collectAsState(); val volume by prefs.volume.collectAsState(); val voice by prefs.voice.collectAsState()
                    SettingsPage(music, prefs::setMusic, sound, prefs::setSound, vibration, prefs::setVibration, "Version ${Version.NAME} vom ${Version.DATE} · WebApp", onBack,
                        top = {
                            ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton({ openLink("https://www.mathenachhilfe-online.com/impressum/") }) { Text("Impressum") }
                                TextButton({ openLink("https://www.mathenachhilfe-online.com/datenschutz/") }) { Text("Datenschutz") }
                            } }
                        },
                        onReset = { stars.clear(); prefs.resetProgress() }, volume = volume, onVolume = prefs::setVolume,
                        voice = voice, onVoice = { prefs.setVoice(it); if (!it) stopSpeaking() }, voiceAvailable = canSpeak(), hasMusic = false,
                        privacy = "Raumlicht speichert nur den Spielstand in diesem Browser (auf diesem Gerät). Keine Anmeldung, keine Werbung, keine Käufe, keine Daten an einen Server. " +
                            "Die App aktualisiert sich von selbst; gesprochen wird mit der Sprachausgabe deines Browsers.")
                })
        } }
    }
}
