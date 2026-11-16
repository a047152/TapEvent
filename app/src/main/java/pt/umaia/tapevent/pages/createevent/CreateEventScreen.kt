package pt.umaia.tapevent.pages.createevent

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.umaia.tapevent.core.model.EVENT_FORMATS
import pt.umaia.tapevent.core.model.EVENT_THEMES
import pt.umaia.tapevent.ui.theme.TapEventTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.submitted) {
        if (state.submitted) {
            Toast.makeText(context, "Event sent for approval.", Toast.LENGTH_SHORT).show()
            onSubmitted()
        }
    }

    CreateEventContent(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onFormatSelect = viewModel::onFormatSelect,
        onThemeSelect = viewModel::onThemeSelect,
        onDateSelect = viewModel::onDateSelect,
        onTimeSelect = viewModel::onTimeSelect,
        onLocationChange = viewModel::onLocationChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onIncreaseCapacity = viewModel::increaseCapacity,
        onDecreaseCapacity = viewModel::decreaseCapacity,
        onSubmit = viewModel::submit,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEventContent(
    state: CreateEventUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onFormatSelect: (String) -> Unit,
    onThemeSelect: (String) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onTimeSelect: (LocalTime) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIncreaseCapacity: () -> Unit,
    onDecreaseCapacity: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text("Create event", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            placeholder = { Text("E.g.: Excel workshop") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SectionLabel("Format")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EVENT_FORMATS.forEach { format ->
                FilterChip(
                    selected = state.format == format,
                    onClick = { onFormatSelect(format) },
                    label = { Text(format) }
                )
            }
        }

        SectionLabel("Theme")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EVENT_THEMES.forEach { theme ->
                FilterChip(
                    selected = state.theme == theme,
                    onClick = { onThemeSelect(theme) },
                    label = { Text(theme) }
                )
            }
        }

        SectionLabel("Date and time")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Text(state.date?.format(DATE_FORMAT) ?: "Choose date")
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                Text(state.time?.format(TIME_FORMAT) ?: "Choose time")
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            placeholder = { Text("Room or space") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("What will happen, who it is for and what to bring") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        SectionLabel("Free UMAIA seats")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDecreaseCapacity, enabled = state.capacity > MIN_CAPACITY) {
                Text("−")
            }
            Text(
                "${state.capacity}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            OutlinedButton(onClick = onIncreaseCapacity) { Text("+") }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Send for approval")
            }
        }
        Text(
            "The event is only visible to students after the administration approves it.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (showDatePicker) {
        EventDatePickerDialog(
            initialDate = state.date,
            onConfirm = { onDateSelect(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showTimePicker) {
        EventTimePickerDialog(
            initialTime = state.time,
            onConfirm = { onTimeSelect(it); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun CreateEventPreview() {
    TapEventTheme {
        CreateEventContent(
            state = CreateEventUiState(
                title = "Excel workshop",
                format = "Workshop",
                theme = "Tecnologia",
                date = LocalDate.now().plusDays(7),
                time = LocalTime.of(18, 0),
                location = "Room B204"
            ),
            onBack = {}, onTitleChange = {}, onFormatSelect = {}, onThemeSelect = {},
            onDateSelect = {}, onTimeSelect = {}, onLocationChange = {}, onDescriptionChange = {},
            onIncreaseCapacity = {}, onDecreaseCapacity = {}, onSubmit = {}
        )
    }
}
