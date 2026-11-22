package pt.umaia.tapevent.pages.myevents

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun MyEventsScreen(
    onOpenTicket: (eventId: String) -> Unit,
    onOpenEvent: (eventId: String) -> Unit,
    onGiveFeedback: (eventId: String) -> Unit,
    onBrowseEvents: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyEventsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    MyEventsContent(
        state = state,
        onOpen = { item ->
            // A seat opens the ticket; a "notify me" request opens the event page
            if (item.isNotifyRequest) onOpenEvent(item.event.id) else onOpenTicket(item.event.id)
        },
        onConfirmAttendance = viewModel::confirmAttendance,
        onGiveFeedback = { item -> onGiveFeedback(item.event.id) },
        onBrowseEvents = onBrowseEvents,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun MyEventsContent(
    state: MyEventsUiState,
    onOpen: (MyEventItem) -> Unit,
    onConfirmAttendance: (MyEventItem) -> Unit,
    onGiveFeedback: (MyEventItem) -> Unit,
    onBrowseEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Os meus eventos", style = MaterialTheme.typography.headlineMedium) }

        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            state.isEmpty -> item { EmptyState(onBrowseEvents) }
            else -> {
                section("Próximos", state.upcoming, state, onOpen, onConfirmAttendance, onGiveFeedback)
                section("Passados", state.past, state, onOpen, onConfirmAttendance, onGiveFeedback)
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<MyEventItem>,
    state: MyEventsUiState,
    onOpen: (MyEventItem) -> Unit,
    onConfirmAttendance: (MyEventItem) -> Unit,
    onGiveFeedback: (MyEventItem) -> Unit
) {
    if (items.isEmpty()) return
    item(key = "header-$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    items(items, key = { it.registration.id }) { item ->
        MyEventCard(
            item = item,
            isConfirming = item.event.id in state.confirmingIds,
            onOpen = { onOpen(item) },
            onConfirmAttendance = { onConfirmAttendance(item) },
            onGiveFeedback = { onGiveFeedback(item) }
        )
    }
}

@Composable
private fun EmptyState(onBrowseEvents: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Ainda não te inscreveste em nenhum evento.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBrowseEvents) { Text("Ver eventos") }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MyEventsPreview() {
    val event = Event(id = "1", title = "Workshop de Excel", startAt = Timestamp.now(), location = "Sala B204")
    fun reg(id: String, status: String) = Registration(id = id, eventId = id, status = status)
    TapEventTheme {
        MyEventsContent(
            state = MyEventsUiState(
                isLoading = false,
                upcoming = listOf(
                    MyEventItem(event, reg("a", RegistrationStatus.REGISTERED), hasStarted = false),
                    MyEventItem(event.copy(title = "Torneio de futsal"), reg("b", RegistrationStatus.NOTIFY), false)
                ),
                past = listOf(
                    MyEventItem(event.copy(title = "Palestra de carreira"), reg("c", RegistrationStatus.REGISTERED), true),
                    MyEventItem(event.copy(title = "Feira de voluntariado"), reg("d", RegistrationStatus.ATTENDED), true)
                )
            ),
            onOpen = {}, onConfirmAttendance = {}, onGiveFeedback = {}, onBrowseEvents = {}
        )
    }
}
