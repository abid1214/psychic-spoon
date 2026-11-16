package app.solace.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.solace.companion.data.Insurance
import app.solace.companion.data.Provider
import app.solace.companion.data.Session
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
private fun EditorScaffold(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Delete",
    deleteWarning: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = { TextButton(onClick = onSave, enabled = canSave) { Text("Save") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
            if (onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(deleteLabel) }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("$deleteLabel?") },
            text = { Text(deleteWarning) },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // The whole row toggles, so the label is a tap target too.
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        prefix = { Text("$") },
        singleLine = true,
        isError = parseMoney(value) == null,
        supportingText = if (parseMoney(value) == null) ({ Text("Enter an amount like 25 or 25.50") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private val Durations = listOf(30, 45, 50, 60, 90)
private val ReminderChoices = listOf(-1 to "None", 15 to "15 min before", 60 to "1 hour before", 1440 to "1 day before")

@Composable
fun SessionEditor(
    initial: Session?,
    providers: List<Provider>,
    now: Long,
    onSave: (Session) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    // New sessions default to tomorrow at the next full hour, with the only provider preselected.
    val base = remember {
        initial ?: Session(
            providerId = providers.singleOrNull()?.id,
            startMillis = zoned(now).plusDays(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli(),
        )
    }
    var providerId by remember { mutableStateOf(base.providerId) }
    var date by remember { mutableStateOf<LocalDate>(zoned(base.startMillis).toLocalDate()) }
    var time by remember { mutableStateOf<LocalTime>(zoned(base.startMillis).toLocalTime()) }
    var duration by remember { mutableStateOf(base.durationMin) }
    var telehealth by remember { mutableStateOf(base.telehealth) }
    var location by remember { mutableStateOf(base.location) }
    var cost by remember { mutableStateOf(moneyFieldText(base.costCents)) }
    var paid by remember { mutableStateOf(base.paid) }
    var reminder by remember { mutableStateOf(base.reminderMin) }
    var notes by remember { mutableStateOf(base.notes) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val start = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    val costCents = parseMoney(cost)

    EditorScaffold(
        title = if (initial == null) "New session" else "Session",
        canSave = costCents != null,
        onSave = {
            onSave(
                base.copy(
                    providerId = providerId, startMillis = start, durationMin = duration,
                    telehealth = telehealth, location = location.trim(), costCents = costCents ?: 0,
                    paid = paid, reminderMin = reminder, notes = notes.trim(),
                )
            )
        },
        onClose = onClose,
        onDelete = initial?.let { { onDelete(it.id) } },
        deleteLabel = "Delete session",
        deleteWarning = "This removes the session, its notes and its cost from billing.",
    ) {
        FieldLabel("Provider")
        if (providers.isEmpty()) {
            Text(
                "No providers yet. Add one in Care team to link sessions to them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { p ->
                    FilterChip(
                        selected = providerId == p.id,
                        onClick = { providerId = if (providerId == p.id) null else p.id },
                        label = { Text(p.name.ifBlank { "Unnamed" }) },
                    )
                }
            }
        }

        FieldLabel("When")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) { Text(formatDay(start)) }
            OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) { Text(formatTime(start)) }
        }

        FieldLabel("Length")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (Durations + duration).distinct().sorted().forEach { d ->
                FilterChip(selected = duration == d, onClick = { duration = d }, label = { Text("$d min") })
            }
        }

        SwitchRow("Video session", telehealth) { telehealth = it }
        if (!telehealth) {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FieldLabel("Reminder")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderChoices.forEach { (min, label) ->
                FilterChip(selected = reminder == min, onClick = { reminder = min }, label = { Text(label) })
            }
        }

        MoneyField("Your cost (copay or fee)", cost) { cost = it }
        SwitchRow("Paid", paid) { paid = it }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(date))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = utcMillisToLocalDate(it) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
fun ProviderEditor(
    initial: Provider?,
    onSave: (Provider) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    val base = remember { initial ?: Provider() }
    var name by remember { mutableStateOf(base.name) }
    var specialty by remember { mutableStateOf(base.specialty) }
    var phone by remember { mutableStateOf(base.phone) }
    var email by remember { mutableStateOf(base.email) }
    var link by remember { mutableStateOf(base.videoLink) }

    EditorScaffold(
        title = if (initial == null) "Add provider" else "Provider",
        canSave = name.isNotBlank(),
        onSave = {
            onSave(
                base.copy(
                    name = name.trim(), specialty = specialty.trim(), phone = phone.trim(),
                    email = email.trim(), videoLink = link.trim(),
                )
            )
        },
        onClose = onClose,
        onDelete = initial?.let { { onDelete(it.id) } },
        deleteLabel = "Remove provider",
        deleteWarning = "Their sessions are kept, but will no longer show a provider.",
    ) {
        OutlinedTextField(
            name, { name = it }, label = { Text("Name") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            specialty, { specialty = it }, label = { Text("Role or specialty") },
            placeholder = { Text("Therapist, psychiatrist…") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            phone, { phone = it }, label = { Text("Phone") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            email, { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            link, { link = it }, label = { Text("Video link") },
            supportingText = { Text("The link you use to join video sessions with them.") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun InsuranceEditor(initial: Insurance, onSave: (Insurance) -> Unit, onClose: () -> Unit) {
    var plan by remember { mutableStateOf(initial.plan) }
    var memberId by remember { mutableStateOf(initial.memberId) }
    var groupId by remember { mutableStateOf(initial.groupId) }
    var deductible by remember { mutableStateOf(moneyFieldText(initial.deductibleCents)) }
    var met by remember { mutableStateOf(moneyFieldText(initial.metCents)) }
    val d = parseMoney(deductible)
    val m = parseMoney(met)

    EditorScaffold(
        title = "Insurance",
        canSave = d != null && m != null,
        onSave = {
            onSave(Insurance(plan.trim(), memberId.trim(), groupId.trim(), d ?: 0, m ?: 0))
        },
        onClose = onClose,
    ) {
        OutlinedTextField(
            plan, { plan = it }, label = { Text("Plan name") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(memberId, { memberId = it }, label = { Text("Member ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(groupId, { groupId = it }, label = { Text("Group number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        MoneyField("Annual deductible", deductible) { deductible = it }
        MoneyField("Deductible met so far", met) { met = it }
    }
}
