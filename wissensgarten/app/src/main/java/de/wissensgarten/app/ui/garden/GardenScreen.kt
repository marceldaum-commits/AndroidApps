package de.wissensgarten.app.ui.garden

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
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.common.formatDays
import de.wissensgarten.app.ui.common.plantsWord
import de.wissensgarten.app.ui.theme.BigNumber
import de.wissensgarten.app.ui.fx.*
import androidx.compose.ui.graphics.Color
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage

@Composable
fun GardenScreen(
    profile: ProfileEntity,
    beds: List<BedWithCards>,
    now: Long,
    seedsLeft: Int,
    onWater: () -> Unit,
    onOpenBed: (String) -> Unit,
    onProfile: () -> Unit,
    onShop: () -> Unit,
    decos: Set<String> = emptySet(),
    reminderPrompt: (@Composable () -> Unit)? = null,
    onDuel: (() -> Unit)? = null,
) {
    val cards = remember(beds) { beds.flatMap { it.cards } }
    val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
    val seedsAvailable = minOf(seedsLeft, cards.count { it.memory.isSeed })
    val nextAt = cards.filter { !it.memory.isSeed }.map { Scheduler.dueAt(it.memory) }.filter { it > now }.minOrNull()
    val trees = cards.count { Stage.of(it.memory) == Stage.TREE }

    // Panorama: bis zu 3 durstige (zum Retten) + die schönsten Pflanzen
    val panorama = remember(beds, now) {
        val all = beds.flatMap { b -> b.cards.filter { !it.memory.isSeed }.map { it to b.bed } }
        val dry = all.filter { Scheduler.isThirsty(it.first.memory, now) }.take(3)
        val best = all.filter { it !in dry }.sortedByDescending { it.first.stability }.take(7 - dry.size)
        (best + dry).sortedBy { it.first.id.hashCode() }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            val textColor = skyTextColor(now)
            SkyScene(now, happy = thirsty == 0 && cards.any { !it.memory.isSeed }, modifier = Modifier.fillMaxWidth().height(290.dp), decos = decos) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)).clickable(onClick = onProfile),
                        contentAlignment = Alignment.Center,
                    ) { Text(profile.avatar, fontSize = 26.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(greeting(now, profile.name), style = MaterialTheme.typography.titleLarge, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${cards.count { !it.memory.isSeed }} Pflanzen · $trees ${if (trees == 1) "Baum" else "Bäume"}",
                            style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.85f),
                        )
                    }
                    DewChip(profile.dewdrops)
                }
                // Brummi fliegt über dem Garten – antippen für Tipps
                val night = DayTime.now(now).first == DayTime.NIGHT
                var tip by remember { mutableIntStateOf(-1) }
                val sfx = LocalSfx.current
                val (mood, line) = when {
                    tip >= 0 -> Mood.HAPPY to BrummiSays.pick(BrummiSays.tips, tip)
                    night -> Mood.SLEEP to "Zzz … bis morgen!"
                    thirsty > 0 -> Mood.THINK to "${thirsty} ${if (thirsty == 1) "Pflanze hat" else "Pflanzen haben"} Durst!"
                    cards.isEmpty() -> Mood.HAPPY to "Hallo! Ich bin Brummi 🐝"
                    else -> Mood.HAPPY to "Dein Garten blüht! 🌸"
                }
                Brummi(
                    mood, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, bottom = 40.dp).widthIn(max = 260.dp),
                    size = 72.dp, speech = line, bubbleOnLeft = true,
                    onTap = { sfx?.play(Sound.PLOP, pitch = 1.4f); tip++ },
                )
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp).height(120.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (panorama.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                        repeat(3) { Plant(Stage.SEED, 0f, 0f, it, Modifier.weight(1f).fillMaxHeight()) }
                        Spacer(Modifier.weight(1f))
                    } else panorama.forEach { (c, bed) ->
                        Plant(Stage.of(c.memory), Scheduler.wilt(c.memory, now), bed.hue, c.id.hashCode() and 0xff, Modifier.weight(1f).fillMaxHeight(), species = bed.species)
                    }
                }
            }
        }
        if (reminderPrompt != null && cards.any { !it.memory.isSeed }) item { Box(Modifier.padding(horizontal = 16.dp)) { reminderPrompt() } }
        item { Box(Modifier.padding(horizontal = 16.dp)) { WaterHero(thirsty, seedsAvailable, nextAt?.let { (it - now).toDouble() / Scheduler.DAY_MS }, onWater) } }
        if (beds.isEmpty()) item {
            Box(Modifier.padding(horizontal = 16.dp)) { EmptyGarden(onShop) }
        }
        if (onDuel != null && beds.isNotEmpty()) item {
            Box(Modifier.padding(horizontal = 16.dp)) { de.wissensgarten.app.ui.duel.DuelEntryCard(onDuel) }
        }
        items(beds, key = { it.bed.id }) { b -> Box(Modifier.padding(horizontal = 16.dp)) { BedCard(b, now) { onOpenBed(b.bed.id) } } }
        if (beds.isNotEmpty()) item {
            OutlinedButton(onClick = onShop, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Weiteres Beet anlegen")
            }
        }
    }
}

