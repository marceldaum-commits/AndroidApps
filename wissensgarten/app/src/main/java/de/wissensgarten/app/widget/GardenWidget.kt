package de.wissensgarten.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.wissensgarten.app.MainActivity
import de.wissensgarten.app.WissensgartenApp
import de.wissensgarten.core.data.GardenRepository
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.SessionBuilder
import de.wissensgarten.core.model.WidgetQuiz
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val KEY_FEEDBACK = stringPreferencesKey("fb")
private val KEY_FEEDBACK_AT = longPreferencesKey("fbAt")
private val KEY_CELEBRATE = longPreferencesKey("party")
private val KEY_LAST = stringPreferencesKey("last")
private val P_CARD = ActionParameters.Key<String>("card")
private val P_RIGHT = ActionParameters.Key<Boolean>("right")

/**
 * Lebendes Startbildschirm-Widget: ein kleiner Garten (Tageszeit-Himmel, Blumen je Beet – durstige lassen die Köpfe hängen)
 * und – wenn Platz ist – eine Mini-Frage mit zwei Antwortknöpfen. Richtig getippt: die Blumen jubeln, Tautropfen gibt's obendrauf.
 */
class GardenWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))
    override val stateDefinition = PreferencesGlanceStateDefinition

    /** Alles, was das Widget zeigt – wird nach jeder Antwort neu geladen. */
    private class Data(val garden: Bitmap, val thirsty: Int, val seeds: Int, val q: de.wissensgarten.core.model.WidgetQuestion?, val feedback: String?)

    private suspend fun load(context: Context, state: Preferences): Data {
        val c = (context.applicationContext as WissensgartenApp).container
        val now = System.currentTimeMillis()
        val profiles = c.repository.profiles.first()
        val profile = profiles.firstOrNull { it.id == c.settings.activeProfileId.value } ?: profiles.firstOrNull()
        val beds = profile?.let { p -> c.database.dao().bedsWithCardsOnce(p.id) }.orEmpty()
        val cards = beds.flatMap { it.cards }
        val thirsty = cards.count { Scheduler.isThirsty(it.memory, now) }
        val seeds = profile?.let { p ->
            minOf(SessionBuilder.seedsLeft(p.seedsPerDay, p.seedDay, p.seedsToday, now), cards.count { it.memory.isSeed })
        } ?: 0
        val feedback = state[KEY_FEEDBACK]?.takeIf { now - (state[KEY_FEEDBACK_AT] ?: 0) < 30 * 60_000L }
        val party = now - (state[KEY_CELEBRATE] ?: 0) < 2 * 60_000L
        // Gleich die nächste Frage – nur nicht dieselbe Karte noch einmal
        val last = state[KEY_LAST]
        val study = cards.map { it.toStudy() }
        val question = WidgetQuiz.pick(study.filter { it.id != last }, now) ?: WidgetQuiz.pick(study, now)
        // Blumen: eine pro Beet (max. 5), Gesundheit = Anteil nicht durstiger Karten
        val flowers = beds.sortedBy { it.bed.sortIndex }.take(5).map { b ->
            val n = b.cards.size.coerceAtLeast(1)
            Flower(b.bed.hue, 1f - b.cards.count { Scheduler.isThirsty(it.memory, now) }.toFloat() / n, b.cards.any { !it.memory.isSeed })
        }.ifEmpty { listOf(Flower(45f, 1f, false)) }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return Data(GardenBitmap.render(480, 240, flowers, hour, party), thirsty, seeds, question, feedback)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = load(context, getAppWidgetState(context, PreferencesGlanceStateDefinition, id))
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WATER, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)   // ohne CLEAR_TASK: ein offenes Spiel bleibt erhalten
        provideContent {
            // Läuft das Widget schon, kommt nach einer Antwort nur der neue Zustand an – dann hier neu laden.
            // (Früher wurde nur beim Start geladen: nach dem Antippen blieb die alte Frage stehen.)
            val state = currentState<Preferences>()
            var data by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(first) }
            androidx.compose.runtime.LaunchedEffect(state[KEY_FEEDBACK_AT], state[KEY_LAST]) { data = load(context, state) }
            GlanceTheme {
                val big = LocalSize.current.width >= LARGE.width && LocalSize.current.height >= 170.dp
                Content(big, data.garden, data.thirsty, data.seeds, data.q, data.feedback, open)
            }
        }
    }

    @Composable
    private fun Content(big: Boolean, garden: Bitmap, thirsty: Int, seeds: Int, q: de.wissensgarten.core.model.WidgetQuestion?, feedback: String?, open: Intent) {
        val fg = Color(0xFF1E3D17)
        Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(Color(0xFFFFFBF0)), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionStartActivity(open)), contentAlignment = Alignment.BottomCenter) {
                Image(ImageProvider(garden), "Dein Garten", GlanceModifier.fillMaxSize(), contentScale = androidx.glance.layout.ContentScale.Crop)
                Text(
                    if (thirsty > 0) "💧 $thirsty durstig" else if (seeds > 0) "🌱 $seeds Samen bereit" else "🌿 Alles grün",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)),
                    modifier = GlanceModifier.padding(bottom = 6.dp).background(Color(0x66000000)).cornerRadius(12.dp).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            if (big) Column(GlanceModifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (feedback != null) {
                    Text(feedback, maxLines = 2, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF2E7D32)), textAlign = TextAlign.Center))
                    Spacer(GlanceModifier.height(4.dp))
                }
                when {
                    q != null -> {
                        Text("🐝 " + q.card.front, maxLines = 2, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorProvider(fg), textAlign = TextAlign.Center))
                        Spacer(GlanceModifier.height(6.dp))
                        Row(GlanceModifier.fillMaxWidth()) {
                            q.options.forEachIndexed { i, o ->
                                if (i > 0) Spacer(GlanceModifier.width(8.dp))
                                Button(o, actionRunCallback<AnswerAction>(actionParametersOf(P_CARD to q.card.id, P_RIGHT to (i == q.correct))),
                                    GlanceModifier.defaultWeight(), maxLines = 2)
                            }
                        }
                    }
                    else -> Text("Säe neue Samen in der App – dann stellt Brummi hier Fragen!", style = TextStyle(fontSize = 13.sp, color = ColorProvider(fg), textAlign = TextAlign.Center))
                }
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val LARGE = DpSize(240.dp, 200.dp)
    }
}

