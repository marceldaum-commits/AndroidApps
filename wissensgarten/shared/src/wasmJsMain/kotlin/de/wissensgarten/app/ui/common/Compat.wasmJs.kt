@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.wissensgarten.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) { /* Web: Zurück über die Knöpfe der Oberfläche */ }

private fun segmentCount(s: String): Int = js("""(() => { try { const seg = new Intl.Segmenter('de', { granularity: 'grapheme' }); globalThis.__wgSeg = Array.from(seg.segment(s), x => x.segment); } catch (e) { globalThis.__wgSeg = Array.from(s); } return globalThis.__wgSeg.length; })()""")
private fun segmentAt(i: Int): String = js("globalThis.__wgSeg[i]")

actual fun graphemes(s: String): List<String> {
    if (s.isEmpty()) return emptyList()
    val n = segmentCount(s)
    return List(n) { segmentAt(it) }
}

actual fun ImageBitmap.encodePng(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)
actual fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
