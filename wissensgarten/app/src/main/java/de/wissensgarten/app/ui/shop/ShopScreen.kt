package de.wissensgarten.app.ui.shop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.common.OwnBedForm
import de.wissensgarten.app.ui.common.PackBrowser
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.Pack

/** Samenhandlung: alle Wissenspakete durchstöbern und als Beet pflanzen. */
@Composable
fun ShopScreen(
    profile: ProfileEntity,
    packs: List<Pack>,
    beds: List<BedWithCards>,
    canUse: (Pack) -> Boolean,
    onPlant: (Pack) -> Unit,
    onOpenBed: (String) -> Unit,
    onOwnBed: (String, String) -> Unit,
) {
    val planted = beds.mapNotNull { b -> b.bed.packId?.let { it to b.bed.id } }.toMap()
    var showOwn by remember { mutableStateOf(false) }
    PackBrowser(
        packs = packs.filter(canUse),
        level = profile.level,
        isChosen = { it.id in planted },
        onToggle = { p -> planted[p.id]?.let(onOpenBed) ?: onPlant(p) },
        chosenLabel = "Im Garten",
        chooseLabel = "Pflanzen",
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        header = {
            item {
                Text("Samenhandlung", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${packs.size} Wissenspakete mit ${packs.sumOf { it.cards.size }} Karten. Alles, was dich interessiert – egal welche Klasse.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("✏️ Eigenes Beet", style = MaterialTheme.typography.titleMedium)
                        Text("Für deine Vokabeln, Lernzettel oder alles, was du dir merken willst.", style = MaterialTheme.typography.bodySmall)
                        if (showOwn) {
                            Spacer(Modifier.height(8.dp))
                            OwnBedForm { n, e -> onOwnBed(n, e); showOwn = false }
                        } else TextButton(onClick = { showOwn = true }) { Text("Eigenes Beet anlegen") }
                    }
                }
            }
        },
    )
}
