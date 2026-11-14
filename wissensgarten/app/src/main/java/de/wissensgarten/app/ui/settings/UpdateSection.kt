package de.wissensgarten.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.update.AppUpdater
import de.wissensgarten.core.model.AppRelease
import de.wissensgarten.core.model.Updates
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val r: AppRelease) : UpdateState
    data class Downloading(val r: AppRelease, val p: Float) : UpdateState
    data class Ready(val r: AppRelease, val apk: File) : UpdateState
    data class Failed(val msg: String) : UpdateState
}

/** Karte „App-Update“: nach neuer Version suchen, laden, installieren – mit Brummi und Fortschritt. */
@Composable
fun UpdateSection(installed: String, initial: UpdateState = UpdateState.Idle) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(initial) }
    // Nach „Installation erlauben“ in den Android-Einstellungen zurück: direkt weitermachen
    var waitingForPermission by remember { mutableStateOf(false) }
    LifecycleResumeEffect(waitingForPermission) {
        val s = state
        if (waitingForPermission && s is UpdateState.Ready && AppUpdater.canInstall(ctx)) {
            waitingForPermission = false; AppUpdater.install(ctx, s.apk)
        }
        onPauseOrDispose { }
    }

    fun check() {
        state = UpdateState.Checking
        scope.launch {
            AppUpdater.check().fold(
                onSuccess = { r ->
                    state = if (r != null && Updates.isNewer(r.version, installed)) { sfx?.play(Sound.FANFARE); UpdateState.Available(r) }
                    else { sfx?.play(Sound.CORRECT, volume = 0.6f); UpdateState.UpToDate }
                },
                onFailure = { state = UpdateState.Failed("Keine Verbindung zu GitHub. Bist du online?") },
            )
        }
    }

    fun download(r: AppRelease) {
        state = UpdateState.Downloading(r, 0f)
        scope.launch {
            AppUpdater.download(ctx, r) { p -> state = UpdateState.Downloading(r, p) }.fold(
                onSuccess = { f ->
                    sfx?.play(Sound.LEVEL_UP)
                    state = UpdateState.Ready(r, f)
                    if (AppUpdater.canInstall(ctx)) AppUpdater.install(ctx, f)
                    else { waitingForPermission = true; AppUpdater.openInstallPermission(ctx) }
                },
                onFailure = { state = UpdateState.Failed("Download abgebrochen. Probier es gleich nochmal.") },
            )
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("App-Update", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            AnimatedContent(state, contentKey = { it::class }, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() }, label = "upd") { s ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (s) {
                        UpdateState.Idle -> {
                            Text("Installiert: Version $installed", style = MaterialTheme.typography.bodyMedium)
                            JuicyButton(onClick = { check() }, modifier = Modifier.fillMaxWidth()) { Text("🔄 Nach neuer Version suchen") }
                        }
                        UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            val spin by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "s")
                            Text("🔄", fontSize = 26.sp, modifier = Modifier.graphicsLayer { rotationZ = spin })
                            Spacer(Modifier.width(10.dp))
                            Text("Brummi schaut nach, ob es etwas Neues gibt …")
                        }
                        UpdateState.UpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Brummi(Mood.HAPPY, size = 56.dp)
                                Text("Alles aktuell! Du hast die neueste Version ($installed).", modifier = Modifier.padding(start = 8.dp))
                            }
                            TextButton(onClick = { check() }) { Text("Nochmal prüfen") }
                        }
                        is UpdateState.Available -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Brummi(Mood.CHEER, size = 56.dp)
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text("Neue Version ${s.r.version} ist da! 🎉", fontWeight = FontWeight.Bold)
                                    Text("Installiert: $installed · ${"%.1f".format(s.r.sizeBytes / 1_048_576.0)} MB", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val notes = s.r.notes.lines().filterNot { it.startsWith("Installieren:") }.joinToString("\n").trim()
                            if (notes.isNotBlank()) Text(notes.take(400), style = MaterialTheme.typography.bodySmall)
                            JuicyButton(onClick = { download(s.r) }, modifier = Modifier.fillMaxWidth().height(52.dp).pulse(true, 0.03f)) {
                                Text("⬇️ Jetzt aktualisieren", fontWeight = FontWeight.Bold)
                            }
                            Text("Dein Garten und alle Profile bleiben erhalten.", style = MaterialTheme.typography.bodySmall)
                        }
                        is UpdateState.Downloading -> {
                            Text("Lade Version ${s.r.version} … ${(s.p * 100).toInt()} %")
                            val p by animateFloatAsState(s.p, label = "p")
                            Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape)) {
                                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxSize(), color = Color(0xFF4F9FD6))
                            }
                        }
                        is UpdateState.Ready -> {
                            Text("✅ Version ${s.r.version} ist geladen.", fontWeight = FontWeight.Bold)
                            if (!AppUpdater.canInstall(ctx)) Text("Einmalig nötig: In den Android-Einstellungen „Wissensgarten“ erlauben, Apps zu installieren – dann zurückkommen.",
                                style = MaterialTheme.typography.bodySmall)
                            JuicyButton(onClick = {
                                if (AppUpdater.canInstall(ctx)) AppUpdater.install(ctx, s.apk)
                                else { waitingForPermission = true; AppUpdater.openInstallPermission(ctx) }
                            }, modifier = Modifier.fillMaxWidth()) { Text("📲 Installieren") }
                        }
                        is UpdateState.Failed -> {
                            Text("😕 ${s.msg}")
                            TextButton(onClick = { check() }) { Text("Nochmal versuchen") }
                        }
                    }
                }
            }
        }
    }
}
