package de.kurvenflug.core.data

import androidx.test.core.app.ApplicationProvider
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
    private val db = FlightDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
    private val repo = ProgressRepository(db.progress()) { 1000L }

    @Test fun bestResultIsKept() = runTest {
        repo.record("2-1", 3, setOf(0), 1)
        repo.record("2-1", 1, setOf(1), 4)   // schlechter nochmal gespielt
        val p = repo.all.first().single()
        assertEquals(3, p.stars)
        assertEquals(0b11, p.cellMask)       // Zellen aus beiden Runden
        assertEquals(5, p.attempts)
        assertEquals(1000L, p.solvedAt)
    }
}
