package pt.umaia.tapevent.pages.eventdetails

import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.util.DateFormats
import pt.umaia.tapevent.ui.theme.TapEventTheme
import java.time.format.DateTimeFormatter

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun EventDetailsScreen(
    eventId: String,
    onBack: () -> Unit,
    onViewTicket: (eventId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailsViewModel = viewModel(
        key = "event-$eventId",
        factory = EventDetailsViewModel.factory(eventId)
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

    EventDetailsContent(
        state = state,
        onBack = onBack,
        onMainAction = {
            if (state.action == EventAction.VIEW_TICKET) onViewTicket(eventId) else viewModel.onMainAction()
        },
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun EventDetailsContent(
    state: EventDetailsUiState,
    onBack: () -> Unit,
    onMainAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = state.event
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        event == null -> Column(modifier.fillMaxSize().padding(20.dp)) {
            TextButton(onClick = onBack) { Text("‹ Voltar") }
            Text(state.errorMessage ?: "Evento não encontrado.", color = MaterialTheme.colorScheme.error)
        }
        else -> Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            EventHeader(event = event, onBack = onBack)

            Column(Modifier.padding(20.dp)) {
                SeatsInfo(event)

                Spacer(Modifier.height(20.dp))
                Text("Sobre o evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(event.description, style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))
                InfoRow("Local", event.location)
                InfoRow("Organiza", event.organizerName)
                InfoRow("Tema", event.theme)
                InfoRow("Formato", event.format)

                Spacer(Modifier.height(24.dp))
                MainActionButton(action = state.action, isWorking = state.isWorking, onClick = onMainAction)
            }
        }
    }
}

@Composable
private fun EventHeader(event: Event, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
            TextButton(onClick = onBack) { Text("‹ Voltar") }
            Column(Modifier.padding(start = 8.dp)) {
                Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    event.startAt?.let { DateFormats.toZoned(it).format(HEADER_DATE) } ?: "Sem data",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SeatsInfo(event: Event) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Vagas UMAIA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Text("${event.freeSeats} de ${event.capacity} livres", style = MaterialTheme.typography.titleSmall)
    }
    Text(
        if (event.freeSeats > 0) "Grátis para alunos UMAIA enquanto houver vagas."
        else "Vagas gratuitas esgotadas.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text(value.ifBlank { "—" })
    }
}

@Composable
private fun MainActionButton(action: EventAction, isWorking: Boolean, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth()
    when (action) {
        EventAction.STARTED -> Text(
            "Este evento já começou.",
            style = MaterialTheme.typography.bodyMedium
        )
        EventAction.NOTIFY_ME, EventAction.NOTIFY_REQUESTED -> {
            if (action == EventAction.NOTIFY_REQUESTED) {
                Text(
                    "Pediste para ser avisado se abrir uma vaga.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            OutlinedButton(onClick = onClick, enabled = !isWorking, modifier = modifier) {
                ButtonLabel(
                    if (action == EventAction.NOTIFY_ME) "Avisar-me se abrirem vagas" else "Cancelar aviso",
                    isWorking
                )
            }
        }
        EventAction.REGISTER -> Button(onClick = onClick, enabled = !isWorking, modifier = modifier) {
            ButtonLabel("Garantir vaga grátis", isWorking)
        }
        EventAction.VIEW_TICKET -> Button(onClick = onClick, modifier = modifier) {
            Text("Ver a minha inscrição")
        }
    }
}

@Composable
private fun ButtonLabel(text: String, isWorking: Boolean) {
    if (isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(text)
}

private val HEADER_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM · HH:mm", DateFormats.LOCALE_PT)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun EventDetailsPreview() {
    TapEventTheme {
        EventDetailsContent(
            state = EventDetailsUiState(
                isLoading = false,
                event = Event(
                    id = "1",
                    title = "Workshop de Excel",
                    description = "Aprende fórmulas, tabelas e gráficos. Traz o portátil.",
                    format = "Workshop",
                    theme = "Tecnologia",
                    startAt = Timestamp.now(),
                    location = "Sala B204",
                    organizerName = "Associação de Estudantes",
                    capacity = 30,
                    freeSeats = 12
                ),
                action = EventAction.REGISTER
            ),
            onBack = {}, onMainAction = {}
        )
    }
}
