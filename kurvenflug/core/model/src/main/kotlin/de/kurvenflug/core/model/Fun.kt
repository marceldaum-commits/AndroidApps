package de.kurvenflug.core.model

import kotlin.random.Random

/** Volts Laune auf der Startseite. */
enum class Feeling { HAPPY, IDLE, SLEEPY, MISSED_YOU }

/** Was Volt beim Öffnen sagt – abhängig von Uhrzeit und wie lange man weg war. Nie vorwurfsvoll. */
object Greeting {
    fun home(hour: Int, daysAway: Int, stars: Int): Pair<Feeling, String> = when {
        stars == 0 -> Feeling.IDLE to "Hey! Ich bin Volt. Bereit für deine erste Mission? 🚀"
        daysAway >= 3 -> Feeling.MISSED_YOU to "Da bist du ja wieder! Ich hab dich vermisst. 🤗 Schön, dass du da bist!"
        hour >= 21 || hour < 6 -> Feeling.SLEEPY to "Gäähn … schon ganz schön spät. 😴 Eine kurze Runde – und dann ab ins Bett?"
        hour < 9 -> Feeling.HAPPY to "Guten Morgen, Commander! ☀️ Schon $stars ⭐ gesammelt."
        else -> Feeling.HAPPY to "Schon $stars ⭐ gesammelt. Weiter so, Commander!"
    }
}

enum class Rarity(val label: String, val weight: Int) { COMMON("Normal", 70), RARE("Selten", 25), EPIC("Super-selten", 5) }

data class Sticker(val id: String, val emoji: String, val name: String, val rarity: Rarity)

/** Ein gezogener Sticker: [glitter] = seltene Glitzer-Variante, [new] = noch nicht im Album. */
data class StickerDrop(val sticker: Sticker, val glitter: Boolean, val new: Boolean)

/**
 * Weltraum-Sticker als Überraschung nach geschafften Leveln – ohne Kauf, ohne Druck.
 * Mit 3 ⭐ ist die Chance höher; seltene Sticker und Glitzer-Varianten machen das Sammeln spannend.
 */
object Stickers {
    val all = listOf(
        Sticker("rocket", "🚀", "Rakete", Rarity.COMMON), Sticker("star", "⭐", "Sternchen", Rarity.COMMON),
        Sticker("moon", "🌙", "Mondsichel", Rarity.COMMON), Sticker("comet", "☄️", "Komet", Rarity.COMMON),
        Sticker("planet", "🪐", "Ringplanet", Rarity.COMMON), Sticker("sat", "🛰️", "Satellit", Rarity.COMMON),
        Sticker("ufo", "🛸", "Ufo", Rarity.COMMON), Sticker("earth", "🌍", "Erde", Rarity.COMMON),
        Sticker("sun", "☀️", "Sonne", Rarity.COMMON), Sticker("bolt", "⚡", "Energiezelle", Rarity.COMMON),
        Sticker("scope", "🔭", "Teleskop", Rarity.COMMON), Sticker("astro", "🧑‍🚀", "Astronaut", Rarity.COMMON),
        Sticker("alien", "👽", "Alien", Rarity.RARE), Sticker("robot", "🤖", "Robo-Freund", Rarity.RARE),
        Sticker("galaxy", "🌌", "Galaxie", Rarity.RARE), Sticker("meteor", "🌠", "Sternschnuppe", Rarity.RARE),
        Sticker("crystal", "💎", "Weltraumkristall", Rarity.RARE), Sticker("blackhole", "🕳️", "Schwarzes Loch", Rarity.RARE),
        Sticker("compass", "🧭", "Sternenkompass", Rarity.RARE), Sticker("dragon", "🐉", "Nebeldrache", Rarity.EPIC),
        Sticker("unicorn", "🦄", "Space-Einhorn", Rarity.EPIC), Sticker("crown", "👑", "Sternenkrone", Rarity.EPIC),
        Sticker("rainbow", "🌈", "Polarlicht", Rarity.EPIC), Sticker("pi", "🥧", "Pi-Kuchen", Rarity.EPIC),
    )

    const val GLITTER_CHANCE = 0.1

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Gibt es diesmal einen Sticker? 3 ⭐: 60 %, sonst 30 %. Seltenheit nach Gewicht, 10 % Glitzer. */
    fun roll(r: Random, stars: Int, owned: Set<String>): StickerDrop? {
        if (stars <= 0 || r.nextDouble() >= if (stars >= 3) 0.6 else 0.3) return null
        var pick = r.nextInt(Rarity.entries.sumOf { it.weight })
        val rarity = Rarity.entries.first { pick -= it.weight; pick < 0 }
        val s = all.filter { it.rarity == rarity }.random(r)
        val glitter = r.nextDouble() < GLITTER_CHANCE
        val key = key(s, glitter)
        return StickerDrop(s, glitter, key !in owned)
    }

    /** Speicher-Schlüssel im Album: „rocket“ bzw. „rocket*“ für Glitzer */
    fun key(s: Sticker, glitter: Boolean) = if (glitter) "${s.id}*" else s.id
}
