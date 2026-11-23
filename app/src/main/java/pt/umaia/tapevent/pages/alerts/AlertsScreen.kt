package pt.umaia.tapevent.pages.alerts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import pt.umaia.tapevent.core.model.AlertSettings
import pt.umaia.tapevent.core.model.EVENT_THEMES
import pt.umaia.tapevent.ui.theme.TapEventTheme

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun AlertsScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    AlertsContent(
        state = state,
        onToggleInterest = viewModel::toggleInterest,
        onSetAlert = viewModel::setAlert,
        onSignOut = onSignOut,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertsContent(
    state: AlertsUiState,
    onToggleInterest: (String) -> Unit,
    onSetAlert: (key: String, enabled: Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Avisos e interesses", style = MaterialTheme.typography.headlineMedium)

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Spacer(Modifier.height(16.dp))
        Text("Avisa-me de novos eventos sobre", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EVENT_THEMES.forEach { theme ->
                FilterChip(
                    selected = theme in state.interests,
                    onClick = { onToggleInterest(theme) },
                    label = { Text(theme) }
                )
            }
        }
        val chosen = EVENT_THEMES.filter { it in state.interests }
        Text(
            if (chosen.isEmpty()) "Escolhe pelo menos um tema."
            else "Vais receber avisos de: ${chosen.joinToString(", ")}.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(20.dp))
        AlertSwitch("Novos eventos do meu interesse", state.alerts.newEvents) {
            onSetAlert(AlertSettings.KEY_NEW, it)
        }
        AlertSwitch("Lembrete 1 dia antes", state.alerts.oneDayBefore) {
            onSetAlert(AlertSettings.KEY_DAY, it)
        }
        AlertSwitch("Lembrete 1 hora antes", state.alerts.oneHourBefore) {
            onSetAlert(AlertSettings.KEY_HOUR, it)
        }
        AlertSwitch("Últimas vagas num evento que marquei", state.alerts.fewSeats) {
            onSetAlert(AlertSettings.KEY_FEW, it)
        }

        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.TextButton(onClick = onSignOut) { Text("Sair") }
    }
}

@Composable
private fun AlertSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        HorizontalDivider()
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AlertsPreview() {
    TapEventTheme {
        AlertsContent(
            state = AlertsUiState(isLoading = false, interests = setOf("Tecnologia", "Carreira")),
            onToggleInterest = {}, onSetAlert = { _, _ -> }, onSignOut = {}
        )
    }
}
