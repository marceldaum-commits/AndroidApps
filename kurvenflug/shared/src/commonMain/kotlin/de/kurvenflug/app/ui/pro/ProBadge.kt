package de.kurvenflug.app.ui.pro

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ProState
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.CalDate

/** Art der Freischaltung, für Eltern lesbar – z. B. „Kauf über Google Play“ oder „Schullizenz bis 31.7.2027“. */
fun ProState.kindText(web: Boolean = false): String {
    val until = license?.until?.let { CalDate.ofEpochDay(it) }
    return when {
        viaPlay -> "Einmalkauf über Google Play – unbegrenzt"
        until != null -> "Schullizenz – gültig bis ${until.dayOfMonth}.${until.monthValue}.${until.year}"
        license != null -> if (web) "Einmalkauf (Code) – gilt in der Web-App, unbegrenzt" else "Einmalkauf über die Website (Code) – unbegrenzt"
        else -> ""
    }
}

/** Gut sichtbares Abzeichen „⭐ Pro aktiv“ (nur wenn Pro freigeschaltet ist) – goldener Glanz, der sanft pulsiert. */
@Composable
fun ProBadge(state: ProState, modifier: Modifier = Modifier) {
    if (!state.pro) return
    val web = de.kurvenflug.app.LocalPlatform.current.isWeb
    val glow by rememberInfiniteTransition(label = "pro").animateFloat(0.85f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val gold = Color(0xFFFFD54A)
    Text("⭐ Pro aktiv", color = Color(0xFF2A1E00), fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, softWrap = false,
        modifier = modifier.graphicsLayer { scaleX = glow; scaleY = glow; alpha = 0.75f + 0.25f * glow }
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(gold, Color(0xFFFFB300), gold)))
            .border(1.dp, Color(0xFFFFF1B0), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = "Kurvenflug Pro ist aktiv. " + state.kindText(web) })
}
