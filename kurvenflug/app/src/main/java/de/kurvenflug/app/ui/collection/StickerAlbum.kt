package de.kurvenflug.app.ui.collection

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.Starfield
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.Rarity
import de.kurvenflug.core.model.Sticker
import de.kurvenflug.core.model.Stickers
import kotlin.math.sin

fun Rarity.color() = when (this) { Rarity.COMMON -> Q.Cyan; Rarity.RARE -> Q.Violet; Rarity.EPIC -> Q.Yellow }

/** Sticker-Album: gesammelte Sticker, seltene leuchten, Glitzer-Varianten funkeln. */
@Composable
fun StickerAlbum(owned: Set<String>, onBack: () -> Unit) {
    val count = Stickers.all.count { it.id in owned || "${it.id}*" in owned }
    val glitter = Stickers.all.count { "${it.id}*" in owned }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 19)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("📒 Sticker · $count/${Stickers.all.size}", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Volt(VoltMood.HAPPY, size = 52.dp, speech = if (count == 0) "Schaff Level – manchmal gibt's danach einen Sticker als Überraschung! 🎁"
                    else "$count Sticker gesammelt${if (glitter > 0) ", davon $glitter mit Glitzer ✨" else ""}! Mit 3 ⭐ kommen sie öfter.")
                Rarity.entries.forEach { r ->
                    Text("${r.label} · ${Stickers.all.count { it.rarity == r && (it.id in owned || "${it.id}*" in owned) }}/${Stickers.all.count { it.rarity == r }}",
                        color = r.color(), fontWeight = FontWeight.Bold)
                    Stickers.all.filter { it.rarity == r }.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { s -> StickerTile(s, s.id in owned || "${s.id}*" in owned, "${s.id}*" in owned, Modifier.weight(1f)) }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerTile(s: Sticker, has: Boolean, glitter: Boolean, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "glitter").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val c = s.rarity.color()
    Column(
        modifier.aspectRatio(0.85f).clip(RoundedCornerShape(16.dp))
            .background(if (glitter) Brush.linearGradient(listOf(Q.Pink.copy(alpha = 0.4f), Q.Yellow.copy(alpha = 0.35f), Q.Cyan.copy(alpha = 0.4f)))
                else Brush.linearGradient(listOf(c.copy(alpha = if (has) 0.2f else 0.05f), c.copy(alpha = if (has) 0.1f else 0.03f))))
            .border(2.dp, if (has) c else c.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(if (has) s.emoji else "❔", fontSize = 30.sp,
            modifier = Modifier.graphicsLayer { if (glitter) { rotationZ = 6f * sin(t * 3f); scaleX = 1f + 0.06f * sin(t * 5f); scaleY = scaleX } })
        Text(if (has) s.name else "???", style = MaterialTheme.typography.labelSmall, color = if (has) Q.Text else Q.Muted,
            textAlign = TextAlign.Center, maxLines = 2)
        if (glitter) Text("✨", fontSize = 12.sp)
    }
}
