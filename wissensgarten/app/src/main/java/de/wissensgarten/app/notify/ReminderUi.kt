package de.wissensgarten.app.notify

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fragt bei Android 13+ nach der Benachrichtigungs-Erlaubnis; ruft [onResult] mit dem Ergebnis auf. */
@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult(it) }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onResult(true)
    }
}

