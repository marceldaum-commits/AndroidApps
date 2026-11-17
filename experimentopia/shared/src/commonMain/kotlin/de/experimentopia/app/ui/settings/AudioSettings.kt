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
import de.experimentopia.core.model.Nova
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
    Panel {
        Text("🎵 Musik & Klang", style = MaterialTheme.typography.titleMedium, color = E.Mint)
        Switcher("🎻 Musik", "Eigene Labor-Musik: Pizzicato, Klarinette, Fagott – jede Welt klingt anders", audio.music, "musik") { onAudio(audio.copy(music = it)) }
        if (audio.music) ValueSlider("Lautstärke Musik", audio.musicVol, 0f..1f, "${(audio.musicVol * 100).toInt()} %", { onAudio(audio.copy(musicVol = it)) }, color = E.Violet)
        Switcher("🧪 Effekte", "Reagenzglas, Blubbern, Zischen, Funken, Messgerät", sound, "effekte", onSound)
        if (sound) ValueSlider("Lautstärke Effekte", audio.sfxVol, 0.1f..1f, "${(audio.sfxVol * 100).toInt()} %", { onAudio(audio.copy(sfxVol = it)); sfx?.play(Sound.GLASS, volume = 1f) }, color = E.Sky)
        Switcher("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, "vibration", onVibration)
        Text("Musik und Ton starten erst, wenn du die App das erste Mal antippst. Ist das Gerät stumm geschaltet, bleibt auch die App still.", color = E.Muted, fontSize = 12.sp)
    }
}

/** Vorlesen: an/aus, automatisch, Probe – gesprochen wird mit der KI-Stimme „Nova“ */
@Composable
fun ReadSettings(audio: AudioPrefs, onAudio: (AudioPrefs) -> Unit) {
    val reader = LocalReader.current
    Panel {
        Text("🔊 Vorlesen", style = MaterialTheme.typography.titleMedium, color = E.Mint)
        Switcher("Vorlesen", "Anleitungen, Fragen und Erklärungen – überall gibt es dann einen 🔊-Knopf", audio.read, "vorlesen_an") { onAudio(audio.copy(read = it)); if (!it) reader?.stop() }
        if (!audio.read) return@Panel
        Switcher("Automatisch vorlesen", "Blubb liest jeden neuen Schritt von selbst vor", audio.autoRead, "vorlesen_auto") { onAudio(audio.copy(autoRead = it)) }
        if (reader == null) { Text("Auf diesem Gerät gibt es keine Sprachausgabe.", color = E.Muted, fontSize = 13.sp); return@Panel }
        Text("Stimme", color = E.Text, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(E.Mint.copy(alpha = 0.16f)).border(1.5.dp, E.Mint, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp).testTag("stimme_nova"), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = E.Mint)
            Spacer(Modifier.width(8.dp))
            Text("Nova – KI-generierte Stimme", color = E.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Text("Nova ist eine mit KI (OpenAI) erzeugte Stimme. Wir vertonen die Texte vorab auf unserem Stimmen-Server; jeder Satz wird einmal geladen und bleibt " +
            "dann auf dem Gerät. Ohne Internet liest die Stimme des Geräts.", color = E.Muted, fontSize = 12.sp)
        Text("Sprechtempo", color = E.Text, fontWeight = FontWeight.Bold)
        val speed = Nova.speed(audio.rate)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Nova.SPEEDS.forEach { v ->
                val sel = v == speed
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) E.Mint.copy(alpha = 0.2f) else E.Deep)
                    .border(1.5.dp, if (sel) E.Mint else E.Line, RoundedCornerShape(10.dp)).clickable { onAudio(audio.copy(rate = v)) }
                    .padding(vertical = 8.dp).testTag("tempo_$v"), contentAlignment = Alignment.Center) {
                    Text(v.toString().replace('.', ','),
                        color = if (sel) E.Mint else E.Text, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Text(if (speed < 1f) "🐢 langsamer" else if (speed > 1f) "🐇 schneller" else "Normal", color = E.Muted, fontSize = 12.sp)
        GlowButton({ reader.configure(audio); reader.say(ReadTexts.ui[1]) }, Modifier.fillMaxWidth().testTag("probe"), color = E.Mint) { Text("▶️ Probe hören", fontWeight = FontWeight.Bold) }
    }
}
