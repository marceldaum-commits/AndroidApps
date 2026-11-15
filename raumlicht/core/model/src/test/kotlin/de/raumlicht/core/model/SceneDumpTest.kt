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
            level("7-2"),
            level("8-4", phase = Phase.DONE, extra = party),
            level("9-1", mapOf("a" to listOf(2))),
            level("9-3", phase = Phase.DONE, extra = party),
            level("9-5", mapOf("a" to listOf(2))),
            level("10-1", phase = Phase.DONE, extra = party),
            level("10-3"),
            level("11-5", mapOf("a" to listOf(1))),
            level("12-3", mapOf("a" to listOf(3))),
            level("12-4"),
            shot("lage", LageCheck.round(3).scene(true)),
            shot("baukasten", Show.scene(Show.demo)),
            Exams.all[0].parts[1].level.let { shot("abi", it.scene(mapOf("n" to listOf(0, 3, -2), "c" to listOf(3)), Phase.AIM)) },
            level("13-2"),
            level("14-2", mapOf("a" to listOf(1))),
            level("14-5"),
            Guides.all[0].parts[0].level.let { shot("schritt", it.scene(mapOf("a" to listOf(1)), Phase.AIM)) },
            shot("fahrt", Coaster.byId("doppel")!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.12,"view":"fp"},"wait":1.2}]"""),
            shot("fahrt-gipfel", Coaster.byId("bergtal")!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.2,"view":"fp"},"wait":1.2}]"""),
            shot("fahrt-aussen", Coaster.byId("doppel")!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.5,"view":"out"},"wait":1.2}]"""),
            Levels.byId("13-1")!!.let { l -> shot("abschuss", l.scene(l.solution, Phase.AIM),
                ""","actions":[{"fire":{"from":[8,0,1],"to":[4,1.6,1.2],"color":"pink","ms":240},"wait":0.2},{"fire":{"from":[8,0,1],"to":[4,2.8,1.2],"color":"pink","ms":240},"wait":0.2},""" +
                """{"fire":{"from":[8,0,1],"to":[4,4,1.2],"color":"pink","ms":240},"wait":0.3},{"state":${l.scene(l.solution, Phase.DONE)},"wait":0.25}]""") },
            level("15-3", mapOf("a" to listOf(-1), "d" to listOf(1), "e" to listOf(4))),
            Levels.byId("13-2")!!.let { l -> shot("zug", l.scene(l.solution, Phase.DONE)) },
            Levels.byId("13-5")!!.let { l -> shot("blitz", l.scene(l.start, Phase.AIM)) },
            shot("eigene", Coaster.custom(Expr.parse("e^(-0,3x)*sin(3x)").getOrThrow(), 0.0, 8.0)!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.35,"view":"out"},"wait":1.0}]"""),
            shot("eigene-pol", Coaster.custom(Expr.parse("1/x").getOrThrow(), -2.0, 2.0)!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.2,"view":"out"},"wait":1.0}]"""),
            shot("ballerbude", BalloonGame(11).scene(9.0, emptySet())),
            shot("ableitung", Coaster.byId("huegel")!!.scene(2.6, derivative = true, fill = false), ""","actions":[{"ride":{"id":"bahn","u":0.65,"view":"out"},"wait":1.0}]"""),
            level("16-1"),
            level("16-3"),
            level("17-4", mapOf("a" to listOf(3))),
            shot("galton", Galton().let { g -> val r = kotlin.random.Random(5); val c = IntArray(9); repeat(120) { c[g.drop(0.5, r).first]++ }; g.scene(c.toList(), 0.5, V3(4.0, 4.4, 3.5)) }),
            shot("raumflug", Flights.byId("kugel")!!.scene(), ""","actions":[{"ride":{"id":"bahn","u":0.08,"view":"fp"},"wait":1.2}]"""),
            shot("fahrt-extra", Coaster.byId("doppel")!!.scene(2.6, derivative = true, fill = true), ""","actions":[{"ride":{"id":"bahn","u":0.65,"view":"out"},"wait":1.0}]"""),
            Gen.jam(JamTopic.MIX, 1, 0).let { shot("jam", it.scene(it.start, Phase.AIM)) },
            Daily.forDay(20_772).let { shot("tag", it.scene(it.start, Phase.AIM)) },
            level("6-5", phase = Phase.DONE, extra = ""","actions":[{"fire":{"from":[4,1,1],"to":[0,5,3],"color":"cyan","ms":300,"hit":false},"wait":0.35},{"fire":{"from":[0,5,3],"to":[2,7,4],"color":"cyan","ms":300},"wait":0.5}]"""),
        )
        File("build").mkdirs()
        File("build/scenes.json").writeText(shots.joinToString(",\n", "[", "]"))
    }
}
