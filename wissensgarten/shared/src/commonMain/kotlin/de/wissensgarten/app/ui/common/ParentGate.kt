package de.wissensgarten.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Eltern-Tor (Google-Play-Familienrichtlinie): Bevor die App Einstellungen, Konten, Teilen nach außen
 * oder Kamera-Scans öffnet, muss ein Erwachsener eine kleine Aufgabe lösen.
 * Einmal gelöst, bleibt das Tor 10 Minuten offen – Eltern sollen nicht genervt werden.
 */
object ParentGate {
    private const val OPEN_MS = 10 * 60_000L
    private var openUntil = 0L
    fun isOpen(now: Long = de.wissensgarten.core.model.Platform.nowMs()) = now < openUntil
    fun open(now: Long = de.wissensgarten.core.model.Platform.nowMs()) { openUntil = now + OPEN_MS }
    /** Für Tests und „Sperren“-Knopf */
    fun close() { openUntil = 0 }
    /** Wird aufgerufen, wenn Eltern das Tor gerade geöffnet haben (für die anonyme Zählung „elternbereich“) */
    var onPassed: (() -> Unit)? = null

    /** Aufgabe: zweistellig × einstellig – für Erwachsene leicht, für Grundschulkinder schwer. */
    data class Task(val a: Int, val b: Int) { val answer get() = a * b }
    fun task(r: Random = Random) = Task(r.nextInt(13, 20), r.nextInt(6, 10))
    /** Vier Antworten, eine richtig – keine Tastatur nötig, aber Raten hilft kaum (3 Fehlversuche → neue Aufgabe). */
    fun options(t: Task, r: Random = Random) = (listOf(t.answer) + generateSequence { t.answer + r.nextInt(-12, 13) }
        .filter { it != t.answer && it > 0 }.distinct().take(3)).shuffled(r)
}

/** Ruft [onPass] auf – sofort, wenn das Tor offen ist, sonst nach gelöster Aufgabe. */
val LocalParentGate = staticCompositionLocalOf<(reason: String, onPass: () -> Unit) -> Unit> { { _, onPass -> onPass() } }

/** Stellt das Eltern-Tor für alles darunter bereit. */
@Composable
fun ParentGateHost(content: @Composable () -> Unit) {
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val ask: (String, () -> Unit) -> Unit = remember {
        { reason, onPass -> if (ParentGate.isOpen()) onPass() else pending = reason to onPass }
    }
    CompositionLocalProvider(LocalParentGate provides ask) {
        content()
        pending?.let { (reason, onPass) ->
            ParentGateDialog(reason, onPass = { ParentGate.open(); ParentGate.onPassed?.invoke(); pending = null; onPass() }, onCancel = { pending = null })
        }
    }
}

@Composable
fun ParentGateDialog(reason: String, onPass: () -> Unit, onCancel: () -> Unit, random: Random = remember { Random }) {
    var task by remember { mutableStateOf(ParentGate.task(random)) }
    var options by remember(task) { mutableStateOf(ParentGate.options(task, random)) }
    var wrong by remember(task) { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("👨‍👩‍👧 Für Eltern") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(reason, style = MaterialTheme.typography.bodyMedium)
                Text("Bitte löse die Aufgabe:", style = MaterialTheme.typography.bodySmall)
                Text("${task.a} × ${task.b} = ?", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).semantics { contentDescription = "${task.a} mal ${task.b}" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    options.forEach { o ->
                        OutlinedButton(onClick = {
                            if (o == task.answer) onPass()
                            else if (++wrong >= 3) task = ParentGate.task(random)
                        }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("$o") }
                    }
                }
                if (wrong > 0) Text("Leider falsch – noch einmal.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
    )
}
