package de.experimentopia.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.ReadTexts

@Composable
private fun Switcher(title: String, sub: String, on: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = E.Text)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = E.Muted)
        }
        Switch(on, onChange, Modifier.testTag(tag))
    }
}

/** Musik, Effekte, Vibration – alles einzeln schaltbar, mit eigener Lautstärke */
@Composable
fun SoundSettings(audio: AudioPrefs, onAudio: (AudioPrefs) -> Unit, sound: Boolean, onSound: (Boolean) -> Unit, vibration: Boolean, onVibration: (Boolean) -> Unit) {
    val sfx = LocalSfx.current
    Panel(Modifier.fillMaxWidth()) {
        Text("🎵 Musik & Klang", style = MaterialTheme.typography.titleMedium, color = E.Mint)
        Switcher("🎻 Musik", "Eigene fröhliche Labor-Lieder – jede Welt hat ihre eigenen Melodien", audio.music, "musik") { onAudio(audio.copy(music = it)) }
        if (audio.music) ValueSlider("Lautstärke Musik", audio.musicVol, 0f..1f, "${(audio.musicVol * 100).toInt()} %", { onAudio(audio.copy(musicVol = it)) }, color = E.Violet)
        Switcher("🧪 Effekte", "Reagenzglas, Blubbern, Zischen, Funken, Messgerät", sound, "effekte", onSound)
        if (sound) ValueSlider("Lautstärke Effekte", audio.sfxVol, 0.1f..1f, "${(audio.sfxVol * 100).toInt()} %", { onAudio(audio.copy(sfxVol = it)); sfx?.play(Sound.GLASS, volume = 1f) }, color = E.Sky)
        Switcher("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, "vibration", onVibration)
        Text(if (LocalMusic.current?.needsTouch == false) "Die Musik startet gleich beim Öffnen und richtet sich nach der Medien-Lautstärke des Geräts. Im Versuch selbst ist es still."
            else "Der Browser erlaubt Ton erst, wenn du die App das erste Mal antippst – dann startet die Musik. Im Versuch selbst ist es still.", color = E.Muted, fontSize = 12.sp)
    }
}
