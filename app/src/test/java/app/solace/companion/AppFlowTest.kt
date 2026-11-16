package app.solace.companion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import app.solace.companion.data.AppData
import app.solace.companion.data.CheckIn
import app.solace.companion.data.Insurance
import app.solace.companion.data.Provider
import app.solace.companion.data.Session
import app.solace.companion.data.Store
import app.solace.companion.data.Topic
import app.solace.companion.data.appDataFromJson
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class AppFlowTest {
    private val now = ZonedDateTime.of(2026, 11, 16, 10, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val hour = 3_600_000L
    private val day = 24 * hour

    init {
        Clock.now = { now }
        Store.resetForTest()
        File(RuntimeEnvironment.getApplication().filesDir, "solace.json").delete()
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        Clock.now = { System.currentTimeMillis() }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("build/screenshots/$name.png")
    }

    private fun seed() {
        val rivera = Provider(name = "Dr. Maya Rivera", specialty = "Therapist, LCSW", phone = "555-0100", email = "maya@example.com", videoLink = "https://meet.example.com/rivera")
        val chen = Provider(name = "Dr. Alan Chen", specialty = "Psychiatrist", phone = "555-0111")
        rule.runOnUiThread { Store.update {
            AppData(
                providers = listOf(rivera, chen),
                sessions = listOf(
                    Session(providerId = rivera.id, startMillis = now + day + 5 * hour, telehealth = true, costCents = 2500),
                    Session(providerId = chen.id, startMillis = now + 9 * day, durationMin = 30, location = "200 Main St, Suite 4", costCents = 4000),
                    Session(providerId = rivera.id, startMillis = now - 6 * day, telehealth = true, costCents = 2500, notes = "Talked about sleep routine. Try the wind-down hour before the next session."),
                    Session(providerId = rivera.id, startMillis = now - 13 * day, telehealth = true, costCents = 2500, paid = true),
                ),
                insurance = Insurance("Evergreen PPO", "EVG-4471-0092", "88120", 150000, 62000),
                checkIns = listOf(3, 2, 3, 4, 4, 3, 5, 4).mapIndexed { i, m ->
                    CheckIn(timeMillis = now - (8 - i) * day, mood = m, note = if (i == 7) "Slept well, walked before work." else "")
                },
                topics = listOf(Topic(text = "Trouble sleeping on Sundays"), Topic(text = "Ask about session frequency", done = true)),
            )
        } }
        rule.waitForIdle()
    }

    @Test
    fun emptyAppThenAddProviderAndSession() {
        rule.onNodeWithText("Nothing scheduled").assertIsDisplayed()
        shot("01-home-empty")

        // Add a provider
        rule.onNodeWithText("Care team").performClick()
        rule.onNodeWithText("Add provider").performClick()
        rule.onNodeWithText("Name").performTextInput("Dr. Maya Rivera")
        rule.onNodeWithText("Video link").performTextInput("meet.example.com/rivera")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Dr. Maya Rivera").assertIsDisplayed()

        // Add a session; the only provider is preselected, default is tomorrow 10:00
        rule.onNodeWithText("Sessions").performClick()
        rule.onNodeWithText("New session").performClick()
        shot("02-session-editor")
        rule.onNodeWithText("Video session").performClick()
        rule.onNodeWithText("Your cost (copay or fee)").performScrollTo().performTextInput("25")
        rule.onNodeWithText("Save").performClick()

        val saved = Store.data.sessions.single()
        assertEquals(Store.data.providers.single().id, saved.providerId)
        assertEquals(now + day, saved.startMillis)
        assertEquals(2500L, saved.costCents)
        assertTrue(saved.telehealth)

        // Home shows it, and a reminder alarm is set one hour before
        rule.onNodeWithText("Home").performClick()
        rule.onNodeWithText("Join video").assertIsDisplayed()
        val app = RuntimeEnvironment.getApplication()
        val alarms = shadowOf(app.getSystemService(android.app.AlarmManager::class.java)).scheduledAlarms
        assertEquals(1, alarms.size)
        assertEquals(now + day - hour, alarms.single().triggerAtMs)

        // Mood tap and topic persist to disk
        rule.onNodeWithContentDescription("Feeling Good").performClick()
        rule.onNodeWithText("Something on your mind…").performScrollTo().performTextInput("Work stress")
        rule.onNodeWithContentDescription("Add topic").performClick()
        val onDisk = appDataFromJson(JSONObject(File(app.filesDir, "solace.json").readText()))
        assertEquals(Store.data, onDisk)
        assertEquals(4, onDisk.checkIns.single().mood)
        assertEquals("Work stress", onDisk.topics.single().text)

        // Deleting the session cancels its alarm
        rule.onNodeWithText("Details").performScrollTo().performClick()
        rule.onNodeWithText("Delete session").performScrollTo().performClick()
        rule.onNodeWithText("Delete").performClick()
        assertTrue(Store.data.sessions.isEmpty())
        assertEquals(0, shadowOf(app.getSystemService(android.app.AlarmManager::class.java)).scheduledAlarms.size)
    }

    @Test
    fun seededScreens() {
        seed()
        shot("03-home")
        rule.onNodeWithText("Tomorrow at 3:00", substring = true).assertIsDisplayed()
        rule.onNodeWithText("$25.00").assertIsDisplayed() // one unpaid past session

        rule.onNodeWithText("Sessions").performClick()
        shot("04-sessions")
        rule.onNodeWithText("Past").performClick()
        shot("05-sessions-past")

        rule.onNodeWithText("Care team").performClick()
        shot("06-care-team")

        rule.onNodeWithText("Billing").performClick()
        shot("07-billing")
        rule.onNodeWithText("Mark paid").performClick()
        assertEquals(0L, Store.data.dueCents(now))

        rule.onNodeWithText("Journal").performClick()
        shot("08-journal")
    }

    // A text field inside a dialog never goes idle under Robolectric at the phone-sized
    // configuration above (stock Material components reproduce it), so this runs at the default size.
    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h470dp-mdpi")
    fun journalCheckIn() {
        seed()
        rule.onNodeWithText("Journal").performClick()
        rule.onNodeWithText("Check in").performClick()
        rule.onNodeWithContentDescription("Feeling Great").performClick()
        rule.onNodeWithText("Note (optional)").performTextInput("Good day")
        rule.onNodeWithText("Save").performClick()
        assertEquals(9, Store.data.checkIns.size)
        assertEquals(5, Store.data.checkIns.last().mood)
        assertEquals("Good day", Store.data.checkIns.last().note)
    }
}
