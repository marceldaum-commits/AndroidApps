package de.kurvenflug.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.kurvenflug.app.ui.fx.BackgroundMusic
import de.kurvenflug.app.ui.fx.Photo
import de.kurvenflug.app.ui.fx.Sfx
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.MusicMood
import de.kurvenflug.core.model.Vec

/** Android: Teilen über das System-Menü, Musik über [BackgroundMusic]. */
class AndroidPlatform(
    private val ctx: Context,
    override val versionName: String,
    override val selfUpdate: Boolean,
    override val sfx: Sfx?,
    private val music: BackgroundMusic?,
    override val proShop: ProShop? = null,
) : Platform {
    override val isWeb = false
    override fun shareText(text: String, title: String) {
        runCatching {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    override fun sharePhoto(level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>) {
        runCatching { Photo.share(ctx, level, path, stars, values) }
    }
    override fun openUrl(url: String) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    override fun setMusicMood(m: MusicMood?) { music?.setMood(m) }
    override fun setMusicVolume(v: Float) { music?.setVolume(v) }
    override fun onFirstTouch() { music?.allow() }
    override fun setMusicEnabled(on: Boolean) { music?.setEnabled(on) }
}

/** Ohne Bereitstellung (Bildschirmfoto-Tests): nichts teilen, Update-Bereich wie in der echten App zeigen */
actual fun defaultPlatform(): Platform = object : Platform {
    override val versionName = ""
    override val isWeb = false
    override val selfUpdate = true
    override val sfx: Sfx? = null
    override fun shareText(text: String, title: String) {}
    override fun sharePhoto(level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>) {}
    override fun openUrl(url: String) {}
}
