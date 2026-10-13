package com.ernestnguyen.schoolcheckin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ernestnguyen.schoolcheckin.portal.PortalApi
import com.ernestnguyen.schoolcheckin.portal.PortalException
import com.ernestnguyen.schoolcheckin.portal.PublicIp
import com.ernestnguyen.schoolcheckin.portal.ScheduleEntry
import com.ernestnguyen.schoolcheckin.portal.SessionExpiredException
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OFFLINE_MESSAGE = "Couldn't reach the portal. Check your connection and try again."

data class ScheduleUiState(
    val weekStart: LocalDate,
    val selectedDay: LocalDate,
    val entries: List<ScheduleEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false,
) {
    val weekDays: List<LocalDate> get() = (0L..6L).map { weekStart.plusDays(it) }
    val dayEntries: List<ScheduleEntry> get() = entries.filter { it.date == selectedDay }
}

data class CheckInUiState(
    val entry: ScheduleEntry,
    /** IP that will be reported; null while it is being looked up, blank if unknown. */
    val ip: String? = null,
    val sending: Boolean = false,
    val result: String? = null,
    val succeeded: Boolean = false,
)

class ScheduleViewModel(
    private val api: PortalApi,
    private val fallbackIp: String,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LocalDate.now().let { today -> ScheduleUiState(weekStart = today.startOfWeek(), selectedDay = today) }
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private val _checkIn = MutableStateFlow<CheckInUiState?>(null)
    val checkIn: StateFlow<CheckInUiState?> = _checkIn.asStateFlow()

    private val weeks = mutableMapOf<LocalDate, List<ScheduleEntry>>()
    private var loadJob: Job? = null

    init {
        load(force = false)
    }

    fun previousWeek() = moveTo(_state.value.selectedDay.minusWeeks(1))

    fun nextWeek() = moveTo(_state.value.selectedDay.plusWeeks(1))

    fun goToToday() = moveTo(LocalDate.now())

    fun selectDay(day: LocalDate) = _state.update { it.copy(selectedDay = day) }

    fun refresh() = load(force = true)

    fun openCheckIn(entry: ScheduleEntry) {
        _checkIn.value = CheckInUiState(entry)
        if (entry.isExam) return
        viewModelScope.launch {
            val ip = PublicIp.lookup() ?: fallbackIp
            _checkIn.update { current ->
                if (current?.entry?.id == entry.id && current.ip == null) current.copy(ip = ip) else current
            }
        }
    }

    fun dismissCheckIn() {
        _checkIn.value = null
    }

    fun submitCheckIn(keyword: String) {
        val current = _checkIn.value ?: return
        if (current.sending || current.entry.isExam || keyword.isBlank()) return
        _checkIn.value = current.copy(sending = true, result = null)
        viewModelScope.launch {
            val ip = current.ip ?: PublicIp.lookup() ?: fallbackIp
            val outcome = try {
                val message = api.checkIn(current.entry, keyword.trim(), ip)
                current.copy(ip = ip, sending = false, result = message, succeeded = true)
            } catch (e: SessionExpiredException) {
                _state.update { it.copy(sessionExpired = true) }
                null
            } catch (e: PortalException) {
                current.copy(ip = ip, sending = false, result = e.message, succeeded = false)
            } catch (e: IOException) {
                current.copy(ip = ip, sending = false, result = OFFLINE_MESSAGE, succeeded = false)
            }
            _checkIn.update { if (it?.entry?.id == current.entry.id) outcome else it }
            if (outcome?.succeeded == true) load(force = true)
        }
    }

    private fun moveTo(day: LocalDate) {
        val weekStart = day.startOfWeek()
        val weekChanged = weekStart != _state.value.weekStart
        _state.update { it.copy(weekStart = weekStart, selectedDay = day) }
        if (weekChanged) load(force = false)
    }

    private fun load(force: Boolean) {
        val weekStart = _state.value.weekStart
        val cached = weeks[weekStart]
        if (cached != null && !force) {
            loadJob?.cancel()
            _state.update { it.copy(entries = cached, loading = false, error = null) }
            return
        }
        loadJob?.cancel()
        _state.update { it.copy(entries = cached.orEmpty(), loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val entries = api.weekSchedule(weekStart, weekStart.plusDays(6))
                weeks[weekStart] = entries
                _state.update { if (it.weekStart == weekStart) it.copy(entries = entries, loading = false) else it }
            } catch (e: SessionExpiredException) {
                _state.update { it.copy(loading = false, sessionExpired = true) }
            } catch (e: PortalException) {
                _state.update { if (it.weekStart == weekStart) it.copy(loading = false, error = e.message) else it }
            } catch (e: IOException) {
                _state.update { if (it.weekStart == weekStart) it.copy(loading = false, error = OFFLINE_MESSAGE) else it }
            }
        }
    }
}

private fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
