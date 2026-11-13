package de.wissensgarten.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.data.Bed
import de.wissensgarten.app.data.GardenState
import de.wissensgarten.app.data.Scheduler
import de.wissensgarten.app.data.Session
import de.wissensgarten.app.data.Stage
import de.wissensgarten.app.ui.theme.BigNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    state: GardenState,
    now: Long,
    onWater: () -> Unit,
    onOpenBed: (String) -> Unit,
    onAddBed: (String, String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val session = remember(state, now) { Session.build(state, now) }
    val thirsty = remember(state, now) { Session.thirstyCount(state, now) }
    val seeds = session.size - thirsty

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wissensgarten") },
                actions = { DewChip(state.dewdrops); Spacer(Modifier.width(12.dp)) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Beet anlegen") },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { WaterHero(thirsty, seeds, nextThirst(state, now), onWater) }
            if (state.beds.isEmpty()) {
                item {
                    Text(
                        "Noch keine Beete. Lege eins an und pflanze deine ersten Karten.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(state.beds, key = { it.id }) { bed -> BedCard(bed, now) { onOpenBed(bed.id) } }
        }
    }

    if (showAdd) AddBedDialog(onDismiss = { showAdd = false }) { name, emoji ->
        onAddBed(name, emoji); showAdd = false
    }
}

private fun nextThirst(state: GardenState, now: Long): Long? =
    state.beds.flatMap { it.cards }.filter { !it.isSeed }.map { Scheduler.dueAt(it) }.filter { it > now }.minOrNull()

@Composable
fun DewChip(dew: Int) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WaterDrop, contentDescription = "Tautropfen", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("$dew", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun WaterHero(thirsty: Int, seeds: Int, nextAt: Long?, onWater: () -> Unit) {
    val now = System.currentTimeMillis()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            if (thirsty > 0) {
                Text("$thirsty", style = BigNumber, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${plantsWord(thirsty)} ${if (thirsty == 1) "hat" else "haben"} Durst", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Gieß sie, bevor du vergisst, was sie wissen." + if (seeds > 0) " Dazu $seeds neue Samen." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text("Alles grün 🌿", style = MaterialTheme.typography.headlineSmall)
                val hint = buildString {
                    if (nextAt != null) append("Nächster Durst in ${formatDays((nextAt - now).toDouble() / Scheduler.DAY_MS)}. ")
                    if (seeds > 0) append("Du kannst heute noch $seeds Samen säen.") else append("Für heute ist alles gesät.")
                }
                Text(hint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            val enabled = thirsty + seeds > 0
            Button(
                onClick = onWater,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(if (thirsty > 0) Icons.Filled.WaterDrop else Icons.Outlined.Spa, null)
                Spacer(Modifier.width(8.dp))
                Text(if (thirsty > 0) "Gießen" else "Samen säen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BedCard(bed: Bed, now: Long, onClick: () -> Unit) {
    val thirsty = bed.cards.count { Scheduler.isThirsty(it, now) }
    val grown = bed.cards.count { !it.isSeed }
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bed.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(bed.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$grown von ${bed.cards.size} gewachsen" + if (thirsty > 0) " · $thirsty durstig" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (thirsty > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (thirsty > 0) FontWeight.SemiBold else null,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Mini-Beet: bis zu 16 Pflanzen
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 6.dp),
            ) {
                bed.cards.sortedByDescending { it.stability }.take(8).forEach { c ->
                    Plant(
                        Stage.of(c), Scheduler.wilt(c, now), bed.hue, c.id.hashCode() and 0xff,
                        Modifier.weight(1f).aspectRatio(0.8f),
                    )
                }
                repeat((8 - bed.cards.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBedDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val emojis = listOf("🌻", "📐", "🧪", "🌍", "📜", "🗣️", "🎵", "🧠")
    var emoji by remember { mutableStateOf(emojis.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Beet") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Thema, z. B. Biologie: Zelle") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        FilterChip(selected = e == emoji, onClick = { emoji = e }, label = { Text(e) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name, emoji) }, enabled = name.isNotBlank()) { Text("Anlegen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
