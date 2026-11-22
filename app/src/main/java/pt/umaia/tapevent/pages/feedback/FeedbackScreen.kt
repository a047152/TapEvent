package pt.umaia.tapevent.pages.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.MAX_COMMENT_LENGTH
import pt.umaia.tapevent.core.ui.RatingStars
import pt.umaia.tapevent.core.util.DateFormats
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun FeedbackScreen(
    eventId: String,
    onBack: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = viewModel(
        key = "feedback-$eventId",
        factory = FeedbackViewModel.factory(eventId)
    )
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.sent) {
        if (state.sent) {
            Toast.makeText(context, "Obrigado pelo teu feedback!", Toast.LENGTH_SHORT).show()
            onSent()
        }
    }

    FeedbackContent(
        state = state,
        onBack = onBack,
        onRatingChange = viewModel::onRatingChange,
        onCommentChange = viewModel::onCommentChange,
        onSubmit = viewModel::submit,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@Composable
fun FeedbackContent(
    state: FeedbackUiState,
    onBack: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readOnly = state.existingFeedback != null

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Voltar") }
        Text("Feedback do evento", style = MaterialTheme.typography.headlineMedium)

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        state.event?.let { event ->
            Spacer(Modifier.height(8.dp))
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${DateFormats.shortDateTime(event.startAt)} · ${event.organizerName}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            if (readOnly) "A tua nota" else "Como correu o evento?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        RatingStars(
            rating = state.rating,
            onRatingChange = if (readOnly) null else onRatingChange
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.comment,
            onValueChange = onCommentChange,
            label = { Text("Comentário (opcional)") },
            placeholder = { Text("O que correu bem? O que podia melhorar?") },
            readOnly = readOnly,
            minLines = 4,
            supportingText = if (readOnly) null else {
                { Text("${state.comment.length}/$MAX_COMMENT_LENGTH") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "A tua nota e o comentário são vistos pelo organizador e pela administração.",
            style = MaterialTheme.typography.bodySmall
        )

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        if (readOnly) {
            Text("Já enviaste o teu feedback. Obrigado!", style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(
                onClick = onSubmit,
                enabled = !state.isSending && state.rating > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Enviar feedback")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun FeedbackPreview() {
    TapEventTheme {
        FeedbackContent(
            state = FeedbackUiState(
                isLoading = false,
                event = Event(
                    id = "1",
                    title = "Workshop de Excel",
                    startAt = Timestamp.now(),
                    organizerName = "Associação de Estudantes"
                ),
                rating = 4,
                comment = "Muito útil."
            ),
            onBack = {}, onRatingChange = {}, onCommentChange = {}, onSubmit = {}
        )
    }
}
