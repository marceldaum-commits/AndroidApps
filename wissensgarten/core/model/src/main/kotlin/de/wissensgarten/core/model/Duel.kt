package de.wissensgarten.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.zip.CRC32
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Duell per Code – ganz ohne Server.
 * Ein Code enthält: Paket, Zufallszahl (bestimmt Fragen und Antwort-Reihenfolge) und bisherige Ergebnisse.
 * Wer spielt, hängt sein Ergebnis an und schickt den neuen Code weiter.
 * Enthalten sind nur Profilname und Punkte – keine weiteren persönlichen Daten.
 */
@Serializable
data class Duel(
    @SerialName("p") val packId: String,
    @SerialName("v") val packVersion: Int,
    @SerialName("s") val seed: Int,
    @SerialName("r") val results: List<DuelResult> = emptyList(),
) {
    fun withResult(r: DuelResult): Duel = copy(results = results.filterNot { it.name.equals(r.name, ignoreCase = true) } + r)
    fun hasPlayed(name: String) = results.any { it.name.equals(name, ignoreCase = true) }
    val ranking: List<DuelResult> get() = results.sortedWith(compareByDescending<DuelResult> { it.score }.thenBy { it.millis })
}

@Serializable
data class DuelResult(
    @SerialName("n") val name: String,
    @SerialName("a") val avatar: String,
    @SerialName("c") val correct: Int,
    @SerialName("t") val millis: Long,
    @SerialName("k") val score: Int,
)

data class DuelQuestion(val cardId: String, val question: String, val answer: String, val options: List<String>, val info: String?)

object Duels {
    const val QUESTIONS = 10
    const val SECONDS = 15
    const val MAX_PLAYERS = 8
    private const val PREFIX = "WG1-"
    /** Ausweich-Seite + Android App Link. Code steht hinter # und wird nie an den Server gesendet. */
    const val LINK_BASE = "https://www.mathenachhilfe-online.com/duell/#"

    fun link(d: Duel) = LINK_BASE + encode(d)
    private val json = Json { ignoreUnknownKeys = true }

    fun new(pack: Pack, random: Random = Random.Default) = Duel(pack.id, pack.version, random.nextInt(1, Int.MAX_VALUE))

    /** Gleiche Zufallszahl → gleiche Fragen in gleicher Reihenfolge, auf jedem Gerät. */
    fun questions(pack: Pack, seed: Int): List<DuelQuestion> {
        val rnd = Random(seed)
        val cards = pack.cards.sortedBy { it.id }.shuffled(rnd).take(QUESTIONS)
        val allAnswers = pack.cards.map { it.a }.distinct()
        return cards.map { c ->
            val given = c.choices.orEmpty().filter { it != c.a }
            // auf 3 Ablenker auffüllen – ähnlich lange Antworten anderer Karten wirken glaubwürdiger
            // Zufallsanteil einmal pro Antwort ziehen (nicht pro Vergleich – sonst ist die Sortierung ungültig)
            val extra = allAnswers.filter { it != c.a && it !in given }
                .map { it to kotlin.math.abs(it.length - c.a.length) + rnd.nextInt(6) }
                .sortedBy { it.second }.map { it.first }
            val wrong = (given + extra).take(3)
            DuelQuestion(c.id, c.q, c.a, (wrong + c.a).distinct().shuffled(rnd), c.info)
        }
    }

    /** 100 Punkte pro richtiger Antwort + bis zu 50 Bonus für Schnelligkeit. */
    fun points(correct: Boolean, millis: Long): Int {
        if (!correct) return 0
        val left = (SECONDS * 1000 - millis).coerceAtLeast(0)
        return 100 + (50.0 * left / (SECONDS * 1000)).roundToInt()
    }

    fun encode(d: Duel): String {
        val bytes = json.encodeToString(Duel.serializer(), d.copy(results = d.results.takeLast(MAX_PLAYERS))).toByteArray()
        val crc = CRC32().apply { update(bytes) }.value.toString(36)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "." + crc
    }

    /** Findet einen Duell-Code auch mitten in einer weitergeleiteten Nachricht. Null, wenn kaputt. */
    fun decode(text: String): Duel? {
        val m = Regex("""WG1-([A-Za-z0-9_-]+)\.([0-9a-z]+)""").find(text) ?: return null
        return runCatching {
            val bytes = Base64.getUrlDecoder().decode(m.groupValues[1])
            val crc = CRC32().apply { update(bytes) }.value.toString(36)
            if (crc != m.groupValues[2]) return null
            json.decodeFromString(Duel.serializer(), String(bytes))
        }.getOrNull()
    }

    /** Text zum Teilen per WhatsApp & Co. */
    fun shareText(d: Duel, packTitle: String, me: DuelResult): String {
        val others = d.ranking.filter { it.name != me.name }
        val head = if (others.isEmpty()) "⚔️ ${me.name} fordert dich im Wissensgarten heraus!"
        else "⚔️ Duell-Stand im Wissensgarten:"
        val table = d.ranking.joinToString("\n") { "${it.avatar} ${it.name}: ${it.score} Punkte (${it.correct}/$QUESTIONS)" }
        return "$head\nThema: $packTitle\n$table\n\nSchaffst du mehr? Tipp auf den Link:\n${link(d)}"
    }
}
