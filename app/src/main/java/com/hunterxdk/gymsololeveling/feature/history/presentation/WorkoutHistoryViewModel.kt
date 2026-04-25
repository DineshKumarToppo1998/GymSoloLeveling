package com.hunterxdk.gymsololeveling.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutHistoryRepository
import com.hunterxdk.gymsololeveling.feature.history.data.WorkoutSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val historyRepository: WorkoutHistoryRepository,
) : ViewModel() {

    val workouts: StateFlow<List<WorkoutSummary>> = historyRepository.getWorkoutHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
