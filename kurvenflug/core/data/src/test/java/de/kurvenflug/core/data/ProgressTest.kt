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

    @Test fun clearRemovesEverything() = runTest {
        repo.record("2-1", 3, setOf(0), 1); repo.record("daily-5", 2, emptySet(), 2)
        repo.clear()
        assertTrue(repo.all.first().isEmpty())
    }

    /** Neu anfangen: Spielstand-Einstellungen weg, Ton und eigene Level bleiben. */
    @Test fun resetGameKeepsSoundAndOwnLevels() {
        val s = Settings(ApplicationProvider.getApplicationContext())
        s.setSound(false); s.saveMyLevel("KFL1-test"); s.setGrade(7); s.markIntroSeen(3); s.markWalkthrough("LINE")
        s.recordStorm(9); s.recordDuel(true); s.badgesSeen = setOf("boss"); s.setLook(1, 2, 1); s.skin = 2; s.detectiveBest = 6
        s.resetGame()
        assertNull(s.grade.value); assertFalse(s.gradeAsked.value)
        assertFalse(s.introSeen(3)); assertEquals(0, s.walkthroughsSeen("LINE"))
        assertEquals(0, s.stormBest.value); assertEquals(0, s.duelsPlayed.value); assertTrue(s.badgesSeen.isEmpty())
        assertEquals(Triple(0, 0, 0), s.look.value); assertEquals(0, s.skin); assertEquals(0, s.detectiveBest)
        // bleibt
        assertFalse(s.sound.value); assertEquals(listOf("KFL1-test"), s.myLevels.value)
        // auch nach Neustart (neu aus den Einstellungen gelesen)
        val again = Settings(ApplicationProvider.getApplicationContext())
        assertNull(again.grade.value); assertFalse(again.sound.value)
    }
}
