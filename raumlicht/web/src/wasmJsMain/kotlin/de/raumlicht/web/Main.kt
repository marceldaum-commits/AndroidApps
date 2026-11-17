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
import de.raumlicht.core.model.Klang
import de.raumlicht.core.model.MusicMood
import de.raumlicht.core.model.Ton
import de.raumlicht.core.model.MusicSynth
import de.raumlicht.core.model.SpaceMusic
import de.raumlicht.core.model.Speech
import de.raumlicht.core.model.Vorlesen
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // eigener Bereich für die Oberfläche: Compose blendet alles andere in seinem Element aus – die 3D-Bühne liegt deshalb daneben, nicht darin
    ComposeViewport(document.getElementById("app")!!) { WebApp() }
}

/** Kurze Klänge aus dem Browser-Synthesizer – dieselben Anlässe wie in der Android-App */
/** Dieselben Kurzklänge wie in der Android-App: im gemeinsamen Rechenkern erzeugt ([Klang]), beim ersten Gebrauch einmal berechnet */
private class WebSfx(private val prefs: WebPrefs) : SoundPlayer {
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!prefs.sound.value || without("ton")) return
        val g = 0.9 * volume * prefs.volume.value / 100.0 * prefs.soundLevel.value / 100.0; if (g <= 0) return
        if (!sfxKnown(s.name)) { val pcm = Klang.render(Ton.valueOf(s.name)); val b = newSamples(pcm.size); for (i in pcm.indices) putSample(b, i, pcm[i]); sfxStore(s.name, b) }
        sfxPlay(s.name, pitch.toDouble().coerceIn(0.5, 2.0), g.coerceAtMost(1.0))
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
    val musicLevel by prefs.musicLevel.collectAsState(); val voiceOn by prefs.voice.collectAsState()
    var exam by remember { mutableStateOf(false) }; var talking by remember { mutableStateOf(false) }
    // unter der Stimme wird die Musik leiser
    LaunchedEffect(voiceOn) { while (voiceOn) { talking = speaking() || novaSpeaking(); kotlinx.coroutines.delay(250) }; talking = false }
    // Musik: aus im Prüfungsmodus, eigene Lautstärke, leiser unter der Stimme
    LaunchedEffect(musicOn, loud, mood, musicLevel, exam, talking) { musicWant(if (musicOn && !exam) mood.name else null, 0.75 * loud / 100.0 * musicLevel / 100.0 * (if (talking) 0.35 else 1.0)) }
    // Zurück-Taste des Browsers schließt eine Ebene
    LaunchedEffect(nav.stack.size) { syncHistory(nav.stack.size) { nav.back() } }

    val sfx = remember { WebSfx(prefs) }
    val speaker = remember { object : Speaker {
        // „Nova“ (KI-generierte Stimme): fertige Datei von unserem Server; kommt sie nicht (offline), liest diesen Satz still die Stimme des Geräts
        override fun say(text: String) { if (prefs.voice.value && prefs.volume.value > 0) Vorlesen.spoken(text).takeIf { it.isNotEmpty() }?.let { spoken ->
            stopSpeaking(); val volume = prefs.volume.value / 100.0
            val rate = prefs.speechRate.value / 100.0
            novaSay(Vorlesen.SERVER, Vorlesen.url(spoken), if (spoken.length <= Vorlesen.LIVE_MAX_CHARS) Vorlesen.LIVE else "", Vorlesen.liveBody(spoken), volume, rate)
                .then { r -> if (r.toString() == "fehlt" && prefs.voice.value) speak(spoken, volume, rate); null } } }
        override fun sayIfQuiet(text: String) { if (!speaking() && !novaSpeaking()) say(text) }
    } }
    CompositionLocalProvider(LocalSfx provides sfx, LocalVibration provides vibration, LocalSpeaker provides speaker,
        LocalBuzzer provides Buzzer { b, on -> if (on) vibrate(when (b) { Buzz.TICK -> 8; Buzz.HIT -> 18; Buzz.CRASH -> 40 }) },
        LocalShareText provides { text -> shareText(text) }) {
        RaumlichtTheme { Surface(Modifier.fillMaxSize(), color = Q.Space) {
            AppRoot(nav, prefs, stars.all, stars::record, { todayLocal().toLong() },
                onRoute = { atHome(it == Routes.HOME); exam = Routes.isExam(it); mood = if (Routes.isAction(it)) MusicMood.FLIGHT else MusicMood.MENU },
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
                        voice = voice, onVoice = { prefs.setVoice(it); if (!it) { stopSpeaking(); novaStop() } },
                        onLicenses = { openLink("lizenzen.txt") },
                        soundNote = if (appleTouch()) "Kein Ton? Auf iPhone und iPad bleibt Raumlicht still, solange das Gerät stumm geschaltet ist (Schalter an der Seite oder Glocke im Kontrollzentrum)." else null,
                        privacy = "Raumlicht speichert nur den Spielstand in diesem Browser (auf diesem Gerät). Keine Anmeldung, keine Werbung, keine Käufe. " +
                            "Die App aktualisiert sich von selbst. Musik und Klänge entstehen im Browser. Ins Internet geht sie sonst nur zum Vorlesen, wenn du „Lux spricht“ einschaltest. " + Vorlesen.PRIVACY)
                })
        } }
    }
}

/** iPhone oder iPad (auch iPadOS, das sich als Mac ausgibt)? Dort schaltet der Stumm-Schalter auch den Ton von Webseiten ab. */
private fun appleTouch(): Boolean = js("/iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)")

// ---- Stimme „Nova“ (siehe resources/rl-nova.js)
private fun novaSay(server: String, url: String, live: String, body: String, volume: Double, rate: Double): kotlin.js.Promise<JsString> =
    js("(window.__rlNova ? window.__rlNova.say(server, url, live, body, volume, rate) : Promise.resolve('fehlt'))")
private fun novaStop(): Unit = js("{ if (window.__rlNova) window.__rlNova.stop(); }")
private fun novaSpeaking(): Boolean = js("!!(window.__rlNova && window.__rlNova.speaking)")
