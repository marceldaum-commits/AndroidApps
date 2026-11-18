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
    /** Vorlesen überhaupt angeboten ([de.kurvenflug.core.model.Nova.ENABLED]; Bildschirmfoto-Test kann es einschalten) */
    readAloudAvailable: Boolean = de.kurvenflug.core.model.Nova.ENABLED,
    readSpeed: Int = 100, onReadSpeed: (Int) -> Unit = {},
    offlineBest: Boolean = false, onOfflineBest: (Boolean) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val platform = de.kurvenflug.app.LocalPlatform.current
            if (platform.selfUpdate) PlatformUpdateSection(installed, update, autoUpdate, onAutoUpdate)
            // Welche Version läuft? (WebApp und Play: kein Update-Knopf, aber die Nummer soll sichtbar sein)
            else if (installed.isNotBlank()) Text("Kurvenflug Version $installed" + if (platform.isWeb) " · aktualisiert sich von selbst" else " · Updates über Google Play",
                style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            if (onPro != null) ProCard(de.kurvenflug.app.LocalPro.current, onPro)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Toggle("🎵 Musik", "Synthwave im All – jedes Kapitel klingt anders", music, onMusic)
                    if (music) Volume("Lautstärke", musicVolume, onMusicVolume)
                    Toggle("🔊 Geräusche", "Start, Flug, Treffer, Sterne, Volts Robo-Stimme", sound, onSound)
                    if (sound) Volume("Lautstärke", sfxVolume, onSfxVolume)
                    Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
                    if (!readAloud || !readAloudAvailable) Toggle("🤖 Volt-Stimme", "Volt plappert in Roboter-Sprache, wenn er spricht", voltVoice, onVoltVoice)
                }
            }
            if (readAloudAvailable) ReadAloudCard(readAloud, onReadAloud, readSpeed, onReadSpeed, offlineBest, onOfflineBest)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Für Eltern & Lehrkräfte", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    val shop = de.kurvenflug.app.LocalPro.current.locking
                    if (shop) Text("Kurvenflug ist kostenlos bis Klasse 10. Optional gibt es „Kurvenflug Pro“ als Einmalkauf ohne Abo " +
                        "(Oberstufe, Extrem-Stufen, mehr Spieler). Kaufen kann man nur nach einer Frage für Erwachsene.", style = MaterialTheme.typography.bodySmall)
                    val nova = readAloudAvailable
                    Text(if (platform.isWeb) "Kurvenflug speichert den Spielstand nur hier in diesem Browser. Keine Anmeldung, keine Werbung, " +
                        "keine Spieldaten an einen Server." + (if (nova) " Nur das Vorlesen (Nova, abschaltbar) lädt Audiodateien von unserem Server." else "") +
                        " Neue Versionen lädt die Seite von selbst."
                        else if (!platform.selfUpdate) "Kurvenflug speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung. " +
                        (if (nova) "Ins Internet geht die App nur für die Vorlese-Stimme Nova (abschaltbar). " else "Es gehen keine Spieldaten ins Internet. ") +
                        "Neue Versionen kommen über Google Play."
                        else "Kurvenflug speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung. " +
                        (if (nova) "Ins Internet geht die App nur, um nach Updates zu schauen und für die Vorlese-Stimme Nova (beides abschaltbar)."
                        else "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar)."), style = MaterialTheme.typography.bodySmall)
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
 * Vorlesen: an/aus und Probe. Stimme ist immer Nova (Vorgabe Marcel) – mit Pflicht-Kennzeichnung „KI-generiert“ und dem
 * Datenschutz-Text, den der Leitstand vorgegeben hat.
 */
@Composable
private fun ReadAloudCard(on: Boolean, onOn: (Boolean) -> Unit, speed: Int, onSpeed: (Int) -> Unit, best: Boolean = false, onBest: (Boolean) -> Unit = {}) {
    val reader = de.kurvenflug.app.ui.fx.LocalReader.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vorlesen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Toggle("🗣️ Volt liest vor", "Sprechblasen, Aufgaben, Rechen-Stern-Fragen und Erklärungen – Formeln richtig gesprochen. Tipp auf 🔊 zum Wiederholen.", on, onOn)
            if (!on) return@Column
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stimme:", style = MaterialTheme.typography.labelLarge)
                Text("✨ ${de.kurvenflug.core.model.Nova.LABEL}", style = MaterialTheme.typography.labelLarge, color = Q.Cyan,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Q.Cyan.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            // Sprechtempo in festen Stufen (gemerkt); 1,0 = das gewählte Server-Tempo
            val steps = de.kurvenflug.core.model.Nova.SPEEDS
            val idx = steps.indexOf(speed).coerceAtLeast(0)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                Text("Sprechtempo ${de.kurvenflug.core.model.Nova.speedLabel(speed)}",
                    style = MaterialTheme.typography.bodySmall, color = Q.Muted, modifier = Modifier.width(120.dp), maxLines = 1)
                Slider(value = idx.toFloat(), onValueChange = { onSpeed(steps[it.toInt().coerceIn(0, steps.lastIndex)]) },
                    valueRange = 0f..steps.lastIndex.toFloat(), steps = steps.size - 2, modifier = Modifier.weight(1f))
            }
            if (reader != null) OutlinedButton(onClick = { reader.say(de.kurvenflug.app.ui.fx.Reader.PROBE) }) { Text("▶ Probe hören") }
            if (reader != null) OfflineBox(reader, best, onBest)
            Text("🔒 $NOVA_PRIVACY", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
    }
}

