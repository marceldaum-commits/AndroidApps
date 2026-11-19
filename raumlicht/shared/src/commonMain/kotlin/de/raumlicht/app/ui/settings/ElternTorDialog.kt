package de.raumlicht.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalSpeaker
import de.raumlicht.app.ui.fx.Sound
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.ElternTor
import kotlin.random.Random

/** Das Eltern-Tor: [LocalElternTor] führt die übergebene Handlung erst aus, wenn die Aufgabe gelöst ist (oder das Tor noch offen steht). Ohne Tor (Vorgabe) sofort. */
val LocalElternTor = staticCompositionLocalOf<(action: () -> Unit) -> Unit> { { it() } }

/**
 * Die Aufgabe des Eltern-Tors als Fenster über dem Bildschirm: Malaufgabe in Worten, eigene Ziffern-Tastatur, kein Zeitdruck.
 * Nach einem Fehler kommt sofort eine neue Aufgabe. „Abbrechen“ geht immer.
 */
@Composable
fun ElternTorDialog(onSolved: () -> Unit, onCancel: () -> Unit, random: Random = remember { Random(de.raumlicht.app.ui.fx.nowNanos()) }) {
    var task by remember { mutableStateOf(ElternTor.fresh(random)) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current; val speaker = LocalSpeaker.current
    fun submit() { if (input.isEmpty()) return
        if (task.solves(input)) { sfx?.play(Sound.HIT); onSolved() } else { sfx?.play(Sound.TAP); wrong = true; task = ElternTor.fresh(random, task); input = "" } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).pointerInput(Unit) { detectTapGestures { } }.systemBarsPadding().testTag("eltern-tor"), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 380.dp).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(22.dp)).background(Q.Deep).border(1.5.dp, Q.Yellow.copy(alpha = 0.7f), RoundedCornerShape(22.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ElternTor.TITLE, color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(ElternTor.NOTE, Modifier.testTag("eltern-tor-hinweis"), color = Q.Text, fontSize = 14.sp, lineHeight = 19.sp)
            if (wrong) Text(ElternTor.WRONG, Modifier.testTag("eltern-tor-falsch"), color = Q.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wie viel ist ${task.words}?", Modifier.testTag("eltern-tor-aufgabe").semantics { contentDescription = "Rechenaufgabe: Wie viel ist ${task.words}?" }, color = Q.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                // Vorlesen, wo es eine Stimme gibt
                if (speaker != null) Text("🔊", Modifier.testTag("eltern-tor-vorlesen").clip(RoundedCornerShape(10.dp)).clickable { speaker.say("Wie viel ist ${task.words}?") }.padding(8.dp).semantics { contentDescription = "Aufgabe vorlesen" }, fontSize = 20.sp)
            }
            Text(input.ifEmpty { "–" }, Modifier.fillMaxWidth().testTag("eltern-tor-eingabe").semantics { contentDescription = if (input.isEmpty()) "Noch keine Eingabe" else "Eingabe $input" }
                .background(Q.Space, RoundedCornerShape(14.dp)).border(1.dp, Q.Muted.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp),
                color = Q.Text, fontWeight = FontWeight.Black, fontSize = 26.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            // eigene Ziffern-Tastatur: 1–9, darunter löschen, 0, fertig
            val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK"))
            keys.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { k ->
                val c = if (k == "OK") Q.Green else if (k == "⌫") Q.Orange else Q.Cyan
                Box(Modifier.weight(1f).heightIn(min = 48.dp).testTag("eltern-tor-taste-$k").clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.16f)).border(1.dp, c.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .semantics { contentDescription = when (k) { "⌫" -> "Letzte Ziffer löschen"; "OK" -> "Antwort prüfen"; else -> "Ziffer $k" } }
                    .clickable { when (k) { "⌫" -> input = input.dropLast(1); "OK" -> submit(); else -> if (input.length < 3) { input += k; wrong = false } } }, contentAlignment = Alignment.Center) {
                    Text(k, color = Q.Text, fontWeight = FontWeight.Black, fontSize = 20.sp) } } } }
            Text("Abbrechen", Modifier.testTag("eltern-tor-abbrechen").clip(RoundedCornerShape(10.dp)).clickable(onClick = onCancel).padding(horizontal = 14.dp, vertical = 12.dp), color = Q.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
