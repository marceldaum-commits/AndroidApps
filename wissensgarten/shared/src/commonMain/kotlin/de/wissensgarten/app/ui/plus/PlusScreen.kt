package de.wissensgarten.app.ui.plus

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.core.data.PlusEntitlements
import de.wissensgarten.core.data.PlusStatus
import de.wissensgarten.core.model.License

/** „19.11.2027“ aus Tagen seit 1970 */
fun plusDate(day: Long): String {
    val d = kotlinx.datetime.LocalDate.fromEpochDays(day.toInt())
    return "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthNumber.toString().padStart(2, '0')}.${d.year}"
}

/** Wissensgarten Plus – im Eltern-Bereich (hinter dem Eltern-Tor). Kein Druck, keine Countdowns, kein Abo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusScreen(
    status: PlusStatus,
    today: Long,
    purchaseAllowed: Boolean,
    checkoutYear: String,
    checkoutForever: String,
    onTrial: () -> Boolean,
    onRedeem: (String) -> PlusEntitlements.Redeem,
    onBack: () -> Unit,
) {
    val uri = LocalUriHandler.current
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val twinkle = rememberInfiniteTransition(label = "plus")
    val glow by twinkle.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
    val spin by twinkle.animateFloat(-8f, 8f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "spin")

    Scaffold(topBar = { TopAppBar(title = { Text("Wissensgarten Plus") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("🐝🌟", fontSize = 64.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = glow; scaleY = glow; rotationZ = spin })

            // Status
            Surface(shape = RoundedCornerShape(18.dp), color = if (status.active) Color(0xFFFFF3C4) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(when {
                        status.active && status.product == License.PRODUCT_FOREVER -> "🌟 Plus ist aktiv – für immer"
                        status.active && status.until != null -> "🌟 Plus ist aktiv bis ${plusDate(status.until!!)}"
                        status.active -> "🌟 Plus-Test läuft noch ${(status.trialUntil!! - today + 1).coerceAtLeast(1)} Tag(e)"
                        else -> "Wissensgarten (frei)"
                    }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Frei für immer: alle Wissenspakete von Klasse 1 bis 10 und alle Spielarten – ohne Werbung.", style = MaterialTheme.typography.bodySmall)
                    if (!status.enforced) Text("Im Moment ist noch alles frei nutzbar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("Das bringt Plus", style = MaterialTheme.typography.titleMedium)
            listOf(
                "🎓" to "Abi- und Experten-Pakete: Stochastik, Organische Chemie, Neurobiologie, Geschichte, Naturwissenschaft, Sprache …",
                "📦" to "Jeden Monat neue Plus-Pakete – kommen von allein in die Web-App.",
                "👪" to "Bis zu 6 Profile für die ganze Familie (frei: 2).",
                "📊" to "Wochenbericht für Eltern: Was wurde gelernt, was sitzt, wo hakt es?",
            ).forEach { (e, t) -> Row(verticalAlignment = Alignment.Top) { Text(e, fontSize = 22.sp); Spacer(Modifier.width(10.dp)); Text(t, style = MaterialTheme.typography.bodyMedium) } }

            if (!status.active && !status.trialUsed) OutlinedButton(
                onClick = { message = if (onTrial()) "🎉 7 Tage Plus: bis zu 6 Profile und der Wochenbericht sind frei. Die Abi- und Experten-Pakete kommen nach dem Kauf mit dem Code. Der Test endet von allein." else null },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🎁 7 Tage gratis testen (Profile + Wochenbericht)") }

            if (purchaseAllowed && status.product != License.PRODUCT_FOREVER) {
                if (checkoutYear.isNotBlank()) Button(onClick = { uri.openUri(checkoutYear) }, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Plus für 1 Jahr – 19,99 €", fontWeight = FontWeight.Bold); Text("inkl. MwSt. · kein Abo, verlängert sich nicht", style = MaterialTheme.typography.labelSmall) }
                }
                if (checkoutForever.isNotBlank()) Button(onClick = { uri.openUri(checkoutForever) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8860B))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Plus für immer – 49,99 €", fontWeight = FontWeight.Bold); Text("inkl. MwSt. · einmal zahlen", style = MaterialTheme.typography.labelSmall) }
                }
            }
            if (!purchaseAllowed) Text("In dieser Version kann Plus noch nicht gekauft werden – bis dahin ist alles frei.", style = MaterialTheme.typography.bodySmall)

            if (purchaseAllowed) {
                HorizontalDivider()
                Text("Code eingeben", style = MaterialTheme.typography.titleMedium)
                Text("Nach dem Kauf kommt der Code per E-Mail (dazu die Rechnung). Er gilt auf allen Geräten der Familie in der Web-App – einfach den Link in der E-Mail öffnen oder hier einfügen.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(code, { code = it; message = null }, label = { Text("WG-…") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = {
                    message = when (onRedeem(code)) {
                        PlusEntitlements.Redeem.OK -> "🌟 Plus ist freigeschaltet – vielen Dank!"
                        PlusEntitlements.Redeem.EXPIRED -> "Dieser Code ist abgelaufen."
                        PlusEntitlements.Redeem.INVALID -> "Das ist kein gültiger Plus-Code. Bitte genau so einfügen, wie er in der E-Mail steht."
                    }
                }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Freischalten") }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
