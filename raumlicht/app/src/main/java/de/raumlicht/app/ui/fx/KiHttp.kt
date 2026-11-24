package de.raumlicht.app.ui.fx

import de.raumlicht.core.model.Ki
import de.raumlicht.core.model.Vorlesen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Verbindung zum KI-Tutor auf unserem Trainer-Server: eine Anfrage als JSON, zurück kommen Status (0 = keine Verbindung) und Antworttext. */
object KiHttp {
    suspend fun post(body: String, url: String = Ki.URL): Pair<Int, String> = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.requestMethod = "POST"; c.connectTimeout = 10_000; c.readTimeout = 45_000; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                // der Dienst lässt nur Anfragen mit der Herkunft unserer App zu
                c.setRequestProperty("Origin", Ki.APP_ORIGIN)
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = c.responseCode
                status to ((if (status in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "")
            } finally { c.disconnect() }
        }.getOrDefault(0 to "")
    }
}
