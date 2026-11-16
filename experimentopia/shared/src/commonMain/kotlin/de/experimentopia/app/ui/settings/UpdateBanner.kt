package de.experimentopia.app.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.fx.pulse
import de.experimentopia.app.ui.theme.E

/** Banner auf der Startseite: Blubb bringt ein Geschenk – die neue Version. */
@Composable
fun UpdateBanner(version: String, onClick: () -> Unit) {
    val wobble by rememberInfiniteTransition(label = "gift").animateFloat(-8f, 8f,
        infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w")
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().pulse(true, 0.02f),
        colors = CardDefaults.cardColors(containerColor = E.Sun.copy(alpha = 0.18f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎁", fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = wobble })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Neue Version $version ist da!", style = MaterialTheme.typography.titleMedium, color = E.Sun)
                Text("Antippen zum Aktualisieren – dein Fortschritt bleibt erhalten.", style = MaterialTheme.typography.bodySmall, color = E.Sun)
            }
            Text("›", fontSize = 28.sp, color = E.Sun)
        }
    }
}
