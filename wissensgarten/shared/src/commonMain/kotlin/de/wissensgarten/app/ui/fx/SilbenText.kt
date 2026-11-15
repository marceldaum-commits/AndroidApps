package de.wissensgarten.app.ui.fx

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import de.wissensgarten.core.model.Syllables

/** Silben farbig anzeigen? (Einstellung „Silben farbig“, automatisch für Klasse 1–2) */
val LocalSyllables = staticCompositionLocalOf { false }

/** Vibration erlaubt? (Einstellung „Vibration“) */
val LocalVibration = staticCompositionLocalOf { true }

val SILBE_BLAU = Color(0xFF1565C0)
val SILBE_ROT = Color(0xFFD32F2F)
private val FOREIGN = setOf("englisch", "franzoesisch", "spanisch", "latein")

/** Silben abwechselnd blau und rot – wie in der Fibel. Nicht-Buchstaben bleiben in der normalen Farbe. */
fun silbenString(text: String): AnnotatedString = buildAnnotatedString {
    // Farbwechsel läuft über den ganzen Satz weiter (wie in der Fibel), nicht pro Wort neu
    var n = 0
    Syllables.segments(text).forEach { (part, idx) ->
        if (idx < 0) append(part)
        else withStyle(SpanStyle(color = if (n++ % 2 == 0) SILBE_BLAU else SILBE_ROT, fontWeight = FontWeight.SemiBold)) { append(part) }
    }
}

/** Text, der im Silben-Modus farbige Silben zeigt (nur bei deutschem Text). */
@Composable
fun SilbenText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
) {
    val on = LocalSyllables.current && LocalSubject.current !in FOREIGN
    if (!on) Text(text, modifier, color = color, style = style, textAlign = textAlign, fontWeight = fontWeight)
    else {
        val s = remember(text) { silbenString(text) }
        Text(s, modifier, color = color, style = style, textAlign = textAlign, fontWeight = fontWeight)
    }
}
