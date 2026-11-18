package de.wissensgarten.app.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.AppHost
import de.wissensgarten.app.InstallMode
import de.wissensgarten.app.InstallOffer
import de.wissensgarten.app.ui.fx.Brummi
import de.wissensgarten.app.ui.fx.LocalSfx
import de.wissensgarten.app.ui.fx.Mood
import de.wissensgarten.app.ui.fx.Sound

private val Ink = Color(0xFF1B3A4B)
private val Sky = Color(0xFFE1F5FE)

/**
 * „App installieren“ (WebApp als PWA): im Garten als kleine Karte, in den Einstellungen als Abschnitt.
 * Erscheint nur, wenn der Browser es anbietet ([InstallMode.PROMPT]) oder auf iPhone/iPad ([InstallMode.IOS_GUIDE]).
 * Die Android-App liefert immer [InstallMode.NONE] – dort gibt es keinen Knopf.
 */
@Composable
fun InstallAppCard(host: AppHost, inGarden: Boolean, modifier: Modifier = Modifier) {
    val offer by host.installOffer.collectAsState()
    var guide by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val visible = offer.mode != InstallMode.NONE && !(inGarden && offer.later)
    if (visible) InstallAppCardContent(
        offer, compact = inGarden, modifier = modifier,
        onInstall = {
            sfx?.play(Sound.PLOP, pitch = 1.3f)
            if (offer.mode == InstallMode.IOS_GUIDE) guide = true else host.requestInstall()
        },
        onLater = if (inGarden) ({ sfx?.play(Sound.TAP); host.postponeInstall() }) else null,
    )
    if (guide) IosInstallGuide { guide = false }
}

/** Die Karte selbst (ohne Plattform) – auch für Bildschirmfotos. */
@Composable
fun InstallAppCardContent(
    offer: InstallOffer,
    compact: Boolean,
    onInstall: () -> Unit,
    onLater: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Das Handy hüpft sanft – Brummi „klopft an“, ohne zu drängeln
    val hop by rememberInfiniteTransition(label = "install").animateFloat(0f, 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hop")
    val title = "Hol den Garten auf den Startbildschirm!"
    val text = if (compact) "Dann startet er wie eine echte App – auch ohne Internet. 🐝"
    else "Brummi summt: „Mit eigenem Symbol findest du mich sofort!“ Der Garten startet dann wie eine echte App, " +
        "im ganzen Bildschirm und auch ohne Internet. Dein Fortschritt bleibt, wo er ist."
    val label = if (offer.mode == InstallMode.IOS_GUIDE) "So geht's" else "App installieren"
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Sky), shape = RoundedCornerShape(20.dp)) {
        Box {
            Column(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = if (onLater != null) 30.dp else 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📲", fontSize = if (compact) 30.sp else 38.sp,
                        modifier = Modifier.graphicsLayer { translationY = -hop * 6.dp.toPx(); rotationZ = (hop - 0.5f) * 10f })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.85f))
                    }
                    if (compact) {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onInstall, contentPadding = PaddingValues(horizontal = 14.dp)) { Text(if (offer.mode == InstallMode.IOS_GUIDE) "So geht's" else "Installieren") }
                    }
                }
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("📲 $label") }
                }
            }
            if (onLater != null) Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).clip(CircleShape)
                    .semantics { contentDescription = "Später" }
                    .background(Color.White.copy(alpha = 0.5f))
                    .clickable(onClick = onLater),
                contentAlignment = Alignment.Center,
            ) {
                // Selbst gezeichnet: das Zeichen „✕“ fehlt in der Web-Schrift
                Canvas(Modifier.size(10.dp)) {
                    val c = Ink.copy(alpha = 0.6f); val w = 1.6.dp.toPx()
                    drawLine(c, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
                    drawLine(c, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
                }
            }
        }
    }
}

/** iPhone/iPad: Safari kann nicht selbst fragen – Brummi zeigt die zwei Handgriffe. */
@Composable
fun IosInstallGuide(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Brummi(Mood.HAPPY, size = 64.dp) },
        title = { Text("So kommt der Garten auf dein iPhone oder iPad", textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GuideStep(1, { ShareSymbol(Modifier.size(30.dp)) }, "Tippe in Safari auf das Teilen-Symbol – unten in der Leiste (iPad: oben rechts).")
                GuideStep(2, { Text("➕", fontSize = 24.sp) }, "Wähle „Zum Home-Bildschirm“. Etwas nach unten wischen, falls du es nicht gleich siehst.")
                GuideStep(3, { Text("🌻", fontSize = 24.sp) }, "Tippe oben rechts auf „Hinzufügen“ – fertig! Jetzt wohnt der Garten auf deinem Startbildschirm.")
                Text("Brummi summt: „Bis gleich auf dem Startbildschirm!“ 🐝", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Alles klar!") } },
    )
}

@Composable
private fun GuideStep(n: Int, symbol: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("$n", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Sky), contentAlignment = Alignment.Center) { symbol() }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Das Teilen-Symbol von iOS (Kästchen mit Pfeil nach oben), selbst gezeichnet – so erkennt man es wieder. */
@Composable
fun ShareSymbol(modifier: Modifier = Modifier, color: Color = Color(0xFF007AFF)) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = w * 0.08f
        // Kästchen, oben offen
        val top = h * 0.38f
        val left = w * 0.22f; val right = w * 0.78f; val bottom = h * 0.92f
        drawLine(color, Offset(left + w * 0.12f, top), Offset(left, top), stroke, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(right, bottom), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right - w * 0.12f, top), stroke, StrokeCap.Round)
        // Pfeil nach oben
        drawLine(color, Offset(w / 2, h * 0.62f), Offset(w / 2, h * 0.08f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w / 2, h * 0.08f), Offset(w * 0.34f, h * 0.24f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w / 2, h * 0.08f), Offset(w * 0.66f, h * 0.24f), stroke, StrokeCap.Round)
    }
}
