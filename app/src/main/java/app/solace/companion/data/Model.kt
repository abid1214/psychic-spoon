package app.solace.companion.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

data class Provider(
    val id: String = newId(),
    val name: String = "",
    val specialty: String = "",
    val phone: String = "",
    val email: String = "",
    val videoLink: String = "",
)

data class Session(
    val id: String = newId(),
    val providerId: String? = null,
    val startMillis: Long = 0,
    val durationMin: Int = 50,
    val telehealth: Boolean = false,
    val location: String = "",
    val costCents: Long = 0,
    val paid: Boolean = false,
    val notes: String = "",
    /** Minutes before the start to notify; negative means no reminder. */
    val reminderMin: Int = 60,
) {
    val endMillis: Long get() = startMillis + durationMin * 60_000L
}

data class Insurance(
    val plan: String = "",
    val memberId: String = "",
    val groupId: String = "",
    val deductibleCents: Long = 0,
    val metCents: Long = 0,
) {
    val isEmpty: Boolean get() = this == Insurance()
}

data class CheckIn(
    val id: String = newId(),
    val timeMillis: Long = 0,
    /** 1 (rough) to 5 (great). */
    val mood: Int = 3,
    val note: String = "",
)

data class Topic(
    val id: String = newId(),
    val text: String = "",
    val done: Boolean = false,
)

data class AppData(
    val providers: List<Provider> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val insurance: Insurance = Insurance(),
    val checkIns: List<CheckIn> = emptyList(),
    val topics: List<Topic> = emptyList(),
) {
    fun provider(id: String?): Provider? = providers.firstOrNull { it.id == id }

    fun upcoming(now: Long): List<Session> =
        sessions.filter { it.endMillis >= now }.sortedBy { it.startMillis }

    fun past(now: Long): List<Session> =
        sessions.filter { it.endMillis < now }.sortedByDescending { it.startMillis }

    /** Unpaid cost of sessions that have already started. */
    fun dueCents(now: Long): Long =
        sessions.filter { !it.paid && it.startMillis <= now }.sumOf { it.costCents }
}

// ---- JSON ----

private fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> =
    (0 until length()).map { f(getJSONObject(it)) }

private fun <T> List<T>.toJsonArray(f: (T) -> JSONObject): JSONArray =
    JSONArray().also { arr -> forEach { arr.put(f(it)) } }

fun AppData.toJson(): JSONObject = JSONObject()
    .put("version", 1)
    .put("providers", providers.toJsonArray {
        JSONObject().put("id", it.id).put("name", it.name).put("specialty", it.specialty)
            .put("phone", it.phone).put("email", it.email).put("videoLink", it.videoLink)
    })
    .put("sessions", sessions.toJsonArray {
        JSONObject().put("id", it.id).put("providerId", it.providerId ?: JSONObject.NULL)
            .put("startMillis", it.startMillis).put("durationMin", it.durationMin)
            .put("telehealth", it.telehealth).put("location", it.location)
            .put("costCents", it.costCents).put("paid", it.paid).put("notes", it.notes)
            .put("reminderMin", it.reminderMin)
    })
    .put("insurance", JSONObject().put("plan", insurance.plan).put("memberId", insurance.memberId)
        .put("groupId", insurance.groupId).put("deductibleCents", insurance.deductibleCents)
        .put("metCents", insurance.metCents))
    .put("checkIns", checkIns.toJsonArray {
        JSONObject().put("id", it.id).put("timeMillis", it.timeMillis).put("mood", it.mood)
            .put("note", it.note)
    })
    .put("topics", topics.toJsonArray {
        JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)
    })

fun appDataFromJson(o: JSONObject): AppData {
    val ins = o.optJSONObject("insurance") ?: JSONObject()
    return AppData(
        providers = (o.optJSONArray("providers") ?: JSONArray()).mapObjects {
            Provider(
                id = it.getString("id"), name = it.optString("name"),
                specialty = it.optString("specialty"), phone = it.optString("phone"),
                email = it.optString("email"), videoLink = it.optString("videoLink"),
            )
        },
        sessions = (o.optJSONArray("sessions") ?: JSONArray()).mapObjects {
            Session(
                id = it.getString("id"),
                providerId = if (it.isNull("providerId")) null else it.getString("providerId"),
                startMillis = it.optLong("startMillis"),
                durationMin = it.optInt("durationMin", 50),
                telehealth = it.optBoolean("telehealth"),
                location = it.optString("location"),
                costCents = it.optLong("costCents"),
                paid = it.optBoolean("paid"),
                notes = it.optString("notes"),
                reminderMin = it.optInt("reminderMin", 60),
            )
        },
        insurance = Insurance(
            plan = ins.optString("plan"), memberId = ins.optString("memberId"),
            groupId = ins.optString("groupId"), deductibleCents = ins.optLong("deductibleCents"),
            metCents = ins.optLong("metCents"),
        ),
        checkIns = (o.optJSONArray("checkIns") ?: JSONArray()).mapObjects {
            CheckIn(
                id = it.getString("id"), timeMillis = it.optLong("timeMillis"),
                mood = it.optInt("mood", 3), note = it.optString("note"),
            )
        },
        topics = (o.optJSONArray("topics") ?: JSONArray()).mapObjects {
            Topic(id = it.getString("id"), text = it.optString("text"), done = it.optBoolean("done"))
        },
    )
}
