package de.experimentopia.core.model

/** Kreuzungen: feste Eltern (p1 × p2) oder Auswahl. blood = Blutgruppen statt Merkmal */
data class GeneticsSetup(val trait: Trait?, val p1: String, val p2: String, val choosable: Boolean = false, val goalRecessive: Double? = null) : Setup { override val lab get() = Lab.GENETIK }
data class OsmosisSetup(val salts: List<Double>, val onion: Boolean = false) : Setup { override val lab get() = Lab.OSMOSE }
data class FoodSetup(val foods: List<Food>, val tests: List<FoodTest>) : Setup { override val lab get() = Lab.NACHWEIS }
data class EnzymeSetup(val temps: List<Double>, val saliva: Boolean = true) : Setup { override val lab get() = Lab.ENZYM }
/** Fotosynthese: Abstände (cm), die gemessen werden sollen; CO₂-Zugabe wählbar */
data class PhotoSetup(val distances: List<Double>, val co2Choice: Boolean = false, val darkChoice: Boolean = false) : Setup { override val lab get() = Lab.WASSERPEST }
data class EcoSetup(val hares: Double, val lynx: Double, val compareNoLynx: Boolean = false) : Setup { override val lab get() = Lab.RAEUBER }
data class SeparationSetup(val mixture: Mixture, val free: Boolean = false) : Setup { override val lab get() = Lab.TRENNEN }
data class ChromaSetup(val markers: List<Marker>) : Setup { override val lab get() = Lab.CHROMATOGRAFIE }
data class PaintSetup(val paints: List<Paint>, val targetName: String?) : Setup { override val lab get() = Lab.MALKASTEN }
data class MicroscopeSetup(val microbes: List<Microbe>, val target: Microbe?, val lamp: Boolean = false) : Setup { override val lab get() = Lab.MIKROSKOP }
data class GerminationSetup(val dishes: List<Dish>, val days: Double = 7.0) : Setup { override val lab get() = Lab.KEIMUNG }
/** goalLength: Kette dieser Länge bauen; energy/poison: Energie oder Gift je Stufe zeigen; remove: dieses Tier kann man verschwinden lassen */
data class FoodChainSetup(val goalLength: Int, val energy: Boolean = false, val poison: Boolean = false, val remove: Organism? = null) : Setup { override val lab get() = Lab.NAHRUNGSKETTE }
enum class PulseMode { ACTIVITY, TAP, RECOVERY }
data class PulseSetup(val mode: PulseMode, val age: Int = 10) : Setup { override val lab get() = Lab.PULS }
