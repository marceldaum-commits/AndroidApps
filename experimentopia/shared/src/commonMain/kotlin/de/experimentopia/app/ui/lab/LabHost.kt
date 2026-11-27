package de.experimentopia.app.ui.lab

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import de.experimentopia.core.model.*

/**
 * Ein Labor-Gerät. [free] = Freies Labor (eigene Werte, kein Ziel). [onGoal] wird aufgerufen, sobald der Auftrag erfüllt ist
 * (darf mehrfach kommen – der Aufrufer merkt es sich).
 */
@Composable
fun LabView(setup: Setup, free: Boolean, onGoal: () -> Unit, modifier: Modifier = Modifier) {
    val goal by rememberUpdatedState(onGoal)
    Column(modifier) {
        when (setup) {
            is FloatSetup -> FloatLab(setup, free) { goal() }
            is TowerSetup -> TowerLab(setup, free) { goal() }
            is LeverSetup -> LeverLab(setup, free) { goal() }
            is PulleySetup -> PulleyLab(setup, free) { goal() }
            is RampSetup -> RampLab(setup, free) { goal() }
            is CircuitSetup -> CircuitLab(setup, free) { goal() }
            is MagnetSetup -> MagnetLab(setup, free) { goal() }
            is PhSetup -> PhLab(setup, free) { goal() }
            is TitrationSetup -> TitrationLab(setup, free) { goal() }
            is FizzSetup -> FizzLab(setup, free) { goal() }
            is ReactionSetup -> ReactionLab(setup, free) { goal() }
            is MeltSetup -> MeltLab(setup, free) { goal() }
            is HeatCurveSetup -> CurveLab(setup, free) { goal() }
            is MixSetup -> MixLab(setup, free) { goal() }
            is LightSetup -> LightLab(setup, free) { goal() }
            is PrismSetup -> PrismLab(free) { goal() }
            is ShadowSetup -> ShadowLab(setup, free) { goal() }
            is MirrorSetup -> MirrorLab(setup, free) { goal() }
            is RefractSetup -> RefractLab(setup, free) { goal() }
            is LensSetup -> LensLab(setup, free) { goal() }
            is PressureSetup -> PressureLab(setup, free) { goal() }
            is CoasterSetup -> CoasterLab(setup, free) { goal() }
            is CoilSetup -> CoilLab(setup, free) { goal() }
            is MotorSetup -> MotorLab(setup, free) { goal() }
            is DynamoSetup -> DynamoLab(setup, free) { goal() }
            is TrafoSetup -> TrafoLab(setup, free) { goal() }
            is SoundSetup -> SoundLab(setup, free) { goal() }
            is FallSetup -> FallLab(setup, free) { goal() }
            is PendulumSetup -> PendulumLab(setup, free) { goal() }
            is NewtonSetup -> NewtonLab(setup, free) { goal() }
            is GeneticsSetup -> GeneticsLab(setup, free) { goal() }
            is OsmosisSetup -> OsmosisLab(setup, free) { goal() }
            is FoodSetup -> FoodLab(setup, free) { goal() }
            is EnzymeSetup -> EnzymeLab(setup, free) { goal() }
            is PhotoSetup -> PhotoLab(setup, free) { goal() }
            is EcoSetup -> EcoLab(setup, free) { goal() }
            is SeparationSetup -> SeparationLab(setup, free) { goal() }
            is ChromaSetup -> ChromaLab(setup, free) { goal() }
            is PaintSetup -> PaintLab(setup, free) { goal() }
            is MicroscopeSetup -> MicroscopeLab(setup, free) { goal() }
            is GerminationSetup -> GerminationLab(setup, free) { goal() }
            is FoodChainSetup -> FoodChainLab(setup, free) { goal() }
            is PulseSetup -> PulseLab(setup, free) { goal() }
        }
    }
}

/** Emoji oder Text mittig an eine Stelle im Canvas malen */
fun DrawScope.label(tm: TextMeasurer, text: String, center: Offset, size: TextUnit = 24.sp, color: Color = Color.White, bold: Boolean = false) {
    val l = tm.measure(text, TextStyle(fontSize = size, color = color, fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null))
    drawText(l, topLeft = Offset(center.x - l.size.width / 2f, center.y - l.size.height / 2f))
}

/** Einmal melden, wenn eine Bedingung wahr wird */
@Composable
fun GoalWhen(condition: Boolean, onGoal: () -> Unit) {
    LaunchedEffect(condition) { if (condition) onGoal() }
}
