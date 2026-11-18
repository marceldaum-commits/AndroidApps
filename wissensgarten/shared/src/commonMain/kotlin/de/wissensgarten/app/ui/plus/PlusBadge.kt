package de.wissensgarten.app.ui.plus

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.core.data.PlusStatus
import de.wissensgarten.core.model.License

/** Aktueller Plus-Stand für Garten-Kopf und Eltern-Bereich (null = unbekannt / nicht angeboten). */
val LocalPlusStatus = compositionLocalOf<PlusStatus?> { null }

/** Text des Abzeichens – null, wenn Plus nicht aktiv ist. */
fun plusBadgeText(s: PlusStatus?, compact: Boolean = false): String? = when {
    s == null || !s.active -> null
    s.product == License.PRODUCT_FOREVER -> if (compact) "🌟 Plus · für immer" else "🌟 Plus aktiv · für immer"
    s.until != null -> if (compact) "🌟 Plus · bis ${plusDate(s.until!!)}" else "🌟 Plus aktiv · 1 Jahr bis ${plusDate(s.until!!)}"
    s.trialUntil != null -> if (compact) "🌟 Plus-Test · bis ${plusDate(s.trialUntil!!)}" else "🌟 Plus-Test aktiv · bis ${plusDate(s.trialUntil!!)}"
    else -> "🌟 Plus aktiv"
}

/**
 * Gut sichtbares Abzeichen „Plus aktiv“ – goldene Pille mit wanderndem Glanz.
 * Erscheint nur, wenn Plus freigeschaltet (oder im Gratis-Test) ist.
 */
@Composable
fun PlusBadge(status: PlusStatus?, modifier: Modifier = Modifier, compact: Boolean = false, onClick: (() -> Unit)? = null) {
    val text = plusBadgeText(status, compact) ?: return
    val shine by rememberInfiniteTransition(label = "plus").animateFloat(
        -0.6f, 1.6f, infiniteRepeatable(tween(2600, delayMillis = 1800, easing = LinearEasing)), label = "glanz",
    )
    val gold = de.wissensgarten.app.ui.fx.SafeBrush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFC94D), Color(0xFFFFE082)))
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(gold)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = text.removePrefix("🌟 ") },
    ) {
        // Kompakt (Garten-Kopf): auf schmalen Handys lieber etwas kleiner als das Datum abschneiden
        val style = (if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
            .copy(fontWeight = FontWeight.Bold, color = Color(0xFF5D4300))
        androidx.compose.foundation.text.BasicText(
            text, Modifier.padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 3.dp else 5.dp),
            style = style, maxLines = if (compact) 1 else 2,
            overflow = if (compact) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            autoSize = if (compact) androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize) else null,
        )
        // Glanz-Streifen, der ab und zu über das Abzeichen huscht
        Box(Modifier.matchParentSize().background(de.wissensgarten.app.ui.fx.SafeBrush.linearGradient(
            0f to Color.Transparent, 0.45f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.55f), 0.55f to Color.Transparent, 1f to Color.Transparent,
            start = Offset(shine * 600f - 300f, 0f), end = Offset(shine * 600f, 120f),
        )))
    }
}
