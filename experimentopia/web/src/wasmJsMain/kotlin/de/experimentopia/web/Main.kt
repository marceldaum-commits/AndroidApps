package de.experimentopia.web

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
import de.experimentopia.app.ui.AppRoot
import de.experimentopia.app.ui.AppState
import de.experimentopia.app.ui.Navigator
import de.experimentopia.app.ui.Route
import de.experimentopia.app.ui.common.Panel
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.ExperimentopiaTheme
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.getElementById("app")!!) { WebApp() }
}

/** Dieselben im Code erzeugten Klänge wie in der Android-App ([Synth]), über WebAudio abgespielt */
private class WebSfx(private val prefs: WebPrefs) : SoundPlayer {
    private fun ensure(key: String, make: () -> FloatArray): Boolean {
        if (!audioReady()) return false
        if (!hasBuffer(key)) { val pcm = make(); val b = newSamples(pcm.size); for (i in pcm.indices) putSample(b, i, pcm[i]); storeBuffer(key, b, Synth.SR) }
        return true
    }
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!prefs.sound.value) return
        if (ensure(s.name) { Synth.render(s) }) playBuffer(s.name, pitch.toDouble(), 0.78 * volume * prefs.audio.value.sfxVol)
    }
    override fun tone(hz: Double, timbre: Timbre) {
        if (!prefs.sound.value) return
        val key = "${timbre.name}_${hz.toInt()}"
        if (ensure(key) { Synth.tone(hz, timbre) }) playBuffer(key, 1.0, 0.78 * prefs.audio.value.sfxVol)
    }
}

@Composable
private fun WebApp() {
    val prefs = remember { WebPrefs() }; val store = remember { WebStore() }; val nav = remember { Navigator() }
    val scope = rememberCoroutineScope()
    val state = remember { AppState(store, prefs, scope, { todayLocal().toLong() }, { dayStartLocal().toLong() }) }
    // Forscher-Pass: Schlüssel/Kasse eintragen; Freischalt-Link aus der E-Mail übernehmen
    remember {
        de.experimentopia.core.model.Pro.configure(Version.PRO_KEY, Version.PRO_CHECKOUT)
        de.experimentopia.core.model.License.extract(locationSearch())?.let { c -> if (de.experimentopia.core.model.Pro.valid(c, todayLocal().toLong())) prefs.setProCode(c); clearSearch() }
        true
    }
    val vibration by prefs.vibration.collectAsState()
    // Schriften als Ergänzung zur Standardschrift: Sonderzeichen (₂, →, Ω …) und Emojis. Die Oberfläche startet erst, wenn beide da sind.
    // Fehlt eine Datei, läuft die App trotzdem (dann mit Kästchen statt Zeichen).
    val resolver = LocalFontFamilyResolver.current
    var ready by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(2) }; var fonts by remember { mutableStateOf(listOf<Pair<String, ByteArray>>()) }
    LaunchedEffect(Unit) { listOf("DejaVuSans" to "fonts/dejavu_sans.ttf", "NotoColorEmoji" to "fonts/noto_color_emoji.ttf").forEach { (name, url) -> loadFile(url) { b -> if (b != null) fonts = fonts + (name to b) else pending-- } } }
    LaunchedEffect(fonts.size, pending) {
        if (fonts.size < pending) return@LaunchedEffect
        fonts.forEach { (name, bytes) -> runCatching { resolver.preload(FontFamily(Font(name, bytes))) } }
        ready = true; hideStartScreen(); wakeAudioOnTouch()
    }
    if (!ready) return
    // Zurück-Taste des Browsers schließt eine Ebene
    LaunchedEffect(nav.stack.size) { syncHistory(nav.stack.size) { nav.back() } }
    val sfx = remember { WebSfx(prefs) }
    val music = remember { WebMusic(scope) }
    val reader = remember { WebReader() }
    CompositionLocalProvider(LocalSfx provides sfx, LocalVibration provides vibration, LocalMusic provides music, LocalReader provides reader, LocalOpenLink provides { url -> openLink(url) },
        LocalBuzzer provides Buzzer { b, on -> if (on) vibrate(when (b) { Buzz.TICK -> 8; Buzz.HIT -> 18; Buzz.BIG -> 40 }) }) {
        ExperimentopiaTheme { Surface(Modifier.fillMaxSize(), color = E.Night) {
            AppRoot(nav, state, Version.NAME, Version.DATE,
                settingsTop = {
                    Panel(Modifier.fillMaxWidth()) {
                        Text("WebApp – aktualisiert sich von selbst", color = E.Mint)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton({ openLink("https://www.mathenachhilfe-online.com/impressum/") }) { Text("Impressum") }
                            TextButton({ openLink("https://www.mathenachhilfe-online.com/datenschutz/") }) { Text("Datenschutz") }
                        }
                    }
                },
                privacy = "Experimentopia speichert den Spielstand und das Forscherbuch nur in diesem Browser (auf diesem Gerät). Keine Anmeldung, keine Werbung, " +
                    "keine Daten an einen Server. " + de.experimentopia.core.model.Pro.purchaseNote + " Die App aktualisiert sich von selbst. Alle Klänge und die Musik entstehen im Browser. " +
                    "Nur wenn du die Stimme „Thorsten“ auswählst und laden lässt, kommt einmal eine Stimmdatei von unserem Stimmen-Server (stimmen.mathenachhilfe-online.com, ca. 75 MB mit Rechenbaustein); sie bleibt in diesem Browser.",
                onRoute = { atHome(it == Route.Home) })
        } }
    }
}
