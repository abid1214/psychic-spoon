package app.solace.companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solace.companion.Clock
import app.solace.companion.data.AppData
import app.solace.companion.data.CheckIn
import app.solace.companion.data.Session
import app.solace.companion.data.Store
import app.solace.companion.data.Topic

@Composable
fun HomeScreen(
    data: AppData,
    now: Long,
    onNewSession: () -> Unit,
    onOpenSession: (Session) -> Unit,
    onOpenBilling: () -> Unit,
) {
    val context = LocalContext.current
    val next = data.upcoming(now).firstOrNull()
    val due = data.dueCents(now)
    val hour = zoned(now).hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(greeting, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                formatDayLong(now),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Next session
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("NEXT SESSION", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                if (next == null) {
                    Text("Nothing scheduled", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Add your next appointment and Solace will remind you before it starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewSession) { Text("Schedule a session") }
                } else {
                    val provider = data.provider(next.providerId)
                    val inProgress = next.startMillis <= now
                    Text(
                        if (inProgress) "Happening now" else "${relativeDay(next.startMillis, now)} at ${formatTime(next.startMillis)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        listOfNotNull(
                            formatDay(next.startMillis),
                            "${next.durationMin} min",
                            provider?.name?.takeIf { it.isNotBlank() }?.let { "with $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    val where = if (next.telehealth) "Video session" else next.location
                    if (where.isNotBlank()) {
                        Text(where, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val link = provider?.videoLink.orEmpty()
                        if (next.telehealth && link.isNotBlank()) {
                            Button(onClick = { context.openLink(link) }) { Text("Join video") }
                        }
                        OutlinedButton(
                            onClick = { onOpenSession(next) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) { Text("Details") }
                    }
                }
            }
        }

        // Balance
        if (due > 0) {
            SoftCard(onClick = onOpenBilling) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Balance due", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMoney(due), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Review", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Mood
        SectionTitle("How are you feeling?")
        MoodCard(data, now)

        // Topics
        SectionTitle("To bring up next session")
        TopicsCard(data.topics)

        // Crisis line
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Need to talk to someone now?", style = MaterialTheme.typography.titleSmall)
                    Text("Call or text 988, the US Suicide & Crisis Lifeline, any time.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(
                    onClick = { context.dial("988") },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                ) {
                    Icon(Icons.Default.Call, null)
                    Spacer(Modifier.width(6.dp))
                    Text("988")
                }
            }
        }
    }
}

@Composable
private fun MoodCard(data: AppData, now: Long) {
    val today = zoned(now).toLocalDate()
    val todays = data.checkIns.filter { zoned(it.timeMillis).toLocalDate() == today }.maxByOrNull { it.timeMillis }
    SoftCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Moods.forEach { m ->
                val selected = todays?.mood == m.value
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .semantics { contentDescription = "Feeling ${m.label}" + if (selected) ", selected" else "" }
                        .clickable {
                            Store.update { d ->
                                // One quick check-in per day from Home: tapping again changes today's answer.
                                if (todays != null) d.copy(checkIns = d.checkIns.map { if (it.id == todays.id) it.copy(mood = m.value) else it })
                                else d.copy(checkIns = d.checkIns + CheckIn(timeMillis = Clock.now(), mood = m.value))
                            }
                        },
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(m.emoji, fontSize = 26.sp)
                        Text(m.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (todays != null) {
            Text(
                "Logged for today. Add a note in Journal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun TopicsCard(topics: List<Topic>) {
    var draft by rememberSaveable { mutableStateOf("") }
    fun add() {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            Store.update { it.copy(topics = it.topics + Topic(text = text)) }
            draft = ""
        }
    }
    SoftCard {
        topics.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = t.done,
                    onCheckedChange = { checked ->
                        Store.update { d -> d.copy(topics = d.topics.map { if (it.id == t.id) it.copy(done = checked) else it }) }
                    },
                )
                Text(
                    t.text,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { Store.update { d -> d.copy(topics = d.topics.filterNot { it.id == t.id }) } }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove ${t.text}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("Something on your mind…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            trailingIcon = {
                IconButton(onClick = { add() }, enabled = draft.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Add topic")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = if (topics.isEmpty()) 0.dp else 8.dp),
        )
    }
}
