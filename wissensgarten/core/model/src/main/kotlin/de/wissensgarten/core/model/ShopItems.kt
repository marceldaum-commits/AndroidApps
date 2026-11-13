package de.wissensgarten.core.model

/** Was man im Gartenladen mit Tautropfen kaufen kann. Neue Artikel einfach ergänzen (id nie ändern). */
enum class ItemKind { SPECIES, DECO }

data class ShopItem(
    val id: String,
    val kind: ItemKind,
    val name: String,
    val description: String,
    val price: Int,
)

object ShopItems {
    const val DEFAULT_SPECIES = "klassik"

    val all: List<ShopItem> = listOf(
        // Pflanzenarten: verändern, wie Blüten und Bäume in einem Beet aussehen
        ShopItem(DEFAULT_SPECIES, ItemKind.SPECIES, "Wiesenblume", "Die Blume, mit der jeder Garten beginnt.", 0),
        ShopItem("sonnenblume", ItemKind.SPECIES, "Sonnenblume", "Groß, gelb und immer der Sonne zugewandt.", 80),
        ShopItem("tulpe", ItemKind.SPECIES, "Tulpe", "Schlanke Kelche in kräftigen Farben.", 100),
        ShopItem("kaktus", ItemKind.SPECIES, "Kaktus", "Robust – und blüht trotzdem.", 120),
        ShopItem("rose", ItemKind.SPECIES, "Rose", "Die Königin im Beet.", 150),
        ShopItem("tanne", ItemKind.SPECIES, "Tanne", "Bäume, die das ganze Jahr grün bleiben.", 200),
        ShopItem("kirschbaum", ItemKind.SPECIES, "Kirschbaum", "Rosa Blütenwolken im Frühling.", 250),
        ShopItem("zauberblume", ItemKind.SPECIES, "Zauberblume", "Leuchtet geheimnisvoll und funkelt.", 500),
        // Deko: erscheint im Garten oben
        ShopItem("zaun", ItemKind.DECO, "Gartenzaun", "Ein weißer Lattenzaun ums Grundstück.", 60),
        ShopItem("zwerg", ItemKind.DECO, "Gartenzwerg", "Passt auf, dass niemand schummelt.", 80),
        ShopItem("windrad", ItemKind.DECO, "Windrad", "Dreht sich, wenn der Wind weht.", 90),
        ShopItem("bank", ItemKind.DECO, "Gartenbank", "Für eine kleine Lernpause.", 100),
        ShopItem("vogelhaus", ItemKind.DECO, "Vogelhaus", "Ein gemütliches Zuhause für Meisen.", 120),
        ShopItem("teich", ItemKind.DECO, "Teich", "Mit Seerose und einem kleinen Frosch.", 150),
        ShopItem("laterne", ItemKind.DECO, "Laterne", "Leuchtet nachts warm im Garten.", 150),
        ShopItem("bienen", ItemKind.DECO, "Bienenstock", "Fleißige Bienen summen umher.", 200),
        ShopItem("regenbogen", ItemKind.DECO, "Regenbogen", "Spannt sich über den Himmel.", 400),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    val species get() = all.filter { it.kind == ItemKind.SPECIES }
    val decos get() = all.filter { it.kind == ItemKind.DECO }
}
