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
    familyClaimed: Boolean = false,
    onClaimFamily: () -> Unit = {},
    onPostcard: (String) -> Unit = {},
    onCertificate: (String) -> Unit = {},
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
        item { FamilyTree(profiles, logs, familyClaimed, onClaimFamily) }
        item { de.wissensgarten.app.ui.duel.DuelEntryCard(onDuel) }
        items(profiles, key = { it.id }) { p ->
            ProfileCard(
                p, p.id == activeId,
                beds.filter { it.bed.profileId == p.id },
                logs.filter { it.profileId == p.id }, now,
                onPostcard = { onPostcard(p.id) }, onCertificate = { onCertificate(p.id) },
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
    p: ProfileEntity, active: Boolean, beds: List<BedWithCards>, logs: List<ReviewLogEntity>, now: Long,
    onPostcard: () -> Unit = {}, onCertificate: () -> Unit = {}, onClick: () -> Unit,
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
            if (grown > 0) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPostcard, modifier = Modifier.weight(1f)) { Text("💌 Postkarte", maxLines = 1) }
                FilledTonalButton(onClick = onCertificate, modifier = Modifier.weight(1f)) { Text("🏅 Urkunde", maxLines = 1) }
            }
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


/**
 * Familienbaum: wächst mit allem, was die ganze Familie diese Woche gießt.
 * Gemeinsames Ziel statt Wettkampf – ist es erreicht, bekommt jeder Tautropfen.
 */
@Composable
private fun FamilyTree(profiles: List<ProfileEntity>, logs: List<ReviewLogEntity>, claimed: Boolean, onClaim: () -> Unit) {
    val goal = 20 * profiles.size.coerceAtLeast(1)
    val total = logs.size
    val p = (total.toFloat() / goal).coerceIn(0f, 1f)
    // erst Keimling, ab 30 % ein Baum, der bis zum Ziel immer größer wird
    val stage = when { p < 0.1f -> Stage.SEED; p < 0.2f -> Stage.SPROUT; p < 0.3f -> Stage.SHOOT; else -> Stage.TREE }
    val treeGrow = if (stage == Stage.TREE) ((p - 0.3f) / 0.7f).coerceIn(0f, 1f) else 1f
    val sfx = de.wissensgarten.app.ui.fx.LocalSfx.current
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE8F5E9)), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌳 Familienbaum", style = MaterialTheme.typography.titleLarge)
            Text("Wächst mit allem, was ihr zusammen diese Woche gießt.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                de.wissensgarten.app.ui.common.Plant(stage, 0f, 330f, 7, Modifier.size(170.dp), grow = treeGrow, species = "kirschbaum")
                // Wer hat mitgegossen? Avatare rund um den Baum
                profiles.forEachIndexed { i, pr ->
                    val n = logs.count { it.profileId == pr.id }
                    // abwechselnd links und rechts neben den Baum, wie Gärtner beim Gießen
                    val side = if (i % 2 == 0) -1 else 1
                    val row = i / 2
                    Column(Modifier.offset(x = (side * (115 + row * 10)).dp, y = (45 - row * 55).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pr.avatar, fontSize = 26.sp)
                        Text("$n", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
            Text("$total von $goal gemeinsam gegossen", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            if (p >= 1f) {
                if (claimed) Text("🎉 Geschafft! Belohnung abgeholt – nächste Woche geht's weiter.", style = MaterialTheme.typography.bodySmall)
                else de.wissensgarten.app.ui.fx.JuicyButton(onClick = { sfx?.play(de.wissensgarten.app.ui.fx.Sound.FANFARE); onClaim() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("🎁 Jeder bekommt +20 💧")
                }
            }
        }
    }
}

