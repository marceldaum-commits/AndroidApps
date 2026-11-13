package de.wissensgarten.app.ui

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
import de.wissensgarten.app.data.Bed
import de.wissensgarten.app.data.Card
import de.wissensgarten.app.data.Scheduler
import de.wissensgarten.app.data.Stage
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedScreen(
    bed: Bed,
    now: Long,
    onBack: () -> Unit,
    onWater: () -> Unit,
    onAddCard: (String, String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onDeleteBed: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Card?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val thirsty = bed.cards.count { Scheduler.isThirsty(it, now) }

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
                text = { Text("Pflanzen") },
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
                FilledTonalButton(
                    onClick = onWater,
                    enabled = thirsty > 0 || bed.cards.any { it.isSeed },
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                ) {
                    Icon(Icons.Filled.WaterDrop, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (thirsty > 0) "Dieses Beet gießen ($thirsty)" else "Neue Samen in diesem Beet säen")
                }
            }
            if (bed.cards.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Das Beet ist leer. Tippe auf „Pflanzen“ und lege deine erste Karte an.", Modifier.padding(16.dp))
                }
            }
            items(bed.cards, key = { it.id }) { c -> PlantTile(c, bed.hue, now) { selected = c } }
        }
    }

    if (showAdd) AddCardDialog(onDismiss = { showAdd = false }, onAdd = onAddCard)
    selected?.let { c ->
        CardDetailDialog(c, now, onDismiss = { selected = null }, onDelete = { onDeleteCard(c.id); selected = null })
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Beet löschen?") },
        text = { Text("„${bed.name}“ und alle ${bed.cards.size} Pflanzen werden entfernt.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteBed() }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Behalten") } },
    )
}

@Composable
private fun PlantTile(card: Card, hue: Float, now: Long, onClick: () -> Unit) {
    val thirsty = Scheduler.isThirsty(card, now)
    Column(
        Modifier.clip(MaterialTheme.shapes.medium)
            .background(if (thirsty) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Plant(Stage.of(card), Scheduler.wilt(card, now), hue, card.id.hashCode() and 0xff, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(card.front, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(
            if (thirsty) "durstig" else Stage.of(card).label,
            style = MaterialTheme.typography.labelSmall,
            color = if (thirsty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var added by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Samen pflanzen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(front, { front = it }, label = { Text("Frage / Vorderseite") })
                OutlinedTextField(back, { back = it }, label = { Text("Antwort / Rückseite") })
                if (added > 0) Text("$added gepflanzt 🌱", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(front, back); front = ""; back = ""; added++ },
                enabled = front.isNotBlank() && back.isNotBlank(),
            ) { Text("Pflanzen & weiter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
private fun CardDetailDialog(card: Card, now: Long, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.front) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.back, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Text("Stufe: ${Stage.of(card).label}")
                if (!card.isSeed) {
                    Text("Erinnerung jetzt: ${(Scheduler.retrievability(card, now) * 100).roundToInt()} %")
                    Text("Hält etwa ${formatDays(card.stability)} · ${card.reps}× gegossen")
                } else Text("Noch nicht gesät")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Ausreißen") } },
    )
}
