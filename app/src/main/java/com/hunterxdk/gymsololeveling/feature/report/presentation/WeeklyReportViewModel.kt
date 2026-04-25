package com.hunterxdk.gymsololeveling.feature.report.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReportUiState())
    val uiState: StateFlow<WeeklyReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadWeeklyData() }
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val weekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
            val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            historyRepository.getWorkoutHistory().collect { workouts ->
                val thisWeek = workouts.filter { it.startedAt >= weekStartMillis }
                val activeDays = thisWeek.map { summary ->
                    Instant.ofEpochMilli(summary.startedAt)
                        .atZone(ZoneId.systemDefault())
                        .dayOfWeek
                }.toSet()

                _uiState.update {
                    it.copy(
                        weeklyWorkouts = thisWeek.size,
                        weeklyXP = thisWeek.sumOf { w -> w.totalXp },
                        weeklyPRs = thisWeek.sumOf { w -> w.prCount },
                        activeDays = activeDays,
                    )
                }
            }
        }
    }
}

data class WeeklyReportUiState(
    val weeklyWorkouts: Int = 0,
    val weeklyXP: Int = 0,
    val weeklyPRs: Int = 0,
    val activeDays: Set<DayOfWeek> = emptySet(),
    val topMuscles: List<Pair<MuscleGroup, Int>> = emptyList(),
)
