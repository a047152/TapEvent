package pt.umaia.tapevent.pages.approvals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.util.DateFormats

/** One event waiting for approval, with Approve / Reject buttons. */
@Composable
fun PendingEventCard(
    event: Event,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${event.organizerName.ifBlank { "Unknown organizer" }} · " +
                        "${DateFormats.shortDateTime(event.startAt)} · ${event.capacity} seats",
                style = MaterialTheme.typography.bodySmall
            )
            Text(event.location, style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(event.format) })
                AssistChip(onClick = {}, label = { Text(event.theme) })
            }

            Text(
                event.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onApprove, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
                    Text("Approve")
                }
                OutlinedButton(onClick = onReject, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
                    Text("Reject")
                }
            }
        }
    }
}
