package pt.umaia.tapevent.pages.myevents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.util.DateFormats

/** One of the student's events, with the status and the action that fits it. */
@Composable
fun MyEventCard(
    item: MyEventItem,
    isConfirming: Boolean,
    onOpen: () -> Unit,
    onConfirmAttendance: () -> Unit,
    onGiveFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                item.event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${DateFormats.shortDateTime(item.event.startAt)} · ${item.event.location}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                statusText(item),
                style = MaterialTheme.typography.labelMedium,
                color = if (item.attended) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                item.canConfirmAttendance -> {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onConfirmAttendance,
                        enabled = !isConfirming,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isConfirming) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Confirmar presença")
                    }
                }
                item.attended -> {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onGiveFeedback, modifier = Modifier.fillMaxWidth()) {
                        Text("Dar feedback")
                    }
                }
            }
        }
    }
}

private fun statusText(item: MyEventItem): String = when {
    item.attended -> "Presença confirmada"
    item.isNotifyRequest -> "À espera de vaga · vais ser avisado"
    item.hasStarted -> "Já começou · confirma a tua presença"
    else -> "Vaga garantida"
}
