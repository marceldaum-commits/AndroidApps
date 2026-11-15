package de.wissensgarten.app.ui.garden

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.pulse

/** Eine Kachel in „Mehr entdecken“. */
data class DiscoverItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val badge: String? = null,
    val onClick: () -> Unit,
)

/**
 * „Mehr entdecken“: Geschichte, Radio, Safari, Mischrunde … als waagrechte Kachelreihe.
 * So bleibt „Gießen“ oben gut sichtbar, und trotzdem ist alles mit einem Wisch erreichbar.
 */
@Composable
fun DiscoverRow(items: List<DiscoverItem>) {
    if (items.isEmpty()) return
    Column {
        Text("✨ Mehr entdecken", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { it ->
                Card(
                    onClick = it.onClick,
                    modifier = Modifier.width(150.dp).height(132.dp).then(if (it.badge != null) Modifier.pulse(true, 0.03f) else Modifier)
                        .semantics { contentDescription = "${it.title}: ${it.subtitle}" },
                    colors = CardDefaults.cardColors(containerColor = it.color),
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(it.emoji, fontSize = 30.sp)
                            Spacer(Modifier.weight(1f))
                            it.badge?.let { b ->
                                Surface(color = Color(0xFFFF7043), shape = CircleShape) {
                                    Text(b, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(it.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, color = Color(0xFF22261F))
                        Text(it.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = Color(0xFF3E3A30))
                    }
                }
            }
        }
    }
}
