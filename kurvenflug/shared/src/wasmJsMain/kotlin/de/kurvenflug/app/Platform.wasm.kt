package de.kurvenflug.app

import de.kurvenflug.app.ui.fx.MusicSynth
import de.kurvenflug.app.ui.fx.Sfx
import de.kurvenflug.app.ui.fx.Sound
import de.kurvenflug.app.ui.fx.Synth
import de.kurvenflug.core.model.Flight
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.MusicMood
import de.kurvenflug.core.model.SpaceMusic
import de.kurvenflug.core.model.Vec
import de.kurvenflug.core.model.num

/** Geräusche im Browser: einmal erzeugt (wie auf Android), über Web Audio abgespielt. */
object WebSfx : Sfx() {
    private val ids = HashMap<Sound, Int>()
    private fun id(s: Sound) = ids.getOrPut(s) {
        val data = Synth.render(s)
        val id = jsBufNew(data.size, Synth.SR)
        for (i in data.indices) jsBufSet(id, i, data[i].toDouble())
        jsBufDone(id); id
    }
    /** Alle Geräusche vorab erzeugen (nach dem Start, damit der erste Tipp sofort klingt) */
    fun prepare() = Sound.entries.forEach { id(it) }
    /** Browser erlauben Ton erst nach dem ersten Tippen – vorher nichts erzeugen */
    internal var ready = false
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (enabled && ready) jsPlay(id(s), pitch.coerceIn(0.5f, 2f).toDouble(), volume.toDouble())
    }
}

/** Die WebApp – teilt über das Teilen-Menü des Geräts bzw. die Zwischenablage, Musik über Web Audio. */
object WebPlatform : Platform {
    /** wird beim Start aus der Build-Version gesetzt */
    override var versionName: String = ""
    override val isWeb = true
    override val selfUpdate = false
    override val sfx: Sfx = WebSfx
    override fun shareText(text: String, title: String) = jsShare(text, title)
    override fun openUrl(url: String) = jsOpen(url)

    override fun sharePhoto(level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>) {
        // Bild wird in kf.js auf einem Canvas gemalt – hier nur die Zahlen (Feld −R…R)
        fun n(v: Double) = ((v / Flight.R) * 1000).toInt().toString()
        val spec = buildString {
            append("P"); path.forEachIndexed { i, p -> if (i % 2 == 0 || i == path.lastIndex) append(' ').append(n(p.x)).append(',').append(n(p.y)) }
            append("\nT"); level.targets.forEach { append(' ').append(n(it.x)).append(',').append(n(it.y)).append(',').append(n(it.r)) }
            append("\nA"); level.asteroids.forEach { append(' ').append(n(it.x)).append(',').append(n(it.y)).append(',').append(n(it.r)) }
            append("\nS ").append(stars)
            append("\nF ").append(level.params.joinToString("   ") { "${it.label} = ${num(level.values(values).getValue(it.key))}${it.unit}" })
        }
        jsPhoto(spec, level.title)
    }

    /** Beim Start aufrufen: sobald der Browser Ton erlaubt, Geräusche und Musik vorbereiten */
    fun init(version: String) {
        versionName = version
        jsOnUnlock { WebSfx.ready = true; WebSfx.prepare(); refresh() }
    }

    private var musicOn = true
    private var mood: MusicMood? = null
    private val musicIds = HashMap<MusicMood, Int>()
    private fun musicId(m: MusicMood) = musicIds.getOrPut(m) {
        val data = MusicSynth.render(SpaceMusic.compose(m))
        val id = jsBufNew(data.size, MusicSynth.SR)
        for (i in data.indices) jsBufSet(id, i, data[i] / 32768.0)
        jsBufDone(id); id
    }
    private fun refresh() = mood.let { m -> if (!WebSfx.ready) return@let; if (musicOn && m != null) jsMusic(musicId(m), 0.32) else jsMusic(-1, 0.0) }
    override fun setMusicMood(m: MusicMood) { mood = m; refresh() }
    override fun setMusicEnabled(on: Boolean) { musicOn = on; refresh() }
}

actual fun defaultPlatform(): Platform = WebPlatform