/**
 * „Alle Nova-Texte offline laden“ – Standard (40 kbit/s) oder Bestqualität (128 kbit/s, unkomprimiert); die Wahl wird gemerkt.
 * Größe live vom Server, Fortschritt „x von n“, Abbrechen, Löschen (beide Qualitäten).
 */
@Composable
private fun OfflineBox(reader: de.kurvenflug.app.ui.fx.Reader, best: Boolean, onBest: (Boolean) -> Unit) {
    LaunchedEffect(Unit) { reader.refreshOffline() }
    val o = reader.offline
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Q.Cyan.copy(alpha = 0.08f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("📦 Offline-Stimme", style = MaterialTheme.typography.labelLarge)
        if (o.loading) {
            Text((if (best) "Bestqualität: " else "Standard: ") + (if (o.todo > 0) "lade … ${o.done} von ${o.todo}" else "lade …"), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { if (o.todo > 0) o.done / o.todo.toFloat() else 0f }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { reader.cancelOffline() }, contentPadding = PaddingValues(0.dp)) { Text("Abbrechen", color = Q.Muted) }
        } else {
            if (o.complete(best)) Text("✓ Alle ${o.total} Texte sind gespeichert (${if (best) "Bestqualität" else "Standard"}) – Volt spricht sie auch ohne Internet.",
                style = MaterialTheme.typography.bodySmall, color = Q.Green)
            else Text("Volt kann die festen Texte auch ohne Internet vorlesen. Am besten im WLAN laden." +
                if (o.count(best) > 0) " Schon gespeichert: ${o.count(best)} von ${o.total}." else "", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            @Composable fun Choice(b: Boolean, label: String) {
                val text = "Offline laden – $label" + (o.missingBytes(b)?.let { " (ca. ${de.kurvenflug.app.ui.fx.mb(it)} MB)" } ?: "")
                val go = { onBest(b); reader.loadOffline(b) }
                if (o.complete(b)) return
                if (b == best) Button(onClick = go, modifier = Modifier.fillMaxWidth()) { Text(text) }
                else OutlinedButton(onClick = go, modifier = Modifier.fillMaxWidth()) { Text(text) }
            }
            Choice(false, "Standard"); Choice(true, "Bestqualität")
        }
        o.problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Q.Orange) }
        if (o.stored > 0 && !o.loading) TextButton(onClick = { reader.clearOffline() }, contentPadding = PaddingValues(0.dp)) {
            Text("Offline-Stimme löschen (${de.kurvenflug.app.ui.fx.mb(o.haveBytes)} MB)", color = Q.Muted)
        }
    }
}

/** Datenschutz-Text zur Stimme Nova (Wortlaut Leitstand 17.11.) */
const val NOVA_PRIVACY = "Vorgelesen wird mit der Stimme Nova, einer mit KI (OpenAI) erzeugten Stimme. Feste Texte vertonen wir vorab. " +
    "Texte, die erst in der Aufgabe entstehen (z. B. Rechenaufgaben), schickt die App ohne Angaben zu dir an unseren Server " +
    "stimmen.mathenachhilfe-online.com; dieser lässt sie einmalig von OpenAI vertonen und speichert die Audiodatei. Es werden keine " +
    "Namen, Konten oder Geräte-Kennungen übertragen, und unser Server speichert keine Zugriffsprotokolle."

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

private val LICENSES = "Kurvenflug nutzt freie Software. Danke an alle, die sie bauen!\n\n" +
    "Apache License 2.0 (apache.org/licenses/LICENSE-2.0): Kotlin, kotlinx.coroutines, kotlinx.serialization, " +
    "Jetpack Compose und Compose Multiplatform, AndroidX (Activity, Core, Lifecycle, Navigation, Room, Glance, WorkManager, " +
    "Fragment, SavedState, Startup, DataStore), Guava, JSpecify, JetBrains Annotations.\n\n" +
    "Nur in der Google-Play-Version: Play In-App Updates und Play Billing (Nutzungsbedingungen von Google), " +
    "Google Data Transport, Firebase Encoders und javax.inject (Apache License 2.0)." +
    (if (de.kurvenflug.core.model.Nova.ENABLED) "\n\nVorlese-Stimme Nova: KI-generierte Audiodateien (OpenAI), erzeugt auf unserem Server." else "")

private const val LICENSES_WEB = "\n\nIm Browser zusätzlich: Skia/Skiko (BSD-3-Clause), Noto Color Emoji (SIL Open Font License 1.1), " +
    "DejaVu Sans (freie Bitstream-Vera-Lizenz). Volltexte in fonts-lizenzen.txt."

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
