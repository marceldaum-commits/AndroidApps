package de.kurvenflug.app

import de.kurvenflug.app.ui.fx.MusicSynth
import kotlinx.coroutines.launch
import de.kurvenflug.app.ui.fx.Sfx
import de.kurvenflug.app.ui.fx.Sound
import de.kurvenflug.app.ui.fx.Synth
import de.kurvenflug.core.model.Flight
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.MusicMood
import de.kurvenflug.core.model.SpaceMusic
import de.kurvenflug.core.model.Vec
import de.kurvenflug.core.model.num

/** In kleinen Stücken an Web Audio übergeben – jeder Wert ist ein Aufruf über die Grenze Kotlin ↔ JavaScript. */
internal suspend fun transfer(id: Int, n: Int, value: (Int) -> Double) {
    var i = 0
    while (i < n) {
        val end = minOf(n, i + 40_000)
        while (i < end) { jsBufSet(id, i, value(i)); i++ }
        kotlinx.coroutines.delay(1)
    }
    jsBufDone(id)
}

/** Geräusche im Browser: einmal erzeugt (wie auf Android), über Web Audio abgespielt – Stück für Stück vorbereitet. */
object WebSfx : Sfx() {
    private val ids = HashMap<Sound, Int>()
    private val scope = kotlinx.coroutines.MainScope()
    private var pending: Triple<Sound, Float, Float>? = null
    private var pendingAt = 0L

    /** Alle Geräusche nach und nach erzeugen (nach dem ersten Tippen), ohne die Oberfläche anzuhalten */
    fun prepare() = scope.launch {
        // kurze Klänge zuerst, die Erkennungsmelodie zuletzt
        for (s in Sound.entries.sortedBy { if (it == Sound.JINGLE) 1 else 0 }) {
            if (s in ids) continue
            val data = Synth.render(s)
            val id = jsBufNew(data.size, Synth.SR)
            transfer(id, data.size) { data[it].toDouble() }
            ids[s] = id
            pending?.let { (ps, pitch, vol) -> if (ps == s && de.kurvenflug.core.model.nowMillis() - pendingAt < 4000) { pending = null; play(ps, pitch, vol) } }
            kotlinx.coroutines.delay(1)
        }
    }
    /** Browser erlauben Ton erst nach dem ersten Tippen – vorher nichts erzeugen */
    internal var ready = false
    override fun play(s: Sound, pitch: Float, volume: Float) {
        if (!enabled || !ready) return
        val id = ids[s]
        // Noch nicht fertig erzeugt? Erkennungsmelodie kurz vormerken, alles andere auslassen
        if (id == null) { if (s == Sound.JINGLE) { pending = Triple(s, pitch, volume); pendingAt = de.kurvenflug.core.model.nowMillis() }; return }
        jsPlay(id, pitch.coerceIn(0.5f, 2f).toDouble(), (volume * level).toDouble())
    }
}

/** Die WebApp – teilt über das Teilen-Menü des Geräts bzw. die Zwischenablage, Musik über Web Audio. */
object WebPlatform : Platform {
    /** wird beim Start aus der Build-Version gesetzt */
    override var versionName: String = ""
    override val isWeb = true
    override val selfUpdate = false
    override val sfx: Sfx = WebSfx
    override val reader: de.kurvenflug.app.ui.fx.Reader? get() = if (de.kurvenflug.core.model.Nova.ENABLED) de.kurvenflug.app.ui.fx.WebReader else null
    override fun onHome(home: Boolean) { runCatching { jsAtHome(home) } }
    override fun installMode(): String = runCatching { jsInstallMode() }.getOrDefault("")
    override fun install() { runCatching { jsInstall() } }
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
    override var proShop: ProShop? = null

    fun init(version: String, proKey: String = "", checkoutUrl: String = "") {
        versionName = version
        proShop = CodeShop(proKey, checkoutUrl) { jsOpen(it) }
        jsOnUnlock { WebSfx.ready = true; WebSfx.prepare(); refresh() }
    }

    private var musicOn = true
    private var mood: MusicMood? = null
    private val musicIds = HashMap<MusicMood, Int>()
    private val rendering = HashSet<MusicMood>()
    private val musicScope = kotlinx.coroutines.MainScope()
    /** Musik im Hintergrund in Häppchen erzeugen; fertig → spielen, falls sie noch gewünscht ist */
    private fun musicId(m: MusicMood): Int? {
        musicIds[m]?.let { return it }
        if (rendering.add(m)) musicScope.launch {
            val data = MusicSynth.renderInSlices(SpaceMusic.compose(m)) { kotlinx.coroutines.delay(1) }
            val id = jsBufNew(data.size, MusicSynth.SR)
            transfer(id, data.size) { data[it] / 32768.0 }
            musicIds[m] = id; rendering.remove(m)
            refresh()
        }
        return null
    }
    private var musicVol = 0.7
    private fun refresh() = mood.let { m ->
        if (!WebSfx.ready) return@let
        if (musicOn && m != null && musicVol > 0.01) musicId(m)?.let { jsMusic(it, 0.32 * musicVol / 0.7) } ?: Unit else jsMusic(-1, 0.0)
    }
    override fun setMusicMood(m: MusicMood?) { mood = m; refresh() }
    override fun setMusicVolume(v: Float) { musicVol = v.toDouble(); refresh() }
    override fun setMusicEnabled(on: Boolean) { musicOn = on; refresh() }
}

actual fun defaultPlatform(): Platform = WebPlatform
