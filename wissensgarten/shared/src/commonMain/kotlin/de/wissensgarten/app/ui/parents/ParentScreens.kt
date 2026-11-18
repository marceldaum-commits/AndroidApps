package de.wissensgarten.app.ui.parents

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.Brummi
import de.wissensgarten.app.ui.fx.Mood
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.Subject
import de.wissensgarten.core.model.WeekReport

/** Eltern-Wochenbericht: pro Kind, nur auf dem Gerät berechnet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekReportScreen(
    profiles: List<ProfileEntity>, startId: String?, report: (String) -> WeekReport.Report,
    /** Hinweis auf unsere Nachhilfe (nur hier hinter dem Eltern-Tor, sachlich, ohne Figur) */
    hint: @Composable ColumnScope.(ProfileEntity) -> Unit = {},
    onBack: () -> Unit,
) {
    var sel by remember { mutableStateOf(startId ?: profiles.firstOrNull()?.id) }
    val p = profiles.firstOrNull { it.id == sel }
    val r = remember(sel) { sel?.let(report) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
            Text("📊 Wochenbericht", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (profiles.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { pr -> FilterChip(selected = pr.id == sel, onClick = { sel = pr.id }, label = { Text("${pr.avatar} ${pr.name}") }) }
            }
            if (p == null || r == null) { Text("Noch kein Profil."); return@Column }
            Text("Die letzten 7 Tage von ${p.name}. Berechnet nur auf diesem Gerät – nichts wird verschickt.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card("Gelernt an ${r.activeDays} von 7 Tagen") {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    val names = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                    val today = de.wissensgarten.app.ui.common.Clock.weekday(de.wissensgarten.core.model.Platform.nowMs())
                    r.days.forEachIndexed { i, on ->
                        val pop by animateFloatAsState(if (on) 1f else 0.7f, spring(Spring.DampingRatioMediumBouncy), label = "d")
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(34.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape)
                                .background(if (on) Color(0xFF7CB342) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(if (on) "💧" else "", fontSize = 16.sp)
                            }
                            Text(names[(today - 6 + i + 14) % 7], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Stat("${r.reviews}", "Karten gegossen", Modifier.weight(1f))
                    Stat("${r.rightPct} %", "gewusst", Modifier.weight(1f))
                    Stat("≈ ${r.minutes}", "Minuten", Modifier.weight(1f))
                }
            }
            if (r.subjects.isNotEmpty()) Card("Fächer") {
                val max = r.subjects.maxOf { it.reviews }.coerceAtLeast(1)
                r.subjects.forEach { s ->
                    val info = Subject.of(s.name)
                    val w by animateFloatAsState(s.reviews / max.toFloat(), tween(700), label = "w")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${info.emoji} ${info.label}", Modifier.width(130.dp), maxLines = 1, softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(7.dp))
                                .background(de.wissensgarten.app.ui.fx.SafeBrush.horizontalGradient(listOf(Color.hsv(info.hue, 0.45f, 0.85f), Color.hsv(info.hue, 0.6f, 0.7f)))))
                        }
                        Text("${s.reviews} · ${s.rightPct} %", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (r.strong.isNotEmpty()) Card("🌳 Sitzt richtig gut") { r.strong.forEach { Text("• $it", maxLines = 2) } }
            if (r.hard.isNotEmpty()) Card("🌱 Fällt noch schwer") {
                r.hard.forEach { Text("• $it", maxLines = 2) }
                Text("Das ist normal – genau diese Pflanzen bekommen jetzt öfter Wasser.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brummi(Mood.THINK, size = 72.dp)
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Brummis Tipp für Eltern", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(r.tip, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            hint(p)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Sachlicher Hinweis an Eltern (Du-Form, ohne Figur, ohne Preis): ein Thema fällt länger schwer oder eine Klassenarbeit steht an.
 * Mit „Ausblenden“ verschwindet er für dieses Thema bzw. diese Arbeit dauerhaft.
 */
@Composable
fun NachhilfeKarte(text: String, button: String, onOpen: () -> Unit, onHide: () -> Unit, second: String? = null, onSecond: () -> Unit = {}) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Für Eltern", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text(button, textAlign = TextAlign.Center) }
                TextButton(onClick = onHide) { Text("Ausblenden") }
            }
            if (second != null) OutlinedButton(onClick = onSecond, modifier = Modifier.fillMaxWidth()) { Text(second, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

/**
 * Gesunde Lernzeit vorbei: Brummi verabschiedet sich freundlich. Keine Strafe, kein Zähler –
 * nur Eltern können (nach der Eltern-Aufgabe) 15 Minuten dazugeben.
 */
@Composable
fun RestOverlay(name: String, onMore: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "nacht").animateFloat(0.4f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "g")
    Box(
        Modifier.fillMaxSize().background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFF1B2547), Color(0xFF3B3F7A), Color(0xFF5B4E8C))))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
    ) {
        // Sterne
        listOf(0.1f to 0.12f, 0.8f to 0.08f, 0.3f to 0.25f, 0.65f to 0.2f, 0.9f to 0.35f, 0.15f to 0.4f).forEachIndexed { i, (x, y) ->
            Text("✦", color = Color.White.copy(alpha = if (i % 2 == 0) glow else 1.4f - glow), fontSize = 14.sp,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart).offset(x = (x * 360).dp, y = (y * 700).dp))
        }
        Column(Modifier.padding(24.dp).widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("🌙", fontSize = 64.sp, modifier = Modifier.graphicsLayer { scaleX = 0.9f + glow * 0.1f; scaleY = 0.9f + glow * 0.1f })
            Brummi(Mood.SLEEP, size = 120.dp)
            Text("Der Garten ruht sich aus", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Text("Super gemacht, $name! Für heute ist die Lernzeit um. Pflanzen – und Köpfe – wachsen am besten mit Pausen. Bis morgen! 💤",
                color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
            TextButton(onClick = onMore) { Text("👨‍👩‍👧 Eltern: 15 Minuten dazugeben", color = Color.White.copy(alpha = 0.8f)) }
        }
    }
}
