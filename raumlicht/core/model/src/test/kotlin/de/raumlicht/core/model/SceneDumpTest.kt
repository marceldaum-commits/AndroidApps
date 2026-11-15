package de.raumlicht.core.model

import org.junit.Test
import java.io.File

/**
 * Schreibt Bühnen-Zustände nach build/scenes.json. Daraus rendert `scene/test/shot.mjs` echte 3D-Bilder,
 * die in den Bildschirmfoto-Tests der App an Stelle der Bühne stehen (Robolectric kann kein WebGL).
 */
class SceneDumpTest {
    private fun shot(name: String, state: String, extra: String = "") = """{"name":"$name","settle":2.2,"state":$state$extra}"""
    private fun level(id: String, values: Values? = null, phase: Phase = Phase.AIM, extra: String = ""): String {
        val l = Levels.byId(id)!!
        return shot(id + if (phase == Phase.DONE) "-fertig" else "", l.scene(values ?: if (phase == Phase.DONE) l.solution else l.start, phase), extra)
    }
    private val party = ""","actions":[{"party":3,"wait":0.9}]"""

    @Test fun dump() {
        val shots = listOf(
            shot("start", Showreel.scene(), ""","camera":{"az":0.75,"el":0.3,"dist":17}"""),
            level("1-2", mapOf("P" to listOf(2, 3, 0))),
            level("1-6", phase = Phase.DONE, extra = party),
            level("2-2", mapOf("v" to listOf(4, -2, 1))),
            level("3-2", phase = Phase.DONE, extra = party),
            level("4-2", mapOf("P" to listOf(3, 3, 2))),
            level("4-4"),
            level("5-1", phase = Phase.DONE, extra = party),
            level("5-3", mapOf("n" to listOf(1, 2, 2), "c" to listOf(6))),
            level("5-4", mapOf("d" to listOf(1, 0, -2))),
            level("6-3", phase = Phase.DONE, extra = party),
            level("6-5", phase = Phase.DONE, extra = ""","actions":[{"fire":{"from":[4,1,1],"to":[0,5,3],"color":"cyan","ms":300,"hit":false},"wait":0.35},{"fire":{"from":[0,5,3],"to":[2,7,4],"color":"cyan","ms":300},"wait":0.5}]"""),
        )
        File("build").mkdirs()
        File("build/scenes.json").writeText(shots.joinToString(",\n", "[", "]"))
    }
}
