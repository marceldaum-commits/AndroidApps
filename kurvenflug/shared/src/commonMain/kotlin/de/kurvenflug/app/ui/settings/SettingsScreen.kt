package de.kurvenflug.app.ui.settings

import de.kurvenflug.app.ui.fx.rememberBuzzer
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.pro.kindText

@Composable
fun SettingsScreen(
    music: Boolean = true, onMusic: (Boolean) -> Unit = {},
    musicVolume: Int = 70, onMusicVolume: (Int) -> Unit = {},
    sfxVolume: Int = 90, onSfxVolume: (Int) -> Unit = {},
    sound: Boolean, onSound: (Boolean) -> Unit,
    vibration: Boolean, onVibration: (Boolean) -> Unit,
    autoUpdate: Boolean, onAutoUpdate: (Boolean) -> Unit,
    installed: String,
    update: UpdateState,
    onBack: () -> Unit,
    /** Neu anfangen: Spielstand löschen (nach 2 s Gedrückthalten) */
    onReset: (() -> Unit)? = null,
    /** Kurvenflug Pro öffnen (null = kein Kaufweg auf dieser Plattform) */
    onPro: (() -> Unit)? = null,
    voltVoice: Boolean = true, onVoltVoice: (Boolean) -> Unit = {},
    readAloud: Boolean = true, onReadAloud: (Boolean) -> Unit = {},
    readVoice: String = "", onReadVoice: (String) -> Unit = {},
    readRate: Int = 100, onReadRate: (Int) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val platform = de.kurvenflug.app.LocalPlatform.current
            if (platform.selfUpdate) PlatformUpdateSection(installed, update, autoUpdate, onAutoUpdate)
            if (onPro != null) ProCard(de.kurvenflug.app.LocalPro.current, onPro)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Toggle("🎵 Musik", "Synthwave im All – jedes Kapitel klingt anders", music, onMusic)
                    if (music) Volume("Lautstärke", musicVolume, onMusicVolume)
                    Toggle("🔊 Geräusche", "Start, Flug, Treffer, Sterne, Volts Robo-Stimme", sound, onSound)
                    if (sound) Volume("Lautstärke", sfxVolume, onSfxVolume)
                    Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
                    if (!readAloud) Toggle("🤖 Volt-Stimme", "Volt plappert in Roboter-Sprache, wenn er spricht", voltVoice, onVoltVoice)
                }
            }
            ReadAloudCard(readAloud, onReadAloud, readVoice, onReadVoice, readRate, onReadRate)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Für Eltern & Lehrkräfte", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    val shop = de.kurvenflug.app.LocalPro.current.locking
                    if (shop) Text("Kurvenflug ist kostenlos bis Klasse 10. Optional gibt es „Kurvenflug Pro“ als Einmalkauf ohne Abo " +
                        "(Oberstufe, Extrem-Stufen, mehr Spieler). Kaufen kann man nur nach einer Frage für Erwachsene.", style = MaterialTheme.typography.bodySmall)
                    Text(if (platform.isWeb) "Kurvenflug speichert den Spielstand nur hier in diesem Browser. Keine Anmeldung, keine Werbung, " +
                        "nichts wird an einen Server geschickt. Neue Versionen lädt die Seite von selbst."
                        else if (!platform.selfUpdate) "Kurvenflug speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung. " +
                        "Ins Internet geht die App nur, wenn Sie die Vorlese-Stimme „Thorsten“ laden. Neue Versionen kommen über Google Play."
                        else "Kurvenflug speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung. " +
                        "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar).", style = MaterialTheme.typography.bodySmall)
                    Text("Inhalte: Koordinaten, Brüche und Kommazahlen, Winkel, Maßstab (Kl. 5/6), Zuordnungen, Prozent und Zinseszins (Kl. 6–8), " +
                        "lineare Funktionen und Gleichungssysteme (Kl. 7/8), Pythagoras (Kl. 8/9), quadratische Funktionen und Nullstellen (Kl. 9), " +
                        "Trigonometrie, Potenzfunktionen, Sinus, Exponential- und Logarithmusfunktionen (Kl. 9/10); Oberstufe: Ableitung, Extrempunkte, " +
                        "Integral, Vektoren, e-Funktion, Binomialverteilung (Kl. 11–13). Physik: gleichförmige und beschleunigte Bewegung, Reflexion, " +
                        "Lichtbrechung, Energieerhaltung, schräger und waagerechter Wurf, Gravitation.", style = MaterialTheme.typography.bodySmall)
                    Text("Version $installed" + if (platform.isWeb) " · WebApp" else "", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                    if (platform.isWeb) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { platform.openUrl(IMPRESSUM) }) { Text("Impressum") }
                        TextButton(onClick = { platform.openUrl(DATENSCHUTZ) }) { Text("Datenschutz") }
                    }
                }
            }
            LicenseCard(platform.isWeb)
            if (onReset != null) ResetCard(onReset)
        }
    }
}

