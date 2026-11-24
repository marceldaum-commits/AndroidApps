package de.experimentopia.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.fx.GlowButton
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.ParentGateMemory
import de.experimentopia.core.model.ParentGateTask
import kotlin.random.Random

/**
 * Eltern-Tor nach dem gemeinsamen Standard der Lern-Apps (ELTERN-TOR.md): Malaufgabe in Worten („siebzehn mal acht“),
 * eigene Ziffern-Tastatur, nach EINEM Fehler sofort eine neue Aufgabe, kein Zeitdruck, Abbrechen jederzeit, Bildschirmleser-Beschreibung.
 * Nach richtiger Antwort bleibt das Tor 10 Minuten offen – nur im Arbeitsspeicher ([ParentGateMemory]).
 * Kein Vorlese-Knopf und keine Gerätestimme (bis die Tonstudio-Stimme läuft).
 * Wo das Tor steht: jeder Kauf- und Shop-Weg, Elternbereich, Familie/Kopplung, Zurücksetzen, Zählung, Teilen nach außen.
 */
@Composable
fun ParentGate(onDismiss: () -> Unit, memory: ParentGateMemory = ParentGateMemory.shared, onPassed: () -> Unit) {
    if (memory.isOpen) {                                  // vor kurzem schon bestanden: gleich weiter
        LaunchedEffect(Unit) { onPassed() }
        return
    }
    var task by remember { mutableStateOf(ParentGateTask.next(Random.Default)) }
    var typed by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val submit = {
        if (ParentGateTask.check(task, typed)) { memory.pass(); onPassed() }
        else { wrong = true; typed = ""; task = ParentGateTask.next(Random.Default, task) }    // ein Fehler → sofort eine neue Aufgabe
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frage für Erwachsene") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rechne bitte:", color = E.Muted)
                Text(task.spoken, fontSize = 26.sp, fontWeight = FontWeight.Black, color = E.Sun, textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("tor_aufgabe").semantics { contentDescription = task.description })
                Text(if (typed.isEmpty()) "…" else typed, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = E.Text,
                    modifier = Modifier.testTag("tor_eingabe").semantics { contentDescription = if (typed.isEmpty()) "Noch nichts eingegeben" else "Eingegeben: $typed" })
                if (wrong) Text("Das war leider nicht richtig. Hier kommt eine neue Aufgabe.", color = E.Pink, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.testTag("tor_falsch"))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val label = when (k) { "⌫" -> "Letzte Ziffer löschen"; "✓" -> "Antwort prüfen"; else -> "Ziffer $k" }
                            GlowButton({ if (k == "✓") { if (typed.isNotEmpty()) submit() } else { typed = ParentGateTask.press(typed, k[0]); wrong = false } },
                                Modifier.weight(1f).heightIn(min = 52.dp).testTag("tor_taste_$k").semantics { contentDescription = label },
                                color = if (k == "✓") E.Mint else E.Sky, enabled = k != "✓" || typed.isNotEmpty()) { Text(k, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss, Modifier.testTag("tor_abbrechen")) { Text("Abbrechen") } },
    )
}
