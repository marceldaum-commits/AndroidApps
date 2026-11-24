package de.experimentopia.core.model

/** Die Labore – jedes ist ein eigenes, frei bespielbares Versuchsgerät. */
enum class Lab(val title: String, val emoji: String, val island: Island, val blurb: String) {
    SCHWIMMEN("Wasserbecken", "🛁", Island.WASSER, "Schwimmt es oder sinkt es? Auch mit eigener Masse und eigenem Volumen."),
    DICHTETURM("Dichteturm", "🌈", Island.WASSER, "Flüssigkeiten schichten sich nach ihrer Dichte."),
    WIPPE("Wippe", "⚖️", Island.KRAFT, "Hebelgesetz: Masse mal Abstand."),
    FLASCHENZUG("Flaschenzug", "🏗️", Island.KRAFT, "Mit mehr Seilen weniger Kraft."),
    RAMPE("Rampe", "📐", Island.KRAFT, "Schiefe Ebene: lange Rampe, wenig Kraft."),
    NEWTON("Wagen & Skateboards", "🛹", Island.KRAFT, "Newton: Kraft, Masse, Beschleunigung – und Rückstoß."),
    STROM("Stromkreis", "⚡", Island.STROM, "Lampen, Schalter, Leitertest, Widerstände."),
    MAGNET("Magnet", "🧲", Island.STROM, "Was zieht ein Magnet an?"),
    ROTKOHL("Rotkohl-Labor", "🟣", Island.CHEMIE, "Sauer oder basisch? Der Saft verrät es mit Farben."),
    TITRATION("Säure & Lauge", "🧪", Island.CHEMIE, "Neutralisieren und verdünnen, Tropfen für Tropfen."),
    REAKTION("Atom-Baukasten", "⚛️", Island.CHEMIE, "Reaktionsgleichungen ausgleichen – dann knallt's."),
    TRENNEN("Trenn-Werkstatt", "🧺", Island.CHEMIE, "Sieb, Magnet, Filter, Eindampfen, Scheidetrichter."),
    CHROMATOGRAFIE("Chromatografie", "🖊️", Island.CHEMIE, "Welche Farben stecken in der Tinte?"),
    BALLON("Natron-Ballon", "🎈", Island.CHEMIE, "Gas aus der Flasche."),
    SCHMELZEN("Heizplatte", "🍫", Island.WAERME, "Was schmilzt wann?"),
    ERHITZEN("Eis erhitzen", "🌡️", Island.WAERME, "Vom Eis zum Dampf – mit Teilchenblick."),
    MISCHEN("Mischbecken", "🛀", Island.WAERME, "Heiß + kalt = ?"),
    LICHT("Lichtbühne", "🔦", Island.LICHTKLANG, "Farbiges Licht mischen."),
    FILTER("Farbfilter", "🎞️", Island.LICHTKLANG, "Folien schlucken Farben."),
    MALKASTEN("Malkasten", "🎨", Island.LICHTKLANG, "Malfarben mischen – anders als Licht!"),
    PRISMA("Prisma", "🔺", Island.LICHTKLANG, "Weißes Licht wird zum Regenbogen."),
    SCHATTEN("Schattenbühne", "🐰", Island.LICHTKLANG, "Lampe näher, Schatten größer – mit Strahlensatz."),
    SPIEGEL("Spiegel-Laser", "🔴", Island.LICHTKLANG, "Dreh den Spiegel und triff die Sterne."),
    BRECHUNG("Lichtknick", "💧", Island.LICHTKLANG, "Licht knickt an Wasser und Glas – oder bleibt gefangen."),
    LINSE("Optische Bank", "🔍", Island.LICHTKLANG, "Kerze, Linse, Schirm: Wann wird das Bild scharf?"),
    SPULE("Elektromagnet", "🧲", Island.STROM, "Spule, Strom und Eisenkern: Wie stark wird das Feld?"),
    MOTOR("Motor-Werkstatt", "⚙️", Island.STROM, "Leiterschaukel und Elektromotor: Kraft auf Strom."),
    DYNAMO("Dynamo-Werkstatt", "🚲", Island.STROM, "Magnet in der Spule – und Strom entsteht."),
    TRAFO("Trafo-Station", "🔌", Island.STROM, "Spannung umspannen und Strom über weite Wege schicken."),
    KLANG("Klangröhren", "🎵", Island.LICHTKLANG, "Gläser, Röhren und Saiten."),
    FALLTURM("Fallturm", "🗼", Island.FALL, "Was fällt schneller – auf Erde, Mond und im Vakuum?"),
    PENDEL("Pendel", "🕰️", Island.FALL, "Schwingungsdauer messen."),
    GENETIK("Gewächshaus", "🧬", Island.BIO, "Kreuzen und aussäen – wie Mendel."),
    OSMOSE("Osmose-Gläser", "🥔", Island.BIO, "Kartoffel und Zwiebel in Salzwasser."),
    NACHWEIS("Nährstoff-Detektiv", "🔍", Island.BIO, "Stärke, Zucker, Fett und Eiweiß nachweisen."),
    ENZYM("Verdauungs-Labor", "👅", Island.BIO, "Spucke gegen Stärke – bei jeder Temperatur."),
    WASSERPEST("Wasserpest", "🌿", Island.BIO, "Fotosynthese: Bläschen zählen."),
    RAEUBER("Räuber & Beute", "🐇", Island.BIO, "Hasen und Luchse im Wald."),
    MIKROSKOP("Mikroskop", "🔬", Island.BIO, "Teichwasser von Lupe bis 400-fach."),
    KEIMUNG("Kresse-Garten", "🌱", Island.BIO, "Was brauchen Samen zum Keimen?"),
    NAHRUNGSKETTE("Nahrungsketten", "🦊", Island.BIO, "Wer frisst wen? Ketten bauen."),
    PULS("Herz & Puls", "❤️", Island.BIO, "Dein Herz in Ruhe und beim Rennen."),
}

