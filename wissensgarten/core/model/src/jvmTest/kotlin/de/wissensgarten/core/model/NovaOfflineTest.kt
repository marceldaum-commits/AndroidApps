package de.wissensgarten.core.model

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaOfflineTest {
    /** Nachgebauter Speicher + Server */
    private class Fake(val fehlen: Set<String> = emptySet(), var netz: Boolean = true, val vorher: Set<String> = emptySet(),
                       val ohneOriginal: Set<String> = emptySet()) : NovaOfflineSpeicher {
        val offline = HashSet(vorher)
        val original = HashSet<String>()
        val urls = ArrayList<String>()
        val anfragen = ArrayList<String>()
        var gleichzeitig = 0; var maxGleichzeitig = 0
        var bremse: (suspend () -> Unit)? = null
        override suspend fun offlineHashes(q: NovaQualitaet) = (if (q == NovaQualitaet.BEST) original else offline).toSet()
        override suspend fun sichern(url: String, h: String, q: NovaQualitaet): Int {
            anfragen += h; urls += url
            gleichzeitig++; maxGleichzeitig = maxOf(maxGleichzeitig, gleichzeitig)
            try {
                bremse?.invoke() ?: yield()
                if (!netz) return -1
                if (h in fehlen) return 404
                if (q == NovaQualitaet.BEST) { if (h in ohneOriginal) return 404; original += h } else offline += h
                return 200
            } finally { gleichzeitig-- }
        }
        override suspend fun text(url: String): String? = null
        override suspend fun belegt() = offline.size * 1000L
        override suspend fun loeschen() { offline.clear(); original.clear() }
    }

    private val hashes = (1..20).map { Nova.hash("Satz $it", "de") }

    @Test fun groesseAusServerStandMinusVorhandenes() {
        val frei = NovaOffline.stand("""{"stimme": "nova", "texte": 7802, "vorhanden": 7802, "fehlend": 0, "bytes": 429111168, "zu_lang": 0}""")!!
        assertEquals(7802, frei.texte); assertEquals(429111168L, frei.bytes)
        // alles fehlt: Gesamtgröße (bis auf Rundung)
        assertEquals(429L, NovaOffline.mb(NovaOffline.restBytes(listOf(frei), 7802)))
        // die Hälfte liegt schon da: halbe Größe
        assertEquals(215L, NovaOffline.mb(NovaOffline.restBytes(listOf(frei), 3901)))
        assertEquals(0L, NovaOffline.restBytes(listOf(frei), 0))
        // mit Plus
        val plus = NovaOffline.stand("""{"texte": 1332, "vorhanden": 1332, "bytes": 95990016}""")
        assertEquals(525L, NovaOffline.mb(NovaOffline.restBytes(listOf(frei, plus), 7802 + 1332)))
    }

    @Test fun bestqualitaetGroesse() {
        val mit = NovaOffline.stand("""{"texte": 100, "vorhanden": 100, "bytes": 2000000, "bytes_original": 9000000}""")
        assertEquals(9_000_000L, NovaOffline.restBytes(listOf(mit), 100, NovaQualitaet.BEST))
        assertEquals(2_000_000L, NovaOffline.restBytes(listOf(mit), 100, NovaQualitaet.STANDARD))
        assertTrue(NovaOffline.genau(listOf(mit), NovaQualitaet.BEST))
        // ohne bytes_original: ca. 3 × Standard
        val ohne = NovaOffline.stand("""{"texte": 100, "vorhanden": 100, "bytes": 2000000}""")
        assertEquals(6_000_000L, NovaOffline.restBytes(listOf(ohne), 100, NovaQualitaet.BEST))
        assertFalse(NovaOffline.genau(listOf(ohne), NovaQualitaet.BEST))
        assertTrue(NovaOffline.genau(listOf(ohne), NovaQualitaet.STANDARD))
    }

    @Test fun bestqualitaetMitRueckfallAufStandard() = runBlocking {
        val f = Fake(ohneOriginal = hashes.take(3).toSet())
        val r = NovaOffline.lade(hashes, f, NovaQualitaet.BEST)
        assertEquals(20, r.geladen); assertEquals(0, r.gibtEsNicht)
        assertEquals(17, f.original.size); assertEquals(3, f.offline.size)   // 3 nur als Standard
        assertTrue(f.urls.any { "/nova/original/" in it })
        assertEquals(Nova.url(hashes[0]), f.urls.first { hashes[0] in it && "/original/" !in it })
    }

    @Test fun ohneBytesWirdGeschaetzt() {
        val ohne = NovaOffline.stand("""{"texte": 100, "vorhanden": 100}""")!!
        assertNull(ohne.bytes)
        assertEquals(100 * NovaOffline.SCHAETZUNG_BYTES, NovaOffline.restBytes(listOf(ohne), 100))
        assertEquals(10 * NovaOffline.SCHAETZUNG_BYTES, NovaOffline.restBytes(listOf(null), 10))
        assertNull(NovaOffline.stand("kaputt"))
        assertNull(NovaOffline.stand(null))
        assertEquals(1L, NovaOffline.mb(1))
    }

    @Test fun listeLesenUndHashesWieDieApp() {
        val json = """{"format": 1, "app": "wissensgarten", "texte": [{"text": "Hallo Welt", "sprache": "de"}, {"text": "the dog", "sprache": "en"}, {"text": "Hallo Welt", "sprache": "de"}]}"""
        val h = NovaOffline.hashes(NovaOffline.liste(json))
        assertEquals(listOf("cb1e6d67882767042ebaa54d163e450c", Nova.hash("the dog", "en")), h)
    }

    @Test fun laedtNurFehlendeVierGleichzeitig() = runBlocking {
        val f = Fake(vorher = hashes.take(5).toSet())
        val progress = ArrayList<Pair<Int, Int>>()
        val r = NovaOffline.lade(hashes, f) { a, b -> progress += a to b }
        assertEquals(20, r.gesamt); assertEquals(5, r.vorhanden); assertEquals(15, r.geladen)
        assertEquals(15, f.anfragen.size)
        assertTrue(f.anfragen.none { it in hashes.take(5) })
        assertEquals(4, f.maxGleichzeitig)
        assertEquals(5 to 20, progress.first()); assertEquals(20 to 20, progress.last())
        assertFalse(r.offline)
    }

    @Test fun vierHundertVierWirdStillUebersprungen() = runBlocking {
        val f = Fake(fehlen = setOf(hashes[3], hashes[7]))
        val r = NovaOffline.lade(hashes, f)
        assertEquals(18, r.geladen); assertEquals(2, r.gibtEsNicht); assertEquals(0, r.netzfehler)
        assertFalse(r.offline)
        assertEquals(18, f.offline.size)
    }

    @Test fun nachAbbruchGehtEsDortWeiter() = runBlocking {
        val f = Fake()
        f.bremse = { delay(5) }
        var job: Job? = null
        job = launch {
            NovaOffline.lade(hashes, f) { done, _ -> if (done >= 8) job?.cancel() }
        }
        job.join()
        val nachAbbruch = f.offline.size
        assertTrue("nach Abbruch: $nachAbbruch", nachAbbruch in 8 until 20)
        val vorher = f.anfragen.size
        f.bremse = null
        val r = NovaOffline.lade(hashes, f)
        assertEquals(nachAbbruch, r.vorhanden)
        assertEquals(20 - nachAbbruch, f.anfragen.size - vorher)   // nur der Rest wird geladen
        assertEquals(20, f.offline.size)
    }

    @Test fun ohneNetzAbbrechenStattEndlosVersuchen() = runBlocking {
        val f = Fake(netz = false)
        val r = NovaOffline.lade(hashes, f)
        assertTrue(r.offline)
        assertTrue("Anfragen: ${f.anfragen.size}", f.anfragen.size < 20)
        assertEquals(0, r.geladen)
    }


    @Test fun bestQualityUsesServerOriginalSizeNotAMultiple() {
        // Server-Stand vom 17.11.2026, 17:46 (frei + Plus)
        val frei = NovaOffline.Stand(7802, 355_088_955, 429_111_168)
        val plus = NovaOffline.Stand(1332, 79_961_841, 95_990_016)
        val nurFrei = NovaOffline.mb(NovaOffline.restBytes(listOf(frei), 7802, NovaQualitaet.BEST))
        org.junit.Assert.assertEquals(429, nurFrei)
        val mitPlus = NovaOffline.mb(NovaOffline.restBytes(listOf(frei, plus), 7802 + 1332, NovaQualitaet.BEST))
        org.junit.Assert.assertEquals(525, mitPlus)
        org.junit.Assert.assertEquals(355, NovaOffline.mb(NovaOffline.restBytes(listOf(frei), 7802, NovaQualitaet.STANDARD)))
    }
}
