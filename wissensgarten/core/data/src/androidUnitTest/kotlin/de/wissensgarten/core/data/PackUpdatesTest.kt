package de.wissensgarten.core.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackUpdatesTest {
    private fun pack(id: String, v: Int, title: String = "T", cards: Int = 1) =
        """{"id":"$id","version":$v,"title":"$title","subject":"mathe","grades":[2],"emoji":"🔢","cards":[""" +
            (1..cards).joinToString(",") { """{"id":"$it","q":"q$it","a":"a$it"}""" } + "]}"

    @Test fun laedtNurNeuereUndNeuePakete() = runBlocking {
        val server = mapOf(
            "versions.json" to """{"alt.json":2,"neu.json":1,"gleich.json":3,"../boese.json":9}""",
            "alt.json" to pack("alt", 2), "neu.json" to pack("neu", 1), "gleich.json" to pack("gleich", 3),
        )
        val saved = mutableMapOf<String, String>()
        val n = PackUpdates(get = { server[it] }, save = { k, v -> saved[k] = v }).fetch(mapOf("alt" to 1, "gleich" to 3))
        assertEquals(2, n)
        assertEquals(setOf("alt.json", "neu.json"), saved.keys)
    }

    @Test fun lehntKaputteOderUnpassendePaketeAb() = runBlocking {
        val server = mapOf(
            "versions.json" to """{"a.json":2,"b.json":1,"c.json":1}""",
            "a.json" to pack("a", 1), // Version passt nicht
            "b.json" to "kein json",
            "c.json" to pack("anders", 1), // id passt nicht zum Dateinamen
        )
        val saved = mutableMapOf<String, String>()
        assertEquals(0, PackUpdates(get = { server[it] }, save = { k, v -> saved[k] = v }).fetch(emptyMap()))
        assertTrue(saved.isEmpty())
        assertNull(PackUpdates.validate(pack("x", 1, cards = 0), "x", 1))
    }

    @Test fun ohneServerPassiertNichts() = runBlocking {
        assertEquals(0, PackUpdates(get = { null }, save = { _, _ -> error("darf nicht speichern") }).fetch(emptyMap()))
        assertEquals(0, PackUpdates(get = { "[]" }, save = { _, _ -> error("darf nicht speichern") }).fetch(emptyMap()))
    }

    @Test fun katalogNimmtDieHoehereVersion() = runBlocking {
        val cat = PackCatalog(texts = { listOf(pack("a", 1, "Alt"), pack("b", 1)) }, extra = { listOf(pack("a", 2, "Neu", cards = 2), pack("c", 1)) })
        val all = cat.all()
        assertEquals(setOf("a", "b", "c"), all.map { it.id }.toSet())
        val a = all.first { it.id == "a" }
        assertEquals(2, a.version); assertEquals("Neu", a.title); assertEquals(2, a.cards.size)
        // Älterer Download verdrängt nie ein neueres mitgeliefertes Paket
        val cat2 = PackCatalog(texts = { listOf(pack("a", 3, "App")) }, extra = { listOf(pack("a", 2, "Download")) })
        assertEquals("App", cat2.all().single().title)
    }
}
