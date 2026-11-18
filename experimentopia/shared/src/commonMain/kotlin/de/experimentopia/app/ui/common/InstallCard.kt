package de.experimentopia.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.fx.GlowButton
import de.experimentopia.app.ui.fx.InstallState
import de.experimentopia.app.ui.fx.LocalInstaller
import de.experimentopia.app.ui.fx.pulse
import de.experimentopia.app.ui.theme.E

/** Gut sichtbarer Knopf „App installieren“ – nur, solange die WebApp noch nicht installiert ist. */
@Composable
fun InstallCard(modifier: Modifier = Modifier) {
    val installer = LocalInstaller.current ?: return
    val st = installer.state
    if (st == InstallState.HIDDEN) return
    var help by remember { mutableStateOf(false) }
    Panel(modifier.fillMaxWidth().testTag("installieren"), border = E.Mint) {
        GlowButton({ if (st == InstallState.PROMPT) installer.install() else help = !help }, Modifier.fillMaxWidth().pulse(true, 0.03f), color = E.Mint) {
            Text("📲 App installieren", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Text("Dann startet Experimentopia wie eine richtige App vom Startbildschirm – auch ohne Internet, ohne Werbung und ohne Anmeldung.", color = E.Muted, fontSize = 12.sp)
        AnimatedVisibility(st == InstallState.IOS && help) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("So geht's auf dem iPhone/iPad:", color = E.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("1. Unten (iPad: oben) auf „Teilen“ tippen – das Kästchen mit dem Pfeil ⬆️", color = E.Text, fontSize = 13.sp)
                Text("2. „Zum Home-Bildschirm“ wählen und „Hinzufügen“ tippen", color = E.Text, fontSize = 13.sp)
            }
        }
    }
}
