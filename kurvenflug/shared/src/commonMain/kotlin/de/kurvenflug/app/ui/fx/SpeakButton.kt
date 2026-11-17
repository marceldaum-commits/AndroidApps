package de.kurvenflug.app.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import de.kurvenflug.app.ui.theme.Q
import kotlinx.coroutines.launch

/** 🔊 zum Vorlesen-Lassen (nur sichtbar, wenn Vorlesen an ist). Hüpft beim Antippen. */
@Composable
fun SpeakButton(text: () -> String, modifier: Modifier = Modifier) {
    if (!LocalReadAloud.current) return
    val reader = LocalReader.current
    val me = remember { Any() }
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(modifier.size(36.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(Q.Cyan.copy(alpha = 0.16f))
        .border(1.dp, Q.Cyan.copy(alpha = 0.6f), CircleShape)
        .clickable { Voice.say(reader, text(), me); scope.launch { pop.snapTo(1.25f); pop.animateTo(1f, spring(dampingRatio = 0.35f)) } }
        .semantics { contentDescription = "Vorlesen" }, contentAlignment = Alignment.Center) {
        Text("🔊", fontSize = 16.sp)
    }
}
