package de.wissensgarten.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Thorsten kommt vom eigenen Stimm-Server; App und WebApp hängen dieselben 127 Bytes an und prüfen dieselbe Prüfsumme. */
class ThorstenSourceTest {
    @Test fun serverFilePlusTailIsTheCheckedModel() {
        assertEquals(ThorstenFiles.SIZE, ThorstenFiles.SOURCE_SIZE + ThorstenFiles.SHERPA_TAIL.size)
        val tail = String(ThorstenFiles.SHERPA_TAIL, Charsets.ISO_8859_1)
        for (key in listOf("model_type", "sample_rate", "22050", "n_speakers")) assertTrue("$key fehlt", tail.contains(key))
        assertTrue(ThorstenFiles.URL.startsWith("https://stimmen.mathenachhilfe-online.com/"))
    }

    @Test fun webAppUsesTheSameSourceAndChecksum() {
        val js = File("../webApp/src/wasmJsMain/resources/wg-stimme.js").readText()
        assertTrue("Web: andere Quelle", js.contains("'${ThorstenFiles.URL}'"))
        assertTrue("Web: andere Größe", js.contains("SRC_SIZE = ${ThorstenFiles.SOURCE_SIZE};"))
        assertTrue("Web: anderer Anhang", js.contains("'${ThorstenFiles.SHERPA_TAIL_B64}'"))
        assertTrue("Web: andere Prüfsumme", js.contains("'${ThorstenFiles.SHA256}'"))
    }
}
