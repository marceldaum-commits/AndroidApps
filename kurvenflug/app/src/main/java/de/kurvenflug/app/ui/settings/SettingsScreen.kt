package de.kurvenflug.app.ui.settings

import kotlinx.coroutines.launch
import de.kurvenflug.app.ui.fx.buzz
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import de.kurvenflug.app.BuildConfig
import de.kurvenflug.app.ui.theme.Q

@Composable
fun SettingsScreen(
    music: Boolean = true, onMusic: (Boolean) -> Unit = {},
    sound: Boolean, onSound: (Boolean) -> Unit,
    vibration: Boolean, onVibration: (Boolean) -> Unit,
    autoUpdate: Boolean, onAutoUpdate: (Boolean) -> Unit,
    installed: String,
    update: UpdateState,
    onBack: () -> Unit,
    /** Neu anfangen: Spielstand löschen (nach 2 s Gedrückthalten) */
    onReset: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (BuildConfig.SELF_UPDATE) UpdateSection(installed, update, autoUpdate, onAutoUpdate)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Toggle("🎵 Musik", "Leise Weltraum-Klänge im Hintergrund", music, onMusic)
                    Toggle("🔊 Geräusche", "Start, Treffer, Volts Piepsen", sound, onSound)
                    Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Für Eltern & Lehrkräfte", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Kurvenflug speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Käufe. " +
                        "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar).", style = MaterialTheme.typography.bodySmall)
                    Text("Inhalte: Koordinaten, Brüche und Kommazahlen, Winkel (Kl. 5/6), Zuordnungen und Prozent (Kl. 6/7), lineare Funktionen und Gleichungssysteme (Kl. 7/8), " +
                        "Pythagoras (Kl. 8/9), quadratische Funktionen und Nullstellen (Kl. 9), Trigonometrie (Kl. 9/10), Sinus, Exponential- und Logarithmusfunktionen (Kl. 10). " +
                        "Physik: gleichförmige Bewegung, Reflexion, Lichtbrechung, Energieerhaltung, schräger Wurf, Gravitation.", style = MaterialTheme.typography.bodySmall)
                    Text("Version $installed", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                }
            }
            if (onReset != null) ResetCard(onReset)
        }
    }
}

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
    val view = androidx.compose.ui.platform.LocalView.current
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
