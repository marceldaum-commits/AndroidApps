package de.kurvenflug.app

import androidx.compose.runtime.staticCompositionLocalOf
import de.kurvenflug.app.ui.fx.Sfx
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.MusicMood
import de.kurvenflug.core.model.Vec

/**
 * Was je nach Gerät anders funktioniert: Teilen, Fotos, Links, Musik, Updates.
 * Android: Intents, AudioTrack, GitHub-Updates. Browser: Teilen-Menü/Zwischenablage, Web Audio, aktualisiert sich selbst.
 */
interface Platform {
    val versionName: String
    /** Läuft im Browser (WebApp) */
    val isWeb: Boolean
    /** Eingebauter Update-Knopf (nur Android außerhalb des Play Stores) */
    val selfUpdate: Boolean
    val sfx: Sfx?
    fun shareText(text: String, title: String = "Teilen")
    /** Foto der geschafften Flugbahn teilen */
    fun sharePhoto(level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>)
    fun openUrl(url: String)
    fun setMusicMood(m: MusicMood) {}
    fun setMusicEnabled(on: Boolean) {}
    /** Kaufweg für Kurvenflug Pro (null = keiner, z. B. in Bildschirmfoto-Tests) */
    val proShop: ProShop? get() = null
}

/** Geräte-Dienste; ohne Bereitstellung (z. B. in Bildschirmfoto-Tests) die Standard-Umsetzung der Plattform. */
val LocalPlatform = staticCompositionLocalOf { defaultPlatform() }

expect fun defaultPlatform(): Platform

/** Für Funktionen, die vorher einen Android-Context bekamen */
fun shareText(p: Platform, text: String, title: String = "Teilen") = p.shareText(text, title)
