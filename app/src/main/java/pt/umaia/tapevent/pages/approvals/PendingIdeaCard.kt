package pt.umaia.tapevent.pages.approvals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.Idea

/** A student idea waiting for approval before voting opens. */
@Composable
fun PendingIdeaCard(
    idea: Idea,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(idea.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${idea.authorName.ifBlank { "Estudante" }} · antes de abrir a votação",
                style = MaterialTheme.typography.bodySmall
            )
            Text(idea.theme, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onApprove, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
                    Text("Aprovar votação")
                }
                OutlinedButton(onClick = onReject, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
                    Text("Rejeitar")
                }
            }
        }
    }
}
