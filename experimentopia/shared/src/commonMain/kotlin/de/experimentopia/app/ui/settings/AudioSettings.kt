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

/** Vorlesen: an/aus, automatisch, Stimme (Gerät/Browser oder „Thorsten“), Tempo, Probe */
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
        val options = listOf(VoiceOption("", "Automatisch (beste Stimme des Geräts)")) + reader.voices + VoiceOption(AudioPrefs.NATURAL, "Thorsten – natürliche Stimme")
        options.forEach { v ->
            val sel = audio.voice == v.id || (v.id == "" && options.none { it.id == audio.voice })
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (sel) E.Mint.copy(alpha = 0.16f) else E.Deep)
                .border(1.5.dp, if (sel) E.Mint else E.Line, RoundedCornerShape(12.dp)).clickable { onAudio(audio.copy(voice = v.id)) }
                .padding(horizontal = 12.dp, vertical = 9.dp).testTag("stimme_${v.id}"), verticalAlignment = Alignment.CenterVertically) {
                Text(if (sel) "●" else "○", color = if (sel) E.Mint else E.Muted)
                Spacer(Modifier.width(8.dp))
                Text(v.label, color = E.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
        }
        if (audio.voice == AudioPrefs.NATURAL) Panel(border = E.Sun) {
            when (val st = reader.natural) {
                NaturalState.Missing -> {
                    Text("Thorsten klingt wie ein echter Mensch und rechnet ganz auf dem Gerät. Dafür muss einmal eine Stimmdatei geladen werden (63 MB, am besten im WLAN). " +
                        "Bis dahin liest die Stimme des Geräts.", color = E.Text, fontSize = 13.sp)
                    GlowButton({ reader.loadNatural() }, Modifier.testTag("thorsten_laden"), color = E.Sun) { Text("⬇️ Thorsten laden (63 MB)") }
                }
                is NaturalState.Loading -> {
                    Text("Thorsten wird geladen … ${st.percent} %", color = E.Text)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(E.Panel)) {
                        Box(Modifier.fillMaxWidth(st.percent / 100f).fillMaxHeight().background(E.Sun))
                    }
                    GlowButton({ reader.cancelNatural() }, color = E.Sky) { Text("Abbrechen") }
                }
                is NaturalState.Failed -> {
                    Text("Das hat nicht geklappt: ${st.why}", color = E.Pink, fontSize = 13.sp)
                    GlowButton({ reader.loadNatural() }, color = E.Sun) { Text("🔁 Nochmal versuchen") }
                }
                NaturalState.Ready -> {
                    Text("✅ Thorsten ist da und spricht ohne Internet. Kennt er ein Wort nicht, liest diesen Satz die Stimme des Geräts.", color = E.Text, fontSize = 13.sp)
                    GlowButton({ reader.removeNatural() }, color = E.Muted) { Text("🗑️ Stimmdatei entfernen") }
                }
            }
        }
        ValueSlider("Tempo", audio.rate, 0.6f..1.6f, "${(audio.rate * 100).toInt()} %", { onAudio(audio.copy(rate = (it * 20).toInt() / 20f)) }, color = E.Mint)
        GlowButton({ reader.configure(audio); reader.say(ReadTexts.ui[1]) }, Modifier.fillMaxWidth().testTag("probe"), color = E.Mint) { Text("▶️ Probe hören", fontWeight = FontWeight.Bold) }
    }
}
