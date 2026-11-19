package de.kurvenflug.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.LocalPlatform
import de.kurvenflug.app.ui.fx.LocalSfx
import de.kurvenflug.app.ui.fx.Sound
import de.kurvenflug.app.ui.fx.pressBounce
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.NextSteps

/** „Wie geht's weiter?“: Ende von Klasse 10 – Volt zeigt die Lern-Apps für die Oberstufe (nur Links, kein Kauf). */
@Composable
fun NextStepsContent(modifier: Modifier = Modifier) {
    val platform = LocalPlatform.current
    val sfx = LocalSfx.current
    val glow by rememberInfiniteTransition(label = "weiter").animateFloat(0.45f, 0.95f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎓", fontSize = 26.sp)
            Text(NextSteps.TITLE, fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Volt(VoltMood.WOW, size = 52.dp, speech = NextSteps.INTRO)
        NextSteps.apps.forEach { app ->
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).pressBounce(interaction).clip(RoundedCornerShape(18.dp)).background(Q.Panel)
                    .border(2.dp, Q.Violet.copy(alpha = glow), RoundedCornerShape(18.dp))
                    .clickable(interaction, null) { sfx?.play(Sound.TAP); platform.openUrl(app.url) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(app.emoji, fontSize = 30.sp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(app.name, fontWeight = FontWeight.Black, color = Q.Text)
                    Text(app.what, color = Q.Cyan, style = MaterialTheme.typography.labelSmall)
                    Text(app.topics, color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                }
                Text("›", fontSize = 26.sp, color = Q.Violet, fontWeight = FontWeight.Black, modifier = Modifier.graphicsLayer { alpha = glow })
            }
        }
    }
}

/** Als Karte am Ende der Galaxie */
@Composable
fun NextStepsCard() {
    NextStepsContent(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(22.dp))
        .background(Color(0x22B388FF)).border(1.5.dp, Q.Violet.copy(alpha = 0.5f), RoundedCornerShape(22.dp)).padding(14.dp))
}
