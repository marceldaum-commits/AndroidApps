package de.wissensgarten.app.ui.duel

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Duel
import de.wissensgarten.core.model.Duels
import de.wissensgarten.core.model.Pack

val DuelOrange = Color(0xFFFF7043)
private val DuelRed = Color(0xFFE53935)

/** Text per WhatsApp & Co. teilen – nur nach dem Eltern-Tor (Familienrichtlinie). */
@androidx.compose.runtime.Composable
fun rememberShareDuel(): (Context, String) -> Unit {
    val gate = de.wissensgarten.app.ui.common.LocalParentGate.current
    return { c, t -> gate("Die Duell-Einladung wird an eine andere App (z. B. WhatsApp) geschickt.") { shareDuel(c, t) } }
}

fun shareDuel(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Duell teilen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Duell-Start: neues Duell anlegen, Code einfügen, letzte Duelle ansehen.
 * [preview] zeigt ein gerade geöffnetes Duell (z. B. aus einem Code) mit „Annehmen“.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelHubScreen(
    myName: String,
    packs: List<Pack>,
    history: List<Duel>,
    preview: Duel?,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onCode: (String) -> Boolean,
    onPlay: (Duel) -> Unit,
    onShowDuel: (Duel) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val share = rememberShareDuel()
    val context = LocalContext.current
    val sfx = LocalSfx.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val title: (Duel) -> String = { d -> packs.firstOrNull { it.id == d.packId }?.title ?: "Unbekanntes Paket" }

    fun tryCode(t: String) {
        if (onCode(t)) { sfx?.play(Sound.PLOP); error = false; code = "" } else { sfx?.play(Sound.WRONG); error = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚔️ Duell") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFB74D), DuelOrange, DuelRed))).padding(20.dp),
                ) {
                    Column {
                        Text("Fordere Familie und Freunde heraus!", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "10 Fragen, 15 Sekunden pro Frage. Danach schickst du den Code per WhatsApp – " +
                                "wer ihn einfügt, spielt genau dieselben Fragen.",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(top = 6.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        JuicyButton(
                            onClick = onNew, modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DuelRed),
                        ) { Text("Neues Duell starten", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                    }
                }
            }

            preview?.let { d ->
                item { DuelCard(d, title(d), myName, highlight = true, onPlay = { onPlay(d) }, onShare = null) }
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Code bekommen?", style = MaterialTheme.typography.titleMedium)
                        Text("Kopiere die ganze Nachricht aus WhatsApp und füge sie hier ein.", style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(
                            onClick = { tryCode(clipboard.getText()?.text.orEmpty()) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Icon(Icons.Filled.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("Aus Zwischenablage einfügen") }
                        OutlinedTextField(
                            code, { code = it; error = false },
                            label = { Text("…oder Code hier eintippen") },
                            isError = error,
                            supportingText = { if (error) Text("Das ist kein gültiger Duell-Code. Bitte die ganze Nachricht kopieren.") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 3,
                        )
                        if (code.isNotBlank()) Button(onClick = { tryCode(code) }) { Text("Öffnen") }
                    }
                }
            }

            val others = history.filter { preview == null || !(it.seed == preview.seed && it.packId == preview.packId) }
            if (others.isNotEmpty()) item { Text("Deine letzten Duelle", style = MaterialTheme.typography.titleMedium) }
            items(others, key = { "${it.packId}-${it.seed}" }) { d ->
                DuelCard(
                    d, title(d), myName, highlight = false,
                    onPlay = { onShowDuel(d) },
                    onShare = {
                        val me = d.results.firstOrNull { it.name.equals(myName, true) } ?: d.results.first()
                        share(context, Duels.shareText(d, title(d), me))
                    },
                )
            }
        }
    }
}

@Composable
fun DuelCard(d: Duel, packTitle: String, myName: String, highlight: Boolean, onPlay: () -> Unit, onShare: (() -> Unit)?) {
    val played = d.hasPlayed(myName)
    Card(
        onClick = onPlay,
        colors = CardDefaults.cardColors(containerColor = if (highlight) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚔️", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(packTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (highlight) Color(0xFF3E2723) else MaterialTheme.colorScheme.onSurface)
                if (onShare != null) IconButton(onClick = onShare) { Icon(Icons.Filled.Share, "Teilen") }
            }
            if (d.results.isEmpty()) Text("Noch niemand hat gespielt.", style = MaterialTheme.typography.bodySmall)
            d.ranking.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(listOf("🥇", "🥈", "🥉").getOrElse(i) { "  ${i + 1}." }, modifier = Modifier.width(32.dp))
                    Text("${r.avatar} ${r.name}", modifier = Modifier.weight(1f), fontWeight = if (r.name.equals(myName, true)) FontWeight.Bold else null,
                        color = if (highlight) Color(0xFF3E2723) else MaterialTheme.colorScheme.onSurface)
                    Text("${r.score}", fontWeight = FontWeight.Bold, color = if (highlight) Color(0xFF3E2723) else MaterialTheme.colorScheme.onSurface)
                }
            }
            if (highlight) {
                Spacer(Modifier.height(10.dp))
                JuicyButton(
                    onClick = onPlay, modifier = Modifier.fillMaxWidth().height(54.dp).pulse(!played, 0.03f),
                    colors = ButtonDefaults.buttonColors(containerColor = DuelOrange),
                ) { Text(if (played) "Noch mal spielen" else "Duell annehmen ⚔️", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Kleiner Einstieg (für Garten und Familie). */
@Composable
fun DuelEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(DuelOrange), contentAlignment = Alignment.Center) { Text("⚔️", fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Duell", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3E2723))
                Text("Fordere Familie und Freunde heraus – per WhatsApp.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723))
            }
            Text("›", fontSize = 28.sp, color = Color(0xFF3E2723))
        }
    }
}
