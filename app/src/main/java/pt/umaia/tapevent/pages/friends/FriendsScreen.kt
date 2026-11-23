package pt.umaia.tapevent.pages.friends

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun FriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    FriendsContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSendRequest = viewModel::sendRequest,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onCancelSent = viewModel::cancelSent,
        onRemove = viewModel::askToRemove,
        modifier = modifier
    )

    state.friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemove,
            title = { Text("Remover amigo?") },
            text = { Text("${friend.name} deixa de estar na tua lista de amigos.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRemove) { Text("Remover") } },
            dismissButton = { TextButton(onClick = viewModel::dismissRemove) { Text("Cancelar") } }
        )
    }
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun FriendsContent(
    state: FriendsUiState,
    onEmailChange: (String) -> Unit,
    onSendRequest: () -> Unit,
    onAccept: (FriendItem) -> Unit,
    onDecline: (FriendItem) -> Unit,
    onCancelSent: (FriendItem) -> Unit,
    onRemove: (FriendItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Amigos", style = MaterialTheme.typography.headlineMedium) }

        // Add a friend
        item {
            OutlinedTextField(
                value = state.emailInput,
                onValueChange = onEmailChange,
                label = { Text("Adicionar um amigo") },
                placeholder = { Text("nome.apelido@umaia.pt") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Só estudantes, com email institucional.", style = MaterialTheme.typography.bodySmall)
            state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSendRequest,
                enabled = !state.isSending && state.emailInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                if (state.isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Enviar pedido")
            }
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }
        state.errorMessage?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }

        if (state.received.isNotEmpty()) {
            header("Pedidos recebidos")
            items(state.received, key = { "r-${it.friendshipId}" }) { item ->
                val busy = item.friendshipId in state.processingIds
                FriendRow(item, subtitle = item.email) {
                    Button(onClick = { onAccept(item) }, enabled = !busy) { Text("Aceitar") }
                    TextButton(onClick = { onDecline(item) }, enabled = !busy) { Text("Recusar") }
                }
            }
        }

        if (state.sent.isNotEmpty()) {
            header("Pedidos enviados")
            items(state.sent, key = { "s-${it.friendshipId}" }) { item ->
                FriendRow(item, subtitle = "A aguardar resposta") {
                    TextButton(
                        onClick = { onCancelSent(item) },
                        enabled = item.friendshipId !in state.processingIds
                    ) { Text("Cancelar") }
                }
            }
        }

        header("Os teus amigos")
        if (state.friends.isEmpty()) {
            item { Text("Ainda sem amigos.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.friends, key = { "f-${it.friendshipId}" }) { item ->
                FriendRow(item, subtitle = item.email) {
                    TextButton(
                        onClick = { onRemove(item) },
                        enabled = item.friendshipId !in state.processingIds
                    ) { Text("Remover") }
                }
            }
        }
    }
}

private fun LazyListScope.header(title: String) {
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun FriendsPreview() {
    TapEventTheme {
        FriendsContent(
            state = FriendsUiState(
                isLoading = false,
                received = listOf(FriendItem("1", "Rui Matos", "rui.matos@umaia.pt")),
                sent = listOf(FriendItem("2", "Sofia Reis", "sofia.reis@umaia.pt")),
                friends = listOf(FriendItem("3", "Ana Costa", "ana.costa@umaia.pt"))
            ),
            onEmailChange = {}, onSendRequest = {}, onAccept = {}, onDecline = {},
            onCancelSent = {}, onRemove = {}
        )
    }
}