/** Antwort im Widget: zählt wie eine richtige/falsche Antwort in der App (ohne Strafe – falsch heißt nur: bald nochmal). */
class AnswerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = (context.applicationContext as WissensgartenApp).container
        val id = parameters[P_CARD] ?: return
        val right = parameters[P_RIGHT] ?: false
        val card = c.database.dao().card(id) ?: return
        val rating = if (right) Rating.GOOD else Rating.AGAIN
        val dew = GardenRepository.baseDew(rating)
        c.repository.review(card.profileId, card.toStudy(), rating, dew)
        val now = System.currentTimeMillis()
        updateAppWidgetState(context, glanceId) { p ->
            p[KEY_FEEDBACK] = if (right) listOf("🎉 Richtig! +$dew 💧", "🌻 Super! +$dew 💧", "⭐ Stark! +$dew 💧").random()
                else "🌱 Fast! Richtig ist: ${card.back}"
            p[KEY_FEEDBACK_AT] = now
            if (right) p[KEY_CELEBRATE] = now
            p[KEY_LAST] = id
        }
        // dieses Widget sofort, weitere Widgets auf dem Startbildschirm ebenfalls
        GardenWidget().update(context, glanceId)
        GardenWidget().updateAll(context)
    }
}

class GardenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GardenWidget()
}

data class Flower(val hue: Float, val health: Float, val grown: Boolean)

