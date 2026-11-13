package de.wissensgarten.app.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.common.formatDays
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.CardEntity
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedScreen(
    data: BedWithCards,
    now: Long,
    seedsLeft: Int,
    onBack: () -> Unit,
    onWater: () -> Unit,
    onAddCard: (String, String, String?) -> Unit,
    onDeleteCard: (String) -> Unit,
    onDeleteBed: () -> Unit,
) {
    val bed = data.bed
    val cards = data.cards.sortedBy { it.sortIndex }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CardEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
    val seeds = minOf(seedsLeft, cards.count { it.memory.isSeed })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${bed.emoji} ${bed.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
                actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Beet löschen") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Karte") },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = pad.calculateTopPadding(), bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    FilledTonalButton(
                        onClick = onWater,
                        enabled = thirsty + seeds > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.WaterDrop, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                thirsty > 0 -> "Dieses Beet gießen ($thirsty)"
                                seeds > 0 -> "$seeds Samen säen"
                                else -> "Heute alles erledigt"
                            }
                        )
                    }
                    if (bed.packId != null) Text(
                        "Wissenspaket · Version ${bed.packVersion} · ${cards.size} Karten. Du kannst eigene Karten ergänzen.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (cards.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Das Beet ist leer. Tippe auf „Karte“ und pflanze deine erste Frage.", Modifier.padding(16.dp))
            }
            items(cards, key = { it.id }) { c -> PlantTile(c, bed.hue, now) { selected = c } }
        }
    }

    if (showAdd) AddCardDialog(onDismiss = { showAdd = false }, onAdd = onAddCard)
    selected?.let { c -> CardDetailDialog(c, now, { selected = null }) { onDeleteCard(c.id); selected = null } }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Beet löschen?") },
        text = { Text("„${bed.name}“ und alle ${cards.size} Pflanzen samt Lernstand werden entfernt.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteBed() }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Behalten") } },
    )
}

@Composable
private fun PlantTile(card: CardEntity, hue: Float, now: Long, onClick: () -> Unit) {
    val thirsty = Scheduler.isThirsty(card.memory, now)
    Column(
        Modifier.clip(MaterialTheme.shapes.medium)
            .background(if (thirsty) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Plant(Stage.of(card.memory), Scheduler.wilt(card.memory, now), hue, card.id.hashCode() and 0xff, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(card.front, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(
            if (thirsty) "durstig" else Stage.of(card.memory).label,
            style = MaterialTheme.typography.labelSmall,
            color = if (thirsty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var added by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Samen pflanzen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(front, { front = it }, label = { Text("Frage / Vorderseite") })
                OutlinedTextField(back, { back = it }, label = { Text("Antwort / Rückseite") })
                OutlinedTextField(info, { info = it }, label = { Text("Merkhilfe (optional)") })
                if (added > 0) Text("$added gepflanzt 🌱", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(front, back, info); front = ""; back = ""; info = ""; added++ },
                enabled = front.isNotBlank() && back.isNotBlank(),
            ) { Text("Pflanzen & weiter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
private fun CardDetailDialog(card: CardEntity, now: Long, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.front) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.back, style = MaterialTheme.typography.titleMedium)
                card.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                HorizontalDivider()
                Text("Stufe: ${Stage.of(card.memory).label}")
                if (!card.memory.isSeed) {
                    Text("Erinnerung jetzt: ${(Scheduler.retrievability(card.memory, now) * 100).roundToInt()} %")
                    Text("Hält etwa ${formatDays(card.stability)} · ${card.reps}× gegossen")
                } else Text("Noch nicht gesät")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Ausreißen") } },
    )
}
