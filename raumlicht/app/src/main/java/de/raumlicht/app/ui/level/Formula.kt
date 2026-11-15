package de.raumlicht.app.ui.level

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Part

fun stageColor(name: String?): Color = when (name) {
    "cyan" -> Q.Cyan; "pink" -> Q.Pink; "yellow" -> Q.Yellow; "green" -> Q.Green; "orange" -> Q.Orange; "violet" -> Q.Violet
    else -> Q.Text
}

/** Formeln wie im Heft: Spaltenvektoren mit runden Klammern. Eingestellte Zahlen leuchten in der Farbe ihres Reglers und ploppen bei Änderung. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaView(lines: List<List<Part>>, colorOf: (String) -> Color, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                line.forEach { part ->
                    when (part) {
                        is Part.Txt -> Text(part.s, fontSize = 16.sp, color = Q.Text, fontWeight = FontWeight.Medium)
                        is Part.Num -> Live(part.s, part.control?.let(colorOf), 17)
                        is Part.Col -> ColumnVector(part.cells, part.control?.let(colorOf))
                    }
                }
            }
        }
    }
}

@Composable
private fun Live(text: String, color: Color?, size: Int) {
    if (color == null) { Text(text, fontSize = size.sp, color = Q.Text, fontWeight = FontWeight.Medium); return }
    AnimatedContent(text, transitionSpec = { (scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f), initialScale = 0.5f) + fadeIn()) togetherWith fadeOut() }, label = "zahl") { t ->
        Text(t, fontSize = size.sp, color = color, fontWeight = FontWeight.Black,
            modifier = Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp))
    }
}

@Composable
private fun ColumnVector(cells: List<String>, color: Color?) {
    val c = color ?: Q.Text
    Column(
        Modifier.padding(horizontal = 3.dp).drawBehind {
            val w = 5.dp.toPx(); val s = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(c, 110f, 140f, false, Offset(0f, 0f), Size(w * 2.4f, size.height), style = s)
            drawArc(c, -70f, 140f, false, Offset(size.width - w * 2.4f, 0f), Size(w * 2.4f, size.height), style = s)
        }.padding(horizontal = 9.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { cells.forEach { Live(it, color, 15) } }
}
