package pt.umaia.tapevent.pages.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun DashboardScreen(
    onCreateEvent: () -> Unit,
    onViewAttendees: (eventId: String) -> Unit,
    onViewFeedback: (eventId: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    DashboardContent(
        state = state,
        onCreateEvent = onCreateEvent,
        onViewAttendees = onViewAttendees,
        onViewFeedback = onViewFeedback,
        onSignOut = onSignOut,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onCreateEvent: () -> Unit,
    onViewAttendees: (String) -> Unit,
    onViewFeedback: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(state.organizerName.ifBlank { "Organizador" }, style = MaterialTheme.typography.labelLarge)
            Text("Os teus eventos", style = MaterialTheme.typography.headlineMedium)
        }
        item { DashboardStats(published = state.publishedCount, registered = state.totalRegistered) }
        item {
            Button(onClick = onCreateEvent, modifier = Modifier.fillMaxWidth()) { Text("Criar evento") }
        }

        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                if (state.pending.isNotEmpty()) {
                    header("Por aprovar")
                    items(state.pending, key = { it.id }) {
                        StatusEventCard(it, "A aguardar a administração")
                    }
                }
                header("Publicados")
                if (state.published.isEmpty()) {
                    item { Text("Ainda não tens eventos publicados.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(state.published, key = { it.event.id }) { item ->
                        PublishedEventCard(
                            item = item,
                            onViewAttendees = { onViewAttendees(item.event.id) },
                            onViewFeedback = { onViewFeedback(item.event.id) }
                        )
                    }
                }
                if (state.rejected.isNotEmpty()) {
                    header("Rejeitados")
                    items(state.rejected, key = { it.id }) {
                        StatusEventCard(it, "Não foi aprovado pela administração")
                    }
                }
            }
        }

        item { TextButton(onClick = onSignOut) { Text("Sair") } }
    }
}

private fun LazyListScope.header(title: String) {
    item(key = "header-$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DashboardPreview() {
    val event = Event(id = "1", title = "Workshop de Excel", theme = "Tecnologia",
        startAt = Timestamp.now(), capacity = 30, freeSeats = 12)
    TapEventTheme {
        DashboardContent(
            state = DashboardUiState(
                isLoading = false,
                organizerName = "Associação de Estudantes",
                pending = listOf(event.copy(id = "p", title = "Torneio de xadrez")),
                published = listOf(
                    PublishedEvent(event, hasStarted = false, registeredCount = 18, averageRating = null, feedbackCount = 0),
                    PublishedEvent(event.copy(id = "2", title = "Palestra de carreira"), true, 40, 4.3, 12)
                )
            ),
            onCreateEvent = {}, onViewAttendees = {}, onViewFeedback = {}, onSignOut = {}
        )
    }
}
