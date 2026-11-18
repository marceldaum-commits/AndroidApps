package de.kurvenflug.app.ui.story

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlin.math.sin

private val roomColors = listOf(Q.Cyan, Q.Green, Color(0xFF7CFF7C), Q.Orange, Q.Violet, Q.Pink)

/** Sticker zu einem Album-Schlüssel („rocket“, „rocket*“ für Glitzer, „s_halloween“ für Jahreszeiten) */
fun stickerFor(key: String): Pair<Sticker, Boolean>? {
    val glitter = key.endsWith("*")
    val id = key.removeSuffix("*")
    return (Stickers.byId(id) ?: SeasonEvent.entries.map { it.sticker }.firstOrNull { it.id == id })?.let { it to glitter }
}

/**
 * Raumstation: Räume mit Sternenstaub freischalten, Sticker an die Wände kleben, Crew wohnt dort.
 * Ein ruhiger Ort ohne Aufgaben – gefüllt wird er durchs Rechnen.
 */
@Composable
fun StationScreen(
    allStars: Map<String, Int>, unlocked: Set<Int>, decor: Map<String, String>, owned: Set<String>, rescued: Set<Int>,
    onUnlock: (Int) -> Unit, onDecor: (room: Int, slot: Int, sticker: String?) -> Unit, onBack: () -> Unit,
) {
    val balance = Station.balance(allStars, unlocked)
    var picking by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    val sfx = LocalSfx.current
    val t by rememberInfiniteTransition(label = "station").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 11)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🏠 Raumstation", style = MaterialTheme.typography.titleLarge, color = Q.Text, modifier = Modifier.weight(1f))
                Text("✨ $balance", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(end = 16.dp))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Volt(VoltMood.HAPPY, size = 52.dp, speech = "Unser Zuhause im All! Jeder Stern, den du sammelst, wird zu Sternenstaub ✨ – damit schalten wir Räume frei. Tipp auf einen Platz und kleb einen Sticker hin!")
                Station.rooms.forEach { room ->
                    val isOpen = room.id in Station.open(unlocked)
                    val color = roomColors[room.id % roomColors.size]
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(if (isOpen) Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Q.Panel)) else Brush.verticalGradient(listOf(Q.Panel, Q.Panel)))
                        .border(1.5.dp, if (isOpen) color.copy(alpha = 0.7f) else Q.Muted.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isOpen) room.emoji else "🔒", fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(room.name, fontWeight = FontWeight.Black, color = if (isOpen) Q.Text else Q.Muted, modifier = Modifier.weight(1f))
                            // Bewohner laufen herum
                            if (isOpen) Station.residents(room.id, rescued, unlocked).forEachIndexed { i, m ->
                                Text(m.emoji, fontSize = 24.sp, modifier = Modifier.graphicsLayer { translationX = 10f * sin(t * 1.5f + i * 2f); translationY = -4f * kotlin.math.abs(sin(t * 4f + i)) })
                            }
                        }
                        if (isOpen) {
                            (0 until Station.SLOTS).chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { slot ->
                                        val key = decor["${room.id}:$slot"]
                                        val st = key?.let { stickerFor(it) }
                                        Box(Modifier.weight(1f).aspectRatio(1.1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .clickable { sfx?.play(Sound.TAP); picking = room.id to slot }, contentAlignment = Alignment.Center) {
                                            if (st != null) Text(st.first.emoji, fontSize = 32.sp, modifier = Modifier.graphicsLayer {
                                                rotationZ = 4f * sin(t * 2f + slot); if (st.second) { scaleX = 1f + 0.06f * sin(t * 5f); scaleY = scaleX }
                                            }) else Text("+", color = Q.Muted, fontSize = 22.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            val can = Station.canUnlock(room, allStars, unlocked)
                            NeonButton(onClick = { sfx?.play(Sound.WIN); onUnlock(room.id) }, enabled = can, color = color, modifier = Modifier.fillMaxWidth()) {
                                Text(if (can) "Freischalten für ${room.cost} ✨" else "Braucht ${room.cost} ✨ (noch ${room.cost - balance})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    picking?.let { (room, slot) ->
        Dialog(onDismissRequest = { picking = null }) {
            Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Q.Space).border(2.dp, Q.Pink, RoundedCornerShape(24.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Welcher Sticker?", fontWeight = FontWeight.Black, color = Q.Text)
                if (owned.isEmpty()) Text("Noch keine Sticker – die gibt's manchmal nach geschafften Leveln! 🎁", color = Q.Muted)
                owned.sorted().mapNotNull { k -> stickerFor(k)?.let { k to it } }.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (k, st) ->
                            Text(st.first.emoji + if (st.second) "✨" else "", fontSize = 28.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .clickable { sfx?.play(Sound.HIT); onDecor(room, slot, k); picking = null }.padding(4.dp))
                        }
                    }
                }
                Row {
                    TextButton(onClick = { onDecor(room, slot, null); picking = null }) { Text("Platz leeren", color = Q.Muted) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { picking = null }) { Text("Schließen", color = Q.Muted) }
                }
            }
        }
    }
}

/** Karte auf der Startseite, solange ein Gast-Planet da ist. */
@Composable
fun SeasonCard(s: SeasonEvent, done: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val t by rememberInfiniteTransition(label = "season").animateFloat(-8f, 8f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w")
    Row(Modifier.fillMaxWidth().pressBounce(interaction).pulse(!done, 0.015f).clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(Q.Orange.copy(alpha = 0.35f), Q.Panel))).border(2.dp, if (done) Q.Green else Q.Orange, RoundedCornerShape(20.dp))
        .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(s.emoji, fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = t })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Gast-Planet: ${s.planet}", fontWeight = FontWeight.Black, color = Q.Text)
            Text(if (done) "Geschafft – ${s.sticker.name}-Sticker ist im Album ✅" else "${s.title}-Besuch · 5 Etappen · Belohnung: ${s.sticker.emoji}-Sticker",
                style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", color = Q.Orange, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/** Der Gast-Planet: 5 Etappen, am Ende der Themen-Sticker. Kommt jedes Jahr wieder. */
@Composable
fun SeasonScreen(s: SeasonEvent, year: Int, allStars: Map<String, Int>, onLevel: (Level) -> Unit, onBack: () -> Unit) {
    val done = Seasons.done(s, year, allStars)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1430), Q.Space)))) {
        Starfield(seed = 7)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("${s.emoji} ${s.planet}", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Volt(if (done) VoltMood.DANCE else VoltMood.HAPPY, size = 56.dp, speech = if (done) de.kurvenflug.core.model.VoltLines.sticker(s.sticker.name, s.emoji)
                    else de.kurvenflug.core.model.VoltLines.guest(s.during))
                de.kurvenflug.app.ui.flight.ChapterCard(Seasons.chapter(s, year), 2, allStars, onLevel)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Panel).padding(12.dp)) {
                    de.kurvenflug.app.ui.collection.StickerTile(s.sticker, done, false, Modifier.width(80.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (done) "Im Album! ✅" else "Belohnung nach Etappe 5", color = Q.Text, textAlign = TextAlign.Start)
                }
            }
        }
    }
}
