package de.wissensgarten.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import de.wissensgarten.app.R
import de.wissensgarten.app.ui.fx.Brummi
import de.wissensgarten.app.ui.fx.Mood
import de.wissensgarten.app.ui.settings.PrivacyText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** App-Symbol (512 × 512) für den Play Store. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w512dp-h512dp-mdpi")
class StoreIconTest {
    @get:Rule val rule = createComposeRule()
    @Test fun icon() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF4D6), Color(0xFFFBE7B5)))), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
            }
        }
        rule.onRoot().captureRoboImage("../store/icon-512.png")
    }

    /** Datenschutz-Webseite aus derselben Quelle wie der App-Bildschirm erzeugen. */
    @Test fun privacyPage() {
        File("../web/datenschutz").mkdirs()
        File("../web/datenschutz/index.html").writeText(PrivacyText.html())
    }
}

/** Titelbild (1024 × 500) für den Play Store. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1024dp-h500dp-mdpi")
class StoreFeatureGraphicTest {
    @get:Rule val rule = createComposeRule()
    @Test fun feature() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF8FD3F4), Color(0xFFCFEFFF), Color(0xFFB8E28A))))) {
                // Wiese mit Blumen
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFF7CC26B), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f))
                    val r = kotlin.random.Random(3)
                    repeat(40) {
                        val x = r.nextFloat() * size.width; val y = size.height * (0.76f + r.nextFloat() * 0.22f)
                        val c = listOf(Color(0xFFF48FB1), Color(0xFFFFF176), Color(0xFFB39DDB), Color.White)[it % 4]
                        drawLine(Color(0xFF3F6B3A), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, y - 18f), 3f)
                        drawCircle(c, 9f, androidx.compose.ui.geometry.Offset(x, y - 20f))
                    }
                }
                Row(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Wissensgarten", fontSize = 68.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E5A2B))
                        Text("Lernen, das wächst 🌱", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F6B3A))
                        Spacer(Modifier.height(10.dp))
                        Text("Jede Karte eine Pflanze · Gießen = Wiederholen · Ohne Werbung", fontSize = 20.sp, color = Color(0xFF22261F))
                    }
                    Brummi(Mood.CHEER, size = 260.dp)
                }
            }
        }
        rule.mainClock.advanceTimeBy(1200)
        rule.onRoot().captureRoboImage("../store/titelbild-1024x500.png")
    }
}
