package de.raumlicht.app.ui

import de.raumlicht.app.ui.stage.StageController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Der Hinweis „3D-Bühne startet nicht“ darf nicht stehen bleiben, wenn die Bühne doch noch startet. */
class StageHintTest {
    @Test fun slowStartHintGoesAwayWhenTheStageArrives() {
        val c = StageController(); c.attach { }
        c.reportSlowStart(); assertEquals(StageController.SLOW_START, c.problem)
        c.onEvent("""{"type":"ready"}"""); assertNull(c.problem)
        c.reportSlowStart(); assertNull("läuft die Bühne, gibt es keinen Hinweis", c.problem)
        // echte Fehler der Bühne bleiben stehen
        c.onEvent("""{"type":"error","msg":"WebGL fehlt"}"""); c.onEvent("""{"type":"ready"}"""); assertEquals("WebGL fehlt", c.problem)
    }
}
