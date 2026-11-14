package de.wissensgarten.app.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import de.wissensgarten.core.data.AppContainer
import de.wissensgarten.core.model.Sync
import de.wissensgarten.core.model.SyncSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Was die Oberfläche über den Geräte-Sync wissen muss. */
data class SyncStatus(
    val connected: Boolean = false,
    val account: String? = null,
    val running: Boolean = false,
    val lastSync: Long = 0,
    val error: String? = null,
)

/**
 * Geräte-Sync über den versteckten App-Ordner („appDataFolder“) im Google Drive der Familie.
 * Zugriff nur auf diesen Ordner (Berechtigung drive.appdata) – die App sieht sonst nichts im Drive,
 * und der Ordner ist für andere Apps unsichtbar. Kein eigener Server.
 */
class DriveSync(private val context: Context, private val container: AppContainer) {
    private val prefs = context.getSharedPreferences("geraete_sync", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SyncStatus(
        connected = prefs.getBoolean("verbunden", false),
        account = prefs.getString("konto", null),
        lastSync = prefs.getLong("zuletzt", 0),
    ))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    /** Schritt 1 der Anmeldung. Liefert entweder direkt ein Ergebnis oder einen Dialog, den die Activity zeigen muss. */
    suspend fun authorize(): Result<AuthOutcome> = runCatching {
        val r = Identity.getAuthorizationClient(context).authorize(request).await()
        if (r.hasResolution()) AuthOutcome.NeedsUser(r.pendingIntent!!.intentSender) else AuthOutcome.Done(r)
    }.recoverCatching { throw IllegalStateException(friendly(it)) }

    /** Schritt 2: Ergebnis des Google-Dialogs auswerten. */
    fun resultFromIntent(data: Intent?): Result<AuthorizationResult> = runCatching {
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
    }.recoverCatching { throw IllegalStateException(friendly(it)) }

    fun markConnected(r: AuthorizationResult) {
        @Suppress("DEPRECATION")
        val email = runCatching { r.toGoogleSignInAccount()?.email }.getOrNull()
        prefs.edit().putBoolean("verbunden", true).putString("konto", email).apply()
        _status.value = _status.value.copy(connected = true, account = email, error = null)
    }

    /** Nur auf diesem Gerät trennen – der Stand im Drive bleibt für die anderen Geräte. */
    fun disconnect() {
        prefs.edit().clear().apply()
        container.syncStore.forgetBase()
        _status.value = SyncStatus()
    }

    /** Im Hintergrund abgleichen, höchstens alle [minGapMs]. Ohne Verbindung passiert nichts. */
    fun syncSoon(minGapMs: Long = 60_000) {
        val s = _status.value
        if (!s.connected || s.running || System.currentTimeMillis() - s.lastSync < minGapMs) return
        scope.launch { syncNow() }
    }

