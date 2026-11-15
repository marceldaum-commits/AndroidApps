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

    @Test fun resetClearsProgressButKeepsSettings() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = StageDatabase.create(ctx, inMemory = true); val repo = ProgressRepository(db.progress())
        repo.record("1-1", 3, 1); repo.record("2-1", 2, 2)
        repo.clear(); assertEquals(0, repo.all.first().size)
        val s = Settings(ctx)
        s.setSound(false); s.setPlan("plan"); s.recordJam(7); s.recordLage(6); s.recordExam("zelt", 11); s.setDiary("1\t2-1\tx"); s.setSkin("green"); s.setLook("rainbow", "crown"); s.markIntroSeen(3)
        s.resetProgress()
        assertEquals(0, s.jamBest.value); assertEquals(0, s.lageBest.value); assertEquals(emptyMap<String, Int>(), s.exams.value); assertEquals("", s.diary.value)
        assertEquals("pink", s.skin.value); assertEquals("none" to "none", s.look.value); assertEquals(false, s.introSeen(3))
        assertEquals(false, s.sound.value); assertEquals("plan", s.plan.value)                    // Einstellungen und Plan bleiben
        val again = Settings(ctx); assertEquals(0, again.jamBest.value); assertEquals(emptyMap<String, Int>(), again.exams.value)   // auch nach Neustart
        db.close()
    }
}
