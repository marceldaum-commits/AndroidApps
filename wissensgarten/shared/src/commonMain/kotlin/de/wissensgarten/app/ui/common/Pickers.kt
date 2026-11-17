package de.wissensgarten.app.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.core.model.Levels
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.Subject

val AVATARS = listOf("🦊", "🐻", "🐼", "🐸", "🦉", "🐢", "🦄", "🐝", "🐱", "🐶", "🐰", "🦁", "🐧", "🐙", "🌵", "🌸")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AVATARS.forEach { a ->
            val sel = a == selected
            Box(
                Modifier.size(56.dp).clip(CircleShape)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                    .border(if (sel) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onSelect(a) },
                contentAlignment = Alignment.Center,
            ) { Text(a, fontSize = 28.sp) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(
            "Grundschule" to (1..4), "Klasse 5–10" to (5..10), "Oberstufe" to (11..13),
        ).forEach { (label, range) ->
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                range.forEach { l -> LevelChip("$l", l == selected) { onSelect(l) } }
            }
        }
        Text("Sonst", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelChip("Erwachsen / Studium / Beruf", selected == Levels.ADULT) { onSelect(Levels.ADULT) }
        }
    }
}

@Composable
private fun LevelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) },
    )
}

/** Filter für Paketlisten. */
enum class PackFilter(val label: String) { RECOMMENDED("Passend"), ALL("Alle"), GS("Grundschule"), MS("Klasse 5–10"), OS("Oberstufe"), ADULT("Erwachsene") }

fun Pack.matches(f: PackFilter, level: Int): Boolean = when (f) {
    PackFilter.RECOMMENDED -> Levels.fit(this, level) >= 2
    PackFilter.ALL -> true
    PackFilter.GS -> grades.any { it in 1..4 }
    PackFilter.MS -> grades.any { it in 5..10 }
    PackFilter.OS -> grades.any { it in 11..13 }
    PackFilter.ADULT -> Levels.ADULT in grades
}

/**
 * Durchsuchbare Paketliste mit Stufen- und Fach-Filter. Wird in der Einrichtung (Auswahl)
 * und in der Samenhandlung (Pflanzen) verwendet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackBrowser(
    packs: List<Pack>,
    level: Int,
    isChosen: (Pack) -> Boolean,
    onToggle: (Pack) -> Unit,
    chosenLabel: String,
    chooseLabel: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: LazyListScope.() -> Unit = {},
    isLocked: (Pack) -> Boolean = { false },
) {
    var filter by remember { mutableStateOf(if (level == 0) PackFilter.ALL else PackFilter.RECOMMENDED) }
    var subject by remember { mutableStateOf<Subject?>(null) }
    val byLevel = packs.filter { it.matches(filter, level) }
    val subjects = byLevel.map { it.subjectInfo }.distinct().sortedBy { it.ordinal }
    val shown = byLevel.filter { subject == null || it.subjectInfo == subject }
        .sortedWith(compareByDescending<Pack> { Levels.fit(it, level) }.thenBy { it.subjectInfo.ordinal }.thenBy { it.grades.min() })

    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        header()
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 6.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PackFilter.entries.filter { level != 0 || it != PackFilter.RECOMMENDED }) { f ->
                        FilterChip(selected = f == filter, onClick = { filter = f; subject = null }, label = { Text(f.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = subject == null, onClick = { subject = null }, label = { Text("Alle Fächer") }) }
                    items(subjects) { s ->
                        FilterChip(selected = s == subject, onClick = { subject = if (subject == s) null else s }, label = { Text("${s.emoji} ${s.label}") })
                    }
                }
            }
        }
        if (shown.isEmpty()) item {
            Text("Hier gibt es noch keine Pakete. Wähle oben einen anderen Filter.", Modifier.padding(8.dp))
        }
        items(shown, key = { it.id }) { p -> PackRow(p, isChosen(p), chosenLabel, if (isLocked(p)) "🌟 Plus" else chooseLabel) { onToggle(p) } }
    }
}

@Composable
fun PackRow(pack: Pack, chosen: Boolean, chosenLabel: String, chooseLabel: String, onToggle0: () -> Unit) {
    val sfx = de.wissensgarten.app.ui.fx.LocalSfx.current
    val onToggle = { sfx?.play(if (chosen) de.wissensgarten.app.ui.fx.Sound.TAP else de.wissensgarten.app.ui.fx.Sound.PLOP); onToggle0() }
    val container = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) { Text(pack.emoji, fontSize = 26.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // zweizeilig: auf schmalen Handys sonst nur „Richtig schrei…“
                Text(pack.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${pack.subjectInfo.label} · ${Levels.range(pack.grades)} · ${pack.cardCount} Karten",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (pack.description.isNotBlank()) Text(
                    pack.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (chosen) {
                AssistChip(onClick = onToggle, label = { Text(chosenLabel) }, leadingIcon = { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) })
            } else {
                FilledTonalButton(onClick = onToggle, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(chooseLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Eingabe für ein eigenes, leeres Beet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnBedForm(onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✏️") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text("Eigenes Beet, z. B. „Vokabeln Unit 3“") },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            de.wissensgarten.core.data.GardenRepository.OWN_BED_EMOJIS.forEach { e ->
                FilterChip(selected = e == emoji, onClick = { emoji = e }, label = { Text(e) })
            }
        }
        Button(onClick = { onAdd(name, emoji); name = "" }, enabled = name.isNotBlank()) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Beet anlegen")
        }
    }
}
