package de.kurvenflug.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.AppRelease
import de.kurvenflug.core.model.Updates
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val r: AppRelease) : UpdateState
    data class Downloading(val r: AppRelease, val p: Float) : UpdateState
    data class Ready(val r: AppRelease, val apk: String) : UpdateState
    data class Failed(val msg: String) : UpdateState
}

/** Banner auf der Startseite: Volt bringt ein Geschenk – die neue Version. */
@Composable
fun UpdateBanner(version: String, onClick: () -> Unit) {
    val wobble by rememberInfiniteTransition(label = "gift").animateFloat(-8f, 8f,
        infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w")
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().pulse(true, 0.02f),
        colors = CardDefaults.cardColors(containerColor = Q.Yellow.copy(alpha = 0.18f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎁", fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = wobble })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Neue Version $version ist da!", style = MaterialTheme.typography.titleMedium, color = Q.Yellow)
                Text("Antippen zum Aktualisieren – dein Fortschritt bleibt erhalten.", style = MaterialTheme.typography.bodySmall, color = Q.Yellow)
            }
            Text("›", fontSize = 28.sp, color = Q.Yellow)
        }
    }
}

/** Update-Bereich in den Einstellungen – nur auf Android (der Browser lädt neue Versionen von selbst). */
@Composable
expect fun PlatformUpdateSection(installed: String, initial: UpdateState = UpdateState.Idle, autoCheck: Boolean = true, onAutoCheck: ((Boolean) -> Unit)? = null)