private fun greeting(now: Long, name: String): String = when (DayTime.now(now).first) {
    DayTime.DAWN -> "Guten Morgen, $name!"
    DayTime.DAY -> "Hallo $name!"
    DayTime.EVENING -> "Guten Abend, $name!"
    DayTime.NIGHT -> "Gute Nacht, $name 🌙"
}

@Composable
fun DewChip(dew: Int) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WaterDrop, "Tautropfen", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("${animatedCount(dew)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun WaterHero(thirsty: Int, seeds: Int, nextInDays: Double?, onWater: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            if (thirsty > 0) {
                Text("$thirsty", style = BigNumber, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${plantsWord(thirsty)} ${if (thirsty == 1) "hat" else "haben"} Durst", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Gieß sie, bevor das Wissen verblasst." + if (seeds > 0) " Dazu $seeds neue Samen." else "",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(if (seeds > 0) "Zeit zum Säen 🌱" else "Alles grün 🌿", style = MaterialTheme.typography.headlineSmall)
                val hint = buildString {
                    if (seeds > 0) append("$seeds neue Samen warten auf dich. ") else append("Für heute ist alles gesät. ")
                    if (nextInDays != null) append("Nächster Durst in ${formatDays(nextInDays)}.")
                }
                Text(hint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            JuicyButton(
                onClick = onWater,
                enabled = thirsty + seeds > 0,
                modifier = Modifier.fillMaxWidth().height(60.dp).pulse(thirsty + seeds > 0),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                sound = Sound.PLOP,
            ) {
                Icon(if (thirsty > 0) Icons.Filled.WaterDrop else Icons.Outlined.Spa, null)
                Spacer(Modifier.width(8.dp))
                Text(if (thirsty > 0) "Gießen" else "Samen säen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EmptyGarden(onShop: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Plant(Stage.SEED, 0f, 0f, 0, Modifier.size(90.dp))
            Text("Dein Garten ist noch leer", style = MaterialTheme.typography.titleLarge)
            Text("Hol dir Samen aus der Samenhandlung oder lege ein eigenes Beet an.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onShop) { Text("Zur Samenhandlung") }
        }
    }
}

@Composable
private fun BedCard(b: BedWithCards, now: Long, onClick: () -> Unit) {
    val thirsty = b.cards.count { Scheduler.isThirsty(it.memory, now) }
    val grown = b.cards.count { !it.memory.isSeed }
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.bed.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.bed.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$grown von ${b.cards.size} gewachsen" + if (thirsty > 0) " · $thirsty durstig" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (thirsty > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (thirsty > 0) FontWeight.SemiBold else null,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 6.dp),
            ) {
                // Gewachsene zuerst, damit man Fortschritt sieht
                b.cards.sortedByDescending { it.stability }.take(8).forEach { c ->
                    Plant(
                        Stage.of(c.memory), Scheduler.wilt(c.memory, now), b.bed.hue, c.id.hashCode() and 0xff,
                        Modifier.weight(1f).aspectRatio(0.8f), species = b.bed.species,
                    )
                }
                repeat((8 - b.cards.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
