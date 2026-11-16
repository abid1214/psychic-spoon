package app.solace.companion

import app.solace.companion.data.AppData
import app.solace.companion.data.CheckIn
import app.solace.companion.data.Insurance
import app.solace.companion.data.Provider
import app.solace.companion.data.Session
import app.solace.companion.data.Topic
import app.solace.companion.data.appDataFromJson
import app.solace.companion.data.toJson
import app.solace.companion.ui.formatMoney
import app.solace.companion.ui.moneyFieldText
import app.solace.companion.ui.parseMoney
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogicTest {
    @Test
    fun moneyParsing() {
        assertEquals(0L, parseMoney(""))
        assertEquals(2500L, parseMoney("25"))
        assertEquals(2550L, parseMoney("25.5"))
        assertEquals(125000L, parseMoney("$1,250.00"))
        assertEquals(5L, parseMoney(".05"))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("1.234"))
        assertNull(parseMoney("."))
        assertNull(parseMoney("-5"))
        assertEquals("$1,250.05", formatMoney(125005))
        assertEquals("25.50", moneyFieldText(2550))
        assertEquals("25", moneyFieldText(2500))
        assertEquals("", moneyFieldText(0))
    }

    @Test
    fun jsonRoundTrip() {
        val p = Provider(name = "Dr. Rivera", specialty = "Therapist", phone = "555-0100", email = "r@example.com", videoLink = "meet.example.com/r")
        val data = AppData(
            providers = listOf(p),
            sessions = listOf(
                Session(providerId = p.id, startMillis = 1_000, telehealth = true, costCents = 2500, paid = true, notes = "n", reminderMin = -1),
                Session(providerId = null, startMillis = 2_000, durationMin = 30, location = "Suite 4"),
            ),
            insurance = Insurance("Plan", "M1", "G1", 150000, 42000),
            checkIns = listOf(CheckIn(timeMillis = 5, mood = 4, note = "ok")),
            topics = listOf(Topic(text = "sleep", done = true)),
        )
        assertEquals(data, appDataFromJson(JSONObject(data.toJson().toString())))
    }

    @Test
    fun balanceAndOrdering() {
        val now = 100_000_000L
        val hour = 3_600_000L
        val pastUnpaid = Session(startMillis = now - 48 * hour, costCents = 3000)
        val pastPaid = Session(startMillis = now - 24 * hour, costCents = 3000, paid = true)
        val inProgress = Session(startMillis = now - 10 * 60_000, costCents = 2000)
        val future = Session(startMillis = now + 24 * hour, costCents = 4000)
        val data = AppData(sessions = listOf(future, pastPaid, inProgress, pastUnpaid))
        assertEquals(5000L, data.dueCents(now))
        assertEquals(listOf(inProgress, future), data.upcoming(now))
        assertEquals(listOf(pastPaid, pastUnpaid), data.past(now))
    }
}
