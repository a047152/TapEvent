package pt.umaia.tapevent.pages.approvals

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun ApprovalsScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApprovalsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    ApprovalsContent(
        state = state,
        onApprove = viewModel::approve,
        onReject = viewModel::askToReject,
        onSignOut = onSignOut,
        modifier = modifier
    )

    state.eventToReject?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRejection,
            title = { Text("Rejeitar evento?") },
            text = { Text("\"${event.title}\" não vai ser publicado. O organizador pode criá-lo outra vez.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRejection) { Text("Rejeitar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRejection) { Text("Cancelar") } }
        )
    }
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun ApprovalsContent(
    state: ApprovalsUiState,
    onApprove: (Event) -> Unit,
    onReject: (Event) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Serviços e associação", style = MaterialTheme.typography.labelLarge)
            Text("Aprovações", style = MaterialTheme.typography.headlineMedium)
        }

        item { PendingCounter(count = state.pendingEvents.size, isLoading = state.isLoading) }

        item {
            Text(
                "Eventos dos organizadores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
            state.pendingEvents.isEmpty() -> item {
                Text("Sem eventos por aprovar.", style = MaterialTheme.typography.bodyMedium)
            }
            else -> items(state.pendingEvents, key = { it.id }) { event ->
                PendingEventCard(
                    event = event,
                    isProcessing = event.id in state.processingIds,
                    onApprove = { onApprove(event) },
                    onReject = { onReject(event) }
                )
            }
        }

        // TODO (plan step 11): "Ideas from students" section, after Propose & vote exists.

        item {
            TextButton(onClick = onSignOut) { Text("Sair") }
        }
    }
}

@Composable
private fun PendingCounter(count: Int, isLoading: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isLoading) "–" else "$count",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text("  eventos por aprovar", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ApprovalsPreview() {
    val sample = Event(
        id = "1",
        title = "Workshop de Excel",
        description = "Aprende fórmulas, tabelas e gráficos. Traz o portátil.",
        format = "Workshop",
        theme = "Tecnologia",
        startAt = Timestamp.now(),
        location = "Sala B204",
        organizerName = "Associação de Estudantes",
        capacity = 30
    )
    TapEventTheme {
        ApprovalsContent(
            state = ApprovalsUiState(isLoading = false, pendingEvents = listOf(sample, sample.copy(id = "2"))),
            onApprove = {}, onReject = {}, onSignOut = {}
        )
    }
}
