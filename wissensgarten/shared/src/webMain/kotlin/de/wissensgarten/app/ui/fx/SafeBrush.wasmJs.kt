package de.wissensgarten.app.ui.fx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb

actual fun solidShader(color: Color): Shader = org.jetbrains.skia.Shader.makeColor(color.toArgb())

private fun consoleWarn(text: String): Unit = js("console.warn('[Wissensgarten] ' + text)")
actual fun drawNote(text: String) = consoleWarn(text)

/**
 * Selbsttest der Zeichenbibliothek (für das Start-Protokoll ?diag=1): Legt die Skia-Objekte an, die der Garten braucht,
 * und malt sie in ein kleines Bild im Speicher. Zeigt auf dem Gerät, ob die Bibliothek selbst richtig zeichnet.
 */
fun skiaSelbsttest(log: (String) -> Unit) {
    fun t(name: String, f: () -> String) = log("Skia $name: " + runCatching { f() }.getOrElse { "FEHLER " + it.toString().take(160) })
    fun hex(c: Int) = (c.toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')
    t("Farbe") { org.jetbrains.skia.Shader.makeColor(0xFF336699.toInt()); "ok" }
    t("Verlauf") { org.jetbrains.skia.Shader.makeLinearGradient(0f, 0f, 8f, 0f, intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt())); "ok" }
    t("Strichmuster") { org.jetbrains.skia.PathEffect.makeDash(floatArrayOf(4f, 4f), 0f); "ok" }
    t("Unschärfe") { org.jetbrains.skia.ImageFilter.makeBlur(3f, 3f, org.jetbrains.skia.FilterTileMode.CLAMP); "ok" }
    t("Malen im Speicher") {
        val bmp = org.jetbrains.skia.Bitmap()
        bmp.allocN32Pixels(8, 8)
        val c = org.jetbrains.skia.Canvas(bmp)
        c.clear(0xFFFFFFFF.toInt())
        c.drawRect(org.jetbrains.skia.Rect.makeXYWH(0f, 0f, 4f, 8f), org.jetbrains.skia.Paint().apply { color = 0xFF336699.toInt() })
        val p = org.jetbrains.skia.Paint().apply { shader = org.jetbrains.skia.Shader.makeLinearGradient(4f, 0f, 8f, 0f, intArrayOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt())) }
        c.drawRect(org.jetbrains.skia.Rect.makeXYWH(4f, 0f, 4f, 8f), p)
        "links ${hex(bmp.getColor(1, 4))} (soll ff336699), rechts ${hex(bmp.getColor(6, 4))} (soll ffff0000)"
    }
}
