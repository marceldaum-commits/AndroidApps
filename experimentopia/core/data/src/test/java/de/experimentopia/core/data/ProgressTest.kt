package de.experimentopia.core.data

import androidx.test.core.app.ApplicationProvider
import de.experimentopia.core.model.Finished
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressTest {
    private val db = LabDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
    private var now = 1000L
    private val repo = ProgressRepository(db.results()) { now }

    @Test fun bestesErgebnisBleibtUndForscherbuchWaechst() = runTest {
        repo.record(Finished("w1", 3, "Nagel", "Nagel", true, 2, true, false))
        now = 2000
        repo.record(Finished("w1", 1, "beide", "Nagel", false, 0, false, false))
        val r = repo.results.first().single()
        assertEquals(3, r.stars); assertTrue("erste Vermutung zählt", r.firstGuessRight)
        assertEquals(2, r.plays); assertEquals(1000L, r.firstAt); assertEquals(2000L, r.lastAt)
        val j = repo.journal.first()
        assertEquals(2, j.size); assertEquals("beide", j.first().guess)   // neuester Eintrag zuerst
    }

    @Test fun zuruecksetzen() = runTest {
        repo.record(Finished("k1", 2, "a", "b", false, 1, true, false))
        repo.reset()
        assertTrue(repo.results.first().isEmpty()); assertTrue(repo.journal.first().isEmpty())
    }
}
