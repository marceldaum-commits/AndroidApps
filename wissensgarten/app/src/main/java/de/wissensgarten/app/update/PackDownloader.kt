package de.wissensgarten.app.update

import android.content.Context
import de.wissensgarten.app.BuildConfig
import de.wissensgarten.core.data.AppContainer
import de.wissensgarten.core.data.PackUpdates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lädt neue Wissenspakete als reine JSON-Daten nach (kein Code). Es werden keine Daten über das Kind gesendet –
 * der Server sieht nur, wie bei jedem Webseitenaufruf, die IP-Adresse.
 */
object PackDownloader {
    suspend fun fetch(context: Context, known: Map<String, Int>, base: String = BuildConfig.PACKS_URL, license: String? = null): Int = withContext(Dispatchers.IO) {
        val dir = AppContainer.remotePackDir(context)
        PackUpdates(
            get = { name -> download(base + name, license) },
            save = { name, text ->
                val tmp = File(dir, "$name.tmp")
                tmp.writeText(text)
                tmp.renameTo(File(dir, name))
            },
        ).fetch(known)
    }

    private fun download(url: String, license: String? = null): String? = runCatching {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000; instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Wissensgarten")
            if (license != null) { setRequestProperty("X-Lizenz", license); useCaches = false }
        }
        try {
            if (c.responseCode != 200) return@runCatching null
            c.inputStream.use { s ->
                val bytes = s.readNBytesCompat(PackUpdates.MAX_BYTES + 1)
                if (bytes.size > PackUpdates.MAX_BYTES) null else bytes.decodeToString()
            }
        } finally { c.disconnect() }
    }.getOrNull()

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(16 * 1024); var total = 0
        while (total < max) { val n = read(buf, 0, minOf(buf.size, max - total)); if (n < 0) break; out.write(buf, 0, n); total += n }
        return out.toByteArray()
    }
}
