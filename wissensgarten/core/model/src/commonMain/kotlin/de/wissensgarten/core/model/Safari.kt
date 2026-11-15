package de.wissensgarten.core.model

/**
 * Wissens-Safari: Das Handy erkennt Dinge auf einem Foto (englische Begriffe der Bilderkennung).
 * Hier: kindgerechte Übersetzung mit Artikel und Merkbild-Emoji, und daraus Lernkarten.
 */
object Safari {
    data class Word(val en: String, val de: String, val emoji: String)

    /** Häufige Begriffe der Bilderkennung → deutsch mit Artikel, englisch fürs Lernen, Emoji */
    private val WORDS: Map<String, Word> = listOf(
        // Tiere
        "Dog" to ("der Hund" to "🐶"), "Cat" to ("die Katze" to "🐱"), "Bird" to ("der Vogel" to "🐦"), "Fish" to ("der Fisch" to "🐟"),
        "Horse" to ("das Pferd" to "🐴"), "Butterfly" to ("der Schmetterling" to "🦋"), "Insect" to ("das Insekt" to "🐞"),
        "Bear" to ("der Bär" to "🐻"), "Teddy bear" to ("der Teddybär" to "🧸"), "Rabbit" to ("das Kaninchen" to "🐰"), "Duck" to ("die Ente" to "🦆"),
        "Chicken" to ("das Huhn" to "🐔"), "Cow" to ("die Kuh" to "🐮"), "Pig" to ("das Schwein" to "🐷"), "Sheep" to ("das Schaf" to "🐑"),
        "Turtle" to ("die Schildkröte" to "🐢"), "Frog" to ("der Frosch" to "🐸"), "Bee" to ("die Biene" to "🐝"), "Snail" to ("die Schnecke" to "🐌"),
        "Spider" to ("die Spinne" to "🕷️"), "Pet" to ("das Haustier" to "🐾"), "Squirrel" to ("das Eichhörnchen" to "🐿️"), "Monkey" to ("der Affe" to "🐒"),
        "Elephant" to ("der Elefant" to "🐘"), "Lion" to ("der Löwe" to "🦁"), "Tiger" to ("der Tiger" to "🐯"), "Penguin" to ("der Pinguin" to "🐧"),
        // Essen
        "Food" to ("das Essen" to "🍽️"), "Fruit" to ("das Obst" to "🍎"), "Apple" to ("der Apfel" to "🍎"), "Banana" to ("die Banane" to "🍌"),
        "Bread" to ("das Brot" to "🍞"), "Cake" to ("der Kuchen" to "🎂"), "Cookie" to ("der Keks" to "🍪"), "Pizza" to ("die Pizza" to "🍕"),
        "Cheese" to ("der Käse" to "🧀"), "Egg" to ("das Ei" to "🥚"), "Vegetable" to ("das Gemüse" to "🥕"), "Carrot" to ("die Karotte" to "🥕"),
        "Tomato" to ("die Tomate" to "🍅"), "Strawberry" to ("die Erdbeere" to "🍓"), "Grape" to ("die Weintraube" to "🍇"), "Orange" to ("die Orange" to "🍊"),
        "Lemon" to ("die Zitrone" to "🍋"), "Ice cream" to ("das Eis" to "🍦"), "Candy" to ("die Süßigkeit" to "🍬"), "Juice" to ("der Saft" to "🥤"),
        "Milk" to ("die Milch" to "🥛"), "Coffee" to ("der Kaffee" to "☕"), "Tea" to ("der Tee" to "🍵"), "Sandwich" to ("das Sandwich" to "🥪"),
        "Pasta" to ("die Nudeln" to "🍝"), "Soup" to ("die Suppe" to "🍲"), "Cheeseburger" to ("der Burger" to "🍔"), "Hot dog" to ("der Hotdog" to "🌭"),
        // Zuhause
        "Cup" to ("die Tasse" to "☕"), "Mug" to ("der Becher" to "☕"), "Plate" to ("der Teller" to "🍽️"), "Bottle" to ("die Flasche" to "🍾"),
        "Spoon" to ("der Löffel" to "🥄"), "Fork" to ("die Gabel" to "🍴"), "Knife" to ("das Messer" to "🔪"), "Glass" to ("das Glas" to "🥛"),
        "Chair" to ("der Stuhl" to "💺"), "Table" to ("der Tisch" to "🍽️"), "Couch" to ("das Sofa" to "🛋️"), "Bed" to ("das Bett" to "🛏️"),
        "Door" to ("die Tür" to "🚪"), "Window" to ("das Fenster" to "🏠"), "Lamp" to ("die Lampe" to "💡"), "Clock" to ("die Uhr" to "⏰"),
        "Television" to ("der Fernseher" to "📺"), "Computer" to ("der Computer" to "💻"), "Laptop" to ("der Laptop" to "💻"),
        "Mobile phone" to ("das Handy" to "📱"), "Keyboard" to ("die Tastatur" to "⌨️"), "Book" to ("das Buch" to "📖"), "Paper" to ("das Papier" to "📄"),
        "Pen" to ("der Stift" to "🖊️"), "Pencil" to ("der Bleistift" to "✏️"), "Scissors" to ("die Schere" to "✂️"), "Bag" to ("die Tasche" to "👜"),
        "Backpack" to ("der Rucksack" to "🎒"), "Toy" to ("das Spielzeug" to "🧸"), "Ball" to ("der Ball" to "⚽"), "Balloon" to ("der Luftballon" to "🎈"),
        "Umbrella" to ("der Regenschirm" to "☂️"), "Key" to ("der Schlüssel" to "🔑"), "Towel" to ("das Handtuch" to "🧺"), "Sink" to ("das Waschbecken" to "🚰"),
        "Bathtub" to ("die Badewanne" to "🛁"), "Mirror" to ("der Spiegel" to "✨"), "Pillow" to ("das Kissen" to "🛏️"), "Blanket" to ("die Decke" to "🛏️"),
        "Shelf" to ("das Regal" to "📚"), "Curtain" to ("der Vorhang" to "🎭"), "Vase" to ("die Vase" to "🏺"), "Candle" to ("die Kerze" to "🕯️"),
        "Musical instrument" to ("das Musikinstrument" to "🎵"), "Guitar" to ("die Gitarre" to "🎸"), "Piano" to ("das Klavier" to "🎹"), "Drum" to ("die Trommel" to "🥁"),
        "Camera" to ("die Kamera" to "📷"), "Headphones" to ("die Kopfhörer" to "🎧"), "Painting" to ("das Bild" to "🖼️"), "Picture frame" to ("der Bilderrahmen" to "🖼️"),
        "Christmas" to ("Weihnachten" to "🎄"), "Gift" to ("das Geschenk" to "🎁"),
        // Kleidung
        "Shoe" to ("der Schuh" to "👟"), "Sneakers" to ("die Turnschuhe" to "👟"), "Hat" to ("der Hut" to "🎩"), "Cap" to ("die Mütze" to "🧢"),
        "Glasses" to ("die Brille" to "👓"), "Sunglasses" to ("die Sonnenbrille" to "🕶️"), "Jacket" to ("die Jacke" to "🧥"), "Jeans" to ("die Jeans" to "👖"),
        "Shirt" to ("das Hemd" to "👕"), "Dress" to ("das Kleid" to "👗"), "Sock" to ("die Socke" to "🧦"), "Scarf" to ("der Schal" to "🧣"),
        "Glove" to ("der Handschuh" to "🧤"), "Watch" to ("die Armbanduhr" to "⌚"),
        // Draußen
        "Flower" to ("die Blume" to "🌸"), "Plant" to ("die Pflanze" to "🌱"), "Tree" to ("der Baum" to "🌳"), "Leaf" to ("das Blatt" to "🍃"),
        "Grass" to ("das Gras" to "🌿"), "Garden" to ("der Garten" to "🏡"), "Sky" to ("der Himmel" to "🌤️"), "Cloud" to ("die Wolke" to "☁️"),
        "Sun" to ("die Sonne" to "☀️"), "Moon" to ("der Mond" to "🌙"), "Rainbow" to ("der Regenbogen" to "🌈"), "Snow" to ("der Schnee" to "❄️"),
        "Water" to ("das Wasser" to "💧"), "Lake" to ("der See" to "🏞️"), "River" to ("der Fluss" to "🏞️"), "Sea" to ("das Meer" to "🌊"),
        "Beach" to ("der Strand" to "🏖️"), "Mountain" to ("der Berg" to "⛰️"), "Rock" to ("der Stein" to "⛰️"), "Sand" to ("der Sand" to "🏖️"),
        "Fire" to ("das Feuer" to "🔥"), "Bonfire" to ("das Lagerfeuer" to "🔥"), "Park" to ("der Park" to "🌳"), "Playground" to ("der Spielplatz" to "🎠"),
        "Building" to ("das Gebäude" to "🏢"), "House" to ("das Haus" to "🏠"), "Bridge" to ("die Brücke" to "🌉"), "Road" to ("die Straße" to "🛣️"),
        "Car" to ("das Auto" to "🚗"), "Bicycle" to ("das Fahrrad" to "🚲"), "Bus" to ("der Bus" to "🚌"), "Train" to ("der Zug" to "🚆"),
        "Airplane" to ("das Flugzeug" to "✈️"), "Boat" to ("das Boot" to "⛵"), "Motorcycle" to ("das Motorrad" to "🏍️"), "Truck" to ("der Lastwagen" to "🚚"),
        "Tractor" to ("der Traktor" to "🚜"), "Wheel" to ("das Rad" to "⚙️"), "Fence" to ("der Zaun" to "🚧"),
        // Menschen & Körper
        "Hand" to ("die Hand" to "✋"), "Smile" to ("das Lächeln" to "😊"), "Baby" to ("das Baby" to "👶"), "Beard" to ("der Bart" to "🧔"),
        "Hair" to ("die Haare" to "💇"), "Eyelash" to ("die Wimper" to "👁️"), "Moustache" to ("der Schnurrbart" to "👨"),
    ).associate { (en, p) -> en.lowercase() to Word(en.lowercase(), p.first, p.second) }

    fun translate(label: String): Word? = WORDS[label.trim().lowercase()]

    /** Was taugt als Fundstück? Erkennungen ohne Übersetzung (z. B. „Event“, „Sitting“) fallen weg. */
    fun candidates(labels: List<Pair<String, Float>>, minConfidence: Float = 0.55f): List<Word> =
        labels.filter { it.second >= minConfidence }.sortedByDescending { it.second }.mapNotNull { translate(it.first) }.distinctBy { it.de }.take(3)

    /** Deutsch ohne Artikel („der Hund“ → „Hund“) */
    fun bare(de: String) = de.substringAfter(' ', de).takeIf { de.startsWith("der ") || de.startsWith("die ") || de.startsWith("das ") } ?: de

    /** Lernkarte zum Fundstück: jüngere Kinder lernen das deutsche Wort, ältere das englische. */
    fun card(w: Word, english: Boolean): ClassCard =
        if (english) ClassCard("Wie heißt „${bare(w.de)}“ auf Englisch?", w.en, w.emoji, "Selbst entdeckt auf deiner Safari 📷")
        else ClassCard("Was ist auf dem Foto?", w.de, w.emoji, "Selbst entdeckt auf deiner Safari 📷")
}
