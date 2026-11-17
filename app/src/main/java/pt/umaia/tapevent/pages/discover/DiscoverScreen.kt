package pt.umaia.tapevent.pages.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import pt.umaia.tapevent.core.model.EVENT_THEMES
import pt.umaia.tapevent.core.model.Event
import pt.umaia.tapevent.core.model.TimeSlot
import pt.umaia.tapevent.core.ui.EventCard
import pt.umaia.tapevent.core.util.DateFormats
import pt.umaia.tapevent.ui.theme.TapEventTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Labels for the "I'm free" chips, mapped to the stored timeSlot values. */
private val TIME_SLOT_LABELS = listOf(
    TimeSlot.MORNING to "Manhã",
    TimeSlot.AFTERNOON to "Tarde",
    TimeSlot.NIGHT to "Noite"
)

/** Stateful screen: connects the ViewModel to the UI. */
@Composable
fun DiscoverScreen(
    onEventClick: (Event) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    DiscoverContent(
        state = state,
        onDayClick = viewModel::selectDay,
        onThemeClick = viewModel::toggleTheme,
        onTimeSlotClick = viewModel::toggleTimeSlot,
        onWeekendClick = viewModel::toggleWeekend,
        onClearFilters = viewModel::clearFilters,
        onEventClick = onEventClick,
        onSignOut = onSignOut,
        modifier = modifier
    )
}

/** Stateless UI: easy to preview and to restyle in Phase 3. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverContent(
    state: DiscoverUiState,
    onDayClick: (LocalDate) -> Unit,
    onThemeClick: (String) -> Unit,
    onTimeSlotClick: (String) -> Unit,
    onWeekendClick: () -> Unit,
    onClearFilters: () -> Unit,
    onEventClick: (Event) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.userName.isBlank()) "Olá" else "Olá, ${state.userName.substringBefore(" ")}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text("Hoje: ${state.today.format(TODAY_FORMAT)}", style = MaterialTheme.typography.labelMedium)
            }
            Text("O que há para fazer?", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            WeekStrip(
                days = state.weekDays,
                selectedDay = state.filters.selectedDay,
                onDayClick = onDayClick
            )
        }

        // Filters
        item {
            FilterLabel("Tema")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EVENT_THEMES.forEach { theme ->
                    FilterChip(
                        selected = theme in state.filters.themes,
                        onClick = { onThemeClick(theme) },
                        label = { Text(theme) }
                    )
                }
            }
        }
        item {
            FilterLabel("Estou livre")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TIME_SLOT_LABELS.forEach { (slot, label) ->
                    FilterChip(
                        selected = slot in state.filters.timeSlots,
                        onClick = { onTimeSlotClick(slot) },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = state.filters.weekendOnly,
                    onClick = onWeekendClick,
                    label = { Text("Fim de semana") }
                )
            }
        }

        // Results
        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> item {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            state.isEmpty -> item {
                EmptyResults(filtersActive = state.filters.isActive, onClearFilters = onClearFilters)
            }
            else -> {
                eventSection("Esta semana", state.thisWeek, onEventClick)
                eventSection("Mais tarde", state.later, onEventClick)
            }
        }

        item {
            TextButton(onClick = onSignOut) { Text("Sair") }
        }
    }
}

private fun LazyListScope.eventSection(
    title: String,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    if (events.isEmpty()) return
    item(key = "header-$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    items(events, key = { it.id }) { event ->
        EventCard(event = event, onClick = { onEventClick(event) })
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun EmptyResults(filtersActive: Boolean, onClearFilters: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        if (filtersActive) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Nenhum evento com estes filtros.\nTira um filtro para ver mais.",
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = onClearFilters, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Limpar filtros")
                }
            }
        } else {
            Text("Ainda não há eventos marcados.", textAlign = TextAlign.Center)
        }
    }
}

private val TODAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", DateFormats.LOCALE_PT)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DiscoverPreview() {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sample = Event(
        id = "1",
        title = "Workshop de Excel",
        format = "Workshop",
        theme = "Tecnologia",
        startAt = Timestamp.now(),
        location = "Sala B204",
        capacity = 30,
        freeSeats = 12
    )
    TapEventTheme {
        DiscoverContent(
            state = DiscoverUiState(
                isLoading = false,
                userName = "Ana Costa",
                today = today,
                weekDays = (0L..6L).map {
                    WeekDay(monday.plusDays(it), monday.plusDays(it) == today, (it % 3).toInt())
                },
                filters = DiscoverFilters(themes = setOf("Tecnologia")),
                thisWeek = listOf(sample),
                later = listOf(sample.copy(id = "2", title = "Torneio de futsal", freeSeats = 0))
            ),
            onDayClick = {}, onThemeClick = {}, onTimeSlotClick = {}, onWeekendClick = {},
            onClearFilters = {}, onEventClick = {}, onSignOut = {}
        )
    }
}
