package de.raumlicht.core.model

enum class ControlKind { POINT, VECTOR, SCALAR }

/** Etwas, das man einstellt: ein Punkt, ein Vektor (je drei ganze Zahlen) oder eine einzelne Zahl. */
data class Control(
    val key: String, val name: String, val kind: ControlKind,
    val start: List<Int>, val min: List<Int>, val max: List<Int>, val color: String,
    /** Anzeige = Wert · step + suffix (z. B. Winkel in 15°-Schritten) */
    val step: Int = 1, val suffix: String = "",
) {
    val size get() = start.size
}

typealias Values = Map<String, List<Int>>
fun Values.vec(key: String) = V3.of(getValue(key))
fun Values.int(key: String) = getValue(key)[0]

/** Baustein der Formelanzeige. Teile mit [control] zeigen live die eingestellten Zahlen in der Farbe des Reglers. */
sealed interface Part {
    data class Txt(val s: String) : Part
    /** Spaltenvektor */
    data class Col(val cells: List<String>, val control: String? = null) : Part
    /** Einzelne Zahl */
    data class Num(val s: String, val control: String? = null) : Part
}

/** Ein Laserstrahl, der beim Auslösen gezeichnet wird. */
data class Beam(val from: V3, val to: V3, val color: String, val hit: Boolean)

/** Was beim Auslösen passiert und was Lux dazu sagt. */
data class Outcome(val success: Boolean, val message: String, val beams: List<Beam> = emptyList(), val flyTo: V3? = null)

/** Griff in der 3D-Szene: Regler [control], gezeichnet an anchor + Wert. */
data class Handle(val control: String, val anchor: V3)

enum class Phase { AIM, DONE }

class Level(
    val id: String, val chapter: Int, val title: String, val mission: String,
    val bounds: Bounds, val controls: List<Control>, val solution: Values,
    /** Rechentipp (kostet einen Stern) */
    val tip: String,
    /** Rechenweg, nach dem Lösen zum Nachlesen */
    val way: List<String>,
    val abi: Boolean = false,
    val handle: Handle? = null,
    val formula: (Values) -> List<List<Part>>,
    val objects: (Values, Phase) -> List<Obj>,
    val evaluate: (Values) -> Outcome,
) {
    val start: Values get() = controls.associate { it.key to it.start }
    fun control(key: String) = controls.first { it.key == key }
    /** Wert setzen, auf den erlaubten Bereich begrenzt */
    fun set(values: Values, key: String, v: List<Int>): Values {
        val c = control(key)
        return values + (key to v.mapIndexed { i, x -> x.coerceIn(c.min[i], c.max[i]) })
    }
    fun scene(values: Values, phase: Phase): String = Stage.encode(bounds, objects(values, phase))

    companion object {
        /** Sterne: beim ersten Versuch 3, beim zweiten 2, danach 1. Mit Rechentipp höchstens 2. */
        fun stars(attempts: Int, usedTip: Boolean) = minOf(if (usedTip) 2 else 3, when (attempts) { 1 -> 3; 2 -> 2; else -> 1 })
    }
}

/** Die drei Gebiete des Mathe-Abiturs – danach ist die Tour auf der Startseite geordnet. */
enum class Area(val title: String, val emoji: String, val color: String) { GEOMETRIE("Geometrie", "📐", "cyan"), ANALYSIS("Analysis", "📈", "yellow"), STOCHASTIK("Stochastik", "🎲", "pink") }

/** [title] sagt, worum es geht (z. B. „Geraden“); [topic] zählt die Inhalte auf; [venue] ist nur die Kulisse. */
data class Chapter(val number: Int, val title: String, val topic: String, val venue: String, val emoji: String, val color: String, val intro: List<String>, val levels: List<Level>,
                   val area: Area = Area.GEOMETRIE)
