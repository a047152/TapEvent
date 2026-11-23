package pt.umaia.tapevent.pages.ideas

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import pt.umaia.tapevent.core.model.EVENT_THEMES
import pt.umaia.tapevent.core.model.Idea
import pt.umaia.tapevent.core.model.IdeaStatus
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun IdeasScreen(
    modifier: Modifier = Modifier,
    viewModel: IdeasViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    IdeasContent(
        state = state,
        onToggleVote = viewModel::toggleVote,
        onTitleChange = viewModel::onTitleChange,
        onThemeSelect = viewModel::onThemeSelect,
        onPropose = viewModel::propose,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeasContent(
    state: IdeasUiState,
    onToggleVote: (Idea) -> Unit,
    onTitleChange: (String) -> Unit,
    onThemeSelect: (String) -> Unit,
    onPropose: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Propor e votar", style = MaterialTheme.typography.headlineMedium)
            Text("Os mais votados passam a evento oficial.", style = MaterialTheme.typography.bodySmall)
        }

        header("Em votação")
        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item { Text(state.errorMessage, color = MaterialTheme.colorScheme.error) }
            state.openIdeas.isEmpty() -> item { Text("Ainda sem ideias em votação.") }
            else -> items(state.openIdeas, key = { "open-${it.id}" }) { idea ->
                VoteCard(
                    idea = idea,
                    maxVotes = state.maxVotes,
                    hasVoted = idea.id in state.myVotes,
                    isVoting = idea.id in state.votingIds,
                    onToggleVote = { onToggleVote(idea) }
                )
            }
        }

        if (state.myIdeas.isNotEmpty()) {
            header("As tuas ideias")
            items(state.myIdeas, key = { "mine-${it.id}" }) { MyIdeaCard(it) }
        }

        header("Tens uma ideia?")
        item {
            OutlinedTextField(
                value = state.newTitle,
                onValueChange = onTitleChange,
                label = { Text("Evento") },
                placeholder = { Text("Ex.: Feira de livros usados") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Tema", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EVENT_THEMES.forEach { theme ->
                    FilterChip(
                        selected = state.newTheme == theme,
                        onClick = { onThemeSelect(theme) },
                        label = { Text(theme) }
                    )
                }
            }
            state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPropose, enabled = !state.isProposing, modifier = Modifier.fillMaxWidth()) {
                if (state.isProposing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Propor evento")
            }
            Text(
                "A administração aprova a ideia antes de abrir a votação.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun LazyListScope.header(title: String) {
    item(key = "header-$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun IdeasPreview() {
    TapEventTheme {
        IdeasContent(
            state = IdeasUiState(
                isLoading = false,
                openIdeas = listOf(
                    Idea("1", "Cinema ao ar livre no pátio", "Cultura e lazer", status = IdeaStatus.APPROVED, votes = 41),
                    Idea("2", "Visita a uma startup do Porto", "Tecnologia", status = IdeaStatus.APPROVED, votes = 33)
                ),
                myVotes = setOf("1"),
                myIdeas = listOf(Idea("3", "Noite de jogos de tabuleiro", "Cultura e lazer"))
            ),
            onToggleVote = {}, onTitleChange = {}, onThemeSelect = {}, onPropose = {}
        )
    }
}
