package de.wissensgarten.app.session

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import de.wissensgarten.app.AppViewModel
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.data.db.BedEntity
import de.wissensgarten.core.data.db.BedWithCards
import de.wissensgarten.core.data.db.CardEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Beendet Android die App mitten in einer Gieß-Runde, geht es danach mit denselben Karten weiter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionRestoreTest {
    private val app = ApplicationProvider.getApplicationContext<WissensgartenApp>()
    private fun card(id: String, reps: Int = 1) = CardEntity(id = id, bedId = "b", profileId = "p", front = "F$id", back = "A$id", createdAt = 0, reps = reps)
    private val beds = listOf(BedWithCards(BedEntity("b", "p", "Englisch", "💬", 340f, null, 0, 0), listOf(card("c1"), card("c2"), card("c3", reps = 7))))

    @Test fun roundComesBackInSameOrderWithFreshProgress() {
        // So legt die laufende Runde ihren Zustand ab (Reihenfolge c3, c1 – c2 war nicht dabei)
        val saved = SavedStateHandle(mapOf("session_ids" to arrayListOf("c3", "c1"), "session_calm" to true, "session_mix" to "MATHE,ENGLISCH"))
        val vm = AppViewModel(app, saved)
        assertTrue(vm.hasSavedSession())

        val restored = vm.restoreSession(beds)
        assertEquals(listOf("c3", "c1"), restored.map { it.id })
        assertEquals(7, restored[0].memory.reps)   // aktueller Lernstand aus der Datenbank
        assertTrue(vm.sessionCalm)
        assertNotNull(vm.mixPair)
        assertFalse(vm.hasSavedSession())
    }

    @Test fun deletedCardsAreSkippedAndFinishedRoundIsForgotten() {
        val saved = SavedStateHandle(mapOf("session_ids" to arrayListOf("weg", "c2")))
        val vm = AppViewModel(app, saved)
        assertEquals(listOf("c2"), vm.restoreSession(beds).map { it.id })
        vm.endSession()
        assertEquals(emptyList<String>(), saved.get<ArrayList<String>>("session_ids"))
        assertFalse(AppViewModel(app, saved).hasSavedSession())
    }
}
