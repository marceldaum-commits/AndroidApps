package de.wissensgarten.app.ui.store

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.ui.garden.DewChip
import de.wissensgarten.core.data.GardenRepository.BuyResult
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.InventoryEntity
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.ItemKind
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.ShopItem
import de.wissensgarten.core.model.ShopItems
import de.wissensgarten.core.model.Stage
import kotlinx.coroutines.launch

private enum class StoreTab(val label: String) { PLANTS("🌸 Pflanzen"), DECO("🏡 Deko"), FRIENDS("🐾 Freunde"), PLUS("⭐ Plus") }

/**
 * Gartenladen: Tautropfen gegen Pflanzenarten und Deko tauschen.
 * Reiter „Wissen Plus“ ist der vorbereitete Platz für spätere kostenpflichtige Pakete.
 */
@Composable
fun StoreScreen(
    profile: ProfileEntity,
    inventory: List<InventoryEntity>,
    beds: List<BedWithCards>,
    plusPacks: List<Pack>,
    onBuy: (String, (BuyResult) -> Unit) -> Unit,
    onToggleDeco: (String, Boolean) -> Unit,
    onUseSpecies: (bedId: String, species: String) -> Unit,
    onOpenPacks: () -> Unit,
    initialTab: Int = 0,
    album: (@Composable () -> Unit)? = null,
) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val owned = inventory.associateBy { it.itemId }
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var plantFor by remember { mutableStateOf<ShopItem?>(null) }

    fun buy(item: ShopItem) {
        if (profile.dewdrops < item.price) {
            sfx?.play(Sound.WRONG)
            scope.launch { snackbar.showSnackbar("Dir fehlen noch ${item.price - profile.dewdrops} 💧 – gieß ein paar Pflanzen!") }
            return
        }
        onBuy(item.id) { r ->
            when (r) {
                BuyResult.OK -> {
                    sfx?.play(Sound.LEVEL_UP)
                    particles.burst(Burst.CONFETTI, count = 70)
                    particles.burst(Burst.SPARKLE, 0.5f, 0.4f, 30)
                    if (item.kind == ItemKind.SPECIES && beds.isNotEmpty()) plantFor = item
                    else scope.launch { snackbar.showSnackbar("${item.name} steht jetzt in deinem Garten! 🎉") }
                }
                BuyResult.TOO_EXPENSIVE -> sfx?.play(Sound.WRONG)
                else -> Unit
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                // Kopf mit Himmel-Farbverlauf
                Box(
                    Modifier.fillMaxWidth()
                        .background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFF8EC5EA), Color(0xFFFFE0B2))))
                        .statusBarsPadding().padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Gartenladen", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF14321A))
                            Text("Tausche Tautropfen gegen Schönes für deinen Garten.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF14321A))
                        }
                        DewChip(profile.dewdrops)
                    }
                }
                PrimaryTabRow(selectedTabIndex = tab) {
                    StoreTab.entries.forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { sfx?.play(Sound.TAP, volume = 0.5f); tab = i }, text = { Text(t.label, maxLines = 1) })
                    }
                }
                when (StoreTab.entries[tab]) {
                    StoreTab.PLANTS -> ItemGrid(ShopItems.species, owned, profile.dewdrops, ::buy, onToggleDeco)
                    StoreTab.DECO -> ItemGrid(ShopItems.decos, owned, profile.dewdrops, ::buy, onToggleDeco)
                    StoreTab.FRIENDS -> album?.invoke()
                    StoreTab.PLUS -> PlusTab(plusPacks, onOpenPacks)
                }
            }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }

    plantFor?.let { item ->
        AlertDialog(
            onDismissRequest = { plantFor = null },
            title = { Text("${item.name} gekauft! 🎉") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("In welchem Beet soll sie wachsen?")
                    beds.forEach { b ->
                        OutlinedButton(onClick = { onUseSpecies(b.bed.id, item.id); plantFor = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${b.bed.emoji} ${b.bed.name}", maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { plantFor = null }) { Text("Später") } },
        )
    }
}

@Composable
private fun ItemGrid(
    items: List<ShopItem>,
    owned: Map<String, InventoryEntity>,
    dew: Int,
    onBuy: (ShopItem) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    LazyVerticalGrid(
        GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.id }) { item -> ItemCard(item, owned[item.id], dew, onBuy, onToggle) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Tautropfen bekommst du fürs Gießen – mit Serien-Bonus sogar doppelt und dreifach. 🔥",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

@Composable
private fun ItemCard(item: ShopItem, inv: InventoryEntity?, dew: Int, onBuy: (ShopItem) -> Unit, onToggle: (String, Boolean) -> Unit) {
    val isOwned = inv != null || item.price == 0
    val wind = LocalWind.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOwned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium)
                    .background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFFCDEBFA), Color(0xFFE8F5E9)))),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (item.kind == ItemKind.SPECIES) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                        Plant(Stage.BLOOM, 0f, 330f, 3, Modifier.weight(1f).fillMaxHeight(), species = item.id)
                        Plant(Stage.TREE, 0f, 330f, 5, Modifier.weight(1f).fillMaxHeight(), species = item.id)
                    }
                } else {
                    Canvas(Modifier.fillMaxSize()) { drawDecoPreview(item.id, wind?.value ?: 0f) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                item.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            when {
                item.price == 0 -> AssistChip(onClick = {}, label = { Text("Standard") })
                inv != null && item.kind == ItemKind.DECO -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (inv.active) "Aufgestellt" else "Im Schuppen", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = inv.active, onCheckedChange = { onToggle(item.id, it) })
                }
                inv != null -> AssistChip(onClick = {}, label = { Text("✓ Gekauft") })
                else -> {
                    val affordable = dew >= item.price
                    JuicyButton(
                        onClick = { onBuy(item) },
                        sound = null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (affordable) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    ) { Text("💧 ${item.price}", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun PlusTab(plusPacks: List<Pack>, onOpenPacks: () -> Unit) {
    val gate = de.wissensgarten.app.ui.common.LocalParentGate.current
    var parentInfo by remember { mutableStateOf(false) }
    val shine by rememberInfiniteTransition(label = "plus").animateFloat(-6f, 6f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3C4))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 34.sp, modifier = Modifier.graphicsLayer { rotationZ = shine })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Wissensgarten Plus", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4E3B00))
                        Box(Modifier.clip(CircleShape).background(Color(0xFFFF8A65)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                            Text("KOMMT BALD", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
                Text("Bald gibt es hier zusätzliche Wissenspakete – für Prüfungen, Abitur und noch mehr Fächer. " +
                    "Bis dahin sind alle Plus-Pakete gratis. Probier sie aus! 🎁",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4E3B00))
                Text("Alle Grundpakete bleiben kostenlos – versprochen.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E3B00))
            }
        }
        // Nur die eigenen, schon freigeschalteten Plus-Pakete – kein Preis, kein Kaufhinweis im Kinderbereich (UWG Anh. Nr. 28)
        if (plusPacks.isNotEmpty()) Text("Deine Plus-Pakete", style = MaterialTheme.typography.titleMedium)
        plusPacks.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = onOpenPacks).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.title, style = MaterialTheme.typography.titleSmall)
                    Text("${p.cardCount} Karten", style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.clip(CircleShape).background(Color(0xFFFFD54F)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("PLUS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFF4E3B00))
                }
            }
        }
        OutlinedButton(onClick = onOpenPacks, modifier = Modifier.fillMaxWidth()) { Text("Alle Wissenspakete ansehen") }
        TextButton(onClick = { gate("Hier steht für Eltern, was Wissensgarten Plus ist.") { parentInfo = true } },
            modifier = Modifier.fillMaxWidth()) { Text("👨‍👩‍👧 Für Eltern: Was ist Plus?") }
    }
    if (parentInfo) AlertDialog(
        onDismissRequest = { parentInfo = false },
        title = { Text("Wissensgarten Plus – für Eltern") },
        text = {
            Text("• Plus ist ein freiwilliger Zusatz mit weiteren Wissenspaketen, vor allem für die Oberstufe und Experten.\n" +
                "• Alle Grundpakete bleiben kostenlos.\n" +
                "• Kaufen geht nur im Elternbereich hinter der Eltern-Aufgabe, über den Shop auf mathenachhilfe-online.com. Der Freischalt-Code kommt per E-Mail.\n" +
                "• Keine Werbung, keine Lockangebote, kein Kaufdruck auf Kinder. Brummi bettelt nie.\n" +
                "• Ein Kauf gilt für die ganze Familie auf allen Geräten in der Web-App – kein Konto nötig.")
        },
        confirmButton = { TextButton(onClick = { parentInfo = false }) { Text("Verstanden") } },
    )
}

