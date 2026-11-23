package pt.umaia.tapevent.pages.attendees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.ui.Avatar
import pt.umaia.tapevent.core.util.DateFormats
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun AttendeesScreen(
    eventId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendeesViewModel = viewModel(
        key = "attendees-$eventId",
        factory = AttendeesViewModel.factory(eventId)
    )
) {
    val state by viewModel.uiState.collectAsState()
    AttendeesContent(
        state = state,
        onBack = onBack,
        onSelectEvent = viewModel::selectEvent,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun AttendeesContent(
    state: AttendeesUiState,
    onBack: () -> Unit,
    onSelectEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Voltar") }
            Text("Inscritos", style = MaterialTheme.typography.headlineMedium)
        }

        // Switch between the organizer's events
        if (state.events.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.events, key = { it.id }) { event ->
                        FilterChip(
                            selected = event.id == state.selectedEventId,
                            onClick = { onSelectEvent(event.id) },
                            label = { Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }

        state.selectedEvent?.let { event -> item { EventSummary(event, state) } }

        item { Text("Estudantes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            state.attendees.isEmpty() -> item { Text("Ainda sem inscritos.") }
            else -> items(state.attendees, key = { it.uid }) { attendee ->
                AttendeeRow(attendee, showAttendance = state.hasStarted)
            }
        }
    }
}

@Composable
private fun EventSummary(event: Event, state: AttendeesUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${DateFormats.shortDateTime(event.startAt)} · ${event.location}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    "${state.attendees.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("de ${event.capacity} vagas", style = MaterialTheme.typography.bodySmall)
                    if (state.hasStarted) {
                        Text("${state.attendedCount} presenças", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.waitingCount > 0) {
                        Text("${state.waitingCount} à espera de vaga", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendeeRow(attendee: Attendee, showAttendance: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(attendee.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attendee.name, fontWeight = FontWeight.Bold)
            if (attendee.email.isNotBlank()) {
                Text(attendee.email, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showAttendance) {
            Text(
                if (attendee.attended) "Presente" else "Não confirmou",
                style = MaterialTheme.typography.labelMedium,
                color = if (attendee.attended) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AttendeesPreview() {
    val event = Event(id = "1", title = "Workshop de Excel", startAt = Timestamp.now(),
        location = "Sala B204", capacity = 30, freeSeats = 27)
    TapEventTheme {
        AttendeesContent(
            state = AttendeesUiState(
                isLoading = false,
                events = listOf(event, event.copy(id = "2", title = "Palestra de carreira")),
                selectedEventId = "1",
                hasStarted = true,
                waitingCount = 2,
                attendees = listOf(
                    Attendee("a", "Ana Costa", "ana.costa@umaia.pt", attended = true),
                    Attendee("b", "Rui Matos", "rui.matos@umaia.pt", attended = false),
                    Attendee("c", "Sofia Reis", "sofia.reis@umaia.pt", attended = true)
                )
            ),
            onBack = {}, onSelectEvent = {}
        )
    }
}
