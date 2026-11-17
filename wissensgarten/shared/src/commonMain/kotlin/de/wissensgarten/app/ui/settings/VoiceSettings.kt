package de.wissensgarten.app.ui.settings

import androidx.compose.animation.AnimatedContent
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
import de.wissensgarten.app.ui.fx.Stimme
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.core.data.Settings
import de.wissensgarten.core.model.Sprechtempo

/** Stimme „Nova“ (Kennzeichnung), Sprechtempo in Stufen und Probe-Taste – unter „Brummi liest vor“. */
@Composable
fun VoiceSettings(settings: Settings, host: AppHost) {
    VoiceSettingsPanel(settings, host.voice ?: return)
}

/** Inhalt der Stimm-Einstellungen (eigene Funktion, damit sie sich ohne Plattform zeigen lässt – Bildschirmfoto-Test) */
@Composable
fun VoiceSettingsPanel(settings: Settings, voice: Voice) {
    val rate by settings.voiceRate.collectAsState()
    val stufen = Sprechtempo.STUFEN
    val index = stufen.indexOf(Sprechtempo.stufe(rate)).coerceAtLeast(0)

    Spacer(Modifier.height(10.dp))
    Surface(color = Color(0xFFFFF3C4), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🎙️ Stimme: ${Stimme.NAME}", fontWeight = FontWeight.Bold, color = Color(0xFF4E3B00))
            Text(
                "Brummi liest mit Nova vor, einer mit KI (OpenAI) erzeugten Stimme. Ohne Internet liest die Stimme des Geräts.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E3B00),
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sprechtempo: ", style = MaterialTheme.typography.bodyMedium)
        AnimatedContent(index, transitionSpec = { scaleIn(spring(dampingRatio = 0.45f), initialScale = 0.6f) + fadeIn() togetherWith fadeOut() }, label = "tempo") { i ->
            Text(Sprechtempo.label(stufen[i]) + if (stufen[i] == Sprechtempo.NORMAL) " (normal)" else "",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🐢", fontSize = 22.sp)
        Slider(
            value = index.toFloat(),
            onValueChange = { v -> val p = stufen[kotlin.math.round(v).toInt().coerceIn(0, stufen.lastIndex)]; if (p != rate) { settings.setVoiceRate(p); voice.setRate(p) } },
            valueRange = 0f..stufen.lastIndex.toFloat(), steps = stufen.size - 2, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text("🐇", fontSize = 22.sp)
    }
    JuicyButton(onClick = { voice.setRate(rate); voice.speak(Stimme.PROBE) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Text("🔊 Probe hören", style = MaterialTheme.typography.titleSmall)
    }
    Text(
        "Englisch, Französisch und Spanisch spricht Nova in der passenden Sprache.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
