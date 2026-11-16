package app.solace.companion.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.solace.companion.Reminders
import org.json.JSONObject
import java.io.File

/**
 * Everything the app knows lives in one JSON file in the app's private storage.
 * Nothing is sent off the device.
 */
object Store {
    private const val FILE = "solace.json"
    private var appContext: Context? = null

    var data by mutableStateOf(AppData())
        private set

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val file = File(context.filesDir, FILE)
        if (file.exists()) {
            try {
                data = appDataFromJson(JSONObject(file.readText()))
            } catch (e: Exception) {
                // Keep the unreadable file aside rather than overwriting it.
                Log.e("Store", "Could not read saved data", e)
                file.copyTo(File(context.filesDir, "$FILE.corrupt"), overwrite = true)
            }
        }
    }

    fun update(transform: (AppData) -> AppData) {
        val old = data
        val new = transform(old)
        if (new == old) return
        data = new
        val context = appContext ?: return
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeText(new.toJson().toString())
        tmp.renameTo(File(context.filesDir, FILE))
        if (new.sessions != old.sessions) Reminders.sync(context, old.sessions, new.sessions)
    }

    /** Test hook: forget the loaded state so [init] reads from disk again. */
    fun resetForTest() {
        appContext = null
        data = AppData()
    }
}
