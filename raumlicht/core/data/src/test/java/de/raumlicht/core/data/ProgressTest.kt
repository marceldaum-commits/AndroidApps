package de.raumlicht.core.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressTest {
    @Test fun bestResultStays() = runTest {
        val db = StageDatabase.create(ApplicationProvider.getApplicationContext(), inMemory = true)
        val repo = ProgressRepository(db.progress()) { 42 }
        repo.record("1-1", 3, 1)
        repo.record("1-1", 1, 4)      // schlechterer Durchgang: Sterne bleiben, Versuche zählen weiter
        val p = repo.all.first().single()
        assertEquals(3, p.stars); assertEquals(5, p.attempts); assertEquals(42, p.solvedAt)
        db.close()
    }
}
