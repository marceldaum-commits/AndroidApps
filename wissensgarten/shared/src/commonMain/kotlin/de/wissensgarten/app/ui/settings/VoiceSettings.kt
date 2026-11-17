package de.wissensgarten.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.AppHost
import de.wissensgarten.app.ui.fx.JuicyButton
import de.wissensgarten.app.ui.fx.NaturalVoicePack
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.app.ui.fx.VoicePackState
import de.wissensgarten.app.ui.fx.VoiceRoute
import de.wissensgarten.core.data.Settings
import de.wissensgarten.core.data.VOICE_THORSTEN

/** Stimme wählen (Gerät/Browser oder „Thorsten“), Tempo, Probe-Taste – unter „Brummi liest vor“. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettings(settings: Settings, host: AppHost) {
    VoiceSettingsPanel(settings, host.voice ?: return)
}

/** Inhalt der Stimm-Einstellungen (eigene Funktion, damit sie sich ohne Plattform zeigen lässt – Bildschirmfoto-Test) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsPanel(settings: Settings, voice: Voice) {
    val name by settings.voiceName.collectAsState()
    val rate by settings.voiceRate.collectAsState()
    val list by voice.voices.collectAsState()
    val pack = voice.natural
    fun choose(id: String) { settings.setVoiceName(id); voice.setVoice(id) }

    Spacer(Modifier.height(10.dp))
    Text("Stimme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val known = name.isEmpty() || name == VOICE_THORSTEN || list.any { it.id == name }
        FilterChip(selected = !known || name.isEmpty(), onClick = { choose("") }, label = { Text("📱 Standard") })
        if (pack != null) FilterChip(selected = name == VOICE_THORSTEN, onClick = { choose(VOICE_THORSTEN) }, label = { Text("🎙️ Thorsten") })
        list.forEach { v -> FilterChip(selected = name == v.id, onClick = { choose(v.id) }, label = { Text(v.label) }) }
    }
    if (pack != null) ThorstenCard(pack, chosen = name == VOICE_THORSTEN)

    Spacer(Modifier.height(6.dp))
    Text("Tempo: $rate %", style = MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🐢", fontSize = 22.sp)
        Slider(
            value = rate.toFloat(), onValueChange = { settings.setVoiceRate((it / 10).toInt() * 10); voice.setRate(settings.voiceRate.value) },
            valueRange = 50f..150f, steps = 9, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text("🐇", fontSize = 22.sp)
    }
    JuicyButton(onClick = { voice.setVoice(name); voice.setRate(rate); voice.speak(VoiceRoute.PROBE) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text("🔊 Probe hören", style = MaterialTheme.typography.titleSmall)
    }
    Text(
        "Englisch, Französisch und Spanisch spricht immer eine Stimme des Geräts in der passenden Sprache. Es werden nur Stimmen angeboten, die ohne Internet sprechen.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Thorsten laden (mit Fortschritt, Abbrechen), Fehler zeigen, wieder entfernen. */
@Composable
private fun ThorstenCard(pack: NaturalVoicePack, chosen: Boolean) {
    val state by pack.state.collectAsState()
    val problem by pack.problem.collectAsState()
    Surface(
        color = if (chosen) Color(0xFFFFF3C4) else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎙️ Thorsten – eine natürliche Stimme", fontWeight = FontWeight.Bold, color = if (chosen) Color(0xFF4E3B00) else Color.Unspecified)
            AnimatedContent(state, transitionSpec = { (scaleIn(spring(dampingRatio = 0.6f), initialScale = 0.9f) + fadeIn()) togetherWith fadeOut() },
                contentKey = { it::class }, label = "thorsten") { s ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (s) {
                        VoicePackState.Missing -> {
                            Text("Klingt wie ein echter Sprecher. Wird einmal geladen (${pack.sizeMb} MB, am besten im WLAN) und rechnet danach ganz ohne Internet auf diesem Gerät.",
                                style = MaterialTheme.typography.bodySmall)
                            JuicyButton(onClick = { pack.download() }, modifier = Modifier.fillMaxWidth()) { Text("⬇️ Thorsten laden (${pack.sizeMb} MB)") }
                        }
                        is VoicePackState.Loading -> {
                            val p by animateFloatAsState(s.percent / 100f, label = "laden")
                            Text("Brummi holt Thorsten … ${s.percent} %", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(10.dp))
                            TextButton(onClick = { pack.cancel() }) { Text("Abbrechen") }
                        }
                        is VoicePackState.Failed -> {
                            Text("Das hat nicht geklappt: ${s.why}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            JuicyButton(onClick = { pack.download() }, modifier = Modifier.fillMaxWidth()) { Text("Nochmal versuchen") }
                        }
                        VoicePackState.Ready -> {
                            Text(if (chosen) "✅ Thorsten ist da und liest vor. Kennt er ein Wort nicht, springt kurz die Gerätestimme ein."
                                 else "✅ Thorsten ist geladen – oben auswählen, dann liest er vor.", style = MaterialTheme.typography.bodySmall)
                            problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            TextButton(onClick = { pack.remove() }) { Text("Entfernen (${pack.sizeMb} MB frei machen)") }
                        }
                    }
                }
            }
        }
    }
}
