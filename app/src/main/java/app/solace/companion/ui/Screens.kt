package app.solace.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solace.companion.Clock
import app.solace.companion.data.AppData
import app.solace.companion.data.CheckIn
import app.solace.companion.data.Provider
import app.solace.companion.data.Session
import app.solace.companion.data.Store

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)

private fun setPaid(id: String, paid: Boolean) =
    Store.update { d -> d.copy(sessions = d.sessions.map { if (it.id == id) it.copy(paid = paid) else it }) }

// ---- Sessions ----

@Composable
fun SessionsScreen(data: AppData, now: Long, onOpen: (Session) -> Unit) {
    var showPast by rememberSaveable { mutableStateOf(false) }
    val list = if (showPast) data.past(now) else data.upcoming(now)

    Column {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("Upcoming", "Past").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = showPast == (i == 1),
                    onClick = { showPast = i == 1 },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }
        if (list.isEmpty()) {
            if (showPast) EmptyState("No past sessions", "Sessions move here once they end.")
            else EmptyState("No upcoming sessions", "Tap New session to add your next appointment.")
        } else {
            LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { s -> SessionCard(s, data.provider(s.providerId), now, onOpen) }
            }
        }
    }
}

@Composable
private fun SessionCard(s: Session, provider: Provider?, now: Long, onOpen: (Session) -> Unit) {
    SoftCard(onClick = { onOpen(s) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDayTime(s.startMillis), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        provider?.name?.takeIf { it.isNotBlank() },
                        "${s.durationMin} min",
                        if (s.telehealth) "Video" else s.location.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                relativeDay(s.startMillis, now),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (s.notes.isNotBlank()) {
            Text(
                s.notes,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---- Care team ----

@Composable
fun CareTeamScreen(data: AppData, onOpen: (Provider) -> Unit) {
    val context = LocalContext.current
    if (data.providers.isEmpty()) {
        EmptyState("No one here yet", "Add your therapist or prescriber to keep their contact details and video link in one place.")
        return
    }
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(data.providers, key = { it.id }) { p ->
            val count = data.sessions.count { it.providerId == p.id }
            SoftCard(onClick = { onOpen(p) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                p.specialty.takeIf { it.isNotBlank() },
                                if (count == 1) "1 session" else "$count sessions",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (p.phone.isNotBlank() || p.email.isNotBlank() || p.videoLink.isNotBlank()) {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (p.phone.isNotBlank()) {
                            AssistChip(onClick = { context.dial(p.phone) }, label = { Text("Call") }, leadingIcon = { Icon(Icons.Default.Call, null) })
                        }
                        if (p.email.isNotBlank()) {
                            AssistChip(onClick = { context.email(p.email) }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) })
                        }
                        if (p.videoLink.isNotBlank()) {
                            AssistChip(onClick = { context.openLink(p.videoLink) }, label = { Text("Video") }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                        }
                    }
                }
            }
        }
    }
}

// ---- Billing ----

@Composable
fun BillingScreen(data: AppData, now: Long, onEditInsurance: () -> Unit) {
    val year = zoned(now).year
    val billed = data.sessions.filter { it.costCents > 0 && it.startMillis <= now }.sortedByDescending { it.startMillis }
    val due = data.dueCents(now)
    val paidThisYear = data.sessions.filter { it.paid && zoned(it.startMillis).year == year }.sumOf { it.costCents }
    val ins = data.insurance

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftCard(Modifier.weight(1f)) {
                    Text("Due now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(due),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (due > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                SoftCard(Modifier.weight(1f)) {
                    Text("Paid in $year", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(paidThisYear), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            SectionTitle("Insurance")
        }
        item {
            SoftCard(onClick = onEditInsurance) {
                if (ins.isEmpty) {
                    Text("Add your insurance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keep your member ID handy and track your deductible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(ins.plan.ifBlank { "Insurance plan" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (ins.memberId.isNotBlank()) Text("Member ID  ${ins.memberId}", style = MaterialTheme.typography.bodyMedium)
                    if (ins.groupId.isNotBlank()) Text("Group  ${ins.groupId}", style = MaterialTheme.typography.bodyMedium)
                    if (ins.deductibleCents > 0) {
                        val fraction = (ins.metCents.toFloat() / ins.deductibleCents).coerceIn(0f, 1f)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text(
                            "${formatMoney(ins.metCents)} of ${formatMoney(ins.deductibleCents)} deductible met",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        item { SectionTitle("Charges") }
        if (billed.isEmpty()) {
            item { EmptyState("No charges yet", "Sessions with a cost show up here after they start.") }
        }
        items(billed, key = { it.id }) { s ->
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatMoney(s.costCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(formatDay(s.startMillis), data.provider(s.providerId)?.name?.takeIf { it.isNotBlank() }).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilterChip(
                        selected = s.paid,
                        onClick = { setPaid(s.id, !s.paid) },
                        label = { Text(if (s.paid) "Paid" else "Mark paid") },
                    )
                }
            }
        }
    }
}

// ---- Journal ----

@Composable
fun JournalScreen(data: AppData) {
    val entries = data.checkIns.sortedByDescending { it.timeMillis }
    if (entries.isEmpty()) {
        EmptyState("Your journal is empty", "Check in with how you feel. Over time you'll see how your mood moves between sessions.")
        return
    }
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { MoodChart(entries.take(14).reversed()) }
        items(entries, key = { it.id }) { c ->
            val mood = moodOf(c.mood)
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mood.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mood.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(formatDayTime(c.timeMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { Store.update { d -> d.copy(checkIns = d.checkIns.filterNot { it.id == c.id }) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete check-in", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (c.note.isNotBlank()) Text(c.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** One bar per check-in, oldest on the left; taller means a better mood. */
@Composable
private fun MoodChart(entries: List<CheckIn>) {
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val average = entries.map { it.mood }.average()
    SoftCard {
        Text("Recent mood", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Averaging ${moodOf(Math.round(average).toInt()).label.lowercase()} over your last ${entries.size} check-in${if (entries.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Canvas(
            Modifier.fillMaxWidth().height(72.dp).padding(top = 12.dp)
                .semantics { contentDescription = "Mood chart of the last ${entries.size} check-ins" },
        ) {
            val slots = 14
            val gap = 6.dp.toPx()
            val w = (size.width - gap * (slots - 1)) / slots
            val r = CornerRadius(w / 3, w / 3)
            for (i in 0 until slots) {
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), r)
                entries.getOrNull(i)?.let { e ->
                    val h = size.height * e.mood / 5f
                    drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), r)
                }
            }
        }
    }
}

@Composable
fun CheckInDialog(onDismiss: () -> Unit) {
    var mood by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Moods.forEach { m ->
                        OutlinedButton(
                            onClick = { mood = m.value },
                            contentPadding = PaddingValues(0.dp),
                            border = if (mood == m.value) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.width(48.dp).semantics { contentDescription = "Feeling ${m.label}" },
                        ) { Text(m.emoji, fontSize = 22.sp) }
                    }
                }
                Text("Feeling ${moodOf(mood).label.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Store.update { it.copy(checkIns = it.checkIns + CheckIn(timeMillis = Clock.now(), mood = mood, note = note.trim())) }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
