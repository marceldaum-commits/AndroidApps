package de.wissensgarten.app.ui.fx

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Merkt sich ein einmal gezeichnetes Bild („Sprite“). Hintergrund: Der Browser zeichnet jede Form (Pfade mit Rundungen, Blätter, Hügel)
 * in jedem Bild neu und kostet dafür auf vielen Geräten die Hauptzeit. Ein fertiges Bild kostet pro Bild nur einen Texturaufruf –
 * bewegt wird es dann nur noch durch Verschieben oder Drehen. Das Bild wird neu gezeichnet, sobald sich [key] oder die Größe ändert.
 */
class Sprite {
    private var key: Any? = null
    private var w = 0
    private var h = 0
    private var image: ImageBitmap? = null

    /** Bild in Größe [width] x [height] (Pixel); zeichnet nur, wenn sich [key] oder die Größe geändert hat */
    fun get(density: Density, width: Int, height: Int, key: Any?, draw: DrawScope.() -> Unit): ImageBitmap? {
        if (width < 1 || height < 1 || width > 4096 || height > 4096) return null
        image?.let { if (this.key == key && w == width && h == height) return it }
        val bmp = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bmp), androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()), draw)
        this.key = key; w = width; h = height; image = bmp
        return bmp
    }
}

/** Zeichnet [draw] über ein gemerktes Bild (siehe [Sprite]); ist kein Bild möglich (zu groß), wird direkt gezeichnet. */
fun DrawScope.drawSprite(sprite: Sprite, key: Any?, draw: DrawScope.() -> Unit) {
    if (Experiment.aus("sprite")) { draw(); return }
    val bmp = sprite.get(this, size.width.toInt(), size.height.toInt(), key, draw)
    if (bmp != null) drawImage(bmp) else draw()
}
