package pt.umaia.tapevent.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/** A button shown on a placeholder home (e.g. "Create event" for organizers). */
data class HomeAction(val label: String, val onClick: () -> Unit)

/**
 * Temporary home for each role. Replaced in the next steps by:
 * student -> Discover, organizer -> Dashboard, admin -> Approvals.
 */
@Composable
fun HomePlaceholderScreen(
    title: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    actions: List<HomeAction> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(Firebase.auth.currentUser?.email.orEmpty())
        Spacer(Modifier.height(24.dp))
        actions.forEach { action ->
            Button(onClick = action.onClick) { Text(action.label) }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
    }
}
