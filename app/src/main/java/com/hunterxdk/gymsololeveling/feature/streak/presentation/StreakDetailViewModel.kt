package com.hunterxdk.gymsololeveling.feature.streak.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.core.service.StreakService
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StreakDetailUiState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalActiveDays: Int = 0,
    val streakStartDate: String? = null,
    val activeDaysSet: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class StreakDetailViewModel @Inject constructor(
    private val streakService: StreakService,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<StreakDetailUiState> = workoutHistoryRepository
        .getWorkoutDates()
        .map { dates ->
            val millis = dates.map { it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            val streakData = streakService.getStreakData(millis)
            StreakDetailUiState(
                currentStreak = streakData.currentStreak,
                bestStreak = streakData.bestStreak,
                totalActiveDays = dates.size,
                streakStartDate = streakData.streakStartDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                activeDaysSet = dates.toSet(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakDetailUiState())
}