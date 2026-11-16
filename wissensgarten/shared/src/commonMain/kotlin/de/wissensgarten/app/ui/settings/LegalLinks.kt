package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/** Impressum und Datenschutz auf der Webseite (Pflicht für WebApp und Store). */
@Composable
fun LegalLinks() {
    val uri = LocalUriHandler.current
    Row(horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = { runCatching { uri.openUri("https://www.mathenachhilfe-online.com/impressum/") } }) {
            Text("Impressum", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = { runCatching { uri.openUri(PrivacyText.WEB_URL) } }) {
            Text("Datenschutz", style = MaterialTheme.typography.labelMedium)
        }
    }
}
