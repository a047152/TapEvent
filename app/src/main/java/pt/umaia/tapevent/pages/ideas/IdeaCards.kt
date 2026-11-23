package pt.umaia.tapevent.pages.ideas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.Idea
import pt.umaia.tapevent.core.model.IdeaStatus

/** An idea open for voting: bar relative to the most voted idea, and the vote button. */
@Composable
fun VoteCard(
    idea: Idea,
    maxVotes: Int,
    hasVoted: Boolean,
    isVoting: Boolean,
    onToggleVote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("${idea.votes}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { idea.votes.toFloat() / maxVotes },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasVoted) {
                    OutlinedButton(onClick = onToggleVote, enabled = !isVoting) { Text("Votaste · retirar voto") }
                } else {
                    Button(onClick = onToggleVote, enabled = !isVoting) { Text("Votar") }
                }
                Text(
                    idea.theme,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
        }
    }
}

/** One of the student's own ideas, with its status. */
@Composable
fun MyIdeaCard(idea: Idea, modifier: Modifier = Modifier) {
    val (text, color) = when (idea.status) {
        IdeaStatus.APPROVED -> "Aprovada, já podes votar" to MaterialTheme.colorScheme.primary
        IdeaStatus.REJECTED -> "Não aprovada" to MaterialTheme.colorScheme.error
        else -> "A aguardar aprovação da administração" to MaterialTheme.colorScheme.tertiary
    }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(idea.title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
