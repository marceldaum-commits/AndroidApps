package de.raumlicht.app.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Ab dieser Fensterbreite (in dp) wird die ganze Oberfläche am PC maßvoll größer gezeichnet – bei 1920 Pixeln Breite ×1,2 */
private const val BIG_FROM = 1500f
private const val HUGE_FROM = 1800f

/**
 * Breites Fenster (PC, Tablet quer): Bühne links groß, Bedienung rechts – statt alles in die Breite zu ziehen. Hochformat: Bühne oben, Bedienung darunter.
 * Dieselbe Aufteilung wie bei den Aufgaben ([de.raumlicht.app.ui.level.LevelScreen]).
 */
@Composable
fun ColumnScope.StageSplit(stage: @Composable (Modifier) -> Unit, panel: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        if (maxWidth > maxHeight * 1.15f) Row(Modifier.fillMaxSize()) {
            stage(Modifier.weight(1.25f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { panel() }
        } else Column(Modifier.fillMaxSize()) {
            stage(Modifier.fillMaxWidth().weight(1f))
            panel()
        }
    }
}

/** Faktor, mit dem die Oberfläche bei einer Fensterbreite von [widthDp] gezeichnet wird: kleine und mittlere Fenster ×1, große PC-Fenster ×1,15 bis ×1,25 */
fun bigScreenScale(widthDp: Float): Float = when { widthDp >= HUGE_FROM -> 1.25f; widthDp >= BIG_FROM -> 1.15f; else -> 1f }

/** Zeichnet den Inhalt auf großen PC-Fenstern etwas größer (Schrift, Regler, Karten), sonst unverändert – Layout und Bühne passen sich an */
@Composable
fun BigScreenScale(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val d = androidx.compose.ui.platform.LocalDensity.current
        val k = bigScreenScale(maxWidth.value)
        val scaled = remember(d.density, d.fontScale, k) { Density(d.density * k, d.fontScale) }
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides scaled) { Column(Modifier.fillMaxSize()) { content() } }
    }
}
