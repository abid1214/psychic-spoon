package app.solace.companion.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---- Formatting ----

fun zoned(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

fun formatTime(millis: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(zoned(millis))

fun formatDay(millis: Long): String =
    DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()).format(zoned(millis))

fun formatDayLong(millis: Long): String =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()).format(zoned(millis))

fun formatDayTime(millis: Long): String = "${formatDay(millis)} · ${formatTime(millis)}"

/** "Today", "Tomorrow", "In 3 days", "Yesterday", "5 days ago". */
fun relativeDay(millis: Long, now: Long): String {
    val days = ChronoUnit.DAYS.between(zoned(now).toLocalDate(), zoned(millis).toLocalDate())
    return when {
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days == -1L -> "Yesterday"
        days > 1 -> "In $days days"
        else -> "${-days} days ago"
    }
}

fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    return String.format(Locale.US, "%s$%,d.%02d", sign, abs / 100, abs % 100)
}

/** Text for a money field: "" for zero, otherwise "25" or "25.50". */
fun moneyFieldText(cents: Long): String = when {
    cents == 0L -> ""
    cents % 100 == 0L -> (cents / 100).toString()
    else -> String.format(Locale.US, "%d.%02d", cents / 100, cents % 100)
}

/** Parses "25", "25.5", "$1,250.00". Returns null when the text is not an amount. */
fun parseMoney(text: String): Long? {
    val t = text.trim().removePrefix("$").replace(",", "")
    if (t.isEmpty()) return 0
    if (!Regex("""\d{0,9}(\.\d{0,2})?""").matches(t) || t == ".") return null
    val parts = t.split(".")
    val dollars = parts[0].ifEmpty { "0" }.toLong()
    val cents = parts.getOrNull(1).orEmpty().padEnd(2, '0').toLong()
    return dollars * 100 + cents
}

fun localDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()

// ---- Mood ----

data class MoodOption(val value: Int, val emoji: String, val label: String)

val Moods = listOf(
    MoodOption(1, "😞", "Rough"),
    MoodOption(2, "😕", "Low"),
    MoodOption(3, "😐", "Okay"),
    MoodOption(4, "🙂", "Good"),
    MoodOption(5, "😄", "Great"),
)

fun moodOf(value: Int): MoodOption = Moods[(value.coerceIn(1, 5)) - 1]

// ---- Intents ----

private fun Context.launch(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app on this phone can open that", Toast.LENGTH_SHORT).show()
    }
}

fun Context.dial(phone: String) = launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))

fun Context.email(address: String) =
    launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(address)}")))

fun Context.openLink(link: String) {
    val url = if (link.contains("://")) link else "https://$link"
    launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

// ---- Small shared pieces ----

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val inner: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(16.dp), content = content) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, content = inner)
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, content = inner)
    }
}

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // Skip titles such as "Dr." so two doctors don't both show "D".
            (name.split(" ").firstOrNull { it.isNotBlank() && !it.endsWith(".") } ?: name.trim())
                .firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
