package de.raumlicht.app.ui.fx

/** Zahl mit passendem Wort: `count(1, "Aufgabe", "Aufgaben")` → „1 Aufgabe“, sonst „n Aufgaben“. */
fun count(n: Int, one: String, many: String) = "$n " + if (n == 1) one else many
