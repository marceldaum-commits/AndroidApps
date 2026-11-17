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
import de.raumlicht.core.model.MusicMood
import de.raumlicht.core.model.MusicSynth
import de.raumlicht.core.model.SpaceMusic
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
    var ready by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(2) }; var fonts by remember { mutableStateOf(listOf<Pair<String, ByteArray>>()) }
    LaunchedEffect(Unit) { listOf("DejaVuSans" to "fonts/dejavu_sans.ttf", "NotoColorEmoji" to "fonts/noto_color_emoji.ttf").filter { (name, _) -> !(name == "NotoColorEmoji" && without("emoji")) }.also { pending = it.size }.forEach { (name, url) -> loadFile(url) { b -> if (b != null) fonts = fonts + (name to b) else pending-- } } }
    LaunchedEffect(fonts.size, pending) {
        if (fonts.size < pending) return@LaunchedEffect
        fonts.forEach { (name, bytes) -> runCatching { resolver.preload(FontFamily(Font(name, bytes))) }.onFailure { note("Schrift $name: $it") } }
        note("Schriften geladen: ${fonts.map { it.first }}")
        ready = true; hideStartScreen()
    }
    if (!ready) return
    LaunchedEffect(Unit) { note("Oberfläche gestartet") }
    // Direkt-Link auf einen Bildschirm („?ziel=binomial“) – für Lesezeichen und für die automatische Prüfung jedes Bildschirms; unbekannte Ziele führen zur Startseite
    LaunchedEffect(Unit) { takeParam("ziel")?.takeIf { it.isNotBlank() && it.length < 60 }?.let { nav.restart(it) } }
    // Freischalt-Link aus der E-Mail: Code prüfen, merken und die Abi-Pass-Seite zeigen (bei einem falschen Code steht dort die Eingabe bereit)
    LaunchedEffect(Unit) { takeProParam()?.let { raw ->
        val code = de.raumlicht.core.model.License.extract("RL$raw") ?: de.raumlicht.core.model.License.extract(raw)
        if (de.raumlicht.core.model.Pro.active(code)) prefs.setProCode(code!!)
        note("Freischalt-Link: " + if (de.raumlicht.core.model.Pro.active(code)) "Code angenommen" else "Code nicht erkannt"); nav.restart(Routes.PRO) } }
    // Musik wie in der Android-App: ruhig im Menü, flotter bei Fahrt und Flug
    var mood by remember { mutableStateOf(MusicMood.MENU) }
    val musicOn by prefs.music.collectAsState(); val loud by prefs.volume.collectAsState()
    LaunchedEffect(Unit) { if (!without("ton")) musicSetup { name -> val pcm = MusicSynth.render(SpaceMusic.compose(MusicMood.valueOf(name))); val b = newSamples(pcm.size); for (i in pcm.indices) putSample(b, i, pcm[i] / 32768f); musicStore(name, b) } }
    LaunchedEffect(musicOn, loud, mood) { musicWant(if (musicOn) mood.name else null, 0.5 * loud / 100.0) }
    // Zurück-Taste des Browsers schließt eine Ebene
    LaunchedEffect(nav.stack.size) { syncHistory(nav.stack.size) { nav.back() } }

    val sfx = remember { WebSfx(prefs) }
    val speaker = remember { object : Speaker {
        override fun say(text: String) { if (prefs.voice.value && prefs.volume.value > 0) Speech.clean(text).takeIf { it.isNotEmpty() }?.let { speak(it, prefs.volume.value / 100.0, prefs.speechRate.value / 100.0, prefs.voiceName) } }
        override fun sayIfQuiet(text: String) { if (!speaking()) say(text) }
    } }
    CompositionLocalProvider(LocalSfx provides sfx, LocalVibration provides vibration, LocalSpeaker provides speaker,
        LocalBuzzer provides Buzzer { b, on -> if (on) vibrate(when (b) { Buzz.TICK -> 8; Buzz.HIT -> 18; Buzz.CRASH -> 40 }) },
        LocalShareText provides { text -> shareText(text) }) {
        RaumlichtTheme { Surface(Modifier.fillMaxSize(), color = Q.Space) {
            AppRoot(nav, prefs, stars.all, stars::record, { todayLocal().toLong() },
                onRoute = { atHome(it == Routes.HOME); mood = if (Routes.isAction(it)) MusicMood.FLIGHT else MusicMood.MENU },
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
                        voice = voice, onVoice = { prefs.setVoice(it); if (!it) stopSpeaking() }, voiceAvailable = canSpeak(),
                        voiceControls = {
                            // Stimme wählen: durch die deutschen Stimmen des Geräts schalten – jede stellt sich gleich vor
                            val names = remember { localVoices().split('\n').filter { it.isNotBlank() } }
                            var current by remember { mutableStateOf(prefs.voiceName.takeIf { it in names } ?: names.firstOrNull().orEmpty()) }
                            Column(Modifier.padding(bottom = 6.dp)) {
                                OutlinedButton({ if (names.size > 1) { current = names[(names.indexOf(current) + 1) % names.size]; prefs.voiceName = current; speaker.say("Hallo, ich bin Lux. So klinge ich jetzt.") } }, enabled = names.size > 1) {
                                    Text(if (names.size > 1) "Andere Stimme (${names.indexOf(current) + 1} von ${names.size})" else "Nur eine Stimme vorhanden") }
                                if (current.isNotBlank()) Text("Stimme des Geräts: $current", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                            }
                        },
                        onLicenses = { openLink("lizenzen.txt") },
                        privacy = "Raumlicht speichert nur den Spielstand in diesem Browser (auf diesem Gerät). Keine Anmeldung, keine Werbung, keine Käufe, keine Daten an einen Server. " +
                            "Die App aktualisiert sich von selbst. Musik und Klänge entstehen im Browser. Schaltest du „Lux spricht“ ein, liest eine Stimme vor, die auf deinem Gerät eingebaut ist.")
                })
        } }
    }
}