    /**
     * Abgleich: Stand aus dem Drive holen, mit dem eigenen zusammenführen, in die Datenbank schreiben, hochladen.
     * Hat ein anderes Gerät inzwischen hochgeladen, wird noch einmal zusammengeführt (nichts geht verloren).
     * @return true, wenn im Drive schon ein Garten lag
     */
    suspend fun syncNow(): Result<Boolean> = mutex.withLock {
        _status.value = _status.value.copy(running = true, error = null)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val token = token()
                val store = container.syncStore
                var foundRemote = false
                repeat(3) { attempt ->
                    val meta = findFile(token)
                    val remote = meta?.let { Sync.decode(download(token, it.first)) }
                    foundRemote = foundRemote || remote != null
                    val merged = Sync.merge(store.snapshot(), remote, store.base(), System.currentTimeMillis())
                    // Hat jemand während des Zusammenführens hochgeladen? Dann nochmal.
                    val now = findFile(token)
                    if (meta != null && now != null && now.second != meta.second && attempt < 2) return@repeat
                    // Erst hochladen, dann übernehmen: scheitert das Hochladen, bleibt alles wie es war
                    upload(token, meta?.first, Sync.encode(merged))
                    store.apply(merged)
                    store.saveBase(merged)
                    return@withContext foundRemote
                }
                foundRemote
            }
        }
        val now = System.currentTimeMillis()
        result.onSuccess { prefs.edit().putLong("zuletzt", now).apply() }
        _status.value = _status.value.copy(
            running = false,
            lastSync = if (result.isSuccess) now else _status.value.lastSync,
            error = result.exceptionOrNull()?.let { friendly(it) },
        )
        result
    }

    /** Gibt es im Drive schon einen Garten (z. B. zum Wiederherstellen auf einem neuen Gerät)? */
    suspend fun remoteExists(): Boolean = withContext(Dispatchers.IO) { findFile(token()) != null }

    // ------------------------------------------------------------ Drive-REST (ohne große Bibliothek)

    private suspend fun token(): String {
        val r = Identity.getAuthorizationClient(context).authorize(request).await()
        if (r.hasResolution()) {
            // Zugriff wurde entzogen – neu anmelden nötig
            prefs.edit().putBoolean("verbunden", false).apply()
            _status.value = _status.value.copy(connected = false)
            error("Bitte erneut mit Google verbinden.")
        }
        return r.accessToken ?: error("Kein Zugang von Google erhalten.")
    }

    private fun http(url: String, token: String, method: String = "GET"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000; readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    /** (Datei-ID, Änderungszeit) der Sync-Datei im App-Ordner */
    private fun findFile(token: String): Pair<String, String>? {
        val q = URLEncoder.encode("name = '$FILE' and trashed = false", "UTF-8")
        val c = http("$API/files?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime)&orderBy=modifiedTime%20desc", token)
        check(c.responseCode == 200) { "Drive antwortet mit ${c.responseCode}" }
        val files = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("files")
        if (files.length() == 0) return null
        val f = files.getJSONObject(0)
        return f.getString("id") to f.optString("modifiedTime")
    }

    private fun download(token: String, id: String): ByteArray {
        val c = http("$API/files/$id?alt=media", token)
        check(c.responseCode == 200) { "Download fehlgeschlagen (${c.responseCode})" }
        return c.inputStream.use { it.readBytes() }
    }

    private fun upload(token: String, existingId: String?, bytes: ByteArray) {
        if (existingId != null) {
            val c = http("$UPLOAD/files/$existingId?uploadType=media", token, "PATCH")
            c.doOutput = true; c.setRequestProperty("Content-Type", "application/gzip")
            c.outputStream.use { it.write(bytes) }
            check(c.responseCode in 200..299) { "Hochladen fehlgeschlagen (${c.responseCode})" }
        } else {
            val boundary = "wissensgarten" + System.nanoTime()
            val meta = """{"name":"$FILE","parents":["appDataFolder"]}"""
            val c = http("$UPLOAD/files?uploadType=multipart", token, "POST")
            c.doOutput = true; c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            c.outputStream.use { o ->
                o.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: application/gzip\r\n\r\n".toByteArray())
                o.write(bytes)
                o.write("\r\n--$boundary--\r\n".toByteArray())
            }
            check(c.responseCode in 200..299) { "Hochladen fehlgeschlagen (${c.responseCode})" }
        }
    }

    private fun friendly(t: Throwable): String = when {
        t is ApiException && t.statusCode == 10 -> "Google-Anmeldung ist für diese App noch nicht eingerichtet."
        t is ApiException && t.statusCode == 7 -> "Keine Internetverbindung."
        t is ApiException && (t.statusCode == 16 || t.statusCode == 12501) -> "Anmeldung abgebrochen."
        t is java.net.UnknownHostException || t is java.net.SocketTimeoutException -> "Keine Internetverbindung."
        else -> t.message ?: "Unbekannter Fehler"
    }

    sealed interface AuthOutcome {
        data class Done(val result: AuthorizationResult) : AuthOutcome
        data class NeedsUser(val sender: IntentSender) : AuthOutcome
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val FILE = "wissensgarten.sync"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
