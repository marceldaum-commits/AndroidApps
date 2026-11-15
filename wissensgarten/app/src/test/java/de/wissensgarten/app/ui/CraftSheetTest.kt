package de.wissensgarten.app.ui

import de.wissensgarten.app.ui.share.CraftSheet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Bastelbogen: Seite als Bild rendern (PdfDocument selbst gibt es in Robolectric nicht) und ablegen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CraftSheetTest {
    @Test fun rendersPage() {
        val items = listOf(
            CraftSheet.Item("Was ist 7 × 8?", "56", "🔢"),
            CraftSheet.Item("Hauptstadt von Frankreich?", "Paris", "🗼🥐"),
            CraftSheet.Item("the dog – der …?", "der Hund", "🐶"),
            CraftSheet.Item("Wie viele Beine hat eine Spinne?", "acht", "🕷️"),
            CraftSheet.Item("Wie heißt der größte Planet unseres Sonnensystems?", "Jupiter", "🪐"),
            CraftSheet.Item("Was ist die Mehrzahl von „Maus“?", "Mäuse", "🐭"),
            CraftSheet.Item("Siebte Karte – kommt auf Seite 2", "…", null),
        )
        val bmp = CraftSheet.preview("Mein Lernbeet", "🌻", items)
        assertEquals(1190, bmp.width)
        File("../screenshots").mkdirs()
        File("../screenshots/65_bastelbogen.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
