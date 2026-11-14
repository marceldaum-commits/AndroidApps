package de.wissensgarten.app.ui.exam

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.ExamPlan
import de.wissensgarten.core.model.ExamPlanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

/** Garten-Karte zu einer Klassenarbeit: Kalenderblatt mit Countdown, Fitness-Ring, Brummi, Start-Knopf. */
@Composable
fun ExamCard(plan: ExamPlan, bedName: String, onStart: () -> Unit, onFeedback: (Int) -> Unit, onDelete: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    var menu by remember { mutableStateOf(false) }
    // Antwort auf „Wie lief's?“: Brummi reagiert erst, dann verschwindet die Karte
    var feedback by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(feedback) { feedback?.let { kotlinx.coroutines.delay(2600); onFeedback(it) } }
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Box {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarPage(plan.daysLeft)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("📝 ${plan.exam.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4E3B00))
                        Text("$bedName · ${LocalDate.ofEpochDay(plan.exam.day).format(DATE)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6D5A1E))
                    }
                    if (plan.phase != ExamPlan.Phase.PAST) FitnessRing(plan.fitness)
                    Box {
                        TextButton(onClick = { menu = true }) { Text("⋮", fontSize = 20.sp) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Termin löschen") }, onClick = { menu = false; onDelete() })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Brummi(if (plan.phase == ExamPlan.Phase.EXAM_DAY || plan.fitness >= 0.8) Mood.CHEER else Mood.HAPPY, size = 44.dp)
                    Text(when (feedback) {
                        0 -> "Juhu! Das Gießen hat sich gelohnt! 🎉"
                        1 -> "Prima! Darauf kannst du stolz sein. 🌻"
                        2 -> "Das ist okay! Jede Arbeit ist anders – und alles, was du gelernt hast, bleibt in deinem Garten. 💛"
                        else -> ExamPlanner.brummiSays(plan)
                    }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp), color = Color(0xFF3E2C00))
                }
                Spacer(Modifier.height(10.dp))
                if (plan.phase == ExamPlan.Phase.PAST) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("😃" to "Super", "🙂" to "Gut", "😕" to "Naja").forEachIndexed { i, (e, l) ->
                            OutlinedButton(onClick = {
                                if (feedback != null) return@OutlinedButton
                                if (i == 0) { sfx?.play(Sound.FANFARE); particles.burst(Burst.CONFETTI, count = 60) } else sfx?.play(Sound.GROW)
                                feedback = i
                            }, modifier = Modifier.weight(1f)) { Text("$e $l") }
                        }
                    }
                } else if (plan.today.isNotEmpty()) {
                    JuicyButton(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp).pulse(plan.daysLeft <= 1, 0.03f)) {
                        Text(when (plan.phase) {
                            ExamPlan.Phase.EXAM_DAY -> "🔥 Aufwärmen (${plan.today.size} Karten)"
                            ExamPlan.Phase.FINAL_DAY -> "🎬 Generalprobe (${plan.today.size} Karten)"
                            else -> "📚 Heute ${plan.today.size} Karten"
                        }, fontWeight = FontWeight.Bold)
                    }
                } else Text("✓ Für heute geschafft", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}

/** Abreißkalender-Blatt mit den restlichen Tagen – blättert beim Erscheinen um. */
@Composable
private fun CalendarPage(daysLeft: Int) {
    val flip = remember { Animatable(-90f) }
    LaunchedEffect(daysLeft) { flip.snapTo(-90f); flip.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 220f)) }
    Column(
        Modifier.width(64.dp).graphicsLayer { rotationX = flip.value; cameraDistance = 10f * density }
            .clip(RoundedCornerShape(10.dp)).background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().background(Color(0xFFE53935)).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text(when { daysLeft == 0 -> "HEUTE"; daysLeft == 1 -> "MORGEN"; daysLeft < 0 -> "VORBEI"; else -> "NOCH" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text(if (daysLeft > 1) "$daysLeft" else if (daysLeft == 1) "1" else "🍀", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF3E2C00))
        Text(if (daysLeft > 1) "Tage" else if (daysLeft == 1) "Tag" else "", fontSize = 13.sp, color = Color(0xFF6D5A1E))
        Spacer(Modifier.height(4.dp))
    }
}

/** Wie fit bin ich bis zum Termin? Ring füllt sich animiert. */
@Composable
private fun FitnessRing(fitness: Double) {
    val p by animateFloatAsState(fitness.toFloat(), tween(1200, easing = FastOutSlowInEasing), label = "fit")
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
            val inset = 4.dp.toPx()
            drawArc(Color(0xFFE6DCC0), 0f, 360f, false, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = s)
            drawArc(if (p >= 0.8f) Color(0xFF43A047) else Color(0xFFFFB300), -90f, 360f * p, false, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = s)
        }
        // ganz am Anfang kein entmutigendes „0 %“ – sondern ein Keimling
        if (fitness < 0.2) Text("🌱", fontSize = 24.sp)
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(p * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text("fit", fontSize = 12.sp)
        }
    }
}

/** Einladung, eine Klassenarbeit einzutragen. */
@Composable
fun ExamPromptCard(onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📝", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Klassenarbeit bald?", style = MaterialTheme.typography.titleSmall)
                Text("Trag sie ein – Brummi plant, damit am Tag X alles sitzt. Ohne Pauken am Vorabend.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Termin eintragen: Art, Beet, Datum. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExamDialog(beds: List<Pair<String, String>>, preselect: String?, onDismiss: () -> Unit, onAdd: (bedId: String, title: String, day: Long) -> Unit) {
    var title by remember { mutableStateOf("Klassenarbeit") }
    var bed by remember { mutableStateOf(preselect ?: beds.firstOrNull()?.first) }
    val today = LocalDate.now().toEpochDay()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = (today + 7) * 86_400_000L,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / 86_400_000L >= today
        },
    )
    var pickDate by remember { mutableStateOf(false) }
    val chosenDay = picker.selectedDateMillis?.let { it / 86_400_000L }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📝 Termin eintragen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Klassenarbeit", "Test", "Vokabeltest", "Abfrage").forEach { t ->
                        FilterChip(title == t, { title = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(title, { title = it.take(30) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Welches Beet?", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    beds.forEach { (id, name) -> FilterChip(bed == id, { bed = id }, label = { Text(name, maxLines = 1) }) }
                }
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(chosenDay?.let { "📝 " + LocalDate.ofEpochDay(it).format(DATE) } ?: "📝 Datum wählen")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { val b = bed; val d = chosenDay; if (b != null && d != null) onAdd(b, title, d) }, enabled = bed != null && chosenDay != null) { Text("Eintragen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
    if (pickDate) DatePickerDialog(
        onDismissRequest = { pickDate = false },
        confirmButton = { TextButton(onClick = { pickDate = false }) { Text("OK") } },
    ) { DatePicker(picker, showModeToggle = false) }
}
