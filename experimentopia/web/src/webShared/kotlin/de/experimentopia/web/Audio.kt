package de.experimentopia.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.experimentopia.app.ui.fx.*
import de.experimentopia.core.model.AudioPrefs
import de.experimentopia.core.model.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Dieselbe Musik wie unter Android ([Composer]): neue Phrasen je Welt, Pausen dazwischen, erst nach der ersten Berührung. */
class WebMusic(private val scope: CoroutineScope) : MusicPlayer {
    private var world: World? = null
    private var enabled = true
    private var volume = 0.5f
    private var ducked = false
    private var job: Job? = null
    private var inJingle = false
    private var seed = Random.nextInt()
    private fun gain() = (if (ducked) 0.3 else 1.0) * volume * 0.8

    private fun restart(withJingle: Boolean = false) {
        job?.cancel(); musicStop()
        if (!enabled) return
        job = scope.launch {
            while (!audioReady()) delay(400)
            if (withJingle) {
                inJingle = true
                storePcm("musik", Composer.jingle(), Composer.SR)
                val d = musicPlay("musik", gain()); delay((d * 1000).toLong() + 2500); inJingle = false
            }
            while (true) {
                val w = world
                if (w == null || pageHidden()) { delay(1000); continue }
                storePcm("musik", Composer.phrase(w, seed++), Composer.SR)
                val d = musicPlay("musik", gain())
                delay((d * 1000).toLong() + 2500 + Random.nextLong(2500))
            }
        }
    }

    override fun world(w: World?) { if (w == world) return; world = w; if (!inJingle) restart() }
    override fun jingle() = restart(withJingle = true)
    override fun configure(p: AudioPrefs) {
        val was = enabled; enabled = p.music; volume = p.musicVol
        if (was != enabled) restart() else musicGain(gain())
    }
    override fun duck(on: Boolean) { ducked = on; musicGain(gain()) }
}

/** „App installieren“ im Browser: merkt sich, ob der Browser die Installation anbietet oder ob es ein iPhone/iPad ist */
class WebInstaller : Installer {
    override var state by mutableStateOf(InstallState.HIDDEN); private set
    private fun refresh() { state = when (installState()) { 1 -> InstallState.PROMPT; 2 -> InstallState.IOS; else -> InstallState.HIDDEN } }
    init { refresh(); onInstallChanged { refresh() } }
    override fun install() { installPrompt { refresh() } }
}
