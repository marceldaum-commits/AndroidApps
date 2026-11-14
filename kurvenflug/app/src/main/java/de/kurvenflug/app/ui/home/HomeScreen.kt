package de.kurvenflug.app.ui.home

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
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import kotlin.math.cos
import kotlin.math.sin

/** Sonden-Farben, freigeschaltet durch Sterne. */
data class Skin(val name: String, val color: Color, val needStars: Int)
val Skins = listOf(Skin("Neon", Q.Cyan, 0), Skin("Plasma", Q.Pink, 10), Skin("Gold", Q.Yellow, 25), Skin("Aurora", Q.Green, 45))

/** Startseite: Volt, Fortschritt, alle Kapitel mit ihren Leveln, Hangar. */
@Composable
fun HomeScreen(
    stars: Int,
    maxStars: Int,
    cells: Int,
    skin: Int,
    onSkin: (Int) -> Unit,
    chapters: List<de.kurvenflug.core.model.Chapter>,
    levelStars: Map<String, Int>,
    onLevel: (de.kurvenflug.core.model.Level) -> Unit,
    onSettings: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        Starfield()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KURVENFLUG", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Q.Cyan, letterSpacing = 4.sp)
                    Text("Mathe & Physik im All · Klasse 5–10", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
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
            chapters.forEachIndexed { i, c -> de.kurvenflug.app.ui.flight.ChapterCard(c, i, levelStars, onLevel) }
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
