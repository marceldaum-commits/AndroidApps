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
    /** null = Stille (z. B. Klassen-Challenge) */
    fun setMusicMood(m: MusicMood?) {}
    fun setMusicEnabled(on: Boolean) {}
    /** Musik-Lautstärke 0…1 */
    fun setMusicVolume(v: Float) {}
    /** Erste Berührung: erst ab jetzt darf Musik spielen (Android; der Browser erlaubt Ton ohnehin erst danach) */
    fun onFirstTouch() {}
    /** Kaufweg für Kurvenflug Pro (null = keiner, z. B. in Bildschirmfoto-Tests) */
    val proShop: ProShop? get() = null
    /** Vorlesen (null = nicht möglich, z. B. in Bildschirmfoto-Tests) */
    val reader: de.kurvenflug.app.ui.fx.Reader? get() = null
    /** Startbildschirm offen? (WebApp: dort darf ein Update neu laden) */
    fun onHome(home: Boolean) {}
    /**
     * „App installieren“ (WebApp als PWA): "prompt" = der Browser bietet die Installation an (Android/Chrome, Edge),
     * "ios" = iPhone/iPad – Anleitung „Teilen → Zum Home-Bildschirm“, "" = nichts anzeigen (schon installiert oder nicht möglich).
     */
    fun installMode(): String = ""
    /** Installations-Dialog des Browsers öffnen (nur bei "prompt") */
    fun install() {}
    /** Anonyme Nutzungszählung: Ereignis zählen ([de.kurvenflug.core.model.UsageCount.EVENTS]); gesendet wird nur, wenn [setCounting] an ist */
    fun count(event: String) {}
    fun setCounting(on: Boolean) {}
    /** „Eltern-Handy koppeln“ (nur WebApp): null = nicht möglich; "" = nicht gekoppelt; sonst der Link fürs Eltern-Handy */
    fun parentLink(): String? = null
    /** Koppeln (Schlüssel entstehen im Browser), gibt den Link zurück */
    fun parentPair(nick: String): String = ""
    fun parentUnpair() {}
    fun parentNick(): String = ""
    /** Neuer Bericht (JSON) – wird verschlüsselt und nach der Lernsitzung hochgeladen */
    fun parentReport(json: String) {}
    /** Lernstand aus der Sicherung wiederherstellen; Ergebnis: ok | vorhanden | keine | andere_app | link | fehler */
    fun parentRestore(link: String, force: Boolean, done: (String) -> Unit) { done("fehler") }
}

/** Geräte-Dienste; ohne Bereitstellung (z. B. in Bildschirmfoto-Tests) die Standard-Umsetzung der Plattform. */
val LocalPlatform = staticCompositionLocalOf { defaultPlatform() }

expect fun defaultPlatform(): Platform

/** Für Funktionen, die vorher einen Android-Context bekamen */
fun shareText(p: Platform, text: String, title: String = "Teilen") = p.shareText(text, title)
