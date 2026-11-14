package de.wissensgarten.core.model

/**
 * Abenteuerpfad: Ein Beet wird in Stationen zu je 5 Karten geteilt.
 * Eine Station ist geschafft, wenn alle ihre Samen gekeimt sind.
 * Jede dritte Station hat eine Schatztruhe mit einem Gartenfreund.
 */
enum class StationState { DONE, CURRENT, LOCKED }

data class Station(val index: Int, val cardIds: List<String>, val state: StationState, val chest: Boolean, val thirsty: Int)

object AdventurePath {
    const val SIZE = 5
    const val CHEST_EVERY = 3

    fun stations(cards: List<StudyCard>, now: Long): List<Station> {
        var currentFound = false
        return cards.chunked(SIZE).mapIndexed { i, chunk ->
            val done = chunk.none { it.memory.isSeed }
            val state = when {
                done && !currentFound -> StationState.DONE
                !currentFound -> { currentFound = true; StationState.CURRENT }
                else -> StationState.LOCKED
            }
            Station(i, chunk.map { it.id }, state, (i + 1) % CHEST_EVERY == 0, chunk.count { Scheduler.isThirsty(it.memory, now) })
        }
    }

    /** Wie viele Truhen-Stationen sind in diesen Beeten geschafft? */
    fun chestsEarned(beds: List<List<StudyCard>>, now: Long): Int =
        beds.sumOf { b -> stations(b, now).count { it.chest && it.state == StationState.DONE } }

    /** Karten für eine Stations-Runde: alle Samen der Station + durstige Pflanzen der Station. */
    fun sessionFor(station: Station, cards: List<StudyCard>, now: Long): List<StudyCard> {
        val mine = cards.filter { it.id in station.cardIds }
        return mine.filter { Scheduler.isThirsty(it.memory, now) } + mine.filter { it.memory.isSeed }
    }
}

/** Sammelfiguren – „Gartenfreunde“. Reihenfolge fest: die nächste Figur ist immer vorher sichtbar (kein Glücksspiel). */
enum class Rarity(val label: String) { NORMAL("Freund"), RARE("Selten"), LEGENDARY("Legendär") }

data class Friend(val id: String, val emoji: String, val name: String, val text: String, val rarity: Rarity)

object Friends {
    val all = listOf(
        Friend("marie", "🐞", "Marie", "Bringt Glück beim Lernen.", Rarity.NORMAL),
        Friend("schnecki", "🐌", "Schnecki", "Langsam – aber vergisst nie etwas.", Rarity.NORMAL),
        Friend("ignaz", "🦔", "Ignaz Igel", "Stachelig außen, kuschelig innen.", Rarity.NORMAL),
        Friend("flatter", "🦋", "Flatter", "War mal eine Raupe – so wie dein Wissen mal ein Samen war.", Rarity.NORMAL),
        Friend("quaki", "🐸", "Quaki", "Springt vor Freude bei jeder richtigen Antwort.", Rarity.NORMAL),
        Friend("nussi", "🐿️", "Nussi", "Sammelt Wissen wie Nüsse für den Winter.", Rarity.NORMAL),
        Friend("turbo", "🐢", "Turbo", "Heißt so, weil er nie aufgibt.", Rarity.NORMAL),
        Friend("raupi", "🐛", "Raupi", "Frisst sich durch jedes Buch.", Rarity.NORMAL),
        Friend("hoppel", "🐇", "Hoppel", "Hoppelt von Beet zu Beet.", Rarity.NORMAL),
        Friend("erna", "🦆", "Ente Erna", "Schwimmt am liebsten im Teich.", Rarity.NORMAL),
        Friend("piep", "🐦", "Piep", "Singt dir morgens die Vokabeln vor.", Rarity.NORMAL),
        Friend("mausi", "🐭", "Mausi", "Klein, flink und superschlau.", Rarity.NORMAL),
        Friend("eule", "🦉", "Professorin Eule", "Weiß fast alles – und lernt trotzdem jeden Tag.", Rarity.RARE),
        Friend("fridolin", "🦊", "Fridolin Fuchs", "Findet für jede Aufgabe einen Trick.", Rarity.RARE),
        Friend("gustav", "🦎", "Gecko Gustav", "Klebt an jedem Thema dran.", Rarity.RARE),
        Friend("summsi", "🐝", "Summsi", "Brummis Cousine – die schnellste Biene im Garten.", Rarity.RARE),
        Friend("grilli", "🦗", "Grilli", "Zirpt ein Lied für jede Serie.", Rarity.RARE),
        Friend("dietrich", "🦡", "Dachs Dietrich", "Gräbt tief nach Wissen.", Rarity.RARE),
        Friend("rudi", "🦌", "Rudi Reh", "Schaut nachts im Garten vorbei.", Rarity.RARE),
        Friend("paula", "🦚", "Pfau Paula", "Zeigt stolz, was du schon kannst.", Rarity.RARE),
        Friend("paco", "🦜", "Papagei Paco", "Plappert alles nach – Wiederholung ist sein Geheimnis.", Rarity.LEGENDARY),
        Friend("elli", "🦄", "Einhorn Elli", "Taucht nur in ganz fleißigen Gärten auf.", Rarity.LEGENDARY),
        Friend("funki", "🐉", "Drache Funki", "Hütet den größten Wissensschatz.", Rarity.LEGENDARY),
        Friend("dora", "🦕", "Dino Dora", "So alt wie die Geschichte – und immer neugierig.", Rarity.LEGENDARY),
    )

    fun earned(count: Int) = all.take(count.coerceIn(0, all.size))
    fun next(count: Int) = all.getOrNull(count)
}
