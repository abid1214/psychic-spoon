package app.solace.companion

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.solace.companion.data.Provider
import app.solace.companion.data.Session
import app.solace.companion.data.Store
import app.solace.companion.ui.BillingScreen
import app.solace.companion.ui.CareTeamScreen
import app.solace.companion.ui.CheckInDialog
import app.solace.companion.ui.HomeScreen
import app.solace.companion.ui.InsuranceEditor
import app.solace.companion.ui.JournalScreen
import app.solace.companion.ui.ProviderEditor
import app.solace.companion.ui.SessionEditor
import app.solace.companion.ui.SessionsScreen
import app.solace.companion.ui.SolaceTheme
import kotlinx.coroutines.delay

/** Overridable so tests can pin "now". */
object Clock {
    var now: () -> Long = { System.currentTimeMillis() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        Reminders.createChannel(this)
        setContent { SolaceTheme { App() } }
    }
}

private sealed interface Editor {
    data class EditSession(val session: Session?) : Editor
    data class EditProvider(val provider: Provider?) : Editor
    data object EditInsurance : Editor
}

private data class Tab(val label: String, val title: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab("Home", "Solace", Icons.Default.Home),
    Tab("Sessions", "Sessions", Icons.Default.DateRange),
    Tab("Care team", "Care team", Icons.Default.Person),
    Tab("Billing", "Billing", Icons.Default.Info),
    Tab("Journal", "Journal", Icons.Default.Edit),
)

@Composable
fun App() {
    val data = Store.data
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<Editor?>(null) }
    var checkInOpen by remember { mutableStateOf(false) }
    val now by produceState(Clock.now()) {
        while (true) {
            delay(30_000)
            value = Clock.now()
        }
    }

    // Asked the first time a session with a reminder is saved, not at launch.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    when (val e = editor) {
        is Editor.EditSession -> {
            BackHandler { editor = null }
            SessionEditor(
                initial = e.session,
                providers = data.providers,
                now = now,
                onSave = { s ->
                    Store.update { d ->
                        if (d.sessions.any { it.id == s.id }) d.copy(sessions = d.sessions.map { if (it.id == s.id) s else it })
                        else d.copy(sessions = d.sessions + s)
                    }
                    if (s.reminderMin >= 0 && Build.VERSION.SDK_INT >= 33) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    editor = null
                },
                onDelete = { id ->
                    Store.update { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }
                    editor = null
                },
                onClose = { editor = null },
            )
            return
        }
        is Editor.EditProvider -> {
            BackHandler { editor = null }
            ProviderEditor(
                initial = e.provider,
                onSave = { p ->
                    Store.update { d ->
                        if (d.providers.any { it.id == p.id }) d.copy(providers = d.providers.map { if (it.id == p.id) p else it })
                        else d.copy(providers = d.providers + p)
                    }
                    editor = null
                },
                onDelete = { id ->
                    Store.update { d ->
                        d.copy(
                            providers = d.providers.filterNot { it.id == id },
                            sessions = d.sessions.map { if (it.providerId == id) it.copy(providerId = null) else it },
                        )
                    }
                    editor = null
                },
                onClose = { editor = null },
            )
            return
        }
        Editor.EditInsurance -> {
            BackHandler { editor = null }
            InsuranceEditor(
                initial = data.insurance,
                onSave = { ins ->
                    Store.update { it.copy(insurance = ins) }
                    editor = null
                },
                onClose = { editor = null },
            )
            return
        }
        null -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Tabs[tab].title) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar {
                Tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label, maxLines = 1) },
                    )
                }
            }
        },
        floatingActionButton = {
            // The content overload keeps the label readable by screen readers.
            val fab: Pair<String, () -> Unit>? = when (tab) {
                1 -> "New session" to { editor = Editor.EditSession(null) }
                2 -> "Add provider" to { editor = Editor.EditProvider(null) }
                4 -> "Check in" to { checkInOpen = true }
                else -> null
            }
            if (fab != null) {
                ExtendedFloatingActionButton(onClick = fab.second) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(fab.first)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    data = data,
                    now = now,
                    onNewSession = { editor = Editor.EditSession(null) },
                    onOpenSession = { editor = Editor.EditSession(it) },
                    onOpenBilling = { tab = 3 },
                )
                1 -> SessionsScreen(data, now, onOpen = { editor = Editor.EditSession(it) })
                2 -> CareTeamScreen(data, onOpen = { editor = Editor.EditProvider(it) })
                3 -> BillingScreen(data, now, onEditInsurance = { editor = Editor.EditInsurance })
                4 -> JournalScreen(data)
            }
        }
    }

    if (checkInOpen) CheckInDialog(onDismiss = { checkInOpen = false })
}
