package de.raumlicht.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.home.Bar
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Pro

/**
 * Hinweis-Seite, wenn etwas zum Abi-Pass gehört: freundlich, ohne Druck, mit klarer Liste, was dazugehört und was frei bleibt.
 * Kaufen kann man hier noch nicht – die Seite erscheint nur, wenn Pro eingeschaltet ist ([Pro.ENABLED]).
 */
@Composable
fun ProScreen(what: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Abi-Pass", onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Lux(LuxMood.HAPPY, size = 64.dp)
                Text("$what gehört zum Abi-Pass. Damit schaltest du alles frei – einmal, ohne Abo.", Modifier.padding(start = 8.dp).testTag("pro-grund")
                    .background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Yellow.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Das bringt der Abi-Pass", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Pro.benefits.forEach { Text(it, color = Q.Text, fontSize = 14.sp) }
            }
            Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(18.dp)).border(1.dp, Q.Green.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Das bleibt immer frei", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Pro.free.forEach { Text("✓ $it", color = Q.Muted, fontSize = 13.sp) }
            }
            Text("Der Abi-Pass ist noch nicht erhältlich.", Modifier.testTag("pro-status"), color = Q.Muted, fontSize = 13.sp)
            NeonButton(onBack, Modifier.fillMaxWidth().testTag("pro-zurueck"), color = Q.Cyan) { Text("Zurück", fontWeight = FontWeight.Bold) }
        }
    }
}
