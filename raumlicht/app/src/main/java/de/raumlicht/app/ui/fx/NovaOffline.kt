package de.raumlicht.app.ui.fx

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.raumlicht.core.model.Vorlesen
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * „Alle Nova-Texte offline laden“: holt die fertigen Dateien aller festen Vorlese-Texte in den App-Speicher, damit Lux auch ohne Internet mit Nova spricht.
 * Zwei Qualitäten (Standard / Bestqualität), je ein eigener Ordner. Geladen wird nur, was fehlt – vier Dateien gleichzeitig; nach einem Abbruch geht es dort weiter.
 * Texte, die erst in der Aufgabe entstehen, gehören nicht dazu.
 */
class NovaOffline(private val ctx: Context, private val fetch: NovaFetch = ::novaFetch,
                  /** Dateinamen aller festen Texte (Tests geben eine kurze Liste) */
                  private val allKeys: () -> List<String> = { Vorlesen.texts().map { Vorlesen.key(it) } }) {
    var loading by mutableStateOf(false); private set
    var total by mutableStateOf(0); private set
    var standardDone by mutableStateOf(0); private set
    var bestDone by mutableStateOf(0); private set
    var standardStored by mutableStateOf(0L); private set
    var bestStored by mutableStateOf(0L); private set
    /** Gesamtgröße laut Server (null = unbekannt) */
    var standardBytes by mutableStateOf<Long?>(null); private set
    var bestBytes by mutableStateOf<Long?>(null); private set
    /** Fortschritt des laufenden Ladens: so viele von [total] sind erledigt */
    var done by mutableStateOf(0); private set
    @Volatile private var cancelled = false
    private val keys: List<String> by lazy { allKeys() }

    /** Zählt, was schon da ist, und fragt den Server nach der Gesamtgröße – im Hintergrund */
    fun refresh(askServer: Boolean = true) { thread(name = "nova-offline-stand", isDaemon = true) { runCatching { count(); if (askServer) sizes() } } }
    private fun count() {
        total = keys.size
        fun have(best: Boolean) = dir(ctx, best).list()?.toSet().orEmpty().let { names -> keys.count { "$it.mp3" in names } }
        fun bytes(best: Boolean) = dir(ctx, best).listFiles()?.sumOf { it.length() } ?: 0L
        standardDone = have(false); bestDone = have(true); standardStored = bytes(false); bestStored = bytes(true)
    }
    private fun sizes() {
        for (host in listOf(Vorlesen.APP_HOST, Vorlesen.TEST_HOST)) {
            val (code, body) = fetch(Vorlesen.standUrl(host), null); if (code != 200) continue
            val s = Vorlesen.stand(body.decodeToString()) ?: continue
            standardBytes = s.first; bestBytes = s.second; return
        }
    }

    /** Fehlende Dateien laden. Läuft schon etwas, passiert nichts. */
    fun start(best: Boolean) {
        if (loading) return
        loading = true; cancelled = false
        thread(name = "nova-offline", isDaemon = true) {
            runCatching {
                val target = dir(ctx, best).apply { mkdirs() }
                val have = target.list()?.toSet().orEmpty()
                val missing = ConcurrentLinkedQueue(keys.filter { "$it.mp3" !in have })
                total = keys.size; val finished = AtomicInteger(keys.size - missing.size); done = finished.get()
                (1..4).map { thread(name = "nova-offline-$it", isDaemon = true) {
                    while (!cancelled) { val key = missing.poll() ?: break
                        runCatching { load(key, best, target) }
                        done = finished.incrementAndGet() }
                } }.forEach { it.join() }
                count()
            }
            loading = false
        }
    }
    /**
     * Eine Datei holen. Bestqualität kommt aus dem Ordner „original“; gibt es sie dort (noch) nicht, wird die normale Datei geholt und als Standard abgelegt –
     * so spricht Lux offline trotzdem, und beim nächsten Tippen auf „Bestqualität“ wird nur das Fehlende nachgeholt. Fehlt auch die normale (404), wird sie still übersprungen.
     */
    private fun load(key: String, best: Boolean, target: File) {
        var into = target
        var (code, bytes) = fetch(Vorlesen.fileUrl(key, best), null)
        if (best && code == 404) {
            into = dir(ctx, false).apply { mkdirs() }
            if (File(into, "$key.mp3").length() > 0) return
            fetch(Vorlesen.fileUrl(key, false), null).let { code = it.first; bytes = it.second }
        }
        if (code != 200 || !NovaVoice.looksLikeMp3(bytes)) return
        val part = File(into, "$key.teil"); part.writeBytes(bytes); part.renameTo(File(into, "$key.mp3"))
    }
    fun cancel() { cancelled = true }
    /** Offline-Stimme löschen – beide Qualitäten */
    fun delete() { cancelled = true; thread(name = "nova-offline-weg", isDaemon = true) { runCatching { root(ctx).deleteRecursively(); count() } } }

    companion object {
        private fun root(ctx: Context) = File(ctx.filesDir, "nova-offline")
        fun dir(ctx: Context, best: Boolean) = File(root(ctx), if (best) "best" else "standard")
        /** Offline geladene Datei zu einem Dateinamen – Bestqualität zuerst; null, wenn keine da ist */
        fun find(ctx: Context, key: String): File? = sequenceOf(true, false).map { File(dir(ctx, it), "$key.mp3") }.firstOrNull { it.length() > 0 }
    }
}