@Composable
private fun Volume(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        Text("$label $value %", style = MaterialTheme.typography.bodySmall, color = Q.Muted, modifier = Modifier.width(120.dp), maxLines = 1)
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..100f, steps = 9, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProCard(state: de.kurvenflug.app.ProState, onPro: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.clickable { onPro() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⭐", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kurvenflug Pro", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    de.kurvenflug.app.ui.pro.ProBadge(state, Modifier.padding(start = 8.dp))
                }
                Text(if (state.pro) state.kindText() + " · danke! 💛" else "Oberstufe, Extrem-Stufen, mehr Spieler" + if (de.kurvenflug.app.LocalPlatform.current.proShop?.acceptsCodes == true) " · Code einlösen" else "",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("›", fontSize = 24.sp, color = Q.Muted)
        }
    }
}

/**
 * Vorlesen: an/aus, Stimme (Gerät/Browser oder „Thorsten“), Tempo, Probe. Thorsten wird nur auf Wunsch geladen –
 * mit klarem Hinweis, woher die Datei kommt.
 */
@Composable
private fun ReadAloudCard(on: Boolean, onOn: (Boolean) -> Unit, voice: String, onVoice: (String) -> Unit, rate: Int, onRate: (Int) -> Unit) {
    val reader = de.kurvenflug.app.ui.fx.LocalReader.current
    val web = de.kurvenflug.app.LocalPlatform.current.isWeb
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vorlesen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Toggle("🗣️ Volt liest vor", "Sprechblasen, Aufgaben, Rechen-Stern-Fragen und Erklärungen – Formeln richtig gesprochen. Tipp auf 🔊 zum Wiederholen.", on, onOn)
            if (!on) return@Column
            if (reader == null) { Text("Vorlesen ist hier nicht möglich.", style = MaterialTheme.typography.bodySmall, color = Q.Muted); return@Column }
            Text("Stimme", style = MaterialTheme.typography.labelLarge)
            val pack = reader.pack
            val choices = listOf(de.kurvenflug.app.ui.fx.VoiceChoice("", "Automatisch")) + reader.voices +
                (if (reader.canLoadPack || pack == de.kurvenflug.app.ui.fx.PackState.Ready)
                    listOf(de.kurvenflug.app.ui.fx.VoiceChoice(de.kurvenflug.app.ui.fx.Reader.NATURAL, "✨ " + de.kurvenflug.app.ui.fx.Reader.NATURAL_LABEL)) else emptyList())
            val current = choices.firstOrNull { it.id == voice }?.id ?: ""
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                choices.forEach { c -> FilterChip(selected = c.id == current, onClick = { onVoice(c.id) }, label = { Text(c.label, maxLines = 1) }) }
            }
            if (reader.voices.isEmpty()) Text(
                (if (web) "Dieser Browser hat keine deutsche Stimme, die ohne Internet spricht." else "Auf diesem Gerät ist keine deutsche Stimme installiert (Einstellungen → Sprachausgabe).") +
                    if (reader.canLoadPack) " Nimm „Thorsten“ – der spricht immer." else "",
                style = MaterialTheme.typography.bodySmall, color = Q.Orange)
            if (current == de.kurvenflug.app.ui.fx.Reader.NATURAL) NaturalVoiceBox(reader, web)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                Text("Tempo $rate %", style = MaterialTheme.typography.bodySmall, color = Q.Muted, modifier = Modifier.width(120.dp), maxLines = 1)
                Slider(value = rate.toFloat(), onValueChange = { onRate(it.toInt()) }, valueRange = 70f..130f, steps = 11, modifier = Modifier.weight(1f))
            }
            OutlinedButton(onClick = { reader.say(de.kurvenflug.app.ui.fx.Reader.PROBE) }) { Text("▶ Probe hören") }
            Text(if (web) "Die Stimmen des Browsers rechnen hier auf dem Gerät – es geht kein Text ins Internet."
                else "Gelesen wird mit der Sprachausgabe des Geräts (nur Stimmen ohne Internet) – es geht kein Text nach draußen.",
                style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
    }
}