/** Was im Versuch zu tun ist und was im Labor steht. Jede Art gehört zu einem Labor. */
sealed interface Setup { val lab: Lab }

data class FloatSetup(val liquids: List<Liquid>, val things: List<Thing>, val eachLiquid: Boolean = false) : Setup { override val lab get() = Lab.SCHWIMMEN }
data class TowerSetup(val liquids: List<Liquid>) : Setup { override val lab get() = Lab.DICHTETURM }

enum class LeverGoal { BALANCE, RELEASE }
data class LeverSetup(val fixed: List<Load>, val tray: List<Load>, val goal: LeverGoal) : Setup { override val lab get() = Lab.WIPPE }
data class PulleySetup(val massKg: Double, val ropes: List<Int>, val maxN: Double? = null, val heightM: Double = 1.0) : Setup { override val lab get() = Lab.FLASCHENZUG }
data class RampSetup(val massKg: Double, val heightM: Double, val lengths: List<Double>) : Setup { override val lab get() = Lab.RAMPE }

enum class CircuitGoal { LIGHT, TEST_ALL, REMOVE_ONE, SHORT, AMPS, MEASURE, BOTH_WAYS }
data class CircuitSetup(
    val layout: Layout, val start: List<Slot>, val tray: List<Part>, val locked: Set<Int>, val volts: Double, val goal: CircuitGoal,
    /** Für AMPS: gewünschte Stromstärke */
    val targetA: Double? = null,
    /** Platz, dessen Widerstand man einstellen kann */
    val adjustable: Int? = null,
) : Setup { override val lab get() = Lab.STROM }

data class MagnetSetup(val items: List<MagnetItem>, val poles: Boolean = false) : Setup { override val lab get() = Lab.MAGNET }

