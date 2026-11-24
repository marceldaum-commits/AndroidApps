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

    /** Aufgabe: zweistellig × einstellig – für Erwachsene leicht, für Grundschulkinder schwer (gemeinsamer Standard ELTERN-TOR.md). */
    data class Task(val a: Int, val b: Int) {
        val answer get() = a * b
        /** In Worten, damit Leseanfänger nicht einfach rechnen: „siebzehn mal acht“ */
        val words get() = "${wort(a)} mal ${wort(b)}"
    }
    fun task(r: Random = Random) = Task(r.nextInt(13, 20), r.nextInt(6, 10))
    private val WORTE = mapOf(6 to "sechs", 7 to "sieben", 8 to "acht", 9 to "neun", 13 to "dreizehn", 14 to "vierzehn", 15 to "fünfzehn",
        16 to "sechzehn", 17 to "siebzehn", 18 to "achtzehn", 19 to "neunzehn")
    fun wort(n: Int) = WORTE[n] ?: n.toString()
    /** Höchstens 3 Ziffern (größtes Ergebnis 19 × 9 = 171) */
    const val MAX_DIGITS = 3
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

/**
 * Eltern-Aufgabe in Worten (ohne Vorlese-Knopf, bis feste Tonstudio-Sätze da sind – keine Gerätestimme), Eingabe über eine eigene Ziffern-Tastatur (keine Auswahlknöpfe, kein Raten).
 * Nach EINEM Fehler kommt sofort eine neue Aufgabe. Kein Zeitdruck, Abbrechen jederzeit.
 */
@Composable
fun ParentGateDialog(reason: String, onPass: () -> Unit, onCancel: () -> Unit, random: Random = remember { Random }) {
    var task by remember { mutableStateOf(ParentGate.task(random)) }
    var input by remember(task) { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val voice = de.wissensgarten.app.ui.fx.LocalVoice.current
    fun check() {
        if (input.isEmpty()) return
        if (input.toIntOrNull() == task.answer) { voice?.stop(); onPass() } else { wrong = true; task = ParentGate.task(random) }
    }
    AlertDialog(
        onDismissRequest = { voice?.stop(); onCancel() },
        title = { Text("👨‍👩‍👧 Für Eltern") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(reason, style = MaterialTheme.typography.bodyMedium)
                Text("Bitte rechnen und das Ergebnis eintippen:", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(task.words, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                        .semantics { contentDescription = "Aufgabe: ${task.words}" })
                }
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                    Text(input.ifEmpty { " " }, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp)
                        .semantics { contentDescription = if (input.isEmpty()) "Noch keine Eingabe" else "Eingabe $input" })
                }
                if (wrong) Text("Das war nicht richtig – hier ist eine neue Aufgabe.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                // Eigene Ziffern-Tastatur: 1–9, ← (löschen), 0, OK – „⌫“ fehlt in der Web-Schrift
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("←", "0", "OK")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { k ->
                            OutlinedButton(onClick = {
                                when (k) {
                                    "←" -> input = input.dropLast(1)
                                    "OK" -> check()
                                    else -> if (input.length < ParentGate.MAX_DIGITS) input += k
                                }
                            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .semantics { contentDescription = when (k) { "←" -> "Löschen"; "OK" -> "Bestätigen"; else -> k } },
                                contentPadding = PaddingValues(4.dp)) { Text(k, fontSize = 18.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { voice?.stop(); onCancel() }) { Text("Abbrechen") } },
    )
}
