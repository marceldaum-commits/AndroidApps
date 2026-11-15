package de.wissensgarten.app.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.BreakIterator

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled, onBack)

actual fun graphemes(s: String): List<String> {
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val out = mutableListOf<String>()
    var start = it.first(); var end = it.next()
    while (end != BreakIterator.DONE) { out += s.substring(start, end); start = end; end = it.next() }
    return out
}

actual fun ImageBitmap.encodePng(): ByteArray = ByteArrayOutputStream().also { asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
actual fun decodeImage(bytes: ByteArray): ImageBitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
