package de.wissensgarten.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import de.wissensgarten.app.ui.fx.Stimme
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.core.data.Settings
import de.wissensgarten.core.model.Sprechtempo
import de.wissensgarten.core.model.NovaOffline
import de.wissensgarten.core.model.NovaQualitaet
import de.wissensgarten.core.model.Pack
import de.wissensgarten.app.ui.fx.NovaOfflineSteuerung

/** KI-Stimme (Kennzeichnung), Sprechtempo in Stufen, Probe-Taste und „Offline laden“ – unter „Brummi liest vor“. */
@Composable
fun VoiceSettings(settings: Settings, host: AppHost, packs: List<Pack> = emptyList(), plusActive: Boolean = false) {
    val voice = host.voice ?: return
    val offline = voice.offline
    LaunchedEffect(packs, plusActive) { offline?.pakete(packs, if (plusActive) packs.filter { it.isPlus } else emptyList()) }
    VoiceSettingsPanel(settings, voice, offline)
}

/** Inhalt der Stimm-Einstellungen (eigene Funktion, damit sie sich ohne Plattform zeigen lässt – Bildschirmfoto-Test) */
@Composable
fun VoiceSettingsPanel(settings: Settings, voice: Voice, offline: NovaOfflineSteuerung? = voice.offline) {
    val rate by settings.voiceRate.collectAsState()
    val stufen = Sprechtempo.STUFEN
    val index = stufen.indexOf(Sprechtempo.stufe(rate)).coerceAtLeast(0)

    Spacer(Modifier.height(10.dp))
    Surface(color = Color(0xFFFFF3C4), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🎙️ Stimme: ${Stimme.NAME}", fontWeight = FontWeight.Bold, color = Color(0xFF4E3B00))
            Text(
                "Brummi liest mit einer KI-generierten Stimme vor (🤖 synthetische Stimme von unserem eigenen Server). Ohne Internet liest die Stimme des Geräts.",
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
        "Englisch, Französisch und Spanisch spricht die KI-Stimme in der passenden Sprache.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (offline != null) OfflineCard(settings, offline)
}

/** „Alle Vorlese-Texte offline laden“: Größe, Hinweis aufs WLAN, Fortschritt „x von n“, Abbrechen, Löschen */
@Composable
private fun OfflineCard(settings: Settings, offline: NovaOfflineSteuerung) {
    val z by offline.zustand.collectAsState()
    val chosen by settings.novaQuality.collectAsState()
    var confirm by remember { mutableStateOf<NovaQualitaet?>(null) }
    LaunchedEffect(Unit) { offline.pruefen() }

    Spacer(Modifier.height(12.dp))
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📦 Alle Vorlese-Texte offline laden", fontWeight = FontWeight.Bold)
            Text("Dann liest Brummi auch ohne Internet mit der KI-Stimme vor.", style = MaterialTheme.typography.bodySmall)
            val laedt = z.laedt
            when {
                !z.geprueft -> Text("Brummi schaut nach, was schon da ist …", style = MaterialTheme.typography.bodySmall)
                laedt != null -> {
                    val p by animateFloatAsState(if (z.ziel > 0) z.fertig.toFloat() / z.ziel else 0f, label = "offline")
                    Text("Brummi lädt Vorlese-Sätze (${laedt.label}) … ${z.fertig} von ${z.ziel}", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(10.dp))
                    TextButton(onClick = { offline.abbrechen() }) { Text("Abbrechen") }
                }
                confirm != null -> {
                    val q = confirm!!
                    Surface(color = Color(0xFFE3F2FD), shape = MaterialTheme.shapes.medium) {
                        Text("📶 Am besten im WLAN laden – das sind ca. ${NovaOffline.mb(z.rest[q] ?: 0)} MB (${q.label}).",
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JuicyButton(onClick = { settings.setNovaQuality(q); confirm = null; offline.laden(q) }, modifier = Modifier.weight(1f)) { Text("Jetzt laden") }
                        OutlinedButton(onClick = { confirm = null }, modifier = Modifier.weight(1f)) { Text("Später") }
                    }
                }
                else -> {
                    val std = z.fehlend[NovaQualitaet.STANDARD] ?: 0
                    if (std == 0 && z.gesamt > 0) Text("✅ Alle ${z.gesamt} Vorlese-Sätze sind offline da" +
                        (if ((z.fehlend[NovaQualitaet.BEST] ?: 0) == 0) " – in Bestqualität." else "."), style = MaterialTheme.typography.bodyMedium)
                    if (z.fehlend.values.any { it > 0 }) Text("📶 Am besten im WLAN laden.", style = MaterialTheme.typography.bodySmall)
                    NovaQualitaet.entries.forEach { q ->
                        val n = z.fehlend[q] ?: 0
                        if (n > 0) {
                            val label = "⬇️ Offline laden – ${q.label} (ca. ${NovaOffline.mb(z.rest[q] ?: 0)} MB)"
                            if (q == chosen) JuicyButton(onClick = { confirm = q }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                            else OutlinedButton(onClick = { confirm = q }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                        }
                    }
                    if ((z.fehlend[NovaQualitaet.BEST] ?: 0) > 0)
                        Text("Bestqualität ist unkomprimiert: klingt noch etwas klarer, braucht aber mehr Platz.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            z.hinweis?.let { if (laedt == null) Text(it, style = MaterialTheme.typography.bodySmall) }
            if (z.belegt > 0 && laedt == null && confirm == null)
                OutlinedButton(onClick = { offline.loeschen() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑️ Offline-Stimme löschen (${NovaOffline.mb(z.belegt)} MB)")
                }
        }
    }
}
