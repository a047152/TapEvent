package pt.umaia.tapevent.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.util.DateFormats
import java.time.format.DateTimeFormatter

/**
 * Event summary used in lists (Discover, My events, organizer Dashboard).
 * Shown as a ticket in Phase 3.
 */
@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = event.startAt?.let(DateFormats::toZoned)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Date block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    start?.dayOfMonth?.toString() ?: "–",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    start?.format(MONTH_FORMAT)?.uppercase().orEmpty(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${DateFormats.time(event.startAt)} · ${event.location}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${event.format} · ${event.theme}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(12.dp))

            SeatsLabel(freeSeats = event.freeSeats, capacity = event.capacity)
        }
    }
}

@Composable
private fun SeatsLabel(freeSeats: Int, capacity: Int) {
    Column(horizontalAlignment = Alignment.End) {
        if (freeSeats > 0) {
            Text("$freeSeats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("de $capacity livres", style = MaterialTheme.typography.labelSmall)
        } else {
            Text(
                "Esgotado",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", DateFormats.LOCALE_PT)
