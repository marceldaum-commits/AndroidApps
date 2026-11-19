package de.wissensgarten.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.core.model.Angebot

/**
 * Hinweis: Sprachen sind zu Quasselquest umgezogen (Marcel, 19.11.2026).
 * @param beete Anzahl ausgeblendeter Sprach-Beete des Kindes (0 = allgemeiner Hinweis, z. B. in der Samenhandlung)
 */
@Composable
fun QuasselquestHinweis(beete: Int, modifier: Modifier = Modifier.padding(horizontal = 16.dp)) {
    val uri = LocalUriHandler.current
    val test = de.wissensgarten.app.ui.fx.LocalHost.current?.testStage ?: false
    val link = remember(test) { Angebot.quasselquestLink(test) }
    val wave by rememberInfiniteTransition(label = "qq").animateFloat(-8f, 8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "w")
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE3F2FD), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🗣️", fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = wave })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sprachen sind umgezogen!", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                Text(
                    if (beete > 0) "Deine ${if (beete == 1) "Sprach-Beet ist" else "$beete Sprach-Beete sind"} jetzt in Quasselquest – dort sprichst, hörst und spielst du Englisch, Deutsch und mehr. Brummi passt hier auf alle anderen Fächer auf. 🐝"
                    else "Englisch, Französisch, Latein, Spanisch und Rechtschreibung & Grammatik lernst du jetzt in Quasselquest.",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF263238),
                )
                FilledTonalButton(onClick = { runCatching { uri.openUri(link) } }) { Text("Zu Quasselquest") }
            }
        }
    }
}