data class PhSetup(val substances: List<Household>) : Setup { override val lab get() = Lab.ROTKOHL }
/** Titration (Lauge zutropfen) oder Verdünnen (Wasser zugeben, [baseC] = null) */
data class TitrationSetup(val acidC: Double, val acidMl: Double, val baseC: Double?, val stepMl: Double, val targetPh: Double = 7.0, val tolPh: Double = 0.5) : Setup { override val lab get() = Lab.TITRATION }
data class ReactionSetup(val reactionId: String) : Setup { override val lab get() = Lab.REAKTION }
data class FizzSetup(val vinegarMl: Double, val maxSpoons: Int, val targetL: Double) : Setup { override val lab get() = Lab.BALLON }

data class MeltSetup(val things: List<Meltable>, val maxC: Double) : Setup { override val lab get() = Lab.SCHMELZEN }
data class HeatCurveSetup(val massG: Double, val startC: Double, val powerW: Double, val untilSteam: Boolean) : Setup { override val lab get() = Lab.ERHITZEN }
data class MixSetup(val a: Stuff, val massA: Double, val tA: Double, val b: Stuff, val massB: Double, val tB: Double,
                    /** einstellbare Masse von A (Schritt in g) und Zieltemperatur */
                    val adjustA: Double? = null, val targetC: Double? = null) : Setup { override val lab get() = Lab.MISCHEN }

enum class LightMode { ADD, SUB }
data class LightSetup(val mode: LightMode, val target: Rgb?) : Setup { override val lab get() = if (mode == LightMode.ADD) Lab.LICHT else Lab.FILTER }
data class PrismSetup(val dummy: Int = 0) : Setup { override val lab get() = Lab.PRISMA }

enum class SoundMode { STRIKE, BLOW, TUNE, MELODY, STRING }
data class SoundSetup(val mode: SoundMode, val target: Note? = null, val melody: List<Note> = emptyList(), val stringFrac: Double = 0.5) : Setup { override val lab get() = Lab.KLANG }

enum class NewtonMode { CART, PUSH, GLIDE }
/** CART: Wagen wird mit Kraft F gezogen (Massen- und Kraft-Auswahl). PUSH: zwei Skateboarder stoßen sich ab (Massenpaare). */
data class NewtonSetup(val mode: NewtonMode, val masses: List<Double> = emptyList(), val forces: List<Double> = emptyList(), val pairs: List<Pair<Double, Double>> = emptyList()) : Setup { override val lab get() = Lab.NEWTON }
data class FallSetup(val a: Faller, val b: Faller?, val planets: List<Planet>, val heightM: Double) : Setup { override val lab get() = Lab.FALLTURM }
data class PendulumSetup(val lengths: List<Double>, val massesKg: List<Double>, val amplitudeDeg: Double = 10.0, val targetT: Double? = null, val g: Double = Planet.ERDE.g) : Setup { override val lab get() = Lab.PENDEL }

