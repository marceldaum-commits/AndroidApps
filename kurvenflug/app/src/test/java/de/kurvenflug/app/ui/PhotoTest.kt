package de.kurvenflug.app.ui

import de.kurvenflug.app.ui.fx.Photo
import de.kurvenflug.core.model.Flight
import de.kurvenflug.core.model.Levels
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Fotomodus: Bild der Flugbahn wird ohne Absturz gezeichnet (und landet fürs README in screenshots/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PhotoTest {
    @Test fun rendersTheFlight() {
        val l = Levels.byId("3-6")!!
        val bmp = Photo.render(l, Flight.fly(l, l.solution).path, 3, l.solution, size = 720)
        assertEquals(720, bmp.width)
        java.io.File("../screenshots/60_foto.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
