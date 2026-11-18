package de.kurvenflug.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.LocalSfx
import de.kurvenflug.app.ui.fx.Sound
import de.kurvenflug.app.ui.theme.Q
import kotlinx.coroutines.delay

/** Auf der Startseite „Später“ getippt: in dieser Sitzung nicht mehr zeigen (in den Einstellungen bleibt der Knopf) */
private var laterThisSession by mutableStateOf(false)

/**
 * „App installieren“ – nur in der WebApp und nur, solange sie nicht installiert ist ([de.kurvenflug.app.Platform.installMode]).
 * Der Browser meldet die Möglichkeit oft erst nach ein paar Sekunden – darum wird regelmäßig nachgesehen.
 */
@Composable
fun InstallOffer(onHome: Boolean, modifier: Modifier = Modifier) {
    val platform = de.kurvenflug.app.LocalPlatform.current
    if (!platform.isWeb) return
    var mode by remember { mutableStateOf(platform.installMode()) }
    LaunchedEffect(Unit) { while (true) { delay(1500); mode = platform.installMode() } }
    AnimatedVisibility(mode.isNotEmpty() && !(onHome && laterThisSession), modifier, enter = fadeIn() + expandVertically()) {
        InstallCard(mode, onInstall = { platform.install(); mode = platform.installMode() }, onLater = if (onHome) ({ laterThisSession = true }) else null)
    }
}

/** Die Karte selbst (ohne Browser-Abfrage, für Bildschirmfotos): [mode] "prompt" oder "ios" */
@Composable
fun InstallCard(mode: String, onInstall: () -> Unit, onLater: (() -> Unit)? = null, showGuide: Boolean = false) {
    val sfx = LocalSfx.current
    var guide by remember { mutableStateOf(showGuide) }
    val bob by rememberInfiniteTransition(label = "inst").animateFloat(-4f, 4f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Q.Cyan.copy(alpha = 0.28f), Q.Panel)))
            .border(2.dp, Q.Cyan, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📲", fontSize = 34.sp, modifier = Modifier.graphicsLayer { translationY = bob * density })
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Kurvenflug als App", fontWeight = FontWeight.Bold, color = Q.Text)
                Text("Mit eigenem Symbol auf dem Startbildschirm, ohne Browser-Leiste – startet auch ohne Internet.",
                    style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                sfx?.play(Sound.TAP)
                if (mode == "ios") guide = !guide else onInstall()
            }, colors = ButtonDefaults.buttonColors(containerColor = Q.Cyan, contentColor = Q.Space), modifier = Modifier.weight(1f)) {
                Text("App installieren", fontWeight = FontWeight.Bold)
            }
            if (onLater != null) TextButton(onClick = { sfx?.play(Sound.TICK); onLater() }) { Text("Später", color = Q.Muted) }
        }
        AnimatedVisibility(guide && mode == "ios") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Q.Space.copy(alpha = 0.6f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("So geht's auf iPhone und iPad:", fontWeight = FontWeight.Bold, color = Q.Yellow)
                Text("1. Unten auf „Teilen“ tippen (Kästchen mit Pfeil nach oben, auf dem iPad oben rechts).", color = Q.Text, style = MaterialTheme.typography.bodySmall)
                Text("2. „Zum Home-Bildschirm“ wählen (eventuell etwas nach unten wischen).", color = Q.Text, style = MaterialTheme.typography.bodySmall)
                Text("3. „Hinzufügen“ tippen – fertig! Kurvenflug startet dann vom Home-Bildschirm.", color = Q.Text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