/** „Thorsten“: laden (mit Datenschutz-Hinweis), Fortschritt, entfernen */
@Composable
private fun NaturalVoiceBox(reader: de.kurvenflug.app.ui.fx.Reader, web: Boolean) {
    val mb = de.kurvenflug.app.ui.fx.Reader.PACK_MB
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Q.Cyan.copy(alpha = 0.08f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (val p = reader.pack) {
            de.kurvenflug.app.ui.fx.PackState.Ready -> {
                Text("✓ Thorsten ist geladen und spricht ohne Internet.", style = MaterialTheme.typography.bodySmall, color = Q.Green)
                Text("Unbekannte Wörter (z. B. Spielernamen) liest die normale Stimme.", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                TextButton(onClick = { reader.removePack() }, contentPadding = PaddingValues(0.dp)) { Text("Stimme entfernen (Speicher freigeben)", color = Q.Muted) }
            }
            is de.kurvenflug.app.ui.fx.PackState.Loading -> {
                Text("Lade Thorsten … ${p.percent} %", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { p.percent / 100f }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { reader.cancelPack() }, contentPadding = PaddingValues(0.dp)) { Text("Abbrechen", color = Q.Muted) }
            }
            else -> {
                Text("Thorsten klingt wie ein echter Mensch und rechnet ganz auf ${if (web) "deinem Gerät" else "dem Handy"}. " +
                    "Dafür wird einmalig eine Stimmdatei geladen (ca. $mb MB – am besten im WLAN).", style = MaterialTheme.typography.bodySmall)
                Text("🔒 Für Eltern: Die Datei kommt von unserem eigenen Server (${de.kurvenflug.app.ui.fx.Reader.PACK_HOST}), nicht von fremden Anbietern. " +
                    "Es werden keine Texte und keine Daten über Ihr Kind gesendet. Danach bleibt alles ${if (web) "in diesem Browser" else "auf dem Gerät"} und funktioniert ohne Internet.", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                if (p is de.kurvenflug.app.ui.fx.PackState.Failed) Text("Hat nicht geklappt: ${p.why}", style = MaterialTheme.typography.bodySmall, color = Q.Orange)
                Button(onClick = { reader.loadPack() }) { Text(if (p is de.kurvenflug.app.ui.fx.PackState.Failed) "Nochmal versuchen" else "Thorsten laden (ca. $mb MB)") }
            }
        }
    }
}

/** Open-Source-Lizenzen (Apache 2.0 verlangt den Hinweis) – aufklappbar, damit es Kinder nicht stört. */
@Composable
private fun LicenseCard(web: Boolean) {
    var open by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { open = !open }, contentPadding = PaddingValues(0.dp)) {
                Text((if (open) "▾ " else "▸ ") + "Open-Source-Lizenzen", style = MaterialTheme.typography.titleSmall)
            }
            if (open) Text(LICENSES + if (web) LICENSES_WEB else "", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
    }
}

private const val LICENSES = "Kurvenflug nutzt freie Software. Danke an alle, die sie bauen!\n\n" +
    "Apache License 2.0 (apache.org/licenses/LICENSE-2.0): Kotlin, kotlinx.coroutines, kotlinx.serialization, " +
    "Jetpack Compose und Compose Multiplatform, AndroidX (Activity, Core, Lifecycle, Navigation, Room, Glance, WorkManager, " +
    "Fragment, SavedState, Startup, DataStore), Guava, JSpecify, JetBrains Annotations.\n\n" +
    "Vorlesen mit „Thorsten“: ONNX Runtime (MIT), Stimme de_DE-thorsten-medium aus dem Piper-Projekt " +
    "(Stimmdaten von Thorsten Müller, CC0; Modell MIT). Die Lautschrift ist vorab erzeugt – kein espeak-ng in der App.\n\n" +
    "Nur in der Google-Play-Version: Play In-App Updates (Play Core SDK, Nutzungsbedingungen von Google)."

private const val LICENSES_WEB = "\n\nIm Browser zusätzlich: Skia/Skiko (BSD-3-Clause), Noto Color Emoji (SIL Open Font License 1.1), " +
    "DejaVu Sans (freie Bitstream-Vera-Lizenz). Volltexte in fonts-lizenzen.txt. " +
    "Vorlesen mit „Thorsten“ im Browser: ONNX Runtime Web (MIT), Hinweis in ort/LIZENZ.txt."

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Switch(on, onChange)
    }
}


