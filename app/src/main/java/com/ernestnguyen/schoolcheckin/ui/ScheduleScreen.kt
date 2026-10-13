@file:OptIn(ExperimentalMaterial3Api::class)

package com.ernestnguyen.schoolcheckin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ernestnguyen.schoolcheckin.R
import com.ernestnguyen.schoolcheckin.portal.ScheduleEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

/** Same palette the portal uses to tell class sections apart. */
private val CLASS_COLORS = listOf(
    Color(0xFF223771),
    Color(0xFFD49F3A),
    Color(0xFFEC4C00),
    Color(0xFF5A7ADB),
    Color(0xFF3C5398),
)

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onSignOut: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val checkIn by viewModel.checkIn.collectAsStateWithLifecycle()

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    LifecycleStartEffect(viewModel) {
        viewModel.offerCurrentClass()
        onStopOrDispose {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Schedule")
                        Text(
                            weekLabel(state.weekStart),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::goToToday) {
                        Icon(painterResource(R.drawable.ic_today), contentDescription = "Today")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(painterResource(R.drawable.ic_logout), contentDescription = "Sign out")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            WeekStrip(
                state = state,
                onPrevious = viewModel::previousWeek,
                onNext = viewModel::nextWeek,
                onSelect = viewModel::selectDay,
            )
            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                DayList(state = state, onRetry = viewModel::refresh, onOpen = viewModel::openCheckIn)
            }
        }
    }

    checkIn?.let { sheet ->
        CheckInSheet(state = sheet, onSubmit = viewModel::submitCheckIn, onDismiss = viewModel::dismissCheckIn)
    }
}

@Composable
private fun WeekStrip(
    state: ScheduleUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val busyDays = remember(state.entries) { state.entries.mapTo(HashSet()) { it.date } }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous week")
        }
        state.weekDays.forEach { day ->
            DayCell(
                day = day,
                selected = day == state.selectedDay,
                isToday = day == today,
                busy = day in busyDays,
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next week")
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        selected -> colors.primary
        isToday -> colors.primaryContainer
        else -> Color.Transparent
    }
    val content = when {
        selected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
        )
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (busy) content else Color.Transparent))
    }
}

@Composable
private fun DayList(
    state: ScheduleUiState,
    onRetry: () -> Unit,
    onOpen: (ScheduleEntry) -> Unit,
) {
    val entries = state.dayEntries
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                state.selectedDay.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.error?.let { error ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onRetry) { Text("Try again") }
                    }
                }
            }
        }
        if (entries.isEmpty() && !state.loading && state.error == null) {
            item {
                Text(
                    "No classes this day.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(entries, key = { it.id }) { entry ->
            EntryCard(entry = entry, onClick = { onOpen(entry) })
        }
    }
}

@Composable
private fun EntryCard(entry: ScheduleEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(classColor(entry)))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(timeLine(entry), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(entry.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOfNotNull(entry.room?.let { "Room $it" }, entry.classSection, entry.lecturer).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.isExam) {
                    Text("Exam", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                entry.attendance?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
internal fun CheckInSheet(
    state: CheckInUiState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val entry = state.entry
    var keyword by rememberSaveable(entry.id) { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(entry.subject, style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(
                    entry.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
                    timeLine(entry),
                    entry.room,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.attendance?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            }
            if (entry.isExam) {
                Text("Check-in isn't available for exams.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Attendance keyword") },
                singleLine = true,
                enabled = !state.sending,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit(keyword) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
            if (state.focusKeyword) {
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(entry.id) {
                    // Wait until the sheet has finished opening; before that its window has no focus and the keyboard won't show.
                    snapshotFlow { sheetState.isVisible }.first { it }
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            }
            Text(
                "IP shown to the lecturer: " + when {
                    state.ip == null -> "looking up…"
                    state.ip.isBlank() -> "unknown"
                    else -> state.ip
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onSubmit(keyword) },
                enabled = keyword.isNotBlank() && !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text("Check in")
                }
            }
            state.result?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun classColor(entry: ScheduleEntry) =
    CLASS_COLORS[Math.floorMod(entry.classSectionId.hashCode(), CLASS_COLORS.size)]

private fun timeLine(entry: ScheduleEntry): String {
    val detail = when {
        entry.isExam -> entry.examShift
        entry.firstPeriod != null -> "Periods ${entry.firstPeriod}–${entry.lastPeriod ?: entry.firstPeriod}"
        else -> null
    }
    return listOfNotNull(entry.timeRange, detail).joinToString(" · ")
}

private fun weekLabel(start: LocalDate): String {
    val end = start.plusDays(6)
    val format = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    return "${start.format(format)} – ${end.format(format)} ${end.year}"
}
