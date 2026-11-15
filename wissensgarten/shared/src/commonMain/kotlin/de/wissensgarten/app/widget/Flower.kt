package de.wissensgarten.app.widget

/** Eine Blume pro Beet (Widget, Postkarte): Farbton, Gesundheit 0..1, schon gewachsen? */
data class Flower(val hue: Float, val health: Float, val grown: Boolean)
