package de.wissensgarten.app.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.data.db.ReviewLogEntity
import de.wissensgarten.core.model.Levels
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage
import java.util.Calendar

/**
 * Familie: alle Profile auf diesem Gerät mit Wochen-Überblick – damit Eltern sehen,
 * was ihr Kind lernt, ohne kontrollieren zu müssen.
 */
@Composable
fun FamilyScreen(
    profiles: List<ProfileEntity>,
    activeId: String?,
    beds: List<BedWithCards>,
    logs: List<ReviewLogEntity>,
    now: Long,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onDuel: () -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Familie", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Jeder hat seinen eigenen Garten. Tippe auf ein Profil, um zu wechseln.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp),
            )
        }
        item { de.wissensgarten.app.ui.duel.DuelEntryCard(onDuel) }
        items(profiles, key = { it.id }) { p ->
            ProfileCard(
                p, p.id == activeId,
                beds.filter { it.bed.profileId == p.id },
                logs.filter { it.profileId == p.id }, now,
            ) { onSwitch(p.id) }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Familienmitglied hinzufügen")
            }
        }
        item {
            Text(
                "Tipp für Eltern: Lernt gemeinsam! Wer zuerst seine durstigen Pflanzen gegossen hat, " +
                    "darf sich das Abendessen aussuchen. 🌱",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

@Composable
private fun ProfileCard(
    p: ProfileEntity, active: Boolean, beds: List<BedWithCards>, logs: List<ReviewLogEntity>, now: Long, onClick: () -> Unit,
) {
    val cards = beds.flatMap { it.cards }
    val grown = cards.count { !it.memory.isSeed }
    val trees = cards.count { Stage.of(it.memory) == Stage.TREE }
    val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
    val perDay = lastSevenDays(logs, now)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    Text(p.avatar, fontSize = 28.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleLarge)
                    Text(Levels.label(p.level) + if (active) " · aktiv" else "", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("${beds.size}", "Beete")
                Stat("$grown", "gewachsen")
                Stat("$trees", "Bäume")
                Stat("$thirsty", "durstig")
            }
            Spacer(Modifier.height(12.dp))
            Text("Gegossen in den letzten 7 Tagen: ${perDay.sum()}", style = MaterialTheme.typography.labelLarge)
            WeekBars(perDay, now)
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val DAY_LETTERS = listOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa")

private fun lastSevenDays(logs: List<ReviewLogEntity>, now: Long): List<Int> {
    val cal = Calendar.getInstance()
    fun dayKey(t: Long): Int { cal.timeInMillis = t; return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR) }
    val counts = logs.groupingBy { dayKey(it.time) }.eachCount()
    return (6 downTo 0).map { counts[dayKey(now - it * Scheduler.DAY_MS)] ?: 0 }
}

@Composable
private fun WeekBars(perDay: List<Int>, now: Long) {
    val max = (perDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val todayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
    Row(Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        perDay.forEachIndexed { i, n ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    Modifier.fillMaxWidth().height((40f * n / max).dp.coerceAtLeast(3.dp)).clip(MaterialTheme.shapes.small)
                        .background(if (n > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                )
                Text(DAY_LETTERS[(todayIdx - 6 + i + 14) % 7], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
