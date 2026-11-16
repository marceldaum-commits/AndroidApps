package de.kurvenflug.app.ui.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.flight.LocalLook
import de.kurvenflug.app.ui.flight.SpaceCanvas
import de.kurvenflug.app.ui.flight.SpaceView
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.home.Skins
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.LocalVoltHat
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Q.Text)
    }
}

/** Planeten-Lexikon: mit Sternen freischalten, antippen für Fakten. */
@Composable
fun CodexScreen(stars: Int, onBack: () -> Unit) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val sfx = LocalSfx.current
    val next = Codex.next(stars)
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 41)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header("🪐 Lexikon · ${Codex.unlocked(stars).size}/${Codex.all.size}", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (next != null) Text("Nächster Eintrag ab ${next.needStars} ⭐ (du hast $stars)", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                Codex.all.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { e ->
                            val got = stars >= e.needStars
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (got) Q.Cyan.copy(alpha = 0.12f) else Q.Panel)
                                    .border(1.5.dp, if (got) Q.Cyan.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(18.dp))
                                    .clickable { if (got) { sfx?.play(Sound.BEEP, volume = 0.5f); open = e.id } else sfx?.play(Sound.WRONG, volume = 0.3f) }
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(if (got) e.emoji else "❔", fontSize = 36.sp, modifier = Modifier.graphicsLayer { alpha = if (got) 1f else 0.4f })
                                Text(if (got) e.name else "${e.needStars} ⭐", color = if (got) Q.Text else Q.Muted, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        val entry = Codex.all.firstOrNull { it.id == open }
        AnimatedVisibility(entry != null, enter = fadeIn() + scaleIn(initialScale = 0.85f), exit = fadeOut()) {
            if (entry != null) Box(Modifier.fillMaxSize().background(Color(0xDD070B20)).clickable { open = null }, contentAlignment = Alignment.Center) {
                Column(Modifier.padding(24.dp).clip(RoundedCornerShape(24.dp)).background(Q.Panel).border(2.dp, Q.Cyan, RoundedCornerShape(24.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(entry.emoji, fontSize = 64.sp)
                    Text(entry.name, style = MaterialTheme.typography.headlineMedium, color = Q.Cyan)
                    entry.facts.forEach { f -> Text("• $f", color = Q.Text) }
                    Volt(VoltMood.WOW, size = 48.dp, speech = "Krass, oder? 🤯")
                    TextButton(onClick = { open = null }) { Text("Schließen") }
                }
            }
        }
    }
}

/**
 * Werkstatt: Farbe (über Sterne), Form, Spur und Volts Hut (über Energiezellen).
 * Nur Aussehen – keine Vorteile im Spiel.
 */
@Composable
fun WorkshopScreen(stars: Int, cells: Int, skin: Int, look: Triple<Int, Int, Int>, onSkin: (Int) -> Unit,
                   onLook: (shape: Int, trail: Int, hat: Int) -> Unit, onBack: () -> Unit) {
    val sfx = LocalSfx.current
    val (shape, trail, hat) = look
    val color = Skins.getOrElse(skin) { Skins[0] }.color
    // kleine Vorschau-Bahn
    val demo = remember {
        val l = Levels.byId("3-6")!!
        Flight.path(l, l.solution).let { p -> l to p }
    }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 42)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header("🎨 Werkstatt", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalVoltHat provides hat) { Volt(VoltMood.HAPPY, size = 72.dp) }
                    Box(Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x66070B20))) {
                        val (l, p) = demo
                        CompositionLocalProvider(LocalLook provides look) {
                            SpaceCanvas(SpaceView(l.copy(asteroids = emptyList(), cells = emptyList()), emptyList(), null, emptyList(), p.take(p.size * 2 / 3),
                                p[p.size * 2 / 3], l.targets.indices.toSet(), emptySet(), true, emptyList(), color, 1f), Modifier.fillMaxSize())
                        }
                    }
                }
                Text("Nur fürs Aussehen – Punkte gibt's fürs Rechnen. 😉", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                Section("Farbe", "⭐ $stars", Skins.map { Cosmetic(it.name, "●", it.needStars) }, skin, stars, colorOf = { Skins[it].color }) { onSkin(it); sfx?.play(Sound.CELL) }
                Section("Sonde", "⚡ $cells", Workshop.shapes, shape, cells) { onLook(it, trail, hat); sfx?.play(Sound.CELL) }
                Section("Spur", "⚡ $cells", Workshop.trails, trail, cells) { onLook(shape, it, hat); sfx?.play(Sound.CELL) }
                Section("Volts Hut", "⚡ $cells", Workshop.hats, hat, cells) { onLook(shape, trail, it); sfx?.play(Sound.CELL) }
            }
        }
    }
}

@Composable
private fun Section(title: String, have: String, items: List<Cosmetic>, selected: Int, budget: Int,
                    colorOf: ((Int) -> Color)? = null, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Q.Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row { Text(title, style = MaterialTheme.typography.titleMedium, color = Q.Text, modifier = Modifier.weight(1f)); Text(have, color = Q.Yellow) }
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    val i = items.indexOf(c)
                    val proLocked = c.pro && !de.kurvenflug.app.LocalPro.current.pro
                    val open = budget >= c.needCells && !proLocked
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (i == selected) Q.Cyan.copy(alpha = 0.2f) else Color(0x22000000))
                            .border(2.dp, if (i == selected) Q.Cyan else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable(enabled = open) { onPick(i) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (colorOf != null) Box(Modifier.size(22.dp).clip(CircleShape).background(if (open) colorOf(i) else Q.Muted.copy(alpha = 0.3f)))
                        else Text(if (open) c.emoji else "🔒", fontSize = 22.sp)
                        Text(if (open) c.name else if (proLocked) "⭐ Pro" else "${c.needCells}${if (colorOf != null) "⭐" else "⚡"}", style = MaterialTheme.typography.labelSmall,
                            color = if (open) Q.Text else Q.Muted, maxLines = 1)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
