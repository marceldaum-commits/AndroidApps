package de.experimentopia.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.Check
import de.experimentopia.core.model.Choice
import de.experimentopia.core.model.checkInput
import de.experimentopia.core.model.Fmt

/** Kopfzeile mit Zurück-Pfeil */
@Composable
fun Bar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("zurueck")) { Text("←", fontSize = 26.sp, color = E.Text, fontWeight = FontWeight.Bold) }
        // Lange Titel dürfen umbrechen (schmale Handys ab 320 dp), werden dafür etwas kleiner
        Text(title, style = if (title.length > 18) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, color = E.Text,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        trailing()
    }
}

/** Glasige Fläche für Inhalte */
@Composable
fun Panel(modifier: Modifier = Modifier, border: Color = E.Line, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(E.Deep.copy(alpha = 0.92f)).border(1.5.dp, border, RoundedCornerShape(20.dp)).padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** Kleine Pille mit Emoji und Zahl */
@Composable
fun Pill(text: String, color: Color = E.Sun, modifier: Modifier = Modifier) {
    Text(text, modifier.background(color.copy(alpha = 0.16f), CircleShape).border(1.dp, color.copy(alpha = 0.6f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

/** Große Antwortkarte (Vermutung, Erklärung, Knobelfrage) */
@Composable
fun ChoiceCard(
    choice: Choice, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    state: Boolean? = null, shakeKey: Int = 0, enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val border by animateColorAsState(when (state) { true -> E.Green; false -> E.Red.copy(alpha = 0.7f); null -> if (selected) E.Sun else E.Line }, label = "b")
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, spring(dampingRatio = 0.5f), label = "s")
    Row(
        modifier.fillMaxWidth().shake(shakeKey).graphicsLayer { scaleX = scale; scaleY = scale }.pressBounce(interaction)
            .clip(RoundedCornerShape(18.dp)).background(if (selected) E.Panel else E.Deep).border(2.dp, border, RoundedCornerShape(18.dp))
            .clickable(interaction, null, enabled = enabled) { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choice.color?.let { Box(Modifier.size(30.dp).clip(CircleShape).background(Color(it.toInt())).border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)); Spacer(Modifier.width(12.dp)) }
        if (choice.emoji.isNotEmpty()) { Text(choice.emoji, fontSize = 26.sp); Spacer(Modifier.width(12.dp)) }
        Text(choice.text, style = MaterialTheme.typography.bodyLarge, color = E.Text, modifier = Modifier.weight(1f))
        when (state) { true -> Text("✅", fontSize = 20.sp); false -> Text("✖️", fontSize = 16.sp); null -> {} }
    }
}

/** Sterne 0..3 */
@Composable
fun Stars(n: Int, size: Int = 16, max: Int = 3) {
    Row { repeat(max) { i -> Text(if (i < n) "⭐" else "☆", fontSize = size.sp, color = if (i < n) E.Sun else E.Muted.copy(alpha = 0.5f)) } }
}

/** Großer Messwert mit Beschriftung (wie auf einem Messgerät) */
@Composable
fun Readout(label: String, value: String, color: Color = E.Mint, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0B0820)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = E.Muted, fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

/** Auswahl-Chips in einer Reihe (umbrechend) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipRow(items: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit, modifier: Modifier = Modifier, color: Color = E.Mint, enabled: (T) -> Boolean = { true },
                selectedIf: (T) -> Boolean = { it == selected }) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { it ->
            val sel = selectedIf(it)
            val ok = enabled(it)
            Text(label(it), Modifier.clip(CircleShape).background(if (sel) color.copy(alpha = 0.3f) else E.Panel)
                .border(1.5.dp, if (sel) color else E.Line, CircleShape)
                .clickable(enabled = ok) { sfx?.play(Sound.TAP); view.buzz(Buzz.TICK, vib); onPick(it) }
                .padding(horizontal = 12.dp, vertical = 7.dp), color = if (ok) E.Text else E.Muted.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

/** Regler mit Beschriftung und Wert */
@Composable
fun ValueSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, text: String, onChange: (Float) -> Unit, steps: Int = 0, color: Color = E.Mint, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = E.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Slider(value, onChange, Modifier.testTag("regler:$label"), valueRange = range, steps = steps, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = E.Panel))
    }
}

/**
 * Eigene Werte eingeben – gut sichtbar, mit verständlicher Rückmeldung bei unpassender Eingabe.
 * Übernommen wird der Wert erst, wenn er passt.
 */
@Composable
fun NumberField(label: String, unit: String, min: Double, max: Double, initial: Double, onValid: (Double) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(initial) { mutableStateOf(Fmt.num(initial)) }
    val check = remember(text) { checkInput(text, min, max, unit) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; (checkInput(it, min, max, unit) as? Check.Ok)?.let { ok -> onValid(ok.value) } },
        label = { Text(if (unit.isBlank()) label else "$label ($unit)") },
        singleLine = true,
        isError = check is Check.Bad,
        supportingText = { Text(when (check) { is Check.Bad -> "🙈 ${check.message}"; is Check.Ok -> "✓ erlaubt: ${Fmt.num(min)} bis ${Fmt.num(max)} $unit" }, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Hinweisstreifen oben im Labor */
@Composable
fun TaskBanner(text: String, done: Boolean, modifier: Modifier = Modifier) {
    val c by animateColorAsState(if (done) E.Green else E.Sun, label = "c")
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.14f)).border(1.5.dp, c.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (done) "✅" else "🎯", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(if (done) "Geschafft! Schau dir an, was passiert ist." else text, color = E.Text, fontSize = 14.sp, modifier = Modifier.testTag("auftrag"))
    }
}

/** Sicherheitshinweis für echte Versuche zu Hause (aus [de.experimentopia.core.model.Safety]) */
@Composable
fun SafetyNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(E.Orange.copy(alpha = 0.14f)).border(1.5.dp, E.Orange, RoundedCornerShape(14.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp).testTag("sicherheit"), verticalAlignment = Alignment.Top) {
        Text("⚠️", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text("In echt: Sicherheit zuerst", color = E.Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text, color = E.Text, fontSize = 13.sp)
            ReadButton("Sicherheit zuerst: $text", Modifier.padding(top = 4.dp))
        }
    }
}

/** „🔊 Vorlesen“ – nur sichtbar, wenn Vorlesen eingeschaltet ist. Nochmal antippen hält an. */
@Composable
fun ReadButton(text: String, modifier: Modifier = Modifier) {
    val reader = LocalReader.current ?: return
    if (!LocalReadOn.current) return
    val busy = reader.speaking
    Row(modifier.clip(CircleShape).background(E.Mint.copy(alpha = 0.14f)).border(1.5.dp, E.Mint, CircleShape)
        .clickable { if (busy) reader.stop() else reader.say(text) }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("vorlesen"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (busy) "⏹️" else "🔊", fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(if (busy) "Stopp" else "Vorlesen", color = E.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