/**
 * Neu anfangen – z. B. wenn ein Geschwisterkind übernimmt. Gegen Versehen: 2 Sekunden gedrückt halten,
 * ein Balken läuft voll; loslassen bricht ab.
 */
@Composable
private fun ResetCard(onReset: () -> Unit) {
    val sfx = de.kurvenflug.app.ui.fx.LocalSfx.current
    val view = de.kurvenflug.app.ui.fx.rememberBuzzer()
    val vib = de.kurvenflug.app.ui.fx.LocalVibration.current
    val fill = remember { androidx.compose.animation.core.Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔄 Neu anfangen", style = MaterialTheme.typography.titleMedium, color = Q.Orange)
            Text("Löscht Sterne, Energiezellen, Tagesmissionen, Rekorde, Abzeichen und die gewählte Klasse – zum Beispiel, " +
                "wenn ein Geschwisterkind von vorn beginnen möchte. Ton-Einstellungen und selbst gebaute Level bleiben.",
                style = MaterialTheme.typography.bodySmall)
            if (done) Text("✅ Alles zurückgesetzt – viel Spaß beim Neustart! 🚀", color = Q.Green, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            else Box(
                Modifier.fillMaxWidth().height(52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Q.Orange.copy(alpha = 0.15f))
                    .border(2.dp, Q.Orange, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            sfx?.play(de.kurvenflug.app.ui.fx.Sound.TICK)
                            val job = scope.launch {
                                fill.animateTo(1f, androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing))
                                done = true
                                view.buzz(de.kurvenflug.app.ui.fx.Buzz.CRASH, vib)
                                sfx?.play(de.kurvenflug.app.ui.fx.Sound.WIN)
                                onReset()
                            }
                            tryAwaitRelease()
                            if (!done) { job.cancel(); scope.launch { fill.animateTo(0f) } }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fill.value).background(Q.Orange.copy(alpha = 0.5f)))
                Text(if (fill.value > 0f) "Festhalten … ${(3 - fill.value * 2).toInt().coerceAtLeast(1)}" else "Zum Löschen 2 Sekunden gedrückt halten",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Q.Text)
            }
        }
    }
}

const val IMPRESSUM = "https://www.mathenachhilfe-online.com/impressum/"
const val DATENSCHUTZ = "https://www.mathenachhilfe-online.com/datenschutz/"
