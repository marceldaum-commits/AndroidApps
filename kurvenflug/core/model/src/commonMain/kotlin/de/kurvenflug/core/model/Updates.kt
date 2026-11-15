package de.kurvenflug.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Eine veröffentlichte Version auf GitHub (Releases), mit Download-Adresse der APK. */
data class AppRelease(val version: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

/**
 * App-Updates aus GitHub Releases – für Installationen außerhalb des Play Stores.
 * Tags heißen „kurvenflug-v0.1.0“, die APK „Kurvenflug-0.1.0.apk“.
 */
object Updates {
    const val REPO = "marceldaum-commits/AndroidApps"
    const val API = "https://api.github.com/repos/$REPO/releases?per_page=30"
    const val TAG_PREFIX = "kurvenflug-v"

    @Serializable private data class Asset(val name: String, @SerialName("browser_download_url") val url: String, val size: Long = 0)
    @Serializable private data class Release(
        @SerialName("tag_name") val tag: String,
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<Asset> = emptyList(),
    )
    private val json = Json { ignoreUnknownKeys = true }

    /** Neueste Kurvenflug-Version aus der GitHub-Antwort (andere Apps im selben Repo werden ignoriert). */
    fun latest(apiResponse: String): AppRelease? =
        json.decodeFromString<List<Release>>(apiResponse)
            .filter { !it.draft && !it.prerelease && it.tag.startsWith(TAG_PREFIX) }
            .mapNotNull { r ->
                val v = r.tag.removePrefix(TAG_PREFIX)
                val apk = r.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@mapNotNull null
                AppRelease(v, r.body.orEmpty().trim(), apk.url, apk.size)
            }
            .maxWithOrNull { a, b -> compare(a.version, b.version) }

    /** Vergleicht Versionsnummern wie „0.8.10“ und „0.8.2“ richtig (nicht als Text). */
    fun compare(a: String, b: String): Int {
        val x = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    fun isNewer(candidate: String, installed: String) = compare(candidate, installed) > 0
}
