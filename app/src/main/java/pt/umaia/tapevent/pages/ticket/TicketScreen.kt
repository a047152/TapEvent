package pt.umaia.tapevent.pages.ticket

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.Registration
import pt.umaia.tapevent.core.model.RegistrationStatus
import pt.umaia.tapevent.core.util.DateFormats
import pt.umaia.tapevent.ui.theme.TapEventTheme
import java.time.format.DateTimeFormatter

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun TicketScreen(
    eventId: String,
    onBack: () -> Unit,
    onCancelled: () -> Unit,
    onBrowseEvents: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TicketViewModel = viewModel(
        key = "ticket-$eventId",
        factory = TicketViewModel.factory(eventId)
    )
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.cancelled) {
        if (state.cancelled) onCancelled()
    }

    TicketContent(
        state = state,
        onBack = onBack,
        onCancel = viewModel::askToCancel,
        onBrowseEvents = onBrowseEvents,
        modifier = modifier
    )

    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCancel,
            title = { Text("Cancelar inscrição?") },
            text = { Text("A tua vaga fica livre para outro aluno.") },
            confirmButton = { TextButton(onClick = viewModel::confirmCancel) { Text("Cancelar inscrição") } },
            dismissButton = { TextButton(onClick = viewModel::dismissCancel) { Text("Manter") } }
        )
    }
}

/** Stateless UI: easy to preview and to restyle in Phase 3 (ticket shape). */
@Composable
fun TicketContent(
    state: TicketUiState,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onBrowseEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Voltar") }
        Text("A tua inscrição", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !state.hasTicket -> NoTicket(onBrowseEvents)
            else -> {
                TicketCard(state)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Mostra este código à entrada do evento.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.canCancel) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !state.isCancelling,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isCancelling) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Cancelar inscrição")
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(state: TicketUiState) {
    val event = state.event ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                event.startAt?.let { DateFormats.toZoned(it).format(TICKET_DATE) } ?: "Sem data",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            InfoRow("Local", event.location)
            InfoRow("Organiza", event.organizerName)
            InfoRow("Estado", statusLabel(state.registration?.status))

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val qr = state.qrCode
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "Código QR da inscrição",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            Text(
                state.registration?.id?.takeLast(8)?.uppercase().orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun NoTicket(onBrowseEvents: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Ainda não tens inscrição neste evento.\nEscolhe um evento para a ver aqui.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBrowseEvents) { Text("Ver eventos") }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.Start) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text(value.ifBlank { "—" })
    }
}

private fun statusLabel(status: String?): String = when (status) {
    RegistrationStatus.ATTENDED -> "Presença confirmada"
    RegistrationStatus.REGISTERED -> "Vaga garantida"
    else -> "—"
}

private val TICKET_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM · HH:mm", DateFormats.LOCALE_PT)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun TicketPreview() {
    TapEventTheme {
        TicketContent(
            state = TicketUiState(
                isLoading = false,
                event = Event(
                    id = "1",
                    title = "Workshop de Excel",
                    startAt = Timestamp.now(),
                    location = "Sala B204",
                    organizerName = "Associação de Estudantes"
                ),
                registration = Registration(id = "abc123_xyz789", status = RegistrationStatus.REGISTERED),
                canCancel = true
            ),
            onBack = {}, onCancel = {}, onBrowseEvents = {}
        )
    }
}
