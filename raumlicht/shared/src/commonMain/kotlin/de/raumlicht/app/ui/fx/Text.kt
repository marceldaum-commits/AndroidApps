package de.raumlicht.app.ui.fx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Zahl mit passendem Wort: `count(1, "Aufgabe", "Aufgaben")` → „1 Aufgabe“, sonst „n Aufgaben“. */
fun count(n: Int, one: String, many: String) = "$n " + if (n == 1) one else many

/**
 * Einzeilige Schrift, die sich selbst verkleinert, bis sie in den Platz passt (schmale Handys, 320 px) – statt abgeschnitten zu werden oder mitten im Wort umzubrechen.
 * @param min kleiner wird sie nicht; reicht auch das nicht, endet der Text mit „…“
 */
@androidx.compose.runtime.Composable
fun FitText(text: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier, fontSize: androidx.compose.ui.unit.TextUnit, min: Float = 8f,
            color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
            textAlign: androidx.compose.ui.text.style.TextAlign? = null, style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current) {
    var size by androidx.compose.runtime.remember(text, fontSize) { androidx.compose.runtime.mutableFloatStateOf(fontSize.value) }
    androidx.compose.material3.Text(text, modifier, color = color, fontSize = androidx.compose.ui.unit.TextUnit(size, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = fontWeight, textAlign = textAlign, style = style,
        maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow && size > min) size = maxOf(min, size - 1f) })
}