/** Malt den Mini-Garten als Bild (Widgets können keine Compose-Zeichnungen). */
object GardenBitmap {
    fun render(w: Int, h: Int, flowers: List<Flower>, hour: Int, party: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val night = hour < 6 || hour >= 21
        val evening = hour in 18..20
        val (top, bottom) = when {
            night -> 0xFF1A237E.toInt() to 0xFF5C6BC0.toInt()
            evening -> 0xFFFF8A65.toInt() to 0xFFFFE0B2.toInt()
            else -> 0xFF64B5F6.toInt() to 0xFFE1F5FE.toInt()
        }
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        // Sonne oder Mond, Sterne
        if (night) {
            p.color = 0xFFFFF9C4.toInt(); cv.drawCircle(w * 0.82f, h * 0.22f, h * 0.09f, p)
            p.color = top; cv.drawCircle(w * 0.84f, h * 0.2f, h * 0.08f, p)
            p.color = 0xFFFFFFFF.toInt()
            val r = java.util.Random(7)
            repeat(18) { cv.drawCircle(r.nextFloat() * w, r.nextFloat() * h * 0.5f, 1.5f + r.nextFloat() * 1.5f, p) }
        } else {
            p.color = 0x55FFF59D; cv.drawCircle(w * 0.84f, h * 0.22f, h * 0.15f, p)
            p.color = 0xFFFFD54F.toInt(); cv.drawCircle(w * 0.84f, h * 0.22f, h * 0.1f, p)
            p.color = 0xEEFFFFFF.toInt()
            cv.drawOval(RectF(w * 0.12f, h * 0.14f, w * 0.32f, h * 0.26f), p)
            cv.drawOval(RectF(w * 0.2f, h * 0.09f, w * 0.34f, h * 0.24f), p)
        }
        // Hügel
        p.color = if (night) 0xFF33691E.toInt() else 0xFF8BC34A.toInt()
        val hill = Path().apply { moveTo(0f, h * 0.72f); quadTo(w * 0.5f, h * 0.55f, w.toFloat(), h * 0.74f); lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close() }
        cv.drawPath(hill, p)
        p.color = if (night) 0xFF558B2F.toInt() else 0xFF689F38.toInt()
        cv.drawRect(0f, h * 0.9f, w.toFloat(), h.toFloat(), p)
        // Blumen
        val n = flowers.size
        flowers.forEachIndexed { i, f ->
            val x = w * (i + 1f) / (n + 1f)
            val ground = h * 0.86f
            val tall = h * (if (f.grown) 0.42f else 0.26f)
            val droop = (1f - f.health).coerceIn(0f, 1f)
            val headX = x + droop * tall * 0.55f
            val headY = ground - tall + droop * tall * 0.45f
            p.style = Paint.Style.STROKE; p.strokeWidth = h * 0.025f; p.strokeCap = Paint.Cap.ROUND
            p.color = 0xFF2E7D32.toInt()
            cv.drawPath(Path().apply { moveTo(x, ground); quadTo(x, headY, headX, headY) }, p)
            p.style = Paint.Style.FILL
            p.color = 0xFF43A047.toInt()
            cv.drawOval(RectF(x - tall * 0.28f, ground - tall * 0.42f, x, ground - tall * 0.3f), p)
            val sat = 0.75f - droop * 0.45f
            val val_ = 0.98f - droop * 0.3f
            val petal = android.graphics.Color.HSVToColor(floatArrayOf(f.hue, sat, val_))
            val r = h * (if (f.grown) 0.075f else 0.05f) * (if (party) 1.2f else 1f)
            p.color = petal
            for (k in 0 until 6) {
                val a = k * PI / 3 + droop
                cv.drawCircle(headX + cos(a).toFloat() * r, headY + sin(a).toFloat() * r, r * 0.75f, p)
            }
            p.color = 0xFFFFC107.toInt(); cv.drawCircle(headX, headY, r * 0.7f, p)
            if (droop > 0.4f) { // Tropfen-Wunsch
                p.color = 0xFF29B6F6.toInt()
                cv.drawCircle(headX + r * 1.6f, headY - r * 1.8f, r * 0.35f, p)
                cv.drawPath(Path().apply { moveTo(headX + r * 1.6f, headY - r * 2.5f); lineTo(headX + r * 1.3f, headY - r * 1.9f); lineTo(headX + r * 1.9f, headY - r * 1.9f); close() }, p)
            }
        }
        // Brummi fliegt mit
        p.textSize = h * 0.16f
        cv.drawText("🐝", w * 0.06f + (hour % 4) * w * 0.05f, h * 0.48f, p)
        if (party) {
            val r = java.util.Random(hour.toLong())
            val cols = intArrayOf(0xFFE53935.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
            repeat(40) {
                p.color = cols[it % cols.size]
                val cx = r.nextFloat() * w; val cy = r.nextFloat() * h * 0.7f
                cv.save(); cv.rotate(r.nextFloat() * 90, cx, cy); cv.drawRect(cx, cy, cx + 8, cy + 4, p); cv.restore()
            }
        }
        return bmp
    }
}

/** Kleiner, runder Widget-Knopf. */
@Composable
private fun Button(label: String, onClick: androidx.glance.action.Action, modifier: GlanceModifier, maxLines: Int = 1) {
    Box(modifier.height(44.dp).cornerRadius(22.dp).background(Color(0xFFFFD54F)).clickable(onClick), contentAlignment = Alignment.Center) {
        Text(label, maxLines = maxLines, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF3E2C00)), textAlign = TextAlign.Center),
            modifier = GlanceModifier.padding(horizontal = 6.dp))
    }
}
