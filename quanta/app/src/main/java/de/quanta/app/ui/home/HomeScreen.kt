package de.quanta.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.quanta.app.ui.fx.*
import de.quanta.app.ui.theme.Q
import de.quanta.app.ui.volt.Volt
import de.quanta.app.ui.volt.VoltMood
import kotlin.math.cos
import kotlin.math.sin

/** Sonden-Farben, freigeschaltet durch Sterne. */
data class Skin(val name: String, val color: Color, val needStars: Int)
val Skins = listOf(Skin("Neon", Q.Cyan, 0), Skin("Plasma", Q.Pink, 10), Skin("Gold", Q.Yellow, 25), Skin("Aurora", Q.Green, 45))

/** Weltenkarte: drei Welten – Kurvenflug ist offen, die anderen kommen nach und nach. */
@Composable
fun HomeScreen(
    stars: Int,
    maxStars: Int,
    cells: Int,
    skin: Int,
    onSkin: (Int) -> Unit,
    onFlight: () -> Unit,
    onSettings: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        Starfield()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("QUANTA", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Q.Cyan, letterSpacing = 6.sp)
                    Text("Mathe & Physik · Klasse 5–10", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Einstellungen", tint = Q.Muted) }
            }
            banner?.invoke()
            Volt(if (stars == 0) VoltMood.IDLE else VoltMood.HAPPY, size = 64.dp,
                speech = if (stars == 0) "Hey! Ich bin Volt. Bereit für deine erste Mission? 🚀" else "Schon $stars ⭐ gesammelt. Weiter so, Commander!")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("⭐", "$stars / $maxStars", Q.Yellow, Modifier.weight(1f))
                Stat("⚡", "$cells", Q.Yellow, Modifier.weight(1f))
            }
            WorldCard("🚀", "Kurvenflug", "Formle die Flugbahn deiner Sonde: Koordinaten, Geraden, Parabeln, Wurf & Schwerkraft.",
                Q.Cyan, open = true, onClick = onFlight) { OrbitArt(Q.Cyan) }
            WorldCard("⚙️", "Werkstatt der Maschinen", "Hebel, Rollen, Spiegel und Stromkreise zu Kettenreaktionen bauen.",
                Q.Orange, open = false) { GearArt(Q.Orange) }
            WorldCard("🗡️", "Formel-Dungeon", "Kämpfe mit Rechen-Karten gegen Gleichungs-Monster.",
                Q.Violet, open = false) { DungeonArt(Q.Violet) }
            // Hangar: Sonden-Farbe
            Text("Hangar – deine Sonde", style = MaterialTheme.typography.titleMedium, color = Q.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Skins.forEachIndexed { i, s ->
                    val unlocked = stars >= s.needStars
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Q.Panel)
                            .border(2.dp, if (i == skin) s.color else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable(enabled = unlocked) { onSkin(i) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(if (unlocked) s.color else Q.Muted.copy(alpha = 0.3f)))
                        Text(if (unlocked) s.name else "🔒 ${s.needStars}⭐", style = MaterialTheme.typography.labelSmall, color = if (unlocked) Q.Text else Q.Muted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Stat(icon: String, value: String, color: Color, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(Q.Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp); Spacer(Modifier.width(8.dp))
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun WorldCard(emoji: String, title: String, text: String, color: Color, open: Boolean, onClick: () -> Unit = {}, art: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = if (open) 0.28f else 0.1f), Q.Panel)))
            .border(2.dp, color.copy(alpha = if (open) 0.9f else 0.25f), RoundedCornerShape(22.dp))
            .clickable(interaction, null) { if (open) { sfx?.play(Sound.TAP); onClick() } else sfx?.play(Sound.BEEP, volume = 0.5f) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) { art() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$emoji $title", style = MaterialTheme.typography.titleMedium, color = if (open) Q.Text else Q.Muted)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            if (!open) Text("🔒 Wird gerade gebaut – kommt bald!", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(top = 4.dp))
            else Text("Spielen ›", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun spin(): Float {
    val t by rememberInfiniteTransition(label = "art").animateFloat(0f, 6.283f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "a")
    return t
}

@Composable
private fun OrbitArt(c: Color) {
    val a = spin()
    Canvas(Modifier.fillMaxSize()) {
        val o = center
        drawCircle(Brush.radialGradient(listOf(Q.Pink, Q.Pink.copy(alpha = 0f)), o, size.minDimension * 0.25f), size.minDimension * 0.25f, o)
        drawCircle(Q.Pink, size.minDimension * 0.12f, o)
        drawOval(c.copy(alpha = 0.5f), Offset(4f, size.height * 0.3f), androidx.compose.ui.geometry.Size(size.width - 8f, size.height * 0.4f), style = Stroke(2.5f))
        val p = o + Offset(cos(a) * (size.width / 2 - 4f), sin(a) * size.height * 0.2f)
        drawCircle(Q.Yellow, 6.dp.toPx(), p)
    }
}

@Composable
private fun GearArt(c: Color) {
    val a = spin()
    Canvas(Modifier.fillMaxSize()) {
        val o = center; val r = size.minDimension * 0.3f
        for (i in 0 until 8) {
            val ang = a * 0.5f + i * 0.785f
            drawLine(c.copy(alpha = 0.5f), o + Offset(cos(ang) * r, sin(ang) * r), o + Offset(cos(ang) * r * 1.4f, sin(ang) * r * 1.4f), 9f)
        }
        drawCircle(c.copy(alpha = 0.5f), r, o, style = Stroke(8f))
        drawCircle(c.copy(alpha = 0.35f), r * 0.35f, o)
    }
}

@Composable
private fun DungeonArt(c: Color) {
    val a = spin()
    Canvas(Modifier.fillMaxSize()) {
        val o = center
        for (i in 0..2) {
            val off = Offset((i - 1) * size.width * 0.18f, sin(a + i) * 4f)
            drawRoundRect(c.copy(alpha = 0.25f + i * 0.12f), o + off - Offset(size.width * 0.14f, size.height * 0.22f),
                androidx.compose.ui.geometry.Size(size.width * 0.28f, size.height * 0.44f), androidx.compose.ui.geometry.CornerRadius(8f))
        }
    }
}
