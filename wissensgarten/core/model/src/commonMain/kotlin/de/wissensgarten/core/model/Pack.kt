package de.wissensgarten.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Ein Wissenspaket, wie es als JSON in content/assets/packs liegt. Siehe content/PACK_FORMAT.md. */
@Serializable
data class Pack(
    val id: String,
    val version: Int,
    val title: String,
    val subject: String,
    val grades: List<Int>,
    val emoji: String,
    val tier: String = Tier.FREE,
    val description: String = "",
    /** Erklär-Geschichte, die Brummi vor dem Start erzählt (3–5 Szenen) */
    val intro: List<IntroScene> = emptyList(),
    val cards: List<PackCard>,
    /** Nur Vorschau (Plus-Paket, noch nicht geladen): Titel, Erklärung und ein paar Beispielkarten */
    val preview: Boolean = false,
    /** Kartenzahl des vollen Pakets (bei Vorschauen) */
    val cardTotal: Int = 0,
) {
    val subjectInfo: Subject get() = Subject.of(subject)
    val isPlus: Boolean get() = tier == Tier.PLUS
    /** Kartenzahl für die Anzeige – bei Vorschauen die des vollen Pakets */
    val cardCount: Int get() = if (cardTotal > 0) cardTotal else cards.size
}

@Serializable
data class IntroScene(val img: String, val text: String)

@Serializable
data class PackCard(
    val id: String,
    val q: String,
    val a: String,
    /** Nur falsche Antworten. Vorhanden = Auswahl-Aufgabe. */
    val choices: List<String>? = null,
    val info: String? = null,
    /** Merkbild: 1–3 Emojis */
    val img: String? = null,
)

object Tier {
    const val FREE = "free"
    const val PLUS = "plus"
}

object PackJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun parse(text: String): Pack = json.decodeFromString(Pack.serializer(), text)
    /** Vorschau-Katalog der Plus-Pakete (Liste) */
    fun parseList(text: String): List<Pack> = json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Pack.serializer()), text)
}

/** Fächer mit Anzeigename, Symbol und Blütenfarbe. Neue Fächer einfach ergänzen. */
enum class Subject(val id: String, val label: String, val emoji: String, val hue: Float) {
    DEUTSCH("deutsch", "Deutsch", "📖", 10f),
    MATHE("mathe", "Mathe", "➗", 210f),
    SACHUNTERRICHT("sachunterricht", "Sachunterricht", "🌻", 45f),
    ENGLISCH("englisch", "Englisch", "💬", 340f),
    FRANZOESISCH("franzoesisch", "Französisch", "🥐", 300f),
    LATEIN("latein", "Latein", "🏛️", 30f),
    SPANISCH("spanisch", "Spanisch", "💃", 20f),
    BIOLOGIE("biologie", "Biologie", "🔬", 120f),
    CHEMIE("chemie", "Chemie", "⚗️", 50f),
    PHYSIK("physik", "Physik", "⚡", 260f),
    INFORMATIK("informatik", "Informatik", "💻", 190f),
    GESCHICHTE("geschichte", "Geschichte", "🏰", 25f),
    ERDKUNDE("erdkunde", "Erdkunde", "🌍", 170f),
    POLITIK("politik", "Politik", "⚖️", 230f),
    WIRTSCHAFT("wirtschaft", "Wirtschaft", "💶", 150f),
    ETHIK("ethik", "Ethik & Religion", "🕊️", 280f),
    MUSIK("musik", "Musik & Kunst", "🎵", 320f),
    KUNST("kunst", "Kunst", "🎨", 0f),
    ALLGEMEIN("allgemeinwissen", "Allgemeinwissen", "💡", 40f),
    EIGENE("eigene", "Eigene Karten", "✏️", 90f);

    companion object {
        fun of(id: String): Subject = entries.firstOrNull { it.id == id } ?: ALLGEMEIN
    }
}

/** Lernstufen. 1–13 = Klassen, 14 = Erwachsene / Studium / Beruf. */
object Levels {
    const val ADULT = 14
    val all = (1..14).toList()

    fun label(level: Int): String = when (level) {
        0 -> "Ohne Angabe"
        ADULT -> "Erwachsen"
        else -> "Klasse $level"
    }

    fun short(level: Int): String = if (level == ADULT) "Erw." else "$level"

    /** z. B. „Klasse 5–8“, „Klasse 3“, „Klasse 10 – Erwachsene“ */
    fun range(grades: List<Int>): String {
        if (grades.isEmpty()) return ""
        val lo = grades.min()
        val hi = grades.max()
        return when {
            lo == ADULT -> "Erwachsene"
            hi == ADULT && lo == ADULT -> "Erwachsene"
            hi == ADULT -> "ab Klasse $lo"
            lo == hi -> "Klasse $lo"
            else -> "Klasse $lo–$hi"
        }
    }

    /** Gruppen für Filter in der Samenhandlung. */
    enum class Band(val label: String, val range: IntRange) {
        GRUNDSCHULE("Grundschule", 1..4),
        MITTELSTUFE("Klasse 5–10", 5..10),
        OBERSTUFE("Oberstufe", 11..13),
        ERWACHSENE("Erwachsene", ADULT..ADULT),
    }

    /** Wie gut passt ein Paket zur Stufe? 0 = gar nicht, höher = besser. */
    fun fit(pack: Pack, level: Int): Int = when {
        level == 0 -> 1
        level in pack.grades -> 3
        pack.grades.any { kotlin.math.abs(it - level) == 1 } -> 2
        level == ADULT && pack.grades.any { it >= 10 } -> 2
        else -> 0
    }
}
