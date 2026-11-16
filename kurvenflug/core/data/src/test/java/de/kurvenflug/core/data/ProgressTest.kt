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
    private val profile = kotlinx.coroutines.flow.MutableStateFlow(0)
    private val repo = ProgressRepository(db.progress(), profile) { 1000L }

    @Test fun bestResultIsKept() = runTest {
        repo.record("2-1", 3, setOf(0), 1)
        repo.record("2-1", 1, setOf(1), 4)   // schlechter nochmal gespielt
        val p = repo.all.first().single()
        assertEquals(3, p.stars)
        assertEquals(0b11, p.cellMask)       // Zellen aus beiden Runden
        assertEquals(5, p.attempts)
        assertEquals(1000L, p.solvedAt)
    }

    /** Rechen-Stern: Sterne steigen, aber kein zweiter Eintrag im Eltern-Protokoll; ungespielte Level bleiben leer */
    @Test fun upgradeRaisesStarsWithoutNewPlay() = runTest {
        repo.record("14-2", 2, emptySet(), 1, 30)
        repo.upgrade("14-2", 3)
        repo.upgrade("14-2", 1)
        repo.upgrade("9-9", 3)
        assertEquals(3, repo.all.first().single().stars)
        assertEquals(1, repo.plays(0).first().size)
    }

    @Test fun clearRemovesEverything() = runTest {
        repo.record("2-1", 3, setOf(0), 1); repo.record("daily-5", 2, emptySet(), 2)
        repo.clear()
        assertTrue(repo.all.first().isEmpty())
    }

    /** Neu anfangen: Spielstand-Einstellungen weg, Ton und eigene Level bleiben. */
    @Test fun resetGameKeepsSoundAndOwnLevels() {
        val s = Settings(ApplicationProvider.getApplicationContext<android.content.Context>())
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
        val again = Settings(ApplicationProvider.getApplicationContext<android.content.Context>())
        assertNull(again.grade.value); assertFalse(again.sound.value)
    }

    /** Jeder Spieler hat seinen eigenen Spielstand; Löschen trifft nur den einen. */
    @Test fun profilesHaveSeparateProgress() = runTest {
        repo.record("2-1", 3, emptySet(), 1)
        profile.value = 1
        assertTrue(repo.all.first().isEmpty())
        repo.record("2-1", 1, emptySet(), 3); repo.record("3-1", 2, emptySet(), 2)
        assertEquals(2, repo.all.first().size)
        repo.clear()
        assertTrue(repo.all.first().isEmpty())
        profile.value = 0
        assertEquals(3, repo.all.first().single().stars)
        assertEquals(1, repo.plays(0).first().size)
    }

    @Test fun settingsArePerProfileButSoundIsShared() {
        val s = Settings(ApplicationProvider.getApplicationContext<android.content.Context>())
        s.setGrade(6); s.recordStorm(5); s.addSticker("rocket"); s.setSound(false)
        val lena = s.addProfile("Lena", Settings.AVATARS[2])!!
        s.switchProfile(lena.id)
        assertNull(s.grade.value); assertEquals(0, s.stormBest.value); assertTrue(s.stickers.value.isEmpty())
        assertFalse(s.sound.value)                                   // Ton gilt für alle
        s.setGrade(9); s.addSticker("ufo")
        s.resetGame()                                                // nur Lena
        assertNull(s.grade.value)
        s.switchProfile(0)
        assertEquals(6, s.grade.value); assertEquals(5, s.stormBest.value); assertEquals(setOf("rocket"), s.stickers.value)
        assertTrue(s.deleteProfile(lena.id))
        assertEquals(1, s.profiles.value.size)
        assertFalse(s.deleteProfile(0))                              // das letzte bleibt
        // nach Neustart: alles noch da
        val again = Settings(ApplicationProvider.getApplicationContext<android.content.Context>())
        assertEquals(6, again.grade.value); assertEquals(0, again.profile.value)
    }
}
