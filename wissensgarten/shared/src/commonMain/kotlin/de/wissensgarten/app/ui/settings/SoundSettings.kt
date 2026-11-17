package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.AppHost
import de.wissensgarten.app.ui.fx.Sound
import de.wissensgarten.core.data.Settings
import kotlin.math.roundToInt

/** Lautstärke für Musik und Geräusche getrennt – unter „Ton“. Geräusche-Regler spielt beim Loslassen eine Probe. */
@Composable
fun SoundSettings(settings: Settings, host: AppHost) {
    SoundSettingsContent(
        musicVol = settings.musicVolume.collectAsState().value, sfxVol = settings.sfxVolume.collectAsState().value,
        music = settings.music.collectAsState().value, sound = settings.sound.collectAsState().value,
        onMusicVol = { settings.setMusicVolume(it) }, onSfxVol = { settings.setSfxVolume(it) },
        onProbe = { host.sfx.play(Sound.CORRECT) },
    )
}

@Composable
fun SoundSettingsContent(musicVol: Int, sfxVol: Int, music: Boolean, sound: Boolean,
                         onMusicVol: (Int) -> Unit, onSfxVol: (Int) -> Unit, onProbe: () -> Unit) {
    VolumeRow("🎵", "Lautstärke Musik", musicVol, enabled = music, onChange = onMusicVol)
    VolumeRow("🔔", "Lautstärke Geräusche", sfxVol, enabled = sound, onChange = onSfxVol, onDone = onProbe)
    Text(
        "Alle Klänge und die Musik sind selbst komponiert und entstehen im Gerät – Marimba, Glockenspiel, Kalimba, Vögel und Wind. " +
            "Jede Jahreszeit klingt anders. In der App startet die Musik beim Öffnen, im Browser beim ersten Antippen – und sie schweigt, wenn das Handy lautlos ist oder „Musik“ aus ist.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun VolumeRow(icon: String, label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit, onDone: () -> Unit = {}) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$icon $label", Modifier.weight(1f))
            Text("${local.roundToInt()} %", style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = local, onValueChange = { local = it; onChange(it.roundToInt()) }, onValueChangeFinished = onDone,
            valueRange = 0f..100f, steps = 19, enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label ${local.roundToInt()} Prozent" },
        )
    }
}
