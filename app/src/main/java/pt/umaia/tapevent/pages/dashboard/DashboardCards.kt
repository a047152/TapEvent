package pt.umaia.tapevent.pages.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.util.DateFormats
import java.util.Locale

/** The two numbers at the top: published events and total registrations. */
@Composable
fun DashboardStats(published: Int, registered: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(published, "Publicados", Modifier.weight(1f))
        StatCard(registered, "Inscritos", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Pending or rejected event: only the title and a status line. */
@Composable
fun StatusEventCard(event: Event, statusText: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(DateFormats.shortDateTime(event.startAt), style = MaterialTheme.typography.bodySmall)
            Text(statusText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/** Approved event: occupancy before it starts, average rating after. */
@Composable
fun PublishedEventCard(
    item: PublishedEvent,
    onViewAttendees: () -> Unit,
    onViewFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = item.event
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${DateFormats.shortDateTime(event.startAt)} · ${event.theme}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))

            if (item.hasStarted) {
                Text(
                    "Realizado · " + (item.averageRating?.let {
                        "Média ${String.format(Locale.forLanguageTag("pt-PT"), "%.1f", it)} (${item.feedbackCount})"
                    } ?: "Sem feedback ainda"),
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                LinearProgressIndicator(
                    progress = { item.occupancy },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${item.registeredCount} de ${event.capacity} vagas ocupadas",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onViewAttendees, modifier = Modifier.weight(1f)) {
                    Text("Ver inscritos")
                }
                if (item.hasStarted) {
                    OutlinedButton(onClick = onViewFeedback, modifier = Modifier.weight(1f)) {
                        Text("Ver feedback")
                    }
                }
            }
        }
    }
}
