package de.wissensgarten.app.ui.fx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.AppHost

/**
 * Dezent über allem: Holt Brummi seine Stimme gerade (länger als ≈ 0,4 s), erscheint oben eine kleine Sprechblase „🎧 …“.
 * Springt einmal je Sitzung die Gerätestimme ein, weil Nova nicht erreichbar ist, kommt ein kurzer Hinweis.
 */
@Composable
fun NovaLadehinweis(voice: Voice, host: AppHost) {
    val laedt by voice.laedt.collectAsState()
    val hinweis by voice.hinweis.collectAsState()
    LaunchedEffect(hinweis) { hinweis?.let { host.toast(it) } }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(laedt, enter = fadeIn() + scaleIn(initialScale = 0.7f), exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
            Surface(
                color = Color.White.copy(alpha = 0.92f), shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp,
                modifier = Modifier.semantics { contentDescription = "Brummis Stimme lädt" },
            ) {
                Text("🎧 …", fontSize = 15.sp, color = Color(0xFF22261F), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }
}