/** Freies Labor: so startet jedes Gerät, wenn man ohne Auftrag forscht. */
object FreeLab {
    fun setup(l: Lab): Setup = when (l) {
        Lab.SCHWIMMEN -> FloatSetup(Liquid.entries.filter { it != Liquid.SPUELI }, Thing.entries)
        Lab.DICHTETURM -> TowerSetup(Liquid.entries)
        Lab.WIPPE -> LeverSetup(emptyList(), listOf(1.0, 2.0, 3.0, 5.0).map { Load(it, 0) }, LeverGoal.BALANCE)
        Lab.FLASCHENZUG -> PulleySetup(50.0, listOf(1, 2, 3, 4, 6))
        Lab.RAMPE -> RampSetup(50.0, 1.0, listOf(1.5, 2.0, 3.0, 4.0))
        Lab.STROM -> CircuitSetup(Layout.ladder(1, listOf(1, 1), 1), listOf(Slot(Part.SCHALTER_ZU), Slot(Part.LAMPE), Slot(Part.LAMPE), Slot(Part.KABEL)),
            Part.entries.filter { it != Part.LEER }, emptySet(), 4.5, CircuitGoal.MEASURE, adjustable = null)
        Lab.MAGNET -> MagnetSetup(MagnetItem.entries, poles = true)
        Lab.ROTKOHL -> PhSetup(Household.entries)
        Lab.TITRATION -> TitrationSetup(0.1, 10.0, 0.1, 1.0)
        Lab.REAKTION -> ReactionSetup(Reactions.all.first().id)
        Lab.BALLON -> FizzSetup(100.0, 6, 0.0)
        Lab.SCHMELZEN -> MeltSetup(Meltable.entries, 200.0)
        Lab.ERHITZEN -> HeatCurveSetup(100.0, -20.0, 500.0, true)
        Lab.MISCHEN -> MixSetup(Stuff.WASSER, 100.0, 80.0, Stuff.WASSER, 100.0, 20.0, adjustA = 10.0)
        Lab.LICHT -> LightSetup(LightMode.ADD, null)
        Lab.FILTER -> LightSetup(LightMode.SUB, null)
        Lab.PRISMA -> PrismSetup()
        Lab.SCHATTEN -> ShadowSetup()
        Lab.SPIEGEL -> MirrorSetup(targets = listOf(40.0, 70.0, 130.0), showAngles = true)
        Lab.BRECHUNG -> RefractSetup(Medium.LUFT, Medium.WASSER, RefractGoal.FREE)
        Lab.LINSE -> LensSetup(goal = LensGoal.FREE)
        Lab.SPULE -> CoilSetup(200, 3.0, false, adjustWindings = true, adjustVolts = true, adjustCore = true)
        Lab.MOTOR -> MotorSetup(MotorMode.FORCE)
        Lab.DYNAMO -> DynamoSetup(DynamoMode.MAGNET, 500, adjustWindings = true)
        Lab.TRAFO -> TrafoSetup(TrafoMode.RATIO)
        Lab.KLANG -> SoundSetup(SoundMode.BLOW)
        Lab.NEWTON -> NewtonSetup(NewtonMode.CART, listOf(1.0, 2.0, 4.0), listOf(5.0, 10.0, 20.0), listOf(30.0 to 30.0, 30.0 to 90.0))
        Lab.FALLTURM -> FallSetup(Faller.KUGEL, Faller.FEDER, Planet.entries, 10.0)
        Lab.PENDEL -> PendulumSetup(listOf(1.0), listOf(0.2, 1.0))
        Lab.TRENNEN -> SeparationSetup(Mixture("Sand-Salz-Wasser", "🏖️", setOf(Component.SAND, Component.SALZ, Component.WASSER)), free = true)
        Lab.CHROMATOGRAFIE -> ChromaSetup(Marker.entries)
        Lab.MALKASTEN -> PaintSetup(Paint.entries, null)
        Lab.GENETIK -> GeneticsSetup(Trait.ERBSE, "Aa", "Aa", choosable = true)
        Lab.OSMOSE -> OsmosisSetup(listOf(0.0, 0.5, 1.0, 2.0, 5.0, 10.0), onion = true)
        Lab.NACHWEIS -> FoodSetup(Food.entries, FoodTest.entries)
        Lab.ENZYM -> EnzymeSetup(listOf(5.0, 20.0, 37.0, 50.0, 80.0))
        Lab.WASSERPEST -> PhotoSetup(listOf(10.0, 15.0, 20.0, 30.0), co2Choice = true, darkChoice = true)
        Lab.RAEUBER -> EcoSetup(400.0, 15.0, compareNoLynx = true)
        Lab.MIKROSKOP -> MicroscopeSetup(Microbe.entries, null, lamp = true)
        Lab.KEIMUNG -> GerminationSetup(listOf(Dish("feucht, 22 °C", true, 22.0)))
        Lab.NAHRUNGSKETTE -> FoodChainSetup(0, energy = true, poison = true, remove = Organism.MAUS)
        Lab.PULS -> PulseSetup(PulseMode.ACTIVITY)
    }
}
